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

package xyz.eulix.space.network.security;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/8/11 17:58
 */
public class DeviceHardwareInfoResult implements EulixKeep, Serializable {
    @SerializedName("btid")
    private String bluetoothId;

    public String getBluetoothId() {
        return bluetoothId;
    }

    public void setBluetoothId(String bluetoothId) {
        this.bluetoothId = bluetoothId;
    }

    @Override
    public String toString() {
        return "DeviceHardwareInfoResult{" +
                "bluetoothId='" + bluetoothId + '\'' +
                '}';
    }
}
