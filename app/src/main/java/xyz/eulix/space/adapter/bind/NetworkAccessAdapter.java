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
import xyz.eulix.space.bean.NetworkAccessBean;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StringUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/12/5 18:13
 */
public class NetworkAccessAdapter extends RecyclerView.Adapter<NetworkAccessAdapter.ViewHolder> implements View.OnClickListener {
    private Context mContext;
    private List<NetworkAccessBean> mNetworkAccessBeanList;
    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onNetworkAccessItemClick(View view, int position);
    }

    public NetworkAccessAdapter(Context context, List<NetworkAccessBean> networkAccessBeans) {
        mContext = context;
        mNetworkAccessBeanList = networkAccessBeans;
    }

    private void generateViewHolderData(ViewHolder holder, int position) {
        if (mNetworkAccessBeanList != null && position >= 0 && mNetworkAccessBeanList.size() > position) {
            NetworkAccessBean networkAccessBean = mNetworkAccessBeanList.get(position);
            if (networkAccessBean != null) {
                boolean isConnect = networkAccessBean.isConnect();
                boolean isShowDetail = networkAccessBean.isShowDetail();
                holder.networkName.setText(StringUtil.nullToEmpty(networkAccessBean.getNetworkName()));
                holder.networkConnectState.setText(isConnect ? R.string.connected : R.string.no_internet_connection);
                holder.networkIndicator.setImageResource(networkAccessBean.isWired() ? R.drawable.ethernet_2x : R.drawable.wifi_4_encrypt_2x);
                if (isConnect) {
                    holder.connectIndicator.setImageDrawable(null);
                } else {
                    holder.connectIndicator.setImageResource(R.drawable.icon_disconnect_2x);
                }
                holder.detailIndicator.setVisibility(isShowDetail ? View.VISIBLE : View.GONE);
                if (isShowDetail) {
                    holder.detailIndicator.setImageResource(R.drawable.icon_detail_2x);
                    holder.networkDetailContainer.setClickable(true);
                    holder.networkDetailContainer.setTag(position);
                    holder.networkDetailContainer.setOnClickListener(this);
                } else {
                    holder.networkDetailContainer.setClickable(false);
                    holder.networkDetailContainer.setOnClickListener(null);
                }
            }
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public void updateData(List<NetworkAccessBean> networkAccessBeans) {
        if (networkAccessBeans == null) {
            networkAccessBeans = new ArrayList<>();
        }
        mNetworkAccessBeanList = networkAccessBeans;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_network_access, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        generateViewHolderData(holder, position);
    }

    @Override
    public int getItemCount() {
        return (mNetworkAccessBeanList == null ? 0 : mNetworkAccessBeanList.size());
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            Object positionTag = v.getTag();
            switch (v.getId()) {
                case R.id.network_detail_container:
                    if (positionTag instanceof Integer) {
                        int position = (int) positionTag;
                        if (mOnItemClickListener != null) {
                            mOnItemClickListener.onNetworkAccessItemClick(v, position);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView networkName;
        private TextView networkConnectState;
        private LinearLayout networkDetailContainer;
        private ImageView networkIndicator;
        private ImageView connectIndicator;
        private ImageView detailIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            networkName = itemView.findViewById(R.id.network_name);
            networkConnectState = itemView.findViewById(R.id.network_connect_state);
            networkDetailContainer = itemView.findViewById(R.id.network_detail_container);
            networkIndicator = itemView.findViewById(R.id.network_indicator);
            connectIndicator = itemView.findViewById(R.id.connect_indicator);
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
