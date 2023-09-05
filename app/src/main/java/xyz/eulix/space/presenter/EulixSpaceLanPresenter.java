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

package xyz.eulix.space.presenter;

import android.net.nsd.NsdServiceInfo;
import android.os.CountDownTimer;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.bean.EulixBoxInfo;
import xyz.eulix.space.bean.EulixBoxToken;
import xyz.eulix.space.bean.IPBean;
import xyz.eulix.space.bean.bind.InitResponse;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.database.EulixSpaceSharePreferenceHelper;
import xyz.eulix.space.event.BoxInsertDeleteEvent;
import xyz.eulix.space.network.EulixBaseResponseExtensionCallback;
import xyz.eulix.space.network.agent.AdminRevokeResult;
import xyz.eulix.space.network.agent.AgentCallCallback;
import xyz.eulix.space.network.agent.AgentUtil;
import xyz.eulix.space.network.agent.AuthInfoCallback;
import xyz.eulix.space.network.agent.DeviceUtil;
import xyz.eulix.space.network.agent.InitialCallback;
import xyz.eulix.space.network.agent.KeyExchangeCallback;
import xyz.eulix.space.network.agent.NetworkConfigCallback;
import xyz.eulix.space.network.agent.PairInitCallback;
import xyz.eulix.space.network.agent.PairingCallback;
import xyz.eulix.space.network.agent.PubKeyExchangeCallback;
import xyz.eulix.space.network.agent.RevokeCallback;
import xyz.eulix.space.network.agent.ScanWifiListCallback;
import xyz.eulix.space.network.agent.SecurityMessagePollCallback;
import xyz.eulix.space.network.agent.SecurityMessagePollResult;
import xyz.eulix.space.network.agent.WifiInfo;
import xyz.eulix.space.network.agent.disk.DiskInitializeProgressCallback;
import xyz.eulix.space.network.agent.disk.DiskInitializeProgressResult;
import xyz.eulix.space.network.agent.disk.DiskManageListResult;
import xyz.eulix.space.network.agent.disk.DiskManagementListCallback;
import xyz.eulix.space.network.agent.disk.DiskRecognitionCallback;
import xyz.eulix.space.network.agent.disk.DiskRecognitionResult;
import xyz.eulix.space.network.agent.disk.DiskUtil;
import xyz.eulix.space.network.agent.disk.ReadyCheckCallback;
import xyz.eulix.space.network.agent.disk.ReadyCheckResult;
import xyz.eulix.space.network.agent.net.EulixNetUtil;
import xyz.eulix.space.network.agent.net.NetworkConfigRequest;
import xyz.eulix.space.network.agent.net.NetworkIgnoreRequest;
import xyz.eulix.space.network.agent.net.NetworkStatusResult;
import xyz.eulix.space.network.agent.platform.EulixPlatformUtil;
import xyz.eulix.space.network.agent.platform.SwitchPlatformCallback;
import xyz.eulix.space.network.agent.platform.SwitchPlatformResult;
import xyz.eulix.space.network.agent.platform.SwitchStatusCallback;
import xyz.eulix.space.network.agent.platform.SwitchStatusResult;
import xyz.eulix.space.network.net.NetworkStatusCallback;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.SystemUtil;
import xyz.eulix.space.util.ThreadPool;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/7/11 14:34
 */
public class EulixSpaceLanPresenter extends AbsPresenter<EulixSpaceLanPresenter.IEulixSpaceLan> {
    private static final int SECOND_UNIT = 1000;
    private CountDownTimer countDownTimer;
    private NsdServiceInfo mNsdServiceInfo;
    private IPBean mIpBean;
    private EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper;
    private String mDeviceAddress;
    private String mDeviceName;
    private String mBtid;
    private String mBaseUrl, mBoxName, mBoxUuid, mBoxPubKey, mRegKey, mUserDomain, mMessage;
    private String mBleKey, mBleIv;
    private String mAuthKey;
    private Integer mCode;
    private long mExpireTime;
    private int mHardwareFunction;
    private int mSecurityFunction;
    private DeviceAbility mDeviceAbility;

    private PubKeyExchangeCallback pubKeyExchangeCallback = new PubKeyExchangeCallback() {
        @Override
        public void onSuccess(int code, String message, String boxPublicKey) {
            mBoxPubKey = StringUtil.unwrapPublicKey(boxPublicKey);
            exchangeSecretKey();
        }

        @Override
        public void onFailed(int code, String message) {
            if (iView != null) {
                iView.handleInit(null);
            }
        }

        @Override
        public void onError(String errMsg) {
            if (iView != null) {
                iView.handleInit(null);
            }
        }
    };

    private KeyExchangeCallback keyExchangeCallback = new KeyExchangeCallback() {
        @Override
        public void onSuccess(int code, String message, String sharedSecret, String iv) {
            mBleKey = sharedSecret;
            mBleIv = iv;
            switch (mHardwareFunction) {
                case ConstantField.HardwareFunction.SECURITY_VERIFICATION:
                    switch (mSecurityFunction) {
                        case ConstantField.SecurityFunction.RESET_PASSWORD:
                        case ConstantField.SecurityFunction.INITIALIZE_SECURITY_MAILBOX:
                        case ConstantField.SecurityFunction.CHANGE_SECURITY_MAILBOX:
                            if (iView != null) {
                                iView.secretExchangeCallback(true);
                            }
                            break;
                        default:
                            break;
                    }
                    break;
                case ConstantField.HardwareFunction.SWITCH_SPACE_PLATFORM:
                    if (iView != null) {
                        iView.secretExchangeCallback(true);
                    }
                    break;
                default:
                    initDevice();
                    break;
            }
        }

        @Override
        public void onFailed(int code, String message) {
            if (iView != null) {
                switch (mHardwareFunction) {
                    case ConstantField.HardwareFunction.SECURITY_VERIFICATION:
                        switch (mSecurityFunction) {
                            case ConstantField.SecurityFunction.RESET_PASSWORD:
                            case ConstantField.SecurityFunction.INITIALIZE_SECURITY_MAILBOX:
                            case ConstantField.SecurityFunction.CHANGE_SECURITY_MAILBOX:
                                iView.secretExchangeCallback(false);
                                break;
                            default:
                                break;
                        }
                        break;
                    case ConstantField.HardwareFunction.SWITCH_SPACE_PLATFORM:
                        iView.secretExchangeCallback(false);
                        break;
                    default:
                        iView.handleInit(null);
                        break;
                }
            }
        }

        @Override
        public void onError(String errMsg) {
            if (iView != null) {
                switch (mHardwareFunction) {
                    case ConstantField.HardwareFunction.SECURITY_VERIFICATION:
                        switch (mSecurityFunction) {
                            case ConstantField.SecurityFunction.RESET_PASSWORD:
                            case ConstantField.SecurityFunction.INITIALIZE_SECURITY_MAILBOX:
                            case ConstantField.SecurityFunction.CHANGE_SECURITY_MAILBOX:
                                iView.secretExchangeCallback(false);
                                break;
                            default:
                                break;
                        }
                        break;
                    case ConstantField.HardwareFunction.SWITCH_SPACE_PLATFORM:
                        iView.secretExchangeCallback(false);
                        break;
                    default:
                        iView.handleInit(null);
                        break;
                }
            }
        }
    };

    private PairInitCallback pairInitCallback = new PairInitCallback() {
        @Override
        public void onSuccess(String code, String message, InitResponse initResponse) {
            if (initResponse != null) {
                mBoxName = initResponse.getBoxName();
                mBoxUuid = initResponse.getBoxUuid();
                mDeviceAbility = initResponse.getDeviceAbility();
            }
            if (iView != null) {
                iView.handleInit(initResponse);
            }
        }

        @Override
        public void onFailed() {
            if (iView != null) {
                iView.handleInit(null);
            }
        }

        @Override
        public void onError(String errMsg) {
            if (iView != null) {
                iView.handleInit(null);
            }
        }
    };

    private RevokeCallback revokeCallback = new RevokeCallback() {
        @Override
        public void onSuccess(String message, Integer code, String boxUuid, int errorTimes, int leftTryTimes, int tryAfterSeconds) {
            AdminRevokeResult adminRevokeResult = new AdminRevokeResult();
            adminRevokeResult.setBoxUuid(boxUuid);
            adminRevokeResult.setErrorTimes(errorTimes);
            adminRevokeResult.setLeftTryTimes(leftTryTimes);
            adminRevokeResult.setTryAfterSeconds(tryAfterSeconds);
            if (iView != null) {
                iView.handleRevoke(code, message, adminRevokeResult);
            }
        }

        @Override
        public void onFailed(String message, Integer code) {
            if (iView != null) {
                iView.handleRevoke(code, message, null);
            }
        }

        @Override
        public void onError(String msg) {
            if (iView != null) {
                iView.handleRevoke(null, msg, null);
            }
        }
    };

    private ScanWifiListCallback scanWifiListCallback = new ScanWifiListCallback() {
        @Override
        public void onSuccess(String code, String message, List<WifiInfo> wifiInfoList) {
            if (iView != null) {
                iView.wifiListCallback(wifiInfoList);
            }
        }

        @Override
        public void onFailed() {
            if (iView != null) {
                iView.wifiListCallback(new ArrayList<>());
            }
        }

        @Override
        public void onError(String errMsg) {
            if (iView != null) {
                iView.wifiListCallback(null);
            }
        }
    };

    private NetworkConfigCallback networkConfigCallback = new NetworkConfigCallback() {
        @Override
        public void onSuccess(String requestUuid, int code, String message, String ssid, List<String> ipAddresses, int status) {
            if (iView != null) {
                iView.setWifiCallback(ssid, ipAddresses, status);
            }
        }

        @Override
        public void onFailed(String requestUuid, int code, String message, String ssid) {
            if (iView != null) {
                iView.setWifiCallback(null, null, -1);
            }
        }

        @Override
        public void onError(String requestUuid, String ssid, String errMsg) {
            if (iView != null) {
                iView.setWifiCallback(null, null, -1);
            }
        }
    };

    private PairingCallback pairingCallback = new PairingCallback() {
        @Override
        public void onSuccess(Integer code, String message, String deviceAddress, String boxName, String boxUuid, String boxPubKey, String authKey, String regKey, String userDomain, int paired) {
            mCode = null;
            mMessage = null;
            if (boxUuid != null) {
                mBoxName = boxName;
                mBoxUuid = boxUuid;
                mRegKey = regKey;
                mUserDomain = userDomain;
                mMessage = message;
                mAuthKey = authKey;
                boolean isPaired = true;
                if (code != null && code >= 0 && code < 400 && paired != 1) {
                    code = ConstantField.BindDeviceHttpCode.BIND_DUPLICATE_CODE;
                    isPaired = false;
                }
                mCode = code;
                handleBoxToken(mBoxUuid, mAuthKey, mCode, isPaired);
            }
        }

        @Override
        public void onFailed(String message, Integer code) {
            if (iView != null) {
                iView.pairingCallback(code, null);
            }
        }

        @Override
        public void onError(String msg) {
            if (iView != null) {
                iView.pairingCallback(null, null);
            }
        }
    };

    private SwitchPlatformCallback switchPlatformCallback = new SwitchPlatformCallback() {
        @Override
        public void onSuccess(int code, String source, String message, String requestId, SwitchPlatformResult result) {
            if (iView != null) {
                iView.handleSwitchPlatform(source, code, result, false);
            }
        }

        @Override
        public void onFailed(int code, String source, String message, String requestId) {
            if (iView != null) {
                iView.handleSwitchPlatform(source, code, null, false);
            }
        }

        @Override
        public void onError(String errMsg) {
            if (iView != null) {
                iView.handleSwitchPlatform(null, 500, null, true);
            }
        }
    };

    private SwitchStatusCallback switchStatusCallback = new SwitchStatusCallback() {
        @Override
        public void onSuccess(int code, String source, String message, String requestId, SwitchStatusResult result) {
            if (iView != null) {
                iView.handleSwitchStatus(source, code, result);
            }
        }

        @Override
        public void onFailed(int code, String source, String message, String requestId) {
            if (iView != null) {
                iView.handleSwitchStatus(source, code, null);
            }
        }

        @Override
        public void onError(String errMsg) {
            if (iView != null) {
                iView.handleSwitchStatus(null, 500, null);
            }
        }
    };

    private AuthInfoCallback authInfoCallback = new AuthInfoCallback() {
        @Override
        public void onSuccess(Integer code, String message, String deviceAddress, String boxUuid, String authKey) {
//            Integer resultCode = (mCode == null ? code : mCode);
//            boolean isBind = (resultCode != null && ConstantField.BindDeviceHttpCode.BIND_DUPLICATE_CODE == resultCode);
//            if (isBind) {
//                mAuthKey = authKey;
//                getAgentInfo();
//            } else {
//                handleBoxToken(boxUuid, authKey, resultCode, false);
//            }
        }

        @Override
        public void onFailed(String message, Integer code) {
//            if (iView != null) {
//                iView.pairingCallback((mCode == null ? code : mCode), "");
//            }
        }

        @Override
        public void onError(String msg) {
//            if (iView != null) {
//                iView.pairingCallback(mCode, null);
//            }
        }
    };

    private InitialCallback initialCallback = new InitialCallback() {
        @Override
        public void onSuccess(String message, Integer code, Integer result) {
            if (iView != null) {
                iView.initialResult(code);
            }
        }

        @Override
        public void onFailed(String message, Integer code) {
            if (iView != null) {
                iView.initialResult(null);
            }
        }

        @Override
        public void onError(String msg, String url, String bleKey, String bleIv) {
            initialize();
        }

        @Override
        public void onError(String msg, String url, String password, String bleKey, String bleIv) {
            initialize(password);
        }
    };

    private ReadyCheckCallback readyCheckCallback = new ReadyCheckCallback() {
        @Override
        public void onSuccess(int code, String source, String message, String requestId, ReadyCheckResult result) {
            if (iView != null) {
                iView.handleSpaceReadyCheck(source, code, result);
            }
        }

        @Override
        public void onFail(int code, String source, String message, String requestId) {
            if (iView != null) {
                iView.handleSpaceReadyCheck(source, code, null);
            }
        }

        @Override
        public void onError(String errMsg) {
            if (iView != null) {
                iView.handleSpaceReadyCheck(null, 500, null);
            }
        }
    };

    private DiskRecognitionCallback diskRecognitionCallback = new DiskRecognitionCallback() {
        @Override
        public void onSuccess(int code, String source, String message, String requestId, DiskRecognitionResult result) {
            if (iView != null) {
                iView.handleDiskRecognition(source, code, result);
            }
        }

        @Override
        public void onFail(int code, String source, String message, String requestId) {
            if (iView != null) {
                iView.handleDiskRecognition(source, code, null);
            }
        }

        @Override
        public void onError(String errMsg) {
            if (iView != null) {
                iView.handleDiskRecognition(null, 500, null);
            }
        }
    };

    private EulixBaseResponseExtensionCallback diskInitializeCallback = new EulixBaseResponseExtensionCallback() {
        @Override
        public void onSuccess(String source, int code, String message, String requestId) {
            if (iView != null) {
                iView.handleDiskInitialize(source, code);
            }
        }

        @Override
        public void onFailed() {
            if (iView != null) {
                iView.handleDiskInitialize(null, -1);
            }
        }

        @Override
        public void onError(String errMsg) {
            if (iView != null) {
                iView.handleDiskInitialize(null, 500);
            }
        }
    };

    private DiskInitializeProgressCallback diskInitializeProgressCallback = new DiskInitializeProgressCallback() {
        @Override
        public void onSuccess(int code, String source, String message, String requestId, DiskInitializeProgressResult result) {
            if (iView != null) {
                iView.handleDiskInitializeProgress(source, code, result);
            }
        }

        @Override
        public void onFail(int code, String source, String message, String requestId) {
            if (iView != null) {
                iView.handleDiskInitializeProgress(source, code, null);
            }
        }

        @Override
        public void onError(String errMsg) {
            if (iView != null) {
                iView.handleDiskInitializeProgress(null, 500, null);
            }
        }
    };

    private DiskManagementListCallback diskManagementListCallback = new DiskManagementListCallback() {
        @Override
        public void onSuccess(int code, String source, String message, String requestId, DiskManageListResult result) {
            if (iView != null) {
                iView.handleDiskManagementList(source, code, result);
            }
        }

        @Override
        public void onFail(int code, String source, String message, String requestId) {
            if (iView != null) {
                iView.handleDiskManagementList(source, code, null);
            }
        }

        @Override
        public void onError(String errMsg) {
            if (iView != null) {
                iView.handleDiskManagementList(null, 500, null);
            }
        }
    };

    private EulixBaseResponseExtensionCallback systemShutdownCallback = new EulixBaseResponseExtensionCallback() {
        @Override
        public void onSuccess(String source, int code, String message, String requestId) {
            if (iView != null) {
                iView.handleSystemShutdown(source, code);
            }
        }

        @Override
        public void onFailed() {
            if (iView != null) {
                iView.handleSystemShutdown(null, -1);
            }
        }

        @Override
        public void onError(String errMsg) {
            if (iView != null) {
                iView.handleSystemShutdown(null, 500);
            }
        }
    };

    private NetworkStatusCallback getNetworkConfigCallback = new NetworkStatusCallback() {
        @Override
        public void onSuccess(int code, String source, String message, String requestId, NetworkStatusResult result) {
            if (iView != null) {
                iView.handleGetNetworkConfig(source, code, result);
            }
        }

        @Override
        public void onFail(int code, String source, String message, String requestId) {
            if (iView != null) {
                iView.handleGetNetworkConfig(source, code, null);
            }
        }

        @Override
        public void onError(String errMsg) {
            if (iView != null) {
                iView.handleGetNetworkConfig(null, 500, null);
            }
        }
    };

    private EulixBaseResponseExtensionCallback updateNetworkConfigCallback = new EulixBaseResponseExtensionCallback() {
        @Override
        public void onSuccess(String source, int code, String message, String requestId) {
            if (iView != null) {
                iView.handleUpdateNetworkConfig(source, code);
            }
        }

        @Override
        public void onFailed() {
            if (iView != null) {
                iView.handleUpdateNetworkConfig(null, -1);
            }
        }

        @Override
        public void onError(String errMsg) {
            if (iView != null) {
                iView.handleUpdateNetworkConfig(null, 500);
            }
        }
    };

    private EulixBaseResponseExtensionCallback ignoreNetworkCallback = new EulixBaseResponseExtensionCallback() {
        @Override
        public void onSuccess(String source, int code, String message, String requestId) {
            if (iView != null) {
                iView.handleIgnoreNetworkConfig(source, code);
            }
        }

        @Override
        public void onFailed() {
            if (iView != null) {
                iView.handleIgnoreNetworkConfig(null, -1);
            }
        }

        @Override
        public void onError(String errMsg) {
            if (iView != null) {
                iView.handleIgnoreNetworkConfig(null, 500);
            }
        }
    };

    public interface IEulixSpaceLan extends IBaseView {
        void countdownTime(int timeSecond);
        void secretExchangeCallback(boolean isSuccess);
        void handleInit(InitResponse initResponse);
        void handleRevoke(Integer code, String message, AdminRevokeResult adminRevokeResult);
        void wifiListCallback(List<WifiInfo> wifiInfoList);
        void setWifiCallback(String ssid, List<String> ipAddresses, int status);
        void pairingCallback(Integer code, String boxUuid);
        void handleInitialCallback(Integer code);
        void initialResult(Integer result);
        void handleSwitchPlatform(String source, int code, SwitchPlatformResult result, boolean isError);
        void handleSwitchStatus(String source, int code, SwitchStatusResult result);
        void handleAccessToken(Integer code, String boxUuid);
        void handleNewDeviceApplyResetPasswordResult(String source, int code);
        void handleSecurityMessagePollResult(String source, int code, String applyId, SecurityMessagePollResult result);
        void handleNewDeviceResetPasswordResult(String source, int code);
        void handleSpaceReadyCheck(String source, int code, ReadyCheckResult result);
        void handleDiskRecognition(String source, int code, DiskRecognitionResult result);
        void handleDiskInitialize(String source, int code);
        void handleDiskInitializeProgress(String source, int code, DiskInitializeProgressResult result);
        void handleDiskManagementList(String source, int code, DiskManageListResult result);
        void handleSystemShutdown(String source, int code);
        void handleGetNetworkConfig(String source, int code, NetworkStatusResult result);
        void handleUpdateNetworkConfig(String source, int code);
        void handleIgnoreNetworkConfig(String source, int code);
    }

    public void startCountdown(int timeSecond) {
        stopCountdown();
        countDownTimer = new CountDownTimer((timeSecond * SECOND_UNIT), SECOND_UNIT) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (iView != null) {
                    iView.countdownTime((1 + (int) (millisUntilFinished / SECOND_UNIT)));
                }
            }

            @Override
            public void onFinish() {
                if (iView != null) {
                    iView.countdownTime(0);
                }
            }
        };
        countDownTimer.start();
    }

    public void stopCountdown() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    public boolean checkPairedSelf(int paired, String pairClientUuid) {
        boolean isPairedSelf = false;
        if (pairClientUuid != null) {
            isPairedSelf = (paired == 0 && pairClientUuid.equals(DataUtil.getClientUuid(context)));
        }
        return isPairedSelf;
    }

    private void generateBaseUrl(NsdServiceInfo serviceInfo) {
        if (serviceInfo != null) {
            InetAddress inetAddress = serviceInfo.getHost();
            if (inetAddress instanceof Inet4Address || inetAddress instanceof Inet6Address) {
                boolean isIpv6 = (inetAddress instanceof Inet6Address);
                String hostAddress = inetAddress.getHostAddress();
                int port = serviceInfo.getPort();
                if (!TextUtils.isEmpty(hostAddress)) {
                    String baseUrl;
                    if (isIpv6) {
                        baseUrl = "http://[" + hostAddress + "]:" + port + "/";
                    } else {
                        baseUrl = "http://" + hostAddress +  ":" + port + "/";
                    }
                    mBaseUrl = baseUrl;
                }
            }
        }
    }

    private void generateBaseUrl() {
        if (mNsdServiceInfo != null) {
            generateBaseUrl(mNsdServiceInfo);
        } else if (mIpBean != null) {
            generateBaseUrl(mIpBean);
        }
    }

    private void generateBaseUrl(IPBean ipBean) {
        if (ipBean != null) {
            String ipv6Address = ipBean.getIPV6Address();
            String ipv4Address = ipBean.getIPV4Address();
            int port = ipBean.getPort();
            if (StringUtil.isNonBlankString(ipv6Address)) {
                mBaseUrl = "http://[" + ipv6Address + "]:" + port + "/";
            } else if (StringUtil.isNonBlankString(ipv4Address)) {
                mBaseUrl = "http://" + ipv4Address +  ":" + port + "/";
            }
        }
    }

    private boolean exchangePublicKey(String btid) {
        boolean isInit = false;
        if (btid != null && mBaseUrl != null) {
            isInit = true;
            try {
                ThreadPool.getInstance().execute(() -> AgentUtil.exchangePublicKey(mBaseUrl
                        , DataUtil.getClientPublicKey(context), DataUtil.getClientPrivateKey(context)
                        , btid, pubKeyExchangeCallback));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
        return isInit;
    }

    public boolean exchangePublicKey(NsdServiceInfo serviceInfo, String deviceName, String btid, String deviceAddress, int hardwareFunction, int securityFunction) {
        mHardwareFunction = hardwareFunction;
        mSecurityFunction = securityFunction;
        boolean isInit = false;
        if (serviceInfo != null) {
            mNsdServiceInfo = serviceInfo;
            mDeviceName = deviceName;
            mBtid = btid;
            mDeviceAddress = deviceAddress;
            generateBaseUrl(serviceInfo);
            isInit = exchangePublicKey(btid);
        }
        return isInit;
    }

    public boolean exchangePublicKey(IPBean ipBean, String deviceName, String btid, String deviceAddress, int hardwareFunction, int securityFunction) {
        mHardwareFunction = hardwareFunction;
        mSecurityFunction = securityFunction;
        boolean isInit = false;
        if (ipBean != null) {
            mIpBean = ipBean;
            mDeviceName = deviceName;
            mBtid = btid;
            mDeviceAddress = deviceAddress;
            generateBaseUrl(ipBean);
            isInit = exchangePublicKey(btid);
        }
        return isInit;
    }

    private void exchangeSecretKey() {
        if (mBaseUrl != null) {
            try {
                ThreadPool.getInstance().execute(() -> AgentUtil.exchangeSecretKey(mBaseUrl, mBtid, mBoxPubKey
                        , DataUtil.getClientPrivateKey(context), keyExchangeCallback));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    private void initDevice() {
        if (mBaseUrl != null) {
            try {
                ThreadPool.getInstance().execute(() -> AgentUtil.pairInit(mBaseUrl, mBleKey, mBleIv, pairInitCallback));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public int checkBoxUuidStatus(String boxUuid) {
        int status = 0;
        if (boxUuid != null) {
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)
                            && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_STATUS)) {
                        String stateValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_STATUS);
                        int state = -1;
                        if (stateValue != null) {
                            try {
                                state = Integer.parseInt(stateValue);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                        if (state >= ConstantField.EulixDeviceStatus.OFFLINE && state <= ConstantField.EulixDeviceStatus.ONLINE_UNINITIALIZED) {
                            String bindValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                            if (state == ConstantField.EulixDeviceStatus.ONLINE_UNINITIALIZED || state == ConstantField.EulixDeviceStatus.OFFLINE_UNINITIALIZED) {
                                status = 2;
                            } else if (status != 2 && bindValue != null) {
                                if ("1".equals(bindValue)) {
                                    status = 1;
                                } else if (status != 1 && "-1".equals(bindValue)) {
                                    status = -1;
                                }
                            }
                        }
                    }
                }
            }
        }
        return status;
    }

    public void revoke(String password) {
        generateBaseUrl();
        if (mBaseUrl != null) {
            try {
                ThreadPool.getInstance().execute(() -> AgentUtil.revoke(mBaseUrl, DataUtil.getClientUuid(context), password, mBleKey, mBleIv, revokeCallback));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public void switchPlatform(String transId, String domain) {
        if (mBaseUrl == null) {
            generateBaseUrl();
        }
        if (mBaseUrl != null) {
            EulixPlatformUtil.switchPlatform(mBaseUrl, mBleKey, mBleIv, transId, domain, switchPlatformCallback);
        }
    }

    public void getSwitchPlatformStatus(String transId) {
        if (mBaseUrl == null) {
            generateBaseUrl();
        }
        if (mBaseUrl != null) {
            EulixPlatformUtil.getSwitchPlatformStatus(mBaseUrl, mBleKey, mBleIv, transId, switchStatusCallback);
        }
    }

    public void newDeviceApplyResetPassword(String applyId) {
        generateBaseUrl();
        if (mBaseUrl != null) {
            try {
                ThreadPool.getInstance().execute(() -> AgentUtil.newDeviceApplyResetSecurityPassword(mBaseUrl
                        , SystemUtil.getPhoneModel(), DataUtil.getClientUuid(context), applyId
                        , mBleKey, mBleIv, new AgentCallCallback() {
                            @Override
                            public void onSuccess(int code, String source, String message, String requestId) {
                                if (iView != null) {
                                    iView.handleNewDeviceApplyResetPasswordResult(source, code);
                                }
                            }

                            @Override
                            public void onFailed(int code, String source, String message, String requestId) {
                                if (iView != null) {
                                    iView.handleNewDeviceApplyResetPasswordResult(source, code);
                                }
                            }

                            @Override
                            public void onError(String errMsg) {
                                if (iView != null) {
                                    iView.handleNewDeviceApplyResetPasswordResult(null, -1);
                                }
                            }
                        }));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public void securityMessagePoll(String applyId) {
        generateBaseUrl();
        if (mBaseUrl != null) {
            try {
                ThreadPool.getInstance().execute(() -> AgentUtil.securityMessagePoll(mBaseUrl
                        , DataUtil.getClientUuid(context), mBleKey, mBleIv, new SecurityMessagePollCallback() {
                            @Override
                            public void onSuccess(int code, String source, String message, String requestId, List<SecurityMessagePollResult> securityMessagePollResultList) {
                                SecurityMessagePollResult result = null;
                                if (securityMessagePollResultList != null) {
                                    for (SecurityMessagePollResult securityMessagePollResult : securityMessagePollResultList) {
                                        if (securityMessagePollResult != null && applyId != null && applyId.equals(securityMessagePollResult.getApplyId())) {
                                            result = securityMessagePollResult;
                                            break;
                                        }
                                    }
                                }
                                if (iView != null) {
                                    iView.handleSecurityMessagePollResult(source, code, applyId, result);
                                }
                            }

                            @Override
                            public void onFailed(int code, String source, String message, String requestId) {
                                if (iView != null) {
                                    iView.handleSecurityMessagePollResult(source, code, applyId, null);
                                }
                            }

                            @Override
                            public void onError(String errMsg) {
                                if (iView != null) {
                                    iView.handleSecurityMessagePollResult(null, -1, applyId, null);
                                }
                            }
                        }));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public void newDeviceResetPassword(String acceptSecurityToken, String emailSecurityToken, String binderClientUuid, String password) {
        generateBaseUrl();
        if (mBaseUrl != null) {
            try {
                ThreadPool.getInstance().execute(() -> AgentUtil.newDeviceResetSecurityPassword(mBaseUrl
                        , acceptSecurityToken, emailSecurityToken, binderClientUuid
                        , DataUtil.getClientUuid(context), password, mBleKey, mBleIv, new AgentCallCallback() {
                            @Override
                            public void onSuccess(int code, String source, String message, String requestId) {
                                if (iView != null) {
                                    iView.handleNewDeviceResetPasswordResult(source, code);
                                }
                            }

                            @Override
                            public void onFailed(int code, String source, String message, String requestId) {
                                if (iView != null) {
                                    iView.handleNewDeviceResetPasswordResult(source, code);
                                }
                            }

                            @Override
                            public void onError(String errMsg) {
                                if (iView != null) {
                                    iView.handleNewDeviceResetPasswordResult(null, -1);
                                }
                            }
                        }));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public void getWifiList() {
        generateBaseUrl();
        if (mBaseUrl != null) {
            try {
                ThreadPool.getInstance().execute(() -> DeviceUtil.getWifiList(mBaseUrl, mBleKey, mBleIv, scanWifiListCallback));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public void setWifi(String ssid, String name, String password) {
        generateBaseUrl();
        if (mBaseUrl != null) {
            try {
                ThreadPool.getInstance().execute(() -> DeviceUtil.setWifi(UUID.randomUUID().toString()
                        , ssid, name, password, mBaseUrl, mBleKey, mBleIv, networkConfigCallback));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public void spaceReadyCheck() {
        if (mBaseUrl == null) {
            generateBaseUrl();
        }
        if (mBaseUrl != null) {
            DiskUtil.getSpaceReadyCheck(mBaseUrl, mBleKey, mBleIv, readyCheckCallback);
        }
    }

    public void bindDevice(int paired) {
        generateBaseUrl();
        EulixSpaceSharePreferenceHelper sharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (mBaseUrl != null && sharePreferenceHelper != null && sharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PUBLIC_KEY)
                && sharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.UUID)
                && sharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PRIVATE_KEY)) {
            try {
                ThreadPool.getInstance().execute(() -> AgentUtil.pairingEnc(mBaseUrl
                        , sharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PUBLIC_KEY)
                        , sharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.UUID)
                        , sharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PRIVATE_KEY)
                        , SystemUtil.getPhoneModel(), mBleKey, mBleIv, null, paired, pairingCallback));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }


    public void handleBoxToken(String boxUuid, String authKey, Integer resultCode, boolean isPaired) {
        long currentTimestamp = System.currentTimeMillis();
        Long expireTimestamp = null;
        if (boxUuid != null && boxUuid.equals(mBoxUuid)) {
            EulixBoxInfo eulixBoxInfo = null;
            mAuthKey = authKey;
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, "1");
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context.getApplicationContext(), queryMap);
            Map<String, String> boxValue = new HashMap<>();
            boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            boxValue.put(EulixSpaceDBManager.FIELD_BOX_NAME, mBoxName);
            boxValue.put(EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY, StringUtil.unwrapPublicKey(mBoxPubKey));
            boxValue.put(EulixSpaceDBManager.FIELD_BOX_AUTHORIZATION, authKey);
            boxValue.put(EulixSpaceDBManager.FIELD_BOX_REGISTER, mRegKey);
            boxValue.put(EulixSpaceDBManager.FIELD_BOX_DOMAIN, mUserDomain);
            boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, String.valueOf(1));
            boxValue.put(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME, String.valueOf(currentTimestamp));
            if (boxValues == null || boxValues.size() <= 0) {
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.BINDING));
                eulixBoxInfo = new EulixBoxInfo();
                eulixBoxInfo.setBluetoothDeviceName(mDeviceName);
                eulixBoxInfo.setBluetoothId(mBtid);
                eulixBoxInfo.setBluetoothAddress(mDeviceAddress);
                eulixBoxInfo.setDeviceAbility(mDeviceAbility);
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_INFO, new Gson().toJson(eulixBoxInfo, EulixBoxInfo.class));
                EulixSpaceDBUtil.insertBox(context.getApplicationContext(), boxValue, 1);
                BoxInsertDeleteEvent boxInsertDeleteEvent = new BoxInsertDeleteEvent(boxUuid, "1", true);
                EventBusUtil.post(boxInsertDeleteEvent);
            } else {
                String eulixBoxInfoValue = null;
                for (Map<String, String> boxV : boxValues) {
                    if (boxV != null && boxV.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
                        String boxTokenValue = boxV.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                        if (boxTokenValue != null) {
                            EulixBoxToken eulixBoxToken = null;
                            try {
                                eulixBoxToken = new Gson().fromJson(boxTokenValue, EulixBoxToken.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                            if (eulixBoxToken != null) {
                                expireTimestamp = eulixBoxToken.getTokenExpire();
                            }
                        }
                        if (boxV.containsKey(EulixSpaceDBManager.FIELD_BOX_INFO)) {
                            eulixBoxInfoValue = boxV.get(EulixSpaceDBManager.FIELD_BOX_INFO);
                        }
                        break;
                    }
                }
                if (eulixBoxInfoValue != null) {
                    try {
                        eulixBoxInfo = new Gson().fromJson(eulixBoxInfoValue, EulixBoxInfo.class);
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                    }
                }
                if (eulixBoxInfo == null) {
                    eulixBoxInfo = new EulixBoxInfo();
                }
                eulixBoxInfo.setBluetoothDeviceName(mDeviceName);
                eulixBoxInfo.setBluetoothId(mBtid);
                eulixBoxInfo.setBluetoothAddress(mDeviceAddress);
                eulixBoxInfo.setDeviceAbility(mDeviceAbility);
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_INFO, new Gson().toJson(eulixBoxInfo, EulixBoxInfo.class));
                EulixSpaceDBUtil.updateBox(context.getApplicationContext(), boxValue);
            }
        }
        if (expireTimestamp == null || expireTimestamp < (currentTimestamp + 10 * 1000)) {
            if (isPaired) {
                if (iView != null) {
                    iView.pairingCallback(resultCode, boxUuid);
                }
            } else {
                if (iView != null) {
                    iView.handleInitialCallback(resultCode);
                }
            }
        } else {
            if (iView != null) {
                iView.pairingCallback(resultCode, boxUuid);
            }
        }
    }

    public void requestUseBox(boolean isDiskInitialize) {
        Map<String, String> boxValue = new HashMap<>();
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, mBoxUuid);
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, "1");
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(isDiskInitialize ? ConstantField.EulixDeviceStatus.ONLINE_UNINITIALIZED : ConstantField.EulixDeviceStatus.REQUEST_USE));
        boxValue.put(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME, String.valueOf(System.currentTimeMillis()));
        EulixSpaceDBUtil.updateBox(context, boxValue);
    }


    public void initialize() {
        try {
            ThreadPool.getInstance().execute(() -> AgentUtil.initial(mBaseUrl, mBleKey, mBleIv, initialCallback));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public void initialize(String password) {
        try {
            ThreadPool.getInstance().execute(() -> AgentUtil.initial(mBaseUrl, password, mBleKey, mBleIv, initialCallback));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public void diskRecognition() {
        if (mBaseUrl != null) {
            DiskUtil.getDiskRecognition(mBaseUrl, mBleKey, mBleIv, diskRecognitionCallback);
        }
    }

    public void diskInitialize(boolean isDiskEncrypt, boolean isRaid, List<String> primaryStorageHardwareIds, List<String> secondaryStorageHardwareIds, List<String> raidDiskHardwareIds) {
        if (mBaseUrl != null) {
            DiskUtil.diskInitialize(mBaseUrl, mBleKey, mBleIv, isDiskEncrypt, isRaid, primaryStorageHardwareIds, secondaryStorageHardwareIds, raidDiskHardwareIds, diskInitializeCallback);
        }
    }

    public void getDiskInitializeProgress() {
        if (mBaseUrl != null) {
            DiskUtil.getDiskInitializeProgress(mBaseUrl, mBleKey, mBleIv, diskInitializeProgressCallback);
        }
    }

    public void getDiskManagementList() {
        if (mBaseUrl != null) {
            DiskUtil.getDiskManagementList(mBaseUrl, mBleKey, mBleIv, diskManagementListCallback);
        }
    }

    public void systemShutdown() {
        if (mBaseUrl != null) {
            DiskUtil.eulixSystemShutdown(mBaseUrl, systemShutdownCallback);
        }
    }

    public void getNetworkConfig() {
        if (mBaseUrl == null) {
            generateBaseUrl();
        }
        if (mBaseUrl != null) {
            EulixNetUtil.getNetworkConfig(mBaseUrl, mBleKey, mBleIv, getNetworkConfigCallback);
        }
    }

    public void updateNetworkConfig(NetworkConfigRequest networkConfigRequest) {
        if (mBaseUrl != null) {
            EulixNetUtil.setNetworkConfig(mBaseUrl, mBleKey, mBleIv, networkConfigRequest, updateNetworkConfigCallback);
        }
    }

    public void ignoreNetwork(NetworkIgnoreRequest networkIgnoreRequest) {
        if (mBaseUrl != null) {
            EulixNetUtil.ignoreNetwork(mBaseUrl, mBleKey, mBleIv, networkIgnoreRequest, ignoreNetworkCallback);
        }
    }

    public DeviceAbility getActiveDeviceAbility() {
        return EulixSpaceDBUtil.getActiveDeviceAbility(context, true);
    }


    public String getBaseUrl() {
        return mBaseUrl;
    }

    public String getUserDomain() {
        return mUserDomain;
    }

    public String getBoxUuid() {
        return mBoxUuid;
    }

    public String getAuthKey() {
        return mAuthKey;
    }

    public Integer getCode() {
        return mCode;
    }

    public String getBleKey() {
        return mBleKey;
    }

    public String getBleIv() {
        return mBleIv;
    }
}
