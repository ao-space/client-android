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

package xyz.eulix.space.util;

import android.text.TextUtils;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.manager.LanManager;

/**
 * Author:      Zhu Fuyu
 * Description:
 * History:     2021/12/24
 */
public class Urls {

    /**
     * @return 只获取ip地址
     */
    public static String getIPBaseUrl() {
        if (LanManager.getInstance().isLanEnable()) {
            return LanManager.getInstance().getIpAddress();
        } else {
            return null;
        }
    }

    //获取boxDomain
    public static String getBaseUrl() {
        if (LanManager.getInstance().isLanEnable()) {
            return LanManager.getInstance().getIpAddress();
        } else {
            GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(EulixSpaceApplication.getContext());
            if (gatewayCommunicationBase != null) {
                return gatewayCommunicationBase.getBoxDomain();
            } else {
                return null;
            }
        }
    }

    public static String generateBaseUrl(String boxDomain) {
        String baseUrl = boxDomain;
        if (baseUrl == null) {
            baseUrl = ConstantField.URL.BASE_GATEWAY_URL_DEBUG;
        } else {
            while ((baseUrl.startsWith(":") || baseUrl.startsWith("/")) && baseUrl.length() > 1) {
                baseUrl = baseUrl.substring(1);
            }
            if (TextUtils.isEmpty(baseUrl)) {
                baseUrl = ConstantField.URL.BASE_GATEWAY_URL_DEBUG;
            } else {
                if (!(baseUrl.startsWith("http://") || baseUrl.startsWith("https://"))) {
                    baseUrl = "https://" + baseUrl;
                }
                if (!baseUrl.endsWith("/")) {
                    baseUrl = baseUrl + "/";
                }
            }
        }
        return baseUrl;
    }
}
