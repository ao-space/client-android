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

package xyz.eulix.space.network.agent.platform;

import android.text.TextUtils;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import xyz.eulix.space.util.DebugUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.network.OkHttpUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/11/25 14:58
 */
public class EulixPlatformManager {
    private static final String TAG = EulixPlatformManager.class.getSimpleName();
    private String baseUrl;
    private Retrofit retrofit;

    EulixPlatformManager(String baseUri) {
        this.baseUrl = generateBaseUrl(baseUri);
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

    private OkHttpClient generateShortTimeoutOkHttpClient() {
        return OkHttpUtil.generateOkHttpClient(false).newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    private OkHttpClient generateMediumTimeoutOkHttpClient() {
        return OkHttpUtil.generateOkHttpClient(false).newBuilder()
                .connectTimeout(2, TimeUnit.MINUTES)
                .readTimeout(2, TimeUnit.MINUTES)
                .writeTimeout(2, TimeUnit.MINUTES)
                .build();
    }

    private OkHttpClient generateLongTimeoutOkHttpClient() {
        return OkHttpUtil.generateOkHttpClient(false).newBuilder()
                .connectTimeout(10, TimeUnit.MINUTES)
                .readTimeout(10, TimeUnit.MINUTES)
                .writeTimeout(10, TimeUnit.MINUTES)
                .build();
    }

    private Retrofit generateRetrofit(OkHttpClient okHttpClient) {
        if (okHttpClient == null || baseUrl == null) {
            if (retrofit == null) {
                retrofit = new Retrofit.Builder()
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .addConverterFactory(GsonConverterFactory.create())
                        .baseUrl(StringUtil.nullToEmpty(baseUrl))
                        .build();
            }
            return retrofit;
        } else {
            return new Retrofit.Builder()
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(okHttpClient)
                    .baseUrl(baseUrl)
                    .build();
        }
    }

    public void getPlatformAbility(IPlatformAbilityCallback callback) {
        if (baseUrl != null) {
            UUID uuid = UUID.randomUUID();
            Retrofit nRetrofit = generateRetrofit(generateShortTimeoutOkHttpClient());
            EulixPlatformService service = nRetrofit.create(EulixPlatformService.class);
            Observable<PlatformAbilityResponse> observable = service.getPlatformAbility(uuid.toString());
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<PlatformAbilityResponse>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "on subscribe");
                        }

                        @Override
                        public void onNext(PlatformAbilityResponse response) {
                            Logger.i(TAG, "on next: " + (response == null ? "null" : response.toString()));
                            if (callback != null) {
                                callback.onResponse(response);
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

    public void switchPlatform(SwitchPlatformRequest request, ISwitchPlatformCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateLongTimeoutOkHttpClient());
            EulixPlatformService service = nRetrofit.create(EulixPlatformService.class);
            Observable<SwitchPlatformResponse> observable = service.switchPlatform(request);
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<SwitchPlatformResponse>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "on subscribe");
                        }

                        @Override
                        public void onNext(SwitchPlatformResponse response) {
                            Logger.i(TAG, "on next: " + (response == null ? "null" : response.toString()));
                            if (callback != null) {
                                callback.onResponse(response);
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

    public void getSwitchPlatformStatus(String transId, ISwitchStatusCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateShortTimeoutOkHttpClient());
            EulixPlatformService service = nRetrofit.create(EulixPlatformService.class);
            Observable<SwitchStatusResponse> observable = service.getSwitchPlatformStatus(transId);
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<SwitchStatusResponse>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "on subscribe");
                        }

                        @Override
                        public void onNext(SwitchStatusResponse response) {
                            Logger.i(TAG, "on next: " + (response == null ? "null" : response.toString()));
                            if (callback != null) {
                                callback.onResponse(response);
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
}
