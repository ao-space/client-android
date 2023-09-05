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

import androidx.collection.SparseArrayCompat;

/**
 * Author:      Zhu Fuyu
 * Description: 主页导航栏管理工具
 * History:     2021/7/16
 */
public class TabImageViewHolder {
    private SparseArrayCompat<TabImageView> mViews = new SparseArrayCompat<>();
    private TabImageView lastCheckedView;
    private OnTabChangedListener mListener;

    public TabImageViewHolder addTabImageView(TabImageView view,
                                               int selectImgId, int normalImgId, String tabName) {
        view.initRes(selectImgId, normalImgId, tabName);
        mViews.put(view.getId(), view);
        view.setOnClickListener(v -> select(view));
        return this;
    }

    public void setSelected(int viewId) {
        int size = mViews.size();
        if (size == 0) {
            return;
        }
        TabImageView view;
        for (int i = 0; i < size; i++) {
            view = mViews.valueAt(i);
            if (view != null && view.getId() == viewId) {
                select(view);
                break;
            }
        }
    }

    public int getSelectedViewId() {
        if (lastCheckedView != null) {
            return lastCheckedView.getId();
        } else {
            return -1;
        }
    }

    public void setOnTabSelectedListener(OnTabChangedListener listener) {
        this.mListener = listener;
    }

    public void clear() {
        mViews.clear();
        lastCheckedView = null;
    }

    private void select(TabImageView view) {
        if (lastCheckedView == view) {
            return;
        }
        if (lastCheckedView != null) {
            lastCheckedView.setChecked(false);
        }
        view.setChecked(true);
        lastCheckedView = view;
        if (mListener != null) {
            mListener.onSelected(view.getId());
        }
    }

    public interface OnTabChangedListener {
        void onSelected(int viewId);
    }
}
