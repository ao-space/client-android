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

package xyz.eulix.space.network.agent.platform;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.network.agent.AgentEncryptedResultsCallback;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.ThreadPool;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/11/25 15:52
 */
public class EulixPlatformUtil {
    private static final String TAG = EulixPlatformUtil.class.getSimpleName();
    private static Map<String, EulixPlatformManager> managerMap = new HashMap<>();

    private EulixPlatformUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    private static EulixPlatformManager generateManager(String baseUrl) {
        EulixPlatformManager eulixPlatformManager = null;
        if (managerMap.containsKey(baseUrl)) {
            eulixPlatformManager = managerMap.get(baseUrl);
        }
        if (eulixPlatformManager == null) {
            eulixPlatformManager = new EulixPlatformManager(baseUrl);
            managerMap.put(baseUrl, eulixPlatformManager);
        }
        return eulixPlatformManager;
    }

    public static void getPlatformAbility(@NonNull String baseUrl, PlatformAbilityCallback callback) {
        getPlatformAbility(baseUrl, false, callback);
    }

    public static void getPlatformAbility(@NonNull String baseUrl, boolean isFore, PlatformAbilityCallback callback) {
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).getPlatformAbility(new IPlatformAbilityCallback() {
                @Override
                public void onResponse(PlatformAbilityResponse response) {
                    List<PlatformApi> platformApis = null;
                    if (response != null) {
                        platformApis = response.getPlatformApis();
                    }
                    if (platformApis == null) {
                        if (callback != null) {
                            callback.onFailed();
                        }
                    } else if (callback != null) {
                        callback.onSuccess(platformApis);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    Logger.e(TAG, "on error: " + errMsg);
                    if (callback != null) {
                        callback.onError(errMsg);
                    }
                }
            }));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void switchPlatform(@NonNull String baseUrl, String bleKey, String bleIv, String transId, String domain, SwitchPlatformCallback callback) {
        SwitchPlatformRequest switchPlatformRequest = new SwitchPlatformRequest();
        switchPlatformRequest.setTransId(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                , null, transId, bleKey, StandardCharsets.UTF_8, bleIv));
        switchPlatformRequest.setDomain(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                , null, domain, bleKey, StandardCharsets.UTF_8, bleIv));
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).switchPlatform(switchPlatformRequest
                    , new ISwitchPlatformCallback() {
                @Override
                public void onResponse(SwitchPlatformResponse response) {
                    Logger.i(TAG, "on result: " + response);
                    int code = -1;
                    String source = null;
                    String message = null;
                    String requestId = null;
                    SwitchPlatformResult result = null;
                    if (response != null) {
                        String codeValue = response.getCode();
                        if (codeValue != null) {
                            code = DataUtil.stringCodeToInt(codeValue);
                            source = DataUtil.stringCodeGetSource(codeValue);
                        }
                        message = response.getMessage();
                        requestId = response.getRequestId();
                        String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                                , null, response.getResults(), bleKey, StandardCharsets.UTF_8, bleIv);
                        Logger.i(TAG, "on decrypt result: " + decryptedContent);
                        if (decryptedContent != null) {
                            try {
                                result = new Gson().fromJson(decryptedContent, SwitchPlatformResult.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (result == null) {
                        if (callback != null) {
                            callback.onFailed(code, source, message, requestId);
                        }
                    } else if (callback != null) {
                        callback.onSuccess(code, source, message, requestId, result);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    Logger.e(TAG, "on error: " + errMsg);
                    if (callback != null) {
                        callback.onError(errMsg);
                    }
                }
            }));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void switchPlatform(@NonNull String baseUrl, SwitchPlatformRequest switchPlatformRequest, AgentEncryptedResultsCallback callback) {
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).switchPlatform(switchPlatformRequest
                    , new ISwitchPlatformCallback() {
                        @Override
                        public void onResponse(SwitchPlatformResponse response) {
                            Logger.i(TAG, "on result: " + response);
                            String results = null;
                            int code = 500;
                            String source = null;
                            String message = null;
                            if (response != null) {
                                String codeValue = response.getCode();
                                code = DataUtil.stringCodeToInt(codeValue);
                                source = DataUtil.stringCodeGetSource(codeValue);
                                message = response.getMessage();
                                results = response.getResults();
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
                            Logger.e(TAG, "on error: " + errMsg);
                            if (callback != null) {
                                callback.onError(errMsg);
                            }
                        }
                    }));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void getSwitchPlatformStatus(@NonNull String baseUrl, String bleKey, String bleIv, String transId, SwitchStatusCallback callback) {
        String query = transId;
        if (bleKey != null && bleIv != null) {
            query = EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, transId, bleKey, StandardCharsets.UTF_8, bleIv);
        }
        String finalQuery = query;
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).getSwitchPlatformStatus(finalQuery
                    , new ISwitchStatusCallback() {
                        @Override
                        public void onResponse(SwitchStatusResponse response) {
                            Logger.i(TAG, "on result: " + response);
                            int code = -1;
                            String source = null;
                            String message = null;
                            String requestId = null;
                            SwitchStatusResult result = null;
                            if (response != null) {
                                String codeValue = response.getCode();
                                if (codeValue != null) {
                                    code = DataUtil.stringCodeToInt(codeValue);
                                    source = DataUtil.stringCodeGetSource(codeValue);
                                }
                                message = response.getMessage();
                                requestId = response.getRequestId();
                                String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                                        , null, response.getResults(), bleKey, StandardCharsets.UTF_8, bleIv);
                                Logger.i(TAG, "on decrypt result: " + decryptedContent);
                                if (decryptedContent != null) {
                                    try {
                                        result = new Gson().fromJson(decryptedContent, SwitchStatusResult.class);
                                    } catch (JsonSyntaxException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            if (result == null) {
                                if (callback != null) {
                                    callback.onFailed(code, source, message, requestId);
                                }
                            } else if (callback != null) {
                                callback.onSuccess(code, source, message, requestId, result);
                            }
                        }

                        @Override
                        public void onError(String errMsg) {
                            Logger.e(TAG, "on error: " + errMsg);
                            if (callback != null) {
                                callback.onError(errMsg);
                            }
                        }
                    }));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void getSwitchPlatformStatus(@NonNull String baseUrl, String transId, AgentEncryptedResultsCallback callback) {
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).getSwitchPlatformStatus(transId
                    , new ISwitchStatusCallback() {
                        @Override
                        public void onResponse(SwitchStatusResponse response) {
                            Logger.i(TAG, "on result: " + response);
                            String results = null;
                            int code = 500;
                            String source = null;
                            String message = null;
                            if (response != null) {
                                String codeValue = response.getCode();
                                code = DataUtil.stringCodeToInt(codeValue);
                                source = DataUtil.stringCodeGetSource(codeValue);
                                message = response.getMessage();
                                results = response.getResults();
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
                            Logger.e(TAG, "on error: " + errMsg);
                            if (callback != null) {
                                callback.onError(errMsg);
                            }
                        }
                    }));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }
}
