package com.xfy.scrolllayout;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.View;

/**
 * 默认3d变换
 * 像在翻一个箱子
 */
public class FlipLikeRotateBox implements IDrawChildren {
    private float eachDegree = 90;
    private int mWidth;
    private int mHeight;
    private Camera mCamera;
    private Matrix matrix;

    public FlipLikeRotateBox(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        if (context != null && attrs != null) {
            final Resources.Theme theme = context.getTheme();
            TypedArray a = theme.obtainStyledAttributes(attrs,
                                                        R.styleable.FlipLikeRotateBoxAttr, defStyleAttr, defStyleRes);
            initStyle(a);
        }
        checkDegree();
        init();
    }

    private void initStyle(TypedArray a) {
        if (a != null) {
            eachDegree = a.getFloat(R.styleable.FlipLikeRotateBoxAttr_flrb_each_degree, eachDegree);
            a.recycle();
        }
    }

    public FlipLikeRotateBox(float eachDegree) {
        this.eachDegree = eachDegree;
        checkDegree();
        init();
    }

    private void init() {
        mCamera = new Camera();
        matrix = new Matrix();
    }

    private void checkDegree() {
        if (eachDegree <= 0 || eachDegree >= 180)
            throw new IllegalArgumentException("degree must be greater than 0 and less than 180.");
    }

    @Override
    public void drawChild(ScrollLayout parent, View child, Canvas canvas,
                          @ScrollLayout.ScrollOrientation int scrollOrientation,
                          int index, long drawingTime) {
        if (mWidth == 0)
            mWidth = parent.getChildWdith();
        if (mHeight == 0)
            mHeight = parent.getChildHeight();
        if (child == null || child.getVisibility() == View.GONE) {
            return;
        }

        float centerX = 0;
        float centerY = 0;
        float degree = 0;
        switch (scrollOrientation) {
            case ScrollLayout.VERTICAL:
                final int curScreenY = mHeight * index;
                final float scrollY = parent.getScrollY();
                if (scrollY + mHeight < curScreenY) {
                    return;
                }
                if (curScreenY < scrollY - mHeight) {
                    return;
                }
                centerX = mWidth / 2;
                centerY = (scrollY > curScreenY) ? curScreenY + mHeight : curScreenY;
                degree = eachDegree * (scrollY - curScreenY) / mHeight;
                if (degree > 90 || degree < -90) {
                    return;
                }
                canvas.save();
                mCamera.save();
                matrix.reset();
                mCamera.rotateX(degree);
                mCamera.getMatrix(matrix);
                mCamera.restore();

                matrix.preTranslate(-centerX, -centerY);
                matrix.postTranslate(centerX, centerY);
                canvas.concat(matrix);

                parent.drawChild(canvas, child, drawingTime);
                canvas.restore();
                break;
            case ScrollLayout.HORIZONTAL:
                final int curScreenX = mWidth * index;
                final float scrollX = parent.getScrollX();
                if (scrollX + mWidth < curScreenX)
                    return;
                if (curScreenX < scrollX - mWidth)
                    return;
                centerX = (scrollX > curScreenX) ? curScreenX + mWidth : curScreenX;
                centerY = mHeight / 2;
                centerX += parent.getPaddingLeft();
                centerY += parent.getPaddingTop();
                degree = eachDegree * (scrollX - curScreenX) / mWidth;

                if (degree > 90 || degree < -90) {
                    return;
                }

                canvas.save();
                mCamera.save();
                matrix.reset();
                mCamera.rotateY(-degree);
                mCamera.getMatrix(matrix);
                mCamera.restore();

                matrix.preTranslate(-centerX, -centerY);
                matrix.postTranslate(centerX, centerY);
                canvas.concat(matrix);

                parent.drawChild(canvas, child, drawingTime);
                canvas.restore();
                break;
        }
    }
}
