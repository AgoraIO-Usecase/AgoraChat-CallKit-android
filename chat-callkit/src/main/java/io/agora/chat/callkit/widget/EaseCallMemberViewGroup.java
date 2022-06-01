package io.agora.chat.callkit.widget;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.annotation.Nullable;

import io.agora.chat.callkit.general.EaseCallType;
import io.agora.util.EMLog;


public class EaseCallMemberViewGroup extends ViewGroup implements View.OnClickListener {
    private static final String TAG = EaseCallMemberViewGroup.class.getSimpleName();
    private OnItemClickListener onItemClickListener;
    private OnScreenModeChangeListener onScreenModeChangeListener;

    private int mWidth = 0;
    private int mHeight = 0;
    // current page index.
    private int pageIndex = 0;
    private int pageCount = 1;
    private View fullScreenView;
    private int itemWidth;
    private int itemHeight;
    private int maxSizeOnePage=1;
    int touchSlop;

    //below is the place where item is setted
    private int itemCountOneLine = 2;
    private int itemCountOneRow = 2;

    private EaseCallType callType = EaseCallType.CONFERENCE_VIDEO_CALL;

    public EaseCallMemberViewGroup(Context context) {
        this(context, null);
    }

    public EaseCallMemberViewGroup(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EaseCallMemberViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        //测量屏幕宽高,该view默认为全屏,若当前view不为全屏,则该计算方式需要修改.
        WindowManager wm = (WindowManager) getContext()
                .getSystemService(Context.WINDOW_SERVICE);
        Point p = new Point();
        wm.getDefaultDisplay().getSize(p);
        mWidth = p.x;
        mHeight = p.y;
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    /**
     * 重写onMeasure方法，这里循环设置当前自定义控件的子控件的大小
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightMeasure = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        //存储最后计算出的高度
        int totalHeight = 0;
        // 得到内部元素的个数
        int count = getChildCount();
        //存储子View
        View child;
        int right = 0;
        pageCount = (getChildCount() - 1) / maxSizeOnePage + 1;
        //遍历子View 计算父控件宽高
        for (int i = 0; i < count; i++) {
            child = getChildAt(i);
            if (View.GONE == child.getVisibility()) {
                continue;
            }
            //先测量子View
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            //随机取一个item的高度都可以
            itemHeight = child.getMeasuredHeight();
            int p = i / maxSizeOnePage;// current page.
            right = Math.max(right, p * itemWidth * itemCountOneLine + itemWidth + getPaddingLeft() + getPaddingRight() + (i % maxSizeOnePage) / itemCountOneRow * itemWidth);
        }

        //适配padding,如果是wrap_content,则除了子控件本身占据的控件，还要在加上父控件的padding
        int measureHeiht = heightMode != MeasureSpec.EXACTLY ? totalHeight + getPaddingTop() + getPaddingBottom() : heightMeasure;

        if(isFullScreenMode()) {
            setMeasuredDimension(mWidth,measureHeiht);
        }else{
            setMeasuredDimension(Math.max(right, mWidth), measureHeiht);
        }


        if (callType == EaseCallType.CONFERENCE_VIDEO_CALL) {
            itemHeight = getMeasuredHeight() / itemCountOneRow;
        } else {
            itemHeight = itemWidth + dp2px(30);
        }
        if (!isFullScreenMode()) {
            resetAllViews();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {

    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        //子控件的个数
        int count = getChildCount();
        int baseLeft = 0;
        int baseTop = 0;
        for (int i = 0; i < count; i++) {

            View child = getChildAt(i);
            int p = i / maxSizeOnePage;// current page.
            int l = 0, t = 0, r = 0, b = 0;
            if (child.getVisibility() == GONE) {
                continue;
            }
            if (callType == EaseCallType.CONFERENCE_VOICE_CALL) {
                if (count == 1) {
                    baseLeft = itemWidth;
                    baseTop = itemHeight + itemHeight / 2;
                } else if (count == 2) {
                    baseLeft = itemWidth / 2;
                    baseTop = itemHeight + itemHeight / 2;
                } else if (count <= 6) {
                    baseLeft = 0;
                    baseTop = itemHeight;
                } else {
                    baseLeft = 0;
                    baseTop = itemHeight / 2;
                }
            }
            if (isFullScreenMode()) {
                l = 0;
                t = 0;
                r = mWidth;
                b = mHeight;
            } else {
                if (p == 0) {
                    //第一页左右排队
                    l = baseLeft + getPaddingLeft() + (i % itemCountOneLine) * itemWidth;
                    t = baseTop + (i % maxSizeOnePage) / itemCountOneLine * itemHeight + getPaddingTop();
                    r = baseLeft + child.getMeasuredWidth() + getPaddingLeft() + (i % itemCountOneLine) * itemWidth;
                    b = baseTop + (i % maxSizeOnePage) / itemCountOneLine * itemHeight + child.getMeasuredHeight() + getPaddingTop();
                } else {
                    //第二页开始上下排队
                    l = baseLeft + p * itemWidth * itemCountOneLine + getPaddingLeft() + (i % maxSizeOnePage) / itemCountOneRow * itemWidth;
                    t = baseTop + (i % maxSizeOnePage) % itemCountOneRow * itemHeight + getPaddingTop();
                    r = baseLeft + p * itemWidth * itemCountOneLine + child.getMeasuredWidth() + getPaddingLeft() + (i % maxSizeOnePage) / itemCountOneRow * itemWidth;
                    b = baseTop + (i % maxSizeOnePage) % itemCountOneRow * itemHeight + child.getMeasuredHeight() + getPaddingTop();
                }
            }
            if (callType == EaseCallType.CONFERENCE_VIDEO_CALL) {
                child.setOnClickListener(this);
            }
            Log.d(TAG, "l=" + l + ",t=" + t + ",r=" + r + ",b=" + b + ",itemwidth=" + itemWidth);
            child.layout(l, t, r, b);
        }
    }

    public void setRowAndLine(int row, int line) {
        itemCountOneRow = row;
        itemCountOneLine = line;
        maxSizeOnePage = itemCountOneLine * itemCountOneRow;
        itemWidth = mWidth / itemCountOneLine;
        requestLayout();
    }

    public void setCallType(EaseCallType callType) {
        this.callType = callType;

        for (int i = 0; i < getChildCount(); i++) {
            ((EaseCallMemberView) getChildAt(i)).setCallType(callType);
        }
        if (callType == EaseCallType.CONFERENCE_VIDEO_CALL) {
            setRowAndLine(2, 2);
        } else {
            setRowAndLine(3, 3);
        }
    }

    @Override
    public void addView(View child) {
        super.addView(child);
        if (isFullScreenMode()) {
            EMLog.i(TAG, "addView, isFullScreenMode: " + isFullScreenMode());
            // 全屏模式下不进行子view的大小设置和滑动
            return;
        }
        if (child instanceof EaseCallMemberView) {
            ((EaseCallMemberView) child).setCallType(callType);
        }
        setViewParams(child, itemWidth, itemHeight);
    }

    private void setViewParams(View target, int widthBorder, int heightBorder) {
        LayoutParams params = target.getLayoutParams();
        params.width = widthBorder;
        params.height = heightBorder;
        target.setLayoutParams(params);
    }

    private void handleItemClickAction(View v, int index) {
        if (isFullScreenMode()) {
            fullScreenView = null;
            if (onScreenModeChangeListener != null) {
                onScreenModeChangeListener.onScreenModeChange(isFullScreenMode(), fullScreenView);
            }
        } else {
            // 仅当开启视频后才能被点击进入全屏
            if (v instanceof EaseCallMemberView && !((EaseCallMemberView) v).isShowVideo()) {
                fullScreen(v);
            }
        }
        if (onItemClickListener != null) {
            onItemClickListener.onItemClick(v, index);
        }
    }

    private void fullScreen(View view) {
        fullScreenView = view;
        if (onScreenModeChangeListener != null) {
            onScreenModeChangeListener.onScreenModeChange(isFullScreenMode(), fullScreenView);
        }
        // 只更改child view所在页的所有子view的layout parameters.
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            LayoutParams lp = child.getLayoutParams();
            if (view == child) {
                lp.width = mWidth;
                lp.height = mHeight;
            } else {
                lp.width = 0;
                lp.height = 0;
            }
            child.setLayoutParams(lp);
        }

        if (view instanceof EaseCallMemberView) {
            ((EaseCallMemberView) view).setFullScreen(isFullScreenMode());
        }
    }

    private void resetAllViews() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (getChildCount() == 3 && i == 2&&callType==EaseCallType.CONFERENCE_VIDEO_CALL) {//设置只有三个时的特殊摆放
                setViewParams(child, getMeasuredWidth(), itemHeight);
            } else {
                setViewParams(child, itemWidth, itemHeight);
            }
            if (child instanceof EaseCallMemberView) {
                ((EaseCallMemberView) child).setFullScreen(isFullScreenMode());
            }
        }

    }

    @Override
    public void onClick(View v) {
        handleItemClickAction(v, indexOfChild(v));
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public interface OnScreenModeChangeListener {
        /**
         * @param isFullScreenMode
         * @param fullScreenView   Is null if {isFullScreenMode} is false.
         */
        void onScreenModeChange(boolean isFullScreenMode, @Nullable View fullScreenView);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams lp) {
        return new MarginLayoutParams(lp);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    public int getPageCount() {
        return pageCount;
    }

    public int currentIndex() {
        return pageIndex;
    }

    /**
     * 设置子控件的点击监听
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        onItemClickListener = listener;
    }

    public void setOnScreenModeChangeListener(OnScreenModeChangeListener listener) {
        onScreenModeChangeListener = listener;
    }

    public boolean isFullScreenMode() {
        return fullScreenView != null;
    }

    public View getFullScreenView() {
        return fullScreenView;
    }

    private int dp2px(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }
}
