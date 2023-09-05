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

import android.net.Uri;
import android.text.TextUtils;
import android.util.Patterns;
import android.webkit.URLUtil;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.AOSpaceAccessBean;
import xyz.eulix.space.bean.EulixBoxInfo;
import xyz.eulix.space.bean.EulixBoxToken;
import xyz.eulix.space.bean.EulixDeviceManageInfo;
import xyz.eulix.space.bean.EulixUser;
import xyz.eulix.space.bean.SpaceStatusStatusLineInfo;
import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.bean.bind.InitResponseNetwork;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.event.BoxInsertDeleteEvent;
import xyz.eulix.space.network.gateway.AlgorithmConfig;
import xyz.eulix.space.network.gateway.AuthAutoLoginCallback;
import xyz.eulix.space.network.gateway.GatewayUtil;
import xyz.eulix.space.network.gateway.TransportationConfig;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FormatUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/4/25 16:14
 */
public class EulixDeviceListPresenter extends AbsPresenter<EulixDeviceListPresenter.IEulixDeviceList> {
    private AuthAutoLoginCallback authAutoLoginCallback = new AuthAutoLoginCallback() {
        @Override
        public void onSuccess(String boxUuid, String boxBind, int code, int httpCode, String accessToken, String refreshToken, AlgorithmConfig algorithmConfig, String encryptedSecret, String expiresAt, Long expiresAtEpochSeconds, String autoLoginExpiresAt) {
            long expireTimestamp = FormatUtil.parseFileApiTimestamp(expiresAt, ConstantField.TimeStampFormat.FILE_API_DAY_FORMAT, ConstantField.TimeStampFormat.FILE_API_SPLIT);
            if (code >= 200 && code < 400) {
                String transformation = null;
                String ivParams = null;
                if (algorithmConfig != null) {
                    TransportationConfig transportationConfig = algorithmConfig.getTransportation();
                    if (transportationConfig != null) {
                        transformation = transportationConfig.getTransformation();
                        ivParams = transportationConfig.getInitializationVector();
                    }
                }
                Map<String, String> boxValue = new HashMap<>();
                if (boxUuid != null && boxBind != null) {
                    boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                    boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                    String boxTokenValue = null;
                    Map<String, String> queryMap = new HashMap<>();
                    queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                    queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                    List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, queryMap);
                    if (boxValues != null) {
                        for (Map<String, String> boxV : boxValues) {
                            if (boxV != null && boxV.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
                                boxTokenValue = boxV.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                                break;
                            }
                        }
                    }
                    EulixBoxToken boxToken = null;
                    if (boxTokenValue != null) {
                        try {
                            boxToken = new Gson().fromJson(boxTokenValue, EulixBoxToken.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                    }
                    if (boxToken == null) {
                        boxToken = new EulixBoxToken();
                    }
                    if (!TextUtils.isEmpty(accessToken)) {
                        boxToken.setAccessToken(accessToken);
                    }
                    if (!TextUtils.isEmpty(encryptedSecret)) {
                        boxToken.setSecretKey(encryptedSecret);
                    }
                    if (!TextUtils.isEmpty(expiresAt)) {
                        boxToken.setTokenExpire(expireTimestamp);
                    }
                    if (!TextUtils.isEmpty(refreshToken)) {
                        boxToken.setRefreshToken(refreshToken);
                    }
                    if (!TextUtils.isEmpty(ivParams)) {
                        boxToken.setInitializationVector(ivParams);
                    }
                    if (!TextUtils.isEmpty(transformation)) {
                        boxToken.setTransformation(transformation);
                    }
                    if (autoLoginExpiresAt != null) {
                        long autoLoginExpireTimestamp = FormatUtil.parseFileApiTimestamp(autoLoginExpiresAt, ConstantField.TimeStampFormat.FILE_API_DAY_FORMAT, ConstantField.TimeStampFormat.FILE_API_SPLIT);
                        if (autoLoginExpireTimestamp >= 0) {
                            boxToken.setLoginValid(String.valueOf(autoLoginExpireTimestamp));
                        }
                    }
                    boxValue.put(EulixSpaceDBManager.FIELD_BOX_TOKEN, new Gson().toJson(boxToken, EulixBoxToken.class));
                    EulixSpaceDBUtil.updateBox(context, boxValue);
                }
            }
            if (iView != null) {
                iView.authAutoLoginResult(boxUuid, boxBind, code, expireTimestamp);
            }
        }

        @Override
        public void onFailed(String boxUuid, String boxBind, int code, int httpCode) {
            if (iView != null) {
                iView.authAutoLoginResult(boxUuid, boxBind, code, -1);
            }
        }

        @Override
        public void onError(String boxUuid, String boxBind, int code, String errMsg) {
            if (iView != null) {
                iView.authAutoLoginResult(boxUuid, boxBind, code, -1);
            }
        }
    };

    public interface IEulixDeviceList extends IBaseView {
        void authAutoLoginResult(String boxUuid, String boxBind, int code, long expireTimestamp);
    }

    public AOSpaceAccessBean getSpecificAOSpaceAccessBean(String boxUuid, String boxBind) {
        return EulixSpaceDBUtil.getSpecificAOSpaceBean(context, boxUuid, boxBind);
    }

    public EulixUser getEulixUser(String boxUuid, String boxBind) {
        EulixUser eulixUser = null;
        if (boxUuid != null && boxBind != null) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            List<UserInfo> userInfoList = EulixSpaceDBUtil.getGranterUserInfoList(context, queryMap);
            AOSpaceAccessBean aoSpaceAccessBean = EulixSpaceDBUtil.getSpecificAOSpaceBean(context, boxUuid, boxBind);
            Boolean isInternetAccess = null;
            if (aoSpaceAccessBean != null) {
                isInternetAccess = aoSpaceAccessBean.getInternetAccess();
            }
            if (userInfoList != null && userInfoList.size() == 1) {
                UserInfo userInfo = userInfoList.get(0);
                if (userInfo != null) {
                    eulixUser = new EulixUser();
                    eulixUser.setUuid(boxUuid);
                    eulixUser.setBind(boxBind);
                    eulixUser.setAvatarPath(userInfo.getAvatarPath());
                    eulixUser.setNickName(userInfo.getNickName());
                    eulixUser.setUserDomain(userInfo.getUserDomain());
                    eulixUser.setInternetAccess(isInternetAccess);
                }
            }
        }
        return eulixUser;
    }

    public void authAutoLoginPoll(String boxUuid, String boxBind) {
        if (boxUuid != null && boxBind != null) {
            String boxDomain = null;
            String boxPublicKey = null;
            String boxTokenValue = null;
            String refreshToken = null;
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, queryMap);
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_DOMAIN)
                            && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY)
                            && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
                        boxDomain = boxValue.get(EulixSpaceDBManager.FIELD_BOX_DOMAIN);
                        boxPublicKey = boxValue.get(EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY);
                        boxTokenValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                        break;
                    }
                }
            }
            if (boxTokenValue != null) {
                EulixBoxToken eulixBoxToken = null;
                try {
                    eulixBoxToken = new Gson().fromJson(boxTokenValue, EulixBoxToken.class);
                } catch (JsonSyntaxException e) {
                    e.printStackTrace();
                }
                if (eulixBoxToken != null) {
                    refreshToken = eulixBoxToken.getRefreshToken();
                }
            }
            if (boxDomain != null && boxPublicKey != null && refreshToken != null) {
                GatewayUtil.authAutoLogin(context, boxUuid, boxBind, boxDomain, boxPublicKey, refreshToken, true, false, true, authAutoLoginCallback);
            }
        }
    }

    public void deleteLoginInvalidSpace(String boxUuid, String boxBind) {
        if (boxUuid != null && boxBind != null && !"1".equals(boxBind) && !"-1".equals(boxBind)) {
            DataUtil.boxUnavailable(boxUuid, boxBind);
            Map<String, String> boxValue = new HashMap<>();
            boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            EulixSpaceDBUtil.deleteBox(context, boxValue);
            BoxInsertDeleteEvent boxInsertDeleteEvent = new BoxInsertDeleteEvent(boxUuid, boxBind, false);
            EventBusUtil.post(boxInsertDeleteEvent);
        }
    }

    public void deleteBox(String boxUuid, String boxBind) {
        DataUtil.boxUnavailable(boxUuid, boxBind);
        Map<String, String> deleteMap = new HashMap<>();
        deleteMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
        deleteMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
        EulixSpaceDBUtil.deleteBox(context, deleteMap);
    }

    public int getSpaceStatus(String boxUuid, String boxBind) {
        return EulixSpaceDBUtil.getDeviceStatus(context, boxUuid, boxBind);
    }

    public int getSpaceStatusResponseCode(String boxUuid, String boxBind) {
        int code = -1;
        if (boxUuid != null && boxBind != null) {
            EulixBoxInfo eulixBoxInfo = EulixSpaceDBUtil.getSpecificBoxInfo(context, boxUuid, boxBind);
            if (eulixBoxInfo != null) {
                SpaceStatusStatusLineInfo spaceStatusStatusLineInfo = eulixBoxInfo.getSpaceStatusStatusLineInfo();
                if (spaceStatusStatusLineInfo != null) {
                    code = spaceStatusStatusLineInfo.getCode();
                }
            }
        }
        return code;
    }

    public EulixDeviceManageInfo getManageInfo(String boxUuid, String boxBind) {
        EulixDeviceManageInfo manageInfo = new EulixDeviceManageInfo();
        if (boxUuid != null && boxBind != null) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, queryMap);
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_UUID)) {
                        manageInfo.setBoxUuid(boxValue.get(EulixSpaceDBManager.FIELD_BOX_UUID));
                        if (boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_BIND)) {
                            manageInfo.setBoxBind(boxValue.get(EulixSpaceDBManager.FIELD_BOX_BIND));
                        }
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
                                }
                            }
                        }
                        break;
                    }
                }
            }
        }
        return manageInfo;
    }

    public boolean checkIsBindQrValid(String qrCodeResult) {
        boolean isQrValid = false;
        if (qrCodeResult != null && (Patterns.WEB_URL.matcher(qrCodeResult).matches() || URLUtil.isValidUrl(qrCodeResult))) {
            Uri uri = Uri.parse(qrCodeResult);
            if (uri != null) {
                String sn = null;
                String ipAddress = null;
                String portValue = null;
                int port = -1;
                Set<String> querySet = uri.getQueryParameterNames();
                if (querySet != null) {
                    for (String query : querySet) {
                        if (ConstantField.SN.equals(query)) {
                            sn = uri.getQueryParameter(ConstantField.SN);
                        }
                        if (ConstantField.IPADDR.equals(query)) {
                            ipAddress = uri.getQueryParameter(ConstantField.IPADDR);
                            String decodeIpAddress = null;
                            if (ipAddress != null) {
                                try {
                                    decodeIpAddress = URLDecoder.decode(ipAddress, "UTF-8");
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }
                            if (decodeIpAddress != null) {
                                ipAddress = decodeIpAddress;
                            }
                        }
                        if (ConstantField.PORT.equals(query)) {
                            portValue = uri.getQueryParameter(ConstantField.PORT);
                        }
                    }
                }
                if (portValue != null) {
                    try {
                        port = Integer.parseInt(portValue);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }

                if (sn != null && ipAddress != null && port >= 0) {
                    isQrValid = true;
                }
            }
        }
        return isQrValid;
    }

}
