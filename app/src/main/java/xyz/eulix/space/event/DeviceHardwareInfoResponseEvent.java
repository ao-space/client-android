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
 * date: 2022/8/12 14:49
 */
public class DeviceHardwareInfoResponseEvent {
    private String requestUuid;
    private int code;
    private String codeSource;
    private String bluetoothId;

    public DeviceHardwareInfoResponseEvent(String requestUuid, int code, String codeSource, String bluetoothId) {
        this.requestUuid = requestUuid;
        this.code = code;
        this.codeSource = codeSource;
        this.bluetoothId = bluetoothId;
    }

    public String getRequestUuid() {
        return requestUuid;
    }

    public int getCode() {
        return code;
    }

    public String getCodeSource() {
        return codeSource;
    }

    public String getBluetoothId() {
        return bluetoothId;
    }
}
