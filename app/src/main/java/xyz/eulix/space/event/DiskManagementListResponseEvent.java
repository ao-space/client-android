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
 * date: 2022/11/8 16:01
 */
public class DiskManagementListResponseEvent {
    private int code;
    private String source;
    private String boxUuid;
    private String boxBind;
    private String requestUuid;

    public DiskManagementListResponseEvent(int code, String source, String boxUuid, String boxBind, String requestUuid) {
        this.code = code;
        this.source = source;
        this.boxUuid = boxUuid;
        this.boxBind = boxBind;
        this.requestUuid = requestUuid;
    }

    public int getCode() {
        return code;
    }

    public String getSource() {
        return source;
    }

    public String getBoxUuid() {
        return boxUuid;
    }

    public String getBoxBind() {
        return boxBind;
    }

    public String getRequestUuid() {
        return requestUuid;
    }
}
