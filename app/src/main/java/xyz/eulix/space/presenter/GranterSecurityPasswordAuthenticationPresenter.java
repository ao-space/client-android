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
import xyz.eulix.space.network.EulixBaseResponseExtensionCallback;
import xyz.eulix.space.network.security.EulixSecurityUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.GatewayUtils;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/7/21 15:54
 */
public class GranterSecurityPasswordAuthenticationPresenter extends AbsPresenter<GranterSecurityPasswordAuthenticationPresenter.IGranterSecurityPasswordAuthentication> {
    public interface IGranterSecurityPasswordAuthentication extends IBaseView {
        void securityPasswordAuthenticationResult(int code, String source, boolean isAccept);
    }

    public void acceptHandleSecurityPassword(String boxUuid, String boxBind, String securityToken, String authClientUuid, boolean isAccept, String applyId, boolean isReset) {
        boolean result = false;
        if (boxUuid != null && boxBind != null) {
            GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context, boxUuid, boxBind);
            if (gatewayCommunicationBase != null && securityToken != null && authClientUuid != null) {
                result = true;
                String boxDomain = gatewayCommunicationBase.getBoxDomain();
                String accessToken = gatewayCommunicationBase.getAccessToken();
                String secretKey = gatewayCommunicationBase.getSecretKey();
                String ivParams = gatewayCommunicationBase.getIvParams();
                EulixBaseResponseExtensionCallback callback = new EulixBaseResponseExtensionCallback() {
                    @Override
                    public void onSuccess(String source, int code, String message, String requestId) {
                        if (iView != null) {
                            iView.securityPasswordAuthenticationResult(code, source, isAccept);
                        }
                    }

                    @Override
                    public void onFailed() {
                        if (iView != null) {
                            iView.securityPasswordAuthenticationResult(-1, null, isAccept);
                        }
                    }

                    @Override
                    public void onError(String errMsg) {
                        if (iView != null) {
                            iView.securityPasswordAuthenticationResult(ConstantField.SERVER_EXCEPTION_CODE, null, isAccept);
                        }
                    }
                };
                EulixSecurityUtil.binderAcceptSetSecurityPassword(isReset, securityToken, authClientUuid
                        , isAccept, applyId, boxDomain, accessToken, secretKey, ivParams, callback);
            }
        }
        if (!result && iView != null) {
            iView.securityPasswordAuthenticationResult(-1, null, isAccept);
        }
    }
}
