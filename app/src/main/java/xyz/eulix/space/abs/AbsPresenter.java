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

package xyz.eulix.space.abs;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.eulix.space.bean.ApplicationLockInfo;
import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.EulixSpaceInfo;
import xyz.eulix.space.bean.GranterAuthorizationBean;
import xyz.eulix.space.bean.GranterSecurityAuthenticationBean;
import xyz.eulix.space.bean.PushBean;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.did.event.DIDDocumentRequestEvent;
import xyz.eulix.space.event.EulixNotificationEvent;
import xyz.eulix.space.manager.EulixBiometricManager;
import xyz.eulix.space.manager.EulixPushManager;
import xyz.eulix.space.network.push.LoginConfirmBean;
import xyz.eulix.space.network.push.SecurityApplyBean;
import xyz.eulix.space.network.security.SecurityTokenResult;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.SystemUtil;

/**
 * Author:      Zhu Fuyu
 * Description: 基础presenter类，主要承载业务逻辑
 * History:     2021/7/16
 */
public class AbsPresenter<V extends IBaseView> {
    public V iView;
    public static Context context;
    protected EulixBiometricManager eulixBiometricManager;

    private Comparator<PushBean> pushBeanComparator = (o1, o2) -> {
        if (o1 == null || o2 == null) {
            if (o1 == null && o2 == null) {
                return 0;
            } else if (o1 == null) {
                return 1;
            } else {
                return -1;
            }
        } else {
            Integer consume1 = o1.getConsume();
            Integer consume2 = o2.getConsume();
            if (consume1.intValue() == consume2.intValue()) {
                Long timestamp1 = o1.getTimestamp();
                Long timestamp2 = o2.getTimestamp();
                return timestamp1.compareTo(timestamp2);
            } else {
                return consume2.compareTo(consume1);
            }
        }
    };

    public static void setAbsContext(Context appContext){
        context = appContext;
    }


    public void attachView(V v) {
        this.iView = v;
    }

    public void detachView() {
        this.iView = null;
    }

    public EulixBoxBaseInfo getBaseInfo() {
        return EulixSpaceDBUtil.getActiveBoxBaseInfo(context, true);
    }

    /**
     * 获取当前设备支持的生物特征
     * @return
     */
    public int getBiometricFeature() {
        return SystemUtil.getBiometricFeatureStatus(context);
    }

    /**
     * 准备验证
     * @param requestId
     * @return
     */
    public EulixBoxBaseInfo authenticate(String requestId) {
        EulixBoxBaseInfo eulixBoxBaseInfo = getBaseInfo();
        String boxUuid = null;
        String boxBind = null;
        if (eulixBoxBaseInfo != null) {
            boxUuid = eulixBoxBaseInfo.getBoxUuid();
            boxBind = eulixBoxBaseInfo.getBoxBind();
        }
        generateEulixBiometricManager();
        if (eulixBiometricManager != null) {
            eulixBiometricManager.setApplicationLockEventInfo(boxUuid, boxBind, requestId, false, true);
        }
        return eulixBoxBaseInfo;
    }

    /**
     * 准备验证
     * @param boxUuid
     * @param boxBind
     * @param requestId
     */
    public void authenticate(String boxUuid, String boxBind, String requestId) {
        generateEulixBiometricManager();
        if (eulixBiometricManager != null) {
            eulixBiometricManager.setApplicationLockEventInfo(boxUuid, boxBind, requestId, false, true);
        }
    }

    /**
     * 正式验证
     * @param requestId
     * @param activity
     * @param promptInfoBean
     * @param isSubThread
     * @return
     */
    public boolean authenticate(String requestId, @NonNull FragmentActivity activity, EulixBiometricManager.PromptInfoBean promptInfoBean, boolean isSubThread) {
        boolean result = false;
        generateEulixBiometricManager();
        if (eulixBiometricManager != null) {
            result = eulixBiometricManager.authenticate(requestId, activity, promptInfoBean, false, new EulixBiometricManager.EulixBiometricAuthenticationCallback() {
                @Override
                public void onError(int code, CharSequence errMsg, String responseId) {
                    DataUtil.setApplicationLockEventInfoError(requestId, true);
                    cancelAuthenticate(requestId, false);
                    if (iView != null) {
                        iView.authenticateError(code, errMsg, responseId, requestId);
                    }
                }

                @Override
                public void onResult(boolean isSuccess, String responseId) {
                    if (isSuccess) {
                        DataUtil.resetApplicationLockEventInfo();
                        cancelAuthenticate(requestId, false);
                    }
                    if (iView != null) {
                        iView.authenticateResult(isSuccess, responseId, requestId);
                    }
                }
            });
        }
        return result;
    }

    /**
     * 取消或者结束验证
     * @param requestId
     * @param isRealCancel true：主动取消；false：验证结束
     */
    public void cancelAuthenticate(String requestId, boolean isRealCancel) {
        generateEulixBiometricManager();
        if (eulixBiometricManager != null) {
            eulixBiometricManager.cancelAuthenticate(requestId, isRealCancel);
        }
    }

    /**
     * 设置指纹解锁开关
     * @param isFingerprintUnlock
     */
    public void setFingerprintLockInfo(boolean isFingerprintUnlock) {
        EulixBoxBaseInfo eulixBoxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(context, true);
        if (eulixBoxBaseInfo != null) {
            String boxUuid = eulixBoxBaseInfo.getBoxUuid();
            String boxBind = eulixBoxBaseInfo.getBoxBind();
            setFingerprintLockInfo(boxUuid, boxBind, isFingerprintUnlock);
        }
    }

    /**
     * 设置指纹解锁开关
     * @param boxUuid
     * @param boxBind
     * @param isFingerprintUnlock
     */
    public void setFingerprintLockInfo(String boxUuid, String boxBind, boolean isFingerprintUnlock) {
        generateEulixBiometricManager();
        if (eulixBiometricManager != null && boxUuid != null && boxBind != null) {
            ApplicationLockInfo applicationLockInfo = eulixBiometricManager.getSpaceApplicationLock(boxUuid, boxBind);
            if (applicationLockInfo == null) {
                applicationLockInfo = new ApplicationLockInfo();
            }
            applicationLockInfo.setFingerprintUnlock(isFingerprintUnlock);
            eulixBiometricManager.setSpaceApplicationLock(boxUuid, boxBind, applicationLockInfo);
        }
    }

    /**
     * 设置面容解锁开关
     * @param isFaceUnlock
     */
    public void setFaceLockInfo(boolean isFaceUnlock) {
        EulixBoxBaseInfo eulixBoxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(context, true);
        if (eulixBoxBaseInfo != null) {
            String boxUuid = eulixBoxBaseInfo.getBoxUuid();
            String boxBind = eulixBoxBaseInfo.getBoxBind();
            setFaceLockInfo(boxUuid, boxBind, isFaceUnlock);
        }
    }

    /**
     * 设置面容解锁开关
     * @param boxUuid
     * @param boxBind
     * @param isFaceUnlock
     */
    public void setFaceLockInfo(String boxUuid, String boxBind, boolean isFaceUnlock) {
        generateEulixBiometricManager();
        if (eulixBiometricManager != null && boxUuid != null && boxBind != null) {
            ApplicationLockInfo applicationLockInfo = eulixBiometricManager.getSpaceApplicationLock(boxUuid, boxBind);
            if (applicationLockInfo == null) {
                applicationLockInfo = new ApplicationLockInfo();
            }
            applicationLockInfo.setFaceUnlock(isFaceUnlock);
            eulixBiometricManager.setSpaceApplicationLock(boxUuid, boxBind, applicationLockInfo);
        }
    }

    /**
     * 设置生物解锁开关
     * @param isFingerprintUnlock
     * @param isFaceUnlock
     */
    public void setApplicationLockInfo(boolean isFingerprintUnlock, boolean isFaceUnlock) {
        EulixBoxBaseInfo eulixBoxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(context, true);
        if (eulixBoxBaseInfo != null) {
            setApplicationLockInfo(eulixBoxBaseInfo.getBoxUuid(), eulixBoxBaseInfo.getBoxBind(), isFingerprintUnlock, isFaceUnlock);
        }
    }

    /**
     * 设置生物解锁开关
     * @param boxUuid
     * @param boxBind
     * @param isFingerprintUnlock
     * @param isFaceUnlock
     */
    public void setApplicationLockInfo(String boxUuid, String boxBind, boolean isFingerprintUnlock, boolean isFaceUnlock) {
        generateEulixBiometricManager();
        if (eulixBiometricManager != null) {
            eulixBiometricManager.setSpaceApplicationLock(boxUuid, boxBind, isFingerprintUnlock, isFaceUnlock);
        }
    }

    protected boolean isPhysicalDevice(DeviceAbility deviceAbility) {
        boolean isPhysical = true;
        if (deviceAbility != null) {
            Integer deviceModelNumber = deviceAbility.getDeviceModelNumber();
            if (deviceModelNumber != null) {
                isPhysical = ((deviceModelNumber / 100) != -2);
            }
        }
        return isPhysical;
    }

    protected void generateEulixBiometricManager() {
        if (eulixBiometricManager == null) {
            eulixBiometricManager = EulixBiometricManager.getInstance();
        }
    }

    private List<PushBean> querySortPush() {
        List<PushBean> pushBeans = null;
        String boxUuid = null;
        String boxBind = null;
        EulixSpaceInfo eulixSpaceInfo = DataUtil.getActiveOrLastEulixSpace(context);
        if (eulixSpaceInfo != null) {
            boxUuid = eulixSpaceInfo.getBoxUuid();
            boxBind = eulixSpaceInfo.getBoxBind();
        }
        if (boxUuid != null && boxBind != null) {
            Map<String, String> queryMap2 = new HashMap<>();
            queryMap2.put(EulixSpaceDBManager.FIELD_PUSH_UUID, boxUuid);
            queryMap2.put(EulixSpaceDBManager.FIELD_PUSH_BIND, boxBind);
            queryMap2.put(EulixSpaceDBManager.FIELD_PUSH_CONSUME, "2");
            List<Map<String, String>> pushValues2 = EulixSpaceDBUtil.queryPush(context, queryMap2);
            Map<String, String> queryMap1 = new HashMap<>();
            queryMap1.put(EulixSpaceDBManager.FIELD_PUSH_UUID, boxUuid);
            queryMap1.put(EulixSpaceDBManager.FIELD_PUSH_BIND, boxBind);
            queryMap1.put(EulixSpaceDBManager.FIELD_PUSH_CONSUME, "1");
            List<Map<String, String>> pushValues1 = EulixSpaceDBUtil.queryPush(context, queryMap1);
            List<Map<String, String>> pushValues = new ArrayList<>();
            if (pushValues2 != null) {
                for (Map<String, String> pushV : pushValues2) {
                    if (pushV != null) {
                        pushValues.add(pushV);
                    }
                }
            }
            if (pushValues1 != null) {
                for (Map<String, String> pushV : pushValues1) {
                    if (pushV != null) {
                        pushValues.add(pushV);
                    }
                }
            }
            if (pushValues.size() > 0) {
                pushBeans = new ArrayList<>();
                for (Map<String, String> pushValue : pushValues) {
                    if (pushValue != null) {
                        int source = -1;
                        String sourceValue = null;
                        if (pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_SOURCE)) {
                            sourceValue = pushValue.get(EulixSpaceDBManager.FIELD_PUSH_SOURCE);
                        }
                        if (sourceValue != null) {
                            try {
                                source = Integer.parseInt(sourceValue);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                        if (source >= 0) {
                            PushBean bean = new PushBean();
                            bean.setSource(source);
                            int consume = 0;
                            String consumeValue = null;
                            if (pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_CONSUME)) {
                                consumeValue = pushValue.get(EulixSpaceDBManager.FIELD_PUSH_CONSUME);
                            }
                            if (consumeValue != null) {
                                try {
                                    consume = Integer.parseInt(consumeValue);
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                            }
                            bean.setConsume(consume);
                            bean.setMessageId(pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID)
                                    ? pushValue.get(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID) : "");
                            bean.setBoxUuid(pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_UUID)
                                    ? pushValue.get(EulixSpaceDBManager.FIELD_PUSH_UUID) : "");
                            bean.setBoxBind(pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_BIND)
                                    ? pushValue.get(EulixSpaceDBManager.FIELD_PUSH_BIND) : "");
                            bean.setType(pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_TYPE)
                                    ? pushValue.get(EulixSpaceDBManager.FIELD_PUSH_TYPE) : "");
                            int priority = 32;
                            String priorityValue = null;
                            if (pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_PRIORITY)) {
                                priorityValue = pushValue.get(EulixSpaceDBManager.FIELD_PUSH_PRIORITY);
                            }
                            if (priorityValue != null) {
                                try {
                                    priority = Integer.parseInt(priorityValue);
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                            }
                            bean.setPriority(priority);
                            bean.setTitle(pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_TITLE)
                                    ? pushValue.get(EulixSpaceDBManager.FIELD_PUSH_TITLE) : "");
                            bean.setContent(pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_CONTENT)
                                    ? pushValue.get(EulixSpaceDBManager.FIELD_PUSH_CONTENT) : "");
                            bean.setCreateAt(pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_CREATE_TIME)
                                    ? pushValue.get(EulixSpaceDBManager.FIELD_PUSH_CREATE_TIME) : "");
                            bean.setRawData(pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA)
                                    ? pushValue.get(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA) : "");
                            long timestamp = 0L;
                            String timestampValue = null;
                            if (pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_TIMESTAMP)) {
                                timestampValue = pushValue.get(EulixSpaceDBManager.FIELD_PUSH_TIMESTAMP);
                            }
                            if (timestampValue != null) {
                                try {
                                    timestamp = Long.parseLong(timestampValue);
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                            }
                            bean.setTimestamp(timestamp);
                            bean.setReserve(pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_RESERVE)
                                    ? pushValue.get(EulixSpaceDBManager.FIELD_PUSH_RESERVE) : "");
                            pushBeans.add(bean);
                        }
                    }
                }
                Collections.sort(pushBeans, pushBeanComparator);
            }
        }
        return pushBeans;
    }

    public PushBean pollPush(String pushCategory) {
        PushBean pushBean = null;
        List<PushBean> pushBeans = querySortPush();
        if (pushBeans != null && pushCategory != null) {
            for (PushBean sortBean : pushBeans) {
                if (sortBean != null) {
                    int sortPriority = sortBean.getPriority();
                    boolean isFind = false;
                    switch (pushCategory) {
                        case EulixPushManager.STRONG:
                            isFind = (sortPriority > 0 && sortPriority < 9);
                            break;
                        case EulixPushManager.WEAK:
                            isFind = (sortPriority > 8 && sortPriority < 17);
                            break;
                        default:
                            break;
                    }
                    if (isFind) {
                        pushBean = sortBean;
                        break;
                    }
                }
            }
        }
        return pushBean;
    }

    public void handlePush(String messageId, String boxUuid, String boxBind, boolean isCustom) {
        if (messageId != null) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID, messageId);
            if (boxUuid != null) {
                queryMap.put(EulixSpaceDBManager.FIELD_PUSH_UUID, boxUuid);
            }
            if (boxBind != null) {
                queryMap.put(EulixSpaceDBManager.FIELD_PUSH_BIND, boxBind);
            }
            List<Map<String, String>> pushValues = EulixSpaceDBUtil.queryPush(context, queryMap);
            if (pushValues != null && pushValues.size() > 0) {
                Map<String, String> pushValue = new HashMap<>();
                pushValue.put(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID, messageId);
                if (boxUuid != null) {
                    pushValue.put(EulixSpaceDBManager.FIELD_PUSH_UUID, boxUuid);
                }
                if (boxBind != null) {
                    pushValue.put(EulixSpaceDBManager.FIELD_PUSH_BIND, boxBind);
                }
                pushValue.put(EulixSpaceDBManager.FIELD_PUSH_CONSUME, String.valueOf(isCustom ? 4 : 2));
                EulixSpaceDBUtil.updatePush(context, pushValue);
                if (isCustom) {
                    EventBusUtil.post(new EulixNotificationEvent(messageId, null, 4));
                }
            }
        }
    }

    /**
     * 设置准备进入的免扫码，或者退出重置
     * @param boxUuid
     * @param boxBind
     * @param loginClientUuid
     */
    public void setGranterAuthorization(String boxUuid, String boxBind, String loginClientUuid) {
        GranterAuthorizationBean granterAuthorizationBean = null;
        if (boxUuid != null && boxBind != null && loginClientUuid != null) {
            granterAuthorizationBean = new GranterAuthorizationBean();
            granterAuthorizationBean.setBoxUuid(boxUuid);
            granterAuthorizationBean.setBoxBind(boxBind);
            granterAuthorizationBean.setLoginClientUuid(loginClientUuid);
        }
        DataUtil.setProcessGranterAuthorizationBean(granterAuthorizationBean);
    }

    /**
     * 设置准备进入的安保，或者退出重置
     * @param boxUuid
     * @param boxBind
     * @param authClientUuid
     * @param messageType
     */
    public void setGranterSecurityAuthentication(String boxUuid, String boxBind, String authClientUuid, String messageType, String applyId, SecurityTokenResult securityTokenResult) {
        GranterSecurityAuthenticationBean granterSecurityAuthenticationBean = null;
        if (boxUuid != null && boxBind != null && authClientUuid != null && messageType != null && securityTokenResult != null) {
            granterSecurityAuthenticationBean = new GranterSecurityAuthenticationBean();
            granterSecurityAuthenticationBean.setBoxUuid(boxUuid);
            granterSecurityAuthenticationBean.setBoxBind(boxBind);
            granterSecurityAuthenticationBean.setAuthClientUuid(authClientUuid);
            granterSecurityAuthenticationBean.setMessageType(messageType);
            granterSecurityAuthenticationBean.setApplyId(applyId);
            granterSecurityAuthenticationBean.setSecurityTokenResult(securityTokenResult);
        }
        DataUtil.setProcessGranterSecurityAuthenticationBean(granterSecurityAuthenticationBean);
    }

    /**
     * 消费重复的免扫码消息
     * @param boxUuid
     * @param boxBind
     * @param loginClientUuid
     */
    public void customLoginConfirmMessage(String boxUuid, String boxBind, String loginClientUuid) {
        if (boxUuid != null && boxBind != null && loginClientUuid != null) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_PUSH_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_PUSH_BIND, boxBind);
            List<Map<String, String>> pushValues = EulixSpaceDBUtil.queryPush(context, queryMap);
            if (pushValues != null) {
                for (Map<String, String> pushValue : pushValues) {
                    if (pushValue != null && pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID)
                            && pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_TYPE)
                            && pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_CONSUME)
                            && pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA)) {
                        String messageId = pushValue.get(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID);
                        String type = pushValue.get(EulixSpaceDBManager.FIELD_PUSH_TYPE);
                        String consumeValue = pushValue.get(EulixSpaceDBManager.FIELD_PUSH_CONSUME);
                        String rawData = pushValue.get(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA);
                        if (ConstantField.PushType.LOGIN_CONFIRM.equals(type) && rawData != null && messageId != null) {
                            int consume = 5;
                            if (consumeValue != null) {
                                consume = Integer.parseInt(consumeValue);
                            }
                            if (consume < 2) {
                                LoginConfirmBean loginConfirmBean = null;
                                try {
                                    loginConfirmBean = new Gson().fromJson(rawData, LoginConfirmBean.class);
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                                if (loginConfirmBean != null && loginClientUuid.equals(loginConfirmBean.getUuid())) {
                                    Map<String, String> newPushValue = new HashMap<>();
                                    newPushValue.put(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID, messageId);
                                    newPushValue.put(EulixSpaceDBManager.FIELD_PUSH_UUID, boxUuid);
                                    newPushValue.put(EulixSpaceDBManager.FIELD_PUSH_BIND, boxBind);
                                    newPushValue.put(EulixSpaceDBManager.FIELD_PUSH_CONSUME, "4");
                                    EulixSpaceDBUtil.updatePush(context, newPushValue);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 消费重复的安保消息
     * @param boxUuid
     * @param boxBind
     * @param authClientUuid
     * @param messageType
     */
    public void customSecurityAuthenticationMessage(String boxUuid, String boxBind, String authClientUuid, String messageType) {
        if (boxUuid != null && boxBind != null && authClientUuid != null && messageType != null) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_PUSH_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_PUSH_BIND, boxBind);
            List<Map<String, String>> pushValues = EulixSpaceDBUtil.queryPush(context, queryMap);
            if (pushValues != null) {
                for (Map<String, String> pushValue : pushValues) {
                    if (pushValue != null && pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID)
                            && pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_TYPE)
                            && pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_CONSUME)
                            && pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA)) {
                        String messageId = pushValue.get(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID);
                        String type = pushValue.get(EulixSpaceDBManager.FIELD_PUSH_TYPE);
                        String consumeValue = pushValue.get(EulixSpaceDBManager.FIELD_PUSH_CONSUME);
                        String rawData = pushValue.get(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA);
                        if (messageType.equals(type) && rawData != null && messageId != null) {
                            int consume = 5;
                            if (consumeValue != null) {
                                consume = Integer.parseInt(consumeValue);
                            }
                            if (consume < 2) {
                                SecurityApplyBean securityApplyBean = null;
                                try {
                                    securityApplyBean = new Gson().fromJson(rawData, SecurityApplyBean.class);
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                                if (securityApplyBean != null && authClientUuid.equals(securityApplyBean.getAuthClientUUid())) {
                                    Map<String, String> newPushValue = new HashMap<>();
                                    newPushValue.put(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID, messageId);
                                    newPushValue.put(EulixSpaceDBManager.FIELD_PUSH_UUID, boxUuid);
                                    newPushValue.put(EulixSpaceDBManager.FIELD_PUSH_BIND, boxBind);
                                    newPushValue.put(EulixSpaceDBManager.FIELD_PUSH_CONSUME, "4");
                                    EulixSpaceDBUtil.updatePush(context, newPushValue);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void refreshDIDDocument() {
        EulixBoxBaseInfo eulixBoxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(context);;
        if (eulixBoxBaseInfo != null) {
            String boxUuid = eulixBoxBaseInfo.getBoxUuid();
            String boxBind = eulixBoxBaseInfo.getBoxBind();
            if (boxUuid != null && boxBind != null) {
                EventBusUtil.post(new DIDDocumentRequestEvent(boxUuid, boxBind));
            }
        }
    }
}
