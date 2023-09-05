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

import android.app.Activity;
import android.content.Intent;
import android.text.Editable;
import android.text.InputType;
import android.text.Selection;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.presenter.EulixCommonInputPresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/7/29 11:04
 */
public class EulixCommonInputActivity extends AbsActivity<EulixCommonInputPresenter.IEulixCommonInput, EulixCommonInputPresenter> implements EulixCommonInputPresenter.IEulixCommonInput, View.OnClickListener {
    public static final String NO_BLANK = "no_blank";
    private ImageButton back;
    private TextView title;
    private Button done;
    private EditText eulixCommonEdit;
    private ImageButton eulixCommonFunction;
    private String inputContent;
    private int inputFunction;
    private int inputType;
    private int imeOptions;
    private boolean inputPrivate;
    private boolean isNoBlank;

    private TextWatcher eulixCommonEditWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Do nothing
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            setEulixCommonFunctionVisibility((s != null && s.length() > 0));
        }

        @Override
        public void afterTextChanged(Editable s) {
            boolean isContainsBlank = (isNoBlank && s != null && StringUtil.containsBlank(s.toString()));
            if (isContainsBlank) {
                String text = s.toString();
                eulixCommonEdit.setText(StringUtil.replaceBlank(text));
                Editable afterEditable = eulixCommonEdit.getText();
                if (afterEditable != null) {
                    Selection.setSelection(afterEditable, afterEditable.length());
                }
            }
        }
    };

    @Override
    public void initView() {
        setContentView(R.layout.activity_eulix_common_input);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        done = findViewById(R.id.function_text);
        eulixCommonEdit = findViewById(R.id.eulix_common_edit);
        eulixCommonFunction = findViewById(R.id.eulix_common_function);
    }

    @Override
    public void initData() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(ConstantField.EulixCommonInputExtra.INPUT_FUNCTION)) {
                inputFunction = intent.getIntExtra(ConstantField.EulixCommonInputExtra.INPUT_FUNCTION, 0);
            }
            if (intent.hasExtra(ConstantField.EulixCommonInputExtra.INPUT_TYPE)) {
                inputType = intent.getIntExtra(ConstantField.EulixCommonInputExtra.INPUT_TYPE, 0);
            }
            if (intent.hasExtra(ConstantField.EulixCommonInputExtra.IME_OPTIONS)) {
                imeOptions = intent.getIntExtra(ConstantField.EulixCommonInputExtra.IME_OPTIONS, 0);
            }
            if (intent.hasExtra(ConstantField.EulixCommonInputExtra.INPUT_CONTENT)) {
                inputContent = intent.getStringExtra(ConstantField.EulixCommonInputExtra.INPUT_CONTENT);
            }
            isNoBlank = intent.getBooleanExtra(NO_BLANK, false);
        }
        switch (inputFunction) {
            case ConstantField.EulixCommonInputExtra.InputFunction.SECURITY_EMAIL_ACCOUNT_FUNCTION:
            case ConstantField.EulixCommonInputExtra.InputFunction.SECURITY_EMAIL_PASSWORD_FUNCTION:
            case ConstantField.EulixCommonInputExtra.InputFunction.SECURITY_EMAIL_SMTP_SERVER_FUNCTION:
            case ConstantField.EulixCommonInputExtra.InputFunction.SECURITY_EMAIL_PORT_FUNCTION:
                setActivityIndex(SECURITY_SERIES_ACTIVITY_INDEX);
                break;
            default:
                break;
        }
    }

    @Override
    public void initViewData() {
        boolean isSetText = true;
        switch (inputFunction) {
            case ConstantField.EulixCommonInputExtra.InputFunction.SECURITY_EMAIL_ACCOUNT_FUNCTION:
                title.setText(R.string.account);
                done.setVisibility(View.VISIBLE);
                done.setText(R.string.done);
                eulixCommonEdit.setHint("example@company.com");
                eulixCommonFunction.setImageResource(R.drawable.icon_edit_clear_2x);
                break;
            case ConstantField.EulixCommonInputExtra.InputFunction.SECURITY_EMAIL_PASSWORD_FUNCTION:
                title.setText(R.string.password);
                done.setVisibility(View.VISIBLE);
                done.setText(R.string.done);
                eulixCommonEdit.setHint(R.string.email_password_or_verification_code);
                setInputPrivate(true, true);
                break;
            case ConstantField.EulixCommonInputExtra.InputFunction.SECURITY_EMAIL_SMTP_SERVER_FUNCTION:
                title.setText(R.string.smtp_server);
                done.setVisibility(View.VISIBLE);
                done.setText(R.string.done);
                eulixCommonEdit.setHint("smtp.company.com");
                eulixCommonFunction.setImageResource(R.drawable.icon_edit_clear_2x);
                break;
            case ConstantField.EulixCommonInputExtra.InputFunction.SECURITY_EMAIL_PORT_FUNCTION:
                title.setText(R.string.port);
                done.setVisibility(View.VISIBLE);
                done.setText(R.string.done);
                eulixCommonFunction.setImageResource(R.drawable.icon_edit_clear_2x);
                break;
            default:
                break;
        }
        switch (inputType) {
            case ConstantField.EulixCommonInputExtra.InputType.NONE:
                eulixCommonEdit.setInputType(InputType.TYPE_NULL);
                isSetText = false;
                break;
            case ConstantField.EulixCommonInputExtra.InputType.TEXT:
                eulixCommonEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
                break;
            case ConstantField.EulixCommonInputExtra.InputType.TEXT_PASSWORD:
                eulixCommonEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                break;
            case ConstantField.EulixCommonInputExtra.InputType.NUMBER:
                eulixCommonEdit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
                inputContent = StringUtil.filterNumber(inputContent);
                break;
            case ConstantField.EulixCommonInputExtra.InputType.NUMBER_PASSWORD:
                eulixCommonEdit.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                inputContent = StringUtil.filterNumber(inputContent);
                break;
            default:
                isSetText = false;
                break;
        }
        switch (imeOptions) {
            case ConstantField.EulixCommonInputExtra.ImeOptions.NORMAL:
                eulixCommonEdit.setImeOptions(EditorInfo.IME_ACTION_NONE);
                break;
            case ConstantField.EulixCommonInputExtra.ImeOptions.ACTION_GO:
                eulixCommonEdit.setImeOptions(EditorInfo.IME_ACTION_GO);
                break;
            case ConstantField.EulixCommonInputExtra.ImeOptions.ACTION_SEARCH:
                eulixCommonEdit.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
                break;
            case ConstantField.EulixCommonInputExtra.ImeOptions.ACTION_SEND:
                eulixCommonEdit.setImeOptions(EditorInfo.IME_ACTION_SEND);
                break;
            case ConstantField.EulixCommonInputExtra.ImeOptions.ACTION_NEXT:
                eulixCommonEdit.setImeOptions(EditorInfo.IME_ACTION_NEXT);
                break;
            case ConstantField.EulixCommonInputExtra.ImeOptions.ACTION_DONE:
                eulixCommonEdit.setImeOptions(EditorInfo.IME_ACTION_DONE);
                break;
            case ConstantField.EulixCommonInputExtra.ImeOptions.ACTION_PREVIOUS:
                eulixCommonEdit.setImeOptions(EditorInfo.IME_ACTION_PREVIOUS);
                break;
            default:
                break;
        }
        if (isSetText && inputContent != null) {
            eulixCommonEdit.setText(inputContent);
        }
        Editable editable = eulixCommonEdit.getText();
        if (editable != null) {
            setEulixCommonFunctionVisibility((!editable.toString().isEmpty()));
        }
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        done.setOnClickListener(this);
        eulixCommonFunction.setOnClickListener(this);
        eulixCommonEdit.addTextChangedListener(eulixCommonEditWatcher);
        eulixCommonEdit.requestFocus();
    }

    private void setEulixCommonFunctionVisibility(boolean isVisible) {
        eulixCommonFunction.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    private void setInputPrivate(boolean isPrivate, boolean isInit) {
        inputPrivate = isPrivate;
        eulixCommonFunction.setImageResource(isPrivate ? R.drawable.icon_private_2x : R.drawable.icon_public_2x);
        if (!isInit) {
            eulixCommonEdit.removeTextChangedListener(eulixCommonEditWatcher);
        }
        eulixCommonEdit.setInputType(isPrivate ? (InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                : InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
        if (!isInit) {
            eulixCommonEdit.setSelection(eulixCommonEdit.getText().length());
            eulixCommonEdit.addTextChangedListener(eulixCommonEditWatcher);
        }
    }

    private void handleEulixCommonFunction() {
        switch (inputFunction) {
            case ConstantField.EulixCommonInputExtra.InputFunction.SECURITY_EMAIL_ACCOUNT_FUNCTION:
            case ConstantField.EulixCommonInputExtra.InputFunction.SECURITY_EMAIL_SMTP_SERVER_FUNCTION:
            case ConstantField.EulixCommonInputExtra.InputFunction.SECURITY_EMAIL_PORT_FUNCTION:
                eulixCommonEdit.setText("");
                break;
            case ConstantField.EulixCommonInputExtra.InputFunction.SECURITY_EMAIL_PASSWORD_FUNCTION:
                setInputPrivate(!inputPrivate, false);
                break;
            default:
                break;
        }
    }

    private void handleResult(boolean isOk) {
        Intent intent = new Intent();
        setResult(isOk ? Activity.RESULT_OK : Activity.RESULT_CANCELED, intent);
        intent.putExtra(ConstantField.EulixCommonInputExtra.INPUT_FUNCTION, inputFunction);
        String content = eulixCommonEdit.getText().toString();
        intent.putExtra(ConstantField.EulixCommonInputExtra.INPUT_CONTENT, (isNoBlank ? StringUtil.replaceBlank(content) : content));
        finish();
    }

    @Override
    public void onBackPressed() {
        handleResult(false);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            handleResult(false);
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @NotNull
    @Override
    public EulixCommonInputPresenter createPresenter() {
        return new EulixCommonInputPresenter();
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    handleResult(false);
                    break;
                case R.id.function_text:
                    handleResult(true);
                    break;
                case R.id.eulix_common_function:
                    handleEulixCommonFunction();
                    break;
                default:
                    break;
            }
        }
    }
}
