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

package xyz.eulix.space.event;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/5/18 11:15
 */
public class AuthAutoLoginEvent {
    private String boxUuid;
    private String boxBind;
    private int code;
    private long expireTimestamp;
    private boolean isForce;
    private Integer httpCode;

    public AuthAutoLoginEvent(String boxUuid, String boxBind, int code, long expireTimestamp, boolean isForce) {
        this.boxUuid = boxUuid;
        this.boxBind = boxBind;
        this.code = code;
        this.expireTimestamp = expireTimestamp;
        this.isForce = isForce;
        this.httpCode = null;
    }

    public AuthAutoLoginEvent(String boxUuid, String boxBind, int code, long expireTimestamp, boolean isForce, Integer httpCode) {
        this.boxUuid = boxUuid;
        this.boxBind = boxBind;
        this.code = code;
        this.expireTimestamp = expireTimestamp;
        this.isForce = isForce;
        this.httpCode = httpCode;
    }

    public String getBoxUuid() {
        return boxUuid;
    }

    public String getBoxBind() {
        return boxBind;
    }

    public int getCode() {
        return code;
    }

    public long getExpireTimestamp() {
        return expireTimestamp;
    }

    public boolean isForce() {
        return isForce;
    }

    public Integer getHttpCode() {
        return httpCode;
    }
}
