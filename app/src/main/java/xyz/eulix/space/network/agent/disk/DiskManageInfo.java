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

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/10/31 14:35
 */
public class DiskManageInfo extends DiskInfo implements EulixKeep {
    public static final int DISK_EXCEPTION_NORMAL = 0;
    public static final int DISK_EXCEPTION_NON_EXIST = 1;
    public static final int DISK_EXCEPTION_NEW_DISK_NOT_EXPAND = 10;
    public static final int DISK_EXCEPTION_NEW_DISK_EXPANDING = 11;
    public static final int DISK_EXCEPTION_NEW_DISK_EXPAND_ERROR = 12;
    private Integer diskException;
    private Long spaceTotal;
    private Long spaceUsage;
    private Long spaceAvailable;
    private String spaceUsePercent;

    public Integer getDiskException() {
        return diskException;
    }

    public void setDiskException(Integer diskException) {
        this.diskException = diskException;
    }

    public Long getSpaceTotal() {
        return spaceTotal;
    }

    public void setSpaceTotal(Long spaceTotal) {
        this.spaceTotal = spaceTotal;
    }

    public Long getSpaceUsage() {
        return spaceUsage;
    }

    public void setSpaceUsage(Long spaceUsage) {
        this.spaceUsage = spaceUsage;
    }

    public Long getSpaceAvailable() {
        return spaceAvailable;
    }

    public void setSpaceAvailable(Long spaceAvailable) {
        this.spaceAvailable = spaceAvailable;
    }

    public String getSpaceUsePercent() {
        return spaceUsePercent;
    }

    public void setSpaceUsePercent(String spaceUsePercent) {
        this.spaceUsePercent = spaceUsePercent;
    }

    @Override
    public String toString() {
        return "DiskManageInfo{" +
                "diskException=" + diskException +
                ", spaceTotal=" + spaceTotal +
                ", spaceUsage=" + spaceUsage +
                ", busNumber=" + busNumber +
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
