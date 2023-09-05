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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.util.BooleanUtil;
import xyz.eulix.space.util.IntegerUtil;
import xyz.eulix.space.util.StringUtil;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/12/7 18:21
 */
public class DeviceAbility implements EulixKeep {
    // 第一代
    public static final int DEVICE_MODEL_NUMBER_GEN_1 = 100;
    // 第二代
    public static final int DEVICE_MODEL_NUMBER_GEN_2 = 200;
    // 从哪个型号开始支持 sn 号
    public static final int DEVICE_MODEL_NUMBER_SUPPORTED_FROM_MODEL_NUMBER = 210;
    // 虚拟机版本
    public static final int DEVICE_MODEL_NUMBER_GEN_VM = -100;
    // 云试用容器版本
    public static final int DEVICE_MODEL_NUMBER_GEN_CLOUD_DOCKER = -200;
    // PC容器版本
    public static final int DEVICE_MODEL_NUMBER_GEN_PC_DOCKER = -300;

    // GXX，G表示代数，产品型号数字(内部使用, 1xx: 树莓派, 2xx: 二代, ...)
    private Integer deviceModelNumber;
    private String snNumber;
    // 内部磁盘支持(SATA 和 m.2)
    private Boolean innerDiskSupport;
    // 是否支持 USB 磁盘. 210型号 及以上不支持
    private Boolean supportUSBDisk;
    // 支持加密芯片
    private Boolean securityChipSupport;
    // 以容器方式运行
    private Boolean runInDocker;
    // 当前设备是否支持蓝牙
    private Boolean bluetoothSupport;
    // 当前设备是否支持网络配置
    private Boolean networkConfigSupport;
    // 当前设备是否支持 Led
    private Boolean ledSupport;
    // 当前设备是否支持备份恢复
    private Boolean backupRestoreSupport;
    // 当前设备是否支持傲空间应用
    private Boolean aospaceappSupport;
    // 是否支持开发者选项
    private Boolean aospaceDevOptionSupport;
    // 是否支持切换平台
    private Boolean aospaceSwitchPlatformSupport;
    // 当前设备是否支持升级API
    private Boolean upgradeApiSupport;
    //是否为开源版本
    private boolean openSource = false;

    public static boolean compare(DeviceAbility deviceAbility1, DeviceAbility deviceAbility2) {
        boolean isEqual = false;
        if (deviceAbility1 == null && deviceAbility2 == null) {
            isEqual = true;
        } else if (deviceAbility1 != null && deviceAbility2 != null) {
            isEqual = (IntegerUtil.compare(deviceAbility1.deviceModelNumber, deviceAbility2.deviceModelNumber)
                    && StringUtil.compare(deviceAbility1.snNumber, deviceAbility2.snNumber)
                    && BooleanUtil.compare(deviceAbility1.innerDiskSupport, deviceAbility2.innerDiskSupport)
                    && BooleanUtil.compare(deviceAbility1.supportUSBDisk, deviceAbility2.supportUSBDisk)
                    && BooleanUtil.compare(deviceAbility1.securityChipSupport, deviceAbility2.securityChipSupport)
                    && BooleanUtil.compare(deviceAbility1.runInDocker, deviceAbility2.runInDocker)
                    && BooleanUtil.compare(deviceAbility1.bluetoothSupport, deviceAbility2.bluetoothSupport)
                    && BooleanUtil.compare(deviceAbility1.networkConfigSupport, deviceAbility2.networkConfigSupport)
                    && BooleanUtil.compare(deviceAbility1.ledSupport, deviceAbility2.ledSupport)
                    && BooleanUtil.compare(deviceAbility1.backupRestoreSupport, deviceAbility2.backupRestoreSupport)
                    && BooleanUtil.compare(deviceAbility1.aospaceappSupport, deviceAbility2.aospaceappSupport)
                    && BooleanUtil.compare(deviceAbility1.aospaceDevOptionSupport, deviceAbility2.aospaceDevOptionSupport)
                    && BooleanUtil.compare(deviceAbility1.aospaceSwitchPlatformSupport, deviceAbility2.aospaceSwitchPlatformSupport)
                    && BooleanUtil.compare(deviceAbility1.upgradeApiSupport, deviceAbility2.upgradeApiSupport)
                    && BooleanUtil.compare(deviceAbility1.openSource, deviceAbility2.openSource));
        }
        return isEqual;
    }

    @NonNull
    public DeviceAbility cloneInstance() {
        DeviceAbility deviceAbility = new DeviceAbility();
        deviceAbility.setDeviceModelNumber(getDeviceModelNumber());
        deviceAbility.setSnNumber(getSnNumber());
        deviceAbility.setInnerDiskSupport(getInnerDiskSupport());
        deviceAbility.setSupportUSBDisk(getSupportUSBDisk());
        deviceAbility.setSecurityChipSupport(getSecurityChipSupport());
        deviceAbility.setRunInDocker(getRunInDocker());
        deviceAbility.setBluetoothSupport(getBluetoothSupport());
        deviceAbility.setNetworkConfigSupport(getNetworkConfigSupport());
        deviceAbility.setLedSupport(getLedSupport());
        deviceAbility.setBackupRestoreSupport(getBackupRestoreSupport());
        deviceAbility.setAospaceappSupport(getAospaceappSupport());
        deviceAbility.setAospaceDevOptionSupport(getAospaceDevOptionSupport());
        deviceAbility.setAospaceSwitchPlatformSupport(getAospaceSwitchPlatformSupport());
        deviceAbility.setUpgradeApiSupport(getUpgradeApiSupport());
        deviceAbility.setOpenSource(getOpenSource());
        return deviceAbility;
    }

    @NonNull
    public DeviceAbility generateDefault() {
        DeviceAbility deviceAbility = cloneInstance();
        Integer nDeviceModelNumberValue = deviceAbility.getDeviceModelNumber();
        int nDeviceModelNumber = (nDeviceModelNumberValue == null ? 0 : nDeviceModelNumberValue);
        if (nDeviceModelNumberValue == null) {
            deviceAbility.setDeviceModelNumber(0);
        }
        if (deviceAbility.getInnerDiskSupport() == null) {
            deviceAbility.setInnerDiskSupport(false);
        }
        if (deviceAbility.getSupportUSBDisk() == null) {
            deviceAbility.setSupportUSBDisk(nDeviceModelNumber >= DEVICE_MODEL_NUMBER_GEN_2 && nDeviceModelNumber < DEVICE_MODEL_NUMBER_SUPPORTED_FROM_MODEL_NUMBER);
        }
        if (deviceAbility.getSecurityChipSupport() == null) {
            deviceAbility.setSecurityChipSupport(nDeviceModelNumber >= DEVICE_MODEL_NUMBER_GEN_2);
        }
        if (deviceAbility.getRunInDocker() == null) {
            deviceAbility.setRunInDocker(nDeviceModelNumber <= DEVICE_MODEL_NUMBER_GEN_CLOUD_DOCKER);
        }
        if (deviceAbility.getBluetoothSupport() == null) {
            deviceAbility.setBluetoothSupport(nDeviceModelNumber > DEVICE_MODEL_NUMBER_GEN_VM);
        }
        if (deviceAbility.getNetworkConfigSupport() == null) {
            deviceAbility.setNetworkConfigSupport(nDeviceModelNumber > DEVICE_MODEL_NUMBER_GEN_VM);
        }
        if (deviceAbility.getLedSupport() == null) {
            deviceAbility.setLedSupport(nDeviceModelNumber >= DEVICE_MODEL_NUMBER_SUPPORTED_FROM_MODEL_NUMBER);
        }
        if (deviceAbility.getBackupRestoreSupport() == null) {
            deviceAbility.setBackupRestoreSupport(nDeviceModelNumber > DEVICE_MODEL_NUMBER_GEN_CLOUD_DOCKER);
        }
        if (deviceAbility.getAospaceappSupport() == null) {
            deviceAbility.setAospaceappSupport(true);
        }
        if (deviceAbility.getAospaceDevOptionSupport() == null) {
            deviceAbility.setAospaceDevOptionSupport(true);
        }
        if (deviceAbility.getAospaceSwitchPlatformSupport() == null) {
            deviceAbility.setAospaceSwitchPlatformSupport(nDeviceModelNumber > DEVICE_MODEL_NUMBER_GEN_PC_DOCKER);
        }
        if (deviceAbility.getUpgradeApiSupport() == null) {
            deviceAbility.setUpgradeApiSupport(nDeviceModelNumber > DEVICE_MODEL_NUMBER_GEN_CLOUD_DOCKER);
        }
        if (deviceAbility.getOpenSource() == null) {
            deviceAbility.setOpenSource(false);
        }
        return deviceAbility;
    }

    @Nullable
    public static DeviceAbility generateDefault(DeviceAbility deviceAbility) {
        if (deviceAbility == null) {
            return null;
        } else {
            return deviceAbility.generateDefault();
        }
    }

    public Integer getDeviceModelNumber() {
        return deviceModelNumber;
    }

    public void setDeviceModelNumber(Integer deviceModelNumber) {
        this.deviceModelNumber = deviceModelNumber;
    }

    public String getSnNumber() {
        return snNumber;
    }

    public void setSnNumber(String snNumber) {
        this.snNumber = snNumber;
    }

    public Boolean getInnerDiskSupport() {
        return innerDiskSupport;
    }

    public void setInnerDiskSupport(Boolean innerDiskSupport) {
        this.innerDiskSupport = innerDiskSupport;
    }

    public Boolean getSupportUSBDisk() {
        return supportUSBDisk;
    }

    public void setSupportUSBDisk(Boolean supportUSBDisk) {
        this.supportUSBDisk = supportUSBDisk;
    }

    public Boolean getSecurityChipSupport() {
        return securityChipSupport;
    }

    public void setSecurityChipSupport(Boolean securityChipSupport) {
        this.securityChipSupport = securityChipSupport;
    }

    public Boolean getRunInDocker() {
        return runInDocker;
    }

    public void setRunInDocker(Boolean runInDocker) {
        this.runInDocker = runInDocker;
    }

    public Boolean getBluetoothSupport() {
        return bluetoothSupport;
    }

    public void setBluetoothSupport(Boolean bluetoothSupport) {
        this.bluetoothSupport = bluetoothSupport;
    }

    public Boolean getNetworkConfigSupport() {
        return networkConfigSupport;
    }

    public void setNetworkConfigSupport(Boolean networkConfigSupport) {
        this.networkConfigSupport = networkConfigSupport;
    }

    public Boolean getLedSupport() {
        return ledSupport;
    }

    public void setLedSupport(Boolean ledSupport) {
        this.ledSupport = ledSupport;
    }

    public Boolean getBackupRestoreSupport() {
        return backupRestoreSupport;
    }

    public void setBackupRestoreSupport(Boolean backupRestoreSupport) {
        this.backupRestoreSupport = backupRestoreSupport;
    }

    public Boolean getAospaceappSupport() {
        return aospaceappSupport;
    }

    public void setAospaceappSupport(Boolean aospaceappSupport) {
        this.aospaceappSupport = aospaceappSupport;
    }

    public Boolean getAospaceDevOptionSupport() {
        return aospaceDevOptionSupport;
    }

    public void setAospaceDevOptionSupport(Boolean aospaceDevOptionSupport) {
        this.aospaceDevOptionSupport = aospaceDevOptionSupport;
    }

    public Boolean getAospaceSwitchPlatformSupport() {
        return aospaceSwitchPlatformSupport;
    }

    public void setAospaceSwitchPlatformSupport(Boolean aospaceSwitchPlatformSupport) {
        this.aospaceSwitchPlatformSupport = aospaceSwitchPlatformSupport;
    }

    public Boolean getUpgradeApiSupport() {
        return upgradeApiSupport;
    }

    public void setUpgradeApiSupport(Boolean upgradeApiSupport) {
        this.upgradeApiSupport = upgradeApiSupport;
    }

    public Boolean getOpenSource() {
        return openSource;
    }

    public void setOpenSource(Boolean openSource) {
        this.openSource = openSource;
    }

    @Override
    public String toString() {
        return "DeviceAbility{" +
                "deviceModelNumber=" + deviceModelNumber +
                ", snNumber='" + snNumber + '\'' +
                ", innerDiskSupport=" + innerDiskSupport +
                ", supportUSBDisk=" + supportUSBDisk +
                ", securityChipSupport=" + securityChipSupport +
                ", runInDocker=" + runInDocker +
                ", bluetoothSupport=" + bluetoothSupport +
                ", networkConfigSupport=" + networkConfigSupport +
                ", ledSupport=" + ledSupport +
                ", backupRestoreSupport=" + backupRestoreSupport +
                ", aospaceappSupport=" + aospaceappSupport +
                ", aospaceDevOptionSupport=" + aospaceDevOptionSupport +
                ", aospaceSwitchPlatformSupport=" + aospaceSwitchPlatformSupport +
                ", upgradeApiSupport=" + upgradeApiSupport +
                ", openSource=" + openSource +
                '}';
    }
}
