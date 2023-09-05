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

package xyz.eulix.space.bean;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.network.gateway.AlgorithmConfig;
import xyz.eulix.space.network.gateway.BoxLanInfo;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/8/30 14:04
 */
public class LoginInfo implements EulixKeep {
    private String accessToken;
    private String secretKey;
    private String domain;
    private String boxName;
    private String boxUUID;
    private String aoid;
    private String refreshToken;
    private String encryptedSecret;
    private String expiresAt;
    private Long expiresAtEpochSeconds;
    private AlgorithmConfig algorithmConfig;
    private String requestId;
    private boolean autoLogin;
    private String autoLoginExpiresAt;
    private String boxPublicKey;
    private BoxLanInfo boxLanInfo;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
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

    public String getAoid() {
        return aoid;
    }

    public void setAoid(String aoid) {
        this.aoid = aoid;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getEncryptedSecret() {
        return encryptedSecret;
    }

    public void setEncryptedSecret(String encryptedSecret) {
        this.encryptedSecret = encryptedSecret;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getExpiresAtEpochSeconds() {
        return expiresAtEpochSeconds;
    }

    public void setExpiresAtEpochSeconds(Long expiresAtEpochSeconds) {
        this.expiresAtEpochSeconds = expiresAtEpochSeconds;
    }

    public AlgorithmConfig getAlgorithmConfig() {
        return algorithmConfig;
    }

    public void setAlgorithmConfig(AlgorithmConfig algorithmConfig) {
        this.algorithmConfig = algorithmConfig;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public boolean isAutoLogin() {
        return autoLogin;
    }

    public void setAutoLogin(boolean autoLogin) {
        this.autoLogin = autoLogin;
    }

    public String getAutoLoginExpiresAt() {
        return autoLoginExpiresAt;
    }

    public void setAutoLoginExpiresAt(String autoLoginExpiresAt) {
        this.autoLoginExpiresAt = autoLoginExpiresAt;
    }

    public String getBoxPublicKey() {
        return boxPublicKey;
    }

    public void setBoxPublicKey(String boxPublicKey) {
        this.boxPublicKey = boxPublicKey;
    }

    public BoxLanInfo getBoxLanInfo() {
        return boxLanInfo;
    }

    public void setBoxLanInfo(BoxLanInfo boxLanInfo) {
        this.boxLanInfo = boxLanInfo;
    }

    @Override
    public String toString() {
        return "LoginInfo{" +
                "accessToken='" + accessToken + '\'' +
                ", secretKey='" + secretKey + '\'' +
                ", domain='" + domain + '\'' +
                ", boxName='" + boxName + '\'' +
                ", boxUUID='" + boxUUID + '\'' +
                ", aoid='" + aoid + '\'' +
                ", refreshToken='" + refreshToken + '\'' +
                ", encryptedSecret='" + encryptedSecret + '\'' +
                ", expiresAt='" + expiresAt + '\'' +
                ", expiresAtEpochSeconds=" + expiresAtEpochSeconds +
                ", algorithmConfig=" + algorithmConfig +
                ", requestId='" + requestId + '\'' +
                ", autoLogin=" + autoLogin +
                ", autoLoginExpiresAt='" + autoLoginExpiresAt + '\'' +
                ", boxPublicKey='" + boxPublicKey + '\'' +
                '}';
    }
}
