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
 * date: 2022/11/3 10:27
 */
public class AccessTokenResultEvent {
    private String boxUuid;
    private String boxBind;
    // true：成功；false：失败；null：错误
    private Boolean result;
    private long expireTimestamp;

    public AccessTokenResultEvent(String boxUuid, String boxBind, Boolean result, long expireTimestamp) {
        this.boxUuid = boxUuid;
        this.boxBind = boxBind;
        this.result = result;
        this.expireTimestamp = expireTimestamp;
    }

    public String getBoxUuid() {
        return boxUuid;
    }

    public String getBoxBind() {
        return boxBind;
    }

    public Boolean getResult() {
        return result;
    }

    public long getExpireTimestamp() {
        return expireTimestamp;
    }
}
