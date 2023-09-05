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

package xyz.eulix.space.network.agent.disk;

import android.text.TextUtils;

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
import xyz.eulix.space.network.EulixBaseRequest;
import xyz.eulix.space.network.EulixBaseResponse;
import xyz.eulix.space.network.IEulixBaseResponseCallback;
import xyz.eulix.space.network.agent.AgentBaseResponse;
import xyz.eulix.space.network.agent.IAgentBaseResponseCallback;
import xyz.eulix.space.util.DebugUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.network.OkHttpUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/11/1 17:47
 */
public class DiskManager {
    private static final String TAG = DiskManager.class.getSimpleName();
    private String baseUrl;
    private Retrofit retrofit;

    DiskManager(String baseUri) {
        this.baseUrl = generateBaseUrl(baseUri);
        if (TextUtils.isEmpty(baseUrl)) {
            retrofit = null;
        } else {
            if (retrofit == null) {
                retrofit = new Retrofit.Builder()
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .addConverterFactory(GsonConverterFactory.create())
                        .baseUrl(baseUrl)
                        .build();
            }
        }
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

    public void getSpaceReadyCheck(IAgentBaseResponseCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateMediumTimeoutOkHttpClient());
            DiskService service = nRetrofit.create(DiskService.class);
            Observable<AgentBaseResponse> observable = service.getSpaceReadyCheck();
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<AgentBaseResponse>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "on subscribe");
                        }

                        @Override
                        public void onNext(AgentBaseResponse response) {
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
                            Logger.d(TAG, "on subscribe");
                        }
                    });
        }
    }

    public void getDiskRecognition(IAgentBaseResponseCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateMediumTimeoutOkHttpClient());
            DiskService service = nRetrofit.create(DiskService.class);
            Observable<AgentBaseResponse> observable = service.getDiskRecognition();
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<AgentBaseResponse>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "on subscribe");
                        }

                        @Override
                        public void onNext(AgentBaseResponse response) {
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
                            Logger.d(TAG, "on subscribe");
                        }
                    });
        }
    }

    public void diskInitialize(EulixBaseRequest request, IEulixBaseResponseCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateMediumTimeoutOkHttpClient());
            DiskService service = nRetrofit.create(DiskService.class);
            Observable<EulixBaseResponse> observable = service.diskInitialize(request);
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
                            Logger.d(TAG, "on subscribe");
                        }
                    });
        }
    }

    public void getDiskInitializeProgress(IAgentBaseResponseCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateMediumTimeoutOkHttpClient());
            DiskService service = nRetrofit.create(DiskService.class);
            Observable<AgentBaseResponse> observable = service.getDiskInitializeProgress();
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<AgentBaseResponse>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "on subscribe");
                        }

                        @Override
                        public void onNext(AgentBaseResponse response) {
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
                            Logger.d(TAG, "on subscribe");
                        }
                    });
        }
    }

    public void getDiskManagementList(IAgentBaseResponseCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateMediumTimeoutOkHttpClient());
            DiskService service = nRetrofit.create(DiskService.class);
            Observable<AgentBaseResponse> observable = service.getDiskManagementList();
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<AgentBaseResponse>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "on subscribe");
                        }

                        @Override
                        public void onNext(AgentBaseResponse response) {
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
                            Logger.d(TAG, "on subscribe");
                        }
                    });
        }
    }

    public void eulixSystemShutdown(IEulixBaseResponseCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateMediumTimeoutOkHttpClient());
            DiskService service = nRetrofit.create(DiskService.class);
            Observable<EulixBaseResponse> observable = service.eulixSystemShutdown();
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
                            Logger.d(TAG, "on subscribe");
                        }
                    });
        }
    }

    public void eulixSystemReboot(IEulixBaseResponseCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateMediumTimeoutOkHttpClient());
            DiskService service = nRetrofit.create(DiskService.class);
            Observable<EulixBaseResponse> observable = service.eulixSystemReboot();
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
                            Logger.d(TAG, "on subscribe");
                        }
                    });
        }
    }
}
