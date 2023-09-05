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
import android.os.Environment;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.common.io.Files;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.event.UploadedFileEvent;
import xyz.eulix.space.interfaces.ResultCallback;
import xyz.eulix.space.interfaces.ResultCallbackObj;
import xyz.eulix.space.manager.AlreadyUploadedManager;
import xyz.eulix.space.transfer.TransferHelper;
import xyz.eulix.space.transfer.calculator.TaskSpeed;
import xyz.eulix.space.transfer.db.TransferDBManager;
import xyz.eulix.space.transfer.model.TransferItem;
import xyz.eulix.space.transfer.model.TransferItemFactory;
import xyz.eulix.space.transfer.multipart.BetagCalculator;
import xyz.eulix.space.transfer.multipart.task.MultipartDownloadTask;
import xyz.eulix.space.transfer.multipart.task.MultipartUploadTask;
import xyz.eulix.space.util.AlbumNotifyHelper;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FailCodeUtil;
import xyz.eulix.space.util.GatewayUtils;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.MD5Util;
import xyz.eulix.space.util.ThreadPool;
import xyz.eulix.space.util.ThumbThreadPool;
import xyz.eulix.space.util.TransferThreadPool;
import xyz.eulix.space.util.Urls;

/**
 * @author: chenjiawei
 * date: 2021/6/24 14:24
 */
public class FileListUtil {
    private static final String TAG = FileListUtil.class.getSimpleName();
    private static final String API_VERSION = ConstantField.BoxVersionName.VERSION_0_2_0;

    //下载文件使用分片，大于该值使用分片
    private static final long DOWNLOAD_MULTIPART_LIMIT_SIZE = 8 * 1024 * 1024;
    //上传文件使用分片，大于该值使用分片
    private static final long UPLOAD_MULTIPART_LIMIT_SIZE = 8 * 1024 * 1024;

    private FileListUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    private static ConcurrentHashMap<String, MultipartUploadTask> mMultiUploadTasks = new ConcurrentHashMap<>();

    private static String getFileId(String requestId) {
        String fileId = DataUtil.removeRequestId(requestId);
        if (fileId == null) {
            fileId = ConstantField.UUID.FILE_ROOT_UUID;
        }
        return fileId;
    }

    private static void updateEulixSpaceDB(Context context, String boxUuid, String boxBind, String currentId, GetFileListResponseResult result) {
        if (result != null) {
            updateEulixSpaceDB(context, boxUuid, boxBind, currentId, result.getFileList(), result.getPageInfo());
        }
    }

    private static void updateEulixSpaceDB(Context context, String boxUuid, String boxBind, String currentId, RecycledListResult result) {
        if (result != null) {
            updateEulixSpaceDB(context, boxUuid, boxBind, currentId, result.getFileList(), result.getPageInfo());
        }
    }

    private static void updateEulixSpaceDB(Context context, String boxUuid, String boxBind, String currentId, List<FileListItem> fileListItemList, PageInfo pageInfo) {
        if (context != null && currentId != null) {
            String fileListValue = null;
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, queryMap);
            if (boxValues != null) {
                for (Map<String, String> boxV : boxValues) {
                    if (boxV != null && boxV.containsKey(EulixSpaceDBManager.FIELD_BOX_FILE_LIST)) {
                        fileListValue = boxV.get(EulixSpaceDBManager.FIELD_BOX_FILE_LIST);
                        break;
                    }
                }
            }
            Map<String, List<FileListItem>> fileListMap = null;
            if (fileListValue != null) {
                try {
                    fileListMap = new Gson().fromJson(fileListValue, new TypeToken<Map<String, List<FileListItem>>>() {
                    }.getType());
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
            if (fileListMap == null) {
                fileListMap = new HashMap<>();
            }
            List<FileListItem> fileListItems = null;
            if (fileListMap.containsKey(currentId)) {
                fileListItems = fileListMap.get(currentId);
            }
            if (fileListItems == null) {
                fileListItems = new ArrayList<>();
            }
            if (pageInfo == null || pageInfo.getPage() == null || pageInfo.getPage() == 1) {
                fileListItems.clear();
            }
            if (fileListItemList != null) {
                for (FileListItem fileListItem : fileListItemList) {
                    if (fileListItem != null) {
                        fileListItems.add(fileListItem);
                    }
                }
            }
            DataUtil.setFileListsMap(boxUuid, boxBind, currentId, fileListItems);
            fileListMap.put(currentId, fileListItems);
            Map<String, String> boxValue = new HashMap<>();
            boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            boxValue.put(EulixSpaceDBManager.FIELD_BOX_FILE_LIST, new Gson().toJson(fileListMap, new TypeToken<Map<String, List<FileListItem>>>() {
            }.getType()));
            EulixSpaceDBUtil.updateBox(context, boxValue);
        }
    }

    private static void updateEulixSpaceDB(Context context, String boxUuid, String boxBind, String currentId, String parentId, FolderInfoResult result) {
        if (context != null && result != null && currentId != null && parentId != null) {
            boolean isUpdate = false;
            String fileListValue = null;
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, queryMap);
            if (boxValues != null) {
                for (Map<String, String> boxV : boxValues) {
                    if (boxV != null && boxV.containsKey(EulixSpaceDBManager.FIELD_BOX_FILE_LIST)) {
                        fileListValue = boxV.get(EulixSpaceDBManager.FIELD_BOX_FILE_LIST);
                        break;
                    }
                }
            }
            Map<String, List<FileListItem>> fileListMap = null;
            if (fileListValue != null) {
                try {
                    fileListMap = new Gson().fromJson(fileListValue, new TypeToken<Map<String, List<FileListItem>>>() {
                    }.getType());
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
            List<FileListItem> fileListItems = null;
            if (fileListMap != null && fileListMap.containsKey(parentId)) {
                fileListItems = fileListMap.get(parentId);
                if (fileListItems != null) {
                    for (FileListItem fileListItem : fileListItems) {
                        if (fileListItem != null && currentId.equals(fileListItem.getUuid())) {
                            Long folderSize = result.getSize();
                            if (folderSize != null) {
                                fileListItem.setSize(folderSize);
                                isUpdate = true;
                            }
                            break;
                        }
                    }
                }
            }
            if (isUpdate) {
                DataUtil.setFileListsMap(boxUuid, boxBind, parentId, fileListItems);
                fileListMap.put(parentId, fileListItems);
                Map<String, String> boxValue = new HashMap<>();
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_FILE_LIST, new Gson().toJson(fileListMap, new TypeToken<Map<String, List<FileListItem>>>() {
                }.getType()));
                EulixSpaceDBUtil.updateBox(context, boxValue);
            }
        }
    }

    public static void getFileList(@NonNull Context context, String boxUuid, String boxBind, UUID uuid, Integer page, Integer pageSize, String order, String category, String boxDomain, String accessToken, String secret, String transformation, String ivParams, boolean isLAN, GetFileListCallback callback) {
        getFileList(context, boxUuid, boxBind, uuid, page, pageSize, order, category, boxDomain, accessToken, secret, transformation, ivParams, isLAN, false, callback);
    }

    // server exception handle
    public static void getFileList(@NonNull Context context, String boxUuid, String boxBind, UUID uuid, Integer page, Integer pageSize, String order, String category, String boxDomain, String accessToken, String secret, String transformation, String ivParams, boolean isLAN, boolean isFore, GetFileListCallback callback) {

        String finalBoxDomain = Urls.getBaseUrl();
        try {
            ThreadPool.getInstance().execute(() -> FileListManager.getFileList(uuid, page, pageSize, order, category, finalBoxDomain, accessToken, secret, transformation, ivParams, API_VERSION, new IGetFileListCallback() {
                @Override
                public void onResult(GetFileListResponseBody result) {
                    Logger.i(TAG, "on result: " + result);
                    if (result == null) {
                        if (callback != null) {
                            callback.onFailed(null, null, null);
                        }
                    } else {
                        String fileId = getFileId(result.getRequestId());
                        String currentId = fileId;
                        GetFileListResponseResult responseResult = result.getResults();
                        if (responseResult != null) {
                            if (ConstantField.UUID.FILE_ROOT_UUID.equals(fileId)) {
                                if (category == null) {
                                    currentId = ConstantField.Category.FILE_ROOT;
                                } else {
                                    switch (category) {
                                        case ConstantField.Category.PICTURE:
                                            currentId = ConstantField.Category.FILE_IMAGE;
                                            break;
                                        case ConstantField.Category.VIDEO:
                                            currentId = ConstantField.Category.FILE_VIDEO;
                                            break;
                                        case ConstantField.Category.DOCUMENT:
                                            currentId = ConstantField.Category.FILE_DOCUMENT;
                                            break;
                                        case ConstantField.Category.OTHER:
                                            currentId = ConstantField.Category.FILE_OTHER;
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            }
                            updateEulixSpaceDB(context, boxUuid, boxBind, currentId, responseResult);
                        }
                        if (callback != null) {
                            callback.onSuccess(result.getCodeInt(), result.getMessage(), fileId
                                    , (responseResult == null ? null : responseResult.getFileList())
                                    , (responseResult == null ? null : responseResult.getPageInfo())
                                    , (responseResult == null ? null : responseResult.getFileCount()));
                        }
                    }
                }

                @Override
                public void onError(String msg) {
                    Logger.e(TAG, "on error: " + msg);
                    if (callback != null) {
                        callback.onError(msg);
                    }
                }
            }), isFore);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void getFolderInfo(@NonNull Context context, String boxUuid, String boxBind, UUID uuid, String parentId, String boxDomain, String accessToken, String secret, String transformation, String ivParams, boolean isLAN, FolderInfoCallback callback) {

        String finalBoxDomain = Urls.getBaseUrl();
        ThreadPool.getInstance().execute(() -> FileListManager.getFolderInfo(uuid, finalBoxDomain, accessToken, secret, transformation, ivParams, API_VERSION, new IFolderInfoCallback() {
            @Override
            public void onResult(FolderInfoResponseBody result) {
                Logger.i(TAG, "on result: " + result);
                if (result == null) {
                    if (callback != null) {
                        callback.onFailed(null, null, null);
                    }
                } else {
                    String fileId = getFileId(result.getRequestId());
                    FolderInfoResult folderInfoResult = result.getResults();
                    String name = null;
                    Long operationAt = null;
                    String path = null;
                    Long size = null;
                    if (folderInfoResult != null) {
                        name = folderInfoResult.getName();
                        operationAt = folderInfoResult.getOperationAt();
                        path = folderInfoResult.getPath();
                        size = folderInfoResult.getSize();
                        updateEulixSpaceDB(context, boxUuid, boxBind, fileId, parentId, folderInfoResult);
                    }
                    if (callback != null) {
                        callback.onSuccess(result.getCodeInt(), result.getMessage(), fileId
                                , name, operationAt, path, size);
                    }
                }
            }

            @Override
            public void onError(String msg) {
                Logger.e(TAG, "on error: " + msg);
                if (callback != null) {
                    callback.onError(msg);
                }
            }
        }));
    }

    public static void searchFile(@NonNull Context context, String boxUuid, UUID uuid, String name, String category, Integer page, Integer pageSize, String order, String boxDomain, String accessToken, String secret, String transformation, String ivParams, boolean isLAN, GetFileListCallback callback) {
        searchFile(context, boxUuid, uuid, name, category, page, pageSize, order, boxDomain, accessToken, secret, transformation, ivParams, isLAN, false, callback);
    }

    // server exception handle
    public static void searchFile(@NonNull Context context, String boxUuid, UUID uuid, String name, String category, Integer page, Integer pageSize, String order, String boxDomain, String accessToken, String secret, String transformation, String ivParams, boolean isLAN, boolean isFore, GetFileListCallback callback) {

        String finalBoxDomain = Urls.getBaseUrl();
        try {
            ThreadPool.getInstance().execute(() -> FileListManager.searchFile(uuid, name, category, page, pageSize, order, finalBoxDomain, accessToken, secret, transformation, ivParams, API_VERSION, new IGetFileListCallback() {
                @Override
                public void onResult(GetFileListResponseBody result) {
                    Logger.i(TAG, "on result: " + result);
                    if (result == null) {
                        if (callback != null) {
                            callback.onFailed(null, null, null);
                        }
                    } else {
                        GetFileListResponseResult responseResult = result.getResults();
                        Integer fileCount = null;
                        if (responseResult != null) {
                            fileCount = responseResult.getFileCount();
                            PageInfo pageInfo = responseResult.getPageInfo();
                            if (pageInfo != null) {
                                fileCount = pageInfo.getCount();
                            }
                        }
                        String fileId = getFileId(result.getRequestId());
                        String currentId = fileId;
                        if (ConstantField.UUID.FILE_ROOT_UUID.equals(fileId)) {
                            if (category == null) {
                                currentId = ConstantField.Category.FILE_ROOT;
                            } else {
                                switch (category) {
                                    case ConstantField.Category.PICTURE:
                                        currentId = ConstantField.Category.FILE_IMAGE;
                                        break;
                                    case ConstantField.Category.VIDEO:
                                        currentId = ConstantField.Category.FILE_VIDEO;
                                        break;
                                    case ConstantField.Category.DOCUMENT:
                                        currentId = ConstantField.Category.FILE_DOCUMENT;
                                        break;
                                    case ConstantField.Category.OTHER:
                                        currentId = ConstantField.Category.FILE_OTHER;
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
                        if (callback != null) {
                            callback.onSuccess(result.getCodeInt(), result.getMessage(), currentId
                                    , (responseResult == null ? null : responseResult.getFileList())
                                    , (responseResult == null ? null : responseResult.getPageInfo())
                                    , fileCount);
                        }
                    }
                }

                @Override
                public void onError(String msg) {
                    Logger.e(TAG, "on error: " + msg);
                    if (callback != null) {
                        callback.onError(msg);
                    }
                }
            }), isFore);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    // server exception handle
    public static void modifyFile(@NonNull Context context, String boxUuid, UUID uuid, String filename, String boxDomain, String accessToken, String secret, String transformation, String ivParams, boolean isLAN, FileRspCallback callback) {

        String finalBoxDomain = Urls.getBaseUrl();
        try {
            ThreadPool.getInstance().execute(() -> FileListManager.modifyFile(uuid, filename, finalBoxDomain, accessToken, secret, transformation, ivParams, API_VERSION, new IFileRspCallback() {
                @Override
                public void onResult(FileRsp result) {
                    Logger.i(TAG, "on result: " + result);
                    if (result == null) {
                        if (callback != null) {
                            callback.onFailed(null, null, null);
                        }
                    } else {
                        String message = result.getMessage();
                        String codeValue = result.getCode();
                        Logger.d(TAG, "code value: " + codeValue + ", message: " + message);
                        int code = -1;
                        if (codeValue == null && message != null && "ok".equalsIgnoreCase(message.trim())) {
                            code = 200;
                        } else {
                            code = DataUtil.stringCodeToInt(codeValue);
                        }
                        if (callback != null) {
                            if (code >= 0 && code < 400) {
                                BaseResponseResult baseResponseResult = result.getResults();
                                callback.onSuccess(code, message, getFileId(result.getRequestId())
                                        , (baseResponseResult == null ? null : baseResponseResult.affectRows), null);
                            } else {
                                callback.onFailed(code, message, getFileId(result.getRequestId()));
                            }
                        }
                    }
                }

                @Override
                public void onError(String msg) {
                    Logger.e(TAG, "on error: " + msg);
                    if (callback != null) {
                        callback.onError(msg);
                    }
                }
            }), true);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    // server exception handle
    public static void copyFile(@NonNull Context context, String boxUuid, List<UUID> uuids, UUID parentUUID, String destination, String boxDomain, String accessToken, String secret, String transformation, String ivParams, boolean isLAN, FileRspCallback callback) {
        CopyFilesReq copyFilesReq = new CopyFilesReq();
        copyFilesReq.setDstPath(destination);
        if (uuids != null) {
            List<String> uuidList = new ArrayList<>();
            for (UUID uuid : uuids) {
                if (uuid != null) {
                    uuidList.add(uuid.toString());
                }
            }
            copyFilesReq.setUuids(uuidList);
        }
        String domainUrl = boxDomain;

        String finalBoxDomain = Urls.getBaseUrl();
        try {
            ThreadPool.getInstance().execute(() -> FileListManager.copyFile(copyFilesReq, parentUUID, finalBoxDomain, accessToken, secret, transformation, ivParams, API_VERSION, new IFileRspCallback() {
                @Override
                public void onResult(FileRsp result) {
                    Logger.i(TAG, "on result: " + result);
                    if (result == null) {
                        if (callback != null) {
                            callback.onFailed(null, null, null);
                        }
                    } else {
                        String message = result.getMessage();
                        String codeValue = result.getCode();
                        int code = -1;
                        if (codeValue == null && message != null && "ok".equalsIgnoreCase(message.trim())) {
                            code = 200;
                        } else {
                            code = DataUtil.stringCodeToInt(codeValue);
                        }
                        if (callback != null) {
                            if (code >= 0 && code < 400) {
                                BaseResponseResult baseResponseResult = result.getResults();
                                callback.onSuccess(code, message, getFileId(result.getRequestId())
                                        , (baseResponseResult == null ? null : baseResponseResult.affectRows), null);
                            } else {
                                callback.onFailed(code, message, getFileId(result.getRequestId()));
                            }
                        }
                    }
                }

                @Override
                public void onError(String msg) {
                    Logger.e(TAG, "on error: " + msg);
                    if (callback != null) {
                        callback.onError(msg);
                    }
                }
            }), true);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    // server exception handle
    public static void moveFile(@NonNull Context context, String boxUuid, List<UUID> uuids, UUID parentUUID, String destination, String boxDomain, String accessToken, String secret, String transformation, String ivParams, boolean isLAN, FileRspCallback callback) {
        MoveFilesReq moveFilesReq = new MoveFilesReq();
        moveFilesReq.setDestPath(destination);
        if (uuids != null) {
            List<String> uuidList = new ArrayList<>();
            for (UUID uuid : uuids) {
                if (uuid != null) {
                    uuidList.add(uuid.toString());
                }
            }
            moveFilesReq.setUuids(uuidList);
        }

        String finalBoxDomain = Urls.getBaseUrl();
        try {
            ThreadPool.getInstance().execute(() -> FileListManager.moveFile(moveFilesReq, parentUUID, finalBoxDomain, accessToken, secret, transformation, ivParams, API_VERSION, new IFileRspCallback() {
                @Override
                public void onResult(FileRsp result) {
                    Logger.i(TAG, "on result: " + result);
                    if (result == null) {
                        if (callback != null) {
                            callback.onFailed(null, null, null);
                        }
                    } else {
                        String message = result.getMessage();
                        String codeValue = result.getCode();
                        int code = -1;
                        if (codeValue == null && message != null && "ok".equalsIgnoreCase(message.trim())) {
                            code = 200;
                        } else {
                            code = DataUtil.stringCodeToInt(codeValue);
                        }
                        if (callback != null) {
                            if (code >= 0 && code < 400) {
                                BaseResponseResult baseResponseResult = result.getResults();
                                callback.onSuccess(code, message, getFileId(result.getRequestId())
                                        , (baseResponseResult == null ? null : baseResponseResult.affectRows), null);
                            } else {
                                callback.onFailed(code, message, getFileId(result.getRequestId()));
                            }
                        }
                    }
                }

                @Override
                public void onError(String msg) {
                    Logger.e(TAG, "on error: " + msg);
                    if (callback != null) {
                        callback.onError(msg);
                    }
                }
            }), true);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    // server exception handle
    public static void deleteFile(@NonNull Context context, String boxUuid, List<UUID> uuids, UUID parentUUID, String boxDomain, String accessToken, String secret, String transformation, String ivParams, boolean isLAN, FileRspCallback callback) {
        FileUUIDs fileUUIDs = new FileUUIDs();
        if (uuids != null) {
            List<String> uuidList = new ArrayList<>();
            for (UUID uuid : uuids) {
                if (uuid != null) {
                    uuidList.add(uuid.toString());
                }
            }
            fileUUIDs.setUuids(uuidList);
        }

        String finalBoxDomain = Urls.getBaseUrl();
        try {
            ThreadPool.getInstance().execute(() -> FileListManager.deleteFile(fileUUIDs, parentUUID, finalBoxDomain, accessToken, secret, transformation, ivParams, API_VERSION, new IFileRspCallback() {
                @Override
                public void onResult(FileRsp result) {
                    Logger.i(TAG, "on result: " + result);
                    if (result == null) {
                        if (callback != null) {
                            callback.onFailed(null, null, null);
                        }
                    } else {
                        String message = result.getMessage();
                        String codeValue = result.getCode();
                        int code = -1;
                        if (codeValue == null && message != null && "ok".equalsIgnoreCase(message.trim())) {
                            code = 200;
                        } else {
                            code = DataUtil.stringCodeToInt(codeValue);
                        }
                        if (callback != null) {
                            if (code >= 0 && code < 400) {
                                BaseResponseResult baseResponseResult = result.getResults();
                                callback.onSuccess(code, message, getFileId(result.getRequestId())
                                        , (baseResponseResult == null ? null : baseResponseResult.affectRows), result);
                            } else {
                                callback.onFailed(code, message, getFileId(result.getRequestId()));
                            }
                        }
                    }
                }

                @Override
                public void onError(String msg) {
                    Logger.e(TAG, "on error: " + msg);
                    if (callback != null) {
                        callback.onError(msg);
                    }
                }
            }), true);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    //查询异步任务状态
    public static void checkAsyncTaskStatus(Context context, String taskId, ResultCallbackObj callback) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            String baseUrl = Urls.getBaseUrl();
            ThreadPool.getInstance().execute(() -> FileListManager.checkAsyncTaskStatus(taskId,
                    baseUrl, gatewayCommunicationBase.getAccessToken(),
                    gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getTransformation(),
                    gatewayCommunicationBase.getIvParams(), ConstantField.BoxVersionName.VERSION_0_1_0, callback));
        } else if (callback != null) {
            callback.onError("gatewayCommunicationBase is null");
        }
    }

    // server exception handle
    public static void createFolder(@NonNull Context context, String boxUuid, UUID uuid, String dirname, String boxDomain, String accessToken, String secret, String transformation, String ivParams, boolean isLAN, NewFolderRspCallback callback) {
        CreateFolderReq createFolderReq = new CreateFolderReq();
        createFolderReq.setCurrentDirUuid((uuid == null ? null : uuid.toString()));
        createFolderReq.setFolderName(dirname);

        String finalBoxDomain = Urls.getBaseUrl();
        try {
            ThreadPool.getInstance().execute(() -> FileListManager.createFolder(createFolderReq, uuid, finalBoxDomain, accessToken, secret, transformation, ivParams, API_VERSION, new INewFolderRspCallback() {
                @Override
                public void onResult(NewFolderRsp result) {
                    Logger.i(TAG, "on result: " + result);
                    if (result == null) {
                        if (callback != null) {
                            callback.onFailed(null, null, null);
                        }
                    } else {
                        String message = result.getMessage();
                        String codeValue = result.getCode();
                        int code = -1;
                        if (codeValue == null && message != null && "ok".equalsIgnoreCase(message.trim())) {
                            code = 200;
                        } else {
                            code = DataUtil.stringCodeToInt(codeValue);
                        }
                        if (callback != null) {
                            if (code >= 0 && code < 400) {
                                callback.onSuccess(code, message, getFileId(result.getRequestId())
                                        , result.getResults());
                            } else {
                                callback.onFailed(code, message, getFileId(result.getRequestId()));
                            }
                        }
                    }
                }

                @Override
                public void onError(String msg) {
                    Logger.e(TAG, "on error: " + msg);
                    if (callback != null) {
                        callback.onError(msg);
                    }
                }
            }), true);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    // server exception handle
    public static void getRecycledList(@NonNull Context context, String boxUuid, String boxBind, Integer page, Integer pageSize, String boxDomain, String accessToken, String secret, String transformation, String ivParams, boolean isLAN, boolean isFore, RecycledListCallback callback) {
        String finalBoxDomain = Urls.getBaseUrl();
        try {
            ThreadPool.getInstance().execute(() -> FileListManager.getRecycledList(page, pageSize, finalBoxDomain, accessToken, secret, transformation, ivParams, API_VERSION, new IRecycledListCallback() {
                @Override
                public void onResult(RecycledListResponse result) {
                    Logger.i(TAG, "on result: " + result);
                    RecycledListResult recycledListResult = null;
                    if (result != null) {
                        recycledListResult = result.getResults();
                    }
                    if (recycledListResult == null) {
                        if (callback != null) {
                            callback.onFailed((result == null ? null : DataUtil.stringCodeToInt(result.getCode()))
                                    , (result == null ? null : result.getMessage()), (result == null ? null : result.getRequestId()));
                        }
                    } else {
                        updateEulixSpaceDB(context, boxUuid, boxBind, ConstantField.Category.FILE_RECYCLE, recycledListResult);
                        if (callback != null) {
                            callback.onSuccess(DataUtil.stringCodeToInt(result.getCode()), result.getMessage(), result.getRequestId()
                                    , recycledListResult.getFileList(), recycledListResult.getPageInfo(), result);
                        }
                    }
                }

                @Override
                public void onError(String msg) {
                    Logger.e(TAG, "on error: " + msg);
                    if (callback != null) {
                        callback.onError(msg);
                    }
                }
            }), isFore);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    // server exception handle
    public static void restoreRecycledList(@NonNull Context context, String boxUuid, String boxBind, List<UUID> uuids, String boxDomain, String accessToken, String secret, String transformation, String ivParams, boolean isLAN, RecycledListCallback callback) {
        FileUUIDs fileUUIDs = new FileUUIDs();
        if (uuids != null) {
            List<String> uuidList = new ArrayList<>();
            for (UUID uuid : uuids) {
                if (uuid != null) {
                    uuidList.add(uuid.toString());
                }
            }
            fileUUIDs.setUuids(uuidList);
        }
        String finalBoxDomain = Urls.getBaseUrl();
        try {
            ThreadPool.getInstance().execute(() -> {
                FileListManager.restoreRecycledFile(fileUUIDs, finalBoxDomain, accessToken, secret, transformation, ivParams, API_VERSION, new IRecycledListCallback() {
                    @Override
                    public void onResult(RecycledListResponse result) {
                        Logger.i(TAG, "on result: " + result);
                        int code = -1;
                        String message = null;
                        RecycledListResult recycledListResult = null;
                        if (result != null) {
                            message = result.getMessage();
                            String codeValue = result.getCode();
                            if (codeValue == null && message != null && "ok".equalsIgnoreCase(message.trim())) {
                                code = 200;
                            } else {
                                code = DataUtil.stringCodeToInt(codeValue);
                            }
                            recycledListResult = result.getResults();
                        }
                        if (code >= 0 && code < 400) {
                            if (callback != null) {
                                callback.onSuccess(code, message, result.getRequestId()
                                        , (recycledListResult == null ? null : recycledListResult.getFileList())
                                        , (recycledListResult == null ? null : recycledListResult.getPageInfo()), result);
                            }
                        } else {
//                            updateEulixSpaceCache(context, boxUuid, boxBind, ConstantField.Category.FILE_RECYCLE);
                            if (callback != null) {
                                callback.onFailed(code, message, (result == null ? null : result.getRequestId()));
                            }
                        }
                    }

                    @Override
                    public void onError(String msg) {
                        Logger.e(TAG, "on error: " + msg);
//                        updateEulixSpaceCache(context, boxUuid, boxBind, ConstantField.Category.FILE_RECYCLE);
                        if (callback != null) {
                            callback.onError(msg);
                        }
                    }
                });
            }, true);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    // server exception handle
    public static void clearRecycledList(@NonNull Context context, String boxUuid, String boxBind, List<UUID> uuids, String boxDomain, String accessToken, String secret, String transformation, String ivParams, boolean isLAN, RecycledListCallback callback) {
        FileUUIDs fileUUIDs = new FileUUIDs();
        if (uuids != null) {
            List<String> uuidList = new ArrayList<>();
            for (UUID uuid : uuids) {
                if (uuid != null) {
                    uuidList.add(uuid.toString());
                }
            }
            fileUUIDs.setUuids(uuidList);
        } else {
            fileUUIDs.setUuids(null);
        }
        String finalBoxDomain = Urls.getBaseUrl();
        try {
            ThreadPool.getInstance().execute(() -> {
                FileListManager.clearRecycledFile(fileUUIDs, finalBoxDomain, accessToken, secret, transformation, ivParams, API_VERSION, new IRecycledListCallback() {
                    @Override
                    public void onResult(RecycledListResponse result) {
                        Logger.i(TAG, "on result: " + result);
                        int code = -1;
                        String message = null;
                        RecycledListResult recycledListResult = null;
                        if (result != null) {
                            message = result.getMessage();
                            String codeValue = result.getCode();
                            if (codeValue == null && message != null && "ok".equalsIgnoreCase(message.trim())) {
                                code = 200;
                            } else {
                                code = DataUtil.stringCodeToInt(codeValue);
                            }
                            recycledListResult = result.getResults();
                        }
                        if (code >= 0 && code < 400) {
                            if (callback != null) {
                                callback.onSuccess(code, message, result.getRequestId()
                                        , (recycledListResult == null ? null : recycledListResult.getFileList())
                                        , (recycledListResult == null ? null : recycledListResult.getPageInfo()), result);
                            }
                        } else {
//                            updateEulixSpaceCache(context, boxUuid, boxBind, ConstantField.Category.FILE_RECYCLE);
                            if (callback != null) {
                                callback.onFailed(code, message, (result == null ? null : result.getRequestId()));
                            }
                        }
                    }

                    @Override
                    public void onError(String msg) {
                        Logger.e(TAG, "on error: " + msg);
//                        updateEulixSpaceCache(context, boxUuid, boxBind, ConstantField.Category.FILE_RECYCLE);
                        if (callback != null) {
                            callback.onError(msg);
                        }
                    }
                });
            }, true);
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    //分片上传文件
    public static void uploadFileMultipart(Context context, String filepath, String filename, String remotePath, boolean isSync, String albumId, ResultCallback callback) {
        try {
            TransferThreadPool.getInstance().execute(() -> {
                File file = new File(filepath, filename);
                if (!file.exists()) {
                    Logger.d("zfy", "file to upload not exist");
                    if (callback != null) {
                        callback.onResult(false, FailCodeUtil.ERROR_UPLOAD_LOCAL_SOURCE_DELETE + "");
                    }
                    return;
                }

                int transferType = isSync ? TransferHelper.TYPE_SYNC : TransferHelper.TYPE_UPLOAD;
                String uniqueTag = TransferItemFactory.getUniqueTagWithAlbumId(transferType, filename, filepath, remotePath, null, albumId);

                String dbMd5 = "";
                TransferItem dbItem = TransferDBManager.getInstance(context).queryByUniqueTag(uniqueTag, transferType);
                TransferItem transferItem;
                if (dbItem != null) {
                    transferItem = dbItem;
                    transferItem.state = TransferHelper.STATE_DOING;
                    transferItem.errorCode = 0;
                    transferItem.currentSize = 0L;
                    dbMd5 = dbItem.md5;
                    TransferDBManager.getInstance(context).updateTransferInfo(uniqueTag, transferItem, true);
                } else {
                    transferItem = TransferItemFactory.createUploadPrepareItem(context, filename, filepath, remotePath, file.length(), file.lastModified(), null, "", uniqueTag, isSync, albumId);
                    TransferDBManager.getInstance(context).insert(transferItem);
                }

                String betag;
                if (TextUtils.isEmpty(dbMd5) || dbMd5.length() != 34) {
                    //计算betag
//                    betag = MultipartUtil.getFileBetag(file.getAbsolutePath());
                    BetagCalculator betagCalculator = new BetagCalculator(file.getAbsolutePath());
                    betag = betagCalculator.getFileBetag();
                    if (TextUtils.isEmpty(betag)) {
                        if (callback != null) {
                            callback.onResult(false, "betag is null");
                        }
                        return;
                    }
                    TransferItem currentItem = TransferDBManager.getInstance(context).queryByUniqueTag(uniqueTag, transferType);
                    if (currentItem != null) {
                        transferItem = currentItem;
                        transferItem.md5 = betag;
                        TransferDBManager.getInstance(context).updateTransferInfo(uniqueTag, transferItem, false);
                    }
                } else {
                    betag = dbMd5;
                }

                String externalFileDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
                String mUploadCacheDirPath = externalFileDir + "/cache/multipart_upload/";

                MultipartUploadTask uploadTask = new MultipartUploadTask(context, remotePath, file.getAbsolutePath(), betag, mUploadCacheDirPath, uniqueTag, isSync, null);
                mMultiUploadTasks.put(transferItem.ext1, uploadTask);

                //设置任务回调
                final TransferItem finalTransferItem = transferItem;
                uploadTask.setCallbackListener(new MultipartUploadTask.CallbackListener() {
                    @Override
                    public void onGetUploadId(String uploadId) {
                        if (!TextUtils.isEmpty(uploadId) && !uploadId.equals(finalTransferItem.ext2)) {
                            //更新uploadId
                            TransferItem tempItem = finalTransferItem;
                            tempItem.ext2 = uploadId;
                            TransferDBManager.getInstance(context).updateTransferInfo(uniqueTag, tempItem, false);
                        }
                    }

                    @Override
                    public void onProgress(long currentSize, long totalSize, long appendSize, boolean isPercentChange, boolean isResume) {
                        if (isPercentChange) {
//                        Logger.d("zfy", "onProgressResult:currentSize=" + currentSize + ",totalSize=" + totalSize);
                            TransferDBManager.getInstance(context).updateTransferSize(finalTransferItem.keyName, transferType, currentSize, totalSize, true, uniqueTag);
                        }
                    }
                });

                //发起上传
                uploadTask.uploadFile(new ResultCallbackObj() {
                    @Override
                    public void onResult(boolean result, Object extraObj) {
                        mMultiUploadTasks.remove(finalTransferItem.ext1);
                        TaskSpeed.getInstance().removeTask(uniqueTag);
                        //更改数据库状态
                        if (result && extraObj != null) {
                            FileListItem fileListItem = (FileListItem) extraObj;
                            String fileUuid = fileListItem.getUuid();
                            Logger.d("zfy", "file upload success:" + filename + ";uuid=" + fileUuid);
                            if (!TextUtils.isEmpty(albumId)) {
                                TransferDBManager.getInstance(context).updateTransferRemotePath(uniqueTag, fileListItem.getPath());
                                //相簿更新路径
                            }
                            EventBusUtil.post(new UploadedFileEvent(fileUuid, filename, remotePath));
                            TransferDBManager.getInstance(context).updateTransferState(filename, transferType, TransferHelper.STATE_FINISH, 0, fileUuid, true, uniqueTag);
                            try {
                                String md5 = MD5Util.getFileMD5String(file);
                                AlreadyUploadedManager.getInstance().insertItem(file.getAbsolutePath(), md5);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if (callback != null) {
                                callback.onResult(true, fileListItem.getUuid());
                            }
                        } else {
                            int code = -1;
                            try {
                                if (extraObj != null && !TextUtils.isEmpty((CharSequence) extraObj)) {
                                    code = Integer.parseInt((String) extraObj);
                                }
                            } catch (Exception e) {
                                Logger.e(e.getMessage());
                            }
                            TransferDBManager.getInstance(context).updateTransferState(filename, transferType, TransferHelper.STATE_ERROR, code, null, true, uniqueTag);
                            if (callback != null) {
                                callback.onResult(result, code + "");
                            }
                        }

                    }

                    @Override
                    public void onError(String msg) {
                        if (callback != null) {
                            callback.onResult(false, msg);
                        }
                    }
                });
            });
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }


    public static void downloadFile(Context context, String uuidStr, String filepath, String filename, long fileSize, String md5, boolean isCache, String from, ResultCallback callback) {
        if (fileSize <= DOWNLOAD_MULTIPART_LIMIT_SIZE) {
            //整体下载
            GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
            if (gatewayCommunicationBase != null) {
                String baseUrl = Urls.getBaseUrl();
                UUID uuid = UUID.fromString(uuidStr);
                try {
                    TransferThreadPool.getInstance().execute(() -> downloadFile(uuid, filepath, filename, fileSize
                            , md5, baseUrl, gatewayCommunicationBase.getAccessToken()
                            , gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getTransformation()
                            , gatewayCommunicationBase.getIvParams(), API_VERSION, context, isCache, from, callback));
                } catch (RejectedExecutionException e) {
                    e.printStackTrace();
                }
            }
        } else {
            //分片下载
            downloadFileMultipart(context, uuidStr, filepath, filename, fileSize, md5, isCache, from, callback);
        }
    }

    //分片下载文件
    public static void downloadFileMultipart(Context context, String uuidStr, String filepath, String filename, long fileSize, String md5,
                                             boolean isCache, String from, ResultCallback callback) {
        try {
            TransferThreadPool.getInstance().execute(() -> {

                int transferType = isCache ? TransferHelper.TYPE_CACHE : TransferHelper.TYPE_DOWNLOAD;
                final String uniqueTag = TransferItemFactory.getUniqueTag(transferType, null, null, null, uuidStr);
                String localPath = getLocalPath(context, filepath, isCache, from);
                //判断是否有已下载同内容文件
                ArrayList<TransferItem> finishedList = TransferDBManager.getInstance(context).queryDownloadFinishItemsByMd5(md5);
                if (finishedList != null && !finishedList.isEmpty()) {
                    for (TransferItem item : finishedList) {
                        File finishedFile = new File(item.localPath, item.keyName);
                        if (finishedFile.exists()) {
                            Logger.d("zfy", "has exist same md5 file,copy!");
                            //复制文件
                            try {

                                File targetFile = new File(localPath, filename);
                                Files.copy(finishedFile, targetFile);
                                TransferDBManager.getInstance(context).updateTransferState(filename, transferType, TransferHelper.STATE_FINISH, 0, uuidStr, true, uniqueTag);
                                if (!isCache) {
                                    //加入媒体库
                                    AlbumNotifyHelper.insertToAlbum(context, targetFile);
                                }
                                if (callback != null) {
                                    callback.onResult(true, targetFile.getAbsolutePath());
                                }
                                return;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                TransferItem dbItem = TransferDBManager.getInstance(context).queryByUniqueTag(uniqueTag, transferType);
                if (dbItem != null) {
                    TransferItem newItem = dbItem;
                    newItem.state = TransferHelper.STATE_DOING;
                    newItem.errorCode = 0;
                    newItem.currentSize = 0L;
                    TransferDBManager.getInstance(context).updateTransferInfo(uniqueTag, newItem, true);
                } else {
                    TransferItem item = TransferItemFactory.createStartTransferItem(filename, transferType, localPath, filepath, null, fileSize, System.currentTimeMillis(), uuidStr, md5, uniqueTag);
                    TransferDBManager.getInstance(context).insert(item);
                }

                String externalFileDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
                String mDownloadCacheDirPath = externalFileDir + "/cache/multipart_cache/";

                MultipartDownloadTask downloadTask = new MultipartDownloadTask(context, uuidStr, localPath,
                        filename, fileSize, mDownloadCacheDirPath, uniqueTag);

                //设置任务回调
                downloadTask.setCallbackListener(new MultipartDownloadTask.CallbackListener() {
                    @Override
                    public void onProgress(long currentSize, long totalSize, long appendSize, boolean isPercentChange, boolean isResume) {
                        if (isPercentChange) {
//                        Logger.d("zfy", "onProgressResult:currentSize=" + currentSize + ",totalSize=" + totalSize);
                            TransferDBManager.getInstance(context).updateTransferSize(filename, transferType, currentSize, totalSize, true, uniqueTag);
                        }
                        //统计传输速度
                        if (!isResume) {
                            TaskSpeed.getInstance().start();
                            TaskSpeed.getInstance().appendDataLength(uniqueTag, appendSize);
                        }
                    }
                });

                //发起下载
                downloadTask.downloadFile((result, extraMsg) -> {
                    TaskSpeed.getInstance().removeTask(uniqueTag);
                    //更改数据库状态
                    if (result) {
                        Logger.d("zfy", "file download success:" + filename + ";uuid=" + uuidStr);
                        TransferDBManager.getInstance(context).updateTransferState(filename, transferType, TransferHelper.STATE_FINISH, 0, uuidStr, true, uniqueTag);
                        //加入媒体库
                        if (!isCache) {
                            File file = new File(localPath, filename);
                            if (file.exists()) {
                                AlbumNotifyHelper.insertToAlbum(context, file);
                            }
                        }
                    } else {
                        int code = -1;
                        try {
                            if (!TextUtils.isEmpty(extraMsg)) {
                                code = Integer.parseInt(extraMsg);
                            }
                        } catch (Exception e) {
                            Logger.e(e.getMessage());
                        }
                        TransferDBManager.getInstance(context).updateTransferState(filename, transferType, TransferHelper.STATE_ERROR, code, uuidStr, true, uniqueTag);
                    }
                    if (callback != null) {
                        callback.onResult(result, extraMsg);
                    }
                });
            });
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    private static String getLocalPath(Context context, String filepath, boolean isCache, String from) {
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
        return nFilepath;
    }

    //下载缩略图
    public static void downloadThumb(Context context, String uuidStr, String filename, String from) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
//            String ipAddressUrl = EulixSpaceDBUtil.getIpAddressUrl(context, gatewayCommunicationBase.getBoxUuid(), false);
//            String baseUrl = (ipAddressUrl == null ? gatewayCommunicationBase.getBoxDomain() : ipAddressUrl);
            String baseUrl = Urls.getBaseUrl();
            UUID uuid = UUID.fromString(uuidStr);
            try {
                ThumbThreadPool.getInstance().execute(() -> FileListManager.downloadThumb(uuid,
                        baseUrl, gatewayCommunicationBase.getAccessToken(),
                        gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getIvParams(),
                        ConstantField.BoxVersionName.VERSION_0_1_0, context, from, null));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }

    //下载压缩图
    public static void downloadCompressedImage(Context context, String uuidStr, String filename, String from, GetCompressedImageCallback listener) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            String baseUrl = Urls.getBaseUrl();
            UUID uuid = UUID.fromString(uuidStr);
            try {
                ThreadPool.getInstance().execute(() -> FileListManager.downloadCompressed(uuid, filename,
                        baseUrl, gatewayCommunicationBase.getAccessToken(),
                        gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getIvParams(),
                        ConstantField.BoxVersionName.VERSION_0_1_0, context, from, listener));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        }
    }


    public static void downloadFile(UUID uuid, String filepath, String filename, long fileSize, String md5, String boxDomain, String accessToken, String secret, String transformation, String ivParams, String apiVersion, Context context, boolean isCache, String from, ResultCallback callback) {
        FileListManager.downloadFile(uuid.toString(), filepath, filename, fileSize, md5, boxDomain, accessToken, secret, transformation, ivParams, apiVersion, context, isCache, from, callback);
    }

    //获取缩略图路径
    public static String getThumbPath(Context context, String uuidStr) {
        if (TextUtils.isEmpty(uuidStr)) {
            return null;
        }
        String folderPath = context.getExternalCacheDir().getAbsolutePath() + ConstantField.FILE_THUMBS_CACHE_PATH;
        String thumbPath = findFileByUUID(uuidStr, folderPath);
        if (!TextUtils.isEmpty(thumbPath)) {
            return thumbPath;
        } else {
            String albumThumbsPath = context.getExternalCacheDir().getAbsolutePath() + ConstantField.ALBUM_THUMBS_CACHE_PATH;
            return findFileByUUID(uuidStr, albumThumbsPath);
        }
    }

    //获取压缩图路径
    public static String getCompressedPath(Context context, String uuidStr) {
        String folderPath = context.getExternalCacheDir().getAbsolutePath() + "/compressed/";
        String compressed = findFileByUUID(uuidStr, folderPath);
        if (!TextUtils.isEmpty(compressed)) {
            return compressed;
        } else {
            String albumCompressedPath = context.getExternalCacheDir().getAbsolutePath() + ConstantField.ALBUM_COMPRESSED_CACHE_PATH;
            return findFileByUUID(uuidStr, albumCompressedPath);
        }
    }

    //获取缓存文件路径
    public static String getCacheFilePath(Context context, String fileName) {
        String folderPath = context.getExternalCacheDir().getAbsolutePath() + ConstantField.FILE_CACHE_PATH;
        String filePath = findFileByName(fileName, folderPath);
        if (!TextUtils.isEmpty(filePath)) {
            return filePath;
        } else {
            String albumPath = context.getExternalCacheDir().getAbsolutePath() + ConstantField.ALBUM_CACHE_PATH;
            return findFileByName(fileName, albumPath);
        }
    }

    private static String findFileByUUID(String uuidStr, String folderPath) {
        String filePath = null;
        File folder = new File(folderPath);
        if (folder.exists()) {
            File[] files = folder.listFiles();
            for (File file : files) {
                if (TextUtils.isEmpty(uuidStr)) {
                    continue;
                }
                if (file.getName().contains(uuidStr)) {
                    filePath = file.getAbsolutePath();
                    break;
                }
            }
        }
        return filePath;
    }

    private static String findFileByName(String name, String folderPath) {
        String filePath = null;
        File folder = new File(folderPath);
        if (folder.exists()) {
            File[] files = folder.listFiles();
            if (files == null) {
                return null;
            }
            for (File file : files) {
                if (file.getName().contains(name)) {
                    filePath = file.getAbsolutePath();
                    break;
                }
            }
        }
        return filePath;
    }
}
