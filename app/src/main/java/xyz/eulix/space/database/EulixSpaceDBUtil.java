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

package xyz.eulix.space.database;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xyz.eulix.space.bean.AOSpaceAccessBean;
import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.EulixBoxBaseInfoCompatible;
import xyz.eulix.space.bean.EulixBoxInfo;
import xyz.eulix.space.bean.EulixBoxOtherInfo;
import xyz.eulix.space.bean.EulixBoxToken;
import xyz.eulix.space.bean.EulixBoxTokenDetail;
import xyz.eulix.space.bean.EulixSpaceExtendInfo;
import xyz.eulix.space.bean.EulixSpaceInfo;
import xyz.eulix.space.bean.SecurityEmailInfo;
import xyz.eulix.space.bean.SpaceStatusStatusLineInfo;
import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.bean.bind.InitResponseNetwork;
import xyz.eulix.space.did.bean.DIDCredentialBean;
import xyz.eulix.space.did.bean.DIDDocument;
import xyz.eulix.space.did.bean.DIDReserveBean;
import xyz.eulix.space.event.BoxInsertDeleteEvent;
import xyz.eulix.space.event.BoxOnlineEvent;
import xyz.eulix.space.event.OfflineGranteeEvent;
import xyz.eulix.space.event.SpaceChangeEvent;
import xyz.eulix.space.manager.EulixSpaceDBBoxManager;
import xyz.eulix.space.network.agent.bind.ConnectedNetwork;
import xyz.eulix.space.network.files.FileListItem;
import xyz.eulix.space.network.net.InternetServiceConfigResult;
import xyz.eulix.space.network.notification.GetNotificationResult;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StringUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/7/27 16:30
 */
public class EulixSpaceDBUtil {
    private static final String TAG = EulixSpaceDBUtil.class.getSimpleName();

    private EulixSpaceDBUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    private static String getStringValueFromMap(@NonNull Map<String, String> stringMap, @NonNull String key, String defaultValue) {
        String value = defaultValue;
        if (stringMap.containsKey(key)) {
            value = stringMap.get(key);
        }
        return value;
    }

    public static void insertBox(Context context, Map<String, String> boxValue, int bind) {
        if (context != null && boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)) {
            String boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
            String boxBind = getStringValueFromMap(boxValue, EulixSpaceDBManager.FIELD_BOX_BIND, String.valueOf(bind));
            ContentResolver resolver = context.getContentResolver();
            if (resolver != null) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                contentValues.put(EulixSpaceDBManager.FIELD_BOX_NAME, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_BOX_NAME, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_BOX_INFO, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_BOX_INFO, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_BOX_AUTHORIZATION, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_BOX_AUTHORIZATION, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_BOX_REGISTER, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_BOX_REGISTER, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_BOX_DOMAIN, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_BOX_DOMAIN, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                contentValues.put(EulixSpaceDBManager.FIELD_BOX_STATUS, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_BOX_STATUS, ""));
                contentValues.put(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FILED_BOX_UPDATE_TIME, String.valueOf(System.currentTimeMillis())));
                contentValues.put(EulixSpaceDBManager.FIELD_BOX_TOKEN, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_BOX_TOKEN, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_BOX_USER_INFO, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_BOX_USER_INFO, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_BOX_FILE_LIST, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_BOX_FILE_LIST, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_BOX_OTHER_INFO, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_BOX_OTHER_INFO, ""));
                resolver.insert(EulixSpaceDBManager.BOX_URI, contentValues);
                Logger.d(TAG, "insert, uuid: " + boxUuid + ", bind: " + boxBind);
            }
        }
    }

    private static void deleteBox(Context context) {
        if (context != null) {
            ContentResolver resolver = context.getContentResolver();
            if (resolver != null) {
                resolver.delete(EulixSpaceDBManager.BOX_URI, null, null);
            }
        }
    }

    public static void deleteBox(Context context, Map<String, String> boxValue) {
        if (context != null && boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)) {
            String boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
            String bindValue = null;
            if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                bindValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
            }
            if (boxUuid != null) {
                ContentResolver resolver = context.getContentResolver();
                String selection;
                String[] selectionArgs;
                if (bindValue == null) {
                    selection = EulixSpaceDBManager.FIELD_BOX_UUID + "= ?";
                    selectionArgs = new String[]{boxUuid};
                } else {
                    selection = EulixSpaceDBManager.FIELD_BOX_UUID + "= ? AND " + EulixSpaceDBManager.FIELD_BOX_BIND + "= ?";
                    selectionArgs = new String[]{boxUuid, bindValue};
                }
                if (resolver != null) {
                    resolver.delete(EulixSpaceDBManager.BOX_URI, selection, selectionArgs);
                    Logger.d(TAG, "delete, uuid: " + boxUuid + ", bind: " + bindValue);
                }
            }
        }
    }

    /**
     * JUST FOR TEST
     * @param context
     */
    private static void deleteUninitializedBox(Context context) {
        if (context != null) {
            List<Map<String, String>> boxValues = queryBox(context);
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID) && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                        String boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                        String boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                        String status = boxValue.get(EulixSpaceDBManager.FIELD_BOX_STATUS);
                        if (status != null && (status.equals(String.valueOf(ConstantField.EulixDeviceStatus.ONLINE_UNINITIALIZED))
                                || status.equals(String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_UNINITIALIZED)))) {
                            Map<String, String> queryMap = new HashMap<>();
                            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                            deleteBox(context, queryMap);
                        }
                    }
                }
            }
        }
    }

    public static void offlineTemperateBox(Context context) {
        if (context != null) {
            List<Map<String, String>> boxValues = queryBox(context);
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID) && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                        String boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                        String boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                        boolean isDelete = false;
                        EulixBoxToken eulixBoxToken = null;
                        if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
                            String boxTokenValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                            if (boxTokenValue != null) {
                                try {
                                    eulixBoxToken = new Gson().fromJson(boxTokenValue, EulixBoxToken.class);
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        if (eulixBoxToken != null) {
                            isDelete = (eulixBoxToken.getLoginValid() == null);
                        }
                        if (isDelete && boxUuid != null && boxBind != null && !("1".equals(boxBind) || "-1".equals(boxBind))) {
                            OfflineGranteeEvent offlineGranteeEvent = new OfflineGranteeEvent(boxUuid, boxBind);
                            EventBusUtil.post(offlineGranteeEvent);
                        }
                    }
                }
            }
        }
    }

    public static void offlineTemperateBox(Context context, String currentBoxUuid, String currentBoxBind) {
        if (currentBoxUuid == null || currentBoxBind == null) {
            offlineTemperateBox(context);
        } else {
            if (context != null) {
                List<Map<String, String>> boxValues = queryBox(context);
                if (boxValues != null) {
                    for (Map<String, String> boxValue : boxValues) {
                        if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID) && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                            String boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                            String boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                            boolean isDelete = false;
                            EulixBoxToken eulixBoxToken = null;
                            if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
                                String boxTokenValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                                if (boxTokenValue != null) {
                                    try {
                                        eulixBoxToken = new Gson().fromJson(boxTokenValue, EulixBoxToken.class);
                                    } catch (JsonSyntaxException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            if (eulixBoxToken != null) {
                                isDelete = (eulixBoxToken.getLoginValid() == null);
                            }
                            if (isDelete && boxUuid != null && boxBind != null && !("1".equals(boxBind) || "-1".equals(boxBind))
                                    && !(currentBoxBind.equals(boxBind) && currentBoxUuid.equals(boxUuid))) {
                                OfflineGranteeEvent offlineGranteeEvent = new OfflineGranteeEvent(boxUuid, boxBind);
                                EventBusUtil.post(offlineGranteeEvent);
                            }
                        }
                    }
                }
            }
        }
    }

    public static void offlineTemperateBox(Context context, long diffTime) {
        if (context != null) {
            List<Map<String, String>> boxValues = queryBox(context);
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                            && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)
                            && boxValue.containsKey(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME)) {
                        boolean isDelete = false;
                        String boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                        String boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                        String updateTimeValue = boxValue.get(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME);
                        long updateTime = -1L;
                        if (updateTimeValue != null) {
                            try {
                                updateTime = Long.parseLong(updateTimeValue);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                        EulixBoxToken eulixBoxToken = null;
                        if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
                            String boxTokenValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                            if (boxTokenValue != null) {
                                try {
                                    eulixBoxToken = new Gson().fromJson(boxTokenValue, EulixBoxToken.class);
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        if (eulixBoxToken != null) {
                            isDelete = (eulixBoxToken.getLoginValid() == null);
                        }
                        if (isDelete && boxUuid != null && boxBind != null && !("1".equals(boxBind) || "-1".equals(boxBind))
                                && updateTime >= 0 && ((System.currentTimeMillis() - updateTime) > diffTime)) {
                            OfflineGranteeEvent offlineGranteeEvent = new OfflineGranteeEvent(boxUuid, boxBind);
                            EventBusUtil.post(offlineGranteeEvent);
                        }
                    }
                }
            }
        }
    }

    /**
     * 删除所有扫码登录空间
     * @param context
     */
    public static void deleteTemperateBox(Context context) {
        if (context != null) {
            List<Map<String, String>> boxValues = queryBox(context);
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID) && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                        String boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                        String boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                        boolean isDelete = false;
                        EulixBoxToken eulixBoxToken = null;
                        if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
                            String boxTokenValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                            if (boxTokenValue != null) {
                                try {
                                    eulixBoxToken = new Gson().fromJson(boxTokenValue, EulixBoxToken.class);
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        if (eulixBoxToken != null) {
                            isDelete = (eulixBoxToken.getLoginValid() == null);
                        }
                        if (isDelete && boxUuid != null && boxBind != null && !("1".equals(boxBind) || "-1".equals(boxBind))) {
                            Map<String, String> queryMap = new HashMap<>();
                            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                            deleteBox(context, queryMap);
                            BoxInsertDeleteEvent boxInsertDeleteEvent = new BoxInsertDeleteEvent(boxUuid, boxBind, false);
                            EventBusUtil.post(boxInsertDeleteEvent);
                        }
                    }
                }
            }
        }
    }

    /**
     * 删除指定空间外的扫码登录空间
     * @param context
     * @param currentBoxUuid 指定空间uuid
     * @param currentBoxBind 指定空间绑定状态
     */
    public static void deleteTemperateBox(Context context, String currentBoxUuid, String currentBoxBind) {
        if (currentBoxUuid == null || currentBoxBind == null) {
            deleteTemperateBox(context);
        } else {
            if (context != null) {
                List<Map<String, String>> boxValues = queryBox(context);
                if (boxValues != null) {
                    for (Map<String, String> boxValue : boxValues) {
                        if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID) && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                            String boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                            String boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                            boolean isDelete = false;
                            EulixBoxToken eulixBoxToken = null;
                            if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
                                String boxTokenValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                                if (boxTokenValue != null) {
                                    try {
                                        eulixBoxToken = new Gson().fromJson(boxTokenValue, EulixBoxToken.class);
                                    } catch (JsonSyntaxException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            if (eulixBoxToken != null) {
                                isDelete = (eulixBoxToken.getLoginValid() == null);
                            }
                            if (isDelete && boxUuid != null && boxBind != null && !("1".equals(boxBind) || "-1".equals(boxBind))
                                    && !(currentBoxBind.equals(boxBind) && currentBoxUuid.equals(boxUuid))) {
                                Map<String, String> queryMap = new HashMap<>();
                                queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                                queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                                deleteBox(context, queryMap);
                                BoxInsertDeleteEvent boxInsertDeleteEvent = new BoxInsertDeleteEvent(boxUuid, boxBind, false);
                                EventBusUtil.post(boxInsertDeleteEvent);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 删除过时的扫码登录空间
     * @param context
     * @param diffTime 有效期时长
     */
    public static void deleteTemperateBox(Context context, long diffTime) {
        if (context != null) {
            List<Map<String, String>> boxValues = queryBox(context);
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                            && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)
                            && boxValue.containsKey(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME)) {
                        boolean isDelete = false;
                        String boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                        String boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                        String updateTimeValue = boxValue.get(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME);
                        long updateTime = -1L;
                        if (updateTimeValue != null) {
                            try {
                                updateTime = Long.parseLong(updateTimeValue);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                        EulixBoxToken eulixBoxToken = null;
                        if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
                            String boxTokenValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                            if (boxTokenValue != null) {
                                try {
                                    eulixBoxToken = new Gson().fromJson(boxTokenValue, EulixBoxToken.class);
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        if (eulixBoxToken != null) {
                            isDelete = (eulixBoxToken.getLoginValid() == null);
                        }
                        if (isDelete && boxUuid != null && boxBind != null && !("1".equals(boxBind) || "-1".equals(boxBind))
                                && updateTime >= 0 && ((System.currentTimeMillis() - updateTime) > diffTime)) {
                            Map<String, String> queryMap = new HashMap<>();
                            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                            deleteBox(context, queryMap);
                            BoxInsertDeleteEvent boxInsertDeleteEvent = new BoxInsertDeleteEvent(boxUuid, boxBind, false);
                            EventBusUtil.post(boxInsertDeleteEvent);
                        }
                    }
                }
            }
        }
    }

    private static void handleTemperateBox(Context context, List<Map<String, String>> boxValues) {
        if (context != null && boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                        && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)
                        && boxValue.containsKey(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME)
                        && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)
                        && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_STATUS)) {
                    String boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                    String boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    if (boxUuid != null && boxBind != null) {
                        if (!("1".equals(boxBind) || "-1".equals(boxBind))) {
                            boolean isExpire = true;
                            long loginValidTime = 0L;
                            String loginValid = null;
                            String boxTokenValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                            EulixBoxToken eulixBoxToken = null;
                            if (boxTokenValue != null) {
                                try {
                                    eulixBoxToken = new Gson().fromJson(boxTokenValue, EulixBoxToken.class);
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (eulixBoxToken != null) {
                                loginValid = eulixBoxToken.getLoginValid();
                            }
                            if (loginValid != null && !TextUtils.isEmpty(loginValid)) {
                                try {
                                    loginValidTime = Long.parseLong(loginValid);
                                } catch (NumberFormatException e) {
                                    e.printStackTrace();
                                }
                            }
                            long currentTimestamp = System.currentTimeMillis();
                            if (loginValid != null) {
                                long expireTime = loginValidTime;
                                EulixBoxTokenDetail eulixBoxTokenDetail = DataUtil.getLastBoxToken();
                                if (eulixBoxTokenDetail != null && boxUuid.equals(eulixBoxTokenDetail.getBoxUuid())
                                        && boxBind.equals(eulixBoxTokenDetail.getBoxBind())) {
                                    long tokenExpireTime = eulixBoxToken.getTokenExpire();
                                    if (tokenExpireTime >= 0) {
                                        if (loginValidTime > currentTimestamp) {
                                            expireTime = Math.min(loginValidTime, tokenExpireTime);
                                        } else {
                                            expireTime = tokenExpireTime;
                                        }
                                    }
                                }
                                isExpire = (expireTime <= currentTimestamp);
                            } else if (eulixBoxToken != null) {
                                // 旧扫码空间
                                isExpire = (eulixBoxToken.getTokenExpire() <= currentTimestamp);
                            }
                            if (isExpire) {
                                boolean isUpdate = false;
                                String boxStatusValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_STATUS);
                                int boxStatus = -2;
                                if (boxStatusValue != null) {
                                    try {
                                        boxStatus = Integer.parseInt(boxStatusValue);
                                    } catch (NumberFormatException e) {
                                        e.printStackTrace();
                                    }
                                }
                                switch (boxStatus) {
                                    case ConstantField.EulixDeviceStatus.ACTIVE:
                                        isUpdate = true;
                                        boxStatus = ConstantField.EulixDeviceStatus.REQUEST_LOGIN;
                                        break;
                                    case ConstantField.EulixDeviceStatus.OFFLINE_USE:
                                        isUpdate = true;
                                        boxStatus = ConstantField.EulixDeviceStatus.OFFLINE;
                                        break;
                                    default:
                                        break;
                                }
                                if (isUpdate) {
                                    Map<String, String> newBoxValue = new HashMap<>();
                                    newBoxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                                    newBoxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                                    newBoxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(boxStatus));
                                    updateBox(context, newBoxValue);
                                    EventBusUtil.post(new SpaceChangeEvent(false, false));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void handleTemperateBox(Context context) {
        if (context != null) {
            handleTemperateBox(context, queryBox(context));
        }
    }

    public static void handleTemperateBox(Context context, String boxUuid, String boxBind) {
        if (context != null) {
            if (boxUuid == null && boxBind == null) {
                handleTemperateBox(context);
            } else {
                Map<String, String> queryMap = new HashMap<>();
                queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                handleTemperateBox(context, queryBox(context, queryMap));
            }
        }
    }

    public static void handleGranteeBox(Context context) {
        List<EulixSpaceInfo> eulixSpaceInfoList = null;
        List<Map<String, String>> boxValues = queryBox(context);
        if (boxValues != null) {
            eulixSpaceInfoList = new ArrayList<>();
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                        && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                    String boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                    String boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    if (boxUuid != null && boxBind != null && !"1".equals(boxBind) && !"-1".equals(boxBind)) {
                        EulixSpaceInfo eulixSpaceInfo = new EulixSpaceInfo();
                        eulixSpaceInfo.setBoxUuid(boxUuid);
                        eulixSpaceInfo.setBoxBind(boxBind);
                        eulixSpaceInfoList.add(eulixSpaceInfo);
                    }
                }
            }
        }
        if (eulixSpaceInfoList != null) {
            for (EulixSpaceInfo eulixSpaceInfo : eulixSpaceInfoList) {
                if (eulixSpaceInfo != null) {
                    String boxUuid = eulixSpaceInfo.getBoxUuid();
                    String boxBind = eulixSpaceInfo.getBoxBind();
                    if (boxUuid != null && boxBind != null && !"1".equals(boxBind) && !"-1".equals(boxBind)
                            && (boxBind.equals(getClientAoId(context, boxUuid, "1"))
                            || boxBind.equals(getClientAoId(context, boxUuid, "-1")))) {
                        Map<String, String> boxValue = new HashMap<>();
                        boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                        boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                        deleteBox(context, boxValue);
                        Logger.d(TAG, "common grantee delete: " + boxUuid + ", " + boxBind);
                        BoxInsertDeleteEvent boxInsertDeleteEvent = new BoxInsertDeleteEvent(boxUuid, boxBind, false);
                        EventBusUtil.post(boxInsertDeleteEvent);
                    }
                }
            }
        }
    }

    public static boolean containsBox(Context context, String boxUuid, String boxBind) {
        boolean isContain = false;
        if (context != null && boxUuid != null && boxBind != null) {
            List<Map<String, String>> boxValues = querySpecificBox(context, boxUuid, boxBind);
            isContain = (boxValues != null && !boxValues.isEmpty());
        }
        return isContain;
    }

    public static List<Map<String, String>> querySpecificBox(Context context, String boxUuid, String boxBind) {
        List<Map<String, String>> boxValues = null;
        if (context != null && boxUuid != null && boxBind != null) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            boxValues = queryBox(context, queryMap);
        }
        return boxValues;
    }

    private static void logoutBox(Context context, String boxUuid, String boxBind) {
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
        queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
        List<Map<String, String>> boxValues = queryBox(context, queryMap);
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_STATUS)) {
                    boolean isUpdate = false;
                    String boxStatusValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_STATUS);
                    int boxStatus = -2;
                    if (boxStatusValue != null) {
                        try {
                            boxStatus = Integer.parseInt(boxStatusValue);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                    switch (boxStatus) {
                        case ConstantField.EulixDeviceStatus.ACTIVE:
                            isUpdate = true;
                            boxStatus = ConstantField.EulixDeviceStatus.REQUEST_LOGIN;
                            break;
                        case ConstantField.EulixDeviceStatus.OFFLINE_USE:
                            isUpdate = true;
                            boxStatus = ConstantField.EulixDeviceStatus.OFFLINE;
                            break;
                        default:
                            break;
                    }
                    if (isUpdate) {
                        Map<String, String> newBoxValue = new HashMap<>();
                        newBoxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                        newBoxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                        newBoxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(boxStatus));
                        updateBox(context, newBoxValue);
                        EventBusUtil.post(new BoxOnlineEvent(boxUuid, boxBind, boxStatus));
                    }
                }
            }
        }
    }

    public static void updateBox(Context context, Map<String, String> boxValue) {
        if (context != null && boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)) {
            String boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
            String bindValue = null;
            if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                bindValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
            }
            if (boxUuid != null) {
                ContentResolver resolver = context.getContentResolver();
                String selection;
                String[] selectionArgs;
                if (bindValue == null) {
                    selection = EulixSpaceDBManager.FIELD_BOX_UUID + "= ?";
                    selectionArgs = new String[]{boxUuid};
                } else {
                    selection = EulixSpaceDBManager.FIELD_BOX_UUID + "= ? AND " + EulixSpaceDBManager.FIELD_BOX_BIND + "= ?";
                    selectionArgs = new String[]{boxUuid, bindValue};
                }
                if (resolver != null) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_NAME)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_BOX_NAME, boxValue.get(EulixSpaceDBManager.FIELD_BOX_NAME));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_INFO)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_BOX_INFO, boxValue.get(EulixSpaceDBManager.FIELD_BOX_INFO));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY, boxValue.get(EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_AUTHORIZATION)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_BOX_AUTHORIZATION, boxValue.get(EulixSpaceDBManager.FIELD_BOX_AUTHORIZATION));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_REGISTER)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_BOX_REGISTER, boxValue.get(EulixSpaceDBManager.FIELD_BOX_REGISTER));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_DOMAIN)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_BOX_DOMAIN, boxValue.get(EulixSpaceDBManager.FIELD_BOX_DOMAIN));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_STATUS)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_BOX_STATUS, boxValue.get(EulixSpaceDBManager.FIELD_BOX_STATUS));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME)) {
                        contentValues.put(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME, boxValue.get(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_BOX_TOKEN, boxValue.get(EulixSpaceDBManager.FIELD_BOX_TOKEN));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_USER_INFO)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_BOX_USER_INFO, boxValue.get(EulixSpaceDBManager.FIELD_BOX_USER_INFO));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_FILE_LIST)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_BOX_FILE_LIST, boxValue.get(EulixSpaceDBManager.FIELD_BOX_FILE_LIST));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_OTHER_INFO)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_BOX_OTHER_INFO, boxValue.get(EulixSpaceDBManager.FIELD_BOX_OTHER_INFO));
                    }
                    resolver.update(EulixSpaceDBManager.BOX_URI, contentValues, selection, selectionArgs);
                    Logger.d(TAG, "update, uuid: " + boxUuid + ", bind: " + bindValue);
                }
            }
        }
    }

    public static List<Map<String, String>> queryBox(Context context) {
        return queryBox(context, null, null);
    }

    /**
     * 根据指定栏目内容查询数据
     * @param context
     * @param columnName 栏目名
     * @param columnValue 栏目属性值
     * @return
     */
    public static List<Map<String, String>> queryBox(Context context, String columnName, String columnValue) {
        List<Map<String, String>> boxValues = null;
        if (context != null) {
            ContentResolver resolver = context.getContentResolver();
            if (resolver != null) {
                Cursor cursor = resolver.query(EulixSpaceDBManager.BOX_URI, null
                        , (columnName == null ? null : columnName + "= ?")
                        , ((columnName == null || columnValue == null) ? null : new String[]{columnValue}), null);
                if (cursor != null) {
                    boxValues = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        Map<String, String> boxValue = new HashMap<>();
                        int uuidIndex, nameIndex, infoIndex, publicKeyIndex, authorizationIndex
                                , registerIndex, domainIndex, bindIndex, statusIndex, updateTimeIndex
                                , tokenIndex, userInfoIndex, fileListIndex, otherInfoIndex;
                        if ((uuidIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_UUID)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, cursor.getString(uuidIndex));
                        }
                        if ((nameIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_NAME)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_NAME, cursor.getString(nameIndex));
                        }
                        if ((infoIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_INFO)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_INFO, cursor.getString(infoIndex));
                        }
                        if ((publicKeyIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY, cursor.getString(publicKeyIndex));
                        }
                        if ((authorizationIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_AUTHORIZATION)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_AUTHORIZATION, cursor.getString(authorizationIndex));
                        }
                        if ((registerIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_REGISTER)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_REGISTER, cursor.getString(registerIndex));
                        }
                        if ((domainIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_DOMAIN)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_DOMAIN, cursor.getString(domainIndex));
                        }
                        if ((bindIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_BIND)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, cursor.getString(bindIndex));
                        }
                        if ((statusIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_STATUS)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, cursor.getString(statusIndex));
                        }
                        if ((updateTimeIndex = cursor.getColumnIndex(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME, cursor.getString(updateTimeIndex));
                        }
                        if ((tokenIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_TOKEN)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_TOKEN, cursor.getString(tokenIndex));
                        }
                        if ((userInfoIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_USER_INFO)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_USER_INFO, cursor.getString(userInfoIndex));
                        }
                        if ((fileListIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_FILE_LIST)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_FILE_LIST, cursor.getString(fileListIndex));
                        }
                        if ((otherInfoIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_OTHER_INFO)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_OTHER_INFO, cursor.getString(otherInfoIndex));
                        }
                        boxValues.add(boxValue);
                    }
                    cursor.close();
                }
            }
        }
        return boxValues;
    }

    /**
     * 根据若干栏目查询数据
     * @param context
     * @param columnMap 栏目名和属性值的表
     * @return
     */
    public static List<Map<String, String>> queryBox(Context context, Map<String, String> columnMap) {
        List<Map<String, String>> boxValues = null;
        StringBuilder selectionBuilder = null;
        String[] selectionArgs = null;
        if (context != null) {
            if (columnMap != null) {
                Set<Map.Entry<String, String>> entrySet = columnMap.entrySet();
                int size = entrySet.size();
                if (size > 0) {
                    selectionArgs = new String[size];
                    int index = 0;
                    for (Map.Entry<String, String> entry : entrySet) {
                        if (entry != null) {
                            String key = entry.getKey();
                            String value = entry.getValue();
                            if (key != null && value != null) {
                                if (selectionBuilder == null) {
                                    selectionBuilder = new StringBuilder((key + "= ?"));
                                } else {
                                    selectionBuilder.append(" AND ");
                                    selectionBuilder.append(key);
                                    selectionBuilder.append("= ?");
                                }
                                selectionArgs[index] = value;
                                index += 1;
                            }
                        }
                    }
                }
            }
            ContentResolver resolver = context.getContentResolver();
            if (resolver != null) {
                Cursor cursor = resolver.query(EulixSpaceDBManager.BOX_URI, null
                        , (selectionBuilder == null ? null : selectionBuilder.toString())
                        , selectionArgs, null);
                if (cursor != null) {
                    boxValues = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        Map<String, String> boxValue = new HashMap<>();
                        int uuidIndex, nameIndex, infoIndex, publicKeyIndex, authorizationIndex
                                , registerIndex, domainIndex, bindIndex, statusIndex, updateTimeIndex
                                , tokenIndex, userInfoIndex, fileListIndex, otherInfoIndex;
                        if ((uuidIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_UUID)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, cursor.getString(uuidIndex));
                        }
                        if ((nameIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_NAME)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_NAME, cursor.getString(nameIndex));
                        }
                        if ((infoIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_INFO)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_INFO, cursor.getString(infoIndex));
                        }
                        if ((publicKeyIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY, cursor.getString(publicKeyIndex));
                        }
                        if ((authorizationIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_AUTHORIZATION)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_AUTHORIZATION, cursor.getString(authorizationIndex));
                        }
                        if ((registerIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_REGISTER)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_REGISTER, cursor.getString(registerIndex));
                        }
                        if ((domainIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_DOMAIN)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_DOMAIN, cursor.getString(domainIndex));
                        }
                        if ((bindIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_BIND)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, cursor.getString(bindIndex));
                        }
                        if ((statusIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_STATUS)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, cursor.getString(statusIndex));
                        }
                        if ((updateTimeIndex = cursor.getColumnIndex(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME, cursor.getString(updateTimeIndex));
                        }
                        if ((tokenIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_TOKEN)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_TOKEN, cursor.getString(tokenIndex));
                        }
                        if ((userInfoIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_USER_INFO)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_USER_INFO, cursor.getString(userInfoIndex));
                        }
                        if ((fileListIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_FILE_LIST)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_FILE_LIST, cursor.getString(fileListIndex));
                        }
                        if ((otherInfoIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_BOX_OTHER_INFO)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_OTHER_INFO, cursor.getString(otherInfoIndex));
                        }
                        boxValues.add(boxValue);
                    }
                    cursor.close();
                }
            }
        }
        return boxValues;
    }

    /**
     * 查询当前使用中(在线或离线使用)的盒子uuid
     * @param context
     * @return
     */
    public static String queryAvailableBoxUuid(Context context) {
        String boxUuid = null;
        if (context != null) {
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
            if (boxValues != null && !boxValues.isEmpty()) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null) {
                        boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                        break;
                    }
                }
            } else {
                boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                        , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
                if(boxValues != null && !boxValues.isEmpty()) {
                    for (Map<String, String> boxValue : boxValues) {
                        if (boxValue != null) {
                            boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                            break;
                        }
                    }
                }
            }
        }
        return boxUuid;
    }

    /**
     * 查询当前使用中(在线或离线使用)的用户id
     * @param context
     * @return
     */
    public static String getCurrentUserId(Context context) {
        String userId = "";
        UserInfo userInfo = EulixSpaceDBUtil.getCompatibleActiveUserInfo(context);
        if (userInfo != null) {
            userId = userInfo.getUserId();
        }
        return userId;
    }

    /**
     * 查询当前使用中(在线或离线使用)的盒子publickKey
     * @param context
     * @return
     */
    public static String getAvailableBoxPublicKey(Context context) {
        String boxPublicKey = null;
        if (context != null) {
            List<Map<String, String>> boxValues = queryAvailableBoxValues(context);
            if (boxValues != null && !boxValues.isEmpty()) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null) {
                        boxPublicKey = boxValue.get(EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY);
                        break;
                    }
                }
            }
        }
        return boxPublicKey;
    }

    /**
     * 查询当前使用中(在线或离线使用)的盒子信息
     * @param context
     * @return
     */
    private static List<Map<String, String>> queryAvailableBoxValues(Context context) {
        if (context != null) {
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
            if (boxValues == null || boxValues.isEmpty()){
                boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                        , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
            }
            return boxValues;
        }
        return null;
    }

    //获取auth key
    public static String queryActiveAuthKey(Context context) {
        String authKey = null;
        if (context != null) {
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null) {
                        authKey = boxValue.get(EulixSpaceDBManager.FIELD_BOX_AUTHORIZATION);
                        break;
                    }
                }
            }
        }
        return authKey;
    }

    public static boolean updateBoxDomain(Context context, String boxUuid, String boxBind, String boxDomain) {
        boolean isUpdate = false;
        if (context != null && boxUuid != null && boxBind != null && boxDomain != null) {
            String currentBoxDomain = null;
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            List<Map<String, String>> boxValues = queryBox(context, queryMap);
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_DOMAIN)) {
                        currentBoxDomain = boxValue.get(EulixSpaceDBManager.FIELD_BOX_DOMAIN);
                        break;
                    }
                }
            }
            isUpdate = (!boxDomain.equals(currentBoxDomain));
            if (isUpdate) {
                Map<String, String> boxV = new HashMap<>();
                boxV.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                boxV.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                boxV.put(EulixSpaceDBManager.FIELD_BOX_DOMAIN, boxDomain);
                updateBox(context, boxV);
            }
        }
        return isUpdate;
    }

    public static EulixSpaceExtendInfo getSpaceInfoFromUserDomain(Context context, String userDomain) {
        EulixSpaceExtendInfo eulixSpaceExtendInfo = null;
        if (context != null && userDomain != null) {
            List<Map<String, String>> boxValues = queryBox(context);
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID) && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                        String boxDomain = null;
                        if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_DOMAIN)) {
                            boxDomain = boxValue.get(EulixSpaceDBManager.FIELD_BOX_DOMAIN);
                        }
                        boolean isFind = userDomain.equals(boxDomain);
                        if (!isFind && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_INFO)) {
                            EulixBoxInfo eulixBoxInfo = null;
                            String boxInfoValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_INFO);
                            if (boxInfoValue != null) {
                                try {
                                    eulixBoxInfo = new Gson().fromJson(boxInfoValue, EulixBoxInfo.class);
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (eulixBoxInfo != null) {
                                AOSpaceAccessBean aoSpaceAccessBean = eulixBoxInfo.getAoSpaceAccessBean();
                                if (aoSpaceAccessBean != null) {
                                    isFind = userDomain.equals(aoSpaceAccessBean.getUserDomain());
                                }
                            }
                        }
                        if (isFind) {
                            eulixSpaceExtendInfo = new EulixSpaceExtendInfo();
                            eulixSpaceExtendInfo.setBoxUuid(boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID));
                            eulixSpaceExtendInfo.setBoxBind(boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND));
                            if (boxValue.containsKey(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME)) {
                                eulixSpaceExtendInfo.setBoxUpdateTime(boxValue.get(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME));
                            }
                            break;
                        }
                    }
                }
            }
        }
        return eulixSpaceExtendInfo;
    }

    /**
     * 查询使用空间存储的文件列表数据
     * @param context
     * @param currentId 文件uuid或根目录id
     * @return
     */
    public static List<FileListItem> generateFileListItems(Context context, String currentId) {
        List<FileListItem> fileListItems = null;
        if (context != null && currentId != null) {
            String fileListValue = null;
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
            if (boxValues == null || boxValues.size() <= 0) {
                boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                        , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
            }
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null) {
                        fileListValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_FILE_LIST);
                        break;
                    }
                }
            }
            if (fileListValue != null) {
                Map<String, List<FileListItem>> fileListMap = null;
                try {
                    fileListMap = new Gson().fromJson(fileListValue, new TypeToken<Map<String, List<FileListItem>>>(){}.getType());
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
                if (fileListMap != null && fileListMap.containsKey(currentId)) {
                    List<FileListItem> fileListItemList = fileListMap.get(currentId);
                    if (fileListItemList != null) {
                        fileListItems = new ArrayList<>(fileListItemList);
                    }
                }
            }
        }
        return fileListItems;
    }

    /**
     * 查询指定空间的状态
     * @param context
     * @param boxUuid 指定盒子uuid
     * @param boxBind 指定盒子绑定状态
     * @return
     */
    public static int getDeviceStatus(@NonNull Context context, String boxUuid, String boxBind) {
        int status = -2;
        if (boxUuid != null && boxBind != null) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            List<Map<String, String>> boxValues = queryBox(context, queryMap);
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_STATUS)) {
                        String statusValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_STATUS);
                        if (statusValue != null) {
                            try {
                                status = Integer.parseInt(statusValue);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }
                }
            }
        }
        return status;
    }

    /**
     * 获取当前盒子的bind数据
     * @param context
     * @return
     */
    public static String getActiveDeviceUserBind(@NonNull Context context) {
        String bind = "0";
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (boxValues == null || boxValues.size() <= 0) {
            boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
        }
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                    bind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    break;
                }
            }
        }
        return bind;
    }

    private static int getUserIdentity(List<Map<String, String>> boxValues) {
        int identity = ConstantField.UserIdentity.NO_IDENTITY;
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                    String bindValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    if (bindValue != null) {
                        if ("1".equals(bindValue)) {
                            identity = ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY;
                        } else if ("-1".equals(bindValue)) {
                            identity = ConstantField.UserIdentity.MEMBER_IDENTITY;
                        } else {
                            identity = ConstantField.UserIdentity.MEMBER_GRANTEE;
                            if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_USER_INFO)) {
                                String userInfoValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_USER_INFO);
                                if (userInfoValue != null) {
                                    Map<String, UserInfo> userInfoMap = null;
                                    try {
                                        userInfoMap = new Gson().fromJson(userInfoValue, new TypeToken<Map<String, UserInfo>>(){}.getType());
                                    } catch (JsonSyntaxException e) {
                                        e.printStackTrace();
                                    }
                                    if (userInfoMap != null) {
                                        Set<Map.Entry<String, UserInfo>> entrySet = userInfoMap.entrySet();
                                        for (Map.Entry<String, UserInfo> entry : entrySet) {
                                            if (entry != null) {
                                                UserInfo userInfo = entry.getValue();
                                                if (userInfo != null && bindValue.equals(userInfo.getUserId())) {
                                                    identity = (userInfo.isAdmin() ? ConstantField.UserIdentity.ADMINISTRATOR_GRANTEE : ConstantField.UserIdentity.MEMBER_GRANTEE);
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
            }
        }
        return identity;
    }

    /**
     * @param context
     * @return 当前用户在活跃盒子上的身份，见ConstantField.UserIdentity
     */
    public static int getActiveDeviceUserIdentity(@NonNull Context context) {
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (boxValues == null || boxValues.size() <= 0) {
            boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
        }
        return getUserIdentity(boxValues);
    }

    public static int getDeviceUserIdentity(@NonNull Context context, String boxUuid, String boxBind) {
        int identity = ConstantField.UserIdentity.NO_IDENTITY;
        if (boxUuid != null && boxBind != null) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            identity = getUserIdentity(queryBox(context, queryMap));
        }
        return identity;
    }

    /**
     * 查询使用空间存储的用户信息
     * @param context
     * @return
     */
    public static Map<String, UserInfo> getSpecificUserInfo(Context context, String boxUuid, String boxBind) {
        Map<String, UserInfo> userInfoMap = null;
        if (context != null && boxUuid != null && boxBind != null) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            String userInfoValue = null;
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, queryMap);
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null) {
                        userInfoValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_USER_INFO);
                        break;
                    }
                }
            }
            if (userInfoValue != null) {
                try {
                    userInfoMap = new Gson().fromJson(userInfoValue, new TypeToken<Map<String, UserInfo>>(){}.getType());
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        return userInfoMap;
    }

    /**
     * 查询使用空间存储的用户信息
     * @param context
     * @return
     */
    public static Map<String, UserInfo> getActiveUserInfo(Context context) {
        Map<String, UserInfo> userInfoMap = null;
        if (context != null) {
            String userInfoValue = null;
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
            if (boxValues == null || boxValues.size() <= 0) {
                boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                        , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
            }
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null) {
                        userInfoValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_USER_INFO);
                        break;
                    }
                }
            }
            if (userInfoValue != null) {
                try {
                    userInfoMap = new Gson().fromJson(userInfoValue, new TypeToken<Map<String, UserInfo>>(){}.getType());
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        return userInfoMap;
    }

    /**
     * 查询使用空间存储的指定用户信息
     * @param context
     * @param clientUuid 指定用户uuid
     * @return
     */
    public static UserInfo getActiveUserInfo(Context context, String clientUuid) {
        UserInfo userInfo = null;
        if (context != null) {
            String userInfoValue = null;
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
            if (boxValues == null || boxValues.size() <= 0) {
                boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                        , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
            }
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_USER_INFO)) {
                        userInfoValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_USER_INFO);
                        break;
                    }
                }
            }
            if (userInfoValue != null) {
                Map<String, UserInfo> userInfoMap = null;
                try {
                    userInfoMap = new Gson().fromJson(userInfoValue, new TypeToken<Map<String, UserInfo>>(){}.getType());
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
                if (userInfoMap != null && clientUuid != null && userInfoMap.containsKey(clientUuid)) {
                    userInfo = userInfoMap.get(clientUuid);
                }
            }
        }
        return userInfo;
    }

    public static UserInfo getActiveGranterUserInfo(Context context) {
        UserInfo userInfo = null;
        if (context != null) {
            String boxBind = null;
            String userInfoValue = null;
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
            if (boxValues == null || boxValues.size() <= 0) {
                boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                        , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
            }
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)
                            && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_USER_INFO)) {
                        boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                        userInfoValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_USER_INFO);
                        break;
                    }
                }
            }
            if (boxBind != null && userInfoValue != null) {
                Map<String, UserInfo> userInfoMap = null;
                try {
                    userInfoMap = new Gson().fromJson(userInfoValue, new TypeToken<Map<String, UserInfo>>(){}.getType());
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
                if ("1".equals(boxBind) || "-1".equals(boxBind)) {
                    String clientUuid = DataUtil.getClientUuid(context);
                    if (userInfoMap != null && clientUuid != null && userInfoMap.containsKey(clientUuid)) {
                        userInfo = userInfoMap.get(clientUuid);
                    }
                } else if (userInfoMap != null) {
                    Set<Map.Entry<String, UserInfo>> entrySet = userInfoMap.entrySet();
                    for (Map.Entry<String, UserInfo> entry : entrySet) {
                        if (entry != null) {
                            UserInfo info = entry.getValue();
                            if (info != null && boxBind.equals(info.getUserId())) {
                                userInfo = info;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return userInfo;
    }

    public static List<UserInfo> getGranterUserInfoList(Context context, Map<String, String> queryMap) {
        List<UserInfo> userInfoList = null;
        String clientUuid = null;
        if (context != null) {
            List<Map<String, String>> boxValues = null;
            if (queryMap == null) {
                boxValues = queryBox(context);
            } else {
                boxValues = queryBox(context, queryMap);
            }
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)
                            && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_USER_INFO)) {
                        String boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                        String userInfoMapValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_USER_INFO);
                        Map<String, UserInfo> userInfoMap = null;
                        if (userInfoMapValue != null) {
                            try {
                                userInfoMap = new Gson().fromJson(userInfoMapValue, new TypeToken<Map<String, UserInfo>>(){}.getType());
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        if ("1".equals(boxBind) || "-1".equals(boxBind)) {
                            clientUuid = DataUtil.getClientUuid(context);
                        } else if (boxBind != null) {
                            if (userInfoMap != null) {
                                Set<Map.Entry<String, UserInfo>> entrySet = userInfoMap.entrySet();
                                for (Map.Entry<String, UserInfo> entry : entrySet) {
                                    if (entry != null) {
                                        UserInfo info = entry.getValue();
                                        if (info != null && boxBind.equals(info.getUserId())) {
                                            clientUuid = entry.getKey();
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                        if (clientUuid != null) {
                            userInfoList = new ArrayList<>();
                            if (userInfoMap != null && userInfoMap.containsKey(clientUuid)) {
                                UserInfo userInfo = userInfoMap.get(clientUuid);
                                userInfoList.add(userInfo);
                            }
                        }
                    }
                }
            }
        }
        return userInfoList;
    }

    public static List<UserInfo> getGranterUserInfoList(Context context, String clientUuid, Map<String, String> queryMap) {
        List<UserInfo> userInfoList = null;
        if (context != null && clientUuid != null) {
            List<Map<String, String>> boxValues = null;
            if (queryMap == null) {
                boxValues = queryBox(context);
            } else {
                boxValues = queryBox(context, queryMap);
            }
            if (boxValues != null) {
                userInfoList = new ArrayList<>();
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)
                            && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_USER_INFO)) {
                        String boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                        if ("1".equals(boxBind) || "-1".equals(boxBind)) {
                            String userInfoMapValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_USER_INFO);
                            if (userInfoMapValue != null) {
                                Map<String, UserInfo> userInfoMap = null;
                                try {
                                    userInfoMap = new Gson().fromJson(userInfoMapValue, new TypeToken<Map<String, UserInfo>>(){}.getType());
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                                if (userInfoMap != null && userInfoMap.containsKey(clientUuid)) {
                                    UserInfo userInfo = userInfoMap.get(clientUuid);
                                    userInfoList.add(userInfo);
                                }
                            }
                        }
                    }
                }
            }
        }
        return userInfoList;
    }

    private static String getClientAoId(Context context, List<Map<String, String>> boxValues) {
        String aoId = null;
        String boxBind = null;
        String userInfoValue = null;
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)
                        && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_USER_INFO)) {
                    boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    userInfoValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_USER_INFO);
                    break;
                }
            }
        }
        if (boxBind != null) {
            if ("1".equals(boxBind) || "-1".equals(boxBind)) {
                if (userInfoValue != null && context != null) {
                    String clientUuid = DataUtil.getClientUuid(context);
                    Map<String, UserInfo> userInfoMap = null;
                    try {
                        userInfoMap = new Gson().fromJson(userInfoValue, new TypeToken<Map<String, UserInfo>>(){}.getType());
                    } catch (JsonSyntaxException e) {
                        e.printStackTrace();
                    }
                    if (userInfoMap != null && clientUuid != null && userInfoMap.containsKey(clientUuid)) {
                        UserInfo userInfo = userInfoMap.get(clientUuid);
                        if (userInfo != null) {
                            aoId = userInfo.getUserId();
                        }
                    }
                }
            } else {
                aoId = boxBind;
            }
        }
        return aoId;
    }

    public static String getClientAoId(Context context) {
        List<Map<String, String>> boxValues = queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (boxValues == null || boxValues.size() <= 0) {
            boxValues = queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
        }
        return getClientAoId(context, boxValues);
    }

    public static String getClientAoId(Context context, String boxUuid, String boxBind) {
        String aoId = null;
        if (context != null && boxUuid != null && boxBind != null) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            aoId = getClientAoId(context, queryBox(context, queryMap));
        } else {
            aoId = getClientAoId(context);
        }
        return aoId;
    }

    private static String getCompatibleActiveClientUuid(Context context, List<Map<String, String>> boxValues) {
        String clientUuid = null;
        String userInfoValue = null;
        String bindValue = null;
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null) {
                    userInfoValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_USER_INFO);
                    bindValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    break;
                }
            }
        }
        if (userInfoValue != null && bindValue != null) {
            Map<String, UserInfo> userInfoMap = null;
            try {
                userInfoMap = new Gson().fromJson(userInfoValue, new TypeToken<Map<String, UserInfo>>(){}.getType());
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
            if ("1".equals(bindValue) || "-1".equals(bindValue)) {
                clientUuid = DataUtil.getClientUuid(context);
            } else {
                if (userInfoMap != null) {
                    Set<Map.Entry<String, UserInfo>> entrySet = userInfoMap.entrySet();
                    for (Map.Entry<String, UserInfo> entry : entrySet) {
                        if (entry != null) {
                            UserInfo info = entry.getValue();
                            if (info != null && bindValue.equals(info.getUserId())) {
                                clientUuid = entry.getKey();
                                break;
                            }
                        }
                    }
                }
            }
        }
        return clientUuid;
    }

    /**
     * 查询使用盒子下所映射的用户uuid
     * @param context
     * @return 绑定状态：自己的uuid；扫码状态：授权用户的uuid
     */
    public static String getCompatibleActiveClientUuid(Context context) {
        String clientUuid = null;
        if (context != null) {
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
            if (boxValues == null || boxValues.size() <= 0) {
                boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                        , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
            }
            clientUuid = getCompatibleActiveClientUuid(context, boxValues);
        }
        return clientUuid;
    }

    public static String getCompatibleActiveClientUuid(Context context, String boxUuid, String boxBind) {
        String clientUuid = null;
        if (context != null && boxUuid != null && boxBind != null) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, queryMap);
            clientUuid = getCompatibleActiveClientUuid(context, boxValues);
        }
        return clientUuid;
    }

    /**
     * 查询使用盒子下所映射的用户数据
     * @param context
     * @return
     */
    public static UserInfo getCompatibleActiveUserInfo(Context context) {
        UserInfo userInfo = null;
        if (context != null) {
            String userInfoValue = null;
            String bindValue = null;
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
            if (boxValues == null || boxValues.size() <= 0) {
                boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                        , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
            }
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null) {
                        userInfoValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_USER_INFO);
                        bindValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                        break;
                    }
                }
            }
            if (userInfoValue != null && bindValue != null) {
                Map<String, UserInfo> userInfoMap = null;
                try {
                    userInfoMap = new Gson().fromJson(userInfoValue, new TypeToken<Map<String, UserInfo>>(){}.getType());
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
                String clientUuid = null;
                if ("1".equals(bindValue) || "-1".equals(bindValue)) {
                    clientUuid = DataUtil.getClientUuid(context);
                } else {
                    if (userInfoMap != null) {
                        Set<Map.Entry<String, UserInfo>> entrySet = userInfoMap.entrySet();
                        for (Map.Entry<String, UserInfo> entry : entrySet) {
                            if (entry != null) {
                                UserInfo info = entry.getValue();
                                if (info != null && bindValue.equals(info.getUserId())) {
                                    clientUuid = entry.getKey();
                                    break;
                                }
                            }
                        }
                    }
                }
                if (userInfoMap != null && clientUuid != null && userInfoMap.containsKey(clientUuid)) {
                    userInfo = userInfoMap.get(clientUuid);
                }
            }
        }
        return userInfo;
    }

    /**
     * 当前盒子所处的网络环境
     * @param context
     * @return true: 局域网；false：公网
     */
    public static boolean isActiveBoxLAN(@NonNull Context context) {
        boolean isLAN = false;
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_INFO)) {
                    String boxInfo = boxValue.get(EulixSpaceDBManager.FIELD_BOX_INFO);
                    if (boxInfo != null) {
                        EulixBoxInfo eulixBoxInfo = null;
                        try {
                            eulixBoxInfo = new Gson().fromJson(boxInfo, EulixBoxInfo.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                        if (eulixBoxInfo != null) {
                            isLAN = eulixBoxInfo.isLAN();
                            break;
                        }
                    }
                }
            }
        }
        return isLAN;
    }

    public static String getIpAddressUrl(Context context, String boxUuid, boolean isForce) {
        String ipAddress = null;
        if (context != null && boxUuid != null) {
            List<Map<String, String>> boxValues = queryBox(context, EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_INFO)) {
                        String boxInfo = boxValue.get(EulixSpaceDBManager.FIELD_BOX_INFO);
                        if (boxInfo != null) {
                            EulixBoxInfo eulixBoxInfo = null;
                            try {
                                eulixBoxInfo = new Gson().fromJson(boxInfo, EulixBoxInfo.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                            if (eulixBoxInfo != null) {
                                if (isForce || eulixBoxInfo.isLAN()) {
                                    List<InitResponseNetwork> networks = eulixBoxInfo.getNetworks();
                                    if (networks != null) {
                                        for (InitResponseNetwork network : networks) {
                                            if (network != null) {
                                                ipAddress = network.getIp();
                                                if (ipAddress != null) {
                                                    ipAddress = ipAddress + ":" + network.getPort();
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }
        if (ipAddress != null) {
            if (!ipAddress.startsWith("http:")) {
                ipAddress = "http://" + ipAddress;
            }
            if (!ipAddress.endsWith("/")) {
                ipAddress = ipAddress + "/";
            }
        }
        return ipAddress;
    }

    /**
     * 获取当前空间数量
     * @param context
     * @param isActive 是否只统计使用的
     * @return
     */
    public static int getDeviceNumber(Context context, boolean isActive) {
        int number = 0;
        if (context != null) {
            List<Map<String, String>> totalBoxValues = EulixSpaceDBUtil.queryBox(context);
            if (totalBoxValues != null) {
                for (Map<String, String> totalBoxValue : totalBoxValues) {
                    if (totalBoxValue != null && totalBoxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_STATUS)) {
                        int status = -1;
                        String statusValue = totalBoxValue.get(EulixSpaceDBManager.FIELD_BOX_STATUS);
                        if (statusValue != null) {
                            try {
                                status = Integer.parseInt(statusValue);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                        if (isActive) {
                            if (status == ConstantField.EulixDeviceStatus.ACTIVE || status == ConstantField.EulixDeviceStatus.OFFLINE_USE) {
                                number += 1;
                            }
                        } else {
                            if (status >= ConstantField.EulixDeviceStatus.OFFLINE && status <= ConstantField.EulixDeviceStatus.ONLINE_UNINITIALIZED) {
                                number += 1;
                            }
                        }
                    }
                }
            }
        }
        return number;
    }

    public static void updateSpaceStatusResponseLineInfo(Context context, String boxUuid, String boxBind, int code, String message) {
        if (context != null && boxUuid != null && boxBind != null) {
            boolean isLogout = false;
            switch (code) {
                case ConstantField.KnownError.SwitchPlatformError.REDIRECT_INVALID_ERROR:
                case ConstantField.KnownError.SwitchPlatformError.DOMAIN_NON_EXIST_ERROR:
                    isLogout = true;
                    break;
                default:
                    break;
            }
            if (isLogout) {
                logoutBox(context, boxUuid, boxBind);
            }
            SpaceStatusStatusLineInfo spaceStatusStatusLineInfo = new SpaceStatusStatusLineInfo();
            spaceStatusStatusLineInfo.setCode(code);
            spaceStatusStatusLineInfo.setMessage(message);
            boolean isHandle = false;
            JSONObject jsonObject = null;
            EulixSpaceDBBoxManager eulixSpaceDBBoxManager = EulixSpaceDBBoxManager.getInstance(boxUuid, boxBind);
            if (eulixSpaceDBBoxManager != null) {
                isHandle = true;
                jsonObject = new JSONObject();
                try {
                    jsonObject.put("spaceStatusStatusLineInfo", new Gson().toJson(spaceStatusStatusLineInfo, SpaceStatusStatusLineInfo.class));
                } catch (JSONException e) {
                    e.printStackTrace();
                    isHandle = false;
                }
            }
            if (isHandle) {
                eulixSpaceDBBoxManager.updateBoxInfo(jsonObject, null);
            } else {
                boolean isUpdate = false;
                EulixBoxInfo eulixBoxInfo = getSpecificBoxInfo(context, boxUuid, boxBind);
                if (eulixBoxInfo != null) {
                    isUpdate = !SpaceStatusStatusLineInfo.compare(spaceStatusStatusLineInfo, eulixBoxInfo.getSpaceStatusStatusLineInfo());
                }
                if (isUpdate) {
                    eulixBoxInfo.setSpaceStatusStatusLineInfo(spaceStatusStatusLineInfo);
                    Map<String, String> boxValue = new HashMap<>();
                    boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                    boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                    boxValue.put(EulixSpaceDBManager.FIELD_BOX_INFO, new Gson().toJson(eulixBoxInfo, EulixBoxInfo.class));
                    EulixSpaceDBUtil.updateBox(context, boxValue);
                }
            }
        }
    }

    /**
     * 设置指定盒子的局域网状态
     * @param context
     * @param boxUuid 指定盒子uuid
     * @param isEnable 局域网是否可用
     */
    public static void setLANEnable(Context context, String boxUuid, boolean isEnable) {
        if (context != null && boxUuid != null) {
            List<Map<String, String>> boxValues = queryBox(context, EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            if (boxValues != null) {
                for (Map<String, String> boxV : boxValues) {
                    if (boxV != null && boxV.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                        String boxBind = boxV.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                        boolean isUpdate = true;
                        boolean isHandle = false;
                        JSONObject jsonObject = null;
                        EulixSpaceDBBoxManager eulixSpaceDBBoxManager = EulixSpaceDBBoxManager.getInstance(boxUuid, boxBind);
                        if (eulixSpaceDBBoxManager != null) {
                            isHandle = true;
                            jsonObject = new JSONObject();
                            try {
                                jsonObject.put("isLAN", isEnable);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                isHandle = false;
                            }
                        }
                        if (isHandle) {
                            eulixSpaceDBBoxManager.updateBoxInfo(jsonObject, null);
                        } else {
                            EulixBoxInfo eulixBoxInfo = null;
                            if (boxV.containsKey(EulixSpaceDBManager.FIELD_BOX_INFO)) {
                                String boxInfo = StringUtil.nullToEmpty(boxV.get(EulixSpaceDBManager.FIELD_BOX_INFO));
                                if (TextUtils.isEmpty(boxInfo.trim())) {
                                    eulixBoxInfo = new EulixBoxInfo();
                                } else {
                                    try {
                                        eulixBoxInfo = new Gson().fromJson(boxInfo, EulixBoxInfo.class);
                                    } catch (JsonSyntaxException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            if (eulixBoxInfo == null && !"1".equals(boxBind)) {
                                eulixBoxInfo = new EulixBoxInfo();
                            }
                            if (boxBind != null && eulixBoxInfo != null) {
                                isUpdate = (eulixBoxInfo.isLAN() != isEnable);
                                if (isUpdate) {
                                    eulixBoxInfo.setLAN(isEnable);
                                    Map<String, String> boxValue = new HashMap<>();
                                    boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                                    boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                                    boxValue.put(EulixSpaceDBManager.FIELD_BOX_INFO, new Gson().toJson(eulixBoxInfo, EulixBoxInfo.class));
                                    updateBox(context, boxValue);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static AOSpaceAccessBean getActiveAOSpaceBean(Context context) {
        AOSpaceAccessBean aoSpaceAccessBean = null;
        if (context != null) {
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
            if (boxValues == null || boxValues.size() <= 0) {
                boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                        , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
            }
            if (boxValues != null && !boxValues.isEmpty()) {
                aoSpaceAccessBean = getAOSpaceBean(context, boxValues);
            }
        }
        return aoSpaceAccessBean;
    }

    public static AOSpaceAccessBean getSpecificAOSpaceBean(Context context, String boxUuid, String boxBind) {
        AOSpaceAccessBean aoSpaceAccessBean = null;
        if (context != null && boxUuid != null && boxBind != null) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            aoSpaceAccessBean = getAOSpaceBean(context, queryBox(context, queryMap));
        }
        return aoSpaceAccessBean;
    }

    private static AOSpaceAccessBean getAOSpaceBean(Context context, List<Map<String, String>> boxValues) {
        AOSpaceAccessBean aoSpaceAccessBean = null;
        if (context != null && boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_INFO)) {
                    EulixBoxInfo eulixBoxInfo = null;
                    String boxInfo = StringUtil.nullToEmpty(boxValue.get(EulixSpaceDBManager.FIELD_BOX_INFO));
                    if (!TextUtils.isEmpty(boxInfo.trim())) {
                        try {
                            eulixBoxInfo = new Gson().fromJson(boxInfo, EulixBoxInfo.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                    if (eulixBoxInfo != null) {
                        aoSpaceAccessBean = eulixBoxInfo.getAoSpaceAccessBean();
                    }
                    break;
                }
            }
        }
        return aoSpaceAccessBean;
    }

    public static void setAOSpaceBean(@NonNull Context context, String boxUuid, String boxBind, InternetServiceConfigResult internetServiceConfigResult, boolean isSet) {
        if (boxUuid != null && boxBind != null) {
            AOSpaceAccessBean aoSpaceAccessBean = null;
            Boolean isInternetAccess = null;
            Boolean isLanAccess = null;
            List<ConnectedNetwork> connectedNetworks = null;
            String userDomain = null;
            if (internetServiceConfigResult != null) {
                connectedNetworks = internetServiceConfigResult.getConnectedNetwork();
                userDomain = internetServiceConfigResult.getUserDomain();
                isInternetAccess = internetServiceConfigResult.getEnableInternetAccess();
                isLanAccess = internetServiceConfigResult.getEnableLAN();
                aoSpaceAccessBean = new AOSpaceAccessBean();
                aoSpaceAccessBean.setLanAccess(isLanAccess);
                aoSpaceAccessBean.setP2PAccess(internetServiceConfigResult.getEnableP2P());
                aoSpaceAccessBean.setInternetAccess(isInternetAccess);
                aoSpaceAccessBean.setPlatformApiBase(internetServiceConfigResult.getPlatformApiBase());
            }
            String ipAddressUrl = null;
            if (connectedNetworks != null) {
                for (ConnectedNetwork connectedNetwork : connectedNetworks) {
                    if (connectedNetwork != null) {
                        ipAddressUrl = connectedNetwork.generateIpAddressUrl();
                        if (ipAddressUrl != null) {
                            break;
                        }
                    }
                }
            }
            String userDomainValue = null;
            if (userDomain != null) {
                userDomainValue = FormatUtil.generateHttpUrlString(userDomain);
            }
            String ipAddressUrlValue = null;
            if (ipAddressUrl != null) {
                ipAddressUrlValue = FormatUtil.generateHttpUrlString(ipAddressUrl);
            }
            if (aoSpaceAccessBean != null && FormatUtil.isHttpUrlString(userDomainValue)) {
                aoSpaceAccessBean.setUserDomain(userDomain);
            }
            if (aoSpaceAccessBean != null && FormatUtil.isHttpUrlString(ipAddressUrlValue)) {
                aoSpaceAccessBean.setIpAddressUrl(ipAddressUrl);
            }
            updateBoxDomain(context, boxUuid, boxBind, isInternetAccess, isLanAccess, userDomain, ipAddressUrl);
            if (aoSpaceAccessBean != null) {
                EulixBoxInfo eulixBoxInfo = getSpecificBoxInfo(context, boxUuid, boxBind);
                if (eulixBoxInfo != null) {
                    boolean isUpdate = true;
                    boolean isHandle = false;
                    JSONObject jsonObject = null;
                    EulixSpaceDBBoxManager eulixSpaceDBBoxManager = EulixSpaceDBBoxManager.getInstance(boxUuid, boxBind);
                    if (eulixSpaceDBBoxManager != null) {
                        isHandle = true;
                        jsonObject = new JSONObject();
                        try {
                            jsonObject.put("aoSpaceAccessBean", new Gson().toJson(aoSpaceAccessBean, AOSpaceAccessBean.class));
                        } catch (JSONException e) {
                            e.printStackTrace();
                            isHandle = false;
                        }
                    }
                    boolean isFinish = true;
                    if (isHandle) {
                        Boolean finalIsInternetAccess = isInternetAccess;
                        Boolean finalIsLanAccess = isLanAccess;
                        String finalUserDomain = userDomain;
                        String finalIpAddressUrl = ipAddressUrl;
                        int result = eulixSpaceDBBoxManager.updateBoxInfo(jsonObject, isUpdate1 -> updateBoxDomain(context
                                , boxUuid, boxBind, finalIsInternetAccess, finalIsLanAccess, finalUserDomain, finalIpAddressUrl));
                        isFinish = (result >= 0);
                    } else {
                        isUpdate = (!AOSpaceAccessBean.compare(aoSpaceAccessBean, eulixBoxInfo.getAoSpaceAccessBean()));
                        if (isUpdate) {
                            eulixBoxInfo.setAoSpaceAccessBean(aoSpaceAccessBean);
                            Map<String, String> boxValue = new HashMap<>();
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                            boxValue.put(EulixSpaceDBManager.FIELD_BOX_INFO, new Gson().toJson(eulixBoxInfo, EulixBoxInfo.class));
                            updateBox(context, boxValue);
                        }
                    }
                    if (isFinish) {
                        updateBoxDomain(context, boxUuid, boxBind, isInternetAccess, isLanAccess, userDomain, ipAddressUrl);
                    }
                }
            }
        }
    }

    public static boolean checkDomainLegal(String domain) {
        return (domain != null && FormatUtil.isHttpUrlString(FormatUtil.generateHttpUrlString(domain)));
    }

    private static void updateBoxDomain(Context context, String boxUuid, String boxBind, Boolean isInternetAccess, Boolean isLanAccess, String userDomain, String ipAddressUrl) {
        if (isInternetAccess != null) {
            if (isInternetAccess) {
                if (checkDomainLegal(userDomain)) {
                    updateBoxDomain(context, boxUuid, boxBind, userDomain);
                    return;
                }
                AOSpaceAccessBean aoSpaceAccessBean = getSpecificAOSpaceBean(context, boxUuid, boxBind);
                if (aoSpaceAccessBean != null) {
                    userDomain = aoSpaceAccessBean.getUserDomain();
                    if (checkDomainLegal(userDomain)) {
                        updateBoxDomain(context, boxUuid, boxBind, userDomain);
                        return;
                    }
                }
                Map<String, UserInfo> userInfoMap = getSpecificUserInfo(context, boxUuid, boxBind);
                if (userInfoMap != null) {
                    Set<Map.Entry<String, UserInfo>> entrySet = userInfoMap.entrySet();
                    for (Map.Entry<String, UserInfo> entry : entrySet) {
                        if (entry != null) {
                            String uuid = entry.getKey();
                            UserInfo userInfo = entry.getValue();
                            if ("1".equals(boxBind) || "-1".equals(boxBind)) {
                                String clientUuid = DataUtil.getClientUuid(context);
                                if (uuid != null && userInfo != null && uuid.equals(clientUuid)) {
                                    userDomain = userInfo.getUserDomain();
                                    break;
                                }
                            } else {
                                if (uuid != null && userInfo != null) {
                                    String userId = userInfo.getUserId();
                                    if (userId != null && userId.equals(boxBind)) {
                                        userDomain = userInfo.getUserDomain();
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                if (userDomain != null && FormatUtil.isHttpUrlString(FormatUtil.generateHttpUrlString(userDomain))) {
                    updateBoxDomain(context, boxUuid, boxBind, userDomain);
                }
            } else if (isLanAccess != null && isLanAccess) {
                if (checkDomainLegal(ipAddressUrl)) {
                    updateBoxDomain(context, boxUuid, boxBind, ipAddressUrl);
                    return;
                }
                AOSpaceAccessBean aoSpaceAccessBean = getSpecificAOSpaceBean(context, boxUuid, boxBind);
                if (aoSpaceAccessBean != null) {
                    ipAddressUrl = aoSpaceAccessBean.getIpAddressUrl();
                    if (checkDomainLegal(ipAddressUrl)) {
                        updateBoxDomain(context, boxUuid, boxBind, ipAddressUrl);
                    }
                }
            }
        }
    }

    public static SecurityEmailInfo getActiveSecurityEmailInfo(@NonNull Context context) {
        SecurityEmailInfo securityEmailInfo = null;
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context
                , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (boxValues == null || boxValues.size() <= 0) {
            boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
        }
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_INFO)) {
                    String boxInfoValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_INFO);
                    if (boxInfoValue != null) {
                        EulixBoxInfo boxInfo = null;
                        try {
                            boxInfo = new Gson().fromJson(boxInfoValue, EulixBoxInfo.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                        if (boxInfo != null) {
                            securityEmailInfo = boxInfo.getSecurityEmailInfo();
                        }
                    }
                    break;
                }
            }
        }
        return securityEmailInfo;
    }

    /**
     * 获取使用空间的其它信息
     * @param context
     * @return
     */
    public static EulixBoxOtherInfo getActiveBoxOtherInfo(@NonNull Context context) {
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (boxValues == null || boxValues.size() <= 0) {
            boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
        }
        String boxUuid = null;
        String boxBind = null;
        if (boxValues != null) {
            for (Map<String, String> boxV : boxValues) {
                if (boxV != null && boxV.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                        && boxV.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                    boxUuid = boxV.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                    boxBind = boxV.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    break;
                }
            }
        }
        return getBoxOtherInfo(context, boxUuid, boxBind);
    }

    private static EulixBoxInfo getBoxInfo(List<Map<String, String>> boxValues) {
        EulixBoxInfo eulixBoxInfo = null;
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_INFO)) {
                    String boxInfoValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_INFO);
                    if (boxInfoValue != null) {
                        try {
                            eulixBoxInfo = new Gson().fromJson(boxInfoValue, EulixBoxInfo.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                    break;
                }
            }
        }
        return eulixBoxInfo;
    }

    public static DeviceAbility getActiveDeviceAbility(@NonNull Context context, boolean isGenerateDefault) {
        DeviceAbility deviceAbility = null;
        EulixBoxInfo eulixBoxInfo = getActiveBoxInfo(context);
        if (eulixBoxInfo != null) {
            deviceAbility = eulixBoxInfo.getDeviceAbility();
        }
        return (isGenerateDefault ? DeviceAbility.generateDefault(deviceAbility) : deviceAbility);
    }

    public static EulixBoxInfo getActiveBoxInfo(@NonNull Context context) {
        EulixBoxInfo eulixBoxInfo = null;
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                , String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (boxValues == null || boxValues.size() <= 0) {
            boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
        }
        eulixBoxInfo = getBoxInfo(boxValues);
        return eulixBoxInfo;
    }

    public static EulixBoxInfo getSpecificBoxInfo(@NonNull Context context, String boxUuid, String boxBind) {
        EulixBoxInfo eulixBoxInfo = null;
        if (boxUuid != null && boxBind != null) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            eulixBoxInfo = getBoxInfo(queryBox(context, queryMap));
        }
        return eulixBoxInfo;
    }

    public static EulixBoxToken getSpecificBoxToken(@NonNull Context context, String boxUuid, String boxBind) {
        EulixBoxToken eulixBoxToken = null;
        if (boxUuid != null && boxBind != null) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            List<Map<String, String>> boxValues = queryBox(context, queryMap);
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
                        String boxTokenValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                        if (boxTokenValue != null) {
                            try {
                                eulixBoxToken = new Gson().fromJson(boxTokenValue, EulixBoxToken.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }
                }
            }
        }
        return eulixBoxToken;
    }

    /**
     * 获取指定空间的其它信息
     * @param context
     * @param boxUuid
     * @param boxBind
     * @return
     */
    public static EulixBoxOtherInfo getBoxOtherInfo(@NonNull Context context, String boxUuid, String boxBind) {
        EulixBoxOtherInfo eulixBoxOtherInfo = null;
        if (boxUuid != null && boxBind != null) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            List<Map<String, String>> boxValues = queryBox(context, queryMap);
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_OTHER_INFO)) {
                        String otherInfoValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_OTHER_INFO);
                        if (otherInfoValue != null) {
                            try {
                                eulixBoxOtherInfo = new Gson().fromJson(otherInfoValue, EulixBoxOtherInfo.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }
                }
            }
        }
        return eulixBoxOtherInfo;
    }

    public static EulixBoxBaseInfoCompatible getActiveBoxBaseInfoCompatible(Context context) {
        EulixBoxBaseInfoCompatible eulixBoxBaseInfoCompatible = null;
        boolean isOnline = true;
        List<Map<String, String>> boxValues = queryBox(context
                , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (boxValues == null || boxValues.size() <= 0) {
            isOnline = false;
            boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
        }
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                        && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)
                        && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_DOMAIN)) {
                    eulixBoxBaseInfoCompatible = new EulixBoxBaseInfoCompatible();
                    eulixBoxBaseInfoCompatible.setBoxUuid(boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID));
                    eulixBoxBaseInfoCompatible.setBoxBind(boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND));
                    eulixBoxBaseInfoCompatible.setBoxDomain(boxValue.get(EulixSpaceDBManager.FIELD_BOX_DOMAIN));
                    break;
                }
            }
        }
        if (eulixBoxBaseInfoCompatible != null) {
            eulixBoxBaseInfoCompatible.setSpaceState(isOnline ? 1 : -1);
        }
        return eulixBoxBaseInfoCompatible;
    }

    public static String getBoxDomain(Context context, String boxUuid, String boxBind) {
        String boxDomain = null;
        List<Map<String, String>> boxValues = querySpecificBox(context, boxUuid, boxBind);
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_DOMAIN)) {
                    boxDomain = boxValue.get(EulixSpaceDBManager.FIELD_BOX_DOMAIN);
                    break;
                }
            }
        }
        return boxDomain;
    }

    //获取当前盒子基本信息
    public static EulixBoxBaseInfo getActiveBoxBaseInfo(Context context) {
        return getActiveBoxBaseInfo(context, true);
    }

    //获取当前盒子基本信息
    public static EulixBoxBaseInfo getActiveBoxBaseInfo(Context context, boolean isSupportOffline) {
        EulixBoxBaseInfo eulixBoxBaseInfo = null;
        List<Map<String, String>> boxValues = queryBox(context
                , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (isSupportOffline && (boxValues == null || boxValues.size() <= 0)) {
            boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
        }
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                        && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)
                        && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_DOMAIN)) {
                    eulixBoxBaseInfo = new EulixBoxBaseInfo();
                    eulixBoxBaseInfo.setBoxUuid(boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID));
                    eulixBoxBaseInfo.setBoxBind(boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND));
                    eulixBoxBaseInfo.setBoxDomain(boxValue.get(EulixSpaceDBManager.FIELD_BOX_DOMAIN));
                    break;
                }
            }
        }
        return eulixBoxBaseInfo;
    }

    public static EulixBoxBaseInfo getBoxSpaceWithAccessToken(Context context, String accessToken) {
        EulixBoxBaseInfo baseInfo = null;
        if (accessToken != null && context != null) {
            List<Map<String, String>> boxValues = queryBox(context);
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                            && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)
                            && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
                        String tokenValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                        EulixBoxToken eulixBoxToken = null;
                        if (tokenValue != null) {
                            try {
                                eulixBoxToken = new Gson().fromJson(tokenValue, EulixBoxToken.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        if (eulixBoxToken != null && accessToken.equals(eulixBoxToken.getAccessToken())) {
                            baseInfo = new EulixBoxBaseInfo();
                            String boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                            String boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                            baseInfo.setBoxUuid(boxUuid);
                            baseInfo.setBoxBind(boxBind);
                            if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_DOMAIN)) {
                                baseInfo.setBoxDomain(boxValue.get(EulixSpaceDBManager.FIELD_BOX_DOMAIN));
                            }
                            break;
                        }
                    }
                }
            }
        }
        return baseInfo;
    }


    public static void insertPush(Context context, Map<String, String> boxValue) {
        if (context != null && boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID)) {
            ContentResolver resolver = context.getContentResolver();
            if (resolver != null) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID, boxValue.get(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID));
                contentValues.put(EulixSpaceDBManager.FIELD_PUSH_UUID, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_PUSH_UUID, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_PUSH_BIND, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_PUSH_BIND, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_PUSH_TYPE, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_PUSH_TYPE, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_PUSH_PRIORITY, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_PUSH_PRIORITY, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_PUSH_SOURCE, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_PUSH_SOURCE, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_PUSH_CONSUME, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_PUSH_CONSUME, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_PUSH_TITLE, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_PUSH_TITLE, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_PUSH_CONTENT, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_PUSH_CONTENT, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_PUSH_RAW_DATA, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_PUSH_CREATE_TIME, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_PUSH_CREATE_TIME, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_PUSH_TIMESTAMP, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_PUSH_TIMESTAMP, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_PUSH_RESERVE, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_PUSH_RESERVE, ""));
                resolver.insert(EulixSpaceDBManager.PUSH_URI, contentValues);
            }
        }
    }

    private static void deletePush(Context context) {
        if (context != null) {
            ContentResolver resolver = context.getContentResolver();
            if (resolver != null) {
                resolver.delete(EulixSpaceDBManager.PUSH_URI, null, null);
            }
        }
    }

    public static void deletePush(Context context, Map<String, String> boxValue) {
        if (context != null && boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID)) {
            String messageId = boxValue.get(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID);
            String boxUuid = null;
            String bindValue = null;
            if (boxValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_UUID)) {
                boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_PUSH_UUID);
            }
            if (boxValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_BIND)) {
                bindValue = boxValue.get(EulixSpaceDBManager.FIELD_PUSH_BIND);
            }
            if (messageId != null) {
                ContentResolver resolver = context.getContentResolver();
                String selection;
                String[] selectionArgs;
                if (boxUuid == null || bindValue == null) {
                    selection = EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID + "= ?";
                    selectionArgs = new String[]{messageId};
                } else {
                    selection = EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID + "= ? AND " + EulixSpaceDBManager.FIELD_PUSH_UUID + "= ? AND " + EulixSpaceDBManager.FIELD_PUSH_BIND + "= ?";
                    selectionArgs = new String[]{messageId, boxUuid, bindValue};
                }
                if (resolver != null) {
                    resolver.delete(EulixSpaceDBManager.PUSH_URI, selection, selectionArgs);
                }
            }
        }
    }

    public static void deleteAppointPush(Context context, String boxUuid, String boxBind) {
        if (context != null && boxUuid != null && boxBind != null) {
            ContentResolver resolver = context.getContentResolver();
            String selection;
            String[] selectionArgs;
            selection = EulixSpaceDBManager.FIELD_PUSH_UUID + "= ? AND " + EulixSpaceDBManager.FIELD_PUSH_BIND
                    + "= ? AND " + EulixSpaceDBManager.FIELD_PUSH_SOURCE + " IN (\"1\", \"2\", \"3\")";
            selectionArgs = new String[]{boxUuid, boxBind};
            if (resolver != null) {
                resolver.delete(EulixSpaceDBManager.PUSH_URI, selection, selectionArgs);
            }
        }
    }

    public static void readAppointPush(Context context, String boxUuid, String boxBind, boolean isReset) {
        if (context != null && boxUuid != null && boxBind != null) {
            ContentResolver resolver = context.getContentResolver();
            String selection;
            String[] selectionArgs;
            selection = EulixSpaceDBManager.FIELD_PUSH_UUID + "= ? AND " + EulixSpaceDBManager.FIELD_PUSH_BIND
                    + "= ? AND " + EulixSpaceDBManager.FIELD_PUSH_SOURCE + " IN (\"1\", \"2\", \"3\")";
            if (isReset) {
                selection = selection + " AND " + EulixSpaceDBManager.FIELD_PUSH_CONSUME + " IN (\"1\", \"2\", \"3\")";
            } else {
                selection = selection + " AND " + EulixSpaceDBManager.FIELD_PUSH_CONSUME + "= \"3\"";
            }
            selectionArgs = new String[]{boxUuid, boxBind};
            if (resolver != null) {
                int size = 0;
                Cursor cursor = resolver.query(EulixSpaceDBManager.PUSH_URI, null, selection, selectionArgs, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        size += 1;
                    }
                    cursor.close();
                }
                if (size > 0) {
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(EulixSpaceDBManager.FIELD_PUSH_UUID, boxUuid);
                    contentValues.put(EulixSpaceDBManager.FIELD_PUSH_BIND, boxBind);
                    contentValues.put(EulixSpaceDBManager.FIELD_PUSH_CONSUME, "4");
                    resolver.update(EulixSpaceDBManager.PUSH_URI, contentValues, selection, selectionArgs);
                }
            }
        }
    }

    public static List<Map<String, String>> queryAppointTypePush(Context context, String boxUuid, String boxBind, String messageType) {
        List<Map<String, String>> pushValues = null;
        if (context != null && boxUuid != null && boxBind != null && messageType != null) {
            ContentResolver resolver = context.getContentResolver();
            String selection = EulixSpaceDBManager.FIELD_PUSH_UUID + "= ? AND " + EulixSpaceDBManager.FIELD_PUSH_BIND
                    + "= ? AND " + EulixSpaceDBManager.FIELD_PUSH_TYPE + "= ? AND "
                    + EulixSpaceDBManager.FIELD_PUSH_SOURCE + " IN (\"0\", \"1\", \"2\", \"3\")"
                    + " AND " + EulixSpaceDBManager.FIELD_PUSH_CONSUME + " IN (\"1\", \"2\")";
            String[] selectionArgs = new String[]{boxUuid, boxBind, messageType};
            if (resolver != null) {
                Cursor cursor = resolver.query(EulixSpaceDBManager.PUSH_URI, null
                        , selection, selectionArgs, null);
                if (cursor != null) {
                    pushValues = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        Map<String, String> boxValue = new HashMap<>();
                        int messageIdIndex, uuidIndex, bindIndex, typeIndex, priorityIndex, sourceIndex
                                , consumeIndex, titleIndex, contentIndex, rawDataIndex, createTimeIndex
                                , timestampIndex, reserveIndex;
                        if ((messageIdIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID, cursor.getString(messageIdIndex));
                        }
                        if ((uuidIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_UUID)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_UUID, cursor.getString(uuidIndex));
                        }
                        if ((bindIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_BIND)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_BIND, cursor.getString(bindIndex));
                        }
                        if ((typeIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_TYPE)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_TYPE, cursor.getString(typeIndex));
                        }
                        if ((priorityIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_PRIORITY)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_PRIORITY, cursor.getString(priorityIndex));
                        }
                        if ((sourceIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_SOURCE)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_SOURCE, cursor.getString(sourceIndex));
                        }
                        if ((consumeIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_CONSUME)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_CONSUME, cursor.getString(consumeIndex));
                        }
                        if ((titleIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_TITLE)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_TITLE, cursor.getString(titleIndex));
                        }
                        if ((contentIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_CONTENT)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_CONTENT, cursor.getString(contentIndex));
                        }
                        if ((rawDataIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA, cursor.getString(rawDataIndex));
                        }
                        if ((createTimeIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_CREATE_TIME)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_CREATE_TIME, cursor.getString(createTimeIndex));
                        }
                        if ((timestampIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_TIMESTAMP)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_TIMESTAMP, cursor.getString(timestampIndex));
                        }
                        if ((reserveIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_RESERVE)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_RESERVE, cursor.getString(reserveIndex));
                        }
                        pushValues.add(boxValue);
                    }
                    cursor.close();
                }
            }
        }
        return pushValues;
    }

    public static void updatePush(Context context, Map<String, String> boxValue) {
        if (context != null && boxValue != null) {
            String messageId = null;
            String boxUuid = null;
            String bindValue = null;
            if (boxValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID)) {
                messageId = boxValue.get(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID);
            }
            if (boxValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_UUID)) {
                boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_PUSH_UUID);
            }
            if (boxValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_BIND)) {
                bindValue = boxValue.get(EulixSpaceDBManager.FIELD_PUSH_BIND);
            }
            if (messageId != null || (boxUuid != null && bindValue != null)) {
                ContentResolver resolver = context.getContentResolver();
                String selection;
                String[] selectionArgs;
                if (messageId == null) {
                    selection = EulixSpaceDBManager.FIELD_PUSH_UUID + "= ? AND " + EulixSpaceDBManager.FIELD_PUSH_BIND + "= ?";
                    selectionArgs = new String[]{boxUuid, bindValue};
                } else {
                    if (boxUuid == null || bindValue == null) {
                        selection = EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID + "= ?";
                        selectionArgs = new String[]{messageId};
                    } else {
                        selection = EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID + "= ? AND " + EulixSpaceDBManager.FIELD_PUSH_UUID + "= ? AND " + EulixSpaceDBManager.FIELD_PUSH_BIND + "= ?";
                        selectionArgs = new String[]{messageId, boxUuid, bindValue};
                    }
                }
                if (resolver != null) {
                    ContentValues contentValues = new ContentValues();
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID, boxValue.get(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_UUID)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_PUSH_UUID, boxValue.get(EulixSpaceDBManager.FIELD_PUSH_UUID));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_BIND)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_PUSH_BIND, boxValue.get(EulixSpaceDBManager.FIELD_PUSH_BIND));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_TYPE)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_PUSH_TYPE, boxValue.get(EulixSpaceDBManager.FIELD_PUSH_TYPE));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_PRIORITY)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_PUSH_PRIORITY, boxValue.get(EulixSpaceDBManager.FIELD_PUSH_PRIORITY));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_SOURCE)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_PUSH_SOURCE, boxValue.get(EulixSpaceDBManager.FIELD_PUSH_SOURCE));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_CONSUME)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_PUSH_CONSUME, boxValue.get(EulixSpaceDBManager.FIELD_PUSH_CONSUME));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_TITLE)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_PUSH_TITLE, boxValue.get(EulixSpaceDBManager.FIELD_PUSH_TITLE));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_CONTENT)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_PUSH_CONTENT, boxValue.get(EulixSpaceDBManager.FIELD_PUSH_CONTENT));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA, boxValue.get(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_CREATE_TIME)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_PUSH_CREATE_TIME, boxValue.get(EulixSpaceDBManager.FIELD_PUSH_CREATE_TIME));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_TIMESTAMP)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_PUSH_TIMESTAMP, boxValue.get(EulixSpaceDBManager.FIELD_PUSH_TIMESTAMP));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_RESERVE)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_PUSH_RESERVE, boxValue.get(EulixSpaceDBManager.FIELD_PUSH_RESERVE));
                    }
                    resolver.update(EulixSpaceDBManager.PUSH_URI, contentValues, selection, selectionArgs);
                }
            }
        }
    }

    public static List<Map<String, String>> queryPush(Context context) {
        return queryPush(context, null, null);
    }

    /**
     * 根据指定栏目内容查询数据
     * @param context
     * @param columnName 栏目名
     * @param columnValue 栏目属性值
     * @return
     */
    public static List<Map<String, String>> queryPush(Context context, String columnName, String columnValue) {
        List<Map<String, String>> boxValues = null;
        if (context != null) {
            ContentResolver resolver = context.getContentResolver();
            if (resolver != null) {
                Cursor cursor = resolver.query(EulixSpaceDBManager.PUSH_URI, null
                        , (columnName == null ? null : columnName + "= ?")
                        , ((columnName == null || columnValue == null) ? null : new String[]{columnValue}), null);
                if (cursor != null) {
                    boxValues = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        Map<String, String> boxValue = new HashMap<>();
                        int messageIdIndex, uuidIndex, bindIndex, typeIndex, priorityIndex, sourceIndex
                                , consumeIndex, titleIndex, contentIndex, rawDataIndex, createTimeIndex
                                , timestampIndex, reserveIndex;
                        if ((messageIdIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID, cursor.getString(messageIdIndex));
                        }
                        if ((uuidIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_UUID)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_UUID, cursor.getString(uuidIndex));
                        }
                        if ((bindIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_BIND)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_BIND, cursor.getString(bindIndex));
                        }
                        if ((typeIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_TYPE)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_TYPE, cursor.getString(typeIndex));
                        }
                        if ((priorityIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_PRIORITY)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_PRIORITY, cursor.getString(priorityIndex));
                        }
                        if ((sourceIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_SOURCE)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_SOURCE, cursor.getString(sourceIndex));
                        }
                        if ((consumeIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_CONSUME)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_CONSUME, cursor.getString(consumeIndex));
                        }
                        if ((titleIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_TITLE)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_TITLE, cursor.getString(titleIndex));
                        }
                        if ((contentIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_CONTENT)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_CONTENT, cursor.getString(contentIndex));
                        }
                        if ((rawDataIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA, cursor.getString(rawDataIndex));
                        }
                        if ((createTimeIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_CREATE_TIME)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_CREATE_TIME, cursor.getString(createTimeIndex));
                        }
                        if ((timestampIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_TIMESTAMP)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_TIMESTAMP, cursor.getString(timestampIndex));
                        }
                        if ((reserveIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_RESERVE)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_RESERVE, cursor.getString(reserveIndex));
                        }
                        boxValues.add(boxValue);
                    }
                    cursor.close();
                }
            }
        }
        return boxValues;
    }

    /**
     * 根据若干栏目查询数据
     * @param context
     * @param columnMap 栏目名和属性值的表
     * @return
     */
    public static List<Map<String, String>> queryPush(Context context, Map<String, String> columnMap) {
        List<Map<String, String>> boxValues = null;
        StringBuilder selectionBuilder = null;
        String[] selectionArgs = null;
        if (context != null) {
            if (columnMap != null) {
                Set<Map.Entry<String, String>> entrySet = columnMap.entrySet();
                int size = entrySet.size();
                if (size > 0) {
                    selectionArgs = new String[size];
                    int index = 0;
                    for (Map.Entry<String, String> entry : entrySet) {
                        if (entry != null) {
                            String key = entry.getKey();
                            String value = entry.getValue();
                            if (key != null && value != null) {
                                if (selectionBuilder == null) {
                                    selectionBuilder = new StringBuilder((key + "= ?"));
                                } else {
                                    selectionBuilder.append(" AND ");
                                    selectionBuilder.append(key);
                                    selectionBuilder.append("= ?");
                                }
                                selectionArgs[index] = value;
                                index += 1;
                            }
                        }
                    }
                }
            }
            ContentResolver resolver = context.getContentResolver();
            if (resolver != null) {
                Cursor cursor = resolver.query(EulixSpaceDBManager.PUSH_URI, null
                        , (selectionBuilder == null ? null : selectionBuilder.toString())
                        , selectionArgs, null);
                if (cursor != null) {
                    boxValues = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        Map<String, String> boxValue = new HashMap<>();
                        int messageIdIndex, uuidIndex, bindIndex, typeIndex, priorityIndex, sourceIndex
                                , consumeIndex, titleIndex, contentIndex, rawDataIndex, createTimeIndex
                                , timestampIndex, reserveIndex;
                        if ((messageIdIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_MESSAGE_ID, cursor.getString(messageIdIndex));
                        }
                        if ((uuidIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_UUID)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_UUID, cursor.getString(uuidIndex));
                        }
                        if ((bindIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_BIND)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_BIND, cursor.getString(bindIndex));
                        }
                        if ((typeIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_TYPE)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_TYPE, cursor.getString(typeIndex));
                        }
                        if ((priorityIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_PRIORITY)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_PRIORITY, cursor.getString(priorityIndex));
                        }
                        if ((sourceIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_SOURCE)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_SOURCE, cursor.getString(sourceIndex));
                        }
                        if ((consumeIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_CONSUME)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_CONSUME, cursor.getString(consumeIndex));
                        }
                        if ((titleIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_TITLE)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_TITLE, cursor.getString(titleIndex));
                        }
                        if ((contentIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_CONTENT)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_CONTENT, cursor.getString(contentIndex));
                        }
                        if ((rawDataIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA, cursor.getString(rawDataIndex));
                        }
                        if ((createTimeIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_CREATE_TIME)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_CREATE_TIME, cursor.getString(createTimeIndex));
                        }
                        if ((timestampIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_TIMESTAMP)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_TIMESTAMP, cursor.getString(timestampIndex));
                        }
                        if ((reserveIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_PUSH_RESERVE)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_PUSH_RESERVE, cursor.getString(reserveIndex));
                        }
                        boxValues.add(boxValue);
                    }
                    cursor.close();
                }
            }
        }
        return boxValues;
    }

    public static List<GetNotificationResult> generateNotificationResultListItems(Context context, String boxUuid, String boxBind) {
        List<GetNotificationResult> getNotificationResults = null;
        if (context != null && boxUuid != null && boxBind != null) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_PUSH_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_PUSH_BIND, boxBind);
            queryMap.put(EulixSpaceDBManager.FIELD_PUSH_SOURCE, "-1");
            List<Map<String, String>> pushValues = EulixSpaceDBUtil.queryPush(context, queryMap);
            if (pushValues != null) {
                for (Map<String, String> pushValue : pushValues) {
                    if (pushValue != null && pushValue.containsKey(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA)) {
                        String dataValue = pushValue.get(EulixSpaceDBManager.FIELD_PUSH_RAW_DATA);
                        if (dataValue != null) {
                            try {
                                getNotificationResults = new Gson().fromJson(dataValue, new TypeToken<List<GetNotificationResult>>() {}.getType());
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }
                }
            }
        }
        return getNotificationResults;
    }


    public static void insertDID(Context context, Map<String, String> boxValue) {
        if (context != null && boxValue != null) {
            ContentResolver resolver = context.getContentResolver();
            if (resolver != null) {
                ContentValues contentValues = new ContentValues();
                contentValues.put(EulixSpaceDBManager.FIELD_DID_UUID, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_DID_UUID, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_DID_BIND, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_DID_BIND, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_DID_AO_ID, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_DID_AO_ID, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_DID_DOC_ENCODE, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_DID_DOC_ENCODE, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_DID_DOCUMENT, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_DID_DOCUMENT, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_DID_CREDENTIAL, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_DID_CREDENTIAL, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_DID_TIMESTAMP, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_DID_TIMESTAMP, ""));
                contentValues.put(EulixSpaceDBManager.FIELD_DID_RESERVE, getStringValueFromMap(boxValue
                        , EulixSpaceDBManager.FIELD_DID_RESERVE, ""));
                resolver.insert(EulixSpaceDBManager.DID_URI, contentValues);
            }
        }
    }

    private static void deleteDID(Context context) {
        if (context != null) {
            ContentResolver resolver = context.getContentResolver();
            if (resolver != null) {
                resolver.delete(EulixSpaceDBManager.DID_URI, null, null);
            }
        }
    }

    public static void deleteDID(Context context, Map<String, String> boxValue) {
        if (context != null && boxValue != null) {
            String boxUuid = null;
            String boxBind = null;
            if (boxValue.containsKey(EulixSpaceDBManager.FIELD_DID_UUID)) {
                boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_DID_UUID);
            }
            if (boxValue.containsKey(EulixSpaceDBManager.FIELD_DID_BIND)) {
                boxBind = boxValue.get(EulixSpaceDBManager.FIELD_DID_BIND);
            }
            if (boxUuid != null && boxBind == null) {
                String selection = EulixSpaceDBManager.FIELD_DID_UUID + "= ? AND " + EulixSpaceDBManager.FIELD_DID_BIND + "= ?";
                String[] selectionArgs = new String[]{boxUuid, boxBind};
                ContentResolver resolver = context.getContentResolver();
                if (resolver != null) {
                    resolver.delete(EulixSpaceDBManager.DID_URI, selection, selectionArgs);
                }
            }
        }
    }

    public static void updateDID(Context context, Map<String, String> boxValue) {
        if (context != null && boxValue != null) {
            String boxUuid = null;
            String boxBind = null;
            if (boxValue.containsKey(EulixSpaceDBManager.FIELD_DID_UUID)) {
                boxUuid = boxValue.get(EulixSpaceDBManager.FIELD_DID_UUID);
            }
            if (boxValue.containsKey(EulixSpaceDBManager.FIELD_DID_BIND)) {
                boxBind = boxValue.get(EulixSpaceDBManager.FIELD_DID_BIND);
            }
            if (boxUuid != null && boxBind != null) {
                String selection = EulixSpaceDBManager.FIELD_DID_UUID + "= ? AND " + EulixSpaceDBManager.FIELD_DID_BIND + "= ?";
                String[] selectionArgs = new String[]{boxUuid, boxBind};
                ContentResolver resolver = context.getContentResolver();
                if (resolver != null) {
                    ContentValues contentValues = new ContentValues();
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_DID_UUID)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_DID_UUID, boxValue.get(EulixSpaceDBManager.FIELD_DID_UUID));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_DID_BIND)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_DID_BIND, boxValue.get(EulixSpaceDBManager.FIELD_DID_BIND));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_DID_AO_ID)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_DID_AO_ID, boxValue.get(EulixSpaceDBManager.FIELD_DID_AO_ID));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_DID_DOC_ENCODE)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_DID_DOC_ENCODE, boxValue.get(EulixSpaceDBManager.FIELD_DID_DOC_ENCODE));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_DID_DOCUMENT)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_DID_DOCUMENT, boxValue.get(EulixSpaceDBManager.FIELD_DID_DOCUMENT));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_DID_CREDENTIAL)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_DID_CREDENTIAL, boxValue.get(EulixSpaceDBManager.FIELD_DID_CREDENTIAL));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_DID_TIMESTAMP)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_DID_TIMESTAMP, boxValue.get(EulixSpaceDBManager.FIELD_DID_TIMESTAMP));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_DID_RESERVE)) {
                        contentValues.put(EulixSpaceDBManager.FIELD_DID_RESERVE, boxValue.get(EulixSpaceDBManager.FIELD_DID_RESERVE));
                    }
                    resolver.update(EulixSpaceDBManager.DID_URI, contentValues, selection, selectionArgs);
                }
            }
        }
    }

    public static List<Map<String, String>> queryDID(Context context) {
        return queryDID(context, null, null);
    }

    /**
     * 根据指定栏目内容查询数据
     * @param context
     * @param columnName 栏目名
     * @param columnValue 栏目属性值
     * @return
     */
    public static List<Map<String, String>> queryDID(Context context, String columnName, String columnValue) {
        List<Map<String, String>> boxValues = null;
        if (context != null) {
            ContentResolver resolver = context.getContentResolver();
            if (resolver != null) {
                Cursor cursor = resolver.query(EulixSpaceDBManager.DID_URI, null
                        , (columnName == null ? null : columnName + "= ?")
                        , ((columnName == null || columnValue == null) ? null : new String[]{columnValue}), null);
                if (cursor != null) {
                    boxValues = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        Map<String, String> boxValue = new HashMap<>();
                        int uuidIndex, bindIndex, aoIdIndex, docEncodeIndex, documentIndex
                                , credentialIndex, timestampIndex, reserveIndex;
                        if ((uuidIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_DID_UUID)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_DID_UUID, cursor.getString(uuidIndex));
                        }
                        if ((bindIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_DID_BIND)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_DID_BIND, cursor.getString(bindIndex));
                        }
                        if ((aoIdIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_DID_AO_ID)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_DID_AO_ID, cursor.getString(aoIdIndex));
                        }
                        if ((docEncodeIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_DID_DOC_ENCODE)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_DID_DOC_ENCODE, cursor.getString(docEncodeIndex));
                        }
                        if ((documentIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_DID_DOCUMENT)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_DID_DOCUMENT, cursor.getString(documentIndex));
                        }
                        if ((credentialIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_DID_CREDENTIAL)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_DID_CREDENTIAL, cursor.getString(credentialIndex));
                        }
                        if ((timestampIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_DID_TIMESTAMP)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_DID_TIMESTAMP, cursor.getString(timestampIndex));
                        }
                        if ((reserveIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_DID_RESERVE)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_DID_RESERVE, cursor.getString(reserveIndex));
                        }
                        boxValues.add(boxValue);
                    }
                    cursor.close();
                }
            }
        }
        return boxValues;
    }

    /**
     * 根据若干栏目查询数据
     * @param context
     * @param columnMap 栏目名和属性值的表
     * @return
     */
    public static List<Map<String, String>> queryDID(Context context, Map<String, String> columnMap) {
        List<Map<String, String>> boxValues = null;
        StringBuilder selectionBuilder = null;
        String[] selectionArgs = null;
        if (context != null) {
            if (columnMap != null) {
                Set<Map.Entry<String, String>> entrySet = columnMap.entrySet();
                int size = entrySet.size();
                if (size > 0) {
                    selectionArgs = new String[size];
                    int index = 0;
                    for (Map.Entry<String, String> entry : entrySet) {
                        if (entry != null) {
                            String key = entry.getKey();
                            String value = entry.getValue();
                            if (key != null && value != null) {
                                if (selectionBuilder == null) {
                                    selectionBuilder = new StringBuilder((key + "= ?"));
                                } else {
                                    selectionBuilder.append(" AND ");
                                    selectionBuilder.append(key);
                                    selectionBuilder.append("= ?");
                                }
                                selectionArgs[index] = value;
                                index += 1;
                            }
                        }
                    }
                }
            }
            ContentResolver resolver = context.getContentResolver();
            if (resolver != null) {
                Cursor cursor = resolver.query(EulixSpaceDBManager.DID_URI, null
                        , (selectionBuilder == null ? null : selectionBuilder.toString())
                        , selectionArgs, null);
                if (cursor != null) {
                    boxValues = new ArrayList<>();
                    while (cursor.moveToNext()) {
                        Map<String, String> boxValue = new HashMap<>();
                        int uuidIndex, bindIndex, aoIdIndex, docEncodeIndex, documentIndex
                                , credentialIndex, timestampIndex, reserveIndex;
                        if ((uuidIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_DID_UUID)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_DID_UUID, cursor.getString(uuidIndex));
                        }
                        if ((bindIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_DID_BIND)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_DID_BIND, cursor.getString(bindIndex));
                        }
                        if ((aoIdIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_DID_AO_ID)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_DID_AO_ID, cursor.getString(aoIdIndex));
                        }
                        if ((docEncodeIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_DID_DOC_ENCODE)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_DID_DOC_ENCODE, cursor.getString(docEncodeIndex));
                        }
                        if ((documentIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_DID_DOCUMENT)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_DID_DOCUMENT, cursor.getString(documentIndex));
                        }
                        if ((credentialIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_DID_CREDENTIAL)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_DID_CREDENTIAL, cursor.getString(credentialIndex));
                        }
                        if ((timestampIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_DID_TIMESTAMP)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_DID_TIMESTAMP, cursor.getString(timestampIndex));
                        }
                        if ((reserveIndex = cursor.getColumnIndex(EulixSpaceDBManager.FIELD_DID_RESERVE)) >= 0) {
                            boxValue.put(EulixSpaceDBManager.FIELD_DID_RESERVE, cursor.getString(reserveIndex));
                        }
                        boxValues.add(boxValue);
                    }
                    cursor.close();
                }
            }
        }
        return boxValues;
    }

    public static DIDDocument getActiveDIDDocument(Context context) {
        DIDDocument didDocument = null;
        if (context != null) {
            EulixBoxBaseInfo eulixBoxBaseInfo = getActiveBoxBaseInfo(context);
            if (eulixBoxBaseInfo != null) {
                didDocument = getSpecificDIDDocument(context, eulixBoxBaseInfo.getBoxUuid(), eulixBoxBaseInfo.getBoxBind(), null);
            }
        }
        return didDocument;
    }

    public static DIDDocument getSpecificDIDDocument(Context context, String boxUuid, String boxBind, String aoId) {
        DIDDocument didDocument = null;
        if (context != null && boxUuid != null && (boxBind != null || aoId != null)) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_DID_UUID, boxUuid);
            if (aoId != null) {
                queryMap.put(EulixSpaceDBManager.FIELD_DID_AO_ID, aoId);
            } else {
                queryMap.put(EulixSpaceDBManager.FIELD_DID_BIND, boxBind);
            }
            List<Map<String, String>> boxValues = queryDID(context, queryMap);
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_DID_DOCUMENT)) {
                        String didDocumentValue = boxValue.get(EulixSpaceDBManager.FIELD_DID_DOCUMENT);
                        if (didDocumentValue != null) {
                            try {
                                didDocument = new Gson().fromJson(didDocumentValue, DIDDocument.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }
                }
            }
        }
        return didDocument;
    }

    public static DIDCredentialBean getSpecificDIDCredentialBean(Context context, String boxUuid, String boxBind, String aoId) {
        DIDCredentialBean didCredentialBean = null;
        if (context != null && boxUuid != null && (boxBind != null || aoId != null)) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_DID_UUID, boxUuid);
            if (aoId != null) {
                queryMap.put(EulixSpaceDBManager.FIELD_DID_AO_ID, aoId);
            } else {
                queryMap.put(EulixSpaceDBManager.FIELD_DID_BIND, boxBind);
            }
            List<Map<String, String>> boxValues = queryDID(context, queryMap);
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_DID_CREDENTIAL)) {
                        String didCredentialValue = boxValue.get(EulixSpaceDBManager.FIELD_DID_CREDENTIAL);
                        if (didCredentialValue != null) {
                            try {
                                didCredentialBean = new Gson().fromJson(didCredentialValue, DIDCredentialBean.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }
                }
            }
        }
        return didCredentialBean;
    }

    public static DIDReserveBean getActiveDIDReserveBean(Context context) {
        DIDReserveBean didReserveBean = null;
        if (context != null) {
            EulixBoxBaseInfo eulixBoxBaseInfo = getActiveBoxBaseInfo(context);
            if (eulixBoxBaseInfo != null) {
                didReserveBean = getSpecificDIDReserveBean(context, eulixBoxBaseInfo.getBoxUuid(), eulixBoxBaseInfo.getBoxBind(), null);
            }
        }
        return didReserveBean;
    }

    public static DIDReserveBean getSpecificDIDReserveBean(Context context, String boxUuid, String boxBind, String aoId) {
        DIDReserveBean didReserveBean = null;
        if (context != null && boxUuid != null && (boxBind != null || aoId != null)) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_DID_UUID, boxUuid);
            if (aoId != null) {
                queryMap.put(EulixSpaceDBManager.FIELD_DID_AO_ID, aoId);
            } else {
                queryMap.put(EulixSpaceDBManager.FIELD_DID_BIND, boxBind);
            }
            List<Map<String, String>> boxValues = queryDID(context, queryMap);
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_DID_RESERVE)) {
                        String didReserveValue = boxValue.get(EulixSpaceDBManager.FIELD_DID_RESERVE);
                        if (didReserveValue != null) {
                            try {
                                didReserveBean = new Gson().fromJson(didReserveValue, DIDReserveBean.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        break;
                    }
                }
            }
        }
        return didReserveBean;
    }
}
