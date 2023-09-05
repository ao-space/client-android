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

package xyz.eulix.space;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.text.TextUtils;
import android.util.Patterns;
import android.webkit.URLUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.bean.DeviceInfo;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.EulixBoxInfo;
import xyz.eulix.space.bean.EulixBoxOtherInfo;
import xyz.eulix.space.bean.EulixBoxToken;
import xyz.eulix.space.bean.EulixSpaceInfo;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.bean.TerminalInfo;
import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.callback.EulixSpaceCallback;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.database.EulixSpaceSharePreferenceHelper;
import xyz.eulix.space.did.bean.DIDProviderBean;
import xyz.eulix.space.did.event.DIDDocumentRequestEvent;
import xyz.eulix.space.did.event.DIDDocumentResponseEvent;
import xyz.eulix.space.did.network.DIDDocumentCallback;
import xyz.eulix.space.did.network.DIDDocumentResult;
import xyz.eulix.space.did.network.DIDUtil;
import xyz.eulix.space.event.AccessInfoRequestEvent;
import xyz.eulix.space.event.AccessInfoResponseEvent;
import xyz.eulix.space.event.AccessTokenCreateEvent;
import xyz.eulix.space.event.AccessTokenResultEvent;
import xyz.eulix.space.event.AppInstallEvent;
import xyz.eulix.space.event.AppUpdateEvent;
import xyz.eulix.space.event.AuthAutoLoginEvent;
import xyz.eulix.space.event.BoxInsertDeleteEvent;
import xyz.eulix.space.event.BoxNetworkRequestEvent;
import xyz.eulix.space.event.BoxOnlineEvent;
import xyz.eulix.space.event.BoxOnlineRequestEvent;
import xyz.eulix.space.event.DeviceAbilityRequestEvent;
import xyz.eulix.space.event.DeviceAbilityResponseEvent;
import xyz.eulix.space.event.DeviceHardwareInfoRequestEvent;
import xyz.eulix.space.event.DeviceHardwareInfoResponseEvent;
import xyz.eulix.space.event.DiskManagementListRequestEvent;
import xyz.eulix.space.event.DiskManagementListResponseEvent;
import xyz.eulix.space.event.DiskManagementRaidInfoRequestEvent;
import xyz.eulix.space.event.DiskManagementRaidInfoResponseEvent;
import xyz.eulix.space.event.EulixSystemShutdownRequestEvent;
import xyz.eulix.space.event.GranteeTokenInvalidEvent;
import xyz.eulix.space.event.MemberListEvent;
import xyz.eulix.space.event.MemberResultEvent;
import xyz.eulix.space.event.OfflineGranteeEvent;
import xyz.eulix.space.event.PlatformAbilityRequestEvent;
import xyz.eulix.space.event.SecurityMessagePollRequestEvent;
import xyz.eulix.space.event.SecurityMessagePollResponseEvent;
import xyz.eulix.space.event.SpaceChangeEvent;
import xyz.eulix.space.event.SpaceStatusTestRequestEvent;
import xyz.eulix.space.event.SpaceStatusTestResponseEvent;
import xyz.eulix.space.event.SpecificBoxOnlineRequestEvent;
import xyz.eulix.space.event.SpecificBoxOnlineResponseEvent;
import xyz.eulix.space.event.StorageInfoRequestEvent;
import xyz.eulix.space.event.StorageInfoResponseEvent;
import xyz.eulix.space.event.TerminalListEvent;
import xyz.eulix.space.event.TerminalResultEvent;
import xyz.eulix.space.manager.BoxNetworkCheckManager;
import xyz.eulix.space.manager.EulixPushManager;
import xyz.eulix.space.manager.EulixSpaceDBBoxManager;
import xyz.eulix.space.manager.LanManager;
import xyz.eulix.space.network.EulixBaseResponseExtensionCallback;
import xyz.eulix.space.network.EulixSpaceWebSocketClient;
import xyz.eulix.space.network.agent.disk.DiskManageListResult;
import xyz.eulix.space.network.agent.disk.DiskManagementListCallback;
import xyz.eulix.space.network.agent.platform.EulixPlatformUtil;
import xyz.eulix.space.network.agent.platform.PlatformAbilityCallback;
import xyz.eulix.space.network.agent.platform.PlatformApi;
import xyz.eulix.space.network.box.AccountInfoUtil;
import xyz.eulix.space.network.box.DeviceInfoCallback;
import xyz.eulix.space.network.disk.DiskUtil;
import xyz.eulix.space.network.disk.RaidInfoCallback;
import xyz.eulix.space.network.disk.RaidInfoResult;
import xyz.eulix.space.network.gateway.AlgorithmConfig;
import xyz.eulix.space.network.gateway.AuthAutoLoginCallback;
import xyz.eulix.space.network.gateway.CreateAuthTokenCallback;
import xyz.eulix.space.network.gateway.GatewayManager;
import xyz.eulix.space.network.gateway.GatewayUtil;
import xyz.eulix.space.network.gateway.ISpaceStatusCallback;
import xyz.eulix.space.network.gateway.SpaceStatusCallback;
import xyz.eulix.space.network.gateway.SpaceStatusResult;
import xyz.eulix.space.network.gateway.TransportationConfig;
import xyz.eulix.space.network.net.DeviceAbilityCallback;
import xyz.eulix.space.network.net.EulixNetUtil;
import xyz.eulix.space.network.net.InternetServiceConfigCallback;
import xyz.eulix.space.network.net.InternetServiceConfigResult;
import xyz.eulix.space.network.platform.AppDownloadCallback;
import xyz.eulix.space.network.platform.AppInfoUtil;
import xyz.eulix.space.network.register.RegisterDeviceCallback;
import xyz.eulix.space.network.security.DeviceHardwareInfoCallback;
import xyz.eulix.space.network.security.DeviceHardwareInfoResult;
import xyz.eulix.space.network.security.EulixSecurityUtil;
import xyz.eulix.space.network.security.SecurityMessagePollCallback;
import xyz.eulix.space.network.security.SecurityMessagePollResult;
import xyz.eulix.space.network.socket.BaseRequest;
import xyz.eulix.space.network.socket.BaseResponse;
import xyz.eulix.space.network.socket.SocketHeart;
import xyz.eulix.space.network.socket.ack.AckParameters;
import xyz.eulix.space.network.socket.ack.Acks;
import xyz.eulix.space.network.socket.login.LoginRequest;
import xyz.eulix.space.network.socket.login.LoginRequestParameters;
import xyz.eulix.space.network.socket.login.LoginResponse;
import xyz.eulix.space.network.socket.login.LoginResponseResult;
import xyz.eulix.space.network.socket.push.PushParameters;
import xyz.eulix.space.network.socket.push.SocketPush;
import xyz.eulix.space.network.socket.query.QueryListItem;
import xyz.eulix.space.network.socket.query.QueryRequest;
import xyz.eulix.space.network.socket.query.QueryRequestParameters;
import xyz.eulix.space.network.socket.query.QueryResponse;
import xyz.eulix.space.network.socket.query.QueryResponseResult;
import xyz.eulix.space.network.userinfo.AccountInfoCallback;
import xyz.eulix.space.network.userinfo.PersonalInfoResult;
import xyz.eulix.space.network.userinfo.TerminalInfoResult;
import xyz.eulix.space.network.userinfo.TerminalListCallback;
import xyz.eulix.space.network.userinfo.TerminalOfflineCallback;
import xyz.eulix.space.network.userinfo.TerminalOfflineResult;
import xyz.eulix.space.network.userinfo.UserInfoUtil;
import xyz.eulix.space.util.AOSpaceUtil;
import xyz.eulix.space.util.AlarmUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.DebugUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.GatewayUtils;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.MD5Util;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.SystemUtil;
import xyz.eulix.space.util.ThreadPool;
import xyz.eulix.space.util.ToastManager;
import xyz.eulix.space.util.Urls;

/**
 * @author: chenjiawei
 * date: 2021/6/16 11:00
 */
public class EulixSpaceService extends Service implements BoxNetworkCheckManager.BoxNetworkCheckCallback {
    private static final String TAG = EulixSpaceService.class.getSimpleName();
    private static final String STORAGE = "storage";
    private static final int GET_UUID_DELAY = 1000;
    private static final int REGISTER_DEVICE_SHORT_DELAY = 2000;
    private static final int REGISTER_DEVICE_LONG_DELAY = 3500;
    private static final int CONNECT_WEB_SOCKET_DELAY = 2000;
    private static final int SEND_HEART_PERIOD = 15000;
    private static final int GET_UUID = 0;
    private static final int BIND_WEB_RTC_SERVICE = GET_UUID + 1;
    private static final int REGISTER_DEVICE = BIND_WEB_RTC_SERVICE + 1;
    private static final int INIT_WEB_SOCKET = REGISTER_DEVICE + 1;
    private static final int CONNECT_WEB_SOCKET = INIT_WEB_SOCKET + 1;
    private static final int SEND_HEART = CONNECT_WEB_SOCKET + 1;
    private static final int RECEIVE_SOCKET_MESSAGE = SEND_HEART + 1;
    private static final int CREATE_AUTH_TOKEN = RECEIVE_SOCKET_MESSAGE + 1;
    private static final int CHECK_BOX_ONLINE = CREATE_AUTH_TOKEN + 1;
    private static final int HEART_FIVE_SECOND = 10000;
    private boolean isForeground, isRequestCreateAuthToken;
    private EulixSpaceHandler mHandler;
    private EulixSpaceBinder mBinder = new EulixSpaceBinder();
    private EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper;
    private EulixSpaceWebSocketClient mWebSocketClient;
    private EulixSpaceCallback mCallback;
    private NotificationManager notificationManager;
    private EulixPushManager eulixPushManager;
    private EulixSpaceReceiver mReceiver;
    private ToastManager toastManager;
    private BoxNetworkCheckManager boxNetworkCheckManager;
    private List<Integer> storageNotificationIds = new ArrayList<>();
    private boolean isRegisterWeChat;

    private RegisterDeviceCallback registerDeviceCallback = new RegisterDeviceCallback() {
        @Override
        public void onSuccess(int code, String data) {
            if (mHandler != null) {
                while (mHandler.hasMessages(INIT_WEB_SOCKET)) {
                    mHandler.removeMessages(INIT_WEB_SOCKET);
                }
                mHandler.sendEmptyMessage(INIT_WEB_SOCKET);
            }
        }

        @Override
        public void onFailed(String message, int code) {
            if (mHandler != null) {
                while (mHandler.hasMessages(REGISTER_DEVICE)) {
                    mHandler.removeMessages(REGISTER_DEVICE);
                }
                mHandler.sendEmptyMessageDelayed(REGISTER_DEVICE, REGISTER_DEVICE_SHORT_DELAY);
            }
        }

        @Override
        public void onError(String msg) {
            if (mHandler != null) {
                while (mHandler.hasMessages(REGISTER_DEVICE)) {
                    mHandler.removeMessages(REGISTER_DEVICE);
                }
                mHandler.sendEmptyMessageDelayed(REGISTER_DEVICE, REGISTER_DEVICE_LONG_DELAY);
            }
        }
    };

    static class EulixSpaceHandler extends Handler {
        private WeakReference<EulixSpaceService> eulixSpaceServiceWeakReference;

        public EulixSpaceHandler(EulixSpaceService service) {
            eulixSpaceServiceWeakReference = new WeakReference<>(service);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            EulixSpaceService service = eulixSpaceServiceWeakReference.get();
            if (service == null) {
                super.handleMessage(msg);
            } else {
                switch (msg.what) {
                    case GET_UUID:
                        final String uuid = service.getUUID();
                        if (TextUtils.isEmpty(uuid) || service.mCallback == null) {
                            sendEmptyMessageDelayed(GET_UUID, GET_UUID_DELAY);
                        } else {
                            while (hasMessages(REGISTER_DEVICE)) {
                                removeMessages(REGISTER_DEVICE);
                            }
                            sendEmptyMessage(REGISTER_DEVICE);
                        }
                        break;
                    case CONNECT_WEB_SOCKET:
                        int reconnectResult = 0;
                        if (msg.arg1 != 0) {
                            reconnectResult = service.reconnectWebSocket();
                        }
                        if (msg.arg1 == 0 || reconnectResult >= 0) {
                            if ((msg.arg1 == 0 ? service.connectWebSocket() : (reconnectResult > 0))) {
                                LoginRequest loginRequest = new LoginRequest();
                                loginRequest.setMethod(ConstantField.SocketMethod.LOGIN);
                                loginRequest.setMessageId(service.getUUID());
                                LoginRequestParameters parameters = new LoginRequestParameters();
                                parameters.setPlatform("android");
                                parameters.setClientUUID("r2VTgYrvX2ujIsAddnsHD7H3xMPHFp8q");
                                parameters.setClientId(service.getUUID());
                                parameters.setDeviceId(service.getUUID());
                                loginRequest.setParameters(parameters);
                                service.sendWebSocket(new Gson().toJson(loginRequest, LoginRequest.class));
                                sendEmptyMessageDelayed(SEND_HEART, SEND_HEART_PERIOD);
                            } else {
                                sendMessageDelayed(obtainMessage(CONNECT_WEB_SOCKET, msg.arg1, 0), CONNECT_WEB_SOCKET_DELAY);
                            }
                        }
                        break;
                    case SEND_HEART:
                        SocketHeart socketHeart = new SocketHeart();
                        socketHeart.setMethod(ConstantField.SocketMethod.PING);
                        service.sendWebSocket(new Gson().toJson(socketHeart, SocketHeart.class));
                        sendEmptyMessageDelayed(SEND_HEART, SEND_HEART_PERIOD);
                        break;
                    case RECEIVE_SOCKET_MESSAGE:
                        if (msg.obj instanceof String) {
                            service.handleSocketMessage((String) msg.obj);
                        }
                        break;
                    case CREATE_AUTH_TOKEN:
                        String boxUuid = null;
                        String boxBind = null;
                        Bundle data = msg.peekData();
                        if (data != null) {
                            if (data.containsKey(ConstantField.BOX_UUID)) {
                                boxUuid = data.getString(ConstantField.BOX_UUID, null);
                            }
                            if (data.containsKey(ConstantField.BOX_BIND)) {
                                boxBind = data.getString(ConstantField.BOX_BIND, null);
                            }
                        }
                        service.createAuthToken(boxUuid, boxBind, (msg.arg1 != 0), (msg.arg2 != 0));
                        break;
                    case CHECK_BOX_ONLINE:
                        service.checkBoxOnline();
                        sendEmptyMessageDelayed(CHECK_BOX_ONLINE, 20000);
                        break;
                    case HEART_FIVE_SECOND:
                        Map<Thread, StackTraceElement[]> threadMap = Thread.getAllStackTraces();
                        Set<Thread> threads = threadMap.keySet();
                        Logger.d(TAG, "thread map size: " + threads.size());
                        int index = 0;
                        StringBuilder stringBuilder = null;
                        for (Thread thread : threads) {
                            if (thread != null) {
                                index += 1;
                                if (stringBuilder == null) {
                                    stringBuilder = new StringBuilder();
                                } else {
                                    stringBuilder.append(" ; ");
                                }
                                stringBuilder.append(thread.getName());
                                if (index > 24) {
                                    Logger.d(TAG, "thread name: " + stringBuilder.toString());
                                    index = 0;
                                    stringBuilder = null;
                                }
                            }
                        }
                        if (stringBuilder != null) {
                            Logger.d(TAG, "thread name: " + stringBuilder.toString());
                        }
                        sendEmptyMessageDelayed(HEART_FIVE_SECOND, 5000);
                        break;
                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        }
    }

    /**
     * 通知栏展示信息
     *
     * @param notificationId
     * @param pendingIntent
     * @param title
     * @param content
     * @param importance       通知级别
     * @param isForeground     区别前台服务（true）和普通通知（false）
     * @param notificationType 通知类型
     */
    private void showNotification(int notificationId, Intent pendingIntent, String title, String content, int importance, boolean isForeground, String notificationType) {
        String channelId = getPackageName();
        if (TextUtils.isEmpty(channelId)) {
            channelId = ConstantField.PACKAGE_NAME;
        }
        Notification.Builder builder = new Notification.Builder(getApplicationContext())
                .setContentIntent(PendingIntent.getActivity(this, SystemUtil.getNotificationId()
                        , pendingIntent, PendingIntent.FLAG_UPDATE_CURRENT))
                .setContentTitle(title)
                .setContentText(content)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.eulix_space_launcher_v3))
                .setSmallIcon(R.mipmap.eulix_space_launcher_v3)
                .setWhen(System.currentTimeMillis());
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(channelId, TAG, importance);
            notificationChannel.enableLights(true);
            notificationChannel.setShowBadge(true);
            notificationChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(notificationChannel);
                builder.setChannelId(channelId);
            }
        }
        if (isForeground) {
            startForeground(notificationId, builder.build());
        } else {
            if (notificationType != null) {
                switch (notificationType) {
                    case STORAGE:
                        storageNotificationIds.add(notificationId);
                        break;
                    default:
                        break;
                }
            }
            if (notificationManager != null) {
                notificationManager.notify(notificationId, builder.build());
            }
        }
    }

    /**
     * 通知栏取消展示
     *
     * @param notificationType
     */
    private void dismissNotification(String notificationType) {
        if (notificationManager != null) {
            if (notificationType == null) {
                notificationManager.cancelAll();
                storageNotificationIds.clear();
            } else {
                switch (notificationType) {
                    case STORAGE:
                        Iterator<Integer> storageIterator = storageNotificationIds.iterator();
                        while (storageIterator.hasNext()) {
                            Integer notificationId = storageIterator.next();
                            if (notificationId != null) {
                                notificationManager.cancel(notificationId);
                            }
                            storageIterator.remove();
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * @return 返回应用UUID
     */
    private String getUUID() {
        String uuid = null;
        eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(getApplicationContext());
        if (eulixSpaceSharePreferenceHelper != null && eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.UUID)) {
            uuid = eulixSpaceSharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.UUID);
        }
        return uuid;
    }


    /**
     * 连接web socket
     *
     * @return
     */
    private boolean connectWebSocket() {
        boolean result = false;
        if (mWebSocketClient != null) {
            try {
                result = mWebSocketClient.connectBlocking();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }
        return result;
    }

    /**
     * 重新连接web socket
     *
     * @return -1：未初始化,0：连接失败；1：连接成功
     */
    private int reconnectWebSocket() {
        int result = 0;
        if (mWebSocketClient == null) {
            result = -1;
            if (mHandler != null) {
                while (mHandler.hasMessages(INIT_WEB_SOCKET)) {
                    mHandler.removeMessages(INIT_WEB_SOCKET);
                }
                mHandler.sendEmptyMessage(INIT_WEB_SOCKET);
            }
        } else {
            try {
                result = (mWebSocketClient.reconnectBlocking() ? 1 : 0);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }
        return result;
    }

    /**
     * web socket发送消息
     *
     * @param message
     * @return
     */
    private boolean sendWebSocket(final String message) {
        Logger.d(TAG, "send message: " + message);
        boolean result = false;
        if (mWebSocketClient != null && mWebSocketClient.isOpen()) {
            try {
                result = ThreadPool.getInstance().execute(() -> mWebSocketClient.send(message));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            } catch (Exception ee) {
                ee.printStackTrace();
            }
        }
        Logger.d(TAG, "send result: " + result);
        return result;
    }

    /**
     * 关闭web socket
     */
    private void closeWebSocket() {
        if (mHandler != null) {
            while (mHandler.hasMessages(SEND_HEART)) {
                mHandler.removeMessages(SEND_HEART);
            }
        }
        if (mWebSocketClient != null && !mWebSocketClient.isClosed()) {
            try {
                mWebSocketClient.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mWebSocketClient = null;
    }

    /**
     * web socket发送ack
     *
     * @param messageId 收到的消息id
     */
    private void sendAck(String messageId) {
        if (messageId != null) {
            BaseRequest baseRequest = new BaseRequest();
            baseRequest.setMethod(ConstantField.SocketMethod.ACK);
            baseRequest.setMessageId(messageId);
            sendWebSocket(new Gson().toJson(baseRequest, BaseRequest.class));
        }
    }

    /**
     * 处理web socket接收的消息
     *
     * @param message
     */
    private void handleSocketMessage(String message) {
        if (!TextUtils.isEmpty(message)) {
            BaseResponse baseResponse = null;
            try {
                baseResponse = new Gson().fromJson(message, BaseResponse.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
            if (baseResponse != null) {
                String method = baseResponse.getMethod();
                if (!TextUtils.isEmpty(method)) {
                    switch (method.toLowerCase().trim()) {
                        case ConstantField.SocketMethod.LOGIN:
                            LoginResponse loginResponse = null;
                            try {
                                loginResponse = new Gson().fromJson(message, LoginResponse.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                            if (loginResponse != null) {
                                LoginResponseResult result = loginResponse.getResult();
                                if (result != null) {
                                    int code = result.getCode();
                                    if (code == 0) {
                                        QueryRequest queryRequest = new QueryRequest();
                                        queryRequest.setMethod(ConstantField.SocketMethod.QUERY);
                                        queryRequest.setMessageId(getUUID());
                                        QueryRequestParameters parameters = new QueryRequestParameters();
                                        parameters.setPage(0);
                                        parameters.setPageSize(10);
                                        queryRequest.setParameters(parameters);
                                        sendWebSocket(new Gson().toJson(queryRequest, QueryRequest.class));
                                    }
                                }
                            }
                            break;
                        case ConstantField.SocketMethod.PUSH:
                            SocketPush socketPush = null;
                            try {
                                socketPush = new Gson().fromJson(message, SocketPush.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                            if (socketPush != null) {
                                sendAck(socketPush.getMessageId());
                                PushParameters result = socketPush.getResult();
                                if (result != null) {
                                    PackageManager packageManager = getPackageManager();
                                    if (packageManager != null) {
                                        Intent launcherIntent = packageManager.getLaunchIntentForPackage(getPackageName());
                                        showNotification(SystemUtil.getNotificationId(), launcherIntent, result.getTitle(), result.getBody()
                                                , (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                                                        ? NotificationManager.IMPORTANCE_HIGH : 0), false, STORAGE);
                                    }
                                }
                            }
                            break;
                        case ConstantField.SocketMethod.QUERY:
                            QueryResponse queryResponse = null;
                            try {
                                queryResponse = new Gson().fromJson(message, QueryResponse.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                            if (queryResponse != null) {
                                QueryResponseResult result = queryResponse.getResult();
                                if (result != null) {
                                    List<QueryListItem> list = result.getList();
                                    if (list != null) {
                                        List<String> messageIds = new ArrayList<>();
                                        for (QueryListItem item : list) {
                                            if (item != null) {
                                                String messageId = item.getMessageId();
                                                if (messageId != null && !messageIds.contains(messageId)) {
                                                    messageIds.add(messageId);
                                                }
                                            }
                                        }
                                        int size = messageIds.size();
                                        if (size > 0) {
                                            if (size == 1) {
                                                sendAck(messageIds.get(0));
                                            } else {
                                                AckParameters parameters = new AckParameters();
                                                parameters.setList(messageIds);
                                                Acks acks = new Acks();
                                                acks.setMethod(ConstantField.SocketMethod.ACK);
                                                acks.setParameters(parameters);
                                                sendWebSocket(new Gson().toJson(acks, Acks.class));
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }


    @Deprecated
    private void checkBoxOnline() {
        if (/*NetUtils.isNetAvailable(getApplicationContext())*/true) {
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(getApplicationContext());
            List<String> checkBoxUuidList = new ArrayList<>();
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                        && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)
                        && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_DOMAIN)
                        && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_STATUS)) {
                    int status = -1;
                    String statusValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_STATUS);
                    if (statusValue != null) {
                        try {
                            status = Integer.parseInt(statusValue);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                        if (status >= ConstantField.EulixDeviceStatus.OFFLINE && status <= ConstantField.EulixDeviceStatus.ONLINE_UNINITIALIZED) {
                            String boxUUID = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                            String boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                            String boxDomain = boxValue.get(EulixSpaceDBManager.FIELD_BOX_DOMAIN);
                            if (boxUUID != null && boxDomain != null && !checkBoxUuidList.contains(boxUUID)) {
                                checkBoxUuidList.add(boxUUID);
                                int finalStatus = status;
                                GatewayUtil.getSpaceStatus(getApplicationContext(), boxDomain, boxUUID, boxBind, true, new SpaceStatusCallback() {
                                    @Override
                                    public void onSuccess(String boxUuid, String boxBind, String status, String version) {
                                        if (boxUUID.equals(boxUuid) && finalStatus == ConstantField.EulixDeviceStatus.OFFLINE) {
                                            String lastBoxUuid = null;
                                            String lastBoxBind = null;
                                            EulixSpaceInfo eulixSpaceInfo = DataUtil.getLastEulixSpace(getApplicationContext());
                                            if (eulixSpaceInfo != null) {
                                                lastBoxUuid = eulixSpaceInfo.getBoxUuid();
                                                lastBoxBind = eulixSpaceInfo.getBoxBind();
                                            }
                                            List<Map<String, String>> nBoxValues = EulixSpaceDBUtil.queryBox(getApplicationContext(), EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                                            if (nBoxValues != null) {
                                                for (Map<String, String> nBoxValue : nBoxValues) {
                                                    if (nBoxValue != null && nBoxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                                                        String bindValue = nBoxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                                                        boolean requireToken = false;
                                                        boolean isLastSpace = (lastBoxUuid != null && lastBoxUuid.equals(boxUuid) && lastBoxBind != null && lastBoxBind.equals(bindValue));
                                                        if (isLastSpace) {
                                                            long currentTimestamp = System.currentTimeMillis();
                                                            Long expireTimestamp = null;
                                                            boolean isExpire = true;
                                                            if (nBoxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
                                                                String tokenValue = nBoxValue.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                                                                if (tokenValue != null) {
                                                                    EulixBoxToken eulixBoxToken = null;
                                                                    try {
                                                                        eulixBoxToken = new Gson().fromJson(tokenValue, EulixBoxToken.class);
                                                                    } catch (JsonSyntaxException e) {
                                                                        e.printStackTrace();
                                                                    }
                                                                    if (eulixBoxToken != null) {
                                                                        expireTimestamp = eulixBoxToken.getTokenExpire();
                                                                    }
                                                                    isExpire = (expireTimestamp == null || expireTimestamp < (currentTimestamp + 10 * 1000));
                                                                }
                                                            }
                                                            if (isExpire) {
                                                                isLastSpace = false;
                                                                requireToken = true;
                                                            } else {
                                                                Integer boxAlarmId = DataUtil.getTokenAlarmId(boxUuid, boxBind);
                                                                if (boxAlarmId != null) {
                                                                    AlarmUtil.cancelAlarm(getApplicationContext(), boxAlarmId);
                                                                }
                                                                int alarmId = AlarmUtil.getAlarmId();
                                                                DataUtil.setTokenAlarmId(boxUuid, boxBind, alarmId);
                                                                long diffTimestamp = 60 * 1000L;
                                                                if (expireTimestamp > currentTimestamp) {
                                                                    diffTimestamp = Math.min(((expireTimestamp - currentTimestamp) / 10), diffTimestamp);
                                                                    AlarmUtil.setAlarm(getApplicationContext(), (expireTimestamp - diffTimestamp), alarmId, boxUuid, boxBind, (diffTimestamp / 2));
                                                                } else {
                                                                    AlarmUtil.setAlarm(getApplicationContext(), (currentTimestamp + diffTimestamp), alarmId, boxUuid, boxBind, (diffTimestamp / 2));
                                                                }
                                                            }
                                                        }

                                                        int spaceStatus = (isLastSpace ? ConstantField.EulixDeviceStatus.ACTIVE
                                                                : (("1".equals(bindValue) || "-1".equals(bindValue))
                                                                ? ConstantField.EulixDeviceStatus.REQUEST_USE : ConstantField.EulixDeviceStatus.REQUEST_LOGIN));
                                                        Map<String, String> nBoxV = new HashMap<>();
                                                        nBoxV.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                                                        nBoxV.put(EulixSpaceDBManager.FIELD_BOX_BIND, bindValue);
                                                        nBoxV.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(spaceStatus));
                                                        EulixSpaceDBUtil.updateBox(getApplicationContext(), nBoxV);
                                                        BoxOnlineEvent boxOnlineEvent = new BoxOnlineEvent(boxUuid, bindValue, spaceStatus);
                                                        EventBusUtil.post(boxOnlineEvent);

                                                        if (requireToken) {
                                                            createAuthToken(boxUuid, bindValue, false, false);
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    @Override
                                    public void onFailed(String boxUuid, String boxBind) {
                                        // Do nothing
                                    }

                                    @Override
                                    public void onError(String boxUuid, String boxBind, String msg) {
                                        if (boxUUID.equals(boxUuid)) {
                                            if (finalStatus == ConstantField.EulixDeviceStatus.ACTIVE) {
                                                toastManager.showImageTextToast(R.drawable.toast_refuse
                                                        , EulixSpaceApplication.getResumeActivityContext().getString(R.string.active_device_offline_hint));
                                            }
                                            if (finalStatus != ConstantField.EulixDeviceStatus.OFFLINE) {
                                                List<Integer> boxAlarmIds = DataUtil.getTokenAlarmIds(boxUuid);
                                                for (int boxAlarmId : boxAlarmIds) {
                                                    AlarmUtil.cancelAlarm(getApplicationContext(), boxAlarmId);
                                                }
                                                Map<String, String> nBoxV = new HashMap<>();
                                                nBoxV.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                                                nBoxV.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE));
                                                EulixSpaceDBUtil.updateBox(getApplicationContext(), nBoxV);

                                                BoxOnlineEvent boxOnlineEvent = new BoxOnlineEvent(boxUuid, boxBind, ConstantField.EulixDeviceStatus.OFFLINE);
                                                EventBusUtil.post(boxOnlineEvent);
                                            }
                                        }
                                    }
                                });
                            }
                        }
                    }
                }
            }
        }
    }

    private void resetSpacePoll(boolean isHeart) {
        handleEulixPush(isHeart);
    }


    private void prepareAuthToken(String boxUuid, String boxBind, boolean isForce, boolean isFromCall) {
        if (mHandler != null) {
            while (mHandler.hasMessages(CREATE_AUTH_TOKEN)) {
                mHandler.removeMessages(CREATE_AUTH_TOKEN);
            }
            Message message = mHandler.obtainMessage(CREATE_AUTH_TOKEN, (isForce ? 1 : 0), (isFromCall ? 1 : 0));
            Bundle data = new Bundle();
            if (boxUuid != null) {
                data.putString(ConstantField.BOX_UUID, boxUuid);
            }
            if (boxBind != null) {
                data.putString(ConstantField.BOX_BIND, boxBind);
            }
            message.setData(data);
            mHandler.sendMessage(message);
        } else {
            createAuthToken(boxUuid, boxBind, isForce, isFromCall);
        }
    }

    /**
     * 生成AuthToken和对称秘钥
     */
    private void createAuthToken(String nBoxUuid, String nBoxBind, boolean isForce, boolean isFromCall) {
        isRequestCreateAuthToken = true;
        String bindValue = null;
        String authKeyValue = null;
        String clientUUIDValue = null;
        String boxPublicKeyValue = null;
        String boxDomainValue = null;
        String boxUUIDValue = null;
        String refreshTokenValue = null;
        String loginValid = null;
        boolean isTrial = false;
        eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(getApplicationContext());
        if (eulixSpaceSharePreferenceHelper != null && eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.UUID)) {
            clientUUIDValue = eulixSpaceSharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.UUID);
        }
        List<Map<String, String>> boxValues;
        if (nBoxUuid == null && nBoxBind == null) {
            boxValues = EulixSpaceDBUtil.queryBox(getApplicationContext()
                    , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
            if (boxValues == null || boxValues.size() <= 0) {
                boxValues = EulixSpaceDBUtil.queryBox(getApplicationContext()
                        , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
            }
        } else {
            Map<String, String> queryMap = new HashMap<>();
            if (nBoxUuid != null) {
                queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, nBoxUuid);
            }
            if (nBoxBind != null) {
                queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, nBoxBind);
            }
            boxValues = EulixSpaceDBUtil.queryBox(getApplicationContext(), queryMap);
        }
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                    bindValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    boxDomainValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_DOMAIN);
                    boxUUIDValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                    boxPublicKeyValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY);
                    String boxTokenValue = null;
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
                        boxTokenValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                    }
                    EulixBoxToken boxToken = null;
                    if (boxTokenValue != null) {
                        try {
                            boxToken = new Gson().fromJson(boxTokenValue, EulixBoxToken.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                    if (boxToken != null) {
                        isTrial = (EulixBoxToken.IDENTITY_TRIAL == boxToken.getIdentity());
                    }
                    if ("1".equals(bindValue) || "-1".equals(bindValue)) {
                        authKeyValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_AUTHORIZATION);
                    } else if (boxToken != null) {
                        refreshTokenValue = boxToken.getRefreshToken();
                        loginValid = boxToken.getLoginValid();
                    }
                    break;
                }
            }
        }
        if (!TextUtils.isEmpty(boxDomainValue) && !TextUtils.isEmpty(boxUUIDValue) && bindValue != null) {
            final String boxUUID = boxUUIDValue;
            String finalBindValue = bindValue;
            boolean finalIsTrial = isTrial;
            switch (bindValue) {
                case "1":
                    if (!TextUtils.isEmpty(authKeyValue) && !TextUtils.isEmpty(clientUUIDValue) && !TextUtils.isEmpty(boxPublicKeyValue)) {
                        GatewayUtil.createAuthToken(getApplicationContext(), boxUUID, finalBindValue, boxPublicKeyValue, boxDomainValue, authKeyValue, clientUUIDValue, true, isForce, new CreateAuthTokenCallback() {
                            @Override
                            public void onSuccess(String boxUUID, String boxBindValue, String accessToken, String transformation, String initializationVector, String secret, String expiresAt, Long expiresAtEpochSeconds, String refreshToken, String requestId) {
                                long expireTimestamp = accessTokenCallback(boxUUID, boxBindValue, accessToken, transformation, initializationVector, secret, expiresAt, expiresAtEpochSeconds, refreshToken, null, isForce, isFromCall);
                                EventBusUtil.post(new AccessTokenResultEvent(boxUUID, boxBindValue, true, expireTimestamp));
                            }

                            @Override
                            public void onFailed() {
                                if (finalIsTrial) {
                                    invalidBox(boxUUID, finalBindValue, isFromCall);
                                } else {
                                    deleteBox(boxUUID, finalBindValue);
                                }
//                                retryCreateAuthToken(boxUUID, finalBindValue);
                                EventBusUtil.post(new AccessTokenResultEvent(boxUUID, finalBindValue, false, -1));
                            }

                            @Override
                            public void onError(int code, String msg) {
                                boolean isRetry = false;
                                switch (code) {
                                    case ConstantField.KnownError.SwitchPlatformError.REDIRECT_INVALID_ERROR:
                                    case ConstantField.KnownError.SwitchPlatformError.DOMAIN_NON_EXIST_ERROR:
                                        break;
                                    default:
                                        isRetry = true;
                                        break;
                                }
                                AccessTokenCreateEvent accessTokenCreateEvent = new AccessTokenCreateEvent(boxUUID, finalBindValue, code, isRetry, isForce);
                                EventBusUtil.post(accessTokenCreateEvent);
                                EventBusUtil.post(new AccessTokenResultEvent(boxUUID, finalBindValue, null, -1));
                            }
                        });
                    }
                    break;
                case "-1":
                    if (!TextUtils.isEmpty(authKeyValue) && !TextUtils.isEmpty(clientUUIDValue) && !TextUtils.isEmpty(boxPublicKeyValue)) {
                        String secretKey = DataUtil.getUID(UUID.randomUUID());
                        UserInfoUtil.createMemberToken(getApplicationContext(), boxUUID, finalBindValue
                                , boxPublicKeyValue, authKeyValue, clientUUIDValue, secretKey
                                , boxDomainValue, true, isForce, new CreateAuthTokenCallback() {
                                    @Override
                                    public void onSuccess(String boxUUID, String boxBindValue, String accessToken, String transformation, String initializationVector, String secret, String expiresAt, Long expiresAtEpochSeconds, String refreshToken, String requestId) {
                                        long expireTimestamp = accessTokenCallback(boxUUID, boxBindValue, accessToken, transformation, initializationVector, secret, expiresAt, expiresAtEpochSeconds, refreshToken, null, isForce, isFromCall);
                                        EventBusUtil.post(new AccessTokenResultEvent(boxUUID, boxBindValue, true, expireTimestamp));
                                    }

                                    @Override
                                    public void onFailed() {
                                        if (finalIsTrial) {
                                            invalidBox(boxUUID, finalBindValue, isFromCall);
                                        } else {
                                            deleteBox(boxUUID, finalBindValue);
                                        }
//                                        retryCreateAuthToken(boxUUID, finalBindValue);
                                        EventBusUtil.post(new AccessTokenResultEvent(boxUUID, finalBindValue, false, -1));
                                    }

                                    @Override
                                    public void onError(int code, String msg) {
                                        boolean isRetry = false;
                                        switch (code) {
                                            case ConstantField.KnownError.SwitchPlatformError.REDIRECT_INVALID_ERROR:
                                            case ConstantField.KnownError.SwitchPlatformError.DOMAIN_NON_EXIST_ERROR:
                                                break;
                                            default:
                                                isRetry = true;
                                                break;
                                        }
                                        AccessTokenCreateEvent accessTokenCreateEvent = new AccessTokenCreateEvent(boxUUID, finalBindValue, code, isRetry, isForce);
                                        EventBusUtil.post(accessTokenCreateEvent);
                                        EventBusUtil.post(new AccessTokenResultEvent(boxUUID, finalBindValue, null, -1));
                                    }
                                });
                    }
                    break;
                default:
                    if (refreshTokenValue != null && !TextUtils.isEmpty(refreshTokenValue)) {
                        long loginValidTime = -1L;
                        if (loginValid != null) {
                            try {
                                loginValidTime = Long.parseLong(loginValid);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                        if (loginValidTime < 0) {
                            GatewayUtil.refreshLoginAuthToken(getApplicationContext(), boxUUID, finalBindValue, boxDomainValue, boxPublicKeyValue, refreshTokenValue, true, isForce, new CreateAuthTokenCallback() {
                                @Override
                                public void onSuccess(String boxUUID, String boxBindValue, String accessToken, String transformation, String initializationVector, String secret, String expiresAt, Long expiresAtEpochSeconds, String refreshToken, String requestId) {
                                    long expireTimestamp = accessTokenCallback(boxUUID, boxBindValue, accessToken, transformation, initializationVector, secret, expiresAt, expiresAtEpochSeconds, refreshToken, null, isForce, isFromCall);
                                    EventBusUtil.post(new AccessTokenResultEvent(boxUUID, boxBindValue, true, expireTimestamp));
                                }

                                @Override
                                public void onFailed() {
                                    if (finalIsTrial) {
                                        invalidBox(boxUUID, finalBindValue, isFromCall);
                                    } else {
                                        deleteBox(boxUUID, finalBindValue);
                                    }
//                                    retryCreateAuthToken(boxUUID, finalBindValue);
                                    EventBusUtil.post(new AccessTokenResultEvent(boxUUID, finalBindValue, false, -1));
                                }

                                @Override
                                public void onError(int code, String msg) {
                                    boolean isRetry = false;
                                    switch (code) {
                                        case ConstantField.KnownError.SwitchPlatformError.REDIRECT_INVALID_ERROR:
                                        case ConstantField.KnownError.SwitchPlatformError.DOMAIN_NON_EXIST_ERROR:
                                            break;
                                        default:
                                            isRetry = true;
                                            break;
                                    }
                                    AccessTokenCreateEvent accessTokenCreateEvent = new AccessTokenCreateEvent(boxUUID, finalBindValue, code, isRetry, isForce);
                                    EventBusUtil.post(accessTokenCreateEvent);
                                    EventBusUtil.post(new AccessTokenResultEvent(boxUUID, finalBindValue, null, -1));
                                }
                            });
                        } else if (isForce || loginValidTime > System.currentTimeMillis()) {
                            GatewayUtil.authAutoLogin(getApplicationContext(), boxUUID, finalBindValue, boxDomainValue, boxPublicKeyValue, refreshTokenValue, !isForce, true, isForce, new AuthAutoLoginCallback() {
                                @Override
                                public void onSuccess(String boxUuid, String boxBind, int code, int httpCode, String accessToken, String refreshToken, AlgorithmConfig algorithmConfig, String encryptedSecret, String expiresAt, Long expiresAtEpochSeconds, String autoLoginExpiresAt) {
                                    if (code >= 200 && code < 400) {
                                        String transformation = null;
                                        String ivParams = null;
                                        if (algorithmConfig != null) {
                                            TransportationConfig transportationConfig = algorithmConfig.getTransportation();
                                            if (transportationConfig != null) {
                                                transformation = transportationConfig.getTransformation();
                                                ivParams = transportationConfig.getInitializationVector();
                                            }
                                        }
                                        accessTokenCallback(boxUuid, boxBind, accessToken, transformation, ivParams, encryptedSecret, expiresAt, expiresAtEpochSeconds, refreshToken, autoLoginExpiresAt, isForce, isFromCall);
                                        EventBusUtil.post(new AuthAutoLoginEvent(boxUuid, boxBind, code
                                                , FormatUtil.parseFileApiTimestamp(expiresAt, ConstantField.TimeStampFormat.FILE_API_DAY_FORMAT
                                                , ConstantField.TimeStampFormat.FILE_API_SPLIT), isForce));
                                    } else {
                                        EventBusUtil.post(new AuthAutoLoginEvent(boxUuid, boxBind, code
                                                , FormatUtil.parseFileApiTimestamp(expiresAt, ConstantField.TimeStampFormat.FILE_API_DAY_FORMAT
                                                , ConstantField.TimeStampFormat.FILE_API_SPLIT), isForce));
                                        handleAuthAutoLoginWrong(boxUuid, boxBind, code, finalIsTrial, isFromCall);
                                    }
                                }

                                @Override
                                public void onFailed(String boxUuid, String boxBind, int code, int httpCode) {
                                    EventBusUtil.post(new AuthAutoLoginEvent(boxUuid, boxBind, code, -1, isForce, httpCode));
                                    handleAuthAutoLoginWrong(boxUuid, boxBind, code, finalIsTrial, isFromCall);
                                }

                                @Override
                                public void onError(String boxUuid, String boxBind, int code, String errMsg) {
                                    EventBusUtil.post(new AuthAutoLoginEvent(boxUuid, boxBind, -1, -1, isForce, code));
                                }
                            });
                        }
                    }
                    break;
            }
        }
    }

    private void handleAuthAutoLoginWrong(String boxUuid, String boxBind, int code, boolean isTrial, boolean isFromCall) {
        if (code == ConstantField.KnownError.AutoLoginError.CONTINUE_WAITING || code == ConstantField.KnownError.AutoLoginError.LOGIN_REFUSE) {
            logoutBox(boxUuid, boxBind);
        } else if (code == ConstantField.KnownError.AutoLoginError.AUTO_LOGIN_INVALID) {
            if (isTrial) {
                invalidBox(boxUuid, boxBind, isFromCall);
            } else {
                deleteBox(boxUuid, boxBind);
            }
        }
    }

    private void logoutBox(String boxUuid, String boxBind) {
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
        queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(getApplicationContext(), queryMap);
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_STATUS)) {
                    boolean isUpdate = false;
                    String boxStatusValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_STATUS);
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
                        Map<String, String> newBoxValue = new HashMap<>();
                        newBoxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                        newBoxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                        newBoxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(boxStatus));
                        EulixSpaceDBUtil.updateBox(getApplicationContext(), newBoxValue);
                        resetSpacePoll(false);
                    }
                }
            }
        }
    }

    private void deleteBox(String boxUuid, String boxBind) {
        DataUtil.boxUnavailable(boxUuid, boxBind);
        Map<String, String> deleteMap = new HashMap<>();
        deleteMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
        deleteMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
        EulixSpaceDBUtil.deleteBox(getApplicationContext(), deleteMap);
        BoxInsertDeleteEvent boxInsertDeleteEvent = new BoxInsertDeleteEvent(boxUuid, boxBind, false);
        EventBusUtil.post(boxInsertDeleteEvent);
    }

    private void invalidBox(String boxUuid, String boxBind, boolean isFromCall) {
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
        queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(getApplicationContext(), queryMap);
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_STATUS)) {
                    boolean isUpdate = false;
                    boolean isUsing = false;
                    String boxStatusValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_STATUS);
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
                        case ConstantField.EulixDeviceStatus.OFFLINE_USE:
                            isUsing = true;
                            break;
                        default:
                            break;
                    }
                    if (boxStatus != ConstantField.EulixDeviceStatus.INVALID) {
                        isUpdate = true;
                        boxStatus = ConstantField.EulixDeviceStatus.INVALID;
                    }
                    if (isUpdate) {
                        Map<String, String> newBoxValue = new HashMap<>();
                        newBoxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                        newBoxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                        newBoxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(boxStatus));
                        EulixSpaceDBUtil.updateBox(getApplicationContext(), newBoxValue);
                    }
                    if (isUsing) {
                        resetSpacePoll(false);
                        if (isFromCall) {
                            if (eulixPushManager == null) {
                                eulixPushManager = EulixPushManager.getInstance();
                            }
                            eulixPushManager.handleNativePushMessage(boxUuid, boxBind, ConstantField.PushType.NativeType.TRIAL_INVALID);
                        }
                    }
                }
            }
        }
    }

    private long accessTokenCallback(String boxUUID, String boxBind, String accessToken, String transformation, String initializationVector, String secret, String expiresAt, Long expiresAtEpochSeconds, String refreshToken, String autoLoginExpiresAt, boolean isForce, boolean isFromCall) {
        long currentTimestamp = System.currentTimeMillis();
        long expireTimestamp = FormatUtil
                .parseFileApiTimestamp(expiresAt, ConstantField.TimeStampFormat.FILE_API_DAY_FORMAT, ConstantField.TimeStampFormat.FILE_API_SPLIT);
        if (expireTimestamp < 0L || expireTimestamp > (currentTimestamp + 10 * 1000)) {
            Map<String, String> boxValue = new HashMap<>();
            String loginValid = null;
            if (boxUUID != null && !TextUtils.isEmpty(boxUUID) && boxBind != null && !TextUtils.isEmpty(boxBind)) {
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUUID);
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                if (!TextUtils.isEmpty(refreshToken) || !TextUtils.isEmpty(initializationVector) || !TextUtils.isEmpty(transformation)) {
                    String boxTokenValue = null;
                    Map<String, String> queryMap = new HashMap<>();
                    queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUUID);
                    queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                    List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(getApplicationContext(), queryMap);
                    if (boxValues != null) {
                        for (Map<String, String> boxV : boxValues) {
                            if (boxV != null && boxV.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
                                boxTokenValue = boxV.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                                break;
                            }
                        }
                    }
                    EulixBoxToken boxToken = null;
                    if (boxTokenValue != null) {
                        try {
                            boxToken = new Gson().fromJson(boxTokenValue, EulixBoxToken.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                    if (boxToken == null) {
                        boxToken = new EulixBoxToken();
                    } else {
                        loginValid = boxToken.getLoginValid();
                    }
                    if (!TextUtils.isEmpty(accessToken)) {
                        boxToken.setAccessToken(accessToken);
                    }
                    if (!TextUtils.isEmpty(secret)) {
                        boxToken.setSecretKey(secret);
                    }
                    if (!TextUtils.isEmpty(expiresAt)) {
                        boxToken.setTokenExpire(expireTimestamp);
                    }
                    if (!TextUtils.isEmpty(refreshToken)) {
                        boxToken.setRefreshToken(refreshToken);
                    }
                    if (!TextUtils.isEmpty(initializationVector)) {
                        boxToken.setInitializationVector(initializationVector);
                    }
                    if (!TextUtils.isEmpty(transformation)) {
                        boxToken.setTransformation(transformation);
                    }
                    if (autoLoginExpiresAt != null) {
                        long autoLoginExpireTimestamp = FormatUtil.parseFileApiTimestamp(autoLoginExpiresAt, ConstantField.TimeStampFormat.FILE_API_DAY_FORMAT, ConstantField.TimeStampFormat.FILE_API_SPLIT);
                        if (autoLoginExpireTimestamp >= 0) {
                            boxToken.setLoginValid(String.valueOf(autoLoginExpireTimestamp));
                        }
                    }
                    boxValue.put(EulixSpaceDBManager.FIELD_BOX_TOKEN, new Gson().toJson(boxToken, EulixBoxToken.class));
                }
            }
            EulixSpaceDBUtil.updateBox(getApplicationContext(), boxValue);
            Integer boxAlarmId = DataUtil.getTokenAlarmId(boxUUID, boxBind);
            if (boxAlarmId != null) {
                AlarmUtil.cancelAlarm(getApplicationContext(), boxAlarmId);
            }
            if ("1".equals(boxBind) || "-1".equals(boxBind) || loginValid == null) {
                int alarmId = AlarmUtil.getAlarmId();
                DataUtil.setTokenAlarmId(boxUUID, boxBind, alarmId);
                long diffTimestamp = 60 * 1000L;
                if (expireTimestamp > currentTimestamp) {
                    diffTimestamp = Math.min(((expireTimestamp - currentTimestamp) / 10), diffTimestamp);
                    int id = AlarmUtil.setAlarm(getApplicationContext(), (expireTimestamp - diffTimestamp), alarmId, boxUUID, boxBind, (diffTimestamp / 2));
                    if (id == 0) {
                        prepareAuthToken(boxUUID, boxBind, isForce, isFromCall);
                    }
                } else {
                    AlarmUtil.setAlarm(getApplicationContext(), (currentTimestamp + diffTimestamp), alarmId, boxUUID, boxBind, (diffTimestamp / 2));
                }
            } else {
                long loginValidValue = 0L;
                try {
                    loginValidValue = Long.parseLong(loginValid);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                if (loginValidValue > 0) {
                    int alarmId = AlarmUtil.getAlarmId();
                    DataUtil.setTokenAlarmId(boxUUID, boxBind, alarmId);
                    long diffTimestamp = 60 * 1000L;
                    long realExpireTimestamp = Math.min(expireTimestamp, loginValidValue);
                    if (realExpireTimestamp > currentTimestamp) {
                        AlarmUtil.setAlarm(getApplicationContext(), realExpireTimestamp, alarmId, boxUUID, boxBind, (diffTimestamp / 2));
                    }
                }
            }
            AccessTokenCreateEvent accessTokenCreateEvent = new AccessTokenCreateEvent(boxUUID, boxBind);
            EventBusUtil.post(accessTokenCreateEvent);
        } else {
            Logger.w(TAG, "Obtain access token expired!");
//            retryCreateAuthToken(boxUUID, boxBind, isForce, isFromCall);
            AccessTokenCreateEvent retryAccessTokenCreateEvent = new AccessTokenCreateEvent(boxUUID, boxBind, -1, true, isForce);
            EventBusUtil.post(retryAccessTokenCreateEvent);
        }
        return expireTimestamp;
    }

    private void handleNotification(boolean isFore) {
        if (isFore && !isForeground) {
            Intent launcherIntent = new Intent();
            launcherIntent.setComponent(new ComponentName(getPackageName(), "xyz.eulix.space.EulixForeActivity"));
            showNotification(ConstantField.RequestCode.EULIX_SPACE_FOREGROUND_ID, launcherIntent
                    , getString(R.string.app_name), getString(R.string.continue_to_serve_you)
                    , (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O
                            ? NotificationManager.IMPORTANCE_DEFAULT : 0), true, null);
            if (isRequestCreateAuthToken && mHandler != null) {
                while (mHandler.hasMessages(CREATE_AUTH_TOKEN)) {
                    mHandler.removeMessages(CREATE_AUTH_TOKEN);
                }
                List<Integer> boxAlarmIds = DataUtil.getTokenAlarmIds();
                for (int boxAlarmId : boxAlarmIds) {
                    AlarmUtil.cancelAlarm(getApplicationContext(), boxAlarmId);
                }
            }
            resetSpacePoll(false);
        } else if (isForeground && !isFore) {
            resetSpacePoll(true);
            stopForeground(true);
            dismissNotification(null);
            if (isRequestCreateAuthToken && mHandler != null && !mHandler.hasMessages(CREATE_AUTH_TOKEN)) {
                Message message = mHandler.obtainMessage(CREATE_AUTH_TOKEN, 0, 0);
                mHandler.sendMessage(message);
            }
        }
    }

    /**
     * 下载应用apk
     *
     * @param appName
     * @param downloadUrl
     * @param md5
     * @param isForce
     */
    private void downloadApk(final String appName, final String downloadUrl, final String md5, final boolean isForce) {
        String savePathValue = null;
        File saveFile = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (saveFile != null) {
            savePathValue = saveFile.getAbsolutePath();
        } else {
            savePathValue = getFilesDir().getAbsolutePath();
        }
        final String savePath = savePathValue;
        try {
            ThreadPool.getInstance().execute(() -> AppInfoUtil.downloadApk(appName, downloadUrl, savePath, new AppDownloadCallback() {
                @Override
                public void onError(String msg) {
                    AppInstallEvent appInstallEvent = new AppInstallEvent(false, null, isForce);
                    EventBusUtil.post(appInstallEvent);
                }

                @Override
                public void onFailed() {
                    AppInstallEvent appInstallEvent = new AppInstallEvent(false, null, isForce);
                    EventBusUtil.post(appInstallEvent);
                }

                @Override
                public void onSuccess(String filePath) {
                    boolean isSuccess = false;
                    File file = new File(filePath);
                    if (file.exists()) {
                        String fileMD5 = null;
                        try {
                            fileMD5 = MD5Util.getFileMD5String(file);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (!StringUtil.isNonBlankString(md5) || (fileMD5 != null && fileMD5.equals(md5))) {
                            isSuccess = true;
                            Logger.d(TAG, "download success: " + filePath);
//                            if (mCallback != null) {
//                                mCallback.appInstall(filePath);
//                            }
                        }
                    }
                    AppInstallEvent appInstallEvent = new AppInstallEvent(isSuccess, filePath, isForce);
                    EventBusUtil.post(appInstallEvent);
                }
            }));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取指定盒子的存储情况
     *
     * @param boxUUID   指定盒子uuid
     * @param boxBind   指定盒子绑定状态
     * @param boxDomain 指定盒子域名
     */
    private void getDeviceStorageInfo(String boxUUID, String boxBind, String boxDomain) {
        String clientUuid = getUUID();
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(getApplicationContext(), true);
        if (gatewayCommunicationBase != null) {
            if (clientUuid != null) {
                AccountInfoUtil.getDeviceStorageInfo(getApplicationContext(), boxUUID
                        , clientUuid, boxDomain, gatewayCommunicationBase.getAccessToken()
                        , gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getTransformation()
                        , gatewayCommunicationBase.getIvParams(), true, new DeviceInfoCallback() {
                            @Override
                            public void onError(String msg) {
                                // Do nothing
                            }

                            @Override
                            public void onFailed() {
                                // Do nothing
                            }

                            @Override
                            public void onSuccess(String boxUuid, String requestId, long usedSize, long totalSize) {
                                if (boxUuid != null) {
                                    totalSize = Math.max(totalSize, 0);
                                    usedSize = Math.max(Math.min(usedSize, totalSize), 0);

                                    List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(getApplicationContext(), EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                                    boolean isUpdate = false;
                                    if (boxValues != null) {
                                        for (Map<String, String> boxV : boxValues) {
                                            if (boxV != null && boxV.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                                                String boxBindValue = boxV.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                                                boolean isHandle = false;
                                                JSONObject jsonObject = null;
                                                EulixSpaceDBBoxManager eulixSpaceDBBoxManager = EulixSpaceDBBoxManager.getInstance(boxUuid, boxBindValue);
                                                if (eulixSpaceDBBoxManager != null) {
                                                    isHandle = true;
                                                    jsonObject = new JSONObject();
                                                    try {
                                                        jsonObject.put("totalSize", totalSize);
                                                        jsonObject.put("usedSize", usedSize);
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                        isHandle = false;
                                                    }
                                                }
                                                if (isHandle) {
                                                    long finalUsedSize = usedSize;
                                                    long finalTotalSize = totalSize;
                                                    int result = eulixSpaceDBBoxManager.updateBoxInfo(jsonObject, isUpdate1 -> {
                                                        if (isUpdate1) {
                                                            StorageInfoResponseEvent responseEvent = new StorageInfoResponseEvent(boxUuid, finalUsedSize, finalTotalSize);
                                                            EventBusUtil.post(responseEvent);
                                                        }
                                                    });
                                                    isUpdate = (result > 0);
                                                } else {
                                                    EulixBoxInfo eulixBoxInfo = null;
                                                    String boxInfoValue = null;
                                                    if (boxV.containsKey(EulixSpaceDBManager.FIELD_BOX_INFO)) {
                                                        boxInfoValue = StringUtil.nullToEmpty(boxV.get(EulixSpaceDBManager.FIELD_BOX_INFO));
                                                    }
                                                    if (boxInfoValue != null) {
                                                        if (TextUtils.isEmpty(boxInfoValue.trim())) {
                                                            eulixBoxInfo = new EulixBoxInfo();
                                                        } else {
                                                            try {
                                                                eulixBoxInfo = new Gson().fromJson(boxInfoValue, EulixBoxInfo.class);
                                                            } catch (JsonSyntaxException e) {
                                                                e.printStackTrace();
                                                            }
                                                        }
                                                    }
                                                    if (eulixBoxInfo == null && !"1".equals(boxBindValue)) {
                                                        eulixBoxInfo = new EulixBoxInfo();
                                                    }
                                                    if (boxBindValue != null && eulixBoxInfo != null) {
                                                        Map<String, String> boxValue = new HashMap<>();
                                                        boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                                                        boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBindValue);
                                                        eulixBoxInfo.setUsedSize(usedSize);
                                                        eulixBoxInfo.setTotalSize(totalSize);
                                                        boxValue.put(EulixSpaceDBManager.FIELD_BOX_INFO, new Gson().toJson(eulixBoxInfo, EulixBoxInfo.class));
                                                        EulixSpaceDBUtil.updateBox(getApplicationContext(), boxValue);
                                                        isUpdate = true;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (isUpdate) {
                                        StorageInfoResponseEvent responseEvent = new StorageInfoResponseEvent(boxUuid, usedSize, totalSize);
                                        EventBusUtil.post(responseEvent);
                                    }
                                }
                            }
                        });
            }
        } else {
            createAuthToken(boxUUID, boxBind, false, false);
        }
    }

    private void getDeviceHardwareInfo(String boxUuid, String boxBind, String requestUuid, boolean isFore) {
        boolean isHandle = false;
        if (boxUuid != null && boxBind != null) {
            int identity = EulixSpaceDBUtil.getDeviceUserIdentity(getApplicationContext(), boxUuid, boxBind);
            if (identity == ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY || identity == ConstantField.UserIdentity.ADMINISTRATOR_GRANTEE) {
                GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(getApplicationContext(), boxUuid, boxBind);
                if (gatewayCommunicationBase != null) {
                    isHandle = true;
                    String baseUrl = Urls.getBaseUrl();
                    EulixSecurityUtil.getDeviceHardwareInfo(baseUrl
                            , gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                            , gatewayCommunicationBase.getIvParams(), isFore, new DeviceHardwareInfoCallback() {
                                @Override
                                public void onSuccess(String source, int code, String message, String requestId, DeviceHardwareInfoResult result) {
                                    handleDeviceHardwareInfoResult(boxUuid, boxBind, code, source, result, requestUuid);
                                }

                                @Override
                                public void onFailed(String source, int code, String message, String requestId) {
                                    handleDeviceHardwareInfoResult(boxUuid, boxBind, code, source, null, requestUuid);
                                }

                                @Override
                                public void onError(String errMsg) {
                                    EventBusUtil.post(new DeviceHardwareInfoResponseEvent(requestUuid, -1, null, null));
                                }
                            });
                }
            }
        }
        if (!isHandle) {
            EventBusUtil.post(new DeviceHardwareInfoResponseEvent(requestUuid, -1, null, null));
        }
    }

    private void handleDeviceHardwareInfoResult(String boxUuid, String boxBind, int code, String source, DeviceHardwareInfoResult deviceHardwareInfoResult, String requestUuid) {
        String bluetoothId = null;
        if (code >= 200 && code < 400 && deviceHardwareInfoResult != null) {
            bluetoothId = deviceHardwareInfoResult.getBluetoothId();
        }
        boolean isPostEvent = false;
        boolean isUpdate = false;
        if (boxUuid != null && boxBind != null && bluetoothId != null) {
            boolean isHandle = false;
            JSONObject jsonObject = null;
            EulixSpaceDBBoxManager eulixSpaceDBBoxManager = EulixSpaceDBBoxManager.getInstance(boxUuid, boxBind);
            if (eulixSpaceDBBoxManager != null) {
                isHandle = true;
                jsonObject = new JSONObject();
                try {
                    jsonObject.put("bluetoothId", bluetoothId);
                } catch (JSONException e) {
                    e.printStackTrace();
                    isHandle = false;
                }
            }
            if (isHandle) {
                String finalBluetoothId = bluetoothId;
                int result = eulixSpaceDBBoxManager.updateBoxInfo(jsonObject, isUpdate1 -> EventBusUtil.post(new DeviceHardwareInfoResponseEvent(requestUuid, code, source, finalBluetoothId)));
                isPostEvent = (result >= 0);
            } else {
                isPostEvent = true;
                Map<String, String> queryMap = new HashMap<>();
                queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(getApplicationContext(), queryMap);
                EulixBoxInfo eulixBoxInfo = null;
                if (boxValues != null) {
                    for (Map<String, String> boxValue : boxValues) {
                        if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_INFO)) {
                            String boxInfoValue = StringUtil.nullToEmpty(boxValue.get(EulixSpaceDBManager.FIELD_BOX_INFO));
                            if (TextUtils.isEmpty(boxInfoValue.trim())) {
                                eulixBoxInfo = new EulixBoxInfo();
                            } else {
                                try {
                                    eulixBoxInfo = new Gson().fromJson(boxInfoValue, EulixBoxInfo.class);
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                                if (eulixBoxInfo == null && !"1".equals(boxBind)) {
                                    eulixBoxInfo = new EulixBoxInfo();
                                }
                            }
                            break;
                        }
                    }
                }
                if (eulixBoxInfo != null) {
                    if (!bluetoothId.equals(eulixBoxInfo.getBluetoothId())) {
                        isUpdate = true;
                        eulixBoxInfo.setBluetoothId(bluetoothId);
                    }
                    if (isUpdate) {
                        Map<String, String> boxV = new HashMap<>();
                        boxV.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                        boxV.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                        boxV.put(EulixSpaceDBManager.FIELD_BOX_INFO, new Gson().toJson(eulixBoxInfo, EulixBoxInfo.class));
                        EulixSpaceDBUtil.updateBox(getApplicationContext(), boxV);
                    }
                }
            }
        } else {
            EventBusUtil.post(new DeviceHardwareInfoResponseEvent(requestUuid, code, source, bluetoothId));
        }
        if (isPostEvent) {
            EventBusUtil.post(new DeviceHardwareInfoResponseEvent(requestUuid, code, source, bluetoothId));
        }
    }

    public void pollSecurityMessage(String boxUuid, String boxBind, String requestUuid) {
        boolean isHandle = false;
        if (boxUuid != null && boxBind != null && requestUuid != null) {
            GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(EulixSpaceApplication.getContext(), boxUuid, boxBind);
            int identity = EulixSpaceDBUtil.getActiveDeviceUserIdentity(EulixSpaceApplication.getContext());
            if (gatewayCommunicationBase != null && identity == ConstantField.UserIdentity.ADMINISTRATOR_GRANTEE) {
                isHandle = true;
                EulixSecurityUtil.securityMessagePoll(DataUtil.getClientUuid(EulixSpaceApplication.getContext())
                        , gatewayCommunicationBase.getBoxDomain(), gatewayCommunicationBase.getAccessToken()
                        , gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getIvParams(), new SecurityMessagePollCallback() {
                            @Override
                            public void onSuccess(String source, int code, String message, String requestId, List<SecurityMessagePollResult> results) {
                                SecurityMessagePollResult securityMessagePollResult = null;
                                if (results != null) {
                                    for (SecurityMessagePollResult result : results) {
                                        if (result != null && requestUuid.equals(result.getApplyId())) {
                                            securityMessagePollResult = result;
                                            break;
                                        }
                                    }
                                }
                                EventBusUtil.post(new SecurityMessagePollResponseEvent(source, code, requestUuid, securityMessagePollResult));
                            }

                            @Override
                            public void onFailed() {
                                EventBusUtil.post(new SecurityMessagePollResponseEvent(null, -2, requestUuid, null));
                            }

                            @Override
                            public void onError(String errMsg) {
                                EventBusUtil.post(new SecurityMessagePollResponseEvent(null, -1, requestUuid, null));
                            }
                        });
            }
        }
        if (!isHandle) {
            EventBusUtil.post(new SecurityMessagePollResponseEvent(null, -3, requestUuid, null));
        }
    }

    /**
     * 获取成员列表
     *
     * @param boxUUID        指定盒子uuid
     * @param boxBind        指定盒子绑定状态
     * @param boxDomain      指定盒子域名
     * @param isUpdateAvatar 是否更新头像
     */
    private void getMemberList(String boxUUID, String boxBind, String boxDomain, String refreshId, boolean isUpdateAvatar, boolean isFore) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(getApplicationContext(), boxUUID, boxBind);
        if (gatewayCommunicationBase != null) {
            UserInfoUtil.getMemberList(getApplicationContext(), boxUUID, boxDomain
                    , gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                    , gatewayCommunicationBase.getIvParams(), true, isFore, new AccountInfoCallback() {
                        @Override
                        public void onSuccess(String code, String message, String requestId, String boxUuid, List<PersonalInfoResult> personalInfoResults) {
                            if (code != null && code.equals(String.valueOf(ConstantField.KnownError.MEMBER_LIST_ERROR_CODE))) {
                                createAuthToken(boxUUID, boxBind, false, false);
                            } else {
                                List<String> clientUuids = null;
                                Boolean isExist = null;
                                boolean deleteSelf = false;
                                if (boxUuid != null) {
                                    isExist = false;
                                    Map<String, String> boxValue = new HashMap<>();
                                    boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                                    String userInfoValue = null;
                                    Map<String, UserInfo> userInfoMap = null;
                                    Map<String, UserInfo> userInfoCurrentMap = null;
                                    String userInfoCurrentValue = null;
                                    Map<String, String> queryMap = new HashMap<>();
                                    queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                                    queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                                    List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(getApplicationContext(), queryMap);
                                    String currentUserDomain = null;
                                    if (boxValues != null) {
                                        for (Map<String, String> boxV : boxValues) {
                                            if (boxV != null && boxV.containsKey(EulixSpaceDBManager.FIELD_BOX_USER_INFO)
                                                    && boxV.containsKey(EulixSpaceDBManager.FIELD_BOX_DOMAIN)) {
                                                userInfoCurrentValue = boxV.get(EulixSpaceDBManager.FIELD_BOX_USER_INFO);
                                                currentUserDomain = boxV.get(EulixSpaceDBManager.FIELD_BOX_DOMAIN);
                                                break;
                                            }
                                        }
                                    }
                                    if (userInfoCurrentValue != null) {
                                        try {
                                            userInfoCurrentMap = new Gson().fromJson(userInfoCurrentValue, new TypeToken<Map<String, UserInfo>>() {
                                            }.getType());
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    if (personalInfoResults != null) {
                                        clientUuids = new ArrayList<>();
                                        for (PersonalInfoResult personalInfoResult : personalInfoResults) {
                                            if (personalInfoResult != null) {
                                                String uuid = personalInfoResult.getClientUuid();
                                                if (uuid == null) {
                                                    uuid = personalInfoResult.getGlobalId();
                                                }
                                                if (uuid != null) {
                                                    if (userInfoMap == null) {
                                                        userInfoMap = new HashMap<>();
                                                    }
                                                    UserInfo userInfo = new UserInfo();
                                                    userInfo.setUserCreateTimestamp(FormatUtil.parseFileApiTimestamp(personalInfoResult.getCreateAt()
                                                            , ConstantField.TimeStampFormat.FILE_API_DAY_FORMAT, ConstantField.TimeStampFormat.FILE_API_SPLIT));
                                                    userInfo.setUserId(personalInfoResult.getGlobalId());
                                                    userInfo.setNickName(personalInfoResult.getPersonalName());
                                                    userInfo.setSignature(personalInfoResult.getPersonalSign());
                                                    userInfo.setUserDomain(personalInfoResult.getUserDomain());
                                                    userInfo.setAvatarMD5(personalInfoResult.getImageMD5());
                                                    DeviceInfo deviceInfo = new DeviceInfo();
                                                    deviceInfo.setPhoneModel(personalInfoResult.getPhoneModel());
                                                    userInfo.setDeviceInfo(new Gson().toJson(deviceInfo, DeviceInfo.class));
                                                    if (userInfoCurrentMap != null && userInfoCurrentMap.containsKey(uuid)) {
                                                        UserInfo info = userInfoCurrentMap.get(uuid);
                                                        if (info != null) {
                                                            userInfo.setAvatarPath(info.getAvatarPath());
                                                            userInfo.setUsedSize(info.getUsedSize());
                                                            userInfo.setTotalSize(info.getTotalSize());
                                                        }
                                                    }
                                                    Boolean isAdmin = null;
                                                    if (personalInfoResult.getRole() != null) {
                                                        switch (personalInfoResult.getRole()) {
                                                            case ConstantField.Role.ADMINISTRATOR:
                                                                isAdmin = true;
                                                                break;
                                                            case ConstantField.Role.GUEST:
                                                                isAdmin = false;
                                                                break;
                                                            default:
                                                                break;
                                                        }
                                                    }
                                                    boolean isMyself = false;
                                                    if (boxBind != null) {
                                                        String aoId = personalInfoResult.getGlobalId();
                                                        if ("1".equals(boxBind) || "-1".equals(boxBind)) {
                                                            isMyself = uuid.equals(getUUID());
                                                            if (isMyself && EulixSpaceDBUtil.containsBox(getApplicationContext(), boxUuid, aoId)) {
                                                                Logger.d(TAG, "family granter delete: " + boxUuid + ", " + aoId);
                                                                deleteBox(boxUuid, aoId);
                                                            }
                                                        } else {
                                                            isMyself = boxBind.equals(aoId);
                                                            if (isMyself && (boxBind.equals(EulixSpaceDBUtil.getClientAoId(getApplicationContext(), boxUuid, "1"))
                                                                    || boxBind.equals(EulixSpaceDBUtil.getClientAoId(getApplicationContext(), boxUuid, "-1")))) {
                                                                deleteSelf = true;
                                                            }
                                                        }
                                                    }
                                                    if (isMyself) {
                                                        isExist = true;
                                                        // 网络高可用动态更新域名
                                                        String userDomain = personalInfoResult.getUserDomain();
                                                        if (FormatUtil.isHttpUrlString(FormatUtil.generateHttpUrlString(userDomain))
                                                                && AOSpaceUtil.isInternetAccessEnable(getApplicationContext(), boxUuid, boxBind)
                                                                && userDomain != null && !userDomain.equals(currentUserDomain)) {
                                                            Map<String, String> myBoxValue = new HashMap<>();
                                                            myBoxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                                                            myBoxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                                                            myBoxValue.put(EulixSpaceDBManager.FIELD_BOX_DOMAIN, userDomain);
                                                            EulixSpaceDBUtil.updateBox(getApplicationContext(), myBoxValue);
                                                            Logger.d(TAG, "obtain: " + userDomain + ", origin: " + currentUserDomain);
                                                        }
                                                    } else {
                                                        clientUuids.add(uuid);
                                                    }
                                                    if (isAdmin == null) {
                                                        isAdmin = false;
                                                    }
                                                    userInfo.setAdmin(isAdmin);
                                                    userInfoMap.put(uuid, userInfo);
                                                }
                                            }
                                        }
                                    }
                                    if (userInfoMap != null) {
                                        userInfoValue = new Gson().toJson(userInfoMap, new TypeToken<Map<String, UserInfo>>() {
                                        }.getType());
                                    }
                                    if (userInfoValue == null) {
                                        userInfoValue = "";
                                    }
                                    boxValue.put(EulixSpaceDBManager.FIELD_BOX_USER_INFO, userInfoValue);
                                    EulixSpaceDBUtil.updateBox(getApplicationContext(), boxValue);
                                    if ("0".equals(boxBind)) {
                                        // todo 扫码尚不知晓
                                        isExist = true;
                                    }
                                }
                                if (deleteSelf) {
                                    Logger.d(TAG, "family grantee delete self: " + boxUuid + ", " + boxBind);
                                    deleteBox(boxUuid, boxBind);
                                } else if (isExist != null) {
                                    if (isExist) {
                                        if (clientUuids != null) {
                                            for (String clientUuid : clientUuids) {
                                                if (clientUuid != null) {
                                                    UserInfoUtil.getHeaderImage(getApplicationContext(), clientUuid);
                                                }
                                            }
                                        }
                                        getMemberInfo(isUpdateAvatar);
                                    } else {
                                        // 统一由token接口处理删除操作
//                                        Map<String, String> deleteMap = new HashMap<>();
//                                        deleteMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
//                                        deleteMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
//                                        EulixSpaceDBUtil.deleteBox(getApplicationContext(), deleteMap);
                                    }
                                }
                            }
                            MemberResultEvent memberResultEvent = new MemberResultEvent(refreshId);
                            EventBusUtil.post(memberResultEvent);
                        }

                        @Override
                        public void onFailed(String code, String message, String requestId) {
                            MemberResultEvent memberResultEvent = new MemberResultEvent(refreshId);
                            EventBusUtil.post(memberResultEvent);
                        }

                        @Override
                        public void onError(String msg) {
                            MemberResultEvent memberResultEvent = new MemberResultEvent(refreshId);
                            EventBusUtil.post(memberResultEvent);
                        }
                    });
        } else {
            EventBusUtil.post(new MemberResultEvent(refreshId));
            createAuthToken(boxUUID, boxBind, false, false);
        }
    }

    private void getMemberInfo(boolean isUpdateAvatar) {
        UserInfoUtil.getUserInfo(getApplicationContext(), true);
        UserInfoUtil.getMemberUsedStorage(getApplicationContext());
        if (isUpdateAvatar) {
            UserInfoUtil.getHeaderImage(getApplicationContext());
        }
    }

    private void getTerminalList(String boxUuid, String boxBind, boolean isFore) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(getApplicationContext(), boxUuid, boxBind);
        String aoId = EulixSpaceDBUtil.getClientAoId(getApplicationContext(), boxUuid, boxBind);
        if (gatewayCommunicationBase != null) {
            UserInfoUtil.getTerminalList(getApplicationContext(), boxUuid, gatewayCommunicationBase.getBoxDomain()
                    , aoId, gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                    , gatewayCommunicationBase.getIvParams(), true, isFore, new TerminalListCallback() {
                        @Override
                        public void onSuccess(int code, String message, String requestId, List<TerminalInfoResult> results) {
                            boolean isPostEvent = true;
                            if (boxUuid != null && boxBind != null) {
                                Map<String, String> queryMap = new HashMap<>();
                                queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                                queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                                List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(getApplicationContext(), queryMap);
                                String currentRegKey = null;
                                if (boxValues != null) {
                                    for (Map<String, String> boxV : boxValues) {
                                        if (boxV != null && boxV.containsKey(EulixSpaceDBManager.FIELD_BOX_REGISTER)) {
                                            currentRegKey = boxV.get(EulixSpaceDBManager.FIELD_BOX_REGISTER);
                                            break;
                                        }
                                    }
                                }
                                Map<String, TerminalInfo> terminalInfoMap = null;
                                if (results != null) {
                                    terminalInfoMap = new HashMap<>();
                                    for (TerminalInfoResult terminalInfoResult : results) {
                                        if (terminalInfoResult != null) {
                                            String clientUuid = terminalInfoResult.getUuid();
                                            if (clientUuid != null) {
                                                if (clientUuid.equals(DataUtil.getClientUuid(getApplicationContext()))) {
                                                    String regKey = terminalInfoResult.getClientRegisterKey();
                                                    if (regKey != null && !TextUtils.isEmpty(regKey) && !regKey.equals(currentRegKey)) {
                                                        Map<String, String> myBoxValue = new HashMap<>();
                                                        myBoxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                                                        myBoxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                                                        myBoxValue.put(EulixSpaceDBManager.FIELD_BOX_REGISTER, regKey);
                                                        EulixSpaceDBUtil.updateBox(getApplicationContext(), myBoxValue);
                                                    }
                                                }
                                                TerminalInfo terminalInfo = new TerminalInfo();
                                                terminalInfo.setName(terminalInfoResult.getTerminalModel());
                                                terminalInfo.setType(terminalInfoResult.getTerminalType());
                                                terminalInfo.setPlace(terminalInfoResult.getAddress());
                                                terminalInfo.setTimestamp(FormatUtil.parseFileApiTimestamp(terminalInfoResult.getLoginTime()
                                                        , ConstantField.TimeStampFormat.FILE_API_DAY_FORMAT, ConstantField.TimeStampFormat.FILE_API_SPLIT));
                                                terminalInfoMap.put(clientUuid, terminalInfo);
                                            }
                                        }
                                    }
                                }
                                if (terminalInfoMap != null) {
                                    boolean isHandle = false;
                                    JSONObject jsonObject = null;
                                    EulixSpaceDBBoxManager eulixSpaceDBBoxManager = EulixSpaceDBBoxManager.getInstance(boxUuid, boxBind);
                                    if (eulixSpaceDBBoxManager != null) {
                                        isHandle = true;
                                        jsonObject = new JSONObject();
                                        try {
                                            jsonObject.put("terminalInfoMap", new Gson().toJson(terminalInfoMap, new TypeToken<Map<String, TerminalInfo>>() {
                                            }.getType()));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                            isHandle = false;
                                        }
                                    }
                                    if (isHandle) {
                                        int result = eulixSpaceDBBoxManager.updateBoxOtherInfo(jsonObject, isUpdate -> {
                                            TerminalResultEvent terminalResultEvent = new TerminalResultEvent();
                                            EventBusUtil.post(terminalResultEvent);
                                        });
                                        isPostEvent = (result >= 0);
                                    } else {
                                        EulixBoxOtherInfo eulixBoxOtherInfo = EulixSpaceDBUtil.getBoxOtherInfo(getApplicationContext(), boxUuid, boxBind);
                                        if (eulixBoxOtherInfo == null) {
                                            eulixBoxOtherInfo = new EulixBoxOtherInfo();
                                        }
                                        eulixBoxOtherInfo.setTerminalInfoMap(terminalInfoMap);
                                        Map<String, String> boxValue = new HashMap<>();
                                        boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                                        boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                                        boxValue.put(EulixSpaceDBManager.FIELD_BOX_OTHER_INFO, new Gson().toJson(eulixBoxOtherInfo, EulixBoxOtherInfo.class));
                                        EulixSpaceDBUtil.updateBox(getApplicationContext(), boxValue);
                                    }
                                }
                            }
                            if (isPostEvent) {
                                TerminalResultEvent terminalResultEvent = new TerminalResultEvent();
                                EventBusUtil.post(terminalResultEvent);
                            }
                        }

                        @Override
                        public void onFail(int code, String message, String requestId) {
                            TerminalResultEvent terminalResultEvent = new TerminalResultEvent();
                            EventBusUtil.post(terminalResultEvent);
                        }

                        @Override
                        public void onError(String errMsg) {
                            TerminalResultEvent terminalResultEvent = new TerminalResultEvent();
                            EventBusUtil.post(terminalResultEvent);
                        }
                    });
        }
    }

    private void getDiskManagementList(String boxUuid, String boxBind, String requestUuid, boolean isFore) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(getApplicationContext(), boxUuid, boxBind);
        if (gatewayCommunicationBase != null) {
            DiskUtil.getDiskManagementList(gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                    , gatewayCommunicationBase.getIvParams(), isFore, new DiskManagementListCallback() {
                        @Override
                        public void onSuccess(int code, String source, String message, String requestId, DiskManageListResult result) {
                            if (result != null) {
                                boolean isStorageDiskManageList = false;
                                boolean isHandle = false;
                                JSONObject jsonObject = null;
                                EulixSpaceDBBoxManager eulixSpaceDBBoxManager = EulixSpaceDBBoxManager.getInstance(boxUuid, boxBind);
                                if (eulixSpaceDBBoxManager != null) {
                                    isHandle = true;
                                    jsonObject = new JSONObject();
                                    try {
                                        jsonObject.put("diskManageListResult", new Gson().toJson(result, DiskManageListResult.class));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        isHandle = false;
                                    }
                                }
                                if (isHandle) {
                                    int resultCode = eulixSpaceDBBoxManager.updateBoxInfo(jsonObject, isUpdate -> {
                                        EventBusUtil.post(new DiskManagementListResponseEvent(code, source, boxUuid, boxBind, requestUuid));
                                    });
                                    isStorageDiskManageList = (resultCode >= 0);
                                } else {
                                    isStorageDiskManageList = true;
                                    EulixBoxInfo eulixBoxInfo = EulixSpaceDBUtil.getSpecificBoxInfo(getApplicationContext(), boxUuid, "1");
                                    if (eulixBoxInfo == null) {
                                        eulixBoxInfo = new EulixBoxInfo();
                                    }
                                    eulixBoxInfo.setDiskManageListResult(result);
                                    Map<String, String> boxV = new HashMap<>();
                                    boxV.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                                    boxV.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                                    boxV.put(EulixSpaceDBManager.FIELD_BOX_INFO, new Gson().toJson(eulixBoxInfo, EulixBoxInfo.class));
                                    EulixSpaceDBUtil.updateBox(getApplicationContext(), boxV);
                                }
                                if (isStorageDiskManageList) {
                                    EventBusUtil.post(new DiskManagementListResponseEvent(code, source, boxUuid, boxBind, requestUuid));
                                }
                            } else {
                                EventBusUtil.post(new DiskManagementListResponseEvent(code, source, boxUuid, boxBind, requestUuid));
                            }
                        }

                        @Override
                        public void onFail(int code, String source, String message, String requestId) {
                            EventBusUtil.post(new DiskManagementListResponseEvent(code, source, boxUuid, boxBind, requestUuid));
                        }

                        @Override
                        public void onError(String errMsg) {
                            EventBusUtil.post(new DiskManagementListResponseEvent(ConstantField.SERVER_EXCEPTION_CODE, null, boxUuid, boxBind, requestUuid));
                        }
                    });

        } else {
            EventBusUtil.post(new DiskManagementListResponseEvent(-1, null, boxUuid, boxBind, requestUuid));
        }
    }

    private void getDiskManagementRaidInfo(String boxUuid, String boxBind) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(getApplicationContext(), boxUuid, boxBind);
        if (gatewayCommunicationBase != null) {
            DiskUtil.getDiskManagementRaidInfo(gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                    , gatewayCommunicationBase.getIvParams(), new RaidInfoCallback() {
                        @Override
                        public void onSuccess(int code, String source, String message, String requestId, RaidInfoResult result) {
                            if (result != null) {
                                boolean isStorage = false;
                                boolean isHandle = false;
                                JSONObject jsonObject = null;
                                EulixSpaceDBBoxManager eulixSpaceDBBoxManager = EulixSpaceDBBoxManager.getInstance(boxUuid, boxBind);
                                if (eulixSpaceDBBoxManager != null) {
                                    isHandle = true;
                                    jsonObject = new JSONObject();
                                    try {
                                        jsonObject.put("raidInfoResult", new Gson().toJson(result, RaidInfoResult.class));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        isHandle = false;
                                    }
                                }
                                if (isHandle) {
                                    int resultCode = eulixSpaceDBBoxManager.updateBoxInfo(jsonObject, isUpdate -> {
                                        EventBusUtil.post(new DiskManagementRaidInfoResponseEvent());
                                    });
                                    isStorage = (resultCode >= 0);
                                } else {
                                    isStorage = true;
                                    EulixBoxInfo eulixBoxInfo = EulixSpaceDBUtil.getSpecificBoxInfo(getApplicationContext(), boxUuid, "1");
                                    if (eulixBoxInfo == null) {
                                        eulixBoxInfo = new EulixBoxInfo();
                                    }
                                    eulixBoxInfo.setRaidInfoResult(result);
                                    Map<String, String> boxV = new HashMap<>();
                                    boxV.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                                    boxV.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                                    boxV.put(EulixSpaceDBManager.FIELD_BOX_INFO, new Gson().toJson(eulixBoxInfo, EulixBoxInfo.class));
                                    EulixSpaceDBUtil.updateBox(getApplicationContext(), boxV);
                                }
                                if (isStorage) {
                                    EventBusUtil.post(new DiskManagementRaidInfoResponseEvent());
                                }
                            } else {
                                EventBusUtil.post(new DiskManagementRaidInfoResponseEvent());
                            }
                        }

                        @Override
                        public void onFail(int code, String source, String message, String requestId) {
                            EventBusUtil.post(new DiskManagementRaidInfoResponseEvent());
                        }

                        @Override
                        public void onError(String errMsg) {
                            EventBusUtil.post(new DiskManagementRaidInfoResponseEvent());
                        }
                    });
        } else {
            EventBusUtil.post(new DiskManagementRaidInfoResponseEvent());
        }
    }

    private void eulixSystemShutdown(String boxUuid, String boxBind) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(getApplicationContext(), boxUuid, boxBind);
        if (gatewayCommunicationBase != null) {
            DiskUtil.eulixSystemShutdown(gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                    , gatewayCommunicationBase.getIvParams(), new EulixBaseResponseExtensionCallback() {
                        @Override
                        public void onSuccess(String source, int code, String message, String requestId) {
                            ;
                        }

                        @Override
                        public void onFailed() {
                            ;
                        }

                        @Override
                        public void onError(String errMsg) {
                            ;
                        }
                    });
        }
    }

    private void getDeviceAbility(String boxUuid, String boxBind, boolean isFore) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(getApplicationContext(), boxUuid, boxBind);
        if (gatewayCommunicationBase != null) {
            EulixNetUtil.getDeviceAbility(gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                    , gatewayCommunicationBase.getIvParams(), isFore, new DeviceAbilityCallback() {
                        @Override
                        public void onSuccess(int code, String source, String message, String requestId, DeviceAbility result) {
                            if (result != null) {
                                boolean isStorage = false;
                                boolean isHandle = false;
                                JSONObject jsonObject = null;
                                EulixSpaceDBBoxManager eulixSpaceDBBoxManager = EulixSpaceDBBoxManager.getInstance(boxUuid, boxBind);
                                if (eulixSpaceDBBoxManager != null) {
                                    isHandle = true;
                                    jsonObject = new JSONObject();
                                    try {
                                        jsonObject.put("deviceAbility", new Gson().toJson(result, DeviceAbility.class));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        isHandle = false;
                                    }
                                }
                                if (isHandle) {
                                    int resultCode = eulixSpaceDBBoxManager.updateBoxInfo(jsonObject, isUpdate -> {
                                        EventBusUtil.post(new DeviceAbilityResponseEvent());
                                    });
                                    isStorage = (resultCode >= 0);
                                } else {
                                    EulixBoxInfo eulixBoxInfo = EulixSpaceDBUtil.getSpecificBoxInfo(getApplicationContext(), boxUuid, boxBind);
                                    if (eulixBoxInfo != null) {
                                        isStorage = true;
                                        if (!DeviceAbility.compare(result, eulixBoxInfo.getDeviceAbility())) {
                                            eulixBoxInfo.setDeviceAbility(result);
                                            Map<String, String> boxV = new HashMap<>();
                                            boxV.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                                            boxV.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                                            boxV.put(EulixSpaceDBManager.FIELD_BOX_INFO, new Gson().toJson(eulixBoxInfo, EulixBoxInfo.class));
                                            EulixSpaceDBUtil.updateBox(getApplicationContext(), boxV);
                                        }
                                    }
                                }
                                if (isStorage) {
                                    EventBusUtil.post(new DeviceAbilityResponseEvent());
                                }
                            } else {
                                EventBusUtil.post(new DeviceAbilityResponseEvent());
                            }
                        }

                        @Override
                        public void onFail(int code, String source, String message, String requestId) {
                            EventBusUtil.post(new DeviceAbilityResponseEvent());
                        }

                        @Override
                        public void onError(String errMsg) {
                            EventBusUtil.post(new DeviceAbilityResponseEvent());
                        }
                    });
        }
    }

    private void getPlatformAbility(String platformServerUrl, boolean isFore) {
        if (platformServerUrl != null && !DataUtil.isPlatformAbilityRequest(platformServerUrl, false)) {
            DataUtil.setPlatformAbilityRequest(platformServerUrl, true);
            EulixPlatformUtil.getPlatformAbility(platformServerUrl, isFore, new PlatformAbilityCallback() {
                @Override
                public void onSuccess(List<PlatformApi> platformApis) {
                    DataUtil.setCurrentPlatformServerHost(StringUtil.urlToHost(platformServerUrl));
                    DataUtil.setPlatformAbility(getApplicationContext(), platformServerUrl, platformApis, false);
                    DataUtil.setPlatformAbilityRequest(platformServerUrl, false);
                    Logger.d(TAG, "request platform ability success: " + platformServerUrl);
                }

                @Override
                public void onFailed() {
                    DataUtil.setPlatformAbilityRequest(platformServerUrl, false);
                    Logger.d(TAG, "request platform ability failed: " + platformServerUrl);
                }

                @Override
                public void onError(String errMsg) {
                    DataUtil.setPlatformAbilityRequest(platformServerUrl, false);
                    Logger.d(TAG, "request platform ability error: " + platformServerUrl);
                }
            });
        } else {
            Logger.d(TAG, "request platform ability in progress: " + platformServerUrl);
        }
    }

    private void getAccessInfo(String boxUuid, String boxBind, String requestUuid) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(getApplicationContext(), boxUuid, boxBind);
        String boxDomain = null;
        if (gatewayCommunicationBase != null) {
            boxDomain = gatewayCommunicationBase.getBoxDomain();
        }
        EulixBoxBaseInfo currentBoxInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(getApplicationContext());
        if (currentBoxInfo != null && boxUuid != null && boxUuid.equals(currentBoxInfo.getBoxUuid())
                && boxBind != null && boxBind.equals(currentBoxInfo.getBoxBind())) {
            String ipBaseUrl = Urls.getIPBaseUrl();
            if (!TextUtils.isEmpty(ipBaseUrl)) {
                boxDomain = ipBaseUrl;
            }
        }
        if (gatewayCommunicationBase != null && boxDomain != null) {
            EulixNetUtil.getInternetServiceConfig(DataUtil.getClientUuid(getApplicationContext())
                    , EulixSpaceDBUtil.getClientAoId(getApplicationContext(), boxUuid, boxBind), boxDomain
                    , gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getIvParams(), false, new InternetServiceConfigCallback() {
                @Override
                public void onSuccess(int code, String source, String message, String requestId, InternetServiceConfigResult result) {
                    EulixSpaceDBUtil.setAOSpaceBean(getApplicationContext(), boxUuid, boxBind, result, false);
                    EventBusUtil.post(new AccessInfoResponseEvent(result, requestUuid));
                }

                @Override
                public void onFail(int code, String source, String message, String requestId) {
                    EventBusUtil.post(new AccessInfoResponseEvent(null, requestUuid));
                }

                @Override
                public void onError(String errMsg) {
                    EventBusUtil.post(new AccessInfoResponseEvent(null, requestUuid));
                }
            });
        } else {
            EventBusUtil.post(new AccessInfoResponseEvent(null, requestUuid));
        }
    }

    private void getDIDDocument(String boxUuid, String boxBind, String requestUuid) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(getApplicationContext(), boxUuid, boxBind);
        String boxDomain = null;
        if (gatewayCommunicationBase != null) {
            boxDomain = gatewayCommunicationBase.getBoxDomain();
        }
        EulixBoxBaseInfo currentBoxInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(getApplicationContext());
        if (currentBoxInfo != null && boxUuid != null && boxUuid.equals(currentBoxInfo.getBoxUuid())
                && boxBind != null && boxBind.equals(currentBoxInfo.getBoxBind())) {
            String ipBaseUrl = Urls.getIPBaseUrl();
            if (!TextUtils.isEmpty(ipBaseUrl)) {
                boxDomain = ipBaseUrl;
            }
        }
        if (gatewayCommunicationBase != null && boxDomain != null) {
            String aoId = EulixSpaceDBUtil.getClientAoId(getApplicationContext(), boxUuid, boxBind);
            DIDUtil.getDIDDocument(aoId, null, boxDomain, gatewayCommunicationBase.getAccessToken()
                    , gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getIvParams()
                    , false, new DIDDocumentCallback() {
                        @Override
                        public void onSuccess(int code, String source, String message, String requestId, DIDDocumentResult result) {
                            String didDoc = result.getDidDoc();
                            String didDocDecode = null;
                            if (didDoc != null) {
                                didDocDecode = StringUtil.base64Decode(didDoc, StandardCharsets.UTF_8);
                                Logger.d(TAG, "get did doc decode: " + didDocDecode);
                            }
                            DIDProviderBean didProviderBean = new DIDProviderBean(boxUuid, boxBind);
                            didProviderBean.setAoId(aoId);
                            didProviderBean.setDidDoc(didDoc);
                            didProviderBean.setDidDocDecode(didDocDecode);
                            didProviderBean.setTimestamp(System.currentTimeMillis());
                            AOSpaceUtil.insertOrUpdateDIDWithPasswordEncryptPrivateKey(getApplicationContext()
                                    , didProviderBean, result.getEncryptedPriKeyBytes());
                            EventBusUtil.post(new DIDDocumentResponseEvent(result, requestUuid));
                        }

                        @Override
                        public void onFail(int code, String source, String message, String requestId) {
                            EventBusUtil.post(new DIDDocumentResponseEvent(null, requestUuid));
                        }

                        @Override
                        public void onError(String errMsg) {
                            EventBusUtil.post(new DIDDocumentResponseEvent(null, requestUuid));
                        }
                    });
        } else {
            EventBusUtil.post(new AccessInfoResponseEvent(null, requestUuid));
        }
    }


    /**
     * 该方法仅适合下线自己
     *
     * @param boxUuid
     * @param boxBind
     */
    private void offlineSelfGrantee(String boxUuid, String boxBind) {
        if (boxUuid != null && boxBind != null && !"1".equals(boxBind) && !"-1".equals(boxBind)) {
            GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(getApplicationContext(), boxUuid, boxBind);
            if (gatewayCommunicationBase != null) {
                String baseUrl = Urls.getBaseUrl();
                UserInfoUtil.offlineTerminal(baseUrl, boxBind, DataUtil.getClientUuid(getApplicationContext())
                        , gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                        , gatewayCommunicationBase.getIvParams(), new TerminalOfflineCallback() {
                            @Override
                            public void onSuccess(int code, String message, String requestId, TerminalOfflineResult results, String customizeSource) {
                                handleSelfGranteeOffline(code, customizeSource, boxUuid, boxBind);
                            }

                            @Override
                            public void onFail(int code, String message, String requestId) {
                                handleSelfGranteeOffline(code, "", boxUuid, boxBind);
                            }

                            @Override
                            public void onError(String errMsg) {
                                if (mHandler != null) {
                                    mHandler.postDelayed(() -> offlineSelfGrantee(boxUuid, boxBind), 10000);
                                }
                            }
                        });
            } else {
                deleteBox(boxUuid, boxBind);
            }
        }
    }

    private void handleSelfGranteeOffline(int code, String customizeSource, String boxUuid, String boxBind) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if ((code >= 200 && code < 400) || (code == ConstantField.KnownError.TerminalError.TERMINAL_OFFLINE_DUPLICATE_CODE && customizeSource != null && customizeSource.trim().toUpperCase().startsWith(ConstantField.KnownSource.ACCOUNT))) {
                    deleteBox(boxUuid, boxBind);
                }
            });
        }
    }

    /**
     * 判定盒子是绑定/邀请，还是扫码
     *
     * @return true表示绑定或邀请，false表示扫码
     */
    public boolean isActiveDeviceBound() {
        boolean isBound = false;
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(getApplicationContext()
                , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (boxValues == null || boxValues.size() <= 0) {
            boxValues = EulixSpaceDBUtil.queryBox(getApplicationContext(), EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
        }
        if (boxValues != null) {
            String bindValue = null;
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null) {
                    bindValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    break;
                }
            }
            isBound = ("1".equals(bindValue) || "-1".equals(bindValue));
        }
        return isBound;
    }


    private void handleEulixPush(boolean heart) {
        EulixBoxBaseInfo eulixBoxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(getApplicationContext(), false);
        boolean isHeart = heart;
        if (eulixBoxBaseInfo == null) {
            isHeart = false;
            eulixBoxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(getApplicationContext());
        }
        if (eulixPushManager == null) {
            eulixPushManager = EulixPushManager.getInstance();
        }
        if (eulixBoxBaseInfo != null) {
            EulixSpaceInfo eulixSpaceInfo = new EulixSpaceInfo();
            eulixSpaceInfo.setBoxUuid(eulixBoxBaseInfo.getBoxUuid());
            eulixSpaceInfo.setBoxBind(eulixBoxBaseInfo.getBoxBind());
            eulixPushManager.resetAliveConnect(eulixSpaceInfo, isHeart);
        } else {
            EulixSpaceInfo eulixSpaceInfo = DataUtil.getLastEulixSpace(getApplicationContext());
            eulixPushManager.resetAliveConnect(eulixSpaceInfo, false);
        }
    }

    /**
     * 解析二维码
     *
     * @param result   二维码数据
     * @param function 需求功能
     * @return
     */
    private boolean resolveQRCode(String result, int function) {
        boolean valid = false;
        switch (function) {
            case ConstantField.ZxingCommunication.LOGIN_EXTRA_VALUE:
                // UUID
                if (result != null) {
                    if (result.contains("p=")) {
                        valid = true;
                    } else {
                        UUID uuid = null;
                        try {
                            uuid = UUID.fromString(result);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        valid = (uuid != null);
                    }
                }
                break;
            case ConstantField.ZxingCommunication.BLUETOOTH_EXTRA_VALUE:
                // https://ao.space/?btid=* 或者 https://ao.space/?sn=*
                if (result != null && (Patterns.WEB_URL.matcher(result).matches() || URLUtil.isValidUrl(result))) {
                    Uri uri = Uri.parse(result);
                    if (uri != null) {
                        String host = uri.getHost();
                        String sn = null;
                        String btid = null;
                        Set<String> querySet = uri.getQueryParameterNames();
                        if (querySet != null) {
                            for (String query : querySet) {
                                if (ConstantField.SN.equals(query)) {
                                    sn = uri.getQueryParameter(ConstantField.SN);
                                }
                                if (ConstantField.BTID.equals(query)) {
                                    btid = uri.getQueryParameter(ConstantField.BTID);
                                }
                            }
                        }
                        valid = (host != null && host.contains("ao.space") && (StringUtil.isNonBlankString(sn) || btid != null));
                    }
                }
                break;
            case ConstantField.ZxingCommunication.PC_HOST_EXTRA_VALUE:
                if (result != null && (Patterns.WEB_URL.matcher(result).matches() || URLUtil.isValidUrl(result))) {
                    Uri uri = Uri.parse(result);
                    if (uri != null) {
                        String host = uri.getHost();
                        String sn = null;
                        String btid = null;
                        String ipAddress = null;
                        String portValue = null;
                        int port = -1;
                        Set<String> querySet = uri.getQueryParameterNames();
                        if (querySet != null) {
                            for (String query : querySet) {
                                if (ConstantField.SN.equals(query)) {
                                    sn = uri.getQueryParameter(ConstantField.SN);
                                }
                                if (ConstantField.BTID.equals(query)) {
                                    btid = uri.getQueryParameter(ConstantField.BTID);
                                }
                                if (ConstantField.IPADDR.equals(query)) {
                                    ipAddress = uri.getQueryParameter(ConstantField.IPADDR);
                                    String decodeIpAddress = null;
                                    if (ipAddress != null) {
                                        try {
                                            decodeIpAddress = URLDecoder.decode(ipAddress, "UTF-8");
                                        } catch (UnsupportedEncodingException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    if (decodeIpAddress != null) {
                                        ipAddress = decodeIpAddress;
                                    }
                                }
                                if (ConstantField.PORT.equals(query)) {
                                    portValue = uri.getQueryParameter(ConstantField.PORT);
                                }
                            }
                        }
                        if (portValue != null) {
                            try {
                                port = Integer.parseInt(portValue);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                        valid = (host != null && host.contains("ao.space") && (StringUtil.isNonBlankString(sn) || btid != null)
                                && ipAddress != null && port >= 0);
                    }
                }
                break;
            default:
                valid = true;
                break;
        }
        return valid;
    }

    private void sendZxingBroadcast(int function, boolean result, String requestId) {
        boolean isSend = false;
        int keyType = 0;
        switch (function) {
            case ConstantField.ZxingCommunication.LOGIN_EXTRA_VALUE:
                keyType = ConstantField.ZxingCommunication.REPLY_ACTIVE_BOX_BIND;
                isSend = true;
                break;
            case ConstantField.ZxingCommunication.BLUETOOTH_EXTRA_VALUE:
                keyType = ConstantField.ZxingCommunication.REPLY_BLUETOOTH_VALID;
                isSend = true;
                break;
            case ConstantField.ZxingCommunication.PC_HOST_EXTRA_VALUE:
                keyType = ConstantField.ZxingCommunication.REPLY_PC_HOST_VALID;
                isSend = true;
                break;
            default:
                break;
        }
        if (isSend) {
            Intent intent = new Intent();
            intent.setAction(ConstantField.ZxingCommunication.ZXING_CAPTURE_RECV_ACTION);
            intent.putExtra(ConstantField.ZxingCommunication.KEY_TYPE, keyType);
            intent.putExtra(ConstantField.ZxingCommunication.RESULT, result);
            intent.putExtra(ConstantField.ZxingCommunication.REQUEST_ID, (requestId == null ? UUID.randomUUID().toString() : requestId));
            sendBroadcast(intent);
        }
    }


    public class EulixSpaceBinder extends Binder {

        public void registerCallback(EulixSpaceCallback callback) {
            mCallback = callback;
        }

        public void unregisterCallback() {
            mCallback = null;
        }

        public boolean sendMessage(String message) {
            return sendWebSocket(message);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.d(TAG, "on create");
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        isForeground = false;
        isRegisterWeChat = false;
        mHandler = new EulixSpaceHandler(this);
        toastManager = new ToastManager(this);
        boxNetworkCheckManager = BoxNetworkCheckManager.getInstance(this);
        boxNetworkCheckManager.registerCallback(this);
        mReceiver = new EulixSpaceReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AlarmUtil.ALARM_ACTION);
        intentFilter.addAction(ConstantField.ZxingCommunication.ZXING_CAPTURE_SEND_ACTION);
        try {
            getApplicationContext().registerReceiver(mReceiver, intentFilter);
        } catch (Exception e) {
            e.printStackTrace();
        }
        mHandler.sendEmptyMessage(GET_UUID);
        mHandler.sendEmptyMessage(BIND_WEB_RTC_SERVICE);
        JobScheduler jobScheduler = (JobScheduler) getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            JobInfo.Builder builder = new JobInfo.Builder(ConstantField.RequestCode.EULIX_SPACE_JOB_ID, new ComponentName(this, EulixSpaceJobService.class));
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                builder.setPeriodic(JobInfo.getMinPeriodMillis(), JobInfo.getMinFlexMillis());
            }
            JobInfo jobInfo = builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).build();
            if (jobInfo != null) {
                jobScheduler.schedule(jobInfo);
            }
        }
        EventBusUtil.register(this);
        String platformServerUrl = DebugUtil.getEnvironmentServices();
        if (platformServerUrl != null) {
            Logger.d(TAG, "init request platform ability: " + platformServerUrl);
            getPlatformAbility(platformServerUrl, false);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.d(TAG, "on start command, flags: " + flags + ", start id: " + startId);
        if (intent != null) {
            boolean isFore = intent.getBooleanExtra(ConstantField.EXTRA.FOREGROUND, false);
            handleNotification(isFore);
            isForeground = isFore;
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case ConstantField.Action.JOB_ACTION:
                        Logger.d(TAG, "job action start service");
                        break;
                    case ConstantField.Action.LAUNCH_ACTION:
//                        checkBoxOnline();
                        Logger.d(TAG, "launch action start service");
                        resetSpacePoll(true);
                        break;
                    case ConstantField.Action.TOKEN_ACTION:
                        String nBoxUuid = null;
                        String nBoxBind = null;
                        if (intent.hasExtra(ConstantField.BOX_UUID)) {
                            nBoxUuid = intent.getStringExtra(ConstantField.BOX_UUID);
                        }
                        if (intent.hasExtra(ConstantField.BOX_BIND)) {
                            nBoxBind = intent.getStringExtra(ConstantField.BOX_BIND);
                        }
                        boolean isForce = intent.getBooleanExtra(ConstantField.FORCE, false);
                        boolean isFromCall = intent.getBooleanExtra(ConstantField.FROM_CALL, false);
                        if (mHandler == null) {
                            createAuthToken(nBoxUuid, nBoxBind, isForce, isFromCall);
                        } else {
                            while (mHandler.hasMessages(CREATE_AUTH_TOKEN)) {
                                mHandler.removeMessages(CREATE_AUTH_TOKEN);
                            }
                            Message message = mHandler.obtainMessage(CREATE_AUTH_TOKEN, (isForce ? 1 : 0), (isFromCall ? 1 : 0));
                            Bundle data = new Bundle();
                            if (nBoxUuid != null) {
                                data.putString(ConstantField.BOX_UUID, nBoxUuid);
                            }
                            if (nBoxBind != null) {
                                data.putString(ConstantField.BOX_BIND, nBoxBind);
                            }
                            message.setData(data);
                            mHandler.sendMessage(message);
                        }
                        break;
                    case ConstantField.Action.STORAGE_ACTION:
                        dismissNotification(STORAGE);
                        break;
                    default:
                        break;
                }
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Logger.d(TAG, "on destroy");
        EventBusUtil.unRegister(this);
        closeWebSocket();
        try {
            getApplicationContext().unregisterReceiver(mReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (boxNetworkCheckManager != null) {
            boxNetworkCheckManager.unregisterCallback();
            boxNetworkCheckManager = null;
        }
        toastManager = null;
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        if (isForeground) {
            stopForeground(true);
            isForeground = false;
        }
        super.onDestroy();
    }

    @Override
    public void createAuthTokenCallback(String boxUuid, String boxBind) {
        if (mHandler == null) {
            createAuthToken(boxUuid, boxBind, false, false);
        } else {
            mHandler.post(() -> createAuthToken(boxUuid, boxBind, false, false));
        }
    }

    @Override
    public void checkSpecificBoxCallback(String boxUuid, String boxBind, String requestId, Boolean isOnline) {
        if (mHandler == null) {
            EventBusUtil.post(new SpecificBoxOnlineResponseEvent(boxUuid, boxBind, requestId, isOnline));
        } else {
            mHandler.post(() -> EventBusUtil.post(new SpecificBoxOnlineResponseEvent(boxUuid, boxBind, requestId, isOnline)));
        }
    }

    private class EulixSpaceReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                String action = intent.getAction();
                if (action != null) {
                    switch (action) {
                        case AlarmUtil.ALARM_ACTION:
                            String name = (intent.hasExtra("name") ? intent.getStringExtra("name") : null);
                            String role = (intent.hasExtra("role") ? intent.getStringExtra("role") : null);
                            Logger.d(TAG, "receive alarm: " + name);
                            String boxUuid = null;
                            String boxBind = null;
                            String boxTokenValue = null;
                            String updateTimestampValue = null;
                            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context
                                    , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
                            if (boxValues != null) {
                                for (Map<String, String> boxValue : boxValues) {
                                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                                            && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                                        boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                                        boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                                        if (boxValue.containsKey(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME)) {
                                            updateTimestampValue = boxValue.get(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME);
                                        }
                                        if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
                                            boxTokenValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                                        }
                                        break;
                                    }
                                }
                            }
                            if (name != null && name.equals(boxUuid) && role != null && role.equals(boxBind)) {
                                if ("1".equals(boxBind) || "-1".equals(boxBind)) {
                                    prepareAuthToken(boxUuid, boxBind, false, false);
                                } else if (updateTimestampValue != null && boxTokenValue != null) {
                                    long loginValidValue = -1L;
                                    String loginValid = null;
                                    EulixBoxToken eulixBoxToken = null;
                                    try {
                                        eulixBoxToken = new Gson().fromJson(boxTokenValue, EulixBoxToken.class);
                                    } catch (JsonSyntaxException e) {
                                        e.printStackTrace();
                                    }
                                    if (eulixBoxToken != null) {
                                        loginValid = eulixBoxToken.getLoginValid();
                                    }
                                    if (loginValid != null) {
                                        try {
                                            loginValidValue = Long.parseLong(loginValid);
                                        } catch (NumberFormatException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    if (loginValidValue >= 0) {
                                        if (loginValidValue > System.currentTimeMillis()) {
                                            prepareAuthToken(boxUuid, boxBind, false, false);
                                        } else if (mHandler != null) {
                                            String finalBoxUuid = boxUuid;
                                            String finalBoxBind = boxBind;
                                            mHandler.post(() -> EulixSpaceDBUtil.handleTemperateBox(EulixSpaceApplication.getContext()
                                                    , finalBoxUuid, finalBoxBind));
                                        }
                                    }
                                }
                            }
                            break;
                        case ConstantField.ZxingCommunication.ZXING_CAPTURE_SEND_ACTION:
                            if (intent.hasExtra(ConstantField.ZxingCommunication.KEY_TYPE) && intent.hasExtra(ConstantField.ZxingCommunication.REQUEST_ID)) {
                                int keyType = intent.getIntExtra(ConstantField.ZxingCommunication.KEY_TYPE, 0);
                                String requestId = intent.getStringExtra(ConstantField.ZxingCommunication.REQUEST_ID);
                                String content = null;
                                if (intent.hasExtra(ConstantField.ZxingCommunication.CONTENT)) {
                                    content = intent.getStringExtra(ConstantField.ZxingCommunication.CONTENT);
                                }
                                Logger.d(TAG, "receive zxing: " + keyType + ", content: " + content);
                                boolean result = false;
                                switch (keyType) {
                                    case ConstantField.ZxingCommunication.QUERY_ACTIVE_BOX_BIND:
                                        result = resolveQRCode(content, ConstantField.ZxingCommunication.LOGIN_EXTRA_VALUE);
                                        boolean bound = isActiveDeviceBound();
                                        if (mHandler == null) {
                                            sendZxingBroadcast(ConstantField.ZxingCommunication.LOGIN_EXTRA_VALUE, (result && bound), requestId);
                                        } else {
                                            boolean finalResult = result;
                                            mHandler.post(() -> {
                                                if (!(finalResult && bound) && toastManager != null) {
                                                    toastManager.showImageTextToast(R.drawable.toast_refuse
                                                            , EulixSpaceApplication.getResumeActivityContext()
                                                                    .getString((!finalResult) ? R.string.qr_code_unrecognized
                                                                            : R.string.operate_on_bind_device));
                                                }
                                                sendZxingBroadcast(ConstantField.ZxingCommunication.LOGIN_EXTRA_VALUE, (finalResult && bound), requestId);
                                            });
                                        }
                                        break;
                                    case ConstantField.ZxingCommunication.QUERY_BLUETOOTH_VALID:
                                        result = resolveQRCode(content, ConstantField.ZxingCommunication.BLUETOOTH_EXTRA_VALUE);
                                        if (mHandler == null) {
                                            sendZxingBroadcast(ConstantField.ZxingCommunication.BLUETOOTH_EXTRA_VALUE, result, requestId);
                                        } else {
                                            boolean finalResult = result;
                                            mHandler.post(() -> {
                                                if (!finalResult && toastManager != null) {
                                                    toastManager.showImageTextToast(R.drawable.toast_refuse
                                                            , EulixSpaceApplication.getResumeActivityContext().getString(R.string.qr_code_unrecognized));
                                                }
                                                sendZxingBroadcast(ConstantField.ZxingCommunication.BLUETOOTH_EXTRA_VALUE, finalResult, requestId);
                                            });
                                        }
                                        break;
                                    case ConstantField.ZxingCommunication.QUERY_PC_HOST_VALID:
                                        result = resolveQRCode(content, ConstantField.ZxingCommunication.PC_HOST_EXTRA_VALUE);
                                        if (mHandler == null) {
                                            sendZxingBroadcast(ConstantField.ZxingCommunication.PC_HOST_EXTRA_VALUE, result, requestId);
                                        } else {
                                            boolean finalResult = result;
                                            mHandler.post(() -> {
                                                if (!finalResult && toastManager != null) {
                                                    toastManager.showImageTextToast(R.drawable.toast_refuse
                                                            , EulixSpaceApplication.getResumeActivityContext().getString(R.string.qr_code_unrecognized));
                                                }
                                                sendZxingBroadcast(ConstantField.ZxingCommunication.PC_HOST_EXTRA_VALUE, finalResult, requestId);
                                            });
                                        }
                                        break;
                                    default:
                                        result = (content != null && !TextUtils.isEmpty(content));
                                        if (mHandler == null) {
                                            sendZxingBroadcast(ConstantField.ZxingCommunication.BLUETOOTH_EXTRA_VALUE, result, requestId);
                                        } else {
                                            boolean finalResult = result;
                                            mHandler.post(() -> {
                                                if (!finalResult && toastManager != null) {
                                                    toastManager.showImageTextToast(R.drawable.toast_refuse
                                                            , EulixSpaceApplication.getResumeActivityContext().getString(R.string.qr_code_unrecognized));
                                                }
                                                sendZxingBroadcast(ConstantField.ZxingCommunication.BLUETOOTH_EXTRA_VALUE, finalResult, requestId);
                                            });
                                        }
                                        break;
                                }
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(StorageInfoRequestEvent event) {
        if (event != null) {
            String boxUuid = event.getBoxUuid();
            String boxBind = event.getBoxBind();
            String boxDomain = event.getBoxDomain();
            if (boxUuid != null && boxBind != null && boxDomain != null) {
                getDeviceStorageInfo(boxUuid, boxBind, boxDomain);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MemberListEvent event) {
        if (event != null) {
            String boxUuid = event.getBoxUuid();
            String boxBind = event.getBoxBind();
            String boxDomain = event.getBoxDomain();
            String refreshId = event.getRefreshId();
            if (boxUuid != null && boxBind != null && boxDomain != null) {
                getMemberList(boxUuid, boxBind, boxDomain, refreshId, event.isUpdateAvatar(), (refreshId != null));
            } else {
                EventBusUtil.post(new MemberResultEvent(refreshId));
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(TerminalListEvent event) {
        if (event != null) {
            String boxUuid = event.getBoxUuid();
            String boxBind = event.getBoxBind();
            if (boxUuid != null && boxBind != null) {
                getTerminalList(boxUuid, boxBind, event.isFore());
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DiskManagementListRequestEvent event) {
        if (event != null) {
            String boxUuid = event.getBoxUuid();
            String boxBind = event.getBoxBind();
            String requestUuid = event.getRequestUuid();
            if (boxUuid != null && boxBind != null) {
                getDiskManagementList(boxUuid, boxBind, requestUuid, event.isFore());
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(EulixSystemShutdownRequestEvent event) {
        if (event != null) {
            String boxUuid = event.getBoxUuid();
            String boxBind = event.getBoxBind();
            if (boxUuid != null && boxBind != null) {
                eulixSystemShutdown(boxUuid, boxBind);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DiskManagementRaidInfoRequestEvent event) {
        if (event != null) {
            String boxUuid = event.getBoxUuid();
            String boxBind = event.getBoxBind();
            if (boxUuid != null && boxBind != null) {
                getDiskManagementRaidInfo(boxUuid, boxBind);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DeviceAbilityRequestEvent event) {
        if (event != null) {
            String boxUuid = event.getBoxUuid();
            String boxBind = event.getBoxBind();
            if (boxUuid != null && boxBind != null) {
                getDeviceAbility(boxUuid, boxBind, event.isFore());
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(PlatformAbilityRequestEvent event) {
        if (event != null) {
            String platformServerUrl = event.getPlatformServerUrl();
            Boolean isSupport = DataUtil.isPlatformAbilitySupport(getApplicationContext(), platformServerUrl
                    , ConstantField.URL.SERVERS_STUN_DETAIL_V2_API, ConstantField.HttpRequestMethod.GET);

            if (platformServerUrl != null) {
                getPlatformAbility(platformServerUrl, event.isFore());
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(AccessInfoRequestEvent event) {
        if (event != null) {
            String boxUuid = event.getBoxUuid();
            String boxBind = event.getBoxBind();
            if (boxUuid != null && boxBind != null) {
                getAccessInfo(boxUuid, boxBind, event.getRequestUuid());
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DIDDocumentRequestEvent event) {
        if (event != null) {
            String boxUuid = event.getBoxUuid();
            String boxBind = event.getBoxBind();
            if (boxUuid != null && boxBind != null) {
                getDIDDocument(boxUuid, boxBind, event.getRequestUuid());
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SpaceChangeEvent event) {
        if (!isForeground && event != null) {
            resetSpacePoll(event.isHeart());
        }
        String platformServerUrl = DebugUtil.getEnvironmentServices();
        if (platformServerUrl != null && !StringUtil.compare(StringUtil.urlToHost(platformServerUrl), DataUtil.getCurrentPlatformServerHost())) {
            Logger.d(TAG, "space change request platform ability: " + platformServerUrl);
            getPlatformAbility(platformServerUrl, false);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DeviceHardwareInfoRequestEvent event) {
        if (event != null) {
            String boxUuid = event.getBoxUuid();
            String boxBind = event.getBoxBind();
            if (boxUuid != null && boxBind != null) {
                getDeviceHardwareInfo(boxUuid, boxBind, event.getRequestUuid(), event.isFore());
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SecurityMessagePollRequestEvent event) {
        if (event != null) {
            String boxUuid = event.getBoxUuid();
            String boxBind = event.getBoxBind();
            String requestUuid = event.getRequestUuid();
            if (boxUuid != null && boxBind != null && requestUuid != null) {
                pollSecurityMessage(boxUuid, boxBind, requestUuid);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(AppUpdateEvent event) {
        if (event != null) {
            String appName = (getString(R.string.app_name) + "v" + event.getNewestVersion() + ".apk");
            downloadApk(appName, event.getDownloadUrl(), event.getMd5(), event.isForce());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(BoxOnlineRequestEvent event) {
        if (event != null && boxNetworkCheckManager != null) {
            if (event.isCheckActiveBox()) {
                boxNetworkCheckManager.checkActiveBoxOnline(event.isCheckImmediate());
            } else {
                boxNetworkCheckManager.checkBoxOnline();
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SpecificBoxOnlineRequestEvent event) {
        if (event != null && boxNetworkCheckManager != null) {
            boxNetworkCheckManager.checkSpecificBoxOnline(event.getBoxUuid(), event.getBoxBind(), event.getRequestId());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(BoxNetworkRequestEvent event) {
        if (event != null && boxNetworkCheckManager != null) {
            String boxUuid = event.getBoxUuid();
            String boxBind = event.getBoxBind();
            if (boxUuid == null || boxBind == null) {
                boxNetworkCheckManager.requestDeviceIpAddress();
            } else {
                boxNetworkCheckManager.requestDeviceIpAddress(boxUuid, boxBind);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(OfflineGranteeEvent event) {
        if (event != null) {
            String boxUuid = event.getBoxUuid();
            String boxBind = event.getBoxBind();
            if (boxUuid != null && boxBind != null) {
                offlineSelfGrantee(boxUuid, boxBind);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(GranteeTokenInvalidEvent event) {
        if (event != null) {
            String boxUuid = event.getBoxUuid();
            String boxBind = event.getBoxBind();
            if (boxUuid != null && boxBind != null && !"1".equals(boxBind) && !"-1".equals(boxBind)) {
                logoutBox(boxUuid, boxBind);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SpaceStatusTestRequestEvent event) {
        if (event != null) {
            long startTimestamp = System.currentTimeMillis();
            long startTime = System.nanoTime();
            String boxDomain = null;
            switch (event.getSpaceStatusSource()) {
                case 1:
                    EulixBoxBaseInfo eulixBoxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(getApplicationContext(), true);
                    if (eulixBoxBaseInfo != null) {
                        boxDomain = eulixBoxBaseInfo.getBoxDomain();
                    }
                    EventBusUtil.post(new SpaceStatusTestResponseEvent(1, false, startTimestamp, System.currentTimeMillis(), 0));
                    break;
                case 2:
                    String ipAddress = LanManager.getInstance().getIpAddress();
                    if (ipAddress != null) {
                        try {
                            ThreadPool.getInstance().execute(() -> new GatewayManager(ipAddress).getSpaceStatus(new ISpaceStatusCallback() {
                                @Override
                                public void onResult(SpaceStatusResult result) {
                                    EventBusUtil.post(new SpaceStatusTestResponseEvent(2, true, startTimestamp, System.currentTimeMillis(), (System.nanoTime() - startTime)));
                                }

                                @Override
                                public void onError(String errMsg) {
                                    EventBusUtil.post(new SpaceStatusTestResponseEvent(2, false, startTimestamp, System.currentTimeMillis(), (System.nanoTime() - startTime)));
                                }
                            }));
                        } catch (RejectedExecutionException e) {
                            e.printStackTrace();
                        }
                    } else {
                        EventBusUtil.post(new SpaceStatusTestResponseEvent(2, false, startTimestamp, System.currentTimeMillis(), 0));
                    }
                    break;
                case 3:
                    GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(getApplicationContext());
                    if (gatewayCommunicationBase != null) {
                        boxDomain = gatewayCommunicationBase.getBoxDomain();
                    } else {
                        boxDomain = null;
                    }
                    if (boxDomain != null) {
                        try {
                            String finalBoxDomain = boxDomain;
                            ThreadPool.getInstance().execute(() -> new GatewayManager(finalBoxDomain).getSpaceStatus(new ISpaceStatusCallback() {
                                @Override
                                public void onResult(SpaceStatusResult result) {
                                    EventBusUtil.post(new SpaceStatusTestResponseEvent(3, true, startTimestamp, System.currentTimeMillis(), (System.nanoTime() - startTime)));
                                }

                                @Override
                                public void onError(String errMsg) {
                                    EventBusUtil.post(new SpaceStatusTestResponseEvent(3, false, startTimestamp, System.currentTimeMillis(), (System.nanoTime() - startTime)));
                                }
                            }));
                        } catch (RejectedExecutionException e) {
                            e.printStackTrace();
                        }
                    } else {
                        EventBusUtil.post(new SpaceStatusTestResponseEvent(3, false, startTimestamp, System.currentTimeMillis(), 0));
                    }
                    break;
                default:
                    break;
            }
        }
    }
}
