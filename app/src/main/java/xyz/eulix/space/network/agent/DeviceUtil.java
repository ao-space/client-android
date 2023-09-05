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

package xyz.eulix.space.network.agent;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.bean.bind.WifiListRsp;
import xyz.eulix.space.bean.bind.WpwdInfo;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.ThreadPool;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/11/18 11:12
 */
public class DeviceUtil {
    private static final String TAG = DeviceUtil.class.getSimpleName();
    private static final String API_VERSION = ConstantField.BoxVersionName.VERSION_0_2_5;

    private DeviceUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    public static void getLocalIps(String boxDomain, String accessToken, String secret, String transformation, String ivParams, LocalIpInfoCallback callback) {
        DeviceManager.getLocalIps(boxDomain, accessToken, secret, transformation, ivParams, API_VERSION, new ILocalIpInfoCallback() {
            @Override
            public void onResult(LocalIpInfo result) {
                if (result == null) {
                    if (callback != null) {
                        callback.onFailed();
                    }
                } else {
                    if (callback != null) {
                        callback.onSuccess(result.getCode(), result.getMessage(), result.getValue());
                    }
                }
            }

            @Override
            public void onError(String errMsg) {
                if (callback != null) {
                    callback.onError(errMsg);
                }
            }
        });
    }

    public static void getWifiList(String boxDomain, String boxKey, String boxIv, ScanWifiListCallback callback) {
        DeviceManager.getWifiList(boxDomain, new IScanWifiListCallback() {
            @Override
            public void onResult(ScanWifiList result) {
                List<WifiInfo> wifiInfoList = null;
                if (result != null) {
                    String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                            , null, result.getWifiInfoList(), boxKey, StandardCharsets.UTF_8, boxIv);
                    if (decryptedContent != null) {
                        try {
                            wifiInfoList = new Gson().fromJson(decryptedContent, new TypeToken<List<WifiInfo>>(){}.getType());
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (wifiInfoList == null) {
                    if (callback != null) {
                        callback.onFailed();
                    }
                } else {
                    if (callback != null) {
                        callback.onSuccess(result.getCode(), result.getMessage(), wifiInfoList);
                    }
                }
            }

            @Override
            public void onError(String errMsg) {
                if (callback != null) {
                    callback.onError(errMsg);
                }
            }
        });
    }

    public static void getWifiList(String boxDomain, AgentEncryptedResultsCallback callback) {
        try {
            ThreadPool.getInstance().execute(() -> DeviceManager.getWifiList(boxDomain, new IScanWifiListCallback() {
                @Override
                public void onResult(ScanWifiList result) {
                    String results = null;
                    int code = 500;
                    String source = null;
                    String message = null;
                    if (result != null) {
                        String codeValue = result.getCode();
                        code = DataUtil.stringCodeToInt(codeValue);
                        source = DataUtil.stringCodeGetSource(codeValue);
                        message = result.getMessage();
                        results = result.getWifiInfoList();
                    }
                    if (results == null) {
                        if (callback != null) {
                            callback.onFailed(code, source, message);
                        }
                    } else if (callback != null) {
                        callback.onSuccess(code, source, message, results);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (callback != null) {
                        callback.onError(errMsg);
                    }
                }
            }));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void getWifiList(String boxDomain, String accessToken, String secret, String transformation, String ivParams, ScanWifiListCallback callback) {
        DeviceManager.getRealWifiList(boxDomain, accessToken, secret, transformation, ivParams, API_VERSION, new IScanRealWifiListCallback() {
            @Override
            public void onResult(ScanRealWifiList result) {
                List<WifiInfo> wifiInfoList = null;
                if (result != null) {
                    wifiInfoList = result.getWifiInfoList();
                }
                if (wifiInfoList == null) {
                    if (callback != null) {
                        callback.onFailed();
                    }
                } else {
                    if (callback != null) {
                        callback.onSuccess(result.getCode(), result.getMessage(), wifiInfoList);
                    }
                }
            }

            @Override
            public void onError(String errMsg) {
                if (callback != null) {
                    callback.onError(errMsg);
                }
            }
        });
    }

    public static void setWifi(String requestUuid, String ssid, String name, String password, String boxDomain, String boxKey, String boxIv, NetworkConfigCallback callback) {
        ConnectWifiReq connectWifiReq = new ConnectWifiReq();
        connectWifiReq.setName(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                , null, name, boxKey, StandardCharsets.UTF_8, boxIv));
        connectWifiReq.setPassword(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                , null, password, boxKey, StandardCharsets.UTF_8, boxIv));
        DeviceManager.setWifi(connectWifiReq, boxDomain, new INetworkConfigCallback() {
            @Override
            public void onResult(NetworkConfigResponse result) {
                NetworkConfigResult networkConfigResult = null;
                int code = -1;
                String message = null;
                if (result != null) {
                    code = DataUtil.stringCodeToInt(result.getCode());
                    message = result.getMessage();
                }
                if (result != null) {
                    String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                            , null, result.getResult(), boxKey, StandardCharsets.UTF_8, boxIv);
                    if (decryptedContent != null) {
                        try {
                            networkConfigResult = new Gson().fromJson(decryptedContent, NetworkConfigResult.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (networkConfigResult == null) {
                    if (callback != null) {
                        callback.onFailed(requestUuid, code, message, ssid);
                    }
                } else {
                    String ssid = networkConfigResult.getAddress();
                    List<String> ipAddresses = networkConfigResult.getIpAddresses();
                    int status = networkConfigResult.getStatus();
                    if (callback != null) {
                        callback.onSuccess(requestUuid, code, message, ssid, ipAddresses, status);
                    }
                }
            }

            @Override
            public void onError(String errMsg) {
                if (callback != null) {
                    callback.onError(requestUuid, ssid, errMsg);
                }
            }
        });
    }

    public static void setWifi(String boxDomain, WpwdInfo wpwdInfo, AgentEncryptedResultsCallback callback) {
        try {
            ThreadPool.getInstance().execute(() -> DeviceManager.setWifi(wpwdInfo, boxDomain, new INetworkConfigCallback() {
                @Override
                public void onResult(NetworkConfigResponse result) {
                    String results = null;
                    int code = 500;
                    String source = null;
                    String message = null;
                    if (result != null) {
                        String codeValue = result.getCode();
                        code = DataUtil.stringCodeToInt(codeValue);
                        source = DataUtil.stringCodeGetSource(codeValue);
                        message = result.getMessage();
                        results = result.getResult();
                    }
                    if (results == null) {
                        if (callback != null) {
                            callback.onFailed(code, source, message);
                        }
                    } else if (callback != null) {
                        callback.onSuccess(code, source, message, results);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (callback != null) {
                        callback.onError(errMsg);
                    }
                }
            }));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void setWifi(String requestUuid, String ssid, String name, String password, String boxDomain, String accessToken, String secret, String transformation, String ivParams, NetworkConfigCallback callback) {
        ConnectWifiReq connectWifiReq = new ConnectWifiReq();
        connectWifiReq.setName(name);
        connectWifiReq.setPassword(password);
        DeviceManager.setRealWifi(connectWifiReq, boxDomain, accessToken, secret, transformation, ivParams, API_VERSION, new INetworkConfigRealCallback() {
            @Override
            public void onResult(NetworkConfigRealResponse result) {
                NetworkConfigResult networkConfigResult = null;
                int code = -1;
                String message = null;
                if (result != null) {
                    networkConfigResult = result.getResult();
                    code = DataUtil.stringCodeToInt(result.getCode());
                    message = result.getMessage();
                }
                if (code == 200 && networkConfigResult == null) {
                    networkConfigResult = new NetworkConfigResult();
                }
                if (networkConfigResult == null) {
                    if (callback != null) {
                        callback.onFailed(requestUuid, code, message, ssid);
                    }
                } else {
                    String ssid = networkConfigResult.getAddress();
                    List<String> ipAddresses = networkConfigResult.getIpAddresses();
                    int status = networkConfigResult.getStatus();
                    if (callback != null) {
                        callback.onSuccess(requestUuid, code, message, ssid, ipAddresses, status);
                    }
                }
            }

            @Override
            public void onError(String errMsg) {
                if (callback != null) {
                    callback.onError(requestUuid, ssid, errMsg);
                }
            }
        });
    }

//    public static void getNetwork(String boxDomain, String accessToken, String secret, String transformation, String ivParams, NetworkInfoCallback callback) {
//        DeviceManager.getNetwork(boxDomain, accessToken, secret, transformation, ivParams, API_VERSION, new INetworkInfoCallback() {
//            @Override
//            public void onResult(NetworkInfo result) {
//                if (result == null) {
//                    if (callback != null) {
//                        callback.onFailed();
//                    }
//                } else {
//                    if (callback != null) {
//                        callback.onSuccess(result.getCode(), result.getMessage(), result.getNetworks());
//                    }
//                }
//            }
//
//            @Override
//            public void onError(String errMsg) {
//                if (callback != null) {
//                    callback.onError(errMsg);
//                }
//            }
//        });
//    }
}
