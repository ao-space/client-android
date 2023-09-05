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

package xyz.eulix.space.presenter;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.CustomizeFile;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.network.files.FileListItem;
import xyz.eulix.space.network.files.FileListUtil;
import xyz.eulix.space.network.files.GetFileListCallback;
import xyz.eulix.space.network.files.PageInfo;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.GatewayUtils;
import xyz.eulix.space.util.ThreadPool;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/9/17 13:46
 */
public class FileSearchPresenter extends AbsPresenter<FileSearchPresenter.IFileSearch> {
    private UUID mUuid = null;

    public interface IFileSearch extends IBaseView {
        void searchEulixSpaceFileResult(Integer code, String currentDirectory, List<CustomizeFile> customizeFileList, PageInfo pageInfo, Integer fileCount);
    }

    private void generateValidAccessToken(String serviceFunction) {
        if (iView != null && serviceFunction != null) {
            switch (serviceFunction) {
                case ConstantField.ServiceFunction.SEARCH_FILES:
                    iView.searchEulixSpaceFileResult(ConstantField.OBTAIN_ACCESS_TOKEN_CODE, null, null, null, null);
                    break;
                default:
                    break;
            }
        }
    }

    private void searchFile(UUID uuid, String name, String category, Integer page, Integer pageSize, String order) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            FileListUtil.searchFile(context, gatewayCommunicationBase.getBoxUuid(), uuid, name, category
                    , page, pageSize, order, gatewayCommunicationBase.getBoxDomain()
                    , gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                    , gatewayCommunicationBase.getTransformation(), gatewayCommunicationBase.getIvParams()
                    , true, true, new GetFileListCallback() {
                        @Override
                        public void onSuccess(Integer code, String message, String requestId, List<FileListItem> fileListItems, PageInfo pageInfo, Integer fileCount) {
                            if (iView != null) {
                                iView.searchEulixSpaceFileResult(code, requestId, FileUtil.convertToCustomFileList(fileListItems), pageInfo, fileCount);
                            }
                        }

                        @Override
                        public void onFailed(Integer code, String message, String requestId) {
                            if (iView != null) {
                                iView.searchEulixSpaceFileResult(code, requestId, null, null, null);
                            }
                        }

                        @Override
                        public void onError(String msg) {
                            if (iView != null) {
                                iView.searchEulixSpaceFileResult(ConstantField.SERVER_EXCEPTION_CODE, null, null, null, null);
                            }
                        }
                    });
        } else {
            generateValidAccessToken(ConstantField.ServiceFunction.SEARCH_FILES);
        }
    }

    public void searchFile(String name, int page, String category) {
        searchFile(mUuid, name, category, page, null, null);
    }

    public List<CustomizeFile> getLocalEulixSpaceStorage(String currentId) {
        List<CustomizeFile> customizeFiles = null;
        List<FileListItem> fileListItems = null;
        String activeBoxUuid = null;
        String activeBoxBind = null;
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
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
            fileListItems = EulixSpaceDBUtil.generateFileListItems(context, currentId);
        }
        if (fileListItems != null) {
            customizeFiles = FileUtil.convertToCustomFileList(fileListItems);
        }
        return customizeFiles;
    }

    public UUID getmUuid() {
        return mUuid;
    }

    public void setmUuid(UUID mUuid) {
        this.mUuid = mUuid;
    }
}
