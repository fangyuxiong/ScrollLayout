package com.xfy.scrolllayout;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.os.Build;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Scroller;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Created by XiongFangyu on 17/1/10.
 *
 * 可让子空间在里面无线循环滚动的Layout
 * 滚动时可设置3D动画
 *
 * 注意：
 *      子控件比如大于1个
 *      子控件若只有两个，必须添加属性'flt_two_children_adapter="你的{@link TwoChildrenAdapter}类名(规则参考{@link #fillPackageName(String)})"'，
 *      若修改其中一个子View，必须调用{@link #notifyViewChanged(View)}
 *      若通过{@link #addView(View)}添加子控件，添加完所有子控件后，必须调用{@link #notifyAddChildViewFinish()}刷新布局
 *      可添加属性'slt_draw_children_interface="{@link IDrawChildren}类名"'来实现切换动画
 * <p>
 * <b>XML attributes</b>
 * <p>
 *     slt_style                    设置属性
 *     slt_resistance               滑动阻力[1, +)  {@link #setResistance(float)}
 *     slt_to_normal_offset         回位速度[1, +)  {@link #setToNormalOffset(int)}
 *     slt_fling_offset             自由滑动速度[1, +)    {@link #setFlingOffset(int)}
 *     slt_do_3d_anim               滚动时是否使用3D动画   {@link #setDo3DAnim(boolean)}
 *     slt_two_children_adapter     2个子View时使用的{@link TwoChildrenAdapter}
 *     slt_can_scroll_by_touch      是否能用手指滚动，默认true {@link #setCanScrollByTouch(boolean)}
 *     slt_scroll_orientation       滚动方向 {@link #VERTICAL} {@link #HORIZONTAL}
 *     slt_draw_children_interface  滚动时3D动画实现，默认为{@link FlipLikeRotateBox} 设置规则参考{@link #fillPackageName(String)}
 */
public class ScrollLayout extends ViewGroup {
    private static final String TAG = "ScrollLayout---xfy---";
    /**
     * 竖直移动方向
     */
    public static final int VERTICAL = 0;
    /**
     * 水平移动方向
     */
    public static final int HORIZONTAL = 1;

    private static final int MAX_SPEED = 2000;
    private static final int MIN_SPEED = 800;
    private static final int MIN_NEED_ADD = 5;

    private static final int STATE_NOMARL = 0;
    private static final int STATE_PRE = -1;
    private static final int STATE_NEXT = 1;

    private int mWidth;
    private int mHeight;
    private int childWdith;
    private int childHeight;

    private View[] children;

    private VelocityTracker mVelocityTracker;
    private Scroller mScroller;

    private int scrollOrientation = VERTICAL;

    @IntDef({VERTICAL, HORIZONTAL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScrollOrientation{}

    private int state = STATE_NOMARL;
    private boolean isSliding;
    private float mDownX;
    private float mDownY;
    private float mTouchSlop;
    private int currentIndex = 0;
    private boolean firstMeasure = true;
    private boolean scrolling = false;
    private boolean onlyTwoChildren = false;
    private int alreadyAddCount = 0;

    private float resistance = 1;
    private int toNormalOffset = 4;
    private int flingOffset = 1;
    private boolean do3DAnim = false;
    private int startIndex = 1;

    private boolean canScrollByTouch = true;

    private OnChangeListener onChangeListener;

    private String twoChildrenAdapterClass;
    private TwoChildrenAdapter twoChildrenAdapter;

    private String iDrawChildrenClass;
    private IDrawChildren iDrawChildren;

    public ScrollLayout(Context context) {
        this(context, null);
    }

    public ScrollLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScrollLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ScrollLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        mScroller = new Scroller(context, new DecelerateInterpolator());
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

        if (context != null && attrs != null) {
            final Resources.Theme theme = context.getTheme();
            TypedArray a = theme.obtainStyledAttributes(attrs,
                    R.styleable.ScrollLayout, defStyleAttr, defStyleRes);
            TypedArray appearance = null;
            int ap = a.getResourceId(
                    R.styleable.ScrollLayout_slt_style, -1);
            if (ap != -1) {
                appearance = theme.obtainStyledAttributes(
                        ap, R.styleable.ScrollLayout);
            }
            initStyle(appearance);
            initStyle(a);
        }
        initTwoChildrenAdapter();
        initIDrawChildren(context, attrs, defStyleAttr, defStyleRes);
        if (iDrawChildren == null) {
            iDrawChildren = new FlipLikeRotateBox(context, attrs, defStyleAttr, defStyleRes);
        }
    }

    private void initTwoChildrenAdapter() {
        twoChildrenAdapterClass = fillPackageName(twoChildrenAdapterClass);
        if (!TextUtils.isEmpty(twoChildrenAdapterClass)) {
            try {
                Class<? extends TwoChildrenAdapter> clz = (Class<? extends TwoChildrenAdapter>) Class.forName(twoChildrenAdapterClass);
                TwoChildrenAdapter adapter = clz.newInstance();
                this.twoChildrenAdapter = adapter;
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                throw new IllegalArgumentException("cannot find " + twoChildrenAdapterClass + ".", e);
            } catch (InstantiationException e) {
                e.printStackTrace();
                throw new IllegalArgumentException("cannot create " + twoChildrenAdapterClass + ".", e);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                throw new IllegalArgumentException("cannot create " + twoChildrenAdapterClass + ".", e);
            }
        }
    }

    /**
     * 给类名前添加包名
     * 若类名以 <code>.</code> 开头，默认在前面添加{@link #getContext()}的包名
     * 若类名不包含 <code>.</code>，默认包名为{@link ScrollLayout}包名(com.xfy.scrolllayout)
     * 若是全名，返回全名
     * @param clz
     * @return
     */
    private String fillPackageName(String clz) {
        if (TextUtils.isEmpty(clz))
            return clz;
        if (clz.startsWith(".")) {
            String packageName = getContext().getClass().getPackage().getName();
            return packageName + clz;
        }
        if (clz.contains(".")) {
            return clz;
        }
        return getClass().getPackage().getName() + "." + clz;
    }

    private void initIDrawChildren(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        iDrawChildrenClass = fillPackageName(iDrawChildrenClass);
        if (!TextUtils.isEmpty(iDrawChildrenClass)) {
            try {
                Class<? extends IDrawChildren> clz = (Class<? extends IDrawChildren>) Class.forName(iDrawChildrenClass);
                try {
                    Constructor<? extends IDrawChildren> constructor = clz.getConstructor(Context.class, AttributeSet.class, int.class, int.class);
                    iDrawChildren = constructor.newInstance(context, attrs, defStyleAttr, defStyleRes);
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                if (iDrawChildren == null) {
                    iDrawChildren = clz.newInstance();
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                throw new IllegalArgumentException("cannot find " + iDrawChildrenClass + ".", e);
            } catch (InstantiationException e) {
                e.printStackTrace();
                errorDrawChildrenClass(e);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                errorDrawChildrenClass(e);
            }
        }
    }

    private void errorDrawChildrenClass(Exception e) {
        throw new IllegalArgumentException("cannot create " + iDrawChildrenClass + " by default constructor. " +
                "class must have a public default constructor, or public constructor with (" +
                Context.class.getName() + ", " + AttributeSet.class.getName() + ", " + int.class.getName() + ", " + int.class.getName() +
                ").", e);
    }

    @SuppressWarnings("WrongConstant")
    private void initStyle(TypedArray a) {
        if (a != null) {
            setResistance(a.getFloat(R.styleable.ScrollLayout_slt_resistance, resistance));
            setToNormalOffset(a.getInt(R.styleable.ScrollLayout_slt_to_normal_offset, toNormalOffset));
            setFlingOffset(a.getInt(R.styleable.ScrollLayout_slt_fling_offset, flingOffset));
            setDo3DAnim(a.getBoolean(R.styleable.ScrollLayout_slt_do_3d_anim, do3DAnim));
            setCanScrollByTouch(a.getBoolean(R.styleable.ScrollLayout_slt_can_scroll_by_touch, canScrollByTouch));
            setScrollOrientation(a.getInt(R.styleable.ScrollLayout_slt_scroll_orientation, scrollOrientation));
            final String clz = a.getString(R.styleable.ScrollLayout_slt_two_children_adapter);
            twoChildrenAdapterClass = TextUtils.isEmpty(clz) ? twoChildrenAdapterClass : clz;
            final String drawChildrenInterface = a.getString(R.styleable.ScrollLayout_slt_draw_children_interface);
            iDrawChildrenClass = TextUtils.isEmpty(drawChildrenInterface) ? iDrawChildrenClass : drawChildrenInterface;
            a.recycle();
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        int childCount = getChildCount();
        if (childCount == 2) {
            onlyTwoChildren();
        } else if (childCount == 1) {
            onlyOneChild();
        } else if (childCount == 0) {
            return;
        } else {
            children = new View[childCount];
        }
    }

    /**
     * 只有两个子View时的处理逻辑
     */
    private void onlyTwoChildren() {
        if (twoChildrenAdapter == null) {
            throw new IllegalArgumentException("if there are only two children, " +
                    "you must set an 'TwoChildrenAdapter' " +
                    "using 'flt_two_children_adapter=\"full class name\"'");
        }
        onlyTwoChildren = true;
        final View first = getChildAt(0);
        final View second = getChildAt(1);
        final View cloneFirst = twoChildrenAdapter.cloneFirstView(this, first);
        checkCloneView(cloneFirst, first);
        final View cloneSecond = twoChildrenAdapter.cloneSecondView(this, second);
        checkCloneView(cloneSecond, second);

        addView(cloneFirst);
        addView(cloneSecond);
        final int childCount = getChildCount();
        children = new View[childCount];
    }

    private void onlyOneChild() {
        throw new IllegalArgumentException("why not use other ViewGroup?");
    }

    private void resetChildren() {
        int childCount = getChildCount();
        if (children == null || children.length != childCount) {
            children = new View[childCount];
        }
        for (int i = 0 ; i < childCount ; i++) {
            children[i] = getChildAt(i);
            resetView(children[i], i);
        }
    }

    /**
     * 重置view的View的translationY
     * @param v
     * @param index view在{@link #children}中的下标
     */
    private void resetView(View v, int index) {
        switch (scrollOrientation) {
            case VERTICAL:
                v.setTranslationY(index * childHeight);
                break;
            case HORIZONTAL:
                v.setTranslationX(index * childWdith);
                break;
        }
    }

    private void checkCloneView(View clone, View res) {
        if (clone == null)
            throw new NullPointerException("clone view is null.");
        if (clone == res)
            throw new IllegalArgumentException("clone view must not be the same object as old view.");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int count = getChildCount();

        final int pl = getPaddingLeft();
        final int pt = getPaddingTop();
        final int pr = getPaddingRight();
        final int pb = getPaddingBottom();

        int maxHeight = 0;
        int maxWidth = 0;
        int childState = 0;

        final View firstMeasureView = getFirstMeasureView();
        if (firstMeasureView != null) {
            measureChildWithMargins(firstMeasureView, widthMeasureSpec, 0, heightMeasureSpec, 0);
            final LayoutParams lp = (LayoutParams) firstMeasureView.getLayoutParams();
            maxWidth = Math.max(maxWidth,
                    firstMeasureView.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);
            maxHeight = Math.max(maxHeight,
                    firstMeasureView.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
            childState = combineMeasuredStates(childState, firstMeasureView.getMeasuredState());

        }

        maxWidth += pl + pr;
        maxHeight += pt + pb;

        maxHeight = Math.max(maxHeight, getSuggestedMinimumHeight());
        maxWidth = Math.max(maxWidth, getSuggestedMinimumWidth());

        setMeasuredDimension(resolveSizeAndState(maxWidth, widthMeasureSpec, childState),
                resolveSizeAndState(maxHeight, heightMeasureSpec,
                        childState << MEASURED_HEIGHT_STATE_SHIFT));

        mWidth = getMeasuredWidth();
        mHeight = getMeasuredHeight();
        childWdith = mWidth - pl - pr;
        childHeight = mHeight - pt - pb;

        if (count == 0)
            return;
        int start = -1;
        if (count > 1) {
            for (int i = 0; i < count; i++) {
                final View child = getChildAt(i);
                if (child.getVisibility() == GONE)
                    continue;
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();
                if (lp.startFromHere && start == -1) {
                    start = i;
                }
                final int childWidthMeasureSpec;
                if (lp.width == LayoutParams.MATCH_PARENT) {
                    final int width = Math.max(0,
                                               childWdith - lp.leftMargin - lp.rightMargin);
                    childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(
                            width, MeasureSpec.EXACTLY);
                } else {
                    childWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec,
                            pr + pr + lp.leftMargin + lp.rightMargin,
                            lp.width);
                }

                final int childHeightMeasureSpec;
                if (lp.height == LayoutParams.MATCH_PARENT) {
                    final int height = Math.max(0,
                                                childHeight - lp.topMargin - lp.bottomMargin);
                    childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                            height, MeasureSpec.EXACTLY);
                } else {
                    childHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec,
                            pt + pb + lp.topMargin + lp.bottomMargin,
                            lp.height);
                }

                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);
            }
        }
        if (firstMeasure) {
            if (count <= 2)
                return;
            firstMeasure = false;
            resetChildren();
            if (start > 0) {
                startIndex = start;
                currentIndex = startIndex;
                switch (scrollOrientation) {
                    case VERTICAL:
                        scrollTo(0, currentIndex * childHeight);
                        break;
                    case HORIZONTAL:
                        scrollTo(currentIndex * childWdith, 0);
                        break;
                }
            } else {
                currentIndex = 1;
                startIndex = 0;
                switch (scrollOrientation) {
                    case VERTICAL:
                        scrollTo(0, currentIndex * childHeight);
                        break;
                    case HORIZONTAL:
                        scrollTo(currentIndex * childWdith, 0);
                        break;
                }
                gotoChild(0, false);
            }
        }
    }

    /**
     * 获取第一个非{@link #GONE}的子控件
     * @return
     */
    private View getFirstMeasureView() {
        final int c = getChildCount();
        View result = null;
        for (int i = 0 ; i < c ; i ++) {
            final View v = getChildAt(i);
            if (v.getVisibility() != GONE) {
                result = v;
                break;
            }
        }
        return result;
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        firstMeasure = true;
        super.addView(child, index, params);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int pl = getPaddingLeft();
        final int pt = getPaddingTop();

        final int childCount = getChildCount();
        for (int i = 0 ; i < childCount; i ++) {
            final View child = getChildAt(i);
            if (child != null && child.getVisibility() != GONE) {
                final int cmh = child.getMeasuredHeight();
                final int cmw = child.getMeasuredWidth();
                final LayoutParams lp = (LayoutParams) child.getLayoutParams();

                int childLeft = pl + lp.leftMargin;
                final int bottom = pt + cmh;
                child.layout(childLeft, pt, childLeft + cmw, bottom);
            }
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (do3DAnim && iDrawChildren != null) {
            final int childCount = getChildCount();
            final long drawingTime = getDrawingTime();
            for (int i = 0; i < childCount; i++) {
                drawChild(canvas, i, drawingTime);
            }
        } else {
            super.dispatchDraw(canvas);
        }
    }

    /**
     * 3d效果，由{@link IDrawChildren}实现
     * @param canvas
     * @param index
     * @param drawingTime
     */
    private void drawChild(Canvas canvas, int index, long drawingTime) {
        iDrawChildren.drawChild(this, children[index], canvas, scrollOrientation, index, drawingTime);
    }

    @Override
    public boolean drawChild(Canvas canvas, View child, long drawingTime) {
        return super.drawChild(canvas, child, drawingTime);
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (mVelocityTracker != null)
            mVelocityTracker.recycle();
        mVelocityTracker = null;
    }

//start-------------事件处理
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return canScrollByTouch && isSliding;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!canScrollByTouch)
            return super.dispatchTouchEvent(ev);
        final int action = ev.getAction();
        float x = ev.getX();
        float y = ev.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                isSliding = false;
                mDownX = x;
                mDownY = y;
                if (!mScroller.isFinished()) {
                    //当上一次滑动没有结束时，再次点击，强制滑动在点击位置结束
                    switch (scrollOrientation) {
                        case VERTICAL:
                            mScroller.setFinalY(mScroller.getCurrY());
                            break;
                        case HORIZONTAL:
                            mScroller.setFinalX(mScroller.getCurrX());
                            break;
                    }
                    mScroller.abortAnimation();
                    scrollTo(getScrollX(), getScrollY());
                    isSliding = true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (!isSliding) {
                    isSliding = canSliding(ev);
                }
                break;
            default:
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!canScrollByTouch)
            return super.onTouchEvent(event);
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
        float y = event.getY();
        float x = event.getX();
        final int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_MOVE:
                if (isSliding) {
                    mVelocityTracker.addMovement(event);
                    if (mScroller.isFinished()) {
                        switch (scrollOrientation) {
                            case VERTICAL:
                                cycleMoveVertical((int) (mDownY - y));
                                break;
                            case HORIZONTAL:
                                cycleMoveHorizontal((int) (mDownX - x));
                                break;
                        }
                    }
                    mDownY = y;
                    mDownX = x;
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (isSliding) {
                    fling(event);
                }
                if (mVelocityTracker != null) {
                    mVelocityTracker.recycle();
                    mVelocityTracker = null;
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    private boolean canSliding(MotionEvent ev) {
        float deltaY = Math.abs(ev.getY() - mDownY);
        float deltaX = Math.abs(ev.getX() - mDownX);
        switch (scrollOrientation) {
            case VERTICAL:
                return deltaY > mTouchSlop && deltaY > deltaX;
            case HORIZONTAL:
                return deltaX > mTouchSlop && deltaX > deltaY;
        }
        return false;
    }

    /**
     * 处理手指竖直方向移动
     * @param delta
     */
    private void cycleMoveVertical(int delta) {
        delta = delta % mHeight;
        delta = (int) (delta / resistance);
        if (Math.abs(delta) > (mHeight >> 2) ) {
            return;
        }
        scrolling = false;
        scrollBy(0, delta);
        int result = needAddPreOrNext(true, 0);
        if (result == -1) {
            addPre(1);
            scrollBy(0, childHeight);
        } else if (result == 1) {
            addNext(1);
            scrollBy(0, -childHeight);
        }
    }

    /**
     * 处理手指水平方向移动
     * @param delta
     */
    private void cycleMoveHorizontal(int delta) {
        delta = delta % mWidth;
        delta = (int) (delta / resistance);
        if (Math.abs(delta) > (mWidth >> 2) ) {
            return;
        }
        scrolling = false;
        scrollBy(delta, 0);
        int result = needAddPreOrNext(false, 0);
        if (result == -1) {
            addPre(1);
            scrollBy(childWdith, 0);
        } else if (result == 1) {
            addNext(1);
            scrollBy(-childWdith, 0);
        }
    }

    /**
     * 是否需要在最前端或最末尾添加一个view
     * @param vertical
     * @param state 1 : next, -1 : pre , 0 : all
     * @return 0: 不添加，1: 在末尾添加，-1: 在前段添加
     */
    private int needAddPreOrNext(boolean vertical, @IntRange(from = -1, to = 1) int state) {
        final int c = children.length;
        int index = state == 1 ? 0 : 1;
        int lindex = state == -1 ? c - 1 : c - 2;
        if (vertical) {
            final int sy = getScrollY();
            //next : 0, pre || all: 1
            final float top = children[index].getTranslationY() + MIN_NEED_ADD;
            //next || all : c - 2, pre : c - 1
            final float bottom = children[lindex].getTranslationY() - MIN_NEED_ADD;
            if (sy < top)
                return -1;
            else if (sy > bottom)
                return 1;
            return 0;
        } else {
            final int sx = getScrollX();
            final float left = children[index].getTranslationX() + MIN_NEED_ADD;
            final float right = children[lindex].getTranslationX() - MIN_NEED_ADD;
            if (sx < left)
                return -1;
            else if (sx > right)
                return 1;
            return 0;
        }
    }

    /**
     * 处理手指抬起后的滑动
     * @param event
     */
    private void fling(MotionEvent event) {
        mVelocityTracker.addMovement(event);
        isSliding = false;
        mVelocityTracker.computeCurrentVelocity(1000);
        float velocity = 0;
        switch (scrollOrientation) {
            case VERTICAL:
                float yVelocity = mVelocityTracker.getYVelocity();
                final int sy = getScrollY();
                //滑动的速度大于规定的速度，或者向下滑动时，上一页页面展现出的高度超过1/2。则设定状态为STATE_PRE
                if (yVelocity > MAX_SPEED
                        || currentIndex * childHeight - sy > (childHeight >> 1)) {
                    state = STATE_PRE;
                }
                //滑动的速度大于规定的速度，或者向上滑动时，下一页页面展现出的高度超过1/2。则设定状态为STATE_NEXT
                else if ( yVelocity < -MAX_SPEED
                        || sy - currentIndex * childHeight > (childHeight >> 1)) {
                    state = STATE_NEXT;
                } else {
                    state = STATE_NOMARL;
                }
                velocity = yVelocity;
                break;
            case HORIZONTAL:
                float xVelocity = mVelocityTracker.getXVelocity();
                final int sx = getScrollX();
                if (xVelocity > MAX_SPEED
                        || currentIndex * childWdith - sx > (childWdith >> 1)) {
                    state = STATE_PRE;
                }
                else if ( xVelocity < -MAX_SPEED
                        || sx - currentIndex * childWdith > (childWdith >> 1)) {
                    state = STATE_NEXT;
                } else {
                    state = STATE_NOMARL;
                }
                velocity = xVelocity;
                break;
        }

        //根据mState进行相应的变化
        changeByState(velocity);
    }

    private void changeByState(float velocity) {
        if ( (scrollOrientation == VERTICAL && getScrollY() != childHeight)
                || getScrollX() != childWdith) {
            switch (state) {
                case STATE_NOMARL:
                    toNormalAction();
                    break;
                case STATE_PRE:
                    toPreAction(velocity);
                    break;
                case STATE_NEXT:
                    toNextAction(velocity);
                    break;
            }
            invalidate();
        }
    }

    /**
     * mState = State.Normal 时进行的动作
     */
    private void toNormalAction() {
        alreadyAddCount = 0;
        state = STATE_NOMARL;
        scrolling = true;
        switch (scrollOrientation) {
            case VERTICAL:
                int startY = getScrollY();
                int delta = childHeight * currentIndex - startY;
                int duration = (Math.abs(delta)) * toNormalOffset;
                mScroller.startScroll(0, startY, 0, delta, duration);
                break;
            case HORIZONTAL:
                int startX = getScrollX();
                int deltax = childWdith * currentIndex - startX;
                int durationX = (Math.abs(deltax)) * toNormalOffset;
                mScroller.startScroll(startX, 0, deltax, 0, durationX);
                break;
        }
    }

    /**
     * state == STATE_PRE
     *
     * @param velocity 速度
     */
    private void toPreAction(float velocity) {
        alreadyAddCount = 0;
        state = STATE_PRE;
        scrolling = true;
        int addCount = getAddCount(velocity);
        int startY = 0;
        int startX = 0;
        int deltaY = 0;
        int deltaX = 0;
        int duration = 0;
        final int result = needAddPreOrNext(scrollOrientation == VERTICAL, -1);
        switch (scrollOrientation) {
            case VERTICAL:
                if (result == -1) {
                    startY = getScrollY() + childHeight;
                    addPre(1);
                    addCount--;
                    deltaY = startY - currentIndex * childHeight + addCount * childHeight;
                } else {
                    startY = getScrollY();
                    deltaY = startY - currentIndex * childHeight + addCount * childHeight;
                    addCount--;
                }
                setScrollY(startY);
                duration = (Math.abs(deltaY)) * flingOffset;
                break;
            case HORIZONTAL:
                if (result == -1) {
                    startX = getScrollX() + childWdith;
                    addPre(1);
                    addCount--;
                    deltaX = startX - currentIndex * childWdith + addCount * childWdith;
                } else {
                    startX = getScrollX();
                    deltaX = startX - currentIndex * childWdith + addCount * childWdith;
                    addCount--;
                }
                setScrollX(startX);
                duration = Math.abs(deltaX) * flingOffset;
                break;
        }
        mScroller.startScroll(startX, startY, -deltaX, -deltaY, duration);
        invalidate();
    }

    private void addCurrentIndex(int offset) {
        final int l = children.length;
        if ((currentIndex <= 1 && offset < 0) || (currentIndex >= l - 2 && offset > 0) )
            return;
        currentIndex += l + offset;
        currentIndex = currentIndex % l;
    }

    /**
     * state == STATE_NEXT
     *
     * @param velocity 速度
     */
    private void toNextAction(float velocity) {
        alreadyAddCount = 0;
        state = STATE_NEXT;
        scrolling = true;
        int addCount = getAddCount(velocity);
        int startY = 0;
        int startX = 0;
        int deltaY = 0;
        int deltaX = 0;
        int duration = 0;
        final int result = needAddPreOrNext(scrollOrientation == VERTICAL, 1);
        switch (scrollOrientation) {
            case VERTICAL:
                if (result == 1) {
                    startY = getScrollY() - childHeight;
                    addNext(1);
                    addCount--;
                    deltaY = childHeight * currentIndex - startY + addCount * childHeight;
                } else {
                    startY = getScrollY();
                    deltaY = childHeight * currentIndex - startY + addCount * childHeight;
                    addCount--;
                }
                setScrollY(startY);
                duration = (Math.abs(deltaY)) * flingOffset;
                break;
            case HORIZONTAL:
                if (result == 1) {
                    startX = getScrollX() - childWdith;
                    addNext(1);
                    addCount--;
                    deltaX = childWdith * currentIndex - startX + addCount * childWdith;
                } else {
                    startX = getScrollX();
                    deltaX = childWdith * currentIndex - startX + addCount * childWdith;
                    addCount--;
                }
                setScrollX(startX);
                duration = Math.abs(deltaX) * flingOffset;
                break;
        }
        mScroller.startScroll(startX, startY, deltaX, deltaY, duration);
        invalidate();
    }

    @Override
    public void computeScroll() {
        //滑动没有结束时，进行的操作
        if (mScroller.computeScrollOffset()) {
            int sy = 0;
            int sx = 0;
            int currX = mScroller.getCurrX();
            int currY = mScroller.getCurrY();
            switch (state) {
                case STATE_PRE:
                    boolean need = needAddPreOrNext(scrollOrientation == VERTICAL, -1) == -1;
                    switch (scrollOrientation) {
                        case VERTICAL:
                            scrollTo(currX, currY + childHeight * alreadyAddCount);
                            sy = getScrollY();
                            if (need) {
                                addPre(1);
                                addCurrentIndex(1);
                                setScrollY(sy + childHeight);
                                alreadyAddCount ++;
                            }
                            break;
                        case HORIZONTAL:
                            scrollTo(currX + childWdith * alreadyAddCount, currY);
                            sx = getScrollX();
                            if (need) {
                                addPre(1);
                                addCurrentIndex(1);
                                setScrollX(sx + childWdith);
                                alreadyAddCount ++;
                            }
                            break;
                    }
                    break;
                case STATE_NEXT:
                    need = needAddPreOrNext(scrollOrientation == VERTICAL, 1) == 1;
                    switch (scrollOrientation) {
                        case VERTICAL:
                            scrollTo(currX, currY - childHeight * alreadyAddCount);
                            sy = getScrollY();
                            if (need) {
                                addNext(1);
                                addCurrentIndex(-1);
                                setScrollY(sy - childHeight);
                                alreadyAddCount ++;
                            }
                            break;
                        case HORIZONTAL:
                            scrollTo(currX - childWdith * alreadyAddCount, currY);
                            sx = getScrollX();
                            if (need) {
                                addNext(1);
                                addCurrentIndex(-1);
                                setScrollX(sx - childWdith);
                                alreadyAddCount ++;
                            }
                            break;
                    }
                    break;
                default:
                    scrollTo(currX, currY);
                    break;
            }
            postInvalidate();
        }
        //滑动结束时相关用于计数变量复位
        if (mScroller.isFinished()) {
            alreadyAddCount = 0;
            if (scrolling) {
                scrolling = false;
                notifyChangeListener();
            }
        }
    }

    /**
     * 根据速度获取需要添加多少个view
     * @param velocity
     * @return
     */
    private int getAddCount(float velocity) {
        float absv = Math.abs(velocity);
        float offset = absv - MAX_SPEED;
        int flingSpeedCount = offset > 0 ? (int) offset : 0;
        return (int) (flingSpeedCount / MIN_SPEED / resistance + 1);
    }

    private float getSpeedByAddCount(int addCount) {
        return (addCount - 1) * resistance * MIN_SPEED + MAX_SPEED;
    }

    /**
     * 把{@link #children}中后num个view移到前面
     * @param num
     */
    private void addPre(int num) {
        final int c = children.length;
        final View[] temp = new View[num];
        for (int i = c - 1; i >= 0 ; i--) {
            final int offset = c - i;
            if (offset <= num) {
                temp[num - offset] = children[i];
            }
            final int pre = i - num;
            if (pre >= 0) {
                children[i] = children[pre];
            } else {
                children[i] = temp[i];
            }
            resetView(children[i], i);
        }
    }

    /**
     * 把{@link #children}中前num个view移到后面
     * @param num
     */
    private void addNext(int num) {
        final int c = children.length;
        final View[] temp = new View[num];
        for (int i = 0 ; i < c ; i ++) {
            if (i < num) {
                temp[i] = children[i];
            }
            final int next = i + num;
            if (next < c) {
                children[i] = children[next];
            } else {
                children[i] = temp[next - c];
            }
            resetView(children[i], i);
        }
    }

//end---------------事件处理

    private void notifyChangeListener() {
        if (onChangeListener != null) {
            int index = currentIndex;
            if (onlyTwoChildren) {
                View v = children[index];
                if (v == getChildAt(0)) {
                    onChangeListener.changeTo(v, 0);
                } else if (v == getChildAt(1)) {
                    onChangeListener.changeTo(v, 1);
                } else {
                    v = children[(index + 2) % children.length];
                    onChangeListener.changeTo(v, getViewIndex(v));
                }
                return;
            }
            final View v = children[index];
            onChangeListener.changeTo(v, getViewIndex(v));
        }
    }

    private int getViewIndex(View v) {
        final int l = getChildCount();
        for (int i = 0 ; i < l ; i ++) {
            final View c = getChildAt(i);
            if (c == v)
                return i;
        }
        return -1;
    }

    private int getChildrenIndex(View v) {
        final int l = children.length;
        for (int i = 0 ; i < l ; i ++) {
            final View c = children[i];
            if (v == c)
                return i;
        }
        return -1;
    }
//start-------------- public method

    public int getChildWdith() {
        return childWdith;
    }

    public int getChildHeight() {
        return childHeight;
    }

    /**
     * 若通过{@link #addView(View)}新添加了子View，需调用此方法
     */
    public void notifyAddChildViewFinish() {
        final int c = getChildCount();
        if (c == 0) {

        } else if (c == 1) {
            onlyOneChild();
        } else if (c == 2) {
            onlyTwoChildren();
        } else {
            children = new View[c];
        }
    }

    public @ScrollOrientation int getScrollOrientation() {
        return scrollOrientation;
    }

    public void setScrollOrientation(@ScrollOrientation int scrollOrientation) {
        if (scrollOrientation != VERTICAL && scrollOrientation != HORIZONTAL)
            return;
        this.scrollOrientation = scrollOrientation;
    }

    public boolean isDo3DAnim() {
        return do3DAnim;
    }

    /**
     * 设置滚动时是否做3D动画
     * @param do3DAnim
     */
    public void setDo3DAnim(boolean do3DAnim) {
        this.do3DAnim = do3DAnim;
    }

    public boolean isCanScrollByTouch() {
        return canScrollByTouch;
    }

    /**
     * 是否能通过触摸来滚动子view，默认为true
     * @param canScrollByTouch
     */
    public void setCanScrollByTouch(boolean canScrollByTouch) {
        this.canScrollByTouch = canScrollByTouch;
    }

    public int getFlingOffset() {
        return flingOffset;
    }

    /**
     * 设置自由滑动时时间倍数[1,+)，1：速度最快
     * @param flingOffset
     */
    public void setFlingOffset(int flingOffset) {
        if (flingOffset < 1)
            throw new IllegalArgumentException("offset must be equals or greater than 1.");
        this.flingOffset = flingOffset;
    }

    public int getToNormalOffset() {
        return toNormalOffset;
    }

    /**
     * 设置回位时间倍数[1,+)，1: 速度最快
     * @param toNormalOffset
     */
    public void setToNormalOffset(int toNormalOffset) {
        if (toNormalOffset < 1)
            throw new IllegalArgumentException("offset must be equals or greater than 1.");
        this.toNormalOffset = toNormalOffset;
    }

    public float getResistance() {
        return resistance;
    }

    /**
     * 设置滑动阻力(0, +)
     * @param resistance 趋近0: 速度最快; 1: 速度和手速相同
     */
    public void setResistance(float resistance) {
        if (resistance <= 0)
            throw new IllegalArgumentException("resistance must be greater than 0.");
        this.resistance = resistance;
    }

    public void reset() {
        resetChildren();
        currentIndex = startIndex == 0 ? 1 : startIndex;
        alreadyAddCount = 0;
        switch (scrollOrientation) {
            case VERTICAL:
                scrollTo(0, currentIndex * childHeight);
                break;
            case HORIZONTAL:
                scrollTo(currentIndex * childWdith, 0);
                break;
        }
        if (startIndex == 0) {
            gotoChild(startIndex, false);
        }
    }

    public void toNext(boolean smooth) {
        if (!mScroller.isFinished())
            mScroller.abortAnimation();
        if (smooth)
            toNextAction(-MAX_SPEED);
        else {
            addNext(1);
            invalidate();
            notifyChangeListener();
        }
    }

    public void toPre(boolean smooth) {
        if (!mScroller.isFinished())
            mScroller.abortAnimation();
        if (smooth)
            toPreAction(MAX_SPEED);
        else {
            addPre(1);
            invalidate();
            notifyChangeListener();
        }
    }

    public void gotoChild(int index, boolean smooth) {
        int c = getChildCount();
        if (onlyTwoChildren)
            c -= 2;
        if (index < 0 || index >= c) {
            throw new IndexOutOfBoundsException("index must be equals or greater than 0 and less than " + c);
        }
        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }
        final View v = getChildAt(index);
        index = getChildrenIndex(v);

        if (currentIndex == index)
            return;
        if (onlyTwoChildren && (currentIndex % 2 == index)) {
            return;
        }

        if (index > currentIndex) {
            int offset = index - currentIndex;
            if (smooth)
                toNextAction(-getSpeedByAddCount(offset));
            else {
                addNext(offset);
                invalidate();
                notifyChangeListener();
            }
        } else if (index < currentIndex) {
            int offset = currentIndex - index;
            if (smooth)
                toPreAction(getSpeedByAddCount(offset));
            else {
                addPre(offset);
                invalidate();
                notifyChangeListener();
            }
        }
    }

    /**
     * 获取当前view在{@link #children}中的下标
     * @return
     */
    public int getCurrentIndex() {
        return currentIndex;
    }

    /**
     * 获取当前view在layout中的下标
     * @return
     */
    public int getCurrentViewIndex() {
        return getViewIndex(children[currentIndex]);
    }

    /**
     * 告知layout 此View更改了状态，并通知{@link #twoChildrenAdapter}将状态同步
     * @param changedView
     */
    public void notifyViewChanged(View changedView) {
        final int c = getChildCount();
        for (int i = 0 ; i < c; i ++) {
            final View v = getChildAt(i);
            if (v == changedView) {
                final int index = (i + 2) % c;
                twoChildrenAdapter.bindViewData(changedView, getChildAt(index));
                break;
            }
        }
    }

    public TwoChildrenAdapter getTwoChildrenAdapter() {
        return twoChildrenAdapter;
    }

    /**
     * 如果只有两个子view的话，需要设置adapter
     * 如果更改了其中某个view的状态，必须调用{@link #notifyViewChanged(View)}告知
     * @param adapter
     */
    public void setTwoChildrenAdapter(TwoChildrenAdapter adapter) {
        this.twoChildrenAdapter = adapter;
    }

    public void setOnChangeListener(OnChangeListener onChangeListener) {
        this.onChangeListener = onChangeListener;
    }

    public IDrawChildren getiDrawChildren() {
        return iDrawChildren;
    }

//end-----------public method

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    public static class LayoutParams extends MarginLayoutParams {
        private boolean startFromHere = false;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);

            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.ScrollLayoutParams);
            if (a != null) {
                startFromHere = a.getBoolean(R.styleable.ScrollLayoutParams_slp_layout_start_from_here, false);

                a.recycle();
            }
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }
    }
}
