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
 * @author: chenjiawei
 * Description:
 * date: 2022/5/17 14:46
 */
public class AuthAutoLoginConfirmResponseBody implements Serializable, EulixKeep {
    @SerializedName("code")
    private String code;
    @SerializedName("message")
    private String message;
    @SerializedName("requestId")
    private String requestId;
    @SerializedName("results")
    private AuthAutoLoginConfirmResult results;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public AuthAutoLoginConfirmResult getResults() {
        return results;
    }

    public void setResults(AuthAutoLoginConfirmResult results) {
        this.results = results;
    }

    @Override
    public String toString() {
        return "AuthAutoLoginConfirmResponseBody{" +
                "code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", requestId='" + requestId + '\'' +
                ", results=" + results +
                '}';
    }
}
