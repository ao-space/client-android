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

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.network.EulixBaseResponseExtensionCallback;
import xyz.eulix.space.network.security.EulixSecurityUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.GatewayUtils;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.SystemUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/7/7 15:40
 */
public class EulixSecuritySettingsPresenter extends AbsPresenter<EulixSecuritySettingsPresenter.IEulixSecuritySettings> {
    public interface IEulixSecuritySettings extends IBaseView {
        void granteeApplyResult(String source, int code);
    }

    public int getIdentity() {
        return EulixSpaceDBUtil.getActiveDeviceUserIdentity(context);
    }

    public boolean hasBiometricFeature() {
        return (SystemUtil.getBiometricFeatureStatus(context) > 0);
    }

    public void granteeApplyModifySecurityPassword(String applyId) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context, true);
        if (gatewayCommunicationBase != null) {
            EulixSecurityUtil.granteeApplySetSecurityPassword(false, StringUtil.nullToEmpty(SystemUtil.getPhoneModel())
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
