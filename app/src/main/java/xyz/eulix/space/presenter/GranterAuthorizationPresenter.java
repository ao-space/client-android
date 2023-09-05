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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.bean.AOSpaceAccessBean;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.bean.UserInfo;
import xyz.eulix.space.database.EulixSpaceDBManager;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.network.gateway.AuthAutoLoginConfirmCallback;
import xyz.eulix.space.network.gateway.GatewayUtil;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.GatewayUtils;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/5/12 9:18
 */
public class GranterAuthorizationPresenter extends AbsPresenter<GranterAuthorizationPresenter.IGranterAuthorization> {
    public AuthAutoLoginConfirmCallback authAutoLoginConfirmCallback = new AuthAutoLoginConfirmCallback() {
        @Override
        public void onSuccess(int code, String message, boolean isSuccess, boolean isConfirm) {
            if (iView != null) {
                iView.loginConfirmResult(code, isSuccess, isConfirm);
            }
        }

        @Override
        public void onFailed(int code, String message, boolean isConfirm) {
            if (iView != null) {
                iView.loginConfirmResult(code, false, isConfirm);
            }
        }

        @Override
        public void onError(String errMsg, boolean isConfirm) {
            if (iView != null) {
                iView.loginConfirmResult(-1, false, isConfirm);
            }
        }
    };

    public interface IGranterAuthorization extends IBaseView {
        void loginConfirmResult(int code, boolean isSuccess, boolean isConfirm);
    }

    public AOSpaceAccessBean getSpecificAOSpaceAccessBean(String boxUuid, String boxBind) {
        return EulixSpaceDBUtil.getSpecificAOSpaceBean(context, boxUuid, boxBind);
    }

    public UserInfo getGranterInfo(String userDomain) {
        UserInfo userInfo = null;
        String clientUuid = DataUtil.getClientUuid(context);
        if (userDomain != null && clientUuid != null) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_DOMAIN, userDomain);
            List<UserInfo> userInfoList = EulixSpaceDBUtil.getGranterUserInfoList(context, clientUuid, queryMap);
            if (userInfoList != null && userInfoList.size() == 1) {
                userInfo = userInfoList.get(0);
            }
        }
        return userInfo;
    }

    public UserInfo getGranterInfo(String boxUuid, String boxBind, String aoId) {
        UserInfo userInfo = null;
        String clientUuid = DataUtil.getClientUuid(context);
        if (boxUuid != null && aoId != null && clientUuid != null) {
            Map<String, String> queryMap = new HashMap<>();
            queryMap.put(EulixSpaceDBManager.FIELD_BOX_UUID, boxUuid);
            if (boxBind != null) {
                queryMap.put(EulixSpaceDBManager.FIELD_BOX_BIND, boxBind);
            }
            List<UserInfo> userInfoList = EulixSpaceDBUtil.getGranterUserInfoList(context, clientUuid, queryMap);
            if (userInfoList != null) {
                for (UserInfo info : userInfoList) {
                    if (info != null && aoId.equals(info.getUserId())) {
                        userInfo = info;
                        break;
                    }
                }
            }
        }
        return userInfo;
    }

    public void authAutoLogin(String boxUuid, String boxBind, String loginClientUuid, boolean isConfirm, boolean isAutoLogin) {
        boolean result = false;
        if (boxUuid != null && boxBind != null) {
            GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context, boxUuid, boxBind);
            if (gatewayCommunicationBase != null && loginClientUuid != null) {
                result = true;
                GatewayUtil.authAutoLoginConfirm(context, boxUuid, gatewayCommunicationBase.getBoxDomain()
                        , gatewayCommunicationBase.getTransformation(), gatewayCommunicationBase.getSecretKey()
                        , gatewayCommunicationBase.getIvParams(), gatewayCommunicationBase.getAccessToken()
                        , loginClientUuid, isAutoLogin, isConfirm, false, authAutoLoginConfirmCallback);
            }
        }
        if (!result && iView != null) {
            iView.loginConfirmResult(-1, false, isConfirm);
        }
    }
}
