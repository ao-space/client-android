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

import android.content.Context;
import android.content.res.TypedArray;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import androidx.annotation.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import xyz.eulix.space.R;
import xyz.eulix.space.util.Logger;

/**
 * Author:      Zhu Fuyu
 * Description: 通用输入框
 * History:     2022/8/18
 */
public class NormalEditInputView extends RelativeLayout {
    private EditText etPassword;
    private ImageView imgClean;
    private ImageView imgHide;
    private InputChangeListener mListener;

    private boolean isPwdStyle = false;
    private int mMaxLength = 1000;
    private boolean mHideFlag = true;
    private String mHintText;

    private boolean isViewEnable = true;


    private String regEx = "[^a-zA-Z0-9]";

    //非密码模式下自定义规则
    private String mCustomRegular = null;

    public NormalEditInputView(Context context) {
        this(context, null);
    }

    public NormalEditInputView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NormalEditInputView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(context, attrs);
        initView(context);
    }

    //属性
    private void initAttrs(Context context, AttributeSet attrs) {
        TypedArray typeArray = context.getTheme().obtainStyledAttributes(attrs,
                R.styleable.NormalPasswordInputView, 0, 0);
        mHintText = typeArray.getString(R.styleable.NormalPasswordInputView_inputHint);
        isPwdStyle = typeArray.getBoolean(R.styleable.NormalPasswordInputView_isPwdStyle, false);
        mMaxLength = typeArray.getInteger(R.styleable.NormalPasswordInputView_inputMaxLength, 1000);
        typeArray.recycle();
    }

    private void initView(Context context) {
        ViewGroup viewGroup = (ViewGroup) LayoutInflater.from(context).inflate(R.layout.layout_normal_password_input_view, this);
        etPassword = viewGroup.findViewById(R.id.et_input);
        imgClean = viewGroup.findViewById(R.id.img_clean);
        imgHide = viewGroup.findViewById(R.id.img_hide);

        if (!TextUtils.isEmpty(mHintText)) {
            etPassword.setHint(mHintText);
        }
        if (isPwdStyle) {
            imgHide.setVisibility(VISIBLE);
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        } else {
            imgHide.setVisibility(GONE);
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        }

        etPassword.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && etPassword.getText().length() > 0) {
                    imgClean.setVisibility(VISIBLE);
                } else {
                    imgClean.setVisibility(GONE);
                }
            }
        });
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (isPwdStyle) {
                    String editableStr = etPassword.getText().toString();
                    String str = stringFilter(editableStr, regEx);
                    if (!editableStr.equals(str)) {
                        etPassword.setText(str);
                        int selectionIndex = Math.min(str.length(), mMaxLength);
                        etPassword.setSelection(selectionIndex); //设置光标位置
                    }
                } else {
                    if (!TextUtils.isEmpty(mCustomRegular)) {
                        String editableStr = etPassword.getText().toString();
                        String str = stringFilter(editableStr, mCustomRegular);
                        if (!editableStr.equals(str)) {
                            etPassword.setText(str);
                            int selectionIndex = Math.min(str.length(), mMaxLength);
                            etPassword.setSelection(selectionIndex); //设置光标位置
                        }
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                imgClean.setVisibility(s.length() > 0 ? VISIBLE : GONE);

                if (s.length() > mMaxLength) {
                    int selectionStart = etPassword.getSelectionStart();
                    int selectionEnd = etPassword.getSelectionEnd();
                    if (selectionStart > 0 && selectionEnd > selectionStart) {
                        s.delete(selectionStart - 1, selectionEnd);
                        etPassword.setSelection(selectionStart);
                        etPassword.setText(s);
                    } else {
                        s.delete(mMaxLength, s.toString().length());
                        if (s.toString().length() <= mMaxLength) {
                            etPassword.setSelection(s.toString().length());
                            etPassword.setText(s);
                        }
                    }
                    etPassword.setSelection(s.toString().length());
                    Logger.d("zfy", "您的输入已超过" + mMaxLength + "位");
                }
                if (mListener != null) {
                    mListener.onInputChange(etPassword.getText().toString());
                }
            }
        });

        imgClean.setOnClickListener(v -> {
            if (isViewEnable) {
                etPassword.setText("");
            }
        });

        imgHide.setOnClickListener(v -> {
            mHideFlag = !mHideFlag;
            if (mHideFlag) {
                //暗文
                imgHide.setImageResource(R.drawable.icon_pwd_hide);
                etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            } else {
                //明文
                imgHide.setImageResource(R.drawable.icon_pwd_show);
                etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            }
            etPassword.setSelection(etPassword.getText().length());
        });
    }

    //设置hint
    public void setHint(String hintText) {
        etPassword.setHint(hintText);
    }

    //设置当前文本内容
    public void setContentText(String contentText) {
        etPassword.setText(contentText);
    }

    //设置最大长度
    public void setMaxLength(int maxLength) {
        this.mMaxLength = maxLength;
    }

    //是指输入内容限制
    public void setDigits(String digits) {
        if (TextUtils.isEmpty(digits)) {
            return;
        }
        etPassword.setKeyListener(DigitsKeyListener.getInstance(digits));
    }

    public void setText(String content){
        etPassword.setText(content);
    }

    //清空已输入内容
    public void clearInput() {
        etPassword.setText("");
    }

    public String getInputText() {
        return etPassword.getText().toString();
    }

    public void setInputChangeListener(InputChangeListener listener) {
        this.mListener = listener;
    }

    public void setCustomRegular(String customRegular) {
        this.mCustomRegular = customRegular;
    }

    public void setViewEnable(boolean isEnable) {
        etPassword.setEnabled(isEnable);
        this.isViewEnable = isEnable;
    }

    public interface InputChangeListener {
        void onInputChange(String inputText);
    }

    private String stringFilter(String str, String regular) {
        Pattern pattern = Pattern.compile(regular);
        Matcher matcher = pattern.matcher(str);
        return matcher.replaceAll("").trim();
    }

}
