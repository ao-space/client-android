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

import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import xyz.eulix.space.R;
import xyz.eulix.space.view.BottomDialog;

/**
 * Author:      Zhu Fuyu
 * Description: 底部弹出选择对话框
 * History:     2021/8/25
 */
public class EulixBottomSelectDialog extends BottomDialog {
    private TextView mTextTV;
    private TextView mPosButton;
    private TextView mNegButton;

    public EulixBottomSelectDialog(@NonNull Context context) {
        super(context, R.style.BottomDialogTheme, true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        getWindow().setBackgroundDrawableResource(android.R.color.transparent);
    }

    //builder设计模式修改固定参数
    public static class Builder {

        public Context mContext;

        private String mText;

        private String mPosButtonText;

        private String mNegButtonText;
        //给确定按钮添加点击事件
        private OnClickListener mPosButtonOnClickListener;

        //给取消按钮添加点击事件
        private OnClickListener mNegButtonOnclickListener;

        public Builder(Context context) {
            this.mContext = context;
        }

        public Builder setContent(String text) {
            this.mText = text;
            return this;
        }

        public Builder setPosButton(String text, OnClickListener listener) {
            this.mPosButtonText = text;
            this.mPosButtonOnClickListener = listener;
            return this;
        }

        public Builder setNegButton(String text, OnClickListener listener) {
            this.mNegButtonText = text;
            this.mNegButtonOnclickListener = listener;
            return this;
        }

        public EulixBottomSelectDialog create() {
            final EulixBottomSelectDialog dialog = new EulixBottomSelectDialog(mContext);

            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View layout = inflater.inflate(R.layout.eulix_bottom_select_layout, null);

            dialog.mTextTV = layout.findViewById(R.id.tv_auth_scope);
            dialog.mTextTV.setText(mText);

            dialog.mPosButton = layout.findViewById(R.id.btn_positive);
            if (!TextUtils.isEmpty(mPosButtonText)) {
                dialog.mPosButton.setText(mPosButtonText);
                dialog.mPosButton.setOnClickListener(v -> {
                    if (mPosButtonOnClickListener != null) {
                        mPosButtonOnClickListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                    }
                    dialog.dismiss();
                });
            }

            //设置negbutton
            dialog.mNegButton = layout.findViewById(R.id.btn_negative);
            if (!TextUtils.isEmpty(mNegButtonText)) {
                dialog.mNegButton.setText(mNegButtonText);

                dialog.mNegButton.setOnClickListener(v -> {
                    if (mNegButtonOnclickListener != null) {
                        mNegButtonOnclickListener.onClick(dialog, DialogInterface.BUTTON_NEGATIVE);
                    }
                    dialog.dismiss();
                });
            }

            dialog.setContentView(layout);
            return dialog;
        }

    }

}
