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

import java.util.ArrayList;
import java.util.List;

import xyz.eulix.space.R;
import xyz.eulix.space.bean.SecuritySettingBean;
import xyz.eulix.space.util.StringUtil;

public class SecuritySettingAdapter extends RecyclerView.Adapter<SecuritySettingAdapter.ViewHolder> implements View.OnClickListener {
    private Context mContext;
    private List<SecuritySettingBean> mSecuritySettingBeanList;
    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public SecuritySettingAdapter(@NonNull Context context, List<SecuritySettingBean> securitySettingBeanList) {
        mContext = context;
        mSecuritySettingBeanList = securitySettingBeanList;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    public void updateData(List<SecuritySettingBean> securitySettingBeans) {
        if (securitySettingBeans == null) {
            securitySettingBeans = new ArrayList<>();
        }
        mSecuritySettingBeanList = securitySettingBeans;
        notifyDataSetChanged();
    }

    private void generateViewHolderData(ViewHolder holder, int position) {
        if (position >= 0 && mSecuritySettingBeanList != null && mSecuritySettingBeanList.size() > position) {
            SecuritySettingBean bean = mSecuritySettingBeanList.get(position);
            if (bean != null) {
                boolean isClick = bean.isClick();
                holder.indicator.setVisibility(isClick ? View.VISIBLE : View.GONE);
                if (isClick) {
                    holder.itemView.setOnClickListener(this);
                }
                holder.itemView.setClickable(isClick);
                holder.hint.setText(StringUtil.nullToEmpty(bean.getHintText()));
                switch (bean.getSecuritySettingFunction()) {
                    case SecuritySettingBean.FUNCTION_SPACE_ACCOUNT:
                        holder.name.setText(R.string.space_account);
                        holder.content.setVisibility(View.GONE);
                        break;
                    case SecuritySettingBean.FUNCTION_SECURITY_PASSWORD:
                        holder.name.setText(R.string.security_password);
                        holder.content.setVisibility(View.GONE);
                        break;
                    case SecuritySettingBean.FUNCTION_APPLICATION_LOCK:
                        holder.name.setText(R.string.application_lock);
                        holder.content.setVisibility(View.GONE);
                        break;
                    default:
                        break;
                }
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_security_setting, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.itemView.setTag(position);
        generateViewHolderData(holder, position);
    }

    @Override
    public int getItemCount() {
        return (mSecuritySettingBeanList == null ? 0 : mSecuritySettingBeanList.size());
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
        private TextView name;
        private TextView hint;
        private ImageView indicator;
        private TextView content;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.security_setting_name);
            hint = itemView.findViewById(R.id.security_setting_hint);
            indicator = itemView.findViewById(R.id.security_setting_indicator);
            content = itemView.findViewById(R.id.security_setting_content);
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
