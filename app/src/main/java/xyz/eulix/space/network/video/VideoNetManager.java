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

package xyz.eulix.space.network.video;

import android.content.Context;
import android.text.TextUtils;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.interfaces.ResultCallbackObj;
import xyz.eulix.space.network.files.BaseResponseBody;
import xyz.eulix.space.network.interceptor.EulixGatewayInterceptor;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.Urls;
import xyz.eulix.space.util.network.OkHttpUtil;

/**
 * Author:      Zhu Fuyu
 * Description: 在线播放接口处理
 * History:     2023/1/5
 */
public class VideoNetManager {

    private static OkHttpClient generateOkHttpClient(String boxDomain, UUID requestId, String requestType, String accessToken, String secret, String transformation, String ivParams, String apiVersion) {
        return OkHttpUtil.generateOkHttpClient(true).newBuilder()
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

    //查询视频是否支持在线播放
    public static void checkVideoSupport(String uuid, GatewayCommunicationBase gatewayCommunicationBase, ResultCallbackObj callback) {
        if (gatewayCommunicationBase == null) {
            if (callback != null) {
                callback.onError("gatewayCommunicationBase is null");
            }
            return;
        }
        UUID requestId = UUID.randomUUID();
        String baseUrl = generateBaseUrl(Urls.getBaseUrl());
        OkHttpClient okHttpClient = generateOkHttpClient(baseUrl, requestId, ConstantField.ServiceFunction.MEDIA_VOD_CHECK,
                gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getTransformation(), gatewayCommunicationBase.getIvParams(), ConstantField.BoxVersionName.VERSION_0_1_0);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        VideoNetService service = retrofit.create(VideoNetService.class);
        Observable<BaseResponseBody> observable = service.videoCheck(uuid);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<BaseResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d("zfy", "check video support on subscribe");
                    }

                    @Override
                    public void onNext(BaseResponseBody responseBody) {
                        Logger.d("zfy", "check video support on next: " + (responseBody == null ? "null" : responseBody.toString()));
                        if (callback != null) {
                            if (responseBody != null && responseBody.getCodeInt() == 200) {
                                callback.onResult(true, null);
                            } else {
                                callback.onResult(false, responseBody != null ? responseBody.getMessage() : null);
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                        Logger.e("zfy", "check video support on error: " + errMsg);
                        if (callback != null) {
                            callback.onError(errMsg);
                        }
                        if (e != null) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onComplete() {
                        Logger.d("zfy", "check video support on complete");
                    }
                });
    }


    //下载文档预览图
    public static void downloadM3u8(Context context, String uuid, GatewayCommunicationBase gatewayCommunicationBase, ResultCallbackObj callback) {
        if (context == null) {
            return;
        }
        if (gatewayCommunicationBase == null) {
            if (callback != null) {
                callback.onError("gatewayCommunicationBase is null");
            }
            return;
        }
        final String secret = gatewayCommunicationBase.getSecretKey();
        final String ivParams = gatewayCommunicationBase.getIvParams();
        UUID requestId = UUID.randomUUID();
        OkHttpClient okHttpClient = OkHttpUtil.generateOkHttpClient(true).newBuilder()
                .addInterceptor(new EulixGatewayInterceptor(generateBaseUrl(Urls.getBaseUrl()), requestId, ConstantField.ServiceFunction.MEDIA_VOD_M3U8_DOWNLOAD
                        , ConstantField.Algorithm.Transformation.AES_CBC_PKCS5, null, ivParams
                        , gatewayCommunicationBase.getAccessToken(), secret, MediaType.parse("application/json; charset=utf-8")
                        , MediaType.parse("*/*"), ConstantField.BoxVersionName.VERSION_0_1_0))
                .build();
        HttpUrl httpParseUrl = HttpUrl.parse(generateBaseUrl(Urls.getBaseUrl()) + ConstantField.URL.DOWNLOAD_GATEWAY_API);
        if (httpParseUrl != null) {
            HttpUrl httpUrl = httpParseUrl.newBuilder()
                    .addQueryParameter("uuid", uuid)
                    .build();
            Logger.d("zfy", "download file preview url: " + httpUrl);
            Request request = new Request.Builder()
                    .url(httpUrl)
                    .get()
                    .build();

            //下载文件路径
            String nFilepath = context.getExternalCacheDir().getAbsolutePath() + ConstantField.FILE_CACHE_PATH;
            File folder = new File(nFilepath);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            final String nFilePathFinal = nFilepath;

            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Logger.e("zfy", "get file preview on failure, e: " + e.getMessage());
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    Logger.i("zfy", "on response " + response.code());
                    if (!response.isSuccessful()) {
                        if (callback != null) {
                            callback.onResult(false, null);
                        }
                        return;
                    }
                    Headers headers = response.headers();
                    String contentType = headers.get("content-type");
                    Logger.d("zfy", "content-type = " + contentType);
                    if (!TextUtils.isEmpty(contentType) && (contentType.contains("stream") || contentType.contains("zip"))) {
                        //文件大小
                        String fileSizeStr = headers.get("file-size");
                        Logger.d("zfy", "file-size=" + fileSizeStr);
                        long fileSize = Long.parseLong(fileSizeStr);
                        //文件名称等(inline; filename="header_chosen.jpg"; filename*=UTF-8''header_chosen.jpg)
                        String contentDisposition = headers.get("content-disposition");
                        Logger.d("zfy", "content-disposition=" + contentDisposition);
                        String suffix = "zip";
                        Logger.d("zfy", "filepath: " + nFilePathFinal);
                        FileUtil.mkFile(nFilePathFinal);
                        String saveFileName = "m3u8_" + uuid + "." + suffix;
                        Logger.d("zfy", "saveFileName = " + saveFileName);
                        File file = new File(nFilePathFinal, saveFileName);

                        if (file.exists()) {
                            boolean result = file.delete();
                            Logger.d("zfy", "delete file result =" + result);
                        }

                        InputStream inputStream = response.body().byteStream();

                        //解密并保存
                        File decryptFile = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5, null, inputStream,
                                secret, StandardCharsets.UTF_8, ivParams, nFilePathFinal, saveFileName, fileSize, null);
                        if (decryptFile != null) {
                            Logger.d("zfy", "get file preview success." + decryptFile.getAbsolutePath());
                            //回调
                            if (callback != null) {
                                callback.onResult(true, decryptFile.getAbsolutePath());
                            }
                        } else if (callback != null) {
                            callback.onResult(false, null);
                        }
                    } else {
                        //没有文件流，下载文件失败
                        Logger.d("zfy", "no stream:" + response.body().toString());
                        if (callback != null) {
                            callback.onResult(false, null);
                        }
                    }
                }
            });
        }
    }

}
