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

package xyz.eulix.space.ui.mine.security;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.Selection;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.UUID;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bridge.SecurityPasswordBridge;
import xyz.eulix.space.event.DeviceHardwareInfoRequestEvent;
import xyz.eulix.space.event.DeviceHardwareInfoResponseEvent;
import xyz.eulix.space.event.ForgetPasswordResultEvent;
import xyz.eulix.space.event.SecurityMessagePollRequestEvent;
import xyz.eulix.space.event.SecurityMessagePollResponseEvent;
import xyz.eulix.space.event.SecurityOperationResultEvent;
import xyz.eulix.space.manager.AODeviceDiscoveryManager;
import xyz.eulix.space.network.EulixBaseResponse;
import xyz.eulix.space.network.agent.NewDeviceResetPasswordEntity;
import xyz.eulix.space.network.security.SecurityMessagePollResult;
import xyz.eulix.space.network.security.SecurityTokenResult;
import xyz.eulix.space.presenter.ModifySecurityPasswordPresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.view.dialog.security.GranteeSecurityRequestDialog;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/7/7 18:41
 */
public class ModifySecurityPasswordActivity extends AbsActivity<ModifySecurityPasswordPresenter.IModifySecurityPassword, ModifySecurityPasswordPresenter> implements ModifySecurityPasswordPresenter.IModifySecurityPassword
        , View.OnClickListener, AODeviceDiscoveryManager.AODeviceDiscoverySinkCallback, SecurityPasswordBridge.SecurityPasswordSinkCallback {
    private static final int GRANTEE_APPLY = 1;
    private final int PASSWORD_LENGTH = 6;
    private String activityId;
    private ImageButton back;
    private TextView title;
    private TextView function;
    private LinearLayout oldPasswordContainer;
    private EditText oldPasswordEdit;
    private View oldPasswordSplit;
    private EditText newPasswordEdit;
    private EditText repeatPasswordEdit;
    private TextView forgetPassword;
    private Dialog operationFailDialog;
    private TextView operationFailDialogTitle;
    private TextView operationFailDialogContent;
    private Button operationFailDialogConfirm;
    private ModifySecurityPasswordHandler mHandler;
    private InputMethodManager inputMethodManager;
    private boolean isResetPassword;
    private SecurityPasswordBridge mBridge;
    private AODeviceDiscoveryManager mManager;
    private boolean isBluetooth;
    private String baseUrl;
    private String bleKey;
    private String bleIv;
    private String forgetPasswordUuid;
    private Boolean isSecurityFinish;
    private boolean isStart;
    private int securityFunction;
    private int authenticationFunction;
    private String authenticationUuid;
    private String granterClientUuid;
    private String securityToken;
    private long securityTokenExpireTimestamp;
    private SecurityTokenResult mGranterSecurityTokenResult;
    private String granterSecurityToken;
    private long granterSecurityTokenExpireTimestamp;
    private GranteeSecurityRequestDialog granteeSecurityRequestDialog;
    private GranteeSecurityRequestDialog.GranteeSecurityRequestCallback granteeSecurityRequestCallback = null;
    private String granteeApplyId;
    private boolean isOperationExpire;
    private Boolean isOperationExpireGranter;
    private Boolean needGranterDataUuid;
    private String deviceHardwareInfoRequestUuid;
    private String mSecurityPassword;

    static class ModifySecurityPasswordHandler extends Handler {
        private WeakReference<ModifySecurityPasswordActivity> modifySecurityPasswordActivityWeakReference;

        public ModifySecurityPasswordHandler(ModifySecurityPasswordActivity activity) {
            modifySecurityPasswordActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            ModifySecurityPasswordActivity activity = modifySecurityPasswordActivityWeakReference.get();
            if (activity == null) {
                super.handleMessage(msg);
            } else {
                switch (msg.what) {
                    case GRANTEE_APPLY:
                        activity.granteeApplyId = null;
                        if (activity.granteeSecurityRequestDialog != null) {
                            activity.granteeSecurityRequestDialog.handleGranteeRequestResult(GranteeSecurityRequestDialog.REQUEST_EXPIRE);
                        }
                        break;
                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        }
    }

    @Override
    public void initView() {
        setContentView(R.layout.activity_modify_security_password);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        function = findViewById(R.id.function_text);
        oldPasswordContainer = findViewById(R.id.old_password_container);
        oldPasswordEdit = findViewById(R.id.old_password_edit);
        oldPasswordSplit = findViewById(R.id.old_password_split);
        newPasswordEdit = findViewById(R.id.new_password_edit);
        repeatPasswordEdit = findViewById(R.id.repeat_password_edit);
        forgetPassword = findViewById(R.id.forget_password);

        View operationFailDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_one_button_dialog, null);
        operationFailDialogTitle = operationFailDialogView.findViewById(R.id.dialog_title);
        operationFailDialogContent = operationFailDialogView.findViewById(R.id.dialog_content);
        operationFailDialogConfirm = operationFailDialogView.findViewById(R.id.dialog_confirm);
        operationFailDialog = new Dialog(this, R.style.EulixDialog);
        operationFailDialog.setCancelable(false);
        operationFailDialog.setContentView(operationFailDialogView);
    }

    @Override
    public void initData() {
        isOperationExpire = false;
        isOperationExpireGranter = null;
        isResetPassword = false;
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra(ConstantField.RESET_PASSWORD)) {
                isResetPassword = intent.getBooleanExtra(ConstantField.RESET_PASSWORD, false);
                isBluetooth = intent.getBooleanExtra("bluetooth", false);
                if (intent.hasExtra(ConstantField.BASE_URL)) {
                    baseUrl = intent.getStringExtra(ConstantField.BASE_URL);
                }
                if (intent.hasExtra(ConstantField.BLE_KEY)) {
                    bleKey = intent.getStringExtra(ConstantField.BLE_KEY);
                }
                if (intent.hasExtra(ConstantField.BLE_IV)) {
                    bleIv = intent.getStringExtra(ConstantField.BLE_IV);
                }
                forgetPasswordUuid = intent.getStringExtra(ConstantField.FORGET_PASSWORD_UUID);
                authenticationUuid = intent.getStringExtra(ConstantField.AUTHENTICATION_UUID);
                authenticationFunction = intent.getIntExtra(ConstantField.AUTHENTICATION_FUNCTION, 0);
                securityFunction = intent.getIntExtra(ConstantField.SECURITY_FUNCTION, 0);
                if (intent.hasExtra(ConstantField.GRANTER_CLIENT_UUID)) {
                    granterClientUuid = intent.getStringExtra(ConstantField.GRANTER_CLIENT_UUID);
                }
                securityToken = null;
                securityTokenExpireTimestamp = -1L;
                if (intent.hasExtra(ConstantField.DATA_UUID)) {
                    String dataUuid = intent.getStringExtra(ConstantField.DATA_UUID);
                    if (dataUuid != null) {
                        String data = DataUtil.getData(dataUuid);
                        SecurityTokenResult securityTokenResult = null;
                        if (data != null) {
                            try {
                                securityTokenResult = new Gson().fromJson(data, SecurityTokenResult.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        if (securityTokenResult != null) {
                            securityToken = securityTokenResult.getSecurityToken();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                securityTokenExpireTimestamp = FormatUtil.parseZonedDateTime(securityTokenResult.getExpiredAt());
                            } else {
                                securityTokenExpireTimestamp = FormatUtil.parseZonedTimestamp(securityTokenResult.getExpiredAt()
                                        , ConstantField.TimeStampFormat.FILE_API_DAY_FORMAT, ConstantField.TimeStampFormat.FILE_API_SPLIT);
                            }
                        }
                    }
                }
            }
            granterSecurityToken = null;
            granterSecurityTokenExpireTimestamp = -1L;
            if (intent.hasExtra(ConstantField.GRANTER_DATA_UUID)) {
                String granterDataUuid = intent.getStringExtra(ConstantField.GRANTER_DATA_UUID);
                if (granterDataUuid != null) {
                    String granterData = DataUtil.getData(granterDataUuid);
                    mGranterSecurityTokenResult = null;
                    if (granterData != null) {
                        try {
                            mGranterSecurityTokenResult = new Gson().fromJson(granterData, SecurityTokenResult.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                    if (mGranterSecurityTokenResult != null) {
                        granterSecurityToken = mGranterSecurityTokenResult.getSecurityToken();
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            granterSecurityTokenExpireTimestamp = FormatUtil.parseZonedDateTime(mGranterSecurityTokenResult.getExpiredAt());
                        } else {
                            granterSecurityTokenExpireTimestamp = FormatUtil.parseZonedTimestamp(mGranterSecurityTokenResult.getExpiredAt()
                                    , ConstantField.TimeStampFormat.FILE_API_DAY_FORMAT, ConstantField.TimeStampFormat.FILE_API_SPLIT);
                        }
                    }
                }
            }
        }
        activityId = UUID.randomUUID().toString();
        mManager = AODeviceDiscoveryManager.getInstance();
        mManager.registerCallback(activityId, this);
        if (authenticationFunction == ConstantField.AuthenticationFunction.HARDWARE_DEVICE || securityFunction == ConstantField.SecurityFunction.NewDeviceSecurityFunction.VERIFY_SECURITY_MAILBOX) {
            mBridge = SecurityPasswordBridge.getInstance();
            mBridge.registerSinkCallback(this);
        }
        if (!isResetPassword) {
            forgetPasswordUuid = UUID.randomUUID().toString();
        }
        isSecurityFinish = null;
        mHandler = new ModifySecurityPasswordHandler(this);
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Override
    public void initViewData() {
        title.setText(isResetPassword ? R.string.security_password_setting : R.string.modify_security_password);
        function.setText(R.string.done);
        function.setVisibility(View.VISIBLE);
        oldPasswordContainer.setVisibility(isResetPassword ? View.GONE : View.VISIBLE);
        oldPasswordSplit.setVisibility(isResetPassword ? View.GONE : View.VISIBLE);
        forgetPassword.setVisibility(isResetPassword ? View.GONE : View.VISIBLE);
        operationFailDialogTitle.setText(R.string.operate_fail);
        operationFailDialogConfirm.setText(R.string.ok);
    }

    @Override
    public void initEvent() {
        back.setOnClickListener(this);
        function.setOnClickListener(this);
        operationFailDialogConfirm.setOnClickListener(v -> {
            dismissOperationDialog();
            if (isOperationExpire) {
                handleBack(false, (isOperationExpireGranter != null && !isOperationExpireGranter));
            }
        });
        if (!isResetPassword) {
            forgetPassword.setOnClickListener(this);
        }
        checkPasswordLength();
        if (!isResetPassword) {
            oldPasswordEdit.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                    // Do nothing
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    checkPasswordLength();
                }

                @Override
                public void afterTextChanged(Editable s) {
                    if (s.length() > PASSWORD_LENGTH) {
//                        forceHideSoftInput();
                        oldPasswordEdit.setText(s.subSequence(0, PASSWORD_LENGTH));
                        Selection.setSelection(oldPasswordEdit.getText(), PASSWORD_LENGTH);
//                        showImageTextToast(R.drawable.toast_wrong, R.string.password_six_number_hint);
                    }
                }
            });
        }
        newPasswordEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkPasswordLength();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > PASSWORD_LENGTH) {
//                    forceHideSoftInput();
                    newPasswordEdit.setText(s.subSequence(0, PASSWORD_LENGTH));
                    Selection.setSelection(newPasswordEdit.getText(), PASSWORD_LENGTH);
//                    showImageTextToast(R.drawable.toast_wrong, R.string.password_six_number_hint);
                }
            }
        });
        repeatPasswordEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkPasswordLength();
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > PASSWORD_LENGTH) {
//                    forceHideSoftInput();
                    repeatPasswordEdit.setText(s.subSequence(0, PASSWORD_LENGTH));
                    Selection.setSelection(repeatPasswordEdit.getText(), PASSWORD_LENGTH);
//                    showImageTextToast(R.drawable.toast_wrong, R.string.password_six_number_hint);
                }
            }
        });
        if (presenter != null) {
            int identity = presenter.getActiveIdentity();
            if (identity == ConstantField.UserIdentity.ADMINISTRATOR_GRANTEE) {
                granteeSecurityRequestCallback = new GranteeSecurityRequestDialog.GranteeSecurityRequestCallback() {
                    @Override
                    public void cancelRequest() {
                        handleResetApplyId();
                    }

                    @Override
                    public void handleRequestCode(int requestCode) {
                        // Do nothing
                    }
                };
                granteeSecurityRequestDialog = new GranteeSecurityRequestDialog(this, granteeSecurityRequestCallback, false);
            }
        }
    }

    private void checkPasswordLength() {
        if ((isResetPassword || oldPasswordEdit != null) && newPasswordEdit != null && repeatPasswordEdit != null) {
            setDonePattern(/*((isResetPassword || PASSWORD_LENGTH == oldPasswordEdit.getText().length())
                    && PASSWORD_LENGTH == newPasswordEdit.getText().length()
                    && repeatPasswordEdit.getText().length() == newPasswordEdit.getText().length())*/ true, true);
        }
    }

    private void setDonePattern(boolean isVisible, boolean isEnable) {
//        if (function != null) {
//            function.setVisibility(isVisible ? View.VISIBLE : View.GONE);
//            function.setClickable(isEnable);
//        }
        if (!isResetPassword && oldPasswordEdit != null) {
            oldPasswordEdit.setEnabled(isEnable);
        }
        if (newPasswordEdit != null) {
            newPasswordEdit.setEnabled(isEnable);
        }
        if (repeatPasswordEdit != null) {
            repeatPasswordEdit.setEnabled(isEnable);
        }
    }

    private void clearPasswordEdit(int index) {
        if (index > 0 && index < 8) {
            if (!isResetPassword && (index % 2) != 0 && oldPasswordEdit != null) {
                oldPasswordEdit.getText().clear();
            }
            if (((index % 4) >> 1) != 0 && newPasswordEdit != null) {
                newPasswordEdit.getText().clear();
            }
            if ((index >> 2) != 0 && repeatPasswordEdit != null) {
                repeatPasswordEdit.getText().clear();
            }
        }
    }

    private void showOperationFailDialog(@StringRes int contentResId) {
        if (operationFailDialogContent != null) {
            operationFailDialogContent.setText(contentResId);
        }
        if (operationFailDialog != null) {
            if (!operationFailDialog.isShowing()){
                operationFailDialog.show();
            }
            Window window = operationFailDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_259)
                        , ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissOperationDialog() {
        if (operationFailDialogContent != null) {
            operationFailDialogContent.setText("");
        }
        if (operationFailDialog != null && operationFailDialog.isShowing()) {
            operationFailDialog.dismiss();
        }
    }

    private void pollSecurityMessage() {
        boolean isHandle = false;
        if (presenter != null) {
            EulixBoxBaseInfo eulixBoxBaseInfo = presenter.getBaseInfo();
            if (eulixBoxBaseInfo != null) {
                String boxUuid = eulixBoxBaseInfo.getBoxUuid();
                String boxBind = eulixBoxBaseInfo.getBoxBind();
                if (boxUuid != null && boxBind != null && granteeApplyId != null) {
                    isHandle = true;
                    EventBusUtil.post(new SecurityMessagePollRequestEvent(boxUuid, boxBind, granteeApplyId));
                }
            }
        }
        if (!isHandle) {
            handleResetApplyId();
            if (granteeSecurityRequestDialog != null) {
                granteeSecurityRequestDialog.dismissGranteeRequestDialog();
            }
        }
    }

    private void handleResetApplyId() {
        if (mHandler != null) {
            while (mHandler.hasMessages(GRANTEE_APPLY)) {
                mHandler.removeMessages(GRANTEE_APPLY);
            }
        }
        granteeApplyId = null;
    }

    private boolean prepareEulixAuthentication(boolean isNeedGranterDataUuid) {
        boolean isAuthentication = true;
        if (presenter != null) {
            isAuthentication = presenter.isOnlyHardwareDeviceCanVerify();
        }
        if (!isAuthentication && presenter != null) {
            EulixBoxBaseInfo eulixBoxBaseInfo = presenter.getBaseInfo();
            if (eulixBoxBaseInfo != null) {
                String boxUuid = eulixBoxBaseInfo.getBoxUuid();
                String boxBind = eulixBoxBaseInfo.getBoxBind();
                if (boxUuid != null && boxBind != null) {
                    needGranterDataUuid = isNeedGranterDataUuid;
                    showLoading("");
                    deviceHardwareInfoRequestUuid = UUID.randomUUID().toString();
                    EventBusUtil.post(new DeviceHardwareInfoRequestEvent(boxUuid, boxBind, deviceHardwareInfoRequestUuid, true));
                }
            }
        }
        return isAuthentication;
    }

    private void startEulixAuthenticationActivity(String granterDataUuid) {
        isSecurityFinish = false;
        Intent intent = new Intent(ModifySecurityPasswordActivity.this, EulixAuthenticationActivity.class);
        intent.putExtra(ConstantField.HARDWARE_FUNCTION, ConstantField.HardwareFunction.SECURITY_VERIFICATION);
        intent.putExtra(ConstantField.SECURITY_FUNCTION, ConstantField.SecurityFunction.RESET_PASSWORD);
        if (forgetPasswordUuid != null) {
            intent.putExtra(ConstantField.FORGET_PASSWORD_UUID, forgetPasswordUuid);
        }
        if (granterDataUuid != null) {
            intent.putExtra(ConstantField.GRANTER_DATA_UUID, granterDataUuid);
        }
        startActivity(intent);
    }

    /**
     * 收起软键盘
     */
    private void forceHideSoftInput() {
        if (inputMethodManager != null) {
            View view = getCurrentFocus();
            if (view != null) {
                inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void handleBack(boolean isOk) {
        if (authenticationFunction == ConstantField.AuthenticationFunction.HARDWARE_DEVICE) {
            handleResult(isOk);
        } else {
            if (authenticationUuid != null) {
                EventBusUtil.post(new SecurityOperationResultEvent(isOk, authenticationUuid));
            }
            if (forgetPasswordUuid != null) {
                EventBusUtil.post(new ForgetPasswordResultEvent(forgetPasswordUuid, isOk));
            }
            finish();
        }
    }

    private void handleBack(boolean isOk, boolean isGranteeSecurityTokenExpire) {
        if (authenticationFunction == ConstantField.AuthenticationFunction.HARDWARE_DEVICE) {
            handleResult(isOk, isGranteeSecurityTokenExpire);
        } else {
            if (authenticationUuid != null) {
                EventBusUtil.post(new SecurityOperationResultEvent((isOk || isGranteeSecurityTokenExpire), authenticationUuid));
            }
            if (forgetPasswordUuid != null) {
                EventBusUtil.post(new ForgetPasswordResultEvent(forgetPasswordUuid, isOk));
            }
            finish();
        }
    }

    private void handleResult(boolean isOk) {
        Intent intent = new Intent();
        setResult(isOk ? Activity.RESULT_OK : Activity.RESULT_CANCELED, intent);
        finish();
    }

    private void handleResult(boolean isOk, boolean isGranteeSecurityTokenExpire) {
        Intent intent = new Intent();
        intent.putExtra(ConstantField.GRANTEE_SECURITY_TOKEN_EXPIRE, isGranteeSecurityTokenExpire);
        setResult(isOk ? Activity.RESULT_OK : Activity.RESULT_CANCELED, intent);
        finish();
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        isStart = true;
        securityFinish();
    }

    private void securityFinish() {
        if (isStart && isSecurityFinish != null && isSecurityFinish) {
            isSecurityFinish = null;
            finish();
        }
    }

    private void handleResetSecurityPasswordResult(int code, String source, Boolean isGranter) {
        boolean isSuccess = (code >= 200 && code < 300);
        if (isSuccess) {
            showImageTextToast(R.drawable.toast_right, R.string.setting_success);
            handleBack(true);
        } else {
            boolean isHandle = false;
            if (isResetPassword && ConstantField.KnownSource.ACCOUNT.equals(source)) {
                isHandle = true;
                switch (code) {
                    case ConstantField.KnownError.EulixSecurityError.SECURITY_TOKEN_EXPIRE:
                        isOperationExpire = true;
                        isOperationExpireGranter = isGranter;
                        showOperationFailDialog(R.string.operate_expire);
                        break;
                    default:
                        isHandle = false;
                        break;
                }
            }
            if (!isHandle) {
                if (code == ConstantField.SERVER_EXCEPTION_CODE) {
                    showServerExceptionToast();
                } else {
                    showImageTextToast(R.drawable.toast_refuse, R.string.setting_fail);
                }
            }
        }
    }

    @Override
    protected void onStop() {
        isStart = false;
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        if (authenticationFunction == ConstantField.AuthenticationFunction.HARDWARE_DEVICE) {
            handleResult(false);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (authenticationFunction == ConstantField.AuthenticationFunction.HARDWARE_DEVICE && keyCode == KeyEvent.KEYCODE_BACK) {
            handleResult(false);
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onDestroy() {
        if (mBridge != null) {
            mBridge.unregisterSinkCallback();
            mBridge = null;
        }
        if (mManager != null) {
            mManager.unregisterCallback(activityId);
            mManager = null;
        }
        granteeApplyId = null;
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }

    @Override
    protected int getActivityIndex() {
        return SECURITY_SERIES_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public ModifySecurityPasswordPresenter createPresenter() {
        return new ModifySecurityPasswordPresenter();
    }

    @Override
    public void modifySecurityPasswordResult(String source, int code, Boolean isGranter) {
        if (mHandler != null) {
            mHandler.post(() -> {
                closeLoading();
                setDonePattern(true, true);
                if (code >= 200 && code < 300) {
                    showImageTextToast(R.drawable.toast_right, R.string.modify_success);
                    finish();
                } else if (code == 403 && ConstantField.KnownSource.ACCOUNT.equals(source)) {
                    showOperationFailDialog(R.string.old_password_wrong);
                    clearPasswordEdit(1);
                } else if (code == ConstantField.KnownError.EulixSecurityError.SECURITY_TOKEN_EXPIRE && ConstantField.KnownSource.ACCOUNT.equals(source)) {
                    isOperationExpire = true;
                    isOperationExpireGranter = isGranter;
                    showOperationFailDialog(R.string.operate_expire);
                } else if (code == ConstantField.SERVER_EXCEPTION_CODE) {
                    showServerExceptionToast();
                } else {
                    showImageTextToast(R.drawable.toast_wrong, R.string.modify_failed_pure);
                }
            });
        }
    }

    @Override
    public void bluetoothHandle(boolean isGranter, String password, String granterSecurityToken) {
        if (mBridge != null) {
            mBridge.resetPassword(isGranter, password, granterSecurityToken);
        }
    }

    @Override
    public void resetSecurityPasswordResult(String source, int code, Boolean isGranter) {
        if (mHandler != null) {
            mHandler.post(() -> {
                closeLoading();
                setDonePattern(true, true);
                handleResetSecurityPasswordResult(code, source, isGranter);
            });
        }
    }

    @Override
    public void granteeApplyResult(String source, int code) {
        if (mHandler != null) {
            mHandler.post(() -> {
                closeLoading();
                if (code >= 200 && code < 400) {
                    while (mHandler.hasMessages(GRANTEE_APPLY)) {
                        mHandler.removeMessages(GRANTEE_APPLY);
                    }
                    mHandler.sendEmptyMessageDelayed(GRANTEE_APPLY, (10 * ConstantField.TimeUnit.MINUTE_UNIT));
                    if (granteeSecurityRequestDialog != null) {
                        granteeSecurityRequestDialog.showGranteeRequestDialog();
                    }
                    pollSecurityMessage();
                } else if (code == ConstantField.KnownError.AccountCommonError.ACCOUNT_410 && ConstantField.KnownSource.ACCOUNT.equals(source)) {
                    if (granteeSecurityRequestDialog != null) {
                        granteeSecurityRequestDialog.handleGranteeRequestResult(GranteeSecurityRequestDialog.REQUEST_TOO_MANY);
                    }
                } else if (code == ConstantField.SERVER_EXCEPTION_CODE) {
                    showServerExceptionToast();
                } else {
                    showImageTextToast(R.drawable.toast_refuse, R.string.apply_authorization_fail);
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    handleBack(false);
                    break;
                case R.id.function_text:
                    if (presenter != null) {
                        forceHideSoftInput();
                        String oldPassword = (isResetPassword ? null : oldPasswordEdit.getText().toString());
                        String password1 = newPasswordEdit.getText().toString();
                        String password2 = repeatPasswordEdit.getText().toString();
                        boolean isPasswordLengthLegal = (password1.length() == PASSWORD_LENGTH && password2.length() == PASSWORD_LENGTH);
                        if (!isResetPassword && isPasswordLengthLegal) {
                            isPasswordLengthLegal = (oldPassword != null && oldPassword.length() == PASSWORD_LENGTH);
                        }
                        if (isPasswordLengthLegal) {
                            if (!password1.equals(password2)) {
                                showOperationFailDialog(R.string.new_password_inconsistent);
                                clearPasswordEdit(6);
                            } else if (password1.equals(oldPassword)) {
                                showOperationFailDialog(R.string.old_new_password_same);
                                clearPasswordEdit(7);
                            } else {
                                setDonePattern(true, false);
                                showLoading("");
                                if (isResetPassword) {
                                    if (authenticationFunction == ConstantField.AuthenticationFunction.HARDWARE_DEVICE) {
                                        presenter.resetPasswordToHardware(password1, granterSecurityToken, baseUrl, isBluetooth, bleKey, bleIv);
                                    } else if (securityFunction != ConstantField.SecurityFunction.NewDeviceSecurityFunction.VERIFY_SECURITY_MAILBOX) {
                                        presenter.resetPassword(securityToken, password1, granterSecurityToken);
                                    } else if (mManager != null) {
                                        NewDeviceResetPasswordEntity entity = new NewDeviceResetPasswordEntity();
                                        entity.setAcceptSecurityToken(granterSecurityToken);
                                        entity.setEmailSecurityToken(securityToken);
                                        entity.setClientUuid(granterClientUuid);
                                        entity.setNewDeviceClientUuid(DataUtil.getClientUuid(getApplicationContext()));
                                        entity.setNewPasswd(password1);
                                        mSecurityPassword = password1;
                                        mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_NEW_DEVICE_RESET_PASSWORD
                                                , new Gson().toJson(entity, NewDeviceResetPasswordEntity.class));
//                                        mBridge.newDeviceResetPasswordRequest(granterSecurityToken, securityToken, granterClientUuid, password1);
                                    }
                                } else {
                                    presenter.modifySecurityPassword(oldPassword, password1, granterSecurityToken);
                                }
                            }
                        } else {
                            showOperationFailDialog(R.string.password_six_number_hint);
                        }
                    }
                    break;
                case R.id.forget_password:
                    if (!isResetPassword && presenter != null) {
                        int identity = presenter.getActiveIdentity();
                        switch (identity) {
                            case ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY:
                                if (prepareEulixAuthentication(false)) {
                                    startEulixAuthenticationActivity(null);
                                }
                                break;
                            case ConstantField.UserIdentity.ADMINISTRATOR_GRANTEE:
                                if (granterSecurityTokenExpireTimestamp > System.currentTimeMillis()) {
                                    if (prepareEulixAuthentication(true)) {
                                        String granterDataUuid = null;
                                        if (mGranterSecurityTokenResult != null) {
                                            granterDataUuid = DataUtil.setData(new Gson().toJson(mGranterSecurityTokenResult, SecurityTokenResult.class));
                                        }
                                        startEulixAuthenticationActivity(granterDataUuid);
                                    }
                                } else {
                                    showLoading("");
                                    granteeApplyId = UUID.randomUUID().toString();
                                    presenter.granteeApplyResetSecurityPassword(granteeApplyId);
                                }
                                break;
                            default:
                                break;
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onFinish() {
        if (mHandler != null) {
            mHandler.post(this::finish);
        }
    }

    @Override
    public void onResponse(int code, String source, int step, String bodyJson) {
        switch (step) {
            case AODeviceDiscoveryManager.STEP_NEW_DEVICE_RESET_PASSWORD:
                EulixBaseResponse eulixBaseResponse = null;
                if (bodyJson != null) {
                    try {
                        eulixBaseResponse = new Gson().fromJson(bodyJson, EulixBaseResponse.class);
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                    }
                }
                if (eulixBaseResponse != null) {
                    String codeValue = eulixBaseResponse.getCode();
                    code = DataUtil.stringCodeToInt(codeValue);
                    source = DataUtil.stringCodeGetSource(codeValue);
                } else {
                    code = 500;
                    source = null;
                }
                resetSecurityPasswordResult(source, code, null);
                if (presenter != null && mManager != null) {
                    // 目前密码只有管理员
                    presenter.resetPasswordCredential(code, mManager.getBoxUuid(), "1", mSecurityPassword, false);
                    mSecurityPassword = null;
                }
                mSecurityPassword = null;
                break;
            default:
                break;
        }
    }

    @Override
    public void securityPasswordResult(int code, String source, Boolean isGranter) {
        if (mHandler != null && isBluetooth) {
            mHandler.post(() -> {
                if (presenter != null) {
                    presenter.resetPasswordCredential(code);
                }
                closeLoading();
                setDonePattern(true, true);
                handleResetSecurityPasswordResult(code, source, isGranter);
            });
        }
    }

    @Override
    public void newDeviceResetSecurityPasswordResult(String source, int code) {
        if (mHandler != null) {
            mHandler.post(() -> {
                closeLoading();
                setDonePattern(true, true);
                handleResetSecurityPasswordResult(code, source, null);
            });
        }
    }

    @Override
    public void handleDisconnect() {
        if (mHandler != null) {
            mHandler.post(this::finish);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ForgetPasswordResultEvent event) {
        if (!isResetPassword && event != null) {
            String uuid = event.getRequestId();
            if (uuid != null && uuid.equals(forgetPasswordUuid)) {
                if (event.isSuccess() && isSecurityFinish != null && !isSecurityFinish) {
                    isSecurityFinish = true;
                    securityFinish();
                }
                if (isSecurityFinish != null) {
                    if (!event.isSuccess()) {
                        isSecurityFinish = null;
                    } else if (!isSecurityFinish) {
                        isSecurityFinish = true;
                        securityFinish();
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SecurityMessagePollResponseEvent event) {
        if (event != null) {
            int code = event.getCode();
            String requestId = event.getRequestUuid();
            SecurityMessagePollResult result = event.getResult();
            if (granteeApplyId != null && granteeApplyId.equals(requestId)) {
                if (code >= 200 && code < 400 && result != null) {
                    handleResetApplyId();
                    boolean isAccept = result.isAccept();
                    if (granteeSecurityRequestDialog != null) {
                        granteeSecurityRequestDialog.handleGranteeRequestResult(isAccept
                                ? GranteeSecurityRequestDialog.REQUEST_ACCEPT : GranteeSecurityRequestDialog.REQUEST_DENY);
                    }
                    if (isAccept) {
                        SecurityTokenResult securityTokenResult = result.getSecurityTokenResult();
                        if (securityTokenResult != null) {
                            mGranterSecurityTokenResult = securityTokenResult;
                            granterSecurityToken = securityTokenResult.getSecurityToken();
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                granterSecurityTokenExpireTimestamp = FormatUtil.parseZonedDateTime(securityTokenResult.getExpiredAt());
                            } else {
                                granterSecurityTokenExpireTimestamp = FormatUtil.parseZonedTimestamp(securityTokenResult.getExpiredAt()
                                        , ConstantField.TimeStampFormat.FILE_API_DAY_FORMAT, ConstantField.TimeStampFormat.FILE_API_SPLIT);
                            }
                        }
                        if (prepareEulixAuthentication(true)) {
                            String granterDataUuid = null;
                            if (securityTokenResult != null) {
                                granterDataUuid = DataUtil.setData(new Gson().toJson(securityTokenResult, SecurityTokenResult.class));
                            }
                            startEulixAuthenticationActivity(granterDataUuid);
                        }
                    }
                } else {
                    switch (code) {
                        case -3:
                            handleResetApplyId();
                            if (granteeSecurityRequestDialog != null) {
                                granteeSecurityRequestDialog.dismissGranteeRequestDialog();
                            }
                            break;
                        default:
                            pollSecurityMessage();
                            break;
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DeviceHardwareInfoResponseEvent event) {
        if (event != null && deviceHardwareInfoRequestUuid != null && deviceHardwareInfoRequestUuid.equals(event.getRequestUuid())) {
            closeLoading();
            deviceHardwareInfoRequestUuid = null;
            int code = event.getCode();
            String bluetoothId = event.getBluetoothId();
            if (code >= 200 && code < 400 && bluetoothId != null) {
                String granterDataUuid = null;
                if (needGranterDataUuid != null) {
                    if (needGranterDataUuid && mGranterSecurityTokenResult != null) {
                        granterDataUuid = DataUtil.setData(new Gson().toJson(mGranterSecurityTokenResult, SecurityTokenResult.class));
                    }
                    needGranterDataUuid = null;
                }
                startEulixAuthenticationActivity(granterDataUuid);
            } else {
                showServerExceptionToast();
            }
        }
    }
}
