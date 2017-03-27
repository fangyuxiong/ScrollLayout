package com.xfy.scrolllayout;

import android.graphics.Canvas;
import android.view.View;

/**
 * 支持3d绘图时，绘制子控件的接口
 * {@link ScrollLayout#do3DAnim} must true
 */
public interface IDrawChildren {
    /**
     * 绘制子控件
     * @param parent    {@link ScrollLayout}
     * @param child     需要绘制的View
     * @param canvas    未经处理
     * @param scrollOrientation {@link ScrollLayout#scrollOrientation}
     * @param index     child在{@link ScrollLayout#children}中的位置
     * @param drawingTime   use in {@link android.view.ViewGroup#drawChild(Canvas, View, long)}
     */
    void drawChild(ScrollLayout parent, View child, Canvas canvas, @ScrollLayout.ScrollOrientation int scrollOrientation, int index, long drawingTime);
}
