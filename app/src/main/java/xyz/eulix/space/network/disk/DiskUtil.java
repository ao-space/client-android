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

package xyz.eulix.space.network.disk;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.network.EulixBaseResponse;
import xyz.eulix.space.network.EulixBaseResponseExtensionCallback;
import xyz.eulix.space.network.IEulixBaseResponseCallback;
import xyz.eulix.space.network.agent.disk.DiskManageListResult;
import xyz.eulix.space.network.agent.disk.DiskManagementListCallback;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.ThreadPool;
import xyz.eulix.space.util.Urls;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/11/8 15:41
 */
public class DiskUtil {
    private static final String API_VERSION = ConstantField.BoxVersionName.VERSION_0_2_5;

    public static void getDiskManagementList(String accessToken, String secret, String ivParams, DiskManagementListCallback callback) {
        getDiskManagementList(accessToken, secret, ivParams, false, callback);
    }

    // server exception handle
    public static void getDiskManagementList(String accessToken, String secret, String ivParams, boolean isFore, DiskManagementListCallback callback) {
        String finalBoxDomain = Urls.getBaseUrl();
        try {
            ThreadPool.getInstance().execute(() -> DiskManager.getDiskManagementList(finalBoxDomain
                    , accessToken, secret, ivParams, API_VERSION, new IDiskManageListCallback() {
                @Override
                public void onResponse(DiskManageListResponse response) {
                    DiskManageListResult diskManageListResult = null;
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
                        diskManageListResult = response.getResults();
                    }
                    if (diskManageListResult == null) {
                        if (callback != null) {
                            callback.onFail(code, source, message, requestId);
                        }
                    } else if (callback != null) {
                        callback.onSuccess(code, source, message, requestId, diskManageListResult);
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

    public static void eulixSystemShutdown(String accessToken, String secret, String ivParams, EulixBaseResponseExtensionCallback callback) {
        String finalBoxDomain = Urls.getBaseUrl();
        try {
            ThreadPool.getInstance().execute(() -> DiskManager.eulixSystemShutdown(finalBoxDomain
                    , accessToken, secret, ivParams, API_VERSION, new IEulixBaseResponseCallback() {
                @Override
                public void onResult(EulixBaseResponse response) {
                    if (response == null) {
                        if (callback != null) {
                            callback.onFailed();
                        }
                    } else {
                        String codeValue = response.getCode();
                        if (callback != null) {
                            callback.onSuccess(DataUtil.stringCodeGetSource(codeValue), DataUtil.stringCodeToInt(codeValue)
                                    , response.getMessage(), response.getRequestId());
                        }
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

    public static void getDiskManagementRaidInfo(String accessToken, String secret, String ivParams, RaidInfoCallback callback) {
        String finalBoxDomain = Urls.getBaseUrl();
        try {
            ThreadPool.getInstance().execute(() -> DiskManager.getDiskManagementRaidInfo(finalBoxDomain
                    , accessToken, secret, ivParams, API_VERSION, new IRaidInfoCallback() {
                        @Override
                        public void onResponse(RaidInfoResponse response) {
                            RaidInfoResult raidInfoResult = null;
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
                                raidInfoResult = response.getResults();
                            }
                            if (raidInfoResult == null) {
                                if (callback != null) {
                                    callback.onFail(code, source, message, requestId);
                                }
                            } else if (callback != null) {
                                callback.onSuccess(code, source, message, requestId, raidInfoResult);
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

    // server exception handle
    public static void diskExpand(List<String> storageHardwareIds, boolean isRaid, List<String> raidDiskHardwareIds, String accessToken, String secret, String ivParams, EulixBaseResponseExtensionCallback callback) {
        DiskExpandRequest diskExpandRequest = new DiskExpandRequest();
        diskExpandRequest.setSecondaryStorageHwIds(storageHardwareIds);
        diskExpandRequest.setRaidType(isRaid ? 2 : 1);
        diskExpandRequest.setRaidDiskHwIds(raidDiskHardwareIds);
        String finalBoxDomain = Urls.getBaseUrl();
        try {
            ThreadPool.getInstance().execute(() -> DiskManager.diskExpand(diskExpandRequest, finalBoxDomain
                    , accessToken, secret, ivParams, API_VERSION, new IEulixBaseResponseCallback() {
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

    public static void getDiskExpandProgress(String accessToken, String secret, String ivParams, DiskExpandProgressCallback callback) {
        getDiskExpandProgress(accessToken, secret, ivParams, false, callback);
    }

    // server exception handle
    public static void getDiskExpandProgress(String accessToken, String secret, String ivParams, boolean isFore, DiskExpandProgressCallback callback) {
        String finalBoxDomain = Urls.getBaseUrl();
        try {
            ThreadPool.getInstance().execute(() -> DiskManager.getDiskExpandProgress(finalBoxDomain
                    , accessToken, secret, ivParams, API_VERSION, new IDiskExpandProgressCallback() {
                @Override
                public void onResponse(DiskExpandProgressResponse response) {
                    DiskExpandProgressResult diskExpandProgressResult = null;
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
                        diskExpandProgressResult = response.getResults();
                    }
                    if (diskExpandProgressResult == null) {
                        if (callback != null) {
                            callback.onFail(code, source, message, requestId);
                        }
                    } else if (callback != null) {
                        callback.onSuccess(code, source, message, requestId, diskExpandProgressResult);
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
