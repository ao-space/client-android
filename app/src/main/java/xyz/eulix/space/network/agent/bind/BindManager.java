package xyz.eulix.space.network.agent.bind;

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
import xyz.eulix.space.network.agent.disk.DiskService;
import xyz.eulix.space.util.DebugUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.network.OkHttpUtil;

public class BindManager {
    private static final String TAG = BindManager.class.getSimpleName();
    private String baseUrl;
    private Retrofit retrofit;

    BindManager(String baseUri) {
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

    public void bindCommunicationStart(IEulixBaseResponseCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateMediumTimeoutOkHttpClient());
            BindService service = nRetrofit.create(BindService.class);
            Observable<EulixBaseResponse> observable = service.bindCommunicationStart();
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

    public void getBindCommunicationProgress(IAgentBaseResponseCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateShortTimeoutOkHttpClient());
            BindService service = nRetrofit.create(BindService.class);
            Observable<AgentBaseResponse> observable = service.getBindCommunicationProgress();
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

    public void bindSpaceCreate(EulixBaseRequest request, IAgentBaseResponseCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateMediumTimeoutOkHttpClient());
            BindService service = nRetrofit.create(BindService.class);
            Observable<AgentBaseResponse> observable = service.bindSpaceCreate(request);
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

    public void bindRevoke(EulixBaseRequest request, IAgentBaseResponseCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateMediumTimeoutOkHttpClient());
            BindService service = nRetrofit.create(BindService.class);
            Observable<AgentBaseResponse> observable = service.bindRevoke(request);
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
}
