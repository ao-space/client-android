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
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.airbnb.lottie.LottieAnimationView;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.UUID;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.EulixSpaceService;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bridge.BindResultBridge;
import xyz.eulix.space.event.AccessTokenResultEvent;
import xyz.eulix.space.manager.AODeviceDiscoveryManager;
import xyz.eulix.space.network.agent.disk.DiskRecognitionResult;
import xyz.eulix.space.network.agent.disk.ReadyCheckResult;
import xyz.eulix.space.network.agent.platform.SwitchPlatformRequest;
import xyz.eulix.space.network.agent.platform.SwitchPlatformResult;
import xyz.eulix.space.presenter.BindResultPresenter;
import xyz.eulix.space.ui.EulixMainActivity;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.DebugUtil;
import xyz.eulix.space.util.LottieUtil;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.ViewUtils;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/11/1 15:28
 */
public class BindResultActivity extends AbsActivity<BindResultPresenter.IBindResult, BindResultPresenter> implements BindResultPresenter.IBindResult, View.OnClickListener, AODeviceDiscoveryManager.AODeviceDiscoverySinkCallback, BindResultBridge.BindResultSinkCallback {
    private static final int REQUEST_ACCESS_TOKEN = 1;
    private static final int REQUEST_DISK_RECOGNITION = REQUEST_ACCESS_TOKEN + 1;
    private String activityId;
    private ImageButton back;
    private TextView title;
    private LinearLayout bindResultContainer;
    private ImageView bindResultImage;
    private TextView bindResultText;
    private TextView bindResultHint;
    private Button bindReturn;
    private LinearLayout bindFunctionContainer;
    private TextView bindFunctionText;
    private LottieAnimationView bindFunctionLoading;
//    private Button bindFunction;
    private Button bindFunctionCancel;
    private LinearLayout uninitializedContainer;
    private ImageView uninitializedIndicator;
    private TextView uninitializedHint;
    private TextView wifiStateSsid;
    private ImageView wifiStateSignal;
    private Button diskInitializeButton;
    private Boolean mType;
    private Integer mResult;
    private String mPlatformUrl;
    private Dialog unbindDeviceDialog;
    private TextView dialogTitle;
    private TextView dialogContent;
    private Button dialogCancel;
    private Button dialogConfirm;

    private TextView switchSuccessDialogTitle;
    private TextView switchSuccessDialogContent;
    private Button switchSuccessDialogConfirm;
    private Dialog switchSuccessDialog;

    private AODeviceDiscoveryManager mManager;
    private long mExitTime = 0L;
    private boolean bindResultRetry = false;
    private boolean bindResultRetryFail = false;
    private String wlanSsid = null;
    private Boolean isWire = null;
    private boolean spaceTokenReady = false;
    private boolean waitingTokenReady = false;
    private Integer mDiskInitialize = null;
    private boolean diskRecognitionReady = false;
    private boolean waitingDiskTokenReady = false;
    private String mBoxUuid;
    private boolean isNoMainStorage;
    private BindResultHandler mHandler;
    private DiskRecognitionResult mDiskRecognitionResult;
    private String mTaskId;

    static class BindResultHandler extends Handler {
        private WeakReference<BindResultActivity> bindResultActivityWeakReference;

        public BindResultHandler(BindResultActivity activity) {
            bindResultActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            BindResultActivity activity = bindResultActivityWeakReference.get();
            if (activity == null) {
                super.handleMessage(msg);
            } else {
                switch (msg.what) {
                    case REQUEST_ACCESS_TOKEN:
                        activity.obtainAccessToken();
                        break;
                    case REQUEST_DISK_RECOGNITION:
                        activity.requestDiskRecognition();
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
        setContentView(R.layout.activity_bind_result);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        bindResultContainer = findViewById(R.id.bind_result_container);
        bindResultImage = findViewById(R.id.bind_result_image);
        bindResultText = findViewById(R.id.bind_result_text);
        bindResultHint = findViewById(R.id.bind_result_hint);
        bindReturn = findViewById(R.id.bind_return);
        bindFunctionContainer = findViewById(R.id.loading_button_container);
        bindFunctionLoading = findViewById(R.id.loading_animation);
        bindFunctionText = findViewById(R.id.loading_content);
//        bindFunction = findViewById(R.id.bind_function);
        bindFunctionCancel = findViewById(R.id.bind_function_cancel);
        uninitializedContainer = findViewById(R.id.uninitialized_container);
        uninitializedIndicator = findViewById(R.id.uninitialized_indicator);
        uninitializedHint = findViewById(R.id.uninitialized_hint);
        wifiStateSsid = findViewById(R.id.wifi_state_ssid);
        wifiStateSignal = findViewById(R.id.wifi_state_signal);
        diskInitializeButton = findViewById(R.id.disk_initialize_button);
        View unbindDeviceDialogView = LayoutInflater.from(this).inflate(R.layout.eulix_space_two_button_dialog, null);
        dialogTitle = unbindDeviceDialogView.findViewById(R.id.dialog_title);
        dialogContent = unbindDeviceDialogView.findViewById(R.id.dialog_content);
        dialogCancel = unbindDeviceDialogView.findViewById(R.id.dialog_cancel);
        dialogConfirm = unbindDeviceDialogView.findViewById(R.id.dialog_confirm);
        unbindDeviceDialog = new Dialog(this, R.style.EulixDialog);
        unbindDeviceDialog.setCancelable(false);
        unbindDeviceDialog.setContentView(unbindDeviceDialogView);

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
        mHandler = new BindResultHandler(this);
        handleIntent(getIntent());
        activityId = UUID.randomUUID().toString();
        mManager = AODeviceDiscoveryManager.getInstance();
        mManager.registerCallback(activityId, this);
    }

    @Override
    public void initViewData() {
        back.setVisibility(View.GONE);
        bindFunctionCancel.setText(R.string.return_back);
        dialogTitle.setText(R.string.unbind_device);
        dialogContent.setText(R.string.unbind_content);
        dialogConfirm.setText(R.string.ok);

        switchSuccessDialogTitle.setText(R.string.switch_success);
        switchSuccessDialogConfirm.setText(R.string.confirm_ok);
    }

    @Override
    public void initEvent() {
        bindReturn.setOnClickListener(this);
        bindFunctionContainer.setOnClickListener(this);
//        bindFunction.setOnClickListener(this);
        bindFunctionCancel.setOnClickListener(this);
        dialogCancel.setOnClickListener(this);
        dialogConfirm.setOnClickListener(this);
        diskInitializeButton.setOnClickListener(this);
        switchSuccessDialogConfirm.setOnClickListener(v -> {
            dismissSwitchSuccessDialog();
            handleResult(true);
        });
        handleView();
        if (mType != null && mType && mResult != null && mResult < 400) {
            if (mDiskInitialize == null) {
                prepareObtainAccessToken(0);
            } else {
                prepareRequestDiskRecognition(0);
            }
        }
    }

    @Override
    protected int getActivityIndex() {
        return BIND_SERIES_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public BindResultPresenter createPresenter() {
        return new BindResultPresenter();
    }

    @Override
    protected void resetStatusBar() {
        super.resetStatusBar();
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    private void handleIntent(Intent intent) {
        if (intent == null) {
            intent = getIntent();
        }
        if (intent != null) {
            bindResultRetry = intent.getBooleanExtra(ConstantField.BIND_RESULT_RETRY, false);
            bindResultRetryFail = intent.getBooleanExtra(ConstantField.BIND_RESULT_RETRY_FAIL, false);
            if (intent.hasExtra(ConstantField.WLAN_SSID)) {
                wlanSsid = intent.getStringExtra(ConstantField.WLAN_SSID);
            }
            isWire = null;
            int wireValue = intent.getIntExtra(ConstantField.IS_WIRE, 0);
            if (wireValue > 0) {
                isWire = true;
            } else if (wireValue < 0) {
                isWire = false;
            }
            isNoMainStorage = intent.getBooleanExtra(ConstantField.DISK_INITIALIZE_NO_MAIN_STORAGE, false);
            if (intent.hasExtra(ConstantField.DISK_INITIALIZE)) {
                mDiskInitialize = intent.getIntExtra(ConstantField.DISK_INITIALIZE, ReadyCheckResult.DISK_NORMAL);
            }
            if (intent.hasExtra(ConstantField.BOX_UUID)) {
                mBoxUuid = intent.getStringExtra(ConstantField.BOX_UUID);
            }
            if (intent.hasExtra(ConstantField.BIND_TYPE)) {
                mType = intent.getBooleanExtra(ConstantField.BIND_TYPE, true);
            }
            if (intent.hasExtra(ConstantField.BIND_RESULT)) {
                mResult = intent.getIntExtra(ConstantField.BIND_RESULT, 400);
            }
            if (intent.hasExtra(ConstantField.PLATFORM_URL)) {
                mPlatformUrl = intent.getStringExtra(ConstantField.PLATFORM_URL);
            }
        }
    }

    private void handleView() {
        if (mType != null && mResult != null) {
            title.setText(mType ? R.string.bind_result : R.string.unbind_device);
            bindResultImage.setImageResource(mResult < 400 ? R.drawable.bind_success_2x : R.drawable.bind_failed_2x);
            bindResultText.setText(mType ? (mResult < 400 ? R.string.bind_device_success : R.string.bind_device_failed)
                    : (mResult < 400 ? R.string.unbind_success : R.string.unbind_fail));
            String failHint = "";
            if (mResult >= 400) {
                switch (mResult) {
                    case ConstantField.BindDeviceHttpCode.BIND_CONFLICT_CODE:
                        failHint = getString(R.string.bind_device_failed_hint_409);
                        break;
                    case ConstantField.RevokeCode.REVOKE_PASSWORD_EXCEED:
                        failHint = getString(R.string.bind_device_failed_hint_461);
                        break;
                    case ConstantField.BindDeviceHttpCode.BIND_PLATFORM_FAILED:
                        failHint = (StringUtil.isNonBlankString(mPlatformUrl) ? (mPlatformUrl + " ") : "")
                                + getString(R.string.space_platform_not_available);
                        break;
                    default:
                        failHint = getString(R.string.bind_device_failed_hint_400);
                        break;
                }
                if (mType) {
                    bindResultHint.setText(failHint);
                } else {
                    bindResultHint.setText(R.string.unbind_fail_hint);
                }
            } else {
                bindResultHint.setText(R.string.bind_success_hint);
            }
            bindResultHint.setVisibility((mType || mResult >= 400) ? View.VISIBLE : View.INVISIBLE);
            bindReturn.setVisibility((mType && (mResult < 400 || mResult == ConstantField.BindDeviceHttpCode.BIND_PLATFORM_FAILED))
                    ? View.INVISIBLE : View.VISIBLE);
            setOneButtonLoadingPattern(true, false, getString(mResult < 400
                    ? (mDiskInitialize == null ? R.string.start_use : (mDiskInitialize == ReadyCheckResult.DISK_NORMAL
                    ? R.string.disk_check : R.string.disk_initialization)) : ((mType && mResult == ConstantField.BindDeviceHttpCode.BIND_PLATFORM_FAILED)
                    ? R.string.switch_official_platform_to_bind : R.string.unbind_device)));
            bindFunctionCancel.setClickable((mType && mResult == ConstantField.BindDeviceHttpCode.BIND_PLATFORM_FAILED));
            bindFunctionCancel.setVisibility((mType && mResult == ConstantField.BindDeviceHttpCode.BIND_PLATFORM_FAILED)
                    ? View.VISIBLE : View.INVISIBLE);
            bindFunctionContainer.setVisibility((mType && (mResult < 400 || mResult == ConstantField.BindDeviceHttpCode.BIND_PLATFORM_FAILED))
                    ? View.VISIBLE : View.INVISIBLE);
        }
        bindResultContainer.setVisibility(bindResultRetry ? View.GONE : View.VISIBLE);
        uninitializedContainer.setVisibility(bindResultRetry ? View.VISIBLE : View.GONE);
        if (bindResultRetry) {
            handleUninitializedContainerPattern(bindResultRetryFail ? false : null);
        }
    }

    private void setOneButtonLoadingPattern(boolean isClickable, boolean isLoading, @Nullable String text) {
        if (bindFunctionContainer != null) {
            bindFunctionContainer.setClickable(isClickable);
        }
        if (bindFunctionLoading != null) {
            if (isLoading) {
                bindFunctionLoading.setVisibility(View.VISIBLE);
                LottieUtil.loop(bindFunctionLoading, "loading_button.json");
            } else {
                LottieUtil.stop(bindFunctionLoading);
                bindFunctionLoading.setVisibility(View.GONE);
            }
        }
        if (text != null && bindFunctionText != null) {
            bindFunctionText.setText(text);
        }
    }

    private void showUnbindDeviceDialog() {
        if (unbindDeviceDialog != null && !unbindDeviceDialog.isShowing()) {
            unbindDeviceDialog.show();
            Window window = unbindDeviceDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(getResources().getDimensionPixelSize(R.dimen.dp_259), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissUnbindDeviceDialog() {
        if (unbindDeviceDialog != null && unbindDeviceDialog.isShowing()) {
            unbindDeviceDialog.dismiss();
        }
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

    private void prepareObtainAccessToken(long delayMillis) {
        if (mHandler == null) {
            obtainAccessToken();
        } else {
            while (mHandler.hasMessages(REQUEST_ACCESS_TOKEN)) {
                mHandler.removeMessages(REQUEST_ACCESS_TOKEN);
            }
            if (delayMillis > 0) {
                mHandler.sendEmptyMessageDelayed(REQUEST_ACCESS_TOKEN, delayMillis);
            } else {
                mHandler.sendEmptyMessage(REQUEST_ACCESS_TOKEN);
            }
        }
    }

    /**
     * 请求token
     */
    private void obtainAccessToken() {
        Intent serviceIntent = new Intent(BindResultActivity.this, EulixSpaceService.class);
        serviceIntent.setAction(ConstantField.Action.TOKEN_ACTION);
        if (mBoxUuid != null) {
            serviceIntent.putExtra(ConstantField.BOX_UUID, mBoxUuid);
        }
        serviceIntent.putExtra(ConstantField.BOX_BIND, "1");
        serviceIntent.putExtra(ConstantField.FORCE, true);
        startService(serviceIntent);
    }

    private void prepareRequestDiskRecognition(long delayMillis) {
        if (mHandler == null) {
            requestDiskRecognition();
        } else {
            while (mHandler.hasMessages(REQUEST_DISK_RECOGNITION)) {
                mHandler.removeMessages(REQUEST_DISK_RECOGNITION);
            }
            if (delayMillis > 0) {
                mHandler.sendEmptyMessageDelayed(REQUEST_DISK_RECOGNITION, delayMillis);
            } else {
                mHandler.sendEmptyMessage(REQUEST_DISK_RECOGNITION);
            }
        }
    }

    private void requestDiskRecognition() {
        if (mManager != null) {
            mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_DISK_RECOGNITION, null);
        }
    }

    private void handleDiskInitialize() {
        String dataUuid = null;
        if (mDiskRecognitionResult != null) {
            dataUuid = DataUtil.setData(new Gson().toJson(mDiskRecognitionResult, DiskRecognitionResult.class));
        }
        Intent intent = new Intent(BindResultActivity.this, DiskInitializationActivity.class);
        intent.putExtra(ConstantField.DISK_INITIALIZE_NO_MAIN_STORAGE, isNoMainStorage);
        if (mBoxUuid != null) {
            intent.putExtra(ConstantField.BOX_UUID, mBoxUuid);
        }
        if (dataUuid != null) {
            intent.putExtra(ConstantField.DATA_UUID, dataUuid);
        }
        if (mDiskInitialize != null) {
            intent.putExtra(ConstantField.DISK_INITIALIZE, mDiskInitialize.intValue());
        }
        startActivity(intent);
        finish();
    }

    private void handleUninitializedContainerPattern(Boolean status) {
        uninitializedIndicator.clearAnimation();
        if (status == null) {
            uninitializedIndicator.setImageDrawable(null);
            uninitializedHint.setText("");
        } else {
            uninitializedIndicator.setImageResource(status ? R.drawable.icon_loading_2x : R.drawable.icon_exception_2x);
            uninitializedHint.setText(status ? R.string.entering_disk_initialization_progress : R.string.enter_disk_initialization_progress_fail);
        }
        if (status != null && status) {
            ViewUtils.setLoadingAnim(this, uninitializedIndicator);
        }
        wifiStateSsid.setText(StringUtil.nullToEmpty(wlanSsid));
        if (wlanSsid == null || isWire == null) {
            wifiStateSignal.setImageDrawable(null);
        } else {
            wifiStateSignal.setImageResource(isWire ? R.drawable.ethernet_2x : R.drawable.wifi_4_encrypt_2x);
        }
        diskInitializeButton.setText((status == null || status) ? R.string.disk_initialization : R.string.quit);
        diskInitializeButton.setClickable((status == null || !status));
        diskInitializeButton.setBackgroundResource((status == null || !status) ? R.drawable.background_ff337aff_ff16b9ff_rectangle_10 : R.drawable.background_ffdfe0e5_rectangle_10);
    }

    private void handleFinish() {
//        if (mBridge != null) {
//            mBridge.bindResultFinish();
//        }
        if (mManager != null) {
            mManager.finishSource();
        }
        finish();
    }

    private void handleResult(boolean isOk) {
        Intent intent = new Intent();
        setResult(isOk ? Activity.RESULT_OK : Activity.RESULT_CANCELED, intent);
        finish();
    }

    private void confirmForceExit() {
        long currentTimeMillis = System.currentTimeMillis();
        if (currentTimeMillis - mExitTime > 2000) {
            showDefaultPureTextToast(R.string.app_exit_hint);
            mExitTime = currentTimeMillis;
        } else {
            EulixSpaceApplication.popAllOldActivity(null);
        }
    }

    private void goMain() {
        Intent intent = new Intent(BindResultActivity.this, EulixMainActivity.class);
        startActivity(intent);
        handleFinish();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
        handleView();
        if (mType != null && mType && mResult != null && mResult < 400) {
            if (mDiskInitialize == null) {
                prepareObtainAccessToken(0);
            } else {
                prepareRequestDiskRecognition(0);
            }
        }
    }

    @Override
    public void onBackPressed() {
        confirmForceExit();
    }

    @Override
    protected void onDestroy() {
        if (mManager != null) {
            mManager.unregisterCallback(activityId);
            mManager = null;
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            confirmForceExit();
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.bind_return:
                    handleFinish();
                    break;
                case R.id.loading_button_container:
                    if (mType != null && mType && mResult != null) {
                        if (mResult < 400) {
                            if (mDiskInitialize == null) {
                                if (spaceTokenReady) {
                                    goMain();
                                } else {
                                    setOneButtonLoadingPattern(false, true, getString(R.string.entering));
                                    waitingTokenReady = true;
                                }
                            } else {
                                if (diskRecognitionReady) {
                                    handleDiskInitialize();
                                } else {
                                    setOneButtonLoadingPattern(false, true, getString(R.string.entering));
                                    waitingDiskTokenReady = true;
                                }
                            }
                        } else {
                            switch (mResult) {
                                case ConstantField.BindDeviceHttpCode.BIND_PLATFORM_FAILED:
                                    if (mManager != null) {
                                        showLoading(getString(R.string.switch_space_platform_hint));
                                        mTaskId = String.valueOf(System.currentTimeMillis());
                                        SwitchPlatformRequest switchPlatformRequest = new SwitchPlatformRequest();
                                        switchPlatformRequest.setTransId(mTaskId);
                                        switchPlatformRequest.setDomain(StringUtil.urlToHost(DebugUtil.getOfficialEnvironmentServices()));
                                        mManager.request(activityId, UUID.randomUUID().toString(), AODeviceDiscoveryManager.STEP_SWITCH_PLATFORM
                                                , new Gson().toJson(switchPlatformRequest, SwitchPlatformRequest.class));
                                    }
                                    break;
                                default:
                                    showUnbindDeviceDialog();
                                    break;
                            }
                        }
                    }
                    break;
                case R.id.bind_function_cancel:
                    handleResult(false);
                    break;
                case R.id.dialog_cancel:
                    dismissUnbindDeviceDialog();
                    break;
                case R.id.dialog_confirm:
                    dismissUnbindDeviceDialog();
                    Intent intent = new Intent(BindResultActivity.this, UnbindDeviceActivity.class);
                    intent.putExtra("bluetooth", true);
                    startActivityForResult(intent, ConstantField.RequestCode.UNBIND_DEVICE_CODE);
                    break;
                case R.id.disk_initialize_button:
                    if (mType != null && mType && mResult != null) {
                        if (mResult < 400) {
                            if (mDiskInitialize != null) {
                                if (bindResultRetryFail) {
                                    EulixSpaceApplication.popAllOldActivity(null);
                                } else {
                                    if (diskRecognitionReady) {
                                        handleDiskInitialize();
                                    } else {
                                        handleUninitializedContainerPattern(true);
                                        waitingDiskTokenReady = true;
                                    }
                                }
                            }
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
            case AODeviceDiscoveryManager.STEP_DISK_RECOGNITION:
                DiskRecognitionResult diskRecognitionResult = null;
                if (bodyJson != null) {
                    try {
                        diskRecognitionResult = new Gson().fromJson(bodyJson, DiskRecognitionResult.class);
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                    }
                }
                handleDiskRecognitionResponse(code, source, diskRecognitionResult);
                break;
            case AODeviceDiscoveryManager.STEP_SWITCH_PLATFORM:
                SwitchPlatformResult switchPlatformResult = null;
                if (bodyJson != null) {
                    try {
                        switchPlatformResult = new Gson().fromJson(bodyJson, SwitchPlatformResult.class);
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                    }
                }
                handleSwitchPlatformResponse(code, source, switchPlatformResult);
                break;
            default:
                break;
        }
    }

    @Override
    public void handleDisconnect() {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (mDiskInitialize != null) {
                    finish();
                }
            });
        }
    }

    @Override
    public void handleDiskRecognitionResponse(int code, String source, DiskRecognitionResult result) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (code >= 200 && code < 400 && result != null) {
                    mDiskRecognitionResult = result;
                    if (waitingDiskTokenReady) {
                        setOneButtonLoadingPattern(true, false, getString((mDiskInitialize != null
                                && mDiskInitialize == ReadyCheckResult.DISK_NORMAL ? R.string.disk_check : R.string.disk_initialization)));
                        waitingDiskTokenReady = false;
                        handleDiskInitialize();
                    }
                    diskRecognitionReady = true;
                } else {
                    prepareRequestDiskRecognition(10 * ConstantField.TimeUnit.SECOND_UNIT);
                }
            });
        }
    }

    @Override
    public void handleSwitchPlatformResponse(int code, String source, SwitchPlatformResult result) {
        if (mHandler != null) {
            mHandler.post(() -> {
                boolean isSuccess = (code >= 200 && code < 400 && result != null);
                if (isSuccess) {
                    if (mTaskId != null) {
                        if (mTaskId.equals(result.getTransId())) {
                            mTaskId = null;
                        } else {
                            isSuccess = false;
                        }
                    }
                }
                closeLoading();
                if (isSuccess) {
                    String content = getString(R.string.switch_official_space_platform_hint) + "\n" + DebugUtil.getOfficialEnvironmentServices();
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ConstantField.RequestCode.UNBIND_DEVICE_CODE:
                mType = false;
                mResult = (resultCode == Activity.RESULT_OK ? 200 : 400);
                handleView();
                break;
            default:
                break;
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(AccessTokenResultEvent event) {
        if (event != null) {
            String boxUuid = event.getBoxUuid();
            String boxBind = event.getBoxBind();
            Boolean result = event.getResult();
            long expireTimestamp = event.getExpireTimestamp();
            if (mBoxUuid != null && mBoxUuid.equals(boxUuid) && "1".equals(boxBind)) {
                if (result == null) {
                    prepareObtainAccessToken(10 * ConstantField.TimeUnit.SECOND_UNIT);
                } else if (result) {
                    if (expireTimestamp <= System.currentTimeMillis()) {
                        prepareObtainAccessToken(0);
                    } else if (presenter != null) {
                        presenter.changeActiveBox(boxUuid, expireTimestamp);
                        if (waitingTokenReady) {
                            setOneButtonLoadingPattern(true, false, getString(R.string.start_use));
                            waitingTokenReady = false;
                            goMain();
                        }
                        spaceTokenReady = true;
                    }
                } else {
                    if (waitingTokenReady) {
                        setOneButtonLoadingPattern(true, false, getString(R.string.start_use));
                        waitingTokenReady = false;
                        goMain();
                    }
                    spaceTokenReady = true;
                }
            }
        }
    }
}
