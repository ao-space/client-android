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
 * date: 2022/4/7 10:34
 */
public class PutPersonalInfoRequestBody implements Serializable, EulixKeep {
    @SerializedName("personalName")
    private String personalName;
    @SerializedName("personalSign")
    private String personalSign;
    @SerializedName("aoId")
    private String aoId;
    @SerializedName("userDomain")
    private String userDomain;
    @SerializedName("phoneModel")
    private String phoneModel;

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

    public String getAoId() {
        return aoId;
    }

    public void setAoId(String aoId) {
        this.aoId = aoId;
    }

    public String getUserDomain() {
        return userDomain;
    }

    public void setUserDomain(String userDomain) {
        this.userDomain = userDomain;
    }

    public String getPhoneModel() {
        return phoneModel;
    }

    public void setPhoneModel(String phoneModel) {
        this.phoneModel = phoneModel;
    }

    @Override
    public String toString() {
        return "PutPersonalInfoRequestBody{" +
                "personalName='" + personalName + '\'' +
                ", personalSign='" + personalSign + '\'' +
                ", aoId='" + aoId + '\'' +
                ", userDomain='" + userDomain + '\'' +
                ", phoneModel='" + phoneModel + '\'' +
                '}';
    }
}
