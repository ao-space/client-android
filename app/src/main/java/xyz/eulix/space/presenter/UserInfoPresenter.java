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

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.AOSpaceAccessBean;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.EulixBoxInfo;
import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.event.UserInfoEvent;
import xyz.eulix.space.network.userinfo.IUpdateUserInfoCallback;
import xyz.eulix.space.network.userinfo.UserInfoUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.PreferenceUtil;

/**
 * Author:      Zhu Fuyu
 * Description:
 * History:     2021/8/23
 */
public class UserInfoPresenter extends AbsPresenter<UserInfoPresenter.IUserInfo> {

    public interface IUserInfo extends IBaseView {
        void onUpdateHeaderResult(Boolean result, String message);
    }

    public void updateHeader(Context context,String pictureAbsolutePath){
        int index = pictureAbsolutePath.lastIndexOf("/");
        String headerFilePath = pictureAbsolutePath.substring(0, index);
        String headerFileName = pictureAbsolutePath.substring(index + 1);
        Logger.d("zfy", "headerPath = " + headerFilePath + ",headerFileName = " + headerFileName);
        String boxUuid = null;
        String boxBind = null;
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (boxValues == null || boxValues.isEmpty()) {
            boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
        }
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                        && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                    boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                    boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                }
            }
        }
        if (boxUuid == null || boxBind == null) {
            iView.onUpdateHeaderResult(false, null);
        } else {
            String clientUuid = DataUtil.getCompatibleClientUuid(context);
            String finalBoxUuid = boxUuid;
            String finalBoxBind = boxBind;
            UserInfoUtil.updateHeader(context, headerFilePath, headerFileName, (result, errorMsg) -> {
                if (result != null && result) {
                    //上传成功，保存头像到指定目录
                    String targetFilepath = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/headers/";
                    String lastHeaderImagePath = pictureAbsolutePath;
                    Logger.d("remove file from " + headerFilePath + " to " + targetFilepath);
                    File sourceFile = new File(pictureAbsolutePath);
                    boolean copyResult = FileUtil.copyFile(sourceFile.getAbsolutePath(), targetFilepath);
                    if (copyResult) {
                        boolean deleteResult = sourceFile.delete();
                        Logger.d("source file delete: " + deleteResult);
                        lastHeaderImagePath = targetFilepath + "/" + headerFileName;
                    }
                    Logger.d("lastHeaderPath = " + lastHeaderImagePath);
                    if (finalBoxUuid != null && finalBoxBind != null && clientUuid != null) {
                        Map<String, String> userMap = new HashMap<>();
                        userMap.put(UserInfoUtil.AVATAR_PATH, lastHeaderImagePath);
                        UserInfoUtil.updateUserInfoDB(context.getApplicationContext(), finalBoxUuid, finalBoxBind, clientUuid, userMap);
                    }
                    PreferenceUtil.saveHeaderPath(context, lastHeaderImagePath);
                    EulixBoxBaseInfo boxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(context);
                    if (boxBaseInfo != null) {
                        String activeBoxUuid = boxBaseInfo.getBoxUuid();
                        String activeBoxBind = boxBaseInfo.getBoxBind();
                        if (activeBoxUuid != null && activeBoxUuid.equals(finalBoxUuid)
                                && activeBoxBind != null && activeBoxBind.equals(finalBoxBind)) {
                            EventBusUtil.post(new UserInfoEvent(UserInfoEvent.TYPE_HEADER, lastHeaderImagePath, null, null));
                        }
                    }
                }
                iView.onUpdateHeaderResult(result, errorMsg);
            });
        }
    }

    public UserInfo getActiveUserInfo() {
        return EulixSpaceDBUtil.getCompatibleActiveUserInfo(context);
//        String clientUuid = DataUtil.getClientUuid(context);
//        return EulixSpaceDBUtil.getActiveUserInfo(context, clientUuid);
    }

    public String getUserDomain() {
        String userDomain = null;
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context
                , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (boxValues == null || boxValues.size() <= 0) {
            boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
        }
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_DOMAIN)) {
                    userDomain = boxValue.get(EulixSpaceDBManager.FIELD_BOX_DOMAIN);
                    break;
                }
            }
        }
        return userDomain;
    }

    public boolean isActiveGranter() {
        int identity = EulixSpaceDBUtil.getActiveDeviceUserIdentity(context);
        return (ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY == identity || ConstantField.UserIdentity.MEMBER_IDENTITY == identity);
    }

    public boolean isActiveAdminGranter() {
        int identity = EulixSpaceDBUtil.getActiveDeviceUserIdentity(context);
        return (ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY == identity);
    }

    public AOSpaceAccessBean getActiveAOSpaceAccessBean() {
        AOSpaceAccessBean aoSpaceAccessBean = null;
        EulixBoxInfo eulixBoxInfo = EulixSpaceDBUtil.getActiveBoxInfo(context);
        if (eulixBoxInfo != null) {
            aoSpaceAccessBean = eulixBoxInfo.getAoSpaceAccessBean();
        }
        return aoSpaceAccessBean;
    }

    public EulixBoxBaseInfo getActiveBoxBaseInfo() {
        return EulixSpaceDBUtil.getActiveBoxBaseInfo(context);
    }
}
