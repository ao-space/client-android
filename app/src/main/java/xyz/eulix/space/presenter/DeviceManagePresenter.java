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

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.bean.DeviceInfo;
import xyz.eulix.space.bean.DeviceVersionInfoBean;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.EulixBoxInfo;
import xyz.eulix.space.bean.EulixBoxOtherInfo;
import xyz.eulix.space.bean.EulixDeviceManageInfo;
import xyz.eulix.space.bean.EulixTerminal;
import xyz.eulix.space.bean.EulixUser;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.bean.NetworkAccessBean;
import xyz.eulix.space.bean.SecurityPasswordInfo;
import xyz.eulix.space.bean.TerminalInfo;
import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.bean.bind.InitResponseNetwork;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.event.BoxInsertDeleteEvent;
import xyz.eulix.space.event.BoxVersionDetailInfoEvent;
import xyz.eulix.space.event.DeviceAbilityRequestEvent;
import xyz.eulix.space.interfaces.ResultCallbackObj;
import xyz.eulix.space.manager.EulixSpaceDBBoxManager;
import xyz.eulix.space.network.agent.disk.DiskManageInfo;
import xyz.eulix.space.network.agent.disk.DiskManageListResult;
import xyz.eulix.space.network.upgrade.UpgradeUtils;
import xyz.eulix.space.network.userinfo.RevokeResultExtensionCallback;
import xyz.eulix.space.network.userinfo.TerminalOfflineCallback;
import xyz.eulix.space.network.userinfo.TerminalOfflineResult;
import xyz.eulix.space.network.userinfo.UserInfoUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.GatewayUtils;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.PreferenceUtil;

/**
 * Author:      Zhu Fuyu
 * Description:
 * History:     2021/8/23
 */
public class DeviceManagePresenter extends AbsPresenter<DeviceManagePresenter.IDeviceManage> {
    public DeviceVersionInfoBean deviceVersionInfoBean;

    private TerminalOfflineCallback terminalOfflineCallback = new TerminalOfflineCallback() {
        @Override
        public void onSuccess(int code, String message, String requestId, TerminalOfflineResult results, String customizeSource) {
            if (iView != null) {
                iView.handleTerminalOffline(code, customizeSource);
            }
        }

        @Override
        public void onFail(int code, String message, String requestId) {
            if (iView != null) {
                iView.handleTerminalOffline(code, "");
            }
        }

        @Override
        public void onError(String errMsg) {
            if (iView != null) {
                iView.handleTerminalOffline(ConstantField.SERVER_EXCEPTION_CODE, "");
            }
        }
    };

    public interface IDeviceManage extends IBaseView {
        void onRevokeResult(Boolean result, String msg);

        void refreshDeviceInfoViews(DeviceVersionInfoBean deviceVersionInfoBean);

        void handleTerminalOffline(int code, String customizeSource);
    }

    public EulixBoxBaseInfo getActiveBoxUuid() {
        return EulixSpaceDBUtil.getActiveBoxBaseInfo(context);
    }

    public boolean isPhysicalDevice() {
        return isPhysicalDevice(EulixSpaceDBUtil.getActiveDeviceAbility(context, true));
    }

    public EulixDeviceManageInfo getActiveManageInfo() {
        boolean isConnect = false;
        EulixDeviceManageInfo manageInfo = new EulixDeviceManageInfo();
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context
                , EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        if (boxValues == null || boxValues.size() <= 0) {
            boxValues = EulixSpaceDBUtil.queryBox(context, EulixSpaceDBManager.FIELD_BOX_STATUS
                    , String.valueOf(ConstantField.EulixDeviceStatus.OFFLINE_USE));
            if (boxValues == null || boxValues.size() <= 0) {
                manageInfo.setOnline(null);
            } else {
                manageInfo.setOnline(false);
            }
        } else {
            isConnect = true;
            manageInfo.setOnline(true);
        }
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)
                        && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                    manageInfo.setBoxUuid(boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID));
                    String boxBind = boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND);
                    manageInfo.setBoxBind(boxBind);
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_NAME)) {
                        manageInfo.setBoxName(boxValue.get(EulixSpaceDBManager.FIELD_BOX_NAME));
                    }
                    if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_INFO)) {
                        String boxInfoValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_INFO);
                        if (boxInfoValue != null) {
                            EulixBoxInfo boxInfo = null;
                            try {
                                boxInfo = new Gson().fromJson(boxInfoValue, EulixBoxInfo.class);
                            } catch (JsonSyntaxException e) {
                                e.printStackTrace();
                            }
                            if (boxInfo != null) {
                                if (isPhysicalDevice(DeviceAbility.generateDefault(boxInfo.getDeviceAbility()))) {
                                    manageInfo.setTotalSize(boxInfo.getTotalSize());
                                    manageInfo.setUsedSize(boxInfo.getUsedSize());
                                } else {
                                    boolean isGranter = ("1".equals(boxBind) || "-1".equals(boxBind));
                                    UserInfo userInfo = DataUtil.getSpecificUserInfo(boxValue, (isGranter
                                            ? DataUtil.getClientUuid(context) : boxBind), isGranter);
                                    if (userInfo != null) {
                                        manageInfo.setTotalSize(userInfo.getTotalSize());
                                        manageInfo.setUsedSize(userInfo.getUsedSize());
                                    }
                                }
                                List<String> wifiSsids = null;
                                List<String> ipAddresses = null;
                                List<NetworkAccessBean> networkAccessBeans = null;
                                List<InitResponseNetwork> networks = boxInfo.getNetworks();
                                if (networks != null) {
                                    Collections.sort(networks, FormatUtil.wireFirstComparator);
                                    wifiSsids = new ArrayList<>();
                                    ipAddresses = new ArrayList<>();
                                    networkAccessBeans = new ArrayList<>();
                                    for (InitResponseNetwork network : networks) {
                                        if (network != null) {
                                            String networkWifiName = network.getWifiName();
                                            NetworkAccessBean networkAccessBean = new NetworkAccessBean();
                                            networkAccessBean.setConnect(isConnect);
                                            networkAccessBean.setWired(network.isWire());
                                            networkAccessBean.setNetworkName(networkWifiName);
                                            networkAccessBean.setShowDetail(false);
                                            networkAccessBeans.add(networkAccessBean);
                                            String ssid = network.getWifiName();
                                            String address = network.getIp();
                                            if (ssid != null && address != null) {
                                                wifiSsids.add(ssid);
                                                ipAddresses.add(address);
                                            }
                                        }
                                    }
                                }
                                manageInfo.setNetworkSsids(wifiSsids);
                                manageInfo.setNetworkIpAddresses(ipAddresses);
                                manageInfo.setNetworkAccessBeans(networkAccessBeans);
                                manageInfo.setBluetoothAddress(boxInfo.getBluetoothAddress());
                                manageInfo.setBluetoothId(boxInfo.getBluetoothId());
                                manageInfo.setBluetoothDeviceName(boxInfo.getBluetoothDeviceName());
                            }
                        }
                    }
                    break;
                }
            }
        }
        return manageInfo;
    }

    //设备解绑
    public void revokeDevice() {

        UserInfoUtil.revokeMember(context, isActiveUserAdmin(), "", new RevokeResultExtensionCallback() {
            @Override
            public void onSuccess(String source, int code, String boxUuidValue, String boxBindValue, String extraMsg, boolean isSuccess) {
                if (isSuccess) {
                    //解绑成功
                    Logger.d("zfy", "revoke member success, box uuid: " + boxUuidValue + ", bind: " + boxBindValue);
                    //删除盒子数据
                    if (boxUuidValue != null && boxBindValue != null) {
                        DataUtil.boxUnavailable(boxUuidValue, boxBindValue);
                        Map<String, String> deleteMap = new HashMap<>();
                        deleteMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuidValue);
                        deleteMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBindValue);
                        EulixSpaceDBUtil.deleteBox(context.getApplicationContext(), deleteMap);
                        BoxInsertDeleteEvent boxInsertDeleteEvent = new BoxInsertDeleteEvent(boxUuidValue, boxBindValue, false);
                        EventBusUtil.post(boxInsertDeleteEvent);
                    }
                } else {
                    //解绑失败
                    Logger.d("revoke member failed:" + extraMsg);
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    iView.onRevokeResult(isSuccess, "");
                });
            }

            @Override
            public void onFailed(String source, int code, String boxUuidValue, String boxBindValue, String extraMsg) {
                //解绑失败
                Logger.d("revoke member failed:" + extraMsg);
                new Handler(Looper.getMainLooper()).post(() -> {
                    iView.onRevokeResult(false, "");
                });
            }

            @Override
            public void onError(String errMsg) {
                //解绑失败
                Logger.d("revoke member failed:" + errMsg);
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (!TextUtils.isEmpty(errMsg) && errMsg.contains(String.valueOf(ConstantField.ErrorCode.SPACE_PLATFORM_CONNECT_ERROR))) {
                        iView.onRevokeResult(false, String.valueOf(ConstantField.ErrorCode.SPACE_PLATFORM_CONNECT_ERROR));
                    } else {
                        iView.onRevokeResult(null, "");
                    }
                });
            }
        });
    }

    public boolean isActiveUserAdmin() {
        return (ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY == EulixSpaceDBUtil.getActiveDeviceUserIdentity(context));
    }

    public boolean isActiveGranter() {
        int identity = EulixSpaceDBUtil.getActiveDeviceUserIdentity(context);
        return (ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY == identity || ConstantField.UserIdentity.MEMBER_IDENTITY == identity);
    }

    public boolean isActiveAdministrator() {
        int identity = EulixSpaceDBUtil.getActiveDeviceUserIdentity(context);
        return (ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY == identity || ConstantField.UserIdentity.ADMINISTRATOR_GRANTEE == identity);
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

    public long getSecurityPasswordPermitTimestamp() {
        long timestamp = -1L;
        EulixBoxInfo eulixBoxInfo = EulixSpaceDBUtil.getActiveBoxInfo(context);
        if (eulixBoxInfo != null) {
            SecurityPasswordInfo securityPasswordInfo = eulixBoxInfo.getSecurityPasswordInfo();
            if (securityPasswordInfo != null) {
                List<Long> revokeDenyTimestamp = securityPasswordInfo.getRevokeDenyTimestamp();
                if (revokeDenyTimestamp != null && revokeDenyTimestamp.size() >= 3) {
                    Long lastTimestamp = null;
                    for (Long denyTimestamp : revokeDenyTimestamp) {
                        if (denyTimestamp != null && (lastTimestamp == null || lastTimestamp < denyTimestamp)) {
                            lastTimestamp = denyTimestamp;
                        }
                    }
                    if (lastTimestamp != null) {
                        timestamp = lastTimestamp + ConstantField.TimeUnit.MINUTE_UNIT;
                    }
                }
            }
        }
        return timestamp;
    }

    public void clearSecurityPasswordRevokeDenyTimestamp() {
        String boxUuid = null;
        String boxBind = null;
        EulixBoxBaseInfo eulixBoxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(context);
        if (eulixBoxBaseInfo != null) {
            boxUuid = eulixBoxBaseInfo.getBoxUuid();
            boxBind = eulixBoxBaseInfo.getBoxBind();
        }
        if (boxUuid != null && boxBind != null) {
            EulixBoxInfo eulixBoxInfo = EulixSpaceDBUtil.getSpecificBoxInfo(context, boxUuid, boxBind);
            if (eulixBoxInfo != null) {
                SecurityPasswordInfo securityPasswordInfo = eulixBoxInfo.getSecurityPasswordInfo();
                if (securityPasswordInfo != null) {
                    List<Long> revokeDenyTimestamp = securityPasswordInfo.getRevokeDenyTimestamp();
                    if (revokeDenyTimestamp != null && revokeDenyTimestamp.size() > 2) {
                        securityPasswordInfo.setRevokeDenyTimestamp(null);
                        boolean isHandle = false;
                        JSONObject jsonObject = null;
                        EulixSpaceDBBoxManager eulixSpaceDBBoxManager = EulixSpaceDBBoxManager.getInstance(boxUuid, boxBind);
                        if (eulixSpaceDBBoxManager != null) {
                            isHandle = true;
                            jsonObject = new JSONObject();
                            try {
                                jsonObject.put("securityPasswordInfo", new Gson().toJson(securityPasswordInfo, SecurityPasswordInfo.class));
                            } catch (JSONException e) {
                                e.printStackTrace();
                                isHandle = false;
                            }
                        }
                        if (isHandle) {
                            int result = eulixSpaceDBBoxManager.updateBoxInfo(jsonObject, isUpdate -> {
                                // Do nothing
                            });
                        } else {
                            eulixBoxInfo.setSecurityPasswordInfo(securityPasswordInfo);
                            Map<String, String> boxV = new HashMap<>();
                            boxV.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                            boxV.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                            boxV.put(EulixSpaceDBManager.FIELD_BOX_INFO, new Gson().toJson(eulixBoxInfo, EulixBoxInfo.class));
                            EulixSpaceDBUtil.updateBox(context, boxV);
                        }
                    }
                }
            }
        }
    }

    public List<EulixTerminal> getEulixTerminalList() {
        List<EulixTerminal> eulixTerminals = new ArrayList<>();
        EulixBoxOtherInfo eulixBoxOtherInfo = EulixSpaceDBUtil.getActiveBoxOtherInfo(context);
        String myClientUuid = DataUtil.getClientUuid(context);
        String granterClientUuid = DataUtil.getCompatibleClientUuid(context);
        if (eulixBoxOtherInfo != null) {
            Map<String, TerminalInfo> terminalInfoMap = eulixBoxOtherInfo.getTerminalInfoMap();
            if (terminalInfoMap != null) {
                Set<Map.Entry<String, TerminalInfo>> entrySet = terminalInfoMap.entrySet();
                for (Map.Entry<String, TerminalInfo> entry : entrySet) {
                    if (entry != null) {
                        String uuid = entry.getKey();
                        TerminalInfo terminalInfo = entry.getValue();
                        if (uuid != null) {
                            EulixTerminal eulixTerminal = new EulixTerminal();
                            eulixTerminal.setTerminalUuid(uuid);
                            eulixTerminal.setTerminalName((terminalInfo == null ? null : terminalInfo.getName()));
                            eulixTerminal.setTerminalType((terminalInfo == null ? null : terminalInfo.getType()));
                            eulixTerminal.setTerminalPlace((terminalInfo == null ? null : terminalInfo.getPlace()));
                            eulixTerminal.setTerminalTimestamp((terminalInfo == null ? 0L : terminalInfo.getTimestamp()));
                            eulixTerminal.setMyself(uuid.equals(myClientUuid));
                            eulixTerminal.setGranter(uuid.equals(granterClientUuid));
                            eulixTerminals.add(eulixTerminal);
                        }
                    }
                }
            }
        }
        return eulixTerminals;
    }

    public EulixTerminal getGranterTerminal() {
        EulixTerminal eulixTerminal = null;
        UserInfo userInfo = EulixSpaceDBUtil.getActiveGranterUserInfo(context);
        if (userInfo != null) {
            eulixTerminal = new EulixTerminal();
            eulixTerminal.setGranter(true);
            String deviceInfoValue = userInfo.getDeviceInfo();
            if (deviceInfoValue != null) {
                DeviceInfo deviceInfo = null;
                try {
                    deviceInfo = new Gson().fromJson(deviceInfoValue, DeviceInfo.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
                if (deviceInfo != null) {
                    eulixTerminal.setTerminalName(deviceInfo.getPhoneModel());
                }
            }
            eulixTerminal.setTerminalTimestamp(userInfo.getUserCreateTimestamp());
        }
        return eulixTerminal;
    }

    public void offlineTerminal(String clientUuid) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase != null) {
            UserInfoUtil.offlineTerminal(context, gatewayCommunicationBase.getBoxUuid(), gatewayCommunicationBase.getBoxDomain()
                    , EulixSpaceDBUtil.getClientAoId(context, gatewayCommunicationBase.getBoxUuid(), gatewayCommunicationBase.getBoxBind())
                    , clientUuid, gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                    , gatewayCommunicationBase.getIvParams(), true, true, terminalOfflineCallback);
        } else if (iView != null) {
            iView.handleTerminalOffline(100, null);
        }
    }

    public DeviceAbility getActiveDeviceAbility() {
        return EulixSpaceDBUtil.getActiveDeviceAbility(context, true);
    }

    public void updateDeviceAbility() {
        String boxUuid = null;
        String boxBind = null;
        EulixBoxBaseInfo eulixBoxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(context, true);
        if (eulixBoxBaseInfo != null) {
            boxUuid = eulixBoxBaseInfo.getBoxUuid();
            boxBind = eulixBoxBaseInfo.getBoxBind();
        }
        EventBusUtil.post(new DeviceAbilityRequestEvent(boxUuid, boxBind, true));
    }

    public boolean isDiskExpand() {
        boolean isExpand = false;
        EulixBoxInfo eulixBoxInfo = EulixSpaceDBUtil.getActiveBoxInfo(context);
        if (eulixBoxInfo != null) {
            DiskManageListResult diskManageListResult = eulixBoxInfo.getDiskManageListResult();
            if (diskManageListResult != null) {
                List<DiskManageInfo> diskManageInfos = diskManageListResult.getDiskManageInfos();
                if (diskManageInfos != null) {
                    for (DiskManageInfo diskManageInfo : diskManageInfos) {
                        if (diskManageInfo != null) {
                            Integer diskException = diskManageInfo.getDiskException();
                            if (diskException != null) {
                                switch (diskException) {
                                    case DiskManageInfo.DISK_EXCEPTION_NEW_DISK_NOT_EXPAND:
                                    case DiskManageInfo.DISK_EXCEPTION_NEW_DISK_EXPANDING:
                                        isExpand = true;
                                        break;
                                    default:
                                        break;
                                }
                            }
                        }
                        if (isExpand) {
                            break;
                        }
                    }
                }
            }
        }
        return isExpand;
    }

    public EulixBoxBaseInfo getActiveBoxBaseInfo() {
        return EulixSpaceDBUtil.getActiveBoxBaseInfo(context, true);
    }
}
