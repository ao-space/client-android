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
 * date: 2022/10/31 15:39
 */
public class DiskInitializeRequest implements EulixKeep {
    private int diskEncrypt;
    private List<String> primaryStorageHwIds;
    private List<String> secondaryStorageHwIds;
    private int raidType;
    private List<String> raidDiskHwIds;

    public int getDiskEncrypt() {
        return diskEncrypt;
    }

    public void setDiskEncrypt(int diskEncrypt) {
        this.diskEncrypt = diskEncrypt;
    }

    public List<String> getPrimaryStorageHwIds() {
        return primaryStorageHwIds;
    }

    public void setPrimaryStorageHwIds(List<String> primaryStorageHwIds) {
        this.primaryStorageHwIds = primaryStorageHwIds;
    }

    public List<String> getSecondaryStorageHwIds() {
        return secondaryStorageHwIds;
    }

    public void setSecondaryStorageHwIds(List<String> secondaryStorageHwIds) {
        this.secondaryStorageHwIds = secondaryStorageHwIds;
    }

    public int getRaidType() {
        return raidType;
    }

    public void setRaidType(int raidType) {
        this.raidType = raidType;
    }

    public List<String> getRaidDiskHwIds() {
        return raidDiskHwIds;
    }

    public void setRaidDiskHwIds(List<String> raidDiskHwIds) {
        this.raidDiskHwIds = raidDiskHwIds;
    }

    @Override
    public String toString() {
        return "DiskInitializeRequest{" +
                "diskEncrypt=" + diskEncrypt +
                ", primaryStorageHwIds=" + primaryStorageHwIds +
                ", secondaryStorageHwIds=" + secondaryStorageHwIds +
                ", raidType=" + raidType +
                ", raidDiskHwIds=" + raidDiskHwIds +
                '}';
    }
}
