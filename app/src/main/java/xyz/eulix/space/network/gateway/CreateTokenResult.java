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
 * date: 2021/7/6 17:55
 */
public class CreateTokenResult implements EulixKeep {
    private String accessToken;
    private AlgorithmConfig algorithmConfig;
    private String encryptedSecret;
    private String expiresAt;
    private Long expiresAtEpochSeconds;
    private String refreshToken;
    private String requestId;
    private String autoLoginExpiresAt;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public AlgorithmConfig getAlgorithmConfig() {
        return algorithmConfig;
    }

    public void setAlgorithmConfig(AlgorithmConfig algorithmConfig) {
        this.algorithmConfig = algorithmConfig;
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

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getAutoLoginExpiresAt() {
        return autoLoginExpiresAt;
    }

    public void setAutoLoginExpiresAt(String autoLoginExpiresAt) {
        this.autoLoginExpiresAt = autoLoginExpiresAt;
    }

    @Override
    public String toString() {
        return "CreateTokenResult{" +
                "accessToken='" + accessToken + '\'' +
                ", algorithmConfig=" + algorithmConfig +
                ", encryptedSecret='" + encryptedSecret + '\'' +
                ", expiresAt='" + expiresAt + '\'' +
                ", expiresAtEpochSeconds=" + expiresAtEpochSeconds +
                ", refreshToken='" + refreshToken + '\'' +
                ", requestId='" + requestId + '\'' +
                ", autoLoginExpiresAt='" + autoLoginExpiresAt + '\'' +
                '}';
    }
}
