package xyz.eulix.space.did.adapter;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import xyz.eulix.space.R;
import xyz.eulix.space.did.bean.CredentialInformationBean;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.StringUtil;

public class CredentialInformationAdapter extends RecyclerView.Adapter<CredentialInformationAdapter.ViewHolder> implements View.OnClickListener {
    private Context mContext;
    private List<CredentialInformationBean> mCredentialInformationBeanList;

    public CredentialInformationAdapter(@NonNull Context context, List<CredentialInformationBean> credentialInformationBeanList) {
        mContext = context;
        mCredentialInformationBeanList = credentialInformationBeanList;
    }

    private void generateViewHolderData(ViewHolder holder, int position) {
        if (position >= 0 && mCredentialInformationBeanList != null && mCredentialInformationBeanList.size() > position) {
            int size = mCredentialInformationBeanList.size();
            CredentialInformationBean bean = mCredentialInformationBeanList.get(position);
            if (bean != null) {
                int credentialType = bean.getCredentialType();
                StringBuilder credentialTypeBuilder = new StringBuilder();
                switch (credentialType) {
                    case CredentialInformationBean.CREDENTIAL_TYPE_AO_SPACE_SERVER:
                        credentialTypeBuilder.append(mContext.getString(R.string.ao_space_server_credential));
                        break;
                    case CredentialInformationBean.CREDENTIAL_TYPE_BIND_PHONE:
                        credentialTypeBuilder.append(mContext.getString(R.string.bind_phone_credential));
                        break;
                    case CredentialInformationBean.CREDENTIAL_TYPE_SECURITY_PASSWORD:
                        credentialTypeBuilder.append(mContext.getString(R.string.security_password_credential));
                        break;
                    case CredentialInformationBean.CREDENTIAL_TYPE_AUTHORIZE_PHONE:
                        credentialTypeBuilder.append(mContext.getString(R.string.authorize_phone_credential));
                        break;
                    case CredentialInformationBean.CREDENTIAL_TYPE_FRIEND:
                        credentialTypeBuilder.append(mContext.getString(R.string.friend_credential));
                        break;
                    default:
                        break;
                }
                if (size > 1) {
                    credentialTypeBuilder.append(" ");
                    credentialTypeBuilder.append((position + 1));
                }
                holder.credentialType.setText(credentialTypeBuilder.toString());
                CredentialInformationBean.StorageLocation storageLocation = bean.getStorageLocation();
                StringBuilder locationTextBuilder = new StringBuilder();
                if (storageLocation != null) {
                    Integer locationIndex = storageLocation.getLocationIndex();
                    if (locationIndex != null) {
                        switch (locationIndex) {
                            case CredentialInformationBean.StorageLocation.LOCATION_SERVER:
                                locationTextBuilder.append(mContext.getString(R.string.ao_space_server));
                                break;
                            case CredentialInformationBean.StorageLocation.LOCATION_CLIENT_BINDER:
                                locationTextBuilder.append(mContext.getString(R.string.bind_phone));
                                String phoneModel = storageLocation.getPhoneModel();
                                if (StringUtil.isNonBlankString(phoneModel)) {
                                    locationTextBuilder.append(" ");
                                    locationTextBuilder.append(phoneModel);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }
                Long lastUpdateTimestamp = bean.getLastUpdateTimestamp();
                String publicKeyHashValue = bean.getPublicKey();
                if (StringUtil.isNonBlankString(publicKeyHashValue)) {
                    holder.publicKeyHash.setText(publicKeyHashValue);
                } else {
                    holder.publicKeyHash.setText(R.string.none);
                }
                String locationText = locationTextBuilder.toString();
                if (StringUtil.isNonBlankString(locationText)) {
                    holder.storageLocation.setText(locationText);
                } else {
                    holder.storageLocation.setText(R.string.none);
                }
                String lastUpdateTimeValue = null;
                if (lastUpdateTimestamp != null) {
                    lastUpdateTimeValue = FormatUtil.formatTime(lastUpdateTimestamp, ConstantField.TimeStampFormat.DATE_FORMAT_2);
                }
                if (StringUtil.isNonBlankString(lastUpdateTimeValue)) {
                    holder.lastUpdateTime.setText(lastUpdateTimeValue);
                } else {
                    holder.lastUpdateTime.setText(R.string.none);
                }
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_credential_information, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        generateViewHolderData(holder, position);
    }

    @Override
    public int getItemCount() {
        return (mCredentialInformationBeanList == null ? 0 : mCredentialInformationBeanList.size());
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            // Do nothing
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView credentialType;
        private TextView publicKeyHash;
        private TextView storageLocation;
        private TextView lastUpdateTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            credentialType = itemView.findViewById(R.id.credential_type);
            publicKeyHash = itemView.findViewById(R.id.public_key_hash);
            storageLocation = itemView.findViewById(R.id.storage_location);
            lastUpdateTime = itemView.findViewById(R.id.last_update_time);
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
