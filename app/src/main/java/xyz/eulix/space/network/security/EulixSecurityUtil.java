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

package xyz.eulix.space.network.security;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.network.EulixBaseResponse;
import xyz.eulix.space.network.EulixBaseResponseExtensionCallback;
import xyz.eulix.space.network.IEulixBaseResponseCallback;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.ThreadPool;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/7/8 16:09
 */
public class EulixSecurityUtil {
    private static final String TAG = EulixSecurityUtil.class.getSimpleName();
    private static final String API_VERSION = ConstantField.BoxVersionName.VERSION_0_2_5;

    public static void getDeviceHardwareInfo(String boxDomain, String accessToken, String secret, String ivParams, DeviceHardwareInfoCallback callback) {
        getDeviceHardwareInfo(boxDomain, accessToken, secret, ivParams, false, callback);
    }

    public static void getDeviceHardwareInfo(String boxDomain, String accessToken, String secret, String ivParams, boolean isFore, DeviceHardwareInfoCallback callback) {
        try {
            ThreadPool.getInstance().execute(() -> EulixSecurityManager.getDeviceHardwareInfo(boxDomain, accessToken, secret, ivParams, API_VERSION, new IDeviceHardwareInfoCallback() {
                @Override
                public void onResult(DeviceHardwareInfoResponse response) {
                    if (response != null) {
                        String code = response.getCode();
                        if (callback != null) {
                            callback.onSuccess(DataUtil.stringCodeGetSource(code), DataUtil.stringCodeToInt(code), response.getMessage(), response.getRequestId(), response.getResults());
                        }
                    } else if (callback != null) {
                        callback.onFailed(null, -1, null, null);
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

    // server exception handle
    public static void binderModifySecurityPassword(@NonNull String oldPassword, String newPassword, String boxDomain, String accessToken, String secret, String ivParams, EulixBaseResponseExtensionCallback callback) {
        BinderModifySecurityPasswordRequest binderModifySecurityPasswordRequest = new BinderModifySecurityPasswordRequest();
        binderModifySecurityPasswordRequest.setOldPassword(oldPassword);
        binderModifySecurityPasswordRequest.setNewPassword(newPassword);
        try {
            ThreadPool.getInstance().execute(() -> EulixSecurityManager.binderModifySecurityPassword(binderModifySecurityPasswordRequest
                    , boxDomain, accessToken, secret, ivParams, API_VERSION, new IEulixBaseResponseCallback() {
                @Override
                public void onResult(EulixBaseResponse response) {
                    if (response == null) {
                        if (callback != null) {
                            callback.onFailed();
                        }
                    } else if (callback != null) {
                        String code = response.getCode();
                        callback.onSuccess(DataUtil.stringCodeGetSource(code), DataUtil.stringCodeToInt(code), response.getMessage(), response.getRequestId());
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


    // server exception handle
    public static void verifySecurityPassword(@NonNull String oldPassword, String boxDomain, String accessToken, String secret, String ivParams, VerifySecurityCallback callback) {
        VerifySecurityPasswordRequest verifySecurityPasswordRequest = new VerifySecurityPasswordRequest();
        verifySecurityPasswordRequest.setOldPassword(oldPassword);
        try {
            ThreadPool.getInstance().execute(() -> EulixSecurityManager.verifySecurityPassword(verifySecurityPasswordRequest
                    , boxDomain, accessToken, secret, ivParams, API_VERSION, new IVerifySecurityCallback() {
                        @Override
                        public void onResult(VerifySecurityResponse response) {
                            if (response != null) {
                                String code = response.getCode();
                                if (callback != null) {
                                    callback.onSuccess(DataUtil.stringCodeGetSource(code), DataUtil.stringCodeToInt(code), response.getMessage(), response.getRequestId(), response.getResults());
                                }
                            } else if (callback != null) {
                                callback.onFailed(null, -1, null, null);
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

    // server exception handle
    public static void binderResetSecurityPassword(@NonNull String securityToken, @NonNull String newPassword, String boxDomain, String accessToken, String secret, String ivParams, EulixBaseResponseExtensionCallback callback) {
        BinderResetSecurityPasswordRequest binderResetSecurityPasswordRequest = new BinderResetSecurityPasswordRequest();
        binderResetSecurityPasswordRequest.setSecurityToken(securityToken);
        binderResetSecurityPasswordRequest.setNewPassword(newPassword);
        try {
            ThreadPool.getInstance().execute(() -> {
                EulixSecurityManager.binderResetSecurityPassword(binderResetSecurityPasswordRequest, boxDomain, accessToken, secret, ivParams, API_VERSION, new IEulixBaseResponseCallback() {
                    @Override
                    public void onResult(EulixBaseResponse response) {
                        if (response == null) {
                            if (callback != null) {
                                callback.onFailed();
                            }
                        } else if (callback != null) {
                            String code = response.getCode();
                            callback.onSuccess(DataUtil.stringCodeGetSource(code), DataUtil.stringCodeToInt(code), response.getMessage(), response.getRequestId());
                        }
                    }

                    @Override
                    public void onError(String errMsg) {
                        if (callback != null) {
                            callback.onError(errMsg);
                        }
                    }
                });
            }, true);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    // server exception handle
    public static void granteeApplySetSecurityPassword(boolean isReset, @NonNull String deviceInfo, String applyId, String boxDomain, String accessToken, String secret, String ivParams, EulixBaseResponseExtensionCallback callback) {
        GranteeApplySetSecurityPasswordRequest granteeApplySetSecurityPasswordRequest = new GranteeApplySetSecurityPasswordRequest();
        granteeApplySetSecurityPasswordRequest.setDeviceInfo(deviceInfo);
        granteeApplySetSecurityPasswordRequest.setApplyId(applyId);
        try {
            ThreadPool.getInstance().execute(() -> {
                IEulixBaseResponseCallback iEulixBaseResponseCallback = new IEulixBaseResponseCallback() {
                    @Override
                    public void onResult(EulixBaseResponse response) {
                        if (response == null) {
                            if (callback != null) {
                                callback.onFailed();
                            }
                        } else if (callback != null) {
                            String code = response.getCode();
                            callback.onSuccess(DataUtil.stringCodeGetSource(code), DataUtil.stringCodeToInt(code), response.getMessage(), response.getRequestId());
                        }
                    }

                    @Override
                    public void onError(String errMsg) {
                        if (callback != null) {
                            callback.onError(errMsg);
                        }
                    }
                };
                if (isReset) {
                    EulixSecurityManager.granteeApplyResetSecurityPassword(granteeApplySetSecurityPasswordRequest
                            , boxDomain, accessToken, secret, ivParams, API_VERSION, iEulixBaseResponseCallback);
                } else {
                    EulixSecurityManager.granteeApplyModifySecurityPassword(granteeApplySetSecurityPasswordRequest
                            , boxDomain, accessToken, secret, ivParams, API_VERSION, iEulixBaseResponseCallback);
                }
            }, true);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    // server exception handle
    public static void binderAcceptSetSecurityPassword(boolean isReset, @NonNull String securityToken, @NonNull String clientUuid, boolean accept, String applyId, String boxDomain, String accessToken, String secret, String ivParams, EulixBaseResponseExtensionCallback callback) {
        SecurityTokenAcceptRequest securityTokenAcceptRequest = new SecurityTokenAcceptRequest();
        securityTokenAcceptRequest.setSecurityToken(securityToken);
        securityTokenAcceptRequest.setClientUuid(clientUuid);
        securityTokenAcceptRequest.setAccept(accept);
        securityTokenAcceptRequest.setApplyId(applyId);
        try {
            ThreadPool.getInstance().execute(() -> {
                IEulixBaseResponseCallback iEulixBaseResponseCallback = new IEulixBaseResponseCallback() {
                    @Override
                    public void onResult(EulixBaseResponse response) {
                        if (response == null) {
                            if (callback != null) {
                                callback.onFailed();
                            }
                        } else if (callback != null) {
                            String code = response.getCode();
                            callback.onSuccess(DataUtil.stringCodeGetSource(code), DataUtil.stringCodeToInt(code), response.getMessage(), response.getRequestId());
                        }
                    }

                    @Override
                    public void onError(String errMsg) {
                        if (callback != null) {
                            callback.onError(errMsg);
                        }
                    }
                };
                if (isReset) {
                    EulixSecurityManager.binderAcceptResetSecurityPassword(securityTokenAcceptRequest
                            , boxDomain, accessToken, secret, ivParams, API_VERSION, iEulixBaseResponseCallback);
                } else {
                    EulixSecurityManager.binderAcceptModifySecurityPassword(securityTokenAcceptRequest
                            , boxDomain, accessToken, secret, ivParams, API_VERSION, iEulixBaseResponseCallback);
                }

            }, true);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    // server exception handle
    public static void granteeModifySecurityPassword(@NonNull String securityToken, @NonNull String clientUuid, @NonNull String oldPassword, String newPassword, String boxDomain, String accessToken, String secret, String ivParams, EulixBaseResponseExtensionCallback callback) {
        GranteeModifySecurityPasswordRequest granteeModifySecurityPasswordRequest = new GranteeModifySecurityPasswordRequest();
        granteeModifySecurityPasswordRequest.setSecurityToken(securityToken);
        granteeModifySecurityPasswordRequest.setClientUuid(clientUuid);
        granteeModifySecurityPasswordRequest.setOldPassword(oldPassword);
        granteeModifySecurityPasswordRequest.setNewPassword(newPassword);
        try {
            ThreadPool.getInstance().execute(() -> EulixSecurityManager.granteeModifySecurityPassword(granteeModifySecurityPasswordRequest
                    , boxDomain, accessToken, secret, ivParams, API_VERSION, new IEulixBaseResponseCallback() {
                        @Override
                        public void onResult(EulixBaseResponse response) {
                            if (response == null) {
                                if (callback != null) {
                                    callback.onFailed();
                                }
                            } else if (callback != null) {
                                String code = response.getCode();
                                callback.onSuccess(DataUtil.stringCodeGetSource(code), DataUtil.stringCodeToInt(code), response.getMessage(), response.getRequestId());
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

    // server exception handle
    public static void granteeResetSecurityPassword(@NonNull String acceptSecurityToken, @NonNull String emailSecurityToken, @NonNull String clientUuid, @NonNull String newPassword, String boxDomain, String accessToken, String secret, String ivParams, EulixBaseResponseExtensionCallback callback) {
        GranteeResetSecurityPasswordRequest granteeResetSecurityPasswordRequest = new GranteeResetSecurityPasswordRequest();
        granteeResetSecurityPasswordRequest.setAcceptSecurityToken(acceptSecurityToken);
        granteeResetSecurityPasswordRequest.setEmailSecurityToken(emailSecurityToken);
        granteeResetSecurityPasswordRequest.setClientUuid(clientUuid);
        granteeResetSecurityPasswordRequest.setNewPassword(newPassword);
        try {
            ThreadPool.getInstance().execute(() -> {
                EulixSecurityManager.granteeResetSecurityPassword(granteeResetSecurityPasswordRequest, boxDomain, accessToken, secret, ivParams, API_VERSION, new IEulixBaseResponseCallback() {
                    @Override
                    public void onResult(EulixBaseResponse response) {
                        if (response == null) {
                            if (callback != null) {
                                callback.onFailed();
                            }
                        } else if (callback != null) {
                            String code = response.getCode();
                            callback.onSuccess(DataUtil.stringCodeGetSource(code), DataUtil.stringCodeToInt(code), response.getMessage(), response.getRequestId());
                        }
                    }

                    @Override
                    public void onError(String errMsg) {
                        if (callback != null) {
                            callback.onError(errMsg);
                        }
                    }
                });
            }, true);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void securityMessagePoll(@NonNull String clientUuid, String boxDomain, String accessToken, String secret, String ivParams, SecurityMessagePollCallback callback) {
        SecurityMessagePollRequest securityMessagePollRequest = new SecurityMessagePollRequest();
        securityMessagePollRequest.setClientUuid(clientUuid);
        try {
            ThreadPool.getInstance().execute(() -> EulixSecurityManager.securityMessagePoll(securityMessagePollRequest, boxDomain, accessToken, secret, ivParams, API_VERSION, new ISecurityMessagePollCallback() {
                @Override
                public void onResult(SecurityMessagePollResponse response) {
                    if (response == null) {
                        if (callback != null) {
                            callback.onFailed();
                        }
                    } else if (callback != null) {
                        String code = response.getCode();
                        callback.onSuccess(DataUtil.stringCodeGetSource(code), DataUtil.stringCodeToInt(code), response.getMessage(), response.getRequestId(), response.getResults());
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
}
