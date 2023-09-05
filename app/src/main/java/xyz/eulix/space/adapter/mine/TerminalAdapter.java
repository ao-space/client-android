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

package xyz.eulix.space.adapter.mine;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import xyz.eulix.space.R;
import xyz.eulix.space.bean.EulixTerminal;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.ViewUtils;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/4/22 9:52
 */
public class TerminalAdapter extends RecyclerView.Adapter<TerminalAdapter.ViewHolder> implements View.OnClickListener {
    private static final String TAG = TerminalAdapter.class.getSimpleName();
    private Context mContext;
    private List<EulixTerminal> mEulixTerminalList;
    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public TerminalAdapter(@NonNull Context context, List<EulixTerminal> eulixTerminalList) {
        mContext = context;
        mEulixTerminalList = eulixTerminalList;
    }

    private void generateViewHolderData(ViewHolder holder, int position) {
        if (mEulixTerminalList != null && position >= 0 && mEulixTerminalList.size() > position) {
            EulixTerminal eulixTerminal = mEulixTerminalList.get(position);
            if (eulixTerminal != null) {
                holder.terminalName.setText(StringUtil.nullToEmpty(eulixTerminal.getTerminalName()));
                StringBuilder typePlaceBuilder = new StringBuilder();
                String type = mContext.getString(R.string.unknown_terminal);
                @DrawableRes int terminalResId = R.drawable.unknown_terminal_2x;
                String terminalType = eulixTerminal.getTerminalType();
                if (terminalType != null) {
                    switch (terminalType.toLowerCase()) {
                        case "android":
                            type = mContext.getString(R.string.android_client);
                            terminalResId = R.drawable.android_terminal_2x;
                            break;
                        case "ios":
                            type = mContext.getString(R.string.ios_client);
                            terminalResId = R.drawable.ios_terminal_2x;
                            break;
                        case "web":
                            type = mContext.getString(R.string.web_browser);
                            terminalResId = R.drawable.browser_terminal_2x;
                            break;
                        default:
                            break;
                    }
                }
                typePlaceBuilder.append(StringUtil.nullToEmpty(type));
                String place = eulixTerminal.getTerminalPlace();
                if (place != null && !TextUtils.isEmpty(place)) {
                    String[] places = place.split("\\|");
                    String nPlace = null;
                    if (places.length >= 4) {
                        nPlace = places[3];
                        if (nPlace == null || TextUtils.isEmpty(nPlace) || nPlace.equals("0")) {
                            nPlace = places[1];
                        }
                        if (nPlace == null || TextUtils.isEmpty(nPlace) || nPlace.equals("0")) {
                            nPlace = places[0];
                        }
                        if (nPlace == null || TextUtils.isEmpty(nPlace) || nPlace.equals("0")) {
                            nPlace = null;
                        }
                    }
                    if (nPlace != null && !TextUtils.isEmpty(nPlace)) {
                        typePlaceBuilder.append("·");
                        typePlaceBuilder.append(nPlace);
                    }
                }
                holder.terminalTypePlace.setText(typePlaceBuilder.toString());
                holder.terminalImage.setImageResource(terminalResId);
                String timeLogin = FormatUtil.formatTime(eulixTerminal.getTerminalTimestamp(), ConstantField.TimeStampFormat.FILE_API_MINUTE_FORMAT)
                        + " " + mContext.getString(R.string.login);
                holder.terminalTime.setText(timeLogin);
                if (eulixTerminal.isMyself() || eulixTerminal.isGranter()) {
                    holder.terminalHintText.setVisibility(View.VISIBLE);
                    holder.terminalHintText.setText(eulixTerminal.isMyself() ? R.string.current_device : R.string.bind_device);
                    int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
                    holder.terminalHintText.measure(measureSpec, measureSpec);
                    holder.terminalName.setMaxWidth(Math.max((ViewUtils.getScreenWidth(mContext)
                            - mContext.getResources().getDimensionPixelSize(R.dimen.dp_181)
                            - holder.terminalHintText.getMeasuredWidth()), 0));
                    holder.terminalGoOffline.setVisibility(View.GONE);
                } else {
//                    holder.terminalHint.setVisibility(View.GONE);
                    holder.terminalHintText.setVisibility(View.GONE);
                    holder.terminalName.setMaxWidth(Math.max((ViewUtils.getScreenWidth(mContext)
                            - mContext.getResources().getDimensionPixelSize(R.dimen.dp_175)), 0));
                    holder.terminalGoOffline.setVisibility(View.VISIBLE);
                    holder.terminalGoOffline.setTag(position);
                    holder.terminalGoOffline.setOnClickListener(this);
                }
            }
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public void updateData(List<EulixTerminal> eulixTerminals) {
        if (eulixTerminals == null) {
            eulixTerminals = new ArrayList<>();
        }
        mEulixTerminalList = eulixTerminals;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.terminal_list_item, parent, false), mContext);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.itemView.setTag(position);
        generateViewHolderData(holder, position);
    }

    @Override
    public int getItemCount() {
        return (mEulixTerminalList == null ? 0 : mEulixTerminalList.size());
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            Object positionTag = v.getTag();
            if (positionTag instanceof Integer) {
                int position = (int) positionTag;
                if (mOnItemClickListener != null) {
                    mOnItemClickListener.onItemClick(v, position);
                }
            }
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView terminalImage;
        private TextView terminalName;
        private ImageView terminalHint;
        private TextView terminalHintText;
        private TextView terminalTypePlace;
        private TextView terminalTime;
        private Button terminalGoOffline;

        public ViewHolder(@NonNull View itemView, Context context) {
            super(itemView);
            terminalImage = itemView.findViewById(R.id.terminal_image);
            terminalName = itemView.findViewById(R.id.terminal_name);
            terminalHint = itemView.findViewById(R.id.terminal_hint);
            terminalHintText = itemView.findViewById(R.id.terminal_hint_text);
            terminalTypePlace = itemView.findViewById(R.id.terminal_type_place);
            terminalTime = itemView.findViewById(R.id.terminal_time);
            terminalGoOffline = itemView.findViewById(R.id.terminal_go_offline);
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
