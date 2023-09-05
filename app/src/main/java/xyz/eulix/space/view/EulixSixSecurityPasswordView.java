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

package xyz.eulix.space.view;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import xyz.eulix.space.R;

public class EulixSixSecurityPasswordView implements View.OnClickListener {
    private static final int PASSWORD_LENGTH = 6;
    private Context mContext;
    private View mView;
    private LinearLayout eulixSixSecurityPasswordContainer;
    private TextView[] mPasswordArray;
    private TextView password1;
    private TextView password2;
    private TextView password3;
    private TextView password4;
    private TextView password5;
    private TextView password6;
    private Dialog eulixSoftInputNumberDialog;
    private ImageButton keyHide;
    private Button key1;
    private Button key2;
    private Button key3;
    private Button key4;
    private Button key5;
    private Button key6;
    private Button key7;
    private Button key8;
    private Button key9;
    private Button key0;
    private ImageButton keyBackspace;
    private IEulixSixSecurityPassword mCallback;
    private boolean mEnable;
    private Integer[] mData;
    private int mIndex;
    private boolean isPassword;

    public EulixSixSecurityPasswordView(@NonNull Context context, boolean isPassword, IEulixSixSecurityPassword callback) {
        mContext = context;
        this.isPassword = isPassword;
        mCallback = callback;
        init();
        initView();
        initListener();
    }

    public interface IEulixSixSecurityPassword {
        void onPrepared(View view);

        void onComplete(String passwordValue);

        void onInserted(String currentValue);
    }

    private void init() {
        mEnable = false;
        resetPassword();
    }

    private void initView() {
        mView = LayoutInflater.from(mContext).inflate(isPassword ? R.layout.eulix_six_security_password : R.layout.eulix_six_security_code, null);
        eulixSixSecurityPasswordContainer = mView.findViewById(R.id.eulix_six_security_password_container);
        password1 = mView.findViewById(R.id.password_1);
        password2 = mView.findViewById(R.id.password_2);
        password3 = mView.findViewById(R.id.password_3);
        password4 = mView.findViewById(R.id.password_4);
        password5 = mView.findViewById(R.id.password_5);
        password6 = mView.findViewById(R.id.password_6);
        mPasswordArray[0] = password1;
        mPasswordArray[1] = password2;
        mPasswordArray[2] = password3;
        mPasswordArray[3] = password4;
        mPasswordArray[4] = password5;
        mPasswordArray[5] = password6;

        View eulixSoftInputNumberDialogView = LayoutInflater.from(mContext).inflate(R.layout.eulix_soft_input_number_window, null);
        keyHide = eulixSoftInputNumberDialogView.findViewById(R.id.key_hide);
        key1 = eulixSoftInputNumberDialogView.findViewById(R.id.key_1);
        key2 = eulixSoftInputNumberDialogView.findViewById(R.id.key_2);
        key3 = eulixSoftInputNumberDialogView.findViewById(R.id.key_3);
        key4 = eulixSoftInputNumberDialogView.findViewById(R.id.key_4);
        key5 = eulixSoftInputNumberDialogView.findViewById(R.id.key_5);
        key6 = eulixSoftInputNumberDialogView.findViewById(R.id.key_6);
        key7 = eulixSoftInputNumberDialogView.findViewById(R.id.key_7);
        key8 = eulixSoftInputNumberDialogView.findViewById(R.id.key_8);
        key9 = eulixSoftInputNumberDialogView.findViewById(R.id.key_9);
        key0 = eulixSoftInputNumberDialogView.findViewById(R.id.key_0);
        keyBackspace = eulixSoftInputNumberDialogView.findViewById(R.id.key_backspace);
        eulixSoftInputNumberDialog = new BottomDialog(mContext, false);
        eulixSoftInputNumberDialog.setCancelable(false);
        eulixSoftInputNumberDialog.setContentView(eulixSoftInputNumberDialogView);
    }

    private void initListener() {
        eulixSixSecurityPasswordContainer.setOnClickListener(this);
        password1.setOnClickListener(this);
        password2.setOnClickListener(this);
        password3.setOnClickListener(this);
        password4.setOnClickListener(this);
        password5.setOnClickListener(this);
        password6.setOnClickListener(this);
        keyHide.setOnClickListener(this);
        key1.setOnClickListener(this);
        key2.setOnClickListener(this);
        key3.setOnClickListener(this);
        key4.setOnClickListener(this);
        key5.setOnClickListener(this);
        key6.setOnClickListener(this);
        key7.setOnClickListener(this);
        key8.setOnClickListener(this);
        key9.setOnClickListener(this);
        key0.setOnClickListener(this);
        keyBackspace.setOnClickListener(this);
        if (mCallback != null) {
            mEnable = true;
            mCallback.onPrepared(mView);
        }
    }

    private void fillPasswordTextView(Integer number) {
        if (mIndex >= 0 && mIndex < PASSWORD_LENGTH) {
            mPasswordArray[mIndex].setText((number == null ? "" : String.valueOf(number.intValue())));
        }
    }

    private void insertPassword(int number) {
        if (mEnable) {
            if (mData != null && mIndex >= 0 && mIndex < PASSWORD_LENGTH && number >= 0 && number <= 9) {
                mData[mIndex] = number;
                fillPasswordTextView(number);
                mIndex += 1;
            }
            StringBuilder passwordBuilder = new StringBuilder();
            for (Integer integer : mData) {
                if (integer != null) {
                    passwordBuilder.append(integer.intValue());
                }
            }
            if (mCallback != null) {
                mCallback.onInserted(passwordBuilder.toString());
            }
            if (mIndex >= PASSWORD_LENGTH) {
                if (mCallback != null) {
                    mCallback.onComplete(passwordBuilder.toString());
                }
            }
        }
    }

    private void deletePassword() {
        if (mEnable && mData != null && mIndex > 0 && mIndex <= PASSWORD_LENGTH) {
            mIndex -= 1;
            fillPasswordTextView(null);
            mData[mIndex] = null;
        }
    }

    public void resetPassword() {
        mIndex = 0;
        if (mPasswordArray == null) {
            mPasswordArray = new TextView[PASSWORD_LENGTH];
        } else {
            for (TextView tv : mPasswordArray) {
                if (tv != null) {
                    tv.setText("");
                }
            }
        }
        if (mData == null) {
            mData = new Integer[PASSWORD_LENGTH];
        } else {
            for (int i = 0; i < PASSWORD_LENGTH; i++) {
                mData[i] = null;
            }
        }
    }

    public void setEnable(boolean isEnable) {
        mEnable = isEnable;
    }

    public void setFocus(boolean isFocus) {
        if (isFocus) {
            showEulixSoftInputNumberDialog();
        } else {
            dismissEulixSoftInputNumberDialog();
        }
    }

    private void showEulixSoftInputNumberDialog() {
        if (eulixSoftInputNumberDialog != null && !eulixSoftInputNumberDialog.isShowing()) {
            eulixSoftInputNumberDialog.show();
            Window window = eulixSoftInputNumberDialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                window.setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            }
        }
    }

    private void dismissEulixSoftInputNumberDialog() {
        if (eulixSoftInputNumberDialog != null && eulixSoftInputNumberDialog.isShowing()) {
            eulixSoftInputNumberDialog.dismiss();
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.password_1:
                case R.id.password_2:
                case R.id.password_3:
                case R.id.password_4:
                case R.id.password_5:
                case R.id.password_6:
                case R.id.eulix_six_security_password_container:
                    if (mEnable) {
                        showEulixSoftInputNumberDialog();
                    }
                    break;
                case R.id.key_hide:
                    dismissEulixSoftInputNumberDialog();
                    break;
                case R.id.key_1:
                    insertPassword(1);
                    break;
                case R.id.key_2:
                    insertPassword(2);
                    break;
                case R.id.key_3:
                    insertPassword(3);
                    break;
                case R.id.key_4:
                    insertPassword(4);
                    break;
                case R.id.key_5:
                    insertPassword(5);
                    break;
                case R.id.key_6:
                    insertPassword(6);
                    break;
                case R.id.key_7:
                    insertPassword(7);
                    break;
                case R.id.key_8:
                    insertPassword(8);
                    break;
                case R.id.key_9:
                    insertPassword(9);
                    break;
                case R.id.key_0:
                    insertPassword(0);
                    break;
                case R.id.key_backspace:
                    deletePassword();
                    break;
                default:
                    break;
            }
        }
    }
}
