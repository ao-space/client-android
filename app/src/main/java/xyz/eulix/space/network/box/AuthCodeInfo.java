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
 * Description: 登录认证授权验证码
 * date: 2021/8/19 16:59
 */
public class AuthCodeInfo implements EulixKeep {
    private String authCode;
    private String bkey;
    private String userDomain;
    private String lanDomain;
    private String lanIp;
    //验证码剩余有效时间 ms
    private String authCodeExpiresAt;
    //验证码总有效时间 ms
    private String authCodeTotalExpiresAt;

    public String getAuthCode() {
        return authCode;
    }

    public void setAuthCode(String authCode) {
        this.authCode = authCode;
    }

    public String getBkey() {
        return bkey;
    }

    public void setBkey(String bkey) {
        this.bkey = bkey;
    }

    public String getUserDomain() {
        return userDomain;
    }

    public void setUserDomain(String userDomain) {
        this.userDomain = userDomain;
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

    public String getAuthCodeExpiresAt() {
        return authCodeExpiresAt;
    }

    public void setAuthCodeExpiresAt(String authCodeExpiresAt) {
        this.authCodeExpiresAt = authCodeExpiresAt;
    }

    public String getAuthCodeTotalExpiresAt() {
        return authCodeTotalExpiresAt;
    }

    public void setAuthCodeTotalExpiresAt(String authCodeTotalExpiresAt) {
        this.authCodeTotalExpiresAt = authCodeTotalExpiresAt;
    }

    @Override
    public String toString() {
        return "AuthCodeInfo{" +
                "authCode='" + authCode + '\'' +
                ", bkey='" + bkey + '\'' +
                ", userDomain='" + userDomain + '\'' +
                ", lanDomain='" + lanDomain + '\'' +
                ", lanIp='" + lanIp + '\'' +
                ", authCodeExpiresAt='" + authCodeExpiresAt + '\'' +
                ", authCodeTotalExpiresAt='" + authCodeTotalExpiresAt + '\'' +
                '}';
    }
}
