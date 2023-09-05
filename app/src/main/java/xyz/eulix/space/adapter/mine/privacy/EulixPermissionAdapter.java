package xyz.eulix.space.adapter.mine.privacy;

import android.Manifest;
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

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/12/13 17:45
 */
public class EulixPermissionAdapter extends RecyclerView.Adapter<EulixPermissionAdapter.ViewHolder> implements View.OnClickListener {
    private Context mContext;
    private List<String> mPermissionGroupList;
    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public EulixPermissionAdapter(Context context, List<String> permissionGroupList) {
        mContext = context;
        mPermissionGroupList = permissionGroupList;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    private void generateViewHolderData(ViewHolder holder, int position) {
        if (mPermissionGroupList != null && position >= 0 && mPermissionGroupList.size() > position) {
            String permissionGroup = mPermissionGroupList.get(position);
            String title = mContext.getString(R.string.unknown);
            String content = mContext.getString(R.string.unknown);
            switch (permissionGroup) {
                case Manifest.permission_group.STORAGE:
                    title = mContext.getString(R.string.storage);
                    content = mContext.getString(R.string.storage_content);
                    holder.permissionImage.setImageResource(R.drawable.icon_permission_storage_2x);
                    break;
                case Manifest.permission_group.LOCATION:
                    title = mContext.getString(R.string.location);
                    content = mContext.getString(R.string.location_content);
                    holder.permissionImage.setImageResource(R.drawable.icon_permission_location_2x);
                    break;
                case Manifest.permission_group.CONTACTS:
                    title = mContext.getString(R.string.contacts);
                    content = mContext.getString(R.string.contacts_content);
                    holder.permissionImage.setImageResource(R.drawable.icon_permission_contacts_2x);
                    break;
                case Manifest.permission_group.CAMERA:
                    title = mContext.getString(R.string.camera);
                    content = mContext.getString(R.string.camera_content);
                    holder.permissionImage.setImageResource(R.drawable.icon_permission_camera_2x);
                    break;
                default:
                    break;
            }
            holder.permissionTitle.setText(title);
            holder.permissionContent.setText(content);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_eulix_permission, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.itemView.setTag(position);
        holder.itemView.setOnClickListener(this);
        generateViewHolderData(holder, position);
    }

    @Override
    public int getItemCount() {
        return (mPermissionGroupList == null ? 0 : mPermissionGroupList.size());
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
        private ImageView permissionImage;
        private TextView permissionTitle;
        private TextView permissionContent;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            permissionImage = itemView.findViewById(R.id.permission_image);
            permissionTitle = itemView.findViewById(R.id.permission_title);
            permissionContent = itemView.findViewById(R.id.permission_content);
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
