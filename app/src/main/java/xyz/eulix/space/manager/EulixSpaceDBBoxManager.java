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

package xyz.eulix.space.manager;

import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.bean.AOSpaceAccessBean;
import xyz.eulix.space.bean.ApplicationLockInfo;
import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.bean.EulixBoxInfo;
import xyz.eulix.space.bean.EulixBoxOtherInfo;
import xyz.eulix.space.bean.SecurityEmailInfo;
import xyz.eulix.space.bean.SecurityPasswordInfo;
import xyz.eulix.space.bean.SpaceStatusStatusLineInfo;
import xyz.eulix.space.bean.TerminalInfo;
import xyz.eulix.space.bean.bind.InitResponseNetwork;
import xyz.eulix.space.bean.developer.SpacePlatformInfo;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.network.agent.disk.DiskManageListResult;
import xyz.eulix.space.network.disk.RaidInfoResult;
import xyz.eulix.space.util.StringUtil;

/**
 * @author: chenjiawei
 * Description: 用于规范往数据库里更新的操作
 * date: 2022/7/19 15:19
 */
public class EulixSpaceDBBoxManager {
    private static final String DB_BOX_REQUEST_ID = "db_box_request_id";
    private static final int UPDATE_BOX_INFO = 1;
    private static final int UPDATE_BOX_OTHER_INFO = UPDATE_BOX_INFO + 1;
    private static Map<String, EulixSpaceDBBoxManager> eulixSpaceDBBoxManagerMap = new HashMap<>();
    private String boxUuid;
    private String boxBind;
    private EulixSpaceDBBoxHandler mHandler;
    private Map<String, UpdateFinishCallback> updateFinishCallbackMap;
    private boolean lockEulixBoxInfo;
    private boolean lockEulixBoxOtherInfo;
    private Queue<JSONObject> eulixBoxInfoQueue;
    private Queue<JSONObject> eulixBoxOtherInfoQueue;

    public interface UpdateFinishCallback {
        void updateFinish(boolean isUpdate);
    }

    private EulixSpaceDBBoxManager(@NonNull String boxUuid, @NonNull String boxBind) {
        this.boxUuid = boxUuid;
        this.boxBind = boxBind;
        mHandler = new EulixSpaceDBBoxHandler(this);
        lockEulixBoxInfo = false;
        eulixBoxInfoQueue = new ConcurrentLinkedQueue<>();
        lockEulixBoxOtherInfo = false;
        eulixBoxOtherInfoQueue = new ConcurrentLinkedQueue<>();
        updateFinishCallbackMap = new HashMap<>();
    }

    public synchronized static EulixSpaceDBBoxManager getInstance(String boxUuid, String boxBind) {
        EulixSpaceDBBoxManager manager = null;
        if (boxUuid != null && boxBind != null) {
            String spaceId = (boxUuid + "_" + boxBind);
            if (eulixSpaceDBBoxManagerMap.containsKey(spaceId)) {
                manager = eulixSpaceDBBoxManagerMap.get(spaceId);
            }
            if (manager == null) {
                manager = new EulixSpaceDBBoxManager(boxUuid, boxBind);
                eulixSpaceDBBoxManagerMap.put(spaceId, manager);
            }
        }
        return manager;
    }

    static class EulixSpaceDBBoxHandler extends Handler {
        private WeakReference<EulixSpaceDBBoxManager> eulixSpaceDBBoxManagerWeakReference;

        public EulixSpaceDBBoxHandler(EulixSpaceDBBoxManager manager) {
            eulixSpaceDBBoxManagerWeakReference = new WeakReference<>(manager);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            EulixSpaceDBBoxManager manager = eulixSpaceDBBoxManagerWeakReference.get();
            if (manager == null) {
                super.handleMessage(msg);
            } else {
                switch (msg.what) {
                    case UPDATE_BOX_INFO:
                        if (manager.eulixBoxInfoQueue.isEmpty()) {
                            manager.lockEulixBoxInfo = false;
                        } else {
                            manager.handleUpdateBoxInfo(manager.eulixBoxInfoQueue.poll());
                        }
                        break;
                    case UPDATE_BOX_OTHER_INFO:
                        if (manager.eulixBoxOtherInfoQueue.isEmpty()) {
                            manager.lockEulixBoxOtherInfo = false;
                        } else {
                            manager.handleUpdateBoxOtherInfo(manager.eulixBoxOtherInfoQueue.poll());
                        }
                        break;
                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        }
    }

    /**
     * @param boxInfoObject
     * @param callback
     * @return -1：依赖Callback回复；0：当场回复，不更新；1：当场回复，更新
     */
    public synchronized int updateBoxInfo(JSONObject boxInfoObject, UpdateFinishCallback callback) {
        int result = 0;
        if (boxInfoObject != null) {
            if (lockEulixBoxInfo) {
                result = -1;
                if (callback != null) {
                    String requestId;
                    do {
                        requestId = UUID.randomUUID().toString();
                    } while (updateFinishCallbackMap.containsKey(requestId));
                    updateFinishCallbackMap.put(requestId, callback);
                    try {
                        boxInfoObject.put(DB_BOX_REQUEST_ID, requestId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                eulixBoxInfoQueue.offer(boxInfoObject);
                // 防止小概率被悬挂
                if (!lockEulixBoxInfo) {
                    lockEulixBoxInfo = true;
                    if (mHandler != null) {
                        mHandler.sendEmptyMessage(UPDATE_BOX_INFO);
                    }
                }
            } else {
                lockEulixBoxInfo = true;
                if (handleUpdateBoxInfo(boxInfoObject)) {
                    result = 1;
                }
            }
        }
        return result;
    }

    private boolean handleUpdateBoxInfo(JSONObject boxInfoObject) {
        boolean isUpdate = false;
        if (boxInfoObject != null) {
            EulixBoxInfo eulixBoxInfo = null;
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(EulixSpaceApplication.getContext(), queryMap);
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_INFO)) {
                        String boxInfoValue = StringUtil.nullToEmpty(boxValue.get(EulixSpaceDBManager.FIELD_BOX_INFO));
                        if (TextUtils.isEmpty(boxInfoValue.trim())) {
                            eulixBoxInfo = new EulixBoxInfo();
                        } else {
                            try {
                                eulixBoxInfo = new Gson().fromJson(boxInfoValue, EulixBoxInfo.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        if (eulixBoxInfo == null && !"1".equals(boxBind)) {
                            eulixBoxInfo = new EulixBoxInfo();
                        }
                        break;
                    }
                }
            }
            if (eulixBoxInfo != null) {
                if (boxInfoObject.has("totalSize")) {
                    long totalSize = boxInfoObject.optLong("totalSize", -1L);
                    if (totalSize >= 0 && totalSize != eulixBoxInfo.getTotalSize()) {
                        isUpdate = true;
                        eulixBoxInfo.setTotalSize(totalSize);
                    }
                }
                if (boxInfoObject.has("usedSize")) {
                    long usedSize = boxInfoObject.optLong("usedSize", -1L);
                    if (usedSize >= 0 && usedSize != eulixBoxInfo.getUsedSize()) {
                        isUpdate = true;
                        eulixBoxInfo.setUsedSize(usedSize);
                    }
                }
                if (boxInfoObject.has("networks")) {
                    boolean isHandleUpdate = false;
                    List<InitResponseNetwork> networks = null;
                    String networksValue = boxInfoObject.optString("networks");
                    if (!TextUtils.isEmpty(networksValue)) {
                        try {
                            networks = new Gson().fromJson(networksValue, new TypeToken<List<InitResponseNetwork>>(){}.getType());
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                        if (networks != null) {
                            isHandleUpdate = true;
                        }
                    } else {
                        isHandleUpdate = true;
                    }
                    if (isHandleUpdate) {
                        eulixBoxInfo.setNetworks(networks);
                        isUpdate = true;
                    }
                }
                if (boxInfoObject.has("isLAN")) {
                    boolean isLAN = boxInfoObject.optBoolean("isLAN");
                    if (isLAN != eulixBoxInfo.isLAN()) {
                        isUpdate = true;
                        eulixBoxInfo.setLAN(isLAN);
                    }
                }
                if (boxInfoObject.has("bluetoothAddress")) {
                    String bluetoothAddress = boxInfoObject.optString("bluetoothAddress");
                    if (!TextUtils.isEmpty(bluetoothAddress) && !bluetoothAddress.equals(eulixBoxInfo.getBluetoothAddress())) {
                        isUpdate = true;
                        eulixBoxInfo.setBluetoothAddress(bluetoothAddress);
                    }
                }
                if (boxInfoObject.has("bluetoothId")) {
                    String bluetoothId = boxInfoObject.optString("bluetoothId");
                    if (!TextUtils.isEmpty(bluetoothId) && !bluetoothId.equals(eulixBoxInfo.getBluetoothId())) {
                        isUpdate = true;
                        eulixBoxInfo.setBluetoothId(bluetoothId);
                    }
                }
                if (boxInfoObject.has("bluetoothDeviceName")) {
                    String bluetoothDeviceName = boxInfoObject.optString("bluetoothDeviceName");
                    if (!TextUtils.isEmpty(bluetoothDeviceName) && !bluetoothDeviceName.equals(eulixBoxInfo.getBluetoothDeviceName())) {
                        isUpdate = true;
                        eulixBoxInfo.setBluetoothDeviceName(bluetoothDeviceName);
                    }
                }
                if (boxInfoObject.has("spaceStatusStatusLineInfo")) {
                    boolean isHandleUpdate = false;
                    SpaceStatusStatusLineInfo spaceStatusStatusLineInfo = null;
                    String spaceStatusStatusLineInfoValue = boxInfoObject.optString("spaceStatusStatusLineInfo");
                    if (!TextUtils.isEmpty(spaceStatusStatusLineInfoValue)) {
                        try {
                            spaceStatusStatusLineInfo = new Gson().fromJson(spaceStatusStatusLineInfoValue, SpaceStatusStatusLineInfo.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                        if (spaceStatusStatusLineInfo != null) {
                            isHandleUpdate = true;
                        }
                    } else {
                        isHandleUpdate = true;
                    }
                    if (isHandleUpdate && !SpaceStatusStatusLineInfo.compare(spaceStatusStatusLineInfo, eulixBoxInfo.getSpaceStatusStatusLineInfo())) {
                        isUpdate = true;
                        eulixBoxInfo.setSpaceStatusStatusLineInfo(spaceStatusStatusLineInfo);
                    }
                }
                if (boxInfoObject.has("securityEmailInfo")) {
                    boolean isHandleUpdate = false;
                    SecurityEmailInfo securityEmailInfo = null;
                    String securityEmailInfoValue = boxInfoObject.optString("securityEmailInfo");
                    if (!TextUtils.isEmpty(securityEmailInfoValue)) {
                        try {
                            securityEmailInfo = new Gson().fromJson(securityEmailInfoValue, SecurityEmailInfo.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                        if (securityEmailInfo != null) {
                            isHandleUpdate = true;
                        }
                    } else {
                        isHandleUpdate = true;
                    }
                    if (isHandleUpdate) {
                        String newEmailAccount = null;
                        String newHost = null;
                        String newPort = null;
                        Boolean newSslEnable = null;
                        if (securityEmailInfo != null) {
                            newEmailAccount = securityEmailInfo.getEmailAccount();
                            newHost = securityEmailInfo.getHost();
                            newPort = securityEmailInfo.getPort();
                            newSslEnable = securityEmailInfo.isSslEnable();
                        }
                        String oldEmailAccount = null;
                        String oldHost = null;
                        String oldPort = null;
                        Boolean oldSslEnable = null;
                        SecurityEmailInfo oldSecurityEmailInfo = eulixBoxInfo.getSecurityEmailInfo();
                        if (oldSecurityEmailInfo != null) {
                            oldEmailAccount = oldSecurityEmailInfo.getEmailAccount();
                            oldHost = oldSecurityEmailInfo.getHost();
                            oldPort = oldSecurityEmailInfo.getPort();
                            oldSslEnable = oldSecurityEmailInfo.isSslEnable();
                        }
                        if ((newEmailAccount == null && oldEmailAccount != null) || (newEmailAccount != null && !newEmailAccount.equals(oldEmailAccount))
                                || (newHost == null && oldHost != null) || (newHost != null && !newHost.equals(oldHost))
                                || (newPort == null && oldPort != null) || (newPort != null && !newPort.equals(oldPort))
                                || (newSslEnable == null && oldSslEnable != null) || (newSslEnable != null && newSslEnable != oldSslEnable)) {
                            isUpdate = true;
                            eulixBoxInfo.setSecurityEmailInfo(securityEmailInfo);
                        }
                    }
                }
                if (boxInfoObject.has("securityPasswordInfo")) {
                    boolean isHandleUpdate = false;
                    SecurityPasswordInfo securityPasswordInfo = null;
                    String securityPasswordInfoValue = boxInfoObject.optString("securityPasswordInfo");
                    if (!TextUtils.isEmpty(securityPasswordInfoValue)) {
                        try {
                            securityPasswordInfo = new Gson().fromJson(securityPasswordInfoValue, SecurityPasswordInfo.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                        if (securityPasswordInfo != null) {
                            isHandleUpdate = true;
                        }
                    } else {
                        isHandleUpdate = true;
                    }
                    if (isHandleUpdate) {
                        isUpdate = true;
                        eulixBoxInfo.setSecurityPasswordInfo(securityPasswordInfo);
                    }
                }
                if (boxInfoObject.has("diskManageListResult")) {
                    boolean isHandleUpdate = false;
                    DiskManageListResult diskManageListResult = null;
                    String diskManageListResultValue = boxInfoObject.optString("diskManageListResult");
                    if (!TextUtils.isEmpty(diskManageListResultValue)) {
                        try {
                            diskManageListResult = new Gson().fromJson(diskManageListResultValue, DiskManageListResult.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                        if (diskManageListResult != null) {
                            isHandleUpdate = true;
                        }
                    } else {
                        isHandleUpdate = true;
                    }
                    if (isHandleUpdate) {
                        isUpdate = true;
                        eulixBoxInfo.setDiskManageListResult(diskManageListResult);
                    }
                }
                if (boxInfoObject.has("raidInfoResult")) {
                    boolean isHandleUpdate = false;
                    RaidInfoResult raidInfoResult = null;
                    String raidInfoResultValue = boxInfoObject.optString("raidInfoResult");
                    if (!TextUtils.isEmpty(raidInfoResultValue)) {
                        try {
                            raidInfoResult = new Gson().fromJson(raidInfoResultValue, RaidInfoResult.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                        if (raidInfoResult != null) {
                            isHandleUpdate = true;
                        }
                    } else {
                        isHandleUpdate = true;
                    }
                    if (isHandleUpdate) {
                        isUpdate = true;
                        eulixBoxInfo.setRaidInfoResult(raidInfoResult);
                    }
                }
                if (boxInfoObject.has("deviceAbility")) {
                    boolean isHandleUpdate = false;
                    DeviceAbility deviceAbility = null;
                    String deviceAbilityValue = boxInfoObject.optString("deviceAbility");
                    if (!TextUtils.isEmpty(deviceAbilityValue)) {
                        try {
                            deviceAbility = new Gson().fromJson(deviceAbilityValue, DeviceAbility.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                        if (deviceAbility != null) {
                            isHandleUpdate = true;
                        }
                    } else {
                        isHandleUpdate = true;
                    }
                    if (isHandleUpdate && !DeviceAbility.compare(deviceAbility, eulixBoxInfo.getDeviceAbility())) {
                        isUpdate = true;
                        eulixBoxInfo.setDeviceAbility(deviceAbility);
                    }
                }
                if (boxInfoObject.has("aoSpaceAccessBean")) {
                    boolean isHandleUpdate = false;
                    AOSpaceAccessBean aoSpaceAccessBean = null;
                    String aoSpaceAccessBeanValue = boxInfoObject.optString("aoSpaceAccessBean");
                    if (!TextUtils.isEmpty(aoSpaceAccessBeanValue)) {
                        try {
                            aoSpaceAccessBean = new Gson().fromJson(aoSpaceAccessBeanValue, AOSpaceAccessBean.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                        if (aoSpaceAccessBean != null) {
                            isHandleUpdate = true;
                        }
                    } else {
                        isHandleUpdate = true;
                    }
                    if (isHandleUpdate && !AOSpaceAccessBean.compare(aoSpaceAccessBean, eulixBoxInfo.getAoSpaceAccessBean())) {
                        isUpdate = true;
                        eulixBoxInfo.setAoSpaceAccessBean(aoSpaceAccessBean);
                    }
                }
                if (boxInfoObject.has("stunAddress")) {
                    String stunAddressValue = boxInfoObject.optString("stunAddress");
                    if (!TextUtils.isEmpty(stunAddressValue) && !stunAddressValue.equals(eulixBoxInfo.getStunAddress())) {
                        isUpdate = true;
                        eulixBoxInfo.setStunAddress(stunAddressValue);
                    }
                }
                if (boxInfoObject.has("stunAddressMap")) {
                    boolean isHandleUpdate = false;
                    Map<String, String> stunAddressMap = null;
                    String stunAddressMapValue = boxInfoObject.optString("stunAddressMap");
                    if (!TextUtils.isEmpty(stunAddressMapValue)) {
                        try {
                            stunAddressMap = new Gson().fromJson(stunAddressMapValue, new TypeToken<Map<String, String>>(){}.getType());
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                    } else {
                        isHandleUpdate = true;
                    }
                    if (stunAddressMap != null) {
                        isHandleUpdate = true;
                    }
                    if (isHandleUpdate) {
                        isUpdate = true;
                        eulixBoxInfo.setStunAddressMap(stunAddressMap);
                    }
                }
                if (isUpdate) {
                    Map<String, String> boxV = new HashMap<>();
                    boxV.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                    boxV.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                    boxV.put(EulixSpaceDBManager.FIELD_BOX_INFO, new Gson().toJson(eulixBoxInfo, EulixBoxInfo.class));
                    EulixSpaceDBUtil.updateBox(EulixSpaceApplication.getContext(), boxV);
                }
            }
            if (boxInfoObject.has(DB_BOX_REQUEST_ID)) {
                String requestId = boxInfoObject.optString(DB_BOX_REQUEST_ID);
                if (!TextUtils.isEmpty(requestId) && updateFinishCallbackMap.containsKey(requestId)) {
                    UpdateFinishCallback callback = updateFinishCallbackMap.get(requestId);
                    if (callback != null) {
                        callback.updateFinish(isUpdate);
                    }
                    try {
                        updateFinishCallbackMap.remove(requestId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (eulixBoxInfoQueue.isEmpty()) {
            lockEulixBoxInfo = false;
        } else if (mHandler != null) {
            mHandler.sendEmptyMessage(UPDATE_BOX_INFO);
        }
        return isUpdate;
    }

    /**
     * @param boxOtherInfoObject
     * @param callback
     * @return -1：依赖Callback回复；0：当场回复，不更新；1：当场回复，更新
     */
    public synchronized int updateBoxOtherInfo(JSONObject boxOtherInfoObject, UpdateFinishCallback callback) {
        int result = 0;
        if (boxOtherInfoObject != null) {
            if (lockEulixBoxOtherInfo) {
                result = -1;
                if (callback != null) {
                    String requestId;
                    do {
                        requestId = UUID.randomUUID().toString();
                    } while (updateFinishCallbackMap.containsKey(requestId));
                    updateFinishCallbackMap.put(requestId, callback);
                    try {
                        boxOtherInfoObject.put(DB_BOX_REQUEST_ID, requestId);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                eulixBoxOtherInfoQueue.offer(boxOtherInfoObject);
                // 防止小概率被悬挂
                if (!lockEulixBoxOtherInfo) {
                    lockEulixBoxOtherInfo = true;
                    if (mHandler != null) {
                        mHandler.sendEmptyMessage(UPDATE_BOX_OTHER_INFO);
                    }
                }
            } else {
                lockEulixBoxOtherInfo = true;
                if (handleUpdateBoxOtherInfo(boxOtherInfoObject)) {
                    result = 1;
                }
            }
        }
        return result;
    }

    private boolean handleUpdateBoxOtherInfo(JSONObject boxOtherInfoObject) {
        boolean isUpdate = false;
        if (boxOtherInfoObject != null) {
            EulixBoxOtherInfo eulixBoxOtherInfo = null;
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(EulixSpaceApplication.getContext(), queryMap);
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_OTHER_INFO)) {
                        String boxOtherInfoValue = StringUtil.nullToEmpty(boxValue.get(EulixSpaceDBManager.FIELD_BOX_OTHER_INFO));
                        if (TextUtils.isEmpty(boxOtherInfoValue.trim())) {
                            eulixBoxOtherInfo = new EulixBoxOtherInfo();
                        } else {
                            try {
                                eulixBoxOtherInfo = new Gson().fromJson(boxOtherInfoValue, EulixBoxOtherInfo.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        if (eulixBoxOtherInfo == null && !"1".equals(boxBind)) {
                            eulixBoxOtherInfo = new EulixBoxOtherInfo();
                        }
                        break;
                    }
                }
            }
            if (eulixBoxOtherInfo != null) {
                if (boxOtherInfoObject.has("terminalInfoMap")) {
                    boolean isHandleUpdate = false;
                    Map<String, TerminalInfo> terminalInfoMap = null;
                    String terminalInfoMapValue = boxOtherInfoObject.optString("terminalInfoMap");
                    if (!TextUtils.isEmpty(terminalInfoMapValue)) {
                        try {
                            terminalInfoMap = new Gson().fromJson(terminalInfoMapValue, new TypeToken<Map<String, TerminalInfo>>(){}.getType());
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                        if (terminalInfoMap != null) {
                            isHandleUpdate = true;
                        }
                    } else {
                        isHandleUpdate = true;
                    }
                    if (isHandleUpdate) {
                        isUpdate = true;
                        eulixBoxOtherInfo.setTerminalInfoMap(terminalInfoMap);
                    }
                }
                if (boxOtherInfoObject.has("applicationLockInfo")) {
                    boolean isHandleUpdate = false;
                    ApplicationLockInfo applicationLockInfo = null;
                    String applicationLockInfoValue = boxOtherInfoObject.optString("applicationLockInfo");
                    if (!TextUtils.isEmpty(applicationLockInfoValue)) {
                        try {
                            applicationLockInfo = new Gson().fromJson(applicationLockInfoValue, ApplicationLockInfo.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                        if (applicationLockInfo != null) {
                            isHandleUpdate = true;
                        }
                    } else {
                        isHandleUpdate = true;
                    }
                    if (isHandleUpdate) {
                        ApplicationLockInfo currentApplicationLockInfo = eulixBoxOtherInfo.getApplicationLockInfo();
                        if (!ApplicationLockInfo.compare(currentApplicationLockInfo, applicationLockInfo)) {
                            isUpdate = true;
                            eulixBoxOtherInfo.setApplicationLockInfo(applicationLockInfo);
                        }
                    }
                }
                if (boxOtherInfoObject.has("eulixPushDeviceToken")) {
                    String eulixPushDeviceToken = boxOtherInfoObject.optString("eulixPushDeviceToken");
                    if (!TextUtils.isEmpty(eulixPushDeviceToken) && !eulixPushDeviceToken.equals(eulixBoxOtherInfo.getEulixPushDeviceToken())) {
                        isUpdate = true;
                        eulixBoxOtherInfo.setEulixPushDeviceToken(eulixPushDeviceToken);
                    }
                }
                if (boxOtherInfoObject.has("eulixPushDeviceTokenMap")) {
                    boolean isHandleUpdate = false;
                    Map<String, String> eulixPushDeviceTokenMap = null;
                    String eulixPushDeviceTokenMapValue = boxOtherInfoObject.optString("eulixPushDeviceTokenMap");
                    if (!TextUtils.isEmpty(eulixPushDeviceTokenMapValue)) {
                        try {
                            eulixPushDeviceTokenMap = new Gson().fromJson(eulixPushDeviceTokenMapValue, new TypeToken<Map<String, String>>(){}.getType());
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                    } else {
                        isHandleUpdate = true;
                    }
                    if (eulixPushDeviceTokenMap != null) {
                        isHandleUpdate = true;
                    }
                    if (isHandleUpdate) {
                        isUpdate = true;
                        eulixBoxOtherInfo.setEulixPushDeviceTokenMap(eulixPushDeviceTokenMap);
                    }
                }
                if (boxOtherInfoObject.has("isDeveloperMode")) {
                    boolean newDeveloperMode = boxOtherInfoObject.optBoolean("isDeveloperMode");
                    boolean oldDeveloperMode = eulixBoxOtherInfo.isDeveloperMode();
                    if (newDeveloperMode != oldDeveloperMode) {
                        isUpdate = true;
                        eulixBoxOtherInfo.setDeveloperMode(newDeveloperMode);
                    }
                }
                if (boxOtherInfoObject.has("spacePlatformInfo")) {
                    boolean isHandleUpdate = false;
                    SpacePlatformInfo spacePlatformInfo = null;
                    String spacePlatformInfoValue = boxOtherInfoObject.optString("spacePlatformInfo");
                    if (!TextUtils.isEmpty(spacePlatformInfoValue)) {
                        try {
                            spacePlatformInfo = new Gson().fromJson(spacePlatformInfoValue, SpacePlatformInfo.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                        if (spacePlatformInfo != null) {
                            isHandleUpdate = true;
                        }
                    } else {
                        isHandleUpdate = true;
                    }
                    if (isHandleUpdate && !SpacePlatformInfo.compare(spacePlatformInfo, eulixBoxOtherInfo.getSpacePlatformInfo())) {
                        isUpdate = true;
                        eulixBoxOtherInfo.setSpacePlatformInfo(spacePlatformInfo);
                    }
                }
                if (isUpdate) {
                    Map<String, String> boxV = new HashMap<>();
                    boxV.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                    boxV.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                    boxV.put(EulixSpaceDBManager.FIELD_BOX_OTHER_INFO, new Gson().toJson(eulixBoxOtherInfo, EulixBoxOtherInfo.class));
                    EulixSpaceDBUtil.updateBox(EulixSpaceApplication.getContext(), boxV);
                }
            }
            if (boxOtherInfoObject.has(DB_BOX_REQUEST_ID)) {
                String requestId = boxOtherInfoObject.optString(DB_BOX_REQUEST_ID);
                if (!TextUtils.isEmpty(requestId) && updateFinishCallbackMap.containsKey(requestId)) {
                    UpdateFinishCallback callback = updateFinishCallbackMap.get(requestId);
                    if (callback != null) {
                        callback.updateFinish(isUpdate);
                    }
                    try {
                        updateFinishCallbackMap.remove(requestId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (eulixBoxOtherInfoQueue.isEmpty()) {
            lockEulixBoxOtherInfo = false;
        } else if (mHandler != null) {
            mHandler.sendEmptyMessage(UPDATE_BOX_OTHER_INFO);
        }
        return isUpdate;
    }
}
