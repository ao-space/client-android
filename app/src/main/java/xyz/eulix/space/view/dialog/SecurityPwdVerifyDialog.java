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
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.view.password.PasswordLayout;

/**
 * Author:      Zhu Fuyu
 * Description: 安全密码校验弹框
 * History:     2021/10/26
 */
public class SecurityPwdVerifyDialog extends Dialog {
    //安全密码校验限制次数
    private int mPasswordVerifyLimitTimes = 3;

    public SecurityPwdVerifyDialog(Context context) {
        super(context, R.style.EulixDialog);
    }

    public static class Builder {
        private Context mContext;
        private InputListener listener;

        public Builder(Context context) {
            this.mContext = context;
        }

        public Builder setVerifyListener(InputListener listener) {
            this.listener = listener;
            return this;
        }

        public SecurityPwdVerifyDialog create() {
            SecurityPwdVerifyDialog dialog = new SecurityPwdVerifyDialog(mContext);

            Window window = dialog.getWindow();
            window.getDecorView().setPadding(0, 0, 0, 0);
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.width = mContext.getResources().getDimensionPixelSize(R.dimen.dp_259);
            window.setAttributes(layoutParams);

            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = inflater.inflate(R.layout.security_pwd_check_dialog_layout, null);
            ImageView btnCancel = view.findViewById(R.id.btn_cancel);

            TextView tvPwdError = view.findViewById(R.id.tv_notice);

            PasswordLayout passwordLayout = view.findViewById(R.id.pass_layout);
            passwordLayout.removeAllPwd();
            tvPwdError.setVisibility(View.INVISIBLE);
            btnCancel.setOnClickListener(v -> {
                dialog.dismiss();
                if (listener != null) {
                    passwordLayout.removeAllPwd();
                    listener.onCancel();
                }
            });
            passwordLayout.setPwdChangeListener(new PasswordLayout.PwdChangeListener() {
                @Override
                public void onChange(String pwd) {
                    tvPwdError.setVisibility(View.INVISIBLE);
                }

                @Override
                public void onNull() {

                }

                @Override
                public void onFinished(String pwd) {
                    Logger.d("zfy", "psw:" + pwd);
                    if (listener != null) {
                        listener.onInputFinish(pwd, new WatchResult() {
                            @Override
                            public void onSuccess() {
                                //验证成功
                            }

                            @Override
                            public void onFailed(int resultCode) {
                                //验证失败
                                if (resultCode == ConstantField.ErrorCode.RESTORE_PASSWORD_ERROR) {
                                    //密码错误
                                    passwordLayout.removeAllPwd();
                                    dialog.mPasswordVerifyLimitTimes--;
                                    if (dialog.mPasswordVerifyLimitTimes <= 0) {
                                        listener.onOverLimitTime();
                                    } else {
                                        tvPwdError.setVisibility(View.VISIBLE);
                                        tvPwdError.setText(mContext.getResources().getString(R.string.sec_pass_verify_fail).replace("%", String.valueOf(dialog.mPasswordVerifyLimitTimes)));
                                    }
                                }
                            }
                        });
                    }
                }
            });

            dialog.setContentView(view);
            dialog.setCancelable(false);
            return dialog;
        }
    }

    @Override
    public void show() {
        super.show();
        //每次打开恢复限制次数
        mPasswordVerifyLimitTimes = 3;
    }

    @Override
    public void dismiss() {
        super.dismiss();
    }

    public interface InputListener {

        void onInputFinish(String pwd, WatchResult result);

        void onCancel();

        //超过密码校验限制次数
        void onOverLimitTime();
    }

    public interface WatchResult {
        void onSuccess();

        void onFailed(int resultCode);
    }

}
