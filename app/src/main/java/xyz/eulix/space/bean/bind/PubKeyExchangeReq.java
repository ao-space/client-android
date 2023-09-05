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

package xyz.eulix.space.bean.bind;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/12/14 15:16
 */
public class PubKeyExchangeReq implements EulixKeep {
    private String clientPubKey;
    private String clientPriKey;
    private String signedBtid;

    public String getClientPubKey() {
        return clientPubKey;
    }

    public void setClientPubKey(String clientPubKey) {
        this.clientPubKey = clientPubKey;
    }

    public String getClientPriKey() {
        return clientPriKey;
    }

    public void setClientPriKey(String clientPriKey) {
        this.clientPriKey = clientPriKey;
    }

    public String getSignedBtid() {
        return signedBtid;
    }

    public void setSignedBtid(String signedBtid) {
        this.signedBtid = signedBtid;
    }

    @Override
    public String toString() {
        return "PubKeyExchangeReq{" +
                "clientPubKey='" + clientPubKey + '\'' +
                ", clientPriKey='" + clientPriKey + '\'' +
                ", signedBtid='" + signedBtid + '\'' +
                '}';
    }
}
