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

package xyz.eulix.space.view.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import xyz.eulix.space.R;
import xyz.eulix.space.util.PermissionUtils;

/**
 * Author:      Zhu Fuyu
 * Description: 权限提示对话框
 * History:     2023/4/25
 */
public class PermissionAlertDialog extends Dialog {
    private TextView mBtnConfirm;
    private TextView mTvDesc;
    private ImageView mImgTop;
    private ImageView mImgExit;

    public PermissionAlertDialog(Context context) {
        super(context, R.style.EulixDialog);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    public static class Builder {
        private Context mContext;
        private boolean mCancelable = true;

        private String mPermission;
        private OnClickListener mOnClickListener;

        public Builder(Context context) {
            this.mContext = context;
        }

        public Builder setPermission(String permission) {
            this.mPermission = permission;
            return this;
        }

        public Builder setOnClickListener(OnClickListener listener) {
            this.mOnClickListener = listener;
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            this.mCancelable = cancelable;
            return this;
        }

        public PermissionAlertDialog create() {
            PermissionAlertDialog dialog = new PermissionAlertDialog(mContext);

            Window window = dialog.getWindow();
            window.getDecorView().setPadding(0, 0, 0, 0);
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.width = mContext.getResources().getDimensionPixelSize(R.dimen.dp_259);
            window.setAttributes(layoutParams);

            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = inflater.inflate(R.layout.eulix_permission_alert_dialog, null);

            dialog.mTvDesc = view.findViewById(R.id.tv_desc);
            dialog.mBtnConfirm = view.findViewById(R.id.btn_confirm);
            dialog.mImgTop = view.findViewById(R.id.img_top);
            dialog.mImgExit = view.findViewById(R.id.img_exit);

            int topImgResId = -1;
            String textDesc = null;
            if (mPermission != null) {
                switch (mPermission) {
                    case PermissionUtils.PERMISSION_WRITE_STORAGE:
                        topImgResId = R.drawable.icon_permission_top_storage;
                        textDesc = mContext.getResources().getString(R.string.tip_permission_storage);
                        break;
                    case PermissionUtils.PERMISSION_CAMERA:
                        topImgResId = R.drawable.icon_permission_top_camera;
                        textDesc = mContext.getResources().getString(R.string.tip_permission_camera);
                        break;
                    case PermissionUtils.PERMISSION_READ_CONTACTS:
                        topImgResId = R.drawable.icon_permission_top_contact;
                        textDesc = mContext.getResources().getString(R.string.tip_permission_contact);
                        break;
                    case PermissionUtils.PERMISSION_ACCESS_COARSE_LOCATION:
                    case PermissionUtils.PERMISSION_ACCESS_FINE_LOCATION:
                        topImgResId = R.drawable.icon_permission_top_location;
                        textDesc = mContext.getResources().getString(R.string.tip_permission_location);
                        break;
                    default:
                }

                if (topImgResId != -1) {
                    dialog.mImgTop.setImageResource(topImgResId);
                }
                if (textDesc != null) {
                    dialog.mTvDesc.setText(textDesc);
                }
            }

            dialog.mBtnConfirm.setOnClickListener(v -> {
                if (mOnClickListener != null) {
                    mOnClickListener.onClickToSet();
                }
                dialog.dismiss();
            });

            dialog.mImgExit.setOnClickListener(v -> {
                if (mOnClickListener != null) {
                    mOnClickListener.onClickExit();
                }
                dialog.dismiss();
            });

            dialog.setContentView(view);
            dialog.setCancelable(mCancelable);

            return dialog;

        }
    }

    public interface OnClickListener {
        void onClickToSet();

        void onClickExit();
    }

}
