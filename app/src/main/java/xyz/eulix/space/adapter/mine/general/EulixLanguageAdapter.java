package xyz.eulix.space.adapter.mine.general;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.TextUtils;
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
import xyz.eulix.space.bean.LocaleBeanItem;
import xyz.eulix.space.util.StringUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/12/13 9:51
 */
public class EulixLanguageAdapter extends RecyclerView.Adapter<EulixLanguageAdapter.ViewHolder> implements View.OnClickListener {
    private Context mContext;
    private List<LocaleBeanItem> mLocaleBeanItemList;
    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public EulixLanguageAdapter(Context context, List<LocaleBeanItem> localeBeanItemList) {
        mContext = context;
        mLocaleBeanItemList = localeBeanItemList;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    private void generateViewHolderData(ViewHolder holder, int position) {
        if (mLocaleBeanItemList != null && position >= 0 && mLocaleBeanItemList.size() > position) {
            LocaleBeanItem localeBeanItem = mLocaleBeanItemList.get(position);
            if (localeBeanItem != null) {
                String name = StringUtil.nullToEmpty(localeBeanItem.formatLocale());
                if (TextUtils.isEmpty(name)) {
                    name = mContext.getString(R.string.follow_up_system);
                }
                holder.languageName.setText(name);
                if (localeBeanItem.isSelect()) {
                    holder.languageSelectIndicator.setImageResource(R.drawable.icon_radio_button_select_2x_temp);
                } else {
                    holder.languageSelectIndicator.setImageDrawable(null);
                }
            }
        }
    }

    public void updateData(List<LocaleBeanItem> localeBeanItems) {
        if (localeBeanItems == null) {
            localeBeanItems = new ArrayList<>();
        }
        mLocaleBeanItemList = localeBeanItems;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_eulix_language, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.itemView.setTag(position);
        holder.itemView.setOnClickListener(this);
        generateViewHolderData(holder, position);
    }

    @Override
    public int getItemCount() {
        return (mLocaleBeanItemList == null ? 0 : mLocaleBeanItemList.size());
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
        private TextView languageName;
        private ImageView languageSelectIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            languageName = itemView.findViewById(R.id.language_name);
            languageSelectIndicator = itemView.findViewById(R.id.language_select_indicator);
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
