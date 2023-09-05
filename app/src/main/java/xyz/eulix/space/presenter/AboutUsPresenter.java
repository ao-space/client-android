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

import android.text.TextUtils;

import com.google.gson.Gson;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.bean.DeviceVersionInfoBean;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.database.EulixSpaceSharePreferenceHelper;
import xyz.eulix.space.event.BoxVersionDetailInfoEvent;
import xyz.eulix.space.interfaces.ResultCallbackObj;
import xyz.eulix.space.network.upgrade.UpgradeUtils;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.PreferenceUtil;
import xyz.eulix.space.util.SystemUtil;

/**
 * Author:      Zhu Fuyu
 * Description:
 * History:     2021/8/23
 */
public class AboutUsPresenter extends AbsPresenter<AboutUsPresenter.IAboutUs> {
    private boolean isUpdate = false;
    private Long apkSize = null;
    private String downloadUrl = "";
    private String md5 = "";
    private String newestVersion = "";
    private String updateDescription = "";
    private EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper;
    public DeviceVersionInfoBean deviceVersionInfoBean;

    public interface IAboutUs extends IBaseView {
        void refreshDeviceInfoViews(DeviceVersionInfoBean deviceVersionInfoBean);
    }



    public boolean isUpdate() {
        return isUpdate;
    }

    public void setUpdate(boolean update) {
        isUpdate = update;
    }

    public Long getApkSize() {
        return apkSize;
    }

    public void setApkSize(Long apkSize) {
        this.apkSize = apkSize;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getNewestVersion() {
        return newestVersion;
    }

    public void setNewestVersion(String newestVersion) {
        this.newestVersion = newestVersion;
    }

    public String getUpdateDescription() {
        return updateDescription;
    }

    public void setUpdateDescription(String updateDescription) {
        this.updateDescription = updateDescription;
    }

    public void initAppUpdate() {
        eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null) {
            String apkVersion = eulixSpaceSharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.APK_VERSION);
            setUpdate(SystemUtil.apkUpdate(apkVersion, SystemUtil.getVersionName(context)));
            setApkSize(eulixSpaceSharePreferenceHelper.getLong(ConstantField.EulixSpaceSPKey.APK_SIZE));
            setDownloadUrl(eulixSpaceSharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.APK_DOWNLOAD_URL));
            setMd5(eulixSpaceSharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.APK_MD5));
            setNewestVersion(apkVersion);
            setUpdateDescription(eulixSpaceSharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.APK_DESCRIPTION));
        }
    }

    public boolean isActiveUserAdmin() {
        return (ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY == EulixSpaceDBUtil.getActiveDeviceUserIdentity(context));
    }

    public boolean isSupportSystemUpdate() {
        boolean isSupport = true;
        DeviceAbility deviceAbility = EulixSpaceDBUtil.getActiveDeviceAbility(context, true);
        if (deviceAbility != null) {
            Boolean isUpgradeApiSupport = deviceAbility.getUpgradeApiSupport();
            if (isUpgradeApiSupport != null) {
                isSupport = isUpgradeApiSupport;
            }
        }
        return isSupport;
    }

    //获取系统信息详情
    public void getDeviceVersionDetailInfo() {
        UpgradeUtils.getDeviceVersionDetailInfo(context, new ResultCallbackObj() {
            @Override
            public void onResult(boolean result, Object extraObj) {
                Logger.d("zfy", "getDeviceVersionDetailInfo onResult:" + result + ",extraObj:" + extraObj.toString());
                if (result) {
                    deviceVersionInfoBean = (DeviceVersionInfoBean) extraObj;
                    Gson gson = new Gson();
                    String jsonStr = gson.toJson(deviceVersionInfoBean);
                    Logger.d("jsonStr=" + jsonStr);
                    if (!TextUtils.isEmpty(jsonStr)) {
                        PreferenceUtil.saveDeviceVersionDetailInfo(context, jsonStr);
                    }
                    iView.refreshDeviceInfoViews(deviceVersionInfoBean);
                    EventBusUtil.post(new BoxVersionDetailInfoEvent());
                } else {
                    iView.refreshDeviceInfoViews(null);
                }
            }

            @Override
            public void onError(String msg) {
                Logger.d("zfy", "getDeviceVersionDetailInfo onError " + msg);
                iView.refreshDeviceInfoViews(null);
            }
        });
    }
}
