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

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.R;
import xyz.eulix.space.bean.EulixBoxInfo;
import xyz.eulix.space.bean.EulixBoxOtherInfo;
import xyz.eulix.space.bean.EulixBoxToken;
import xyz.eulix.space.bean.EulixSpaceInfo;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.bean.SwitchPlatformTaskBean;
import xyz.eulix.space.bean.bind.InitResponseNetwork;
import xyz.eulix.space.bean.developer.SpacePlatformInfo;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.event.BoxAllCheckedEvent;
import xyz.eulix.space.event.BoxInsertDeleteEvent;
import xyz.eulix.space.event.BoxOnlineEvent;
import xyz.eulix.space.event.DeviceNetworkResponseEvent;
import xyz.eulix.space.event.PlatformAbilityRequestEvent;
import xyz.eulix.space.event.SpaceChangeEvent;
import xyz.eulix.space.event.SpaceOnlineCallbackEvent;
import xyz.eulix.space.event.SpaceValidEvent;
import xyz.eulix.space.network.agent.DeviceUtil;
import xyz.eulix.space.network.agent.LocalIpInfoCallback;
import xyz.eulix.space.network.gateway.GatewayUtil;
import xyz.eulix.space.network.gateway.PlatformInfo;
import xyz.eulix.space.network.gateway.SpaceStatusExtensionCallback;
import xyz.eulix.space.network.gateway.SpaceStatusResult;
import xyz.eulix.space.util.AOSpaceUtil;
import xyz.eulix.space.util.AlarmUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.GatewayUtils;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.ThreadPool;
import xyz.eulix.space.util.ToastManager;
import xyz.eulix.space.util.Urls;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/12/28 17:13
 */
public class BoxNetworkCheckManager {
    private static final String TAG = BoxNetworkCheckManager.class.getSimpleName();
    private static BoxNetworkCheckManager instance;
    private static final long CHECK_LONG_PERIOD = 35000L;
    private static final long CHECK_SHORT_PERIOD = 20000L;
    private static final int CHECK_USE_BOX = 1;
    private static final int CHECK_ALL_BOX = CHECK_USE_BOX + 1;
    private static boolean isShowOffline = true;
    private ToastManager toastManager;
    private BoxNetworkCheckHandler mHandler;
    private BoxNetworkCheckCallback mCallback;
    private int allCheckingNumber;
    private int allCheckedNumber;
    private boolean allCheckRedirect = false;

    private BoxNetworkCheckManager(Context context) {
        mHandler = new BoxNetworkCheckHandler(this);
        toastManager = new ToastManager(context);
    }

    public synchronized static BoxNetworkCheckManager getInstance(Context context) {
        if (instance == null) {
            instance = new BoxNetworkCheckManager(context);
        }
        return instance;
    }

    public interface BoxNetworkCheckCallback {
        void createAuthTokenCallback(String boxUuid, String boxBind);
        void checkSpecificBoxCallback(String boxUuid, String boxBind, String requestId, Boolean isOnline);
    }

    static class BoxNetworkCheckHandler extends Handler {
        private WeakReference<BoxNetworkCheckManager> boxNetworkCheckManagerWeakReference;

        public BoxNetworkCheckHandler(BoxNetworkCheckManager manager) {
            boxNetworkCheckManagerWeakReference = new WeakReference<>(manager);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            BoxNetworkCheckManager manager = boxNetworkCheckManagerWeakReference.get();
            if (manager == null) {
                super.handleMessage(msg);
            } else {
                switch (msg.what) {
                    case CHECK_USE_BOX:
                        if (msg.obj instanceof BoxNetworkCheckBean) {
                            BoxNetworkCheckBean boxNetworkCheckBean = (BoxNetworkCheckBean) msg.obj;
                            int boxSpaceStatus = EulixSpaceDBUtil.getDeviceStatus(EulixSpaceApplication.getContext()
                                    , boxNetworkCheckBean.boxUuid, boxNetworkCheckBean.boxBind);
                            if (boxSpaceStatus >= ConstantField.EulixDeviceStatus.OFFLINE && boxSpaceStatus <= ConstantField.EulixDeviceStatus.ONLINE_UNINITIALIZED) {
                                boxNetworkCheckBean.setBoxStatus(boxSpaceStatus);
                                manager.checkBoxOnline(boxNetworkCheckBean);
                                Message message = obtainMessage(CHECK_USE_BOX, boxNetworkCheckBean);
                                sendMessageDelayed(message, (DataUtil.isSpaceStatusOnline(boxSpaceStatus, true)
                                        ? CHECK_LONG_PERIOD : CHECK_SHORT_PERIOD));
                            }
                        }
                        break;
                    case CHECK_ALL_BOX:
                        manager.checkBoxOnline();
                        break;
                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        }
    }

    public void registerCallback(BoxNetworkCheckCallback callback) {
        mCallback = callback;
    }

    public void unregisterCallback() {
        mCallback = null;
    }

    public void checkSpecificBoxOnline(String boxUuid, String boxBind, String requestId) {
        boolean isFind = false;
        if (boxUuid != null && boxBind != null) {
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.querySpecificBox(EulixSpaceApplication.getContext(), boxUuid, boxBind);
            if (boxValues != null && !boxValues.isEmpty()) {
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
                                String boxDomain = boxValue.get(EulixSpaceDBManager.FIELD_BOX_DOMAIN);
                                if (boxDomain != null) {
                                    isFind = true;
                                    BoxNetworkCheckBean boxNetworkCheckBean = new BoxNetworkCheckBean();
                                    boxNetworkCheckBean.setBoxUuid(boxUuid);
                                    boxNetworkCheckBean.setBoxBind(boxBind);
                                    boxNetworkCheckBean.setRequestId(requestId);
                                    boxNetworkCheckBean.setBoxDomain(boxDomain);
                                    boxNetworkCheckBean.setBoxStatus(status);
                                    boxNetworkCheckBean.setCheckType(BoxNetworkCheckBean.CHECK_TYPE_SPECIFIC);
                                    checkBoxOnline(boxNetworkCheckBean);
                                }
                            }
                        }
                        break;
                    }
                }


            }
        }
        if (!isFind && mCallback != null) {
            mCallback.checkSpecificBoxCallback(boxUuid, boxBind, requestId, false);
        }
    }

    /**
     * 查所有盒子
     */
    public void checkBoxOnline() {
        if (mHandler != null) {
            while (mHandler.hasMessages(CHECK_ALL_BOX)) {
                mHandler.removeMessages(CHECK_ALL_BOX);
            }
            while (mHandler.hasMessages(CHECK_USE_BOX)) {
                mHandler.removeMessages(CHECK_USE_BOX);
            }
        }
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(EulixSpaceApplication.getContext());
        List<BoxNetworkCheckBean> boxNetworkCheckBeans = new ArrayList<>();
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
                        if (boxUUID != null && boxDomain != null) {
                            BoxNetworkCheckBean boxNetworkCheckBean = new BoxNetworkCheckBean();
                            boxNetworkCheckBean.setBoxUuid(boxUUID);
                            boxNetworkCheckBean.setBoxBind(boxBind);
                            boxNetworkCheckBean.setBoxDomain(boxDomain);
                            boxNetworkCheckBean.setBoxStatus(status);
                            boxNetworkCheckBean.setCheckType(BoxNetworkCheckBean.CHECK_TYPE_ALL);
                            boxNetworkCheckBeans.add(boxNetworkCheckBean);
                        }
                    }
                }
            }
        }
        allCheckedNumber = 0;
        allCheckingNumber = 0;
        allCheckRedirect = false;
        for (BoxNetworkCheckBean boxNetworkCheckBean : boxNetworkCheckBeans) {
            if (boxNetworkCheckBean != null) {
                allCheckingNumber += 1;
                checkBoxOnline(boxNetworkCheckBean);
            }
        }
        if (allCheckingNumber <= 0) {
            allCheckedNumber = 0;
            allCheckingNumber = 0;
            EventBusUtil.post(new BoxAllCheckedEvent(allCheckRedirect));
            allCheckRedirect = false;
        }
        checkBoxOnlinePeriod(false);
    }

    /**
     * 盒子切换时使用
     */
    public void checkActiveBoxOnline(boolean isCheckImmediate) {
        if (mHandler != null) {
            while (mHandler.hasMessages(CHECK_USE_BOX)) {
                mHandler.removeMessages(CHECK_USE_BOX);
            }
        }
        checkBoxOnlinePeriod(isCheckImmediate);
        requestDeviceIpAddress();
    }

    private void checkBoxOnlinePeriod(boolean isCheckImmediate) {
        Context context = EulixSpaceApplication.getContext();
        EulixSpaceInfo eulixSpaceInfo = DataUtil.getLastEulixSpace(context);
        if (eulixSpaceInfo != null) {
            String lastBoxUuid = eulixSpaceInfo.getBoxUuid();
            String lastBoxBind = eulixSpaceInfo.getBoxBind();
            if (lastBoxUuid != null && lastBoxBind != null) {
                Map<String, String> queryMap = new HashMap<>();
                queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, lastBoxUuid);
                queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, lastBoxBind);
                List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, queryMap);
                if (boxValues != null) {
                    for (Map<String, String> boxValue : boxValues) {
                        if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_DOMAIN)
                                && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_STATUS)) {
                            int status = -1;
                            String boxDomain = boxValue.get(EulixSpaceDBManager.FIELD_BOX_DOMAIN);
                            String statusValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_STATUS);
                            if (statusValue != null) {
                                try {
                                    status = Integer.parseInt(statusValue);
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (status >= ConstantField.EulixDeviceStatus.OFFLINE && status <= ConstantField.EulixDeviceStatus.ONLINE_UNINITIALIZED) {
                                BoxNetworkCheckBean boxNetworkCheckBean = new BoxNetworkCheckBean();
                                boxNetworkCheckBean.setBoxUuid(lastBoxUuid);
                                boxNetworkCheckBean.setBoxBind(lastBoxBind);
                                boxNetworkCheckBean.setBoxDomain(boxDomain);
                                boxNetworkCheckBean.setBoxStatus(status);
                                boxNetworkCheckBean.setCheckType(BoxNetworkCheckBean.CHECK_TYPE_USE);
                                if (mHandler != null) {
                                    Message message = mHandler.obtainMessage(CHECK_USE_BOX, boxNetworkCheckBean);
                                    if (isCheckImmediate) {
                                        mHandler.sendMessage(message);
                                    } else {
                                        mHandler.sendMessageDelayed(message, (DataUtil.isSpaceStatusOnline(status, true)
                                                ? CHECK_LONG_PERIOD : CHECK_SHORT_PERIOD));
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }
        } else if (mHandler != null) {
            while (mHandler.hasMessages(CHECK_ALL_BOX)) {
                mHandler.removeMessages(CHECK_ALL_BOX);
            }
            mHandler.sendEmptyMessageDelayed(CHECK_ALL_BOX, CHECK_SHORT_PERIOD);
        }
    }

    private void checkBoxAllChecked() {
        allCheckedNumber += 1;
        if (allCheckedNumber >= allCheckingNumber) {
            allCheckedNumber = 0;
            allCheckingNumber = 0;
            EventBusUtil.post(new BoxAllCheckedEvent(allCheckRedirect));
            allCheckRedirect = false;
        }
    }

    private void checkBoxOnline(BoxNetworkCheckBean boxNetworkCheckBean) {
        Context context = EulixSpaceApplication.getContext();
        if (boxNetworkCheckBean != null/* && ((EulixSpaceApplication) EulixSpaceApplication.getContext()).getIsAppForeground()*/) {
            String boxDomain = boxNetworkCheckBean.boxDomain;
            String boxUUID = boxNetworkCheckBean.boxUuid;
            String boxBindValue = boxNetworkCheckBean.boxBind;
            String requestId = boxNetworkCheckBean.requestId;
            int checkType = boxNetworkCheckBean.checkType;
            GatewayUtil.getSpaceStatus(context, boxDomain, boxUUID, boxBindValue, false, null, new SpaceStatusExtensionCallback() {
                public void onSuccess(boolean lan, int code, String locationHost, String boxUuid, String boxBind, SpaceStatusResult result) {
                    boolean isCount = true;
                    if (boxUUID != null && boxUUID.equals(boxUuid) && boxBindValue != null && boxBindValue.equals(boxBind)) {
                        boolean isRedirectHandle = false;
                        if (code >= 300 && code < 400 && locationHost != null && !lan) {
                            isRedirectHandle = handleRedirectEvent(boxUuid, boxBind, locationHost);
                            if (isRedirectHandle) {
                                isCount = false;
                                boxNetworkCheckBean.setBoxDomain(locationHost);
                                checkBoxOnline(boxNetworkCheckBean);
                            }
                        }
                        if (!isRedirectHandle) {
                            int finalStatus = EulixSpaceDBUtil.getDeviceStatus(context, boxUuid, boxBind);
                            if (result != null) {
                                PlatformInfo platformInfo = result.getPlatformInfo();
                                if (platformInfo != null) {
                                    SpacePlatformInfo currentSpacePlatformInfo = null;
                                    EulixBoxOtherInfo eulixBoxOtherInfo = EulixSpaceDBUtil.getBoxOtherInfo(context, boxUuid, boxBind);
                                    if (eulixBoxOtherInfo != null) {
                                        currentSpacePlatformInfo = eulixBoxOtherInfo.getSpacePlatformInfo();
                                    }
                                    boolean isUpdate = false;
                                    boolean isPrivateSpacePlatform = !platformInfo.isOfficial();
                                    String platformServerUrl = platformInfo.getPlatformUrl();
                                    SpacePlatformInfo spacePlatformInfo = new SpacePlatformInfo();
                                    spacePlatformInfo.setPrivateSpacePlatform(isPrivateSpacePlatform);
                                    spacePlatformInfo.setPlatformServerUrl(platformServerUrl);
                                    String platformServerHost = StringUtil.urlToHost(platformServerUrl);
                                    if ((finalStatus == ConstantField.EulixDeviceStatus.ACTIVE || finalStatus == ConstantField.EulixDeviceStatus.OFFLINE_USE)
                                            && platformServerUrl != null && !StringUtil.compare(platformServerHost, DataUtil.getCurrentPlatformServerHost())) {
                                        Logger.d(TAG, "space status platform change request platform ability: " + platformServerUrl);
                                        if (currentSpacePlatformInfo == null) {
                                            EventBusUtil.post(new PlatformAbilityRequestEvent(platformServerUrl, false, true));
                                        } else {
                                            EventBusUtil.post(new PlatformAbilityRequestEvent(platformServerUrl
                                                    , !StringUtil.compare(platformServerHost, StringUtil.urlToHost(currentSpacePlatformInfo.getPlatformServerUrl())), true));
                                        }
                                    }
                                    boolean isHandle = false;
                                    JSONObject jsonObject = null;
                                    EulixSpaceDBBoxManager eulixSpaceDBBoxManager = EulixSpaceDBBoxManager.getInstance(boxUuid, boxBind);
                                    if (eulixSpaceDBBoxManager != null) {
                                        isHandle = true;
                                        jsonObject = new JSONObject();
                                        try {
                                            jsonObject.put("spacePlatformInfo", new Gson().toJson(spacePlatformInfo, SpacePlatformInfo.class));
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                            isHandle = false;
                                        }
                                    }
                                    if (isHandle) {
                                        int updateResult = eulixSpaceDBBoxManager.updateBoxOtherInfo(jsonObject
                                                , (currentSpacePlatformInfo == null ? null : (EulixSpaceDBBoxManager.UpdateFinishCallback) isUpdate1 -> {
                                                    if (isUpdate1) {
                                                        handleRemoveSwitchPlatformTask(boxUuid, boxBind);
                                                    }
                                                }));
                                        isUpdate = (updateResult > 0);
                                    } else {
                                        if (eulixBoxOtherInfo != null) {
                                            isUpdate = !SpacePlatformInfo.compare(spacePlatformInfo, currentSpacePlatformInfo);
                                        }
                                        if (isUpdate) {
                                            eulixBoxOtherInfo.setSpacePlatformInfo(spacePlatformInfo);
                                            Map<String, String> boxValue = new HashMap<>();
                                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_OTHER_INFO, new Gson().toJson(eulixBoxOtherInfo, EulixBoxOtherInfo.class));
                                            EulixSpaceDBUtil.updateBox(context, boxValue);
                                        }
                                    }
                                    if (currentSpacePlatformInfo != null && isUpdate) {
                                        handleRemoveSwitchPlatformTask(boxUuid, boxBind);
                                    }
                                }
                            }
                            if (!DataUtil.isSpaceStatusOnline(finalStatus, true)) {
                                String lastBoxUuid = null;
                                String lastBoxBind = null;
                                EulixSpaceInfo eulixSpaceInfo = DataUtil.getLastEulixSpace(context);
                                if (eulixSpaceInfo != null) {
                                    lastBoxUuid = eulixSpaceInfo.getBoxUuid();
                                    lastBoxBind = eulixSpaceInfo.getBoxBind();
                                }
                                Map<String, String> queryMap = new HashMap<>();
                                queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                                queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                                List<Map<String, String>> nBoxValues = EulixSpaceDBUtil.queryBox(context, queryMap);
                                if (nBoxValues != null) {
                                    for (Map<String, String> nBoxValue : nBoxValues) {
                                        if (nBoxValue != null) {
                                            boolean isDiskInitialized = (finalStatus != ConstantField.EulixDeviceStatus.OFFLINE_UNINITIALIZED);
                                            boolean requireToken = false;
                                            boolean isOfflineUse = (finalStatus == ConstantField.EulixDeviceStatus.OFFLINE_USE);
                                            boolean isLastSpace = (lastBoxUuid != null && lastBoxUuid.equals(boxUuid) && lastBoxBind != null && lastBoxBind.equals(boxBind));
                                            if (isDiskInitialized && isOfflineUse) {
                                                long currentTimestamp = System.currentTimeMillis();
                                                Long expireTimestamp = null;
                                                boolean isExpire = true;
                                                boolean isAutoLoginExpire = false;
                                                long loginValidTimestamp = -1L;
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
                                                            String loginValid = eulixBoxToken.getLoginValid();
                                                            if (loginValid != null) {
                                                                try {
                                                                    loginValidTimestamp = Long.parseLong(loginValid);
                                                                } catch (NumberFormatException e) {
                                                                    e.printStackTrace();
                                                                }
                                                            }
                                                        }
                                                        isExpire = (expireTimestamp == null || expireTimestamp < (currentTimestamp + 10 * 1000));
                                                        isAutoLoginExpire = (loginValidTimestamp >= 0 && loginValidTimestamp < currentTimestamp);
                                                    }
                                                }
                                                if (isExpire || isAutoLoginExpire) {
                                                    isOfflineUse = false;
                                                    requireToken = !isAutoLoginExpire;
                                                } else {
                                                    Integer boxAlarmId = DataUtil.getTokenAlarmId(boxUuid, boxBind);
                                                    if (boxAlarmId != null) {
                                                        AlarmUtil.cancelAlarm(context, boxAlarmId);
                                                    }
                                                    int alarmId = AlarmUtil.getAlarmId();
                                                    DataUtil.setTokenAlarmId(boxUuid, boxBind, alarmId);
                                                    long diffTimestamp = 60 * 1000L;
                                                    if (expireTimestamp > currentTimestamp) {
                                                        diffTimestamp = Math.min(((expireTimestamp - currentTimestamp) / 10), diffTimestamp);
                                                        AlarmUtil.setAlarm(context, (expireTimestamp - diffTimestamp), alarmId, boxUuid, boxBind, (diffTimestamp / 2));
                                                    } else {
                                                        AlarmUtil.setAlarm(context, (currentTimestamp + diffTimestamp), alarmId, boxUuid, boxBind, (diffTimestamp / 2));
                                                    }
                                                }
                                                requestDeviceIpAddress(boxUuid, boxBind);
                                            }

                                            int spaceStatus = (isDiskInitialized ? (isOfflineUse ? ConstantField.EulixDeviceStatus.ACTIVE
                                                    : (("1".equals(boxBind) || "-1".equals(boxBind))
                                                    ? ConstantField.EulixDeviceStatus.REQUEST_USE : ConstantField.EulixDeviceStatus.REQUEST_LOGIN))
                                                    : ConstantField.EulixDeviceStatus.ONLINE_UNINITIALIZED);
                                            Map<String, String> nBoxV = new HashMap<>();
                                            nBoxV.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                                            nBoxV.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                                            nBoxV.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(spaceStatus));
                                            EulixSpaceDBUtil.updateBox(context, nBoxV);
                                            BoxOnlineEvent boxOnlineEvent = new BoxOnlineEvent(boxUuid, boxBind, spaceStatus);
                                            EventBusUtil.post(boxOnlineEvent);

                                            if (requireToken && mCallback != null) {
                                                mCallback.createAuthTokenCallback(boxUuid, boxBind);
                                            }
                                        }
                                    }
                                }
                                if (finalStatus == ConstantField.EulixDeviceStatus.OFFLINE_USE) {
                                    EventBusUtil.post(new SpaceChangeEvent(true));
                                }
                            }

                            if (code >= 200 && code < 300 && finalStatus == ConstantField.EulixDeviceStatus.OFFLINE_USE) {
                                EventBusUtil.post(new SpaceOnlineCallbackEvent(boxUuid, boxBind, true));
                            }
                            if (!lan) {
                                handleAppointCodeEvent(code, boxUuid, boxBind);
                            }
                            if (BoxNetworkCheckBean.CHECK_TYPE_SPECIFIC == checkType && mCallback != null) {
                                mCallback.checkSpecificBoxCallback(boxUuid, boxBind, requestId, true);
                            }
                        }
                    }
                    if (BoxNetworkCheckBean.CHECK_TYPE_ALL == checkType && isCount) {
                        checkBoxAllChecked();
                    }
                }

                @Override
                public void onFailed(boolean lan, int code, String locationHost, String boxUuid, String boxBind) {
                    boolean isRedirectHandle = false;
                    if (code >= 300 && code < 400 && locationHost != null && !lan) {
                        isRedirectHandle = handleRedirectEvent(boxUuid, boxBind, locationHost);
                        if (isRedirectHandle) {
                            boxNetworkCheckBean.setBoxDomain(locationHost);
                            checkBoxOnline(boxNetworkCheckBean);
                        }
                    }
                    if (!isRedirectHandle) {
                        if (!lan) {
                            handleAppointCodeEvent(code, boxUuid, boxBind);
                        }
                        onError(lan, code, boxUuid, boxBind, "result null");
                    }
                }

                @Override
                public void onError(boolean lan, int code, String boxUuid, String boxBind, String msg) {
                    if (boxUUID != null && boxUUID.equals(boxUuid) && boxBindValue != null && boxBindValue.equals(boxBind)) {
                        int finalStatus = EulixSpaceDBUtil.getDeviceStatus(context, boxUuid, boxBind);
                        if (DataUtil.isSpaceStatusOnline(finalStatus, false)) {
                            List<Integer> boxAlarmIds = DataUtil.getTokenAlarmIds(boxUuid);
                            for (int boxAlarmId : boxAlarmIds) {
                                AlarmUtil.cancelAlarm(context, boxAlarmId);
                            }
                            int spaceStatus = (finalStatus == ConstantField.EulixDeviceStatus.ONLINE_UNINITIALIZED
                                    ? ConstantField.EulixDeviceStatus.OFFLINE_UNINITIALIZED
                                    : (finalStatus == ConstantField.EulixDeviceStatus.ACTIVE
                                    ? ConstantField.EulixDeviceStatus.OFFLINE_USE : ConstantField.EulixDeviceStatus.OFFLINE));
                            Map<String, String> nBoxV = new HashMap<>();
                            nBoxV.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                            nBoxV.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                            nBoxV.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(spaceStatus));
                            EulixSpaceDBUtil.updateBox(context, nBoxV);

                            BoxOnlineEvent boxOnlineEvent = new BoxOnlineEvent(boxUuid, boxBind, spaceStatus);
                            EventBusUtil.post(boxOnlineEvent);
                        }

                        if (finalStatus == ConstantField.EulixDeviceStatus.ACTIVE) {
                            EventBusUtil.post(new SpaceOnlineCallbackEvent(boxUuid, boxBind, false));
                            if (isShowOffline) {
                                toastManager.showImageTextToast(R.drawable.toast_refuse, R.string.active_device_offline_hint_v2);
                            }
                            updateDeviceNetwork(boxUuid, null);
                            EventBusUtil.post(new SpaceChangeEvent(false, true));
                        }

                        if (BoxNetworkCheckBean.CHECK_TYPE_SPECIFIC == checkType && mCallback != null) {
                            mCallback.checkSpecificBoxCallback(boxUuid, boxBind, requestId, false);
                        }
                    }
                    if (BoxNetworkCheckBean.CHECK_TYPE_ALL == checkType) {
                        checkBoxAllChecked();
                    }
                }
            });
        }
    }

    public void handleAppointCodeEvent(int code, String boxUuid, String boxBind) {
        if (boxUuid != null && boxBind != null) {
            switch (code) {
                case 461:
                    boolean isTrial = false;
                    EulixBoxToken eulixBoxToken = EulixSpaceDBUtil.getSpecificBoxToken(EulixSpaceApplication.getContext(), boxUuid, boxBind);
                    if (eulixBoxToken != null) {
                        isTrial = (EulixBoxToken.IDENTITY_TRIAL == eulixBoxToken.getIdentity());
                    }
                    if (isTrial) {
                        invalidBox(boxUuid, boxBind);
                    } else {
                        deleteBox(boxUuid, boxBind);
                    }
                    break;
                default:
                    break;
            }
        }
    }

    private void deleteBox(String boxUuid, String boxBind) {
        DataUtil.boxUnavailable(boxUuid, boxBind);
        Map<String, String> deleteMap = new HashMap<>();
        deleteMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
        deleteMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
        EulixSpaceDBUtil.deleteBox(EulixSpaceApplication.getContext(), deleteMap);
        BoxInsertDeleteEvent boxInsertDeleteEvent = new BoxInsertDeleteEvent(boxUuid, boxBind, false);
        EventBusUtil.post(boxInsertDeleteEvent);
    }

    private void invalidBox(String boxUuid, String boxBind) {
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
        queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(EulixSpaceApplication.getContext(), queryMap);
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
                        EulixSpaceDBUtil.updateBox(EulixSpaceApplication.getContext(), newBoxValue);
                        EventBusUtil.post(new SpaceValidEvent(boxUuid, boxBind, false));
                    }
                    if (isUsing) {
                        EulixPushManager eulixPushManager = EulixPushManager.getInstance();
                        EulixSpaceInfo eulixSpaceInfo = new EulixSpaceInfo();
                        eulixSpaceInfo.setBoxUuid(boxUuid);
                        eulixSpaceInfo.setBoxBind(boxBind);
                        eulixPushManager.resetAliveConnect(eulixSpaceInfo, false);
                        eulixPushManager.handleNativePushMessage(boxUuid, boxBind, ConstantField.PushType.NativeType.TRIAL_INVALID);
                    }
                }
            }
        }
    }

    public boolean handleRedirectEvent(String boxUuid, String boxBind, String locationHost) {
        boolean isHandle = false;
        Context context = EulixSpaceApplication.getContext();
        if (context != null && boxUuid != null && boxBind != null && locationHost != null
                && FormatUtil.isHttpUrlString(FormatUtil.generateHttpUrlString(locationHost))
                && AOSpaceUtil.isInternetAccessEnable(context, boxUuid, boxBind)) {
            isHandle = true;
            if (allCheckingNumber > 0) {
                allCheckRedirect = true;
            }
            EulixSpaceDBUtil.updateBoxDomain(context, boxUuid, boxBind, locationHost);
        }
        return isHandle;
    }

    public void handleRemoveSwitchPlatformTask(String boxUuid, String boxBind) {
        Context context = EulixSpaceApplication.getContext();
        if (boxUuid != null && boxBind != null) {
            long timestamp = System.currentTimeMillis();
            List<SwitchPlatformTaskBean> taskIds = DataUtil.getSwitchPlatformTaskList(context, boxUuid, boxBind);
            if (taskIds != null) {
                Iterator<SwitchPlatformTaskBean> iterator = taskIds.iterator();
                while (iterator.hasNext()) {
                    SwitchPlatformTaskBean bean = iterator.next();
                    if (bean != null && timestamp > bean.getTaskTimestamp()) {
                        iterator.remove();
                    }
                }
                DataUtil.setSwitchPlatformTaskList(context, boxUuid, boxBind, taskIds, false);
            }
        }
    }

    public void requestDeviceIpAddress() {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(EulixSpaceApplication.getContext());
        if (gatewayCommunicationBase != null) {
            try {
                String baseUrl = Urls.getBaseUrl();
                ThreadPool.getInstance().execute(() -> DeviceUtil.getLocalIps(baseUrl,
                        gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey(),
                        gatewayCommunicationBase.getTransformation(), gatewayCommunicationBase.getIvParams(),
                        new LocalIpInfoCallback() {
                            @Override
                            public void onSuccess(String code, String message, List<InitResponseNetwork> ipList) {
                                updateDeviceNetwork(gatewayCommunicationBase.getBoxUuid(), ipList);
                            }

                            @Override
                            public void onFailed() {
                                updateDeviceNetwork(gatewayCommunicationBase.getBoxUuid(), null);
                            }

                            @Override
                            public void onError(String errMsg) {
                                updateDeviceNetwork(gatewayCommunicationBase.getBoxUuid(), null);
                            }
                        }));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }

        }
    }

    public void requestDeviceIpAddress(String boxUuid, String boxBind) {
        if (boxUuid == null || boxBind == null) {
            requestDeviceIpAddress();
        } else {
            String boxDomain = null;
            String boxToken = null;
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(EulixSpaceApplication.getContext(), queryMap);
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_DOMAIN) && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
                        boxDomain = boxValue.get(EulixSpaceDBManager.FIELD_BOX_DOMAIN);
                        boxToken = boxValue.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                        break;
                    }
                }
            }
            if (boxDomain != null && boxToken != null) {
                EulixBoxToken eulixBoxToken = null;
                try {
                    eulixBoxToken = new Gson().fromJson(boxToken, EulixBoxToken.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
                if (eulixBoxToken != null) {
                    String finalBoxDomain = boxDomain;
                    EulixBoxToken finalEulixBoxToken = eulixBoxToken;
                    try {
                        ThreadPool.getInstance().execute(() -> DeviceUtil.getLocalIps(finalBoxDomain
                                , finalEulixBoxToken.getAccessToken(), finalEulixBoxToken.getSecretKey()
                                , finalEulixBoxToken.getTransformation(), finalEulixBoxToken.getInitializationVector()
                                , new LocalIpInfoCallback() {
                            @Override
                            public void onSuccess(String code, String message, List<InitResponseNetwork> ipList) {
                                updateDeviceNetwork(boxUuid, ipList);
                            }

                            @Override
                            public void onFailed() {
                                updateDeviceNetwork(boxUuid, null);
                            }

                            @Override
                            public void onError(String errMsg) {
                                updateDeviceNetwork(boxUuid, null);
                            }
                        }));
                    } catch (RejectedExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 网络状态更新到数据库
     * @param boxUuid
     * @param ipAddressList
     */
    private void updateDeviceNetwork(String boxUuid, List<InitResponseNetwork> ipAddressList) {
        Context context = EulixSpaceApplication.getContext();
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
        boolean isUpdate = false;
        if (boxValues != null) {
            for (Map<String, String> boxV : boxValues) {
                if (boxV != null && boxV.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                    String boxBind = boxV.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    boolean isHandle = false;
                    JSONObject jsonObject = null;
                    EulixSpaceDBBoxManager eulixSpaceDBBoxManager = EulixSpaceDBBoxManager.getInstance(boxUuid, boxBind);
                    if (eulixSpaceDBBoxManager != null) {
                        isHandle = true;
                        jsonObject = new JSONObject();
                        try {
                            jsonObject.put("networks", (ipAddressList == null ? "" : new Gson().toJson(ipAddressList, new TypeToken<List<InitResponseNetwork>>(){}.getType())));
                        } catch (JSONException e) {
                            e.printStackTrace();
                            isHandle = false;
                        }
                    }
                    if (isHandle) {
                        int result = eulixSpaceDBBoxManager.updateBoxInfo(jsonObject, isUpdate1 -> {
                            if (isUpdate1) {
                                DeviceNetworkResponseEvent responseEvent = new DeviceNetworkResponseEvent(boxUuid, ipAddressList);
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
                        if (eulixBoxInfo == null && !"1".equals(boxBind)) {
                            eulixBoxInfo = new EulixBoxInfo();
                        }
                        if (boxBind != null && eulixBoxInfo != null) {
                            Map<String, String> boxValue = new HashMap<>();
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                            eulixBoxInfo.setNetworks(ipAddressList);
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_INFO, new Gson().toJson(eulixBoxInfo, EulixBoxInfo.class));
                            EulixSpaceDBUtil.updateBox(context, boxValue);
                            isUpdate = true;
                        }
                    }
                }
            }
        }
        if (isUpdate) {
            DeviceNetworkResponseEvent responseEvent = new DeviceNetworkResponseEvent(boxUuid, ipAddressList);
            EventBusUtil.post(responseEvent);
        }
    }

    public static void setShowOffline(boolean isShowOffline) {
        BoxNetworkCheckManager.isShowOffline = isShowOffline;
    }

    //获取当前盒子在线情况
    public static boolean getActiveDeviceOnlineStrict() {
        return getActiveDeviceOnline(false);
    }

    public static boolean getActiveDeviceOnline(boolean defaultValue) {
        Boolean isOnline = getActiveDeviceOnline();
        return (isOnline == null ? defaultValue : isOnline);
    }

    /**
     * 查询当前使用中的空间是否在线
     * @return 在线（TRUE），离线（FALSE），不存在使用中的空间（NULL）
     */
    public static Boolean getActiveDeviceOnline() {
        Boolean isOnline = null;
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(EulixSpaceApplication.getContext(), EulixSpaceDBManager.FIELD_BOX_STATUS
                , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (boxValues != null && !boxValues.isEmpty()) {
            isOnline = true;
        } else {
            boxValues = EulixSpaceDBUtil.queryBox(EulixSpaceApplication.getContext(), EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
            if (boxValues != null && !boxValues.isEmpty()) {
                isOnline = false;
            }
        }
        return isOnline;
    }

    static class BoxNetworkCheckBean {
        public static final int CHECK_TYPE_USE = 0;
        public static final int CHECK_TYPE_SPECIFIC = CHECK_TYPE_USE + 1;
        public static final int CHECK_TYPE_ALL = CHECK_TYPE_SPECIFIC + 1;
        private String boxUuid;
        private String boxBind;
        private String boxDomain;
        private String requestId;
        private int boxStatus;
        private int checkType;

        public String getBoxUuid() {
            return boxUuid;
        }

        public void setBoxUuid(String boxUuid) {
            this.boxUuid = boxUuid;
        }

        public String getBoxBind() {
            return boxBind;
        }

        public void setBoxBind(String boxBind) {
            this.boxBind = boxBind;
        }

        public String getBoxDomain() {
            return boxDomain;
        }

        public void setBoxDomain(String boxDomain) {
            this.boxDomain = boxDomain;
        }

        public String getRequestId() {
            return requestId;
        }

        public void setRequestId(String requestId) {
            this.requestId = requestId;
        }

        public int getBoxStatus() {
            return boxStatus;
        }

        public void setBoxStatus(int boxStatus) {
            this.boxStatus = boxStatus;
        }

        public int getCheckType() {
            return checkType;
        }

        public void setCheckType(int checkType) {
            this.checkType = checkType;
        }

        @Override
        public String toString() {
            return "BoxNetworkCheckBean{" +
                    "boxUuid='" + boxUuid + '\'' +
                    ", boxBind='" + boxBind + '\'' +
                    ", boxDomain='" + boxDomain + '\'' +
                    ", requestId='" + requestId + '\'' +
                    ", boxStatus=" + boxStatus +
                    ", checkType=" + checkType +
                    '}';
        }
    }
}
