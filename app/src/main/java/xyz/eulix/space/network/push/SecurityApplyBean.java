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

package xyz.eulix.space.network.push;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.network.security.SecurityTokenResult;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/7/21 15:02
 */
public class SecurityApplyBean implements EulixKeep {
    private SecurityTokenResult securityTokenRes;
    private String authUserId;
    private String authClientUUid;
    private String authDeviceInfo;
    private String requestId;
    private String applyId;

    public SecurityTokenResult getSecurityTokenRes() {
        return securityTokenRes;
    }

    public void setSecurityTokenRes(SecurityTokenResult securityTokenRes) {
        this.securityTokenRes = securityTokenRes;
    }

    public String getAuthUserId() {
        return authUserId;
    }

    public void setAuthUserId(String authUserId) {
        this.authUserId = authUserId;
    }

    public String getAuthClientUUid() {
        return authClientUUid;
    }

    public void setAuthClientUUid(String authClientUUid) {
        this.authClientUUid = authClientUUid;
    }

    public String getAuthDeviceInfo() {
        return authDeviceInfo;
    }

    public void setAuthDeviceInfo(String authDeviceInfo) {
        this.authDeviceInfo = authDeviceInfo;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getApplyId() {
        return applyId;
    }

    public void setApplyId(String applyId) {
        this.applyId = applyId;
    }

    @Override
    public String toString() {
        return "SecurityApplyBean{" +
                "securityTokenRes=" + securityTokenRes +
                ", authUserId='" + authUserId + '\'' +
                ", authClientUUid='" + authClientUUid + '\'' +
                ", authDeviceInfo='" + authDeviceInfo + '\'' +
                ", requestId='" + requestId + '\'' +
                ", applyId='" + applyId + '\'' +
                '}';
    }
}
