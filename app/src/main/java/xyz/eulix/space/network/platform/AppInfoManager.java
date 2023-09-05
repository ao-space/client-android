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

package xyz.eulix.space.network.platform;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import xyz.eulix.space.util.DebugUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.network.OkHttpUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/9/15 17:44
 */
public class AppInfoManager {
    private static final String TAG = AppInfoManager.class.getSimpleName();
    private static Retrofit retrofit;
    private static Retrofit officialRetrofit;

    static {
        if (retrofit == null) {
            OkHttpClient client = OkHttpUtil.generateOkHttpClient(false).newBuilder().
                    connectTimeout(60, TimeUnit.SECONDS).
                    readTimeout(60, TimeUnit.SECONDS).
                    writeTimeout(60, TimeUnit.SECONDS).build();
            retrofit = new Retrofit.Builder()
                    .baseUrl(DebugUtil.getEnvironmentServices())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
            officialRetrofit = new Retrofit.Builder()
                    .baseUrl(DebugUtil.getOfficialEnvironmentServices())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build();
        }
    }

    public static void generateRetrofit() {
        OkHttpClient client = OkHttpUtil.generateOkHttpClient(false).newBuilder().
                connectTimeout(60, TimeUnit.SECONDS).
                readTimeout(60, TimeUnit.SECONDS).
                writeTimeout(60, TimeUnit.SECONDS).build();
        retrofit = new Retrofit.Builder()
                .baseUrl(DebugUtil.getEnvironmentServices())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(client)
                .build();
    }


    private AppInfoManager() {
        throw new AssertionError("not allow to be instantiation!");
    }

    public static void downloadApk(String appName, String downloadUrl, String savePath, IAppDownloadCallback callback) {
        if (downloadUrl != null) {
            OkHttpClient okHttpClient = OkHttpUtil.generateOkHttpClient(false);
            HttpUrl httpUrl = HttpUrl.parse(downloadUrl);
            if (httpUrl != null) {
                Request request = new Request.Builder()
                        .url(httpUrl)
                        .get()
                        .build();
                Call call = okHttpClient.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        Logger.e(TAG, "download apk on failure: " + e.getMessage());
                        if (callback != null) {
                            callback.onError(e.getMessage());
                        }
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) {
                        Logger.i(TAG, "download apk on response");
                        ResponseBody responseBody = response.body();
                        FileUtil.mkFile(savePath);
                        File file = new File(savePath, appName);
                        if (file.exists() && file.delete()) {
                            Logger.d(TAG, "delete current file");
                        }
                        if (responseBody != null) {
                            int len = 0;
                            byte[] buffer = new byte[2048];
                            try (InputStream inputStream = responseBody.byteStream();
                                 FileOutputStream fileOutputStream = new FileOutputStream(file)) {
                                while ((len = inputStream.read(buffer)) != -1) {
                                    fileOutputStream.write(buffer, 0, len);
                                }
                                fileOutputStream.flush();
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException ee) {
                                ee.printStackTrace();
                            } catch (Exception eee) {
                                eee.printStackTrace();
                            }
                        }
                        if (callback != null) {
                            callback.onSuccess(file.getAbsolutePath());
                        }
                    }
                });
            }
        }
    }

}
