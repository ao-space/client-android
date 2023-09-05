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

package xyz.eulix.space.network.userinfo;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.event.MemberListEvent;
import xyz.eulix.space.event.StorageInfoRequestEvent;
import xyz.eulix.space.event.TerminalListEvent;
import xyz.eulix.space.event.UserInfoEvent;
import xyz.eulix.space.network.gateway.AlgorithmConfig;
import xyz.eulix.space.network.gateway.CreateAuthTokenCallback;
import xyz.eulix.space.network.gateway.CreateMemberTokenInfo;
import xyz.eulix.space.network.gateway.CreateTokenInfo;
import xyz.eulix.space.network.gateway.CreateTokenResult;
import xyz.eulix.space.network.gateway.ICreateAuthTokenCallback;
import xyz.eulix.space.network.gateway.TransportationConfig;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FileThreadPool;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.GatewayUtils;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.MD5Util;
import xyz.eulix.space.util.PreferenceUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.ThreadPool;
import xyz.eulix.space.util.Urls;

/**
 * Author:      Zhu Fuyu
 * Description:
 * History:     2021/9/17
 */
public class UserInfoUtil {
    private static final String TAG = UserInfoUtil.class.getSimpleName();
    private static String spUUID = null;
    private static String activeBoxUuid;
    private static String activeBoxBind;
    private static final String API_VERSION = ConstantField.BoxVersionName.VERSION_0_2_5;
    public static final String AVATAR_PATH = "avatarPath";
    public static final String NICKNAME = "nickname";
    public static final String SIGNATURE = "signature";

    public static final String NICK_FORMAT_ERROR_CODE = "403";
    public static final String NICK_REPEAT_ERROR_CODE = "400";

    private static boolean isAvatarUpdate(String currentAvatarPath, String newestAvatarMD5) {
        boolean isUpdate = true;
        if (currentAvatarPath != null && newestAvatarMD5 != null) {
            boolean isFileExist = FileUtil.existFile(currentAvatarPath);
            if (isFileExist) {
                String currentAvatarMD5 = null;
                try {
                    currentAvatarMD5 = MD5Util.getFileMD5String(currentAvatarPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Logger.d(TAG, "current avatar MD5: " + currentAvatarMD5 + ", newest avatar MD5: " + newestAvatarMD5);
                isUpdate = !newestAvatarMD5.equals(currentAvatarMD5);
            }
        }
        return isUpdate;
    }

    //获取用户信息（昵称、签名）
    public static boolean getUserInfo(@NonNull Context context, boolean isLAN) {
        boolean result = false;
        String clientUuid = DataUtil.getCompatibleClientUuid(context.getApplicationContext());
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context, true);
        if (gatewayCommunicationBase != null && !TextUtils.isEmpty(clientUuid)) {
            result = true;
            String boxUuid = gatewayCommunicationBase.getBoxUuid();
            String boxBind = gatewayCommunicationBase.getBoxBind();
            String boxDomain = gatewayCommunicationBase.getBoxDomain();
            String finalClientUuid = clientUuid;
            String finalBoxDomain = Urls.getBaseUrl();
            try {
                ThreadPool.getInstance().execute(() -> {
                    UserInfoManager.getPersonalInfo(finalBoxDomain, gatewayCommunicationBase.getAccessToken(),
                            gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getIvParams(), API_VERSION, new IAccountInfoCallback() {
                                @Override
                                public void onResult(AccountInfoResult result) {
                                    String name = null;
                                    String sign = null;
                                    boolean isFind = false;
                                    if (result != null) {
                                        List<PersonalInfoResult> personalInfoResults = result.getResults();
                                        if (personalInfoResults != null) {
                                            for (PersonalInfoResult personalInfoResult : personalInfoResults) {
                                                if (personalInfoResult != null) {
                                                    String clientUuid = personalInfoResult.getClientUuid();
                                                    if (clientUuid != null && clientUuid.equals(finalClientUuid)) {
                                                        name = personalInfoResult.getPersonalName();
                                                        sign = personalInfoResult.getPersonalSign();
                                                        isFind = true;
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (isFind) {
                                        Logger.d("zfy", "getUserInfo success, name:" + name + ",sign:" + sign);
                                        Map<String, String> userMap = new HashMap<>();
                                        userMap.put(NICKNAME, name);
                                        userMap.put(SIGNATURE, sign);
                                        updateUserInfoDB(context, boxUuid, boxBind, finalClientUuid, userMap);
                                        //发送广播更新
                                        EulixBoxBaseInfo boxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(context);
                                        if (boxBaseInfo != null) {
                                            String activeBoxUuid = boxBaseInfo.getBoxUuid();
                                            String activeBoxBind = boxBaseInfo.getBoxBind();
                                            if (activeBoxUuid != null && activeBoxUuid.equals(boxUuid)
                                                    && activeBoxBind != null && activeBoxBind.equals(boxBind)) {
                                                PreferenceUtil.saveNickname(context, name);
                                                PreferenceUtil.saveSignature(context, sign);
                                                EventBusUtil.post(new UserInfoEvent(UserInfoEvent.TYPE_NAME, null, name, sign));
                                            }
                                        }
                                    } else {
                                        Logger.w(TAG, "not found user info: " + clientUuid);
                                    }
                                }

                                @Override
                                public void onError(String msg) {
                                    Logger.d("zfy", "get user info error:" + msg);
                                }
                            });
                });
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    //更新用户信息
    @Deprecated
    public static void updateUserInfoOld(Context context, String nickName, String signature, IUpdateUserInfoCallback listener) {
        spUUID = DataUtil.getClientUuid(context.getApplicationContext());

        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null && !TextUtils.isEmpty(spUUID)) {
            try {
                ThreadPool.getInstance().execute(() -> {
                    UserInfoManager.updateUserInfo(spUUID, nickName, signature, gatewayCommunicationBase.getBoxDomain(),
                            gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey(),
                            gatewayCommunicationBase.getIvParams(), API_VERSION, listener);
                });
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    // server exception handle
    //更新用户信息
    public static void updateUserInfo(Context context, String nickName, String signature, boolean isLAN, IUpdateUserInfoCallback listener) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context, true);
        if (gatewayCommunicationBase != null) {
            String finalBoxDomain = Urls.getBaseUrl();
            try {
                ThreadPool.getInstance().execute(() -> {
                    UserInfoManager.updateUserInfo(nickName, signature, finalBoxDomain,
                            gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey(),
                            gatewayCommunicationBase.getIvParams(), API_VERSION, new IAccountInfoCallback() {
                                @Override
                                public void onResult(AccountInfoResult result) {
                                    String message = "";
                                    if (result != null){
                                        if (result.getCodeInt()>=200 && result.getCodeInt() < 300){
                                            if (listener != null) {
                                                listener.onResult(true, null);
                                            }
                                        } else if (result.getCode().contains(NICK_FORMAT_ERROR_CODE)) {
                                            message = NICK_FORMAT_ERROR_CODE;
                                            if (listener != null) {
                                                listener.onResult(false, message);
                                            }
                                        } else if (result.getCode().contains(NICK_REPEAT_ERROR_CODE)) {
                                            message = NICK_REPEAT_ERROR_CODE;
                                            if (listener != null) {
                                                listener.onResult(false, message);
                                            }
                                        } else {
                                            if (listener != null) {
                                                listener.onResult(false, message);
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void onError(String msg) {
                                    if (listener != null) {
                                        listener.onResult(null, msg);
                                    }
                                }
                            });
                }, true);
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    //更新头像
    public static void updateHeader(Context context, String headerPath, String fileName, IUpdateUserInfoCallback listener) {
        String clientUuid = DataUtil.getCompatibleClientUuid(context.getApplicationContext());

        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            String baseUrl = Urls.getBaseUrl();
            try {
                FileThreadPool.getInstance().execute(() -> {
                    UserInfoManager.updateHeader(context, clientUuid, headerPath, fileName,
                            baseUrl, gatewayCommunicationBase.getAccessToken(),
                            gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getTransformation(),
                            gatewayCommunicationBase.getIvParams(), API_VERSION, listener);
                });
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean getHeaderImage(@NonNull Context context) {
        boolean result = false;
        String clientUuid = DataUtil.getCompatibleClientUuid(context.getApplicationContext());
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context.getApplicationContext());
        if (gatewayCommunicationBase != null) {
            String aoid = null;
            String avatarPath = null;
            String avatarMD5 = null;
            String boxUuid = gatewayCommunicationBase.getBoxUuid();
            String boxBind = gatewayCommunicationBase.getBoxBind();
            if (boxUuid != null && boxBind != null) {
                Map<String, String> queryMap = new HashMap<>();
                queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, queryMap);
                if (boxValues != null) {
                    for (Map<String, String> boxValue : boxValues) {
                        if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_USER_INFO)) {
                            String userInfoValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_USER_INFO);
                            if (userInfoValue != null) {
                                Map<String, UserInfo> userInfoMap = null;
                                try {
                                    userInfoMap = new Gson().fromJson(userInfoValue, new TypeToken<Map<String, UserInfo>>() {
                                    }.getType());
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                                if (userInfoMap != null && userInfoMap.containsKey(clientUuid)) {
                                    UserInfo userInfo = userInfoMap.get(clientUuid);
                                    if (userInfo != null) {
                                        aoid = userInfo.getUserId();
                                        avatarPath = userInfo.getAvatarPath();
                                        avatarMD5 = userInfo.getAvatarMD5();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            result = true;
            if (aoid != null && isAvatarUpdate(avatarPath, avatarMD5)) {
                String baseUrl = Urls.getBaseUrl();
                String finalAoid = aoid;
                try {
                    FileThreadPool.getInstance().execute(() -> {
                        UserInfoManager.downloadHeader(context, clientUuid, finalAoid, boxUuid, boxBind
                                , baseUrl
                                , gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                                , gatewayCommunicationBase.getIvParams(), API_VERSION, true);
                    });
                } catch (RejectedExecutionException e) {
                    e.printStackTrace();
                }
            } else {
                Logger.d(TAG, "ao id: " + (aoid == null ? "null, avatar update unknown" : (aoid + ", avatar newest")));
            }
        }
        return result;
    }

    public static boolean getHeaderImage(@NonNull Context context, String clientUuid) {
        boolean result = false;
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context.getApplicationContext());
        if (gatewayCommunicationBase != null && clientUuid != null && !TextUtils.isEmpty(clientUuid)) {
            String aoid = null;
            String avatarPath = null;
            String avatarMD5 = null;
            String boxUuid = gatewayCommunicationBase.getBoxUuid();
            String boxBind = gatewayCommunicationBase.getBoxBind();
            if (boxUuid != null && boxBind != null) {
                Map<String, String> queryMap = new HashMap<>();
                queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, queryMap);
                if (boxValues != null) {
                    for (Map<String, String> boxValue : boxValues) {
                        if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_USER_INFO)) {
                            String userInfoValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_USER_INFO);
                            if (userInfoValue != null) {
                                Map<String, UserInfo> userInfoMap = null;
                                try {
                                    userInfoMap = new Gson().fromJson(userInfoValue, new TypeToken<Map<String, UserInfo>>() {
                                    }.getType());
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                                if (userInfoMap != null && userInfoMap.containsKey(clientUuid)) {
                                    UserInfo userInfo = userInfoMap.get(clientUuid);
                                    if (userInfo != null) {
                                        aoid = userInfo.getUserId();
                                        avatarPath = userInfo.getAvatarPath();
                                        avatarMD5 = userInfo.getAvatarMD5();
                                    }
                                }
                            }
                        }
                    }
                }
            }
            result = true;
            if (aoid != null && isAvatarUpdate(avatarPath, avatarMD5)) {
                String baseUrl = Urls.getBaseUrl();
                String finalAoid = aoid;
                try {
                    FileThreadPool.getInstance().execute(() -> {
                        UserInfoManager.downloadHeader(context, clientUuid, finalAoid, boxUuid, boxBind
                                , baseUrl
                                , gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                                , gatewayCommunicationBase.getIvParams(), API_VERSION, false);
                    });
                } catch (RejectedExecutionException e) {
                    e.printStackTrace();
                }
            } else {
                Logger.d(TAG, "ao id: " + (aoid == null ? "null, avatar update unknown" : (aoid + ", avatar newest")));
            }
        }
        return result;
    }

    public static boolean getMemberUsedStorage(@NonNull Context context) {
        boolean result = false;
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context.getApplicationContext());
        if (gatewayCommunicationBase != null) {
            result = true;
            String boxUuid = gatewayCommunicationBase.getBoxUuid();
            int identity = EulixSpaceDBUtil.getActiveDeviceUserIdentity(context);
            String checkAoId = null;
            if (identity == ConstantField.UserIdentity.MEMBER_GRANTEE) {
                checkAoId = gatewayCommunicationBase.getBoxBind();
            }
            String ipAddressUrl = Urls.getBaseUrl();
            Map<String, UserInfo> userInfoMap = EulixSpaceDBUtil.getActiveUserInfo(context);
            if (userInfoMap != null) {
                if (identity == ConstantField.UserIdentity.MEMBER_IDENTITY) {
                    String clientUuid = DataUtil.getClientUuid(context);
                    if (clientUuid != null && userInfoMap.containsKey(clientUuid)) {
                        UserInfo userInfo = userInfoMap.get(clientUuid);
                        if (userInfo != null) {
                            String aoId = userInfo.getUserId();
                            if (aoId != null) {
                                getMemberUsedStorage(context, boxUuid, clientUuid, gatewayCommunicationBase.getBoxDomain()
                                        , ipAddressUrl, gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                                        , gatewayCommunicationBase.getIvParams(), aoId, true);
                            }
                        }
                    }
                } else {
                    Set<Map.Entry<String, UserInfo>> entrySet = userInfoMap.entrySet();
                    for (Map.Entry<String, UserInfo> entry : entrySet) {
                        if (entry != null) {
                            String clientUuid = entry.getKey();
                            UserInfo userInfo = entry.getValue();
                            if (clientUuid != null && userInfo != null) {
                                String aoId = userInfo.getUserId();
                                if (aoId != null && (checkAoId == null || aoId.equals(checkAoId))) {
                                    getMemberUsedStorage(context, boxUuid, clientUuid, gatewayCommunicationBase.getBoxDomain()
                                            , ipAddressUrl, gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                                            , gatewayCommunicationBase.getIvParams(), aoId, true);
                                }
                            }
                        }
                    }
                }
            }
        }
        return result;
    }

    private static void getMemberUsedStorage(@NonNull Context context, String boxUuid, String clientUuid, String boxDomain, String ipAddressUrl, String accessToken, String secretKey, String ivParams, String aoId, boolean isLAN) {
        String domainUrl = boxDomain;
        if (isLAN) {
            if (ipAddressUrl != null) {
                boxDomain = ipAddressUrl;
            } else {
                isLAN = false;
            }
        }
        String finalBoxDomain = boxDomain;
        boolean finalIsLAN = isLAN;
        try {
            ThreadPool.getInstance().execute(() -> UserInfoManager.getMemberUsedStorage(clientUuid
                    , finalBoxDomain, accessToken, secretKey, ivParams, API_VERSION, aoId, new IMemberUsedStorageCallback() {
                        @Override
                        public void onResult(String clientUuid1, MemberUsedStorageResponseBody result1) {
                            boolean isRetry = false;
                            long usedStorage = -1L;
                            long totalStorage = -1L;
                            if (result1 != null) {
                                MemberUsedStorageResult memberUsedStorageResult = result1.getResults();
                                if (memberUsedStorageResult != null) {
                                    usedStorage = memberUsedStorageResult.getUserStorage();
                                    totalStorage = memberUsedStorageResult.getTotalStorage();
                                }
                            } else if (finalIsLAN) {
                                isRetry = true;
                                getMemberUsedStorage(context, boxUuid, clientUuid, domainUrl, null, accessToken, secretKey, ivParams, aoId, false);
                            }
                            if (!isRetry && usedStorage >= 0) {
                                String currentBoxUuid = EulixSpaceDBUtil.queryAvailableBoxUuid(context);
                                if (currentBoxUuid != null && currentBoxUuid.equals(boxUuid)) {
                                    Map<String, UserInfo> boxV = EulixSpaceDBUtil.getActiveUserInfo(context);
                                    UserInfo info = null;
                                    if (boxV != null && boxV.containsKey(clientUuid1)) {
                                        info = boxV.get(clientUuid1);
                                    }
                                    if (boxV != null && info != null) {
                                        info.setUsedSize(usedStorage);
                                        info.setTotalSize(totalStorage);
                                        boxV.put(clientUuid1, info);
                                        Map<String, String> boxValue = new HashMap<>();
                                        boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                                        boxValue.put(EulixSpaceDBManager.FIELD_BOX_USER_INFO, new Gson().toJson(boxV, new TypeToken<Map<String, UserInfo>>() {
                                        }.getType()));
                                        EulixSpaceDBUtil.updateBox(context, boxValue);
                                    }
                                }
                            }
                        }

                        @Override
                        public void onError(String msg) {
                            Logger.e(TAG, "on error: " + msg);
                            if (finalIsLAN) {
                                getMemberUsedStorage(context, boxUuid, clientUuid, domainUrl, null, accessToken, secretKey, ivParams, aoId, false);
                            }
                        }
                    }));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void getMemberList(@NonNull Context context, String boxUuid, String boxDomain, String accessToken, String secret, String ivParams, boolean isLAN, boolean isFore, AccountInfoCallback callback) {
        String tempBoxDomain = boxDomain;
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null && gatewayCommunicationBase.getBoxDomain() != null && gatewayCommunicationBase.getBoxDomain().equals(boxDomain)) {
            tempBoxDomain = Urls.getBaseUrl();
        }
        String finalBoxDomain = tempBoxDomain;
        try {
            ThreadPool.getInstance().execute(() -> UserInfoManager.getMemberList(finalBoxDomain, accessToken, secret, ivParams, API_VERSION, new IAccountInfoCallback() {
                @Override
                public void onResult(AccountInfoResult result) {
                    Logger.i(TAG, "on result: " + result);
                    if (result == null) {
                        if (callback != null) {
                            callback.onFailed("-1", "", "");
                        }
                    } else {
                        if (callback != null) {
                            callback.onSuccess(result.getCode(), result.getMessage(), result.getRequestId()
                                    , boxUuid, result.getResults());
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
            }), isFore);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void getTerminalList(@NonNull Context context, String boxUuid, String boxDomain, String aoId, String accessToken, String secret, String ivParams, boolean isLAN, TerminalListCallback callback) {
        getTerminalList(context, boxUuid, boxDomain, aoId, accessToken, secret, ivParams, isLAN, false, callback);
    }

    public static void getTerminalList(@NonNull Context context, String boxUuid, String boxDomain, String aoId, String accessToken, String secret, String ivParams, boolean isLAN, boolean isFore, TerminalListCallback callback) {
        String finalBoxDomain = Urls.getBaseUrl();
        try {
            ThreadPool.getInstance().execute(() -> UserInfoManager.getTerminalList(finalBoxDomain, aoId, accessToken, secret, ivParams, API_VERSION, new ITerminalListCallback() {
                @Override
                public void onResult(TerminalListResponse result) {
                    Logger.i(TAG, "on result: " + result);
                    if (result == null) {
                        if (callback != null) {
                            callback.onFail(-1, "", "");
                        }
                    } else {
                        if (callback != null) {
                            callback.onSuccess(DataUtil.stringCodeToInt(result.getCode()), result.getMessage(), result.getRequestId(), result.getResults());
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
            }), isFore);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void offlineTerminal(@NonNull Context context, String boxUuid, String boxDomain, String aoId, String clientUuid, String accessToken, String secret, String ivParams, boolean isLAN, TerminalOfflineCallback callback) {
        offlineTerminal(context, boxUuid, boxDomain, aoId, clientUuid, accessToken, secret, ivParams, isLAN, false, callback);
    }

    public static void offlineTerminal(@NonNull Context context, String boxUuid, String boxDomain, String aoId, String clientUuid, String accessToken, String secret, String ivParams, boolean isLAN, boolean isFore, TerminalOfflineCallback callback) {
        String finalBoxDomain = Urls.getBaseUrl();
        try {
            ThreadPool.getInstance().execute(() -> UserInfoManager.offlineTerminal(aoId, clientUuid, finalBoxDomain, accessToken, secret, ivParams, API_VERSION, new ITerminalOfflineCallback() {
                @Override
                public void onResult(TerminalOfflineResponse result) {
                    Logger.i(TAG, "on result: " + result);
                    if (result == null) {
                        if (callback != null) {
                            callback.onFail(-1, "", "");
                        }
                    } else {
                        if (callback != null) {
                            callback.onSuccess(DataUtil.stringCodeToInt(result.getCode()), result.getMessage(), result.getRequestId(), result.getResults(), DataUtil.stringCodeGetSource(result.getCode()));
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
            }));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void offlineTerminal(String boxDomain, String aoId, String clientUuid, String accessToken, String secret, String ivParams, TerminalOfflineCallback callback) {
        try {
            ThreadPool.getInstance().execute(() -> UserInfoManager.offlineTerminal(aoId, clientUuid, boxDomain, accessToken, secret, ivParams, API_VERSION, new ITerminalOfflineCallback() {
                @Override
                public void onResult(TerminalOfflineResponse result) {
                    Logger.i(TAG, "on result: " + result);
                    if (result == null) {
                        if (callback != null) {
                            callback.onFail(-1, "", "");
                        }
                    } else {
                        if (callback != null) {
                            callback.onSuccess(DataUtil.stringCodeToInt(result.getCode()), result.getMessage(), result.getRequestId(), result.getResults(), DataUtil.stringCodeGetSource(result.getCode()));
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
            }));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    // server exception handle
    public static void updateMemberName(@NonNull Context context, String boxUuid, String aoId, String nickname, String boxDomain, String accessToken, String secret, String ivParams, boolean isLAN, MemberNameUpdateCallback callback) {
        if (aoId != null && nickname != null) {
            MemberNameUpdateInfo memberNameUpdateInfo = new MemberNameUpdateInfo();
            memberNameUpdateInfo.setAoId(aoId);
            memberNameUpdateInfo.setNickname(nickname);
            String finalBoxDomain = Urls.getBaseUrl();
            try {
                ThreadPool.getInstance().execute(() -> UserInfoManager.updateMemberName(memberNameUpdateInfo, finalBoxDomain, accessToken, secret, ivParams, API_VERSION, new IMemberNameUpdateCallback() {
                    @Override
                    public void onResult(MemberNameUpdateResult result) {
                        Logger.i(TAG, "on result: " + result);
                        boolean isSuccess = false;
                        String code = "";
                        String message = "";
                        String requestId = "";
                        if (result != null) {
                            code = result.getCode();
                            if (code.contains(NICK_FORMAT_ERROR_CODE)) {
                                isSuccess = false;
                                message = NICK_FORMAT_ERROR_CODE;
                            } else if (code.contains(NICK_REPEAT_ERROR_CODE)) {
                                isSuccess = false;
                                message = NICK_REPEAT_ERROR_CODE;
                            } else {
                                message = result.getMessage();
                                requestId = result.getRequestId();
                                MemberNameUpdateInfo info = result.getMemberNameUpdateInfo();
                                if (info != null) {
                                    String aoID = info.getAoId();
                                    String nickName = info.getNickname();
                                    if (aoID != null && aoID.equals(aoId)
                                            && nickName != null && nickName.equals(nickname)) {
                                        isSuccess = true;
                                    }
                                }
                            }
                        }
                        if (isSuccess) {
                            if (callback != null) {
                                callback.onSuccess(code, message, requestId);
                            }
                        } else {
                            if (callback != null) {
                                callback.onFailed(code, message, requestId);
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
                }), true);
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    // server exception handle
    public static void createMember(String boxPubKey, String clientUuid, String inviteCode, String secretKey, String nickname, String phoneModel, String boxDomain, String aoId, MemberCreateCallback callback) {
        createMember(boxPubKey, clientUuid, inviteCode, secretKey, nickname, phoneModel, null, boxDomain, aoId, callback);
    }

    public static void createMember(String boxPubKey, String clientUuid, String inviteCode, String secretKey, String nickname, String phoneModel, String applyEmail, String boxDomain, String aoId, MemberCreateCallback callback) {
        Logger.d(TAG, "create member secret: " + secretKey);
        MemberCreateInfo memberCreateInfo = new MemberCreateInfo();
        memberCreateInfo.setClientUuid(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.RSA_ECB_PKCS1
                , null, clientUuid, boxPubKey, null, null));
        memberCreateInfo.setInviteCode(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.RSA_ECB_PKCS1
                , null, inviteCode, boxPubKey, null, null));
        memberCreateInfo.setTempEncryptedSecret(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.RSA_ECB_PKCS1
                , null, secretKey, boxPubKey, null, null));
        memberCreateInfo.setNickname(nickname);
        memberCreateInfo.setPhoneModel(phoneModel);
        memberCreateInfo.setPhoneType("android");
        if (applyEmail != null) {
            memberCreateInfo.setApplyEmail(applyEmail);
        }
        Logger.d(TAG, "member create info: " + memberCreateInfo);
        UserInfoManager.createMember(memberCreateInfo, boxDomain, aoId, new IMemberCreateCallback() {
            @Override
            public void onResult(MemberCreateResult result) {
                Logger.i(TAG, "on result: " + result);
                TransportationConfig transportationConfig = null;
                if (result != null) {
                    if (result.getCodeInt().equals(ConstantField.ErrorCode.SPACE_PLATFORM_CONNECT_ERROR)){
                        if (callback != null) {
                            callback.onError(String.valueOf(ConstantField.ErrorCode.SPACE_PLATFORM_CONNECT_ERROR));
                        }
                        return;
                    }
                    AlgorithmConfig algorithmConfig = result.getAlgorithmConfig();
                    if (algorithmConfig != null) {
                        transportationConfig = algorithmConfig.getTransportation();
                    }
                }
                if (transportationConfig == null) {
                    if (callback != null) {
                        callback.onFailed((result == null ? null : result.getCode()), (result == null ? null : result.getMessage()));
                    }
                } else {
                    String transformation = transportationConfig.getTransformation();
                    String initializationVector = transportationConfig.getInitializationVector();
                    if (callback != null) {
                        callback.onSuccess(EncryptionUtil.decrypt(transformation, null
                                , result.getBoxUuid(), secretKey, StandardCharsets.UTF_8
                                , initializationVector), EncryptionUtil.decrypt(transformation
                                , null, result.getAuthKey(), secretKey, StandardCharsets.UTF_8
                                , initializationVector), result.getUserDomain(), result.getCode(), result.getMessage());
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

    /**
     * 成员获取token，需要使用domain
     *
     * @param context
     * @param boxUuid
     * @param boxBindValue
     * @param boxPubKey
     * @param authKey
     * @param clientUuid
     * @param secretKey
     * @param boxDomain
     * @param isLAN
     * @param callback
     */
    public static void createMemberToken(@NonNull Context context, String boxUuid, String boxBindValue, String boxPubKey, String authKey, String clientUuid, String secretKey, String boxDomain, boolean isLAN, boolean isFore, CreateAuthTokenCallback callback) {
        Logger.d(TAG, "create member token secret: " + secretKey);
        CreateMemberTokenInfo createMemberTokenInfo = new CreateMemberTokenInfo();
        createMemberTokenInfo.setEncryptedAuthKey(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.RSA_ECB_PKCS1
                , null, authKey, boxPubKey, null, null));
        createMemberTokenInfo.setEncryptedClientUUID(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.RSA_ECB_PKCS1
                , null, clientUuid, boxPubKey, null, null));
        createMemberTokenInfo.setTempEncryptedSecret(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.RSA_ECB_PKCS1
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
            ThreadPool.getInstance().execute(() -> UserInfoManager.createMemberToken(createMemberTokenInfo, finalBoxDomain, new ICreateAuthTokenCallback() {
                @Override
                public void onResult(CreateTokenResult result) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Logger.i(TAG, "on result: " + result);
                        AlgorithmConfig algorithmConfig = null;
                        TransportationConfig transportationConfig = null;
                        if (result != null) {
                            algorithmConfig = result.getAlgorithmConfig();
                            if (algorithmConfig != null) {
                                transportationConfig = algorithmConfig.getTransportation();
                            }
                        }
                        if (result != null && algorithmConfig != null && transportationConfig != null) {
                            String transformation = transportationConfig.getTransformation();
                            String initializationVector = transportationConfig.getInitializationVector();
                            if (callback != null) {
                                callback.onSuccess(boxUuid, boxBindValue, result.getAccessToken(), transformation
                                        , initializationVector, EncryptionUtil.decrypt(transformation
                                                , null, result.getEncryptedSecret(), secretKey, StandardCharsets.UTF_8
                                                , initializationVector), result.getExpiresAt(), result.getExpiresAtEpochSeconds()
                                        , result.getRefreshToken(), result.getRequestId());
                            }
                        } else {
                            if (finalIsLAN) {
                                createMemberToken(context, boxUuid, boxBindValue, boxPubKey, authKey, clientUuid, secretKey, domainUrl, false, isFore, callback);
                            } else if (callback != null) {
                                callback.onFailed();
                            }
                        }
                    });
                }

                @Override
                public void onError(int code, String msg) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Logger.e(TAG, "on error: " + msg);
                        if (finalIsLAN) {
                            createMemberToken(context, boxUuid, boxBindValue, boxPubKey, authKey, clientUuid, secretKey, domainUrl, false, isFore, callback);
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

    public static String getLocalHeaderPath(Context context) {
        return PreferenceUtil.getHeaderPath(context);
    }

    public static String getNickname(Context context) {
        String nickname = null;
        UserInfo userInfo = EulixSpaceDBUtil.getCompatibleActiveUserInfo(context);
        if (userInfo != null) {
            nickname = userInfo.getNickName();
        }
        return StringUtil.nullToEmpty(nickname);
    }

    public static String getSignature(Context context) {
        String signature = null;
        UserInfo userInfo = EulixSpaceDBUtil.getCompatibleActiveUserInfo(context);
        if (userInfo != null) {
            signature = userInfo.getSignature();
        }
        return StringUtil.nullToEmpty(signature);
    }

    public static String getLocalHeaderPath(Context context, String clientUuid) {
        String headerPath = null;
        if (clientUuid != null) {
            UserInfo userInfo = EulixSpaceDBUtil.getCompatibleActiveUserInfo(context);
            if (userInfo != null) {
                headerPath = userInfo.getAvatarPath();
            }
        }
        return StringUtil.nullToEmpty(headerPath);
    }

    public static void updateUserInfoDB(Context context, String boxUuid, String boxBind, String clientUuid, Map<String, String> userMap) {
        if (userMap != null && boxUuid != null && clientUuid != null) {
            String userInfoValue = null;
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, queryMap);
            if (boxValues != null) {
                for (Map<String, String> boxV : boxValues) {
                    if (boxV != null && boxV.containsKey(EulixSpaceDBManager.FIELD_BOX_USER_INFO)) {
                        userInfoValue = boxV.get(EulixSpaceDBManager.FIELD_BOX_USER_INFO);
                        break;
                    }
                }
            }
            Map<String, UserInfo> userInfoMap = null;
            if (userInfoValue != null) {
                try {
                    userInfoMap = new Gson().fromJson(userInfoValue, new TypeToken<Map<String, UserInfo>>() {
                    }.getType());
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
            if (userInfoMap != null) {
                UserInfo userInfo = null;
                if (userInfoMap.containsKey(clientUuid)) {
                    userInfo = userInfoMap.get(clientUuid);
                }
                if (userInfo != null) {
                    if (userMap.containsKey(AVATAR_PATH)) {
                        userInfo.setAvatarPath(userMap.get(AVATAR_PATH));
                    }
                    if (userMap.containsKey(NICKNAME)) {
                        userInfo.setNickName(userMap.get(NICKNAME));
                    }
                    if (userMap.containsKey(SIGNATURE)) {
                        userInfo.setSignature(userMap.get(SIGNATURE));
                    }
                    userInfoMap.put(clientUuid, userInfo);
                    Map<String, String> boxValue = new HashMap<>();
                    boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                    boxValue.put(EulixSpaceDBManager.FIELD_BOX_USER_INFO, new Gson().toJson(userInfoMap, new TypeToken<Map<String, UserInfo>>() {
                    }.getType()));
                    EulixSpaceDBUtil.updateBox(context, boxValue);
                }
            }
        }
    }

    // server exception handle
    //成员解绑
    public static void revokeMember(Context context, boolean isAdmin, String password, RevokeResultExtensionCallback resultCallback) {
        ThreadPool.getInstance().execute(() -> {
            try {
                boolean isFindSpace = false;
                String clientUuid = DataUtil.getClientUuid(context.getApplicationContext());
                String authKey = null;
                String boxPublicKeyValue = null;
                String boxDomain = null;
                String boxUuid = null;
                String boxBind = null;

                List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
                if (boxValues == null || boxValues.isEmpty()) {
                    boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                            , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
                }
                if (boxValues != null && boxValues.size() > 0) {
                    isFindSpace = true;
                    Map<String, String> boxValue = boxValues.get(0);
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)
                            && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_DOMAIN)
                            && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY)) {

                        boxDomain = boxValue.get(EulixSpaceDBManager.FIELD_BOX_DOMAIN);
                        authKey = boxValue.get(EulixSpaceDBManager.FIELD_BOX_AUTHORIZATION);
                        boxPublicKeyValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY);
                        boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                        boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    }
                }

                final String boxUuidFinal = boxUuid;
                final String boxBindFinal = boxBind;

                if (isFindSpace && !TextUtils.isEmpty(clientUuid)) {

                    Logger.d("zfy", "authKey=" + authKey);
                    Logger.d("zfy", "clientUuid=" + clientUuid);

                    CreateTokenInfo createTokenInfo = new CreateTokenInfo();
                    createTokenInfo.setEncryptedAuthKey(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.RSA_ECB_PKCS1
                            , null, authKey, boxPublicKeyValue, null, null));
                    createTokenInfo.setEncryptedClientUUID(EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.RSA_ECB_PKCS1
                            , null, clientUuid, boxPublicKeyValue, null, null));
                    if (isAdmin) {
                        String encryptPwd = EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.RSA_ECB_PKCS1
                                , null, password, boxPublicKeyValue, null, null);
                        Logger.d("zfy", "pwd=" + password + ";encryptPwd=" + encryptPwd);
                        createTokenInfo.setEncryptedPasscode(encryptPwd);
                    }
                    String baseUrl = Urls.getBaseUrl();
                    UserInfoManager.revokeDevice(createTokenInfo, baseUrl, isAdmin, new IRevokeResultExtensionCallback() {
                        @Override
                        public void onResult(RevokeMemberResponseBody result) {
                            RevokeMemberResponseBody.Results results = null;
                            String source = null;
                            int code = -1;
                            String message = null;
                            if (result != null) {
                                if (result.getCodeInt() == ConstantField.ErrorCode.SPACE_PLATFORM_CONNECT_ERROR) {
                                    if (resultCallback != null) {
                                        resultCallback.onError(String.valueOf(ConstantField.ErrorCode.SPACE_PLATFORM_CONNECT_ERROR));
                                    }
                                    return;
                                }
                                results = result.getResults();
                                String codeValue = result.getCode();
                                source = DataUtil.stringCodeGetSource(codeValue);
                                code = DataUtil.stringCodeToInt(codeValue);
                                message = result.getMessage();
                            }
                            if (results != null) {
                                if (resultCallback != null) {
                                    resultCallback.onSuccess(source, code, boxUuidFinal, boxBindFinal, message, results.succeed);
                                }
                            } else if (resultCallback != null) {
                                resultCallback.onFailed(source, code, boxUuidFinal, boxBindFinal, message);
                            }
                        }

                        @Override
                        public void onError(String errMsg) {
                            if (resultCallback != null) {
                                resultCallback.onError(errMsg);
                            }
                        }
                    });
                } else {
                    resultCallback.onError("base info error");
                }
            } catch (Exception e) {
                e.printStackTrace();
                resultCallback.onError("base info error");
            }
        }, true);
    }

    public static void changeBox(String boxUuid, String boxBind, String boxDomain, boolean isForce) {
        boolean isChange = isForce;
        if (activeBoxUuid == null || !activeBoxUuid.equals(boxUuid) || activeBoxBind == null || !activeBoxBind.equals(boxBind)) {
            activeBoxUuid = boxUuid;
            activeBoxBind = boxBind;
            isChange = true;
        }
        StorageInfoRequestEvent storageInfoRequestEvent = new StorageInfoRequestEvent(boxUuid, boxBind, boxDomain);
        EventBusUtil.post(storageInfoRequestEvent);
        MemberListEvent memberListEvent = new MemberListEvent(boxUuid, boxBind, boxDomain, isChange);
        EventBusUtil.post(memberListEvent);
        TerminalListEvent terminalListEvent = new TerminalListEvent(boxUuid, boxBind);
        EventBusUtil.post(terminalListEvent);
    }
}
