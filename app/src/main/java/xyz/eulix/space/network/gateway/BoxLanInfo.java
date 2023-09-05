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
 * Author:      Zhu Fuyu
 * Description: 盒子局域网信息
 * History:     2023/4/19
 */
public class BoxLanInfo implements EulixKeep, Serializable {

    @SerializedName("publicKey")
    public String publicKey;
    @SerializedName("lanDomain")
    public String lanDomain;
    @SerializedName("lanIp")
    public String lanIp;
    @SerializedName("userDomain")
    public String userDomain;
    @SerializedName("port")
    public String port;
    @SerializedName("tlsPort")
    public String tlsPort;

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getLanDomain() {
        return lanDomain;
    }

    public void setLanDomain(String lanDomain) {
        this.lanDomain = lanDomain;
    }

    public String getLanIp() {
        return lanIp;
    }

    public void setLanIp(String lanIp) {
        this.lanIp = lanIp;
    }

    public String getUserDomain() {
        return userDomain;
    }

    public void setUserDomain(String userDomain) {
        this.userDomain = userDomain;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getTlsPort() {
        return tlsPort;
    }

    public void setTlsPort(String tlsPort) {
        this.tlsPort = tlsPort;
    }

    @Override
    public String toString() {
        return "BoxLanInfo{" +
                "publicKey='" + publicKey + '\'' +
                ", lanDomain='" + lanDomain + '\'' +
                ", lanIp='" + lanIp + '\'' +
                ", userDomain='" + userDomain + '\'' +
                ", port='" + port + '\'' +
                ", tlsPort='" + tlsPort + '\'' +
                '}';
    }
}
