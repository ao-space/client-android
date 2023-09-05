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
 * date: 2022/5/17 14:42
 */
public class AuthAutoLoginConfirmRequestBody implements Serializable, EulixKeep {
    @SerializedName("accessToken")
    private String accessToken;
    @SerializedName("encryptedClientUUID")
    private String encryptedClientUUID;
    @SerializedName("autoLogin")
    private boolean autoLogin;
    @SerializedName("login")
    private boolean login;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getEncryptedClientUUID() {
        return encryptedClientUUID;
    }

    public void setEncryptedClientUUID(String encryptedClientUUID) {
        this.encryptedClientUUID = encryptedClientUUID;
    }

    public boolean isAutoLogin() {
        return autoLogin;
    }

    public void setAutoLogin(boolean autoLogin) {
        this.autoLogin = autoLogin;
    }

    public boolean isLogin() {
        return login;
    }

    public void setLogin(boolean login) {
        this.login = login;
    }

    @Override
    public String toString() {
        return "AuthAutoLoginConfirmRequestBody{" +
                "accessToken='" + accessToken + '\'' +
                ", encryptedClientUUID='" + encryptedClientUUID + '\'' +
                ", autoLogin=" + autoLogin +
                ", login=" + login +
                '}';
    }
}
