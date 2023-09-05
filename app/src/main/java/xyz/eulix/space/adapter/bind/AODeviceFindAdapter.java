package xyz.eulix.space.adapter.bind;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.airbnb.lottie.LottieAnimationView;

import java.util.ArrayList;
import java.util.List;

import xyz.eulix.space.R;
import xyz.eulix.space.bean.AODeviceFindBean;
import xyz.eulix.space.bean.BoxGenerationShowBean;
import xyz.eulix.space.bean.bind.InitResponse;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.LottieUtil;
import xyz.eulix.space.util.StringUtil;

public class AODeviceFindAdapter extends RecyclerView.Adapter<AODeviceFindAdapter.ViewHolder> implements View.OnClickListener {
    private Context mContext;
    private List<AODeviceFindBean> mAoDeviceFindBeanList;
    private OnItemClickListener mOnItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public AODeviceFindAdapter(Context context, List<AODeviceFindBean> aoDeviceFindBeans) {
        if (aoDeviceFindBeans == null) {
            aoDeviceFindBeans = new ArrayList<>();
        }
        mContext = context;
        mAoDeviceFindBeanList = aoDeviceFindBeans;
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    private void generateViewHolderData(ViewHolder holder, int position) {
        if (holder != null && mAoDeviceFindBeanList != null && position >= 0 && mAoDeviceFindBeanList.size() > position) {
            AODeviceFindBean aoDeviceFindBean = mAoDeviceFindBeanList.get(position);
            if (aoDeviceFindBean != null) {
                BoxGenerationShowBean boxGenerationShowBean = new BoxGenerationShowBean(mContext.getString(R.string.device_server_name), R.drawable.eulix_box_v3_2x);
                if (aoDeviceFindBean.isOpenSource()) {
                    boxGenerationShowBean.setBoxName(mContext.getString(R.string.open_source_version));
                    boxGenerationShowBean.setBoxResId(R.drawable.eulix_device_computer_2x);
                } else {
                    boxGenerationShowBean = DataUtil.generationBoxGenerationShowBean(mContext, aoDeviceFindBean.getDeviceModelNumber(), boxGenerationShowBean);
                }
                holder.tvDeviceName.setText(boxGenerationShowBean.getBoxName());
                holder.ivDevice.setImageResource(boxGenerationShowBean.getBoxResId());
                String sn = aoDeviceFindBean.getSn();
                if (StringUtil.isNonBlankString(sn)) {
                    String snContent = ("SN: " + sn);
                    holder.tvDeviceSn.setText(snContent);
                } else {
                    holder.tvDeviceSn.setText("");
                }
                if (aoDeviceFindBean.getBindStatus() == InitResponse.PAIRED_BOUND) {
                    holder.tvDeviceHint.setVisibility(View.VISIBLE);
                    holder.tvDeviceHint.setText(R.string.binding_been_initialized_hint);
                } else {
                    holder.tvDeviceHint.setText("");
                    holder.tvDeviceHint.setVisibility(View.GONE);
                }
                if (aoDeviceFindBean.isOpenSource()) {
                    holder.layoutIncompatible.setVisibility(View.GONE);
                    holder.loadingButtonContainer.setTag(position);
                    holder.loadingButtonContainer.setBackgroundResource(R.drawable.background_ff337aff_ff16b9ff_rectangle_10);
                    if (aoDeviceFindBean.isBinding()) {
                        holder.loadingButtonContainer.setClickable(false);
                        holder.loadingContent.setText(R.string.binding);
                        holder.loadingAnimation.setVisibility(View.VISIBLE);
                        LottieUtil.loop(holder.loadingAnimation, "loading_button.json");
                    } else {
                        holder.loadingButtonContainer.setClickable(true);
                        holder.loadingButtonContainer.setOnClickListener(this);
                        LottieUtil.stop(holder.loadingAnimation);
                        holder.loadingAnimation.setVisibility(View.GONE);
                        holder.loadingContent.setText(R.string.bind);
                    }
                } else {
                    holder.layoutIncompatible.setVisibility(View.VISIBLE);
                    holder.loadingButtonContainer.setBackgroundResource(R.drawable.background_ffd2dcef_rectangle_10);
                    LottieUtil.stop(holder.loadingAnimation);
                    holder.loadingAnimation.setVisibility(View.GONE);
                    holder.loadingContent.setText(R.string.cannot_bind);

                    SpannableStringBuilder style = new SpannableStringBuilder();
                    String content = mContext.getResources().getString(R.string.download_address);
                    String trim = "https://ao.space/download";
                    style.append(content).append(trim);

                    ClickableSpan clickableSpan = new ClickableSpan() {
                        @Override
                        public void onClick(View widget) {
                            Intent intent = new Intent();
                            intent.setData(Uri.parse(trim));
                            intent.setAction(Intent.ACTION_VIEW);
                            mContext.startActivity(intent);
                        }
                    };
                    int trimStart = content.length();
                    style.setSpan(clickableSpan, trimStart, trimStart + trim.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(mContext.getResources().getColor(R.color.c_ff337aff));
                    style.setSpan(foregroundColorSpan, trimStart, trimStart + trim.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    holder.tvDownloadAddress.setMovementMethod(LinkMovementMethod.getInstance());
                    holder.tvDownloadAddress.setText(style);
                }
            }
        }
    }

    public void updateData(List<AODeviceFindBean> aoDeviceFindBeans) {
        if (aoDeviceFindBeans == null) {
            aoDeviceFindBeans = new ArrayList<>();
        }
        mAoDeviceFindBeanList = aoDeviceFindBeans;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(mContext).inflate(R.layout.item_ao_device_find, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        generateViewHolderData(holder, position);
    }

    @Override
    public int getItemCount() {
        return (mAoDeviceFindBeanList == null ? 0 : mAoDeviceFindBeanList.size());
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
        private TextView tvDeviceName;
        private TextView tvDeviceSn;
        private ImageView ivDevice;
        private TextView tvDeviceHint;
        private LinearLayout loadingButtonContainer;
        private LottieAnimationView loadingAnimation;
        private TextView loadingContent;
        private LinearLayout layoutIncompatible;
        private TextView tvDownloadAddress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDeviceName = itemView.findViewById(R.id.tv_device_name);
            tvDeviceSn = itemView.findViewById(R.id.tv_device_sn);
            ivDevice = itemView.findViewById(R.id.iv_device);
            tvDeviceHint = itemView.findViewById(R.id.tv_device_hint);
            loadingButtonContainer = itemView.findViewById(R.id.loading_button_container);
            loadingAnimation = itemView.findViewById(R.id.loading_animation);
            loadingContent = itemView.findViewById(R.id.loading_content);
            layoutIncompatible = itemView.findViewById(R.id.layout_incompatible);
            tvDownloadAddress = itemView.findViewById(R.id.tv_download_address);
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
