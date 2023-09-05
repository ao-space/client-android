package xyz.eulix.space.util;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.eulix.space.bean.AOSpaceAccessBean;
import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.bean.EulixBoxInfo;
import xyz.eulix.space.bean.EulixBoxToken;
import xyz.eulix.space.bean.EulixBoxTokenDetail;
import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.did.bean.DIDCredentialBean;
import xyz.eulix.space.did.bean.DIDProviderBean;
import xyz.eulix.space.did.bean.DIDReserveBean;
import xyz.eulix.space.event.BoxInsertDeleteEvent;
import xyz.eulix.space.event.BoxOnlineRequestEvent;
import xyz.eulix.space.event.BoxStatusEvent;
import xyz.eulix.space.event.LanStatusEvent;
import xyz.eulix.space.event.SpaceChangeEvent;
import xyz.eulix.space.manager.AlreadyUploadedManager;
import xyz.eulix.space.manager.EulixSpaceDBBoxManager;
import xyz.eulix.space.manager.LanManager;
import xyz.eulix.space.manager.TransferTaskManager;
import xyz.eulix.space.network.agent.PairingBoxInfo;
import xyz.eulix.space.network.agent.bind.ConnectedNetwork;
import xyz.eulix.space.network.agent.disk.DiskManageListResult;

public class AOSpaceUtil {
    private static final String TAG = AOSpaceUtil.class.getSimpleName();

    private AOSpaceUtil() {}

    public static int checkBoxUuidStatus(@NonNull Context context, String boxUuid) {
        int status = 0;
        if (boxUuid != null) {
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)
                            && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_STATUS)) {
                        String stateValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_STATUS);
                        int state = -1;
                        if (stateValue != null) {
                            try {
                                state = Integer.parseInt(stateValue);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                        if (state >= ConstantField.EulixDeviceStatus.OFFLINE && state <= ConstantField.EulixDeviceStatus.ONLINE_UNINITIALIZED) {
                            String bindValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                            if (state == ConstantField.EulixDeviceStatus.ONLINE_UNINITIALIZED || state == ConstantField.EulixDeviceStatus.OFFLINE_UNINITIALIZED) {
                                status = 2;
                            } else if (status != 2 && bindValue != null) {
                                if ("1".equals(bindValue)) {
                                    status = 1;
                                } else if (status != 1 && "-1".equals(bindValue)) {
                                    status = -1;
                                }
                            }
                        }
                    }
                }
            }
        }
        return status;
    }

    public static boolean checkPairedSelf(@NonNull Context context, int paired, String pairClientUuid) {
        boolean isPairedSelf = false;
        if (pairClientUuid != null) {
            isPairedSelf = (paired == 0 && pairClientUuid.equals(DataUtil.getClientUuid(context)));
        }
        return isPairedSelf;
    }

    public static void handleSaveBindingBox(@NonNull Context context, PairingBoxInfo pairingBoxInfo, String boxPubKey, String deviceName, String deviceAddress, String bluetoothId, DeviceAbility deviceAbility) {
        if (pairingBoxInfo != null) {
            String boxUuid = pairingBoxInfo.getBoxUuid();
            if (boxPubKey == null) {
                boxPubKey = pairingBoxInfo.getBoxPubKey();
            }
            long currentTimestamp = System.currentTimeMillis();
            Long expireTimestamp = null;
            if (boxUuid != null) {
                EulixBoxInfo eulixBoxInfo = null;
                Map<String, String> queryMap = new HashMap<>();
                queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, "1");
                List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context.getApplicationContext(), queryMap);
                Map<String, String> boxValue = new HashMap<>();
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_NAME, pairingBoxInfo.getBoxName());
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY, StringUtil.unwrapPublicKey(boxPubKey));
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_AUTHORIZATION, pairingBoxInfo.getAuthKey());
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_REGISTER, pairingBoxInfo.getRegKey());
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_DOMAIN, pairingBoxInfo.getUserDomain());
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, String.valueOf(1));
                boxValue.put(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME, String.valueOf(currentTimestamp));
                if (boxValues == null || boxValues.size() <= 0) {
                    boxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.BINDING));
                    eulixBoxInfo = new EulixBoxInfo();
                    eulixBoxInfo.setBluetoothAddress(deviceAddress);
                    eulixBoxInfo.setBluetoothId(bluetoothId);
                    eulixBoxInfo.setBluetoothDeviceName(deviceName);
                    eulixBoxInfo.setDeviceAbility(deviceAbility);
                    boxValue.put(EulixSpaceDBManager.FIELD_BOX_INFO, new Gson().toJson(eulixBoxInfo, EulixBoxInfo.class));
                    EulixSpaceDBUtil.insertBox(context.getApplicationContext(), boxValue, 1);
                    BoxInsertDeleteEvent boxInsertDeleteEvent = new BoxInsertDeleteEvent(boxUuid, "1", true);
                    EventBusUtil.post(boxInsertDeleteEvent);
                } else {
                    String eulixBoxInfoValue = null;
                    for (Map<String, String> boxV : boxValues) {
                        if (boxV != null && boxV.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
                            String boxTokenValue = boxV.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                            if (boxTokenValue != null) {
                                EulixBoxToken eulixBoxToken = null;
                                try {
                                    eulixBoxToken = new Gson().fromJson(boxTokenValue, EulixBoxToken.class);
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                                if (eulixBoxToken != null) {
                                    expireTimestamp = eulixBoxToken.getTokenExpire();
                                }
                            }
                            if (boxV.containsKey(EulixSpaceDBManager.FIELD_BOX_INFO)) {
                                eulixBoxInfoValue = boxV.get(EulixSpaceDBManager.FIELD_BOX_INFO);
                            }
                            break;
                        }
                    }
                    if (eulixBoxInfoValue != null) {
                        try {
                            eulixBoxInfo = new Gson().fromJson(eulixBoxInfoValue, EulixBoxInfo.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                    if (eulixBoxInfo == null) {
                        eulixBoxInfo = new EulixBoxInfo();
                    }
                    eulixBoxInfo.setBluetoothAddress(deviceAddress);
                    eulixBoxInfo.setBluetoothId(bluetoothId);
                    eulixBoxInfo.setBluetoothDeviceName(deviceName);
                    eulixBoxInfo.setDeviceAbility(deviceAbility);
                    boxValue.put(EulixSpaceDBManager.FIELD_BOX_INFO, new Gson().toJson(eulixBoxInfo, EulixBoxInfo.class));
                    EulixSpaceDBUtil.updateBox(context.getApplicationContext(), boxValue);
                }
            }
        }
    }

    public static void requestAdministratorBindUseBox(@NonNull Context context, String boxUuid, boolean isDiskInitialize) {
        Map<String, String> boxValue = new HashMap<>();
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, "1");
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(isDiskInitialize ? ConstantField.EulixDeviceStatus.ONLINE_UNINITIALIZED : ConstantField.EulixDeviceStatus.REQUEST_USE));
        boxValue.put(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME, String.valueOf(System.currentTimeMillis()));
        EulixSpaceDBUtil.updateBox(context, boxValue);
    }

    public static boolean requestUseBox(@NonNull Context context, String boxUuid, String boxBind, PairingBoxInfo pairingBoxInfo, String ipAddressUrl, String boxPubKey, String deviceName, String deviceAddress, String bluetoothId, DeviceAbility deviceAbility, boolean isDiskInitialize) {
        return requestUseBox(context, boxUuid, boxBind, pairingBoxInfo, null, ipAddressUrl, boxPubKey, deviceName, deviceAddress, bluetoothId, deviceAbility, isDiskInitialize);
    }

    public static boolean requestUseBox(@NonNull Context context, String boxUuid, String boxBind, PairingBoxInfo pairingBoxInfo, AOSpaceAccessBean aoSpaceAccessBean, String ipAddressUrl, String boxPubKey, String deviceName, String deviceAddress, String bluetoothId, DeviceAbility deviceAbility, boolean isDiskInitialize) {
        boolean isHandle = false;
        if (pairingBoxInfo != null) {
            String boxDomain = null;
            String userDomain = pairingBoxInfo.getUserDomain();
            boolean isInternetAccess = true;
            boolean isLanAccess = true;
            String spaceName = pairingBoxInfo.getSpaceName();
            if (aoSpaceAccessBean != null) {
                Boolean isInternetAccessValue = aoSpaceAccessBean.getInternetAccess();
                if (isInternetAccessValue != null) {
                    isInternetAccess = isInternetAccessValue;
                }
                Boolean isLanAccessValue = aoSpaceAccessBean.getLanAccess();
                if (isLanAccessValue != null) {
                    isLanAccess = isLanAccessValue;
                }
                if (userDomain != null && FormatUtil.isHttpUrlString(FormatUtil.generateHttpUrlString(userDomain))) {
                    aoSpaceAccessBean.setUserDomain(userDomain);
                }
                if (ipAddressUrl != null && FormatUtil.isHttpUrlString(FormatUtil.generateHttpUrlString(ipAddressUrl))) {
                    aoSpaceAccessBean.setIpAddressUrl(ipAddressUrl);
                }
            }
            if (isInternetAccess) {
                boxDomain = userDomain;
            } else if (isLanAccess) {
                boxDomain = ipAddressUrl;
            }
            if (boxPubKey == null) {
                boxPubKey = pairingBoxInfo.getBoxPubKey();
            }
            long currentTimestamp = System.currentTimeMillis();
            Long expireTimestamp = null;
            String clientUuid = DataUtil.getClientUuid(context);
            if (boxUuid != null && boxBind != null && boxDomain != null && FormatUtil.isHttpUrlString(FormatUtil.generateHttpUrlString(boxDomain))) {
                isHandle = true;
                EulixBoxInfo eulixBoxInfo = null;
                Map<String, String> queryMap = new HashMap<>();
                queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context.getApplicationContext(), queryMap);
                Map<String, String> boxValue = new HashMap<>();
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_NAME, pairingBoxInfo.getBoxName());
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY, StringUtil.unwrapPublicKey(boxPubKey));
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_AUTHORIZATION, pairingBoxInfo.getAuthKey());
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_REGISTER, pairingBoxInfo.getRegKey());
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_DOMAIN, boxDomain);
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                boxValue.put(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME, String.valueOf(currentTimestamp));
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(isDiskInitialize
                        ? ConstantField.EulixDeviceStatus.ONLINE_UNINITIALIZED
                        : (("1".equals(boxBind) || "-1".equals(boxBind))
                        ? ConstantField.EulixDeviceStatus.REQUEST_USE : ConstantField.EulixDeviceStatus.REQUEST_LOGIN)));
                if (boxValues == null || boxValues.size() <= 0) {
                    eulixBoxInfo = new EulixBoxInfo();
                    eulixBoxInfo.setBluetoothAddress(deviceAddress);
                    eulixBoxInfo.setBluetoothId(bluetoothId);
                    eulixBoxInfo.setBluetoothDeviceName(deviceName);
                    eulixBoxInfo.setDeviceAbility(deviceAbility);
                    eulixBoxInfo.setAoSpaceAccessBean(aoSpaceAccessBean);
                    boxValue.put(EulixSpaceDBManager.FIELD_BOX_INFO, new Gson().toJson(eulixBoxInfo, EulixBoxInfo.class));
                    if (spaceName != null && clientUuid != null) {
                        Map<String, UserInfo> userInfoMap = new HashMap<>();
                        UserInfo userInfo = new UserInfo();
                        userInfo.setNickName(spaceName);
                        userInfo.setUserDomain(userDomain);
                        userInfoMap.put(clientUuid, userInfo);
                        boxValue.put(EulixSpaceDBManager.FIELD_BOX_USER_INFO, new Gson().toJson(userInfoMap, new TypeToken<Map<String, UserInfo>>(){}.getType()));
                    }
                    EulixSpaceDBUtil.insertBox(context.getApplicationContext(), boxValue, 0);
                    BoxInsertDeleteEvent boxInsertDeleteEvent = new BoxInsertDeleteEvent(boxUuid, boxBind, true);
                    EventBusUtil.post(boxInsertDeleteEvent);
                } else {
                    String eulixBoxInfoValue = null;
                    String userInfoValue = null;
                    for (Map<String, String> boxV : boxValues) {
                        if (boxV != null && boxV.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
                            String boxTokenValue = boxV.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                            if (boxTokenValue != null) {
                                EulixBoxToken eulixBoxToken = null;
                                try {
                                    eulixBoxToken = new Gson().fromJson(boxTokenValue, EulixBoxToken.class);
                                } catch (JsonSyntaxException e) {
                                    e.printStackTrace();
                                }
                                if (eulixBoxToken != null) {
                                    expireTimestamp = eulixBoxToken.getTokenExpire();
                                }
                            }
                            if (boxV.containsKey(EulixSpaceDBManager.FIELD_BOX_INFO)) {
                                eulixBoxInfoValue = boxV.get(EulixSpaceDBManager.FIELD_BOX_INFO);
                            }
                            if (boxV.containsKey(EulixSpaceDBManager.FIELD_BOX_USER_INFO)) {
                                userInfoValue = boxV.get(EulixSpaceDBManager.FIELD_BOX_USER_INFO);
                            }
                            break;
                        }
                    }
                    if (eulixBoxInfoValue != null) {
                        try {
                            eulixBoxInfo = new Gson().fromJson(eulixBoxInfoValue, EulixBoxInfo.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                    if (eulixBoxInfo == null) {
                        eulixBoxInfo = new EulixBoxInfo();
                    }
                    eulixBoxInfo.setBluetoothAddress(deviceAddress);
                    eulixBoxInfo.setBluetoothId(bluetoothId);
                    eulixBoxInfo.setBluetoothDeviceName(deviceName);
                    eulixBoxInfo.setDeviceAbility(deviceAbility);
                    eulixBoxInfo.setAoSpaceAccessBean(aoSpaceAccessBean);
                    boxValue.put(EulixSpaceDBManager.FIELD_BOX_INFO, new Gson().toJson(eulixBoxInfo, EulixBoxInfo.class));
                    if (spaceName != null && clientUuid != null) {
                        Map<String, UserInfo> userInfoMap = null;
                        if (userInfoValue != null) {
                            try {
                                userInfoMap = new Gson().fromJson(userInfoValue, new TypeToken<Map<String, UserInfo>>(){}.getType());
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        if (userInfoMap == null) {
                            userInfoMap = new HashMap<>();
                        }
                        UserInfo userInfo = null;
                        if (userInfoMap.containsKey(clientUuid)) {
                            userInfo = userInfoMap.get(clientUuid);
                        }
                        if (userInfo == null) {
                            userInfo = new UserInfo();
                        }
                        userInfo.setNickName(spaceName);
                        userInfo.setUserDomain(userDomain);
                        userInfoMap.put(clientUuid, userInfo);
                        boxValue.put(EulixSpaceDBManager.FIELD_BOX_USER_INFO, new Gson().toJson(userInfoMap, new TypeToken<Map<String, UserInfo>>(){}.getType()));
                    }
                    EulixSpaceDBUtil.updateBox(context.getApplicationContext(), boxValue);
                }
            }
        }
        return isHandle;
    }

    public static void requestUseBox(@NonNull Context context, String boxUuid, String boxBind, DiskManageListResult diskManageListResult) {
        Map<String, String> boxValue = new HashMap<>();
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.REQUEST_USE));
        boxValue.put(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME, String.valueOf(System.currentTimeMillis()));
        EulixSpaceDBUtil.updateBox(context, boxValue);
        if (diskManageListResult != null) {
            boolean isStorageDiskManageList = false;
            boolean isHandle = false;
            JSONObject jsonObject = null;
            EulixSpaceDBBoxManager eulixSpaceDBBoxManager = EulixSpaceDBBoxManager.getInstance(boxUuid, boxBind);
            if (eulixSpaceDBBoxManager != null) {
                isHandle = true;
                jsonObject = new JSONObject();
                try {
                    jsonObject.put("diskManageListResult", new Gson().toJson(diskManageListResult, DiskManageListResult.class));
                } catch (JSONException e) {
                    e.printStackTrace();
                    isHandle = false;
                }
            }
            if (isHandle) {
                int result = eulixSpaceDBBoxManager.updateBoxInfo(jsonObject, isUpdate -> {
                    ;
                });
                isStorageDiskManageList = (result >= 0);
            } else {
                isStorageDiskManageList = true;
                EulixBoxInfo eulixBoxInfo = EulixSpaceDBUtil.getSpecificBoxInfo(context, boxUuid, boxBind);
                if (eulixBoxInfo == null) {
                    eulixBoxInfo = new EulixBoxInfo();
                }
                eulixBoxInfo.setDiskManageListResult(diskManageListResult);
                Map<String, String> boxV = new HashMap<>();
                boxV.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                boxV.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                boxV.put(EulixSpaceDBManager.FIELD_BOX_INFO, new Gson().toJson(eulixBoxInfo, EulixBoxInfo.class));
                EulixSpaceDBUtil.updateBox(context, boxV);
            }
        }
    }

    public static void updateDiskManagementData(@NonNull Context context, String boxUuid, String boxBind, DiskManageListResult diskManageListResult) {
        if (diskManageListResult != null) {
            boolean isStorageDiskManageList = false;
            boolean isHandle = false;
            JSONObject jsonObject = null;
            EulixSpaceDBBoxManager eulixSpaceDBBoxManager = EulixSpaceDBBoxManager.getInstance(boxUuid, boxBind);
            if (eulixSpaceDBBoxManager != null) {
                isHandle = true;
                jsonObject = new JSONObject();
                try {
                    jsonObject.put("diskManageListResult", new Gson().toJson(diskManageListResult, DiskManageListResult.class));
                } catch (JSONException e) {
                    e.printStackTrace();
                    isHandle = false;
                }
            }
            if (isHandle) {
                int result = eulixSpaceDBBoxManager.updateBoxInfo(jsonObject, isUpdate -> {
                    ;
                });
                isStorageDiskManageList = (result >= 0);
            } else {
                isStorageDiskManageList = true;
                EulixBoxInfo eulixBoxInfo = EulixSpaceDBUtil.getSpecificBoxInfo(context, boxUuid, boxBind);
                if (eulixBoxInfo == null) {
                    eulixBoxInfo = new EulixBoxInfo();
                }
                eulixBoxInfo.setDiskManageListResult(diskManageListResult);
                Map<String, String> boxV = new HashMap<>();
                boxV.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                boxV.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                boxV.put(EulixSpaceDBManager.FIELD_BOX_INFO, new Gson().toJson(eulixBoxInfo, EulixBoxInfo.class));
                EulixSpaceDBUtil.updateBox(context, boxV);
            }
            if (isStorageDiskManageList) {
                // Do nothing
            }
        }
    }

    public static boolean requestMemberBindUseBox(@NonNull Context context, String boxUuid, String boxDomain, String authKey, String boxPublicKey, boolean isForce) {
        boolean isHandle = false;
        if (FormatUtil.isHttpUrlString(FormatUtil.generateHttpUrlString(boxDomain))) {
            isHandle = true;
            long currentTimestamp = System.currentTimeMillis();
            Long expireTimestamp = null;
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, "-1");
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context.getApplicationContext(), queryMap);
            Map<String, String> boxValue = new HashMap<>();
            boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, "-1");
            boxValue.put(EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY, StringUtil.unwrapPublicKey(boxPublicKey));
            boxValue.put(EulixSpaceDBManager.FIELD_BOX_AUTHORIZATION, authKey);
            boxValue.put(EulixSpaceDBManager.FIELD_BOX_DOMAIN, boxDomain);
            boxValue.put(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME, String.valueOf(currentTimestamp));
            if (boxValues == null || boxValues.size() <= 0) {
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.REQUEST_USE));
                EulixSpaceDBUtil.insertBox(context, boxValue, -1);
                BoxInsertDeleteEvent boxInsertDeleteEvent = new BoxInsertDeleteEvent(boxUuid, "-1", true);
                EventBusUtil.post(boxInsertDeleteEvent);
            } else {
                for (Map<String, String> boxV : boxValues) {
                    if (boxV != null && boxV.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN) && !isForce) {
                        String boxTokenValue = boxV.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                        if (boxTokenValue != null) {
                            EulixBoxToken eulixBoxToken = null;
                            try {
                                eulixBoxToken = new Gson().fromJson(boxTokenValue, EulixBoxToken.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                            if (eulixBoxToken != null) {
                                expireTimestamp = eulixBoxToken.getTokenExpire();
                            }
                        }
                        break;
                    }
                }
                if (isForce) {
                    boxValue.put(EulixSpaceDBManager.FIELD_BOX_TOKEN, "");
                }
                EulixSpaceDBUtil.updateBox(context, boxValue);
            }
        }
        return isHandle;
    }

    /**
     * 更改活跃盒子
     * @param context
     * @param boxUuid
     * @param boxBind
     * @param expireTimestamp
     * @param isOnline
     */
    public static void changeActiveBox(@NonNull Context context, String boxUuid, String boxBind, long expireTimestamp, boolean isOnline) {
        EulixSpaceDBUtil.readAppointPush(context, boxUuid, boxBind, true);
        List<Map<String, String>> activeBoxValues = EulixSpaceDBUtil.queryBox(context
                , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (activeBoxValues != null) {
            for (Map<String, String> activeBoxValue : activeBoxValues) {
                if (activeBoxValue != null && activeBoxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                        && activeBoxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                    String activeBoxUuid = activeBoxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                    String activeBoxBind = activeBoxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    Integer activeAlarmId = DataUtil.getTokenAlarmId(activeBoxUuid, activeBoxBind);
                    if (activeAlarmId != null) {
                        AlarmUtil.cancelAlarm(context, activeAlarmId);
                    }
                    String isBindValue = "";
                    if (activeBoxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                        isBindValue = activeBoxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    }
                    Map<String, String> requestUseBoxValue = new HashMap<>();
                    requestUseBoxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, activeBoxUuid);
                    requestUseBoxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, activeBoxBind);
                    requestUseBoxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf((("1".equals(isBindValue) || "-1".equals(isBindValue))
                            ? ConstantField.EulixDeviceStatus.REQUEST_USE
                            : ConstantField.EulixDeviceStatus.REQUEST_LOGIN)));
                    EulixSpaceDBUtil.updateBox(context, requestUseBoxValue);
                }
            }
        }
        List<Map<String, String>> offlineUseBoxValues = EulixSpaceDBUtil.queryBox(context
                , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
        if (offlineUseBoxValues != null) {
            for (Map<String, String> offlineUseBox : offlineUseBoxValues) {
                if (offlineUseBox != null && offlineUseBox.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                        && offlineUseBox.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                    String offlineUseBoxUuid = offlineUseBox.get(EulixSpaceDBManager.FIELD_BOX_UUID);
                    String offlineUseBoxBind = offlineUseBox.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    Map<String, String> offlineBoxValue = new HashMap<>();
                    offlineBoxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, offlineUseBoxUuid);
                    offlineBoxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, offlineUseBoxBind);
                    offlineBoxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE));
                    EulixSpaceDBUtil.updateBox(context, offlineBoxValue);
                }
            }
        }
        long currentTimestamp = System.currentTimeMillis();
        Integer boxAlarmId = DataUtil.getTokenAlarmId(boxUuid, boxBind);
        if (boxAlarmId != null) {
            AlarmUtil.cancelAlarm(context, boxAlarmId);
        }
        Map<String, String> boxValue = new HashMap<>();
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(isOnline ? ConstantField.EulixDeviceStatus.ACTIVE : ConstantField.EulixDeviceStatus.OFFLINE_USE));
        boxValue.put(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME, String.valueOf(currentTimestamp));
        EulixSpaceDBUtil.updateBox(context, boxValue);
        EulixBoxTokenDetail eulixBoxTokenDetail = new EulixBoxTokenDetail();
        eulixBoxTokenDetail.setBoxUuid(boxUuid);
        eulixBoxTokenDetail.setBoxBind(boxBind);
        eulixBoxTokenDetail.setTokenExpire(expireTimestamp);
        DataUtil.setLastBoxToken(eulixBoxTokenDetail);
        DataUtil.setLastEulixSpace(context, boxUuid, boxBind);
        int alarmId = AlarmUtil.getAlarmId();
        DataUtil.setTokenAlarmId(boxUuid, boxBind, alarmId);
        long diffTimestamp = 60 * 1000L;
        if (expireTimestamp > currentTimestamp) {
            diffTimestamp = Math.min(((expireTimestamp - currentTimestamp) / 10), diffTimestamp);
            AlarmUtil.setAlarm(context, (expireTimestamp - diffTimestamp), alarmId, boxUuid, boxBind, (diffTimestamp / 2));
        } else {
            AlarmUtil.setAlarm(context, (currentTimestamp + diffTimestamp), alarmId, boxUuid, boxBind, (diffTimestamp / 2));
        }
        EulixSpaceDBUtil.offlineTemperateBox(context);
        EventBusUtil.post(new BoxOnlineRequestEvent(true));
        EventBusUtil.post(new SpaceChangeEvent(true));
    }

    public static void prepareGoMain(@NonNull Context context) {
        AlreadyUploadedManager.getInstance().init(context);
        TransferTaskManager.getInstance().resetManagerData();
        LanManager.getInstance().setLanEnable(false);
        EventBusUtil.post(new LanStatusEvent(false));
        LanManager.getInstance().startPollCheckTask();
        EventBusUtil.post(new BoxStatusEvent(true));
    }

    public static boolean isInternetAccessEnable(Context context, String boxUuid, String boxBind) {
        boolean isEnable = true;
        AOSpaceAccessBean aoSpaceAccessBean = EulixSpaceDBUtil.getSpecificAOSpaceBean(context, boxUuid, boxBind);
        if (aoSpaceAccessBean != null) {
            Boolean isInternetAccess = aoSpaceAccessBean.getInternetAccess();
            if (isInternetAccess != null) {
                isEnable = isInternetAccess;
            }
        }
        return isEnable;
    }

    public static void insertOrUpdateDID(Context context, String boxUuid, String boxBind, DIDCredentialBean didCredentialBean) {
        if (context != null && boxUuid != null && boxBind != null && didCredentialBean != null) {
            DIDCredentialBean.BinderCredential binderCredential = didCredentialBean.getBinderCredential();
//            DIDCredentialBean.PasswordCredential passwordCredential = didCredentialBean.getPasswordCredential();
            Boolean isUpdate = null;
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_DID_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_DID_BIND, boxBind);
            List<Map<String, String>> didValues = EulixSpaceDBUtil.queryDID(context, queryMap);
            if (didValues != null && !didValues.isEmpty()) {
                isUpdate = true;
                for (Map<String, String> didV : didValues) {
                    if (didV != null && didV.containsKey(EulixSpaceDBManager.FIELD_DID_CREDENTIAL)) {
                        DIDCredentialBean.BinderCredential currentBinderCredential = null;
//                        DIDCredentialBean.PasswordCredential currentPasswordCredential = null;
                        String currentDIDCredentialValue = didV.get(EulixSpaceDBManager.FIELD_DID_CREDENTIAL);
                        if (StringUtil.isNonBlankString(currentDIDCredentialValue)) {
                            DIDCredentialBean currentDIDCredentialBean = null;
                            try {
                                currentDIDCredentialBean = new Gson().fromJson(currentDIDCredentialValue, DIDCredentialBean.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                            if (currentDIDCredentialBean != null) {
                                currentBinderCredential = currentDIDCredentialBean.getBinderCredential();
//                                currentPasswordCredential = currentDIDCredentialBean.getPasswordCredential();
                            } else {
                                isUpdate = false;
                                break;
                            }
                        }
                        if (binderCredential == null) {
                            binderCredential = currentBinderCredential;
                            didCredentialBean.setBinderCredential(currentBinderCredential);
                        }
//                        if (passwordCredential == null) {
//                            passwordCredential = currentPasswordCredential;
//                            didCredentialBean.setPasswordCredential(currentPasswordCredential);
//                        }
                        isUpdate = !(DIDCredentialBean.BinderCredential.compare(currentBinderCredential, binderCredential)
                                /*&& DIDCredentialBean.PasswordCredential.compare(currentPasswordCredential, passwordCredential)*/);
                        break;
                    }
                }
            }
            Logger.d(TAG, "did update: " + isUpdate);
            Logger.d(TAG, "binder credential: " + didCredentialBean.getBinderCredential());
//            Logger.d(TAG, "password credential: " + didCredentialBean.getPasswordCredential());
            if (isUpdate == null || isUpdate) {
                Map<String, String> didValue = new HashMap<>();
                didValue.put(EulixSpaceDBManager.FIELD_DID_UUID, boxUuid);
                didValue.put(EulixSpaceDBManager.FIELD_DID_BIND, boxBind);
                didValue.put(EulixSpaceDBManager.FIELD_DID_CREDENTIAL, new Gson().toJson(didCredentialBean, DIDCredentialBean.class));
                if (isUpdate == null) {
                    EulixSpaceDBUtil.insertDID(context, didValue);
                } else {
                    EulixSpaceDBUtil.updateDID(context, didValue);
                }
            }
        }
    }

    public static void insertOrUpdateDID(Context context, DIDProviderBean didProviderBean) {
        String boxUuid = null;
        String boxBind = null;
        String aoId = null;
        String didDoc = null;
        String didDocDecode = null;
        if (didProviderBean != null) {
            boxUuid = didProviderBean.getBoxUuid();
            boxBind = didProviderBean.getBoxBind();
            aoId = didProviderBean.getAoId();
            didDoc = didProviderBean.getDidDoc();
            didDocDecode = didProviderBean.getDidDocDecode();
        }
        if (context != null && boxUuid != null && boxBind != null) {
            Boolean isUpdate = null;
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_DID_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_DID_BIND, boxBind);
            List<Map<String, String>> didValues = EulixSpaceDBUtil.queryDID(context, queryMap);
            if (didValues != null && !didValues.isEmpty()) {
                isUpdate = true;
                for (Map<String, String> didV : didValues) {
                    if (didV != null && didV.containsKey(EulixSpaceDBManager.FIELD_DID_DOC_ENCODE)
                            && didV.containsKey(EulixSpaceDBManager.FIELD_DID_DOCUMENT)) {
                        isUpdate = !(StringUtil.compare(didDoc, didV.get(EulixSpaceDBManager.FIELD_DID_DOC_ENCODE))
                                && StringUtil.compare(didDocDecode, didV.get(EulixSpaceDBManager.FIELD_DID_DOCUMENT)));
                        break;
                    }
                }
            }
            Logger.d(TAG, "did update: " + isUpdate);
            if (isUpdate == null || isUpdate) {
                Map<String, String> didValue = new HashMap<>();
                didValue.put(EulixSpaceDBManager.FIELD_DID_UUID, boxUuid);
                didValue.put(EulixSpaceDBManager.FIELD_DID_BIND, boxBind);
                if (aoId != null) {
                    didValue.put(EulixSpaceDBManager.FIELD_DID_AO_ID, aoId);
                }
                didValue.put(EulixSpaceDBManager.FIELD_DID_DOC_ENCODE, StringUtil.nullToEmpty(didDoc));
                didValue.put(EulixSpaceDBManager.FIELD_DID_DOCUMENT, StringUtil.nullToEmpty(didDocDecode));
                didValue.put(EulixSpaceDBManager.FIELD_DID_TIMESTAMP, String.valueOf(didProviderBean.getTimestamp()));
                if (isUpdate == null) {
                    EulixSpaceDBUtil.insertDID(context, didValue);
                } else {
                    EulixSpaceDBUtil.updateDID(context, didValue);
                }
            }
        }
    }

    public static void insertOrUpdateDIDWithPasswordEncryptPrivateKey(Context context, DIDProviderBean didProviderBean, String passwordEncryptPrivateKey) {
        String boxUuid = null;
        String boxBind = null;
        String aoId = null;
        String didDoc = null;
        String didDocDecode = null;
        DIDReserveBean didReserveBean = null;
        if (didProviderBean != null) {
            boxUuid = didProviderBean.getBoxUuid();
            boxBind = didProviderBean.getBoxBind();
            aoId = didProviderBean.getAoId();
            didDoc = didProviderBean.getDidDoc();
            didDocDecode = didProviderBean.getDidDocDecode();
        }
        if (context != null && boxUuid != null && boxBind != null) {
            Boolean isUpdate = null;
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_DID_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_DID_BIND, boxBind);
            List<Map<String, String>> didValues = EulixSpaceDBUtil.queryDID(context, queryMap);
            if (didValues != null && !didValues.isEmpty()) {
                isUpdate = true;
                for (Map<String, String> didV : didValues) {
                    if (didV != null && didV.containsKey(EulixSpaceDBManager.FIELD_DID_DOC_ENCODE)
                            && didV.containsKey(EulixSpaceDBManager.FIELD_DID_DOCUMENT)) {
                        String currentPasswordEncryptPrivateKey = null;
                        String currentDIDReserveBeanValue = null;
                        if (didV.containsKey(EulixSpaceDBManager.FIELD_DID_RESERVE)) {
                            currentDIDReserveBeanValue = didV.get(EulixSpaceDBManager.FIELD_DID_RESERVE);
                        }
                        if (StringUtil.isNonBlankString(currentDIDReserveBeanValue)) {
                            try {
                                didReserveBean = new Gson().fromJson(currentDIDReserveBeanValue, DIDReserveBean.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                        }
                        if (didReserveBean != null) {
                            currentPasswordEncryptPrivateKey = didReserveBean.getPasswordEncryptedPriKeyBytes();
                        }
                        isUpdate = !(StringUtil.compare(didDoc, didV.get(EulixSpaceDBManager.FIELD_DID_DOC_ENCODE))
                                && StringUtil.compare(didDocDecode, didV.get(EulixSpaceDBManager.FIELD_DID_DOCUMENT))
                                && StringUtil.compare(passwordEncryptPrivateKey, currentPasswordEncryptPrivateKey));
                        break;
                    }
                }
            }
            Logger.d(TAG, "did update: " + isUpdate);
            Logger.d(TAG, "password encrypt private key: " + passwordEncryptPrivateKey);
            if (isUpdate == null || isUpdate) {
                Map<String, String> didValue = new HashMap<>();
                didValue.put(EulixSpaceDBManager.FIELD_DID_UUID, boxUuid);
                didValue.put(EulixSpaceDBManager.FIELD_DID_BIND, boxBind);
                if (aoId != null) {
                    didValue.put(EulixSpaceDBManager.FIELD_DID_AO_ID, aoId);
                }
                didValue.put(EulixSpaceDBManager.FIELD_DID_DOC_ENCODE, StringUtil.nullToEmpty(didDoc));
                didValue.put(EulixSpaceDBManager.FIELD_DID_DOCUMENT, StringUtil.nullToEmpty(didDocDecode));
                if (didReserveBean == null) {
                    didReserveBean = new DIDReserveBean();
                }
                didReserveBean.setPasswordEncryptedPriKeyBytes(passwordEncryptPrivateKey);
                didValue.put(EulixSpaceDBManager.FIELD_DID_RESERVE, new Gson().toJson(didReserveBean, DIDReserveBean.class));
                didValue.put(EulixSpaceDBManager.FIELD_DID_TIMESTAMP, String.valueOf(didProviderBean.getTimestamp()));
                if (isUpdate == null) {
                    EulixSpaceDBUtil.insertDID(context, didValue);
                } else {
                    EulixSpaceDBUtil.updateDID(context, didValue);
                }
            }
        }
    }
}
