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

import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import xyz.eulix.space.network.interceptor.EulixGatewayInterceptor;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.network.OkHttpUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/6/8 15:44
 */
public class EulixNotificationManager {
    private static final String TAG = EulixNotificationManager.class.getSimpleName();

    private static OkHttpClient generateOkHttpClient(String boxDomain, UUID requestId, String requestType, String accessToken, String secret, String ivParams, String apiVersion) {
        return OkHttpUtil.generateOkHttpClient(true).newBuilder()
                .addInterceptor(new EulixGatewayInterceptor(boxDomain, requestId, requestType
                        , ConstantField.Algorithm.Transformation.AES_CBC_PKCS5, null, ivParams
                        , accessToken, secret, MediaType.parse("application/json; charset=utf-8")
                        , MediaType.parse("application/json; charset=utf-8"), apiVersion))
                .build();
    }

    private static String generateBaseUrl(String boxDomain) {
        String baseUrl = boxDomain;
        if (baseUrl == null) {
            baseUrl = ConstantField.URL.BASE_GATEWAY_URL_DEBUG;
        } else {
            while ((baseUrl.startsWith(":") || baseUrl.startsWith("/")) && baseUrl.length() > 1) {
                baseUrl = baseUrl.substring(1);
            }
            if (TextUtils.isEmpty(baseUrl)) {
                baseUrl = ConstantField.URL.BASE_GATEWAY_URL_DEBUG;
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

    private static Retrofit generateRetrofit(String baseUrl, OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
    }

    public static void getNotification(String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, @NonNull String messageId, IGetNotificationCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.NOTIFICATION_GET, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        EulixNotificationService service = retrofit.create(EulixNotificationService.class);
        Observable<GetNotificationResponse> observable = service.getNotification(uuid.toString(), messageId);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<GetNotificationResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(GetNotificationResponse getNotificationResponse) {
                        Logger.i(TAG, "on next: " + (getNotificationResponse == null ? "null" : getNotificationResponse.toString()));
                        if (callback != null) {
                            callback.onResult(getNotificationResponse);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                        Logger.e(TAG, "on error: " + errMsg);
                        if (callback != null) {
                            callback.onError(errMsg);
                        }
                    }

                    @Override
                    public void onComplete() {
                        Logger.d(TAG, "on complete");
                    }
                });
    }

    public static void getAllNotification(String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, NotificationAllRequest request, INotificationAllCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.NOTIFICATION_GET_ALL, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        EulixNotificationService service = retrofit.create(EulixNotificationService.class);
        Observable<NotificationAllResponse> observable = service.getAllNotification(uuid.toString(), request);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<NotificationAllResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(NotificationAllResponse notificationAllResponse) {
                        Logger.i(TAG, "on next: " + (notificationAllResponse == null ? "null" : notificationAllResponse.toString()));
                        if (callback != null) {
                            callback.onResult(notificationAllResponse);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                        Logger.e(TAG, "on error: " + errMsg);
                        if (callback != null) {
                            callback.onError(errMsg);
                        }
                    }

                    @Override
                    public void onComplete() {
                        Logger.d(TAG, "on complete");
                    }
                });
    }

    public static void deleteNotification(String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, NotificationDeleteRequest request, INotificationDeleteCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.NOTIFICATION_DELETE_ALL, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        EulixNotificationService service = retrofit.create(EulixNotificationService.class);
        Observable<NotificationDeleteResponse> observable = service.deleteNotification(uuid.toString(), request);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<NotificationDeleteResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(NotificationDeleteResponse notificationDeleteResponse) {
                        Logger.i(TAG, "on next: " + (notificationDeleteResponse == null ? "null" : notificationDeleteResponse.toString()));
                        if (callback != null) {
                            callback.onResult(notificationDeleteResponse);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                        Logger.e(TAG, "on error: " + errMsg);
                        if (callback != null) {
                            callback.onError(errMsg);
                        }
                    }

                    @Override
                    public void onComplete() {
                        Logger.d(TAG, "on complete");
                    }
                });
    }
}
