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

package xyz.eulix.space.network.files;

import android.content.Context;
import android.text.TextUtils;

import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
import xyz.eulix.space.interfaces.ResultCallback;
import xyz.eulix.space.interfaces.ResultCallbackObj;
import xyz.eulix.space.manager.ThumbManager;
import xyz.eulix.space.network.gateway.RealCallResult;
import xyz.eulix.space.network.interceptor.EulixGatewayInterceptor;
import xyz.eulix.space.transfer.TransferHelper;
import xyz.eulix.space.transfer.TransferProgressListener;
import xyz.eulix.space.transfer.calculator.TaskSpeed;
import xyz.eulix.space.transfer.db.TransferDBManager;
import xyz.eulix.space.transfer.event.TransferStateEvent;
import xyz.eulix.space.transfer.model.TransferItem;
import xyz.eulix.space.transfer.model.TransferItemFactory;
import xyz.eulix.space.util.AlbumNotifyHelper;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.network.OkHttpUtil;

/**
 * @author: chenjiawei
 * date: 2021/6/24 14:05
 */
public class FileListManager {
    private static final String TAG = FileListManager.class.getSimpleName();
    //文件校验开关
    private static final boolean FILE_CHECK_SWITCH = false;

    private static OkHttpClient generateOkHttpClient(String boxDomain, UUID requestId, String requestType, String accessToken, String secret, String transformation, String ivParams, String apiVersion) {
        return OkHttpUtil.generateOkHttpClient(true).newBuilder()
                .addInterceptor(new EulixGatewayInterceptor(boxDomain, requestId, requestType
                        , transformation, null, ivParams
                        , accessToken, secret, MediaType.parse("application/json; charset=utf-8")
                        , MediaType.parse("application/json; charset=utf-8"), apiVersion))
                .build();
    }

    private static OkHttpClient generateMediumTimeoutOkHttpClient(String boxDomain, UUID requestId, String requestType, String accessToken, String secret, String transformation, String ivParams, String apiVersion) {
        return OkHttpUtil.generateOkHttpClient(true).newBuilder()
                .connectTimeout(2, TimeUnit.MINUTES)
                .readTimeout(2, TimeUnit.MINUTES)
                .writeTimeout(2, TimeUnit.MINUTES)
                .addInterceptor(new EulixGatewayInterceptor(boxDomain, requestId, requestType
                        , transformation, null, ivParams
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

    private static UUID setUUIDMapping(UUID uuid) {
        UUID requestId = UUID.randomUUID();
        String fileId;
        if (uuid == null) {
            fileId = ConstantField.UUID.FILE_ROOT_UUID;
        } else {
            fileId = uuid.toString();
        }
        DataUtil.setRequestFileId(requestId.toString(), fileId);
        return requestId;
    }

    public static void getFileList(UUID uuid, Integer page, Integer pageSize, String order, String category, String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, IGetFileListCallback callback) {
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, setUUIDMapping(uuid), ConstantField.ServiceFunction.LIST_FOLDERS, accessToken, secret, transformation, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        FileListService service = retrofit.create(FileListService.class);
        Observable<GetFileListResponseBody> observable = service.getFileList((uuid == null ? null : uuid.toString()), page, pageSize, order, category);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<GetFileListResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(GetFileListResponseBody getFileListResponseBody) {
                        Logger.i(TAG, "on next: " + (getFileListResponseBody == null ? "null" : getFileListResponseBody.toString()));
                        if (callback != null) {
                            callback.onResult(getFileListResponseBody);
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

    public static void getFolderInfo(UUID uuid, String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, IFolderInfoCallback callback) {
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, setUUIDMapping(uuid), ConstantField.ServiceFunction.FOLDER_INFO, accessToken, secret, transformation, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        FileListService service = retrofit.create(FileListService.class);
        Observable<FolderInfoResponseBody> observable = service.getFolderInfo((uuid == null ? null : uuid.toString()));
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<FolderInfoResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(FolderInfoResponseBody folderInfoResponseBody) {
                        Logger.i(TAG, "on next: " + (folderInfoResponseBody == null ? "null" : folderInfoResponseBody.toString()));
                        if (callback != null) {
                            callback.onResult(folderInfoResponseBody);
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

    public static void searchFile(UUID uuid, String name, String category, Integer page, Integer pageSize, String order, String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, IGetFileListCallback callback) {
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, setUUIDMapping(uuid), ConstantField.ServiceFunction.SEARCH_FILES, accessToken, secret, transformation, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        FileListService service = retrofit.create(FileListService.class);
        Observable<GetFileListResponseBody> observable = service.searchFile((uuid == null ? null : uuid.toString()), name, category, page, pageSize, order);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<GetFileListResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(GetFileListResponseBody getFileListResponseBody) {
                        Logger.i(TAG, "on next: " + (getFileListResponseBody == null ? "null" : getFileListResponseBody.toString()));
                        if (callback != null) {
                            callback.onResult(getFileListResponseBody);
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

    public static void modifyFile(UUID uuid, String filename, String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, IFileRspCallback callback) {
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, setUUIDMapping(uuid), ConstantField.ServiceFunction.MODIFY_FILE, accessToken, secret, transformation, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        RenameFilesReq renameFilesReq = new RenameFilesReq();
        renameFilesReq.setUuid((uuid == null ? null : uuid.toString()));
        renameFilesReq.setFileName(filename);
        FileListService service = retrofit.create(FileListService.class);
        Observable<FileRsp> observable = service.modifyFile((uuid == null ? null : uuid.toString()), filename, renameFilesReq);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<FileRsp>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(FileRsp fileRsp) {
                        Logger.i(TAG, "on next: " + (fileRsp == null ? "null" : fileRsp.toString()));
                        if (callback != null) {
                            callback.onResult(fileRsp);
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

    public static void copyFile(CopyFilesReq copyFilesReq, UUID parentUUID, String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, IFileRspCallback callback) {
        OkHttpClient okHttpClient = generateMediumTimeoutOkHttpClient(boxDomain, setUUIDMapping(parentUUID), ConstantField.ServiceFunction.COPY_FILE, accessToken, secret, transformation, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        FileListService service = retrofit.create(FileListService.class);
        Observable<FileRsp> observable = service.copyFile(copyFilesReq);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<FileRsp>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(FileRsp fileRsp) {
                        Logger.i(TAG, "on next: " + (fileRsp == null ? "null" : fileRsp.toString()));
                        if (callback != null) {
                            callback.onResult(fileRsp);
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

    public static void moveFile(MoveFilesReq moveFilesReq, UUID parentUUID, String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, IFileRspCallback callback) {
        OkHttpClient okHttpClient = generateMediumTimeoutOkHttpClient(boxDomain, setUUIDMapping(parentUUID), ConstantField.ServiceFunction.MOVE_FILE, accessToken, secret, transformation, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        FileListService service = retrofit.create(FileListService.class);
        Observable<FileRsp> observable = service.moveFile(moveFilesReq);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<FileRsp>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(FileRsp fileRsp) {
                        Logger.i(TAG, "on next: " + (fileRsp == null ? "null" : fileRsp.toString()));
                        if (callback != null) {
                            callback.onResult(fileRsp);
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

    public static void deleteFile(FileUUIDs fileUUIDs, UUID parentUUID, String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, IFileRspCallback callback) {
        OkHttpClient okHttpClient = generateMediumTimeoutOkHttpClient(boxDomain, setUUIDMapping(parentUUID), ConstantField.ServiceFunction.DELETE_FILE, accessToken, secret, transformation, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        FileListService service = retrofit.create(FileListService.class);
        Observable<FileRsp> observable = service.deleteFile(fileUUIDs);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<FileRsp>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(FileRsp fileRsp) {
                        Logger.i(TAG, "on next: " + (fileRsp == null ? "null" : fileRsp.toString()));
                        if (callback != null) {
                            callback.onResult(fileRsp);
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

    //查询异步任务状态
    public static void checkAsyncTaskStatus(String taskId, String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, ResultCallbackObj callback) {
        UUID requestUUID = UUID.randomUUID();
        OkHttpClient okHttpClient = generateMediumTimeoutOkHttpClient(boxDomain, setUUIDMapping(requestUUID), ConstantField.ServiceFunction.ASYNC_TASK_INFO, accessToken, secret, transformation, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        FileListService service = retrofit.create(FileListService.class);
        Observable<AsyncTaskStatusResponseBody> observable = service.checkAsyncTaskStatus(taskId);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<AsyncTaskStatusResponseBody>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(AsyncTaskStatusResponseBody responseBody) {
                        Logger.i(TAG, "checkAsyncTaskStatus on next: " + (responseBody == null ? "null" : responseBody.toString()));
                        if (callback != null) {
                            callback.onResult(true, responseBody);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        String errMsg = (e == null ? "null" : (e.getMessage() == null ? "" : e.getMessage()));
                        Logger.e(TAG, "checkAsyncTaskStatus on error: " + errMsg);
                        if (callback != null) {
                            callback.onError(errMsg);
                        }
                    }

                    @Override
                    public void onComplete() {
                        Logger.d(TAG, "checkAsyncTaskStatus on complete");
                    }
                });
    }

    public static void createFolder(CreateFolderReq createFolderReq, UUID parentUUID, String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, INewFolderRspCallback callback) {
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, setUUIDMapping(parentUUID), ConstantField.ServiceFunction.CREATE_FOLDER, accessToken, secret, transformation, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        FileListService service = retrofit.create(FileListService.class);
        Observable<NewFolderRsp> observable = service.createFolder(createFolderReq);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<NewFolderRsp>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(NewFolderRsp newFolderRsp) {
                        Logger.i(TAG, "on next: " + (newFolderRsp == null ? "null" : newFolderRsp.toString()));
                        if (callback != null) {
                            callback.onResult(newFolderRsp);
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

    public static void getRecycledList(Integer page, Integer pageSize, String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, IRecycledListCallback callback) {
        OkHttpClient okHttpClient = generateOkHttpClient(boxDomain, UUID.randomUUID(), ConstantField.ServiceFunction.LIST_RECYCLED, accessToken, secret, transformation, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        FileListService service = retrofit.create(FileListService.class);
        Observable<RecycledListResponse> observable = service.getRecycledList(page, pageSize);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<RecycledListResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(RecycledListResponse recycledListResponse) {
                        Logger.i(TAG, "on next: " + (recycledListResponse == null ? "null" : recycledListResponse.toString()));
                        if (callback != null) {
                            callback.onResult(recycledListResponse);
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

    public static void restoreRecycledFile(FileUUIDs fileUUIDs, String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, IRecycledListCallback callback) {
        OkHttpClient okHttpClient = generateMediumTimeoutOkHttpClient(boxDomain, UUID.randomUUID(), ConstantField.ServiceFunction.RESTORE_RECYCLED, accessToken, secret, transformation, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        FileListService service = retrofit.create(FileListService.class);
        Observable<RecycledListResponse> observable = service.restoreRecycled(fileUUIDs);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<RecycledListResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(RecycledListResponse recycledListResponse) {
                        Logger.i(TAG, "on next: " + (recycledListResponse == null ? "null" : recycledListResponse.toString()));
                        if (callback != null) {
                            callback.onResult(recycledListResponse);
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

    public static void clearRecycledFile(FileUUIDs fileUUIDs, String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, IRecycledListCallback callback) {
        OkHttpClient okHttpClient = generateMediumTimeoutOkHttpClient(boxDomain, UUID.randomUUID(), ConstantField.ServiceFunction.CLEAR_RECYCLED, accessToken, secret, transformation, ivParams, apiVersion);
        String baseUrl = generateBaseUrl(boxDomain);
        Retrofit retrofit = generateRetrofit(baseUrl, okHttpClient);
        FileListService service = retrofit.create(FileListService.class);
        Observable<RecycledListResponse> observable = service.clearRecycled(fileUUIDs);
        observable.subscribeOn(Schedulers.trampoline())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<RecycledListResponse>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        Logger.d(TAG, "on subscribe");
                    }

                    @Override
                    public void onNext(RecycledListResponse recycledListResponse) {
                        Logger.i(TAG, "on next: " + (recycledListResponse == null ? "null" : recycledListResponse.toString()));
                        if (callback != null) {
                            callback.onResult(recycledListResponse);
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

    public static void downloadFile(String fileUuid, String filepath, String filename, long fileSize, final String md5, String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, Context context, boolean isCache, String from, ResultCallback callback) {
        if (context == null) {
            return;
        }

        Logger.d("zfy", "start download:" + filename);
        Logger.d("GarveyP2P", "http download single start");
        OkHttpClient okHttpClient = OkHttpUtil.generateOkHttpClient(true).newBuilder()
                .addInterceptor(new EulixGatewayInterceptor(boxDomain, setUUIDMapping(UUID.fromString(fileUuid)), ConstantField.ServiceFunction.DOWNLOAD_FILE
                        , transformation, null, ivParams
                        , accessToken, secret, MediaType.parse("application/json; charset=utf-8")
                        , MediaType.parse("*/*"), apiVersion))
                .build();
        //传输类型
        int transferType = isCache ? TransferHelper.TYPE_CACHE : TransferHelper.TYPE_DOWNLOAD;
        HttpUrl httpParseUrl = HttpUrl.parse(generateBaseUrl(boxDomain) + ConstantField.URL.DOWNLOAD_GATEWAY_API);
        if (httpParseUrl != null) {
            HttpUrl httpUrl = httpParseUrl.newBuilder()
                    .addQueryParameter("uuid", fileUuid)
                    .build();
            Logger.i(TAG, "download url: " + httpUrl);
            Request request = new Request.Builder()
                    .url(httpUrl)
                    .get()
                    .build();

            String nFilepath;
            if (isCache) {
                //缓存文件路径
                if (TransferHelper.FROM_ALBUM.equals(from)) {
                    //来自相册
                    nFilepath = context.getExternalCacheDir().getAbsolutePath() + "/album/cache/";
                } else {
                    nFilepath = context.getExternalCacheDir().getAbsolutePath() + "/cache/";
                }
            } else {
                //下载文件路径
                nFilepath = ConstantField.SDCARD_ROOT_PATH + File.separator + ConstantField.APP_PATH;
                if (filepath != null) {
                    int nLength = nFilepath.length();
                    if (filepath.startsWith("/")) {
                        if (nFilepath.endsWith("/")) {
                            nFilepath = nFilepath.substring(0, (nLength - 1));
                        }
                    } else {
                        if (!nFilepath.endsWith("/")) {
                            nFilepath = nFilepath + "/";
                        }
                    }
                    nFilepath = nFilepath + filepath;
                }
            }
            final String nFilePathFinal = nFilepath;
            final String uniqueTag = TransferItemFactory.getUniqueTag(transferType, null, null, null, fileUuid);
            //添加到数据库
            boolean isRestart = false;
            TransferItem dbItem = TransferDBManager.getInstance(context).queryByUniqueTag(uniqueTag, transferType);
            TransferItem item;
            if (dbItem != null) {
                //删除下载记录，重新下载
                dbItem.state = TransferHelper.STATE_DOING;
                dbItem.errorCode = 0;
                dbItem.currentSize = 0L;
                dbItem.localPath = nFilepath;
                item = dbItem;
                TransferDBManager.getInstance(context).updateTransferInfo(uniqueTag, dbItem, true);

            } else {
                item = TransferItemFactory.createStartTransferItem(filename, transferType, nFilepath, filepath, null, fileSize, System.currentTimeMillis(), fileUuid, md5, uniqueTag);
                TransferDBManager.getInstance(context).insert(item);
            }

            //发送消息通知刷新
            EventBusUtil.post(new TransferStateEvent(item.keyName, item.transferType, item.state, uniqueTag));
            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Logger.e(TAG, "on failure, e: " + e.getMessage());
                    TransferDBManager.getInstance(context).updateTransferState(filename, transferType, TransferHelper.STATE_ERROR, -1, null, true, uniqueTag);
                    if (callback != null) {
                        callback.onResult(false, e.getMessage());
                    }
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    Logger.i(TAG, "on response " + response.code());

                    if (!response.isSuccessful()) {
                        return;
                    }

                    Logger.d(TAG, "filepath: " + nFilePathFinal);
                    FileUtil.mkFile(nFilePathFinal);
                    File file = new File(nFilePathFinal, filename);
                    if (file.exists()) {
                        boolean result = file.delete();
                        Logger.d("zfy", "delete file result =" + result);
                    }
                    Headers headers = response.headers();
                    String contentType = headers.get("content-type");
                    Logger.d("zfy", "content-type = " + contentType);
                    if (TextUtils.isEmpty(contentType) || contentType.contains("json")) {
                        //没有文件流，下载文件失败
                        Logger.d("zfy", "no stream");
                        try {
                            String cipherResponseText = response.body().string();
                            if (!TextUtils.isEmpty(cipherResponseText)) {
                                RealCallResult realCallResult = null;
                                realCallResult = new Gson().fromJson(cipherResponseText, RealCallResult.class);
                                if (realCallResult != null) {
                                    String body = realCallResult.getBody();
                                    String decryptBody = EncryptionUtil.decrypt(transformation, null, body, secret, StandardCharsets.UTF_8, ivParams);
                                    Logger.d("zfy", "decryptBody=" + decryptBody);
                                }
                            }
                        } catch (Exception e) {
                            Logger.d("zfy", "exception " + e.getMessage());
                        }
                        TransferDBManager.getInstance(context).updateTransferState(filename, transferType, TransferHelper.STATE_ERROR, 0, null, true, uniqueTag);
                    } else {
                        //文件大小
                        String headerFileSizeStr = headers.get("file-size");
                        Logger.d("zfy", "file-size=" + headerFileSizeStr);
                        if (TextUtils.isEmpty(headerFileSizeStr)) {
                            headerFileSizeStr = "0";
                        }
                        long headerFileSize = Long.parseLong(headerFileSizeStr);
                        InputStream inputStream = response.body().byteStream();

                        TransferProgressListener progressListener = (currentSize, totalSize, appendSize, isPercentChange, isResume) -> {
                            if (isPercentChange) {
                                Logger.d("zfy", "currentDownSize=" + currentSize + ",totalSize=" + totalSize);
                                //回调返回的totalSize为加密文件的值，与实际文件大小有区别
                                if (currentSize > fileSize) {
                                    currentSize = fileSize;
                                }
                                TransferDBManager.getInstance(context).updateTransferSize(filename, transferType, currentSize, fileSize, true, uniqueTag);
                            }
                            if (transferType == TransferHelper.TYPE_DOWNLOAD) {
                                TaskSpeed.getInstance().start();
                                TaskSpeed.getInstance().appendDataLength(uniqueTag, appendSize);
                            }
                        };
                        //解密并保存
                        File decryptFile = EncryptionUtil.decrypt(transformation, null, inputStream,
                                secret, StandardCharsets.UTF_8, ivParams, nFilePathFinal, filename, headerFileSize, progressListener);
                        if (transferType == TransferHelper.TYPE_DOWNLOAD) {
                            TaskSpeed.getInstance().removeTask(uniqueTag);
                        }
                        if (decryptFile != null) {
                            //下载解析成功，size校验
                            long tempFileSize = decryptFile.length();
                            Logger.d("zfy", "downloadSize=" + tempFileSize);
                            Logger.d("zfy", "targetSize=" + fileSize);

                            if (FILE_CHECK_SWITCH) {
                                if (tempFileSize == fileSize) {
                                    Logger.d("zfy", "size校验通过");
                                    TransferDBManager.getInstance(context).updateTransferState(filename, transferType, TransferHelper.STATE_FINISH, 0, null, true, uniqueTag);
                                    if (!isCache) {
                                        AlbumNotifyHelper.insertToAlbum(context, decryptFile);
                                    }
                                    if (callback != null) {
                                        callback.onResult(true, decryptFile.getAbsolutePath());
                                    }
                                } else {
                                    Logger.d("zfy", "size校验失败");
                                    boolean result = decryptFile.delete();
                                    Logger.d("zfy", "decrypt file delete: " + result);
                                    TransferDBManager.getInstance(context).updateTransferState(filename, transferType, TransferHelper.STATE_ERROR, -1, null, true, uniqueTag);
                                    if (callback != null) {
                                        callback.onResult(false, "size校验失败");
                                    }
                                }
                            } else {
                                Logger.d("zfy", "下载完成");
                                TransferDBManager.getInstance(context).updateTransferState(filename, transferType, TransferHelper.STATE_FINISH, 0, null, true, uniqueTag);
                                if (!isCache) {
                                    AlbumNotifyHelper.insertToAlbum(context, decryptFile);
                                }
                                if (callback != null) {
                                    callback.onResult(true, decryptFile.getAbsolutePath());
                                }
                            }
                        } else {
                            //下载解析失败
                            TransferDBManager.getInstance(context).updateTransferState(filename, transferType, TransferHelper.STATE_ERROR, -1, null, true, uniqueTag);
                            if (callback != null) {
                                callback.onResult(false, "下载解析失败");
                            }
                        }
                    }
                }
            });
        }
        Logger.d("GarveyP2P", "http download single end");
    }

    //下载缩略图
    public static void downloadThumb(UUID uuid, String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, Context context, String from, ThumbCacheCallback callback) {
        if (context == null) {
            return;
        }
        OkHttpClient okHttpClient = OkHttpUtil.generateOkHttpClient(true).newBuilder()
                .addInterceptor(new EulixGatewayInterceptor(boxDomain, setUUIDMapping(uuid), ConstantField.ServiceFunction.DOWNLOAD_THUMBNAILS
                        , ConstantField.Algorithm.Transformation.AES_CBC_PKCS5, null, ivParams
                        , accessToken, secret, MediaType.parse("application/json; charset=utf-8")
                        , MediaType.parse("*/*"), apiVersion))
                .build();
        HttpUrl httpParseUrl = HttpUrl.parse(generateBaseUrl(boxDomain) + ConstantField.URL.DOWNLOAD_GATEWAY_API);
        if (httpParseUrl != null) {
            HttpUrl httpUrl = httpParseUrl.newBuilder()
                    .addQueryParameter("uuid", uuid.toString())
                    .build();
            Logger.i(TAG, "download url: " + httpUrl);
            Request request = new Request.Builder()
                    .url(httpUrl)
                    .get()
                    .build();

            //缩略图保存路径
            String nFilepath;
            if (TransferHelper.FROM_ALBUM.equals(from)) {
                nFilepath = context.getExternalCacheDir().getAbsolutePath() + "/album/thumbs/";
            } else {
                nFilepath = context.getExternalCacheDir().getAbsolutePath() + "/thumbs/";
            }

            File folder = new File(nFilepath);
            if (!folder.exists()) {
                folder.mkdir();
            }

            final String nFilePathFinal = nFilepath;

            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Logger.e("zfy", "get thumb on failure, e: " + e.getMessage());
                    if (callback != null) {
                        callback.onError("response not stream");
                    }
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    Logger.i("zfy", "get thumb on response " + response.code());
                    if (!response.isSuccessful()) {
                        return;
                    }
                    Headers headers = response.headers();
                    String contentType = headers.get("content-type");
                    Logger.d("zfy", "thumb content-type = " + contentType);
                    if (!TextUtils.isEmpty(contentType) && (contentType.contains("stream") || contentType.contains("image"))) {
                        //文件大小
                        String fileSizeStr = headers.get("file-size");
                        Logger.d("zfy", "file-size=" + fileSizeStr);
                        long fileSize = Long.parseLong(fileSizeStr);
                        //文件名称等(inline; filename="header_chosen.jpg"; filename*=UTF-8''header_chosen.jpg)
                        String contentDisposition = headers.get("content-disposition");
                        Logger.d("zfy", "content-disposition=" + contentDisposition);
                        String suffix = "jpg";
                        if (!TextUtils.isEmpty(contentDisposition)) {
                            int contentDotIndex = contentDisposition.lastIndexOf(".");
                            suffix = contentDisposition.substring(contentDotIndex + 1);
                        }
                        Logger.d(TAG, "filepath: " + nFilePathFinal);
                        FileUtil.mkFile(nFilePathFinal);
                        String saveFileName = uuid.toString() + "." + suffix;
                        Logger.d("zfy", "saveFileName = " + saveFileName);
                        File file = new File(nFilePathFinal, saveFileName);
                        if (file.exists()) {
                            boolean result = file.delete();
                            Logger.d("zfy", "delete file result =" + result);
                        }

                        InputStream inputStream = response.body().byteStream();
                        //解密并保存
                        String tempFileName = "temp_" + saveFileName;
                        File decryptFile = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5, null, inputStream,
                                secret, StandardCharsets.UTF_8, ivParams, nFilePathFinal, tempFileName, fileSize, null);
                        if (decryptFile != null) {
                            Logger.d("zfy", "get thumb success." + decryptFile.getAbsolutePath());
                            //获取完成，修改名称
                            File finalFile = new File(nFilePathFinal, saveFileName);
                            decryptFile.renameTo(finalFile);
                            ThumbManager.getInstance().insertLocalThumbPath(uuid.toString(), finalFile.getAbsolutePath());
                            if (callback != null) {
                                callback.onResult(uuid.toString(), finalFile.getAbsolutePath());
                            }
                        } else {
                            if (callback != null) {
                                callback.onError("decryptFile is null");
                            }
                        }
                    } else {//没有文件流，下载文件失败
                        Logger.d("zfy", "no stream");
                        if (callback != null) {
                            callback.onError("response not stream");
                        }
                    }
                }
            });
        }
    }

    //下载压缩图
    public static void downloadCompressed(UUID uuid, String filename, String boxDomain, String accessToken, String secret, String ivParams, String apiVersion, Context context, String from, GetCompressedImageCallback listener) {
        if (context == null) {
            return;
        }
        OkHttpClient okHttpClient = OkHttpUtil.generateOkHttpClient(true).newBuilder()
                .addInterceptor(new EulixGatewayInterceptor(boxDomain, setUUIDMapping(uuid), ConstantField.ServiceFunction.DOWNLOAD_COMPRESSED
                        , ConstantField.Algorithm.Transformation.AES_CBC_PKCS5, null, ivParams
                        , accessToken, secret, MediaType.parse("application/json; charset=utf-8")
                        , MediaType.parse("*/*"), apiVersion))
                .build();
        HttpUrl httpParseUrl = HttpUrl.parse(generateBaseUrl(boxDomain) + ConstantField.URL.DOWNLOAD_GATEWAY_API);
        if (httpParseUrl != null) {
            HttpUrl httpUrl = httpParseUrl.newBuilder()
                    .addQueryParameter("uuid", uuid.toString())
                    .build();
            Logger.i(TAG, "download compressed url: " + httpUrl);
            Request request = new Request.Builder()
                    .url(httpUrl)
                    .get()
                    .build();

            //下载文件路径
            String nFilepath;
            if (TransferHelper.FROM_ALBUM.equals(from)) {
                nFilepath = context.getExternalCacheDir().getAbsolutePath() + "/album/compressed/";
            } else {
                nFilepath = context.getExternalCacheDir().getAbsolutePath() + "/compressed/";
            }

            final String uniqueTag = uuid.toString();

            File folder = new File(nFilepath);
            if (!folder.exists()) {
                folder.mkdir();
            }

            final String nFilePathFinal = nFilepath;

            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Logger.e("zfy", "get compressed on failure, e: " + e.getMessage());
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) {
                    Logger.i("zfy", "on response " + response.code());
                    if (!response.isSuccessful()) {
                        if (listener != null) {
                            listener.onResult(false, null);
                        }
                        return;
                    }
                    Headers headers = response.headers();
                    String contentType = headers.get("content-type");
                    Logger.d("zfy", "content-type = " + contentType);
                    if (!TextUtils.isEmpty(contentType) && (contentType.contains("stream") || contentType.contains("image"))) {
                        //文件大小
                        String fileSizeStr = headers.get("file-size");
                        Logger.d("zfy", "file-size=" + fileSizeStr);
                        long fileSize = Long.parseLong(fileSizeStr);
                        //文件名称等(inline; filename="header_chosen.jpg"; filename*=UTF-8''header_chosen.jpg)
                        String contentDisposition = headers.get("content-disposition");
                        Logger.d("zfy", "content-disposition=" + contentDisposition);
                        String suffix = "";
                        if (!TextUtils.isEmpty(filename)) {
                            int typeIndex = filename.lastIndexOf(".");
                            suffix = filename.substring(typeIndex + 1);
                        }
                        if (!TextUtils.isEmpty(contentDisposition)) {
                            int contentDotIndex = contentDisposition.lastIndexOf(".");
                            suffix = contentDisposition.substring(contentDotIndex + 1);
                        }
                        Logger.d(TAG, "filepath: " + nFilePathFinal);
                        FileUtil.mkFile(nFilePathFinal);
                        String saveFileName = uuid + "." + suffix;
                        Logger.d("zfy", "saveFileName = " + saveFileName);
                        File file = new File(nFilePathFinal, saveFileName);

                        if (file.exists()) {
                            boolean result = file.delete();
                            Logger.d("zfy", "delete file result =" + result);
                        }

                        InputStream inputStream = response.body().byteStream();
                        TransferProgressListener progressListener = (currentSize, totalSize, appendSize, isPercentChange, isResume) -> {
                            Logger.d("zfy", "currentDownSize=" + currentSize + ",totalSize=" + totalSize);
                            if (isPercentChange) {
                                TransferDBManager.getInstance(context).updateTransferSize(filename, TransferHelper.TYPE_CACHE, currentSize, totalSize, true, uniqueTag);
                            }
                        };
                        //解密并保存
                        String tempFileName = "temp_" + saveFileName;
                        File decryptFile = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5, null, inputStream,
                                secret, StandardCharsets.UTF_8, ivParams, nFilePathFinal, tempFileName, fileSize, progressListener);
                        if (decryptFile != null) {
                            //获取完成，修改名称
                            Logger.d("zfy", "get compressed image success." + decryptFile.getAbsolutePath());
                            File finalFile = new File(nFilePathFinal, saveFileName);
                            decryptFile.renameTo(finalFile);
                            ThumbManager.getInstance().insertLocalCompressPath(uuid.toString(), finalFile.getAbsolutePath());
                            //回调
                            if (listener != null) {
                                listener.onResult(true, finalFile.getAbsolutePath());
                            }
                        } else if (listener != null) {
                            listener.onResult(false, null);
                        }
                    } else {
                        //没有文件流，下载文件失败
                        Logger.d("zfy", "no stream:" + response.body().toString());
                        listener.onResult(false, null);
                    }
                }
            });
        }
    }

}
