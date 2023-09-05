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
 * date: 2022/5/12 17:57
 */
public class EulixBoxTokenDetail {
    private String boxUuid;
    private String boxBind;
    private String secretKey;
    private String accessToken;
    private long tokenExpire;
    private String refreshToken;
    private String initializationVector;
    private String transformation;

    public String getBoxUuid() {
        return boxUuid;
    }

    public void setBoxUuid(String boxUuid) {
        this.boxUuid = boxUuid;
    }

    public String getBoxBind() {
        return boxBind;
    }

    public void setBoxBind(String boxBind) {
        this.boxBind = boxBind;
    }

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

    @Override
    public String toString() {
        return "EulixBoxTokenDetail{" +
                "boxUuid='" + boxUuid + '\'' +
                ", boxBind='" + boxBind + '\'' +
                ", secretKey='" + secretKey + '\'' +
                ", accessToken='" + accessToken + '\'' +
                ", tokenExpire=" + tokenExpire +
                ", refreshToken='" + refreshToken + '\'' +
                ", initializationVector='" + initializationVector + '\'' +
                ", transformation='" + transformation + '\'' +
                '}';
    }
}
