package com.example.xiezhen.flexdraglayout;

import android.graphics.Rect;
import android.support.v4.util.Pools;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xiezhen on 2017/7/9.
 */
public class FlowDragLayoutManager extends RecyclerView.LayoutManager {
    public static final int UP_TO_DOWN = 1;
    public static final int DOWN_TO_UP = 2;

    private LayoutState mLayoutState;
    private List<View> mRowViews;

    private SparseArray<LineItemPosRecord> preLayoutedViews = new SparseArray<>();
    private Pools.SimplePool<LineItemPosRecord> rectSimplePool;
    private List<View> pendingRecycleView = new ArrayList<>();
    private int maxLineNumbser = Integer.MIN_VALUE;

    public FlowDragLayoutManager() {
        mLayoutState = new LayoutState();
        mRowViews = new ArrayList<>();
    }

    @Override
    public RecyclerView.LayoutParams generateDefaultLayoutParams() {
        return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (state.getItemCount() == 0) {
            removeAndRecycleAllViews(recycler);
            return;
        }
        if (mLayoutState.mHasReset) {
            mLayoutState.mHasReset = false;
        } else {
            initLayoutState();
        }
        detachAndScrapAttachedViews(recycler);
        onLayout(recycler, state);
    }

    private void onLayout(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (mLayoutState.mLayoutOrientation == DOWN_TO_UP) {
            if (mLayoutState.mFirstAnchor + mLayoutState.mScrollDistance <= getPaddingTop()) {
                return;
            }
            layoutReverse(recycler, state);
            checkoutTopOutOfRange(state);
        } else if (mLayoutState.mLayoutOrientation == UP_TO_DOWN) {
            int xLeftOffSet = getPaddingLeft();
            int startPos = mLayoutState.mHasLayoutByScroll ? mLayoutState.mStartPos : 0;
            if (!mLayoutState.mHasLayoutByScroll) {
                willCalculateUnVisibleViews();
            }
            for (int i = startPos; i < state.getItemCount(); i++) {
                final View view = recycler.getViewForPosition(i);
                addView(view);
                measureChildWithMargins(view, 0, 0);

                final int widthSpace = getWidthWithMargins(view);

                if (xLeftOffSet + widthSpace <= getContentHorizontalSpace()) {
                    mRowViews.add(view);
                    xLeftOffSet += widthSpace;
                    if (i == state.getItemCount() - 1) {
                        if (!mLayoutState.mHasLayoutByScroll) {
                            mLayoutState.mHasJustCalculate = i < mLayoutState.mStartPos;
                        }
                        layoutRowView(mRowViews, recycler, true);
                    }
                } else {
                    //已经是下一行了,先布局上一行
                    if (!mLayoutState.mHasLayoutByScroll) {
                        mLayoutState.mHasJustCalculate = i - 1 < mLayoutState.mStartPos;
                    }
                    layoutRowView(mRowViews, recycler, false);
                    //越界检查
                    if (mLayoutState.mFirstAnchor - mLayoutState.mScrollDistance >= getHeight() - getPaddingBottom()) {
                        removeAndRecycleView(view, recycler);
                        break;
                    }
                    xLeftOffSet = getPaddingLeft();
                    mRowViews.add(view);
                    xLeftOffSet += widthSpace;

                    if (i == state.getItemCount() - 1) {
                        if (!mLayoutState.mHasLayoutByScroll) {
                            mLayoutState.mHasJustCalculate = i < mLayoutState.mStartPos;
                        }
                        layoutRowView(mRowViews, recycler, true);
                    }
                }
            }
            if (mLayoutState.mScrollDistance != 0) {
                //最后检查一下底部是否超出了滑动范围
                checkoutBottomOutOfRange(state);
            }
        }
    }

    private void checkoutTopOutOfRange(RecyclerView.State state) {
        final View view = findFirstOrLastView(true);
        if (getPosition(view) == 0) {
            int interval = getPaddingTop() - (getViewTopWithMargin(view) + mLayoutState.mScrollDistance);
            if (interval < 0) {
                mLayoutState.mScrollDistance = Math.abs(getViewTopWithMargin(view) - getPaddingTop());
            }
        }
    }

    private void checkoutBottomOutOfRange(RecyclerView.State state) {
        final View view = findFirstOrLastView(false);
        if (getPosition(view) == state.getItemCount() - 1) {
            int interval = getHeight() - getPaddingBottom() - (getViewBottomWithMargin(view) - mLayoutState.mScrollDistance);
            if (interval > 0) {
                mLayoutState.mScrollDistance = getViewBottomWithMargin(view) - (getHeight() - getPaddingBottom());
            }
        }
    }

    private void initLayoutState() {
        if (getChildCount() != 0) {
            final View view = findFirstOrLastView(true);
            mLayoutState.mFirstVisibleViewTop = getViewTopWithMargin(view);
            mLayoutState.mStartPos = getPosition(view);
        } else {
            mLayoutState.mFirstVisibleViewTop = getPaddingTop();
            mLayoutState.mStartPos = 0;
        }
        mLayoutState.mFirstAnchor = mLayoutState.mFirstVisibleViewTop;
        mLayoutState.mScrollDistance = 0;
        mLayoutState.mLayoutOrientation = UP_TO_DOWN;
        mLayoutState.mHasLayoutByScroll = false;
        mLayoutState.mHasJustCalculate = false;
    }

    @Override
    public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (dy == 0) return 0;
        if (getChildCount() == 0) return 0;
        if (dy > 0) {
            //向上拖动
            final View lastVisibleView = findFirstOrLastView(false);
            if (getPosition(lastVisibleView) == state.getItemCount() - 1) {
                //判断最后一个View是显示情况(完全显示,完全显示且底部有空白,不完全显示)
                int bottomInterval = getHeight() - getPaddingBottom() - getViewBottomWithMargin(lastVisibleView);
                if (bottomInterval == 0) {
                    //正好完全显示
                    return 0;
                } else if (bottomInterval < 0) {
                    //不完全显示
                    dy = Math.min(-bottomInterval, dy);
                } else {
                    //底部还有空白
                    return 0;
                }
            }
        } else {
            //向下拖动
            final View firstView = findFirstOrLastView(true);
            if (getPosition(firstView) == 0) {
                int topInterval = getPaddingTop() - getViewTopWithMargin(firstView);
                if (topInterval == 0) {
                    //第一个View正好完全显示
                    return 0;
                } else if (topInterval > 0) {
                    //第一个View不完全显示
                    dy = Math.max(-topInterval, dy);
                } else {
                    //顶部有空白
                    return 0;
                }
            }
        }

        //准备回收,
        if (dy > 0) {
            mLayoutState.mScrollDistance = Math.min(getViewBottomWithMargin(findFirstOrLastView(false)) - (getHeight() - getPaddingBottom()), dy);
            mLayoutState.mLayoutOrientation = UP_TO_DOWN;
        } else {
            mLayoutState.mScrollDistance = Math.min(Math.abs(getPaddingTop() - getViewTopWithMargin(findFirstOrLastView(true))), -dy);
            mLayoutState.mLayoutOrientation = DOWN_TO_UP;
        }
        recycleUnVisibleViews(recycler, state);

        //准备布局
        mLayoutState.mScrollDistance = Math.abs(dy);
        if (dy > 0) {
            final View last = findFirstOrLastView(false);
            mLayoutState.mFirstAnchor = getViewBottomWithMargin(last);
            mLayoutState.mStartPos = getPosition(last) + 1;
        } else {
            final View first = findFirstOrLastView(true);
            mLayoutState.mFirstAnchor = getViewTopWithMargin(first);
            mLayoutState.mStartPos = getPosition(first) - 1;
        }
        mLayoutState.mHasLayoutByScroll = true;

        onLayout(recycler, state);
        dy = dy > 0 ? mLayoutState.mScrollDistance : -mLayoutState.mScrollDistance;
        offsetChildrenVertical(-dy);
        return dy;
    }

    protected int getViewTopWithMargin(final View view) {
        final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
        return getDecoratedTop(view) - lp.topMargin;
    }

    protected View findFirstOrLastView(boolean isFirst) {
        return getChildAt(isFirst ? 0 : getChildCount() - 1);
    }

    protected int getViewBottomWithMargin(View view) {
        final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
        return getDecoratedBottom(view) + lp.bottomMargin;
    }

    protected int getContentHorizontalSpace() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    protected int getWidthWithMargins(View view) {
        final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
        return getDecoratedMeasuredWidth(view) + lp.leftMargin + lp.rightMargin;
    }

    protected int getHeightWithMargins(View view) {
        final RecyclerView.LayoutParams lp = (RecyclerView.LayoutParams) view.getLayoutParams();
        return getDecoratedMeasuredHeight(view) + lp.topMargin + lp.bottomMargin;
    }

    protected LayoutState getLayoutState() {
        return mLayoutState;
    }


    /**
     * 记录和布局相关的全局信息
     */
    protected final static class LayoutState {
        int mFirstAnchor;
        int mScrollDistance;
        int mStartPos;
        int mFirstVisibleViewTop;
        int mLayoutOrientation;
        boolean mHasReset = false;
        boolean mHasLayoutByScroll = false;
        boolean mHasJustCalculate = false;
    }


    @Override
    public void onItemsMoved(RecyclerView recyclerView, int from, int to, int itemCount) {
        mLayoutState.mHasReset = true;
        initLayoutState();
    }

    @Override
    public void onItemsChanged(RecyclerView recyclerView) {
        mLayoutState.mHasReset = true;
        initLayoutState();
    }

    public void layoutRowView(List<View> views, RecyclerView.Recycler recycler, boolean isLastRow) {
        alignLayout(views, isLastRow, recycler);
        if (getLayoutState().mHasLayoutByScroll
                || (!getLayoutState().mHasLayoutByScroll && !getLayoutState().mHasJustCalculate)) {
            final View last = views.get(views.size() - 1);
            getLayoutState().mFirstAnchor = getViewBottomWithMargin(last);
        }

        if (views.size() > maxLineNumbser) {
            maxLineNumbser = views.size();
            recycler.setViewCacheSize(maxLineNumbser);
        }
        views.clear();
    }

    private void alignLayout(List<View> views, boolean isLastRow, RecyclerView.Recycler recycler) {
        //计算行内间距
        int interval = 0;
        if (views.size() > 1 && !isLastRow) {
            int totalWidth = 0;
            for (View view : views) {
                totalWidth += getWidthWithMargins(view);
            }
            int rest = getContentHorizontalSpace() - totalWidth;
            interval = rest / (views.size() - 1);
        }

        //开始布局
        int xOffset = getPaddingLeft();
        int heightSpace = 0;
        for (int i = 0; i < views.size(); i++) {
            final View view = views.get(i);
            final int widthSpace = getWidthWithMargins(view);
            heightSpace = getHeightWithMargins(view);

            int l = xOffset;
            int t = getLayoutState().mFirstAnchor;
            int r = l + widthSpace;
            int b = t + heightSpace;

            realLayoutItem(l, t, r, b, view, recycler, i == 0);

            xOffset = r + interval;
        }
    }

    public void layoutReverse(RecyclerView.Recycler recycler, RecyclerView.State state) {
        final LayoutState layoutState = getLayoutState();
        for (int i = layoutState.mStartPos; i >= 0; i--) {
            LineItemPosRecord lineItemPosRecord = preLayoutedViews.get(i);
            Rect rect = lineItemPosRecord.rect;
            int heightSpace = rect.bottom - rect.top;

            if (layoutState.mFirstAnchor + layoutState.mScrollDistance <= getPaddingTop()) {
                break;
            }

            final View view = recycler.getViewForPosition(i);
            addView(view, 0);
            measureChildWithMargins(view, 0, 0);
            int l = rect.left, t = layoutState.mFirstAnchor - heightSpace, r = rect.right, b = layoutState.mFirstAnchor;
            layoutDecoratedWithMargins(view, l, t, r, b);

            if (lineItemPosRecord.isFirstItemInLine) {
                layoutState.mFirstAnchor -= heightSpace;
            }

            releaseItemLayoutInfo(lineItemPosRecord);
            preLayoutedViews.remove(i);
        }
    }

    private void realLayoutItem(int l, int t, int r, int b, View view, RecyclerView.Recycler recycler, boolean isFirstItemInARow) {
        final LayoutState layoutState = getLayoutState();
        if (layoutState.mHasLayoutByScroll) {
            layoutDecoratedWithMargins(view, l, t, r, b);
        } else {
            if (layoutState.mHasJustCalculate) {
                LineItemPosRecord lineItemPosRecord = generateALineItem();
                lineItemPosRecord.setFirstItemInLine(isFirstItemInARow);
                lineItemPosRecord.rect.set(l, t, r, b);
                preLayoutedViews.put(getPosition(view), lineItemPosRecord);
                removeAndRecycleView(view, recycler);
            } else {
                layoutDecoratedWithMargins(view, l, t, r, b);
            }
        }
    }

    /**
     * 回收Rect对象再利用
     */
    private void releaseItemLayoutInfo(LineItemPosRecord rect) {
        try {
            rectSimplePool.release(rect);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void recycleUnVisibleViews(RecyclerView.Recycler recycler, RecyclerView.State state) {
        if (getChildCount() == 0) return;
        final LayoutState layoutState = getLayoutState();
        if (layoutState.mScrollDistance < 0) {
            return;
        }
        int top = Integer.MAX_VALUE;
        if (layoutState.mLayoutOrientation == FlowDragLayoutManager.DOWN_TO_UP) {
            //回收底部不可见的View
            for (int i = getChildCount() - 1; i >= 0; i--) {
                final View view = getChildAt(i);
                int afterScrollTop = getViewTopWithMargin(view) + layoutState.mScrollDistance;
                if (afterScrollTop >= getHeight() - getPaddingBottom()) {
                    pendingRecycleView.add(view);
                } else {
                    break;
                }
            }
        } else if (layoutState.mLayoutOrientation == FlowDragLayoutManager.UP_TO_DOWN) {
            //回收顶部不可见的View
            for (int i = 0; i < getChildCount(); i++) {
                final View view = getChildAt(i);
                int afterScrollBottom = getViewBottomWithMargin(view) - layoutState.mScrollDistance;
                if (afterScrollBottom <= getPaddingTop()) {
                    final int viewTop = getViewTopWithMargin(view);
                    if (viewTop != top) {
                        saveLayoutInfo(view, true);
                        top = viewTop;
                    } else {
                        saveLayoutInfo(view, false);
                    }

                    pendingRecycleView.add(view);
                } else {
                    break;
                }
            }
        }

        for (View view : pendingRecycleView) {
            removeAndRecycleView(view, recycler);
        }

        pendingRecycleView.clear();
    }

    public void willCalculateUnVisibleViews() {
        for (int i = 0; i < preLayoutedViews.size(); i++) {
            LineItemPosRecord record = preLayoutedViews.get(i, null);
            if (record != null) {
                releaseItemLayoutInfo(record);
            }
        }
        preLayoutedViews.clear();
    }

    private void saveLayoutInfo(View view, boolean isFirstItemInLine) {
        LineItemPosRecord out = generateALineItem();
        out.setFirstItemInLine(isFirstItemInLine);
        getDecoratedBoundsWithMargins(view, out.rect);
        preLayoutedViews.put(getPosition(view), out);
    }

    private LineItemPosRecord generateALineItem() {
        if (rectSimplePool == null) {
            rectSimplePool = new Pools.SimplePool<>(getChildCount());
        }
        LineItemPosRecord out = rectSimplePool.acquire();
        if (out == null) {
            out = new LineItemPosRecord();
        } else {
        }
        return out;
    }

    private static final class LineItemPosRecord {
        Rect rect = new Rect();
        boolean isFirstItemInLine;

        void setFirstItemInLine(boolean firstItemInLine) {
            isFirstItemInLine = firstItemInLine;
        }
    }

    @Override
    public boolean canScrollVertically() {
        return true;
    }
}
