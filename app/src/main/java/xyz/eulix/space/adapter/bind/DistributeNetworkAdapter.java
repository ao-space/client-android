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

package xyz.eulix.space.adapter.bind;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import xyz.eulix.space.R;
import xyz.eulix.space.bean.WLANItem;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/10/28 14:44
 */
public class DistributeNetworkAdapter extends RecyclerView.Adapter<DistributeNetworkAdapter.ViewHolder> implements View.OnClickListener {
    private Context mContext;
    private List<WLANItem> mWlanItemList;
    private OnItemClickListener mOnItemClickListener;
    private boolean isShowDetail;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
        void onDetailClick(View view, int position);
    }

    public DistributeNetworkAdapter(Context context, List<WLANItem> wlanItemList) {
        mContext = context;
        mWlanItemList = wlanItemList;
    }

    private void generateViewHolderData(ViewHolder holder, int position) {
        if (mWlanItemList != null && position >= 0 && mWlanItemList.size() > position) {
            WLANItem wlanItem = mWlanItemList.get(position);
            if (wlanItem != null) {
                holder.wlanSsid.setText(wlanItem.getWlanSsid());
                holder.wlanIndicator.setImageResource(R.drawable.wifi_4_encrypt_2x);
                holder.itemView.setTag(position);
                holder.itemView.setOnClickListener(this);
                if (isShowDetail) {
                    holder.detailIndicator.setVisibility(View.VISIBLE);
                    holder.detailIndicator.setImageResource(R.drawable.icon_detail_2x);
                    holder.networkDetailContainer.setClickable(true);
                    holder.networkDetailContainer.setTag(position);
                    holder.networkDetailContainer.setOnClickListener(this);
                } else {
                    holder.detailIndicator.setVisibility(View.GONE);
                    holder.networkDetailContainer.setClickable(false);
                    holder.networkDetailContainer.setOnClickListener(null);
                }
            }
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public void updateData(List<WLANItem> wlanItemList, boolean isShowDetail) {
        this.isShowDetail = isShowDetail;
        if (wlanItemList == null) {
            wlanItemList = new ArrayList<>();
        }
        mWlanItemList = wlanItemList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.distribute_wlan_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        generateViewHolderData(holder, position);
    }

    @Override
    public int getItemCount() {
        return (mWlanItemList == null ? 0 : mWlanItemList.size());
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        Object positionTag = holder.itemView.getTag();
        if (positionTag instanceof Integer) {
            int position = (int) positionTag;
            generateViewHolderData(holder, position);
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            Object positionTag = v.getTag();
            if (positionTag instanceof Integer) {
                int position = (int) positionTag;
                if (mOnItemClickListener != null) {
                    switch (v.getId()) {
                        case R.id.network_detail_container:
                            mOnItemClickListener.onDetailClick(v, position);
                            break;
                        default:
                            mOnItemClickListener.onItemClick(v, position);
                            break;
                    }
                }
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView wlanSsid;
        private LinearLayout networkDetailContainer;
        private ImageView wlanIndicator;
        private ImageView detailIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            wlanSsid = itemView.findViewById(R.id.wlan_ssid);
            networkDetailContainer = itemView.findViewById(R.id.network_detail_container);
            wlanIndicator = itemView.findViewById(R.id.wlan_indicator);
            detailIndicator = itemView.findViewById(R.id.detail_indicator);
        }
    }

    public static class ItemDecoration extends RecyclerView.ItemDecoration {
        private Paint paint;
        private int orientation;
        private int dividerWidth;

        public ItemDecoration(int orientation, int dividerWidth, @ColorInt int dividerColor) {
            this.orientation = orientation;
            this.dividerWidth = dividerWidth;
            paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(dividerColor);
            paint.setStyle(Paint.Style.FILL);
        }

        private void drawHorizontal(Canvas canvas, RecyclerView parent) {
            final int top = parent.getPaddingTop();
            final int bottom = parent.getMeasuredHeight() - parent.getPaddingBottom();
            final int childSize = parent.getChildCount();
            for (int i = 0; i < (childSize - 1); i++) {
                final View child = parent.getChildAt(i);
                RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) child.getLayoutParams();
                final int left = child.getRight() + layoutParams.rightMargin;
                final int right = left + dividerWidth;
                if (paint != null) {
                    canvas.drawRect(left, top, right, bottom, paint);
                }
            }
        }

        private void drawVertical(Canvas canvas, RecyclerView parent) {
            final int left = parent.getPaddingLeft();
            final int right = parent.getMeasuredWidth() - parent.getPaddingRight();
            final int childSize = parent.getChildCount();
            for (int i = 0; i < (childSize - 1); i++) {
                final View child = parent.getChildAt(i);
                RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) child.getLayoutParams();
                final int top = child.getBottom() + layoutParams.bottomMargin;
                final int bottom = top + dividerWidth;
                if (paint != null) {
                    canvas.drawRect(left, top, right, bottom, paint);
                }
            }
        }

        @Override
        public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            super.onDraw(c, parent, state);
            if (orientation == RecyclerView.HORIZONTAL) {
                drawHorizontal(c, parent);
            } else {
                drawVertical(c, parent);
            }
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            super.getItemOffsets(outRect, view, parent, state);
            if (orientation == RecyclerView.HORIZONTAL) {
                outRect.set(0, 0, dividerWidth, 0);
            } else {
                outRect.set(0, 0, 0, dividerWidth);
            }
        }
    }
}
