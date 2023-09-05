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

package xyz.eulix.space.network.agent.disk;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.network.EulixBaseRequest;
import xyz.eulix.space.network.EulixBaseResponse;
import xyz.eulix.space.network.EulixBaseResponseExtensionCallback;
import xyz.eulix.space.network.IEulixBaseResponseCallback;
import xyz.eulix.space.network.agent.AgentBaseResponse;
import xyz.eulix.space.network.agent.AgentEncryptedResultsCallback;
import xyz.eulix.space.network.agent.IAgentBaseResponseCallback;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.ThreadPool;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/11/2 9:11
 */
public class DiskUtil {
    private static final String TAG = DiskUtil.class.getSimpleName();
    private static Map<String, DiskManager> managerMap = new HashMap<>();

    private DiskUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    private static DiskManager generateManager(String baseUrl) {
        DiskManager diskManager = null;
        if (managerMap.containsKey(baseUrl)) {
            diskManager = managerMap.get(baseUrl);
        }
        if (diskManager == null) {
            diskManager = new DiskManager(baseUrl);
            managerMap.put(baseUrl, diskManager);
        }
        return diskManager;
    }

    public static void getSpaceReadyCheck(@NonNull String baseUrl, String bleKey, String bleIv, ReadyCheckCallback callback) {
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).getSpaceReadyCheck(new IAgentBaseResponseCallback() {
                @Override
                public void onResponse(AgentBaseResponse response) {
                    int code = -1;
                    String source = null;
                    String message = null;
                    String requestId = null;
                    ReadyCheckResult result = null;
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
                        if (decryptedContent != null) {
                            try {
                                result = new Gson().fromJson(decryptedContent, ReadyCheckResult.class);
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

    public static void getSpaceReadyCheck(@NonNull String baseUrl, AgentEncryptedResultsCallback callback) {
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).getSpaceReadyCheck(new IAgentBaseResponseCallback() {
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

    public static void getDiskRecognition(@NonNull String baseUrl, String bleKey, String bleIv, DiskRecognitionCallback callback) {
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).getDiskRecognition(new IAgentBaseResponseCallback() {
                @Override
                public void onResponse(AgentBaseResponse response) {
                    int code = -1;
                    String source = null;
                    String message = null;
                    String requestId = null;
                    DiskRecognitionResult result = null;
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
                        if (decryptedContent != null) {
                            try {
                                result = new Gson().fromJson(decryptedContent, DiskRecognitionResult.class);
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

    public static void getDiskRecognition(@NonNull String baseUrl, AgentEncryptedResultsCallback callback) {
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).getDiskRecognition(new IAgentBaseResponseCallback() {
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

    public static void diskInitialize(@NonNull String baseUrl, String bleKey, String bleIv, boolean isDiskEncrypt, boolean isRaid, List<String> primaryStorageHardwareIds, List<String> secondaryStorageHardwareIds, List<String> raidDiskHardwareIds, EulixBaseResponseExtensionCallback callback) {
        DiskInitializeRequest diskInitializeRequest = new DiskInitializeRequest();
        diskInitializeRequest.setDiskEncrypt(isDiskEncrypt ? 1 : 2);
        diskInitializeRequest.setRaidType(isRaid ? 2 : 1);
        diskInitializeRequest.setPrimaryStorageHwIds(primaryStorageHardwareIds);
        diskInitializeRequest.setSecondaryStorageHwIds(secondaryStorageHardwareIds);
        diskInitializeRequest.setRaidDiskHwIds(raidDiskHardwareIds);
        String request = new Gson().toJson(diskInitializeRequest, DiskInitializeRequest.class);
        if (bleKey != null && bleIv != null) {
            request = EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, request, bleKey, StandardCharsets.UTF_8, bleIv);
        }
        EulixBaseRequest eulixBaseRequest = new EulixBaseRequest();
        eulixBaseRequest.setBody(request);
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).diskInitialize(eulixBaseRequest
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
    }

    public static void diskInitialize(@NonNull String baseUrl, String request, EulixBaseResponseExtensionCallback callback) {
        EulixBaseRequest eulixBaseRequest = new EulixBaseRequest();
        eulixBaseRequest.setBody(request);
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).diskInitialize(eulixBaseRequest
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

    public static void getDiskInitializeProgress(@NonNull String baseUrl, String bleKey, String bleIv, DiskInitializeProgressCallback callback) {
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).getDiskInitializeProgress(new IAgentBaseResponseCallback() {
                @Override
                public void onResponse(AgentBaseResponse response) {
                    int code = -1;
                    String source = null;
                    String message = null;
                    String requestId = null;
                    DiskInitializeProgressResult result = null;
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
                        if (decryptedContent != null) {
                            try {
                                result = new Gson().fromJson(decryptedContent, DiskInitializeProgressResult.class);
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

    public static void getDiskInitializeProgress(@NonNull String baseUrl, AgentEncryptedResultsCallback callback) {
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).getDiskInitializeProgress(new IAgentBaseResponseCallback() {
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

    public static void getDiskManagementList(@NonNull String baseUrl, String bleKey, String bleIv, DiskManagementListCallback callback) {
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).getDiskManagementList(new IAgentBaseResponseCallback() {
                @Override
                public void onResponse(AgentBaseResponse response) {
                    int code = -1;
                    String source = null;
                    String message = null;
                    String requestId = null;
                    DiskManageListResult result = null;
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
                        if (decryptedContent != null) {
                            try {
                                result = new Gson().fromJson(decryptedContent, DiskManageListResult.class);
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

    public static void getDiskManagementList(@NonNull String baseUrl, AgentEncryptedResultsCallback callback) {
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).getDiskManagementList(new IAgentBaseResponseCallback() {
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

    public static void eulixSystemShutdown(@NonNull String baseUrl, EulixBaseResponseExtensionCallback callback) {
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).eulixSystemShutdown(new IEulixBaseResponseCallback() {
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
    }

    public static void eulixSystemReboot(@NonNull String baseUrl, EulixBaseResponseExtensionCallback callback) {
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).eulixSystemReboot(new IEulixBaseResponseCallback() {
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
    }
}
