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

package xyz.eulix.space.network.gateway;

import android.net.Uri;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.ConnectionPool;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import xyz.eulix.space.interfaces.ResultCallback;
import xyz.eulix.space.network.interceptor.EncryptJsonInterceptor;
import xyz.eulix.space.network.interceptor.EulixCommonInterceptor;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.DebugUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.network.OkHttpUtil;

/**
 * @author: chenjiawei
 * date: 2021/7/7 16:16
 */
public class GatewayManager {
    private static final String TAG = GatewayManager.class.getSimpleName();
    private String boxDomain;
    private OkHttpClient okHttpClient;
    private OkHttpClient nonRedirectOkHttpClient;
    private Retrofit retrofit;

    public GatewayManager(String boxDomain) {
        this.boxDomain = boxDomain;
        if (TextUtils.isEmpty(boxDomain)) {
            retrofit = null;
            okHttpClient = null;
        } else {
            if (okHttpClient == null) {
                okHttpClient = OkHttpUtil.generateOkHttpClient(false).newBuilder()
                        .addInterceptor(new EulixCommonInterceptor())
                        .build();
            }
            if (retrofit == null) {
                retrofit = new Retrofit.Builder()
                        .baseUrl(generateBaseUrl(boxDomain))
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .addConverterFactory(GsonConverterFactory.create())
                        .client(okHttpClient)
                        .build();
            }
        }
    }

    GatewayManager(String boxDomain, String boxPublicKey) {
        this.boxDomain = boxDomain;
        if (TextUtils.isEmpty(boxDomain)) {
            retrofit = null;
            okHttpClient = null;
        } else {
            if (okHttpClient == null) {
                okHttpClient = OkHttpUtil.generateOkHttpClient(false).newBuilder()
                        .addInterceptor(new EncryptJsonInterceptor(boxPublicKey, ConstantField.ApiType.CREATE_AUTH_TOKEN
                                , ConstantField.Algorithm.Transformation.RSA_ECB_PKCS1, null
                                , MediaType.parse("application/json; charset=utf-8"), MediaType.parse("application/json; charset=utf-8")))
                        .build();
            }
            if (retrofit == null) {
                retrofit = new Retrofit.Builder()
                        .baseUrl(generateBaseUrl(boxDomain))
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        .addConverterFactory(GsonConverterFactory.create())
                        .client(okHttpClient)
                        .build();
            }
        }
    }

    private void generateNonRedirectOkHttpClient() {
        if (nonRedirectOkHttpClient == null) {
            nonRedirectOkHttpClient = OkHttpUtil.generateOkHttpClient(false).newBuilder()
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .addInterceptor(new EulixCommonInterceptor())
                    .connectionPool(new ConnectionPool(5, 10, TimeUnit.SECONDS))
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

    public void getSpaceStatus(ISpaceStatusCallback callback) {
        if (retrofit != null && boxDomain != null) {
            GatewayService service = retrofit.create(GatewayService.class);
            Observable<SpaceStatusResult> observable = service.getSpaceStatus();
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<SpaceStatusResult>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "get space status on subscribe");
                        }

                        @Override
                        public void onNext(SpaceStatusResult spaceStatusResult) {
                            Logger.i(TAG, "get space status on next: " + (spaceStatusResult == null ? "null" : spaceStatusResult));
                            if (callback != null) {
                                callback.onResult(spaceStatusResult);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                            Logger.e(TAG, "get space status on error: " + errMsg);
                            if (callback != null) {
                                callback.onError(errMsg);
                            }
                        }

                        @Override
                        public void onComplete() {
                            Logger.d(TAG, "get space status on complete");
                        }
                    });
        }
    }

    public void getSpaceStatus(String requestId, ISpaceStatusExtensionCallback callback) {
        if (boxDomain != null) {
            HttpUrl httpUrl = HttpUrl.parse(generateBaseUrl(boxDomain) + ConstantField.URL.SPACE_STATUS_API);
            if (httpUrl != null) {
                Request.Builder builder = new Request.Builder()
                        .url(httpUrl);
                if (requestId != null) {
                    builder.addHeader("Request-Id", requestId);
                }
                Request request = builder
                        .get()
                        .build();
                generateNonRedirectOkHttpClient();
                Call call = nonRedirectOkHttpClient.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        if (callback != null) {
                            callback.onError(500, e.getMessage(), requestId);
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        int code = response.code();
                        String message = response.message();
                        String locationHost = null;
                        SpaceStatusResult spaceStatusResult = null;
                        if (code >= 300 && code < 400) {
                            String location = response.header("Location", null);
                            Logger.d(TAG, "location: " + location);
                            if (location != null) {
                                Uri redirectUri = Uri.parse(location);
                                if (redirectUri != null) {
                                    locationHost = redirectUri.getHost();
                                }
                            }
                        }
                        ResponseBody responseBody = response.body();
                        if (responseBody != null) {
                            String content = null;
                            try {
                                content = responseBody.string();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if (content != null) {
                                try {
                                    spaceStatusResult = new Gson().fromJson(content, SpaceStatusResult.class);
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        Logger.d(TAG, "get space status code: " + code + ", message: " + message + ", location host: " + locationHost + ", result: " + spaceStatusResult + ", request url: " + httpUrl.toString());
                        if (callback != null) {
                            callback.onResult(code, message, requestId, locationHost, spaceStatusResult);
                        }
                    }
                });
            } else if (callback != null) {
                callback.onError(-1, "http url null", requestId);
            }
        } else {
            callback.onError(-1, "box domain null", requestId);
        }
    }

    public void getSpacePoll(String requestId, String accessToken, ISpacePollCallback callback) {
        if (retrofit != null && boxDomain != null) {
            GatewayService service = retrofit.create(GatewayService.class);
            Observable<SpacePollResult> observable = service.getSpacePoll(requestId, accessToken, 1);
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<SpacePollResult>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "get space poll on subscribe");
                        }

                        @Override
                        public void onNext(SpacePollResult spacePollResult) {
                            Logger.i(TAG, "get space poll on next: " + (spacePollResult == null ? "null" : spacePollResult));
                            if (callback != null) {
                                callback.onResult(spacePollResult);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                            Logger.e(TAG, "get space poll on error: " + errMsg);
                            if (callback != null) {
                                callback.onError(errMsg);
                            }
                        }

                        @Override
                        public void onComplete() {
                            Logger.d(TAG, "get space poll on complete");
                        }
                    });
        }
    }

    public void createAuthToken(String requestId, CreateTokenInfo createTokenInfo, ICreateAuthTokenCallback callback) {
        if (retrofit != null && boxDomain != null) {
            HttpUrl httpUrl = retrofit.baseUrl();
            Logger.d(TAG, "create auth token request id: " + requestId + ", base url: " + httpUrl.toString() + ", api: " + ConstantField.URL.CREATE_AUTH_TOKEN_API);
//            GatewayService service = retrofit.create(GatewayService.class);
//            Observable<CreateTokenResult> observable = service.createAuthToken(requestId, createTokenInfo);
//            observable.subscribeOn(Schedulers.trampoline())
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe(new Observer<CreateTokenResult>() {
//                        @Override
//                        public void onSubscribe(Disposable d) {
//                            Logger.d(TAG, "on subscribe");
//                        }
//
//                        @Override
//                        public void onNext(CreateTokenResult createTokenResult) {
//                            Logger.i(TAG, "create auth token on next: " + (createTokenResult == null ? "null" : createTokenResult.toString()));
//                            if (callback != null) {
//                                callback.onResult(createTokenResult);
//                            }
//                        }
//
//                        @Override
//                        public void onError(Throwable e) {
//                            String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
//                            Logger.e(TAG, "create auth token on error: " + errMsg);
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
            if (okHttpClient != null) {
                String requestUrl = (generateBaseUrl(boxDomain) + ConstantField.URL.CREATE_AUTH_TOKEN_API);
                HttpUrl requestHttpUrl = HttpUrl.parse(requestUrl);
                if (requestHttpUrl != null) {
                    String requestContent = new Gson().toJson(createTokenInfo, CreateTokenInfo.class);
                    RequestBody requestBody = RequestBody.create(requestContent, MediaType.parse("application/json; charset=utf-8"));
                    Request request = new Request.Builder()
                            .url(requestHttpUrl)
                            .addHeader("Request-Id", requestId)
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Accept", "application/json")
                            .post(requestBody)
                            .build();
                    Call call = okHttpClient.newCall(request);
                    call.enqueue(new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                            String errMsg = (e.getMessage() == null ? "" : e.getMessage());
                            Logger.e(TAG, "on error: " + errMsg);
                            if (callback != null) {
                                callback.onError(-1, errMsg);
                            }
                        }

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                            int code = response.code();
                            String message = response.message();
                            ResponseBody responseBody = response.body();
                            CreateTokenResult createTokenResult = null;
                            boolean isSuccess = false;
                            if (responseBody != null) {
                                String responseContent = null;
                                try {
                                    responseContent = responseBody.string();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                Logger.d(TAG, "create auth token content: " + responseContent);
                                if (responseContent != null) {
                                    if (code < 400) {
                                        try {
                                            createTokenResult = new Gson().fromJson(responseContent, CreateTokenResult.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        isSuccess = (createTokenResult != null);
                                    } else {
                                        RealCallResult realCallResult = null;
                                        try {
                                            realCallResult = new Gson().fromJson(responseContent, RealCallResult.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (realCallResult != null && ConstantField.KnownError.MEMBER_LIST_ERROR_CODE
                                                == DataUtil.stringCodeToInt(realCallResult.getCode())) {
                                            isSuccess = true;
                                        }
                                    }
                                }
                            }
                            if (isSuccess) {
                                if (callback != null) {
                                    callback.onResult(createTokenResult);
                                }
                            } else if (callback != null) {
                                callback.onError(code, message);
                            }
                        }
                    });
                }
            }
        }
    }

    public void refreshAuthToken(String requestId, RefreshTokenInfo refreshTokenInfo, ICreateAuthTokenCallback callback) {
        if (retrofit != null && boxDomain != null) {
            HttpUrl httpUrl = retrofit.baseUrl();
            Logger.d(TAG, "refresh auth token request id: " + requestId + ", base url: " + httpUrl.toString() + ", api: " + ConstantField.URL.REFRESH_AUTH_TOKEN_API);
            GatewayService service = retrofit.create(GatewayService.class);
            Observable<CreateTokenResult> observable = service.refreshAuthToken(requestId, refreshTokenInfo);
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<CreateTokenResult>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "on subscribe");
                        }

                        @Override
                        public void onNext(CreateTokenResult createTokenResult) {
                            Logger.i(TAG, "refresh auth token on next: " + (createTokenResult == null ? "null" : createTokenResult.toString()));
                            if (callback != null) {
                                callback.onResult(createTokenResult);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                            Logger.e(TAG, "refresh auth token on error: " + errMsg);
                            if (callback != null) {
                                callback.onError(-1, errMsg);
                            }
                        }

                        @Override
                        public void onComplete() {
                            Logger.d(TAG, "on complete");
                        }
                    });
        }
    }

    public void refreshLoginAuthToken(String requestId, String encryptedSecretKey, RefreshTokenInfo refreshTokenInfo, ICreateAuthTokenCallback callback) {
        if (retrofit != null && boxDomain != null) {
            HttpUrl httpUrl = retrofit.baseUrl();
            Logger.d(TAG, "refresh login auth token request id: " + requestId + ", base url: " + httpUrl.toString() + ", api: " + ConstantField.URL.REFRESH_BOX_KEY_AUTH_TOKEN_API);
//            GatewayService service = retrofit.create(GatewayService.class);
//            Observable<CreateTokenResult> observable = service.refreshLoginAuthToken(requestId, encryptedSecretKey, refreshTokenInfo);
//            observable.subscribeOn(Schedulers.trampoline())
//                    .observeOn(AndroidSchedulers.mainThread())
//                    .subscribe(new Observer<CreateTokenResult>() {
//                        @Override
//                        public void onSubscribe(Disposable d) {
//                            Logger.d(TAG, "on subscribe");
//                        }
//
//                        @Override
//                        public void onNext(CreateTokenResult createTokenResult) {
//                            Logger.i(TAG, "refresh login auth token on next: " + (createTokenResult == null ? "null" : createTokenResult.toString()));
//                            if (callback != null) {
//                                callback.onResult(createTokenResult);
//                            }
//                        }
//
//                        @Override
//                        public void onError(Throwable e) {
//                            String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
//                            Logger.e(TAG, "refresh login auth token on error: " + errMsg);
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
            if (okHttpClient != null) {
                String requestUrl = (generateBaseUrl(boxDomain) + ConstantField.URL.REFRESH_BOX_KEY_AUTH_TOKEN_API);
                HttpUrl requestHttpUrl = HttpUrl.parse(requestUrl);
                if (requestHttpUrl != null) {
                    httpUrl = requestHttpUrl.newBuilder()
                            .addQueryParameter("tmpEncryptedSecret", encryptedSecretKey)
                            .build();
                    String requestContent = new Gson().toJson(refreshTokenInfo, RefreshTokenInfo.class);
                    RequestBody requestBody = RequestBody.create(requestContent, MediaType.parse("application/json; charset=utf-8"));
                    Request request = new Request.Builder()
                            .url(httpUrl)
                            .addHeader("Request-Id", requestId)
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Accept", "application/json")
                            .post(requestBody)
                            .build();
                    Call call = okHttpClient.newCall(request);
                    call.enqueue(new Callback() {
                        @Override
                        public void onFailure(@NotNull Call call, @NotNull IOException e) {
                            String errMsg = (e.getMessage() == null ? "" : e.getMessage());
                            Logger.e(TAG, "on error: " + errMsg);
                            if (callback != null) {
                                callback.onError(-1, errMsg);
                            }
                        }

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                            int code = response.code();
                            String message = response.message();
                            ResponseBody responseBody = response.body();
                            CreateTokenResult createTokenResult = null;
                            boolean isSuccess = false;
                            if (responseBody != null) {
                                String responseContent = null;
                                try {
                                    responseContent = responseBody.string();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                Logger.d(TAG, "refresh login auth token content: " + responseContent);
                                if (responseContent != null) {
                                    if (code < 400) {
                                        try {
                                            createTokenResult = new Gson().fromJson(responseContent, CreateTokenResult.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        isSuccess = (createTokenResult != null);
                                    } else {
                                        RealCallResult realCallResult = null;
                                        try {
                                            realCallResult = new Gson().fromJson(responseContent, RealCallResult.class);
                                        } catch (JsonSyntaxException e) {
                                            e.printStackTrace();
                                        }
                                        if (realCallResult != null && ConstantField.KnownError.AUTHORIZATION_MEMBER_ERROR_CODE
                                                == DataUtil.stringCodeToInt(realCallResult.getCode())) {
                                            isSuccess = true;
                                        }
                                    }
                                }
                            }
                            if (isSuccess) {
                                if (callback != null) {
                                    callback.onResult(createTokenResult);
                                }
                            } else if (callback != null) {
                                callback.onError(code, message);
                            }
                        }
                    });
                }
            }
        }
    }

    public void authAutoLogin(String requestId, AuthAutoLoginRequestBody authAutoLoginRequestBody, boolean isPoll, IAuthAutoLoginCallback callback) {
        if (boxDomain != null && okHttpClient != null) {
            String requestUrl = (generateBaseUrl(boxDomain) + (isPoll ? ConstantField.URL.AUTH_AUTO_LOGIN_POLL_API : ConstantField.URL.AUTH_AUTO_LOGIN_API));
            HttpUrl httpUrl = HttpUrl.parse(requestUrl);
            if (httpUrl != null) {
                String requestContent = new Gson().toJson(authAutoLoginRequestBody, AuthAutoLoginRequestBody.class);
                RequestBody requestBody = RequestBody.create(requestContent, MediaType.parse("application/json; charset=utf-8"));
                Request request = new Request.Builder()
                        .url(httpUrl)
                        .addHeader("Request-Id", requestId)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Accept", "application/json")
                        .post(requestBody)
                        .build();
                Call call = okHttpClient.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        String errMsg = (e.getMessage() == null ? "" : e.getMessage());
                        Logger.e(TAG, "on error: " + errMsg);
                        if (callback != null) {
                            callback.onError(-1, errMsg);
                        }
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        int code = response.code();
                        String message = response.message();
                        ResponseBody responseBody = response.body();
                        String responseContent = null;
                        if (responseBody != null) {
                            try {
                                responseContent = responseBody.string();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        AuthAutoLoginResponseBody authAutoLoginResponseBody = null;
                        if (responseContent != null) {
                            try {
                                authAutoLoginResponseBody = new Gson().fromJson(responseContent, AuthAutoLoginResponseBody.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        if (authAutoLoginResponseBody != null) {
                            if (callback != null) {
                                callback.onResult(code, authAutoLoginResponseBody);
                            }
                        } else if (callback != null) {
                            callback.onError(code, message);
                        }
                    }
                });
            } else if (callback != null) {
                callback.onError(-1, "unknown");
            }
        }
    }

    public void authAutoLoginConfirm(String requestId, AuthAutoLoginConfirmRequestBody authAutoLoginConfirmRequestBody, IAuthAutoLoginConfirmCallback callback) {
        if (retrofit != null && boxDomain != null) {
            GatewayService service = retrofit.create(GatewayService.class);
            Observable<AuthAutoLoginConfirmResponseBody> observable = service.authAutoLoginConfirm(requestId, authAutoLoginConfirmRequestBody);
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<AuthAutoLoginConfirmResponseBody>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "auth auto login confirm on subscribe");
                        }

                        @Override
                        public void onNext(AuthAutoLoginConfirmResponseBody authAutoLoginConfirmResponseBody) {
                            Logger.i(TAG, "auth auto login confirm on next: " + (authAutoLoginConfirmResponseBody == null ? "null" : authAutoLoginConfirmResponseBody));
                            if (callback != null) {
                                callback.onResult(authAutoLoginConfirmResponseBody);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                            Logger.e(TAG, "auth auto login confirm on error: " + errMsg);
                            if (callback != null) {
                                callback.onError(errMsg);
                            }
                        }

                        @Override
                        public void onComplete() {
                            Logger.d(TAG, "auth auto login confirm on complete");
                        }
                    });
        }
    }

    //盒子系统or App版本升级检查
    public void boxVersionCheck(String requestId, boolean isBox, String pkgName, String currentAppVersion, String channelCode, IVersionCheckCallback callback) {
        if (retrofit != null) {
            GatewayService service = retrofit.create(GatewayService.class);
            Observable<VersionCheckResponseBody> observable = service.boxVersionCheck(requestId, pkgName, "android", currentAppVersion);
            if (!isBox) {
                //app升级查询
                observable = service.appVersionCheck(requestId, pkgName, "android", currentAppVersion, channelCode);
            }
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<VersionCheckResponseBody>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "version check on subscribe");
                        }

                        @Override
                        public void onNext(VersionCheckResponseBody versionCheckRes) {
                            Logger.d(TAG, "version check on next: " + (versionCheckRes == null ? "null" : versionCheckRes.toString()));
                            if (callback != null) {
                                callback.onResult(versionCheckRes);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                            Logger.d(TAG, "version check on error: " + errMsg);
                            if (callback != null) {
                                callback.onError(errMsg);
                            }
                        }

                        @Override
                        public void onComplete() {
                            Logger.d(TAG, "version check on complete");
                        }
                    });
        } else if (callback != null) {
            callback.onError("");
        }
    }

    //适应性升级检查
    public void compatibleVersionCheck(String requestId, String pkgName, String currentAppVersion, String channelCode, IVersionCompatibleCallback callback) {
        if (retrofit != null) {
            GatewayService service = retrofit.create(GatewayService.class);
            Observable<VersionCompatibleResponseBody> observable = service.getVersionCompatible(requestId, pkgName, "android", currentAppVersion, channelCode);
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<VersionCompatibleResponseBody>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "version check on subscribe");
                        }

                        @Override
                        public void onNext(VersionCompatibleResponseBody responseBody) {
                            Logger.d(TAG, "version check on next: " + (responseBody == null ? "null" : responseBody.toString()));
                            if (callback != null) {
                                callback.onResult(responseBody);
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                            Logger.d(TAG, "version check on error: " + errMsg);
                            if (callback != null) {
                                callback.onError(errMsg);
                            }
                        }

                        @Override
                        public void onComplete() {
                            Logger.d(TAG, "version check on complete");
                        }
                    });
        }
    }

    //获取当前盒子系统版本
    public void getCurrentBoxVersion(String requestId, ResultCallback callback) {
        if (retrofit != null) {
            GatewayService service = retrofit.create(GatewayService.class);
            Observable<CurrentBoxVersionResponseBody> observable = service.getCurrentBoxVersion(requestId);
            observable.subscribeOn(Schedulers.trampoline())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Observer<CurrentBoxVersionResponseBody>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            Logger.d(TAG, "get current box version on subscribe");
                        }

                        @Override
                        public void onNext(CurrentBoxVersionResponseBody versionCheckRes) {
                            Logger.d(TAG, "get current box version on next: " + (versionCheckRes == null ? "null" : versionCheckRes.toString()));
                            if (callback != null) {
                                if (versionCheckRes != null) {
                                    callback.onResult(true, versionCheckRes.results);
                                } else {
                                    callback.onResult(true, null);
                                }
                            }
                        }

                        @Override
                        public void onError(Throwable e) {
                            String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                            Logger.d(TAG, "get current box version on error: " + errMsg);
                            if (callback != null) {
                                callback.onResult(false, errMsg);
                            }
                        }

                        @Override
                        public void onComplete() {
                            Logger.d(TAG, "version checkLogger.d(\"zfy\", \"get current box version on complete");
                        }
                    });
        }
    }
}
