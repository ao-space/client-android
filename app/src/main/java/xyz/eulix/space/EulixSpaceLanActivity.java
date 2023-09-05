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

package xyz.eulix.space;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.nsd.NsdServiceInfo;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.View;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.IPBean;
import xyz.eulix.space.bean.bind.InitResponse;
import xyz.eulix.space.callback.EulixSpaceLanCallback;
import xyz.eulix.space.network.agent.AdminRevokeResult;
import xyz.eulix.space.network.agent.SecurityMessagePollResult;
import xyz.eulix.space.network.agent.WifiInfo;
import xyz.eulix.space.network.agent.disk.DiskInitializeProgressResult;
import xyz.eulix.space.network.agent.disk.DiskManageListResult;
import xyz.eulix.space.network.agent.disk.DiskRecognitionResult;
import xyz.eulix.space.network.agent.disk.ReadyCheckResult;
import xyz.eulix.space.network.agent.net.NetworkStatusResult;
import xyz.eulix.space.network.agent.platform.SwitchPlatformResult;
import xyz.eulix.space.network.agent.platform.SwitchStatusResult;
import xyz.eulix.space.presenter.EulixSpaceLanPresenter;
import xyz.eulix.space.ui.mine.security.ModifySecurityPasswordActivity;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StringUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/7/11 14:33
 */
public abstract class EulixSpaceLanActivity extends AbsActivity<EulixSpaceLanPresenter.IEulixSpaceLan
        , EulixSpaceLanPresenter> implements EulixSpaceLanPresenter.IEulixSpaceLan, View.OnClickListener, EulixSpaceLanCallback {
    private static final String TAG = EulixSpaceLanActivity.class.getSimpleName();
    protected static final int SCAN_COUNTDOWN_SECOND = 30;
    private static final int BIND_EULIX_SPACE_LAN_SERVICE_DELAY = 2000;
    private static final int GET_EULIX_SPACE_LAN_BINDER_DELAY = 1000;
    private static final int BIND_EULIX_SPACE_LAN_SERVICE = 1;
    private static final int GET_EULIX_SPACE_LAN_BINDER = BIND_EULIX_SPACE_LAN_SERVICE + 1;
    private EulixSpaceLanHandler mHandler;
    private EulixSpaceLanService.EulixSpaceLanBinder eulixSpaceLanBinder;
    protected List<NsdServiceInfo> nsdServiceInfos;
    protected int bindDeviceStatus;
    private boolean isBindEulixSpaceLanService;
    protected boolean isActiveStartSearchDevice;
    private String deviceName;
    protected String btid;
    private String deviceAddress;
    protected int mHardwareFunction = 0;
    protected int securityFunction = 0;
    protected String authenticationUuid;
    protected int authenticationFunction;
    protected String granterDataUuid;
    protected boolean isDeviceListDiskInitialize;
    protected IPBean mIpBean;

    static class EulixSpaceLanHandler extends Handler {
        private WeakReference<EulixSpaceLanActivity> eulixSpaceLanActivityWeakReference;

        public EulixSpaceLanHandler(EulixSpaceLanActivity activity) {
            eulixSpaceLanActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            EulixSpaceLanActivity activity = eulixSpaceLanActivityWeakReference.get();
            if (activity == null) {
                super.handleMessage(msg);
            } else {
                switch (msg.what) {
                    case BIND_EULIX_SPACE_LAN_SERVICE:
                        if (activity.bindEulixSpaceLanService()) {
                            sendEmptyMessage(GET_EULIX_SPACE_LAN_BINDER);
                        } else {
                            sendEmptyMessageDelayed(BIND_EULIX_SPACE_LAN_SERVICE, BIND_EULIX_SPACE_LAN_SERVICE_DELAY);
                        }
                        break;
                    case GET_EULIX_SPACE_LAN_BINDER:
                        if (activity.eulixSpaceLanBinder == null) {
                            sendEmptyMessageDelayed(GET_EULIX_SPACE_LAN_BINDER, GET_EULIX_SPACE_LAN_BINDER_DELAY);
                        } else {
                            activity.eulixSpaceLanBinder.registerCallback(activity);
                            if (!activity.isActiveStartSearchDevice) {
                                activity.prepareStartSearchDevice();
                            }
                        }
                        break;
                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        }
    }

    private ServiceConnection eulixSpaceLanServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if (service instanceof EulixSpaceLanService.EulixSpaceLanBinder) {
                eulixSpaceLanBinder = (EulixSpaceLanService.EulixSpaceLanBinder) service;
                isBindEulixSpaceLanService = true;
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBindEulixSpaceLanService = false;
            eulixSpaceLanBinder = null;
            if (mHandler != null) {
                mHandler.sendEmptyMessageDelayed(BIND_EULIX_SPACE_LAN_SERVICE, BIND_EULIX_SPACE_LAN_SERVICE_DELAY);
            }
        }
    };

    @Override
    public void initData() {
        mHandler = new EulixSpaceLanHandler(this);
        isActiveStartSearchDevice = false;
        isBindEulixSpaceLanService = false;
        handleIntent(getIntent());
    }

    @Override
    public void initEvent() {
        if (mIpBean == null) {
            mHandler.sendEmptyMessage(BIND_EULIX_SPACE_LAN_SERVICE);
        } else {
            directConnectDevice();
        }
    }

    protected void bindDeviceStatusChange(int status) {}

    protected void setBoxSearingIndicator(int index) {}

    protected void handleIntent(Intent intent) {
        if (intent == null) {
            intent = getIntent();
        }
        if (intent != null) {
            isDeviceListDiskInitialize = intent.getBooleanExtra(ConstantField.DISK_INITIALIZE_DEVICE_LIST, false);
            mHardwareFunction = intent.getIntExtra(ConstantField.HARDWARE_FUNCTION, 0);
            securityFunction = intent.getIntExtra(ConstantField.SECURITY_FUNCTION, 0);
            if (intent.hasExtra(ConstantField.DEVICE_NAME)) {
                deviceName = intent.getStringExtra(ConstantField.DEVICE_NAME);
            }
            if (intent.hasExtra(ConstantField.BLUETOOTH_ID)) {
                btid = intent.getStringExtra(ConstantField.BLUETOOTH_ID);
            }
            if (intent.hasExtra(ConstantField.DEVICE_ADDRESS)) {
                deviceAddress = intent.getStringExtra(ConstantField.DEVICE_ADDRESS);
            }
            if (intent.hasExtra(ConstantField.AUTHENTICATION_UUID)) {
                authenticationUuid = intent.getStringExtra(ConstantField.AUTHENTICATION_UUID);
            }
            authenticationFunction = intent.getIntExtra(ConstantField.AUTHENTICATION_FUNCTION, 0);
            if (intent.hasExtra(ConstantField.GRANTER_DATA_UUID)) {
                granterDataUuid = intent.getStringExtra(ConstantField.GRANTER_DATA_UUID);
            }
            if (intent.hasExtra(ConstantField.IP_INFORMATION)) {
                String ipInfoValue = intent.getStringExtra(ConstantField.IP_INFORMATION);
                if (ipInfoValue != null) {
                    try {
                        mIpBean = new Gson().fromJson(ipInfoValue, IPBean.class);
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    protected void handleConnectEvent() {
        if (mIpBean == null) {
            if (!isActiveStartSearchDevice) {
                if (presenter != null) {
                    presenter.startCountdown(SCAN_COUNTDOWN_SECOND);
                }
                prepareStartSearchDevice();
            }
        } else {
            directConnectDevice();
        }
    }

    private boolean bindEulixSpaceLanService() {
        Intent intent = new Intent(this, EulixSpaceLanService.class);
        return bindService(intent, eulixSpaceLanServiceConnection, Context.BIND_AUTO_CREATE);
    }

    protected void directConnectDevice() {
        if (btid != null) {
            if (bindDeviceStatus != ConstantField.BindDeviceStatus.SEARCHING) {
                bindDeviceStatusChange(ConstantField.BindDeviceStatus.SEARCHING);
            }
            bindDeviceStatusChange(ConstantField.BindDeviceStatus.BINDING);
            if (presenter != null) {
                if (!presenter.exchangePublicKey(mIpBean, deviceName, btid, deviceAddress, mHardwareFunction, securityFunction)) {
                    bindDeviceStatusChange(ConstantField.BindDeviceStatus.EMPTY);
                }
            }
        } else {
            bindDeviceStatusChange(ConstantField.BindDeviceStatus.EMPTY);
        }
    }

    protected void prepareStartSearchDevice() {
        if (btid != null && startSearchDevice()) {
            if (bindDeviceStatus != ConstantField.BindDeviceStatus.SEARCHING) {
                bindDeviceStatusChange(ConstantField.BindDeviceStatus.SEARCHING);
            }
        } else {
            bindDeviceStatusChange(ConstantField.BindDeviceStatus.EMPTY);
        }
    }

    private boolean startSearchDevice() {
        if (eulixSpaceLanBinder != null) {
            isActiveStartSearchDevice = true;
            return eulixSpaceLanBinder.discoverService(ConstantField.ServiceType.EULIXSPACE_SD_TCP);
        } else {
            return false;
        }
    }

    private void stopSearchDevice() {
        isActiveStartSearchDevice = false;
        if (eulixSpaceLanBinder != null) {
            eulixSpaceLanBinder.stopServiceDiscovery();
        }
    }

    protected int checkBoxUuid(String boxUuid, boolean isShowToast) {
        int status = 0;
        if (presenter != null) {
            status = presenter.checkBoxUuidStatus(boxUuid);
            if (isShowToast && status != 0) {
                switch (status) {
                    case 1:
                        showImageTextToast(R.drawable.toast_refuse, R.string.bind_duplicate_administrator);
                        break;
                    case -1:
                        showImageTextToast(R.drawable.toast_refuse, R.string.bind_duplicate_member);
                        break;
                    default:
                        break;
                }
            }
        }
        return status;
    }

    protected void countDownFinish() {
        stopSearchDevice();
        bindDeviceStatusChange(ConstantField.BindDeviceStatus.EMPTY);
    }

    protected void resetSecurityPassword() {
        if (securityFunction == ConstantField.SecurityFunction.RESET_PASSWORD) {
            showImageTextToast(R.drawable.toast_right, R.string.verify_success);
            Intent intent = new Intent(this, ModifySecurityPasswordActivity.class);
            intent.putExtra(ConstantField.RESET_PASSWORD, true);
            intent.putExtra("bluetooth", false);
            if (presenter != null) {
                String baseUrl = presenter.getBaseUrl();
                String bleKey = presenter.getBleKey();
                String bleIv = presenter.getBleIv();
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
            intent.putExtra(ConstantField.AUTHENTICATION_FUNCTION, authenticationFunction);
            if (granterDataUuid != null) {
                intent.putExtra(ConstantField.GRANTER_DATA_UUID, granterDataUuid);
            }
            startActivityForResult(intent, ConstantField.RequestCode.RESET_SECURITY_PASSWORD_CODE);
        }
    }

    protected void spacePlatformEnvironment() {}

    @Override
    protected void onDestroy() {
        if (isBindEulixSpaceLanService && eulixSpaceLanBinder != null) {
            try {
                unbindService(eulixSpaceLanServiceConnection);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                eulixSpaceLanBinder = null;
            }
            isBindEulixSpaceLanService = false;
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }

    protected void handlerPost(Runnable runnable) {
        if (mHandler != null && runnable != null) {
            mHandler.post(runnable);
        }
    }

    @Override
    protected int getActivityIndex() {
        return BIND_SERIES_ACTIVITY_INDEX;
    }

    @NotNull
    @Override
    public EulixSpaceLanPresenter createPresenter() {
        return new EulixSpaceLanPresenter();
    }

    protected void handleInitCallback(InitResponse initResponse) {}

    protected void handleRevokeCallback(Integer code, String message, AdminRevokeResult adminRevokeResult) {}

    protected void wifiListCallbackHandle(List<WifiInfo> wifiInfoList) {}

    protected void setWifiCallbackHandle(String ssid, List<String> ipAddresses, int status) {}

    protected void pairingCallbackHandle(Integer code, String boxUuid) {}

    protected void handleOnInitialCallback(Integer code) {}

    protected void initialResultCallback(Integer result) {

    }

    protected void spaceReadyCheckHandle(String source, int code, ReadyCheckResult result) {}

    protected void diskRecognitionHandle(String source, int code, DiskRecognitionResult result) {}

    protected void diskInitializeHandle(String source, int code) {}

    protected void diskInitializeProgressHandle(String source, int code, DiskInitializeProgressResult result) {}

    protected void diskManagementListHandle(String source, int code, DiskManageListResult result) {}

    protected void systemShutdownHandle(String source, int code) {}

    protected void getNetworkConfig(String source, int code, NetworkStatusResult result) {}

    protected void updateNetworkConfig(String source, int code) {}

    protected void ignoreNetworkConfig(String source, int code) {}

    protected void handleSwitchPlatformCallback(String source, int code, SwitchPlatformResult result, boolean isError) {}

    protected void handleSwitchStatusCallback(String source, int code, SwitchStatusResult result) {}

    protected void handleNewDeviceApplyResetPasswordCallback(String source, int code) {}

    protected void handleSecurityMessagePollCallback(String source, int code, String applyId, SecurityMessagePollResult result) {}

    protected void handleNewDeviceResetPasswordCallback(String source, int code) {}

    @Override
    public void countdownTime(int timeSecond) {
        if (timeSecond > 0) {
            setBoxSearingIndicator((4 - (timeSecond % 4)) % 4);
        } else {
            countDownFinish();
        }
    }

    @Override
    public void secretExchangeCallback(boolean isSuccess) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (isSuccess) {
                    switch (mHardwareFunction) {
                        case ConstantField.HardwareFunction.SECURITY_VERIFICATION:
                            switch (securityFunction) {
                                case ConstantField.SecurityFunction.RESET_PASSWORD:
                                    resetSecurityPassword();
                                    break;
                                default:
                                    break;
                            }
                            break;
                        case ConstantField.HardwareFunction.SWITCH_SPACE_PLATFORM:
                            spacePlatformEnvironment();
                            break;
                        default:
                            break;
                    }
                } else {
                    countDownFinish();
                }
            });
        }
    }

    @Override
    public void handleInit(InitResponse initResponse) {
        if (mHandler != null) {
            mHandler.post(() -> handleInitCallback(initResponse));
        }
    }

    @Override
    public void handleRevoke(Integer code, String message, AdminRevokeResult adminRevokeResult) {
        if (mHandler != null) {
            mHandler.post(() -> handleRevokeCallback(code, message, adminRevokeResult));
        }
    }

    @Override
    public void wifiListCallback(List<WifiInfo> wifiInfoList) {
        if (mHandler != null) {
            mHandler.post(() -> wifiListCallbackHandle(wifiInfoList));
        }
    }

    @Override
    public void setWifiCallback(String ssid, List<String> ipAddresses, int status) {
        if (mHandler != null) {
            mHandler.post(() -> {
                setWifiCallbackHandle(ssid, ipAddresses, status);
            });
        }
    }

    @Override
    public void pairingCallback(Integer code, String boxUuid) {
        if (mHandler != null) {
            mHandler.post(() -> {
                pairingCallbackHandle(code, boxUuid);
            });
        }
    }

    @Override
    public void handleInitialCallback(Integer code) {
        if (mHandler != null) {
            mHandler.post(() -> handleOnInitialCallback(code));
        }
    }

    @Override
    public void initialResult(Integer result) {
        if (mHandler != null) {
            mHandler.post(() -> initialResultCallback(result));
        }
    }

    @Override
    public void handleSwitchPlatform(String source, int code, SwitchPlatformResult result, boolean isError) {
        if (mHandler != null) {
            mHandler.post(() -> handleSwitchPlatformCallback(source, code, result, isError));
        }
    }

    @Override
    public void handleSwitchStatus(String source, int code, SwitchStatusResult result) {
        if (mHandler != null) {
            mHandler.post(() -> handleSwitchStatusCallback(source, code, result));
        }
    }

    @Override
    public void handleAccessToken(Integer code, String boxUuid) {

    }

    @Override
    public void handleNewDeviceApplyResetPasswordResult(String source, int code) {
        if (mHandler != null) {
            mHandler.post(() -> handleNewDeviceApplyResetPasswordCallback(source, code));
        }
    }

    @Override
    public void handleSecurityMessagePollResult(String source, int code, String applyId, SecurityMessagePollResult result) {
        if (mHandler != null) {
            mHandler.post(() -> handleSecurityMessagePollCallback(source, code, applyId, result));
        }
    }

    @Override
    public void handleNewDeviceResetPasswordResult(String source, int code) {
        if (mHandler != null) {
            mHandler.post(() -> handleNewDeviceResetPasswordCallback(source, code));
        }
    }

    @Override
    public void handleSpaceReadyCheck(String source, int code, ReadyCheckResult result) {
        spaceReadyCheckHandle(source, code, result);
    }

    @Override
    public void handleDiskRecognition(String source, int code, DiskRecognitionResult result) {
        diskRecognitionHandle(source, code, result);
    }

    @Override
    public void handleDiskInitialize(String source, int code) {
        diskInitializeHandle(source, code);
    }

    @Override
    public void handleDiskInitializeProgress(String source, int code, DiskInitializeProgressResult result) {
        diskInitializeProgressHandle(source, code, result);
    }

    @Override
    public void handleDiskManagementList(String source, int code, DiskManageListResult result) {
        diskManagementListHandle(source, code, result);
    }

    @Override
    public void handleSystemShutdown(String source, int code) {
        systemShutdownHandle(source, code);
    }

    @Override
    public void handleGetNetworkConfig(String source, int code, NetworkStatusResult result) {
        getNetworkConfig(source, code, result);
    }

    @Override
    public void handleUpdateNetworkConfig(String source, int code) {
        updateNetworkConfig(source, code);
    }

    @Override
    public void handleIgnoreNetworkConfig(String source, int code) {
        ignoreNetworkConfig(source, code);
    }

    @Override
    public void discoveryChange(int number) {
        // Do nothing
    }

    @Override
    public void resolveDevice(NsdServiceInfo nsdServiceInfo) {
        if (mHandler != null) {
            mHandler.post(() -> {
                boolean isFind = false;
                if (nsdServiceInfo != null && btid != null) {
                    String btidHash = FormatUtil.getSHA256String(("eulixspace-" + btid));
                    Logger.d(TAG, "btid hash: " + btidHash);
                    if (btidHash != null) {
                        Map<String, byte[]> attributes = nsdServiceInfo.getAttributes();
                        if (attributes != null) {
                            Set<Map.Entry<String, byte[]>> entrySet = attributes.entrySet();
                            for (Map.Entry<String, byte[]> entry : entrySet) {
                                if (entry != null) {
                                    String key = entry.getKey();
                                    String value = StringUtil.byteArrayToString(entry.getValue(), StandardCharsets.UTF_8);
                                    if (value != null && "btidhash".equals(key) && btidHash.startsWith(value)) {
                                        isFind = true;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                if (isFind) {
                    nsdServiceInfos = new ArrayList<>();
                    nsdServiceInfos.add(nsdServiceInfo);
                    bindDeviceStatusChange(ConstantField.BindDeviceStatus.BINDING);
                    stopSearchDevice();
                    if (presenter != null) {
                        presenter.stopCountdown();
                        if (!presenter.exchangePublicKey(nsdServiceInfo, deviceName, btid, deviceAddress, mHardwareFunction, securityFunction)) {
                            bindDeviceStatusChange(ConstantField.BindDeviceStatus.EMPTY);
                        }
                    }
                }
            });
        }
    }
}
