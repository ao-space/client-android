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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.EulixBoxInfo;
import xyz.eulix.space.bean.EulixBoxOtherInfo;
import xyz.eulix.space.bean.EulixDeviceManageInfo;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.bean.SecurityPasswordInfo;
import xyz.eulix.space.bean.bind.InitResponseNetwork;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.manager.EulixSpaceDBBoxManager;
import xyz.eulix.space.network.developer.DevelopOptionsSwitchInfo;
import xyz.eulix.space.network.developer.DevelopOptionsUtil;
import xyz.eulix.space.network.developer.GetDevelopOptionsSwitchCallback;
import xyz.eulix.space.network.developer.PostDevelopOptionsSwitchCallback;
import xyz.eulix.space.util.AOSpaceUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.GatewayUtils;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/10/17 11:28
 */
public class DeveloperOptionsPresenter extends AbsPresenter<DeveloperOptionsPresenter.IDeveloperOptions> {
    public interface IDeveloperOptions extends IBaseView {
        void getDeveloperOptionsSwitchCallback(int code, String source, DevelopOptionsSwitchInfo result);
        void setDeveloperOptionsSwitchCallback(int code, String source, boolean isOn, Boolean result);
    }

    public int getIdentity() {
        return EulixSpaceDBUtil.getActiveDeviceUserIdentity(context);
    }

    public DeviceAbility getActiveDeviceAbility() {
        return EulixSpaceDBUtil.getActiveDeviceAbility(context, true);
    }

    public boolean isInternetAccessEnable() {
        boolean isEnable = true;
        EulixBoxBaseInfo eulixBoxBaseInfo = EulixSpaceDBUtil.getActiveBoxBaseInfo(context);
        if (eulixBoxBaseInfo != null) {
            String boxUuid = eulixBoxBaseInfo.getBoxUuid();
            String boxBind = eulixBoxBaseInfo.getBoxBind();
            if (boxUuid != null && boxBind != null) {
                isEnable = AOSpaceUtil.isInternetAccessEnable(context, boxUuid, boxBind);
            }
        }
        return isEnable;
    }

    public void getDeveloperOptionsSwitch(boolean isFore) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context, true);
        if (gatewayCommunicationBase != null) {
            DevelopOptionsUtil.getDevelopOptionsSwitch(gatewayCommunicationBase.getAccessToken()
                    , gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getIvParams()
                    , isFore, new GetDevelopOptionsSwitchCallback() {
                        @Override
                        public void onSuccess(int code, String source, String message, String requestId, DevelopOptionsSwitchInfo result) {
                            if (iView != null) {
                                iView.getDeveloperOptionsSwitchCallback(code, source, result);
                            }
                        }

                        @Override
                        public void onFailed(int code, String source, String message, String requestId) {
                            if (iView != null) {
                                iView.getDeveloperOptionsSwitchCallback(code, source, null);
                            }
                        }

                        @Override
                        public void onError(String errMsg) {
                            if (iView != null) {
                                iView.getDeveloperOptionsSwitchCallback(ConstantField.SERVER_EXCEPTION_CODE, null, null);
                            }
                        }
                    });
        } else if (iView != null) {
            iView.getDeveloperOptionsSwitchCallback(-1, null, null);
        }
    }

    public void setDeveloperOptionsSwitch(boolean isOn) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context, true);
        if (gatewayCommunicationBase != null) {
            DevelopOptionsUtil.postDevelopOptionsSwitch(isOn, gatewayCommunicationBase.getAccessToken()
                    , gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getIvParams()
                    , new PostDevelopOptionsSwitchCallback() {
                        @Override
                        public void onSuccess(int code, String source, String message, String requestId, Boolean result) {
                            if (iView != null) {
                                iView.setDeveloperOptionsSwitchCallback(code, source, isOn, result);
                            }
                        }

                        @Override
                        public void onFailed(int code, String source, String message, String requestId) {
                            if (iView != null) {
                                iView.setDeveloperOptionsSwitchCallback(code, source, isOn, null);
                            }
                        }

                        @Override
                        public void onError(String errMsg) {
                            if (iView != null) {
                                iView.setDeveloperOptionsSwitchCallback(500, null, isOn, null);
                            }
                        }
                    });
        } else if (iView != null) {
            iView.setDeveloperOptionsSwitchCallback(-1, null, isOn, null);
        }
    }

    public long getSecurityPasswordPermitTimestamp() {
        long timestamp = -1L;
        EulixBoxInfo eulixBoxInfo = EulixSpaceDBUtil.getActiveBoxInfo(context);
        if (eulixBoxInfo != null) {
            SecurityPasswordInfo securityPasswordInfo = eulixBoxInfo.getSecurityPasswordInfo();
            if (securityPasswordInfo != null) {
                List<Long> verificationDenyTimestamp = securityPasswordInfo.getVerificationDenyTimestamp();
                if (verificationDenyTimestamp != null && verificationDenyTimestamp.size() >= 3) {
                    Long lastTimestamp = null;
                    for (Long denyTimestamp : verificationDenyTimestamp) {
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

    public void clearSecurityPasswordVerificationDenyTimestamp() {
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
                    List<Long> verificationDenyTimestamp = securityPasswordInfo.getVerificationDenyTimestamp();
                    if (verificationDenyTimestamp != null && verificationDenyTimestamp.size() > 2) {
                        securityPasswordInfo.setVerificationDenyTimestamp(null);
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

    public EulixDeviceManageInfo getActiveManageInfo() {
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
            manageInfo.setOnline(true);
        }
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)) {
                    manageInfo.setBoxUuid(boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID));
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
                                manageInfo.setTotalSize(boxInfo.getTotalSize());
                                manageInfo.setUsedSize(boxInfo.getUsedSize());
                                List<String> wifiSsids = null;
                                List<String> ipAddresses = null;
                                List<InitResponseNetwork> networks = boxInfo.getNetworks();
                                if (networks != null) {
                                    Collections.sort(networks, FormatUtil.wifiFirstComparator);
                                    wifiSsids = new ArrayList<>();
                                    ipAddresses = new ArrayList<>();
                                    for (InitResponseNetwork network : networks) {
                                        if (network != null) {
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
                                manageInfo.setBluetoothAddress(boxInfo.getBluetoothAddress());
                                manageInfo.setBluetoothId(boxInfo.getBluetoothId());
                                manageInfo.setBluetoothDeviceName(boxInfo.getBluetoothDeviceName());
                                manageInfo.setSecurityEmailInfo(boxInfo.getSecurityEmailInfo());
                            }
                        }
                    }
                    break;
                }
            }
        }
        return manageInfo;
    }
}
