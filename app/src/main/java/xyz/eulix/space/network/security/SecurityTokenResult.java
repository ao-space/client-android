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
 * date: 2022/7/21 9:12
 */
public class SecurityTokenResult implements EulixKeep, Serializable {
    @SerializedName("securityToken")
    private String securityToken;
    @SerializedName("expiredAt")
    private String expiredAt;

    public String getSecurityToken() {
        return securityToken;
    }

    public void setSecurityToken(String securityToken) {
        this.securityToken = securityToken;
    }

    public String getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(String expiredAt) {
        this.expiredAt = expiredAt;
    }

    @Override
    public String toString() {
        return "SecurityTokenResult{" +
                "securityToken='" + securityToken + '\'' +
                ", expiredAt='" + expiredAt + '\'' +
                '}';
    }
}
