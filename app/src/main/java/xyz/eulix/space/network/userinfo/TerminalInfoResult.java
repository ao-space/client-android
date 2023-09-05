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
 * date: 2022/4/24 10:31
 */
public class TerminalInfoResult implements Serializable, EulixKeep {
    @SerializedName("aoid")
    private String aoId;
    @SerializedName("uuid")
    private String uuid;
    @SerializedName("terminalModel")
    private String terminalModel;
    @SerializedName("terminalType")
    private String terminalType;
    @SerializedName("loginTime")
    private String loginTime;
    @SerializedName("address")
    private String address;
    @SerializedName("clientRegKey")
    private String clientRegisterKey;

    public String getAoId() {
        return aoId;
    }

    public void setAoId(String aoId) {
        this.aoId = aoId;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTerminalModel() {
        return terminalModel;
    }

    public void setTerminalModel(String terminalModel) {
        this.terminalModel = terminalModel;
    }

    public String getTerminalType() {
        return terminalType;
    }

    public void setTerminalType(String terminalType) {
        this.terminalType = terminalType;
    }

    public String getLoginTime() {
        return loginTime;
    }

    public void setLoginTime(String loginTime) {
        this.loginTime = loginTime;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getClientRegisterKey() {
        return clientRegisterKey;
    }

    public void setClientRegisterKey(String clientRegisterKey) {
        this.clientRegisterKey = clientRegisterKey;
    }

    @Override
    public String toString() {
        return "TerminalInfoResult{" +
                "aoId='" + aoId + '\'' +
                ", uuid='" + uuid + '\'' +
                ", terminalModel='" + terminalModel + '\'' +
                ", terminalType='" + terminalType + '\'' +
                ", loginTime='" + loginTime + '\'' +
                ", address='" + address + '\'' +
                ", clientRegisterKey='" + clientRegisterKey + '\'' +
                '}';
    }
}
