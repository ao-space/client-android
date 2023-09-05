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

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DebugUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.network.OkHttpUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/8/18 14:15
 */
public class PKeyManager {
    private static final String TAG = PKeyManager.class.getSimpleName();
    private static Retrofit retrofit;

    static {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(DebugUtil.getEnvironmentServices())
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
    }

    public static void generateRetrofit() {
        retrofit = new Retrofit.Builder()
                .baseUrl(DebugUtil.getEnvironmentServices())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }

    public static void sendBoxInfo(String requestId, PKeyBoxInfo pKeyBoxInfo, IPKeyBoxInfoCallback callback) {
        OkHttpClient okHttpClient = OkHttpUtil.generateOkHttpClient(false);
        HttpUrl httpParseUrl = HttpUrl.parse((DebugUtil.getEnvironmentServices() + ConstantField.URL.AUTH_PLATFORM_KEY_BOX_INFO_API));
        if (httpParseUrl != null) {
            HttpUrl httpUrl = httpParseUrl.newBuilder()
                    .build();
            String requestBody = new Gson().toJson(pKeyBoxInfo, PKeyBoxInfo.class);
            Request request = new Request.Builder()
                    .url(httpUrl)
                    .header("Request-Id", requestId)
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json; charset=utf-8")))
                    .build();
            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Logger.e(TAG, "on failure, e: " + e.getMessage());
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    int code = response.code();
                    String message = response.message();
                    Logger.i(TAG, "on response: " + code + ", message: " + message);
                    if (callback != null) {
                        callback.onResult(code);
                    }
                }
            });
        }
    }

    public static void sendBoxInfoV2(String platformKey, String requestId, PKeyBoxInfoV2 pKeyBoxInfo, IPKeyBoxInfoCallback callback) {
        OkHttpClient okHttpClient = OkHttpUtil.generateOkHttpClient(false);
        HttpUrl httpParseUrl = HttpUrl.parse((DebugUtil.getOfficialEnvironmentServices() + ConstantField.URL.AUTH_PLATFORM_KEY_BOX_INFO_API_V2_PREFIX
                + StringUtil.nullToEmpty(platformKey) + ConstantField.URL.AUTH_PLATFORM_KEY_BOX_INFO_API_V2_SUFFIX));
        if (httpParseUrl != null) {
            HttpUrl httpUrl = httpParseUrl.newBuilder()
                    .build();
            String requestBody = new Gson().toJson(pKeyBoxInfo, PKeyBoxInfoV2.class);
            Request request = new Request.Builder()
                    .url(httpUrl)
                    .header("Request-Id", requestId)
                    .post(RequestBody.create(requestBody, MediaType.parse("application/json; charset=utf-8")))
                    .build();
            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Logger.e(TAG, "on failure, e: " + e.getMessage());
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    int code = response.code();
                    String message = response.message();
                    Logger.i(TAG, "on response: " + code + ", message: " + message);
                    if (callback != null) {
                        callback.onResult(code);
                    }
                }
            });
        }
    }
}
