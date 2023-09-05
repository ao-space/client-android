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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.EulixBoxInfo;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.bean.SecurityEmailInfo;
import xyz.eulix.space.bean.SecurityPasswordInfo;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.event.BoxInsertDeleteEvent;
import xyz.eulix.space.manager.EulixSpaceDBBoxManager;
import xyz.eulix.space.network.EulixBaseResponseExtensionCallback;
import xyz.eulix.space.network.agent.AgentUtil;
import xyz.eulix.space.network.agent.ResetCallback;
import xyz.eulix.space.network.agent.RevokeCallback;
import xyz.eulix.space.network.security.EulixSecurityUtil;
import xyz.eulix.space.network.security.SecurityTokenResult;
import xyz.eulix.space.network.security.VerifySecurityCallback;
import xyz.eulix.space.network.userinfo.RevokeResultExtensionCallback;
import xyz.eulix.space.network.userinfo.UserInfoUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.GatewayUtils;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.SystemUtil;
import xyz.eulix.space.util.ThreadPool;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/11/1 18:23
 */
public class UnbindDevicePresenter extends AbsPresenter<UnbindDevicePresenter.IUnbindDevice> {
    private String mBoxUuid;
    private ResetCallback resetCallback = new ResetCallback() {
        @Override
        public void onSuccess(String message, Integer code) {
            if (iView != null) {
                iView.handleResultCallback(code != null && code < 400);
            }
        }

        @Override
        public void onFailed(String message, Integer code) {
            if (iView != null) {
                iView.handleResultCallback(false);
            }
        }

        @Override
        public void onError(String msg) {
            if (iView != null) {
                iView.handleResultCallback(false);
            }
        }
    };

    private RevokeCallback revokeCallback = new RevokeCallback() {
        @Override
        public void onSuccess(String message, Integer code, String boxUuid, int errorTimes, int leftTryTimes, int tryAfterSeconds) {
            if (boxUuid == null || boxUuid.equalsIgnoreCase(mBoxUuid)) {
                if (iView != null) {
                    iView.handleAdminRevokeResult((code == null ? 200 : code), errorTimes, leftTryTimes, tryAfterSeconds);
                }
            } else {
                if (iView != null) {
                    iView.handleAdminRevokeResult(500, -1, -1, -1);
                }
            }
        }

        @Override
        public void onFailed(String message, Integer code) {
            if (iView != null) {
                iView.handleAdminRevokeResult(500, -1, -1, -1);
            }
        }

        @Override
        public void onError(String msg) {
            if (iView != null) {
                iView.handleAdminRevokeResult(500, -1, -1, -1);
            }
        }
    };

    public interface IUnbindDevice extends IBaseView {
        void handleResultCallback(boolean isSuccess);
        void handleAdminRevokeResult(int code, int errorTimes, int leftTryTimes, int tryAfterSeconds);
        void onRevokeResult(boolean result, int code, String source, List<Long> timestampList);
        void onVerifyResult(int code, String source, SecurityTokenResult result, List<Long> timestampList);
        void granteeApplyResult(String source, int code);
    }

    @Deprecated
    public void revoke(String password, String boxUuid, String baseUrl) {
        String boxPubKey = null;
        if (boxUuid != null) {
            mBoxUuid = boxUuid;
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, "1");
            List<Map<String, String>> boxValues = EulixSpaceDBUtil.queryBox(context, queryMap);
            if (boxValues != null) {
                for (Map<String, String> boxValue : boxValues) {
                    if (boxValue != null && boxValue.containsKey(EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY)) {
                        boxPubKey = boxValue.get(EulixSpaceDBManager.FIELD_BOX_PUBLIC_KEY);
                        break;
                    }
                }
            }
        }
        if (boxPubKey == null) {
            if (iView != null) {
                iView.handleResultCallback(false);
            }
        } else {
            ThreadPool.getInstance().execute(() -> AgentUtil.revoke(baseUrl, DataUtil.getClientUuid(context), password, null, null, revokeCallback));
        }
    }

    //设备解绑
    public void revokeDevice(String password) {
        UserInfoUtil.revokeMember(context, true, password, new RevokeResultExtensionCallback() {
            @Override
            public void onSuccess(String source, int code, String boxUuidValue, String boxBindValue, String extraMsg, boolean isSuccess) {
                if (isSuccess) {
                    //解绑成功
                    Logger.d("zfy","revoke member success, box uuid: "+ boxUuidValue + ", bind: " + boxBindValue);
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
                    if (iView != null) {
                        iView.onRevokeResult(true, code, source, null);
                    }
                } else {
                    //解绑失败
                    Logger.d("zfy","revoke member failed:" + extraMsg + ", code: " + code + ", source: " + source);
                    handleRevokeResult(boxUuidValue, boxBindValue, code, source);
                }
            }

            @Override
            public void onFailed(String source, int code, String boxUuidValue, String boxBindValue, String extraMsg) {
                //解绑失败
                Logger.d("zfy","revoke member failed:" + extraMsg + ", code: " + code + ", source: " + source);
                if (iView != null) {
                    iView.onRevokeResult(false, code, source, null);
                }
            }

            @Override
            public void onError(String errMsg) {
                //解绑失败
                Logger.d("zfy", "revoke member failed:" + errMsg);
                int errorCode = ConstantField.SERVER_EXCEPTION_CODE;
                if (!TextUtils.isEmpty(errMsg)) {
                    try {
                        errorCode = Integer.parseInt(errMsg);
                    } catch (Exception e) {
                        Logger.e(e.getMessage());
                    }
                }
                if (iView != null) {
                    iView.onRevokeResult(false, errorCode, null, null);
                }
            }
        });
    }

    private void handleRevokeResult(String boxUuid, String boxBind, int code, String source) {
        boolean isCallback = true;
        List<Long> revokeDenyTimestamp = null;
        if (boxUuid != null && boxBind != null && (code == ConstantField.KnownError.GatewayCommonError.GATEWAY_406)
                && ConstantField.KnownSource.GATEWAY.equals(source)) {
            SecurityPasswordInfo securityPasswordInfo = null;
            EulixBoxInfo eulixBoxInfo = EulixSpaceDBUtil.getSpecificBoxInfo(context, boxUuid, boxBind);
            if (eulixBoxInfo != null) {
                securityPasswordInfo = eulixBoxInfo.getSecurityPasswordInfo();
            }
            if (securityPasswordInfo == null) {
                securityPasswordInfo = new SecurityPasswordInfo();
            }
            List<Long> rawRevokeDenyTimestamp = securityPasswordInfo.getRevokeDenyTimestamp();
            if (rawRevokeDenyTimestamp == null) {
                rawRevokeDenyTimestamp = new ArrayList<>();
            }
            long currentTime = System.currentTimeMillis();
            rawRevokeDenyTimestamp.add(currentTime);
            revokeDenyTimestamp = DataUtil.getValidInputWrongTimestampList(rawRevokeDenyTimestamp, ConstantField.TimeUnit.MINUTE_UNIT);
            securityPasswordInfo.setRevokeDenyTimestamp(revokeDenyTimestamp);
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
                List<Long> finalRevokeDenyTimestamp = revokeDenyTimestamp;
                int result = eulixSpaceDBBoxManager.updateBoxInfo(jsonObject, isUpdate -> {
                    if (iView != null) {
                        iView.onRevokeResult(false, code, source, finalRevokeDenyTimestamp);
                    }
                });
                isCallback = (result >= 0);
            } else {
                if (eulixBoxInfo == null && !"1".equals(boxBind)) {
                    eulixBoxInfo = new EulixBoxInfo();
                }
                if (eulixBoxInfo != null) {
                    eulixBoxInfo.setSecurityPasswordInfo(securityPasswordInfo);
                    Map<String, String> boxV = new HashMap<>();
                    boxV.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                    boxV.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                    boxV.put(EulixSpaceDBManager.FIELD_BOX_INFO, new Gson().toJson(eulixBoxInfo, EulixBoxInfo.class));
                    EulixSpaceDBUtil.updateBox(context, boxV);
                }
            }
        }
        if (isCallback && iView != null) {
            iView.onRevokeResult(false, code, source, revokeDenyTimestamp);
        }
    }

    public int getActiveIdentity() {
        return EulixSpaceDBUtil.getActiveDeviceUserIdentity(context);
    }

    /**
     * 确保没有密保邮箱下硬件设备验证正常
     * @return 没有密保邮箱且没有bluetooth id为FALSE，其余TRUE
     */
    public boolean isOnlyHardwareDeviceCanVerify() {
        boolean result = false;
        EulixBoxInfo eulixBoxInfo = EulixSpaceDBUtil.getActiveBoxInfo(context);
        if (eulixBoxInfo != null) {
            if (eulixBoxInfo.getBluetoothId() == null) {
                SecurityEmailInfo securityEmailInfo = eulixBoxInfo.getSecurityEmailInfo();
                result = (securityEmailInfo != null && securityEmailInfo.getEmailAccount() != null);
            } else {
                result = true;
            }
        }
        return result;
    }

    public void verifySecurityPassword(String password) {
        if (password != null) {
            GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context, true);
            if (gatewayCommunicationBase != null) {
                String boxUuid = gatewayCommunicationBase.getBoxUuid();
                String boxBind = gatewayCommunicationBase.getBoxBind();
                EulixSecurityUtil.verifySecurityPassword(password, gatewayCommunicationBase.getBoxDomain()
                        , gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                        , gatewayCommunicationBase.getIvParams(), new VerifySecurityCallback() {
                            @Override
                            public void onSuccess(String source, int code, String message, String requestId, SecurityTokenResult result) {
                                handleVerifyResult(boxUuid, boxBind, code, source, result);
                            }

                            @Override
                            public void onFailed(String source, int code, String message, String requestId) {
                                handleVerifyResult(boxUuid, boxBind, code, source, null);
                            }

                            @Override
                            public void onError(String errMsg) {
                                if (iView != null) {
                                    iView.onVerifyResult(ConstantField.SERVER_EXCEPTION_CODE, null, null, null);
                                }
                            }
                        });
            } else if (iView != null) {
                iView.onVerifyResult(ConstantField.OBTAIN_ACCESS_TOKEN_CODE, null, null, null);
            }
        } else if (iView != null) {
            iView.onVerifyResult(-1, null, null, null);
        }
    }

    private void handleVerifyResult(String boxUuid, String boxBind, int code, String source, SecurityTokenResult securityTokenResult) {
        boolean isCallback = true;
        List<Long> verificationDenyTimestamp = null;
        if (boxUuid != null && boxBind != null && ((code >= 200 && code < 300) || (code == ConstantField.KnownError.AccountCommonError.ACCOUNT_403 && ConstantField.KnownSource.ACCOUNT.equals(source)))) {
            SecurityPasswordInfo securityPasswordInfo = null;
            EulixBoxInfo eulixBoxInfo = EulixSpaceDBUtil.getSpecificBoxInfo(context, boxUuid, boxBind);
            if (eulixBoxInfo != null) {
                securityPasswordInfo = eulixBoxInfo.getSecurityPasswordInfo();
            }
            if (securityPasswordInfo == null) {
                securityPasswordInfo = new SecurityPasswordInfo();
            }
            if (code >= 200 && code < 300) {
                securityPasswordInfo.setVerificationDenyTimestamp(null);
            } else {
                List<Long> rawVerificationDenyTimestamp = securityPasswordInfo.getVerificationDenyTimestamp();
                if (rawVerificationDenyTimestamp == null) {
                    rawVerificationDenyTimestamp = new ArrayList<>();
                }
                rawVerificationDenyTimestamp.add(System.currentTimeMillis());
                verificationDenyTimestamp = DataUtil.getValidInputWrongTimestampList(rawVerificationDenyTimestamp, ConstantField.TimeUnit.MINUTE_UNIT);
                securityPasswordInfo.setVerificationDenyTimestamp(verificationDenyTimestamp);
            }
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
                List<Long> finalVerificationDenyTimestamp = verificationDenyTimestamp;
                int result = eulixSpaceDBBoxManager.updateBoxInfo(jsonObject, isUpdate -> {
                    if (iView != null) {
                        iView.onVerifyResult(code, source, securityTokenResult, finalVerificationDenyTimestamp);
                    }
                });
                isCallback = (result >= 0);
            } else {
                if (eulixBoxInfo == null && !"1".equals(boxBind)) {
                    eulixBoxInfo = new EulixBoxInfo();
                }
                if (eulixBoxInfo != null) {
                    eulixBoxInfo.setSecurityPasswordInfo(securityPasswordInfo);
                    Map<String, String> boxV = new HashMap<>();
                    boxV.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
                    boxV.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
                    boxV.put(EulixSpaceDBManager.FIELD_BOX_INFO, new Gson().toJson(eulixBoxInfo, EulixBoxInfo.class));
                    EulixSpaceDBUtil.updateBox(context, boxV);
                }
            }
        }
        if (isCallback && iView != null) {
            iView.onVerifyResult(code, source, securityTokenResult, verificationDenyTimestamp);
        }
    }

    public void granteeApplyResetSecurityPassword(String applyId) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context, true);
        if (gatewayCommunicationBase != null) {
            EulixSecurityUtil.granteeApplySetSecurityPassword(true, StringUtil.nullToEmpty(SystemUtil.getPhoneModel())
                    , applyId, gatewayCommunicationBase.getBoxDomain(), gatewayCommunicationBase.getAccessToken()
                    , gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getIvParams()
                    , new EulixBaseResponseExtensionCallback() {
                        @Override
                        public void onSuccess(String source, int code, String message, String requestId) {
                            if (iView != null) {
                                iView.granteeApplyResult(source, code);
                            }
                        }

                        @Override
                        public void onFailed() {
                            if (iView != null) {
                                iView.granteeApplyResult(null, -1);
                            }
                        }

                        @Override
                        public void onError(String errMsg) {
                            if (iView != null) {
                                iView.granteeApplyResult(null, ConstantField.SERVER_EXCEPTION_CODE);
                            }
                        }
                    });
        } else if (iView != null) {
            iView.granteeApplyResult(null, -1);
        }
    }
}
