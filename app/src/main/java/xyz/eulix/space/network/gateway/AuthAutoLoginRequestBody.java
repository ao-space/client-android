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

package xyz.eulix.space.network.gateway;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/5/17 9:57
 */
public class AuthAutoLoginRequestBody implements Serializable, EulixKeep {
    @SerializedName("refreshToken")
    private String refreshToken;
    @SerializedName("tmpEncryptedSecret")
    private String tempEncryptedSecret;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTempEncryptedSecret() {
        return tempEncryptedSecret;
    }

    public void setTempEncryptedSecret(String tempEncryptedSecret) {
        this.tempEncryptedSecret = tempEncryptedSecret;
    }

    @Override
    public String toString() {
        return "AuthAutoLoginRequestBody{" +
                "refreshToken='" + refreshToken + '\'' +
                ", tempEncryptedSecret='" + tempEncryptedSecret + '\'' +
                '}';
    }
}
