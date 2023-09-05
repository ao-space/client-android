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

package xyz.eulix.space.network.agent.net;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.network.EulixBaseRequest;
import xyz.eulix.space.network.EulixBaseResponse;
import xyz.eulix.space.network.EulixBaseResponseExtensionCallback;
import xyz.eulix.space.network.IEulixBaseResponseCallback;
import xyz.eulix.space.network.agent.AgentBaseResponse;
import xyz.eulix.space.network.agent.AgentEncryptedResultsCallback;
import xyz.eulix.space.network.agent.IAgentBaseResponseCallback;
import xyz.eulix.space.network.net.NetworkStatusCallback;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.ThreadPool;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/12/1 16:53
 */
public class EulixNetUtil {
    private static final String TAG = EulixNetUtil.class.getSimpleName();
    private static Map<String, EulixNetManager> managerMap = new HashMap<>();

    private EulixNetUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    private static EulixNetManager generateManager(String baseUrl) {
        EulixNetManager eulixNetManager = null;
        if (managerMap.containsKey(baseUrl)) {
            eulixNetManager = managerMap.get(baseUrl);
        }
        if (eulixNetManager == null) {
            eulixNetManager = new EulixNetManager(baseUrl);
            managerMap.put(baseUrl, eulixNetManager);
        }
        return eulixNetManager;
    }

    private static EulixBaseRequest generateEulixBaseRequest(@NonNull String request, String bleKey, String bleIv) {
        if (bleKey != null && bleIv != null) {
            request = EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, request, bleKey, StandardCharsets.UTF_8, bleIv);
        }
        EulixBaseRequest eulixBaseRequest = new EulixBaseRequest();
        eulixBaseRequest.setBody(request);
        return eulixBaseRequest;
    }

    public static void getNetworkConfig(@NonNull String baseUrl, String bleKey, String bleIv, NetworkStatusCallback callback) {
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).getNetworkConfig(new IAgentBaseResponseCallback() {
                @Override
                public void onResponse(AgentBaseResponse response) {
                    int code = -1;
                    String source = null;
                    String message = null;
                    String requestId = null;
                    NetworkStatusResult result = null;
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
                        Logger.i(TAG, "get network config decrypt: " + decryptedContent);
                        if (decryptedContent != null) {
                            try {
                                result = new Gson().fromJson(decryptedContent, NetworkStatusResult.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
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
            }));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void getNetworkConfig(@NonNull String baseUrl, AgentEncryptedResultsCallback callback) {
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).getNetworkConfig(new IAgentBaseResponseCallback() {
                @Override
                public void onResponse(AgentBaseResponse response) {
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
                    if (callback != null) {
                        callback.onError(errMsg);
                    }
                }
            }));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void setNetworkConfig(@NonNull String baseUrl, String bleKey, String bleIv, NetworkConfigRequest networkConfigRequest, EulixBaseResponseExtensionCallback callback) {
        if (networkConfigRequest != null) {
            EulixBaseRequest eulixBaseRequest = generateEulixBaseRequest(new Gson().toJson(networkConfigRequest, NetworkConfigRequest.class), bleKey, bleIv);
            try {
                ThreadPool.getInstance().execute(() -> generateManager(baseUrl).setNetworkConfig(eulixBaseRequest
                        , new IEulixBaseResponseCallback() {
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
                }));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        } else if (callback != null) {
            callback.onError("request body null");
        }
    }

    public static void setNetworkConfig(@NonNull String baseUrl, String request, EulixBaseResponseExtensionCallback callback) {
        EulixBaseRequest eulixBaseRequest = new EulixBaseRequest();
        eulixBaseRequest.setBody(request);
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).setNetworkConfig(eulixBaseRequest
                    , new IEulixBaseResponseCallback() {
                        @Override
                        public void onResult(EulixBaseResponse response) {
                            int code = 500;
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
                    }));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void ignoreNetwork(@NonNull String baseUrl, String bleKey, String bleIv, NetworkIgnoreRequest networkIgnoreRequest, EulixBaseResponseExtensionCallback callback) {
        if (networkIgnoreRequest != null) {
            EulixBaseRequest eulixBaseRequest = generateEulixBaseRequest(new Gson().toJson(networkIgnoreRequest, NetworkIgnoreRequest.class), bleKey, bleIv);
            try {
                ThreadPool.getInstance().execute(() -> generateManager(baseUrl).ignoreNetwork(eulixBaseRequest
                        , new IEulixBaseResponseCallback() {
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
                        }));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        } else if (callback != null) {
            callback.onError("request body null");
        }
    }

    public static void ignoreNetwork(@NonNull String baseUrl, String request, EulixBaseResponseExtensionCallback callback) {
        EulixBaseRequest eulixBaseRequest = new EulixBaseRequest();
        eulixBaseRequest.setBody(request);
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).ignoreNetwork(eulixBaseRequest
                    , new IEulixBaseResponseCallback() {
                        @Override
                        public void onResult(EulixBaseResponse response) {
                            int code = 500;
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
                    }));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }
}
