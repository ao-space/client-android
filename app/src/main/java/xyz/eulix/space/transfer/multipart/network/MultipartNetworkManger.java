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

package xyz.eulix.space.transfer.multipart.network;

/**
 * Author:      Zhu Fuyu
 * Description:
 * History:     2022/2/21
 */

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
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
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import xyz.eulix.space.R;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.interfaces.ResultCallback;
import xyz.eulix.space.interfaces.ResultCallbackObj;
import xyz.eulix.space.manager.LanManager;
import xyz.eulix.space.network.files.BaseResponseBody;
import xyz.eulix.space.network.files.UploadResponseBodyResult;
import xyz.eulix.space.network.gateway.CallRequest;
import xyz.eulix.space.network.gateway.RealCallResult;
import xyz.eulix.space.network.interceptor.EulixGatewayInterceptor;
import xyz.eulix.space.transfer.TransferHelper;
import xyz.eulix.space.transfer.TransferProgressListener;
import xyz.eulix.space.transfer.db.TransferDBManager;
import xyz.eulix.space.transfer.model.TransferItemFactory;
import xyz.eulix.space.transfer.multipart.bean.GetCertResponseBody;
import xyz.eulix.space.transfer.multipart.bean.UploadChunkBean;
import xyz.eulix.space.transfer.multipart.bean.UploadCompleteResponseBody;
import xyz.eulix.space.transfer.multipart.bean.UploadCreateRequestBody;
import xyz.eulix.space.transfer.multipart.bean.UploadCreateResponseBody;
import xyz.eulix.space.transfer.multipart.bean.UploadIdRequestBody;
import xyz.eulix.space.transfer.multipart.bean.UploadListResponseBody;
import xyz.eulix.space.transfer.multipart.lan.LanUploadFileProgressRequestBody;
import xyz.eulix.space.transfer.net.TransferNetUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.FailCodeUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.GatewayUtils;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.ToastUtil;
import xyz.eulix.space.util.Urls;
import xyz.eulix.space.util.network.OkHttpUtil;

/**
 * Author:      Zhu Fuyu
 * Description: 断点续传接口管理类
 * History:     2022/02/21
 */
public class MultipartNetworkManger {

    private static OkHttpClient generateOkHttpClient(GatewayCommunicationBase gatewayCommunicationBase, UUID requestId, String requestType) {
        String boxDomain = Urls.getBaseUrl();
        String accessToken = gatewayCommunicationBase.getAccessToken();
        String secret = gatewayCommunicationBase.getSecretKey();
        String transformation = gatewayCommunicationBase.getTransformation();
        String ivParams = gatewayCommunicationBase.getIvParams();
        String apiVersion = ConstantField.BoxVersionName.VERSION_0_1_0;

        return OkHttpUtil.generateOkHttpClient(true).newBuilder()
                .callTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(60, TimeUnit.SECONDS)
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

    //创建上传任务
    // 此接口完成分段上传任务的创建，正常情况返回任务id。
    //如果文件实体已经存在，则增加文件索引，并返回文件结构信息，文件秒传完成。
    //如果上传任务已存在，则返回任务已存在
    public static void createUpload(Context context, GatewayCommunicationBase gatewayCommunicationBase, String targetPath, String fileLocalPath, String betag, boolean isSync, String albumId, ResultCallbackObj callback) {
        File file = new File(fileLocalPath);
        if (!file.exists()) {
            if (callback != null) {
                callback.onError("file not exist");
            }
            return;
        }
        if (gatewayCommunicationBase == null) {
            if (callback != null) {
                callback.onError("gatewayCommunicationBase is null");
            }
            return;
        }
        String boxDomain = Urls.getBaseUrl();

        OkHttpClient okHttpClient = generateOkHttpClient(gatewayCommunicationBase, UUID.randomUUID(), ConstantField.ServiceFunction.MULTIPART_CREATE_UPLOAD);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        MultipartNetworkService service = retrofit.create(MultipartNetworkService.class);
        UploadCreateRequestBody requestBody = new UploadCreateRequestBody();
        requestBody.betag = betag;
        if (!TextUtils.isEmpty(albumId)) {
            requestBody.businessId = ConstantField.UploadBusinessIdType.TYPE_ALBUM;
            try {
                requestBody.albumId = Integer.parseInt(albumId);
            } catch (Exception ignored) {
            }
        } else {
            requestBody.businessId = isSync ? ConstantField.UploadBusinessIdType.TYPE_SYNC : ConstantField.UploadBusinessIdType.TYPE_DEFAULT;
        }
        requestBody.folderPath = targetPath;
        requestBody.fileName = file.getName();
        requestBody.createTime = file.lastModified();
        requestBody.modifyTime = file.lastModified();
        requestBody.mime = FileUtil.getMimeTypeByPath(file.getName());
        requestBody.size = file.length();

        Observable<UploadCreateResponseBody> observable = service.createUpload(requestBody);
        observable.subscribeOn(Schedulers.trampoline())
//                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<UploadCreateResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d("zfy", "create upload on subscribe");
                    }

                    @Override
                    public void onNext(UploadCreateResponseBody result) {
                        Logger.d("zfy", "create upload on next: " + (result == null ? "null" : result.toString()));
                        if (result != null && result.getCodeInt() == 1036) {
                            //盒子空间不足
                            Logger.d("zfy", "空间不足，上传失败，请清理空间后再重试~");
                            ToastUtil.showToast(context.getString(R.string.transfer_upload_failed));
                            if (callback != null) {
                                callback.onResult(false, String.valueOf(result.getCodeInt()));
                            }
                            return;
                        }

                        if (!TextUtils.isEmpty(albumId)) {
                            //相簿更新文件上传路径
                            if (result != null && result.getResults().completeInfo != null) {
                                Logger.d("zfy", "update album upload target path");
                                String uniqueTag = TransferItemFactory.getUniqueTagWithAlbumId(TransferHelper.TYPE_UPLOAD, file.getName(), fileLocalPath, targetPath, null, albumId);
                                TransferDBManager.getInstance(context).updateTransferRemotePath(uniqueTag, result.getResults().completeInfo.getPath());
                            }
                        }
                        if (callback != null) {
                            callback.onResult(true, result);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                        Logger.e("zfy", "create upload on error: " + errMsg);
                        if (callback != null) {
                            callback.onError(errMsg);
                        }
                        if (e != null) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onComplete() {
                        Logger.d("zfy", "create upload on complete");
                    }
                });
    }

    /**
     * 获取已上传片段列表
     *
     * @param context
     * @param uploadId
     * @param callback
     */
    public static void listUpload(Context context, String uploadId, ResultCallbackObj callback) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase == null) {
            if (callback != null) {
                callback.onError("gatewayCommunicationBase is null");
            }
            return;
        }
        String boxDomain = Urls.getBaseUrl();

        OkHttpClient okHttpClient = generateOkHttpClient(gatewayCommunicationBase, UUID.randomUUID(), ConstantField.ServiceFunction.MULTIPART_LIST_UPLOAD);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        MultipartNetworkService service = retrofit.create(MultipartNetworkService.class);

        Observable<UploadListResponseBody> observable = service.listUpload(uploadId);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<UploadListResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d("zfy", "list upload on subscribe");
                    }

                    @Override
                    public void onNext(UploadListResponseBody result) {
                        Logger.d("zfy", "list upload on next: " + (result == null ? "null" : result.toString()));
                        if (callback != null) {
                            callback.onResult(true, result);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                        Logger.e("zfy", "list upload on error: " + errMsg);
                        if (callback != null) {
                            callback.onError(errMsg);
                        }
                        if (e != null) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onComplete() {
                        Logger.d("zfy", "list upload on complete");
                    }
                });
    }


    /**
     * 上传分片文件
     *
     * @param context
     * @param uploadId
     * @param uploadChunk
     * @param callback
     * @param progressListener
     */
    public static void uploadFile(Context context, GatewayCommunicationBase gatewayCommunicationBase, String uploadId, UploadChunkBean uploadChunk, boolean isSync, ResultCallback callback, TransferProgressListener progressListener) {
        if (TextUtils.isEmpty(uploadId)) {
            callback.onResult(false, "uploadId is empty");
            return;
        }

        File file = new File(uploadChunk.path);
        if (!file.exists()) {
            callback.onResult(false, "local file is not exist");
            return;
        }
        Logger.d("zfy", "filePath=" + file.getAbsolutePath());

        String transformation = gatewayCommunicationBase.getTransformation();
        String secret = gatewayCommunicationBase.getSecretKey();
        String ivParams = gatewayCommunicationBase.getIvParams();
        String accessToken = gatewayCommunicationBase.getAccessToken();

        String boxDomain = Urls.getBaseUrl();
        HttpUrl httpParseUrl = HttpUrl.parse(generateBaseUrl(boxDomain) + ConstantField.URL.MULTIPART_UPLOAD_GATEWAY_API);
        if (httpParseUrl != null) {

            UUID requestId = UUID.randomUUID();

            JSONObject queryJsonObject = new JSONObject();
            JSONObject entityJsonObject = new JSONObject();
            JSONObject headerJsonObject = new JSONObject();
            JSONObject callRequestJson = new JSONObject();
            try {
                queryJsonObject.put("uuid", "/");

                queryJsonObject.put("uploadId", uploadId);
                queryJsonObject.put("start", uploadChunk.start);
                queryJsonObject.put("end", uploadChunk.end);
                queryJsonObject.put("md5sum", uploadChunk.md5);
                entityJsonObject.put("businessId", isSync ? 1 : 0);
                queryJsonObject.put("mediaType", "application/octet-stream");
                Logger.d("zfy", "entity:" + entityJsonObject.toString());

                headerJsonObject.put("Request-Id", requestId.toString());
                headerJsonObject.put("Accept", "*/*");
                headerJsonObject.put("Content-Type", "multipart/form-data");

                callRequestJson.put("apiVersion", ConstantField.BoxVersionName.VERSION_0_2_0);
                callRequestJson.put("queries", queryJsonObject);
                callRequestJson.put("apiName", ConstantField.ServiceFunction.MULTIPART_UPLOAD_UPLOAD);
                callRequestJson.put("requestId", requestId.toString());
                callRequestJson.put("headers", headerJsonObject);
                callRequestJson.put("entity", entityJsonObject);
                callRequestJson.put("serviceName", ConstantField.ServiceName.EULIXSPACE_FILE_SERVICE);
                Logger.d("zfy", "callJson=" + callRequestJson.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }


            //加密callRequest
            String encryptCallJsonStr = EncryptionUtil.encrypt(transformation, null,
                    callRequestJson.toString(), secret, StandardCharsets.UTF_8, ivParams);
            Logger.d("zfy", "encryptCallJsonStr=" + encryptCallJsonStr);

            CallRequest callRequest = new CallRequest();
            callRequest.setAccessToken(accessToken);
            callRequest.setBody(encryptCallJsonStr);

            String callRequestStr = new Gson().toJson(callRequest, CallRequest.class);

            HttpUrl httpUrl = httpParseUrl.newBuilder()
                    .build();
            Logger.d("zfy", "multipart upload url: " + httpUrl);

            String responseBodyStr = TransferNetUtil.postFile(httpUrl.toString(), file.getName(), file, accessToken, callRequestStr,
                    requestId.toString(), secret, transformation, ivParams, progressListener);

            //删除加密缓存文件
            if (file.exists()) {
                Logger.d("zfy", "delete chunk cache file");
                boolean result = file.delete();
                Logger.d("zfy", "chunk file delete: " + result);
            }

            if (!TextUtils.isEmpty(responseBodyStr)) {
                UploadResponseBodyResult uploadResponseBody = new Gson().fromJson(responseBodyStr, UploadResponseBodyResult.class);
                int code = uploadResponseBody.getCodeInt();
                Logger.d("zfy", "upload chunk response code=" + code);
//                CodeMultipartRangeUploaded CodeType = 1037 //分片范围已上传
                if (code == 200 || code == 1037) {
                    Logger.d("zfy", "片段上传成功：" + file.getName());
                    if (callback != null) {
                        callback.onResult(true, null);
                    }
                } else if (code == 1036) {
                    //盒子空间不足
                    Logger.d("zfy", "空间不足，上传失败，请清理空间后再重试~");
                    ToastUtil.showToast(context.getString(R.string.transfer_upload_failed));
                    if (callback != null) {
                        callback.onResult(false, String.valueOf(code));
                    }
                } else {
                    String message = uploadResponseBody.getMessage();
                    Logger.d("zfy", "上传失败" + code + "\n" + message);
                    if (callback != null) {
                        callback.onResult(false, String.valueOf(code));
                    }
                }
            } else {
                Logger.d("zfy", "片段上传失败：" + file.getName());
                if (callback != null) {
                    callback.onResult(false, "上传失败");
                }
            }

        }
    }

    /**
     * 合并已上传片段列表
     *
     * @param context
     * @param uploadId
     * @param callback
     */
    public static void completeUpload(Context context, GatewayCommunicationBase gatewayCommunicationBase, String uploadId, ResultCallbackObj callback) {
        if (gatewayCommunicationBase == null) {
            if (callback != null) {
                callback.onError("gatewayCommunicationBase is null");
            }
            return;
        }
        String boxDomain = Urls.getBaseUrl();

        OkHttpClient okHttpClient = generateOkHttpClient(gatewayCommunicationBase, UUID.randomUUID(), ConstantField.ServiceFunction.MULTIPART_COMPLETE_UPLOAD);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        MultipartNetworkService service = retrofit.create(MultipartNetworkService.class);
        UploadIdRequestBody uploadIdRequestBody = new UploadIdRequestBody();
        uploadIdRequestBody.uploadId = uploadId;
        Observable<UploadCompleteResponseBody> observable = service.completeUpload(uploadIdRequestBody);
        observable.subscribeOn(Schedulers.trampoline())
//                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<UploadCompleteResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d("zfy", "complete upload on subscribe");
                    }

                    @Override
                    public void onNext(UploadCompleteResponseBody result) {
                        Logger.d("zfy", "complete upload on next: " + (result == null ? "null" : result.toString()));
                        if (result != null) {
                            int code = result.getCodeInt();
                            if (code == 200) {
                                Logger.d("zfy", "complete chunks success!");
                                callback.onResult(true, result.Results);
                            } else {
                                String message = result.getMessage();
                                Logger.d("zfy", "complete chunks failed!!" + message);
                                callback.onResult(false, String.valueOf(code));
                            }
                        } else {
                            callback.onResult(false, null);
                        }

                    }

                    @Override
                    public void onError(Throwable e) {
                        String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                        Logger.e("zfy", "complete upload on error: " + errMsg);
                        if (callback != null) {
                            callback.onError(errMsg);
                        }
                        if (e != null) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onComplete() {
                        Logger.d("zfy", "complete upload on complete");
                    }
                });
    }


    /**
     * 删除已上传片段列表
     *
     * @param context
     * @param uploadId
     * @param callback
     */
    public static void deleteUpload(Context context, String uploadId, ResultCallbackObj callback) {
        if (TextUtils.isEmpty(uploadId)) {
            return;
        }
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase == null) {
            if (callback != null) {
                callback.onError("gatewayCommunicationBase is null");
            }
            return;
        }
        String boxDomain = Urls.getBaseUrl();

        OkHttpClient okHttpClient = generateOkHttpClient(gatewayCommunicationBase, UUID.randomUUID(), ConstantField.ServiceFunction.MULTIPART_DELETE_UPLOAD);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        MultipartNetworkService service = retrofit.create(MultipartNetworkService.class);
        UploadIdRequestBody uploadIdRequestBody = new UploadIdRequestBody();
        uploadIdRequestBody.uploadId = uploadId;
        Observable<BaseResponseBody> observable = service.deleteUpload(uploadIdRequestBody);
        observable.subscribeOn(Schedulers.trampoline())
//                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<BaseResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d("zfy", "delete upload on subscribe");
                    }

                    @Override
                    public void onNext(BaseResponseBody result) {
                        Logger.d("zfy", "delete upload on next: " + (result == null ? "null" : result.toString()));
                        if (callback != null) {
                            if (result != null && result.getCodeInt() == 200) {
                                callback.onResult(true, result);
                            } else {
                                callback.onResult(false, result.getMessage());
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                        Logger.e("zfy", "delete upload on error: " + errMsg);
                        if (callback != null) {
                            callback.onError(errMsg);
                        }
                        if (e != null) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onComplete() {
                        Logger.d("zfy", "delete upload on complete");
                    }
                });
    }


    /**
     * 下载文件片段
     *
     * @param fileUuid
     * @param cacheDirPath
     * @param start
     * @param end
     * @param callback
     * @param progressListener
     */
    public static void downloadFile(GatewayCommunicationBase gatewayCommunicationBase, String fileUuid, String cacheDirPath, long start, long end, ResultCallback callback, TransferProgressListener progressListener) {
        if (gatewayCommunicationBase == null) {
            if (callback != null) {
                callback.onResult(false, "gatewayCommunicationBase is null");
            }
            return;
        }

        String transformation = gatewayCommunicationBase.getTransformation();
        String secret = gatewayCommunicationBase.getSecretKey();
        String ivParams = gatewayCommunicationBase.getIvParams();

        String boxDomain = Urls.getBaseUrl();
        Logger.d("GarveyP2P", "http multi download start");
        OkHttpClient okHttpClient = generateOkHttpClient(gatewayCommunicationBase, UUID.randomUUID(), ConstantField.ServiceFunction.DOWNLOAD_FILE);
        HttpUrl httpParseUrl = HttpUrl.parse(generateBaseUrl(boxDomain) + ConstantField.URL.DOWNLOAD_GATEWAY_API);

        long fileSize = end - start + 1;

        File cacheDir = new File(cacheDirPath);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        String chunkFileName = fileUuid + "_" + start + "_" + end;
        Logger.d("zfy", "chunkFileName = " + chunkFileName);

        String rangeValue = "bytes=" + start + "-" + end;
        Logger.d("zfy", "range is:" + rangeValue);

        if (httpParseUrl != null) {
            HttpUrl httpUrl = httpParseUrl.newBuilder()
                    .addQueryParameter("uuid", fileUuid)
                    .build();
            Logger.d("zfy", "download url: " + httpUrl);
            Request request = new Request.Builder()
                    .url(httpUrl)
                    .addHeader("Range", rangeValue)
                    .get()
                    .build();

            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    if (callback != null) {
                        callback.onResult(false, e.getMessage());
                    }
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    Logger.d("zfy", "on response " + response.code());

                    if (!response.isSuccessful()) {
                        if (callback != null) {
                            callback.onResult(false, response.code() + "");
                        }
                        return;
                    }

                    Headers headers = response.headers();
                    String contentType = headers.get("content-type");
                    Logger.d("zfy", "content-type = " + contentType);
                    if (TextUtils.isEmpty(contentType) || contentType.contains("json")) {
                        //没有文件流，下载文件失败
                        Logger.d("zfy", "no stream");
                        String errorCode = "-1";
                        try {
                            byte[] bodyStr = response.body().bytes();
                            byte[] cipherResponseByte = EncryptionUtil.decrypt(transformation, null, bodyStr, secret, StandardCharsets.UTF_8, ivParams);
                            String cipherResponseText = new String(cipherResponseByte);
                            Logger.d("zfy", "cipherResponseText=" + cipherResponseText);
                            if (!TextUtils.isEmpty(cipherResponseText)) {
                                RealCallResult realCallResult = null;
                                realCallResult = new Gson().fromJson(cipherResponseText, RealCallResult.class);
                                if (realCallResult != null) {
                                    //判断文件是否不存在
                                    if (realCallResult.getCode().contains("1003") ||
                                            (!TextUtils.isEmpty(realCallResult.getMessage()) && realCallResult.getMessage().contains("not found"))) {
                                        errorCode = FailCodeUtil.ERROR_DOWNLOAD_REMOTE_SOURCE_DELETE + "";
                                    } else {
                                        errorCode = realCallResult.getCode();
                                    }
                                }
                            }
                        } catch (Exception e) {
                            Logger.d("zfy", "exception " + e.getMessage());
                        }
                        if (callback != null) {
                            callback.onResult(false, errorCode);
                        }
                    } else {
                        //文件大小
                        String headerFileSizeStr = headers.get("file-size");
                        Logger.d("zfy", "file-size=" + headerFileSizeStr);
                        if (TextUtils.isEmpty(headerFileSizeStr)) {
                            headerFileSizeStr = "0";
                        }
                        long headerFileSize = Long.parseLong(headerFileSizeStr);
                        InputStream inputStream = response.body().byteStream();

                        File chunkFile = new File(cacheDirPath, chunkFileName);
                        if (chunkFile.exists()) {
                            Logger.d("zfy", "chunk file to download exist, delete");
                            chunkFile.delete();
                        }

                        String tempChunkFileName = chunkFileName + "_temp";
                        //解密并保存
                        File decryptFile = EncryptionUtil.decrypt(transformation, null, inputStream,
                                secret, StandardCharsets.UTF_8, ivParams, cacheDirPath, tempChunkFileName, headerFileSize, progressListener);

                        if (decryptFile != null) {
                            //下载解析成功，size校验
                            long tempFileSize = decryptFile.length();
                            Logger.d("zfy", "downloadSize=" + tempFileSize);
                            Logger.d("zfy", "targetSize=" + fileSize);

                            if (tempFileSize == fileSize) {
                                Logger.d("zfy", "size校验通过");
                                decryptFile.renameTo(chunkFile);
                                if (callback != null) {
                                    callback.onResult(true, null);
                                }
                            } else {
                                Logger.d("zfy", "size校验失败");
                                boolean result = decryptFile.delete();
                                Logger.d("zfy", "decrypt file delete: " + result);
                                if (callback != null) {
                                    callback.onResult(false, "size校验失败");
                                }
                            }
                        } else {
                            //下载解析失败
                            if (callback != null) {
                                callback.onResult(false, "下载解析失败");
                            }
                        }
                    }
                }
            });
        }
        Logger.d("GarveyP2P", "http multi download end");
    }


    private static String generateHost(String urlStr) {
        String host = urlStr;
        if (urlStr != null) {
            int totalLength = urlStr.length();
            String prefix = "://";
            int prefixIndex = urlStr.indexOf(prefix);
            int startIndex = (prefixIndex + prefix.length());
            if (prefixIndex >= 0 && startIndex <= totalLength) {
                if (startIndex == totalLength) {
                    host = "";
                } else {
                    String nUrlStr = urlStr.substring(startIndex);
                    host = nUrlStr;
                    String suffix = "/";
                    int suffixIndex = nUrlStr.indexOf(suffix);
                    if (suffixIndex >= 0) {
                        if (suffixIndex == 0) {
                            host = "";
                        } else {
                            host = nUrlStr.substring(0, suffixIndex);
                        }
                    }
                }
            }
        }
        return host;
    }

    /**
     * 获取Https证书
     *
     * @param gatewayCommunicationBase
     * @param callback
     */
    public static void getHttpsCert(GatewayCommunicationBase gatewayCommunicationBase, ResultCallbackObj callback) {
        if (gatewayCommunicationBase == null) {
            if (callback != null) {
                callback.onError("gatewayCommunicationBase is null");
            }
            return;
        }
        String boxDomain = Urls.getBaseUrl();

        OkHttpClient okHttpClient = generateOkHttpClient(gatewayCommunicationBase, UUID.randomUUID(), ConstantField.ServiceFunction.GET_HTTPS_CERT);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        MultipartNetworkService service = retrofit.create(MultipartNetworkService.class);

        Observable<GetCertResponseBody> observable = service.getHttpsCert();
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<GetCertResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d("zfy", "list upload on subscribe");
                    }

                    @Override
                    public void onNext(GetCertResponseBody responseBody) {
                        Logger.d("zfy", "list upload on next: " + (responseBody == null ? "null" : responseBody.toString()));
                        if (callback != null) {
                            if (responseBody != null) {
                                if (responseBody.getCodeInt() == 200) {
                                    callback.onResult(true, responseBody.results.certBase64);
                                } else {
                                    callback.onResult(false, responseBody.getMessage());
                                }
                            } else {
                                callback.onResult(false, null);
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                        Logger.e("zfy", "list upload on error: " + errMsg);
                        if (callback != null) {
                            callback.onError(errMsg);
                        }
                        if (e != null) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onComplete() {
                        Logger.d("zfy", "list upload on complete");
                    }
                });
    }

    //通过https上传
    public static void uploadFromHttps(Context context, OkHttpClient okHttpClient, String httpsDomain, String verifyToken, UploadChunkBean chunkBean, String uploadId, ResultCallback callback, TransferProgressListener progressListener, boolean isSync) {
        Logger.d("zfy", "uploadFromHttps");
        if (chunkBean == null) {
            return;
        }
        StringBuilder urlSb = new StringBuilder();
        urlSb.append(httpsDomain);
        urlSb.append(ConstantField.URL.MULTI_UPLOAD_UPLOAD_API);
        urlSb.append("?requestId=");
        urlSb.append(UUID.randomUUID().toString());
        urlSb.append("&uploadId=");
        urlSb.append(uploadId);
        urlSb.append("&start=");
        urlSb.append(chunkBean.start);
        urlSb.append("&end=");
        urlSb.append(chunkBean.end);
        urlSb.append("&md5sum=");
        urlSb.append(chunkBean.md5);
        String url = urlSb.toString();
        HttpUrl httpUrl = HttpUrl.parse(url);
        Logger.d("zfy", "url = " + url);
        Logger.d("zfy", "token:" + verifyToken);
        if (httpUrl == null) {
            Logger.d("zfy", "url is error");
            return;
        }
        File file = new File(chunkBean.path);
        if (!file.exists()) {
            Logger.d("zfy", "local file not exist");
            return;
        }
        LanUploadFileProgressRequestBody requestBody = new LanUploadFileProgressRequestBody(file, chunkBean.start, chunkBean.end - chunkBean.start, progressListener);
        Request request = new Request.Builder()
                .url(httpUrl)
                .post(requestBody)
                .addHeader("Content-Type", "application/octet-stream")
                .addHeader("Token", verifyToken)
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                int code = response.code();
                String message = response.message();
                if (code == 200 || code == 1037) {
                    Logger.d("zfy", "片段上传成功：" + file.getName() + "_" + chunkBean.start + "_" + chunkBean.end);
                    if (callback != null) {
                        callback.onResult(true, null);
                    }
                } else if (code == 1036) {
                    //盒子空间不足
                    Logger.d("zfy", "空间不足，上传失败，请清理空间后再重试~");
                    ToastUtil.showToast(context.getString(R.string.transfer_upload_failed));
                    if (callback != null) {
                        callback.onResult(false, String.valueOf(code));
                    }
                } else {
                    Logger.d("zfy", "片段上传失败：" + file.getName() + "_" + chunkBean.start + "_" + chunkBean.end + " " + code + "\n" + message);
                    //关闭通道https通道可用性
                    LanManager.getInstance().closeHttpsChannel();
                    if (callback != null) {
                        callback.onResult(false, String.valueOf(code));
                    }
                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Logger.d("zfy", "片段上传失败onFailure：" + file.getName() + "_" + chunkBean.start + "_" + chunkBean.end);
                //关闭通道https通道可用性
                LanManager.getInstance().closeHttpsChannel();
                Logger.e(e.getMessage());
                if (callback != null) {
                    callback.onResult(false, "上传失败");
                }
            }
        });

    }

    //通过https下载
    public static void downloadFromHttps(OkHttpClient okHttpClient, String httpsDomain, String verifyToken, String fileUuid, String cacheDirPath, long start, long end, ResultCallback callback, TransferProgressListener progressListener) {
        Logger.d("zfy", "download from https");

        StringBuilder urlSb = new StringBuilder();
        urlSb.append(httpsDomain);
        urlSb.append(ConstantField.URL.FILE_DOWNLOAD_API);
        urlSb.append("?uuid=");
        urlSb.append(fileUuid);
        String url = urlSb.toString();
        HttpUrl httpUrl = HttpUrl.parse(url);
        Logger.d("zfy", "download url = " + url);
        Logger.d("zfy", "token:" + verifyToken);
        if (httpUrl == null) {
            Logger.d("zfy", "url is error");
            return;
        }

        String chunkFileName = fileUuid + "_" + start + "_" + end;
        Logger.d("zfy", "chunkFileName = " + chunkFileName);

        String rangeValue = "bytes=" + start + "-" + end;
        Logger.d("zfy", "range is:" + rangeValue);

        long fileSize = end - start + 1;

        File cacheDir = new File(cacheDirPath);
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        Request request = new Request.Builder()
                .url(httpUrl)
                .get()
                .addHeader("Range", rangeValue)
                .addHeader("Token", verifyToken)
                .build();
        Call call = okHttpClient.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) {
                Logger.d("zfy", "https download on response " + response.code());

                if (!response.isSuccessful()) {
                    if (callback != null) {
                        callback.onResult(false, response.code() + "");
                    }
                    return;
                }

                int code = response.code();
                Headers headers = response.headers();
                String contentType = headers.get("content-type");
                Logger.d("zfy", "content-type = " + contentType);
                if (TextUtils.isEmpty(contentType) || contentType.contains("json")) {
                    //没有文件流，下载文件失败
                    Logger.d("zfy", "no stream");
                    String errorCode = code + "";
                    String errorMsg = response.message();
                    if (code == 1003) {
                        errorCode = FailCodeUtil.ERROR_DOWNLOAD_REMOTE_SOURCE_DELETE + "";
                    } else {
                        //通道异常，关闭
                        LanManager.getInstance().closeHttpsChannel();
                    }
                    if (callback != null) {
                        callback.onResult(false, errorCode);
                    }

                    if (callback != null) {
                        callback.onResult(false, errorCode);
                    }
                } else {
                    //文件大小
                    String headerFileSizeStr = headers.get("file-size");
                    Logger.d("zfy", "file-size=" + headerFileSizeStr);

                    File chunkFile = new File(cacheDirPath, chunkFileName);
                    if (chunkFile.exists()) {
                        Logger.d("zfy", "chunk file to download exist, delete");
                        chunkFile.delete();
                    }

                    String tempChunkFileName = chunkFileName + "_temp";
                    File tempChunkFile = new File(cacheDirPath, tempChunkFileName);
                    if (tempChunkFile.exists()) {
                        Logger.d("zfy", "temp chunk file to download exist, delete");
                        tempChunkFile.delete();
                    }
                    try (InputStream inputStream = Objects.requireNonNull(response.body()).byteStream();
                         FileOutputStream outputStream = new FileOutputStream(tempChunkFile);) {

                        long currentSize = 0L;
                        int oldPercent = 0; //上次进度
                        int currentPercent;
                        boolean isPercentChange = false;
                        int len = 0;
                        byte[] buffer = new byte[1024];
                        while ((len = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, len);
                            outputStream.flush();

                            currentSize += len;
                            if (progressListener != null && fileSize >= 0) {
                                if (fileSize > 0) {
                                    currentPercent = (int) (currentSize * 100 / fileSize);
                                    //进度有变化时再回调，减少回调次数
                                    if (currentPercent > oldPercent) {
                                        oldPercent = currentPercent;
                                        isPercentChange = true;
                                    } else {
                                        isPercentChange = false;
                                    }
                                    progressListener.onProgress(currentSize, fileSize, len, isPercentChange, false);
                                } else {
                                    progressListener.onProgress(currentSize, fileSize, len, false, false);
                                }
                            }
                        }
                    } catch (IOException e) {
                        Logger.e(e.getMessage());
                    }

                    //下载解析成功，size校验
                    long tempFileSize = tempChunkFile.length();
                    Logger.d("zfy", "downloadSize=" + tempFileSize);
                    Logger.d("zfy", "targetSize=" + fileSize);

                    if (tempFileSize == fileSize) {
                        Logger.d("zfy", "size校验通过");
                        if (chunkFile.exists()) {
                            Logger.d("zfy", "chunk file to download exist, delete");
                            chunkFile.delete();
                        }
                        tempChunkFile.renameTo(chunkFile);
                        if (callback != null) {
                            callback.onResult(true, null);
                        }
                    } else {
                        Logger.d("zfy", "size校验失败");
                        Logger.d("zfy", "tmp file delete: " + tempChunkFile.delete());
                        if (callback != null) {
                            callback.onResult(false, "size校验失败");
                        }
                    }

                }
            }

            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Logger.d("zfy", "https片段下载失败：" + chunkFileName);
                //通道异常，关闭
                LanManager.getInstance().closeHttpsChannel();
                Logger.e("zfy", e.getMessage());
                if (callback != null) {
                    callback.onResult(false, "下载失败");
                }
            }
        });

    }

}
