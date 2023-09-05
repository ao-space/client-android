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
import xyz.eulix.space.bean.bind.WpwdInfo;
import xyz.eulix.space.network.interceptor.EulixGatewayInterceptor;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DebugUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.network.OkHttpUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/11/18 10:50
 */
public class DeviceManager {
    private static final String TAG = DeviceManager.class.getSimpleName();

    private static OkHttpClient generateOkHttpClient(String boxDomain, UUID requestId, String requestType, String accessToken, String secret, String transformation, String ivParams, String apiVersion) {
        return OkHttpUtil.generateOkHttpClient(true).newBuilder()
                .addInterceptor(new EulixGatewayInterceptor(boxDomain, requestId, requestType
                        , transformation, null, ivParams
                        , accessToken, secret, MediaType.parse("application/json; charset=utf-8")
                        , MediaType.parse("application/json; charset=utf-8"), apiVersion))
                .build();
    }

    private static OkHttpClient generateOkHttpClient(String boxDomain, UUID requestId, String requestType, String accessToken, String secret, String transformation, String ivParams, String apiVersion, int minuteTimeout) {
        return OkHttpUtil.generateOkHttpClient(true).newBuilder()
                .connectTimeout(Math.max(minuteTimeout, 1), TimeUnit.MINUTES)
                .readTimeout(Math.max(minuteTimeout, 1), TimeUnit.MINUTES)
                .writeTimeout(Math.max(minuteTimeout, 1), TimeUnit.MINUTES)
                .addInterceptor(new EulixGatewayInterceptor(boxDomain, requestId, requestType
                        , transformation, null, ivParams
                        , accessToken, secret, MediaType.parse("application/json; charset=utf-8")
                        , MediaType.parse("application/json; charset=utf-8"), apiVersion))
                .build();
    }

    private static OkHttpClient generateMediumTimeoutOkHttpClient() {
        return OkHttpUtil.generateOkHttpClient(false).newBuilder()
                .connectTimeout(2, TimeUnit.MINUTES)
                .readTimeout(2, TimeUnit.MINUTES)
                .writeTimeout(2, TimeUnit.MINUTES)
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

    private static Retrofit generateRetrofit(String baseUrl) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
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

    public static void getLocalIps(String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, ILocalIpInfoCallback callback) {
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, UUID.randomUUID(), ConstantField.ServiceFunction.LOCAL_IPS, accessToken, secret, transformation, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        DeviceService service = retrofit.create(DeviceService.class);
        Observable<LocalIpInfo> observable = service.getDeviceLocalIps();
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<LocalIpInfo>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d("zfy", "get local ip on subscribe");
                    }

                    @Override
                    public void onNext(LocalIpInfo localIpInfo) {
                        Logger.i("zfy", "on next: " + (localIpInfo == null ? "null" : localIpInfo.toString()));
                        if (callback != null) {
                            callback.onResult(localIpInfo);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                        Logger.e("zfy", "on error: " + errMsg);
                        if (callback != null) {
                            callback.onError(errMsg);
                        }
                    }

                    @Override
                    public void onComplete() {
                        Logger.d("zfy", "get local ip on complete");
                    }
                });
    }

    public static void getWifiList(String boxDomain, IScanWifiListCallback callback) {
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, generateMediumTimeoutOkHttpClient());
        DeviceService service = retrofit.create(DeviceService.class);
        Observable<ScanWifiList> observable = service.getDeviceWifiList();
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ScanWifiList>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "get wifi list on subscribe");
                    }

                    @Override
                    public void onNext(ScanWifiList scanWifiList) {
                        Logger.i(TAG, "on next: " + (scanWifiList == null ? "null" : scanWifiList.toString()));
                        if (callback != null) {
                            callback.onResult(scanWifiList);
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
                        Logger.d(TAG, "get wifi list on complete");
                    }
                });
    }

    public static void getWifiList(String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, IScanWifiListCallback callback) {
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, UUID.randomUUID(), ConstantField.ServiceFunction.NET_CONFIG, accessToken, secret, transformation, ivParams, apiVersion, 2);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        DeviceService service = retrofit.create(DeviceService.class);
        Observable<ScanWifiList> observable = service.getDeviceWifiList();
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ScanWifiList>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "get wifi list on subscribe");
                    }

                    @Override
                    public void onNext(ScanWifiList scanWifiList) {
                        Logger.i(TAG, "on next: " + (scanWifiList == null ? "null" : scanWifiList.toString()));
                        if (callback != null) {
                            callback.onResult(scanWifiList);
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
                        Logger.d(TAG, "get wifi list on complete");
                    }
                });
    }

    public static void getRealWifiList(String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, IScanRealWifiListCallback callback) {
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, UUID.randomUUID(), ConstantField.ServiceFunction.NET_CONFIG, accessToken, secret, transformation, ivParams, apiVersion, 2);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        DeviceService service = retrofit.create(DeviceService.class);
        Observable<ScanRealWifiList> observable = service.getDeviceRealWifiList();
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<ScanRealWifiList>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "get wifi list on subscribe");
                    }

                    @Override
                    public void onNext(ScanRealWifiList scanWifiList) {
                        Logger.i(TAG, "on next: " + (scanWifiList == null ? "null" : scanWifiList.toString()));
                        if (callback != null) {
                            callback.onResult(scanWifiList);
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
                        Logger.d(TAG, "get wifi list on complete");
                    }
                });
    }

    public static void setWifi(ConnectWifiReq connectWifiReq, String boxDomain, INetworkConfigCallback callback) {
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, generateMediumTimeoutOkHttpClient());
        DeviceService service = retrofit.create(DeviceService.class);
        Observable<NetworkConfigResponse> observable = service.setDeviceWifi(connectWifiReq);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<NetworkConfigResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "set wifi on subscribe");
                    }

                    @Override
                    public void onNext(NetworkConfigResponse networkConfigResponse) {
                        Logger.i(TAG, "on next: " + (networkConfigResponse == null ? "null" : networkConfigResponse.toString()));
                        if (callback != null) {
                            callback.onResult(networkConfigResponse);
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
                        Logger.d(TAG, "set wifi on complete");
                    }
                });
    }

    public static void setWifi(WpwdInfo wpwdInfo, String boxDomain, INetworkConfigCallback callback) {
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl);
        DeviceService service = retrofit.create(DeviceService.class);
        Observable<NetworkConfigResponse> observable = service.setDeviceWifi(wpwdInfo);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<NetworkConfigResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "set wifi on subscribe");
                    }

                    @Override
                    public void onNext(NetworkConfigResponse networkConfigResponse) {
                        Logger.i(TAG, "on next: " + (networkConfigResponse == null ? "null" : networkConfigResponse.toString()));
                        if (callback != null) {
                            callback.onResult(networkConfigResponse);
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
                        Logger.d(TAG, "set wifi on complete");
                    }
                });
    }

    public static void setWifi(ConnectWifiReq connectWifiReq, String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, INetworkConfigCallback callback) {
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, UUID.randomUUID(), ConstantField.ServiceFunction.NET_CONFIG_SETTING, accessToken, secret, transformation, ivParams, apiVersion, 2);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        DeviceService service = retrofit.create(DeviceService.class);
        Observable<NetworkConfigResponse> observable = service.setDeviceWifi(connectWifiReq);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<NetworkConfigResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "set wifi on subscribe");
                    }

                    @Override
                    public void onNext(NetworkConfigResponse networkConfigResponse) {
                        Logger.i(TAG, "on next: " + (networkConfigResponse == null ? "null" : networkConfigResponse.toString()));
                        if (callback != null) {
                            callback.onResult(networkConfigResponse);
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
                        Logger.d(TAG, "set wifi on complete");
                    }
                });
    }

    public static void setRealWifi(ConnectWifiReq connectWifiReq, String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, INetworkConfigRealCallback callback) {
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, UUID.randomUUID(), ConstantField.ServiceFunction.NET_CONFIG_SETTING, accessToken, secret, transformation, ivParams, apiVersion, 2);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        DeviceService service = retrofit.create(DeviceService.class);
        Observable<NetworkConfigRealResponse> observable = service.setDeviceRealWifi(connectWifiReq);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<NetworkConfigRealResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "set wifi on subscribe");
                    }

                    @Override
                    public void onNext(NetworkConfigRealResponse networkConfigResponse) {
                        Logger.i(TAG, "on next: " + (networkConfigResponse == null ? "null" : networkConfigResponse.toString()));
                        if (callback != null) {
                            callback.onResult(networkConfigResponse);
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
                        Logger.d(TAG, "set wifi on complete");
                    }
                });
    }

//    public static void getNetwork(String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, INetworkInfoCallback callback) {
//        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, UUID.randomUUID(), ConstantField.ServiceFunction.NETWORK, accessToken, secret, transformation, ivParams, apiVersion);
//        String baseUrl = generateBaseUrl(boxDomain);
//        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
//        DeviceService service = retrofit.create(DeviceService.class);
//        Observable<NetworkInfo> observable = service.getDeviceNetwork();
//        observable.subscribeOn(Schedulers.trampoline())
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Observer<NetworkInfo>() {
//                    @Override
//                    public void onSubscribe(Disposable d) {
//                        Logger.d(TAG, "get network on subscribe");
//                    }
//
//                    @Override
//                    public void onNext(NetworkInfo networkInfo) {
//                        Logger.i(TAG, "on next: " + (networkInfo == null ? "null" : networkInfo.toString()));
//                        if (callback != null) {
//                            callback.onResult(networkInfo);
//                        }
//                    }
//
//                    @Override
//                    public void onError(Throwable e) {
//                        String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
//                        Logger.e(TAG, "on error: " + errMsg);
//                        if (callback != null) {
//                            callback.onError(errMsg);
//                        }
//                    }
//
//                    @Override
//                    public void onComplete() {
//                        Logger.d(TAG, "get network on complete");
//                    }
//                });
//    }
}
