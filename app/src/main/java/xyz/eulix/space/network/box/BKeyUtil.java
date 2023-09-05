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

package xyz.eulix.space.network.box;

import android.content.Context;

import androidx.annotation.NonNull;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.interfaces.ResultCallback;
import xyz.eulix.space.interfaces.ResultCallbackObj;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.GatewayUtils;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.ThreadPool;
import xyz.eulix.space.util.Urls;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/8/19 17:33
 */
public class BKeyUtil {
    private static final String TAG = BKeyUtil.class.getSimpleName();
    private static Map<String, BKeyManager> managerMap = new HashMap<>();

    private BKeyUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    private static BKeyManager generateManager(String boxDomain) {
        BKeyManager bKeyManager = null;
        if (managerMap.containsKey(boxDomain)) {
            bKeyManager = managerMap.get(boxDomain);
        }
        if (bKeyManager == null) {
            bKeyManager = new BKeyManager(boxDomain);
            managerMap.put(boxDomain, bKeyManager);
        }
        return bKeyManager;
    }

    //获取平台授权码
    public static void obtainAuthCode(@NonNull Context context, String boxDomain, String accessToken, String authKey, String clientUUID, String boxName, String boxUUID, String secret, String transformation, String ivParams, boolean isLAN, BKeyCreateCallback callback) {

        BKeyCreate bKeyCreate = new BKeyCreate();
        bKeyCreate.setAccessToken(accessToken);
        bKeyCreate.setAuthKey(EncryptionUtil.encrypt(transformation, null, authKey, secret, StandardCharsets.UTF_8, ivParams));
        bKeyCreate.setClientUUID(EncryptionUtil.encrypt(transformation, null, clientUUID, secret, StandardCharsets.UTF_8, ivParams));
        bKeyCreate.setBoxName(EncryptionUtil.encrypt(transformation, null, boxName, secret, StandardCharsets.UTF_8, ivParams));
        bKeyCreate.setBoxUUID(EncryptionUtil.encrypt(transformation, null, boxUUID, secret, StandardCharsets.UTF_8, ivParams));
        bKeyCreate.setVersion("v2");

        String finalBoxDomain = Urls.getBaseUrl();
        if (finalBoxDomain != null) {
            try {
                ThreadPool.getInstance().execute(() -> generateManager(finalBoxDomain).obtainAuthCode(UUID.randomUUID().toString(), bKeyCreate, new IBKeyCreateCallback() {
                    @Override
                    public void onError(String msg) {
                        if (callback != null) {
                            callback.onError(msg);
                        }
                    }

                    @Override
                    public void onResult(BKeyCreateResponseBody result) {
                        Logger.i(TAG, "on result: " + result);
                        AuthCodeInfo authCodeInfo = null;
                        if (result != null) {
                            authCodeInfo = result.getAuthCodeInfo();
                        }
                        if (authCodeInfo == null) {
                            if (callback != null) {
                                callback.onFailed((result == null ? -1 : result.getCode()), (result == null ? "" : result.getMessage()));
                            }
                        } else {
                            if (callback != null) {
                                //解密authCodeInfo
                                String authCode = EncryptionUtil.decrypt(transformation, null, authCodeInfo.getAuthCode(), secret, StandardCharsets.UTF_8, ivParams);
                                String bKey = EncryptionUtil.decrypt(transformation, null, authCodeInfo.getBkey(), secret, StandardCharsets.UTF_8, ivParams);
                                //配置明文信息
                                authCodeInfo.setAuthCode(authCode);
                                authCodeInfo.setBkey(bKey);

                                callback.onSuccess(result.getCode(), result.getMessage(), authCodeInfo);
                            }
                        }
                    }
                }));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        } else if (callback != null) {
            callback.onError(null);
        }
    }

    public static void obtainAuthResult(@NonNull Context context, String boxDomain, String boxUUID, String boxKey, boolean isAutoLogin, boolean isLAN, BKeyPollCallback callback) {
        Logger.d("zfy", "#obtainAuthResult bKey=" + boxKey);

        String finalBoxDomain = Urls.getBaseUrl();
        if (finalBoxDomain != null) {
            try {
                ThreadPool.getInstance().execute(() -> generateManager(finalBoxDomain).obtainAuthResult(UUID.randomUUID().toString(), boxKey, isAutoLogin, new IBKeyPollCallback() {
                    @Override
                    public void onError(String msg) {
                        Logger.e(TAG, "on error: " + msg);
                        if (callback != null) {
                            callback.onError(msg);
                        }
                    }

                    @Override
                    public void onResult(BKeyPollResponseBody result) {
                        Logger.i(TAG, "on result: " + result);
                        if (result == null) {
                            if (callback != null) {
                                callback.onFailed("");
                            }
                        } else {
                            if (callback != null) {
                                if (result.isResult()) {
                                    callback.onSuccess(result.getRequestId());
                                } else {
                                    callback.onFailed(result.getRequestId());
                                }
                            }
                        }
                    }
                }));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        } else if (callback != null) {
            callback.onError(null);
        }
    }

    //获取局域网盒子授权码
    public static void obtainBoxLoginAuthCode(Context context, BKeyCreateCallback callback) {

        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            String baseUrl = Urls.getBaseUrl();
            ThreadPool.getInstance().execute(() -> BKeyManager.obtainBoxLoginAuthCode(
                    baseUrl, gatewayCommunicationBase.getAccessToken(),
                    gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getTransformation(),
                    gatewayCommunicationBase.getIvParams(), ConstantField.BoxVersionName.VERSION_0_1_0, callback));
        } else if (callback != null) {
            callback.onError("gatewayCommunicationBase is null");
        }
    }

    //获取局域网盒子授权扫码结果
    public static void obtainBoxLoginAuthResult(Context context, String boxKey, boolean isAutoLogin, BKeyPollCallback callback) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            String baseUrl = Urls.getBaseUrl();
            ThreadPool.getInstance().execute(() -> BKeyManager.obtainBoxLoginAuthResult(
                    baseUrl, gatewayCommunicationBase.getAccessToken(),
                    gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getTransformation(),
                    gatewayCommunicationBase.getIvParams(), ConstantField.BoxVersionName.VERSION_0_1_0, boxKey, isAutoLogin, new ResultCallbackObj() {
                        @Override
                        public void onResult(boolean result, Object extraObj) {
                            if (extraObj == null) {
                                if (callback != null) {
                                    callback.onFailed("");
                                }
                            } else {
                                BKeyCheckResponseBody responseBody = (BKeyCheckResponseBody) extraObj;
                                if (callback != null) {
                                    if (responseBody.results) {
                                        callback.onSuccess(responseBody.getRequestId());
                                    } else {
                                        callback.onFailed(responseBody.getRequestId());
                                    }
                                }
                            }
                        }

                        @Override
                        public void onError(String msg) {
                            Logger.e(TAG, "on error: " + msg);
                            if (callback != null) {
                                callback.onError(msg);
                            }
                        }
                    }));
        } else if (callback != null) {
            callback.onError("gatewayCommunicationBase is null");
        }
    }

    //校验盒子bKey
    public static void bKeyVerify(Context context, String boxKey, ResultCallback callback) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            String baseUrl = Urls.getBaseUrl();
            ThreadPool.getInstance().execute(() -> BKeyManager.bKeyVerify(
                    baseUrl, gatewayCommunicationBase.getAccessToken(),
                    gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getTransformation(),
                    gatewayCommunicationBase.getIvParams(), ConstantField.BoxVersionName.VERSION_0_1_0, boxKey, callback));
        } else if (callback != null) {
            callback.onResult(false, "gatewayCommunicationBase is null");
        }
    }
}
