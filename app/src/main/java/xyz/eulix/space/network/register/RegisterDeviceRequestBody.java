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

package xyz.eulix.space.network.register;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * date: 2021/6/16 18:10
 */
public class RegisterDeviceRequestBody implements EulixKeep {
    private String clientRegKey;
    private String clientUUID;
    private String deviceId;
    private String deviceToken;
    private String platform;

    public String getClientRegKey() {
        return clientRegKey;
    }

    public void setClientRegKey(String clientRegKey) {
        this.clientRegKey = clientRegKey;
    }

    public String getClientUUID() {
        return clientUUID;
    }

    public void setClientUUID(String clientUUID) {
        this.clientUUID = clientUUID;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    @Override
    public String toString() {
        return "RegisterDeviceRequestBody{" +
                "clientRegKey='" + clientRegKey + '\'' +
                ", clientUUID='" + clientUUID + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", deviceToken='" + deviceToken + '\'' +
                ", platform='" + platform + '\'' +
                '}';
    }
}
