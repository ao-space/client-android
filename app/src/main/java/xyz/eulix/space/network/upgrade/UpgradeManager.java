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

package xyz.eulix.space.network.upgrade;

import android.content.Context;
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
import xyz.eulix.space.bean.DeviceVersionInfoResponseBody;
import xyz.eulix.space.interfaces.ResultCallbackObj;
import xyz.eulix.space.interfaces.ResultWithNullCallback;
import xyz.eulix.space.network.files.BaseResponseBody;
import xyz.eulix.space.network.interceptor.EulixGatewayInterceptor;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.network.OkHttpUtil;

/**
 * Author:      Zhu Fuyu
 * Description: 盒子系统升级管理类
 * History:     2021/11/8
 */
public class UpgradeManager {

    private static OkHttpClient generateOkHttpClient(String boxDomain, UUID requestId, String requestType, String accessToken, String secret, String transformation, String ivParams, String apiVersion) {
        return OkHttpUtil.generateOkHttpClient(true).newBuilder()
                .connectTimeout(2, TimeUnit.MINUTES)
                .readTimeout(2, TimeUnit.MINUTES)
                .writeTimeout(2, TimeUnit.MINUTES)
                .addInterceptor(new EulixGatewayInterceptor(boxDomain, requestId, requestType
                        , transformation, null, ivParams, accessToken, secret
                        , MediaType.parse("application/json; charset=utf-8")
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

    //获取系统自动升级配置
    public static void getSystemAutoUpgradeConfig(String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, IGetUpgradeConfigCallback callback) {
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, UUID.randomUUID(), ConstantField.ServiceFunction.GET_UPGRADE_CONFIG, accessToken, secret, transformation, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);

        UpgradeService service = retrofit.create(UpgradeService.class);
        Observable<UpgradeConfigResponseBody> observable = service.getUpgradeConfig(UUID.randomUUID().toString());
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<UpgradeConfigResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d("zfy", "get upgrade config on subscribe");
                    }

                    @Override
                    public void onNext(UpgradeConfigResponseBody baseRsp) {
                        Logger.d("zfy", "get upgrade config on next: " + (baseRsp == null ? "null" : baseRsp.toString()));
                        if (callback != null) {
                            if (baseRsp != null && baseRsp.getCodeInt() == 0) {
                                Logger.d("zfy", "baseRsp=" + baseRsp.toString());
                                callback.onResult(baseRsp.autoDownload, baseRsp.autoInstall);
                            } else {
                                callback.onError(baseRsp != null ? baseRsp.getMessage() : "");
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                        Logger.d("zfy", "get upgrade config on error: " + errMsg);
                        if (callback != null) {
                            callback.onError(errMsg);
                        }
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        Logger.d("zfy", "get upgrade config on complete");
                    }
                });
    }

    //设置系统自动升级配置
    public static void setSystemAutoUpgradeConfig(boolean autoDownload, boolean autoInstall, String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, ResultWithNullCallback callback) {
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, UUID.randomUUID(), ConstantField.ServiceFunction.SET_UPGRADE_CONFIG, accessToken, secret, transformation, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);

        SetUpgradeConfigReq setUpgradeConfigReq = new SetUpgradeConfigReq();
        setUpgradeConfigReq.setAutoDownload(autoDownload);
        setUpgradeConfigReq.setAutoInstall(autoInstall);

        UpgradeService service = retrofit.create(UpgradeService.class);
        Observable<BaseResponseBody> observable = service.setUpgradeConfig(setUpgradeConfigReq);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<BaseResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d("zfy", "set upgrade config on subscribe");
                    }

                    @Override
                    public void onNext(BaseResponseBody baseRsp) {
                        Logger.d("zfy", "set upgrade config on next: " + (baseRsp == null ? "null" : baseRsp.toString()));
                        if (callback != null) {
                            callback.onResult(true, "");
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                        Logger.d("zfy", "set upgrade config on error: " + errMsg);
                        if (callback != null) {
                            callback.onResult(null, errMsg);
                        }
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        Logger.d("zfy", "set upgrade config on complete");
                    }
                });
    }

    //查询系统升级状态
    public static void checkUpgradeStatus(String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, ICheckUpgradeStatusCallback callback) {
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, UUID.randomUUID(), ConstantField.ServiceFunction.UPGRADE_STATUS, accessToken, secret, transformation, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);

        UpgradeService service = retrofit.create(UpgradeService.class);
        Observable<UpgradeStatusResponseBody> observable = service.checkUpgradeStatus(UUID.randomUUID().toString());
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<UpgradeStatusResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d("zfy", "get upgrade status on subscribe");
                    }

                    @Override
                    public void onNext(UpgradeStatusResponseBody baseRsp) {
                        Logger.d("zfy", "get upgrade status on next: " + (baseRsp == null ? "null" : baseRsp.toString()));
                        if (callback != null) {
                            if (baseRsp != null && baseRsp.getCodeInt() == 0) {
                                Logger.d("zfy", "baseRsp=" + baseRsp.toString());
//                                callback.onResult(baseRsp.autoInstall, baseRsp.autoDownload);
                                callback.onResult(baseRsp);
                            } else {
                                callback.onError("");
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                        Logger.d("zfy", "get upgrade status on error: " + errMsg);
                        if (callback != null) {
                            callback.onError(errMsg);
                        }
                        e.printStackTrace();
                    }

                    @Override
                    public void onComplete() {
                        Logger.d("zfy", "get upgrade status on complete");
                    }
                });
    }

    /**
     * 开始升级
     *
     * @param isPull         true:拉取镜像；false:升级
     * @param versionId
     * @param boxDomain
     * @param accessToken
     * @param secret
     * @param transformation
     * @param ivParams
     * @param apiVersion
     * @param callback
     */
    public static void startUpgrade(Context context, boolean isPull, String versionId, String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, ResultWithNullCallback callback) {
        String requestType = ConstantField.ServiceFunction.UPGRADE_START_UPGRADE;
        if (isPull) {
            requestType = ConstantField.ServiceFunction.UPGRADE_START_PULL;
        }
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, UUID.randomUUID(), requestType, accessToken, secret, transformation, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        UpgradeService service = retrofit.create(UpgradeService.class);
        StartUpgradeReq startUpgradeReq = new StartUpgradeReq();
        startUpgradeReq.setVersionId(versionId);
        startUpgradeReq.setAnew(true);
        Observable<UpgradeStatusResponseBody> observable = service.startUpgrade(startUpgradeReq);
        if (isPull) {
            observable = service.startPull(startUpgradeReq);
        }
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<UpgradeStatusResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d("zfy", "start upgrade on subscribe");
                    }

                    @Override
                    public void onNext(UpgradeStatusResponseBody responseBody) {
                        Logger.i("zfy", "start upgrade on next: " + (responseBody == null ? "null" : responseBody.toString()));
                        if (callback != null) {
                            if (responseBody != null && responseBody.getCodeInt() == 0) {
                                callback.onResult(true, null);
                            } else if (responseBody.getCodeInt() == 208) {
                                //已下载，发起安装
                                UpgradeUtils.startUpgrade(context, false, versionId, callback);
                            } else if (responseBody != null && responseBody.getCodeInt() == 400) {
                                //无需升级
                                responseBody.status = ConstantField.UpgradeStatus.STATUS_PULLED;
                                callback.onResult(true, null);
                            } else {
                                callback.onResult(false, responseBody != null ? responseBody.getMessage() : "");
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                        Logger.e("zfy", "start upgrade on error: " + errMsg);
                        e.printStackTrace();
                        if (callback != null) {
                            callback.onResult(null, errMsg);
                        }
                    }

                    @Override
                    public void onComplete() {
                        Logger.d("zfy", "start upgrade on complete");
                    }
                });
    }

    //获取傲空间设备信息
    public static void getDeviceVersionInfo(Context context, String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, ResultCallbackObj callback) {

        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, UUID.randomUUID(), ConstantField.ServiceFunction.DEVICE_INFO, accessToken, secret, transformation, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        UpgradeService service = retrofit.create(UpgradeService.class);
        Observable<DeviceVersionInfoResponseBody> observable = service.getDeviceInfo(UUID.randomUUID().toString());
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<DeviceVersionInfoResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d("zfy", "get device version info on subscribe");
                    }

                    @Override
                    public void onNext(DeviceVersionInfoResponseBody responseBody) {
                        Logger.i("zfy", "get device version info on next: " + (responseBody == null ? "null" : responseBody.toString()));
                        if (callback != null) {
                            if (responseBody != null && responseBody.getCodeInt() == 200) {
                                callback.onResult(true, responseBody.results);
                            } else {
                                callback.onResult(false, "");
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                        Logger.e("zfy", "get device version info on error: " + errMsg);
                        e.printStackTrace();
                        if (callback != null) {
                            callback.onResult(false, errMsg);
                        }
                    }

                    @Override
                    public void onComplete() {
                        Logger.d("zfy", "get device version info on complete");
                    }
                });
    }

}
