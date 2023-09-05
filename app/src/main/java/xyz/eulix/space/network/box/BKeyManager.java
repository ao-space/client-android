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

package xyz.eulix.space.network.box;

import android.text.TextUtils;

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
import xyz.eulix.space.interfaces.ResultCallback;
import xyz.eulix.space.interfaces.ResultCallbackObj;
import xyz.eulix.space.network.interceptor.EulixGatewayInterceptor;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DebugUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.network.OkHttpUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/8/19 17:05
 */
public class BKeyManager {
    private static final String TAG = BKeyManager.class.getSimpleName();
    private String boxDomain;
    private Retrofit retrofit;

    BKeyManager(String boxDomain) {
        this.boxDomain = boxDomain;
        if (TextUtils.isEmpty(boxDomain)) {
            retrofit = null;
        } else {
            if (retrofit == null) {
                retrofit = new Retrofit.Builder()
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .addConverterFactory(GsonConverterFactory.create())
                        .baseUrl(generateBaseUrl(boxDomain))
                        .build();
            }
        }
    }

    private static String generateBaseUrl(String boxDomain) {
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

    private static OkHttpClient generateOkHttpClient() {
        return OkHttpUtil.generateOkHttpClient(false).newBuilder()
                .build();
    }

    private static OkHttpClient generateOkHttpClient(String boxDomain, UUID requestId, String requestType, String accessToken, String secret, String transformation, String ivParams, String apiVersion) {
        return OkHttpUtil.generateOkHttpClient(true).newBuilder()
                .addInterceptor(new EulixGatewayInterceptor(boxDomain, requestId, requestType
                        , transformation, null, ivParams, accessToken, secret
                        , MediaType.parse("application/json; charset=utf-8")
                        , MediaType.parse("application/json; charset=utf-8"), apiVersion))
                .build();
    }

    private static Retrofit generateRetrofit(String baseUrl, OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
    }

    public void obtainAuthCode(String requestId, BKeyCreate bKeyCreate, IBKeyCreateCallback callback) {
        if (boxDomain != null) {
            Retrofit nRetrofit = generateRetrofit(generateBaseUrl(boxDomain), generateOkHttpClient());
            BKeyService service = nRetrofit.create(BKeyService.class);
            Observable<BKeyCreateResponseBody> observable = service.obtainAuthCode(requestId, bKeyCreate);
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<BKeyCreateResponseBody>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "on subscribe");
                        }

                        @Override
                        public void onNext(BKeyCreateResponseBody bKeyCreateResponseBody) {
                            Logger.i(TAG, "on next: " + (bKeyCreateResponseBody == null ? "null" : bKeyCreateResponseBody.toString()));
                            if (callback != null) {
                                callback.onResult(bKeyCreateResponseBody);
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

    public void obtainAuthResult(String requestId, String boxKey, boolean isAutoLogin, IBKeyPollCallback callback) {
        if (boxDomain != null) {
            Retrofit nRetrofit = generateRetrofit(generateBaseUrl(boxDomain), generateOkHttpClient());
            BKeyService service = nRetrofit.create(BKeyService.class);
            Observable<BKeyPollResponseBody> observable = service.obtainAuthResult(requestId, boxKey, isAutoLogin);
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<BKeyPollResponseBody>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "on subscribe");
                        }

                        @Override
                        public void onNext(BKeyPollResponseBody bKeyPollResponseBody) {
                            Logger.i(TAG, "on next: " + (bKeyPollResponseBody == null ? "null" : bKeyPollResponseBody.toString()));
                            if (callback != null) {
                                callback.onResult(bKeyPollResponseBody);
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

    //校验bkey
    public static void bKeyVerify(String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, String boxKey, ResultCallback callback) {
        UUID requestId = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, requestId, ConstantField.ServiceFunction.BOX_LOGIN_BKEY_VERIFY, accessToken, secret, transformation, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        BKeyService service = retrofit.create(BKeyService.class);
        Observable<BKeyCheckResponseBody> observable = service.bKeyVerify(requestId.toString(), boxKey);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<BKeyCheckResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(BKeyCheckResponseBody responseBody) {
                        Logger.i(TAG, "bKeyVerify on next: " + (responseBody == null ? "null" : responseBody.toString()));
                        if (callback != null) {
                            callback.onResult(responseBody != null && responseBody.results, "");
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                        Logger.e(TAG, "bKeyVerify on error: " + errMsg);
                        if (callback != null) {
                            callback.onResult(false, errMsg);
                        }
                    }

                    @Override
                    public void onComplete() {
                        Logger.d(TAG, "on complete");
                    }
                });
    }

    //盒子扫码授权登录，获取授权码
    public static void obtainBoxLoginAuthCode(String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, BKeyCreateCallback callback) {
        UUID requestId = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, UUID.randomUUID(), ConstantField.ServiceFunction.BOX_LOGIN_GET_AUTH_CODE, accessToken, secret, transformation, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        BKeyService service = retrofit.create(BKeyService.class);
        Observable<BoxLoginAuthCodeResponseBody> observable = service.obtainBoxLoginAuthCode(requestId.toString());
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<BoxLoginAuthCodeResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(BoxLoginAuthCodeResponseBody bKeyCreateResponseBody) {
                        Logger.i(TAG, "on next: " + (bKeyCreateResponseBody == null ? "null" : bKeyCreateResponseBody.toString()));
                        if (callback != null) {
                            if (bKeyCreateResponseBody != null) {
                                callback.onSuccess(bKeyCreateResponseBody.getCodeInt(), bKeyCreateResponseBody.getMessage(), bKeyCreateResponseBody.authCodeInfo);
                            } else {
                                callback.onFailed(-1, "");
                            }
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

    //盒子扫码授权登录，获取授权结果
    public static void obtainBoxLoginAuthResult(String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, String boxKey, boolean isAutoLogin, ResultCallbackObj callback) {
        UUID requestId = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, requestId, ConstantField.ServiceFunction.BOX_LOGIN_POLL_AUTH_RESULT, accessToken, secret, transformation, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        BKeyService service = retrofit.create(BKeyService.class);
        Observable<BKeyCheckResponseBody> observable = service.obtainBoxLoginAuthResult(requestId.toString(), boxKey, isAutoLogin);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<BKeyCheckResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(BKeyCheckResponseBody bKeyPollResponseBody) {
                        Logger.i(TAG, "on next: " + (bKeyPollResponseBody == null ? "null" : bKeyPollResponseBody.toString()));
                        if (callback != null) {
                            callback.onResult(true, bKeyPollResponseBody);
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
