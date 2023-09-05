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

package xyz.eulix.space.network.agent;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import xyz.eulix.space.bean.bind.KeyExchangeReq;
import xyz.eulix.space.bean.bind.PairingBoxInfoEnc;
import xyz.eulix.space.bean.bind.PairingBoxResult;
import xyz.eulix.space.bean.bind.PubKeyExchangeReq;
import xyz.eulix.space.bean.bind.RvokInfo;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DebugUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.network.OkHttpUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/7/27 10:40
 */
public class AgentManager {
    private static final String TAG = AgentManager.class.getSimpleName();
    private String baseUrl;
    private Retrofit retrofit;

    AgentManager(String baseUri) {
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

    public void getAgentInfo(IAgentInfoCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateShortTimeoutOkHttpClient());
            AgentService service = nRetrofit.create(AgentService.class);
//            Observable<AgentInfo> observable = service.getAgentInfo();
//            observable.subscribeOn(Schedulers.trampoline())
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe(new Observer<AgentInfo>() {
//                        @Override
//                        public void onSubscribe(Disposable d) {
//                            Logger.d(TAG, "on subscribe");
//                        }
//
//                        @Override
//                        public void onNext(AgentInfo agentInfo) {
//                            Logger.i(TAG, "on next: " + (agentInfo == null ? "null" : agentInfo.toString()));
//                            if (callback != null) {
//                                callback.onResult(agentInfo);
//                            }
//                        }
//
//                        @Override
//                        public void onError(Throwable e) {
//                            String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
//                            Logger.e(TAG, "on error: " + errMsg);
//                            if (callback != null) {
//                                callback.onError(errMsg);
//                            }
//                        }
//
//                        @Override
//                        public void onComplete() {
//                            Logger.d(TAG, "on complete");
//                        }
//                    });
            Observable<ResponseBody> observable = service.getAgentInfo();
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<ResponseBody>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "on subscribe");
                        }

                        @Override
                        public void onNext(ResponseBody responseBody) {
                            String body = null;
                            try {
                                body = responseBody.string();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            Logger.i(TAG, "on next: " + (body == null ? "null" : body));
                            AgentInfo agentInfo = null;
                            if (body != null && body.contains("\"isClientPaired\"")) {
                                int index = body.indexOf("\"isClientPaired\"");
                                if (index >= 0) {
                                    body = body.substring(index);
                                    int splitIndex = body.indexOf(",");
                                    if (splitIndex > 0) {
                                        body = "{" + body.substring(0, splitIndex) + "}";
                                        Logger.d(TAG, "body: " + body);
                                        try {
                                            agentInfo = new Gson().fromJson(body, AgentInfo.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }
                            }
                            Logger.i(TAG, "agent info: " + (agentInfo == null ? "null" : agentInfo.toString()));
                            if (callback != null) {
                                callback.onResult(agentInfo);
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

    public void exchangePublicKey(PubKeyExchangeReq pubKeyExchangeReq, IPubKeyExchangeCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateMediumTimeoutOkHttpClient());
            AgentService service = nRetrofit.create(AgentService.class);
            Observable<PubKeyExchangeResponse> observable = service.exchangePublicKey(pubKeyExchangeReq);
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<PubKeyExchangeResponse>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "on subscribe");
                        }

                        @Override
                        public void onNext(PubKeyExchangeResponse pubKeyExchangeResponse) {
                            Logger.i(TAG, "on next: " + (pubKeyExchangeResponse == null ? "null" : pubKeyExchangeResponse.toString()));
                            if (callback != null) {
                                callback.onResult(pubKeyExchangeResponse);
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

    public void exchangeSecretKey(KeyExchangeReq keyExchangeReq, IKeyExchangeCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateMediumTimeoutOkHttpClient());
            AgentService service = nRetrofit.create(AgentService.class);
            Observable<KeyExchangeResponse> observable = service.exchangeSecretKey(keyExchangeReq);
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<KeyExchangeResponse>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "on subscribe");
                        }

                        @Override
                        public void onNext(KeyExchangeResponse keyExchangeResponse) {
                            Logger.i(TAG, "on next: " + (keyExchangeResponse == null ? "null" : keyExchangeResponse.toString()));
                            if (callback != null) {
                                callback.onResult(keyExchangeResponse);
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

    public void pairing(PairingClientInfo pairingClientInfo, int version, IPairingCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateLongTimeoutOkHttpClient());
            AgentService service = nRetrofit.create(AgentService.class);
            Observable<PairingResponseBody> observable = null;
            switch (version) {
                case 2:
                    observable = service.pairingV2(pairingClientInfo);
                    break;
                case 1:
                    observable = service.pairing(pairingClientInfo);
                    break;
                default:
                    break;
            }
            if (observable != null) {
                observable.subscribeOn(Schedulers.trampoline())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<PairingResponseBody>() {
                            @Override
                            public void onSubscribe(Disposable d) {
                                Logger.d(TAG, "on subscribe");
                            }

                            @Override
                            public void onNext(PairingResponseBody pairingResponseBody) {
                                Logger.i(TAG, "on next: " + (pairingResponseBody == null ? "null" : pairingResponseBody.toString()));
                                if (callback != null) {
                                    callback.onResult(pairingResponseBody);
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

    public void pairing(PairingClientInfo pairingClientInfo, IPairingEncCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateLongTimeoutOkHttpClient());
            AgentService service = nRetrofit.create(AgentService.class);
            Observable<PairingBoxResult> observable = service.pairingV3(pairingClientInfo);
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<PairingBoxResult>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "on subscribe");
                        }

                        @Override
                        public void onNext(PairingBoxResult pairingBoxResult) {
                            Logger.i(TAG, "on next: " + (pairingBoxResult == null ? "null" : pairingBoxResult.toString()));
                            if (callback != null) {
                                callback.onResult(pairingBoxResult);
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

    public void pairing(PairingBoxInfoEnc pairingBoxInfoEnc, IPairingEncCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateLongTimeoutOkHttpClient());
            AgentService service = nRetrofit.create(AgentService.class);
            Observable<PairingBoxResult> observable = service.pairing(pairingBoxInfoEnc);
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<PairingBoxResult>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "on subscribe");
                        }

                        @Override
                        public void onNext(PairingBoxResult pairingBoxResult) {
                            Logger.i(TAG, "on next: " + (pairingBoxResult == null ? "null" : pairingBoxResult.toString()));
                            if (callback != null) {
                                callback.onResult(pairingBoxResult);
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

    public void getAuthInfo(IAuthInfoCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateShortTimeoutOkHttpClient());
            AgentService service = nRetrofit.create(AgentService.class);
            Observable<AuthInfoRsp> observable = service.getAuthInfo();
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<AuthInfoRsp>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "on subscribe");
                        }

                        @Override
                        public void onNext(AuthInfoRsp authInfoRsp) {
                            Logger.i(TAG, "on next: " + (authInfoRsp == null ? "null" : authInfoRsp.toString()));
                            if (callback != null) {
                                callback.onResult(authInfoRsp);
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

    public void setPassword(PasswordInfo passwordInfo, ISetPasswordCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateMediumTimeoutOkHttpClient());
            AgentService service = nRetrofit.create(AgentService.class);
            Observable<SetPasswordResponse> observable = service.setPassword(passwordInfo);
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<SetPasswordResponse>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "on subscribe");
                        }

                        @Override
                        public void onNext(SetPasswordResponse setPasswordResponse) {
                            Logger.i(TAG, "on next: " + (setPasswordResponse == null ? "null" : setPasswordResponse.toString()));
                            if (callback != null) {
                                callback.onResult(setPasswordResponse);
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

    public void revoke(AdminRevokeReq adminRevokeReq, IRevokeCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateMediumTimeoutOkHttpClient());
            AgentService service = nRetrofit.create(AgentService.class);
            Observable<AdminRevokeResponse> observable = service.revoke(adminRevokeReq);
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<AdminRevokeResponse>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "on subscribe");
                        }

                        @Override
                        public void onNext(AdminRevokeResponse adminRevokeResponse) {
                            Logger.i(TAG, "on next: " + (adminRevokeResponse == null ? "null" : adminRevokeResponse.toString()));
                            if (callback != null) {
                                callback.onResult(adminRevokeResponse);
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

    public void revoke(RvokInfo rvokInfo, IRevokeCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateMediumTimeoutOkHttpClient());
            AgentService service = nRetrofit.create(AgentService.class);
            Observable<AdminRevokeResponse> observable = service.revoke(rvokInfo);
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<AdminRevokeResponse>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "on subscribe");
                        }

                        @Override
                        public void onNext(AdminRevokeResponse adminRevokeResponse) {
                            Logger.i(TAG, "on next: " + (adminRevokeResponse == null ? "null" : adminRevokeResponse.toString()));
                            if (callback != null) {
                                callback.onResult(adminRevokeResponse);
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

    public void reset(ResetClientReq resetClientReq, IResetCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateMediumTimeoutOkHttpClient());
            AgentService service = nRetrofit.create(AgentService.class);
            Observable<BaseRsp> observable = service.reset(resetClientReq);
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<BaseRsp>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "on subscribe");
                        }

                        @Override
                        public void onNext(BaseRsp baseRsp) {
                            Logger.i(TAG, "on next: " + (baseRsp == null ? "null" : baseRsp.toString()));
                            if (callback != null) {
                                callback.onResult(baseRsp);
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

    public void agentPassthrough(PassthroughRequest request, IAgentCallCallback callback) {
        if (baseUrl != null) {
            String requestId = UUID.randomUUID().toString();
            Retrofit nRetrofit = generateRetrofit(generateMediumTimeoutOkHttpClient());
            AgentService service = nRetrofit.create(AgentService.class);
            Observable<AgentCallResponse> observable = service.agentPassthrough(requestId, request);
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<AgentCallResponse>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "on subscribe");
                        }

                        @Override
                        public void onNext(AgentCallResponse agentCallResponse) {
                            Logger.i(TAG, "on next: " + (agentCallResponse == null ? "null" : agentCallResponse.toString()));
                            if (callback != null) {
                                callback.onResult(agentCallResponse);
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

    public void initial(IInitialCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateMediumTimeoutOkHttpClient());
            AgentService service = nRetrofit.create(AgentService.class);
            Observable<InitialRsp> observable = service.initial();
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<InitialRsp>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "on subscribe");
                        }

                        @Override
                        public void onNext(InitialRsp initialRsp) {
                            Logger.i(TAG, "on next: " + (initialRsp == null ? "null" : initialRsp.toString()));
                            if (callback != null) {
                                callback.onResult(initialRsp);
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

    public void initial(PasswordInfo passwordInfo, IInitialCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateMediumTimeoutOkHttpClient());
            AgentService service = nRetrofit.create(AgentService.class);
            Observable<InitialRsp> observable = service.initial(passwordInfo);
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<InitialRsp>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "on subscribe");
                        }

                        @Override
                        public void onNext(InitialRsp initialRsp) {
                            Logger.i(TAG, "on next: " + (initialRsp == null ? "null" : initialRsp.toString()));
                            if (callback != null) {
                                callback.onResult(initialRsp);
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

    public void pairInit(IPairInitCallback callback) {
        if (baseUrl != null) {
            Retrofit nRetrofit = generateRetrofit(generateMediumTimeoutOkHttpClient());
            AgentService service = nRetrofit.create(AgentService.class);
            Observable<PairInitResponse> observable = service.pairInit();
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<PairInitResponse>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "on subscribe");
                        }

                        @Override
                        public void onNext(PairInitResponse pairInitResponse) {
                            Logger.i(TAG, "on next: " + (pairInitResponse == null ? "null" : pairInitResponse.toString()));
                            if (callback != null) {
                                callback.onResult(pairInitResponse);
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
