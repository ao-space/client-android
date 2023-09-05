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

package xyz.eulix.space.network.notification;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.ThreadPool;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/6/8 15:59
 */
public class EulixNotificationUtil {
    private static final String TAG = EulixNotificationUtil.class.getSimpleName();
    private static final String API_VERSION = ConstantField.BoxVersionName.VERSION_0_2_5;

    public static void getNotification(String requestId, String boxDomain, String accessToken, String secret, String ivParams, @NonNull String messageId, GetNotificationCallback callback) {
        try {
            ThreadPool.getInstance().execute(() -> EulixNotificationManager.getNotification(boxDomain, accessToken, secret, ivParams, API_VERSION, messageId, new IGetNotificationCallback() {
                @Override
                public void onResult(GetNotificationResponse response) {
                    GetNotificationResult result = null;
                    String code = null;
                    String message = null;
                    if (response != null) {
                        result = response.getResults();
                        code = response.getCode();
                        message = response.getMessage();
                    }
                    if (result == null) {
                        if (callback != null) {
                            callback.onFailed(DataUtil.stringCodeToInt(code), message, requestId);
                        }
                    } else if (callback != null) {
                        callback.onSuccess(DataUtil.stringCodeToInt(code), message, requestId, result);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (callback != null) {
                        callback.onError(errMsg, requestId);
                    }
                }
            }));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void getAllNotification(String boxDomain, String accessToken, String secret, String ivParams, @Nullable Integer page, @Nullable Integer pageSize, @Nullable List<String> typeList, NotificationAllCallback callback) {
        getAllNotification(boxDomain, accessToken, secret, ivParams, page, pageSize, typeList, false, callback);
    }

    public static void getAllNotification(String boxDomain, String accessToken, String secret, String ivParams, @Nullable Integer page, @Nullable Integer pageSize, @Nullable List<String> typeList, boolean isFore, NotificationAllCallback callback) {
        NotificationAllRequest notificationAllRequest = new NotificationAllRequest();
        notificationAllRequest.setPage(page);
        notificationAllRequest.setPageSize(pageSize);
        notificationAllRequest.setTypes(typeList);
        try {
            ThreadPool.getInstance().execute(() -> EulixNotificationManager.getAllNotification(boxDomain
                    , accessToken, secret, ivParams, API_VERSION, notificationAllRequest, new INotificationAllCallback() {
                @Override
                public void onResult(NotificationAllResponse response) {
                    int code = -1;
                    String message = "";
                    String requestId = "";
                    List<GetNotificationResult> notificationResults = null;
                    PageInfo pageInfo = null;
                    if (response != null) {
                        code = DataUtil.stringCodeToInt(response.getCode());
                        message = response.getMessage();
                        requestId = response.getRequestId();
                        NotificationAllResult notificationAllResult = response.getResults();
                        if (notificationAllResult != null) {
                            notificationResults = notificationAllResult.getNotification();
                            pageInfo = notificationAllResult.getPageInfo();
                        }
                    }
                    if (notificationResults == null) {
                        if (callback != null) {
                            callback.onFailed(code, message, requestId);
                        }
                    } else if (callback != null) {
                        callback.onSuccess(code, message, requestId, notificationResults, pageInfo);
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
    public static void deleteNotification(String boxDomain, String accessToken, String secret, String ivParams, @Nullable List<String> messageIdList, NotificationDeleteCallback callback) {
        NotificationDeleteRequest notificationDeleteRequest = new NotificationDeleteRequest();
        notificationDeleteRequest.setMessageIdList(messageIdList);
        try {
            ThreadPool.getInstance().execute(() -> EulixNotificationManager.deleteNotification(boxDomain
                    , accessToken, secret, ivParams, API_VERSION, notificationDeleteRequest, new INotificationDeleteCallback() {
                        @Override
                        public void onResult(NotificationDeleteResponse response) {
                            if (response == null) {
                                if (callback != null) {
                                    callback.onFailed(-1, "", "");
                                }
                            } else if (callback != null) {
                                callback.onSuccess(DataUtil.stringCodeToInt(response.getCode())
                                        , response.getMessage(), response.getRequestId(), response.getResults());
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
