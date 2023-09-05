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
 * date: 2022/4/24 10:51
 */
public class TerminalOfflineResult implements Serializable, EulixKeep {
    @SerializedName("id")
    private Integer id;
    @SerializedName("userid")
    private Integer userId;
    @SerializedName("aoid")
    private String aoId;
    @SerializedName("uuid")
    private String uuid;
    @SerializedName("terminalMode")
    private String terminalMode;
    @SerializedName("clientRegKey")
    private String clientRegKey;
    @SerializedName("address")
    private String address;
    @SerializedName("terminalType")
    private String terminalType;
    @SerializedName("createAt")
    private String createAt;
    @SerializedName("expireAt")
    private String expireAt;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

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

    public String getTerminalMode() {
        return terminalMode;
    }

    public void setTerminalMode(String terminalMode) {
        this.terminalMode = terminalMode;
    }

    public String getClientRegKey() {
        return clientRegKey;
    }

    public void setClientRegKey(String clientRegKey) {
        this.clientRegKey = clientRegKey;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTerminalType() {
        return terminalType;
    }

    public void setTerminalType(String terminalType) {
        this.terminalType = terminalType;
    }

    public String getCreateAt() {
        return createAt;
    }

    public void setCreateAt(String createAt) {
        this.createAt = createAt;
    }

    public String getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(String expireAt) {
        this.expireAt = expireAt;
    }

    @Override
    public String toString() {
        return "TerminalOfflineResult{" +
                "id=" + id +
                ", userId=" + userId +
                ", aoId='" + aoId + '\'' +
                ", uuid='" + uuid + '\'' +
                ", terminalMode='" + terminalMode + '\'' +
                ", clientRegKey='" + clientRegKey + '\'' +
                ", address='" + address + '\'' +
                ", terminalType='" + terminalType + '\'' +
                ", createAt='" + createAt + '\'' +
                ", expireAt='" + expireAt + '\'' +
                '}';
    }
}
