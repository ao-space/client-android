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
import com.google.gson.JsonSyntaxException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.EulixBoxToken;
import xyz.eulix.space.bean.EulixBoxTokenDetail;
import xyz.eulix.space.bean.EulixDevice;
import xyz.eulix.space.bean.bind.InitResponse;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.database.EulixSpaceSharePreferenceHelper;
import xyz.eulix.space.event.BoxInsertDeleteEvent;
import xyz.eulix.space.event.BoxOnlineRequestEvent;
import xyz.eulix.space.event.SpaceChangeEvent;
import xyz.eulix.space.network.agent.AgentInfoCallback;
import xyz.eulix.space.network.agent.AuthInfoCallback;
import xyz.eulix.space.network.agent.PairInitCallback;
import xyz.eulix.space.network.agent.PairingCallback;
import xyz.eulix.space.network.agent.AgentUtil;
import xyz.eulix.space.util.AlarmUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.SystemUtil;
import xyz.eulix.space.util.ThreadPool;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/7/27 14:21
 */
public class FindDevicePresenter extends AbsPresenter<FindDevicePresenter.IFindDevice> {
    private EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper;
    private EulixDevice mEulixDevice;
    private String mBaseUrl, mBoxName, mBoxUuid, mBoxPubKey, mRegKey, mUserDomain, mMessage;
    private String mAuthKey;
    private Integer mCode;
    private long mExpireTime;

    private PairInitCallback pairInitCallback = new PairInitCallback() {
        @Override
        public void onSuccess(String code, String message, InitResponse initResponse) {
            if (initResponse != null) {
                String bleKey = initResponse.getKey();
                String bleIv = initResponse.getIv();
                if (bleKey == null) {
                    bleKey = ConstantField.QAEnvironment.BLE_KEY;
                }
                if (bleIv == null) {
                    bleIv = ConstantField.QAEnvironment.BLE_IV;
                }
                mBoxName = initResponse.getBoxName();
                mBoxUuid = initResponse.getBoxUuid();
                bindDevice(bleKey, bleIv);
            } else if (iView != null) {
                iView.pairingCallback(null, null, null, null, null, null, null);
            }
        }

        @Override
        public void onFailed() {
            if (iView != null) {
                iView.pairingCallback(null, null, null, null, null, null, null);
            }
        }

        @Override
        public void onError(String errMsg) {
            if (iView != null) {
                iView.pairingCallback(null, null, null, null, null, null, null);
            }
        }
    };

    private PairingCallback pairingCallback = new PairingCallback() {
        @Override
        public void onSuccess(Integer code, String message, String deviceAddress, String boxName, String boxUuid, String boxPubKey, String authKey, String regKey, String userDomain, int paired) {
            mCode = null;
            mMessage = null;
            if (boxUuid != null) {
                mBoxName = boxName;
                mBoxUuid = boxUuid;
                mBoxPubKey = boxPubKey;
                mRegKey = regKey;
                mUserDomain = userDomain;
                mCode = code;
                mMessage = message;
                obtainAuthKey(mBaseUrl, boxUuid);
            }
        }

        @Override
        public void onFailed(String message, Integer code) {
            if (iView != null) {
                iView.pairingCallback(null, code, "", "", "", "", "");
            }
        }

        @Override
        public void onError(String msg) {
            if (iView != null) {
                iView.pairingCallback(null, null, null, null, null, null, null);
            }
        }
    };

    private AuthInfoCallback authInfoCallback = new AuthInfoCallback() {
        @Override
        public void onSuccess(Integer code, String message, String deviceAddress, String boxUuid, String authKey) {
            Integer resultCode = (mCode == null ? code : mCode);
            boolean isBind = (resultCode != null && ConstantField.BindDeviceHttpCode.BIND_DUPLICATE_CODE == resultCode);
            if (isBind) {
                mAuthKey = authKey;
                getAgentInfo();
            } else {
                handleBoxToken(boxUuid, authKey, resultCode, false);
            }
        }

        @Override
        public void onFailed(String message, Integer code) {
            if (iView != null) {
                iView.pairingCallback(null, (mCode == null ? code : mCode), "", "", "", "", "");
            }
        }

        @Override
        public void onError(String msg) {
            if (iView != null) {
                iView.pairingCallback(null, mCode, null, null, null, null, null);
            }
        }
    };

    private AgentInfoCallback agentInfoCallback = new AgentInfoCallback() {
        @Override
        public void onSuccess(String deviceAddress, String status, String version, Boolean isClientPaired, Integer dockerStatus) {
            if (isClientPaired != null && isClientPaired) {
                handleBoxToken(mBoxUuid, mAuthKey, mCode, true);
            } else {
                if (mCode != null && mCode == ConstantField.BindDeviceHttpCode.BIND_DUPLICATE_CODE) {
                    mCode = 200;
                }
                handleBoxToken(mBoxUuid, mAuthKey, mCode, false);
            }
        }

        @Override
        public void onFailed() {
            if (mCode != null && mCode == ConstantField.BindDeviceHttpCode.BIND_DUPLICATE_CODE) {
                mCode = 200;
            }
            handleBoxToken(mBoxUuid, mAuthKey, mCode, false);
        }

        @Override
        public void onError(String msg) {
            if (mCode != null && mCode == ConstantField.BindDeviceHttpCode.BIND_DUPLICATE_CODE) {
                mCode = 200;
            }
            handleBoxToken(mBoxUuid, mAuthKey, mCode, false);
        }
    };

    public interface IFindDevice extends IBaseView {
        void pairingCallback(EulixDevice device, Integer code, String boxUuid, String boxPubKey, String authKey, String regKey, String userDomain);
        void handleAccessToken(EulixDevice device, Integer code, String boxUuid);
    }

    private void obtainAuthKey(String baseUrl, String boxUuid) {
        String clientRSAPrivateKey = null;
        if (eulixSpaceSharePreferenceHelper == null) {
            eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
        }
        if (eulixSpaceSharePreferenceHelper != null && eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PRIVATE_KEY)) {
            clientRSAPrivateKey = eulixSpaceSharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PRIVATE_KEY);
        }
        final String clientPrivateKey = clientRSAPrivateKey;
        try {
            ThreadPool.getInstance().execute(() -> AgentUtil.getAuthInfo(baseUrl, boxUuid, clientPrivateKey, null, authInfoCallback));
        } catch (RejectedExecutionException e) {
            e.printStackTrace();
        }
    }

    private void getAgentInfo() {
        ThreadPool.getInstance().execute(() -> AgentUtil.getAgentInfo(mBaseUrl, null, agentInfoCallback));
    }

    private void handleBoxToken(String boxUuid, String authKey, Integer resultCode, boolean isBind) {
        long currentTimestamp = System.currentTimeMillis();
        Long expireTimestamp = null;
        if (boxUuid != null && boxUuid.equals(mBoxUuid)) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, "1");
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context.getApplicationContext(), queryMap);
            Map<String, String> boxValue = new HashMap<>();
            boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            boxValue.put(EulixSpaceDBManager.FIELD_BOX_NAME, mBoxName);
            boxValue.put(EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY, StringUtil.unwrapPublicKey(mBoxPubKey));
            boxValue.put(EulixSpaceDBManager.FIELD_BOX_AUTHORIZATION, authKey);
            boxValue.put(EulixSpaceDBManager.FIELD_BOX_REGISTER, mRegKey);
            boxValue.put(EulixSpaceDBManager.FIELD_BOX_DOMAIN, mUserDomain);
            boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, String.valueOf(1));
            boxValue.put(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME, String.valueOf(currentTimestamp));
            if (boxValues == null || boxValues.size() <= 0) {
                boxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.BINDING));
                EulixSpaceDBUtil.insertBox(context.getApplicationContext(), boxValue, 1);
                BoxInsertDeleteEvent boxInsertDeleteEvent = new BoxInsertDeleteEvent(boxUuid, "1", true);
                EventBusUtil.post(boxInsertDeleteEvent);
            } else {
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
                        break;
                    }
                }
                EulixSpaceDBUtil.updateBox(context.getApplicationContext(), boxValue);
            }
        }
        if (expireTimestamp == null || expireTimestamp < (currentTimestamp + 10 * 1000)) {
            if (isBind) {
                mAuthKey = authKey;
                if (iView != null) {
                    iView.handleAccessToken(mEulixDevice, resultCode, boxUuid);
                }
            } else {
                if (iView != null) {
                    iView.pairingCallback(mEulixDevice, resultCode, boxUuid, mBoxPubKey, authKey, mRegKey, mUserDomain);
                }
            }
        } else {
            if (resultCode != null && resultCode < 400) {
                changeActiveBox(boxUuid, currentTimestamp, expireTimestamp);
            }
            if (iView != null) {
                iView.pairingCallback(mEulixDevice, resultCode, boxUuid, mBoxPubKey, authKey, mRegKey, mUserDomain);
            }
        }
    }

    private void changeActiveBox(String boxUuid, long currentTimestamp, long expireTimestamp) {
        EulixSpaceDBUtil.readAppointPush(context, boxUuid, "1", true);
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
        Integer boxAlarmId = DataUtil.getTokenAlarmId(boxUuid, "1");
        if (boxAlarmId != null) {
            AlarmUtil.cancelAlarm(context, boxAlarmId);
        }
        Map<String, String> boxValue = new HashMap<>();
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_BIND, "1");
        boxValue.put(EulixSpaceDBManager.FIELD_BOX_STATUS, String.valueOf(ConstantField.EulixDeviceStatus.ACTIVE));
        boxValue.put(EulixSpaceDBManager.FILED_BOX_UPDATE_TIME, String.valueOf(currentTimestamp));
        EulixSpaceDBUtil.updateBox(context, boxValue);
        EulixBoxTokenDetail eulixBoxTokenDetail = new EulixBoxTokenDetail();
        eulixBoxTokenDetail.setBoxUuid(boxUuid);
        eulixBoxTokenDetail.setBoxBind("1");
        eulixBoxTokenDetail.setTokenExpire(expireTimestamp);
        DataUtil.setLastBoxToken(eulixBoxTokenDetail);
        DataUtil.setLastEulixSpace(context, boxUuid, "1");
        int alarmId = AlarmUtil.getAlarmId();
        DataUtil.setTokenAlarmId(boxUuid, "1", alarmId);
        long diffTimestamp = 60 * 1000L;
        if (expireTimestamp > currentTimestamp) {
            diffTimestamp = Math.min(((expireTimestamp - currentTimestamp) / 10), diffTimestamp);
            AlarmUtil.setAlarm(context, (expireTimestamp - diffTimestamp), alarmId, boxUuid, "1", (diffTimestamp / 2));
        } else {
            AlarmUtil.setAlarm(context, (currentTimestamp + diffTimestamp), alarmId, boxUuid, "1", (diffTimestamp / 2));
        }
        EulixSpaceDBUtil.offlineTemperateBox(context);
        EventBusUtil.post(new BoxOnlineRequestEvent(true));
        EventBusUtil.post(new SpaceChangeEvent(true));
    }

    public boolean initDevice(EulixDevice device) {
        boolean isInit = false;
        if (device != null) {
            mEulixDevice = device;
            String hostAddress = device.getHostAddress();
            Integer port = device.getPort();
            boolean isIpv6 = device.isIpv6();
            if (!TextUtils.isEmpty(hostAddress) && port != null) {
                EulixSpaceSharePreferenceHelper sharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
                if (sharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PUBLIC_KEY)
                        && sharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.UUID)
                        && sharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PRIVATE_KEY)) {
                    isInit = true;
                    String baseUrl;
                    if (isIpv6) {
                        baseUrl = "http://[" + hostAddress + "]:" + port + "/";
//                        baseUrl = "http://" + device.getHostName() + ".local:" + port + "/";
                    } else {
                        baseUrl = "http://" + hostAddress +  ":" + port + "/";
                    }
                    mBaseUrl = baseUrl;
                    try {
                        ThreadPool.getInstance().execute(() -> AgentUtil.pairInit(baseUrl, null, null, pairInitCallback));
                    } catch (RejectedExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return isInit;
    }

    public void bindDevice(String bleKey, String bleIv) {
        if (mEulixDevice != null) {
            String hostAddress = mEulixDevice.getHostAddress();
            Integer port = mEulixDevice.getPort();
            boolean isIpv6 = mEulixDevice.isIpv6();
            if (!TextUtils.isEmpty(hostAddress) && port != null) {
                EulixSpaceSharePreferenceHelper sharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
                if (sharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PUBLIC_KEY)
                        && sharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.UUID)
                        && sharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PRIVATE_KEY)) {
                    String baseUrl;
                    if (isIpv6) {
                        baseUrl = "http://[" + hostAddress + "]:" + port + "/";
                    } else {
                        baseUrl = "http://" + hostAddress +  ":" + port + "/";
                    }
                    mBaseUrl = baseUrl;
                    try {
                        ThreadPool.getInstance().execute(() -> AgentUtil.pairingEnc(baseUrl
                                , sharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PUBLIC_KEY)
                                , sharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.UUID)
                                , sharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PRIVATE_KEY)
                                , SystemUtil.getPhoneModel(), bleKey, bleIv, null, 0, pairingCallback));
                    } catch (RejectedExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public boolean bindDevice(EulixDevice device) {
        boolean isPairing = false;
        if (device != null) {
            mEulixDevice = device;
            String hostAddress = device.getHostAddress();
            Integer port = device.getPort();
            boolean isIpv6 = device.isIpv6();
            if (!TextUtils.isEmpty(hostAddress) && port != null) {
                EulixSpaceSharePreferenceHelper sharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(context);
                if (sharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PUBLIC_KEY)
                        && sharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.UUID)
                        && sharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PRIVATE_KEY)) {
                    isPairing = true;
                    String baseUrl;
                    if (isIpv6) {
                        baseUrl = "http://[" + hostAddress + "]:" + port + "/";
                    } else {
                        baseUrl = "http://" + hostAddress +  ":" + port + "/";
                    }
                    mBaseUrl = baseUrl;
                    try {
                        ThreadPool.getInstance().execute(() -> AgentUtil.pairingEnc(baseUrl
                                , sharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PUBLIC_KEY)
                                , sharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.UUID)
                                , sharePreferenceHelper.getString(ConstantField.EulixSpaceSPKey.CLIENT_RSA_2048_PRIVATE_KEY)
                                , SystemUtil.getPhoneModel(), ConstantField.QAEnvironment.BLE_KEY
                                , ConstantField.QAEnvironment.BLE_IV, null, 0, pairingCallback));
                    } catch (RejectedExecutionException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return isPairing;
    }

    public boolean handleDBChange() {
        boolean result = false;
        Map<String, String> queryMap = new HashMap<>();
        queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, mBoxUuid);
        queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, "1");
        List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, queryMap);
        if (boxValues != null) {
            for (Map<String, String> boxValue : boxValues) {
                if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_TOKEN)) {
                    String boxTokenValue = boxValue.get(EulixSpaceDBManager.FIELD_BOX_TOKEN);
                    if (boxTokenValue != null) {
                        EulixBoxToken eulixBoxToken = null;
                        try {
                            eulixBoxToken = new Gson().fromJson(boxTokenValue, EulixBoxToken.class);
                        } catch (JsonSyntaxException e) {
                            e.printStackTrace();
                        }
                        if (eulixBoxToken != null) {
                            mExpireTime = eulixBoxToken.getTokenExpire();
                            result = (mExpireTime > System.currentTimeMillis());
                        }
                    }
                    break;
                }
            }
        }
        return result;
    }

    public void handleDBResult(Integer code) {
        changeActiveBox(mBoxUuid, System.currentTimeMillis(), mExpireTime);
        if (iView != null) {
            iView.pairingCallback(mEulixDevice, code, mBoxUuid, mBoxPubKey, mAuthKey, mRegKey, mUserDomain);
        }
    }
}
