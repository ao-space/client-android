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
 * date: 2022/8/15 11:54
 */
public class GranteeResetSecurityPasswordRequest implements EulixKeep, Serializable {
    @SerializedName("acceptSecurityToken")
    private String acceptSecurityToken;
    @SerializedName("emailSecurityToken")
    private String emailSecurityToken;
    @SerializedName("clientUuid")
    private String clientUuid;
    @SerializedName("newPasswd")
    private String newPassword;

    public String getAcceptSecurityToken() {
        return acceptSecurityToken;
    }

    public void setAcceptSecurityToken(String acceptSecurityToken) {
        this.acceptSecurityToken = acceptSecurityToken;
    }

    public String getEmailSecurityToken() {
        return emailSecurityToken;
    }

    public void setEmailSecurityToken(String emailSecurityToken) {
        this.emailSecurityToken = emailSecurityToken;
    }

    public String getClientUuid() {
        return clientUuid;
    }

    public void setClientUuid(String clientUuid) {
        this.clientUuid = clientUuid;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    @Override
    public String toString() {
        return "GranteeResetSecurityPasswordRequest{" +
                "acceptSecurityToken='" + acceptSecurityToken + '\'' +
                ", emailSecurityToken='" + emailSecurityToken + '\'' +
                ", clientUuid='" + clientUuid + '\'' +
                ", newPassword='" + newPassword + '\'' +
                '}';
    }
}
