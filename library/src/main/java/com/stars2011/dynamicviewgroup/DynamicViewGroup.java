package com.stars2011.dynamicviewgroup;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by stars2011
 */

public class DynamicViewGroup extends ViewGroup {

    public static final String TAG = "DynamicViewGroup";
    public static final int HORIZONTAL = 0; // 横向布局
    public static final int VERTICAL = 1; // 竖向布局

    private int mode = VERTICAL;

    public DynamicViewGroup(Context context) {
        this(context, null);
    }

    public DynamicViewGroup(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DynamicViewGroup(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureDependOnMode(widthMeasureSpec, heightMeasureSpec);
    }

    /**
     * 根据当前模式选择对应的测量模式并设置
     */
    private void measureDependOnMode(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        if (widthMode == MeasureSpec.AT_MOST && heightMode == MeasureSpec.AT_MOST) { // 宽高都是wrap_content
            calculateSizeAndSetMeasuredDimensionDependOnMode(widthSize, false, heightSize, false);
        } else if (widthMode == MeasureSpec.AT_MOST) { // 无论高是EXACTLY还是UNSPECIFIED，都直接使用heightSize
            calculateSizeAndSetMeasuredDimensionDependOnMode(widthSize, false, heightSize, true);
        } else if (heightMode == MeasureSpec.AT_MOST) { // 无论宽是EXACTLY还是UNSPECIFIED，都直接使用widthSize
            calculateSizeAndSetMeasuredDimensionDependOnMode(widthSize, true, heightSize, false);
        }
    }

    /**
     * 根据模式计算ViewGroup的尺寸，并设置
     *
     * @param maxWidth 最大宽度
     * @param widthBeMax 最后尺寸是否直接使用最大宽度（当此选项为真则表示宽度是已经固定的，不需要计算，传入参数为了给计算高度的时候提供一个边界）
     * @param maxHeight 最大高度
     * @param heightBeMax 最后尺寸是否直接使用最大高度（当此选项为真则表示高度是已经固定的，不需要计算，传入参数为了给计算宽度的时候提供一个边界）
     */
    private void calculateSizeAndSetMeasuredDimensionDependOnMode(int maxWidth, boolean widthBeMax, int maxHeight, boolean heightBeMax) {
        ResultSize resultSize = null;
        switch (mode) {
            case HORIZONTAL:
                resultSize = getMeasureResultSizeForHorizontal(maxWidth, widthBeMax, maxHeight, heightBeMax);
                break;
            case VERTICAL:
                resultSize = getMeasureResultSizeForVertical(maxWidth, widthBeMax, maxHeight, heightBeMax);
                break;
        }
        if (resultSize == null) {
            Log.e(TAG, "resultSize null when calculateSize");
            return;
        }
        setMeasuredDimension(resultSize.getResultWidth(), resultSize.getResultHeight());
    }

    /**
     * 横向模式获取计算好的尺寸
     */
    private ResultSize getMeasureResultSizeForHorizontal(int maxWidth, boolean widthBeMax, int maxHeight, boolean heightBeMax) {
        int resultWidth = 0;
        int resultHeight = 0;
        int calculateWidth = 0;
        int calculateHeight = 0;
        int childCount = getChildCount();
        // 遍历子View计算宽高
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            // 获取margin信息
            ChildViewMarginSize marginSize = getChildViewMargin(childView);
            int leftMargin = marginSize.getLeftMargin();
            int rightMargin = marginSize.getRightMargin();
            int topMargin = marginSize.getTopMargin();
            int bottomMargin = marginSize.getBottomMargin();

            int childViewWidth = childView.getMeasuredWidth() + leftMargin + rightMargin;
            int childViewHeight = childView.getMeasuredHeight() + topMargin + bottomMargin;
            if (calculateWidth + childViewWidth > maxWidth) { // 超过了单行最大的宽度,需要换行
                // 换行的时候更新left和top
                resultWidth = Math.max(resultWidth, calculateWidth);
                resultHeight += calculateHeight;
                if (resultHeight > maxHeight) {
                    Log.e(TAG, "we have no room for view");
                    return new ResultSize(resultWidth, resultHeight);
                }
                calculateWidth = 0;
                calculateHeight = 0;
                // 继续做插入测量
                calculateWidth += childViewWidth;
                calculateHeight = Math.max(calculateHeight, childViewHeight);
                if (i == (childCount - 1)) { // 到了最后一个View了,即将返回，对Result赋值
                    resultWidth = Math.max(resultWidth, calculateWidth);
                    resultHeight += calculateHeight;
                }
            } else {
                calculateWidth += childViewWidth;
                calculateHeight = Math.max(calculateHeight, childViewHeight);
                if (i == (childCount - 1)) { // 到了最后一个View了,即将返回，对Result赋值
                    resultWidth = Math.max(resultWidth, calculateWidth);
                    resultHeight += calculateHeight;
                }
            }
        }

        // 添加padding的计算
        int[] resultWidthAndHeightAfterAddPadding = addPaddingToWidthAndHeight(resultWidth, maxWidth, resultHeight, maxHeight);
        resultWidth = resultWidthAndHeightAfterAddPadding[0];
        resultHeight = resultWidthAndHeightAfterAddPadding[1];

        if (widthBeMax) {
            resultWidth = maxWidth;
        }
        if (heightBeMax) {
            resultHeight = maxHeight;
        }
        return new ResultSize(resultWidth, resultHeight);
    }

    /**
     * 竖向模式获取计算好的尺寸
     */
    private ResultSize getMeasureResultSizeForVertical(int maxWidth, boolean widthBeMax, int maxHeight, boolean heightBeMax) {
        int resultWidth = 0;
        int resultHeight = 0;
        int calculateWidth = 0;
        int calculateHeight = 0;
        int childCount = getChildCount();
        // 遍历子View计算宽高
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            // 获取margin信息
            ChildViewMarginSize marginSize = getChildViewMargin(childView);
            int leftMargin = marginSize.getLeftMargin();
            int rightMargin = marginSize.getRightMargin();
            int topMargin = marginSize.getTopMargin();
            int bottomMargin = marginSize.getBottomMargin();

            int childViewWidth = childView.getMeasuredWidth() + leftMargin + rightMargin;
            int childViewHeight = childView.getMeasuredHeight() + topMargin + bottomMargin;

            if (calculateHeight + childViewHeight > maxHeight) { // 超过了单列最大的高度,需要换列
                // 换列的时候更新left和top
                resultHeight = Math.max(resultHeight, calculateHeight);
                resultWidth += calculateWidth;
                if (resultWidth > maxWidth) {
                    Log.e(TAG, "we have no room for view");
                    return new ResultSize(resultWidth, resultHeight);
                }
                calculateWidth = 0;
                calculateHeight = 0;
                // 继续做插入测量
                calculateHeight += childViewHeight;
                calculateWidth = Math.max(calculateWidth, childViewWidth);
                if (i == (childCount - 1)) { // 到了最后一个View了,即将返回，对Result赋值
                    resultHeight = Math.max(resultHeight, calculateHeight);
                    resultWidth += calculateWidth;
                }
            } else {
                calculateHeight += childViewHeight;
                calculateWidth = Math.max(calculateWidth, childViewWidth);
                if (i == (childCount - 1)) { // 到了最后一个View了,即将返回，对Result赋值
                    resultHeight = Math.max(resultHeight, calculateHeight);
                    resultWidth += calculateWidth;
                }
            }
        }

        // 添加padding的计算
        int[] resultWidthAndHeightAfterAddPadding = addPaddingToWidthAndHeight(resultWidth, maxWidth, resultHeight, maxHeight);
        resultWidth = resultWidthAndHeightAfterAddPadding[0];
        resultHeight = resultWidthAndHeightAfterAddPadding[1];

        if (widthBeMax) {
            resultWidth = maxWidth;
        }
        if (heightBeMax) {
            resultHeight = maxHeight;
        }
        return new ResultSize(resultWidth, resultHeight);
    }

    /**
     * 计算ViewGroup宽高的时候为宽和高添加padding
     * @param resultWidth 根据子View计算出的ViewGroup宽度
     * @param maxWidth ViewGroup的最大宽度
     * @param resultHeight 根据子View计算出的ViewGroup高度
     * @param maxHeight ViewGroup的最大高度
     * @return 包含两个元素的int数组，宽度的索引为0，高度的索引为1
     */
    private int[] addPaddingToWidthAndHeight(int resultWidth, int maxWidth, int resultHeight, int maxHeight) {
        int addPaddingResultWidth = resultWidth + getPaddingLeft() + getPaddingRight();
        int addPaddingResultHeight = resultHeight + getPaddingTop() + getPaddingBottom();
        if (addPaddingResultWidth > maxWidth) {
            addPaddingResultWidth = maxWidth;
        }
        if (addPaddingResultHeight > maxHeight) {
            addPaddingResultHeight = maxHeight;
        }
        return new int[] { addPaddingResultWidth, addPaddingResultHeight };
    }

    /**
     * 获取子View左上右下边距
     */
    private ChildViewMarginSize getChildViewMargin(View childView) {
        ViewGroup.LayoutParams childViewLayoutParams = childView.getLayoutParams();
        int leftMargin = 0;
        int rightMargin = 0;
        int topMargin = 0;
        int bottomMargin = 0;
        if (childViewLayoutParams instanceof MarginLayoutParams) {
            leftMargin = ((MarginLayoutParams) childViewLayoutParams).leftMargin;
            rightMargin = ((MarginLayoutParams) childViewLayoutParams).rightMargin;
            topMargin = ((MarginLayoutParams) childViewLayoutParams).topMargin;
            bottomMargin = ((MarginLayoutParams) childViewLayoutParams).bottomMargin;
            if (leftMargin < 0) {
                leftMargin = 0;
            }
            if (rightMargin < 0) {
                rightMargin = 0;
            }
            if (topMargin < 0) {
                topMargin = 0;
            }
            if (bottomMargin < 0) {
                bottomMargin = 0;
            }
        }
        return new ChildViewMarginSize(leftMargin, topMargin, rightMargin, bottomMargin);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        switch (mode) {
            case HORIZONTAL:
                layoutHorizontal();
                break;

            case VERTICAL:
                layoutVertical();
                break;
        }
    }

    /**
     * 根据横向布局模式layout子View
     */
    private void layoutHorizontal() {
        int left = 0 + getPaddingLeft();
        int top = 0 + getPaddingTop();
        int lastLine = 0;
        int viewGroupWidth = getMeasuredWidth() - getPaddingRight();
        int viewGroupHeight = getMeasuredHeight() - getPaddingBottom();
        int childCount = getChildCount();
        int maxHeightInThisLine = 0;
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            if (childView.getVisibility() == View.GONE) {
                continue;
            }

            ChildViewMarginSize marginSize = getChildViewMargin(childView);
            int leftMargin = marginSize.getLeftMargin();
            int rightMargin = marginSize.getRightMargin();
            int topMargin = marginSize.getTopMargin();
            int bottomMargin = marginSize.getBottomMargin();

            int right = left + childView.getMeasuredWidth() + leftMargin;
            int bottom = top + childView.getMeasuredHeight() + topMargin;
            if (right > viewGroupWidth) {
                // 不够位置，需要换行
                top += maxHeightInThisLine;
                left = 0;
                maxHeightInThisLine = 0;
                // 换行后继续layout
                right = left + childView.getMeasuredWidth() + leftMargin;
                bottom = top + childView.getMeasuredHeight() + topMargin;
                childView.layout(left + leftMargin, top + topMargin, right, bottom);
                left = right + rightMargin;
                maxHeightInThisLine = Math.max(maxHeightInThisLine, childView.getMeasuredHeight());
                if (top >= viewGroupHeight) {
                    break;
                }
            } else { // 从左到右排列
                childView.layout(left + leftMargin, top + topMargin, right, bottom);
                left = right + rightMargin;
                maxHeightInThisLine = Math.max(maxHeightInThisLine, childView.getMeasuredHeight() + topMargin + bottomMargin);
            }
        }
    }

    /**
     * 根据竖向布局模式layout子View
     */
    private void layoutVertical() {
        int left = 0 + getPaddingLeft();
        int top = 0 + getPaddingTop();
        int lastLine = 0;
        int viewGroupWidth = getMeasuredWidth() - getPaddingRight();
        int viewGroupHeight = getMeasuredHeight() - getPaddingBottom();
        int childCount = getChildCount();
        int maxWidthInThisColumn = 0;
        for (int i = 0; i < childCount; i++) {
            View childView = getChildAt(i);
            if (childView.getVisibility() == View.GONE) {
                continue;
            }

            ChildViewMarginSize marginSize = getChildViewMargin(childView);
            int leftMargin = marginSize.getLeftMargin();
            int rightMargin = marginSize.getRightMargin();
            int topMargin = marginSize.getTopMargin();
            int bottomMargin = marginSize.getBottomMargin();

            int right = left + childView.getMeasuredWidth() + leftMargin;
            int bottom = top + childView.getMeasuredHeight() + topMargin;

            if (bottom > viewGroupHeight) {
                // 不够位置，需要换列
                left += maxWidthInThisColumn;
                top = 0;
                maxWidthInThisColumn = 0;
                // 换列后继续layout
                right = left + childView.getMeasuredWidth() + leftMargin;
                bottom = top + childView.getMeasuredHeight() + topMargin;
                childView.layout(left + leftMargin, top + topMargin, right, bottom);
                top = bottom + bottomMargin;
                maxWidthInThisColumn = Math.max(maxWidthInThisColumn, childView.getMeasuredWidth() + leftMargin + rightMargin);
                if (left >= viewGroupWidth) {
                    break;
                }
            } else { // 从上到下排列
                childView.layout(left + leftMargin, top + topMargin, right, bottom);
                top = bottom + bottomMargin;
                maxWidthInThisColumn = Math.max(maxWidthInThisColumn, childView.getMeasuredWidth() + leftMargin + rightMargin);
            }
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public static class LayoutParams extends MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
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

    static class ChildViewMarginSize {
        int leftMargin = 0;
        int topMargin = 0;
        int rightMargin = 0;
        int bottomMargin = 0;

        public ChildViewMarginSize(int leftMargin, int topMargin, int rightMargin, int bottomMargin) {
            this.leftMargin = leftMargin;
            this.topMargin = topMargin;
            this.rightMargin = rightMargin;
            this.bottomMargin = bottomMargin;
        }

        public int getLeftMargin() {
            return leftMargin;
        }

        public void setLeftMargin(int leftMargin) {
            this.leftMargin = leftMargin;
        }

        public int getRightMargin() {
            return rightMargin;
        }

        public void setRightMargin(int rightMargin) {
            this.rightMargin = rightMargin;
        }

        public int getTopMargin() {
            return topMargin;
        }

        public void setTopMargin(int topMargin) {
            this.topMargin = topMargin;
        }

        public int getBottomMargin() {
            return bottomMargin;
        }

        public void setBottomMargin(int bottomMargin) {
            this.bottomMargin = bottomMargin;
        }
    }

    static class ResultSize {
        int resultWidth = 0;
        int resultHeight = 0;

        public ResultSize(int resultWidth, int resultHeight) {
            this.resultWidth = resultWidth;
            this.resultHeight = resultHeight;
        }

        public int getResultWidth() {
            return resultWidth;
        }

        public void setResultWidth(int resultWidth) {
            this.resultWidth = resultWidth;
        }

        public int getResultHeight() {
            return resultHeight;
        }

        public void setResultHeight(int resultHeight) {
            this.resultHeight = resultHeight;
        }
    }
}
