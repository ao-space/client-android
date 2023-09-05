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

package xyz.eulix.space.ui.bind;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.UUID;

import xyz.eulix.space.EulixDeviceListActivity;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.bind.RvokInfo;
import xyz.eulix.space.bridge.BindFailBridge;
import xyz.eulix.space.event.BoxOnlineRequestEvent;
import xyz.eulix.space.event.DeviceHardwareInfoRequestEvent;
import xyz.eulix.space.event.DeviceHardwareInfoResponseEvent;
import xyz.eulix.space.event.SecurityMessagePollRequestEvent;
import xyz.eulix.space.event.SecurityMessagePollResponseEvent;
import xyz.eulix.space.manager.AODeviceDiscoveryManager;
import xyz.eulix.space.network.EulixBaseResponse;
import xyz.eulix.space.network.agent.AdminRevokeResult;
import xyz.eulix.space.network.agent.AdminRevokeResults;
import xyz.eulix.space.network.agent.NewDeviceApplyResetPasswordEntity;
import xyz.eulix.space.network.agent.SecurityMessagePollEntity;
import xyz.eulix.space.network.agent.SecurityMessagePollResponse;
import xyz.eulix.space.network.agent.bind.BindRevokeRequest;
import xyz.eulix.space.network.agent.bind.BindRevokeResult;
import xyz.eulix.space.network.security.SecurityMessagePollResult;
import xyz.eulix.space.network.security.SecurityTokenResult;
import xyz.eulix.space.presenter.UnbindDevicePresenter;
import xyz.eulix.space.ui.mine.security.EulixAuthenticationActivity;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.SystemUtil;
import xyz.eulix.space.view.EulixSixSecurityPasswordView;
import xyz.eulix.space.view.dialog.security.GranteeSecurityRequestDialog;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/11/1 17:44
 */
public class UnbindDeviceActivity extends AbsActivity<UnbindDevicePresenter.IUnbindDevice, UnbindDevicePresenter> implements UnbindDevicePresenter.IUnbindDevice
        , View.OnClickListener, EulixSixSecurityPasswordView.IEulixSixSecurityPassword, AODeviceDiscoveryManager.AODeviceDiscoverySinkCallback
        , BindFailBridge.BindFailSinkCallback {
    private static final int GRANTEE_APPLY = 1;
    private static final int NEW_DEVICE_APPLY = GRANTEE_APPLY + 1;
    private static final int PASSWORD_WRONG_HINT_EXPIRE = NEW_DEVICE_APPLY + 1;
    private String activityId;
    private ImageButton back;
    private TextView title;
    private TextView unbindErrorHint;
    //    private VerificationCodeInput unbindCode;
    private FrameLayout unbindCodeContainer;
    private TextView forgetPassword;
    private LinearLayout unbindDeviceHintContainer;
    private Dialog deviceSecurityPasswordDialog;
    private ImageButton deviceSecurityPasswordDialogExit;
    private TextView deviceSecurityPasswordDialogTitle;
    private TextView deviceSecurityPasswordDialogContent;
    private Dialog securityEmailVerifyFailDialog;
    private ImageView securityEmailVerifyFailImage;
    private TextView securityEmailVerifyFailTitle;
    private TextView securityEmailVerifyFailContent;
    private TextView securityEmailVerifyFailHint;
    private Button securityEmailVerifyFailButton;
    private EulixSixSecurityPasswordView eulixSixSecurityPasswordView;
    private InputMethodManager inputMethodManager;
    private UnbindDeviceHandler mHandler;
    private BindFailBridge mBridge;
    private AODeviceDiscoveryManager mManager;
    private String boxUuid;
    private String baseUrl;
    private String bleKey;
    private String bleIv;
    private String mPassword;
    private int bluetoothState;
    private int paired = 0;
    private int securityFunction;
    private String authenticationUuid;
    private int authenticationFunction;
    private GranteeSecurityRequestDialog granteeSecurityRequestDialog;
    private GranteeSecurityRequestDialog.GranteeSecurityRequestCallback granteeSecurityRequestCallback = null;
    private String granteeApplyId;
    private GranteeSecurityRequestDialog newDeviceSecurityRequestDialog;
    private GranteeSecurityRequestDialog.GranteeSecurityRequestCallback newDeviceSecurityRequestCallback = null;
    private String newDeviceApplyId;
    private SecurityTokenResult newDeviceSecurityTokenResult;
    private String granterClientUuid;
    private Boolean needGranterDataUuid;
    private String deviceHardwareInfoRequestUuid;
    private SecurityTokenResult tempGranterSecurityTokenResult;

    static class UnbindDeviceHandler extends Handler {
        private WeakReference<UnbindDeviceActivity> unbindDeviceActivityWeakReference;

        public UnbindDeviceHandler(UnbindDeviceActivity activity) {
            unbindDeviceActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            UnbindDeviceActivity activity = unbindDeviceActivityWeakReference.get();
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
                    case NEW_DEVICE_APPLY:
                        activity.newDeviceApplyId = null;
                        if (activity.newDeviceSecurityRequestDialog != null) {
                            activity.newDeviceSecurityRequestDialog.handleGranteeRequestResult(GranteeSecurityRequestDialog.REQUEST_EXPIRE);
                        }
                        break;
                    case PASSWORD_WRONG_HINT_EXPIRE:
                        if (activity.unbindErrorHint != null) {
                            activity.unbindErrorHint.setText("");
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
        setContentView(R.layout.activity_unbind_device);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        unbindErrorHint = findViewById(R.id.unbind_error_hint);
//        unbindCode = findViewById(R.id.unbind_code);
        unbindCodeContainer = findViewById(R.id.unbind_code_container);
        unbindDeviceHintContainer = findViewById(R.id.unbind_device_hint_container);
        forgetPassword = findViewById(R.id.forget_password);

        View deviceSecurityPasswordDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_common_pure_dialog, null);
        deviceSecurityPasswordDialogExit = deviceSecurityPasswordDialogView.findViewById(R.id.dialog_exit);
        deviceSecurityPasswordDialogTitle = deviceSecurityPasswordDialogView.findViewById(R.id.dialog_title);
        deviceSecurityPasswordDialogContent = deviceSecurityPasswordDialogView.findViewById(R.id.dialog_content);
        deviceSecurityPasswordDialog = new Dialog(this, R.style.EulixDialog);
        deviceSecurityPasswordDialog.setCancelable(true);
        deviceSecurityPasswordDialog.setContentView(deviceSecurityPasswordDialogView);

        View securityEmailVerifyFailDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_error_one_button_two_content_dialog_style_2, null);
        securityEmailVerifyFailImage = securityEmailVerifyFailDialogView.findViewById(R.id.dialog_image);
        securityEmailVerifyFailTitle = securityEmailVerifyFailDialogView.findViewById(R.id.dialog_title);
        securityEmailVerifyFailContent = securityEmailVerifyFailDialogView.findViewById(R.id.dialog_content);
        securityEmailVerifyFailHint = securityEmailVerifyFailDialogView.findViewById(R.id.dialog_hint);
        securityEmailVerifyFailButton = securityEmailVerifyFailDialogView.findViewById(R.id.dialog_button);
        securityEmailVerifyFailDialog = new Dialog(this, R.style.EulixDialog);
        securityEmailVerifyFailDialog.setCancelable(false);
        securityEmailVerifyFailDialog.setContentView(securityEmailVerifyFailDialogView);

        eulixSixSecurityPasswordView = new EulixSixSecurityPasswordView(this, true, this);
    }

    @Override
    public void initData() {
        mHandler = new UnbindDeviceHandler(this);
        handleIntent(getIntent());
        inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        activityId = UUID.randomUUID().toString();
        mManager = AODeviceDiscoveryManager.getInstance();
        mManager.registerCallback(activityId, this);
        mBridge = BindFailBridge.getInstance();
        mBridge.registerSinkCallback(this);
    }

    @Override
    public void initViewData() {
        title.setText(securityFunction == 0 ? (paired == 0 ? R.string.unbind_device : R.string.bind_device) : R.string.security_password_verify);
        unbindErrorHint.setText("");

        deviceSecurityPasswordDialogTitle.setText(R.string.device_security_password);
        deviceSecurityPasswordDialogContent.setText(R.string.device_security_password_hint);

        securityEmailVerifyFailImage.setImageResource(R.drawable.security_mailbox_long_2x);
        securityEmailVerifyFailTitle.setText(R.string.security_mailbox_verify_fail);
        securityEmailVerifyFailContent.setText(R.string.no_bind_security_mailbox_reason);
        securityEmailVerifyFailHint.setText(R.string.no_bind_security_mailbox_hint);
        securityEmailVerifyFailButton.setText(R.string.ok);

        if (bluetoothState != 0) {
            forgetPassword.setVisibility(View.GONE);
        }
    }

    @Override
    public void initEvent() {
        eulixSixSecurityPasswordView.setFocus(true);
        back.setOnClickListener(this);
        unbindDeviceHintContainer.setOnClickListener(this);

        forgetPassword.setOnClickListener(this);

        deviceSecurityPasswordDialogExit.setOnClickListener(v -> dismissDeviceSecurityPasswordDialog());

        securityEmailVerifyFailButton.setOnClickListener(v -> dismissSecurityEmailVerifyFailDialog());

        if (bluetoothState == 0) {
            if (presenter != null) {
                int identity = presenter.getActiveIdentity();
                if (identity == ConstantField.UserIdentity.ADMINISTRATOR_GRANTEE) {
                    granteeSecurityRequestCallback = new GranteeSecurityRequestDialog.GranteeSecurityRequestCallback() {
                        @Override
                        public void cancelRequest() {
                            handleResetApplyId(false);
                        }

                        @Override
                        public void handleRequestCode(int requestCode) {
                            // Do nothing
                        }
                    };
                    granteeSecurityRequestDialog = new GranteeSecurityRequestDialog(this, granteeSecurityRequestCallback, false);
                }
            }
        } else {
            newDeviceSecurityRequestCallback = new GranteeSecurityRequestDialog.GranteeSecurityRequestCallback() {
                @Override
                public void cancelRequest() {
                    handleResetApplyId(true);
                }

                @Override
                public void handleRequestCode(int requestCode) {
                    if (requestCode == GranteeSecurityRequestDialog.NEW_DEVICE_REQUEST_CANCEL) {
                        newDeviceSecurityTokenResult = null;
                        granterClientUuid = null;
                        showLoading("");
//                        if (mBridge != null) {
//                            mBridge.securityEmailSettingRequest();
//                        }
                        if (mManager != null) {
                            mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_SECURITY_EMAIL_SETTING, null);
                        }
                    }
                }
            };
            newDeviceSecurityRequestDialog = new GranteeSecurityRequestDialog(this, newDeviceSecurityRequestCallback, true);
        }
    }

    private void showDeviceSecurityPasswordDialog() {
        if (deviceSecurityPasswordDialog != null && !deviceSecurityPasswordDialog.isShowing()) {
            deviceSecurityPasswordDialog.show();
            Window window = deviceSecurityPasswordDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_259), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissDeviceSecurityPasswordDialog() {
        if (deviceSecurityPasswordDialog != null && deviceSecurityPasswordDialog.isShowing()) {
            deviceSecurityPasswordDialog.dismiss();
        }
    }

    private void dismissSecurityEmailVerifyFailDialog() {
        if (securityEmailVerifyFailDialog != null && securityEmailVerifyFailDialog.isShowing()) {
            securityEmailVerifyFailDialog.dismiss();
        }
    }

    private void resetEulixSixSecurityPasswordPattern(boolean isGainFocus) {
        if (eulixSixSecurityPasswordView != null) {
            eulixSixSecurityPasswordView.setEnable(true);
            eulixSixSecurityPasswordView.resetPassword();
            if (isGainFocus) {
                eulixSixSecurityPasswordView.setFocus(true);
            }
        }
    }

    @Override
    protected int getActivityIndex() {
        return BIND_SERIES_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public UnbindDevicePresenter createPresenter() {
        return new UnbindDevicePresenter();
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    private void handlePasswordWrong(List<Long> timestampList) {
        int count = 0;
        Long permitTimestamp = null;
        Long firstTimestamp = null;
        if (timestampList != null) {
            count = Math.max((3 - timestampList.size()), 0);
            for (Long timestamp : timestampList) {
                if (timestamp != null) {
                    if (permitTimestamp == null || permitTimestamp < timestamp) {
                        permitTimestamp = timestamp;
                    }
                    if (firstTimestamp == null || timestamp < firstTimestamp) {
                        firstTimestamp = timestamp;
                    }
                }
            }
            if (permitTimestamp != null) {
                permitTimestamp = permitTimestamp + ConstantField.TimeUnit.MINUTE_UNIT;
            }
        }
        if (count >= 0 && count < 3 && permitTimestamp != null) {
            if (count > 0) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(getString(R.string.password_wrong_chance_hint_part_1));
                stringBuilder.append(count);
                stringBuilder.append(getString((Math.abs(count) == 1
                        ? R.string.password_wrong_chance_hint_part_2_singular : R.string.password_wrong_chance_hint_part_2_plural)));
                unbindErrorHint.setText(stringBuilder);
                if (firstTimestamp != null) {
                    long diffTimestamp = (firstTimestamp + ConstantField.TimeUnit.MINUTE_UNIT - System.currentTimeMillis());
                    if (diffTimestamp > 0) {
                        if (mHandler != null) {
                            while (mHandler.hasMessages(PASSWORD_WRONG_HINT_EXPIRE)) {
                                mHandler.removeMessages(PASSWORD_WRONG_HINT_EXPIRE);
                            }
                            mHandler.sendEmptyMessageDelayed(PASSWORD_WRONG_HINT_EXPIRE, diffTimestamp);
                        }
                    } else {
                        unbindErrorHint.setText("");
                    }
                }
                resetEulixSixSecurityPasswordPattern(true);
            } else {
                unbindErrorHint.setText("");
                long currentTimestamp = System.currentTimeMillis();
                if (currentTimestamp < permitTimestamp) {
                    long minute = (long) Math.ceil((permitTimestamp - currentTimestamp) * 1.0 / ConstantField.TimeUnit.MINUTE_UNIT);
                    if (minute > 0) {
                        String stringBuilder = getString(R.string.common_retry_hint_minute_part_1) +
                                minute +
                                getString((Math.abs(minute) == 1L
                                        ? R.string.common_retry_hint_minute_part_2_singular : R.string.common_retry_hint_minute_part_2_plural));
                        showImageTextToast(R.drawable.toast_refuse, stringBuilder);
                    }
                }
                resetEulixSixSecurityPasswordPattern(false);
                if (securityFunction >= 0) {
                    finish();
                } else {
                    handleResult(false);
                }
            }
        } else {
            resetEulixSixSecurityPasswordPattern(true);
        }
    }

    @Override
    public void onRevokeResult(boolean result, int code, String source, List<Long> timestampList) {
        Logger.d("zfy", "revoke result:" + result);
        if (mHandler != null) {
            mHandler.post(() -> {
                closeLoading();
                if (result) {
                    showImageTextToast(R.drawable.toast_right, R.string.unbind_toast_success);
                    EventBusUtil.post(new BoxOnlineRequestEvent(false));
                    resetEulixSixSecurityPasswordPattern(false);
                    Intent loginIntent = new Intent(UnbindDeviceActivity.this, EulixDeviceListActivity.class);
                    loginIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(loginIntent);
                    finish();
                } else {
                    if ((code == ConstantField.KnownError.GatewayCommonError.GATEWAY_406) && ConstantField.KnownSource.GATEWAY.equals(source)) {
                        handlePasswordWrong(timestampList);
                    } else if (code == ConstantField.ErrorCode.SPACE_PLATFORM_CONNECT_ERROR) {
                        showImageTextToast(R.drawable.toast_refuse, R.string.platform_connect_error);
                        resetEulixSixSecurityPasswordPattern(true);
                    } else {
                        if (code == ConstantField.SERVER_EXCEPTION_CODE) {
                            showServerExceptionToast();
                        } else {
                            showImageTextToast(R.drawable.toast_wrong, R.string.unbind_toast_failed);
                        }
                        resetEulixSixSecurityPasswordPattern(true);
                    }
//                    unbindCode.setEnabled(true);
//                    unbindCode.clearAll();
                }
            });
        }
    }

    @Override
    public void onVerifyResult(int code, String source, SecurityTokenResult result, List<Long> timestampList) {
        if (mHandler != null) {
            mHandler.post(() -> {
                closeLoading();
                if (code >= 200 && code < 300) {
                    unbindErrorHint.setText("");
                    if (securityFunction != ConstantField.SecurityFunction.DeveloperOptionsSecurityFunction.OPEN_DEVELOPER_OPTIONS) {
                        showImageTextToast(R.drawable.toast_right, R.string.verify_success);
                    }
                    resetEulixSixSecurityPasswordPattern(false);
                    String dataUuid = null;
                    if (result != null) {
                        dataUuid = DataUtil.setData(new Gson().toJson(result, SecurityTokenResult.class));
                    }

                    handleResult(true, dataUuid);
                } else if (code == ConstantField.KnownError.AccountCommonError.ACCOUNT_403 && ConstantField.KnownSource.ACCOUNT.equals(source)) {
                    handlePasswordWrong(timestampList);
                } else {
                    if (code == ConstantField.SERVER_EXCEPTION_CODE) {
                        showServerExceptionToast();
                    } else {
                        showImageTextToast(R.drawable.toast_wrong, R.string.verify_fail);
                    }
                    resetEulixSixSecurityPasswordPattern(true);
                }
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
                    pollSecurityMessage(false);
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

    private void handleIntent(Intent intent) {
        if (intent == null) {
            intent = getIntent();
        }
        if (intent != null) {
            securityFunction = intent.getIntExtra(ConstantField.SECURITY_FUNCTION, 0);
            bluetoothState = intent.getIntExtra("bluetooth", 0);
            paired = intent.getIntExtra(ConstantField.BOUND, 0);
            if (intent.hasExtra(ConstantField.BASE_URL)) {
                baseUrl = intent.getStringExtra(ConstantField.BASE_URL);
            }
            if (intent.hasExtra(ConstantField.BLE_KEY)) {
                bleKey = intent.getStringExtra(ConstantField.BLE_KEY);
            }
            if (intent.hasExtra(ConstantField.BLE_IV)) {
                bleIv = intent.getStringExtra(ConstantField.BLE_IV);
            }
            if (intent.hasExtra(ConstantField.AUTHENTICATION_UUID)) {
                authenticationUuid = intent.getStringExtra(ConstantField.AUTHENTICATION_UUID);
            }
            authenticationFunction = intent.getIntExtra(ConstantField.AUTHENTICATION_FUNCTION, 0);
        }
    }

    private void pollSecurityMessage(boolean isNewDevice) {
        boolean isHandle = false;
        if (isNewDevice) {
            if (newDeviceApplyId != null && mManager != null) {
                isHandle = true;
                SecurityMessagePollEntity entity = new SecurityMessagePollEntity();
                entity.setClientUuid(DataUtil.getClientUuid(getApplicationContext()));
                mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_SECURITY_MESSAGE_POLL
                        , new Gson().toJson(entity, SecurityMessagePollEntity.class));
                //mBridge.securityMessagePollRequest(newDeviceApplyId);
            }
        } else if (presenter != null) {
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
            handleResetApplyId(isNewDevice);
            if (isNewDevice) {
                if (newDeviceSecurityRequestDialog != null) {
                    newDeviceSecurityRequestDialog.dismissGranteeRequestDialog();
                }
            } else if (granteeSecurityRequestDialog != null) {
                granteeSecurityRequestDialog.dismissGranteeRequestDialog();
            }
        }
    }

    private void handleResetApplyId(boolean isNewDevice) {
        if (isNewDevice) {
            if (mHandler != null) {
                while (mHandler.hasMessages(NEW_DEVICE_APPLY)) {
                    mHandler.removeMessages(NEW_DEVICE_APPLY);
                }
            }
            newDeviceApplyId = null;
        } else {
            if (mHandler != null) {
                while (mHandler.hasMessages(GRANTEE_APPLY)) {
                    mHandler.removeMessages(GRANTEE_APPLY);
                }
            }
            granteeApplyId = null;
        }
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
        tempGranterSecurityTokenResult = null;
        if (bluetoothState != 0 && mBridge != null) {
            mBridge.handleVerification();
        }
        Intent intent = new Intent(UnbindDeviceActivity.this, EulixAuthenticationActivity.class);
        intent.putExtra(ConstantField.HARDWARE_FUNCTION, ConstantField.HardwareFunction.SECURITY_VERIFICATION);
        intent.putExtra(ConstantField.SECURITY_FUNCTION, ConstantField.SecurityFunction.RESET_PASSWORD);
        if (bluetoothState != 0) {
            intent.putExtra(ConstantField.HARDWARE_INDEX, (bluetoothState > 0 ? ConstantField.HardwareIndex.BLUETOOTH : ConstantField.HardwareIndex.LAN));
            if (baseUrl != null) {
                intent.putExtra(ConstantField.BASE_URL, baseUrl);
            }
            if (bleKey != null) {
                intent.putExtra(ConstantField.BLE_KEY, bleKey);
            }
            if (bleIv != null) {
                intent.putExtra(ConstantField.BLE_IV, bleIv);
            }
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

    private void startFinish(boolean isOk, int code) {
        forceHideSoftInput();
        if (mBridge != null) {
            mBridge.unbindResult(isOk, code, mPassword);
        }
    }

    private void prepareFinish(boolean isOk) {
        startFinish(isOk, isOk ? 200 : 400);
        finish();
    }

    private void prepareFinish(boolean isOk, int code) {
        startFinish(isOk, code);
        finish();
    }

    private void handleResult(boolean isOk) {
        handleResult(isOk, null);
    }

    private void handleResult(boolean isOk, String dataUuid) {
        forceHideSoftInput();
        Intent intent = new Intent();
        setResult(isOk ? Activity.RESULT_OK : Activity.RESULT_CANCELED, intent);
        if (dataUuid != null) {
            intent.putExtra(ConstantField.DATA_UUID, dataUuid);
        }
        finish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
        if (title != null) {
            title.setText(securityFunction == 0 ? (paired == 0 ? R.string.unbind_device : R.string.bind_device) : R.string.security_password_verify);
        }
    }

    @Override
    public void onBackPressed() {
        startFinish(false, 0);
        super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            startFinish(false, 0);
            finish();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    protected void onDestroy() {
        closeLoading();
        if (eulixSixSecurityPasswordView != null) {
            eulixSixSecurityPasswordView.setFocus(false);
        }
        if (mBridge != null) {
            mBridge.unregisterSinkCallback();
            mBridge = null;
        }
        if (mManager != null) {
            mManager.unregisterCallback(activityId);
            mManager = null;
        }
        activityId = null;
        granteeApplyId = null;
        newDeviceApplyId = null;
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }

    @Override
    public void handleResultCallback(boolean isSuccess) {
        if (mHandler != null) {
            mHandler.post(() -> {
                closeLoading();
//                unbindCode.setEnabled(true);
                if (isSuccess) {
                    resetEulixSixSecurityPasswordPattern(false);
                    if (mBridge != null) {
                        mBridge.prepareFinish();
                    }
                    handleResult(true);
                } else {
                    // todo 展示dialog
//                    unbindCode.clearAll();
                    resetEulixSixSecurityPasswordPattern(true);
                }
            });
        }
    }

    @Override
    public void handleAdminRevokeResult(int code, int errorTimes, int leftTryTimes, int tryAfterSeconds) {
        // Do nothing
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    prepareFinish(false, 0);
                    break;
                case R.id.unbind_device_hint_container:
                    showDeviceSecurityPasswordDialog();
                    break;
                case R.id.forget_password:
                    forceHideSoftInput();
                    if (presenter != null) {
                        if (bluetoothState == 0) {
                            int identity = presenter.getActiveIdentity();
                            switch (identity) {
                                case ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY:
                                    if (prepareEulixAuthentication(false)) {
                                        startEulixAuthenticationActivity(null);
                                    }
                                    break;
                                case ConstantField.UserIdentity.ADMINISTRATOR_GRANTEE:
                                    showLoading("");
                                    granteeApplyId = UUID.randomUUID().toString();
                                    presenter.granteeApplyResetSecurityPassword(granteeApplyId);
                                    break;
                                default:
                                    break;
                            }
                        } else if (paired != 0) {
                            handleResetApplyId(true);
                            if (newDeviceSecurityRequestDialog != null) {
                                newDeviceSecurityRequestDialog.handleGranteeRequestResult(GranteeSecurityRequestDialog.NEW_DEVICE_REQUEST_CANCEL);
                            }
                        } else if (mManager != null) {
                            showLoading("");
                            newDeviceApplyId = UUID.randomUUID().toString();
                            NewDeviceApplyResetPasswordEntity entity = new NewDeviceApplyResetPasswordEntity();
                            entity.setDeviceInfo(SystemUtil.getPhoneModel());
                            entity.setClientUuid(DataUtil.getClientUuid(getApplicationContext()));
                            entity.setApplyId(newDeviceApplyId);
                            mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_NEW_DEVICE_APPLY_RESET_PASSWORD
                                    , new Gson().toJson(entity, NewDeviceApplyResetPasswordEntity.class));
//                            mBridge.newDeviceApplyResetPasswordRequest(newDeviceApplyId);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onPrepared(View view) {
        if (unbindCodeContainer != null) {
            unbindCodeContainer.removeAllViews();
            if (view != null) {
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT
                        , getResources().getDimensionPixelSize(R.dimen.dp_55));
                unbindCodeContainer.addView(view, layoutParams);
            }
        }
    }

    @Override
    public void onInserted(String currentValue) {
        //do nothing
    }

    @Override
    public void onComplete(String passwordValue) {
        eulixSixSecurityPasswordView.setEnable(false);
        eulixSixSecurityPasswordView.setFocus(false);
        mPassword = passwordValue;
        showLoading("");
        switch (securityFunction) {
            case ConstantField.SecurityFunction.INITIALIZE_SECURITY_MAILBOX:
            case ConstantField.SecurityFunction.CHANGE_SECURITY_MAILBOX:
            case ConstantField.SecurityFunction.DeveloperOptionsSecurityFunction.OPEN_DEVELOPER_OPTIONS:
                if (presenter != null) {
                    presenter.verifySecurityPassword(passwordValue);
                } else {
                    closeLoading();
                }
                break;
            default:
                if (bluetoothState == 0) {
                    if (presenter != null) {
                        //网络方式，管理员解绑
                        presenter.revokeDevice(passwordValue);
                    }
                } else if (securityFunction == 0) {
                    if (mManager != null) {
                        boolean isHandle = false;
                        if (mManager.isNewBindProcessSupport()) {
                            BindRevokeRequest bindRevokeRequest = new BindRevokeRequest();
                            bindRevokeRequest.setClientUuid(DataUtil.getClientUuid(getApplicationContext()));
                            bindRevokeRequest.setPassword(passwordValue);
                            isHandle = mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_BIND_REVOKE
                                    , new Gson().toJson(bindRevokeRequest, BindRevokeRequest.class));
                        } else {
                            RvokInfo rvokInfo = new RvokInfo();
                            rvokInfo.setPassword(passwordValue);
                            isHandle = mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_REVOKE
                                    , new Gson().toJson(rvokInfo, RvokInfo.class));
                        }
                        if (!isHandle) {
                            closeLoading();
                            showServerExceptionToast();
                            resetEulixSixSecurityPasswordPattern(true);
                        }
                    }
                } else {
                    if (mBridge != null) {
                        mBridge.unbindDevice(passwordValue);
                    }
                }
                break;
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
        AdminRevokeResult adminRevokeResult = null;
        String boxUuid = null;
        int errorTimes = -1;
        int leftTryTimes = -1;
        int tryAfterSeconds = -1;
        AdminRevokeResults adminRevokeResults = null;
        switch (step) {
            case AODeviceDiscoveryManager.STEP_REVOKE:
                if (bodyJson != null) {
                    try {
                        adminRevokeResults = new Gson().fromJson(bodyJson, AdminRevokeResults.class);
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                    }
                }
                if (adminRevokeResults != null) {
                    code = DataUtil.stringCodeToInt(adminRevokeResults.getCode());
                    adminRevokeResult = adminRevokeResults.getResults();
                } else {
                    code = 400;
                }
                if (adminRevokeResult != null) {
                    boxUuid = adminRevokeResult.getBoxUuid();
                    errorTimes = adminRevokeResult.getErrorTimes();
                    leftTryTimes = adminRevokeResult.getLeftTryTimes();
                    tryAfterSeconds = adminRevokeResult.getTryAfterSeconds();
                }
                handleUnbind(code, boxUuid, errorTimes, leftTryTimes, tryAfterSeconds);
                break;
            case AODeviceDiscoveryManager.STEP_BIND_REVOKE:
                BindRevokeResult bindRevokeResult = null;
                if (bodyJson != null) {
                    try {
                        bindRevokeResult = new Gson().fromJson(bodyJson, BindRevokeResult.class);
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                    }
                }
                if (bindRevokeResult != null) {
                    code = DataUtil.stringCodeToInt(bindRevokeResult.getCode());
                    adminRevokeResult = bindRevokeResult.getResults();
                } else {
                    code = 400;
                }
                if (adminRevokeResult != null) {
                    boxUuid = adminRevokeResult.getBoxUuid();
                    errorTimes = adminRevokeResult.getErrorTimes();
                    leftTryTimes = adminRevokeResult.getLeftTryTimes();
                    tryAfterSeconds = adminRevokeResult.getTryAfterSeconds();
                }
                handleUnbind(code, boxUuid, errorTimes, leftTryTimes, tryAfterSeconds);
                break;
            case AODeviceDiscoveryManager.STEP_NEW_DEVICE_APPLY_RESET_PASSWORD:
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
                handleNewDeviceApplyResetPasswordResult(source, code);
                break;
            case AODeviceDiscoveryManager.STEP_SECURITY_MESSAGE_POLL:
                SecurityMessagePollResponse securityMessagePollResponse = null;
                if (bodyJson != null) {
                    try {
                        securityMessagePollResponse = new Gson().fromJson(bodyJson, SecurityMessagePollResponse.class);
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                    }
                }
                xyz.eulix.space.network.agent.SecurityMessagePollResult securityMessagePollResult = null;
                if (securityMessagePollResponse != null) {
                    String codeValue = securityMessagePollResponse.getCode();
                    code = DataUtil.stringCodeToInt(codeValue);
                    source = DataUtil.stringCodeGetSource(codeValue);
                    List<xyz.eulix.space.network.agent.SecurityMessagePollResult> securityMessagePollResults = securityMessagePollResponse.getResults();
                    if (securityMessagePollResults != null) {
                        for (xyz.eulix.space.network.agent.SecurityMessagePollResult pollResult : securityMessagePollResults) {
                            if (pollResult != null && newDeviceApplyId != null && newDeviceApplyId.equals(pollResult.getApplyId())) {
                                securityMessagePollResult = pollResult;
                                break;
                            }
                        }
                    }
                } else {
                    code = 500;
                    source = null;
                }
                handleSecurityMessagePollResult(source, code, newDeviceApplyId, securityMessagePollResult);
                break;
            default:
                break;
        }
    }

    @Override
    public void handleUnbind(int result, String boxUuid, int errorTimes, int leftTryTimes, int tryAfterSeconds) {
        if (mHandler != null) {
            mHandler.post(() -> {
                closeLoading();
//                unbindCode.setEnabled(true);
                if (result >= 200 && result < 400) {
                    unbindErrorHint.setText("");
                    if (bluetoothState == 0) {
                        showImageTextToast(R.drawable.toast_right, R.string.unbind_toast_success);
                    }
                    resetEulixSixSecurityPasswordPattern(false);
                    prepareFinish(true, 200);
                } else if (result == ConstantField.RevokeCode.REVOKE_PASSWORD_EXCEED) {
//                    unbindCode.clearAll();
                    resetEulixSixSecurityPasswordPattern(false);
                    prepareFinish(false, result);
                } else if (result == ConstantField.RevokeCode.REVOKE_NOT_PAIR) {
//                    unbindCode.clearAll();
                    resetEulixSixSecurityPasswordPattern(false);
                    prepareFinish(true, result);
                } else if (result == ConstantField.RevokeCode.REVOKE_PASSWORD_WRONG) {
//                    unbindCode.clearAll();
                    if (leftTryTimes > 0) {
                        StringBuilder unbindErrorTextBuilder = new StringBuilder();
                        unbindErrorTextBuilder.append(getString(R.string.unbind_error_hint_content_part_1));
                        unbindErrorTextBuilder.append(leftTryTimes);
                        if (leftTryTimes > 1) {
                            unbindErrorTextBuilder.append(getString(R.string.unbind_error_hint_content_part_2_plural));
                        } else {
                            unbindErrorTextBuilder.append(getString(R.string.unbind_error_hint_content_part_2_singular));
                        }
                        unbindErrorHint.setText(unbindErrorTextBuilder.toString());
                        resetEulixSixSecurityPasswordPattern(true);
                    } else {
                        resetEulixSixSecurityPasswordPattern(false);
                        prepareFinish(false, ConstantField.RevokeCode.REVOKE_PASSWORD_EXCEED);
                    }
                } else if (result == ConstantField.RevokeCode.REVOKE_SERVICE_ERROR) {
//                    unbindCode.clearAll();
                    resetEulixSixSecurityPasswordPattern(false);
                    prepareFinish(false, result);
                } else {
                    unbindErrorHint.setText("");
                    if (result == ConstantField.ErrorCode.SPACE_PLATFORM_CONNECT_ERROR) {
                        showImageTextToast(R.drawable.toast_refuse, R.string.platform_connect_error);
                    } else {
                        showImageTextToast(R.drawable.toast_wrong, R.string.unbind_toast_failed);
                    }
//                    unbindCode.clearAll();
                    resetEulixSixSecurityPasswordPattern(true);
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

    @Override
    public void handleNewDeviceApplyResetPasswordResult(String source, int code) {
        if (mHandler != null) {
            mHandler.post(() -> {
                closeLoading();
                if (code >= 200 && code < 400) {
                    while (mHandler.hasMessages(NEW_DEVICE_APPLY)) {
                        mHandler.removeMessages(NEW_DEVICE_APPLY);
                    }
                    mHandler.sendEmptyMessageDelayed(NEW_DEVICE_APPLY, (10 * ConstantField.TimeUnit.MINUTE_UNIT));
                    if (newDeviceSecurityRequestDialog != null) {
                        newDeviceSecurityRequestDialog.showGranteeRequestDialog();
                    }
                    pollSecurityMessage(true);
                } else if (code == ConstantField.KnownError.AccountCommonError.ACCOUNT_410 && ConstantField.KnownSource.ACCOUNT.equals(source)) {
                    if (newDeviceSecurityRequestDialog != null) {
                        newDeviceSecurityRequestDialog.handleGranteeRequestResult(GranteeSecurityRequestDialog.REQUEST_TOO_MANY);
                    }
                } else {
                    showImageTextToast(R.drawable.toast_refuse, R.string.apply_authorization_fail);
                }
            });
        }
    }

    @Override
    public void handleSecurityMessagePollResult(String source, int code, String applyId, xyz.eulix.space.network.agent.SecurityMessagePollResult result) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (newDeviceApplyId != null && newDeviceApplyId.equals(applyId)) {
                    if (code >= 200 && code < 400 && result != null) {
                        handleResetApplyId(true);
                        boolean isAccept = result.isAccept();
                        if (newDeviceSecurityRequestDialog != null) {
                            newDeviceSecurityRequestDialog.handleGranteeRequestResult(isAccept
                                    ? GranteeSecurityRequestDialog.REQUEST_ACCEPT : GranteeSecurityRequestDialog.REQUEST_DENY);
                        }
                        if (isAccept) {
                            newDeviceSecurityTokenResult = result.getSecurityTokenRes();
                            granterClientUuid = result.getClientUuid();
                            showLoading("");
                            if (mManager != null) {
                                mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_SECURITY_EMAIL_SETTING, null);
                            }
                        }
                    } else {
                        switch (code) {
                            case -3:
                                handleResetApplyId(true);
                                if (newDeviceSecurityRequestDialog != null) {
                                    newDeviceSecurityRequestDialog.dismissGranteeRequestDialog();
                                }
                                break;
                            default:
                                pollSecurityMessage(true);
                                break;
                        }
                    }
                }
            });
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
                    handleResetApplyId(false);
                    boolean isAccept = result.isAccept();
                    if (granteeSecurityRequestDialog != null) {
                        granteeSecurityRequestDialog.handleGranteeRequestResult(isAccept
                                ? GranteeSecurityRequestDialog.REQUEST_ACCEPT : GranteeSecurityRequestDialog.REQUEST_DENY);
                    }
                    if (isAccept) {
                        SecurityTokenResult securityTokenResult = result.getSecurityTokenResult();
                        tempGranterSecurityTokenResult = securityTokenResult;
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
                            handleResetApplyId(false);
                            if (granteeSecurityRequestDialog != null) {
                                granteeSecurityRequestDialog.dismissGranteeRequestDialog();
                            }
                            break;
                        default:
                            pollSecurityMessage(false);
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
                    if (needGranterDataUuid && tempGranterSecurityTokenResult != null) {
                        granterDataUuid = DataUtil.setData(new Gson().toJson(tempGranterSecurityTokenResult, SecurityTokenResult.class));
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
