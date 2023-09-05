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

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.ArrayStack;
import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.EulixBoxInfo;
import xyz.eulix.space.bean.EulixBoxOtherInfo;
import xyz.eulix.space.bean.EulixUser;
import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.database.EulixSpaceSharePreferenceHelper;
import xyz.eulix.space.database.cache.CacheDBManager;
import xyz.eulix.space.did.event.DIDDocumentRequestEvent;
import xyz.eulix.space.event.AccessInfoRequestEvent;
import xyz.eulix.space.event.AppCheckResponseEvent;
import xyz.eulix.space.event.BoxVersionCheckEvent;
import xyz.eulix.space.event.DeviceAbilityRequestEvent;
import xyz.eulix.space.event.DeviceHardwareInfoRequestEvent;
import xyz.eulix.space.event.DeviceNetworkEvent;
import xyz.eulix.space.network.gateway.GatewayUtil;
import xyz.eulix.space.network.gateway.IVersionCheckCallback;
import xyz.eulix.space.network.gateway.VersionCheckResponseBody;
import xyz.eulix.space.network.userinfo.UserInfoUtil;
import xyz.eulix.space.transfer.db.TransferDBManager;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.DebugUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.NetUtils;
import xyz.eulix.space.util.PreferenceUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.SystemUtil;
import xyz.eulix.space.util.ThreadPool;

/**
 * Author:      Zhu Fuyu
 * Description:
 * History:     2021/7/16
 */
public class MainPresenter extends AbsPresenter<MainPresenter.IMain> {
    private static final String TAG = MainPresenter.class.getSimpleName();
    private ArrayStack<UUID> uuids;
    private Long apkSize = null;
    private String downloadUrl = "";
    private String md5 = "";
    private String newestVersion = "";
    private String updateDescription = "";
    private int mPage = 1;
    private boolean isAppUpdateRemindForce;

    private IVersionCheckCallback mainVersionCheckCallback = new IVersionCheckCallback() {
        @Override
        public void onResult(VersionCheckResponseBody result) {
            if (result != null) {
                VersionCheckResponseBody.Results results = result.results;
                if (results != null) {
                    EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
                    boolean isNewAppVersionExist = false;
                    Boolean newVersionExist = results.newVersionExist;
                    Long pkgSize = null;
                    String downloadUrl = null;
                    boolean isUpdate = false;
                    String md5 = null;
                    String pkgVersion = null;
                    String updateDescription = null;
                    if (newVersionExist == null || newVersionExist) {
                        VersionCheckResponseBody.Results.LatestAppPkg latestAppPkg = results.latestAppPkg;
                        if (latestAppPkg != null) {
                            isNewAppVersionExist = true;
                            String pkgName = latestAppPkg.pkgName;
                            String pkgType = StringUtil.nullToEmpty(latestAppPkg.pkgType);
                            pkgVersion = latestAppPkg.pkgVersion;
                            pkgSize = latestAppPkg.pkgSize;
                            downloadUrl = latestAppPkg.downloadUrl;
                            updateDescription = latestAppPkg.updateDesc;
                            md5 = latestAppPkg.md5;
                            Boolean isForceUpdate = latestAppPkg.isForceUpdate;
                            String minAndroidVersion = latestAppPkg.minAndroidVersion;
                            setNewestVersion(pkgVersion);
                            setApkSize(pkgSize);
                            setDownloadUrl(downloadUrl);
                            setMd5(md5);
                            setUpdateDescription(updateDescription);
                            if (downloadUrl != null && !TextUtils.isEmpty(downloadUrl) && (TextUtils.isEmpty(pkgType) || "android".equalsIgnoreCase(pkgType))) {
                                if (eulixSpaceSharePreferenceHelper != null) {
                                    eulixSpaceSharePreferenceHelper.setLong(ConstantField.EulixSpaceSPKey.APK_SIZE, pkgSize, false);
                                    eulixSpaceSharePreferenceHelper.setString(ConstantField.EulixSpaceSPKey.APK_DOWNLOAD_URL, downloadUrl, false);
                                    eulixSpaceSharePreferenceHelper.setString(ConstantField.EulixSpaceSPKey.APK_MD5, md5, false);
                                    eulixSpaceSharePreferenceHelper.setString(ConstantField.EulixSpaceSPKey.APK_VERSION, pkgVersion, false);
                                    eulixSpaceSharePreferenceHelper.setString(ConstantField.EulixSpaceSPKey.APK_DESCRIPTION, updateDescription, false);
                                }
                            }
                            if (isForceUpdate != null && isForceUpdate) {
                                isUpdate = true;
                            } else {
                                // todo 暂时不判
//                                isUpdate = SystemUtil.apkUpdate(pkgVersion, SystemUtil.getVersionName(context));
                                isUpdate = isNewAppVersionExist;
                            }
                        }
                    }
                    if (!isNewAppVersionExist && eulixSpaceSharePreferenceHelper != null) {
                        eulixSpaceSharePreferenceHelper.remove(ConstantField.EulixSpaceSPKey.APK_SIZE, true);
                        eulixSpaceSharePreferenceHelper.remove(ConstantField.EulixSpaceSPKey.APK_DOWNLOAD_URL, true);
                        eulixSpaceSharePreferenceHelper.remove(ConstantField.EulixSpaceSPKey.APK_MD5, true);
                        eulixSpaceSharePreferenceHelper.remove(ConstantField.EulixSpaceSPKey.APK_VERSION, true);
                        eulixSpaceSharePreferenceHelper.remove(ConstantField.EulixSpaceSPKey.APK_DESCRIPTION, true);
                    }
                    if (isUpdate && pkgVersion != null && !pkgVersion.equals(DataUtil.getAppUpdateVersion(context)) && iView != null) {
                        iView.appVersionUpdateCallback(pkgVersion);
                    }
                }
            }
        }

        @Override
        public void onError(String msg) {
            // Do nothing
        }
    };

    private IVersionCheckCallback appVersionCheckCallback = new IVersionCheckCallback() {
        @Override
        public void onResult(VersionCheckResponseBody result) {
            if (result != null) {
                if (!TextUtils.isEmpty(result.getCode()) && result.getCode().contains(String.valueOf(ConstantField.ErrorCode.PRODUCT_PLATFORM_CONNECT_ERROR))) {
                    AppCheckResponseEvent responseEvent = new AppCheckResponseEvent(null, null, null, null, null, null, isAppUpdateRemindForce, true);
                    EventBusUtil.post(responseEvent);
                    return;
                }

                VersionCheckResponseBody.Results results = result.results;
                if (results != null) {
                    EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
                    boolean isNewAppVersionExist = false;
                    Boolean newVersionExist = results.newVersionExist;
                    Long pkgSize = null;
                    String downloadUrl = null;
                    boolean isUpdate = false;
                    String md5 = null;
                    String pkgVersion = null;
                    String updateDescription = null;
                    if (newVersionExist == null || newVersionExist) {
                        VersionCheckResponseBody.Results.LatestAppPkg latestAppPkg = results.latestAppPkg;
                        if (latestAppPkg != null) {
                            isNewAppVersionExist = true;
                            String pkgName = latestAppPkg.pkgName;
                            String pkgType = StringUtil.nullToEmpty(latestAppPkg.pkgType);
                            pkgVersion = latestAppPkg.pkgVersion;
                            pkgSize = latestAppPkg.pkgSize;
                            downloadUrl = latestAppPkg.downloadUrl;
                            updateDescription = latestAppPkg.updateDesc;
                            md5 = latestAppPkg.md5;
                            Boolean isForceUpdate = latestAppPkg.isForceUpdate;
                            String minAndroidVersion = latestAppPkg.minAndroidVersion;
                            if (downloadUrl != null && !TextUtils.isEmpty(downloadUrl) && (TextUtils.isEmpty(pkgType) || "android".equalsIgnoreCase(pkgType))) {
                                if (eulixSpaceSharePreferenceHelper != null) {
                                    eulixSpaceSharePreferenceHelper.setLong(ConstantField.EulixSpaceSPKey.APK_SIZE, pkgSize, false);
                                    eulixSpaceSharePreferenceHelper.setString(ConstantField.EulixSpaceSPKey.APK_DOWNLOAD_URL, downloadUrl, false);
                                    eulixSpaceSharePreferenceHelper.setString(ConstantField.EulixSpaceSPKey.APK_MD5, md5, false);
                                    eulixSpaceSharePreferenceHelper.setString(ConstantField.EulixSpaceSPKey.APK_VERSION, pkgVersion, false);
                                    eulixSpaceSharePreferenceHelper.setString(ConstantField.EulixSpaceSPKey.APK_DESCRIPTION, updateDescription, false);
                                }
                            }
                            if (isForceUpdate != null && isForceUpdate) {
                                isUpdate = true;
                            } else {
                                // todo 暂时不判
//                                isUpdate = SystemUtil.apkUpdate(pkgVersion, SystemUtil.getVersionName(context));
                                isUpdate = isNewAppVersionExist;
                            }
                        }
                    }
                    if (!isNewAppVersionExist && eulixSpaceSharePreferenceHelper != null) {
                        eulixSpaceSharePreferenceHelper.remove(ConstantField.EulixSpaceSPKey.APK_SIZE, true);
                        eulixSpaceSharePreferenceHelper.remove(ConstantField.EulixSpaceSPKey.APK_DOWNLOAD_URL, true);
                        eulixSpaceSharePreferenceHelper.remove(ConstantField.EulixSpaceSPKey.APK_MD5, true);
                        eulixSpaceSharePreferenceHelper.remove(ConstantField.EulixSpaceSPKey.APK_VERSION, true);
                        eulixSpaceSharePreferenceHelper.remove(ConstantField.EulixSpaceSPKey.APK_DESCRIPTION, true);
                    }
                    AppCheckResponseEvent responseEvent = new AppCheckResponseEvent(pkgSize, downloadUrl, isUpdate, md5, pkgVersion, updateDescription, isAppUpdateRemindForce, false);
                    EventBusUtil.post(responseEvent);
                }
            }
        }

        @Override
        public void onError(String msg) {
            boolean isPlatformConnectFail = false;
            if (!TextUtils.isEmpty(msg) && msg.contains(String.valueOf(ConstantField.ErrorCode.PRODUCT_PLATFORM_CONNECT_ERROR))) {
                isPlatformConnectFail = true;
            }
            AppCheckResponseEvent responseEvent = new AppCheckResponseEvent(null, null, null, null, null, null, isAppUpdateRemindForce, isPlatformConnectFail);
            EventBusUtil.post(responseEvent);
        }
    };

    public interface IMain extends IBaseView {
        void appVersionUpdateCallback(String versionName);
    }

    public boolean deviceRegister() {
        boolean result = true;
        String eulixToken = null;
        boolean isEnable = true;
        String platformServices = DebugUtil.getEnvironmentServices();
        EulixBoxOtherInfo eulixBoxOtherInfo = EulixSpaceDBUtil.getActiveBoxOtherInfo(context);
        if (eulixBoxOtherInfo != null) {
            Map<String, String> tokenMap = eulixBoxOtherInfo.getEulixPushDeviceTokenMap();
            if (tokenMap != null && platformServices != null && tokenMap.containsKey(platformServices)) {
                eulixToken = tokenMap.get(platformServices);
            }
        }
        Logger.d(TAG, "platform: " + platformServices + ", support device register: " + isEnable);
        if (!isEnable) {
            return result;

        }
        return result;
    }

    public boolean getNotificationReminderVersion() {
        boolean isUpdate = true;
        String currentVersion = SystemUtil.getVersionName(context);
        String reminderVersion = DataUtil.getNotificationReminderVersion(context);
        if (reminderVersion != null && reminderVersion.equals(currentVersion)) {
            isUpdate = false;
        }
        return isUpdate;
    }

    public void setNotificationReminderVersion() {
        DataUtil.setNotificationReminderVersion(context, SystemUtil.getVersionName(context));
    }

    public void setAppVersionUpdate(String versionName) {
        DataUtil.setAppUpdateVersion(context, versionName);
    }

    public EulixBoxBaseInfo getActiveBoxUuid() {
        return EulixSpaceDBUtil.getActiveBoxBaseInfo(context);
    }

    public boolean updateNetwork() {
        boolean result = false;
        if (NetUtils.isNetAvailable(context)) {
            EulixBoxBaseInfo eulixBoxBaseInfo = getActiveBoxUuid();
            if (eulixBoxBaseInfo != null) {
                result = true;
                String boxUuid = eulixBoxBaseInfo.getBoxUuid();
                String boxBind = eulixBoxBaseInfo.getBoxBind();
                String boxDomain = eulixBoxBaseInfo.getBoxDomain();
                if (boxUuid != null && boxBind != null && boxDomain != null) {
                    DeviceNetworkEvent deviceNetworkEvent = new DeviceNetworkEvent(boxUuid, boxBind, boxDomain);
                    EventBusUtil.post(deviceNetworkEvent);
                }
            }
        }
        return result;
    }

    public void checkAppUpdate() {
        try {
            ThreadPool.getInstance().execute(() -> GatewayUtil.checkVersionBoxOrApp(context, false, mainVersionCheckCallback));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    public void checkAppUpdate(boolean isForce) {
        isAppUpdateRemindForce = isForce;
        String pkgNameValue = context.getPackageName();
        if (pkgNameValue == null) {
            pkgNameValue = ConstantField.PACKAGE_NAME;
        }
        String versionNameValue = SystemUtil.getVersionName(context);
        if (versionNameValue == null) {
            versionNameValue = "0.0.0";
        }
        String pkgName = pkgNameValue;
        String versionName = versionNameValue;
        GatewayUtil.checkVersionBoxOrApp(context, false, isForce, appVersionCheckCallback);
    }

    public String getApkDownloadPath() {
        return DataUtil.getApkDownloadPath(context);
    }

    public boolean setApkDownloadPath(String apkDownloadPath) {
        return DataUtil.setApkDownloadPath(context, apkDownloadPath, true);
    }

    public int getIdentity() {
        return EulixSpaceDBUtil.getActiveDeviceUserIdentity(context);
    }

    public EulixBoxBaseInfo getActiveBoxBaseInfo() {
        return EulixSpaceDBUtil.getActiveBoxBaseInfo(context, true);
    }

    public boolean getBluetoothId(@NonNull String requestUuid) {
        boolean isHandle = false;
        EulixBoxBaseInfo eulixBoxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(context, true);
        if (eulixBoxBaseInfo != null) {
            String boxUuid = eulixBoxBaseInfo.getBoxUuid();
            String boxBind = eulixBoxBaseInfo.getBoxBind();
            if (boxUuid != null && boxBind != null) {
                isHandle = true;
                EventBusUtil.post(new DeviceHardwareInfoRequestEvent(boxUuid, boxBind, requestUuid));
            }
        }
        return isHandle;
    }

    public boolean updateMemberList(boolean isForce) {
        boolean result = false;
        EulixBoxBaseInfo eulixBoxBaseInfo = getActiveBoxUuid();
        if (eulixBoxBaseInfo != null) {
            result = true;
            String boxUuid = eulixBoxBaseInfo.getBoxUuid();
            String boxBind = eulixBoxBaseInfo.getBoxBind();
            String boxDomain = eulixBoxBaseInfo.getBoxDomain();
            if (boxUuid != null && boxBind != null && boxDomain != null) {
                UserInfoUtil.changeBox(boxUuid, boxBind, boxDomain, isForce);
            }
        }
        return result;
    }

    public boolean updateDeviceAbility(boolean isForce) {
        boolean result = false;
        EulixBoxBaseInfo eulixBoxBaseInfo = getActiveBoxUuid();
        if (eulixBoxBaseInfo != null) {
            result = true;
            String boxUuid = eulixBoxBaseInfo.getBoxUuid();
            String boxBind = eulixBoxBaseInfo.getBoxBind();
            if (boxUuid != null && boxBind != null) {
                if (isForce) {
                    EventBusUtil.post(new DeviceAbilityRequestEvent(boxUuid, boxBind));
                } else {
                    DeviceAbility deviceAbility = null;
                    EulixBoxInfo eulixBoxInfo = EulixSpaceDBUtil.getSpecificBoxInfo(context, boxUuid, boxBind);
                    if (eulixBoxInfo != null) {
                        deviceAbility = eulixBoxInfo.getDeviceAbility();
                    }
                    if (deviceAbility == null) {
                        EventBusUtil.post(new DeviceAbilityRequestEvent(boxUuid, boxBind, true));
                    }
                }
            }
        }
        return result;
    }

    public void updateCommonInfo() {
        EulixBoxBaseInfo eulixBoxBaseInfo = getActiveBoxUuid();
        if (eulixBoxBaseInfo != null) {
            String boxUuid = eulixBoxBaseInfo.getBoxUuid();
            String boxBind = eulixBoxBaseInfo.getBoxBind();
            if (boxUuid != null && boxBind != null) {
                EventBusUtil.post(new AccessInfoRequestEvent(boxUuid, boxBind));
                EventBusUtil.post(new DIDDocumentRequestEvent(boxUuid, boxBind));
            }
        }
    }

    public ArrayStack<UUID> getUuids() {
        return uuids;
    }

    public void setUuids(ArrayStack<UUID> uuids) {
        this.uuids = uuids;
    }

    public boolean isActiveUserAdmin() {
        return (ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY == EulixSpaceDBUtil.getActiveDeviceUserIdentity(context));
    }

    public DeviceAbility getActiveDeviceAbility() {
        return EulixSpaceDBUtil.getActiveDeviceAbility(context, true);
    }

    //检查盒子系统版本更新
    public void checkBoxVersion() {
        GatewayUtil.checkVersionBoxOrApp(context, true, new IVersionCheckCallback() {
            @Override
            public void onResult(VersionCheckResponseBody responseBody) {
                if (responseBody != null && responseBody.results != null) {
                    VersionCheckResponseBody.Results results = responseBody.results;
                    Logger.d("zfy", "result newVersionExist:" + results.newVersionExist);

                    if (results.newVersionExist && results.latestBoxPkg != null) {
                        ConstantField.boxVersionCheckBody = results;
                        EventBusUtil.post(new BoxVersionCheckEvent());
                        return;
                    }
                }
                ConstantField.boxVersionCheckBody = null;
            }

            @Override
            public void onError(String msg) {
                Logger.d("zfy", "checkVersion error:" + msg);
            }
        });
    }

    public void getCurrentBoxVersion() {
        GatewayUtil.getCurrentBoxVersion(context, (result, extraMsg) -> {
            if (result && !TextUtils.isEmpty(extraMsg)) {
                Logger.d("zfy", "get current box version:" + extraMsg);
                PreferenceUtil.saveCurrentBoxVersion(context, extraMsg);
            }
        });
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

    public void upgradeDbAccountValue() {
        Logger.d("zfy", "#upgradeDbAccountValue");
        ThreadPool.getInstance().getBackThreadPoolExecutor().execute(() -> {
            String clientUuid = DataUtil.getClientUuid(EulixSpaceApplication.getContext());
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(EulixSpaceApplication.getContext());
            if (boxValues != null) {
                HashMap<String, Boolean> boxUpgradeStateMap = new HashMap<>();
                for (int i = 0; i < boxValues.size(); i++) {
                    Map<String, String> boxValue = boxValues.get(i);
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                            && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)
                            && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_STATUS)) {

                        String boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                        Logger.d("zfy", "boxUuid=" + boxUuid);
                        if (boxUpgradeStateMap.containsKey(boxUuid) && Boolean.TRUE.equals(boxUpgradeStateMap.get(boxUuid))) {
                            //该盒子已处理过
                            Logger.d("zfy", "this box value has upgraded");
                            continue;
                        }
                        boolean hasMultiUser = false;
                        for (int j = i + 1; j < boxValues.size(); j++) {
                            if (Objects.equals(boxValues.get(j).get(EulixSpaceDBManager.FIELD_BOX_UUID), boxUuid)) {
                                hasMultiUser = true;
                                break;
                            }
                        }
                        if (hasMultiUser) {
                            //该盒子上有多个用户
                            Logger.d("zfy", "has multi user");
                            CacheDBManager.getInstance(EulixSpaceApplication.getContext()).deleteByAccount(boxUuid);
                            TransferDBManager.getInstance(EulixSpaceApplication.getContext()).deleteByAccount(boxUuid);
                        } else {
                            //该盒子上只有一个用户
                            Logger.d("zfy", "only one user");
                            String userId = getUserIdByBox(boxValue, clientUuid);
                            Logger.d("zfy", "userId = " + userId);
                            String newAccount = boxUuid + userId;
                            CacheDBManager.getInstance(EulixSpaceApplication.getContext()).upgradeAccountValue(boxUuid, newAccount);
                            TransferDBManager.getInstance(EulixSpaceApplication.getContext()).upgradeAccountValue(boxUuid, newAccount);
                        }
                        boxUpgradeStateMap.put(boxUuid, Boolean.TRUE);
                    }
                }
            }
            PreferenceUtil.saveHasUpgradeDbAccountValue(context, true);
        });
    }

    private String getUserIdByBox(Map<String, String> boxValue, String clientUuid) {
        String userId = "";
        String statusValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_STATUS);
        if (statusValue != null) {
            int status = -1;
            try {
                status = Integer.parseInt(statusValue);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
            if ((status >= ConstantField.EulixDeviceStatus.OFFLINE && status <= ConstantField.EulixDeviceStatus.ONLINE_UNINITIALIZED) || status == ConstantField.EulixDeviceStatus.INVALID) {
                EulixUser user = new EulixUser();
                String boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                String boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                user.setUuid(boxUuid);
                user.setBind(boxBind);

                String userInfoValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_USER_INFO);
                if (userInfoValue != null) {
                    Map<String, UserInfo> userInfoMap = null;
                    try {
                        userInfoMap = new Gson().fromJson(userInfoValue, new TypeToken<Map<String, UserInfo>>() {
                        }.getType());
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                    }
                    if (userInfoMap != null) {
                        Set<Map.Entry<String, UserInfo>> entrySet = userInfoMap.entrySet();
                        boolean findClient = false;
                        boolean findAdmin = false;
                        String bind = user.getBind();
                        for (Map.Entry<String, UserInfo> entry : entrySet) {
                            if (entry != null) {
                                String uuid = entry.getKey();
                                UserInfo userInfo = entry.getValue();
                                if (uuid != null && userInfo != null) {
                                    boolean isClient;
                                    if (bind != null && !"1".equals(bind) && !"-1".equals(bind) && !"0".equals(bind)) {
                                        String aoId = userInfo.getUserId();
                                        isClient = (bind.equals(aoId));
                                    } else {
                                        isClient = uuid.equals(clientUuid);
                                    }
                                    if (isClient) {
                                        userId = userInfo.getUserId();
                                        findClient = true;
                                    }
                                    if (userInfo.isAdmin()) {
                                        findAdmin = true;
                                    }
                                }
                            }
                            if (findClient && findAdmin) {
                                break;
                            }
                        }
                    }
                }

            }
        }
        return userId;
    }
}
