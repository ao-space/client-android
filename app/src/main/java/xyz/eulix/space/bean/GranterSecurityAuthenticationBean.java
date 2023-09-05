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

package xyz.eulix.space.bean;

import xyz.eulix.space.network.security.SecurityTokenResult;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/7/22 9:37
 */
public class GranterSecurityAuthenticationBean {
    private String boxUuid;
    private String boxBind;
    private String authClientUuid;
    private String messageType;
    private String applyId;
    private SecurityTokenResult securityTokenResult;

    public String getBoxUuid() {
        return boxUuid;
    }

    public void setBoxUuid(String boxUuid) {
        this.boxUuid = boxUuid;
    }

    public String getBoxBind() {
        return boxBind;
    }

    public void setBoxBind(String boxBind) {
        this.boxBind = boxBind;
    }

    public String getAuthClientUuid() {
        return authClientUuid;
    }

    public void setAuthClientUuid(String authClientUuid) {
        this.authClientUuid = authClientUuid;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getApplyId() {
        return applyId;
    }

    public void setApplyId(String applyId) {
        this.applyId = applyId;
    }

    public SecurityTokenResult getSecurityTokenResult() {
        return securityTokenResult;
    }

    public void setSecurityTokenResult(SecurityTokenResult securityTokenResult) {
        this.securityTokenResult = securityTokenResult;
    }

    @Override
    public String toString() {
        return "GranterSecurityAuthenticationBean{" +
                "boxUuid='" + boxUuid + '\'' +
                ", boxBind='" + boxBind + '\'' +
                ", authClientUuid='" + authClientUuid + '\'' +
                ", messageType='" + messageType + '\'' +
                ", applyId='" + applyId + '\'' +
                ", securityTokenResult=" + securityTokenResult +
                '}';
    }
}
