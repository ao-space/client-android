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

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/7/27 10:30
 */
public class PairingBoxInfo implements EulixKeep {
    private String authKey;
    private String boxName;
    private String boxPubKey;
    private String boxUuid;
    private String clientUUID;
    private String regKey;
    private String userDomain;
    private String aoId;
    private String spaceName;
    private String avatarUrl;

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    public String getBoxName() {
        return boxName;
    }

    public void setBoxName(String boxName) {
        this.boxName = boxName;
    }

    public String getBoxPubKey() {
        return boxPubKey;
    }

    public void setBoxPubKey(String boxPubKey) {
        this.boxPubKey = boxPubKey;
    }

    public String getBoxUuid() {
        return boxUuid;
    }

    public void setBoxUuid(String boxUuid) {
        this.boxUuid = boxUuid;
    }

    public String getClientUUID() {
        return clientUUID;
    }

    public void setClientUUID(String clientUUID) {
        this.clientUUID = clientUUID;
    }

    public String getRegKey() {
        return regKey;
    }

    public void setRegKey(String regKey) {
        this.regKey = regKey;
    }

    public String getUserDomain() {
        return userDomain;
    }

    public void setUserDomain(String userDomain) {
        this.userDomain = userDomain;
    }

    public String getAoId() {
        return aoId;
    }

    public void setAoId(String aoId) {
        this.aoId = aoId;
    }

    public String getSpaceName() {
        return spaceName;
    }

    public void setSpaceName(String spaceName) {
        this.spaceName = spaceName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    @Override
    public String toString() {
        return "PairingBoxInfo{" +
                "authKey='" + authKey + '\'' +
                ", boxName='" + boxName + '\'' +
                ", boxPubKey='" + boxPubKey + '\'' +
                ", boxUuid='" + boxUuid + '\'' +
                ", clientUUID='" + clientUUID + '\'' +
                ", regKey='" + regKey + '\'' +
                ", userDomain='" + userDomain + '\'' +
                ", aoId='" + aoId + '\'' +
                ", spaceName='" + spaceName + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                '}';
    }
}
