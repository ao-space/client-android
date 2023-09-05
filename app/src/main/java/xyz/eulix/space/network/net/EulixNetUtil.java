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

package xyz.eulix.space.network.net;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.network.EulixBaseResponse;
import xyz.eulix.space.network.EulixBaseResponseExtensionCallback;
import xyz.eulix.space.network.IEulixBaseResponseCallback;
import xyz.eulix.space.network.agent.net.NetworkAdapter;
import xyz.eulix.space.network.agent.net.NetworkConfigRequest;
import xyz.eulix.space.network.agent.net.NetworkIgnoreRequest;
import xyz.eulix.space.network.agent.net.NetworkStatusResult;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.ThreadPool;
import xyz.eulix.space.util.Urls;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/12/1 17:15
 */
public class EulixNetUtil {
    private static final String TAG = EulixNetUtil.class.getSimpleName();
    private static final String API_VERSION = ConstantField.BoxVersionName.VERSION_0_2_5;

    public static void getNetworkConfig(String accessToken, String secret, String ivParams, NetworkStatusCallback callback) {
        getNetworkConfig(accessToken, secret, ivParams, false, callback);
    }

    public static void getNetworkConfig(String accessToken, String secret, String ivParams, boolean isFore, NetworkStatusCallback callback) {
        String finalBoxDomain = Urls.getBaseUrl();
        try {
            ThreadPool.getInstance().execute(() -> EulixNetManager.getNetworkConfig(finalBoxDomain
                    , accessToken, secret, ivParams, API_VERSION, new INetworkStatusCallback() {
                @Override
                public void onResponse(NetworkStatusResponse response) {
                    NetworkStatusResult result = null;
                    int code = -1;
                    String source = null;
                    String message = null;
                    String requestId = null;
                    if (response != null) {
                        String codeValue = response.getCode();
                        code = DataUtil.stringCodeToInt(codeValue);
                        source = DataUtil.stringCodeGetSource(codeValue);
                        message = response.getMessage();
                        requestId = response.getRequestId();
                        result = response.getResults();
                    }
                    if (result == null) {
                        if (callback != null) {
                            callback.onFail(code, source, message, requestId);
                        }
                    } else if (callback != null) {
                        callback.onSuccess(code, source, message, requestId, result);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (callback != null) {
                        callback.onError(errMsg);
                    }
                }
            }), isFore);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void setNetworkConfig(String dns1, String dns2, String ipv6DNS1, String ipv6DNS2, List<NetworkAdapter> networkAdapters, String accessToken, String secret, String ivParams, EulixBaseResponseExtensionCallback callback) {
        NetworkConfigRequest networkConfigRequest = new NetworkConfigRequest();
        networkConfigRequest.setdNS1(dns1);
        networkConfigRequest.setdNS2(dns2);
        networkConfigRequest.setIpv6DNS1(ipv6DNS1);
        networkConfigRequest.setIpv6DNS2(ipv6DNS2);
        networkConfigRequest.setNetworkAdapters(networkAdapters);
        String finalBoxDomain = Urls.getBaseUrl();
        try {
            ThreadPool.getInstance().execute(() -> EulixNetManager.setNetworkConfig(networkConfigRequest
                    , finalBoxDomain, accessToken, secret, ivParams, API_VERSION, new IEulixBaseResponseCallback() {
                @Override
                public void onResult(EulixBaseResponse response) {
                    int code = -1;
                    String source = null;
                    String message = null;
                    String requestId = null;
                    if (response != null) {
                        String codeValue = response.getCode();
                        if (codeValue != null) {
                            code = DataUtil.stringCodeToInt(codeValue);
                            source = DataUtil.stringCodeGetSource(codeValue);
                        }
                        message = response.getMessage();
                        requestId = response.getRequestId();
                    }
                    if (response == null) {
                        if (callback != null) {
                            callback.onFailed();
                        }
                    } else if (callback != null) {
                        callback.onSuccess(source, code, message, requestId);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (callback != null) {
                        callback.onError(errMsg);
                    }
                }
            }), true);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void ignoreNetwork(String wifiName, String accessToken, String secret, String ivParams, EulixBaseResponseExtensionCallback callback) {
        NetworkIgnoreRequest networkIgnoreRequest = new NetworkIgnoreRequest();
        networkIgnoreRequest.setwIFIName(wifiName);
        String finalBoxDomain = Urls.getBaseUrl();
        try {
            ThreadPool.getInstance().execute(() -> EulixNetManager.ignoreNetwork(networkIgnoreRequest
                    , finalBoxDomain, accessToken, secret, ivParams, API_VERSION, new IEulixBaseResponseCallback() {
                        @Override
                        public void onResult(EulixBaseResponse response) {
                            int code = -1;
                            String source = null;
                            String message = null;
                            String requestId = null;
                            if (response != null) {
                                String codeValue = response.getCode();
                                if (codeValue != null) {
                                    code = DataUtil.stringCodeToInt(codeValue);
                                    source = DataUtil.stringCodeGetSource(codeValue);
                                }
                                message = response.getMessage();
                                requestId = response.getRequestId();
                            }
                            if (response == null) {
                                if (callback != null) {
                                    callback.onFailed();
                                }
                            } else if (callback != null) {
                                callback.onSuccess(source, code, message, requestId);
                            }
                        }

                        @Override
                        public void onError(String errMsg) {
                            if (callback != null) {
                                callback.onError(errMsg);
                            }
                        }
                    }), true);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void getDeviceAbility(String accessToken, String secret, String ivParams, DeviceAbilityCallback callback) {
        getDeviceAbility(accessToken, secret, ivParams, false, callback);
    }

    public static void getDeviceAbility(String accessToken, String secret, String ivParams, boolean isFore, DeviceAbilityCallback callback) {
        String finalBoxDomain = Urls.getBaseUrl();
        try {
            ThreadPool.getInstance().execute(() -> EulixNetManager.getDeviceAbility(finalBoxDomain
                    , accessToken, secret, ivParams, API_VERSION, new IDeviceAbilityCallback() {
                        @Override
                        public void onResponse(DeviceAbilityResponse response) {
                            DeviceAbility result = null;
                            int code = -1;
                            String source = null;
                            String message = null;
                            String requestId = null;
                            if (response != null) {
                                String codeValue = response.getCode();
                                code = DataUtil.stringCodeToInt(codeValue);
                                source = DataUtil.stringCodeGetSource(codeValue);
                                message = response.getMessage();
                                requestId = response.getRequestId();
                                result = response.getResults();
                            }
                            if (result == null) {
                                if (callback != null) {
                                    callback.onFail(code, source, message, requestId);
                                }
                            } else if (callback != null) {
                                callback.onSuccess(code, source, message, requestId, result);
                            }
                        }

                        @Override
                        public void onError(String errMsg) {
                            if (callback != null) {
                                callback.onError(errMsg);
                            }
                        }
                    }), isFore);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void getNetworkChannelInfo(String accessToken, String secret, String ivParams, boolean isFore, ChannelInfoCallback callback) {
        String finalBoxDomain = Urls.getBaseUrl();
        try {
            ThreadPool.getInstance().execute(() -> EulixNetManager.getNetworkChannelInfo(finalBoxDomain
                    , accessToken, secret, ivParams, API_VERSION, new IChannelInfoCallback() {
                @Override
                public void onResponse(ChannelInfoResponse response) {
                    ChannelInfoResult result = null;
                    int code = -1;
                    String source = null;
                    String message = null;
                    String requestId = null;
                    if (response != null) {
                        String codeValue = response.getCode();
                        code = DataUtil.stringCodeToInt(codeValue);
                        source = DataUtil.stringCodeGetSource(codeValue);
                        message = response.getMessage();
                        requestId = response.getRequestId();
                        result = response.getResults();
                    }
                    if (result == null) {
                        if (callback != null) {
                            callback.onFail(code, source, message, requestId);
                        }
                    } else if (callback != null) {
                        callback.onSuccess(code, source, message, requestId, result);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (callback != null) {
                        callback.onError(errMsg);
                    }
                }
            }), isFore);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void setNetworkChannelWan(boolean isWan, String accessToken, String secret, String ivParams, boolean isFore, ChannelInfoCallback callback) {
        ChannelWanRequest channelWanRequest = new ChannelWanRequest();
        channelWanRequest.setWan(isWan);
        String finalBoxDomain = Urls.getBaseUrl();
        Logger.d(TAG, "url: " + finalBoxDomain + ", request: " + channelWanRequest);
        try {
            ThreadPool.getInstance().execute(() -> EulixNetManager.setNetworkChannelWan(channelWanRequest
                    , finalBoxDomain, accessToken, secret, ivParams, API_VERSION, new IChannelInfoCallback() {
                @Override
                public void onResponse(ChannelInfoResponse response) {
                    ChannelInfoResult result = null;
                    int code = -1;
                    String source = null;
                    String message = null;
                    String requestId = null;
                    if (response != null) {
                        String codeValue = response.getCode();
                        code = DataUtil.stringCodeToInt(codeValue);
                        source = DataUtil.stringCodeGetSource(codeValue);
                        message = response.getMessage();
                        requestId = response.getRequestId();
                        result = response.getResults();
                    }
                    if (result == null) {
                        if (callback != null) {
                            callback.onFail(code, source, message, requestId);
                        }
                    } else if (callback != null) {
                        callback.onSuccess(code, source, message, requestId, result);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (callback != null) {
                        callback.onError(errMsg);
                    }
                }
            }), isFore);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void getInternetServiceConfig(String clientUuid, String aoId, String boxDomain, String accessToken, String secret, String ivParams, boolean isFore, InternetServiceConfigCallback callback) {
        try {
            ThreadPool.getInstance().execute(() -> EulixNetManager.getInternetServiceConfig(clientUuid, aoId
                    , boxDomain, accessToken, secret, ivParams, API_VERSION, new IInternetServiceConfigCallback() {
                @Override
                public void onResponse(InternetServiceConfigResponse response) {
                    InternetServiceConfigResult result = null;
                    int code = -1;
                    String source = null;
                    String message = null;
                    String requestId = null;
                    if (response != null) {
                        String codeValue = response.getCode();
                        code = DataUtil.stringCodeToInt(codeValue);
                        source = DataUtil.stringCodeGetSource(codeValue);
                        message = response.getMessage();
                        requestId = response.getRequestId();
                        result = response.getResults();
                    }
                    if (result == null) {
                        if (callback != null) {
                            callback.onFail(code, source, message, requestId);
                        }
                    } else if (callback != null) {
                        callback.onSuccess(code, source, message, requestId, result);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (callback != null) {
                        callback.onError(errMsg);
                    }
                }
            }), isFore);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void setInternetServiceConfig(String clientUuid, boolean isEnableInternetAccess, String platformApiBase, String accessToken, String secret, String ivParams, boolean isFore, InternetServiceConfigCallback callback) {
        InternetServiceConfigRequest internetServiceConfigRequest = new InternetServiceConfigRequest();
        internetServiceConfigRequest.setClientUUID(clientUuid);
        internetServiceConfigRequest.setEnableInternetAccess(isEnableInternetAccess);
        internetServiceConfigRequest.setPlatformApiBase(platformApiBase);
        String finalBoxDomain = Urls.getBaseUrl();
        Logger.d(TAG, "url: " + finalBoxDomain + ", request: " + internetServiceConfigRequest);
        try {
            ThreadPool.getInstance().execute(() -> EulixNetManager.setInternetServiceConfig(internetServiceConfigRequest
                    , finalBoxDomain, accessToken, secret, ivParams, API_VERSION, new IInternetServiceConfigCallback() {
                @Override
                public void onResponse(InternetServiceConfigResponse response) {
                    InternetServiceConfigResult result = null;
                    int code = -1;
                    String source = null;
                    String message = null;
                    String requestId = null;
                    if (response != null) {
                        String codeValue = response.getCode();
                        code = DataUtil.stringCodeToInt(codeValue);
                        source = DataUtil.stringCodeGetSource(codeValue);
                        message = response.getMessage();
                        requestId = response.getRequestId();
                        result = response.getResults();
                    }
                    if (result == null) {
                        if (callback != null) {
                            callback.onFail(code, source, message, requestId);
                        }
                    } else if (callback != null) {
                        callback.onSuccess(code, source, message, requestId, result);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (callback != null) {
                        callback.onError(errMsg);
                    }
                }
            }), isFore);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }
}
