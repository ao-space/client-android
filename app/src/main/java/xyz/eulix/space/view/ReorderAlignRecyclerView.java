/*
 * Copyright (c) 2022 Institute of Software, Chinese Academy of Sciences (ISCAS)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xyz.eulix.space.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

/**
 * @author: chenjiawei
 * date: 2021/7/15 15:21
 */
public class ReorderAlignRecyclerView extends RecyclerView {

    public ReorderAlignRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public ReorderAlignRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ReorderAlignRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setChildrenDrawingOrderEnabled(true);
    }

    public LayoutManager getCustomizeLayoutManager() {
        RecyclerView.LayoutManager layoutManager = getLayoutManager();
        if (layoutManager instanceof LayoutManager) {
            return (LayoutManager) layoutManager;
        } else {
            return null;
        }
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        LayoutManager layoutManager = getCustomizeLayoutManager();
        if (layoutManager != null) {
            int center = layoutManager.getCenterPosition() - layoutManager.getFirstVisiblePosition();
            int order = i;
            if (i == center) {
                order = childCount - 1;
            } else if (i > center) {
                order = center + childCount - 1 - i;
            }
            return order;
        } else {
            return super.getChildDrawingOrder(childCount, i);
        }
    }

    public static class LayoutManager extends RecyclerView.LayoutManager {
        private int mSumDx = 0;
        private int mItemWidth, mItemHeight;
        private int mIntervalWidth;
        private int mStartX;
        private SparseArray<Rect> mItemRects = new SparseArray<>();
        private SparseBooleanArray mHasAttachedItems = new SparseBooleanArray();

        private int getHorizontalSpace() {
            return getWidth() - getPaddingEnd() - getPaddingStart();
        }

        private Rect getHorizontalVisibleArea() {
            return new Rect(getPaddingLeft() + mSumDx, getPaddingTop()
                    , getHorizontalSpace() + mSumDx, getHeight() + getPaddingBottom());
        }

        private void handleChildView(View childView, int dx) {
            float ratio = computeScale(dx);
            childView.setScaleX(ratio);
            childView.setScaleY(ratio);
        }

        private float computeScale(int x) {
            return Math.max(Math.min((1- Math.abs(x * 1.0f / (307.0f / 102 * getIntervalWidth()))), 1), 0);
        }

        private int getMaxOffset() {
            return (getItemCount() - 1) * getIntervalWidth();
        }

        private int getCenterPosition() {
            int position = mSumDx / getIntervalWidth();
            int more = mSumDx % getIntervalWidth();
            if (more > getIntervalWidth() * 0.5f) {
                position++;
            }
            return position;
        }

        private int getFirstVisiblePosition() {
            if (getChildCount() <= 0) {
                return 0;
            }
            View view = getChildAt(0);
            if (view == null) {
                return 0;
            }
            return getPosition(view);
        }

        private void insertHorizontalView(int position, Rect visibleRect, RecyclerView.Recycler recycler, boolean isFirstPosition) {
            Rect rect = mItemRects.get(position);
            if (Rect.intersects(visibleRect, rect) && !mHasAttachedItems.get(position)) {
                View childView = recycler.getViewForPosition(position);
                if (isFirstPosition) {
                    addView(childView, 0);
                } else {
                    addView(childView);
                }
                measureChildWithMargins(childView, 0, 0);
                layoutDecorated(childView, rect.left - mSumDx, rect.top, rect.right - mSumDx, rect.bottom);
                handleChildView(childView, rect.left - mStartX - mSumDx);
                mHasAttachedItems.put(position, true);
            }
        }

        @Override
        public RecyclerView.LayoutParams generateDefaultLayoutParams() {
            return new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        @Override
        public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
            int itemCount = getItemCount();
            if (itemCount <= 0) {
                detachAndScrapAttachedViews(recycler);
                return;
            }
            mHasAttachedItems.clear();
            mItemRects.clear();
            detachAndScrapAttachedViews(recycler);
            View childView = recycler.getViewForPosition(0);
            measureChildWithMargins(childView, 0, 0);
            mItemWidth = getDecoratedMeasuredWidth(childView);
            mItemHeight = getDecoratedMeasuredHeight(childView);
            mIntervalWidth = getIntervalWidth();
            mStartX = (getWidth() - mItemWidth) / 2;
            int offsetX = 0;
            for (int i = 0; i < itemCount; i++) {
                Rect rect = new Rect(mStartX + offsetX, 0, mStartX + offsetX + mItemWidth, mItemHeight);
                mItemRects.put(i, rect);
                mHasAttachedItems.put(i, false);
                offsetX += mIntervalWidth;
            }
            int visibleCount = (int) Math.round(Math.ceil(getHorizontalSpace() * 1.0 / mIntervalWidth));
            for (int i = 0; i < visibleCount; i++) {
                insertHorizontalView(i, getHorizontalVisibleArea(), recycler, false);
            }
        }

        @Override
        public boolean canScrollHorizontally() {
            return true;
        }

        @Override
        public int scrollHorizontallyBy(int dx, RecyclerView.Recycler recycler, RecyclerView.State state) {
            if (getChildCount() <= 0) {
                return dx;
            }
            int travel = dx;
            if (mSumDx + dx < 0) {
                travel = -mSumDx;
            } else if (mSumDx + dx > getMaxOffset()) {
                travel = getMaxOffset() - mSumDx;
            }
            mSumDx += travel;
            Rect visibleRect = getHorizontalVisibleArea();
            for (int i = getChildCount() - 1; i >= 0; i--) {
                View childView = getChildAt(i);
                if (childView != null) {
                    int position = getPosition(childView);
                    Rect rect = mItemRects.get(position);
                    if (Rect.intersects(visibleRect, rect)) {
                        layoutDecoratedWithMargins(childView, rect.left - mSumDx, rect.top, rect.right - mSumDx, rect.bottom);
                        handleChildView(childView, rect.left - mStartX - mSumDx);
                        mHasAttachedItems.put(position, true);
                    } else {
                        removeAndRecycleView(childView, recycler);
                        mHasAttachedItems.put(position, false);
                    }
                }
            }
            View lastView = getChildAt(getChildCount() - 1);
            View firstView = getChildAt(0);
            if (travel >= 0) {
                if (lastView != null) {
                    int minPosition = getPosition(lastView) + 1;
                    int itemCount = getItemCount();
                    for (int i = minPosition; i < itemCount; i++) {
                        insertHorizontalView(i, visibleRect, recycler, false);
                    }
                }
            } else {
                if (firstView != null) {
                    int maxPosition = getPosition(firstView) - 1;
                    for (int i = maxPosition; i >= 0; i--) {
                        insertHorizontalView(i, visibleRect, recycler, true);
                    }
                }
            }
            return travel;
        }

        public int getIntervalWidth() {
            return Math.round((256 * mItemWidth + 10 * mItemHeight) / 307.f);
        }
    }
}
