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

package xyz.eulix.space.view.dialog.folder;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.bean.ArrayStack;
import xyz.eulix.space.bean.CustomizeFile;
import xyz.eulix.space.bean.EulixSpaceInfo;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.network.files.FileListItem;
import xyz.eulix.space.network.files.FileListUtil;
import xyz.eulix.space.network.files.FileRspCallback;
import xyz.eulix.space.network.files.GetFileListCallback;
import xyz.eulix.space.network.files.NewFolderRspCallback;
import xyz.eulix.space.network.files.PageInfo;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.GatewayUtils;
import xyz.eulix.space.util.ThreadPool;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/9/2 16:18
 */
public class FolderListController {
    private Context mContext;
    private IFolderListCallback mCallback;
    private ArrayStack<UUID> uuidStack;
    private UUID mUuid = null;
    private boolean isQuery = false;

    public interface IFolderListCallback {
        void getEulixSpaceFileListResult(Integer code, String currentDirectory, List<CustomizeFile> customizeFiles, PageInfo pageInfo);
        void createEulixSpaceDirectoryResult(Integer code, boolean isOk, String currentUUID, String folderName, String folderUuid);
        void uuidStackChange(int depth);
    }

    public FolderListController(@NonNull Context context) {
        mContext = context;
    }

    public void registerCallback(IFolderListCallback callback) {
        mCallback = callback;
    }

    private void generateValidAccessToken(String serviceFunction) {
        isQuery = false;
        if (mCallback != null && serviceFunction != null) {
            switch (serviceFunction) {
                case ConstantField.ServiceFunction.LIST_FOLDERS:
                    mCallback.getEulixSpaceFileListResult(ConstantField.OBTAIN_ACCESS_TOKEN_CODE, null, null, null);
                    break;
                case ConstantField.ServiceFunction.CREATE_FOLDER:
                    mCallback.createEulixSpaceDirectoryResult(ConstantField.OBTAIN_ACCESS_TOKEN_CODE, false, null, null, null);
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
                            List<FileListItem> fileListItemList = null;
                            if (fileListItems != null) {
                                for (FileListItem fileListItem : fileListItems) {
                                    if (fileListItem != null) {
                                        Boolean isFolder = fileListItem.getIs_dir();
                                        if (isFolder != null && isFolder) {
                                            if (fileListItemList == null) {
                                                fileListItemList = new ArrayList<>();
                                            }
                                            fileListItemList.add(fileListItem);
                                        }
                                    }
                                }
                            }
                            isQuery = false;
                            if (mCallback != null) {
                                mCallback.getEulixSpaceFileListResult(code, requestId, FileUtil.convertToCustomFileList(fileListItemList), pageInfo);
                            }
                        }

                        @Override
                        public void onFailed(Integer code, String message, String requestId) {
                            isQuery = false;
                            if (mCallback != null) {
                                mCallback.getEulixSpaceFileListResult(code, requestId, null, null);
                            }
                        }

                        @Override
                        public void onError(String msg) {
                            isQuery = false;
                            if (mCallback != null) {
                                mCallback.getEulixSpaceFileListResult(ConstantField.SERVER_EXCEPTION_CODE, null, null, null);
                            }
                        }
                    });
        } else {
            generateValidAccessToken(ConstantField.ServiceFunction.LIST_FOLDERS);
        }
    }

    /**
     * 新建在线文件夹
     * @param uuid
     * @param dirname
     */
    private void createFolder(UUID uuid, String dirname) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(mContext);
        if (gatewayCommunicationBase != null) {
            FileListUtil.createFolder(mContext, gatewayCommunicationBase.getBoxUuid(), uuid, dirname, gatewayCommunicationBase.getBoxDomain()
                    , gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                    , gatewayCommunicationBase.getTransformation(), gatewayCommunicationBase.getIvParams()
                    , true, new NewFolderRspCallback() {
                        @Override
                        public void onSuccess(Integer code, String message, String requestId, FileListItem fileListItem) {
                            String folderName = null;
                            String folderUuid = null;
                            if (fileListItem != null && fileListItem.getIs_dir()) {
                                folderName = fileListItem.getName();
                                folderUuid = fileListItem.getUuid();
                            }
                            if (mCallback != null) {
                                mCallback.createEulixSpaceDirectoryResult(code, true, requestId, folderName, folderUuid);
                            }
                        }

                        @Override
                        public void onFailed(Integer code, String message, String requestId) {
                            if (mCallback != null) {
                                mCallback.createEulixSpaceDirectoryResult(code, false, requestId, null, null);
                            }
                        }

                        @Override
                        public void onError(String msg) {
                            if (mCallback != null) {
                                mCallback.createEulixSpaceDirectoryResult(ConstantField.SERVER_EXCEPTION_CODE, false, null, null, null);
                            }
                        }
                    });
        } else {
            generateValidAccessToken(ConstantField.ServiceFunction.CREATE_FOLDER);
        }
    }

    private void uuidStackChange() {
        if (mCallback != null) {
            mCallback.uuidStackChange(getDepth());
        }
    }

    public int getDepth() {
        return (uuidStack == null ? 0 : uuidStack.size());
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

    public void resetUUID() {
        mUuid = null;
        if (uuidStack != null) {
            uuidStack.clear();
            uuidStack = null;
            uuidStackChange();
        }
    }

    public void resetUUID(ArrayStack<UUID> stack) {
        if (stack == null || stack.empty()) {
            resetUUID();
        } else {
            if (uuidStack == null) {
                uuidStack = new ArrayStack<>();
            } else {
                uuidStack.clear();
            }
            for (UUID uuid : stack) {
                uuidStack.push(uuid);
            }
            mUuid = uuidStack.peek();
            uuidStackChange();
        }
    }

    public List<CustomizeFile> getLocalEulixSpaceStorage(String currentId) {
        List<CustomizeFile> customizeFiles = null;
        List<FileListItem> fileListItems = null;
        String activeBoxUuid = null;
        String activeBoxBind = null;
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(mContext.getApplicationContext()
                , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (boxValues == null || boxValues.size() == 0) {
            EulixSpaceInfo eulixSpaceInfo = DataUtil.getLastEulixSpace(mContext);
            if (eulixSpaceInfo != null) {
                String lastBoxUuid = eulixSpaceInfo.getBoxUuid();
                String lastBoxBind = eulixSpaceInfo.getBoxBind();
                if (lastBoxUuid != null && lastBoxBind != null) {
                    Map<String, String> queryMap = new HashMap<>();
                    queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, lastBoxUuid);
                    queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, lastBoxBind);
                    boxValues = EulixSpaceDBUtil.queryBox(mContext, queryMap);
                }
            }
        }
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                        && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                    activeBoxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                    activeBoxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    break;
                }
            }
        }
        if (activeBoxUuid != null && activeBoxBind != null) {
            fileListItems = DataUtil.getFileListsMap(activeBoxUuid, activeBoxBind, currentId);
        }
        if (fileListItems == null) {
            fileListItems = EulixSpaceDBUtil.generateFileListItems(mContext.getApplicationContext(), currentId);
        }
        if (fileListItems != null) {
            customizeFiles = FileUtil.convertToCustomFileList(fileListItems);
        }
        return customizeFiles;
    }

    public void getEulixSpaceStorage(Integer page) {
        getEulixSpaceStorage(page, false);
    }

    public void getEulixSpaceStorage(Integer page, boolean isFore) {
        getEulixSpaceStorage(mUuid, page, null, null, null, isFore);
    }

    public void getEulixSpaceStorage(UUID uuid, Integer page, Integer pageSize, String order, String category) {
        getEulixSpaceStorage(uuid, page, pageSize, order, category, false);
    }

    public void getEulixSpaceStorage(UUID uuid, Integer page, Integer pageSize, String order, String category, boolean isFore) {
        if (uuid != null && ConstantField.UUID.FILE_ROOT_UUID.equals(uuid.toString())) {
            uuid = null;
        }
        isQuery = true;
        getFileList(uuid, page, pageSize, order, category, isFore);
    }

    public void createNewFolder(String folderName) {
        createNewFolder(mUuid, folderName);
    }

    public void createNewFolder(UUID uuid, String folderName) {
        if (uuid != null && uuid.toString().equals(ConstantField.UUID.FILE_ROOT_UUID)) {
            uuid = null;
        }
        createFolder(uuid, folderName);
    }

    public void handleNext(String currentDirectory) {
        if (uuidStack == null) {
            uuidStack = new ArrayStack<>();
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
        uuidStackChange();
    }

    public UUID handleBack() {
        UUID uuid = null;
        uuidStack.pop();
        if (uuidStack.empty()) {
            uuidStack = null;
        } else {
            uuid = uuidStack.peek();
        }
        mUuid = uuid;
        uuidStackChange();
        return uuid;
    }

    public UUID getmUuid() {
        return mUuid;
    }

    public ArrayStack<UUID> getUuidStack() {
        return uuidStack;
    }
}
