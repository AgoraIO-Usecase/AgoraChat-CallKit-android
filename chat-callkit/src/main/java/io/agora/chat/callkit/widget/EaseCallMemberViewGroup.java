package io.agora.chat.callkit.widget;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
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
        //Measure the width and height of the screen. The default view is full screen. If the current view is not full screen, you need to modify the calculation method.
        WindowManager wm = (WindowManager) getContext()
                .getSystemService(Context.WINDOW_SERVICE);
        Point p = new Point();
        wm.getDefaultDisplay().getSize(p);
        mWidth = p.x;
        mHeight = p.y;
        touchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
    }

    /**
     * Override the onMeasure method, which loops to set the size of the child controls of the current custom control
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int heightMeasure = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        //Store the last calculated height
        int totalHeight = 0;
        // Get the number of inner elements
        int count = getChildCount();
        //Store Child View
        View child;
        int right = 0;
        pageCount = (getChildCount() - 1) / maxSizeOnePage + 1;
        //Traverse the child View to calculate the width and height of the parent control
        for (int i = 0; i < count; i++) {
            child = getChildAt(i);
            if (View.GONE == child.getVisibility()) {
                continue;
            }
            //Measurement Child View
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            //You can randomly choose the height of the item
            itemHeight = child.getMeasuredHeight();
            int p = i / maxSizeOnePage;// current page.
            right = Math.max(right, p * itemWidth * itemCountOneLine + itemWidth + getPaddingLeft() + getPaddingRight() + (i % maxSizeOnePage) / itemCountOneRow * itemWidth);
        }

        //Adapt to padding. If it is wrap_content, in addition to the controls occupied by the child control itself, the padding of the parent control should also be added.
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
        //Child View Count
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
                    //The first page is arranged left and right
                    l = baseLeft + getPaddingLeft() + (i % itemCountOneLine) * itemWidth;
                    t = baseTop + (i % maxSizeOnePage) / itemCountOneLine * itemHeight + getPaddingTop();
                    r = baseLeft + child.getMeasuredWidth() + getPaddingLeft() + (i % itemCountOneLine) * itemWidth;
                    b = baseTop + (i % maxSizeOnePage) / itemCountOneLine * itemHeight + child.getMeasuredHeight() + getPaddingTop();
                } else {
                    //The second page starts to arrange up and down
                    l = baseLeft + p * itemWidth * itemCountOneLine + getPaddingLeft() + (i % maxSizeOnePage) / itemCountOneRow * itemWidth;
                    t = baseTop + (i % maxSizeOnePage) % itemCountOneRow * itemHeight + getPaddingTop();
                    r = baseLeft + p * itemWidth * itemCountOneLine + child.getMeasuredWidth() + getPaddingLeft() + (i % maxSizeOnePage) / itemCountOneRow * itemWidth;
                    b = baseTop + (i % maxSizeOnePage) % itemCountOneRow * itemHeight + child.getMeasuredHeight() + getPaddingTop();
                }
            }
            if (callType == EaseCallType.CONFERENCE_VIDEO_CALL) {
                child.setOnClickListener(this);
            }
            Log.d(TAG, "l=" + l + ",t=" + t + ",r=" + r + ",b=" + b + ",itemWidth=" + itemWidth);
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
        ViewParent parent = child.getParent();
        if(parent!=null) {
            ((ViewGroup)parent).removeView(child);
        }
        super.addView(child);
        doAddView(child);
    }

    @Override
    public void addView(View child, int index) {
        ViewParent parent = child.getParent();
        if(parent!=null) {
            ((ViewGroup)parent).removeView(child);
        }
        super.addView(child, index);
        doAddView(child);
    }

    private void doAddView(View child) {
        if (isFullScreenMode()) {
            EMLog.i(TAG, "addView, isFullScreenMode: " + isFullScreenMode());
            // The size setting and sliding of the child view are not performed in full screen mode
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
            // Can only be clicked to enter full screen when video is turned on
            if (v instanceof EaseCallMemberView && ((EaseCallMemberView) v).isShowVideo()) {
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
        // Only change the layout parameters of all child views on the page where the child view is located.
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            LayoutParams lp = child.getLayoutParams();
            if (view == child) {
                lp.width = mWidth;
                lp.height = mHeight;
            } else {
                //Cannot be 0, because abnormal behavior may occur on some phones
                lp.width = 1;
                lp.height = 1;
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
            if (getChildCount() == 3 && i == 2&&callType==EaseCallType.CONFERENCE_VIDEO_CALL) {//Special placement when there are only three settings
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
     * Set Item Click Listener
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
