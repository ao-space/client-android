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
import xyz.eulix.space.presenter.EulixCommonInputMatchPresenter;
import xyz.eulix.space.presenter.EulixCommonInputPresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2023/1/9 9:27
 */
public class EulixCommonInputMatchActivity extends AbsActivity<EulixCommonInputMatchPresenter.IEulixCommonInputMatch, EulixCommonInputMatchPresenter> implements EulixCommonInputPresenter.IEulixCommonInput, View.OnClickListener {
    public static final String NO_BLANK = "no_blank";
    private ImageButton back;
    private TextView title;
    private Button done;
    private EditText eulixCommonEdit;
    private ImageButton eulixCommonFunction;
    private TextView eulixCommonHint;
    private int commonInputId;
    private String commonTitle;
    private String inputContent;
    private int inputType;
    private int imeOptions;
    private String inputHint;
    private String inputTextMatch;
    private int inputCommonFunction;
    private boolean inputPrivate;
    private boolean isNoBlank;
    private int inputCharacterMinLength = -1;
    private int inputCharacterMaxLength = -1;
    private String commonHint;

    private TextWatcher eulixCommonEditWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // Do nothing
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            boolean isNotEmpty = (s != null && s.length() > 0);
            setDonePattern(isNotEmpty);
            setEulixCommonFunctionVisibility(isNotEmpty);
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
        setContentView(R.layout.activity_eulix_common_input_match);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        done = findViewById(R.id.function_text);
        eulixCommonEdit = findViewById(R.id.eulix_common_edit);
        eulixCommonFunction = findViewById(R.id.eulix_common_function);
        eulixCommonHint = findViewById(R.id.eulix_common_hint);
    }

    @Override
    public void initData() {
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(ConstantField.EulixCommonInputExtra.COMMON_INPUT_ID)) {
                commonInputId = intent.getIntExtra(ConstantField.EulixCommonInputExtra.COMMON_INPUT_ID, 0);
            }
            if (intent.hasExtra(ConstantField.EulixCommonInputExtra.COMMON_TITLE)) {
                commonTitle = intent.getStringExtra(ConstantField.EulixCommonInputExtra.COMMON_TITLE);
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
            if (intent.hasExtra(ConstantField.EulixCommonInputExtra.INPUT_HINT)) {
                inputHint = intent.getStringExtra(ConstantField.EulixCommonInputExtra.INPUT_HINT);
            }
            if (intent.hasExtra(ConstantField.EulixCommonInputExtra.INPUT_TEXT_MATCH)) {
                inputTextMatch = intent.getStringExtra(ConstantField.EulixCommonInputExtra.INPUT_TEXT_MATCH);
            }
            if (intent.hasExtra(ConstantField.EulixCommonInputExtra.INPUT_COMMON_FUNCTION)) {
                inputCommonFunction = intent.getIntExtra(ConstantField.EulixCommonInputExtra.INPUT_COMMON_FUNCTION, 0);
            }
            if (intent.hasExtra(ConstantField.EulixCommonInputExtra.COMMON_HINT)) {
                commonHint = intent.getStringExtra(ConstantField.EulixCommonInputExtra.COMMON_HINT);
            }
            isNoBlank = intent.getBooleanExtra(NO_BLANK, false);
            if (intent.hasExtra(ConstantField.EulixCommonInputExtra.INPUT_CHARACTER_MIN_LENGTH)) {
                inputCharacterMinLength = intent.getIntExtra(ConstantField.EulixCommonInputExtra.INPUT_CHARACTER_MIN_LENGTH, -1);
            }
            if (intent.hasExtra(ConstantField.EulixCommonInputExtra.INPUT_CHARACTER_MAX_LENGTH)) {
                inputCharacterMaxLength = intent.getIntExtra(ConstantField.EulixCommonInputExtra.INPUT_CHARACTER_MAX_LENGTH, -1);
            }
        }
    }

    @Override
    public void initViewData() {
        title.setText(StringUtil.nullToEmpty(commonTitle));
        done.setVisibility(View.VISIBLE);
        done.setText(R.string.done);
        switch (inputCommonFunction) {
            case ConstantField.EulixCommonInputExtra.InputCommonFunction.COMMON_CLEAR:
                eulixCommonFunction.setImageResource(R.drawable.icon_edit_clear_2x);
                break;
            case ConstantField.EulixCommonInputExtra.InputCommonFunction.COMMON_PRIVATE:
                setInputPrivate(true, true);
                break;
            default:
                break;
        }
        if (StringUtil.isNonBlankString(commonHint)) {
            eulixCommonHint.setVisibility(View.VISIBLE);
            eulixCommonHint.setText(commonHint);
        } else {
            eulixCommonHint.setText("");
            eulixCommonHint.setVisibility(View.GONE);
        }
        boolean isSetText = true;
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
        eulixCommonEdit.setHint(StringUtil.nullToEmpty(inputHint));
        if (isSetText && inputContent != null) {
            eulixCommonEdit.setText(inputContent);
        }
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        done.setOnClickListener(this);
        eulixCommonFunction.setOnClickListener(this);
        Editable editable = eulixCommonEdit.getText();
        boolean isNotEmpty = false;
        if (editable != null) {
            isNotEmpty = (!editable.toString().isEmpty());
        }
        setDonePattern(isNotEmpty);
        setEulixCommonFunctionVisibility(isNotEmpty);
        eulixCommonEdit.addTextChangedListener(eulixCommonEditWatcher);
        eulixCommonEdit.requestFocus();
    }

    private void setDonePattern(boolean isClick) {
        if (done != null) {
            done.setClickable(isClick);
            done.setTextColor(getResources().getColor(isClick ? R.color.blue_ff337aff : R.color.gray_ffcecece));
        }
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
        switch (inputCommonFunction) {
            case ConstantField.EulixCommonInputExtra.InputCommonFunction.COMMON_CLEAR:
                eulixCommonEdit.setText("");
                break;
            case ConstantField.EulixCommonInputExtra.InputCommonFunction.COMMON_PRIVATE:
                setInputPrivate(!inputPrivate, false);
                break;
            default:
                break;
        }
    }

    private void handleResult(boolean isOk, String content) {
        Intent intent = new Intent();
        setResult(isOk ? Activity.RESULT_OK : Activity.RESULT_CANCELED, intent);
        intent.putExtra(ConstantField.EulixCommonInputExtra.COMMON_INPUT_ID, commonInputId);
        if (content != null) {
            intent.putExtra(ConstantField.EulixCommonInputExtra.INPUT_CONTENT, (isNoBlank ? StringUtil.replaceBlank(content) : content));
        }
        finish();
    }

    @Override
    public void onBackPressed() {
        handleResult(false, null);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            handleResult(false, null);
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
    public EulixCommonInputMatchPresenter createPresenter() {
        return new EulixCommonInputMatchPresenter();
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    handleResult(false, null);
                    break;
                case R.id.function_text:
                    String content = eulixCommonEdit.getText().toString();
                    if ((!StringUtil.isNonBlankString(inputTextMatch) || StringUtil.checkTextMatch(content, inputTextMatch))
                            && StringUtil.checkCharacterLength(content, inputCharacterMinLength, inputCharacterMaxLength)) {
                        handleResult(true, content);
                    } else {
                        showImageTextToast(R.drawable.toast_refuse, R.string.name_not_match_hint);
                    }
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
