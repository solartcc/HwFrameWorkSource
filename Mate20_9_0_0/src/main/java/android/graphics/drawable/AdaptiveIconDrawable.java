package android.graphics.drawable;

import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.Drawable.Callback;
import android.graphics.drawable.Drawable.ConstantState;
import android.util.AttributeSet;
import android.util.PathParser;
import com.android.internal.R;
import java.io.IOException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class AdaptiveIconDrawable extends Drawable implements Callback {
    private static final int BACKGROUND_ID = 0;
    private static final float DEFAULT_VIEW_PORT_SCALE = 0.6666667f;
    private static final float EXTRA_INSET_PERCENTAGE = 0.25f;
    private static final int FOREGROUND_ID = 1;
    public static final float MASK_SIZE = 100.0f;
    private static final float SAFEZONE_SCALE = 0.9166667f;
    private static Path sMask;
    private final Canvas mCanvas;
    private boolean mChildRequestedInvalidation;
    private Rect mHotspotBounds;
    LayerState mLayerState;
    private Bitmap mLayersBitmap;
    private Shader mLayersShader;
    private final Path mMask;
    private Bitmap mMaskBitmap;
    private final Matrix mMaskMatrix;
    private boolean mMutated;
    private Paint mPaint;
    private boolean mSuspendChildInvalidation;
    private final Rect mTmpOutRect;
    private final Region mTransparentRegion;

    static class ChildDrawable {
        public int mDensity = 160;
        public Drawable mDrawable;
        public int[] mThemeAttrs;

        ChildDrawable(int density) {
            this.mDensity = density;
        }

        ChildDrawable(ChildDrawable orig, AdaptiveIconDrawable owner, Resources res) {
            Drawable clone;
            Drawable dr = orig.mDrawable;
            if (dr != null) {
                ConstantState cs = dr.getConstantState();
                if (cs == null) {
                    clone = dr;
                } else if (res != null) {
                    clone = cs.newDrawable(res);
                } else {
                    clone = cs.newDrawable();
                }
                clone.setCallback(owner);
                clone.setBounds(dr.getBounds());
                clone.setLevel(dr.getLevel());
            } else {
                clone = null;
            }
            this.mDrawable = clone;
            this.mThemeAttrs = orig.mThemeAttrs;
            this.mDensity = Drawable.resolveDensity(res, orig.mDensity);
        }

        public boolean canApplyTheme() {
            return this.mThemeAttrs != null || (this.mDrawable != null && this.mDrawable.canApplyTheme());
        }

        public final void setDensity(int targetDensity) {
            if (this.mDensity != targetDensity) {
                this.mDensity = targetDensity;
            }
        }
    }

    static class LayerState extends ConstantState {
        static final int N_CHILDREN = 2;
        private boolean mAutoMirrored = false;
        int mChangingConfigurations;
        private boolean mCheckedOpacity;
        private boolean mCheckedStateful;
        ChildDrawable[] mChildren;
        int mChildrenChangingConfigurations;
        int mDensity;
        private boolean mIsStateful;
        private int mOpacity;
        int mOpacityOverride = 0;
        int mSrcDensityOverride = 0;
        private int[] mThemeAttrs;

        LayerState(LayerState orig, AdaptiveIconDrawable owner, Resources res) {
            int i = 0;
            this.mDensity = Drawable.resolveDensity(res, orig != null ? orig.mDensity : 0);
            this.mChildren = new ChildDrawable[2];
            if (orig != null) {
                ChildDrawable[] origChildDrawable = orig.mChildren;
                this.mChangingConfigurations = orig.mChangingConfigurations;
                this.mChildrenChangingConfigurations = orig.mChildrenChangingConfigurations;
                while (i < 2) {
                    this.mChildren[i] = new ChildDrawable(origChildDrawable[i], owner, res);
                    i++;
                }
                this.mCheckedOpacity = orig.mCheckedOpacity;
                this.mOpacity = orig.mOpacity;
                this.mCheckedStateful = orig.mCheckedStateful;
                this.mIsStateful = orig.mIsStateful;
                this.mAutoMirrored = orig.mAutoMirrored;
                this.mThemeAttrs = orig.mThemeAttrs;
                this.mOpacityOverride = orig.mOpacityOverride;
                this.mSrcDensityOverride = orig.mSrcDensityOverride;
                return;
            }
            while (i < 2) {
                this.mChildren[i] = new ChildDrawable(this.mDensity);
                i++;
            }
        }

        public final void setDensity(int targetDensity) {
            if (this.mDensity != targetDensity) {
                this.mDensity = targetDensity;
            }
        }

        public boolean canApplyTheme() {
            if (this.mThemeAttrs != null || super.canApplyTheme()) {
                return true;
            }
            ChildDrawable[] array = this.mChildren;
            for (int i = 0; i < 2; i++) {
                if (array[i].canApplyTheme()) {
                    return true;
                }
            }
            return false;
        }

        public Drawable newDrawable() {
            return new AdaptiveIconDrawable(this, null);
        }

        public Drawable newDrawable(Resources res) {
            return new AdaptiveIconDrawable(this, res);
        }

        public int getChangingConfigurations() {
            return this.mChangingConfigurations | this.mChildrenChangingConfigurations;
        }

        public final int getOpacity() {
            if (this.mCheckedOpacity) {
                return this.mOpacity;
            }
            int i;
            ChildDrawable[] array = this.mChildren;
            int firstIndex = -1;
            for (i = 0; i < 2; i++) {
                if (array[i].mDrawable != null) {
                    firstIndex = i;
                    break;
                }
            }
            if (firstIndex >= 0) {
                i = array[firstIndex].mDrawable.getOpacity();
            } else {
                i = -2;
            }
            for (int i2 = firstIndex + 1; i2 < 2; i2++) {
                Drawable dr = array[i2].mDrawable;
                if (dr != null) {
                    i = Drawable.resolveOpacity(i, dr.getOpacity());
                }
            }
            this.mOpacity = i;
            this.mCheckedOpacity = true;
            return i;
        }

        public final boolean isStateful() {
            if (this.mCheckedStateful) {
                return this.mIsStateful;
            }
            ChildDrawable[] array = this.mChildren;
            boolean isStateful = false;
            for (int i = 0; i < 2; i++) {
                Drawable dr = array[i].mDrawable;
                if (dr != null && dr.isStateful()) {
                    isStateful = true;
                    break;
                }
            }
            this.mIsStateful = isStateful;
            this.mCheckedStateful = true;
            return isStateful;
        }

        public final boolean hasFocusStateSpecified() {
            ChildDrawable[] array = this.mChildren;
            for (int i = 0; i < 2; i++) {
                Drawable dr = array[i].mDrawable;
                if (dr != null && dr.hasFocusStateSpecified()) {
                    return true;
                }
            }
            return false;
        }

        public final boolean canConstantState() {
            ChildDrawable[] array = this.mChildren;
            for (int i = 0; i < 2; i++) {
                Drawable dr = array[i].mDrawable;
                if (dr != null && dr.getConstantState() == null) {
                    return false;
                }
            }
            return true;
        }

        public void invalidateCache() {
            this.mCheckedOpacity = false;
            this.mCheckedStateful = false;
        }
    }

    AdaptiveIconDrawable() {
        this((LayerState) null, null);
    }

    AdaptiveIconDrawable(LayerState state, Resources res) {
        this.mTmpOutRect = new Rect();
        this.mPaint = new Paint(7);
        this.mLayerState = createConstantState(state, res);
        if (sMask == null) {
            sMask = PathParser.createPathFromPathData(Resources.getSystem().getString(R.string.config_icon_mask));
        }
        this.mMask = PathParser.createPathFromPathData(Resources.getSystem().getString(R.string.config_icon_mask));
        this.mMaskMatrix = new Matrix();
        this.mCanvas = new Canvas();
        this.mTransparentRegion = new Region();
    }

    private ChildDrawable createChildDrawable(Drawable drawable) {
        ChildDrawable layer = new ChildDrawable(this.mLayerState.mDensity);
        layer.mDrawable = drawable;
        layer.mDrawable.setCallback(this);
        LayerState layerState = this.mLayerState;
        layerState.mChildrenChangingConfigurations |= layer.mDrawable.getChangingConfigurations();
        return layer;
    }

    LayerState createConstantState(LayerState state, Resources res) {
        return new LayerState(state, this, res);
    }

    public AdaptiveIconDrawable(Drawable backgroundDrawable, Drawable foregroundDrawable) {
        this((LayerState) null, null);
        if (backgroundDrawable != null) {
            addLayer(0, createChildDrawable(backgroundDrawable));
        }
        if (foregroundDrawable != null) {
            addLayer(1, createChildDrawable(foregroundDrawable));
        }
    }

    private void addLayer(int index, ChildDrawable layer) {
        this.mLayerState.mChildren[index] = layer;
        this.mLayerState.invalidateCache();
    }

    public void inflate(Resources r, XmlPullParser parser, AttributeSet attrs, Theme theme) throws XmlPullParserException, IOException {
        super.inflate(r, parser, attrs, theme);
        LayerState state = this.mLayerState;
        if (state != null) {
            int i = 0;
            int deviceDensity = Drawable.resolveDensity(r, 0);
            state.setDensity(deviceDensity);
            state.mSrcDensityOverride = this.mSrcDensityOverride;
            ChildDrawable[] array = state.mChildren;
            while (i < state.mChildren.length) {
                array[i].setDensity(deviceDensity);
                i++;
            }
            inflateLayers(r, parser, attrs, theme);
        }
    }

    public static float getExtraInsetFraction() {
        return 0.25f;
    }

    public static float getExtraInsetPercentage() {
        return 0.25f;
    }

    public Path getIconMask() {
        return this.mMask;
    }

    public Drawable getForeground() {
        return this.mLayerState.mChildren[1].mDrawable;
    }

    public Drawable getBackground() {
        return this.mLayerState.mChildren[0].mDrawable;
    }

    protected void onBoundsChange(Rect bounds) {
        if (!bounds.isEmpty()) {
            updateLayerBounds(bounds);
        }
    }

    private void updateLayerBounds(Rect bounds) {
        if (!bounds.isEmpty()) {
            try {
                suspendChildInvalidation();
                updateLayerBoundsInternal(bounds);
                updateMaskBoundsInternal(bounds);
            } finally {
                resumeChildInvalidation();
            }
        }
    }

    private void updateLayerBoundsInternal(Rect bounds) {
        int cX = bounds.width() / 2;
        int cY = bounds.height() / 2;
        LayerState layerState = this.mLayerState;
        for (int i = 0; i < 2; i++) {
            ChildDrawable r = this.mLayerState.mChildren[i];
            if (r != null) {
                Drawable d = r.mDrawable;
                if (d != null) {
                    int insetWidth = (int) (((float) bounds.width()) / 1068149419);
                    int insetHeight = (int) (((float) bounds.height()) / 1.3333334f);
                    Rect outRect = this.mTmpOutRect;
                    outRect.set(cX - insetWidth, cY - insetHeight, cX + insetWidth, cY + insetHeight);
                    d.setBounds(outRect);
                }
            }
        }
    }

    private void updateMaskBoundsInternal(Rect b) {
        this.mMaskMatrix.setScale(((float) b.width()) / 100.0f, ((float) b.height()) / 100.0f);
        sMask.transform(this.mMaskMatrix, this.mMask);
        if (!(this.mMaskBitmap != null && this.mMaskBitmap.getWidth() == b.width() && this.mMaskBitmap.getHeight() == b.height())) {
            this.mMaskBitmap = Bitmap.createBitmap(b.width(), b.height(), Config.ALPHA_8);
            this.mLayersBitmap = Bitmap.createBitmap(b.width(), b.height(), Config.ARGB_8888);
        }
        this.mCanvas.setBitmap(this.mMaskBitmap);
        this.mPaint.setShader(null);
        this.mCanvas.drawPath(this.mMask, this.mPaint);
        this.mMaskMatrix.postTranslate((float) b.left, (float) b.top);
        this.mMask.reset();
        sMask.transform(this.mMaskMatrix, this.mMask);
        this.mTransparentRegion.setEmpty();
        this.mLayersShader = null;
    }

    public void draw(Canvas canvas) {
        if (this.mLayersBitmap != null) {
            if (this.mLayersShader == null) {
                this.mCanvas.setBitmap(this.mLayersBitmap);
                this.mCanvas.drawColor(-16777216);
                int i = 0;
                while (true) {
                    LayerState layerState = this.mLayerState;
                    if (i >= 2) {
                        break;
                    }
                    if (this.mLayerState.mChildren[i] != null) {
                        Drawable dr = this.mLayerState.mChildren[i].mDrawable;
                        if (dr != null) {
                            dr.draw(this.mCanvas);
                        }
                    }
                    i++;
                }
                this.mLayersShader = new BitmapShader(this.mLayersBitmap, TileMode.CLAMP, TileMode.CLAMP);
                this.mPaint.setShader(this.mLayersShader);
            }
            if (this.mMaskBitmap != null) {
                Rect bounds = getBounds();
                canvas.drawBitmap(this.mMaskBitmap, (float) bounds.left, (float) bounds.top, this.mPaint);
            }
        }
    }

    public void invalidateSelf() {
        this.mLayersShader = null;
        super.invalidateSelf();
    }

    public void getOutline(Outline outline) {
        outline.setConvexPath(this.mMask);
    }

    public Region getSafeZone() {
        this.mMaskMatrix.reset();
        this.mMaskMatrix.setScale(SAFEZONE_SCALE, SAFEZONE_SCALE, (float) getBounds().centerX(), (float) getBounds().centerY());
        Path p = new Path();
        this.mMask.transform(this.mMaskMatrix, p);
        Region safezoneRegion = new Region(getBounds());
        safezoneRegion.setPath(p, safezoneRegion);
        return safezoneRegion;
    }

    public Region getTransparentRegion() {
        if (this.mTransparentRegion.isEmpty()) {
            this.mMask.toggleInverseFillType();
            this.mTransparentRegion.set(getBounds());
            this.mTransparentRegion.setPath(this.mMask, this.mTransparentRegion);
            this.mMask.toggleInverseFillType();
        }
        return this.mTransparentRegion;
    }

    public void applyTheme(Theme t) {
        super.applyTheme(t);
        LayerState state = this.mLayerState;
        if (state != null) {
            int i = 0;
            int density = Drawable.resolveDensity(t.getResources(), 0);
            state.setDensity(density);
            ChildDrawable[] array = state.mChildren;
            while (i < 2) {
                ChildDrawable layer = array[i];
                layer.setDensity(density);
                if (layer.mThemeAttrs != null) {
                    TypedArray a = t.resolveAttributes(layer.mThemeAttrs, R.styleable.AdaptiveIconDrawableLayer);
                    updateLayerFromTypedArray(layer, a);
                    a.recycle();
                }
                Drawable d = layer.mDrawable;
                if (d != null && d.canApplyTheme()) {
                    d.applyTheme(t);
                    state.mChildrenChangingConfigurations |= d.getChangingConfigurations();
                }
                i++;
            }
        }
    }

    private void inflateLayers(Resources r, XmlPullParser parser, AttributeSet attrs, Theme theme) throws XmlPullParserException, IOException {
        XmlPullParser xmlPullParser;
        Resources resources = r;
        AttributeSet attributeSet = attrs;
        Theme theme2 = theme;
        LayerState state = this.mLayerState;
        int innerDepth = parser.getDepth() + 1;
        while (true) {
            int next = parser.next();
            int type = next;
            if (next == 1) {
                break;
            }
            next = parser.getDepth();
            int depth = next;
            if (next < innerDepth && type == 3) {
                break;
            }
            if (type == 2 && depth <= innerDepth) {
                int childIndex;
                String tagName = parser.getName();
                if (tagName.equals("background")) {
                    childIndex = 0;
                } else if (tagName.equals("foreground")) {
                    childIndex = 1;
                }
                ChildDrawable layer = new ChildDrawable(state.mDensity);
                TypedArray a = Drawable.obtainAttributes(resources, theme2, attributeSet, R.styleable.AdaptiveIconDrawableLayer);
                updateLayerFromTypedArray(layer, a);
                a.recycle();
                if (layer.mDrawable == null && layer.mThemeAttrs == null) {
                    while (true) {
                        int next2 = parser.next();
                        type = next2;
                        if (next2 != 4) {
                            break;
                        }
                    }
                    if (type == 2) {
                        layer.mDrawable = Drawable.createFromXmlInnerForDensity(resources, parser, attributeSet, this.mLayerState.mSrcDensityOverride, theme2);
                        layer.mDrawable.setCallback(this);
                        state.mChildrenChangingConfigurations |= layer.mDrawable.getChangingConfigurations();
                    } else {
                        xmlPullParser = parser;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(parser.getPositionDescription());
                        stringBuilder.append(": <foreground> or <background> tag requires a 'drawable'attribute or child tag defining a drawable");
                        throw new XmlPullParserException(stringBuilder.toString());
                    }
                }
                xmlPullParser = parser;
                addLayer(childIndex, layer);
            }
            xmlPullParser = parser;
        }
        xmlPullParser = parser;
    }

    private void updateLayerFromTypedArray(ChildDrawable layer, TypedArray a) {
        LayerState state = this.mLayerState;
        state.mChildrenChangingConfigurations |= a.getChangingConfigurations();
        layer.mThemeAttrs = a.extractThemeAttrs();
        Drawable dr = a.getDrawableForDensity(0, state.mSrcDensityOverride);
        if (dr != null) {
            if (layer.mDrawable != null) {
                layer.mDrawable.setCallback(null);
            }
            layer.mDrawable = dr;
            layer.mDrawable.setCallback(this);
            state.mChildrenChangingConfigurations |= layer.mDrawable.getChangingConfigurations();
        }
    }

    public boolean canApplyTheme() {
        return (this.mLayerState != null && this.mLayerState.canApplyTheme()) || super.canApplyTheme();
    }

    public boolean isProjected() {
        if (super.isProjected()) {
            return true;
        }
        ChildDrawable[] layers = this.mLayerState.mChildren;
        int i = 0;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i >= 2) {
                return false;
            }
            if (layers[i].mDrawable.isProjected()) {
                return true;
            }
            i++;
        }
    }

    private void suspendChildInvalidation() {
        this.mSuspendChildInvalidation = true;
    }

    private void resumeChildInvalidation() {
        this.mSuspendChildInvalidation = false;
        if (this.mChildRequestedInvalidation) {
            this.mChildRequestedInvalidation = false;
            invalidateSelf();
        }
    }

    public void invalidateDrawable(Drawable who) {
        if (this.mSuspendChildInvalidation) {
            this.mChildRequestedInvalidation = true;
        } else {
            invalidateSelf();
        }
    }

    public void scheduleDrawable(Drawable who, Runnable what, long when) {
        scheduleSelf(what, when);
    }

    public void unscheduleDrawable(Drawable who, Runnable what) {
        unscheduleSelf(what);
    }

    public int getChangingConfigurations() {
        return super.getChangingConfigurations() | this.mLayerState.getChangingConfigurations();
    }

    public void setHotspot(float x, float y) {
        ChildDrawable[] array = this.mLayerState.mChildren;
        int i = 0;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i < 2) {
                Drawable dr = array[i].mDrawable;
                if (dr != null) {
                    dr.setHotspot(x, y);
                }
                i++;
            } else {
                return;
            }
        }
    }

    public void setHotspotBounds(int left, int top, int right, int bottom) {
        ChildDrawable[] array = this.mLayerState.mChildren;
        int i = 0;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i >= 2) {
                break;
            }
            Drawable dr = array[i].mDrawable;
            if (dr != null) {
                dr.setHotspotBounds(left, top, right, bottom);
            }
            i++;
        }
        if (this.mHotspotBounds == null) {
            this.mHotspotBounds = new Rect(left, top, right, bottom);
        } else {
            this.mHotspotBounds.set(left, top, right, bottom);
        }
    }

    public void getHotspotBounds(Rect outRect) {
        if (this.mHotspotBounds != null) {
            outRect.set(this.mHotspotBounds);
        } else {
            super.getHotspotBounds(outRect);
        }
    }

    public boolean setVisible(boolean visible, boolean restart) {
        boolean changed = super.setVisible(visible, restart);
        ChildDrawable[] array = this.mLayerState.mChildren;
        int i = 0;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i >= 2) {
                return changed;
            }
            Drawable dr = array[i].mDrawable;
            if (dr != null) {
                dr.setVisible(visible, restart);
            }
            i++;
        }
    }

    public void setDither(boolean dither) {
        ChildDrawable[] array = this.mLayerState.mChildren;
        int i = 0;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i < 2) {
                Drawable dr = array[i].mDrawable;
                if (dr != null) {
                    dr.setDither(dither);
                }
                i++;
            } else {
                return;
            }
        }
    }

    public void setAlpha(int alpha) {
        this.mPaint.setAlpha(alpha);
    }

    public int getAlpha() {
        return -3;
    }

    public void setColorFilter(ColorFilter colorFilter) {
        ChildDrawable[] array = this.mLayerState.mChildren;
        int i = 0;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i < 2) {
                Drawable dr = array[i].mDrawable;
                if (dr != null) {
                    dr.setColorFilter(colorFilter);
                }
                i++;
            } else {
                return;
            }
        }
    }

    public void setTintList(ColorStateList tint) {
        ChildDrawable[] array = this.mLayerState.mChildren;
        LayerState layerState = this.mLayerState;
        for (int i = 0; i < 2; i++) {
            Drawable dr = array[i].mDrawable;
            if (dr != null) {
                dr.setTintList(tint);
            }
        }
    }

    public void setTintMode(Mode tintMode) {
        ChildDrawable[] array = this.mLayerState.mChildren;
        LayerState layerState = this.mLayerState;
        for (int i = 0; i < 2; i++) {
            Drawable dr = array[i].mDrawable;
            if (dr != null) {
                dr.setTintMode(tintMode);
            }
        }
    }

    public void setOpacity(int opacity) {
        this.mLayerState.mOpacityOverride = opacity;
    }

    public int getOpacity() {
        if (this.mLayerState.mOpacityOverride != 0) {
            return this.mLayerState.mOpacityOverride;
        }
        return this.mLayerState.getOpacity();
    }

    public void setAutoMirrored(boolean mirrored) {
        this.mLayerState.mAutoMirrored = mirrored;
        ChildDrawable[] array = this.mLayerState.mChildren;
        int i = 0;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i < 2) {
                Drawable dr = array[i].mDrawable;
                if (dr != null) {
                    dr.setAutoMirrored(mirrored);
                }
                i++;
            } else {
                return;
            }
        }
    }

    public boolean isAutoMirrored() {
        return this.mLayerState.mAutoMirrored;
    }

    public void jumpToCurrentState() {
        ChildDrawable[] array = this.mLayerState.mChildren;
        int i = 0;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i < 2) {
                Drawable dr = array[i].mDrawable;
                if (dr != null) {
                    dr.jumpToCurrentState();
                }
                i++;
            } else {
                return;
            }
        }
    }

    public boolean isStateful() {
        return this.mLayerState.isStateful();
    }

    public boolean hasFocusStateSpecified() {
        return this.mLayerState.hasFocusStateSpecified();
    }

    protected boolean onStateChange(int[] state) {
        boolean changed = false;
        ChildDrawable[] array = this.mLayerState.mChildren;
        int i = 0;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i >= 2) {
                break;
            }
            Drawable dr = array[i].mDrawable;
            if (dr != null && dr.isStateful() && dr.setState(state)) {
                changed = true;
            }
            i++;
        }
        if (changed) {
            updateLayerBounds(getBounds());
        }
        return changed;
    }

    protected boolean onLevelChange(int level) {
        boolean changed = false;
        ChildDrawable[] array = this.mLayerState.mChildren;
        int i = 0;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i >= 2) {
                break;
            }
            Drawable dr = array[i].mDrawable;
            if (dr != null && dr.setLevel(level)) {
                changed = true;
            }
            i++;
        }
        if (changed) {
            updateLayerBounds(getBounds());
        }
        return changed;
    }

    public int getIntrinsicWidth() {
        return (int) (((float) getMaxIntrinsicWidth()) * DEFAULT_VIEW_PORT_SCALE);
    }

    private int getMaxIntrinsicWidth() {
        int width = -1;
        int i = 0;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i >= 2) {
                return width;
            }
            ChildDrawable r = this.mLayerState.mChildren[i];
            if (r.mDrawable != null) {
                int w = r.mDrawable.getIntrinsicWidth();
                if (w > width) {
                    width = w;
                }
            }
            i++;
        }
    }

    public int getIntrinsicHeight() {
        return (int) (((float) getMaxIntrinsicHeight()) * DEFAULT_VIEW_PORT_SCALE);
    }

    private int getMaxIntrinsicHeight() {
        int height = -1;
        int i = 0;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i >= 2) {
                return height;
            }
            ChildDrawable r = this.mLayerState.mChildren[i];
            if (r.mDrawable != null) {
                int h = r.mDrawable.getIntrinsicHeight();
                if (h > height) {
                    height = h;
                }
            }
            i++;
        }
    }

    public ConstantState getConstantState() {
        if (!this.mLayerState.canConstantState()) {
            return null;
        }
        this.mLayerState.mChangingConfigurations = getChangingConfigurations();
        return this.mLayerState;
    }

    public Drawable mutate() {
        if (!this.mMutated && super.mutate() == this) {
            this.mLayerState = createConstantState(this.mLayerState, null);
            int i = 0;
            while (true) {
                LayerState layerState = this.mLayerState;
                if (i >= 2) {
                    break;
                }
                Drawable dr = this.mLayerState.mChildren[i].mDrawable;
                if (dr != null) {
                    dr.mutate();
                }
                i++;
            }
            this.mMutated = true;
        }
        return this;
    }

    public void clearMutated() {
        super.clearMutated();
        ChildDrawable[] array = this.mLayerState.mChildren;
        int i = 0;
        while (true) {
            LayerState layerState = this.mLayerState;
            if (i < 2) {
                Drawable dr = array[i].mDrawable;
                if (dr != null) {
                    dr.clearMutated();
                }
                i++;
            } else {
                this.mMutated = false;
                return;
            }
        }
    }
}
