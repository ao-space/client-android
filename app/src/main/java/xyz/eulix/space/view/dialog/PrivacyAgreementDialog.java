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
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import xyz.eulix.space.R;
import xyz.eulix.space.ui.EulixWebViewActivity;
import xyz.eulix.space.ui.mine.AboutUsActivity;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DebugUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.view.BottomDialog;
import xyz.eulix.space.view.LinkClickSpan;

/**
 * Author:      Zhu Fuyu
 * Description: 隐私协议对话框
 * History:     2021/11/12
 */
public class PrivacyAgreementDialog extends BottomDialog {
    private TextView mTextTV;
    private TextView mPosButton;
    private TextView mNegButton;

    public PrivacyAgreementDialog(@NonNull Context context) {
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

        private String mPosButtonText;

        private String mNegButtonText;
        //给确定按钮添加点击事件
        private OnClickListener mPosButtonOnClickListener;

        //给取消按钮添加点击事件
        private OnClickListener mNegButtonOnclickListener;

        public Builder(Context context) {
            this.mContext = context;
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

        public PrivacyAgreementDialog create() {
            final PrivacyAgreementDialog dialog = new PrivacyAgreementDialog(mContext);

            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View layout = inflater.inflate(R.layout.privacy_agreement_dialog_layout, null);

            dialog.mTextTV = layout.findViewById(R.id.tv_auth_scope);

            String agreementStr = mContext.getResources().getString(R.string.eulix_space_agreement);
            String privacyStr =  mContext.getResources().getString(R.string.eulix_space_privacy);

            SpannableString agreementSpannable = new SpannableString(agreementStr);
            SpannableString privacySpannable = new SpannableString(privacyStr);
            LinkClickSpan trialSpan = new LinkClickSpan() {
                @Override
                public void onClick(View widget) {
                    Logger.d("zfy","click trial");
                    String url = FormatUtil.isChinese(FormatUtil.getLocale(mContext)
                            , false) ? ConstantField.URL.AGREEMENT_API
                            : ConstantField.URL.EN_AGREEMENT_API;
                    EulixWebViewActivity.startWeb(mContext, mContext.getResources().getString(R.string.user_agreement),
                            url);
                }
            };
            LinkClickSpan privacySpan = new LinkClickSpan() {
                @Override
                public void onClick(View widget) {
                    Logger.d("zfy","click privacy");
                    String url = FormatUtil.isChinese(FormatUtil.getLocale(mContext)
                            , false) ? ConstantField.URL.PRIVACY_API
                            : ConstantField.URL.EN_PRIVACY_API;
                    EulixWebViewActivity.startWeb(mContext, mContext.getResources().getString(R.string.privacy_policy),
                            url);
                }
            };

            agreementSpannable.setSpan(trialSpan, 0, agreementStr.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            privacySpannable.setSpan(privacySpan, 0, privacyStr.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

            dialog.mTextTV.append(mContext.getResources().getString(R.string.privacy_desc_part_1));
            dialog.mTextTV.append(agreementSpannable);
            dialog.mTextTV.append(mContext.getResources().getString(R.string.and));
            dialog.mTextTV.append(privacySpannable);
            dialog.mTextTV.append(mContext.getResources().getString(R.string.privacy_desc_part_2));
            dialog.mTextTV.setMovementMethod(LinkMovementMethod.getInstance());

            dialog.mPosButton = layout.findViewById(R.id.btn_positive);
            if (!TextUtils.isEmpty(mPosButtonText)) {
                dialog.mPosButton.setText(mPosButtonText);
                dialog.mPosButton.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (mPosButtonOnClickListener != null) {
                        mPosButtonOnClickListener.onClick(dialog, DialogInterface.BUTTON_POSITIVE);
                    }
                });
            }

            //设置negbutton
            dialog.mNegButton = layout.findViewById(R.id.btn_negative);
            if (!TextUtils.isEmpty(mNegButtonText)) {
                dialog.mNegButton.setText(mNegButtonText);

                dialog.mNegButton.setOnClickListener(v -> {
                    dialog.dismiss();
                    if (mNegButtonOnclickListener != null) {
                        mNegButtonOnclickListener.onClick(dialog, DialogInterface.BUTTON_NEGATIVE);
                    }
                });
            }

            dialog.setContentView(layout);
            dialog.setCancelable(false);
            return dialog;
        }

    }

}
