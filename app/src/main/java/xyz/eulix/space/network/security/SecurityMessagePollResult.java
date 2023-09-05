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

package xyz.eulix.space.network.security;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/8/16 15:34
 */
public class SecurityMessagePollResult implements EulixKeep, Serializable {
    @SerializedName("msgType")
    private String messageType;
    @SerializedName("securityTokenRes")
    private SecurityTokenResult securityTokenResult;
    @SerializedName("clientUuid")
    private String clientUuid;
    @SerializedName("accept")
    private boolean accept;
    @SerializedName("requestId")
    private String requestId;
    @SerializedName("applyId")
    private String applyId;

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public SecurityTokenResult getSecurityTokenResult() {
        return securityTokenResult;
    }

    public void setSecurityTokenResult(SecurityTokenResult securityTokenResult) {
        this.securityTokenResult = securityTokenResult;
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
                "messageType='" + messageType + '\'' +
                ", securityTokenResult=" + securityTokenResult +
                ", clientUuid='" + clientUuid + '\'' +
                ", accept=" + accept +
                ", requestId='" + requestId + '\'' +
                ", applyId='" + applyId + '\'' +
                '}';
    }
}
