package android.media.update;

import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

public interface ViewGroupProvider {
    boolean checkLayoutParams_impl(LayoutParams layoutParams);

    boolean dispatchTouchEvent_impl(MotionEvent motionEvent);

    LayoutParams generateDefaultLayoutParams_impl();

    LayoutParams generateLayoutParams_impl(AttributeSet attributeSet);

    LayoutParams generateLayoutParams_impl(LayoutParams layoutParams);

    CharSequence getAccessibilityClassName_impl();

    int getSuggestedMinimumHeight_impl();

    int getSuggestedMinimumWidth_impl();

    void measureChildWithMargins_impl(View view, int i, int i2, int i3, int i4);

    void onAttachedToWindow_impl();

    void onDetachedFromWindow_impl();

    void onFinishInflate_impl();

    void onLayout_impl(boolean z, int i, int i2, int i3, int i4);

    void onMeasure_impl(int i, int i2);

    boolean onTouchEvent_impl(MotionEvent motionEvent);

    boolean onTrackballEvent_impl(MotionEvent motionEvent);

    void onVisibilityAggregated_impl(boolean z);

    void setEnabled_impl(boolean z);

    void setMeasuredDimension_impl(int i, int i2);

    boolean shouldDelayChildPressedState_impl();
}
