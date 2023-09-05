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

import java.util.List;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/9/22 16:52
 */
public class EulixDeviceManageInfo {
    private Boolean isOnline;
    private String boxUuid;
    private String boxBind;
    private String boxName;
    private long totalSize;
    private long usedSize;
    private List<String> networkSsids;
    private List<String> networkIpAddresses;
    private List<NetworkAccessBean> networkAccessBeans;
    private String bluetoothAddress;
    private String bluetoothId;
    private String bluetoothDeviceName;
    private SecurityEmailInfo securityEmailInfo;
    private DeviceAbility deviceAbility;

    public Boolean getOnline() {
        return isOnline;
    }

    public void setOnline(Boolean online) {
        isOnline = online;
    }

    public String getBoxUuid() {
        return boxUuid;
    }

    public void setBoxUuid(String boxUuid) {
        this.boxUuid = boxUuid;
    }

    public String getBoxBind() {
        return boxBind;
    }

    public void setBoxBind(String boxBind) {
        this.boxBind = boxBind;
    }

    public String getBoxName() {
        return boxName;
    }

    public void setBoxName(String boxName) {
        this.boxName = boxName;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long getUsedSize() {
        return usedSize;
    }

    public void setUsedSize(long usedSize) {
        this.usedSize = usedSize;
    }

    public List<String> getNetworkSsids() {
        return networkSsids;
    }

    public void setNetworkSsids(List<String> networkSsids) {
        this.networkSsids = networkSsids;
    }

    public List<String> getNetworkIpAddresses() {
        return networkIpAddresses;
    }

    public void setNetworkIpAddresses(List<String> networkIpAddresses) {
        this.networkIpAddresses = networkIpAddresses;
    }

    public List<NetworkAccessBean> getNetworkAccessBeans() {
        return networkAccessBeans;
    }

    public void setNetworkAccessBeans(List<NetworkAccessBean> networkAccessBeans) {
        this.networkAccessBeans = networkAccessBeans;
    }

    public String getBluetoothAddress() {
        return bluetoothAddress;
    }

    public void setBluetoothAddress(String bluetoothAddress) {
        this.bluetoothAddress = bluetoothAddress;
    }

    public String getBluetoothId() {
        return bluetoothId;
    }

    public void setBluetoothId(String bluetoothId) {
        this.bluetoothId = bluetoothId;
    }

    public String getBluetoothDeviceName() {
        return bluetoothDeviceName;
    }

    public void setBluetoothDeviceName(String bluetoothDeviceName) {
        this.bluetoothDeviceName = bluetoothDeviceName;
    }

    public SecurityEmailInfo getSecurityEmailInfo() {
        return securityEmailInfo;
    }

    public void setSecurityEmailInfo(SecurityEmailInfo securityEmailInfo) {
        this.securityEmailInfo = securityEmailInfo;
    }

    public DeviceAbility getDeviceAbility() {
        return deviceAbility;
    }

    public void setDeviceAbility(DeviceAbility deviceAbility) {
        this.deviceAbility = deviceAbility;
    }

    @Override
    public String toString() {
        return "EulixDeviceManageInfo{" +
                "isOnline=" + isOnline +
                ", boxUuid='" + boxUuid + '\'' +
                ", boxBind='" + boxBind + '\'' +
                ", boxName='" + boxName + '\'' +
                ", totalSize=" + totalSize +
                ", usedSize=" + usedSize +
                ", networkSsids=" + networkSsids +
                ", networkIpAddresses=" + networkIpAddresses +
                ", networkAccessBeans=" + networkAccessBeans +
                ", bluetoothAddress='" + bluetoothAddress + '\'' +
                ", bluetoothId='" + bluetoothId + '\'' +
                ", bluetoothDeviceName='" + bluetoothDeviceName + '\'' +
                ", securityEmailInfo=" + securityEmailInfo +
                ", deviceAbility=" + deviceAbility +
                '}';
    }
}
