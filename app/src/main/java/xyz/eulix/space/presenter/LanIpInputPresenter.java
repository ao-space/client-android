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
import xyz.eulix.space.network.gateway.GatewayManager;
import xyz.eulix.space.network.gateway.ISpaceStatusCallback;
import xyz.eulix.space.network.gateway.SpaceStatusResult;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.NetUtils;
import xyz.eulix.space.util.ThreadPool;

/**
 * Author:      Zhu Fuyu
 * Description:
 * History:     2023/3/24
 */
public class LanIpInputPresenter extends AbsPresenter<LanIpInputPresenter.ILanIpInput> {
    public interface ILanIpInput extends IBaseView {
        void onCheckDomainResult(boolean isAvailable);
    }

    public void checkDomainAvailable(String domain) {
        String realDomain;
        if (NetUtils.isIpAddress(domain)) {
            realDomain = "http://" + domain;
        } else {
            realDomain = "https://" + domain;
        }

        GatewayManager gatewayManager = new GatewayManager(realDomain);
        ThreadPool.getInstance().execute(() -> gatewayManager.getSpaceStatus(new ISpaceStatusCallback() {
            @Override
            public void onResult(SpaceStatusResult result) {
                Logger.d("zfy", "checkConnectStates on result: " + result);
                if (result != null) {
                    iView.onCheckDomainResult(true);
                } else {
                    iView.onCheckDomainResult(false);
                }
            }

            @Override
            public void onError(String errMsg) {
                Logger.d("zfy", "checkConnectStates on error: " + errMsg);
                iView.onCheckDomainResult(false);
            }
        }));
    }
}
