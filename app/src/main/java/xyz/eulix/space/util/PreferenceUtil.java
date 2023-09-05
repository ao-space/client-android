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

package xyz.eulix.space.util;

import android.content.Context;
import android.content.SharedPreferences;

import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.database.EulixSpaceDBUtil;

/**
 * Author: 		Zhufy
 * Description: SharedPreference工具类，用于持久化轻量级数据
 * History:		2021/7/14
 */
public class PreferenceUtil {
    private static final String PREFERENCE_NAME = "eulixspace";
    private static final String KEY_GUIDE_SHOWED = "key_guide_first";
    private static final String KEY_HAS_NEW_UPLOAD_TASK = "key_has_new_upload_task";
    private static final String KEY_CLOSE_NOVICE_GUIDE = "key_close_novice_guide";
    private static final String KEY_NICK_NAME = "key_nick_name";
    private static final String KEY_SIGNATURE = "key_signature";
    private static final String KEY_HEADER_PATH = "key_header_path";
    private static final String KEY_SYNC_PICTURE = "key_sync_picture";
    private static final String KEY_SYNC_VIDEO = "key_sync_video";
    private static final String KEY_SYNC_BACKGROUND = "key_sync_background";
    private static final String KEY_SYNC_MOBILE_DATA_USAGE = "key_mobile_data_usage";
    private static final String KEY_SYNC_NOTICE_DATE = "key_sync_notice_date";
    private static final String KEY_SYNC_LAST_TIME = "key_sync_last_time";
    private static final String KEY_DOING_BACKUP_OR_RESTORE_TYPE = "key_doing_backup_or_restore_type";
    private static final String KEY_DOING_BACKUP_OR_RESTORE_ID = "key_doing_backup_or_restore_id";
    private static final String KEY_DOING_RESTORE_USER_DOMAIN = "key_doing_backup_or_restore_id";
    private static final String KEY_IS_LAST_BACKUP_NO_FINISH = "key_is_last_backup_no_finish";
    private static final String KEY_LAST_RESTORE_SOURCE = "key_last_restore_source";
    private static final String KEY_LAST_RESTORE_USER = "key_last_restore_user";
    private static final String KEY_UPGRADE_AUTO_DOWNLOAD = "key_upgrade_auto_download";
    private static final String KEY_UPGRADE_AUTO_INSTALL = "key_upgrade_auto_install";
    private static final String KEY_CURRENT_BOX_VERSION = "key_current_box_version";
    private static final String KEY_PRIVACY_AGREEMENT = "key_privacy_agreement";
    private static final String KEY_SYNC_DATE = "key_sync_date";
    private static final String KEY_SYNC_COUNT = "key_sync_count";
    private static final String KEY_LOGGER_SWITCH = "key_logger_switch";
    private static final String KEY_DEVICE_VERSION_DETAIL_INFO = "key_device_version_detail_info";
    private static final String KEY_APPLET_AUTH = "key_applet_auth";
    private static final String KEY_APPLET_UPDATE_VERSION = "key_applet_update_version";
    private static final String KEY_ALBUM_LAST_OPERATE_ID = "key_album_last_operate_id";
    private static final String KEY_LAN_DOMAIN = "key_lan_domain";
    private static final String KEY_AUTHENTICATOR_SETTED = "key_authenticator_setted";
    private static final String KEY_HAS_UPGRADE_DB_ACCOUNT_VALUE = "key_has_upgrade_db_account_value";

    public static void saveGuideShowed(Context context, boolean hasShowed) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(KEY_GUIDE_SHOWED, hasShowed).apply();
    }

    //获取引导页是否已访问
    public static boolean getGuideShowed(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(KEY_GUIDE_SHOWED, false);
    }

    public static void saveHasNewUploadTask(Context context, boolean hasNewUpload) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        preferences.edit().putBoolean(KEY_HAS_NEW_UPLOAD_TASK, hasNewUpload).apply();
    }

    //是否有新的上传任务
    public static boolean getHasNewUploadTask(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getBoolean(KEY_HAS_NEW_UPLOAD_TASK, false);
    }

    public static void saveCloseNoviceGuide(Context context, boolean closeNoviceGuide) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        preferences.edit().putBoolean(KEY_CLOSE_NOVICE_GUIDE, closeNoviceGuide).apply();
    }

    //是否关闭了新手指南
    public static boolean getCloseNoviceGuide(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getBoolean(KEY_CLOSE_NOVICE_GUIDE, false);
    }

    public static void saveNickname(Context context, String nickName) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_NICK_NAME, nickName).apply();
    }

    //获取昵称
    public static String getNickname(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getString(KEY_NICK_NAME, "");
    }

    public static void saveSignature(Context context, String signature) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_SIGNATURE, signature).apply();
    }

    //获取签名
    public static String getSignature(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getString(KEY_SIGNATURE, "");
    }

    public static void saveHeaderPath(Context context, String headerPath) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_HEADER_PATH, headerPath).apply();
    }

    //获取头像路径
    public static String getHeaderPath(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getString(KEY_HEADER_PATH, null);
    }

    public static void saveSyncPictures(Context context, boolean flag) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        preferences.edit().putBoolean(KEY_SYNC_PICTURE, flag).apply();
    }

    //自动同步照片
    public static boolean getSyncPictures(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getBoolean(KEY_SYNC_PICTURE, false);
    }

    public static void saveSyncVideos(Context context, boolean flag) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        preferences.edit().putBoolean(KEY_SYNC_VIDEO, flag).apply();
    }

    //自动同步视频
    public static boolean getSyncVideos(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getBoolean(KEY_SYNC_VIDEO, false);
    }

    public static void saveSyncBackground(Context context, boolean flag) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        preferences.edit().putBoolean(KEY_SYNC_BACKGROUND, flag).apply();
    }

    //自动后台同步
    public static boolean getSyncBackground(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getBoolean(KEY_SYNC_BACKGROUND, false);
    }

    public static void saveSyncMobileDataUsage(Context context, boolean flag) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        preferences.edit().putBoolean(KEY_SYNC_MOBILE_DATA_USAGE, flag).apply();
    }

    //使用手机流量同步
    public static boolean getSyncMobileDataUsage(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getBoolean(KEY_SYNC_MOBILE_DATA_USAGE, false);
    }

    public static void saveSyncNoticeDate(Context context, String date) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_SYNC_NOTICE_DATE, date).apply();
    }

    //获取同步提示显示日期（一天只提示一次）
    public static String getSyncNoticeDate(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getString(KEY_SYNC_NOTICE_DATE, null);
    }

    public static void saveLastSyncSyncTime(Context context, String time) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_SYNC_LAST_TIME, time).apply();
    }

    //获取上次同步时间
    public static String getLastSyncTime(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getString(KEY_SYNC_LAST_TIME, "");
    }

    public static void saveIsLastBackupNoFinish(Context context, boolean flag) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        preferences.edit().putBoolean(KEY_IS_LAST_BACKUP_NO_FINISH, flag).apply();
    }

    //备份恢复相关，存储进行中的类型、事务id。备份恢复不能同时进行，数据不会同时存在
    //进行中的备份or恢复类型
    public static int getDoingBackupOrRestoreType(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getInt(KEY_DOING_BACKUP_OR_RESTORE_TYPE, -1);
    }

    public static void saveDoingBackupOrRestoreType(Context context, int type) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        preferences.edit().putInt(KEY_DOING_BACKUP_OR_RESTORE_TYPE, type).apply();
    }

    //进行中的备份or恢复id
    public static String getDoingBackupOrRestoreId(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getString(KEY_DOING_BACKUP_OR_RESTORE_ID, null);
    }

    public static void saveDoingBackupOrRestoreId(Context context, String transId) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_DOING_BACKUP_OR_RESTORE_ID, transId).apply();
    }

    //进行中的恢复成员域名
    public static String getDoingRestoreMemberDomain(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getString(KEY_DOING_RESTORE_USER_DOMAIN, null);
    }

    public static void saveDoingRestoreMemberDomain(Context context, String domain) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_DOING_RESTORE_USER_DOMAIN, domain).apply();
    }

    //上次备份是否未结束
    public static boolean getIsLastBackupNoFinish(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getBoolean(KEY_IS_LAST_BACKUP_NO_FINISH, false);
    }

    public static void saveLastRestoreSource(Context context, String folderName) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME, Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_LAST_RESTORE_SOURCE, folderName).apply();
    }

    //获取上次未恢复完的文件夹名称
    public static String getLastRestoreSource(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getString(KEY_LAST_RESTORE_SOURCE, "");
    }

    public static void saveLastRestoreUser(Context context, String userName) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME, Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_LAST_RESTORE_USER, userName).apply();
    }

    //获取上次为恢复完的用户显示
    public static String getLastRestoreUser(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getString(KEY_LAST_RESTORE_USER, "");
    }

    public static void saveUpgradeAutoDownload(Context context, boolean hasShowed) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(KEY_UPGRADE_AUTO_DOWNLOAD, hasShowed).apply();
    }


    //获取系统升级是否自动下载
    public static boolean getUpgradeAutoDownload(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(KEY_UPGRADE_AUTO_DOWNLOAD, false);
    }

    public static void saveUpgradeAutoInstall(Context context, boolean hasShowed) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(KEY_UPGRADE_AUTO_INSTALL, hasShowed).apply();
    }

    //获取系统升级是否自动安装
    public static boolean getUpgradeAutoInstall(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(KEY_UPGRADE_AUTO_INSTALL, false);
    }

    public static void saveCurrentBoxVersion(Context context, String currentBoxVersion) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME, Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_CURRENT_BOX_VERSION, currentBoxVersion).apply();
    }

    //获取当前盒子系统版本
    public static String getCurrentBoxVersion(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME, Context.MODE_PRIVATE);
        return preferences.getString(KEY_CURRENT_BOX_VERSION, "");
    }

    public static void savePrivacyAgreed(Context context, boolean hasAgreed) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(KEY_PRIVACY_AGREEMENT, hasAgreed).apply();
    }

    //获取是否同意过隐私因协议
    public static boolean getPrivacyAgreed(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(KEY_PRIVACY_AGREEMENT, false);
    }

    public static void saveSyncTaskDate(Context context, String date) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_SYNC_DATE, date).apply();
    }

    //获取同步任务执行日期
    public static String getSyncTaskDate(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getString(KEY_SYNC_DATE, null);
    }

    public static void saveSyncCount(Context context, int count) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        preferences.edit().putInt(KEY_SYNC_COUNT, count).apply();
    }

    //获取任务数量
    public static int getSyncCount(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getInt(KEY_SYNC_COUNT, 0);
    }

    public static void saveLoggerSwitch(Context context, boolean isOpen) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(KEY_LOGGER_SWITCH, isOpen).apply();
    }

    //获取debug开关是否开启
    public static boolean getLoggerSwitch(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(KEY_LOGGER_SWITCH, false);
    }

    private static String getUserId(Context context) {
        String userId = "";
        UserInfo userInfo = EulixSpaceDBUtil.getCompatibleActiveUserInfo(context);
        if (userInfo != null) {
            userId = userInfo.getUserId();
        }
        return userId;
    }

    //获取设备版本详情信息
    public static String getDeviceVersionDetailInfo(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getString(KEY_DEVICE_VERSION_DETAIL_INFO, null);
    }

    public static void saveDeviceVersionDetailInfo(Context context, String versionInfoStr) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_DEVICE_VERSION_DETAIL_INFO, versionInfoStr).apply();
    }

    public static void saveAppletAuthInfo(Context context, String appletId, boolean authState) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        preferences.edit().putBoolean(KEY_SYNC_DATE + "_" + appletId, authState).apply();
    }

    //获取小程序授权情况
    public static boolean getAppletAuthState(Context context, String appletId) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getBoolean(KEY_SYNC_DATE + "_" + appletId, false);
    }


    // 判断权限是否被拒绝过
    public static void savePermissionDeny(Context context, String permission, boolean deny) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(permission, deny).apply();
    }


    //判断权限是否被拒绝过
    public static boolean getPermissionDeny(Context context, String permission) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(permission, false);
    }

    //缓存web数据
    public static void saveWebData(Context context, String appletId, String key, String value) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        preferences.edit().putString(appletId + key, value).apply();
    }

    public static String getWebData(Context context, String appletId, String key) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getString(appletId + key, null);
    }

    //缓存小程序版本信息
    public static void saveAppletUpdateVersionWithId(Context context, String appletId, String version) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_APPLET_UPDATE_VERSION + appletId, version).apply();
    }

    public static String getAppletUpdateVersionById(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getString(KEY_APPLET_UPDATE_VERSION + key, null);
    }

    //缓存智能相册lastOperateId
    public static void saveAlbumLastOperateId(Context context, int lastOperateId) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        preferences.edit().putInt(KEY_ALBUM_LAST_OPERATE_ID, lastOperateId).apply();
    }

    public static int getAlbumLastOperateId(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getInt(KEY_ALBUM_LAST_OPERATE_ID, -1);
    }

    //获取局域网domain
    public static String getLanDomain(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context), Context.MODE_PRIVATE);
        return preferences.getString(KEY_LAN_DOMAIN, null);
    }

    public static void saveLanDomain(Context context, String domain) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context), Context.MODE_PRIVATE);
        preferences.edit().putString(KEY_LAN_DOMAIN, domain).apply();
    }

    public static void saveAuthenticatorSetted(Context context, boolean setted) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        preferences.edit().putBoolean(KEY_AUTHENTICATOR_SETTED, setted).apply();
    }

    //是否已设置过身份认证器
    public static boolean getAuthenticatorSetted(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME + "_" + EulixSpaceDBUtil.queryAvailableBoxUuid(context) + "_" + getUserId(context), Context.MODE_PRIVATE);
        return preferences.getBoolean(KEY_AUTHENTICATOR_SETTED, false);
    }

    //查询是否包含某个字段
    public static boolean checkBaseContains(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return preferences.contains(key);
    }

    public static void saveBaseKeyBoolean(Context context, String key, boolean value) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(key, value).apply();
    }

    public static boolean getBaseKeyBoolean(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(key, false);
    }

    public static void saveHasUpgradeDbAccountValue(Context context, boolean flag) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME, Context.MODE_PRIVATE);
        preferences.edit().putBoolean(KEY_HAS_UPGRADE_DB_ACCOUNT_VALUE, flag).apply();
    }

    //是否已设置升级过数据库account值
    public static boolean getHasUpgradeDbAccountValue(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(
                PREFERENCE_NAME, Context.MODE_PRIVATE);
        return preferences.getBoolean(KEY_HAS_UPGRADE_DB_ACCOUNT_VALUE, false);
    }
}
