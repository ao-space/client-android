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

package xyz.eulix.space.network.gateway;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.BuildConfig;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.interfaces.ResultCallback;
import xyz.eulix.space.util.BaseParamsUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.DebugUtil;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.GatewayUtils;
import xyz.eulix.space.util.ImportantThreadPool;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.ThreadPool;
import xyz.eulix.space.util.Urls;

/**
 * @author: chenjiawei
 * date: 2021/7/9 14:48
 */
public class GatewayUtil {
    private static final String TAG = GatewayUtil.class.getSimpleName();
    private static Map<String, GatewayManager> bindManagerMap = new HashMap<>();
    private static Map<String, GatewayManager> loginManagerMap = new HashMap<>();

    private GatewayUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    private static boolean compatibleBoolean(Boolean value) {
        boolean result = false;
        if (value != null) {
            result = value;
        }
        return result;
    }

    private static GatewayManager generateBindManager(String boxDomain, String boxPublicKey) {
        GatewayManager gatewayManager = null;
        if (bindManagerMap.containsKey(boxDomain)) {
            gatewayManager = bindManagerMap.get(boxDomain);
        }
        if (gatewayManager == null) {
            gatewayManager = new GatewayManager(boxDomain, boxPublicKey);
            bindManagerMap.put(boxDomain, gatewayManager);
        }
        return gatewayManager;
    }

    private static GatewayManager generateLoginManager(String boxDomain) {
        GatewayManager gatewayManager = null;
        if (loginManagerMap.containsKey(boxDomain)) {
            gatewayManager = loginManagerMap.get(boxDomain);
        }
        if (gatewayManager == null) {
            gatewayManager = new GatewayManager(boxDomain);
            loginManagerMap.put(boxDomain, gatewayManager);
        }
        return gatewayManager;
    }

    public static void getSpaceStatus(@NonNull Context context, String baseUrl, String boxUuid, String boxBind, boolean isLAN, SpaceStatusCallback callback) {
        String domainUrl = baseUrl;
        if (isLAN) {
            String ipAddressUrl = EulixSpaceDBUtil.getIpAddressUrl(context, boxUuid, true);
            if (ipAddressUrl != null) {
                baseUrl = ipAddressUrl;
            } else {
                isLAN = false;
            }
        }
        if (!TextUtils.isEmpty(baseUrl)) {
            String finalBaseUrl = baseUrl;
            boolean finalIsLAN = isLAN;
            try {
                ThreadPool.getInstance().execute(() -> generateLoginManager(finalBaseUrl).getSpaceStatus(new ISpaceStatusCallback() {
                    @Override
                    public void onResult(SpaceStatusResult result) {
                        Logger.i(TAG, "on result: " + result);
                        if (result == null) {
                            if (finalIsLAN) {
                                getSpaceStatus(context, domainUrl, boxUuid, boxBind, false, callback);
                            } else if (callback != null) {
                                callback.onFailed(boxUuid, boxBind);
                            }
                        } else {
                            if (finalIsLAN) {
                                EulixSpaceDBUtil.setLANEnable(context, boxUuid, true);
                            }
                            if (callback != null) {
                                callback.onSuccess(boxUuid, boxBind, result.getStatus(), result.getVersion());
                            }
                        }
                    }

                    @Override
                    public void onError(String errMsg) {
                        Logger.e(TAG, "on error: " + errMsg);
                        if (finalIsLAN) {
                            EulixSpaceDBUtil.setLANEnable(context, boxUuid, false);
                            getSpaceStatus(context, domainUrl, boxUuid, boxBind, false, callback);
                        } else if (callback != null) {
                            callback.onError(boxUuid, boxBind, errMsg);
                        }
                    }
                }));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public static void getSpaceStatus(@NonNull Context context, String baseUrl, String boxUuid, String boxBind, boolean isLAN, String savedJson, SpaceStatusExtensionCallback callback) {
        String domainUrl = baseUrl;
        if (isLAN) {
            EulixBoxBaseInfo eulixBoxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(context, true);
            String activeBoxUuid = null;
            String activeBoxBind = null;
            if (eulixBoxBaseInfo != null) {
                activeBoxUuid = eulixBoxBaseInfo.getBoxUuid();
                activeBoxBind = eulixBoxBaseInfo.getBoxBind();
            }
            if (activeBoxUuid != null && activeBoxUuid.equals(boxUuid) && activeBoxBind != null && activeBoxBind.equals(boxBind)) {
                baseUrl = Urls.getIPBaseUrl();
                if (baseUrl == null || baseUrl.isEmpty()) {
                    isLAN = false;
                }
            } else {
                isLAN = false;
            }
            if (!isLAN && savedJson != null) {
                boolean isError = true;
                SavedSpaceStatus savedSpaceStatus = null;
                try {
                    savedSpaceStatus = new Gson().fromJson(savedJson, SavedSpaceStatus.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
                if (savedSpaceStatus != null) {
                    isError = savedSpaceStatus.isError();
                }
                if (isError) {
                    if (callback != null) {
                        callback.onError(true, (savedSpaceStatus == null ? 500 : savedSpaceStatus.getCode())
                                , boxUuid, boxBind, (savedSpaceStatus == null ? null : savedSpaceStatus.getErrMsg()));
                    }
                } else {
                    if (callback != null) {
                        callback.onFailed(true, savedSpaceStatus.getCode()
                                , savedSpaceStatus.getLocationHost(), boxUuid, boxBind);
                    }
                }
                return;
            }
        }
        if (!TextUtils.isEmpty(baseUrl)) {
            String finalBaseUrl = baseUrl;
            boolean finalIsLAN = isLAN;
            String requestId = UUID.randomUUID().toString();
            Logger.d(TAG, "space status request, uuid: " + boxUuid + ", bind: " + boxBind + ", request: " + requestId + ", url: " + finalBaseUrl);
            // todo 先防止局域网项目冲突，只改此项
            try {
                ThreadPool.getInstance().execute(() -> generateLoginManager(finalBaseUrl).getSpaceStatus(requestId, new ISpaceStatusExtensionCallback() {
                    @Override
                    public void onResult(int code, String message, String requestId, String locationHost, SpaceStatusResult result) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            if (!finalIsLAN) {
                                EulixSpaceDBUtil.updateSpaceStatusResponseLineInfo(context, boxUuid, boxBind, code, message);
                            }
                            Logger.i(TAG, "space status on result: " + result + ", uuid: " + boxUuid + ", bind: " + boxBind + ", request: " + requestId);
                            if (result == null) {
                                if (!finalIsLAN && !((code >= 300 && code < 400) || code == 461)) {
                                    getSpaceStatus(context, domainUrl, boxUuid, boxBind, true, new Gson()
                                            .toJson(new SavedSpaceStatus(code, message, locationHost, result)
                                                    , SavedSpaceStatus.class), callback);
                                } else if (callback != null) {
                                    callback.onFailed(finalIsLAN, code, locationHost, boxUuid, boxBind);
                                }
                            } else {
                                if (finalIsLAN) {
                                    EulixSpaceDBUtil.setLANEnable(context, boxUuid, true);
                                }
                                if (callback != null) {
                                    callback.onSuccess(finalIsLAN, code, locationHost, boxUuid, boxBind, result);
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(int code, String errMsg, String requestId) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Logger.e(TAG, "space status on error: " + errMsg + ", uuid: " + boxUuid + ", bind: " + boxBind + ", request: " + requestId);
                            if (!finalIsLAN) {
                                getSpaceStatus(context, domainUrl, boxUuid, boxBind, true, new Gson()
                                        .toJson(new SavedSpaceStatus(code, errMsg), SavedSpaceStatus.class), callback);
                            } else {
                                EulixSpaceDBUtil.setLANEnable(context, boxUuid, false);
                                if (callback != null) {
                                    callback.onError(true, code, boxUuid, boxBind, errMsg);
                                }
                            }
                        });
                    }
                }), true);
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public static void getSpacePoll(@NonNull Context context, String baseUrl, String boxUuid, String boxBind, String accessToken, boolean isLAN, SpacePollCallback callback) {
        String domainUrl = baseUrl;
        if (isLAN) {
            String ipAddressUrl = EulixSpaceDBUtil.getIpAddressUrl(context, boxUuid, true);
            if (ipAddressUrl != null) {
                baseUrl = ipAddressUrl;
            } else {
                isLAN = false;
            }
        }
        if (!TextUtils.isEmpty(baseUrl)) {
            String finalBaseUrl = baseUrl;
            boolean finalIsLAN = isLAN;
            try {
                ThreadPool.getInstance().execute(() -> generateLoginManager(finalBaseUrl).getSpacePoll(UUID.randomUUID().toString()
                        , accessToken, new ISpacePollCallback() {
                            @Override
                            public void onResult(SpacePollResult result) {
                                Logger.i(TAG, "on result: " + result);
                                if (result == null) {
                                    if (finalIsLAN) {
                                        getSpacePoll(context, domainUrl, boxUuid, boxBind, accessToken, false, callback);
                                    } else if (callback != null) {
                                        callback.onFailed();
                                    }
                                } else {
                                    if (finalIsLAN) {
                                        EulixSpaceDBUtil.setLANEnable(context, boxUuid, true);
                                    }
                                    if (callback != null) {
                                        callback.onSuccess(result.getStatus(), result.getVersion(), result.getMessage());
                                    }
                                }
                            }

                            @Override
                            public void onError(String errMsg) {
                                Logger.e(TAG, "on error: " + errMsg);
                                if (finalIsLAN) {
                                    EulixSpaceDBUtil.setLANEnable(context, boxUuid, false);
                                    getSpacePoll(context, domainUrl, boxUuid, boxBind, accessToken, false, callback);
                                } else if (callback != null) {
                                    callback.onError(errMsg);
                                }
                            }
                        }));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 管理员获取token，需要使用domain
     *
     * @param context
     * @param boxUUID
     * @param boxBindValue
     * @param boxPublicKey
     * @param boxDomain
     * @param authKey
     * @param clientUUID
     * @param isLAN
     * @param callback
     */
    public static void createAuthToken(@NonNull Context context, String boxUUID, String boxBindValue, String boxPublicKey, String boxDomain, String authKey, String clientUUID, boolean isLAN, boolean isFore, CreateAuthTokenCallback callback) {
        CreateTokenInfo createTokenInfo = new CreateTokenInfo();

        createTokenInfo.setEncryptedAuthKey(authKey);
        createTokenInfo.setEncryptedClientUUID(clientUUID);
        String domainUrl = boxDomain;

        //判断是否为当前盒子
        EulixBoxBaseInfo currentBoxInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(context);
        if (currentBoxInfo != null && boxUUID.equals(currentBoxInfo.getBoxUuid()) && boxBindValue.equals(currentBoxInfo.getBoxBind())) {
            if (!TextUtils.isEmpty(Urls.getIPBaseUrl())) {
                domainUrl = Urls.getIPBaseUrl();
            }
        }

        String finalBoxDomain = domainUrl;
        String finalBoxPublicKey = boxPublicKey;
        try {
            ThreadPool.getInstance().execute(() -> generateBindManager(finalBoxDomain, finalBoxPublicKey).createAuthToken(UUID.randomUUID().toString(), createTokenInfo, new ICreateAuthTokenCallback() {
                @Override
                public void onResult(CreateTokenResult result) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Logger.i(TAG, "on result: " + result);
                        if (result == null || result.getAlgorithmConfig() == null || result.getAlgorithmConfig().getTransportation() == null) {
                            if (callback != null) {
                                callback.onFailed();
                            }
                        } else {
                            if (callback != null) {
                                callback.onSuccess(boxUUID, boxBindValue, result.getAccessToken(), result.getAlgorithmConfig().getTransportation().getTransformation()
                                        , result.getAlgorithmConfig().getTransportation().getInitializationVector()
                                        , result.getEncryptedSecret(), result.getExpiresAt(), result.getExpiresAtEpochSeconds()
                                        , result.getRefreshToken(), result.getRequestId());
                            }
                        }
                    });
                }

                @Override
                public void onError(int code, String msg) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Logger.e(TAG, "on error: " + msg);
                        if (callback != null) {
                            callback.onError(code, msg);
                        }
                    });
                }
            }), isFore);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }

    }

    public static void refreshAuthToken(@NonNull Context context, String boxUUID, String boxBindValue, String boxDomain, String refreshToken, boolean isLAN, CreateAuthTokenCallback callback) {
        RefreshTokenInfo refreshTokenInfo = new RefreshTokenInfo();
        refreshTokenInfo.setRefreshToken(refreshToken);
        String domainUrl = boxDomain;
        if (isLAN) {
            String ipAddressUrl = EulixSpaceDBUtil.getIpAddressUrl(context, boxUUID, false);
            if (ipAddressUrl != null) {
                boxDomain = ipAddressUrl;
            } else {
                isLAN = false;
            }
        }
        String finalBoxDomain = boxDomain;
        boolean finalIsLAN = isLAN;
        try {
            ThreadPool.getInstance().execute(() -> generateLoginManager(finalBoxDomain).refreshAuthToken(UUID.randomUUID().toString(), refreshTokenInfo, new ICreateAuthTokenCallback() {
                @Override
                public void onResult(CreateTokenResult result) {
                    Logger.i(TAG, "on result: " + result);
                    if (result == null || result.getAlgorithmConfig() == null || result.getAlgorithmConfig().getTransportation() == null) {
                        if (finalIsLAN) {
                            refreshAuthToken(context, boxUUID, boxBindValue, domainUrl, refreshToken, false, callback);
                        } else if (callback != null) {
                            callback.onFailed();
                        }
                    } else {
                        if (callback != null) {
                            callback.onSuccess(boxUUID, boxBindValue, result.getAccessToken(), result.getAlgorithmConfig().getTransportation().getTransformation()
                                    , result.getAlgorithmConfig().getTransportation().getInitializationVector()
                                    , result.getEncryptedSecret(), result.getExpiresAt(), result.getExpiresAtEpochSeconds()
                                    , result.getRefreshToken(), result.getRequestId());
                        }
                    }
                }

                @Override
                public void onError(int code, String msg) {
                    Logger.e(TAG, "on error: " + msg);
                    if (finalIsLAN) {
                        refreshAuthToken(context, boxUUID, boxBindValue, domainUrl, refreshToken, false, callback);
                    } else if (callback != null) {
                        callback.onError(code, msg);
                    }
                }
            }));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * 扫码登录获取token，需要使用domain
     *
     * @param context
     * @param boxUUID
     * @param boxBindValue
     * @param boxDomain
     * @param boxPublicKey
     * @param refreshToken
     * @param isLAN
     * @param callback
     */
    public static void refreshLoginAuthToken(@NonNull Context context, String boxUUID, String boxBindValue, String boxDomain, String boxPublicKey, String refreshToken, boolean isLAN, boolean isFore, CreateAuthTokenCallback callback) {
        String secretKey = DataUtil.getUID(UUID.randomUUID());
        RefreshTokenInfo refreshTokenInfo = new RefreshTokenInfo();

        refreshTokenInfo.setRefreshToken(refreshToken);

        String domainUrl = boxDomain;
        if (isLAN) {
            String ipAddressUrl = EulixSpaceDBUtil.getIpAddressUrl(context, boxUUID, false);
            if (ipAddressUrl != null) {
                boxDomain = ipAddressUrl;
            } else {
                isLAN = false;
            }
        }
        String finalBoxDomain = boxDomain;
        boolean finalIsLAN = isLAN;
        try {
            ThreadPool.getInstance().execute(() -> generateLoginManager(finalBoxDomain).refreshLoginAuthToken(UUID.randomUUID().toString()
                    , EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.RSA_ECB_PKCS1
                            , null, secretKey, boxPublicKey, null, null)
                    , refreshTokenInfo, new ICreateAuthTokenCallback() {
                        @Override
                        public void onResult(CreateTokenResult result) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                Logger.i(TAG, "on result: " + result);
                                if (result == null || result.getAlgorithmConfig() == null || result.getAlgorithmConfig().getTransportation() == null) {
                                    if (finalIsLAN) {
                                        refreshLoginAuthToken(context, boxUUID, boxBindValue, domainUrl, boxPublicKey, refreshToken, false, isFore, callback);
                                    } else if (callback != null) {
                                        callback.onFailed();
                                    }
                                } else {
                                    AlgorithmConfig algorithmConfig = result.getAlgorithmConfig();
                                    TransportationConfig transportationConfig = algorithmConfig.getTransportation();
                                    String transformation = transportationConfig.getTransformation();
                                    String initializationVector = transportationConfig.getInitializationVector();
                                    if (callback != null) {
                                        callback.onSuccess(boxUUID, boxBindValue, result.getAccessToken(), transformation
                                                , initializationVector, EncryptionUtil.decrypt(transformation, null
                                                        , result.getEncryptedSecret(), secretKey, StandardCharsets.UTF_8
                                                        , initializationVector), result.getExpiresAt(), result.getExpiresAtEpochSeconds()
                                                , result.getRefreshToken(), result.getRequestId());
                                    }
                                }
                            });
                        }

                        @Override
                        public void onError(int code, String msg) {
                            new Handler(Looper.getMainLooper()).post(() -> {
                                Logger.e(TAG, "on error: " + msg);
                                if (finalIsLAN) {
                                    refreshLoginAuthToken(context, boxUUID, boxBindValue, domainUrl, boxPublicKey, refreshToken, false, isFore, callback);
                                } else if (callback != null) {
                                    callback.onError(code, msg);
                                }
                            });
                        }
                    }), isFore);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void authAutoLogin(@NonNull Context context, String boxUuid, String boxBind, String boxDomain, String boxPubKey, String refreshToken, boolean isPoll, boolean isLAN, boolean isFore, AuthAutoLoginCallback callback) {
        String secretKey = DataUtil.getUID(UUID.randomUUID());
        AuthAutoLoginRequestBody authAutoLoginRequestBody = new AuthAutoLoginRequestBody();
        authAutoLoginRequestBody.setRefreshToken(refreshToken);
        authAutoLoginRequestBody.setTempEncryptedSecret(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.RSA_ECB_PKCS1
                , null, secretKey, boxPubKey, null, null));
        String domainUrl = boxDomain;
        if (isLAN) {
            String ipAddressUrl = EulixSpaceDBUtil.getIpAddressUrl(context, boxUuid, false);
            if (ipAddressUrl != null) {
                boxDomain = ipAddressUrl;
            } else {
                isLAN = false;
            }
        }
        String finalBoxDomain = boxDomain;
        boolean finalIsLAN = isLAN;
        try {
            ThreadPool.getInstance().execute(() -> generateLoginManager(finalBoxDomain).authAutoLogin(UUID.randomUUID().toString(), authAutoLoginRequestBody, isPoll, new IAuthAutoLoginCallback() {
                @Override
                public void onResult(int httpCode, AuthAutoLoginResponseBody authAutoLoginResponseBody) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Logger.i(TAG, "on result: " + authAutoLoginResponseBody);
                        int code = -1;
                        AlgorithmConfig algorithmConfig = null;
                        CreateTokenResult createTokenResult = null;
                        if (authAutoLoginResponseBody != null) {
                            code = DataUtil.stringCodeToInt(authAutoLoginResponseBody.getCode());
                            createTokenResult = authAutoLoginResponseBody.getResults();
                        }
                        if (createTokenResult == null) {
                            if (finalIsLAN) {
                                authAutoLogin(context, boxUuid, boxBind, domainUrl, boxPubKey, refreshToken, isPoll, false, isFore, callback);
                            } else if (callback != null) {
                                callback.onFailed(boxUuid, boxBind, code, httpCode);
                            }
                        } else {
                            TransportationConfig transportationConfig = null;
                            algorithmConfig = createTokenResult.getAlgorithmConfig();
                            if (algorithmConfig != null) {
                                transportationConfig = algorithmConfig.getTransportation();
                            }
                            if (transportationConfig != null) {
                                if (callback != null) {
                                    callback.onSuccess(boxUuid, boxBind, code, httpCode, createTokenResult.getAccessToken()
                                            , createTokenResult.getRefreshToken(), createTokenResult.getAlgorithmConfig()
                                            , EncryptionUtil.decrypt(transportationConfig.getTransformation()
                                                    , null, createTokenResult.getEncryptedSecret()
                                                    , secretKey, StandardCharsets.UTF_8, transportationConfig.getInitializationVector())
                                            , createTokenResult.getExpiresAt(), createTokenResult.getExpiresAtEpochSeconds()
                                            , createTokenResult.getAutoLoginExpiresAt());
                                }
                            } else if (callback != null) {
                                callback.onFailed(boxUuid, boxBind, code, httpCode);
                            }
                        }
                    });
                }

                @Override
                public void onError(int httpCode, String errMsg) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Logger.e(TAG, "on error: " + errMsg);
                        if (finalIsLAN) {
                            authAutoLogin(context, boxUuid, boxBind, domainUrl, boxPubKey, refreshToken, isPoll, false, isFore, callback);
                        } else if (callback != null) {
                            callback.onError(boxUuid, boxBind, httpCode, errMsg);
                        }
                    });
                }
            }), isFore);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void authAutoLoginConfirm(@NonNull Context context, String boxUuid, String boxDomain, String transformation, String secret, String ivParams, String accessToken, String loginClientUuid, boolean isAutoLogin, boolean isLogin, boolean isLAN, AuthAutoLoginConfirmCallback callback) {
        AuthAutoLoginConfirmRequestBody authAutoLoginConfirmRequestBody = new AuthAutoLoginConfirmRequestBody();
        authAutoLoginConfirmRequestBody.setAccessToken(accessToken);
        authAutoLoginConfirmRequestBody.setEncryptedClientUUID(EncryptionUtil.encrypt(transformation
                , null, loginClientUuid, secret, StandardCharsets.UTF_8, ivParams));
        authAutoLoginConfirmRequestBody.setAutoLogin(isAutoLogin);
        authAutoLoginConfirmRequestBody.setLogin(isLogin);
        String domainUrl = boxDomain;
        if (isLAN) {
            String ipAddressUrl = EulixSpaceDBUtil.getIpAddressUrl(context, boxUuid, false);
            if (ipAddressUrl != null) {
                boxDomain = ipAddressUrl;
            } else {
                isLAN = false;
            }
        }
        String finalBoxDomain = boxDomain;
        boolean finalIsLAN = isLAN;
        String requestId = UUID.randomUUID().toString();
        try {
            ThreadPool.getInstance().execute(() -> generateLoginManager(finalBoxDomain).authAutoLoginConfirm(requestId, authAutoLoginConfirmRequestBody, new IAuthAutoLoginConfirmCallback() {
                @Override
                public void onResult(AuthAutoLoginConfirmResponseBody authAutoLoginConfirmResponseBody) {
                    Logger.d(TAG, "on result: " + (authAutoLoginConfirmResponseBody == null ? "null" : authAutoLoginConfirmResponseBody.toString()));
                    AuthAutoLoginConfirmResult authAutoLoginConfirmResult = null;
                    int code = -1;
                    String message = null;
                    if (authAutoLoginConfirmResponseBody != null) {
                        authAutoLoginConfirmResult = authAutoLoginConfirmResponseBody.getResults();
                        code = DataUtil.stringCodeToInt(authAutoLoginConfirmResponseBody.getCode());
                        message = authAutoLoginConfirmResponseBody.getMessage();
                    }
                    if (authAutoLoginConfirmResult == null) {
                        if (finalIsLAN) {
                            authAutoLoginConfirm(context, boxUuid, domainUrl, transformation, secret, ivParams, accessToken, loginClientUuid, isAutoLogin, isLogin, false, callback);
                        } else if (callback != null) {
                            callback.onFailed(code, message, isLogin);
                        }
                    } else if (callback != null) {
                        String nRequestId = authAutoLoginConfirmResult.getRequestId();
                        callback.onSuccess(code, message, (authAutoLoginConfirmResult.isResult()
                                && (nRequestId == null || TextUtils.isEmpty(nRequestId) || nRequestId.equals(requestId))), isLogin);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    Logger.e(TAG, "on error: " + errMsg);
                    if (finalIsLAN) {
                        authAutoLoginConfirm(context, boxUuid, domainUrl, transformation, secret, ivParams, accessToken, loginClientUuid, isAutoLogin, isLogin, false, callback);
                    } else if (callback != null) {
                        callback.onError(errMsg, isLogin);
                    }
                }
            }));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void checkVersionBoxOrApp(Context context, boolean isBox, IVersionCheckCallback callback) {
        checkVersionBoxOrApp(context, isBox, false, callback);
    }

    //检查版本更新
    public static void checkVersionBoxOrApp(Context context, boolean isBox, boolean isFore, IVersionCheckCallback callback) {
        GatewayCommunicationBase gatewayCommunicationBase = null;
        if (!isBox || ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY == EulixSpaceDBUtil.getActiveDeviceUserIdentity(context)) {
            gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context, true);
        }
        if (gatewayCommunicationBase != null) {
            String baseUrl = Urls.getBaseUrl();
            try {
                ThreadPool.getInstance().execute(() -> generateLoginManager(baseUrl).
                        boxVersionCheck(UUID.randomUUID().toString(), isBox, BuildConfig.APPLICATION_ID
                                , BuildConfig.VERSION_NAME, BaseParamsUtil.sChannelCode, callback), isFore);
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        } else if (callback != null) {
            callback.onError("");
        }
    }

    private static void checkVersionCompatible(String finalBoxDomain, VersionCompatibleCallback callback) {
        generateLoginManager(finalBoxDomain).
                compatibleVersionCheck(UUID.randomUUID().toString(), BuildConfig.APPLICATION_ID
                        , BuildConfig.VERSION_NAME, BaseParamsUtil.sChannelCode, new IVersionCompatibleCallback() {
                            @Override
                            public void onResult(VersionCompatibleResponseBody result) {
                                VersionCompatibleResponseBody.Results results = null;
                                if (result != null) {
                                    results = result.results;
                                }
                                if (results == null) {
//                                            if (finalIsLAN) {
//                                                checkVersionCompatible(context, false, callback);
//                                            } else if (callback != null) {
                                    if (callback != null) {
                                        callback.onFail();
                                    }
//                                            }
                                } else {
                                    if (callback != null) {
                                        callback.onSuccess(compatibleBoolean(results.isAppForceUpdate)
                                                , compatibleBoolean(results.isBoxForceUpdate)
                                                , results.latestAppPkg, results.latestBoxPkg);
                                    }
                                }
                            }

                            @Override
                            public void onError(String errMsg) {
//                                        if (finalIsLAN) {
//                                            checkVersionCompatible(context, false, callback);
//                                        } else if (callback != null) {
                                if (callback != null) {
                                    callback.onError(errMsg);
                                }
//                                        }
                            }
                        });
    }

    public static void checkVersionCompatibleImportant(@NonNull Context context, boolean isLAN, VersionCompatibleCallback callback) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
//            String boxDomain = gatewayCommunicationBase.getBoxDomain();
//            if (isLAN) {
//                String ipAddressUrl = EulixSpaceDBUtil.getIpAddressUrl(context, gatewayCommunicationBase.getBoxUuid(), false);
//                if (ipAddressUrl != null) {
//                    boxDomain = ipAddressUrl;
//                } else {
//                    isLAN = false;
//                }
//            }
            String finalBoxDomain = (isLAN ? Urls.getBaseUrl() : gatewayCommunicationBase.getBoxDomain());
//            boolean finalIsLAN = isLAN;
            try {
                ImportantThreadPool.getInstance().execute(() -> checkVersionCompatible(finalBoxDomain, callback));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        } else if (callback != null) {
            callback.onFail();
        }
    }

    //检查兼容性
    public static void checkVersionCompatible(@NonNull Context context, boolean isLAN, boolean isFore, VersionCompatibleCallback callback) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
//            String boxDomain = gatewayCommunicationBase.getBoxDomain();
//            if (isLAN) {
//                String ipAddressUrl = EulixSpaceDBUtil.getIpAddressUrl(context, gatewayCommunicationBase.getBoxUuid(), false);
//                if (ipAddressUrl != null) {
//                    boxDomain = ipAddressUrl;
//                } else {
//                    isLAN = false;
//                }
//            }
            String finalBoxDomain = (isLAN ? Urls.getBaseUrl() : gatewayCommunicationBase.getBoxDomain());
//            boolean finalIsLAN = isLAN;
            try {
                ThreadPool.getInstance().execute(() -> checkVersionCompatible(finalBoxDomain, callback), isFore);
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        } else if (callback != null) {
            callback.onFail();
        }
    }

    //获取当前盒子系统版本
    public static void getCurrentBoxVersion(Context context, ResultCallback callback) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            try {
                String baseUrl = Urls.getBaseUrl();
                ThreadPool.getInstance().execute(() -> generateLoginManager(baseUrl).
                        getCurrentBoxVersion(UUID.randomUUID().toString(), callback));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public static class SavedSpaceStatus {
        private boolean isError;
        private int code;
        private String message;
        private String locationHost;
        private SpaceStatusResult result;
        private String errMsg;

        public SavedSpaceStatus(int code, String errMsg) {
            this.code = code;
            this.errMsg = errMsg;
            isError = true;
        }

        public SavedSpaceStatus(int code, String message, String locationHost, SpaceStatusResult result) {
            this.code = code;
            this.message = message;
            this.locationHost = locationHost;
            this.result = result;
            isError = false;
        }

        public boolean isError() {
            return isError;
        }

        public void setError(boolean error) {
            isError = error;
        }

        public int getCode() {
            return code;
        }

        public void setCode(int code) {
            this.code = code;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getLocationHost() {
            return locationHost;
        }

        public void setLocationHost(String locationHost) {
            this.locationHost = locationHost;
        }

        public SpaceStatusResult getResult() {
            return result;
        }

        public void setResult(SpaceStatusResult result) {
            this.result = result;
        }

        public String getErrMsg() {
            return errMsg;
        }

        public void setErrMsg(String errMsg) {
            this.errMsg = errMsg;
        }

        @Override
        public String toString() {
            return "SavedSpaceStatus{" +
                    "isError=" + isError +
                    ", code=" + code +
                    ", message='" + message + '\'' +
                    ", locationHost='" + locationHost + '\'' +
                    ", result=" + result +
                    ", errMsg='" + errMsg + '\'' +
                    '}';
        }
    }
}
