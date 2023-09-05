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

package xyz.eulix.space.network.userinfo;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/10/13 18:08
 */
public class MemberCreateInfo implements Serializable, EulixKeep {
    @SerializedName("clientUUID")
    private String clientUuid;
    @SerializedName("inviteCode")
    private String inviteCode;
    @SerializedName("tempEncryptedSecret")
    private String tempEncryptedSecret;
    @SerializedName("nickName")
    private String nickname;
    @SerializedName("phoneModel")
    private String phoneModel;
    @SerializedName("phoneType")
    private String phoneType;
    @SerializedName("applyEmail")
    private String applyEmail;

    public String getClientUuid() {
        return clientUuid;
    }

    public void setClientUuid(String clientUuid) {
        this.clientUuid = clientUuid;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public String getTempEncryptedSecret() {
        return tempEncryptedSecret;
    }

    public void setTempEncryptedSecret(String tempEncryptedSecret) {
        this.tempEncryptedSecret = tempEncryptedSecret;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPhoneModel() {
        return phoneModel;
    }

    public void setPhoneModel(String phoneModel) {
        this.phoneModel = phoneModel;
    }

    public String getPhoneType() {
        return phoneType;
    }

    public void setPhoneType(String phoneType) {
        this.phoneType = phoneType;
    }

    public String getApplyEmail() {
        return applyEmail;
    }

    public void setApplyEmail(String applyEmail) {
        this.applyEmail = applyEmail;
    }

    @Override
    public String toString() {
        return "MemberCreateInfo{" +
                "clientUuid='" + clientUuid + '\'' +
                ", inviteCode='" + inviteCode + '\'' +
                ", tempEncryptedSecret='" + tempEncryptedSecret + '\'' +
                ", nickname='" + nickname + '\'' +
                ", phoneModel='" + phoneModel + '\'' +
                ", phoneType='" + phoneType + '\'' +
                ", applyEmail='" + applyEmail + '\'' +
                '}';
    }
}
