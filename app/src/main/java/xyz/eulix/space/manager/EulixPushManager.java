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

package xyz.eulix.space.manager;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.EulixSpaceService;
import xyz.eulix.space.bean.EulixBoxOtherInfo;
import xyz.eulix.space.bean.EulixBoxToken;
import xyz.eulix.space.bean.EulixPushReserveBean;
import xyz.eulix.space.bean.EulixPushReserveRecord;
import xyz.eulix.space.bean.EulixSpaceExtendInfo;
import xyz.eulix.space.bean.EulixSpaceInfo;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.bean.GranterAuthorizationBean;
import xyz.eulix.space.bean.GranterSecurityAuthenticationBean;
import xyz.eulix.space.bean.PushBean;
import xyz.eulix.space.bean.TerminalInfo;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.event.BoxOnlineEvent;
import xyz.eulix.space.event.BoxSystemRestartEvent;
import xyz.eulix.space.event.EulixNotificationEvent;
import xyz.eulix.space.event.EulixPushEvent;
import xyz.eulix.space.event.PlatformAbilityRequestEvent;
import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.network.gateway.SpacePollResult;
import xyz.eulix.space.network.notification.EulixNotificationUtil;
import xyz.eulix.space.network.notification.GetNotificationCallback;
import xyz.eulix.space.network.notification.GetNotificationResult;
import xyz.eulix.space.network.notification.NotificationAllCallback;
import xyz.eulix.space.network.notification.PageInfo;
import xyz.eulix.space.network.push.LoginBean;
import xyz.eulix.space.network.push.LoginConfirmBean;
import xyz.eulix.space.network.push.PushMessage;
import xyz.eulix.space.network.push.SecurityApplyBean;
import xyz.eulix.space.network.security.SecurityTokenResult;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.DebugUtil;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.GatewayUtils;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.ThreadPool;
import xyz.eulix.space.util.Urls;
import xyz.eulix.space.util.network.OkHttpUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/5/5 18:35
 */
public class EulixPushManager {
    private static final String TAG = EulixPushManager.class.getSimpleName();
    public static final String STRONG = "strong";
    public static final String WEAK = "weak";
    private static EulixPushManager eulixPushManager = new EulixPushManager();
    private static final String MESSAGE_ID = "message_id";
    private static final String REQUEST_ID = "request_id";
    private static final int SECOND_UNIT = 1000;
    private static final int KEEP_ALIVE_CONNECT = 1;
    private static final int REQUEST_DETAIL_MESSAGE = KEEP_ALIVE_CONNECT + 1;
    private static final int REFRESH_NOTIFICATION_OFFLINE = REQUEST_DETAIL_MESSAGE + 1;
    private boolean isLocked;
    private boolean isCancel;
    private Map<String, PushTempValueBean> pushTempValueBeanMap = new HashMap<>();
    private EulixPushHandler mHandler;
    private OkHttpClient mOkHttpClient;
    private Call mCall;
    private EulixSpaceInfo mEulixSpaceInfo;
    private boolean lastHeart = false;

    private EulixPushManager() {
        mHandler = new EulixPushHandler(this);
        mOkHttpClient = OkHttpUtil.generateOkHttpClient(false).newBuilder()
                .connectTimeout(40, TimeUnit.SECONDS)
                .readTimeout(40, TimeUnit.SECONDS)
                .writeTimeout(40, TimeUnit.SECONDS)
                .build();
        isLocked = false;
        mEulixSpaceInfo = new EulixSpaceInfo();
    }

    public static EulixPushManager getInstance() {
        return eulixPushManager;
    }

    static class EulixPushHandler extends Handler {
        private WeakReference<EulixPushManager> eulixPushManagerWeakReference;

        public EulixPushHandler(EulixPushManager manager) {
            eulixPushManagerWeakReference = new WeakReference<>(manager);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            EulixPushManager manager = eulixPushManagerWeakReference.get();
            if (manager == null) {
                super.handleMessage(msg);
            } else {
                Bundle data = msg.peekData();
                switch (msg.what) {
                    case KEEP_ALIVE_CONNECT:
                        if (manager.isLocked) {
                            sendEmptyMessageDelayed(KEEP_ALIVE_CONNECT, (manager.isCancel ? 2000 : 3500));
                        } else {
                            manager.keepAliveConnect();
                        }
                        break;
                    case REQUEST_DETAIL_MESSAGE:
                        if (data != null) {
                            String messageId = data.getString(MESSAGE_ID, "");
                            String requestId = data.getString(REQUEST_ID, "");
                            manager.getNotification(messageId, requestId, (msg.arg1 != 0));
                        }
                        break;
                    case REFRESH_NOTIFICATION_OFFLINE:
                        if (DataUtil.isStartLauncher()) {
                            manager.refreshNotificationOffline();
                        } else {
                            sendEmptyMessageDelayed(REFRESH_NOTIFICATION_OFFLINE, SECOND_UNIT);
                        }
                        break;
                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        }
    }

    private String generateBaseUrl(String boxDomain) {
        String baseUrl = boxDomain;
        if (baseUrl == null) {
            baseUrl = DebugUtil.getEnvironmentServices();
        } else {
            while ((baseUrl.startsWith(":") || baseUrl.startsWith("/")) && baseUrl.length() > 1) {
                baseUrl = baseUrl.substring(1);
            }
            if (TextUtils.isEmpty(baseUrl)) {
                baseUrl = DebugUtil.getEnvironmentServices();
            } else {
                if (!(baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))) {
                    baseUrl = "https://" + baseUrl;
                }
                if (!baseUrl.endsWith("/")) {
                    baseUrl = baseUrl + "/";
                }
            }
        }
        return baseUrl;
    }

    public void resetAliveConnect(EulixSpaceInfo eulixSpaceInfo, boolean isHeart) {
        if (!isHeart) {
            mEulixSpaceInfo.setBoxUuid(null);
            mEulixSpaceInfo.setBoxBind(null);
        }
        if (eulixSpaceInfo != null) {
            String newBoxUuid = eulixSpaceInfo.getBoxUuid();
            String newBoxBind = eulixSpaceInfo.getBoxBind();
            if ((lastHeart != isHeart) || ((newBoxUuid != null && !newBoxUuid.equals(mEulixSpaceInfo.getBoxUuid()))
                    || (newBoxBind != null && !newBoxBind.equals(mEulixSpaceInfo.getBoxBind())))) {
                if (isHeart) {
                    mEulixSpaceInfo.setBoxUuid(newBoxUuid);
                    mEulixSpaceInfo.setBoxBind(newBoxBind);
                }
                refreshAliveConnect(isHeart ? 0 : -1);
            }
        }
        lastHeart = isHeart;
    }

    private void refreshAliveConnect(int delay) {
        if (mHandler != null) {
            while (mHandler.hasMessages(KEEP_ALIVE_CONNECT)) {
                mHandler.removeMessages(KEEP_ALIVE_CONNECT);
            }
            if (mCall != null && isLocked) {
                isCancel = true;
                mCall.cancel();
            }
            if (delay > 0) {
                mHandler.sendEmptyMessageDelayed(KEEP_ALIVE_CONNECT, delay);
            } else if (delay == 0) {
                mHandler.sendEmptyMessage(KEEP_ALIVE_CONNECT);
            } else if (mEulixSpaceInfo != null) {
                mEulixSpaceInfo.setBoxUuid(null);
                mEulixSpaceInfo.setBoxBind(null);
            }
        }
    }

    private void keepAliveConnect() {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(EulixSpaceApplication.getContext(), true);
        if (gatewayCommunicationBase != null) {
            String boxUuid = gatewayCommunicationBase.getBoxUuid();
            String boxBind = gatewayCommunicationBase.getBoxBind();
            if (mEulixSpaceInfo != null) {
                mEulixSpaceInfo.setBoxUuid(boxUuid);
                mEulixSpaceInfo.setBoxBind(boxBind);
            }
            String transformation = gatewayCommunicationBase.getTransformation();
            String secret = gatewayCommunicationBase.getSecretKey();
            String ivParams = gatewayCommunicationBase.getIvParams();
            String baseUrl = Urls.getBaseUrl();
            HttpUrl httpParseUrl = HttpUrl.parse((generateBaseUrl(baseUrl) + ConstantField.URL.SPACE_POLL_API));
            if (httpParseUrl != null) {
                String requestId = UUID.randomUUID().toString();
                HttpUrl httpUrl = httpParseUrl.newBuilder()
                        .addQueryParameter("accessToken", gatewayCommunicationBase.getAccessToken())
                        .addQueryParameter("count", String.valueOf(1))
                        .build();
                Request request = new Request.Builder()
                        .url(httpUrl)
                        .addHeader("Request-Id", requestId)
                        .get()
                        .build();
                // 日志驻点
                Log.d(TAG, "eulix push request: " + httpParseUrl.toString() + ", request id: " + requestId);
                mCall = mOkHttpClient.newCall(request);
                isLocked = true;
                isCancel = false;
                try {
                    ThreadPool.getInstance().execute(() -> mCall.enqueue(new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                            // 日志驻点
                            Log.d(TAG, "eulix push response on failure");
                            Logger.e(TAG, "on failure: " + e.getMessage());
                            mHandler.post(() -> {
                                isLocked = false;
                                if (isCancel) {
                                    isCancel = false;
                                } else {
                                    String errMsg = e.getMessage();
                                    refreshAliveConnect((errMsg != null && errMsg.contains("timeout")) ? 0 : 30000);
                                }
                            });
                        }

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                            Logger.i(TAG, "on response: " + response.toString());
                            int code = response.code();
                            // 日志驻点
                            Log.d(TAG, "eulix push response on response: " + code);
                            int delay = 15000;
                            if (code < 300) {
                                delay = 0;
                            } else if (code < 400) {
                                delay = 5000;
                            } else if (code < 500) {
                                delay = 10000;
                            }
                            ResponseBody responseBody = response.body();
                            if (responseBody != null) {
                                String content = null;
                                try {
                                    content = responseBody.string();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                Logger.i(TAG, "push content: " + content);
                                if (content != null) {
                                    SpacePollResult spacePollResult = null;
                                    try {
                                        spacePollResult = new Gson().fromJson(content, SpacePollResult.class);
                                    } catch (JsonSyntaxException e) {
                                        e.printStackTrace();
                                        delay = 30000;
                                    }
                                    handlePollMessage(spacePollResult, boxUuid, boxBind, transformation, secret, ivParams);
                                }
                            }
                            int finalDelay = delay;
                            mHandler.post(() -> {
                                isLocked = false;
                                refreshAliveConnect(finalDelay);
                            });
                        }
                    }));
                } catch (RejectedExecutionException e) {
                    e.printStackTrace();
                }
            }
        } else {
            lastHeart = false;
            if (mEulixSpaceInfo != null) {
                mEulixSpaceInfo.setBoxUuid(null);
                mEulixSpaceInfo.setBoxBind(null);
            }
        }
    }

    private void getNotification(String messageId, String requestId, boolean isOffline) {
        if (messageId != null && requestId != null) {
            GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(EulixSpaceApplication.getContext(), true);
            if (gatewayCommunicationBase != null) {
                EulixNotificationUtil.getNotification(requestId, gatewayCommunicationBase.getBoxDomain()
                        , gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                        , gatewayCommunicationBase.getIvParams(), messageId, new GetNotificationCallback() {
                            @Override
                            public void onSuccess(int code, String message, String requestId, GetNotificationResult result) {
                                if (requestId != null && pushTempValueBeanMap != null && pushTempValueBeanMap.containsKey(requestId)) {
                                    PushTempValueBean pushTempValueBean = pushTempValueBeanMap.get(requestId);
                                    if (pushTempValueBean != null) {
                                        PushBean pushBean = pushTempValueBean.getPushBean();
                                        Map<String, String> pushValue = pushTempValueBean.getPushValue();
                                        if (pushBean != null && pushValue != null) {
                                            String optType = pushBean.getType();
                                            String boxUuid = pushBean.getBoxUuid();
                                            String boxBind = pushBean.getBoxBind();
                                            if (boxUuid != null && boxBind != null) {
                                                String data = result.getData();
                                                Boolean isRealUpdate = saveNotificationRecord(optType, boxUuid, boxBind, result.getMessageId(), data, FormatUtil.parseFileApiTimestamp(result.getCreateAt()
                                                        , ConstantField.TimeStampFormat.FILE_API_DAY_FORMAT, ConstantField.TimeStampFormat.FILE_API_SPLIT));
                                                if (!isOffline || isRealUpdate == null || isRealUpdate) {
                                                    boolean isShow = isPushShow(optType, boxUuid, boxBind, data);
                                                    Boolean isUpdate = null;
                                                    Map<String, String> queryMap = new HashMap<>();
                                                    queryMap.put(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID, messageId);
                                                    if (!TextUtils.isEmpty(boxUuid) && !TextUtils.isEmpty(boxBind)) {
                                                        queryMap.put(EulixSpaceDBManager.FIELD_PUSH_UUID, boxUuid);
                                                        queryMap.put(EulixSpaceDBManager.FIELD_PUSH_BIND, boxBind);
                                                    }
                                                    List<Map<String, String>> currentPushValues = EulixSpaceDBUtil.queryPush(EulixSpaceApplication.getContext(), queryMap);
                                                    if (currentPushValues != null && currentPushValues.size() > 0) {
                                                        for (Map<String, String> currentPushValue : currentPushValues) {
                                                            if (currentPushValue != null && currentPushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_SOURCE)
                                                                    && currentPushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_CONSUME)) {
                                                                String source = currentPushValue.get(EulixSpaceDBManager.FIELD_PUSH_SOURCE);
                                                                String consume = currentPushValue.get(EulixSpaceDBManager.FIELD_PUSH_CONSUME);
                                                                if ("1".equals(source)) {
                                                                    isUpdate = true;
                                                                } else if ("2".equals(consume) || "3".equals(consume) || "4".equals(consume)) {
                                                                    isUpdate = false;
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                    }
                                                    if (isUpdate == null || isUpdate) {
                                                        pushValue.put(EulixSpaceDBManager.FIELD_PUSH_SOURCE, "1");
                                                        pushValue.put(EulixSpaceDBManager.FIELD_PUSH_CONSUME, isShow ? "1" : "3");
                                                        pushValue.put(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA, StringUtil.nullToEmpty(data));
                                                        pushValue.put(EulixSpaceDBManager.FIELD_PUSH_CREATE_TIME, StringUtil.nullToEmpty(result.getCreateAt()));
                                                        pushValue.put(EulixSpaceDBManager.FIELD_PUSH_TIMESTAMP, String.valueOf(System.currentTimeMillis()));

                                                        if (isUpdate == null) {
                                                            EulixSpaceDBUtil.insertPush(EulixSpaceApplication.getContext(), pushValue);
                                                        } else {
                                                            EulixSpaceDBUtil.updatePush(EulixSpaceApplication.getContext(), pushValue);
                                                        }
                                                        if (isShow) {
                                                            EulixPushEvent eulixPushEvent = new EulixPushEvent(pushBean);
                                                            EventBusUtil.post(eulixPushEvent);
                                                            Logger.d(TAG, "push event");
                                                        }
                                                        EventBusUtil.post(new EulixNotificationEvent(messageId, optType, (isShow ? 1 : 3)));
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    try {
                                        pushTempValueBeanMap.remove(requestId);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            @Override
                            public void onFailed(int code, String message, String requestId) {
                                if (requestId != null && pushTempValueBeanMap != null) {
                                    try {
                                        pushTempValueBeanMap.remove(requestId);
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }

                            @Override
                            public void onError(String errMsg, String requestId) {
                                if (requestId != null && mHandler != null) {
                                    Message message = mHandler.obtainMessage(REQUEST_DETAIL_MESSAGE);
                                    Bundle data = new Bundle();
                                    data.putString(MESSAGE_ID, messageId);
                                    data.putString(REQUEST_ID, requestId);
                                    message.setData(data);
                                    mHandler.sendMessageDelayed(message, 10000);
                                }
                            }
                        });
            }
        }
    }

    /**
     * 根据具体消息确定消息优先级
     * @param optType
     * @return
     */
    private int generatePriority(String optType) {
        int priority = 32;
        if (optType != null) {
            switch (optType) {
                case ConstantField.PushType.LOGIN:
                    // 存在弱提醒，顶部dialog显示
                case ConstantField.PushType.BOX_START_UPGRADE:
                    priority = 16;
                    break;
                case ConstantField.PushType.LOGOUT:
                case ConstantField.PushType.REVOKE:
                case ConstantField.PushType.MEMBER_SELF_DELETE:
                case ConstantField.PushType.LOGIN_CONFIRM:
                case ConstantField.PushType.SECURITY_PASSWORD_MODIFY_APPLY:
                case ConstantField.PushType.SECURITY_PASSWORD_RESET_APPLY:
                case ConstantField.PushType.BOX_UPGRADE_PACKAGE_PULLED:
                case ConstantField.PushType.NativeType.TRIAL_INVALID:
                    // 存在强提醒，dialog或者跳转页面
                    priority = 8;
                    break;
                default:
                    break;
            }
        }
        return priority;
    }

    /**
     * 对必要的消息进行预处理
     * @param optType
     * @param boxUuid
     * @param boxBind
     *
     * @return 是否已消费 true-已消费，不进行后续处理；false-未消费，进行后续处理
     */
    private boolean handleMessage(String optType, String boxUuid, String boxBind, String data) {
        boolean hasConsumed = false;
        if (optType != null && boxUuid != null && !TextUtils.isEmpty(boxUuid) && boxBind != null && !TextUtils.isEmpty(boxBind)) {
            switch (optType) {
                case ConstantField.PushType.LOGOUT:
                case ConstantField.PushType.REVOKE:
                case ConstantField.PushType.MEMBER_SELF_DELETE:
                    if (inactiveSpace(boxUuid, boxBind)) {
                        refreshAccessToken(boxUuid, boxBind);
                    }
                    break;
                case ConstantField.PushType.ABILITY_CHANGE:
                    String platformServerUrl = DebugUtil.getEnvironmentServices();
                    if (platformServerUrl != null) {
                        Logger.d(TAG, "push ability change request platform ability: " + platformServerUrl);
                        EventBusUtil.post(new PlatformAbilityRequestEvent(platformServerUrl));
                    }
                    hasConsumed = true;
                    break;
                case ConstantField.PushType.BOX_SYSTEM_RESTART:
                    Logger.d("zfy", "manageBoxSystemRestartPush:" + data);
                    EventBusUtil.post(new BoxSystemRestartEvent());
                    hasConsumed = true;
                    break;
                default:
                    break;
            }
        }
        return hasConsumed;
    }


    /**
     * 对消息细节分析是否展示终判断，用于决定往数据库里写对应内容，通知中心、长连接必走
     * @param optType
     * @param boxUuid
     * @param boxBind
     * @param data
     * @return
     */
    private boolean isPushShow(String optType, String boxUuid, String boxBind, String data) {
        boolean isShow = true;
        boolean isKnown = false;
        if (optType != null) {
            switch (optType) {
                case ConstantField.PushType.APP_UPGRADE:
                case ConstantField.PushType.BOX_UPGRADE:
                    isKnown = true;
                    isShow = DataUtil.getSystemMessageEnable(EulixSpaceApplication.getContext());
                    break;
                case ConstantField.PushType.LOGIN:
                case ConstantField.PushType.LOGOUT:
                case ConstantField.PushType.REVOKE:
                case ConstantField.PushType.MEMBER_SELF_DELETE:
                case ConstantField.PushType.UPGRADE_SUCCESS:
                case ConstantField.PushType.SECURITY_PASSWORD_MODIFY_SUCCESS:
                case ConstantField.PushType.SECURITY_PASSWORD_RESET_SUCCESS:
                case ConstantField.PushType.BOX_UPGRADE_PACKAGE_PULLED:
                case ConstantField.PushType.BOX_START_UPGRADE:
                    isKnown = true;
                    isShow = DataUtil.getBusinessMessageEnable(EulixSpaceApplication.getContext());
                    break;
                case ConstantField.PushType.LOGIN_CONFIRM:
                    isKnown = true;
                    // 判断当前正在展示的免扫码
                    GranterAuthorizationBean granterAuthorizationBean = DataUtil.getProcessGranterAuthorizationBean();
                    if (granterAuthorizationBean != null) {
                        String granterBoxUuid = granterAuthorizationBean.getBoxUuid();
                        String granterBoxBind = granterAuthorizationBean.getBoxBind();
                        String loginClientUuid = granterAuthorizationBean.getLoginClientUuid();
                        if (granterBoxUuid != null && granterBoxBind != null && loginClientUuid != null) {
                            LoginConfirmBean loginConfirmBean = null;
                            if (data != null) {
                                try {
                                    loginConfirmBean = new Gson().fromJson(data, LoginConfirmBean.class);
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (loginConfirmBean != null && boxUuid != null && boxBind != null) {
                                isShow = !(boxUuid.equals(granterBoxUuid) && boxBind.equals(granterBoxBind)
                                        && loginClientUuid.equals(loginConfirmBean.getUuid()));
                            }
                        }
                    }
                    break;
                case ConstantField.PushType.SECURITY_PASSWORD_MODIFY_APPLY:
                case ConstantField.PushType.SECURITY_PASSWORD_RESET_APPLY:
                    isKnown = true;
                    SecurityApplyBean securityApplyBean = null;
                    if (data != null) {
                        try {
                            securityApplyBean = new Gson().fromJson(data, SecurityApplyBean.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                    boolean isApplyHandle = false;
                    if (securityApplyBean != null) {
                        SecurityTokenResult pushSecurityTokenResult = securityApplyBean.getSecurityTokenRes();
                        String pushApplyId = securityApplyBean.getApplyId();
                        if (pushSecurityTokenResult != null && pushSecurityTokenResult.getExpiredAt() != null) {
                            long securityTokenExpireTimestamp = -1;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                securityTokenExpireTimestamp = FormatUtil.parseZonedDateTime(pushSecurityTokenResult.getExpiredAt());
                            } else {
                                securityTokenExpireTimestamp = FormatUtil.parseZonedTimestamp(pushSecurityTokenResult.getExpiredAt()
                                        , ConstantField.TimeStampFormat.FILE_API_DAY_FORMAT, ConstantField.TimeStampFormat.FILE_API_SPLIT);
                            }
                            if (securityTokenExpireTimestamp < 0 || securityTokenExpireTimestamp > System.currentTimeMillis()) {
                                isApplyHandle = true;
                                // 判断当前展示的身份验证
                                GranterSecurityAuthenticationBean granterSecurityAuthenticationBean = DataUtil.getProcessGranterSecurityAuthenticationBean();
                                if (granterSecurityAuthenticationBean != null) {
                                    String messageType = granterSecurityAuthenticationBean.getMessageType();
                                    String granterBoxUuid = granterSecurityAuthenticationBean.getBoxUuid();
                                    String granterBoxBind = granterSecurityAuthenticationBean.getBoxBind();
                                    String authClientUuid = granterSecurityAuthenticationBean.getAuthClientUuid();
                                    if (optType.equals(messageType) && granterBoxUuid != null && granterBoxBind != null && authClientUuid != null) {
                                        if (boxUuid != null && boxBind != null) {
                                            boolean isSame = (boxUuid.equals(granterBoxUuid) && boxBind.equals(granterBoxBind)
                                                    && authClientUuid.equals(securityApplyBean.getAuthClientUUid()));
                                            if (isSame) {
                                                SecurityTokenResult currentSecurityTokenResult = granterSecurityAuthenticationBean.getSecurityTokenResult();
                                                boolean isUpdate = (securityTokenExpireTimestamp < 0 || currentSecurityTokenResult == null);
                                                if (!isUpdate) {
                                                    Boolean isUpdateValue = null;
                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                        isUpdateValue = FormatUtil.compareZonedDateTime(pushSecurityTokenResult.getExpiredAt(), currentSecurityTokenResult.getExpiredAt(), true);
                                                    }
                                                    if (isUpdateValue == null) {
                                                        long currentSecurityTokenExpireTimestamp = FormatUtil.parseZonedTimestamp(currentSecurityTokenResult.getExpiredAt()
                                                                , ConstantField.TimeStampFormat.FILE_API_DAY_FORMAT, ConstantField.TimeStampFormat.FILE_API_SPLIT);
                                                        isUpdate = (currentSecurityTokenExpireTimestamp <= securityTokenExpireTimestamp);
                                                    } else {
                                                        isUpdate = isUpdateValue;
                                                    }
                                                }
                                                if (isUpdate) {
                                                    granterSecurityAuthenticationBean.setSecurityTokenResult(pushSecurityTokenResult);
                                                    granterSecurityAuthenticationBean.setApplyId(pushApplyId);
                                                }
                                            }
                                            isShow = !isSame;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (!isApplyHandle) {
                        isShow = false;
                    }
                    break;
                default:
                    break;
            }
        }
        // 判断是否是当前的盒子
        if (isShow) {
            EulixSpaceInfo eulixSpaceInfo = DataUtil.getActiveOrLastEulixSpace(EulixSpaceApplication.getContext());
            isShow = (eulixSpaceInfo != null && boxUuid != null && boxUuid.equals(eulixSpaceInfo.getBoxUuid())
                    && boxBind != null && boxBind.equals(eulixSpaceInfo.getBoxBind()));
        }
        return (isShow && isKnown);
    }

    /**
     * 处理未展示的旧消息
     * @param boxUuid
     * @param boxBind
     * @param messageType
     * @param messageData
     */
    private void handleOldMessage(String boxUuid, String boxBind, String messageType, String messageData) {
        if (boxUuid != null && boxBind != null && messageType != null) {
            List<Map<String, String>> pushValues = EulixSpaceDBUtil.queryAppointTypePush(EulixSpaceApplication.getContext(), boxUuid, boxBind, messageType);
            if (pushValues != null) {
                for (Map<String, String> pushValue : pushValues) {
                    if (pushValue != null && pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_SOURCE)
                            && pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID)) {
                        String source = pushValue.get(EulixSpaceDBManager.FIELD_PUSH_SOURCE);
                        String dataValue = null;
                        if (pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA)) {
                            dataValue = pushValue.get(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA);
                        }
                        String messageId = pushValue.get(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID);
                        if (source != null && messageId != null) {
                            switch (messageType) {
                                case ConstantField.PushType.LOGIN:
                                    if (messageData != null && dataValue != null && "2".equals(source)) {
                                        LoginBean oldLoginBean = null;
                                        LoginBean newLoginBean = null;
                                        try {
                                            oldLoginBean = new Gson().fromJson(dataValue, LoginBean.class);
                                            newLoginBean = new Gson().fromJson(messageData, LoginBean.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (oldLoginBean != null && newLoginBean != null) {
                                            String oldUuid = oldLoginBean.getUuid();
                                            String newUuid = newLoginBean.getUuid();
                                            if (newUuid != null && newUuid.equals(oldUuid)) {
                                                Map<String, String> pushV = new HashMap<>();
                                                pushV.put(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID, messageId);
                                                pushV.put(EulixSpaceDBManager.FIELD_PUSH_UUID, StringUtil.nullToEmpty(boxUuid));
                                                pushV.put(EulixSpaceDBManager.FIELD_PUSH_BIND, StringUtil.nullToEmpty(boxBind));
                                                pushV.put(EulixSpaceDBManager.FIELD_PUSH_SOURCE, source);
                                                pushV.put(EulixSpaceDBManager.FIELD_PUSH_CONSUME, "3");
                                                EulixSpaceDBUtil.updatePush(EulixSpaceApplication.getContext(), pushV);
                                            }
                                        }
                                    }
                                    break;
                                case ConstantField.PushType.NativeType.TRIAL_INVALID:
                                    if ("0".equals(source)) {
                                        Map<String, String> pushV = new HashMap<>();
                                        pushV.put(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID, messageId);
                                        pushV.put(EulixSpaceDBManager.FIELD_PUSH_UUID, StringUtil.nullToEmpty(boxUuid));
                                        pushV.put(EulixSpaceDBManager.FIELD_PUSH_BIND, StringUtil.nullToEmpty(boxBind));
                                        pushV.put(EulixSpaceDBManager.FIELD_PUSH_SOURCE, source);
                                        pushV.put(EulixSpaceDBManager.FIELD_PUSH_CONSUME, "4");
                                        EulixSpaceDBUtil.updatePush(EulixSpaceApplication.getContext(), pushV);
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 通知中心接口来源消息
     * @param result
     * @param boxUuid
     * @param boxBind
     */
    private void handleNotificationAllMessage(GetNotificationResult result, String boxUuid, String boxBind) {
        if (result != null && boxUuid != null && boxBind != null) {
            String messageId = result.getMessageId();
            String messageType = result.getOptType();
            String data = result.getData();
            String createTime = result.getCreateAt();
            int priority = generatePriority(messageType);
            boolean isShow = isPushShow(messageType, boxUuid, boxBind, data);
            if (messageId != null) {
                Boolean isRealUpdate = saveNotificationRecord(messageType, boxUuid, boxBind, messageId, data, FormatUtil.parseFileApiTimestamp(createTime
                        , ConstantField.TimeStampFormat.FILE_API_DAY_FORMAT, ConstantField.TimeStampFormat.FILE_API_SPLIT));
                if (isRealUpdate == null || isRealUpdate) {
                    Boolean isUpdate = null;
                    Map<String, String> queryMap = new HashMap<>();
                    queryMap.put(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID, messageId);
                    queryMap.put(EulixSpaceDBManager.FIELD_PUSH_UUID, boxUuid);
                    queryMap.put(EulixSpaceDBManager.FIELD_PUSH_BIND, boxBind);
                    List<Map<String, String>> currentPushValues = EulixSpaceDBUtil.queryPush(EulixSpaceApplication.getContext(), queryMap);
                    if (currentPushValues != null && currentPushValues.size() > 0) {
                        for (Map<String, String> currentPushValue : currentPushValues) {
                            if (currentPushValue != null && currentPushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_SOURCE)
                                    && currentPushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_CONSUME)) {
                                String source = currentPushValue.get(EulixSpaceDBManager.FIELD_PUSH_SOURCE);
                                String consume = currentPushValue.get(EulixSpaceDBManager.FIELD_PUSH_CONSUME);
                                if ("2".equals(consume) || "3".equals(consume) || "4".equals(consume)) {
                                    isUpdate = false;
                                    break;
                                } else if ("3".equals(source)) {
                                    isUpdate = true;
                                }
                            }
                        }
                    }
                    if (isUpdate == null || isUpdate) {
                        Map<String, String> pushValue = new HashMap<>();
                        pushValue.put(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID, messageId);
                        pushValue.put(EulixSpaceDBManager.FIELD_PUSH_UUID, StringUtil.nullToEmpty(boxUuid));
                        pushValue.put(EulixSpaceDBManager.FIELD_PUSH_BIND, StringUtil.nullToEmpty(boxBind));
                        pushValue.put(EulixSpaceDBManager.FIELD_PUSH_TYPE, StringUtil.nullToEmpty(messageType));
                        pushValue.put(EulixSpaceDBManager.FIELD_PUSH_PRIORITY, String.valueOf(priority));
                        pushValue.put(EulixSpaceDBManager.FIELD_PUSH_SOURCE, "3");
                        pushValue.put(EulixSpaceDBManager.FIELD_PUSH_CONSUME, isShow ? (priority <= 16 ? "1" : "4") : "3");
                        pushValue.put(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA, StringUtil.nullToEmpty(data));
                        pushValue.put(EulixSpaceDBManager.FIELD_PUSH_CREATE_TIME, StringUtil.nullToEmpty(createTime));
                        pushValue.put(EulixSpaceDBManager.FIELD_PUSH_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
                        if (isUpdate == null) {
                            EulixSpaceDBUtil.insertPush(EulixSpaceApplication.getContext(), pushValue);
                        } else {
                            EulixSpaceDBUtil.updatePush(EulixSpaceApplication.getContext(), pushValue);
                        }
                        if (isShow) {
                            PushBean pushBean = new PushBean();
                            pushBean.setMessageId(messageId);
                            pushBean.setBoxUuid(boxUuid);
                            pushBean.setBoxBind(boxBind);
                            pushBean.setType(messageType);
                            pushBean.setPriority(priority);
                            pushBean.setSource(3);
                            pushBean.setRawData(data);
                            EulixPushEvent eulixPushEvent = new EulixPushEvent(pushBean);
                            EventBusUtil.post(eulixPushEvent);
                            Logger.d(TAG, "push event");
                        }
                        EventBusUtil.post(new EulixNotificationEvent(messageId, messageType, (isShow ? 1 : 3)));
                    }
                }
            }
        }
    }

    /**
     * 长连接来源消息
     * @param spacePollResult
     * @param boxUuid
     * @param boxBind
     * @param transformation
     * @param secretKey
     * @param ivParmas
     */
    private void handlePollMessage(SpacePollResult spacePollResult, String boxUuid, String boxBind, String transformation, String secretKey, String ivParmas) {
        if (spacePollResult != null) {
            String rawMessage = spacePollResult.getMessage();
            String decryptMessage = null;
            if (rawMessage != null) {
                decryptMessage = EncryptionUtil.decrypt(transformation, null, rawMessage, secretKey, StandardCharsets.UTF_8, ivParmas);
            }
            if (decryptMessage != null) {
                rawMessage = decryptMessage;
            }
            Logger.d(TAG, "push content decrypt: " + rawMessage);
            List<PushMessage> messages = null;
            if (rawMessage != null) {
                try {
                    messages = new Gson().fromJson(rawMessage, new TypeToken<List<PushMessage>>(){}.getType());
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
            if (messages != null) {
                for (PushMessage message : messages) {
                    if (message != null) {
                        String messageId = message.getMessageId();
                        String optType = message.getOptType();
                        String data = message.getData();
                        String createTime = message.getCreateAt();
                        int priority = generatePriority(optType);
                        boolean hasConsumed = handleMessage(optType, boxUuid, boxBind, data);
                        if (hasConsumed){
                            //该条推送已被消费，不进行后续处理
                            return;
                        }
                        boolean isShow = isPushShow(optType, boxUuid, boxBind, data);
                        if (messageId != null) {
                            saveNotificationRecord(optType, boxUuid, boxBind, messageId, data, FormatUtil.parseFileApiTimestamp(createTime
                                    , ConstantField.TimeStampFormat.FILE_API_DAY_FORMAT, ConstantField.TimeStampFormat.FILE_API_SPLIT));
                            handleOldMessage(boxUuid, boxBind, optType, data);
                            Boolean isUpdate = null;
                            Map<String, String> queryMap = new HashMap<>();
                            queryMap.put(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID, messageId);
                            if (boxUuid != null && boxBind != null) {
                                queryMap.put(EulixSpaceDBManager.FIELD_PUSH_UUID, boxUuid);
                                queryMap.put(EulixSpaceDBManager.FIELD_PUSH_BIND, boxBind);
                            }
                            List<Map<String, String>> currentPushValues = EulixSpaceDBUtil.queryPush(EulixSpaceApplication.getContext(), queryMap);
                            if (currentPushValues != null && currentPushValues.size() > 0) {
                                for (Map<String, String> currentPushValue : currentPushValues) {
                                    if (currentPushValue != null && currentPushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_SOURCE)
                                            && currentPushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_CONSUME)) {
                                        String source = currentPushValue.get(EulixSpaceDBManager.FIELD_PUSH_SOURCE);
                                        String consume = currentPushValue.get(EulixSpaceDBManager.FIELD_PUSH_CONSUME);
                                        if ("2".equals(source)) {
                                            isUpdate = true;
                                        } else if ("2".equals(consume) || "3".equals(consume) || "4".equals(consume)) {
                                            isUpdate = false;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (isUpdate == null || isUpdate) {
                                Map<String, String> pushValue = new HashMap<>();
                                pushValue.put(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID, messageId);
                                pushValue.put(EulixSpaceDBManager.FIELD_PUSH_UUID, StringUtil.nullToEmpty(boxUuid));
                                pushValue.put(EulixSpaceDBManager.FIELD_PUSH_BIND, StringUtil.nullToEmpty(boxBind));
                                pushValue.put(EulixSpaceDBManager.FIELD_PUSH_TYPE, StringUtil.nullToEmpty(optType));
                                pushValue.put(EulixSpaceDBManager.FIELD_PUSH_PRIORITY, String.valueOf(priority));
                                pushValue.put(EulixSpaceDBManager.FIELD_PUSH_SOURCE, "2");
                                pushValue.put(EulixSpaceDBManager.FIELD_PUSH_CONSUME, isShow ? (priority <= 16 ? "1" : "4") : "3");
                                pushValue.put(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA, StringUtil.nullToEmpty(data));
                                pushValue.put(EulixSpaceDBManager.FIELD_PUSH_CREATE_TIME, StringUtil.nullToEmpty(createTime));
                                pushValue.put(EulixSpaceDBManager.FIELD_PUSH_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
                                if (isUpdate == null) {
                                    EulixSpaceDBUtil.insertPush(EulixSpaceApplication.getContext(), pushValue);
                                } else {
                                    EulixSpaceDBUtil.updatePush(EulixSpaceApplication.getContext(), pushValue);
                                }
                                if (isShow) {
                                    PushBean pushBean = new PushBean();
                                    pushBean.setMessageId(messageId);
                                    pushBean.setBoxUuid(boxUuid);
                                    pushBean.setBoxBind(boxBind);
                                    pushBean.setType(optType);
                                    pushBean.setPriority(priority);
                                    pushBean.setSource(2);
                                    pushBean.setRawData(data);
                                    EulixPushEvent eulixPushEvent = new EulixPushEvent(pushBean);
                                    EventBusUtil.post(eulixPushEvent);
                                    Logger.d(TAG, "push event");
                                }
                                EventBusUtil.post(new EulixNotificationEvent(messageId, optType, (isShow ? 1 : 3)));
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 存入消息信息，用来过滤如通知中心、离线消息点击导致的消息滞后的过期或者处理过的消息
     * @param messageType
     * @param boxUuid
     * @param boxBind
     * @param messageId
     * @param data
     * @param messageCreateTime
     * @return
     */
    private Boolean saveNotificationRecord(String messageType, String boxUuid, String boxBind, String messageId, String data, long messageCreateTime) {
        // null: insert; true: update; false: do nothing
        Boolean isUpdate = null;
        if (boxUuid != null && boxBind != null && messageType != null) {
            EulixPushReserveBean eulixPushReserveBean = null;
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID, (boxUuid + "_" + boxBind));
            queryMap.put(EulixSpaceDBManager.FIELD_PUSH_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_PUSH_BIND, boxBind);
            queryMap.put(EulixSpaceDBManager.FIELD_PUSH_SOURCE, "-1");
            List<Map<String, String>> pushValues = EulixSpaceDBUtil.queryPush(EulixSpaceApplication.getContext(), queryMap);
            if (pushValues != null && pushValues.size() > 0) {
                isUpdate = true;
                for (Map<String, String> pushValue : pushValues) {
                    if (pushValue != null && pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_RESERVE)) {
                        String pushReserveValue = pushValue.get(EulixSpaceDBManager.FIELD_PUSH_RESERVE);
                        if (pushReserveValue != null && !TextUtils.isEmpty(pushReserveValue)) {
                            try {
                                eulixPushReserveBean = new Gson().fromJson(pushReserveValue, EulixPushReserveBean.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }
                }
            }
            if (eulixPushReserveBean == null) {
                eulixPushReserveBean = new EulixPushReserveBean();
            }
            Map<String, Map<String, String>> allRecordMap = eulixPushReserveBean.getTypeRecentRecordMap();
            if (allRecordMap == null) {
                allRecordMap = new HashMap<>();
            }
            Map<String, String> recordMap = null;
            if (allRecordMap.containsKey(messageType)) {
                recordMap = allRecordMap.get(messageType);
            }
            if (recordMap == null) {
                recordMap = new HashMap<>();
            }
            String keyUuid = null;
            switch (messageType) {
                case ConstantField.PushType.LOGIN_CONFIRM:
                    LoginConfirmBean loginConfirmBean = null;
                    if (data != null) {
                        try {
                            loginConfirmBean = new Gson().fromJson(data, LoginConfirmBean.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                        if (loginConfirmBean != null) {
                            keyUuid = loginConfirmBean.getUuid();
                        }
                    }
                    break;
                case ConstantField.PushType.SECURITY_PASSWORD_MODIFY_APPLY:
                case ConstantField.PushType.SECURITY_PASSWORD_RESET_APPLY:
                    SecurityApplyBean securityApplyBean = null;
                    if (data != null) {
                        try {
                            securityApplyBean = new Gson().fromJson(data, SecurityApplyBean.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                        if (securityApplyBean != null) {
                            keyUuid = securityApplyBean.getAuthClientUUid();
                        }
                    }
                    break;
                default:
                    break;
            }
            if (keyUuid == null) {
                keyUuid = DataUtil.getClientUuid(EulixSpaceApplication.getContext());
            }
            if (keyUuid == null) {
                keyUuid = UUID.randomUUID().toString();
            }
            String currentData = null;
            EulixPushReserveRecord eulixPushReserveRecord = null;
            if (recordMap.containsKey(keyUuid)) {
                currentData = recordMap.get(keyUuid);
            }
            if (currentData != null && !TextUtils.isEmpty(currentData)) {
                try {
                    eulixPushReserveRecord = new Gson().fromJson(currentData, EulixPushReserveRecord.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
            if (eulixPushReserveRecord == null) {
                eulixPushReserveRecord = new EulixPushReserveRecord();
            } else {
                isUpdate = (messageCreateTime > eulixPushReserveRecord.getMessageCreateTime());
            }
            if (isUpdate == null || isUpdate) {
                eulixPushReserveRecord.setRecordData(data);
                eulixPushReserveRecord.setMessageId(messageId);
                eulixPushReserveRecord.setMessageCreateTime(messageCreateTime);
                currentData = new Gson().toJson(eulixPushReserveRecord, EulixPushReserveRecord.class);
                recordMap.put(keyUuid, currentData);
                allRecordMap.put(messageType, recordMap);
                eulixPushReserveBean.setTypeRecentRecordMap(allRecordMap);
                Map<String, String> pushV = new HashMap<>();
                pushV.put(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID, (boxUuid + "_" + boxBind));
                pushV.put(EulixSpaceDBManager.FIELD_PUSH_UUID, boxUuid);
                pushV.put(EulixSpaceDBManager.FIELD_PUSH_BIND, boxBind);
                pushV.put(EulixSpaceDBManager.FIELD_PUSH_PRIORITY, "-1");
                pushV.put(EulixSpaceDBManager.FIELD_PUSH_SOURCE, "-1");
                pushV.put(EulixSpaceDBManager.FIELD_PUSH_CONSUME, "-1");
                pushV.put(EulixSpaceDBManager.FIELD_PUSH_RESERVE, new Gson().toJson(eulixPushReserveBean, EulixPushReserveBean.class));
                if (isUpdate == null) {
                    EulixSpaceDBUtil.insertPush(EulixSpaceApplication.getContext(), pushV);
                } else {
                    EulixSpaceDBUtil.updatePush(EulixSpaceApplication.getContext(), pushV);
                }
            }
        }
        return isUpdate;
    }

    private boolean handleNotificationMessageDetail(String optType) {
        boolean isHandleDetail = false;
        if (optType != null) {
            switch (optType) {
                case ConstantField.PushType.LOGIN_CONFIRM:
                case ConstantField.PushType.SECURITY_PASSWORD_MODIFY_APPLY:
                case ConstantField.PushType.SECURITY_PASSWORD_RESET_APPLY:
                case ConstantField.PushType.BOX_UPGRADE_PACKAGE_PULLED:
                    isHandleDetail = true;
                    break;
                default:
                    break;
            }
        }
        return isHandleDetail;
    }

    private void handleNotificationMessageDetail(String optType, String messageId, String requestId, boolean isOffline) {
        if (optType != null && messageId != null) {
            switch (optType) {
                case ConstantField.PushType.LOGIN_CONFIRM:
                case ConstantField.PushType.SECURITY_PASSWORD_MODIFY_APPLY:
                case ConstantField.PushType.SECURITY_PASSWORD_RESET_APPLY:
                case ConstantField.PushType.BOX_UPGRADE_PACKAGE_PULLED:
                    if (mHandler == null) {
                        getNotification(messageId, requestId, isOffline);
                    } else {
                        Message message = mHandler.obtainMessage(REQUEST_DETAIL_MESSAGE, isOffline ? 1 : 0, 0);
                        Bundle data = new Bundle();
                        data.putString(MESSAGE_ID, messageId);
                        data.putString(REQUEST_ID, requestId);
                        message.setData(data);
                        mHandler.sendMessage(message);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * 通知栏来源消息
     * @param title
     * @param text
     * @param extra
     * @param isShow
     * @param isOffline 在线消息（TRUE）、离线消息（FALSE）
     */
    public void handleNotificationMessage(String title, String text, Map<String, String> extra, boolean isShow, boolean isOffline) {
        if (extra != null && extra.containsKey(ConstantField.PushExtraKey.OPT_TYPE) && extra.containsKey(ConstantField.PushExtraKey.MESSAGE_ID)) {
            String optType = extra.get(ConstantField.PushExtraKey.OPT_TYPE);
            String messageId = extra.get(ConstantField.PushExtraKey.MESSAGE_ID);
            String userDomain = null;
            if (extra.containsKey(ConstantField.PushExtraKey.SUB_DOMAIN)) {
                userDomain = extra.get(ConstantField.PushExtraKey.SUB_DOMAIN);
            }
            String boxUuid = null;
            String boxBind = null;
            if (userDomain != null && !TextUtils.isEmpty(userDomain)) {
                EulixSpaceExtendInfo eulixSpaceExtendInfo = EulixSpaceDBUtil.getSpaceInfoFromUserDomain(EulixSpaceApplication.getContext(), userDomain);
                if (eulixSpaceExtendInfo != null) {
                    boxUuid = eulixSpaceExtendInfo.getBoxUuid();
                    boxBind = eulixSpaceExtendInfo.getBoxBind();
                }
            } else {
                boxUuid = "";
                boxBind = "";
            }
            if (optType != null && messageId != null && boxUuid != null && boxBind != null) {
                int priority = generatePriority(optType);
                boolean hasConsumed = handleMessage(optType, boxUuid, boxBind, null);
                if (hasConsumed){
                    //该条推送已被消费，不进行后续处理
                    return;
                }
                Boolean isUpdate = null;
                Map<String, String> queryMap = new HashMap<>();
                queryMap.put(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID, messageId);
                if (!TextUtils.isEmpty(boxUuid) && !TextUtils.isEmpty(boxBind)) {
                    queryMap.put(EulixSpaceDBManager.FIELD_PUSH_UUID, boxUuid);
                    queryMap.put(EulixSpaceDBManager.FIELD_PUSH_BIND, boxBind);
                }
                List<Map<String, String>> currentPushValues = EulixSpaceDBUtil.queryPush(EulixSpaceApplication.getContext(), queryMap);
                if (currentPushValues != null && currentPushValues.size() > 0) {
                    for (Map<String, String> currentPushValue : currentPushValues) {
                        if (currentPushValue != null && currentPushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_SOURCE)
                                && currentPushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_CONSUME)) {
                            String source = currentPushValue.get(EulixSpaceDBManager.FIELD_PUSH_SOURCE);
                            String consume = currentPushValue.get(EulixSpaceDBManager.FIELD_PUSH_CONSUME);
                            boolean isHandle = ("2".equals(consume) || "3".equals(consume) || "4".equals(consume));
                            if (isOffline) {
                                if (isHandle) {
                                    isUpdate = false;
                                    break;
                                } else if ("3".equals(source)) {
                                    isUpdate = true;
                                }
                            } else {
                                if ("1".equals(source)) {
                                    isUpdate = true;
                                } else if (isHandle) {
                                    isUpdate = false;
                                    break;
                                }
                            }
                        }
                    }
                }
                if (isUpdate == null || isUpdate) {
                    long currentTimestamp = System.currentTimeMillis();
                    String rawData = new Gson().toJson(extra, new TypeToken<Map<String, String>>(){}.getType());
                    Map<String, String> pushValue = new HashMap<>();
                    pushValue.put(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID, messageId);
                    pushValue.put(EulixSpaceDBManager.FIELD_PUSH_UUID, StringUtil.nullToEmpty(boxUuid));
                    pushValue.put(EulixSpaceDBManager.FIELD_PUSH_BIND, StringUtil.nullToEmpty(boxBind));
                    pushValue.put(EulixSpaceDBManager.FIELD_PUSH_TYPE, StringUtil.nullToEmpty(optType));
                    pushValue.put(EulixSpaceDBManager.FIELD_PUSH_PRIORITY, String.valueOf(priority));
                    pushValue.put(EulixSpaceDBManager.FIELD_PUSH_SOURCE, "1");
                    pushValue.put(EulixSpaceDBManager.FIELD_PUSH_CONSUME, isShow ? (priority <= 16 ? "1" : "4") : "3");
                    pushValue.put(EulixSpaceDBManager.FIELD_PUSH_TITLE, StringUtil.nullToEmpty(title));
                    pushValue.put(EulixSpaceDBManager.FIELD_PUSH_CONTENT, StringUtil.nullToEmpty(text));
                    pushValue.put(EulixSpaceDBManager.FIELD_PUSH_TIMESTAMP, String.valueOf(currentTimestamp));

                    PushBean pushBean = new PushBean();
                    pushBean.setMessageId(messageId);
                    pushBean.setBoxUuid(boxUuid);
                    pushBean.setBoxBind(boxBind);
                    pushBean.setType(optType);
                    pushBean.setPriority(priority);
                    pushBean.setSource(1);
                    pushBean.setRawData(rawData);
                    Boolean isRealUpdate = saveNotificationRecord(optType, boxUuid, boxBind, messageId, rawData, currentTimestamp);
                    if (!isOffline || isRealUpdate == null || isRealUpdate) {
                        if (handleNotificationMessageDetail(optType)) {
                            PushTempValueBean pushTempValueBean = new PushTempValueBean();
                            pushTempValueBean.setPushValue(pushValue);
                            pushTempValueBean.setPushBean(pushBean);
                            String requestId = UUID.randomUUID().toString();
                            pushTempValueBeanMap.put(requestId, pushTempValueBean);
                            handleNotificationMessageDetail(optType, messageId, requestId, isOffline);
                        } else {
                            pushValue.put(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA, rawData);
                            if (isUpdate == null) {
                                EulixSpaceDBUtil.insertPush(EulixSpaceApplication.getContext(), pushValue);
                            } else {
                                EulixSpaceDBUtil.updatePush(EulixSpaceApplication.getContext(), pushValue);
                            }
                            if (isShow) {
                                EulixPushEvent eulixPushEvent = new EulixPushEvent(pushBean);
                                EventBusUtil.post(eulixPushEvent);
                                Logger.d(TAG, "push event");
                            }
                            EventBusUtil.post(new EulixNotificationEvent(messageId, optType, (isShow ? 1 : 3)));
                        }
                    }
                }
            }
        }
    }

    public void handleNativePushMessage(String boxUuid, String boxBind, String type) {
        if (boxUuid != null && boxBind != null && type != null) {
            int priority = generatePriority(type);
            handleOldMessage(boxUuid, boxBind, type, null);
            String messageId = UUID.randomUUID().toString();
            Map<String, String> pushValue = new HashMap<>();
            pushValue.put(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID, messageId);
            pushValue.put(EulixSpaceDBManager.FIELD_PUSH_UUID, StringUtil.nullToEmpty(boxUuid));
            pushValue.put(EulixSpaceDBManager.FIELD_PUSH_BIND, StringUtil.nullToEmpty(boxBind));
            pushValue.put(EulixSpaceDBManager.FIELD_PUSH_TYPE, StringUtil.nullToEmpty(type));
            pushValue.put(EulixSpaceDBManager.FIELD_PUSH_PRIORITY, String.valueOf(priority));
            pushValue.put(EulixSpaceDBManager.FIELD_PUSH_SOURCE, "0");
            pushValue.put(EulixSpaceDBManager.FIELD_PUSH_CONSUME, (priority <= 16 ? "1" : "4"));
            pushValue.put(EulixSpaceDBManager.FIELD_PUSH_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
            EulixSpaceDBUtil.insertPush(EulixSpaceApplication.getContext(), pushValue);
            PushBean pushBean = new PushBean();
            pushBean.setMessageId(messageId);
            pushBean.setBoxUuid(boxUuid);
            pushBean.setBoxBind(boxBind);
            pushBean.setType(type);
            pushBean.setPriority(priority);
            pushBean.setSource(0);
            EulixPushEvent eulixPushEvent = new EulixPushEvent(pushBean);
            EventBusUtil.post(eulixPushEvent);
            Logger.d(TAG, "push event");
        }
    }

    private void refreshAccessToken(String boxUuid, String boxBind) {
        Intent serviceIntent = new Intent(EulixSpaceApplication.getContext(), EulixSpaceService.class);
        serviceIntent.setAction(ConstantField.Action.TOKEN_ACTION);
        if (boxUuid != null) {
            serviceIntent.putExtra(ConstantField.BOX_UUID, boxUuid);
        }
        serviceIntent.putExtra(ConstantField.BOX_BIND, boxBind);
        EulixSpaceApplication.getContext().startService(serviceIntent);
    }

    private boolean inactiveSpace(String boxUuid, String boxBind) {
        boolean tokenCheck = true;
        if (boxUuid != null && boxBind != null) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(EulixSpaceApplication.getContext(), queryMap);
            String boxStatusValue = null;
            String boxTokenValue = null;
            EulixBoxToken eulixBoxToken = null;
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_STATUS)) {
                        boxStatusValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_STATUS);
                        if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
                            boxTokenValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                        }
                        break;
                    }
                }
            }
            if (boxTokenValue != null) {
                try {
                    eulixBoxToken = new Gson().fromJson(boxTokenValue, EulixBoxToken.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
            boolean isUpdate = false;
            if (eulixBoxToken != null) {
                long tokenExpireTimestamp = eulixBoxToken.getTokenExpire();
                if (tokenExpireTimestamp > System.currentTimeMillis()) {
                    isUpdate = true;
                    eulixBoxToken.setTokenExpire(System.currentTimeMillis());
                }
            }
            int boxStatus = -2;
            if (boxStatusValue != null) {
                try {
                    boxStatus = Integer.parseInt(boxStatusValue);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
            switch (boxStatus) {
                case ConstantField.EulixDeviceStatus.ACTIVE:
                    isUpdate = true;
                    boxStatus = ConstantField.EulixDeviceStatus.REQUEST_LOGIN;
                    break;
                case ConstantField.EulixDeviceStatus.OFFLINE_USE:
                    isUpdate = true;
                    boxStatus = ConstantField.EulixDeviceStatus.OFFLINE;
                    break;
                default:
                    break;
            }
            if (isUpdate) {
                DataUtil.boxUnavailable(boxUuid, boxBind);
                Map<String, String> newBoxValue = new HashMap<>();
                newBoxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                newBoxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                newBoxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(boxStatus));
                if (eulixBoxToken != null) {
                    newBoxValue.put(EulixSpaceDBManager.FIELD_BOX_TOKEN, new Gson().toJson(eulixBoxToken, EulixBoxToken.class));
                }
                EulixSpaceDBUtil.updateBox(EulixSpaceApplication.getContext(), newBoxValue);
                EventBusUtil.post(new BoxOnlineEvent(boxUuid, boxBind, boxStatus));
                EulixSpaceInfo eulixSpaceInfo = new EulixSpaceInfo();
                eulixSpaceInfo.setBoxUuid(boxUuid);
                eulixSpaceInfo.setBoxBind(boxBind);
                resetAliveConnect(eulixSpaceInfo, false);
            }
            if (!"1".equals(boxBind) && !"-1".equals(boxBind) && eulixBoxToken != null) {
                tokenCheck = (eulixBoxToken.getLoginValid() == null);
            }
        }
        return tokenCheck;
    }

    public void prepareRefreshNotificationOffline() {
        mHandler.sendEmptyMessage(REFRESH_NOTIFICATION_OFFLINE);
    }

    private void refreshNotificationOffline() {
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(EulixSpaceApplication.getContext());
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID) && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                    String boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                    String boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    String boxOtherInfoValue = null;
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_OTHER_INFO)) {
                        boxOtherInfoValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_OTHER_INFO);
                    }
                    if (boxUuid != null && boxBind != null) {
                        refreshGranteeOffline(boxUuid, boxBind, boxOtherInfoValue);
                    }
                }
            }
        }
    }

    private void refreshGranteeOffline(String boxUuid, String boxBind, String boxOtherInfoValue) {
        if (boxUuid != null && ("1".equals(boxBind) || "-1".equals(boxBind))) {
            List<String> typeList = new ArrayList<>();
            typeList.add(ConstantField.PushType.LOGIN_CONFIRM);
            if ("1".equals(boxBind)) {
                typeList.add(ConstantField.PushType.SECURITY_PASSWORD_MODIFY_APPLY);
                typeList.add(ConstantField.PushType.SECURITY_PASSWORD_RESET_APPLY);
                typeList.add(ConstantField.PushType.BOX_UPGRADE_PACKAGE_PULLED);
            }
            long pageSize = 20L;
            EulixBoxOtherInfo eulixBoxOtherInfo = null;
            if (boxOtherInfoValue != null) {
                try {
                    eulixBoxOtherInfo = new Gson().fromJson(boxOtherInfoValue, EulixBoxOtherInfo.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
            if (eulixBoxOtherInfo != null) {
                Map<String, TerminalInfo> terminalInfoMap = eulixBoxOtherInfo.getTerminalInfoMap();
                if (terminalInfoMap != null) {
                    int granteeSize = terminalInfoMap.size();
                    if (granteeSize > 0) {
                        pageSize = FormatUtil.quantification((granteeSize * 3 * typeList.size()), 10, false);
                    } else {
                        pageSize = 0L;
                    }
                }
            }
            if (pageSize > 0) {
                GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(EulixSpaceApplication.getContext(), boxUuid, boxBind);
                if (gatewayCommunicationBase != null) {
                    String baseUrl = Urls.getBaseUrl();
                    EulixNotificationUtil.getAllNotification(baseUrl, gatewayCommunicationBase.getAccessToken()
                            , gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getIvParams()
                            , 1, (pageSize > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) pageSize), typeList, new NotificationAllCallback() {
                                @Override
                                public void onSuccess(int code, String message, String requestId, List<GetNotificationResult> notificationResults, PageInfo pageInfo) {
                                    if (notificationResults != null) {
                                        for (GetNotificationResult result : notificationResults) {
                                            if (result != null) {
                                                handleNotificationAllMessage(result, boxUuid, boxBind);
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void onFailed(int code, String message, String requestId) {
                                    // Do nothing
                                }

                                @Override
                                public void onError(String errMsg) {
                                    // Do nothing
                                }
                            });
                }
            }
        }
    }

    static class PushTempValueBean implements EulixKeep {
        private Map<String, String> pushValue;
        private PushBean pushBean;

        public Map<String, String> getPushValue() {
            return pushValue;
        }

        public void setPushValue(Map<String, String> pushValue) {
            this.pushValue = pushValue;
        }

        public PushBean getPushBean() {
            return pushBean;
        }

        public void setPushBean(PushBean pushBean) {
            this.pushBean = pushBean;
        }
    }
}
