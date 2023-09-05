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
 * date: 2022/10/31 15:07
 */
public class ReadyCheckResult implements EulixKeep {
    public static final int DISK_NORMAL = 1;
    public static final int DISK_UNINITIALIZED = DISK_NORMAL + 1;
    public static final int DISK_INITIALIZING = DISK_UNINITIALIZED + 1;
    public static final int DISK_DATA_SYNCHRONIZATION = DISK_INITIALIZING + 1;
    public static final int DISK_INITIALIZE_ERROR = 100;
    public static final int DISK_FORMAT_ERROR = DISK_INITIALIZE_ERROR + 1;
    private Integer diskInitialCode;
    private Boolean missingMainStorage;
    private Integer paired;

    public Integer getDiskInitialCode() {
        return diskInitialCode;
    }

    public void setDiskInitialCode(Integer diskInitialCode) {
        this.diskInitialCode = diskInitialCode;
    }

    public Boolean getMissingMainStorage() {
        return missingMainStorage;
    }

    public void setMissingMainStorage(Boolean missingMainStorage) {
        this.missingMainStorage = missingMainStorage;
    }

    public Integer getPaired() {
        return paired;
    }

    public void setPaired(Integer paired) {
        this.paired = paired;
    }

    @Override
    public String toString() {
        return "ReadyCheckResult{" +
                "diskInitialCode=" + diskInitialCode +
                ", missingMainStorage=" + missingMainStorage +
                ", paired=" + paired +
                '}';
    }
}
