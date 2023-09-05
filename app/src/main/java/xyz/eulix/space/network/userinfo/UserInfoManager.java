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

package xyz.eulix.space.network.userinfo;

import android.content.Context;
import android.os.Environment;
import android.os.Handler;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

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
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.event.UserInfoEvent;
import xyz.eulix.space.network.files.UploadResponseBodyResult;
import xyz.eulix.space.network.gateway.CallRequest;
import xyz.eulix.space.network.gateway.CreateMemberTokenInfo;
import xyz.eulix.space.network.gateway.CreateTokenInfo;
import xyz.eulix.space.network.gateway.CreateTokenResult;
import xyz.eulix.space.network.gateway.ICreateAuthTokenCallback;
import xyz.eulix.space.network.gateway.RealCallResult;
import xyz.eulix.space.network.interceptor.EulixGatewayInterceptor;
import xyz.eulix.space.transfer.net.TransferNetUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.MD5Util;
import xyz.eulix.space.util.PreferenceUtil;
import xyz.eulix.space.util.network.OkHttpUtil;

/**
 * Author:      Zhu Fuyu
 * Description: 个人信息处理类（头像、空间标识、签名）
 * History:     2021/9/17
 */
public class UserInfoManager {
    private static final String TAG = "UserInfoManager";

    private static OkHttpClient generateOkHttpClient() {
        return OkHttpUtil.generateOkHttpClient(false).newBuilder()
                .build();
    }

    private static OkHttpClient generateOkHttpClient(String boxDomain, UUID requestId, String requestType, String accessToken, String secret, String ivParams, String apiVersion) {
        return OkHttpUtil.generateOkHttpClient(true).newBuilder()
                .callTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
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

    private static Retrofit generateRetrofit(String baseUrl, OkHttpClient okHttpClient) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .client(okHttpClient)
                .build();
    }

    @Deprecated
    public static void getPersonalInfo(String clientUUID, String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, IGetUserInfoCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.PERSONALINFO_SHOW, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        UserInfoService service = retrofit.create(UserInfoService.class);
        Observable<GetUserInfoResponseBody> observable = service.getUserInfoOld(clientUUID);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<GetUserInfoResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(GetUserInfoResponseBody response) {
                        Logger.i(TAG, "on next: " + (response == null ? "null" : response.toString()));
                        if (callback != null && response != null) {
                            callback.onResult(response.getPersonalName(), response.getPersonalSign());
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

    public static void getPersonalInfo(String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, IAccountInfoCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.PERSONALINFO_SHOW, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        UserInfoService service = retrofit.create(UserInfoService.class);
        Observable<AccountInfoResult> observable = service.getUserInfo(uuid.toString());
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<AccountInfoResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(AccountInfoResult accountInfoResult) {
                        Logger.i(TAG, "get personalInfo on next: " + (accountInfoResult == null ? "null" : accountInfoResult.toString()));
                        if (callback != null) {
                            callback.onResult(accountInfoResult);
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

    //更新昵称
    @Deprecated
    public static void updateUserInfo(String clientUUID, String nickname, String signature, String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, IUpdateUserInfoCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.PERSONALINFO_UPDATE, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        UserInfoService service = retrofit.create(UserInfoService.class);
        UpdateUserInfoRsq updateUserInfoRsq = new UpdateUserInfoRsq();
        updateUserInfoRsq.setPersonalName(nickname);
        updateUserInfoRsq.setPersonalSign(signature);
        Observable<UpdateUserInfoResponseBody> observable = service.updateUserInfoOld(clientUUID, updateUserInfoRsq);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<UpdateUserInfoResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(UpdateUserInfoResponseBody response) {
                        Logger.i(TAG, "on next: " + response.getCode());
                        int code = response.getCode();
                        if (callback == null) {
                            return;
                        }
                        if (code >= 200 && code < 300) {
                            callback.onResult(true, null);
                        } else {
                            callback.onResult(false, response.getCode() + "");
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                        Logger.e(TAG, "on error: " + errMsg);
                        if (callback != null) {
                            callback.onResult(null, errMsg);
                        }
                    }

                    @Override
                    public void onComplete() {
                        Logger.d(TAG, "on complete");
                    }
                });
    }

    //更新昵称
    public static void updateUserInfo(String nickname, String signature, String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, IAccountInfoCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.PERSONALINFO_UPDATE, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        UserInfoService service = retrofit.create(UserInfoService.class);
        UpdateUserInfoRsq updateUserInfoRsq = new UpdateUserInfoRsq();
        updateUserInfoRsq.setPersonalName(nickname);
        updateUserInfoRsq.setPersonalSign(signature);
        Observable<AccountInfoResult> observable = service.updateUserInfo(uuid.toString(), updateUserInfoRsq);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<AccountInfoResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(AccountInfoResult accountInfoResult) {
                        Logger.i(TAG, "on next: " + (accountInfoResult == null ? "null" : accountInfoResult.toString()));
                        if (callback != null) {
                            callback.onResult(accountInfoResult);
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

    //更新头像
    public static void updateHeader(Context context, String clientUUID, String filepath, String filename, String boxDomain, String accessToken,
                                    String secret, String transformation, String ivParams, String apiVersion, IUpdateUserInfoCallback listener) {
        String remotePath = "/";
        if (filepath != null && filename != null) {
            File file = new File(filepath, filename);
            if (file.exists()) {
                Logger.d("zfy", "filePath=" + file.getAbsolutePath());
                HttpUrl httpParseUrl = HttpUrl.parse(generateBaseUrl(boxDomain) + ConstantField.URL.UPLOAD_GATEWAY_API);
                if (httpParseUrl != null) {
                    HttpUrl httpUrl = httpParseUrl.newBuilder()
                            .build();
                    Logger.i(TAG, "upload url: " + httpUrl);

                    UUID requestId = UUID.randomUUID();
                    File externalCacheDir = context.getExternalCacheDir();
                    if (externalCacheDir != null) {
                        String cachePath = externalCacheDir.getAbsolutePath() + "/header/";
                        FileUtil.mkFile(cachePath);
                        Logger.d("zfy", "cachePath=" + cachePath);

                        String fileMD5 = "";
                        try {
                            fileMD5 = MD5Util.getFileMD5String(file);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        //生成临时加密文件
                        File encryptFile = EncryptionUtil.encrypt(transformation, null,
                                file, secret, StandardCharsets.UTF_8, ivParams, cachePath);


                        JSONObject queryJsonObject = new JSONObject();
                        JSONObject entityJsonObject = new JSONObject();
                        JSONObject headerJsonObject = new JSONObject();
                        JSONObject callRequestJson = new JSONObject();
                        try {
                            queryJsonObject.put("uuid", "/");

                            entityJsonObject.put("filename", filename);
                            entityJsonObject.put("path", remotePath);
                            entityJsonObject.put("createTime", file.lastModified() * 1000);
                            entityJsonObject.put("modifyTime", file.lastModified() * 1000);
                            entityJsonObject.put("md5sum", fileMD5);
                            entityJsonObject.put("mediaType", "application/octet-stream");
                            Logger.d(TAG, "entity:" + entityJsonObject.toString());

                            headerJsonObject.put("Request-Id", requestId.toString());
                            headerJsonObject.put("Accept", "*/*");
                            headerJsonObject.put("clientUUID", clientUUID);
                            headerJsonObject.put("Content-Type", "multipart/form-data");

                            callRequestJson.put("apiVersion", apiVersion);
                            callRequestJson.put("queries", queryJsonObject);
                            callRequestJson.put("apiName", ConstantField.ServiceFunction.PERSONAL_IMAGE_UPDATE);
                            callRequestJson.put("requestId", requestId.toString());
                            callRequestJson.put("headers", headerJsonObject);
                            callRequestJson.put("entity", entityJsonObject);
                            callRequestJson.put("serviceName", ConstantField.ServiceName.EULIXSPACE_ACCOUNT_SERVICE);
                            Logger.d("zfy", "callJson=" + callRequestJson.toString());
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        //加密callRequest
                        String encryptCallJsonStr = EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5, null,
                                callRequestJson.toString(), secret, StandardCharsets.UTF_8, ivParams);
                        Logger.d("zfy", "encryptCallJsonStr=" + encryptCallJsonStr);

                        CallRequest callRequest = new CallRequest();
                        callRequest.setAccessToken(accessToken);
                        callRequest.setBody(encryptCallJsonStr);

                        String callRequestStr = new Gson().toJson(callRequest, CallRequest.class);


                        String responseBodyStr = TransferNetUtil.postFile(httpUrl.toString(), filename, encryptFile, accessToken, callRequestStr,
                                requestId.toString(), secret, transformation, ivParams, null);
                        if (!TextUtils.isEmpty(responseBodyStr)) {
                            UploadResponseBodyResult uploadResponseBody = new Gson().fromJson(responseBodyStr, UploadResponseBodyResult.class);
                            Integer code = uploadResponseBody.getCodeInt();
                            //删除加密缓存文件
                            if (encryptFile != null && encryptFile.exists()) {
                                Logger.d("zfy", "delete cache file");
                                boolean result = encryptFile.delete();
                                Logger.d("zfy", "encrypt file delete: " + result);
                            }
                            new Handler(context.getMainLooper()).post(() -> {
                                if (code != null && code >= 200 && code < 300) {
                                    Logger.d("zfy", "上传成功");
                                    if (listener != null) {
                                        listener.onResult(true, null);
                                    }
                                } else {
                                    String message = uploadResponseBody.getMessage();
                                    Logger.d("zfy", "上传失败" + code + "\n" + message);
                                    listener.onResult(false, message);
                                }
                            });
                        } else {
                            listener.onResult(null, "上传失败");
                        }
                    }
                }
            }
        }
    }

    //下载头像
    public static void downloadHeader(Context context, String clientUUID, String aoid, String boxUuid, String boxBind, String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, boolean isUpdateAvatar) {
        if (context == null) {
            return;
        }
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = OkHttpUtil.generateOkHttpClient(true).newBuilder()
                .addInterceptor(new EulixGatewayInterceptor(boxDomain, uuid, ConstantField.ServiceFunction.IMAGE_SHOW
                        , ConstantField.Algorithm.Transformation.AES_CBC_PKCS5, null, ivParams
                        , accessToken, secret, MediaType.parse("application/json; charset=utf-8")
                        , MediaType.parse("*/*"), apiVersion))
                .build();

        HttpUrl httpParseUrl = HttpUrl.parse(generateBaseUrl(boxDomain) + ConstantField.URL.DOWNLOAD_GATEWAY_API);
        if (httpParseUrl != null) {
            HttpUrl httpUrl = httpParseUrl.newBuilder()
                    .addQueryParameter("aoid", aoid)
                    .build();
            Logger.i("zfy", "download header url: " + httpUrl);
            Request request = new Request.Builder()
                    .url(httpUrl)
                    .addHeader("Request-Id", uuid.toString())
                    .addHeader("uuid", uuid.toString())
                    .addHeader("Accept", "*/*")
                    .addHeader("clientUUID", clientUUID)
                    .get()
                    .build();

            //下载文件路径
            File externalFilesDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            if (externalFilesDir != null) {
                final String nFilePathFinal = externalFilesDir.getAbsolutePath() + "/LineHeader/";
                File folder = new File(nFilePathFinal);
                if (!folder.exists()) {
                    folder.mkdir();
                }
                StringBuilder fileBuilder = new StringBuilder();
                fileBuilder.append("avatar");
                if (boxUuid != null) {
                    fileBuilder.append("_box_");
                    fileBuilder.append(boxUuid);
                }
                fileBuilder.append("_client_");
                fileBuilder.append(clientUUID);
                String filename = fileBuilder.toString();

                Call call = okHttpClient.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        Logger.e(TAG, "on failure, e: " + e.getMessage());
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        Logger.i("zfy", "on response " + response.code());
                        if (!response.isSuccessful()) {
                            return;
                        }
                        Headers headers = response.headers();
                        //文件大小
                        String fileSizeStr = headers.get("file-size");
                        Logger.d("zfy", "file-size=" + fileSizeStr);
                        long fileSize = (fileSizeStr == null ? 0L : Long.parseLong(fileSizeStr));
                        //文件名称等(inline; filename="header_chosen.jpg"; filename*=UTF-8''header_chosen.jpg)
                        String contentDisposition= headers.get("content-disposition");
                        Logger.d("zfy", "content-disposition=" + contentDisposition);
                        String suffix = "png";
                        if (contentDisposition != null && !TextUtils.isEmpty(contentDisposition)) {
                            int contentDotIndex = contentDisposition.lastIndexOf(".");
                            suffix = contentDisposition.substring(contentDotIndex + 1);
                        }
                        String saveFilename = filename + "." + suffix;
                        Logger.d(TAG, "filepath: " + nFilePathFinal);
                        FileUtil.mkFile(nFilePathFinal);
                        Logger.d("zfy", "saveFileName = " + saveFilename);
                        File file = new File(nFilePathFinal, saveFilename);

                        if (file.exists()) {
                            boolean result = file.delete();
                            Logger.d("zfy", "delete file result =" + result);
                        }

                        ResponseBody responseBody = response.body();
                        if (responseBody != null) {
                            InputStream inputStream = responseBody.byteStream();

                            //解密并保存
                            File decryptFile = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5, null, inputStream,
                                    secret, StandardCharsets.UTF_8, ivParams, nFilePathFinal, saveFilename, fileSize, null);
                            if (decryptFile != null){
                                Logger.d("get header image success." + decryptFile.getAbsolutePath());
                                if (boxUuid != null) {
                                    Map<String, String> userMap = new HashMap<>();
                                    userMap.put(UserInfoUtil.AVATAR_PATH, decryptFile.getAbsolutePath());
                                    UserInfoUtil.updateUserInfoDB(context, boxUuid, boxBind, clientUUID, userMap);
                                }
                                if (isUpdateAvatar) {
                                    EulixBoxBaseInfo boxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(context);
                                    if (boxBaseInfo != null) {
                                        String activeBoxUuid = boxBaseInfo.getBoxUuid();
                                        String activeBoxBind = boxBaseInfo.getBoxBind();
                                        if (activeBoxUuid != null && activeBoxUuid.equals(boxUuid)
                                                && activeBoxBind != null && activeBoxBind.equals(boxBind)) {
                                            PreferenceUtil.saveHeaderPath(context, decryptFile.getAbsolutePath());
                                            EventBusUtil.post(new UserInfoEvent(UserInfoEvent.TYPE_HEADER, decryptFile.getAbsolutePath(), null, null));
                                        }
                                    }
                                }
                            }
                            inputStream.close();
                            response.close();
                        }
                    }
                });
            }
        }
    }

    public static void getMemberUsedStorage(String clientUuid, String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, String aoId, IMemberUsedStorageCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.MEMBER_USED_STORAGE, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        UserInfoService service = retrofit.create(UserInfoService.class);
        Observable<MemberUsedStorageResponseBody> observable = service.getMemberUsedStorage(uuid.toString(), aoId);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<MemberUsedStorageResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(MemberUsedStorageResponseBody memberUsedStorageResponseBody) {
                        Logger.i(TAG, "on next: " + (memberUsedStorageResponseBody == null ? "null" : memberUsedStorageResponseBody.toString()));
                        if (callback != null) {
                            callback.onResult(clientUuid, memberUsedStorageResponseBody);
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

    public static void getMemberList(String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, IAccountInfoCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.MEMBER_LIST, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        UserInfoService service = retrofit.create(UserInfoService.class);
        Observable<AccountInfoResult> observable = service.getMemberList(uuid.toString());
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<AccountInfoResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(AccountInfoResult result) {
                        Logger.i(TAG, "on next: " + (result == null ? "null" : result.toString()));
                        if (callback != null) {
                            callback.onResult(result);
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

    public static void getTerminalList(String boxDomain, String aoId, String accessToken, String secret, String ivParams, String apiVersion, ITerminalListCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.TERMINAL_INFO_ALL_SHOW, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        UserInfoService service = retrofit.create(UserInfoService.class);
        Observable<TerminalListResponse> observable = service.getTerminalList(uuid.toString(), aoId);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<TerminalListResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(TerminalListResponse terminalListResponse) {
                        Logger.i(TAG, "on next: " + (terminalListResponse == null ? "null" : terminalListResponse.toString()));
                        if (callback != null) {
                            callback.onResult(terminalListResponse);
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


    public static void offlineTerminal(String aoId, String clientUuid, String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, ITerminalOfflineCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.TERMINAL_INFO_DELETE, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        UserInfoService service = retrofit.create(UserInfoService.class);
        Observable<TerminalOfflineResponse> observable = service.offlineTerminal(uuid.toString(), aoId, clientUuid);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<TerminalOfflineResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(TerminalOfflineResponse terminalOfflineResponse) {
                        Logger.i(TAG, "on next: " + (terminalOfflineResponse == null ? "null" : terminalOfflineResponse.toString()));
                        if (callback != null) {
                            callback.onResult(terminalOfflineResponse);
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

    public static void updateMemberName(MemberNameUpdateInfo memberNameUpdateInfo, String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, IMemberNameUpdateCallback callback) {
        UUID uuid = UUID.randomUUID();
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, uuid, ConstantField.ServiceFunction.MEMBER_NAME_UPDATE, accessToken, secret, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        UserInfoService service = retrofit.create(UserInfoService.class);
        Observable<MemberNameUpdateResult> observable = service.updateMemberName(uuid.toString(), memberNameUpdateInfo);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<MemberNameUpdateResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(MemberNameUpdateResult memberNameUpdateResult) {
                        Logger.i(TAG, "on next: " + (memberNameUpdateResult == null ? "null" : memberNameUpdateResult.toString()));
                        if (callback != null) {
                            callback.onResult(memberNameUpdateResult);
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

    public static void createMember(MemberCreateInfo memberCreateInfo, String boxDomain, String aoId, IMemberCreateCallback callback) {
        UUID uuid = UUID.randomUUID();
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, generateOkHttpClient());
        UserInfoService service = retrofit.create(UserInfoService.class);
        Observable<MemberCreateResult> observable = service.createMember(uuid.toString(), aoId, memberCreateInfo);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<MemberCreateResult>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(MemberCreateResult memberCreateResult) {
                        Logger.i(TAG, "on next: " + (memberCreateResult == null ? "null" : memberCreateResult.toString()));
                        if (callback != null) {
                            callback.onResult(memberCreateResult);
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

    public static void createMemberToken(CreateMemberTokenInfo createMemberTokenInfo, String boxDomain, ICreateAuthTokenCallback callback) {
        UUID uuid = UUID.randomUUID();

        OkHttpClient okHttpClient = OkHttpUtil.generateOkHttpClient(false);
        String requestUrl = (generateBaseUrl(boxDomain) + ConstantField.URL.GATEWAY_CREATE_MEMBER_TOKEN_API);
        String requestContent = new Gson().toJson(createMemberTokenInfo, CreateMemberTokenInfo.class);
        HttpUrl httpUrl = HttpUrl.parse(requestUrl);
        if (httpUrl != null) {
            RequestBody requestBody = RequestBody.create(requestContent, MediaType.parse("application/json; charset=utf-8"));
            Request request = new Request.Builder()
                    .url(httpUrl)
                    .addHeader("Request-Id", uuid.toString())
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Accept", "application/json")
                    .post(requestBody)
                    .build();
            String headersContent = request.headers().toString();
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
                        Logger.d(TAG, "create member token content: " + responseContent);
                        if (code < 400) {
                            try {
                                createTokenResult = new Gson().fromJson(responseContent, CreateTokenResult.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                            isSuccess = (createTokenResult != null);
                        } else {
                            RealCallResult realCallResult = null;
                            if (responseContent != null) {
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
        } else if (callback != null) {
            callback.onError(-1, "");
        }
    }

    //管理员、成员解绑
    public static void revokeDevice(CreateTokenInfo createTokenInfo, String boxDomain, boolean isAdmin, final IRevokeResultExtensionCallback callback) {
        UUID uuid = UUID.randomUUID();
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, generateOkHttpClient());
        UserInfoService service = retrofit.create(UserInfoService.class);
        Observable<RevokeMemberResponseBody> observable = service.revokeMember(uuid.toString(), createTokenInfo);
        if (isAdmin){
            Logger.d("zfy", "current is admin");
            observable = service.revokeAdmin(uuid.toString(), createTokenInfo);
        }
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<RevokeMemberResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(RevokeMemberResponseBody revokeMemberResult) {
                        Logger.i("zfy", "on next: " + (revokeMemberResult == null ? "null" : revokeMemberResult.toString()));
                        if (callback != null) {
                            callback.onResult(revokeMemberResult);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                        Logger.e("zfy", "on error: " + errMsg);
                        if (callback != null) {
                            callback.onError(errMsg);
                            if (e != null) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onComplete() {
                        Logger.d(TAG, "on complete");
                    }
                });
    }

}
