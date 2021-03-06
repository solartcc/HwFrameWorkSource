package com.android.server.wm;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.Surface.OutOfResourcesException;
import android.view.SurfaceControl;

class Watermark {
    private final int mDeltaX;
    private final int mDeltaY;
    private final Display mDisplay;
    private boolean mDrawNeeded;
    private int mLastDH;
    private int mLastDW;
    private final Surface mSurface = new Surface();
    private final SurfaceControl mSurfaceControl;
    private final String mText;
    private final int mTextHeight;
    private final Paint mTextPaint;
    private final int mTextWidth;
    private final String[] mTokens;

    Watermark(DisplayContent dc, DisplayMetrics dm, String[] tokens) {
        int c2;
        int c1;
        DisplayMetrics displayMetrics = dm;
        String[] strArr = tokens;
        this.mDisplay = dc.getDisplay();
        this.mTokens = strArr;
        StringBuilder builder = new StringBuilder(32);
        int len = this.mTokens[0].length() & -2;
        for (int i = 0; i < len; i += 2) {
            int c12 = this.mTokens[0].charAt(i);
            c2 = this.mTokens[0].charAt(i + 1);
            if (c12 >= 97 && c12 <= 102) {
                c1 = (c12 - 97) + 10;
            } else if (c12 < 65 || c12 > 70) {
                c1 = c12 - 48;
            } else {
                c1 = (c12 - 65) + 10;
            }
            if (c2 >= 97 && c2 <= 102) {
                c12 = (c2 - 97) + 10;
            } else if (c2 < 65 || c2 > 70) {
                c12 = c2 - 48;
            } else {
                c12 = (c2 - 65) + 10;
            }
            builder.append((char) (255 - ((c1 * 16) + c12)));
        }
        this.mText = builder.toString();
        c2 = WindowManagerService.getPropertyInt(strArr, 1, 1, 20, displayMetrics);
        this.mTextPaint = new Paint(1);
        this.mTextPaint.setTextSize((float) c2);
        this.mTextPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, 1));
        FontMetricsInt fm = this.mTextPaint.getFontMetricsInt();
        this.mTextWidth = (int) this.mTextPaint.measureText(this.mText);
        this.mTextHeight = fm.descent - fm.ascent;
        this.mDeltaX = WindowManagerService.getPropertyInt(strArr, 2, 0, this.mTextWidth * 2, displayMetrics);
        this.mDeltaY = WindowManagerService.getPropertyInt(strArr, 3, 0, this.mTextHeight * 3, displayMetrics);
        int shadowColor = WindowManagerService.getPropertyInt(strArr, 4, 0, -1342177280, displayMetrics);
        int color = WindowManagerService.getPropertyInt(strArr, 5, 0, 1627389951, displayMetrics);
        int shadowRadius = WindowManagerService.getPropertyInt(strArr, 6, 0, 7, displayMetrics);
        c1 = WindowManagerService.getPropertyInt(strArr, 8, 0, 0, displayMetrics);
        int shadowDy = WindowManagerService.getPropertyInt(strArr, 9, 0, 0, displayMetrics);
        this.mTextPaint.setColor(color);
        this.mTextPaint.setShadowLayer((float) shadowRadius, (float) c1, (float) shadowDy, shadowColor);
        SurfaceControl ctrl = null;
        try {
            ctrl = dc.makeOverlay().setName("WatermarkSurface").setSize(1, 1).setFormat(-3).build();
            ctrl.setLayerStack(this.mDisplay.getLayerStack());
            ctrl.setLayer(1000000);
            ctrl.setPosition(0.0f, 0.0f);
            ctrl.show();
            this.mSurface.copyFrom(ctrl);
        } catch (OutOfResourcesException e) {
        }
        this.mSurfaceControl = ctrl;
    }

    void positionSurface(int dw, int dh) {
        if (this.mLastDW != dw || this.mLastDH != dh) {
            this.mLastDW = dw;
            this.mLastDH = dh;
            this.mSurfaceControl.setSize(dw, dh);
            this.mDrawNeeded = true;
        }
    }

    void drawIfNeeded() {
        if (this.mDrawNeeded) {
            int dw = this.mLastDW;
            int dh = this.mLastDH;
            this.mDrawNeeded = false;
            Canvas c = null;
            try {
                c = this.mSurface.lockCanvas(new Rect(0, 0, dw, dh));
            } catch (OutOfResourcesException | IllegalArgumentException e) {
            }
            if (c != null) {
                c.drawColor(0, Mode.CLEAR);
                int deltaX = this.mDeltaX;
                int deltaY = this.mDeltaY;
                int rem = (this.mTextWidth + dw) - (((this.mTextWidth + dw) / deltaX) * deltaX);
                int qdelta = deltaX / 4;
                if (rem < qdelta || rem > deltaX - qdelta) {
                    deltaX += deltaX / 3;
                }
                int y = -this.mTextHeight;
                int x = -this.mTextWidth;
                while (y < this.mTextHeight + dh) {
                    c.drawText(this.mText, (float) x, (float) y, this.mTextPaint);
                    x += deltaX;
                    if (x >= dw) {
                        x -= this.mTextWidth + dw;
                        y += deltaY;
                    }
                }
                this.mSurface.unlockCanvasAndPost(c);
            }
        }
    }
}
