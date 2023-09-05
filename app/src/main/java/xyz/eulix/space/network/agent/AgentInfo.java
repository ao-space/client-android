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

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/11/2 16:42
 */
public class AgentInfo implements Serializable, EulixKeep {
    @SerializedName("status")
    private String status;
    @SerializedName("version")
    private String version;
    @SerializedName("isClientPaired")
    private Boolean isClientPaired;
    @SerializedName("dockerStatus")
    private Integer dockerStatus;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Boolean getClientPaired() {
        return isClientPaired;
    }

    public void setClientPaired(Boolean clientPaired) {
        isClientPaired = clientPaired;
    }

    public Integer getDockerStatus() {
        return dockerStatus;
    }

    public void setDockerStatus(Integer dockerStatus) {
        this.dockerStatus = dockerStatus;
    }

    @Override
    public String toString() {
        return "AgentInfo{" +
                "status='" + status + '\'' +
                ", version='" + version + '\'' +
                ", isClientPaired=" + isClientPaired +
                ", dockerStatus=" + dockerStatus +
                '}';
    }
}
