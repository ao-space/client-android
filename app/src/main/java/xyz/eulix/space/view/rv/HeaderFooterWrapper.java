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

package xyz.eulix.space.view.rv;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.collection.SparseArrayCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Author:      Zhu Fuyu
 * Description: 带Header和Footer的RecyclerView.Adapter
 * History:     2021/9/29
 */
public class HeaderFooterWrapper extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public RecyclerView.Adapter<RecyclerView.ViewHolder> dataAdapter;
    private SparseArrayCompat<View> mHeaders = new SparseArrayCompat<>();
    private SparseArrayCompat<View> mFooters = new SparseArrayCompat<>();
    private ILoadMore iLoadMore;

    private static final int HEADER_VIEW_TYPE = 10000;
    private static final int FOOTER_VIEW_TYPE = 20000;

    public HeaderFooterWrapper(RecyclerView.Adapter<RecyclerView.ViewHolder> dataAdapter) {
        this.dataAdapter = dataAdapter;
    }

    @NonNull
    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View value = mHeaders.get(viewType);
        if (value != null) {
            return new HeaderFooterViewHolder(value);
        }

        value = mFooters.get(viewType);
        if (value != null) {
            return new HeaderFooterViewHolder(value);
        }

        return dataAdapter.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull RecyclerView.ViewHolder holder, int position) {
        if (isHeaderPosition(position)) {
            return;
        }
        if (isFooterPosition(position)) {
            if (iLoadMore != null) {
                iLoadMore.loadMore();
            }
            return;
        }
        dataAdapter.onBindViewHolder(holder, position - getHeaderCount());
    }

    //局部更新
    @Override
    public void onBindViewHolder(@NonNull @NotNull RecyclerView.ViewHolder holder, int position, @NonNull @NotNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        } else {
            dataAdapter.onBindViewHolder(holder, position - getHeaderCount(), payloads);
        }
    }

    @Override
    public int getItemCount() {
        return dataAdapter.getItemCount() + getHeaderCount() + getFooterCount();
    }

    @Override
    public int getItemViewType(int position) {
        if (isHeaderPosition(position)) {
            return mHeaders.keyAt(position);
        }
        if (isFooterPosition(position)) {
            return mFooters.keyAt(position - getHeaderCount() - dataAdapter.getItemCount());
        }
        return dataAdapter.getItemViewType(position - getHeaderCount());
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        RecyclerView.LayoutManager manager = recyclerView.getLayoutManager();
        if (manager instanceof GridLayoutManager) {
            // 布局是GridLayoutManager所管理
            final GridLayoutManager gridLayoutManager = (GridLayoutManager) manager;
            gridLayoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
                @Override
                public int getSpanSize(int position) {
                    // 如果是Header、Footer的对象则占据spanCount的位置，否则就只占用1个位置
                    return (isHeaderPosition(position) || isFooterPosition(position)) ? gridLayoutManager.getSpanCount() : 1;
                }
            });
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        dataAdapter.onViewAttachedToWindow(holder);
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        dataAdapter.onViewDetachedFromWindow(holder);
    }

    public int getHeaderCount() {
        return mHeaders.size();
    }

    private int getFooterCount() {
        return mFooters.size();
    }

    private boolean isHeaderPosition(int position) {
        return position < getHeaderCount();
    }

    private boolean isFooterPosition(int position) {
        if (mFooters.size() == 0) {
            return false;
        }
        return position >= getFooterCount() + dataAdapter.getItemCount() - 1;
    }

    public void addHeaderView(View view) {
        mHeaders.put(mHeaders.size() + HEADER_VIEW_TYPE, view);
    }

    public void addFooterView(View view, ILoadMore loadMore) {
        mFooters.put(mFooters.size() + FOOTER_VIEW_TYPE, view);
        iLoadMore = loadMore;
    }

    public void removeAllFooters() {
        mFooters.clear();
    }

    public int getFooterViewSize() {
        int size = 0;
        if (mFooters != null) {
            size = mFooters.size();
        }
        return size;
    }

    public interface ILoadMore {
        void loadMore();
    }

    private class HeaderFooterViewHolder extends RecyclerView.ViewHolder {

        public HeaderFooterViewHolder(View itemView) {
            super(itemView);
        }
    }
}
