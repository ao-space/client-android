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

package xyz.eulix.space.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;

import java.util.ArrayList;

import xyz.eulix.space.R;
import xyz.eulix.space.util.ViewUtils;

/**
 * Author:      Zhu Fuyu
 * Description: 验证码输入框
 * History:     2021/10/25
 */
public class VerificationCodeInput extends LinearLayout implements TextWatcher, View.OnKeyListener {
    private Context context;
    private AttributeSet attrs;

    private int mItemWidth = 0;
    private int mItemHeight = 0;
    private float mTextSize = 0f;
    private int mTextTopBottomPadding = 0;
    private int mTextTopLeftRight = 0;

    private int childHPadding = 0;
    private int childVPadding = 0;

    private Drawable mBgNormal = null;
    private Drawable mBgFocus = null;

    private ArrayList<EditText> mEditTextList = new ArrayList<>();
    private int currentPosition = 0;

    private int mInputCount = 6;
    private boolean mShowSoft = true;

    private boolean isSoftInit = false;

    private CompleteListener listener = null;

    public VerificationCodeInput(Context context) {
        this(context, null);
    }

    public VerificationCodeInput(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerificationCodeInput(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        this.attrs = attrs;
        init();
    }

    private void init() {
        initAttrs();
        initScale();
        initView();
    }

    private void initAttrs() {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.VerificationCodeInput);
        mInputCount = ta.getInt(R.styleable.VerificationCodeInput_input_count, 6);
        mShowSoft = ta.getBoolean(R.styleable.VerificationCodeInput_show_soft, true);
        ta.recycle();
    }

    private void initSoft(EditText editText) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            InputMethodManager inputManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputManager.showSoftInput(editText, InputMethodManager.SHOW_FORCED);
            isSoftInit = true;
            editText.requestFocus();
        }, 500);

    }

    private void initScale() {
        mItemWidth = ViewUtils.dp2px(context, 38);
        mItemHeight = ViewUtils.dp2px(context, 55);

        childHPadding = ViewUtils.dp2px(context, 10);

        int textSizePix = context.getResources().getDimensionPixelSize(R.dimen.dp_30);
        mTextSize = (float) ViewUtils.px2dp(context, (float) textSizePix);

        mTextTopBottomPadding = ViewUtils.dp2px(context, 0);
        mTextTopLeftRight = ViewUtils.dp2px(context, 5);

        mBgNormal = context.getResources().getDrawable(R.drawable.verification_edit_bg_normal);
        mBgFocus = context.getResources().getDrawable(R.drawable.verification_edit_bg_focus);
    }

    private void initView() {
        for (int i = 0; i < mInputCount; i++) {
            EditText editText = new EditText(context);
            if (!isSoftInit && i == 0 && mShowSoft) {
                initSoft(editText);
            }
            LayoutParams layoutP = new LayoutParams(mItemWidth, mItemHeight);
            layoutP.bottomMargin = mTextTopBottomPadding;
            layoutP.topMargin = mTextTopBottomPadding;
            layoutP.leftMargin = mTextTopLeftRight;
            layoutP.rightMargin = mTextTopLeftRight;
            layoutP.gravity = Gravity.CENTER;

            editText.setOnKeyListener(this);
            if (i == 0) {
                setBg(editText, true);
            } else {
                setBg(editText, false);
            }

            editText.setTextSize(mTextSize);
            editText.setTextColor(Color.BLACK);
            editText.setGravity(Gravity.CENTER);
            editText.setLayoutParams(layoutP);
            editText.setInputType(InputType.TYPE_CLASS_NUMBER);
//            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            editText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(1)});
            editText.addTextChangedListener(this);

            addView(editText, i);
            mEditTextList.add(editText);
        }
    }

    private void setBg(EditText editText, boolean focus) {
        if (mBgNormal != null && !focus) {
            editText.setBackground(mBgNormal);
        } else if (mBgFocus != null && focus) {
            editText.setBackground(mBgFocus);
        }
    }

    /**
     * edittext获取焦点
     */
    private void focus() {
        int count = getChildCount();
        EditText editText;
        for (int i = 0; i < count; i++) {
            editText = (EditText) getChildAt(i);
            if (editText.getText().toString().isEmpty()) {
                editText.requestFocus();
                return;
            }
        }
    }

    /**
     * 完成n个数字后回调
     */
    private void commit() {
        StringBuilder sb = new StringBuilder();      // 用于存储已输入验证码
        boolean isFull = true;
        for (int i = 0; i < getChildCount(); i++) {
            EditText editText = (EditText) getChildAt(i);
            String text = editText.getText().toString();
            if (TextUtils.isEmpty(text)) {
                isFull = false;
                break;
            } else {
                sb.append(text);
            }
        }
        if (isFull && listener != null) {
            listener.onCompleted(sb.toString());
        }
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        if (start == 0 && count >= 1 && currentPosition != mEditTextList.size() - 1) {
            currentPosition++;
            mEditTextList.get(currentPosition).requestFocus();
            setBg(mEditTextList.get(currentPosition), true);
            setBg(mEditTextList.get(currentPosition - 1), false);
            mEditTextList.get(currentPosition - 1).setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_TEXT_VARIATION_PASSWORD);
//            mEditTextList.get(currentPosition).setInputType(InputType.TYPE_CLASS_NUMBER| InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }

    }

    @Override
    public void afterTextChanged(Editable s) {
        if (null != s) {
            focus();
            commit();
        }
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        EditText editText = (EditText) v;
        if (keyCode == KeyEvent.KEYCODE_DEL && TextUtils.isEmpty(editText.getText().toString())) {
            int action = event.getAction();
            if (currentPosition != 0 && action == KeyEvent.ACTION_DOWN) {
                currentPosition--;
                mEditTextList.get(currentPosition).requestFocus();
                setBg(mEditTextList.get(currentPosition), true);
                setBg(mEditTextList.get(currentPosition + 1), false);
                mEditTextList.get(currentPosition).setText("");
            }
        }
        return false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int count = getChildCount();
        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            this.measureChild(child, widthMeasureSpec, heightMeasureSpec);
        }
        if (getChildCount() > 0) {
            View child = getChildAt(0);
            int cH = child.getMeasuredHeight();
            int cW = child.getMeasuredWidth();
            int maxH = cH + 2 * childVPadding;
            int maxW = (cW + childHPadding) * mInputCount - childHPadding;  // 减去最后一个padding
            setMeasuredDimension(resolveSize(maxW, widthMeasureSpec),
                    resolveSize(maxH, heightMeasureSpec));
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);

            child.setVisibility(View.VISIBLE);
            int cWidth = child.getMeasuredWidth();
            int cHeight = child.getMeasuredHeight();
            int cl = (i) * (cWidth + childHPadding);
            int cr = cl + cWidth;
            int ct = childVPadding;
            int cb = ct + cHeight;
            child.layout(cl, ct, cr, cb);
        }
    }

    /**
     * 输入完成后disable所有EditText
     */
    @Override
    public void setEnabled(boolean enabled) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.setEnabled(enabled);
        }
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
        return new LayoutParams(context, attrs);
    }

    public void setOnCompleteListener(CompleteListener listener) {
        this.listener = listener;
    }

    //获取当前数据
    public String getCurrentText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < getChildCount(); i++) {
            EditText editText = (EditText) getChildAt(i);
            String text = editText.getText().toString();
            if (TextUtils.isEmpty(text)) {
                break;
            } else {
                sb.append(text);
            }
        }
        return sb.toString();
    }

    //清空数据
    public void clearAll() {
        removeAllViews();
        isSoftInit = false;
        mEditTextList.clear();
        currentPosition = 0;
        initView();
        mEditTextList.get(0).requestFocus();
    }


    public interface CompleteListener {
        void onCompleted(String string);
    }
}
