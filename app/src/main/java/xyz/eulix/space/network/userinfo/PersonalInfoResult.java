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
 * date: 2021/10/8 16:24
 */
public class PersonalInfoResult implements Serializable, EulixKeep {
    @SerializedName("clientUUID")
    private String clientUuid;
    @SerializedName("createAt")
    private String createAt;
    @SerializedName("aoId")
    private String globalId;
    @SerializedName("personalName")
    private String personalName;
    @SerializedName("personalSign")
    private String personalSign;
    @SerializedName("role")
    private String role;
    @SerializedName("phoneModel")
    private String phoneModel;
    @SerializedName("userDomain")
    private String userDomain;
    @SerializedName("imageMd5")
    private String imageMD5;

    public String getClientUuid() {
        return clientUuid;
    }

    public void setClientUuid(String clientUuid) {
        this.clientUuid = clientUuid;
    }

    public String getCreateAt() {
        return createAt;
    }

    public void setCreateAt(String createAt) {
        this.createAt = createAt;
    }

    public String getGlobalId() {
        return globalId;
    }

    public void setGlobalId(String globalId) {
        this.globalId = globalId;
    }

    public String getPersonalName() {
        return personalName;
    }

    public void setPersonalName(String personalName) {
        this.personalName = personalName;
    }

    public String getPersonalSign() {
        return personalSign;
    }

    public void setPersonalSign(String personalSign) {
        this.personalSign = personalSign;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhoneModel() {
        return phoneModel;
    }

    public void setPhoneModel(String phoneModel) {
        this.phoneModel = phoneModel;
    }

    public String getUserDomain() {
        return userDomain;
    }

    public void setUserDomain(String userDomain) {
        this.userDomain = userDomain;
    }

    public String getImageMD5() {
        return imageMD5;
    }

    public void setImageMD5(String imageMD5) {
        this.imageMD5 = imageMD5;
    }

    @Override
    public String toString() {
        return "PersonalInfoResult{" +
                "clientUuid='" + clientUuid + '\'' +
                ", createAt='" + createAt + '\'' +
                ", globalId='" + globalId + '\'' +
                ", personalName='" + personalName + '\'' +
                ", personalSign='" + personalSign + '\'' +
                ", role='" + role + '\'' +
                ", phoneModel='" + phoneModel + '\'' +
                ", userDomain='" + userDomain + '\'' +
                ", imageMD5='" + imageMD5 + '\'' +
                '}';
    }
}
