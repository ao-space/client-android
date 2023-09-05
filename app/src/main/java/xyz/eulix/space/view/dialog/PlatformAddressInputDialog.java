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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;

import xyz.eulix.space.R;
import xyz.eulix.space.view.BottomDialog;
import xyz.eulix.space.view.NormalEditInputView;

/**
 * @author: Zhufy
 * Description: 平台域名输入对话框
 * date: 2023/8/14
 */
public class PlatformAddressInputDialog extends BottomDialog {
    private Context mContext;
    private TextView tvConfirm;
    private TextView tvCancel;
    private NormalEditInputView inputView;

    public PlatformAddressInputDialog(@NonNull Context context) {
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
        private String mAddress;
        private OnConfirmListener mListener;

        public Builder(Context context) {
            this.mContext = context;
        }

        public Builder setAddress(String address) {
            this.mAddress = address;
            return this;
        }

        public Builder setListener(OnConfirmListener listener) {
            this.mListener = listener;
            return this;
        }

        public PlatformAddressInputDialog create() {
            final PlatformAddressInputDialog dialog = new PlatformAddressInputDialog(mContext);

            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View layout = inflater.inflate(R.layout.dialog_platform_address_input_layout, null);

            dialog.tvConfirm = layout.findViewById(R.id.tv_confirm);
            dialog.tvCancel = layout.findViewById(R.id.tv_cancel);
            dialog.inputView = layout.findViewById(R.id.input_view);

            dialog.tvConfirm.setOnClickListener(v -> {
                dialog.dismiss();
                if (mListener != null) {
                    mListener.onResult(dialog.inputView.getInputText());
                }
            });
            dialog.tvCancel.setOnClickListener(v -> {
                dialog.dismiss();
            });

            if (!TextUtils.isEmpty(mAddress)) {
                dialog.inputView.setText(mAddress);
            }

            dialog.setContentView(layout);
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

            return dialog;
        }
    }

    @Override
    public void show() {
        super.show();
        inputView.requestFocus();
    }

    @Override
    public void dismiss() {
        super.dismiss();
    }

    public interface OnConfirmListener {
        void onResult(String address);
    }
}
