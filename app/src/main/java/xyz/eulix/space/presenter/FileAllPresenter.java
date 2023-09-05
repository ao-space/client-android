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
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.network.files.FileListItem;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.FileUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/7/19 18:32
 */
public class FileAllPresenter extends AbsPresenter<FileAllPresenter.IFileAll> {

    public interface IFileAll extends IBaseView {

    }

    public boolean isOnline() {
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        return (boxValues != null && boxValues.size() > 0);
    }

    public List<CustomizeFile> getLocalEulixSpaceStorage(String currentId) {
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
