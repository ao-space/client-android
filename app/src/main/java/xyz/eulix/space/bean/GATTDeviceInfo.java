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

package xyz.eulix.space.bean;

/**
 * @author: chenjiawei
 * date: 2021/6/11 15:41
 */
public class GATTDeviceInfo {
    private String serviceUUID;
    private String characteristicUUID;
    private String descriptorUUID;
    private int characteristicType;
    private int characteristicProperty;
    private boolean enable;
    private String writeValue;
    private byte[] writeValueByteArray;

    public String getServiceUUID() {
        return serviceUUID;
    }

    public void setServiceUUID(String serviceUUID) {
        this.serviceUUID = serviceUUID;
    }

    public String getCharacteristicUUID() {
        return characteristicUUID;
    }

    public void setCharacteristicUUID(String characteristicUUID) {
        this.characteristicUUID = characteristicUUID;
    }

    public String getDescriptorUUID() {
        return descriptorUUID;
    }

    public void setDescriptorUUID(String descriptorUUID) {
        this.descriptorUUID = descriptorUUID;
    }

    public int getCharacteristicType() {
        return characteristicType;
    }

    public void setCharacteristicType(int characteristicType) {
        this.characteristicType = characteristicType;
    }

    public int getCharacteristicProperty() {
        return characteristicProperty;
    }

    public void setCharacteristicProperty(int characteristicProperty) {
        this.characteristicProperty = characteristicProperty;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getWriteValue() {
        return writeValue;
    }

    public void setWriteValue(String writeValue) {
        this.writeValue = writeValue;
    }

    public byte[] getWriteValueByteArray() {
        return writeValueByteArray;
    }

    public void setWriteValueByteArray(byte[] writeValueByteArray) {
        this.writeValueByteArray = writeValueByteArray;
    }
}
