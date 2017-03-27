package com.xfy.scrolllayout;

import android.support.annotation.NonNull;
import android.view.View;

/**
 * 如果只有两个子view的话，需要设置adapter
 *
 * 如果更改了其中某个view的状态，必须调用{@link ScrollLayout#notifyViewChanged(View)}告知
 *
 * @param <V> 泛型
 */
public interface TwoChildrenAdapter<V extends View> {
    /**
     * 通过第一个子view克隆一个新的View
     * @param first child in {@link ScrollLayout}
     * @return  new child like first child
     */
    @NonNull V cloneFirstView(ScrollLayout parent, V first);

    /**
     * 通过第二个子view克隆一个新的View
     * @param second child in {@link ScrollLayout}
     * @return  new child like second child
     */
    @NonNull V cloneSecondView(ScrollLayout parent, V second);

    /**
     * 在新的view中填充数据
     * 调用{@link ScrollLayout#notifyViewChanged(View)}会执行到这里
     * @param res   外部修改的View
     * @param newView   需要同步修改的View
     */
    void bindViewData(V res, V newView);
}
