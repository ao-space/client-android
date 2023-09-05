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
 * date: 2021/7/27 10:27
 */
public class PairingClientInfo implements EulixKeep {
    private String clientPubKey;
    private String clientUuid;
    private String clientPriKey;
    private String clientPhoneModel;

    public String getClientPubKey() {
        return clientPubKey;
    }

    public void setClientPubKey(String clientPubKey) {
        this.clientPubKey = clientPubKey;
    }

    public String getClientUuid() {
        return clientUuid;
    }

    public void setClientUuid(String clientUuid) {
        this.clientUuid = clientUuid;
    }

    public String getClientPriKey() {
        return clientPriKey;
    }

    public void setClientPriKey(String clientPriKey) {
        this.clientPriKey = clientPriKey;
    }

    public String getClientPhoneModel() {
        return clientPhoneModel;
    }

    public void setClientPhoneModel(String clientPhoneModel) {
        this.clientPhoneModel = clientPhoneModel;
    }

    @Override
    public String toString() {
        return "PairingClientInfo{" +
                "clientPubKey='" + clientPubKey + '\'' +
                ", clientUuid='" + clientUuid + '\'' +
                ", clientPriKey='" + clientPriKey + '\'' +
                ", clientPhoneModel='" + clientPhoneModel + '\'' +
                '}';
    }
}
