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
import java.util.Map;

import xyz.eulix.space.bean.bind.InitResponseNetwork;
import xyz.eulix.space.network.agent.disk.DiskManageListResult;
import xyz.eulix.space.network.disk.RaidInfoResult;

/**
 * @author: chenjiawei
 * Description: 增加字段需要更新EulixSpaceDBBoxManager
 * date: 2021/9/13 17:51
 */
public class EulixBoxInfo {
    private long totalSize;
    private long usedSize;
    private List<InitResponseNetwork> networks;
    private boolean isLAN;
    private String bluetoothAddress;
    private String bluetoothId;
    private String bluetoothDeviceName;
    private SpaceStatusStatusLineInfo spaceStatusStatusLineInfo;
    private SecurityEmailInfo securityEmailInfo;
    private SecurityPasswordInfo securityPasswordInfo;
    private DiskManageListResult diskManageListResult;
    private RaidInfoResult raidInfoResult;
    private DeviceAbility deviceAbility;
    private AOSpaceAccessBean aoSpaceAccessBean;
    private String stunAddress;
    private Map<String, String> stunAddressMap;

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

    public List<InitResponseNetwork> getNetworks() {
        return networks;
    }

    public void setNetworks(List<InitResponseNetwork> networks) {
        this.networks = networks;
    }

    public boolean isLAN() {
        return isLAN;
    }

    public void setLAN(boolean LAN) {
        isLAN = LAN;
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

    public SpaceStatusStatusLineInfo getSpaceStatusStatusLineInfo() {
        return spaceStatusStatusLineInfo;
    }

    public void setSpaceStatusStatusLineInfo(SpaceStatusStatusLineInfo spaceStatusStatusLineInfo) {
        this.spaceStatusStatusLineInfo = spaceStatusStatusLineInfo;
    }

    public SecurityEmailInfo getSecurityEmailInfo() {
        return securityEmailInfo;
    }

    public void setSecurityEmailInfo(SecurityEmailInfo securityEmailInfo) {
        this.securityEmailInfo = securityEmailInfo;
    }

    public SecurityPasswordInfo getSecurityPasswordInfo() {
        return securityPasswordInfo;
    }

    public void setSecurityPasswordInfo(SecurityPasswordInfo securityPasswordInfo) {
        this.securityPasswordInfo = securityPasswordInfo;
    }

    public DiskManageListResult getDiskManageListResult() {
        return diskManageListResult;
    }

    public void setDiskManageListResult(DiskManageListResult diskManageListResult) {
        this.diskManageListResult = diskManageListResult;
    }

    public RaidInfoResult getRaidInfoResult() {
        return raidInfoResult;
    }

    public void setRaidInfoResult(RaidInfoResult raidInfoResult) {
        this.raidInfoResult = raidInfoResult;
    }

    public DeviceAbility getDeviceAbility() {
        return deviceAbility;
    }

    public void setDeviceAbility(DeviceAbility deviceAbility) {
        this.deviceAbility = deviceAbility;
    }

    public AOSpaceAccessBean getAoSpaceAccessBean() {
        return aoSpaceAccessBean;
    }

    public void setAoSpaceAccessBean(AOSpaceAccessBean aoSpaceAccessBean) {
        this.aoSpaceAccessBean = aoSpaceAccessBean;
    }

    public String getStunAddress() {
        return stunAddress;
    }

    public void setStunAddress(String stunAddress) {
        this.stunAddress = stunAddress;
    }

    public Map<String, String> getStunAddressMap() {
        return stunAddressMap;
    }

    public void setStunAddressMap(Map<String, String> stunAddressMap) {
        this.stunAddressMap = stunAddressMap;
    }

    @Override
    public String toString() {
        return "EulixBoxInfo{" +
                "totalSize=" + totalSize +
                ", usedSize=" + usedSize +
                ", networks=" + networks +
                ", isLAN=" + isLAN +
                ", bluetoothAddress='" + bluetoothAddress + '\'' +
                ", bluetoothId='" + bluetoothId + '\'' +
                ", bluetoothDeviceName='" + bluetoothDeviceName + '\'' +
                ", spaceStatusStatusLineInfo=" + spaceStatusStatusLineInfo +
                ", securityEmailInfo=" + securityEmailInfo +
                ", securityPasswordInfo=" + securityPasswordInfo +
                ", diskManageListResult=" + diskManageListResult +
                ", raidInfoResult=" + raidInfoResult +
                ", deviceAbility=" + deviceAbility +
                ", aoSpaceAccessBean=" + aoSpaceAccessBean +
                ", stunAddress='" + stunAddress + '\'' +
                ", stunAddressMap=" + stunAddressMap +
                '}';
    }
}
