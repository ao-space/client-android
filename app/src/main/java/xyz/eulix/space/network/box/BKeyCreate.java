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

package xyz.eulix.space.network.box;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/8/19 16:57
 */
public class BKeyCreate implements EulixKeep {
    private String accessToken;
    private String authKey;
    private String clientUUID;
    private String boxName;
    private String boxUUID;

    private String version;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    public String getClientUUID() {
        return clientUUID;
    }

    public void setClientUUID(String clientUUID) {
        this.clientUUID = clientUUID;
    }

    public String getBoxName() {
        return boxName;
    }

    public void setBoxName(String boxName) {
        this.boxName = boxName;
    }

    public String getBoxUUID() {
        return boxUUID;
    }

    public void setBoxUUID(String boxUUID) {
        this.boxUUID = boxUUID;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return "BKeyCreate{" +
                "accessToken='" + accessToken + '\'' +
                ", authKey='" + authKey + '\'' +
                ", clientUUID='" + clientUUID + '\'' +
                ", boxName='" + boxName + '\'' +
                ", boxUUID='" + boxUUID + '\'' +
                ", version='" + version + '\'' +
                '}';
    }
}
