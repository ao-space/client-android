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

package xyz.eulix.space.network.agent.disk;

import java.util.List;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/10/31 14:30
 */
public class DiskInfo implements EulixKeep {
    public static final int BUS_NUMBER_UNKNOWN = -1;
    public static final int BUS_NUMBER_SATA_0 = 0;
    public static final int BUS_NUMBER_SATA_4 = BUS_NUMBER_SATA_0 + 1;
    public static final int BUS_NUMBER_SATA_8 = BUS_NUMBER_SATA_4 + 1;
    public static final int BUS_NUMBER_M2 = 101;
    // sata硬盘总线号码. -1： unknown; 0: in sata 0; 1: in sata 4; 2: in sata 8; 101: m.2;
    protected Integer busNumber;
    protected String deviceModel;
    // 设备名称
    protected String deviceName;
    protected String diskUniId;
    // 用来初始化
    protected String hwId;
    protected String modelFamily;
    protected String modelNumber;
    protected List<String> partedNames;
    protected List<String> partedUniIds;
    protected String serialNumber;
    /**
     传输类型 1: usb, 2: sata, 3: nvme
     目前根据这个字段来判断 1:磁盘 1，2:磁盘 2，3: SSD，等拿到真实的板子后，可能有变动
     */
    protected Integer transportType;

    protected String displayName;

    public Integer getBusNumber() {
        return busNumber;
    }

    public void setBusNumber(Integer busNumber) {
        this.busNumber = busNumber;
    }

    public String getDeviceModel() {
        return deviceModel;
    }

    public void setDeviceModel(String deviceModel) {
        this.deviceModel = deviceModel;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDiskUniId() {
        return diskUniId;
    }

    public void setDiskUniId(String diskUniId) {
        this.diskUniId = diskUniId;
    }

    public String getHwId() {
        return hwId;
    }

    public void setHwId(String hwId) {
        this.hwId = hwId;
    }

    public String getModelFamily() {
        return modelFamily;
    }

    public void setModelFamily(String modelFamily) {
        this.modelFamily = modelFamily;
    }

    public String getModelNumber() {
        return modelNumber;
    }

    public void setModelNumber(String modelNumber) {
        this.modelNumber = modelNumber;
    }

    public List<String> getPartedNames() {
        return partedNames;
    }

    public void setPartedNames(List<String> partedNames) {
        this.partedNames = partedNames;
    }

    public List<String> getPartedUniIds() {
        return partedUniIds;
    }

    public void setPartedUniIds(List<String> partedUniIds) {
        this.partedUniIds = partedUniIds;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Integer getTransportType() {
        return transportType;
    }

    public void setTransportType(Integer transportType) {
        this.transportType = transportType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return "DiskInfo{" +
                "busNumber=" + busNumber +
                ", deviceModel='" + deviceModel + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", diskUniId='" + diskUniId + '\'' +
                ", hwId='" + hwId + '\'' +
                ", modelFamily='" + modelFamily + '\'' +
                ", modelNumber='" + modelNumber + '\'' +
                ", partedNames=" + partedNames +
                ", partedUniIds=" + partedUniIds +
                ", serialNumber='" + serialNumber + '\'' +
                ", transportType=" + transportType +
                ", displayName='" + displayName + '\'' +
                '}';
    }
}
