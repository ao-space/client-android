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
 * date: 2022/10/31 15:43
 */
public class DiskManageListResult implements EulixKeep {
    private String createdTime;
    // 1：加密；2：未加密
    private Integer diskEncrypt;
    private Integer diskInitialCode;
    private String diskInitialMessage;
    private Integer diskInitialProgress;
    private List<DiskManageInfo> diskManageInfos;
    private Boolean isMissingMainStorage;
    private List<String> PrimaryStorageHwIds;
    private String primaryStorageMountPaths;
    private List<String> secondaryStorageHwIds;
    private List<String> secondaryStorageMountPaths;
    private List<String> raidDiskHwIds;
    // 1：最大容量；2：双盘互备
    private Integer raidType;
    private String updatedTime;

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public Integer getDiskEncrypt() {
        return diskEncrypt;
    }

    public void setDiskEncrypt(Integer diskEncrypt) {
        this.diskEncrypt = diskEncrypt;
    }

    public Integer getDiskInitialCode() {
        return diskInitialCode;
    }

    public void setDiskInitialCode(Integer diskInitialCode) {
        this.diskInitialCode = diskInitialCode;
    }

    public String getDiskInitialMessage() {
        return diskInitialMessage;
    }

    public void setDiskInitialMessage(String diskInitialMessage) {
        this.diskInitialMessage = diskInitialMessage;
    }

    public Integer getDiskInitialProgress() {
        return diskInitialProgress;
    }

    public void setDiskInitialProgress(Integer diskInitialProgress) {
        this.diskInitialProgress = diskInitialProgress;
    }

    public List<DiskManageInfo> getDiskManageInfos() {
        return diskManageInfos;
    }

    public void setDiskManageInfos(List<DiskManageInfo> diskManageInfos) {
        this.diskManageInfos = diskManageInfos;
    }

    public Boolean getMissingMainStorage() {
        return isMissingMainStorage;
    }

    public void setMissingMainStorage(Boolean missingMainStorage) {
        isMissingMainStorage = missingMainStorage;
    }

    public List<String> getPrimaryStorageHwIds() {
        return PrimaryStorageHwIds;
    }

    public void setPrimaryStorageHwIds(List<String> primaryStorageHwIds) {
        PrimaryStorageHwIds = primaryStorageHwIds;
    }

    public String getPrimaryStorageMountPaths() {
        return primaryStorageMountPaths;
    }

    public void setPrimaryStorageMountPaths(String primaryStorageMountPaths) {
        this.primaryStorageMountPaths = primaryStorageMountPaths;
    }

    public List<String> getSecondaryStorageHwIds() {
        return secondaryStorageHwIds;
    }

    public void setSecondaryStorageHwIds(List<String> secondaryStorageHwIds) {
        this.secondaryStorageHwIds = secondaryStorageHwIds;
    }

    public List<String> getSecondaryStorageMountPaths() {
        return secondaryStorageMountPaths;
    }

    public void setSecondaryStorageMountPaths(List<String> secondaryStorageMountPaths) {
        this.secondaryStorageMountPaths = secondaryStorageMountPaths;
    }

    public List<String> getRaidDiskHwIds() {
        return raidDiskHwIds;
    }

    public void setRaidDiskHwIds(List<String> raidDiskHwIds) {
        this.raidDiskHwIds = raidDiskHwIds;
    }

    public Integer getRaidType() {
        return raidType;
    }

    public void setRaidType(Integer raidType) {
        this.raidType = raidType;
    }

    public String getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(String updatedTime) {
        this.updatedTime = updatedTime;
    }

    @Override
    public String toString() {
        return "DiskManageListResult{" +
                "createdTime='" + createdTime + '\'' +
                ", diskEncrypt=" + diskEncrypt +
                ", diskInitialCode=" + diskInitialCode +
                ", diskInitialMessage='" + diskInitialMessage + '\'' +
                ", diskInitialProgress=" + diskInitialProgress +
                ", diskManageInfos=" + diskManageInfos +
                ", isMissingMainStorage=" + isMissingMainStorage +
                ", PrimaryStorageHwIds=" + PrimaryStorageHwIds +
                ", primaryStorageMountPaths='" + primaryStorageMountPaths + '\'' +
                ", secondaryStorageHwIds=" + secondaryStorageHwIds +
                ", secondaryStorageMountPaths=" + secondaryStorageMountPaths +
                ", raidDiskHwIds=" + raidDiskHwIds +
                ", raidType=" + raidType +
                ", updatedTime='" + updatedTime + '\'' +
                '}';
    }
}
