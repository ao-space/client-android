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

import android.os.CountDownTimer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.CommonDeviceInfo;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.network.EulixBaseResponseExtensionCallback;
import xyz.eulix.space.network.agent.DeviceUtil;
import xyz.eulix.space.network.agent.NetworkConfigCallback;
import xyz.eulix.space.network.agent.ScanWifiListCallback;
import xyz.eulix.space.network.agent.WifiInfo;
import xyz.eulix.space.network.agent.net.NetworkAdapter;
import xyz.eulix.space.network.agent.net.NetworkStatusResult;
import xyz.eulix.space.network.net.EulixNetUtil;
import xyz.eulix.space.network.net.NetworkStatusCallback;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.GatewayUtils;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.ThreadPool;
import xyz.eulix.space.util.Urls;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/10/28 14:06
 */
public class DistributeNetworkPresenter extends AbsPresenter<DistributeNetworkPresenter.IDistributeNetwork> {
    private static final int SECOND_UNIT = 1000;
    private String setWifiUuid = null;
    private CountDownTimer countDownTimer;

    public ScanWifiListCallback scanWifiListCallback = new ScanWifiListCallback() {
        @Override
        public void onSuccess(String code, String message, List<WifiInfo> wifiInfoList) {
            if (iView != null) {
                iView.wifiListCallback(wifiInfoList);
            }
        }

        @Override
        public void onFailed() {
            if (iView != null) {
                iView.wifiListCallback(null);
            }
        }

        @Override
        public void onError(String errMsg) {
            if (iView != null) {
                iView.wifiListCallback(null);
            }
        }
    };

    private NetworkStatusCallback networkStatusCallback = new NetworkStatusCallback() {
        @Override
        public void onSuccess(int code, String source, String message, String requestId, NetworkStatusResult result) {
            if (iView != null) {
                iView.networkAccessCallback(true, result);
            }
        }

        @Override
        public void onFail(int code, String source, String message, String requestId) {
            if (iView != null) {
                iView.networkAccessCallback(false, null);
            }
        }

        @Override
        public void onError(String errMsg) {
            if (iView != null) {
                iView.networkAccessCallback(false, null);
            }
        }
    };

    private NetworkConfigCallback setWifiCallback = new NetworkConfigCallback() {
        @Override
        public void onSuccess(String requestUuid, int code, String message, String ssid, List<String> ipAddresses, int status) {
            if (setWifiUuid != null && setWifiUuid.equals(requestUuid)) {
                boolean isSuccess = (code < 400);
                if (iView != null) {
                    iView.distributeNetworkCallback(ssid, (ipAddresses == null ? new ArrayList<>() : ipAddresses), isSuccess);
                }
            }
        }

        @Override
        public void onFailed(String requestUuid, int code, String message, String ssid) {
            if (setWifiUuid != null && setWifiUuid.equals(requestUuid)) {
                if (iView != null && code == 561 && message != null && message.startsWith("ConnectToWifi failed")) {
                    iView.distributeNetworkCallback(null, null, false);
                }
            }
        }

        @Override
        public void onError(String requestUuid, String ssid, String errMsg) {
//            if (setWifiUuid != null && setWifiUuid.equals(requestUuid)) {
//                if (iView != null) {
//                    iView.distributeNetworkCallback(null, null, false);
//                }
//            }
        }
    };

    private EulixBaseResponseExtensionCallback ignoreNetworkCallback = new EulixBaseResponseExtensionCallback() {
        @Override
        public void onSuccess(String source, int code, String message, String requestId) {
            if (iView != null) {
                iView.ignoreNetworkCallback(code, source);
            }
        }

        @Override
        public void onFailed() {
            if (iView != null) {
                iView.ignoreNetworkCallback(-1, null);
            }
        }

        @Override
        public void onError(String errMsg) {
//            if (iView != null) {
//                iView.ignoreNetworkCallback(500, null);
//            }
        }
    };

    public interface IDistributeNetwork extends IBaseView {
        void wifiListCallback(List<WifiInfo> wifiInfoList);
        void networkAccessCallback(Boolean isSuccess, NetworkStatusResult networkStatusResult);
        void setNetworkConfigCallback(int code, String source);
        void ignoreNetworkCallback(int code, String source);
        void distributeNetworkCallback(String ssid, List<String> ipAddresses, Boolean isSuccess);
        void countdownTime(int timeSecond, int totalTimeSecond);
    }

    public void getWifiList() {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            try {
                String baseUrl = Urls.getBaseUrl();
                ThreadPool.getInstance().execute(() -> DeviceUtil.getWifiList(baseUrl
                        , gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                        , gatewayCommunicationBase.getTransformation(), gatewayCommunicationBase.getIvParams()
                        , scanWifiListCallback));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        } else if (iView != null) {
            iView.wifiListCallback(null);
        }
    }

    public void getAccessNetwork(boolean isRefresh) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            EulixNetUtil.getNetworkConfig(gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                    , gatewayCommunicationBase.getIvParams(), isRefresh, networkStatusCallback);
        } else if (iView != null) {
            iView.networkAccessCallback(null, null);
        }
    }

    public void setWifi(String ssid, String name, String password) {
        GatewayCommunicationBase gatewayCommunicationBase = null;
        if (name != null && password != null) {
            gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
            if (gatewayCommunicationBase == null) {
                gatewayCommunicationBase = GatewayUtils.generateLastGatewayCommunication(context);
            }
        }
        if (gatewayCommunicationBase != null) {
            GatewayCommunicationBase finalGatewayCommunicationBase = gatewayCommunicationBase;
            setWifiUuid = UUID.randomUUID().toString();
            final String requestUuid = setWifiUuid;
            try {
                ThreadPool.getInstance().execute(() -> DeviceUtil.setWifi(requestUuid, ssid, name, password
                        , finalGatewayCommunicationBase.getBoxDomain(), finalGatewayCommunicationBase.getAccessToken()
                        , finalGatewayCommunicationBase.getSecretKey(), finalGatewayCommunicationBase.getTransformation()
                        , finalGatewayCommunicationBase.getIvParams(), setWifiCallback));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        } else if (iView != null) {
            iView.distributeNetworkCallback(null, null, null);
        }
    }

    public void setNetworkConfig(String dns1, String dns2, String ipv6DNS1, String ipv6DNS2, List<NetworkAdapter> networkAdapters, boolean isChangeWifi) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            EulixNetUtil.setNetworkConfig(dns1, dns2, ipv6DNS1, ipv6DNS2, networkAdapters, gatewayCommunicationBase.getAccessToken()
                    , gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getIvParams(), new EulixBaseResponseExtensionCallback() {
                        @Override
                        public void onSuccess(String source, int code, String message, String requestId) {
                            if (isChangeWifi && iView != null) {
                                iView.setNetworkConfigCallback(code, source);
                            }
                        }

                        @Override
                        public void onFailed() {
                            if (isChangeWifi && iView != null) {
                                iView.setNetworkConfigCallback(-1, null);
                            }
                        }

                        @Override
                        public void onError(String errMsg) {
//                            if (iView != null) {
//                                iView.setNetworkConfigCallback(500, null);
//                            }
                        }
                    });
            if (!isChangeWifi && iView != null) {
                iView.setNetworkConfigCallback(100, null);
            }
        } else if (iView != null) {
            iView.setNetworkConfigCallback(501, null);
        }
    }

    public void ignoreNetwork(String wifiName) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            EulixNetUtil.ignoreNetwork(wifiName, gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                    , gatewayCommunicationBase.getIvParams(), ignoreNetworkCallback);
        } else if (iView != null) {
            iView.ignoreNetworkCallback(500, null);
        }
    }

    public CommonDeviceInfo getCommonDeviceInfo() {
        CommonDeviceInfo commonDeviceInfo = null;
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                        && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)
                        && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_DOMAIN)) {
                    commonDeviceInfo = new CommonDeviceInfo();
                    commonDeviceInfo.setBoxUuid(boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID));
                    commonDeviceInfo.setBoxBind(boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND));
                    commonDeviceInfo.setBoxDomain(boxValue.get(EulixSpaceDBManager.FIELD_BOX_DOMAIN));
                    break;
                }
            }
        }
        return commonDeviceInfo;
    }

    public void resetSetWifiUuid() {
        setWifiUuid = null;
    }

    public void startCountdown(int timeSecond) {
        stopCountdown();
        countDownTimer = new CountDownTimer((timeSecond * SECOND_UNIT), SECOND_UNIT) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (iView != null) {
                    iView.countdownTime((1 + (int) (millisUntilFinished / SECOND_UNIT)), timeSecond);
                }
            }

            @Override
            public void onFinish() {
                if (iView != null) {
                    iView.countdownTime(0, timeSecond);
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
}
