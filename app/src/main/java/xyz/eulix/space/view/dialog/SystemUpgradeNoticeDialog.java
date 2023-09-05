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
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import xyz.eulix.space.R;
import xyz.eulix.space.ui.mine.SystemUpdateActivity;
import xyz.eulix.space.util.ConstantField;

/**
 * Author:      Zhu Fuyu
 * Description: 系统升级提示对话框
 * History:     2021/7/20
 */
public class SystemUpgradeNoticeDialog extends Dialog {
    public static final int INSTALL_NOW = 1;
    public static final int LATER = INSTALL_NOW + 1;
    public static final int DETAIL_INFO = LATER + 1;
    private TextView mTvContent;
    private TextView btnInstallNow;
    private TextView btnLater;
    private TextView btnDetailInfo;

    public SystemUpgradeNoticeDialog(Context context) {
        super(context, R.style.EulixDialog);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    public static class Builder {
        private Context mContext;
        private String mVersionName;
        private boolean mCancelable = false;
        private OnClickListener onClickListener;

        public Builder(Context context) {
            this.mContext = context;
        }


        public Builder setVersionName(String versionName) {
            this.mVersionName = versionName;
            return this;
        }

        public Builder setCancelable(boolean cancelable) {
            this.mCancelable = cancelable;
            return this;
        }

        public Builder setOnClickListener(OnClickListener clickListener){
            this.onClickListener = clickListener;
            return this;
        }

        public SystemUpgradeNoticeDialog create() {
            SystemUpgradeNoticeDialog dialog = new SystemUpgradeNoticeDialog(mContext);

            Window window = dialog.getWindow();
            window.getDecorView().setPadding(0, 0, 0, 0);
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.width = mContext.getResources().getDimensionPixelSize(R.dimen.dp_259);
            window.setAttributes(layoutParams);

            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = inflater.inflate(R.layout.layout_dialog_system_upgrade_notice, null);

            dialog.mTvContent = view.findViewById(R.id.dialog_content);
            dialog.btnInstallNow = view.findViewById(R.id.tv_install_now);
            dialog.btnLater = view.findViewById(R.id.tv_later);
            dialog.btnDetailInfo = view.findViewById(R.id.tv_detail_info);

            if (!TextUtils.isEmpty(mVersionName)) {
                dialog.mTvContent.setText(mContext.getResources().getString(R.string.system_upgrade_dialog_content).replace("%$", mVersionName));
            }
            dialog.btnInstallNow.setOnClickListener(v -> {
                //跳转到系统升级页面，并发起安装
                Intent intent = new Intent(mContext, SystemUpdateActivity.class);
                intent.putExtra(SystemUpdateActivity.KEY_DIALOG_OPERATE_TYPE, SystemUpdateActivity.DIALOG_OPERATE_TYPE_INSTALL_NOW);
                mContext.startActivity(intent);

                if (onClickListener != null){
                    onClickListener.onClick(dialog, INSTALL_NOW);
                }
                dialog.dismiss();
            });
            dialog.btnLater.setOnClickListener(v -> {
                ConstantField.hasClickSystemUpgradeInstallLater = true;
                if (onClickListener != null){
                    onClickListener.onClick(dialog, LATER);
                }
                dialog.dismiss();
            });
            dialog.btnDetailInfo.setOnClickListener(v -> {
                //跳转到系统升级页面，不发起安装
                Intent intent = new Intent(mContext, SystemUpdateActivity.class);
                intent.putExtra(SystemUpdateActivity.KEY_DIALOG_OPERATE_TYPE, SystemUpdateActivity.DIALOG_OPERATE_TYPE_INSTALL_LATER);
                mContext.startActivity(intent);

                if (onClickListener != null){
                    onClickListener.onClick(dialog, DETAIL_INFO);
                }
                dialog.dismiss();
            });

            dialog.setContentView(view);
            dialog.setCancelable(mCancelable);

            return dialog;

        }
    }

}
