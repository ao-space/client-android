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

package xyz.eulix.space.network.userinfo;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.network.files.BaseResponseBody;

/**
 * Author:      Zhu Fuyu
 * Description:
 * History:     2021/9/17
 */
public class GetUserInfoResponseBody extends BaseResponseBody implements Serializable, EulixKeep {
    @SerializedName("role")
    private String role;
    @SerializedName("personalName")
    private String personalName;
    @SerializedName("personalSign")
    private String personalSign;
    @SerializedName("requestId")
    private String requestId;

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPersonalName() {
        return personalName;
    }

    public void setPersonalName(String personalName) {
        this.personalName = personalName;
    }

    public String getPersonalSign() {
        return personalSign;
    }

    public void setPersonalSign(String personalSign) {
        this.personalSign = personalSign;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
