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

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/9/30 9:40
 */
public class EulixBoxToken {
    public static final int IDENTITY_TRIAL = 1;
    private String secretKey;
    private String accessToken;
    private long tokenExpire;
    private String refreshToken;
    private String initializationVector;
    private String transformation;
    // 仅扫码登录者使用，long型数据，为0表示没有序列化token，上述字段存储在RAM里
    private String loginValid;
    // 仅试用用户使用，true表示试用
    private int identity;

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public long getTokenExpire() {
        return tokenExpire;
    }

    public void setTokenExpire(long tokenExpire) {
        this.tokenExpire = tokenExpire;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getInitializationVector() {
        return initializationVector;
    }

    public void setInitializationVector(String initializationVector) {
        this.initializationVector = initializationVector;
    }

    public String getTransformation() {
        return transformation;
    }

    public void setTransformation(String transformation) {
        this.transformation = transformation;
    }

    public String getLoginValid() {
        return loginValid;
    }

    public void setLoginValid(String loginValid) {
        this.loginValid = loginValid;
    }

    public int getIdentity() {
        return identity;
    }

    public void setIdentity(int identity) {
        this.identity = identity;
    }

    @Override
    public String toString() {
        return "EulixBoxToken{" +
                "secretKey='" + secretKey + '\'' +
                ", accessToken='" + accessToken + '\'' +
                ", tokenExpire=" + tokenExpire +
                ", refreshToken='" + refreshToken + '\'' +
                ", initializationVector='" + initializationVector + '\'' +
                ", transformation='" + transformation + '\'' +
                ", loginValid='" + loginValid + '\'' +
                ", identity=" + identity +
                '}';
    }
}
