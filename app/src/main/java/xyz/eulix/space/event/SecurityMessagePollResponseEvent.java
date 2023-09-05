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

import xyz.eulix.space.network.security.SecurityMessagePollResult;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/8/16 17:37
 */
public class SecurityMessagePollResponseEvent {
    private String codeSource;
    private int code;
    private String requestUuid;
    private SecurityMessagePollResult result;

    public SecurityMessagePollResponseEvent(String codeSource, int code, String requestUuid, SecurityMessagePollResult result) {
        this.codeSource = codeSource;
        this.code = code;
        this.requestUuid = requestUuid;
        this.result = result;
    }

    public String getCodeSource() {
        return codeSource;
    }

    public int getCode() {
        return code;
    }

    public String getRequestUuid() {
        return requestUuid;
    }

    public SecurityMessagePollResult getResult() {
        return result;
    }
}
