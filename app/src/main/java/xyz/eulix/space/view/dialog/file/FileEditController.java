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

package xyz.eulix.space.view.dialog.file;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.io.File;
import java.util.List;
import java.util.UUID;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.bean.ArrayStack;
import xyz.eulix.space.bean.CustomizeFile;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.interfaces.ResultCallback;
import xyz.eulix.space.interfaces.ResultCallbackObj;
import xyz.eulix.space.manager.TransferTaskManager;
import xyz.eulix.space.network.files.AsyncTaskStatusResponseBody;
import xyz.eulix.space.network.files.FileListItem;
import xyz.eulix.space.network.files.FileListUtil;
import xyz.eulix.space.network.files.FileRsp;
import xyz.eulix.space.network.files.FileRspCallback;
import xyz.eulix.space.network.files.FolderInfoCallback;
import xyz.eulix.space.network.files.GetFileListCallback;
import xyz.eulix.space.network.files.PageInfo;
import xyz.eulix.space.network.files.RecycledListCallback;
import xyz.eulix.space.network.files.RecycledListResponse;
import xyz.eulix.space.transfer.TransferHelper;
import xyz.eulix.space.transfer.db.TransferDBManager;
import xyz.eulix.space.transfer.model.TransferItem;
import xyz.eulix.space.transfer.model.TransferItemFactory;
import xyz.eulix.space.util.AlbumNotifyHelper;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.GatewayUtils;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.ThreadPool;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/9/16 14:30
 */
public class FileEditController {
    private Context mContext;
    private IFileEditCallback mCallback;
    private ArrayStack<UUID> uuidStack;
    private ArrayStack<UUID> searchUuidStack;
    private UUID mUuid = null;
    private String category;

    //操作类型
    //删除
    public static final int OPERATE_TYPE_DELETE = 1;
    //恢复
    public static final int OPERATE_TYPE_RESTORE = 2;

    public interface IFileEditCallback {
        void getEulixSpaceFileListResult(Integer code, String currentDirectory, List<CustomizeFile> customizeFiles, PageInfo pageInfo);

        void getFolderInfoResult(Integer code, String folderUuid, String name, Long operationAt, String path, Long size);

        void modifyEulixSpaceFileResult(Integer code, boolean isOk, String parentId, Integer affectRows, String newFileName);

        void copyEulixSpaceFileResult(Integer code, boolean isOk, String sourceParentId, String destinationParentId, Integer affectRows);

        void cutEulixSpaceFileResult(Integer code, boolean isOk, String sourceParentId, String destinationParentId, Integer affectRows);

        void deleteEulixSpaceFileResult(Integer code, boolean isOk, String parentId, Integer affectRows, FileRsp fileRsp);

        void downloadEulixSpaceFileResult(Integer code, boolean isOk);

        void onCheckFileIsExist(String absolutePath);

        void handleFileSearchShowOrDestroy(boolean isShow);

        void restoreRecycledResult(Integer code, boolean isOk, RecycledListResponse response);

        void clearRecycledResult(Integer code, boolean isOk);

        void onAsyncTaskStatusRefresh(boolean isSuccess, String taskId, String taskStatus, int precessed, int total, int operateType);
    }

    public FileEditController(String category) {
        mContext = EulixSpaceApplication.getContext();
        this.category = category;
    }

    public void registerCallback(IFileEditCallback callback) {
        mCallback = callback;
    }

    private void generateValidAccessToken(String serviceFunction) {
        if (mCallback != null && serviceFunction != null) {
            switch (serviceFunction) {
                case ConstantField.ServiceFunction.LIST_FOLDERS:
                    mCallback.getEulixSpaceFileListResult(ConstantField.OBTAIN_ACCESS_TOKEN_CODE, null, null, null);
                    break;
                case ConstantField.ServiceFunction.FOLDER_INFO:
                    mCallback.getFolderInfoResult(ConstantField.OBTAIN_ACCESS_TOKEN_CODE, null, null, null, null, null);
                    break;
                case ConstantField.ServiceFunction.MODIFY_FILE:
                    mCallback.modifyEulixSpaceFileResult(ConstantField.OBTAIN_ACCESS_TOKEN_CODE, false, null, null, null);
                    break;
                case ConstantField.ServiceFunction.COPY_FILE:
                    mCallback.copyEulixSpaceFileResult(ConstantField.OBTAIN_ACCESS_TOKEN_CODE, false, null, null, null);
                    break;
                case ConstantField.ServiceFunction.MOVE_FILE:
                    mCallback.cutEulixSpaceFileResult(ConstantField.OBTAIN_ACCESS_TOKEN_CODE, false, null, null, null);
                    break;
                case ConstantField.ServiceFunction.DELETE_FILE:
                    mCallback.deleteEulixSpaceFileResult(ConstantField.OBTAIN_ACCESS_TOKEN_CODE, false, null, null, null);
                    break;
                case ConstantField.ServiceFunction.UPLOAD_FILE:
                    break;
                case ConstantField.ServiceFunction.DOWNLOAD_FILE:
                    mCallback.downloadEulixSpaceFileResult(ConstantField.OBTAIN_ACCESS_TOKEN_CODE, false);
                    break;
                case ConstantField.ServiceFunction.RESTORE_RECYCLED:
                    mCallback.restoreRecycledResult(ConstantField.OBTAIN_ACCESS_TOKEN_CODE, false, null);
                    break;
                case ConstantField.ServiceFunction.CLEAR_RECYCLED:
                    mCallback.clearRecycledResult(ConstantField.OBTAIN_ACCESS_TOKEN_CODE, false);
                    break;
                default:
                    break;
            }
        }
    }

    private void getFileList(UUID uuid, Integer page, Integer pageSize, String order, String category) {
        getFileList(uuid, page, pageSize, order, category, false);
    }

    /**
     * 获取在线文件列表
     *
     * @param uuid
     * @param page
     */
    private void getFileList(UUID uuid, Integer page, Integer pageSize, String order, String category, boolean isFore) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(mContext);
        if (gatewayCommunicationBase != null) {
            FileListUtil.getFileList(mContext, gatewayCommunicationBase.getBoxUuid()
                    , gatewayCommunicationBase.getBoxBind(), uuid, page, (pageSize == null ? Integer.valueOf(20) : pageSize), order, category
                    , gatewayCommunicationBase.getBoxDomain(), gatewayCommunicationBase.getAccessToken()
                    , gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getTransformation()
                    , gatewayCommunicationBase.getIvParams(), true, isFore, new GetFileListCallback() {
                        @Override
                        public void onSuccess(Integer code, String message, String requestId, List<FileListItem> fileListItems, PageInfo pageInfo, Integer fileCount) {
                            if (mCallback != null) {
                                mCallback.getEulixSpaceFileListResult(code, requestId, FileUtil.convertToCustomFileList(fileListItems), pageInfo);
                            }
                        }

                        @Override
                        public void onFailed(Integer code, String message, String requestId) {
                            if (mCallback != null) {
                                mCallback.getEulixSpaceFileListResult(code, requestId, null, null);
                            }
                        }

                        @Override
                        public void onError(String msg) {
                            if (mCallback != null) {
                                mCallback.getEulixSpaceFileListResult(ConstantField.SERVER_EXCEPTION_CODE, null, null, null);
                            }
                        }
                    });
        } else {
            generateValidAccessToken(ConstantField.ServiceFunction.LIST_FOLDERS);
        }
    }

    private void getFolderInfo(UUID uuid, String parentId) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(mContext);
        if (gatewayCommunicationBase != null) {
            FileListUtil.getFolderInfo(mContext, gatewayCommunicationBase.getBoxUuid()
                    , gatewayCommunicationBase.getBoxBind(), uuid, parentId, gatewayCommunicationBase.getBoxDomain()
                    , gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                    , gatewayCommunicationBase.getTransformation(), gatewayCommunicationBase.getIvParams()
                    , true, new FolderInfoCallback() {
                        @Override
                        public void onSuccess(Integer code, String message, String requestId, String name, Long operationAt, String path, Long size) {
                            if (mCallback != null) {
                                mCallback.getFolderInfoResult(code, requestId, name, operationAt, path, size);
                            }
                        }

                        @Override
                        public void onFailed(Integer code, String message, String requestId) {
                            if (mCallback != null) {
                                mCallback.getFolderInfoResult(code, requestId, null, null, null, null);
                            }
                        }

                        @Override
                        public void onError(String msg) {
                            if (mCallback != null) {
                                mCallback.getFolderInfoResult(-1, null, null, null, null, null);
                            }
                        }
                    });
        } else {
            generateValidAccessToken(ConstantField.ServiceFunction.FOLDER_INFO);
        }
    }

    /**
     * 在线文件更名
     *
     * @param uuid
     * @param filename
     */
    private void modifyFile(UUID uuid, String filename, String parentId) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(mContext);
        if (gatewayCommunicationBase != null) {
            FileListUtil.modifyFile(mContext, gatewayCommunicationBase.getBoxUuid(), uuid, filename
                    , gatewayCommunicationBase.getBoxDomain(), gatewayCommunicationBase.getAccessToken()
                    , gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getTransformation()
                    , gatewayCommunicationBase.getIvParams(), true, new FileRspCallback() {
                        @Override
                        public void onSuccess(Integer code, String message, String requestId, Integer affectRows, FileRsp fileRsp) {
                            if (mCallback != null) {
                                mCallback.modifyEulixSpaceFileResult(code, true, parentId, affectRows, filename);
                            }
                        }

                        @Override
                        public void onFailed(Integer code, String message, String requestId) {
                            if (mCallback != null) {
                                mCallback.modifyEulixSpaceFileResult(code, false, parentId, null, filename);
                            }
                        }

                        @Override
                        public void onError(String msg) {
                            if (mCallback != null) {
                                mCallback.modifyEulixSpaceFileResult(ConstantField.SERVER_EXCEPTION_CODE, false, parentId, null, filename);
                            }
                        }
                    });
        } else {
            generateValidAccessToken(ConstantField.ServiceFunction.MODIFY_FILE);
        }
    }

    /**
     * 删除在线文件
     *
     * @param uuids
     */
    private void deleteFileList(List<UUID> uuids, String parentId) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(mContext);
        if (gatewayCommunicationBase != null) {
            FileListUtil.deleteFile(mContext, gatewayCommunicationBase.getBoxUuid(), uuids, mUuid, gatewayCommunicationBase.getBoxDomain()
                    , gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                    , gatewayCommunicationBase.getTransformation(), gatewayCommunicationBase.getIvParams()
                    , true, new FileRspCallback() {
                        @Override
                        public void onSuccess(Integer code, String message, String requestId, Integer affectRows, FileRsp fileRsp) {
                            if (mCallback != null) {
                                mCallback.deleteEulixSpaceFileResult(code, true, parentId, affectRows, fileRsp);
                            }
                        }

                        @Override
                        public void onFailed(Integer code, String message, String requestId) {
                            if (mCallback != null) {
                                mCallback.deleteEulixSpaceFileResult(code, false, parentId, null, null);
                            }
                        }

                        @Override
                        public void onError(String msg) {
                            if (mCallback != null) {
                                mCallback.deleteEulixSpaceFileResult(ConstantField.SERVER_EXCEPTION_CODE, false, parentId, null, null);
                            }
                        }
                    });
        } else {
            generateValidAccessToken(ConstantField.ServiceFunction.DELETE_FILE);
        }
    }

    //轮询异步任务状态
    public void poolCheckAsyncTaskStatus(String taskId, int operateType) {
        FileListUtil.checkAsyncTaskStatus(mContext, taskId, new ResultCallbackObj() {
            @Override
            public void onResult(boolean result, Object extraObj) {
                if (result && extraObj != null) {
                    AsyncTaskStatusResponseBody responseBody = (AsyncTaskStatusResponseBody) extraObj;
                    if (responseBody.getCodeInt() >= 200 && responseBody.getCodeInt()<300 && responseBody.results != null) {
                        if (mCallback != null) {
                            mCallback.onAsyncTaskStatusRefresh(true, responseBody.results.taskId, responseBody.results.taskStatus, responseBody.results.processed, responseBody.results.total, operateType);
                        }
                        if (ConstantField.FileAsyncTaskStatus.STATUS_PROCESSING.equals(responseBody.results.taskStatus) || ConstantField.FileAsyncTaskStatus.STATUS_INIT.equals(responseBody.results.taskStatus)) {
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                poolCheckAsyncTaskStatus(taskId, operateType);
                            }, 500);
                        }
                    } else {
                        if (mCallback != null) {
                            mCallback.onAsyncTaskStatusRefresh(false, null, null, -1, -1, operateType);
                        }
                    }
                } else {
                    if (mCallback != null) {
                        mCallback.onAsyncTaskStatusRefresh(false, null, null, -1, -1, operateType);
                    }
                }
            }

            @Override
            public void onError(String msg) {
                if (mCallback != null) {
                    mCallback.onAsyncTaskStatusRefresh(false, null, null, -1, -1, operateType);
                }
            }
        });
    }

    private void copyFileList(String destinationUUID, List<UUID> uuids, String parentId) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(mContext);
        if (gatewayCommunicationBase != null) {
            FileListUtil.copyFile(mContext, gatewayCommunicationBase.getBoxUuid(), uuids, mUuid, destinationUUID
                    , gatewayCommunicationBase.getBoxDomain(), gatewayCommunicationBase.getAccessToken()
                    , gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getTransformation()
                    , gatewayCommunicationBase.getIvParams(), true, new FileRspCallback() {
                        @Override
                        public void onSuccess(Integer code, String message, String requestId, Integer affectRows, FileRsp fileRsp) {
                            if (mCallback != null) {
                                mCallback.copyEulixSpaceFileResult(code, true, parentId, destinationUUID, affectRows);
                            }
                        }

                        @Override
                        public void onFailed(Integer code, String message, String requestId) {
                            if (mCallback != null) {
                                mCallback.copyEulixSpaceFileResult(code, false, parentId, destinationUUID, null);
                            }
                        }

                        @Override
                        public void onError(String msg) {
                            if (mCallback != null) {
                                mCallback.copyEulixSpaceFileResult(ConstantField.SERVER_EXCEPTION_CODE, false, parentId, destinationUUID, null);
                            }
                        }
                    });
        } else {
            generateValidAccessToken(ConstantField.ServiceFunction.COPY_FILE);
        }
    }

    private void cutFileList(String destinationUUID, List<UUID> uuids, String parentId) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(mContext);
        if (gatewayCommunicationBase != null) {
            FileListUtil.moveFile(mContext, gatewayCommunicationBase.getBoxUuid(), uuids, mUuid, destinationUUID
                    , gatewayCommunicationBase.getBoxDomain(), gatewayCommunicationBase.getAccessToken()
                    , gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getTransformation()
                    , gatewayCommunicationBase.getIvParams(), true, new FileRspCallback() {
                        @Override
                        public void onSuccess(Integer code, String message, String requestId, Integer affectRows, FileRsp fileRsp) {
                            if (mCallback != null) {
                                mCallback.cutEulixSpaceFileResult(code, true, parentId, destinationUUID, null);
                            }
                        }

                        @Override
                        public void onFailed(Integer code, String message, String requestId) {
                            if (mCallback != null) {
                                mCallback.cutEulixSpaceFileResult(code, false, parentId, destinationUUID, null);
                            }
                        }

                        @Override
                        public void onError(String msg) {
                            if (mCallback != null) {
                                mCallback.cutEulixSpaceFileResult(ConstantField.SERVER_EXCEPTION_CODE, false, parentId, destinationUUID, null);
                            }
                        }
                    });
        } else {
            generateValidAccessToken(ConstantField.ServiceFunction.MOVE_FILE);
        }
    }

    private void restoreRecycledList(List<UUID> uuids) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(mContext);
        if (gatewayCommunicationBase != null) {
            FileListUtil.restoreRecycledList(mContext, gatewayCommunicationBase.getBoxUuid(), gatewayCommunicationBase.getBoxBind()
                    , uuids, gatewayCommunicationBase.getBoxDomain(), gatewayCommunicationBase.getAccessToken()
                    , gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getTransformation()
                    , gatewayCommunicationBase.getIvParams(), true, new RecycledListCallback() {
                        @Override
                        public void onSuccess(Integer code, String message, String requestId, List<FileListItem> fileListItems, PageInfo pageInfo, RecycledListResponse response) {
                            if (mCallback != null) {
                                mCallback.restoreRecycledResult(code, true, response);
                            }
                        }

                        @Override
                        public void onFailed(Integer code, String message, String requestId) {
                            if (mCallback != null) {
                                mCallback.restoreRecycledResult(code, false, null);
                            }
                        }

                        @Override
                        public void onError(String msg) {
                            if (mCallback != null) {
                                mCallback.restoreRecycledResult(ConstantField.SERVER_EXCEPTION_CODE, false, null);
                            }
                        }
                    });
        } else {
            generateValidAccessToken(ConstantField.ServiceFunction.RESTORE_RECYCLED);
        }
    }

    private void clearRecycledList(List<UUID> uuids) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(mContext);
        if (gatewayCommunicationBase != null) {
            FileListUtil.clearRecycledList(mContext, gatewayCommunicationBase.getBoxUuid(), gatewayCommunicationBase.getBoxBind()
                    , uuids, gatewayCommunicationBase.getBoxDomain(), gatewayCommunicationBase.getAccessToken()
                    , gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getTransformation()
                    , gatewayCommunicationBase.getIvParams(), true, new RecycledListCallback() {
                        @Override
                        public void onSuccess(Integer code, String message, String requestId, List<FileListItem> fileListItems, PageInfo pageInfo, RecycledListResponse response) {
                            if (mCallback != null) {
                                mCallback.clearRecycledResult(code, true);
                            }
                        }

                        @Override
                        public void onFailed(Integer code, String message, String requestId) {
                            if (mCallback != null) {
                                mCallback.clearRecycledResult(code, false);
                            }
                        }

                        @Override
                        public void onError(String msg) {
                            if (mCallback != null) {
                                mCallback.clearRecycledResult(ConstantField.SERVER_EXCEPTION_CODE, false);
                            }
                        }
                    });
        } else {
            generateValidAccessToken(ConstantField.ServiceFunction.CLEAR_RECYCLED);
        }
    }

    public int getDepth() {
        return (uuidStack == null ? 0 : uuidStack.size());
    }

    public int getSearchDepth() {
        return (searchUuidStack == null ? 0 : searchUuidStack.size());
    }

    public boolean isUUIDSame() {
        if (uuidStack == null || uuidStack.empty()) {
            return (mUuid == null || ConstantField.UUID.FILE_ROOT_UUID.equals(mUuid.toString()));
        } else {
            UUID uuid = uuidStack.peek();
            if (uuid == null || mUuid == null) {
                return (mUuid == null || ConstantField.UUID.FILE_ROOT_UUID.equals(mUuid.toString()));
            } else {
                return (uuid.toString().equals(mUuid.toString()));
            }
        }
    }

    public boolean isSearchUUIDSame() {
        boolean isSame = false;
        if (searchUuidStack != null && !searchUuidStack.empty()) {
            UUID uuid = searchUuidStack.peek();
            if (uuid != null && mUuid != null) {
                isSame = (uuid.toString().equals(mUuid.toString()));
            }
        }
        return isSame;
    }

    public void getEulixSpaceStorage(Integer page) {
        getEulixSpaceStorage(mUuid, page, null, null);
    }

    public void getEulixSpaceStorage(Integer page, String order) {
        getEulixSpaceStorage(page, order, false);
    }

    public void getEulixSpaceStorage(Integer page, String order, boolean isFore) {
        getEulixSpaceStorage(mUuid, page, null, order, isFore);
    }

    public void getEulixSpaceStorage(UUID uuid, Integer page, Integer pageSize, String order) {
        getEulixSpaceStorage(uuid, page, pageSize, order, false);
    }

    public void getEulixSpaceStorage(UUID uuid, Integer page, Integer pageSize, String order, boolean isFore) {
        if (uuid != null && ConstantField.UUID.FILE_ROOT_UUID.equals(uuid.toString())) {
            uuid = null;
        }
        getFileList(uuid, page, pageSize, order, category, isFore);
    }

    public void getFolderDetail(UUID uuid) {
        String parentId = null;
        if (mUuid == null || ConstantField.UUID.FILE_ROOT_UUID.equals(mUuid.toString())) {
            if (category == null) {
                parentId = ConstantField.Category.FILE_ROOT;
            } else {
                switch (category) {
                    case ConstantField.Category.PICTURE:
                        parentId = ConstantField.Category.FILE_IMAGE;
                        break;
                    case ConstantField.Category.VIDEO:
                        parentId = ConstantField.Category.FILE_VIDEO;
                        break;
                    case ConstantField.Category.DOCUMENT:
                        parentId = ConstantField.Category.FILE_DOCUMENT;
                        break;
                    case ConstantField.Category.OTHER:
                        parentId = ConstantField.Category.FILE_OTHER;
                        break;
                    default:
                        break;
                }
            }
        } else {
            parentId = mUuid.toString();
        }
        getFolderInfo(uuid, parentId);
    }

    public void deleteFile(List<UUID> uuids) {
        deleteFile(uuids, (mUuid == null ? null : mUuid.toString()));
    }

    public void deleteFile(List<UUID> uuids, String parentId) {
        deleteFileList(uuids, parentId);
    }

    public void renameFile(UUID uuid, String filename) {
        renameFile(uuid, filename, (mUuid == null ? null : mUuid.toString()));
    }

    public void renameFile(UUID uuid, String filename, String parentId) {
        modifyFile(uuid, filename, parentId);
    }

    public void copyFile(UUID destinationUUID, List<UUID> uuids) {
        copyFile(destinationUUID, uuids, (mUuid == null ? null : mUuid.toString()));
    }

    public void copyFile(UUID destinationUUID, List<UUID> uuids, String parentId) {
        if (destinationUUID != null && ConstantField.UUID.FILE_ROOT_UUID.equals(destinationUUID.toString())) {
            destinationUUID = null;
        }
        copyFileList(destinationUUID == null ? null : destinationUUID.toString(), uuids, parentId);
    }

    public void cutFile(UUID destinationUUID, List<UUID> uuids) {
        cutFile(destinationUUID, uuids, (mUuid == null ? null : mUuid.toString()));
    }

    public void cutFile(UUID destinationUUID, List<UUID> uuids, String parentId) {
        if (destinationUUID != null && ConstantField.UUID.FILE_ROOT_UUID.equals(destinationUUID.toString())) {
            destinationUUID = null;
        }
        cutFileList(destinationUUID == null ? null : destinationUUID.toString(), uuids, parentId);
    }

    public void restoreRecycled(List<UUID> uuids) {
        restoreRecycledList(uuids);
    }

    public void clearRecycled(List<UUID> uuids) {
        clearRecycledList(uuids);
    }

    public void downloadFile(UUID uuid, String filepath, String filename, long fileSize, String md5) {
        String uniqueTag = TransferItemFactory.getUniqueTag(TransferHelper.TYPE_CACHE, null, null, null, uuid.toString());
        TransferItem transferItem = TransferDBManager.getInstance(EulixSpaceApplication.getContext()).queryByUniqueTag(uniqueTag, TransferHelper.TYPE_CACHE);
        if (transferItem != null && transferItem.state == TransferHelper.STATE_FINISH) {
            //已缓存
            //1.移动文件至下载路径
            String currentPath = transferItem.localPath;
            String remotePath = transferItem.remotePath;
            if (TextUtils.isEmpty(remotePath)) {
                remotePath = "/";
            }
            String targetFilepath = ConstantField.SDCARD_ROOT_PATH + File.separator + ConstantField.APP_PATH;
            if (currentPath != null) {
                int nLength = targetFilepath.length();
                if (remotePath.startsWith("/")) {
                    if (targetFilepath.endsWith("/")) {
                        targetFilepath = targetFilepath.substring(0, (nLength - 1));
                    }
                } else {
                    if (!targetFilepath.endsWith("/")) {
                        targetFilepath = targetFilepath + "/";
                    }
                }
                targetFilepath = targetFilepath + remotePath;
            }
            Logger.d("copy file from " + currentPath + " to " + targetFilepath);
            File sourceFile = new File(currentPath, filename);
            boolean copyResult = FileUtil.copyFile(sourceFile.getAbsolutePath(), targetFilepath);
            if (copyResult) {
                Logger.d("zfy", "copy file success");
//                boolean result = sourceFile.delete();
//                Logger.d("source file delete: " + result);
            }
            //2.插入媒体库
            File targetFile = new File(targetFilepath, filename);
            AlbumNotifyHelper.insertToAlbum(EulixSpaceApplication.getContext(), targetFile);
            //3.修改数据库类型字段
            String newUniqueTag = TransferItemFactory.getUniqueTag(TransferHelper.TYPE_DOWNLOAD, null, null, null, uuid.toString());
            TransferItem newItem = transferItem;
            newItem.transferType = TransferHelper.TYPE_DOWNLOAD;
            newItem.ext1 = newUniqueTag;
            newItem.localPath = targetFilepath;
            long changeTime = System.currentTimeMillis();
            newItem.createTime = changeTime;
            newItem.updateTime = changeTime;
            if (TransferDBManager.getInstance(EulixSpaceApplication.getContext()).queryByUniqueTag(newUniqueTag, TransferHelper.TYPE_DOWNLOAD) != null) {
                TransferDBManager.getInstance(EulixSpaceApplication.getContext()).updateTransferInfo(newUniqueTag, newItem, true);
            } else {
                TransferDBManager.getInstance(EulixSpaceApplication.getContext()).insert(newItem);
            }
        } else {
            //未缓存完成
            ThreadPool.getInstance().execute(() -> {
                TransferTaskManager.getInstance().insertDownloadTask(uuid.toString(), filepath, filename, fileSize, md5, false, TransferHelper.FROM_FILE);
            });
        }

    }

    public void checkFileExist(Context context, String fileName, String uuidStr, String filePath, long fileSize, String md5) {
        String uniqueTag = TransferItemFactory.getUniqueTag(TransferHelper.TYPE_DOWNLOAD, null, null, null, uuidStr);
        TransferItem downloadItem = TransferDBManager.getInstance(context).queryByUniqueTag(uniqueTag, TransferHelper.TYPE_DOWNLOAD);
        //已下载
        if (downloadItem != null && downloadItem.state == TransferHelper.STATE_FINISH) {
            File fileDownload = new File(downloadItem.localPath, fileName);
            if (fileDownload.exists() && mCallback != null) {
                //文件存在
                mCallback.onCheckFileIsExist(fileDownload.getAbsolutePath());
                return;
            }
        }

        //查看是否已缓存
        String cacheFilePath = FileListUtil.getCacheFilePath(context, fileName);
        if (!TextUtils.isEmpty(cacheFilePath)) {
            //文件存在
            if (mCallback != null) {
                mCallback.onCheckFileIsExist(cacheFilePath);
            }
            return;
        }


        //文件不存在，重新下载
        FileListUtil.downloadFile(context.getApplicationContext(), uuidStr, filePath, fileName, fileSize, md5, true, TransferHelper.FROM_FILE, new ResultCallback() {
            @Override
            public void onResult(boolean result, String extraMsg) {
                if (result) {
                    int cacheTransferType = TransferHelper.TYPE_CACHE;
                    String uniqueTag = TransferItemFactory.getUniqueTag(cacheTransferType, null, null, null, uuidStr);
                    TransferItem cacheItem = TransferDBManager.getInstance(context).queryByUniqueTag(uniqueTag, cacheTransferType);
                    File file = new File(cacheItem.localPath, cacheItem.keyName);
                    if (file.exists()) {
                        mCallback.onCheckFileIsExist(file.getAbsolutePath());
                    } else {
                        mCallback.onCheckFileIsExist("");
                    }
                } else {
                    mCallback.onCheckFileIsExist("");
                }
            }
        });
    }

    public void handleNext(String currentDirectory, boolean isSearch) {
        if (uuidStack == null) {
            uuidStack = new ArrayStack<>();
            if (!isSearch && currentDirectory != null && !currentDirectory.equals(ConstantField.UUID.FILE_ROOT_UUID)) {
                uuidStack.push(UUID.fromString(ConstantField.UUID.FILE_ROOT_UUID));
            }
        }
        boolean isSuccess = false;
        if (TextUtils.isEmpty(currentDirectory)) {
            mUuid = null;
            isSuccess = true;
        } else {
            try {
                mUuid = UUID.fromString(currentDirectory);
                isSuccess = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (isSuccess) {
            int direction = 1;
            if (!uuidStack.empty()) {
                UUID topUUID = uuidStack.peek();
                if ((topUUID == null && mUuid == null) || (topUUID != null && topUUID.equals(mUuid))) {
                    direction = 0;
                }
            }
            if (direction > 0) {
                uuidStack.push(mUuid);
            }
        }

//        if (isSearch) {
//            if (searchUuidStack == null) {
//                searchUuidStack = new ArrayStack<>();
//            }
//            boolean isSuccess = false;
//            if (TextUtils.isEmpty(currentDirectory)) {
//                mUuid = null;
//                isSuccess = true;
//            } else {
//                try {
//                    mUuid = UUID.fromString(currentDirectory);
//                    isSuccess = true;
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//            if (isSuccess) {
//                int direction = 1;
//                if (!searchUuidStack.empty()) {
//                    UUID topUUID = searchUuidStack.peek();
//                    if ((topUUID == null && mUuid == null) || (topUUID != null && topUUID.equals(mUuid))) {
//                        direction = 0;
//                    }
//                }
//                if (direction > 0) {
//                    searchUuidStack.push(mUuid);
//                }
//            }
//        } else {
//            resetSearchUuidStack();
//            if (uuidStack == null) {
//                uuidStack = new ArrayStack<>();
//                if (currentDirectory != null && !currentDirectory.equals(ConstantField.UUID.FILE_ROOT_UUID)) {
//                    uuidStack.push(UUID.fromString(ConstantField.UUID.FILE_ROOT_UUID));
//                }
//            }
//            boolean isSuccess = false;
//            if (TextUtils.isEmpty(currentDirectory)) {
//                mUuid = null;
//                isSuccess = true;
//            } else {
//                try {
//                    mUuid = UUID.fromString(currentDirectory);
//                    isSuccess = true;
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
//            if (isSuccess) {
//                int direction = 1;
//                if (!uuidStack.empty()) {
//                    UUID topUUID = uuidStack.peek();
//                    if ((topUUID == null && mUuid == null) || (topUUID != null && topUUID.equals(mUuid))) {
//                        direction = 0;
//                    }
//                }
//                if (direction > 0) {
//                    uuidStack.push(mUuid);
//                }
//            }
//        }
    }

    public UUID handleBack() {
        UUID uuid = null;
        uuidStack.pop();
        if (uuidStack.empty()) {
            uuidStack = null;
        } else {
            uuid = uuidStack.peek();
        }

//        if (searchUuidStack != null && !searchUuidStack.empty()) {
//            searchUuidStack.pop();
//            if (searchUuidStack.empty()) {
//                searchUuidStack = null;
//                if (uuidStack != null && !uuidStack.empty()) {
//                    uuid = uuidStack.peek();
//                }
//                if (mCallback != null) {
//                    mCallback.handleFileSearchShowOrDestroy(true);
//                }
//            } else {
//                uuid = searchUuidStack.peek();
//            }
//        } else {
//            resetSearchUuidStack();
//            uuidStack.pop();
//            if (uuidStack.empty()) {
//                uuidStack = null;
//            } else {
//                uuid = uuidStack.peek();
//            }
//        }
        mUuid = uuid;
        return uuid;
    }

    public UUID getmUuid() {
        return mUuid;
    }

    public ArrayStack<UUID> getUuidStack() {
        return uuidStack;
    }

    public void setUuidStack(ArrayStack<UUID> uuidStack) {
        this.uuidStack = uuidStack;
        if (uuidStack == null) {
            mUuid = null;
        } else {
            mUuid = uuidStack.peek();
        }
    }

    public void reset() {
        resetSearchUuidStack();
        mUuid = null;
        if (uuidStack != null) {
            uuidStack.clear();
            uuidStack = null;
        }
    }

    public void resetSearchUuidStack() {
        if (searchUuidStack != null) {
            searchUuidStack.clear();
            searchUuidStack = null;
            if (uuidStack != null && !uuidStack.empty()) {
                mUuid = uuidStack.peek();
            } else {
                mUuid = null;
            }
            if (mCallback != null) {
                mCallback.handleFileSearchShowOrDestroy(false);
            }
        }
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
