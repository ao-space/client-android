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

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.concurrent.RejectedExecutionException;

import javax.crypto.spec.IvParameterSpec;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.EulixBoxBaseInfo;
import xyz.eulix.space.bean.EulixBoxInfo;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.bean.SecurityEmailInfo;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.did.bean.DIDCredentialBean;
import xyz.eulix.space.network.EulixBaseResponseExtensionCallback;
import xyz.eulix.space.network.agent.AgentCallCallback;
import xyz.eulix.space.network.agent.AgentUtil;
import xyz.eulix.space.network.security.EulixSecurityUtil;
import xyz.eulix.space.util.AOSpaceUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EncryptionUtil;
import xyz.eulix.space.util.GatewayUtils;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.SystemUtil;
import xyz.eulix.space.util.ThreadPool;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/7/7 18:41
 */
public class ModifySecurityPasswordPresenter extends AbsPresenter<ModifySecurityPasswordPresenter.IModifySecurityPassword> {
    private static final String TAG = ModifySecurityPasswordPresenter.class.getSimpleName();
    private String securityBoxUuid;
    private String securityBoxBind;
    private String securityPassword;

    public interface IModifySecurityPassword extends IBaseView {
        void modifySecurityPasswordResult(String source, int code, Boolean isGranter);
        void bluetoothHandle(boolean isGranter, String password, String granterSecurityToken);
        void resetSecurityPasswordResult(String source, int code, Boolean isGranter);
        void granteeApplyResult(String source, int code);
    }

    private void generateValidAccessToken(String serviceFunction) {
        if (iView != null && serviceFunction != null) {
            switch (serviceFunction) {
                case ConstantField.ServiceFunction.SECURITY_PASSWORD_MODIFY_BINDER:
                    iView.modifySecurityPasswordResult(null, ConstantField.OBTAIN_ACCESS_TOKEN_CODE, null);
                    break;
                default:
                    break;
            }
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

    private void resetSecurityInformation() {
        securityBoxUuid = null;
        securityBoxBind = null;
        securityPassword = null;
    }

    public void resetPasswordCredential(int code) {
        resetPasswordCredential(code, securityBoxUuid, securityBoxBind, securityPassword, true);
        resetSecurityInformation();
    }

    public void resetPasswordCredential(int code, String boxUuid, String boxBind, String password, boolean isRefreshDID) {
        if (code >= 200 && code < 300) {
//            updatePasswordCredential(boxUuid, boxBind, password);
            if (isRefreshDID) {
                refreshDIDDocument();
            }
        }
    }

//    /**
//     * 重置密码
//     * @param boxUuid
//     * @param boxBind
//     * @param password
//     */
//    private void updatePasswordCredential(String boxUuid, String boxBind, String password) {
//        updatePasswordCredential(boxUuid, boxBind, null, null, password);
//    }

//    /**
//     * 修改密码
//     * @param boxUuid
//     * @param boxBind
//     * @param passwordCredential
//     * @param oldPassword
//     * @param newPassword
//     */
//    private void updatePasswordCredential(String boxUuid, String boxBind, DIDCredentialBean.PasswordCredential passwordCredential, String oldPassword, String newPassword) {
//        boolean isGenerateKeyPair = (passwordCredential == null);
//        String privateKey = null;
//        if (passwordCredential != null) {
//            if (passwordCredential.isEncryptPrivateKey()) {
//                privateKey = EncryptionUtil.decrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
//                        , null, passwordCredential.getPasswordPrivateKey()
//                        , StringUtil.getCustomizeSecret32(oldPassword), StandardCharsets.UTF_8, passwordCredential.getPasswordIv());
//            } else {
//                privateKey = passwordCredential.getPasswordPrivateKey();
//            }
//            // todo 修改流程不更改凭证
////            if (passwordCredential.getPasswordPublicKey() == null || privateKey == null) {
////                isGenerateKeyPair = true;
////                passwordCredential = null;
////            }
//        }
//        if (isGenerateKeyPair) {
//            KeyPair keyPair = EncryptionUtil.generateKeyPair(ConstantField.Algorithm.RSA, null, 2048);
//            if (keyPair != null) {
//                String newPublicKey = StringUtil.byteArrayToString(keyPair.getPublic().getEncoded());
//                String newPrivateKey = StringUtil.byteArrayToString(keyPair.getPrivate().getEncoded());
//                if (newPublicKey != null && newPrivateKey != null) {
//                    privateKey = newPrivateKey;
//                    passwordCredential = new DIDCredentialBean.PasswordCredential();
//                    passwordCredential.setPasswordPublicKey(newPublicKey);
//                    passwordCredential.setEncryptPrivateKey(false);
//                    passwordCredential.setPasswordPrivateKey(newPrivateKey);
//                    passwordCredential.setPasswordIv(null);
//                }
//            }
//        }
//        if (passwordCredential != null && privateKey != null) {
//            String ivParams = DataUtil.generateIVParam();
//            passwordCredential.setPasswordIv(ivParams);
//            String encryptPrivateKey = EncryptionUtil.encrypt(ConstantField.Algorithm.Transformation.AES_CBC_PKCS5
//                    , null, privateKey, StringUtil.getCustomizeSecret32(newPassword)
//                    , StandardCharsets.UTF_8, ivParams);
//            passwordCredential.setEncryptPrivateKey(encryptPrivateKey != null);
//            passwordCredential.setPasswordPrivateKey((encryptPrivateKey == null ? privateKey : encryptPrivateKey));
//            Logger.d(TAG, "password credential: " + passwordCredential);
//            DIDCredentialBean didCredentialBean = new DIDCredentialBean();
//            didCredentialBean.setPasswordCredential(passwordCredential);
//            AOSpaceUtil.insertOrUpdateDID(context, boxUuid, boxBind, didCredentialBean);
//        }
//    }

    public void modifySecurityPassword(String oldPassword, String newPassword, String granterSecurityToken) {
        if (oldPassword != null) {
            GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
            String boxUuid = gatewayCommunicationBase.getBoxUuid();
            String boxBind = gatewayCommunicationBase.getBoxBind();
            String granterClientUuid = EulixSpaceDBUtil.getCompatibleActiveClientUuid(context);
            int identity = getActiveIdentity();
            if (gatewayCommunicationBase != null && (identity == ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY
                    || identity == ConstantField.UserIdentity.ADMINISTRATOR_GRANTEE)) {
                boolean isGranter = (identity == ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY);
                EulixBaseResponseExtensionCallback eulixBaseResponseExtensionCallback = new EulixBaseResponseExtensionCallback() {
                    @Override
                    public void onSuccess(String source, int code, String message, String requestId) {
//                        if (code >= 200 && code < 300 && boxUuid != null && boxBind != null) {
//                            updatePasswordCredential(boxUuid, boxBind, EulixSpaceDBUtil.getSpecificPasswordCredential(context, boxUuid, boxBind, null)
//                                    , oldPassword, newPassword);
//                        }
                        if (code >= 200 && code < 300) {
                            refreshDIDDocument();
                        }
                        if (iView != null) {
                            iView.modifySecurityPasswordResult(source, code, isGranter);
                        }
                    }

                    @Override
                    public void onFailed() {
                        if (iView != null) {
                            iView.modifySecurityPasswordResult(null, -1, isGranter);
                        }
                    }

                    @Override
                    public void onError(String errMsg) {
                        if (iView != null) {
                            iView.modifySecurityPasswordResult(null, ConstantField.SERVER_EXCEPTION_CODE, isGranter);
                        }
                    }
                };
                if (isGranter) {
                    EulixSecurityUtil.binderModifySecurityPassword(oldPassword, newPassword, gatewayCommunicationBase.getBoxDomain()
                            , gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                            , gatewayCommunicationBase.getIvParams(), eulixBaseResponseExtensionCallback);
                } else {
                    EulixSecurityUtil.granteeModifySecurityPassword(granterSecurityToken, granterClientUuid
                            , oldPassword, newPassword, gatewayCommunicationBase.getBoxDomain()
                            , gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                            , gatewayCommunicationBase.getIvParams(), eulixBaseResponseExtensionCallback);
                }
            } else {
                generateValidAccessToken(ConstantField.ServiceFunction.SECURITY_PASSWORD_MODIFY_BINDER);
            }
        } else if (iView != null) {
            iView.modifySecurityPasswordResult(null, -1, null);
        }
    }

    public void resetPasswordToHardware(String password, String granterSecurityToken, String baseUrl, boolean isBluetooth, String bleKey, String bleIv) {
        int identity = getActiveIdentity();
        if (identity == ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY || identity == ConstantField.UserIdentity.ADMINISTRATOR_GRANTEE) {
            boolean isGranter = (identity == ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY);
            String boxUuid = null;
            String boxBind = null;
            GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context, true);
            if (gatewayCommunicationBase != null) {
                boxUuid = gatewayCommunicationBase.getBoxUuid();
                boxBind = gatewayCommunicationBase.getBoxBind();
            }
            if (isBluetooth) {
                if (iView != null) {
                    securityBoxUuid = boxUuid;
                    securityBoxBind = boxBind;
                    securityPassword = password;
                    iView.bluetoothHandle(isGranter, password, granterSecurityToken);
                }
            } else {
                String granterClientUuid = EulixSpaceDBUtil.getCompatibleActiveClientUuid(context);
                if (gatewayCommunicationBase != null) {
                    String finalBoxUuid = boxUuid;
                    String finalBoxBind = boxBind;
                    AgentCallCallback agentCallCallback = new AgentCallCallback() {
                        @Override
                        public void onSuccess(int code, String source, String message, String requestId) {
                            resetPasswordCredential(code, finalBoxUuid, finalBoxBind, password, true);
                            if (iView != null) {
                                iView.resetSecurityPasswordResult(source, code, isGranter);
                            }
                        }

                        @Override
                        public void onFailed(int code, String source, String message, String requestId) {
                            if (iView != null) {
                                iView.resetSecurityPasswordResult(source, code, isGranter);
                            }
                        }

                        @Override
                        public void onError(String errMsg) {
                            if (iView != null) {
                                iView.resetSecurityPasswordResult(null, -1, isGranter);
                            }
                        }
                    };
                    try {
                        ThreadPool.getInstance().execute(() -> {
                            if (isGranter) {
                                AgentUtil.binderResetSecurityPassword(baseUrl, gatewayCommunicationBase.getAccessToken()
                                        , password, bleKey, bleIv, agentCallCallback);
                            } else {
                                AgentUtil.granteeResetSecurityPassword(baseUrl, gatewayCommunicationBase.getAccessToken()
                                        , granterSecurityToken, granterClientUuid, password, bleKey, bleIv, agentCallCallback);
                            }
                        });
                    } catch (RejectedExecutionException e) {
                        e.printStackTrace();
                    }
                } else if (iView != null) {
                    iView.resetSecurityPasswordResult(null, -1, isGranter);
                }
            }
        } else if (iView != null) {
            iView.resetSecurityPasswordResult(null, -1, null);
        }
    }

    public void resetPassword(String securityToken, String password, String granterSecurityToken) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context, true);
        String granterClientUuid = EulixSpaceDBUtil.getCompatibleActiveClientUuid(context);
        int identity = getActiveIdentity();
        if (gatewayCommunicationBase != null && securityToken != null && (identity == ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY
                || identity == ConstantField.UserIdentity.ADMINISTRATOR_GRANTEE)) {
            String boxUuid = gatewayCommunicationBase.getBoxUuid();
            String boxBind = gatewayCommunicationBase.getBoxBind();
            boolean isGranter = (identity == ConstantField.UserIdentity.ADMINISTRATOR_IDENTITY);
            EulixBaseResponseExtensionCallback eulixBaseResponseExtensionCallback = new EulixBaseResponseExtensionCallback() {
                @Override
                public void onSuccess(String source, int code, String message, String requestId) {
                    resetPasswordCredential(code, boxUuid, boxBind, password, true);
                    if (iView != null) {
                        iView.resetSecurityPasswordResult(source, code, isGranter);
                    }
                }

                @Override
                public void onFailed() {
                    if (iView != null) {
                        iView.resetSecurityPasswordResult(null, -1, isGranter);
                    }
                }

                @Override
                public void onError(String errMsg) {
                    if (iView != null) {
                        iView.resetSecurityPasswordResult(null, ConstantField.SERVER_EXCEPTION_CODE, isGranter);
                    }
                }
            };
            if (isGranter) {
                EulixSecurityUtil.binderResetSecurityPassword(securityToken, password, gatewayCommunicationBase.getBoxDomain()
                        , gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                        , gatewayCommunicationBase.getIvParams(), eulixBaseResponseExtensionCallback);
            } else {
                EulixSecurityUtil.granteeResetSecurityPassword(granterSecurityToken, securityToken
                        , granterClientUuid, password, gatewayCommunicationBase.getBoxDomain()
                        , gatewayCommunicationBase.getAccessToken(), gatewayCommunicationBase.getSecretKey()
                        , gatewayCommunicationBase.getIvParams(), eulixBaseResponseExtensionCallback);
            }
        } else if (iView != null) {
            iView.resetSecurityPasswordResult(null, -1, null);
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
