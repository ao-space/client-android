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

package xyz.eulix.space.network.files;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * date: 2021/6/23 14:18
 */
public class BaseResponseBody implements EulixKeep {
    protected Integer codeInt;
    @SerializedName("code")
    protected String code;
    protected String message;
    protected String requestId;

    public Integer getCodeInt() {
        codeInt = 0;
        if (!TextUtils.isEmpty(code)) {
            if (code.contains("-")) {
                int index = code.indexOf("-");
                String codeWithoutSuffix = code.substring(index + 1);
                codeInt = Integer.parseInt(codeWithoutSuffix);
            } else {
                codeInt = Integer.valueOf(code);
            }
        }
        return codeInt;
    }

    public void setCodeInt(Integer codeInt) {
        this.codeInt = codeInt;
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "BaseResponseBody{" +
                "codeInt=" + codeInt +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}
