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

package xyz.eulix.space.presenter;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.bean.MessageCenterBean;
import xyz.eulix.space.bean.SecurityEmailInfo;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.network.notification.EulixNotificationUtil;
import xyz.eulix.space.network.notification.GetNotificationResult;
import xyz.eulix.space.network.notification.NotificationAllCallback;
import xyz.eulix.space.network.notification.NotificationDeleteCallback;
import xyz.eulix.space.network.notification.PageInfo;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.GatewayUtils;
import xyz.eulix.space.util.ThreadPool;
import xyz.eulix.space.util.Urls;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/6/20 15:42
 */
public class MessageCenterPresenter extends AbsPresenter<MessageCenterPresenter.IMessageCenter> {
    public interface IMessageCenter extends IBaseView {
        void allNotificationResult(Integer code, List<MessageCenterBean> messageCenterBeanList, PageInfo pageInfo);
        void deleteNotificationResult(Integer code, Integer result);
        void handleReadAll();
    }

    public EulixBoxBaseInfo getEulixBoxBaseInfo() {
        return EulixSpaceDBUtil.getActiveBoxBaseInfo(context, true);
    }

    private void generateValidAccessToken(String serviceFunction) {
        if (iView != null && serviceFunction != null) {
            switch (serviceFunction) {
                case ConstantField.ServiceFunction.NOTIFICATION_GET_ALL:
                    iView.allNotificationResult(ConstantField.OBTAIN_ACCESS_TOKEN_CODE, null, null);
                    break;
                case ConstantField.ServiceFunction.NOTIFICATION_DELETE_ALL:
                    iView.deleteNotificationResult(ConstantField.OBTAIN_ACCESS_TOKEN_CODE, null);
                    break;
                default:
                    break;
            }
        }
    }

    public void readAllMessage() {
        try {
            ThreadPool.getInstance().execute(() -> {
                EulixBoxBaseInfo baseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(context, true);
                if (baseInfo != null) {
                    String boxUuid = baseInfo.getBoxUuid();
                    String boxBind = baseInfo.getBoxBind();
                    if (boxUuid != null && boxBind != null) {
                        EulixSpaceDBUtil.readAppointPush(context, boxUuid, boxBind, true);
                    }
                }
                if (iView != null) {
                    iView.handleReadAll();
                }
            });
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public void deleteAllMessage() {
        try {
            ThreadPool.getInstance().execute(() -> {
                EulixBoxBaseInfo baseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(context, true);
                if (baseInfo != null) {
                    String boxUuid = baseInfo.getBoxUuid();
                    String boxBind = baseInfo.getBoxBind();
                    if (boxUuid != null && boxBind != null) {
                        EulixSpaceDBUtil.deleteAppointPush(context, boxUuid, boxBind);
                    }
                }
            });
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public List<MessageCenterBean> getLocalNotification() {
        List<MessageCenterBean> messageCenterBeans = new ArrayList<>();
        List<GetNotificationResult> getNotificationResults = null;
        EulixBoxBaseInfo baseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(context, true);
        if (baseInfo != null) {
            String boxUuid = baseInfo.getBoxUuid();
            String boxBind = baseInfo.getBoxBind();
            if (boxUuid != null && boxBind != null) {
                getNotificationResults = DataUtil.getNotificationResultList(boxUuid, boxBind);
                if (getNotificationResults == null) {
                    getNotificationResults = EulixSpaceDBUtil.generateNotificationResultListItems(context, boxUuid, boxBind);
                }
            }
        }
        if (getNotificationResults != null) {
            messageCenterBeans = convertToMessageCenterBeanList(getNotificationResults);
        }
        return checkMessageCenterBeanList(messageCenterBeans);
    }

    public void getAllNotification(int page) {
        getAllNotification(page, null);
    }

    private void getAllNotification(Integer page, Integer pageSize) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            String baseUrl = Urls.getBaseUrl();
            EulixNotificationUtil.getAllNotification(baseUrl
                    , gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                    , gatewayCommunicationBase.getIvParams(), page, pageSize, null, true, new NotificationAllCallback() {
                        @Override
                        public void onSuccess(int code, String message, String requestId, List<GetNotificationResult> notificationResults, PageInfo pageInfo) {
                            updateEulixPushDB(gatewayCommunicationBase.getBoxUuid(), gatewayCommunicationBase.getBoxBind(), notificationResults, pageInfo);
                            if (iView != null) {
                                iView.allNotificationResult(code, convertToMessageCenterBeanList(notificationResults), pageInfo);
                            }
                        }

                        @Override
                        public void onFailed(int code, String message, String requestId) {
                            if (iView != null) {
                                iView.allNotificationResult(code, null, null);
                            }
                        }

                        @Override
                        public void onError(String errMsg) {
                            if (iView != null) {
                                iView.allNotificationResult(ConstantField.SERVER_EXCEPTION_CODE, null, null);
                            }
                        }
                    });
        } else {
            generateValidAccessToken(ConstantField.ServiceFunction.NOTIFICATION_GET_ALL);
        }
    }

    public void deleteAllNotification() {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            String boxUuid = gatewayCommunicationBase.getBoxUuid();
            String boxBind = gatewayCommunicationBase.getBoxBind();
            deleteEulixPushCache(boxUuid, boxBind);
            String baseUrl = Urls.getBaseUrl();
            EulixNotificationUtil.deleteNotification(baseUrl
                    , gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                    , gatewayCommunicationBase.getIvParams(), null, new NotificationDeleteCallback() {
                        @Override
                        public void onSuccess(int code, String message, String requestId, Integer result) {
                            if (!((code >= 200 && code < 400) || code == ConstantField.KnownError.NotificationError.MESSAGE_NOT_EXIST)) {
                                updateEulixPushCache(boxUuid, boxBind);
                            }
                            if (iView != null) {
                                iView.deleteNotificationResult(code, result);
                            }
                        }

                        @Override
                        public void onFailed(int code, String message, String requestId) {
                            updateEulixPushCache(boxUuid, boxBind);
                            if (iView != null) {
                                iView.deleteNotificationResult(code, null);
                            }
                        }

                        @Override
                        public void onError(String errMsg) {
                            updateEulixPushCache(boxUuid, boxBind);
                            if (iView != null) {
                                iView.deleteNotificationResult(ConstantField.SERVER_EXCEPTION_CODE, null);
                            }
                        }
                    });
        } else {
            generateValidAccessToken(ConstantField.ServiceFunction.NOTIFICATION_DELETE_ALL);
        }
    }

    private void deleteEulixPushCache(String boxUuid, String boxBind) {
        if (boxUuid != null && boxBind != null) {
            DataUtil.deleteNotificationResultList(boxUuid, boxBind);
        }
    }

    private void updateEulixPushCache(String boxUuid, String boxBind) {
        if (boxUuid != null && boxBind != null) {
            List<GetNotificationResult> getNotificationResults = null;
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_PUSH_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_PUSH_BIND, boxBind);
            queryMap.put(EulixSpaceDBManager.FIELD_PUSH_SOURCE, "-1");
            List<Map<String, String>> pushValues = EulixSpaceDBUtil.queryPush(context, queryMap);
            if (pushValues != null && pushValues.size() > 0) {
                for (Map<String, String> pushValue : pushValues) {
                    if (pushValue != null && pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA)) {
                        String dataValue = pushValue.get(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA);
                        if (dataValue != null) {
                            try {
                                getNotificationResults = new Gson().fromJson(dataValue, new TypeToken<List<GetNotificationResult>>() {}.getType());
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }
                }
            }
            if (getNotificationResults == null) {
                getNotificationResults = new ArrayList<>();
            }
            DataUtil.setNotificationResultMap(boxUuid, boxBind, getNotificationResults);
        }
    }

    private void updateEulixPushDB(String boxUuid, String boxBind, List<GetNotificationResult> getNotificationResultList, PageInfo pageInfo) {
        if (boxUuid != null && boxBind != null) {
            boolean isInsert = true;
            List<GetNotificationResult> getNotificationResults = null;
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_PUSH_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_PUSH_BIND, boxBind);
            queryMap.put(EulixSpaceDBManager.FIELD_PUSH_SOURCE, "-1");
            List<Map<String, String>> pushValues = EulixSpaceDBUtil.queryPush(context, queryMap);
            if (pushValues != null && pushValues.size() > 0) {
                isInsert = false;
                for (Map<String, String> pushValue : pushValues) {
                    if (pushValue != null && pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA)) {
                        String dataValue = pushValue.get(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA);
                        if (dataValue != null) {
                            try {
                                getNotificationResults = new Gson().fromJson(dataValue, new TypeToken<List<GetNotificationResult>>() {}.getType());
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }
                }
            }
            if (getNotificationResults == null) {
                getNotificationResults = new ArrayList<>();
            }
            if (pageInfo == null || pageInfo.getPage() == null || pageInfo.getPage() == 1) {
                getNotificationResults.clear();
            }
            if (getNotificationResultList != null) {
                for (GetNotificationResult result : getNotificationResultList) {
                    if (result != null) {
                        getNotificationResults.add(result);
                    }
                }
            }
            DataUtil.setNotificationResultMap(boxUuid, boxBind, getNotificationResults);
            Map<String, String> pushV = new HashMap<>();
            pushV.put(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID, (boxUuid + "_" + boxBind));
            pushV.put(EulixSpaceDBManager.FIELD_PUSH_UUID, boxUuid);
            pushV.put(EulixSpaceDBManager.FIELD_PUSH_BIND, boxBind);
            pushV.put(EulixSpaceDBManager.FIELD_PUSH_PRIORITY, "-1");
            pushV.put(EulixSpaceDBManager.FIELD_PUSH_SOURCE, "-1");
            pushV.put(EulixSpaceDBManager.FIELD_PUSH_CONSUME, "-1");
            pushV.put(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA, new Gson().toJson(getNotificationResults, new TypeToken<List<GetNotificationResult>>() {}.getType()));
            pushV.put(EulixSpaceDBManager.FIELD_PUSH_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
            if (isInsert) {
                EulixSpaceDBUtil.insertPush(context, pushV);
            } else {
                EulixSpaceDBUtil.updatePush(context, pushV);
            }
        }
    }

    private MessageCenterBean convertToMessageCenterBean(GetNotificationResult getNotificationResult) {
        MessageCenterBean messageCenterBean = new MessageCenterBean();
        if (getNotificationResult != null) {
            messageCenterBean.setMessageId(getNotificationResult.getMessageId());
            messageCenterBean.setMessageType(getNotificationResult.getOptType());
            messageCenterBean.setMessageTimestamp(FormatUtil.parseFileApiTimestamp(getNotificationResult.getCreateAt()
                    , ConstantField.TimeStampFormat.FILE_API_DAY_FORMAT, ConstantField.TimeStampFormat.FILE_API_SPLIT));
            messageCenterBean.setData(getNotificationResult.getData());
        }
        return messageCenterBean;
    }

    private List<MessageCenterBean> convertToMessageCenterBeanList(List<GetNotificationResult> getNotificationResults) {
        List<MessageCenterBean> messageCenterBeans = new ArrayList<>();
        if (getNotificationResults != null) {
            for (GetNotificationResult getNotificationResult : getNotificationResults) {
                if (getNotificationResult != null) {
                    messageCenterBeans.add(convertToMessageCenterBean(getNotificationResult));
                }
            }
        }
        return messageCenterBeans;
    }

    private List<MessageCenterBean> checkMessageCenterBeanList(List<MessageCenterBean> messageCenterBeanList) {
        List<MessageCenterBean> messageCenterBeans = null;
        if (messageCenterBeanList != null) {
            messageCenterBeans = new ArrayList<>();
            List<String> messageIds = new ArrayList<>();
            for (MessageCenterBean messageCenterBean : messageCenterBeanList) {
                if (messageCenterBean != null) {
                    String messageId = messageCenterBean.getMessageId();
                    if (messageId != null && !messageIds.contains(messageId)) {
                        messageIds.add(messageId);
                        messageCenterBeans.add(messageCenterBean);
                    }
                }
            }
        }
        return messageCenterBeans;
    }

    public boolean hasSecurityEmail() {
        boolean hasEmail = false;
        SecurityEmailInfo securityEmailInfo = EulixSpaceDBUtil.getActiveSecurityEmailInfo(context);
        if (securityEmailInfo != null) {
            hasEmail = (securityEmailInfo.getEmailAccount() != null);
        }
        return hasEmail;
    }
}
