# ScrollLayout
## 效果图
![horizontal test](pic/horizontal_test.gif)
![vertical test](pic/vertical_test.gif)
![vertical fade](pic/vertical_fade.gif)

## 介绍
一个可使用自定义动画的滚动切换布局。

## 使用方法

`build.gradle`中添加:
```
dependencies {
    compile 'com.xfy.scrolllayout:library:1.1'
}
```
layout中添加:
```
<com.xfy.scrolllayout.ScrollLayout
    style="@style/normal_scroll_style"
    android:layout_width="100dp"
    android:layout_height="100dp">
    <!--子控件-->
</com.xfy.scrolllayout.ScrollLayout>
```
`normal_scroll_style`:
```
<style name="normal_scroll_style">
    <!--滑动阻力(0, +)，越大阻力越大-->
    <item name="slt_resistance">1</item>
    <!--设置回位时间[1, +)，越大时间越长-->
    <item name="slt_to_normal_offset">3</item>
    <!--设置自由滑动时间[1, +)，越大时间越长-->
    <item name="slt_fling_offset">1</item>
    <!--切换时是否有动画-->
    <item name="slt_do_3d_anim">true</item>
    <!--是否可用手指滚动-->
    <item name="slt_can_scroll_by_touch">true</item>
    <!--滚动方向[vertical|horizontal]-->
    <item name="slt_scroll_orientation">vertical</item>
    <!--'slt_do_3d_anim'属性设为'true'时可用
        动画实现类名，默认为com.xfy.scrolllayout.FlipLikeRotateBox-->
    <item name="slt_draw_children_interface">FadeDrawChildren</item>
    <!--初始后只有两个子控件时须设置-->
    <item name="slt_two_children_adapter">adapter class name</item>

    <!--使用com.xfy.scrolllayout.FlipLikeRotateBox动画时可用
        设置立体角度(0°, 180°)-->
    <item name="flrb_each_degree">90</item>

    <!--使用com.xfy.scrolllayout.FadeDrawChildren动画时可用
        设置最小透明度[0, 1]-->
    <item name="fdc_min_alpha">0.2</item>
</style>
```
代码中:
```
scrollLayout.setOnChangeListener(new OnChangeListener() {
    @Override
    public void changeTo(View child, int index) {
        //切换到child
    }
});

scrollLayout.toNext(true);//平滑切换到下一个子控件
scrollLayout.toPre(true);//平滑切换到上一个子控件
scrollLayout.toNext(false);//直接切换到下一个子控件
scrollLayout.gotoChild(0, true);//平滑切换到第1个子控件
scrollLayout.gotoChild(1, false);//直接切换到第2个子控件
```

## 注意
ScrollLayout不支持1个子控件，若只有两个子控件，须添加`TwoChildrenAdapter`(或`BaseTwoChildrenAdapter`)，实现其中`cloneFirstView`（通过第一个子控件克隆一个新的控件），`cloneSecondView`（通过第二个子控件克隆一个新的控件），`bindViewData`（刷新子控件）。
若动态更改其中的子控件，须调用`notifyViewChanged(changedView)`同步子控件。可参考[EditTextTwoChildrenAdapter](sample/src/main/java/com/xfy/sample/EditTextTwoChildrenAdapter.java)

若需通过代码添加子控件，调用`addView`添加完成所有子控件后，需调用`notifyAddChildViewFinish()`刷新布局。

ScrollLayout滚动动画可自定义，只需实现`IDrawChildren`接口，并通过`slt_draw_children_interface=类名`来设置。

自定义动画类名有两种设置方式
* 若自定义动画类和加载ScrollLayout的`Context`为同一包名，可直接使用`.类名`来指定
* 直接使用类名全名，比如`com.xfy.sample.TestDrawChildren`

## 最后
欢迎提出意见及建议。

* email: s18810577589@sina.com