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
import xyz.eulix.space.network.files.BaseResponseBody;
import xyz.eulix.space.network.gateway.AlgorithmConfig;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/10/13 18:11
 */
public class MemberCreateResult extends BaseResponseBody implements Serializable, EulixKeep {
    @SerializedName("algorithmConfig")
    private AlgorithmConfig algorithmConfig;
    @SerializedName("boxUUID")
    private String boxUuid;
    @SerializedName("authKey")
    private String authKey;
    @SerializedName("userDomain")
    private String userDomain;

    public AlgorithmConfig getAlgorithmConfig() {
        return algorithmConfig;
    }

    public void setAlgorithmConfig(AlgorithmConfig algorithmConfig) {
        this.algorithmConfig = algorithmConfig;
    }

    public String getBoxUuid() {
        return boxUuid;
    }

    public void setBoxUuid(String boxUuid) {
        this.boxUuid = boxUuid;
    }

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getUserDomain() {
        return userDomain;
    }

    public void setUserDomain(String userDomain) {
        this.userDomain = userDomain;
    }

    @Override
    public String toString() {
        return "MemberCreateResult{" +
                "algorithmConfig=" + algorithmConfig +
                ", boxUuid='" + boxUuid + '\'' +
                ", authKey='" + authKey + '\'' +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", requestId='" + requestId + '\'' +
                ", userDomain='" + userDomain + '\'' +
                '}';
    }
}
