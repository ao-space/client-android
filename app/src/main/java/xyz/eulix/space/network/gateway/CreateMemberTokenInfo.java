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

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/11/4 16:08
 */
public class CreateMemberTokenInfo implements EulixKeep {
    private String encryptedAuthKey;
    private String encryptedClientUUID;
    private String tempEncryptedSecret;

    public String getEncryptedAuthKey() {
        return encryptedAuthKey;
    }

    public void setEncryptedAuthKey(String encryptedAuthKey) {
        this.encryptedAuthKey = encryptedAuthKey;
    }

    public String getEncryptedClientUUID() {
        return encryptedClientUUID;
    }

    public void setEncryptedClientUUID(String encryptedClientUUID) {
        this.encryptedClientUUID = encryptedClientUUID;
    }

    public String getTempEncryptedSecret() {
        return tempEncryptedSecret;
    }

    public void setTempEncryptedSecret(String tempEncryptedSecret) {
        this.tempEncryptedSecret = tempEncryptedSecret;
    }

    @Override
    public String toString() {
        return "CreateMemberTokenInfo{" +
                "encryptedAuthKey='" + encryptedAuthKey + '\'' +
                ", encryptedClientUUID='" + encryptedClientUUID + '\'' +
                ", tempEncryptedSecret='" + tempEncryptedSecret + '\'' +
                '}';
    }
}
