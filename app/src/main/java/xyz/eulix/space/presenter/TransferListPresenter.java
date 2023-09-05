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

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.List;
import java.util.Map;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.bean.EulixBoxInfo;
import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.FormatUtil;

/**
 * Author:      Zhu Fuyu
 * Description:
 * History:     2021/8/13
 */
public class TransferListPresenter extends AbsPresenter<TransferListPresenter.ITransferList> {
    public interface ITransferList extends IBaseView{

    }

    @NonNull
    public String getSpaceStorageSizeContent() {
        String sizeContent = "--/--";
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context
                , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (boxValues == null || boxValues.size() <= 0) {
            boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
        }
        EulixBoxInfo boxInfo = null;
        String boxBind = null;
        Map<String, String> boxV = null;
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)
                        && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_INFO)) {
                    boxV = boxValue;
                    boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    String boxInfoValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_INFO);
                    if (boxInfoValue != null) {
                        try {
                            boxInfo = new Gson().fromJson(boxInfoValue, EulixBoxInfo.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                }
            }
        }
        if (boxInfo != null && boxBind != null) {
            long usedSize = -1;
            long totalSize = -1;
            if (isPhysicalDevice(DeviceAbility.generateDefault(boxInfo.getDeviceAbility()))) {
                usedSize = boxInfo.getUsedSize();
                totalSize = boxInfo.getTotalSize();
            } else {
                boolean isGranter = ("1".equals(boxBind) || "-1".equals(boxBind));
                UserInfo userInfo = DataUtil.getSpecificUserInfo(boxV, (isGranter
                        ? DataUtil.getClientUuid(context) : boxBind), isGranter);
                if (userInfo != null) {
                    usedSize = userInfo.getUsedSize();
                    totalSize = userInfo.getTotalSize();
                }
            }
            if (usedSize >= 0 && totalSize >= 0) {
                sizeContent = FormatUtil.formatSimpleSize(usedSize, ConstantField.SizeUnit.FORMAT_1F) +
                        "/" + FormatUtil.formatSimpleSize(totalSize, ConstantField.SizeUnit.FORMAT_1F);
            }
        }
        return sizeContent;
    }
}
