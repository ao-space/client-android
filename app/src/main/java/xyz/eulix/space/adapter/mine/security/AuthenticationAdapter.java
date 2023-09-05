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

package xyz.eulix.space.adapter.mine.security;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import xyz.eulix.space.R;
import xyz.eulix.space.util.ConstantField;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/7/18 17:19
 */
public class AuthenticationAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements View.OnClickListener {
    private Context mContext;
    private List<Integer> mAuthenticationList;
    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public AuthenticationAdapter(@NonNull Context context, List<Integer> authenticationList) {
        mContext = context;
        mAuthenticationList = authenticationList;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    private void generateViewHolderData(ViewHolder holder, int position) {
        if (position >= 0 && mAuthenticationList != null && mAuthenticationList.size() > position) {
            holder.itemView.setOnClickListener(this);
            int size = mAuthenticationList.size();
            if (size <= 1) {
                holder.mode.setText(R.string.mode_0);
            } else {
                switch (position) {
                    case 0:
                        holder.mode.setText(R.string.mode_1);
                        break;
                    case 1:
                        holder.mode.setText(R.string.mode_2);
                        break;
                    case 2:
                        holder.mode.setText(R.string.mode_3);
                        break;
                    default:
                        break;
                }
            }
            int authentication = mAuthenticationList.get(position);
            switch (authentication) {
                case ConstantField.AuthenticationFunction.HARDWARE_DEVICE:
                    holder.image.setImageResource(R.drawable.hardware_device_2x);
                    holder.name.setText(R.string.hardware_device_verify);
                    break;
                case ConstantField.AuthenticationFunction.SECURITY_PASSWORD:
                    holder.image.setImageResource(R.drawable.security_password_2x);
                    holder.name.setText(R.string.security_password_verify);
                    break;
                case ConstantField.AuthenticationFunction.SECURITY_MAILBOX:
                    holder.image.setImageResource(R.drawable.security_mailbox_2x);
                    holder.name.setText(R.string.security_mailbox_verify);
                    break;
                case ConstantField.AuthenticationFunction.OLD_SECURITY_MAILBOX:
                    holder.image.setImageResource(R.drawable.security_mailbox_2x);
                    holder.name.setText(R.string.old_security_mailbox_verify);
                    break;
                default:
                    break;
            }
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_eulix_authentication, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof ViewHolder) {
            holder.itemView.setTag(position);
            generateViewHolderData((ViewHolder) holder, position);
        }
    }

    @Override
    public int getItemCount() {
        return (mAuthenticationList == null ? 0 : mAuthenticationList.size());
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
        private ImageView image;
        private TextView mode;
        private TextView name;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.authentication_image);
            mode = itemView.findViewById(R.id.authentication_mode);
            name = itemView.findViewById(R.id.authentication_name);
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
