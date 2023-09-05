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
 * date: 2022/8/12 15:22
 */
public class DeviceHardwareInfoRequestEvent {
    private String boxUuid;
    private String boxBind;
    private String requestUuid;
    private boolean isFore;

    public DeviceHardwareInfoRequestEvent(String boxUuid, String boxBind) {
        this(boxUuid, boxBind, null);
    }

    public DeviceHardwareInfoRequestEvent(String boxUuid, String boxBind, boolean isFore) {
        this(boxUuid, boxBind, null, isFore);
    }

    public DeviceHardwareInfoRequestEvent(String boxUuid, String boxBind, String requestUuid) {
        this(boxUuid, boxBind, requestUuid, false);
    }

    public DeviceHardwareInfoRequestEvent(String boxUuid, String boxBind, String requestUuid, boolean isFore) {
        this.boxUuid = boxUuid;
        this.boxBind = boxBind;
        this.requestUuid = requestUuid;
        this.isFore = isFore;
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

    public boolean isFore() {
        return isFore;
    }
}
