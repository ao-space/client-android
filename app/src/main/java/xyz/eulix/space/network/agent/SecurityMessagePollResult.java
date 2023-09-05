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

package xyz.eulix.space.network.agent;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.network.security.SecurityTokenResult;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/8/26 15:10
 */
public class SecurityMessagePollResult implements EulixKeep {
    private String msgType;
    private SecurityTokenResult securityTokenRes;
    private String clientUuid;
    private boolean accept;
    private String requestId;
    private String applyId;

    public String getMsgType() {
        return msgType;
    }

    public void setMsgType(String msgType) {
        this.msgType = msgType;
    }

    public SecurityTokenResult getSecurityTokenRes() {
        return securityTokenRes;
    }

    public void setSecurityTokenRes(SecurityTokenResult securityTokenRes) {
        this.securityTokenRes = securityTokenRes;
    }

    public String getClientUuid() {
        return clientUuid;
    }

    public void setClientUuid(String clientUuid) {
        this.clientUuid = clientUuid;
    }

    public boolean isAccept() {
        return accept;
    }

    public void setAccept(boolean accept) {
        this.accept = accept;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getApplyId() {
        return applyId;
    }

    public void setApplyId(String applyId) {
        this.applyId = applyId;
    }

    @Override
    public String toString() {
        return "SecurityMessagePollResult{" +
                "msgType='" + msgType + '\'' +
                ", securityTokenRes=" + securityTokenRes +
                ", clientUuid='" + clientUuid + '\'' +
                ", accept=" + accept +
                ", requestId='" + requestId + '\'' +
                ", applyId='" + applyId + '\'' +
                '}';
    }
}
