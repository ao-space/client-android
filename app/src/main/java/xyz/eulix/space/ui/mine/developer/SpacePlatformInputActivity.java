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

package xyz.eulix.space.ui.mine.developer;

import android.app.Dialog;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bridge.SpacePlatformEnvironmentInnerBridge;
import xyz.eulix.space.network.agent.platform.SwitchPlatformResult;
import xyz.eulix.space.presenter.SpacePlatformInputPresenter;
import xyz.eulix.space.ui.EulixMainActivity;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/10/17 17:30
 */
public class SpacePlatformInputActivity extends AbsActivity<SpacePlatformInputPresenter.ISpacePlatformInput, SpacePlatformInputPresenter> implements SpacePlatformInputPresenter.ISpacePlatformInput
        , View.OnClickListener, SpacePlatformEnvironmentInnerBridge.SpacePlatformEnvironmentInnerSinkCallback {
    private ImageButton back;
    private TextView title;
    private Button done;
    private EditText eulixCommonEdit;
    private ImageButton eulixCommonFunction;
    private TextView switchSuccessDialogTitle;
    private TextView switchSuccessDialogContent;
    private Button switchSuccessDialogConfirm;
    private Dialog switchSuccessDialog;
    private SpacePlatformEnvironmentInnerBridge mBridge;
    private SpacePlatformInputHandler mHandler;
    private String lastSpacePlatformUrlString;

    static class SpacePlatformInputHandler extends Handler {
        private WeakReference<SpacePlatformInputActivity> spacePlatformInputActivityWeakReference;

        public SpacePlatformInputHandler(SpacePlatformInputActivity activity) {
            spacePlatformInputActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            SpacePlatformInputActivity activity = spacePlatformInputActivityWeakReference.get();
            if (activity == null) {
                super.handleMessage(msg);
            } else {
                switch (msg.what) {
                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        }
    }
    @Override
    public void initView() {
        setContentView(R.layout.activity_eulix_common_input);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        done = findViewById(R.id.function_text);
        eulixCommonEdit = findViewById(R.id.eulix_common_edit);
        eulixCommonFunction = findViewById(R.id.eulix_common_function);

        View switchSuccessDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_one_button_dialog, null);
        switchSuccessDialogTitle = switchSuccessDialogView.findViewById(R.id.dialog_title);
        switchSuccessDialogContent = switchSuccessDialogView.findViewById(R.id.dialog_content);
        switchSuccessDialogConfirm = switchSuccessDialogView.findViewById(R.id.dialog_confirm);
        switchSuccessDialog = new Dialog(this, R.style.EulixDialog);
        switchSuccessDialog.setCancelable(false);
        switchSuccessDialog.setContentView(switchSuccessDialogView);
    }

    @Override
    public void initData() {
        mHandler = new SpacePlatformInputHandler(this);
        mBridge = SpacePlatformEnvironmentInnerBridge.getInstance();
        mBridge.registerSinkCallback(this);
    }

    @Override
    public void initViewData() {
        title.setText(R.string.private_space_platform_address);
        done.setText(R.string.confirm_ok);
        done.setVisibility(View.VISIBLE);
        eulixCommonEdit.setHint(R.string.space_platform_input_hint);
        eulixCommonEdit.setText("");
        eulixCommonFunction.setImageResource(R.drawable.icon_edit_clear_2x);

        switchSuccessDialogTitle.setText(R.string.switch_success);
        switchSuccessDialogConfirm.setText(R.string.confirm_ok);
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        done.setOnClickListener(this);
        eulixCommonFunction.setOnClickListener(this);
        eulixCommonEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_NORMAL);
        eulixCommonEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                ;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                setEulixCommonFunctionVisibility((s != null && s.length() > 0));
            }

            @Override
            public void afterTextChanged(Editable s) {
                ;
            }
        });
        Editable editable = eulixCommonEdit.getText();
        if (editable != null) {
            setEulixCommonFunctionVisibility((!editable.toString().isEmpty()));
        }
        eulixCommonEdit.requestFocus();

        switchSuccessDialogConfirm.setOnClickListener(v -> {
            dismissSwitchSuccessDialog();
            popAllAndGoMain();
        });
    }

    private void setEulixCommonFunctionVisibility(boolean isVisible) {
        eulixCommonFunction.setVisibility(isVisible ? View.VISIBLE : View.GONE);
        done.setTextColor(getResources().getColor(isVisible ? R.color.blue_ff337aff : R.color.gray_ffbcbfcd));
        done.setClickable(isVisible);
    }

    private void showSwitchSuccessDialog() {
        if (switchSuccessDialog != null && !switchSuccessDialog.isShowing()) {
            switchSuccessDialog.show();
            Window window = switchSuccessDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_259), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissSwitchSuccessDialog() {
        if (switchSuccessDialog != null && switchSuccessDialog.isShowing()) {
            switchSuccessDialog.dismiss();
        }
    }

    private void popAllAndGoMain() {
        EulixSpaceApplication.popAllOldActivity(this);
        Intent intent = new Intent(SpacePlatformInputActivity.this, EulixMainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    protected void onDestroy() {
        if (mBridge != null) {
            mBridge.unregisterSinkCallback();
            mBridge = null;
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }

    @Override
    protected int getActivityIndex() {
        return DEVELOPER_SERIES_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public SpacePlatformInputPresenter createPresenter() {
        return new SpacePlatformInputPresenter();
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    finish();
                    break;
                case R.id.function_text:
                    String spacePlatformUrlString = StringUtil.toHttpUrlString(eulixCommonEdit.getText().toString().trim());
                    if (spacePlatformUrlString == null) {
                        showImageTextToast(R.drawable.toast_wrong, R.string.illegal_url_hint);
                    } else if (mBridge != null) {
                        lastSpacePlatformUrlString = spacePlatformUrlString;
                        showLoading(getString(R.string.switch_space_platform_hint));
                        mBridge.handleRequestSwitchPlatform(spacePlatformUrlString);
                    }
                    break;
                case R.id.eulix_common_function:
                    eulixCommonEdit.setText("");
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void handleSwitchPlatformResponse(int code, String source, SwitchPlatformResult result) {
        if (mHandler != null) {
            mHandler.post(() -> {
                closeLoading();
                if (code >= 200 && code < 400 && result != null) {
                    String content = getString(R.string.switch_private_space_platform_hint) + "\n" + StringUtil.nullToEmpty(lastSpacePlatformUrlString);
                    switchSuccessDialogContent.setText(content);
                    showSwitchSuccessDialog();
                } else {
                    boolean isHandleError = false;
                    if (ConstantField.KnownSource.AGENT.equals(source)) {
                        switch (code) {
                            case ConstantField.KnownError.SwitchPlatformError.RESOURCE_BUSY_ERROR:
                                isHandleError = true;
                                showImageTextToast(R.drawable.toast_refuse, R.string.switch_space_platform_resource_busy_error_content);
                                break;
                            case ConstantField.KnownError.SwitchPlatformError.CONNECT_ERROR:
                                isHandleError = true;
                                showImageTextToast(R.drawable.toast_refuse, R.string.switch_space_platform_connect_error_content);
                                break;
                            default:
                                break;
                        }
                    }
                    if (!isHandleError) {
                        String errContent = getString(R.string.switch_fail_hint_part_1) + code + getString(R.string.switch_fail_hint_part_2);
                        showImageTextToast(R.drawable.toast_wrong, errContent);
                    }
                }
            });
        }
    }

    @Override
    public void handleDisconnect() {
        if (mHandler != null) {
            mHandler.post(this::finish);
        }
    }
}
