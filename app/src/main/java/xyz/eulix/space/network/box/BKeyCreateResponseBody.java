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
 * Description:
 * date: 2021/8/19 16:59
 */
public class BKeyCreateResponseBody implements EulixKeep {
    private AuthCodeInfo authCodeInfo;
    private int code;
    private String message;

    public AuthCodeInfo getAuthCodeInfo() {
        return authCodeInfo;
    }

    public void setAuthCodeInfo(AuthCodeInfo authCodeInfo) {
        this.authCodeInfo = authCodeInfo;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "BKeyCreateResponseBody{" +
                "authCodeInfo=" + authCodeInfo +
                ", code=" + code +
                ", message='" + message + '\'' +
                '}';
    }
}
