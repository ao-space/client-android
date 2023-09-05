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

package xyz.eulix.space.network.disk;

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
import xyz.eulix.space.network.EulixBaseResponse;
import xyz.eulix.space.network.IEulixBaseResponseCallback;
import xyz.eulix.space.network.interceptor.EulixGatewayInterceptor;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.network.OkHttpUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/11/8 15:41
 */
public class DiskManager {
    private static final String TAG = DiskManager.class.getSimpleName();

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

    public static void getDiskManagementList(String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, IDiskManageListCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.DISK_MANAGEMENT_LIST, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        DiskService diskService = retrofit.create(DiskService.class);
        Observable<DiskManageListResponse> observable = diskService.getDiskManagementList(uuid.toString());
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<DiskManageListResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(DiskManageListResponse diskManageListResponse) {
                        Logger.i(TAG, "on next: " + (diskManageListResponse == null ? "null" : diskManageListResponse.toString()));
                        if (callback != null) {
                            callback.onResponse(diskManageListResponse);
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

    public static void eulixSystemShutdown(String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, IEulixBaseResponseCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.SYSTEM_SHUTDOWN, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        DiskService diskService = retrofit.create(DiskService.class);
        Observable<EulixBaseResponse> observable = diskService.eulixSystemShutdown(uuid.toString());
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

    public static void getDiskManagementRaidInfo(String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, IRaidInfoCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.DISK_MANAGEMENT_RAID_INFO, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        DiskService diskService = retrofit.create(DiskService.class);
        Observable<RaidInfoResponse> observable = diskService.getDiskManagementRaidInfo(uuid.toString());
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<RaidInfoResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(RaidInfoResponse raidInfoResponse) {
                        Logger.i(TAG, "on next: " + (raidInfoResponse == null ? "null" : raidInfoResponse.toString()));
                        if (callback != null) {
                            callback.onResponse(raidInfoResponse);
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

    public static void diskExpand(DiskExpandRequest request, String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, IEulixBaseResponseCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.DISK_MANAGEMENT_EXPAND, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        DiskService diskService = retrofit.create(DiskService.class);
        Observable<EulixBaseResponse> observable = diskService.diskExpand(uuid.toString(), request);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<EulixBaseResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(EulixBaseResponse eulixBaseResponse) {
                        Logger.i(TAG, "on next: " + (eulixBaseResponse == null ? "null" : eulixBaseResponse.toString()));
                        if (callback != null) {
                            callback.onResult(eulixBaseResponse);
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

    public static void getDiskExpandProgress(String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, IDiskExpandProgressCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.DISK_MANAGEMENT_EXPAND_PROGRESS, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        DiskService diskService = retrofit.create(DiskService.class);
        Observable<DiskExpandProgressResponse> observable = diskService.getDiskExpandProgress(uuid.toString());
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<DiskExpandProgressResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(DiskExpandProgressResponse diskExpandProgressResponse) {
                        Logger.i(TAG, "on next: " + (diskExpandProgressResponse == null ? "null" : diskExpandProgressResponse.toString()));
                        if (callback != null) {
                            callback.onResponse(diskExpandProgressResponse);
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
