package com.xfy.scrolllayout;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * 子类只需实现{@link TwoChildrenAdapter#bindViewData(View, View)}
 *
 * @param <V> 类中必须有public并且有且只有{@link Context}参数的构造方法
 */
public abstract class BaseTwoChildrenAdapter<V extends View> implements TwoChildrenAdapter<V> {
    /**
     * 通过第一个子view克隆一个新的View
     * @param first child in {@link ScrollLayout}
     * @return  new child like first child
     */
    @Override
    public @NonNull
    V cloneFirstView(ScrollLayout parent, V first) {
        Class<V> clz = (Class<V>) first.getClass();
        try {
            Constructor<V> c = clz.getConstructor(Context.class);
            V clone = c.newInstance(parent.getContext());
            clone.setLayoutParams(first.getLayoutParams());
            bindViewData(first, clone);
            return clone;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 通过第二个子view克隆一个新的View
     * @param second child in {@link ScrollLayout}
     * @return  new child like second child
     */
    @Override
    public @NonNull
    V cloneSecondView(ScrollLayout parent, V second) {
        Class<V> clz = (Class<V>) second.getClass();
        try {
            Constructor<V> c = clz.getConstructor(Context.class);
            V clone = c.newInstance(parent.getContext());
            clone.setLayoutParams(second.getLayoutParams());
            bindViewData(second, clone);
            return clone;
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }
}
