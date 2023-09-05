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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.CustomizeFile;
import xyz.eulix.space.bean.EulixSpaceInfo;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.network.files.FileListItem;
import xyz.eulix.space.network.files.FileListUtil;
import xyz.eulix.space.network.files.PageInfo;
import xyz.eulix.space.network.files.RecycledListCallback;
import xyz.eulix.space.network.files.RecycledListResponse;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.GatewayUtils;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/3/10 16:00
 */
public class RecycleBinPresenter extends AbsPresenter<RecycleBinPresenter.IRecycleBin> {
    public interface IRecycleBin extends IBaseView {
        void recycleBinFileResult(Integer code, List<CustomizeFile> customizeFileList, PageInfo pageInfo);
    }

    private void generateValidAccessToken(String serviceFunction) {
        if (iView != null && serviceFunction != null) {
            switch (serviceFunction) {
                case ConstantField.ServiceFunction.LIST_RECYCLED:
                    iView.recycleBinFileResult(ConstantField.OBTAIN_ACCESS_TOKEN_CODE, null, null);
                    break;
                default:
                    break;
            }
        }
    }

    private void getRecycleBinFile(Integer page, Integer pageSize) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            FileListUtil.getRecycledList(context, gatewayCommunicationBase.getBoxUuid(), gatewayCommunicationBase.getBoxBind()
                    , page, pageSize, gatewayCommunicationBase.getBoxDomain(), gatewayCommunicationBase.getAccessToken()
                    , gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getTransformation()
                    , gatewayCommunicationBase.getIvParams(), true, true, new RecycledListCallback() {
                        @Override
                        public void onSuccess(Integer code, String message, String requestId, List<FileListItem> fileListItems, PageInfo pageInfo, RecycledListResponse response) {
                            if (iView != null) {
                                iView.recycleBinFileResult(code, FileUtil.convertToCustomFileList(fileListItems), pageInfo);
                            }
                        }

                        @Override
                        public void onFailed(Integer code, String message, String requestId) {
                            if (iView != null) {
                                iView.recycleBinFileResult(code, null, null);
                            }
                        }

                        @Override
                        public void onError(String msg) {
                            if (iView != null) {
                                iView.recycleBinFileResult(ConstantField.SERVER_EXCEPTION_CODE, null, null);
                            }
                        }
                    });
        } else {
            generateValidAccessToken(ConstantField.ServiceFunction.LIST_RECYCLED);
        }
    }

    public void getRecycleBinFile(int page) {
        getRecycleBinFile(page, null);
    }

    public List<CustomizeFile> getLocalEulixSpaceStorage() {
        String currentId = ConstantField.Category.FILE_RECYCLE;
        List<CustomizeFile> customizeFiles = null;
        List<FileListItem> fileListItems = null;
        String activeBoxUuid = null;
        String activeBoxBind = null;
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (boxValues == null || boxValues.size() == 0) {
            EulixSpaceInfo eulixSpaceInfo = DataUtil.getLastEulixSpace(context);
            if (eulixSpaceInfo != null) {
                String lastBoxUuid = eulixSpaceInfo.getBoxUuid();
                String lastBoxBind = eulixSpaceInfo.getBoxBind();
                if (lastBoxUuid != null && lastBoxBind != null) {
                    Map<String, String> queryMap = new HashMap<>();
                    queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, lastBoxUuid);
                    queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, lastBoxBind);
                    boxValues = EulixSpaceDBUtil.queryBox(context, queryMap);
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
            fileListItems = EulixSpaceDBUtil.generateFileListItems(context, currentId);
        }
        if (fileListItems != null) {
            customizeFiles = FileUtil.convertToCustomFileList(fileListItems);
        }
        return customizeFiles;
    }
}
