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

package xyz.eulix.space.network.platform;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/8/18 14:05
 */
public class PKeyBoxInfo implements Serializable, EulixKeep {
    @SerializedName("bkey")
    private String bkey;
    @SerializedName("userDomain")
    private String boxDomain;
    @SerializedName("boxPubKey")
    private String boxPubKey;
    @SerializedName("pkey")
    private String pkey;

    public String getBkey() {
        return bkey;
    }

    public void setBkey(String bkey) {
        this.bkey = bkey;
    }

    public String getBoxDomain() {
        return boxDomain;
    }

    public void setBoxDomain(String boxDomain) {
        this.boxDomain = boxDomain;
    }

    public String getBoxPubKey() {
        return boxPubKey;
    }

    public void setBoxPubKey(String boxPubKey) {
        this.boxPubKey = boxPubKey;
    }

    public String getPkey() {
        return pkey;
    }

    public void setPkey(String pkey) {
        this.pkey = pkey;
    }

    @Override
    public String toString() {
        return "PKeyBoxInfoRequestBody{" +
                "bkey='" + bkey + '\'' +
                ", boxDomain='" + boxDomain + '\'' +
                ", boxPubKey='" + boxPubKey + '\'' +
                ", pkey='" + pkey + '\'' +
                '}';
    }
}
