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

package xyz.eulix.space.network.interceptor;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import xyz.eulix.space.database.EulixSpaceSharePreferenceHelper;
import xyz.eulix.space.network.gateway.CreateTokenInfo;
import xyz.eulix.space.network.gateway.CreateTokenResult;
import xyz.eulix.space.network.gateway.RealCallResult;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.Logger;

/**
 * @author: chenjiawei
 * date: 2021/7/6 10:52
 */
public class EncryptJsonInterceptor implements Interceptor {
    private static final String TAG = EncryptJsonInterceptor.class.getSimpleName();
    private EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper;
    private String boxPublicKey;
    private int apiType;
    private String algorithm;
    private String provider;
    private MediaType requestMediaType;
    private MediaType responseMediaType;

    public EncryptJsonInterceptor(String boxPublicKey, int apiType, String algorithm, String provider, MediaType requestMediaType, MediaType responseMediaType) {
        this.boxPublicKey = boxPublicKey;
        this.apiType = apiType;
        this.algorithm = algorithm;
        this.provider = provider;
        this.requestMediaType = requestMediaType;
        this.responseMediaType = responseMediaType;
    }

    @NotNull
    @Override
    public Response intercept(@NotNull Chain chain) throws IOException {
        if (eulixSpaceSharePreferenceHelper == null) {
            eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance();
        }
        Request request = chain.request();
        RequestBody plainRequestBody = request.body();
        boolean isEncrypt = false;
        boolean isDecrypt = false;
        String requestBodyContent = "";
        if (plainRequestBody != null && eulixSpaceSharePreferenceHelper != null
                && eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PUBLIC_KEY)
                && eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PRIVATE_KEY)) {
            Buffer buffer = new Buffer();
            plainRequestBody.writeTo(buffer);
            String plainRequestText = buffer.readUtf8();
            buffer.close();
            if (!TextUtils.isEmpty(plainRequestText)) {
                switch (apiType) {
                    case ConstantField.ApiType.CREATE_AUTH_TOKEN:
                        CreateTokenInfo createTokenInfo = null;
                        try {
                            createTokenInfo = new Gson().fromJson(plainRequestText, CreateTokenInfo.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                        if (createTokenInfo != null) {
                            Logger.d(TAG, "create token info before: " + createTokenInfo);
                            createTokenInfo.setEncryptedAuthKey(EncryptionUtil.encrypt(algorithm
                                    , provider, createTokenInfo.getEncryptedAuthKey()
                                    , boxPublicKey, null, null));
                            createTokenInfo.setEncryptedClientUUID(EncryptionUtil.encrypt(algorithm
                                    , provider, createTokenInfo.getEncryptedClientUUID()
                                    , boxPublicKey, null, null));
                            Logger.d(TAG, "create token info encrypt: " + createTokenInfo.toString());
                            requestBodyContent = new Gson().toJson(createTokenInfo, CreateTokenInfo.class);
                            RequestBody cipherRequestBody = RequestBody.create(requestBodyContent, requestMediaType);
                            request = request.newBuilder()
                                    .post(cipherRequestBody)
                                    .build();
                            isEncrypt = true;
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        Response response = chain.proceed(request);
        int responseCode = response.code();
        ResponseBody cipherResponseBody = response.body();
        if (isEncrypt && eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PRIVATE_KEY)) {
            if (cipherResponseBody != null) {
                String cipherResponseText = null;
                try {
                    cipherResponseText = cipherResponseBody.string();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Logger.d(TAG, "create token response body: " + cipherResponseText + ", code: " + responseCode);
                String plainResponseText = cipherResponseText;
                if (!TextUtils.isEmpty(cipherResponseText)) {
                    switch (apiType) {
                        case ConstantField.ApiType.CREATE_AUTH_TOKEN:
                            if (responseCode < 400) {
                                CreateTokenResult createTokenResult = null;
                                try {
                                    createTokenResult = new Gson().fromJson(cipherResponseText, CreateTokenResult.class);
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                                if (createTokenResult != null) {
                                    Logger.d(TAG, "create token result before: " + createTokenResult);
                                    if (eulixSpaceSharePreferenceHelper != null) {
                                        String clientPrivateKey = eulixSpaceSharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PRIVATE_KEY);

                                        if (clientPrivateKey != null) {
                                            createTokenResult.setAccessToken(createTokenResult.getAccessToken());
                                            createTokenResult.setEncryptedSecret(EncryptionUtil.decrypt(algorithm, provider
                                                    , createTokenResult.getEncryptedSecret(), clientPrivateKey, null, null));
                                            createTokenResult.setExpiresAt(createTokenResult.getExpiresAt());
                                            createTokenResult.setRequestId(createTokenResult.getRequestId());
                                            plainResponseText = new Gson().toJson(createTokenResult, CreateTokenResult.class);
                                            Logger.d(TAG, "create token result decrypt: " + createTokenResult);
                                            isDecrypt = true;
                                        }
                                    }
                                }
                            } else {
                                RealCallResult realCallResult = null;
                                try {
                                    realCallResult = new Gson().fromJson(cipherResponseText, RealCallResult.class);
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                                if (realCallResult != null) {
                                    plainResponseText = new Gson().toJson(realCallResult, RealCallResult.class);
                                    isDecrypt = true;
                                }
                            }
                            break;
                        default:
                            break;
                    }
                }
                cipherResponseBody.close();
                if (isDecrypt) {
                    ResponseBody plainResponseBody = ResponseBody.create(plainResponseText, responseMediaType);
                    response = response.newBuilder()
                            .body(plainResponseBody)
                            .build();
                }
            }
        }
        response.close();
        return response;
    }
}
