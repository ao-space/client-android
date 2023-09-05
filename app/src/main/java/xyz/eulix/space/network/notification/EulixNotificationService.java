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

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;
import xyz.eulix.space.util.ConstantField;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/6/8 15:30
 */
public interface EulixNotificationService {
    @Headers({"Accept: application/json"})
    @GET(ConstantField.URL.GET_NOTIFICATION_API)
    Observable<GetNotificationResponse> getNotification(@Header("Request-Id") String requestId, @Query("messageId") String messageId);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.NOTIFICATION_ALL_API)
    Observable<NotificationAllResponse> getAllNotification(@Header("Request-Id") String requestId, @Body NotificationAllRequest notificationAllRequest);

    @Headers({"Content-Type: application/json","Accept: application/json"})
    @POST(ConstantField.URL.NOTIFICATION_ALL_DELETE_API)
    Observable<NotificationDeleteResponse> deleteNotification(@Header("Request-Id") String requestId, @Body NotificationDeleteRequest notificationDeleteRequest);
}
