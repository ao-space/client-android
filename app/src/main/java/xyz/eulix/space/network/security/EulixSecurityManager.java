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

package xyz.eulix.space.network.security;

import android.text.TextUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
import xyz.eulix.space.network.EulixBaseResponse;
import xyz.eulix.space.network.IEulixBaseResponseCallback;
import xyz.eulix.space.network.interceptor.EulixGatewayInterceptor;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.network.OkHttpUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/7/8 15:16
 */
public class EulixSecurityManager {
    private static final String TAG = EulixSecurityManager.class.getSimpleName();

    private static OkHttpClient generateOkHttpClient(String boxDomain, UUID requestId, String requestType, String accessToken, String secret, String ivParams, String apiVersion) {
        return OkHttpUtil.generateOkHttpClient(true).newBuilder()
                .addInterceptor(new EulixGatewayInterceptor(boxDomain, requestId, requestType
                        , ConstantField.Algorithm.Transformation.AES_CBC_PKCS5, null, ivParams
                        , accessToken, secret, MediaType.parse("application/json; charset=utf-8")
                        , MediaType.parse("application/json; charset=utf-8"), apiVersion))
                .build();
    }

    private static OkHttpClient generateOkHttpClient(String boxDomain, UUID requestId, String requestType, String accessToken, String secret, String ivParams, String apiVersion, long timeout, TimeUnit unit) {
        return OkHttpUtil.generateOkHttpClient(true).newBuilder()
                .connectTimeout(timeout, unit)
                .readTimeout(timeout, unit)
                .writeTimeout(timeout, unit)
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

    public static void getDeviceHardwareInfo(String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, IDeviceHardwareInfoCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.DEVICE_HARDWARE_INFO, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        EulixSecurityService service = retrofit.create(EulixSecurityService.class);
        Observable<DeviceHardwareInfoResponse> observable = service.getDeviceHardwareInfo(uuid.toString());
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<DeviceHardwareInfoResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(DeviceHardwareInfoResponse response) {
                        Logger.i(TAG, "on next: " + (response == null ? "null" : response.toString()));
                        if (callback != null) {
                            callback.onResult(response);
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

    public static void binderModifySecurityPassword(BinderModifySecurityPasswordRequest request, String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, IEulixBaseResponseCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.SECURITY_PASSWORD_MODIFY_BINDER, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        EulixSecurityService service = retrofit.create(EulixSecurityService.class);
        Observable<EulixBaseResponse> observable = service.binderModifySecurityPassword(uuid.toString(), request);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<EulixBaseResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(EulixBaseResponse response) {
                        Logger.i(TAG, "on next: " + (response == null ? "null" : response.toString()));
                        if (callback != null) {
                            callback.onResult(response);
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

    public static void verifySecurityPassword(VerifySecurityPasswordRequest request, String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, IVerifySecurityCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.SECURITY_PASSWORD_VERIFY, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        EulixSecurityService service = retrofit.create(EulixSecurityService.class);
        Observable<VerifySecurityResponse> observable = service.verifySecurityPassword(uuid.toString(), request);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<VerifySecurityResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(VerifySecurityResponse response) {
                        Logger.i(TAG, "on next: " + (response == null ? "null" : response.toString()));
                        if (callback != null) {
                            callback.onResult(response);
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

    public static void binderResetSecurityPassword(BinderResetSecurityPasswordRequest request, String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, IEulixBaseResponseCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.SECURITY_PASSWORD_RESET_BINDER, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        EulixSecurityService service = retrofit.create(EulixSecurityService.class);
        Observable<EulixBaseResponse> observable = service.binderResetSecurityPassword(uuid.toString(), request);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<EulixBaseResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(EulixBaseResponse response) {
                        Logger.i(TAG, "on next: " + (response == null ? "null" : response.toString()));
                        if (callback != null) {
                            callback.onResult(response);
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


    public static void granteeApplyModifySecurityPassword(GranteeApplySetSecurityPasswordRequest request, String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, IEulixBaseResponseCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.SECURITY_PASSWORD_MODIFY_AUTHORIZED_APPLY, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        EulixSecurityService service = retrofit.create(EulixSecurityService.class);
        Observable<EulixBaseResponse> observable = service.granteeApplyModifySecurityPassword(uuid.toString(), request);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<EulixBaseResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(EulixBaseResponse response) {
                        Logger.i(TAG, "on next: " + (response == null ? "null" : response.toString()));
                        if (callback != null) {
                            callback.onResult(response);
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

    public static void binderAcceptModifySecurityPassword(SecurityTokenAcceptRequest request, String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, IEulixBaseResponseCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.SECURITY_PASSWORD_MODIFY_BINDER_ACCEPT, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        EulixSecurityService service = retrofit.create(EulixSecurityService.class);
        Observable<EulixBaseResponse> observable = service.binderAcceptModifySecurityPassword(uuid.toString(), request);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<EulixBaseResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(EulixBaseResponse response) {
                        Logger.i(TAG, "on next: " + (response == null ? "null" : response.toString()));
                        if (callback != null) {
                            callback.onResult(response);
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

    public static void granteeApplyResetSecurityPassword(GranteeApplySetSecurityPasswordRequest request, String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, IEulixBaseResponseCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.SECURITY_PASSWORD_RESET_AUTHORIZED_APPLY, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        EulixSecurityService service = retrofit.create(EulixSecurityService.class);
        Observable<EulixBaseResponse> observable = service.granteeApplyResetSecurityPassword(uuid.toString(), request);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<EulixBaseResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(EulixBaseResponse response) {
                        Logger.i(TAG, "on next: " + (response == null ? "null" : response.toString()));
                        if (callback != null) {
                            callback.onResult(response);
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

    public static void binderAcceptResetSecurityPassword(SecurityTokenAcceptRequest request, String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, IEulixBaseResponseCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.SECURITY_PASSWORD_RESET_BINDER_ACCEPT, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        EulixSecurityService service = retrofit.create(EulixSecurityService.class);
        Observable<EulixBaseResponse> observable = service.binderAcceptResetSecurityPassword(uuid.toString(), request);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<EulixBaseResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(EulixBaseResponse response) {
                        Logger.i(TAG, "on next: " + (response == null ? "null" : response.toString()));
                        if (callback != null) {
                            callback.onResult(response);
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

    public static void granteeModifySecurityPassword(GranteeModifySecurityPasswordRequest request, String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, IEulixBaseResponseCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.SECURITY_PASSWORD_MODIFY_AUTHORIZED, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        EulixSecurityService service = retrofit.create(EulixSecurityService.class);
        Observable<EulixBaseResponse> observable = service.granteeModifySecurityPassword(uuid.toString(), request);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<EulixBaseResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(EulixBaseResponse response) {
                        Logger.i(TAG, "on next: " + (response == null ? "null" : response.toString()));
                        if (callback != null) {
                            callback.onResult(response);
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

    public static void granteeResetSecurityPassword(GranteeResetSecurityPasswordRequest request, String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, IEulixBaseResponseCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.SECURITY_PASSWORD_RESET_AUTHORIZED, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        EulixSecurityService service = retrofit.create(EulixSecurityService.class);
        Observable<EulixBaseResponse> observable = service.granteeResetSecurityPassword(uuid.toString(), request);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<EulixBaseResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(EulixBaseResponse response) {
                        Logger.i(TAG, "on next: " + (response == null ? "null" : response.toString()));
                        if (callback != null) {
                            callback.onResult(response);
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

    public static void securityMessagePoll(SecurityMessagePollRequest request, String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, ISecurityMessagePollCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.SECURITY_MESSAGE_POLL, accessToken, secret, ivParams, apiVersion, 25, TimeUnit.SECONDS);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        EulixSecurityService service = retrofit.create(EulixSecurityService.class);
        Observable<SecurityMessagePollResponse> observable = service.securityMessagePoll(uuid.toString(), request);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<SecurityMessagePollResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(SecurityMessagePollResponse response) {
                        Logger.i(TAG, "on next: " + (response == null ? "null" : response.toString()));
                        if (callback != null) {
                            callback.onResult(response);
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
