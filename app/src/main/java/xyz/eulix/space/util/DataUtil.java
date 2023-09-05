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
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.crypto.spec.IvParameterSpec;

import xyz.eulix.space.R;
import xyz.eulix.space.bean.AOSpaceAccessBean;
import xyz.eulix.space.bean.ApplicationLockEventInfo;
import xyz.eulix.space.bean.ArrayStack;
import xyz.eulix.space.bean.BoxGenerationShowBean;
import xyz.eulix.space.bean.CustomizeFile;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.EulixBoxBaseInfoCompatible;
import xyz.eulix.space.bean.EulixBoxTokenDetail;
import xyz.eulix.space.bean.EulixSpaceInfo;
import xyz.eulix.space.bean.EulixSpaceInfoCompatible;
import xyz.eulix.space.bean.FileSearchData;
import xyz.eulix.space.bean.GranterAuthorizationBean;
import xyz.eulix.space.bean.GranterSecurityAuthenticationBean;
import xyz.eulix.space.bean.SwitchPlatformTaskBean;
import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.database.EulixSpaceSharePreferenceHelper;
import xyz.eulix.space.database.NetworkPasswordSharePreferenceHelper;
import xyz.eulix.space.manager.EulixBiometricManager;
import xyz.eulix.space.network.agent.platform.PlatformApi;
import xyz.eulix.space.network.files.FileListItem;
import xyz.eulix.space.network.notification.GetNotificationResult;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/9/6 14:52
 */
public class DataUtil {
    private static Map<String, String> dataMap;
    private static String screenShotPath;
    private static boolean isStartLauncher = false;
    private static Map<String, String> requestFileIdMap;
    private static Map<String, String> uuidTitleMap;
    private static Map<String, Map<String, Map<String, List<FileListItem>>>> fileListsMap;
    private static Map<String, List<GetNotificationResult>> notificationResultMap;
    private static ArrayStack<UUID> uuidStack;
    private static Map<String, Map<String, Integer>> boxTokenAlarmMap;
    private static UUID fileSearchUuid;
    private static FileSearchData fileSearchData;
    private static EulixBoxTokenDetail lastBoxToken;
    private static GranterAuthorizationBean processGranterAuthorizationBean;
    private static GranterSecurityAuthenticationBean processGranterSecurityAuthenticationBean;
    private static List<ApplicationLockEventInfo> applicationLockEventInfoList;
    private static List<String> cancelAuthenticationList;
    private static int activityIndex;
    private static String currentPlatformServerHost;
    private static Map<String, Boolean> platformAbilityRequestMap = new HashMap<>();
    private static AOSpaceAccessBean aoSpaceAccessBean;

    private static Comparator<Long> ascendLongComparator = (o1, o2) -> {
        if (o1 == null || o2 == null) {
            return 0;
        } else {
            return o1.compareTo(o2);
        }
    };

    static {
        dataMap = new HashMap<>();
        requestFileIdMap = new HashMap<>();
        uuidTitleMap = new HashMap<>();
        // 第一层：box uuid；第二层：box bind；第三层：file uuid
        fileListsMap = new HashMap<>();
        notificationResultMap = new HashMap<>();
        boxTokenAlarmMap = new HashMap<>();
    }

    private DataUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    public static boolean isStartLauncher() {
        return isStartLauncher;
    }

    public static void setStartLauncher(boolean isStartLauncher) {
        DataUtil.isStartLauncher = isStartLauncher;
    }

    public static String getData(String requestId) {
        String data = null;
        if (requestId != null && dataMap.containsKey(requestId)) {
            data = dataMap.get(requestId);
            try {
                dataMap.remove(requestId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return data;
    }

    public static String setData(String data) {
        String requestId = null;
        if (data != null) {
            do {
                requestId = UUID.randomUUID().toString();
            } while (dataMap.containsKey(requestId));
            dataMap.put(requestId, data);
        }
        return requestId;
    }

    private static String getFileId(String requestId) {
        String fileId = null;
        if (requestId != null && requestFileIdMap.containsKey(requestId)) {
            fileId = requestFileIdMap.get(requestId);
        }
        return fileId;
    }

    public static void setRequestFileId(@NonNull String requestId, String fileId) {
        requestFileIdMap.put(requestId, fileId);
    }

    public static String removeRequestId(String requestId) {
        String fileId = null;
        if (requestId != null && requestFileIdMap.containsKey(requestId)) {
            fileId = getFileId(requestId);
            try {
                fileId = requestFileIdMap.remove(requestId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return fileId;
    }

    public static Map<String, String> getUuidTitleMap() {
        return uuidTitleMap;
    }

    public static void setUuidTitleMap(String uuid, String title) {
        uuidTitleMap.put(uuid, title);
    }

    public static List<FileListItem> getFileListsMap(String boxUuid, String boxBind, String currentId) {
        List<FileListItem> fileListItems = null;
        if (boxUuid != null && fileListsMap.containsKey(boxUuid)) {
            Map<String, Map<String, List<FileListItem>>> subFileListsMap = fileListsMap.get(boxUuid);
            if (boxBind != null && subFileListsMap != null && subFileListsMap.containsKey(boxBind)) {
                Map<String, List<FileListItem>> fileListMap = subFileListsMap.get(boxBind);
                if (currentId != null && fileListMap != null && fileListMap.containsKey(currentId)) {
                    fileListItems = fileListMap.get(currentId);
                }
            }
        }
        return fileListItems;
    }

    public static void setFileListsMap(String boxUuid, String boxBind, String currentId, List<FileListItem> fileListItems) {
        if (boxUuid != null && boxBind != null && currentId != null) {
            Map<String, Map<String, List<FileListItem>>> subFileListsMap = null;
            Map<String, List<FileListItem>> fileListMap = null;
            if (fileListsMap.containsKey(boxUuid)) {
                subFileListsMap = fileListsMap.get(boxUuid);
                if (subFileListsMap != null && subFileListsMap.containsKey(boxBind)) {
                    fileListMap = subFileListsMap.get(boxBind);
                }
            }
            if (fileListMap == null) {
                fileListMap = new HashMap<>();
            }
            fileListMap.put(currentId, fileListItems);
            if (subFileListsMap == null) {
                subFileListsMap = new HashMap<>();
            }
            subFileListsMap.put(boxBind, fileListMap);
            fileListsMap.put(boxUuid, subFileListsMap);
        }
    }

    public static void deleteFileListsMap(String boxUuid, String boxBind, String currentId, List<String> fileIdList) {
        if (boxUuid != null && boxBind != null && currentId != null) {
            Map<String, Map<String, List<FileListItem>>> subFileListsMap = null;
            Map<String, List<FileListItem>> fileListMap = null;
            if (fileListsMap.containsKey(boxUuid)) {
                subFileListsMap = fileListsMap.get(boxUuid);
                if (subFileListsMap != null && subFileListsMap.containsKey(boxBind)) {
                    fileListMap = subFileListsMap.get(boxBind);
                }
            }
            if (fileListMap != null && fileListMap.containsKey(currentId)) {
                List<FileListItem> fileListItems = fileListMap.get(currentId);
                if (fileListItems != null) {
                    if (fileIdList == null) {
                        fileListItems.clear();
                    } else {
                        Iterator<FileListItem> fileListItemIterator = fileListItems.iterator();
                        while (fileListItemIterator.hasNext()) {
                            FileListItem fileListItem = fileListItemIterator.next();
                            if (fileListItem != null) {
                                String fileId = fileListItem.getUuid();
                                if (fileId != null && fileIdList.contains(fileId)) {
                                    fileListItemIterator.remove();
                                }
                            }
                        }
                    }
                    fileListMap.put(currentId, fileListItems);
                    subFileListsMap.put(boxBind, fileListMap);
                    fileListsMap.put(boxUuid, subFileListsMap);
                }
            }
        }
    }

    public static List<GetNotificationResult> getNotificationResultList(String boxUuid, String boxBind) {
        List<GetNotificationResult> getNotificationResults = null;
        if (boxUuid != null && boxBind != null) {
            String notificationKey = (boxUuid + "_" + boxBind);
            if (notificationResultMap.containsKey(notificationKey)) {
                getNotificationResults = notificationResultMap.get(notificationKey);
            }
        }
        return getNotificationResults;
    }

    public static void setNotificationResultMap(String boxUuid, String boxBind, List<GetNotificationResult> getNotificationResults) {
        if (boxUuid != null && boxBind != null) {
            String notificationKey = (boxUuid + "_" + boxBind);
            notificationResultMap.put(notificationKey, getNotificationResults);
        }
    }

    public static void deleteNotificationResultList(String boxUuid, String boxBind) {
        if (boxUuid != null && boxBind != null) {
            String notificationKey = (boxUuid + "_" + boxBind);
            if (notificationResultMap.containsKey(notificationKey)) {
                List<GetNotificationResult> notificationResults = notificationResultMap.get(notificationKey);
                if (notificationResults != null) {
                    notificationResults.clear();
                    notificationResultMap.put(notificationKey, notificationResults);
                }
            }
        }
    }

    public static ArrayStack<UUID> getUuidStack() {
        return uuidStack;
    }

    public static void setUuidStack(ArrayStack<UUID> uuids) {
        if (uuids == null) {
            if (uuidStack != null) {
                uuidStack.clear();
                uuidStack = null;
            }
        } else {
            if (uuidStack == null) {
                uuidStack = new ArrayStack<>();
            } else {
                uuidStack.clear();
            }
            for (UUID uuid : uuids) {
                if (uuid != null) {
                    uuidStack.push(uuid);
                }
            }
        }
    }

    public static ArrayStack<UUID> cloneUUIDStack(ArrayStack<UUID> uuidStack) {
        ArrayStack<UUID> uuids = null;
        if (uuidStack != null) {
            uuids = new ArrayStack<>();
            for (UUID uuid : uuidStack) {
                uuids.push(uuid);
            }
        }
        return uuids;
    }

    public static ArrayList<CustomizeFile> cloneCustomizeFileList(List<CustomizeFile> customizeFiles) {
        ArrayList<CustomizeFile> customizeFileArrayList = null;
        if (customizeFiles != null) {
            customizeFileArrayList = new ArrayList<>(customizeFiles);
        }
        return customizeFileArrayList;
    }

    /**
     * 取消闹钟所用
     *
     * @param boxUuid
     * @return
     */
    public static Integer getTokenAlarmId(String boxUuid, String boxBind) {
        Integer alarmId = null;
        if (boxUuid != null && boxTokenAlarmMap.containsKey(boxUuid)) {
            Map<String, Integer> tokenMap = boxTokenAlarmMap.get(boxUuid);
            if (tokenMap != null && boxBind != null && tokenMap.containsKey(boxBind)) {
                alarmId = tokenMap.get(boxBind);
            }
        }
        return alarmId;
    }

    public static List<Integer> getTokenAlarmIds(String boxUuid) {
        List<Integer> alarmIds = new ArrayList<>();
        if (boxUuid != null && boxTokenAlarmMap.containsKey(boxUuid)) {
            Map<String, Integer> tokenMap = boxTokenAlarmMap.get(boxUuid);
            if (tokenMap != null) {
                Set<Map.Entry<String, Integer>> subEntrySet = tokenMap.entrySet();
                for (Map.Entry<String, Integer> subEntry : subEntrySet) {
                    if (subEntry != null) {
                        Integer alarmId = subEntry.getValue();
                        if (alarmId != null) {
                            alarmIds.add(alarmId);
                        }
                    }
                }
            }
        }
        return alarmIds;
    }

    public static List<Integer> getTokenAlarmIds() {
        List<Integer> alarmIds = new ArrayList<>();
        Set<Map.Entry<String, Map<String, Integer>>> entrySet = boxTokenAlarmMap.entrySet();
        for (Map.Entry<String, Map<String, Integer>> entry : entrySet) {
            if (entry != null) {
                Map<String, Integer> subMap = entry.getValue();
                if (subMap != null) {
                    Set<Map.Entry<String, Integer>> subEntrySet = subMap.entrySet();
                    for (Map.Entry<String, Integer> subEntry : subEntrySet) {
                        Integer alarmId = subEntry.getValue();
                        if (alarmId != null) {
                            alarmIds.add(alarmId);
                        }
                    }
                }
            }
        }
        return alarmIds;
    }

    /**
     * 用于生成闹钟的记录
     *
     * @param boxUuid
     * @param alarmId
     */
    public static void setTokenAlarmId(String boxUuid, String boxBind, int alarmId) {
        if (boxUuid != null && boxBind != null) {
            Map<String, Integer> subMap = null;
            if (boxTokenAlarmMap.containsKey(boxUuid)) {
                subMap = boxTokenAlarmMap.get(boxUuid);
            }
            if (subMap == null) {
                subMap = new HashMap<>();
            }
            subMap.put(boxBind, alarmId);
            boxTokenAlarmMap.put(boxUuid, subMap);
        }
    }

    /**
     * @param context
     * @return 设备UUID
     */
    public static String getClientUuid(@NonNull Context context) {
        String clientUuid = null;
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null && eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.UUID)) {
            clientUuid = eulixSpaceSharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.UUID);
        }
        return clientUuid;
    }

    public static String getClientPublicKey(@NonNull Context context) {
        String clientPublicKey = null;
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null && eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PUBLIC_KEY)) {
            clientPublicKey = eulixSpaceSharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PUBLIC_KEY);
        }
        return clientPublicKey;
    }

    public static String getClientPrivateKey(@NonNull Context context) {
        String clientPublicKey = null;
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null && eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PRIVATE_KEY)) {
            clientPublicKey = eulixSpaceSharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PRIVATE_KEY);
        }
        return clientPublicKey;
    }

    /**
     * @param context
     * @return 如果是管理员或者成员，是设备UUID，否则是授权方的设备UUID
     */
    public static String getCompatibleClientUuid(@NonNull Context context) {
        String clientUuid = EulixSpaceDBUtil.getCompatibleActiveClientUuid(context);
        if (clientUuid == null) {
            clientUuid = getClientUuid(context);
        }
        return clientUuid;
    }

    public static String getCompatibleClientUuid(@NonNull Context context, String boxUuid, String boxBind) {
        String clientUuid = null;
        if (boxUuid != null && boxBind != null) {
            clientUuid = EulixSpaceDBUtil.getCompatibleActiveClientUuid(context, boxUuid, boxBind);
        } else {
            clientUuid = EulixSpaceDBUtil.getCompatibleActiveClientUuid(context);
        }
        if (clientUuid == null) {
            clientUuid = getClientUuid(context);
        }
        return clientUuid;
    }

    public static EulixSpaceInfo getLastEulixSpace(@NonNull Context context) {
        EulixSpaceInfo eulixSpaceInfo = null;
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null && eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.LAST_EULIX_SPACE)) {
            String value = eulixSpaceSharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.LAST_EULIX_SPACE);
            if (value != null && !TextUtils.isEmpty(value)) {
                try {
                    eulixSpaceInfo = new Gson().fromJson(value, EulixSpaceInfo.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        return eulixSpaceInfo;
    }

    public static void setLastEulixSpace(@NonNull Context context, String boxUuid, String boxBind) {
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null) {
            if (boxUuid != null && boxBind != null) {
                EulixSpaceInfo eulixSpaceInfo = new EulixSpaceInfo();
                eulixSpaceInfo.setBoxUuid(boxUuid);
                eulixSpaceInfo.setBoxBind(boxBind);
                eulixSpaceSharePreferenceHelper.setString(ConstantField.EulixSpaceSPKey.LAST_EULIX_SPACE
                        , new Gson().toJson(eulixSpaceInfo, EulixSpaceInfo.class), false);
            } else if (boxUuid == null && boxBind == null) {
                eulixSpaceSharePreferenceHelper.remove(ConstantField.EulixSpaceSPKey.LAST_EULIX_SPACE, false);
            }
        }
    }

    public static EulixSpaceInfo getActiveOrLastEulixSpace(@NonNull Context context) {
        EulixSpaceInfo eulixSpaceInfo = null;
        EulixBoxBaseInfo eulixBoxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(context);
        if (eulixBoxBaseInfo == null) {
            eulixSpaceInfo = DataUtil.getLastEulixSpace(context);
        } else {
            eulixSpaceInfo = new EulixSpaceInfo();
            eulixSpaceInfo.setBoxUuid(eulixBoxBaseInfo.getBoxUuid());
            eulixSpaceInfo.setBoxBind(eulixBoxBaseInfo.getBoxBind());
        }
        return eulixSpaceInfo;
    }

    public static EulixSpaceInfoCompatible getActiveOrLastEulixSpaceCompatible(@NonNull Context context) {
        EulixSpaceInfoCompatible eulixSpaceInfoCompatible = null;
        String boxUuid = null;
        String boxBind = null;
        int spaceState = 0;
        EulixBoxBaseInfoCompatible eulixBoxBaseInfoCompatible = EulixSpaceDBUtil.getActiveBoxBaseInfoCompatible(context);
        if (eulixBoxBaseInfoCompatible == null) {
            EulixSpaceInfo eulixSpaceInfo = DataUtil.getLastEulixSpace(context);
            if (eulixSpaceInfo != null) {
                eulixSpaceInfoCompatible = new EulixSpaceInfoCompatible();
                boxUuid = eulixSpaceInfo.getBoxUuid();
                boxBind = eulixSpaceInfo.getBoxBind();
            }
        } else {
            eulixSpaceInfoCompatible = new EulixSpaceInfoCompatible();
            boxUuid = eulixBoxBaseInfoCompatible.getBoxUuid();
            boxBind = eulixBoxBaseInfoCompatible.getBoxBind();
            spaceState = eulixBoxBaseInfoCompatible.getSpaceState();
        }
        if (eulixSpaceInfoCompatible != null) {
            eulixSpaceInfoCompatible.setBoxUuid(boxUuid);
            eulixSpaceInfoCompatible.setBoxBind(boxBind);
            eulixSpaceInfoCompatible.setSpaceState(spaceState);
        }
        return eulixSpaceInfoCompatible;
    }

    public static String getApplicationLocale(Context context) {
        String localeValue = null;
        if (context != null) {
            EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
            if (eulixSpaceSharePreferenceHelper != null && eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.APPLICATION_LOCALE)) {
                localeValue = eulixSpaceSharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.APPLICATION_LOCALE);
            }
        }
        return localeValue;
    }

    public static boolean setApplicationLocale(Context context, String localeValue, boolean isImmediate) {
        boolean result = false;
        if (context != null) {
            EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
            if (eulixSpaceSharePreferenceHelper != null) {
                if (localeValue != null) {
                    result = eulixSpaceSharePreferenceHelper.setString(ConstantField.EulixSpaceSPKey.APPLICATION_LOCALE, localeValue, isImmediate);
                } else if (eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.APPLICATION_LOCALE)) {
                    result = eulixSpaceSharePreferenceHelper.remove(ConstantField.EulixSpaceSPKey.APPLICATION_LOCALE, isImmediate);
                } else {
                    result = true;
                }
            }
        }
        return result;
    }

    /**
     * 生成盒子展示名和图
     *
     * @param context
     * @param deviceModelNumber 设备型号
     * @return
     */
    @NonNull
    public static BoxGenerationShowBean generationBoxGenerationShowBean(Context context, int deviceModelNumber, @NonNull BoxGenerationShowBean defaultBean) {
        if (context != null) {
            StringBuilder boxNameBuilder = new StringBuilder();
            boxNameBuilder.append(context.getString(R.string.device_server_name));
            switch ((deviceModelNumber / 100)) {
                case 1:
                    boxNameBuilder.append(context.getString(R.string.generation_part_1));
                    boxNameBuilder.append(context.getString(R.string.chinese_ordinal_one_abbr));
                    boxNameBuilder.append(context.getString(R.string.generation_part_2));
                    defaultBean.setBoxResId(R.drawable.eulix_device_g1_manage_2x);
                    break;
                case 2:
                    boxNameBuilder.append(context.getString(R.string.generation_part_1));
                    boxNameBuilder.append(context.getString(R.string.chinese_ordinal_two_abbr));
                    boxNameBuilder.append(context.getString(R.string.generation_part_2));
                    defaultBean.setBoxResId(R.drawable.eulix_device_g2_manage_2x);
                    break;
                case -1:
                case -3:
                    boxNameBuilder.append(context.getString(R.string.left_bracket_left_enter));
                    boxNameBuilder.append(context.getString(R.string.pc_version));
                    boxNameBuilder.append(context.getString(R.string.right_bracket));
                    defaultBean.setBoxResId(R.drawable.eulix_device_computer_2x);
                    break;
                case -2:
                    boxNameBuilder.append(context.getString(R.string.left_bracket_left_enter));
                    boxNameBuilder.append(context.getString(R.string.online_version));
                    boxNameBuilder.append(context.getString(R.string.right_bracket));
                    defaultBean.setBoxResId(R.drawable.eulix_device_cloud_2x);
                    break;
                default:
                    break;
            }
            defaultBean.setBoxName(boxNameBuilder.toString());
        }
        return defaultBean;
    }

    public static UserInfo getSpecificUserInfo(Map<String, String> boxValue, String specificId, boolean isGranter) {
        UserInfo info = null;
        Map<String, UserInfo> userInfoMap = null;
        String userInfoValue = null;
        if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_USER_INFO)) {
            userInfoValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_USER_INFO);
        }
        if (userInfoValue != null) {
            try {
                userInfoMap = new Gson().fromJson(userInfoValue, new TypeToken<Map<String, UserInfo>>() {
                }.getType());
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
        if (userInfoMap != null && specificId != null) {
            Set<Map.Entry<String, UserInfo>> entrySet = userInfoMap.entrySet();
            for (Map.Entry<String, UserInfo> entry : entrySet) {
                if (entry != null) {
                    String uuid = entry.getKey();
                    UserInfo userInfo = entry.getValue();
                    if (isGranter) {
                        if (specificId.equals(uuid)) {
                            info = userInfo;
                            break;
                        }
                    } else if (userInfo != null && specificId.equals(userInfo.getUserId())) {
                        info = userInfo;
                        break;
                    }
                }
            }
        }
        return info;
    }

    public static String getFileSortOrder(Context context) {
        String fileSortOrder = null;
        if (context != null) {
            EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
            if (eulixSpaceSharePreferenceHelper != null && eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.FILE_SORT_ORDER)) {
                fileSortOrder = eulixSpaceSharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.FILE_SORT_ORDER);
            }
            if (fileSortOrder == null) {
                fileSortOrder = ConstantField.Sort.OPERATION_TIME_DESCEND;
                setFileSortOrder(context, fileSortOrder);
            }
        }
        return fileSortOrder;
    }

    public static int stringCodeToInt(String inValue) {
        int outValue = -1;
        if (inValue != null) {
            int spiltIndex = inValue.lastIndexOf("-");
            String parseValue = inValue;
            if (spiltIndex >= 0 && (spiltIndex + 1) < inValue.length()) {
                parseValue = inValue.substring((spiltIndex + 1));
            }
            try {
                outValue = Integer.parseInt(parseValue);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return outValue;
    }

    public static String stringCodeGetSource(String inValue) {
        String source = null;
        if (inValue != null) {
            int spiltIndex = inValue.lastIndexOf("-");
            source = inValue;
            if (spiltIndex > 0 && spiltIndex < inValue.length()) {
                source = inValue.substring(0, spiltIndex);
            } else if (spiltIndex == 0) {
                source = "";
            }
        }
        return source;
    }

    public static boolean isSpaceStatusOnline(int spaceStatus, boolean defaultStatus) {
        boolean status = defaultStatus;
        switch (spaceStatus) {
            case ConstantField.EulixDeviceStatus.OFFLINE:
            case ConstantField.EulixDeviceStatus.OFFLINE_USE:
            case ConstantField.EulixDeviceStatus.OFFLINE_UNINITIALIZED:
                status = false;
                break;
            case ConstantField.EulixDeviceStatus.REQUEST_LOGIN:
            case ConstantField.EulixDeviceStatus.REQUEST_USE:
            case ConstantField.EulixDeviceStatus.ACTIVE:
            case ConstantField.EulixDeviceStatus.ONLINE_UNINITIALIZED:
                status = true;
                break;
            default:
                break;
        }
        return status;
    }

    public static void setFileSortOrder(@NonNull Context context, String fileSortOrder) {
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null) {
            if (fileSortOrder == null) {
                eulixSpaceSharePreferenceHelper.remove(ConstantField.EulixSpaceSPKey.FILE_SORT_ORDER, false);
            } else {
                eulixSpaceSharePreferenceHelper.setString(ConstantField.EulixSpaceSPKey.FILE_SORT_ORDER, fileSortOrder, false);
            }
        }
    }

    public static String getApkDownloadPath(@NonNull Context context) {
        String apkDownloadPath = null;
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null && eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.APK_DOWNLOAD_PATH)) {
            apkDownloadPath = eulixSpaceSharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.APK_DOWNLOAD_PATH);
        }
        return apkDownloadPath;
    }

    public static boolean setApkDownloadPath(@NonNull Context context, String apkDownloadPath, boolean isForce) {
        boolean result = true;
        if (apkDownloadPath != null) {
            EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
            if (eulixSpaceSharePreferenceHelper != null) {
                result = eulixSpaceSharePreferenceHelper.setString(ConstantField.EulixSpaceSPKey.APK_DOWNLOAD_PATH, apkDownloadPath, isForce);
            }
        }
        return result;
    }

    public static String getNotificationReminderVersion(Context context) {
        String version = null;
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null && eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.NOTIFICATION_REMINDER_VERSION)) {
            version = eulixSpaceSharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.NOTIFICATION_REMINDER_VERSION);
        }
        return version;
    }

    public static boolean setNotificationReminderVersion(Context context, String version) {
        boolean result = false;
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null) {
            result = (version == null || eulixSpaceSharePreferenceHelper.setString(ConstantField.EulixSpaceSPKey.NOTIFICATION_REMINDER_VERSION, version, false));
        }
        return result;
    }

    public static boolean getSystemMessageEnable(Context context) {
        boolean isSystemMessageEnable = true;
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null && eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.SYSTEM_MESSAGE_ENABLE)) {
            isSystemMessageEnable = eulixSpaceSharePreferenceHelper.getBoolean(ConstantField.EulixSpaceSPKey.SYSTEM_MESSAGE_ENABLE, true);
        } else {
            setSystemMessageEnable(context, true, false);
        }
        return isSystemMessageEnable;
    }

    public static boolean setSystemMessageEnable(Context context, boolean isSystemMessageEnable, boolean isForce) {
        boolean result = false;
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null) {
            result = eulixSpaceSharePreferenceHelper.setBoolean(ConstantField.EulixSpaceSPKey.SYSTEM_MESSAGE_ENABLE, isSystemMessageEnable, isForce);
        }
        return result;
    }

    public static boolean getBusinessMessageEnable(Context context) {
        boolean isBusinessMessageEnable = true;
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null && eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.BUSINESS_MESSAGE_ENABLE)) {
            isBusinessMessageEnable = eulixSpaceSharePreferenceHelper.getBoolean(ConstantField.EulixSpaceSPKey.BUSINESS_MESSAGE_ENABLE, true);
        } else {
            setBusinessMessageEnable(context, true, false);
        }
        return isBusinessMessageEnable;
    }

    public static boolean setBusinessMessageEnable(Context context, boolean isBusinessMessageEnable, boolean isForce) {
        boolean result = false;
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null) {
            result = eulixSpaceSharePreferenceHelper.setBoolean(ConstantField.EulixSpaceSPKey.BUSINESS_MESSAGE_ENABLE, isBusinessMessageEnable, isForce);
        }
        return result;
    }


    public static String getAppUpdateVersion(Context context) {
        String appUpdateVersion = null;
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null && eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.APP_UPDATE_VERSION)) {
            appUpdateVersion = eulixSpaceSharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.APP_UPDATE_VERSION);
        }
        return appUpdateVersion;
    }

    public static boolean setAppUpdateVersion(Context context, String appUpdateVersion) {
        boolean result = false;
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null) {
            if (appUpdateVersion != null) {
                result = eulixSpaceSharePreferenceHelper.setString(ConstantField.EulixSpaceSPKey.APP_UPDATE_VERSION, appUpdateVersion, false);
            } else if (eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.APP_UPDATE_VERSION)) {
                result = eulixSpaceSharePreferenceHelper.remove(ConstantField.EulixSpaceSPKey.APP_UPDATE_VERSION, false);
            }
        }
        return result;
    }

    public static String getSecurityEmailConfigurations(Context context) {
        String response = null;
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null && eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.SECURITY_EMAIL_CONFIGURATIONS)) {
            response = eulixSpaceSharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.SECURITY_EMAIL_CONFIGURATIONS);
        }
        return response;
    }

    public static boolean setSecurityEmailConfigurations(Context context, String response) {
        boolean result = false;
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null) {
            if (response != null) {
                result = eulixSpaceSharePreferenceHelper.setString(ConstantField.EulixSpaceSPKey.SECURITY_EMAIL_CONFIGURATIONS, response, false);
            } else if (eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.SECURITY_EMAIL_CONFIGURATIONS)) {
                result = eulixSpaceSharePreferenceHelper.remove(ConstantField.EulixSpaceSPKey.SECURITY_EMAIL_CONFIGURATIONS, false);
            }
        }
        return result;
    }

    public static List<String> getSecurityEmailReminderBoxList(Context context) {
        List<String> boxList = null;
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null && eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.SECURITY_EMAIL_REMINDER_BOX_LIST)) {
            String boxListValue = eulixSpaceSharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.SECURITY_EMAIL_REMINDER_BOX_LIST);
            if (boxListValue != null && !TextUtils.isEmpty(boxListValue)) {
                try {
                    boxList = new Gson().fromJson(boxListValue, new TypeToken<List<String>>() {
                    }.getType());
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        return boxList;
    }

    public static void setSecurityEmailReminderBoxList(Context context, List<String> boxList) {
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null) {
            if (boxList != null) {
                eulixSpaceSharePreferenceHelper.setString(ConstantField.EulixSpaceSPKey.SECURITY_EMAIL_REMINDER_BOX_LIST, new Gson().toJson(boxList, new TypeToken<List<String>>() {
                }.getType()), false);
            } else {
                eulixSpaceSharePreferenceHelper.remove(ConstantField.EulixSpaceSPKey.SECURITY_EMAIL_REMINDER_BOX_LIST, false);
            }
        }
    }

    public static long getBackgroundRunningTimestamp(Context context) {
        long backgroundRunningTimestamp = -1;
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null && eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.BACKGROUND_RUNNING_TIMESTAMP)) {
            backgroundRunningTimestamp = eulixSpaceSharePreferenceHelper.getLong(ConstantField.EulixSpaceSPKey.BACKGROUND_RUNNING_TIMESTAMP);
        }
        return backgroundRunningTimestamp;
    }

    public static void setBackgroundRunningTimestamp(Context context, Long backgroundRunningTimestamp) {
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null) {
            if (backgroundRunningTimestamp != null) {
                eulixSpaceSharePreferenceHelper.setLong(ConstantField.EulixSpaceSPKey.BACKGROUND_RUNNING_TIMESTAMP, backgroundRunningTimestamp, false);
            } else {
                eulixSpaceSharePreferenceHelper.remove(ConstantField.EulixSpaceSPKey.BACKGROUND_RUNNING_TIMESTAMP, true);
            }
        }
    }

    private static Map<String, List<SwitchPlatformTaskBean>> getSwitchPlatformTaskMap(Context context) {
        Map<String, List<SwitchPlatformTaskBean>> switchPlatformTaskMap = null;
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null && eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.SWITCH_PLATFORM_TASK)) {
            String switchPlatformTaskValue = eulixSpaceSharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.SWITCH_PLATFORM_TASK);
            if (switchPlatformTaskValue != null) {
                try {
                    switchPlatformTaskMap = new Gson().fromJson(switchPlatformTaskValue, new TypeToken<Map<String, List<SwitchPlatformTaskBean>>>() {
                    }.getType());
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        return switchPlatformTaskMap;
    }

    public static List<SwitchPlatformTaskBean> getSwitchPlatformTaskList(Context context, String boxUuid, String boxBind) {
        List<SwitchPlatformTaskBean> switchPlatformTaskList = null;
        if (boxUuid != null && boxBind != null) {
            String key = (boxUuid + "_" + boxBind);
            Map<String, List<SwitchPlatformTaskBean>> switchPlatformTaskMap = getSwitchPlatformTaskMap(context);
            if (switchPlatformTaskMap != null && switchPlatformTaskMap.containsKey(key)) {
                switchPlatformTaskList = switchPlatformTaskMap.get(key);
            }
        }
        return switchPlatformTaskList;
    }

    private static void setSwitchPlatformTaskMap(Context context, Map<String, List<SwitchPlatformTaskBean>> switchPlatformTaskMap, boolean isImmediate) {
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null) {
            if (switchPlatformTaskMap != null && !switchPlatformTaskMap.isEmpty()) {
                eulixSpaceSharePreferenceHelper.setString(ConstantField.EulixSpaceSPKey.SWITCH_PLATFORM_TASK
                        , new Gson().toJson(switchPlatformTaskMap, new TypeToken<Map<String, List<SwitchPlatformTaskBean>>>() {
                        }.getType()), isImmediate);
            } else if (eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.SWITCH_PLATFORM_TASK)) {
                eulixSpaceSharePreferenceHelper.remove(ConstantField.EulixSpaceSPKey.SWITCH_PLATFORM_TASK, isImmediate);
            }
        }
    }

    public static void setSwitchPlatformTaskList(Context context, String boxUuid, String boxBind, List<SwitchPlatformTaskBean> switchPlatformTaskList, boolean isImmediate) {
        if (boxUuid != null && boxBind != null) {
            String key = (boxUuid + "_" + boxBind);
            Map<String, List<SwitchPlatformTaskBean>> switchPlatformTaskMap = getSwitchPlatformTaskMap(context);
            boolean isMapContains = (switchPlatformTaskMap != null && switchPlatformTaskMap.containsKey(key));
            boolean isListNonEmpty = (switchPlatformTaskList != null && !switchPlatformTaskList.isEmpty());
            if (isMapContains) {
                if (isListNonEmpty) {
                    switchPlatformTaskMap.put(key, switchPlatformTaskList);
                } else {
                    switchPlatformTaskMap.remove(key);
                }
            } else if (isListNonEmpty) {
                if (switchPlatformTaskMap == null) {
                    switchPlatformTaskMap = new HashMap<>();
                }
                switchPlatformTaskMap.put(key, switchPlatformTaskList);
            }
            setSwitchPlatformTaskMap(context, switchPlatformTaskMap, isImmediate);
        }
    }

    public static String getCurrentPlatformServerHost() {
        return currentPlatformServerHost;
    }

    public static void setCurrentPlatformServerHost(String currentPlatformServerHost) {
        DataUtil.currentPlatformServerHost = currentPlatformServerHost;
    }

    public static boolean isPlatformAbilityRequest(String platformServerUrl, boolean defaultValue) {
        boolean isRequest = defaultValue;
        Boolean isRequestValue = isPlatformAbilityRequest(platformServerUrl);
        if (isRequestValue != null) {
            isRequest = isRequestValue;
        }
        return isRequest;
    }

    public static Boolean isPlatformAbilityRequest(String platformServerUrl) {
        Boolean isRequestValue = null;
        String platformServerHost = StringUtil.urlToHost(platformServerUrl);
        if (platformServerHost != null && platformAbilityRequestMap != null && platformAbilityRequestMap.containsKey(platformServerHost)) {
            isRequestValue = platformAbilityRequestMap.get(platformServerHost);
        }
        return isRequestValue;
    }

    public static void setPlatformAbilityRequest(String platformServerUrl, boolean isRequest) {
        String platformServerHost = StringUtil.urlToHost(platformServerUrl);
        if (platformServerHost != null && platformAbilityRequestMap != null) {
            platformAbilityRequestMap.put(platformServerHost, isRequest);
        }
    }

    private static Map<String, List<PlatformApi>> getPlatformAbilityMap(Context context) {
        Map<String, List<PlatformApi>> platformAbilityMap = null;
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null && eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.PLATFORM_ABILITY_MAP)) {
            String platformAbilityMapValue = eulixSpaceSharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.PLATFORM_ABILITY_MAP);
            if (platformAbilityMapValue != null) {
                try {
                    platformAbilityMap = new Gson().fromJson(platformAbilityMapValue, new TypeToken<Map<String, List<PlatformApi>>>() {
                    }.getType());
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        return platformAbilityMap;
    }

    public static List<PlatformApi> getPlatformAbility(Context context, String platformServerUrl) {
        List<PlatformApi> platformApis = null;
        String platformServerHost = StringUtil.urlToHost(platformServerUrl);
        if (platformServerHost != null) {
            Map<String, List<PlatformApi>> platformAbilityMap = getPlatformAbilityMap(context);
            if (platformAbilityMap != null && platformAbilityMap.containsKey(platformServerHost)) {
                platformApis = platformAbilityMap.get(platformServerHost);
            }
        }
        return platformApis;
    }

    public static boolean isPlatformAbilitySupport(Context context, String platformServerUrl, String api, String method, boolean defaultValue) {
        boolean isSupport = defaultValue;
        Boolean isSupportValue = isPlatformAbilitySupport(context, platformServerUrl, api, method);
        if (isSupportValue != null) {
            isSupport = isSupportValue;
        }
        return isSupport;
    }

    public static Boolean isPlatformAbilitySupport(Context context, String platformServerUrl, String api, String method) {
        Boolean isSupportValue = null;
        if (platformServerUrl != null) {
            List<PlatformApi> platformAbility = getPlatformAbility(context, platformServerUrl);
            if (platformAbility != null) {
                isSupportValue = false;
                if (api != null && method != null) {
                    api = StringUtil.insertApiSeparator(api);
                    for (PlatformApi platformApi : platformAbility) {
                        if (platformApi != null && api.equals(StringUtil.insertApiSeparator(platformApi.getUri()))
                                && method.equalsIgnoreCase(platformApi.getMethod())) {
                            isSupportValue = true;
                            break;
                        }
                    }
                }
            }
        }
        return isSupportValue;
    }

    private static void setPlatformAbilityMap(Context context, Map<String, List<PlatformApi>> platformAbilityMap, boolean isImmediate) {
        EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        if (eulixSpaceSharePreferenceHelper != null) {
            if (platformAbilityMap != null && !platformAbilityMap.isEmpty()) {
                eulixSpaceSharePreferenceHelper.setString(ConstantField.EulixSpaceSPKey.PLATFORM_ABILITY_MAP
                        , new Gson().toJson(platformAbilityMap, new TypeToken<Map<String, List<PlatformApi>>>() {
                        }.getType()), isImmediate);
            } else if (eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.PLATFORM_ABILITY_MAP)) {
                eulixSpaceSharePreferenceHelper.remove(ConstantField.EulixSpaceSPKey.PLATFORM_ABILITY_MAP, isImmediate);
            }
        }
    }

    public static void setPlatformAbility(Context context, String platformServerUrl, List<PlatformApi> platformApis, boolean isImmediate) {
        String platformServerHost = StringUtil.urlToHost(platformServerUrl);
        if (platformServerHost != null) {
            Map<String, List<PlatformApi>> platformAbilityMap = getPlatformAbilityMap(context);
            boolean isMapContains = (platformAbilityMap != null && platformAbilityMap.containsKey(platformServerHost));
            boolean isListNonEmpty = (platformApis != null && !platformApis.isEmpty());
            if (isMapContains) {
                if (isListNonEmpty) {
                    platformAbilityMap.put(platformServerHost, platformApis);
                } else {
                    platformAbilityMap.remove(platformServerHost);
                }
            } else if (isListNonEmpty) {
                if (platformAbilityMap == null) {
                    platformAbilityMap = new HashMap<>();
                }
                platformAbilityMap.put(platformServerHost, platformApis);
            }
            setPlatformAbilityMap(context, platformAbilityMap, isImmediate);
        }
    }

    public static AOSpaceAccessBean getAoSpaceAccessBean() {
        return aoSpaceAccessBean;
    }

    public static void setAoSpaceAccessBean(AOSpaceAccessBean aoSpaceAccessBean) {
        DataUtil.aoSpaceAccessBean = aoSpaceAccessBean;
    }

    public static void resetAoSpaceAccessBean() {
        aoSpaceAccessBean = null;
    }

    public static String getNetworkPassword(@NonNull Context context, String bssid) {
        String password = null;
        if (bssid != null) {
            NetworkPasswordSharePreferenceHelper networkPasswordSharePreferenceHelper = NetworkPasswordSharePreferenceHelper.getInstance(context);
            if (networkPasswordSharePreferenceHelper != null) {
                password = "";
                if (networkPasswordSharePreferenceHelper.containsKey(bssid)) {
                    password = networkPasswordSharePreferenceHelper.getString(bssid);
                }
            }
        }
        return password;
    }

    public static boolean setNetworkPassword(@NonNull Context context, String bssid, String password) {
        boolean result = false;
        if (bssid != null) {
            NetworkPasswordSharePreferenceHelper networkPasswordSharePreferenceHelper = NetworkPasswordSharePreferenceHelper.getInstance(context);
            if (networkPasswordSharePreferenceHelper != null) {
                if (password != null) {
                    result = networkPasswordSharePreferenceHelper.setString(bssid, password, false);
                } else if (networkPasswordSharePreferenceHelper.containsKey(bssid)) {
                    result = networkPasswordSharePreferenceHelper.removeString(bssid, false);
                } else {
                    result = true;
                }
            }
        }
        return result;
    }

    /**
     * 用来生成16位临时密钥
     *
     * @param uuid
     * @return
     */
    public static String getUID(UUID uuid) {
        String uid = null;
        if (uuid != null) {
            int machineId = 1;//最大支持1-9个集群机器部署
            int hashCodeV = Math.abs(uuid.toString().hashCode());
            if (hashCodeV < 0) {//有可能是负数
                hashCodeV = -hashCodeV;
            }
            // 0 代表前面补充0
            // 4 代表长度为4
            // d 代表参数为正数型
            uid = (machineId + String.format("%015d", hashCodeV));
        }
        return uid;
    }

    public static String generateRandomNumber(int length) {
        StringBuilder idBuilder = new StringBuilder();
        while (length > 0) {
            idBuilder.append(Math.max(Math.min((int) Math.round(Math.floor(10 * Math.random())), 9), 0));
            length -= 1;
        }
        return idBuilder.toString();
    }

    public static String generateIVParam() {
        byte[] iv = StringUtil.getRandom(16);
        IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
        return StringUtil.byteArrayToString(ivParameterSpec.getIV());
    }

    public static byte intToByte(int value) {
        value = value % 256;
        while (value < -128) {
            value += 256;
        }
        while (value > 127) {
            value -= 256;
        }
        return (byte) value;
    }

    public static int byteToInt(byte value) {
        int result = value;
        while (result < 0) {
            result += 256;
        }
        return result;
    }

    public static String getScreenShotPath() {
        return screenShotPath;
    }

    public static void setScreenShotPath(String screenShotPath) {
        DataUtil.screenShotPath = screenShotPath;
    }

    public static UUID getFileSearchUuid() {
        return fileSearchUuid;
    }

    public static void setFileSearchUuid(UUID fileSearchUuid) {
        DataUtil.fileSearchUuid = fileSearchUuid;
    }

    public static FileSearchData getFileSearchData() {
        return fileSearchData;
    }

    public static void setFileSearchData(FileSearchData fileSearchData) {
        DataUtil.fileSearchData = fileSearchData;
    }

    public static EulixBoxTokenDetail getLastBoxToken() {
        return lastBoxToken;
    }

    public static void setLastBoxToken(EulixBoxTokenDetail lastBoxToken) {
        DataUtil.lastBoxToken = lastBoxToken;
    }

    public static GranterAuthorizationBean getProcessGranterAuthorizationBean() {
        return processGranterAuthorizationBean;
    }

    public static void setProcessGranterAuthorizationBean(GranterAuthorizationBean processGranterAuthorizationBean) {
        DataUtil.processGranterAuthorizationBean = processGranterAuthorizationBean;
    }

    public static GranterSecurityAuthenticationBean getProcessGranterSecurityAuthenticationBean() {
        return processGranterSecurityAuthenticationBean;
    }

    public static void setProcessGranterSecurityAuthenticationBean(GranterSecurityAuthenticationBean processGranterSecurityAuthenticationBean) {
        DataUtil.processGranterSecurityAuthenticationBean = processGranterSecurityAuthenticationBean;
    }

    /**
     * 获取当前正在运行中的应用锁验证请求id
     *
     * @return
     */
    public static List<String> getApplicationLockRunningRequestId() {
        List<String> requestIds = null;
        if (applicationLockEventInfoList != null && !applicationLockEventInfoList.isEmpty()) {
            for (ApplicationLockEventInfo applicationLockEventInfo : applicationLockEventInfoList) {
                if (applicationLockEventInfo != null) {
                    Boolean isError = applicationLockEventInfo.isError();
                    if (isError != null && !isError) {
                        String requestId = applicationLockEventInfo.getRequestId();
                        if (requestIds == null) {
                            requestIds = new ArrayList<>();
                        }
                        if (requestId != null) {
                            requestIds.add(requestId);
                        }
                    }
                }
            }
        }
        return requestIds;
    }

    /**
     * 获取指定空间的应用锁事件
     *
     * @param boxUuid
     * @param boxBind
     * @return
     */
    public synchronized static ApplicationLockEventInfo getApplicationLockEventInfo(String boxUuid, String boxBind) {
        ApplicationLockEventInfo applicationLockEventInfo = null;
        if (applicationLockEventInfoList != null && boxUuid != null && boxBind != null) {
            for (ApplicationLockEventInfo info : applicationLockEventInfoList) {
                if (info != null && boxUuid.equals(info.getBoxUuid()) && boxBind.equals(info.getBoxBind())) {
                    applicationLockEventInfo = info;
                    break;
                }
            }
        }
        return applicationLockEventInfo;
    }

    /**
     * 设置/更新应用锁事件
     *
     * @param applicationLockEventInfo
     * @return
     */
    public synchronized static List<String> setApplicationLockEventInfo(ApplicationLockEventInfo applicationLockEventInfo, boolean isClick) {
        List<String> cancelRequestIds = null;
        if (applicationLockEventInfo != null) {
            String boxUuid = applicationLockEventInfo.getBoxUuid();
            String boxBind = applicationLockEventInfo.getBoxBind();
            String requestId = applicationLockEventInfo.getRequestId();
            if (boxUuid != null && boxBind != null && requestId != null) {
                boolean isAdd = true;
                boolean isContains = false;
                if (applicationLockEventInfoList != null) {
                    for (ApplicationLockEventInfo info : applicationLockEventInfoList) {
                        if (info != null) {
                            String uuid = info.getBoxUuid();
                            String bind = info.getBoxBind();
                            Boolean isError = info.isError();
                            if (isError != null && !isError && !isClick) {
                                isAdd = false;
                                break;
                            } else if (boxUuid.equals(uuid) && boxBind.equals(bind)) {
                                isContains = true;
                            }
                        }
                    }
                }
                if (isAdd) {
                    if (isContains && applicationLockEventInfoList != null) {
                        for (ApplicationLockEventInfo info : applicationLockEventInfoList) {
                            if (info != null) {
                                String uuid = info.getBoxUuid();
                                String bind = info.getBoxBind();
                                Boolean isError = info.isError();
                                if (boxUuid.equals(uuid) && boxBind.equals(bind)) {
                                    Logger.d("onAuth", "old info will replace: " + info.toString());
                                    if (isError != null) {
                                        String cancelRequestId = info.getRequestId();
                                        if (cancelRequestId != null && (isError || isClick)) {
                                            if (cancelRequestIds == null) {
                                                cancelRequestIds = new ArrayList<>();
                                            }
                                            cancelRequestIds.add(cancelRequestId);
                                        }
                                    }
                                    info.setRequestId(requestId);
                                    info.setError(applicationLockEventInfo.isError());
                                    info.setFingerprintUnlock(applicationLockEventInfo.isFingerprintUnlock());
                                    info.setFaceUnlock(applicationLockEventInfo.isFaceUnlock());
                                }
                            }
                        }
                    } else {
                        if (applicationLockEventInfoList == null) {
                            applicationLockEventInfoList = new CopyOnWriteArrayList<>();
                        }
                        applicationLockEventInfoList.add(applicationLockEventInfo);
                    }
                }
            }
        }
        return cancelRequestIds;
    }

    /**
     * 重置掉所有应用锁事件
     */
    public synchronized static void resetApplicationLockEventInfo() {
        applicationLockEventInfoList = null;
    }

    /**
     * 重置掉指定请求id的事件
     *
     * @param requestId
     */
    public synchronized static void resetApplicationLockEventInfo(String requestId) {
        if (applicationLockEventInfoList != null && requestId != null) {
            List<ApplicationLockEventInfo> removeInfoList = null;
            for (ApplicationLockEventInfo applicationLockEventInfo : applicationLockEventInfoList) {
                if (applicationLockEventInfo != null && requestId.equals(applicationLockEventInfo.getRequestId())) {
                    if (removeInfoList == null) {
                        removeInfoList = new ArrayList<>();
                    }
                    removeInfoList.add(applicationLockEventInfo);
                }
            }
            if (removeInfoList != null && !removeInfoList.isEmpty()) {
                for (ApplicationLockEventInfo info : removeInfoList) {
                    if (info != null) {
                        try {
                            applicationLockEventInfoList.remove(info);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * 修改应用锁事件的错误状态
     *
     * @param requestId
     * @param isError   true：准备验证；false：验证报error或者主动取消
     */
    public synchronized static void setApplicationLockEventInfoError(String requestId, boolean isError) {
        if (applicationLockEventInfoList != null && requestId != null) {
            for (ApplicationLockEventInfo applicationLockEventInfo : applicationLockEventInfoList) {
                if (applicationLockEventInfo != null && requestId.equals(applicationLockEventInfo.getRequestId())) {
                    applicationLockEventInfo.setError(isError);
                }
            }
        }
    }

    /**
     * 是否包含主动取消
     *
     * @param requestId
     * @return
     */
    public static boolean containsCancelAuthentication(String requestId) {
        boolean isContains = false;
        if (cancelAuthenticationList != null && requestId != null) {
            isContains = cancelAuthenticationList.contains(requestId);
        }
        return isContains;
    }

    /**
     * 增加主动取消
     *
     * @param requestId
     */
    public static void addCancelAuthentication(String requestId) {
        if (cancelAuthenticationList == null) {
            cancelAuthenticationList = new ArrayList<>();
        }
        if (requestId != null) {
            cancelAuthenticationList.add(requestId);
        }
    }

    /**
     * 删除主动取消
     *
     * @param requestId
     */
    public static void removeCancelAuthentication(String requestId) {
        if (cancelAuthenticationList != null && requestId != null) {
            Iterator<String> iterator = cancelAuthenticationList.iterator();
            while (iterator.hasNext()) {
                String id = iterator.next();
                if (requestId.equals(id)) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * 盒子不可达触发
     *
     * @param boxUuid
     * @param boxBind
     */
    public static void boxUnavailable(String boxUuid, String boxBind) {
        ApplicationLockEventInfo applicationLockEventInfo = DataUtil.getApplicationLockEventInfo(boxUuid, boxBind);
        if (applicationLockEventInfo != null) {
            Boolean isError = applicationLockEventInfo.isError();
            String requestId = applicationLockEventInfo.getRequestId();
            DataUtil.resetApplicationLockEventInfo(requestId);
            if (isError != null && !isError) {
                EulixBiometricManager eulixBiometricManager = EulixBiometricManager.getInstance();
                if (eulixBiometricManager != null) {
                    eulixBiometricManager.cancelAuthenticate(requestId, true);
                }
            }
        }
    }

    public static int getActivityIndex() {
        return activityIndex;
    }

    public static void setActivityIndex(int activityIndex) {
        DataUtil.activityIndex = activityIndex;
    }

    public static List<Long> getValidInputWrongTimestampList(List<Long> inputWrongTimestamp, long inputWindow) {
        List<Long> outputWrongTimestampList = null;
        if (inputWrongTimestamp != null) {
            outputWrongTimestampList = new ArrayList<>();
            if (inputWrongTimestamp.size() > 0) {
                Collections.sort(inputWrongTimestamp, ascendLongComparator);
                int size = inputWrongTimestamp.size();
                if (size > 0) {
                    long lastTimestamp = inputWrongTimestamp.get((size - 1));
                    long firstTimestamp = inputWrongTimestamp.get(0);
                    Long initExpireTimestamp = ((lastTimestamp - firstTimestamp < inputWindow) ? null : firstTimestamp);
                    for (Long timestamp : inputWrongTimestamp) {
                        boolean isAdd = false;
                        if (timestamp != null) {
                            if (initExpireTimestamp == null) {
                                if (lastTimestamp - timestamp < inputWindow) {
                                    isAdd = true;
                                } else {
                                    initExpireTimestamp = timestamp;
                                }
                            } else if (timestamp - initExpireTimestamp >= inputWindow) {
                                if (lastTimestamp - timestamp < inputWindow) {
                                    initExpireTimestamp = null;
                                    isAdd = true;
                                } else {
                                    initExpireTimestamp = timestamp;
                                }
                            }
                        }
                        if (isAdd) {
                            outputWrongTimestampList.add(timestamp);
                        }
                    }
                }
            }
        }
        return outputWrongTimestampList;
    }
}
