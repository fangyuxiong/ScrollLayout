package com.xfy.scrolllayout;

import android.view.View;

/**
 * Created by XiongFangyu on 17/1/22.
 *
 * 当滚动完全停下时，会触发{@link #changeTo(View, int)}方法
 */
public interface OnChangeListener {
    /**
     * 当动画停止时，将调用此方法
     *
     * @param child 显示的子view
     * @param index 显示的view所在的位置
     */
    void changeTo(View child, int index);
}
