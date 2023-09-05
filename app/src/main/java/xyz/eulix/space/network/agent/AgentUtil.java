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

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.bean.bind.InitResponse;
import xyz.eulix.space.bean.bind.KeyExchangeReq;
import xyz.eulix.space.bean.bind.KeyExchangeRsp;
import xyz.eulix.space.bean.bind.PairingBoxResult;
import xyz.eulix.space.bean.bind.PairingBoxResults;
import xyz.eulix.space.bean.bind.PubKeyExchangeReq;
import xyz.eulix.space.bean.bind.PubKeyExchangeRsp;
import xyz.eulix.space.bean.bind.RvokInfo;
import xyz.eulix.space.network.EulixBaseResponse;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.ThreadPool;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/7/27 11:01
 */
public class AgentUtil {
    private static final String TAG = AgentUtil.class.getSimpleName();
    private static final int VERSION = 2;
    private static Map<String, AgentManager> managerMap = new HashMap<>();

    private AgentUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    private static AgentManager generateManager(String baseUrl) {
        AgentManager agentManager = null;
        if (managerMap.containsKey(baseUrl)) {
            agentManager = managerMap.get(baseUrl);
        }
        if (agentManager == null) {
            agentManager = new AgentManager(baseUrl);
            managerMap.put(baseUrl, agentManager);
        }
        return agentManager;
    }

    public static void getAgentInfo(String baseUrl, String deviceAddress, AgentInfoCallback callback) {
        if (!TextUtils.isEmpty(baseUrl)) {
            generateManager(baseUrl).getAgentInfo(new IAgentInfoCallback() {
                @Override
                public void onResult(AgentInfo result) {
                    Logger.i(TAG, "on result: " + result);
                    if (result == null) {
                        if (callback != null) {
                            callback.onFailed();
                        }
                    } else {
                        if (callback != null) {
                            callback.onSuccess(deviceAddress, result.getStatus(), result.getVersion(), result.getClientPaired(), result.getDockerStatus());
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
            });
        }
    }

    public static void exchangePublicKey(String baseUrl, String clientPublicKey, String clientPrivateKey, String btid, PubKeyExchangeCallback callback) {
        if (!TextUtils.isEmpty(baseUrl)) {
            PubKeyExchangeReq pubKeyExchangeReq = new PubKeyExchangeReq();
            pubKeyExchangeReq.setClientPubKey(StringUtil.wrapPublicKey(clientPublicKey));
            pubKeyExchangeReq.setSignedBtid(EncryptionUtil.signRSAPrivateKey(ConstantField.Algorithm.SignatureAlgorithm.SHA256_WITH_RSA
                    , null, btid, clientPrivateKey, StandardCharsets.UTF_8));
            generateManager(baseUrl).exchangePublicKey(pubKeyExchangeReq, new IPubKeyExchangeCallback() {
                @Override
                public void onResult(PubKeyExchangeResponse result) {
                    Logger.i(TAG, "on result: " + result);
                    PubKeyExchangeRsp pubKeyExchangeRsp = null;
                    String boxPublicKey = null;
                    boolean isSuccess = false;
                    if (result != null) {
                        pubKeyExchangeRsp = result.getResults();
                        if (pubKeyExchangeRsp != null) {
                            boxPublicKey = pubKeyExchangeRsp.getBoxPubKey();
                            if (boxPublicKey != null) {
                                isSuccess = EncryptionUtil.verifyRSAPublicKey(ConstantField.Algorithm.SignatureAlgorithm.SHA256_WITH_RSA
                                        , null, pubKeyExchangeRsp.getSignedBtid(), btid, StringUtil.unwrapPublicKey(boxPublicKey), StandardCharsets.UTF_8);
                            }
                        }
                    }
                    if (isSuccess) {
                        if (callback != null) {
                            callback.onSuccess(DataUtil.stringCodeToInt(result.getCode()), result.getMessage(), boxPublicKey);
                        }
                    } else {
                        if (callback != null) {
                            callback.onFailed((result == null ? -1 : DataUtil.stringCodeToInt(result.getCode())), (result == null ? "" : result.getMessage()));
                        }
                    }
                }

                @Override
                public void onError(String errMsg) {
                    Logger.e(TAG, "on error: " + errMsg);
                    if (callback != null) {
                        callback.onError(errMsg);
                    }
                }
            });
        }
    }

    public static void exchangePublicKey(String baseUrl, PubKeyExchangeReq pubKeyExchangeReq, PubKeyExchangeCallbackV2 callback) {
        if (!TextUtils.isEmpty(baseUrl) && pubKeyExchangeReq != null) {
            try {
                ThreadPool.getInstance().execute(() -> generateManager(baseUrl).exchangePublicKey(pubKeyExchangeReq, new IPubKeyExchangeCallback() {
                    @Override
                    public void onResult(PubKeyExchangeResponse result) {
                        Logger.i(TAG, "on result: " + result);
                        PubKeyExchangeRsp pubKeyExchangeRsp = null;
                        int code = 500;
                        String source = null;
                        String message = null;
                        if (result != null) {
                            String codeValue = result.getCode();
                            code = DataUtil.stringCodeToInt(codeValue);
                            source = DataUtil.stringCodeGetSource(codeValue);
                            message = result.getMessage();
                            pubKeyExchangeRsp = result.getResults();
                        }
                        if (pubKeyExchangeRsp == null) {
                            if (callback != null) {
                                callback.onFailed(code, source, message);
                            }
                        } else if (callback != null) {
                            callback.onSuccess(code, source, message, pubKeyExchangeRsp);
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

    public static void exchangeSecretKey(String baseUrl, String btid, String boxPublicKey, String clientPrivateKey, KeyExchangeCallback callback) {
        if (!TextUtils.isEmpty(baseUrl)) {
            KeyExchangeReq keyExchangeReq = new KeyExchangeReq();
            keyExchangeReq.setClientPreSecret(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.RSA_ECB_PKCS1, null
                    , DataUtil.generateRandomNumber(32), boxPublicKey, null, null));
            keyExchangeReq.setEncBtid(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.RSA_ECB_PKCS1, null
                    , btid, boxPublicKey, null, null));
            generateManager(baseUrl).exchangeSecretKey(keyExchangeReq, new IKeyExchangeCallback() {
                @Override
                public void onResult(KeyExchangeResponse result) {
                    Logger.i(TAG, "on result: " + result);
                    KeyExchangeRsp keyExchangeRsp = null;
                    if (result != null) {
                        keyExchangeRsp = result.getResults();
                    }
                    if (keyExchangeRsp == null) {
                        if (callback != null) {
                            callback.onFailed((result == null ? -1 : DataUtil.stringCodeToInt(result.getCode())), (result == null ? "" : result.getMessage()));
                        }
                    } else {
                        if (callback != null) {
                            callback.onSuccess(DataUtil.stringCodeToInt(result.getCode()), result.getMessage()
                                    , EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.RSA_ECB_PKCS1
                                            , null, keyExchangeRsp.getSharedSecret(), clientPrivateKey, null, null)
                                    , EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.RSA_ECB_PKCS1
                                            , null, keyExchangeRsp.getIv(), clientPrivateKey, null, null));
                        }
                    }
                }

                @Override
                public void onError(String errMsg) {
                    Logger.e(TAG, "on error: " + errMsg);
                    if (callback != null) {
                        callback.onError(errMsg);
                    }
                }
            });
        }
    }

    public static void exchangeSecretKey(String baseUrl, KeyExchangeReq keyExchangeReq, KeyExchangeCallbackV2 callback) {
        if (!TextUtils.isEmpty(baseUrl) && keyExchangeReq != null) {
            try {
                ThreadPool.getInstance().execute(() -> generateManager(baseUrl).exchangeSecretKey(keyExchangeReq, new IKeyExchangeCallback() {
                    @Override
                    public void onResult(KeyExchangeResponse result) {
                        Logger.i(TAG, "on result: " + result);
                        KeyExchangeRsp keyExchangeRsp = null;
                        int code = 500;
                        String source = null;
                        String message = null;
                        if (result != null) {
                            String codeValue = result.getCode();
                            code = DataUtil.stringCodeToInt(codeValue);
                            source = DataUtil.stringCodeGetSource(codeValue);
                            message = result.getMessage();
                            keyExchangeRsp = result.getResults();
                        }
                        if (keyExchangeRsp == null) {
                            if (callback != null) {
                                callback.onFailed(code, source, message);
                            }
                        } else if (callback != null) {
                            callback.onSuccess(code, source, message, keyExchangeRsp);
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

    public static void pairing(String baseUrl, String clientPublicKey, String clientUuid, String clientPrivateKey, String clientPhoneModel, String deviceAddress, PairingCallback callback) {
        if (!TextUtils.isEmpty(baseUrl)) {
            PairingClientInfo pairingClientInfo = new PairingClientInfo();
            pairingClientInfo.setClientPubKey(StringUtil.wrapPublicKey(clientPublicKey));
            pairingClientInfo.setClientUuid(clientUuid);
            pairingClientInfo.setClientPriKey(StringUtil.wrapRSAPrivateKey(clientPrivateKey));
            pairingClientInfo.setClientPhoneModel(clientPhoneModel);
            generateManager(baseUrl).pairing(pairingClientInfo, VERSION, new IPairingCallback() {
                @Override
                public void onResult(PairingResponseBody result) {
                    Logger.i(TAG, "on result: " + result);
                    PairingBoxInfo pairingBoxInfo = null;
                    if (result != null) {
                        pairingBoxInfo = result.getResult();
                    }
                    if (pairingBoxInfo == null) {
                        if (callback != null) {
                            callback.onFailed((result == null ? "" : result.getMessage()), (result == null ? -1 : DataUtil.stringCodeToInt(result.getCode())));
                        }
                    } else {
                        if (callback != null) {
                            callback.onSuccess(DataUtil.stringCodeToInt(result.getCode()), result.getMessage(), deviceAddress
                                    , pairingBoxInfo.getBoxName(), pairingBoxInfo.getBoxUuid()
                                    , pairingBoxInfo.getBoxPubKey(), pairingBoxInfo.getAuthKey()
                                    , pairingBoxInfo.getRegKey(), pairingBoxInfo.getUserDomain(), 0);
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
            });
        }
    }

    public static void pairingEnc(String baseUrl, String clientPublicKey, String clientUuid, String clientPrivateKey, String clientPhoneModel, String bleKey, String bleIv, String deviceAddress, int paired, PairingCallback callback) {
        if (!TextUtils.isEmpty(baseUrl)) {
            PairingClientInfo pairingClientInfo = new PairingClientInfo();
//            pairingClientInfo.setClientPubKey(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
//                    , null, StringUtil.wrapPublicKey(clientPublicKey)
//                    , bleKey, StandardCharsets.UTF_8, bleIv));
            pairingClientInfo.setClientUuid(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, clientUuid, bleKey, StandardCharsets.UTF_8, bleIv));
//            if (clientPrivateKey != null) {
//                pairingClientInfo.setClientPriKey(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
//                        , null, StringUtil.wrapRSAPrivateKey(clientPrivateKey)
//                        , bleKey, StandardCharsets.UTF_8, bleIv));
//            }
            pairingClientInfo.setClientPhoneModel(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, clientPhoneModel, bleKey, StandardCharsets.UTF_8, bleIv));
            Logger.i(TAG, "request body: " + pairingClientInfo);
            generateManager(baseUrl).pairing(pairingClientInfo, new IPairingEncCallback() {
                @Override
                public void onResult(PairingBoxResult result) {
                    Logger.i(TAG, "on result: " + result);
                    int code = -1;
                    int contentCode = -1;
                    PairingBoxInfo pairingBoxInfo = null;
                    if (result != null) {
                        code = DataUtil.stringCodeToInt(result.getCode());
                        String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                                , null, result.getResults(), bleKey, StandardCharsets.UTF_8, bleIv);
                        Logger.i(TAG, "pair decrypt: " + decryptedContent);
                        if (decryptedContent != null) {
                            PairingBoxResults pairingBoxResults = null;
                            try {
                                pairingBoxResults = new Gson().fromJson(decryptedContent, PairingBoxResults.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                            if (pairingBoxResults != null) {
                                contentCode = DataUtil.stringCodeToInt(pairingBoxResults.getCode());
                                pairingBoxInfo = pairingBoxResults.getResults();
                            }
                        }
                    }
                    if (contentCode <= 0) {
                        contentCode = 500;
                    }
                    if (pairingBoxInfo == null) {
                        int nCode = code;
                        if (code >= 200 && code < 400) {
                            nCode = contentCode;
                        }
                        if (callback != null) {
                            callback.onFailed((result == null ? "" : result.getMessage()), nCode);
                        }
                    } else {
                        Logger.i(TAG, "decrypt result: " + pairingBoxInfo);
                        if (callback != null) {
                            callback.onSuccess(code, result.getMessage(), deviceAddress
                                    , pairingBoxInfo.getBoxName(), pairingBoxInfo.getBoxUuid()
                                    , pairingBoxInfo.getBoxPubKey(), pairingBoxInfo.getAuthKey()
                                    , pairingBoxInfo.getRegKey(), pairingBoxInfo.getUserDomain(), paired);
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
            });
        }
    }

    public static void pairingEnc(String baseUrl, PairingClientInfo pairingClientInfo, AgentEncryptedResultsCallback callback) {
        if (!TextUtils.isEmpty(baseUrl)) {
            Logger.i(TAG, "request body: " + pairingClientInfo);
            try {
                ThreadPool.getInstance().execute(() -> generateManager(baseUrl).pairing(pairingClientInfo, new IPairingEncCallback() {
                    @Override
                    public void onResult(PairingBoxResult result) {
                        Logger.i(TAG, "on result: " + result);
                        String results = null;
                        int code = 500;
                        String source = null;
                        String message = null;
                        if (result != null) {
                            String codeValue = result.getCode();
                            code = DataUtil.stringCodeToInt(codeValue);
                            source = DataUtil.stringCodeGetSource(codeValue);
                            message = result.getMessage();
                            results = result.getResults();
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
                    public void onError(String msg) {
                        Logger.e(TAG, "on error: " + msg);
                        if (callback != null) {
                            callback.onError(msg);
                        }
                    }
                }));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public static void getAuthInfo(String baseUrl, String boxUuid, String clientPrivateKey, String deviceAddress, AuthInfoCallback callback) {
        if (!TextUtils.isEmpty(baseUrl)) {
            generateManager(baseUrl).getAuthInfo(new IAuthInfoCallback() {
                @Override
                public void onResult(AuthInfoRsp result) {
                    Logger.i(TAG, "on result: " + result);
                    AuthInfo authInfo = null;
                    if (result != null) {
                        authInfo = result.getResults();
                    }
                    if (authInfo == null) {
                        if (callback != null) {
                            callback.onFailed((result == null ? "" : result.getMessage()), (result == null ? -1 : DataUtil.stringCodeToInt(result.getCode())));
                        }
                    } else {
                        if (callback != null) {
                            String encryptAuthKey = authInfo.getAuthKey();
                            String authKey = encryptAuthKey;
                            if (clientPrivateKey != null && encryptAuthKey != null) {
                                authKey = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.RSA_ECB_PKCS1
                                        , null, encryptAuthKey, clientPrivateKey, null, null);
                            }
                            Logger.i(TAG, "decrypt auth key: " + authKey);
                            callback.onSuccess(DataUtil.stringCodeToInt(result.getCode()), result.getMessage(), deviceAddress, boxUuid, authKey);
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
            });
        }
    }

    public static void setPassword(String baseUrl, String oldPassword, String password, String bleKey, String bleIv, ResetCallback callback) {
        if (!TextUtils.isEmpty(baseUrl) && password != null && bleKey != null && bleIv != null) {
            PasswordInfo passwordInfo = new PasswordInfo();
            passwordInfo.setPassword(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                    , null, password, bleKey, StandardCharsets.UTF_8, bleIv));
            if (oldPassword != null) {
                passwordInfo.setOldPassword(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                        , null, oldPassword, bleKey, StandardCharsets.UTF_8, bleIv));
            }
            generateManager(baseUrl).setPassword(passwordInfo, new ISetPasswordCallback() {
                @Override
                public void onResult(SetPasswordResponse result) {
                    Logger.i(TAG, "on result: " + result);
                    SetPasswordResults setPasswordResults = null;
                    if (result != null) {
                        String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                                , null, result.getResults(), bleKey, StandardCharsets.UTF_8, bleIv);
                        Logger.i(TAG, "set password decrypt: " + decryptedContent);
                        if (decryptedContent != null) {
                            try {
                                setPasswordResults = new Gson().fromJson(decryptedContent, SetPasswordResults.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (setPasswordResults == null) {
                        if (callback != null) {
                            callback.onFailed("", -1);
                        }
                    } else {
                        if (callback != null) {
                            callback.onSuccess(setPasswordResults.getMessage(), DataUtil.stringCodeToInt(setPasswordResults.getCode()));
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
            });
        }
    }

    public static void setPassword(String baseUrl, PasswordInfo passwordInfo, AgentEncryptedResultsCallback callback) {
        if (!TextUtils.isEmpty(baseUrl) && passwordInfo != null) {
            try {
                ThreadPool.getInstance().execute(() -> generateManager(baseUrl).setPassword(passwordInfo, new ISetPasswordCallback() {
                    @Override
                    public void onResult(SetPasswordResponse result) {
                        Logger.i(TAG, "on result: " + result);
                        String results = null;
                        int code = 500;
                        String source = null;
                        String message = null;
                        if (result != null) {
                            String codeValue = result.getCode();
                            code = DataUtil.stringCodeToInt(codeValue);
                            source = DataUtil.stringCodeGetSource(codeValue);
                            message = result.getMessage();
                            results = result.getResults();
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
                    public void onError(String msg) {
                        Logger.e(TAG, "on error: " + msg);
                        if (callback != null) {
                            callback.onError(msg);
                        }
                    }
                }));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public static void binderResetSecurityPassword(String baseUrl, String accessToken, String password, String bleKey, String bleIv, AgentCallCallback callback) {
        BinderResetPasswordEntity entity = new BinderResetPasswordEntity();
        entity.setAccessToken(accessToken);
        entity.setNewPasswd(password);
        BinderResetPasswordRequest request = new BinderResetPasswordRequest();
        request.setApiName(ConstantField.AgentApi.ApiPath.SECURITY_PASSWORD_RESET_BINDER_LOCAL);
        request.setServiceName(ConstantField.AgentApi.ServiceName.EULIXSPACE_GATEWAY);
        request.setApiPath(ConstantField.AgentApi.ApiPath.SECURITY_PASSWORD_RESET_BINDER_LOCAL);
        request.setApiVersion(ConstantField.AgentApi.ApiVersion.V1);
        request.setEntity(entity);
        resetSecurityPassword(baseUrl, new Gson().toJson(request, BinderResetPasswordRequest.class), bleKey, bleIv, callback);
    }

    public static void granteeResetSecurityPassword(String baseUrl, String accessToken, String securityToken, String clientUuid, String password, String bleKey, String bleIv, AgentCallCallback callback) {
        GranteeResetPasswordEntity entity = new GranteeResetPasswordEntity();
        entity.setAccessToken(accessToken);
        entity.setAcceptSecurityToken(securityToken);
        entity.setClientUuid(clientUuid);
        entity.setNewPasswd(password);
        GranteeResetPasswordRequest request = new GranteeResetPasswordRequest();
        request.setApiName(ConstantField.AgentApi.ApiPath.SECURITY_PASSWORD_RESET_AUTHORIZED_LOCAL);
        request.setServiceName(ConstantField.AgentApi.ServiceName.EULIXSPACE_GATEWAY);
        request.setApiPath(ConstantField.AgentApi.ApiPath.SECURITY_PASSWORD_RESET_AUTHORIZED_LOCAL);
        request.setApiVersion(ConstantField.AgentApi.ApiVersion.V1);
        request.setEntity(entity);
        resetSecurityPassword(baseUrl, new Gson().toJson(request, GranteeResetPasswordRequest.class), bleKey, bleIv, callback);
    }

    public static void newDeviceResetSecurityPassword(String baseUrl, String acceptSecurityToken, String emailSecurityToken, String binderClientUuid, String newDeviceClientUuid, String password, String bleKey, String bleIv, AgentCallCallback callback) {
        NewDeviceResetPasswordEntity entity = new NewDeviceResetPasswordEntity();
        entity.setAcceptSecurityToken(acceptSecurityToken);
        entity.setEmailSecurityToken(emailSecurityToken);
        entity.setClientUuid(binderClientUuid);
        entity.setNewDeviceClientUuid(newDeviceClientUuid);
        entity.setNewPasswd(password);
        NewDeviceResetPasswordRequest request = new NewDeviceResetPasswordRequest();
        request.setApiName(ConstantField.AgentApi.ApiPath.SECURITY_PASSWORD_RESET_NEW_DEVICE_LOCAL);
        request.setServiceName(ConstantField.AgentApi.ServiceName.EULIXSPACE_GATEWAY);
        request.setApiPath(ConstantField.AgentApi.ApiPath.SECURITY_PASSWORD_RESET_NEW_DEVICE_LOCAL);
        request.setApiVersion(ConstantField.AgentApi.ApiVersion.V1);
        request.setEntity(entity);
        resetSecurityPassword(baseUrl, new Gson().toJson(request, NewDeviceResetPasswordRequest.class), bleKey, bleIv, callback);
    }

    private static void resetSecurityPassword(String baseUrl, String text, String bleKey, String bleIv, AgentCallCallback callback) {
        if (!TextUtils.isEmpty(baseUrl)) {
            String encryptText = text;
            if (bleKey != null && bleIv != null) {
                encryptText = EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                        , null, text, bleKey, StandardCharsets.UTF_8, bleIv);
            }
            PassthroughRequest passthroughRequest = new PassthroughRequest();
            passthroughRequest.setBody(encryptText);
            generateManager(baseUrl).agentPassthrough(passthroughRequest, new IAgentCallCallback() {
                @Override
                public void onResult(AgentCallResponse response) {
                    EulixBaseResponse eulixBaseResponse = null;
                    if (response != null) {
                        String results = response.getResults();
                        if (results != null) {
                            String decryptContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                                    , null, results, bleKey, StandardCharsets.UTF_8, bleIv);
                            if (decryptContent != null) {
                                Logger.i(TAG, "on result: " + decryptContent);
                                try {
                                    eulixBaseResponse = new Gson().fromJson(decryptContent, EulixBaseResponse.class);
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    if (eulixBaseResponse == null) {
                        if (callback != null) {
                            callback.onFailed(500, null, null, null);
                        }
                    } else if (callback != null) {
                        String code = eulixBaseResponse.getCode();
                        callback.onSuccess(DataUtil.stringCodeToInt(code), DataUtil.stringCodeGetSource(code)
                                , eulixBaseResponse.getMessage(), eulixBaseResponse.getRequestId());
                    }
                }

                @Override
                public void onError(String errMsg) {
                    Logger.e(TAG, "on error: " + errMsg);
                    if (callback != null) {
                        callback.onError(errMsg);
                    }
                }
            });
        }
    }

    public static void resetSecurityPassword(String baseUrl, String request, AgentEncryptedResultsCallback callback) {
        PassthroughRequest passthroughRequest = new PassthroughRequest();
        passthroughRequest.setBody(request);
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).agentPassthrough(passthroughRequest, new IAgentCallCallback() {
                @Override
                public void onResult(AgentCallResponse response) {
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

    public static void newDeviceApplyResetSecurityPassword(String baseUrl, String deviceInfo, String newDeviceClientUuid, String applyId, String bleKey, String bleIv, AgentCallCallback callback) {
        NewDeviceApplyResetPasswordEntity entity = new NewDeviceApplyResetPasswordEntity();
        entity.setDeviceInfo(deviceInfo);
        entity.setClientUuid(newDeviceClientUuid);
        entity.setApplyId(applyId);
        NewDeviceApplyResetPasswordRequest request = new NewDeviceApplyResetPasswordRequest();
        request.setApiName(ConstantField.AgentApi.ApiPath.SECURITY_PASSWORD_RESET_NEW_DEVICE_APPLY_LOCAL);
        request.setServiceName(ConstantField.AgentApi.ServiceName.EULIXSPACE_GATEWAY);
        request.setApiPath(ConstantField.AgentApi.ApiPath.SECURITY_PASSWORD_RESET_NEW_DEVICE_APPLY_LOCAL);
        request.setApiVersion(ConstantField.AgentApi.ApiVersion.V1);
        request.setEntity(entity);
        String text = new Gson().toJson(request, NewDeviceApplyResetPasswordRequest.class);
        if (!TextUtils.isEmpty(baseUrl)) {
            String encryptText = text;
            if (bleKey != null && bleIv != null) {
                encryptText = EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                        , null, text, bleKey, StandardCharsets.UTF_8, bleIv);
            }
            PassthroughRequest passthroughRequest = new PassthroughRequest();
            passthroughRequest.setBody(encryptText);
            generateManager(baseUrl).agentPassthrough(passthroughRequest, new IAgentCallCallback() {
                @Override
                public void onResult(AgentCallResponse response) {
                    EulixBaseResponse eulixBaseResponse = null;
                    if (response != null) {
                        String results = response.getResults();
                        if (results != null) {
                            String decryptContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                                    , null, results, bleKey, StandardCharsets.UTF_8, bleIv);
                            if (decryptContent != null) {
                                Logger.i(TAG, "on result: " + decryptContent);
                                try {
                                    eulixBaseResponse = new Gson().fromJson(decryptContent, EulixBaseResponse.class);
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    if (eulixBaseResponse == null) {
                        if (callback != null) {
                            callback.onFailed(500, null, null, null);
                        }
                    } else if (callback != null) {
                        String code = eulixBaseResponse.getCode();
                        callback.onSuccess(DataUtil.stringCodeToInt(code), DataUtil.stringCodeGetSource(code)
                                , eulixBaseResponse.getMessage(), eulixBaseResponse.getRequestId());
                    }
                }

                @Override
                public void onError(String errMsg) {
                    Logger.e(TAG, "on error: " + errMsg);
                    if (callback != null) {
                        callback.onError(errMsg);
                    }
                }
            });
        }
    }

    public static void newDeviceApplyResetSecurityPassword(String baseUrl, String request, AgentEncryptedResultsCallback callback) {
        PassthroughRequest passthroughRequest = new PassthroughRequest();
        passthroughRequest.setBody(request);
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).agentPassthrough(passthroughRequest, new IAgentCallCallback() {
                @Override
                public void onResult(AgentCallResponse response) {
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

    public static void securityMessagePoll(String baseUrl, String clientUuid, String bleKey, String bleIv, SecurityMessagePollCallback callback) {
        SecurityMessagePollEntity entity = new SecurityMessagePollEntity();
        entity.setClientUuid(clientUuid);
        SecurityMessagePollRequest request = new SecurityMessagePollRequest();
        request.setApiName(ConstantField.AgentApi.ApiPath.SECURITY_MESSAGE_POLL_LOCAL);
        request.setServiceName(ConstantField.AgentApi.ServiceName.EULIXSPACE_GATEWAY);
        request.setApiPath(ConstantField.AgentApi.ApiPath.SECURITY_MESSAGE_POLL_LOCAL);
        request.setApiVersion(ConstantField.AgentApi.ApiVersion.V1);
        request.setEntity(entity);
        String text = new Gson().toJson(request, SecurityMessagePollRequest.class);
        if (!TextUtils.isEmpty(baseUrl)) {
            String encryptText = text;
            if (bleKey != null && bleIv != null) {
                encryptText = EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                        , null, text, bleKey, StandardCharsets.UTF_8, bleIv);
            }
            PassthroughRequest passthroughRequest = new PassthroughRequest();
            passthroughRequest.setBody(encryptText);
            generateManager(baseUrl).agentPassthrough(passthroughRequest, new IAgentCallCallback() {
                @Override
                public void onResult(AgentCallResponse response) {
                    SecurityMessagePollResponse securityMessagePollResponse = null;
                    if (response != null) {
                        String results = response.getResults();
                        if (results != null) {
                            String decryptContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                                    , null, results, bleKey, StandardCharsets.UTF_8, bleIv);
                            if (decryptContent != null) {
                                Logger.i(TAG, "on result: " + decryptContent);
                                try {
                                    securityMessagePollResponse = new Gson().fromJson(decryptContent, SecurityMessagePollResponse.class);
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                    if (securityMessagePollResponse == null) {
                        if (callback != null) {
                            callback.onFailed(500, null, null, null);
                        }
                    } else if (callback != null) {
                        String code = securityMessagePollResponse.getCode();
                        callback.onSuccess(DataUtil.stringCodeToInt(code), DataUtil.stringCodeGetSource(code)
                                , securityMessagePollResponse.getMessage(), securityMessagePollResponse.getRequestId()
                                , securityMessagePollResponse.getResults());
                    }
                }

                @Override
                public void onError(String errMsg) {
                    Logger.e(TAG, "on error: " + errMsg);
                    if (callback != null) {
                        callback.onError(errMsg);
                    }
                }
            });
        }
    }

    public static void securityMessagePoll(String baseUrl, String request, AgentEncryptedResultsCallback callback) {
        PassthroughRequest passthroughRequest = new PassthroughRequest();
        passthroughRequest.setBody(request);
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).agentPassthrough(passthroughRequest, new IAgentCallCallback() {
                @Override
                public void onResult(AgentCallResponse response) {
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

    public static void securityEmailSetting(String baseUrl, String request, AgentEncryptedResultsCallback callback) {
        PassthroughRequest passthroughRequest = new PassthroughRequest();
        passthroughRequest.setBody(request);
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).agentPassthrough(passthroughRequest, new IAgentCallCallback() {
                @Override
                public void onResult(AgentCallResponse response) {
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

    public static void verifySecurityEmail(String baseUrl, String request, AgentEncryptedResultsCallback callback) {
        PassthroughRequest passthroughRequest = new PassthroughRequest();
        passthroughRequest.setBody(request);
        try {
            ThreadPool.getInstance().execute(() -> generateManager(baseUrl).agentPassthrough(passthroughRequest, new IAgentCallCallback() {
                @Override
                public void onResult(AgentCallResponse response) {
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

    public static void revoke(String baseUrl, String clientUuid, String password, String bleKey, String bleIv, RevokeCallback callback) {
        if (!TextUtils.isEmpty(baseUrl) && password != null) {
            AdminRevokeReq adminRevokeReq = new AdminRevokeReq();
            if (bleKey != null && bleIv != null) {
                adminRevokeReq.setClientUUID(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                        , null, clientUuid, bleKey, StandardCharsets.UTF_8, bleIv));
                adminRevokeReq.setPassword(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                        , null, password, bleKey, StandardCharsets.UTF_8, bleIv));
            } else {
                adminRevokeReq.setClientUUID(clientUuid);
                adminRevokeReq.setPassword(password);
            }
            generateManager(baseUrl).revoke(adminRevokeReq, new IRevokeCallback() {
                @Override
                public void onResult(AdminRevokeResponse result) {
                    Logger.i(TAG, "on result: " + result);
                    int code = -1;
                    AdminRevokeResult adminRevokeResult = null;
                    String boxUuid = null;
                    int errorTimes = -1;
                    int leftTryTimes = -1;
                    int tryAfterSeconds = -1;
                    if (result != null) {
                        code = DataUtil.stringCodeToInt(result.getCode());
                        String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                                , null, result.getResults(), bleKey, StandardCharsets.UTF_8, bleIv);
                        if (decryptedContent != null) {
                            Logger.i(TAG, "revoke decrypt: " + decryptedContent);
                            AdminRevokeResults adminRevokeResults = null;
                            try {
                                adminRevokeResults = new Gson().fromJson(decryptedContent, AdminRevokeResults.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                            if (adminRevokeResults != null) {
                                code = DataUtil.stringCodeToInt(adminRevokeResults.getCode());
                                adminRevokeResult = adminRevokeResults.getResults();
                                if (adminRevokeResult != null) {
                                    boxUuid = adminRevokeResult.getBoxUuid();
                                    errorTimes = adminRevokeResult.getErrorTimes();
                                    leftTryTimes = adminRevokeResult.getLeftTryTimes();
                                    tryAfterSeconds = adminRevokeResult.getTryAfterSeconds();
                                }
                            }
                        }
                    }
                    if (result == null) {
                        if (callback != null) {
                            callback.onFailed("", -1);
                        }
                    } else {
                        if (callback != null) {
                            callback.onSuccess(result.getMessage(), code, boxUuid
                                    , errorTimes, leftTryTimes, tryAfterSeconds);
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
            });
        }
    }

    public static void revoke(String baseUrl, RvokInfo rvokInfo, AgentEncryptedResultsCallback callback) {
        if (!TextUtils.isEmpty(baseUrl) && rvokInfo != null) {
            try {
                ThreadPool.getInstance().execute(() -> generateManager(baseUrl).revoke(rvokInfo, new IRevokeCallback() {
                    @Override
                    public void onResult(AdminRevokeResponse result) {
                        Logger.i(TAG, "on result: " + result);
                        String results = null;
                        int code = 500;
                        String source = null;
                        String message = null;
                        if (result != null) {
                            String codeValue = result.getCode();
                            code = DataUtil.stringCodeToInt(codeValue);
                            source = DataUtil.stringCodeGetSource(codeValue);
                            message = result.getMessage();
                            results = result.getResults();
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
                    public void onError(String msg) {
                        Logger.e(TAG, "on error: " + msg);
                        if (callback != null) {
                            callback.onError(msg);
                        }
                    }
                }));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public static void reset(String baseUrl, String hashed, String msg, ResetCallback callback) {
        if (!TextUtils.isEmpty(baseUrl)) {
            ResetClientReq resetClientReq = new ResetClientReq();
            resetClientReq.setHashed(hashed);
            resetClientReq.setMsg(msg);
            generateManager(baseUrl).reset(resetClientReq, new IResetCallback() {
                @Override
                public void onResult(BaseRsp result) {
                    Logger.i(TAG, "on result: " + result);
                    if (result == null) {
                        if (callback != null) {
                            callback.onFailed("", -1);
                        }
                    } else {
                        if (callback != null) {
                            callback.onSuccess(result.getMessage(), DataUtil.stringCodeToInt(result.getCode()));
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
            });
        }
    }

    public static void initial(String baseUrl, String bleKey, String bleIv, InitialCallback callback) {
        if (!TextUtils.isEmpty(baseUrl)) {
            generateManager(baseUrl).initial(new IInitialCallback() {
                @Override
                public void onResult(InitialRsp result) {
                    Logger.i(TAG, "on result: " + result);
                    InitialResults initialResults = null;
                    if (result != null) {
                        String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                                , null, result.getResults(), bleKey, StandardCharsets.UTF_8, bleIv);
                        Logger.i(TAG, "initial decrypt: " + decryptedContent);
                        if (decryptedContent != null) {
                            try {
                                initialResults = new Gson().fromJson(decryptedContent, InitialResults.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (initialResults == null) {
                        if (callback != null) {
                            callback.onFailed("", -1);
                        }
                    } else {
                        if (callback != null) {
                            callback.onSuccess(initialResults.getMessage(), DataUtil.stringCodeToInt(initialResults.getCode()), DataUtil.stringCodeToInt(result.getCode()));
                        }
                    }
                }

                @Override
                public void onError(String msg) {
                    Logger.e(TAG, "on error: " + msg);
                    if (callback != null) {
                        callback.onError(msg, baseUrl, bleKey, bleIv);
                    }
                }
            });
        }
    }

    public static void initial(String baseUrl, AgentEncryptedResultsCallback callback) {
        if (!TextUtils.isEmpty(baseUrl)) {
            try {
                ThreadPool.getInstance().execute(() -> generateManager(baseUrl).initial(new IInitialCallback() {
                    @Override
                    public void onResult(InitialRsp result) {
                        Logger.i(TAG, "on result: " + result);
                        String results = null;
                        int code = 500;
                        String source = null;
                        String message = null;
                        if (result != null) {
                            String codeValue = result.getCode();
                            code = DataUtil.stringCodeToInt(codeValue);
                            source = DataUtil.stringCodeGetSource(codeValue);
                            message = result.getMessage();
                            results = result.getResults();
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
                    public void onError(String msg) {
                        Logger.e(TAG, "on error: " + msg);
                        if (callback != null) {
                            callback.onError(msg);
                        }
                    }
                }));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public static void initial(String baseUrl, String password, String bleKey, String bleIv, InitialCallback callback) {
        if (!TextUtils.isEmpty(baseUrl)) {
            PasswordInfo passwordInfo = new PasswordInfo();
            if (bleKey != null && bleIv != null) {
                passwordInfo.setPassword(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                        , null, password, bleKey, StandardCharsets.UTF_8, bleIv));
            } else {
                passwordInfo.setPassword(password);
            }
            generateManager(baseUrl).initial(passwordInfo, new IInitialCallback() {
                @Override
                public void onResult(InitialRsp result) {
                    Logger.i(TAG, "on result: " + result);
                    InitialResults initialResults = null;
                    if (result != null) {
                        String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                                , null, result.getResults(), bleKey, StandardCharsets.UTF_8, bleIv);
                        Logger.i(TAG, "set password decrypt: " + decryptedContent);
                        if (decryptedContent != null) {
                            try {
                                initialResults = new Gson().fromJson(decryptedContent, InitialResults.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (initialResults == null) {
                        if (callback != null) {
                            callback.onFailed("", -1);
                        }
                    } else {
                        if (callback != null) {
                            callback.onSuccess(initialResults.getMessage(), DataUtil.stringCodeToInt(initialResults.getCode()), DataUtil.stringCodeToInt(result.getCode()));
                        }
                    }
                }

                @Override
                public void onError(String msg) {
                    Logger.e(TAG, "on error: " + msg);
                    if (callback != null) {
                        callback.onError(msg, baseUrl, password, bleKey, bleIv);
                    }
                }
            });
        }
    }

    public static void initial(String baseUrl, PasswordInfo passwordInfo, AgentEncryptedResultsCallback callback) {
        if (!TextUtils.isEmpty(baseUrl) && passwordInfo != null) {
            try {
                ThreadPool.getInstance().execute(() -> generateManager(baseUrl).initial(passwordInfo, new IInitialCallback() {
                    @Override
                    public void onResult(InitialRsp result) {
                        Logger.i(TAG, "on result: " + result);
                        String results = null;
                        int code = 500;
                        String source = null;
                        String message = null;
                        if (result != null) {
                            String codeValue = result.getCode();
                            code = DataUtil.stringCodeToInt(codeValue);
                            source = DataUtil.stringCodeGetSource(codeValue);
                            message = result.getMessage();
                            results = result.getResults();
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
                    public void onError(String msg) {
                        Logger.e(TAG, "on error: " + msg);
                        if (callback != null) {
                            callback.onError(msg);
                        }
                    }
                }));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public static void pairInit(String baseUrl, String boxKey, String boxIv, PairInitCallback callback) {
        if (!TextUtils.isEmpty(baseUrl)) {
            generateManager(baseUrl).pairInit(new IPairInitCallback() {
                @Override
                public void onResult(PairInitResponse result) {
                    Logger.i(TAG, "on result: " + result);
                    InitResponse initResponse = null;
                    if (result != null) {
                        String decryptedContent = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
                                , null, result.getResults(), boxKey, StandardCharsets.UTF_8, boxIv);
                        Logger.i(TAG, "init decrypt: " + decryptedContent);
                        if (decryptedContent != null) {
                            try {
                                initResponse = new Gson().fromJson(decryptedContent, InitResponse.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if (initResponse == null) {
                        if (callback != null) {
                            callback.onFailed();
                        }
                    } else {
                        if (callback != null) {
                            callback.onSuccess(result.getCode(), result.getMessage(), initResponse);
                        }
                    }
                }

                @Override
                public void onError(String errMsg) {
                    Logger.e(TAG, "on error: " + errMsg);
                    if (callback != null) {
                        callback.onError(errMsg);
                    }
                }
            });
        }
    }

    public static void pairInit(String baseUrl, AgentEncryptedResultsCallback callback) {
        if (!TextUtils.isEmpty(baseUrl)) {
            try {
                ThreadPool.getInstance().execute(() -> generateManager(baseUrl).pairInit(new IPairInitCallback() {
                    @Override
                    public void onResult(PairInitResponse result) {
                        Logger.i(TAG, "on result: " + result);
                        String results = null;
                        int code = 500;
                        String source = null;
                        String message = null;
                        if (result != null) {
                            String codeValue = result.getCode();
                            code = DataUtil.stringCodeToInt(codeValue);
                            source = DataUtil.stringCodeGetSource(codeValue);
                            message = result.getMessage();
                            results = result.getResults();
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
}
