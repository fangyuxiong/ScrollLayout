package com.xfy.scrolllayout;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;

import static com.xfy.scrolllayout.ScrollLayout.HORIZONTAL;
import static com.xfy.scrolllayout.ScrollLayout.VERTICAL;

/**
 * Created by XiongFangyu on 17/1/22.
 * 简单的透明度变换
 */
public class FadeDrawChildren implements IDrawChildren {

    private float minAlpha = 0;
    private int mWidth;
    private int mHeight;

    public FadeDrawChildren() {
        minAlpha = 0;
    }

    public FadeDrawChildren(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (context != null && attrs != null) {
            final Resources.Theme theme = context.getTheme();
            TypedArray a = theme.obtainStyledAttributes(attrs,
                    R.styleable.FadeDrawChildren, defStyleAttr, defStyleRes);
            initStyle(a);
        }
    }

    private void initStyle(TypedArray a) {
        if (a != null) {
            minAlpha = a.getFloat(R.styleable.FadeDrawChildren_fdc_min_alpha, minAlpha);
            a.recycle();
        }
    }

    @Override
    public void drawChild(ScrollLayout parent, View child, Canvas canvas,
                          @ScrollLayout.ScrollOrientation int scrollOrientation,
                          int index, long drawingTime) {
        if (mWidth == 0)
            mWidth = parent.getWidth();
        if (mHeight == 0)
            mHeight = parent.getHeight();
        if (child == null || child.getVisibility() == View.GONE)
            return;

        float percent = 0;
        switch (scrollOrientation) {
            case VERTICAL:
                final int curScreenY = mHeight * index;
                final float scrollY = parent.getScrollY();
                if (scrollY + mHeight < curScreenY) {
                    return;
                }
                if (curScreenY < scrollY - mHeight) {
                    return;
                }
                percent = Math.abs(scrollY - curScreenY) / mHeight;
                break;
            case HORIZONTAL:
                final int curScreenX = mWidth * index;
                final float scrollX = parent.getScrollX();
                if (scrollX + mWidth < curScreenX) {
                    return;
                }
                if (curScreenX < scrollX - mWidth) {
                    return;
                }
                percent = 1 - Math.abs(scrollX - curScreenX) / mWidth;
                break;
        }
        if (percent < 0)
            percent = 0;
        if (percent > 1)
            percent = 1;
        setAlpha(child, percent);
        parent.drawChild(canvas, child, drawingTime);
    }

    private void setAlpha(View child, float percent) {
        float a = (1f - minAlpha) * percent + minAlpha;
        child.setAlpha(a);
    }
}
