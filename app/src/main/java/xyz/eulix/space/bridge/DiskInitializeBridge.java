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

package xyz.eulix.space.bridge;

import java.util.List;

import xyz.eulix.space.abs.AbsBridge;
import xyz.eulix.space.network.agent.disk.DiskInitializeProgressResult;
import xyz.eulix.space.network.agent.disk.DiskManageListResult;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/11/4 14:50
 */
public class DiskInitializeBridge extends AbsBridge {
    private static final DiskInitializeBridge INSTANCE = new DiskInitializeBridge();

    public interface DiskInitializeSourceCallback extends SourceCallback {
        void handleDiskInitializeRequest(boolean isDiskEncrypt, boolean isRaid, List<String> primaryStorageHardwareIds, List<String> secondaryStorageHardwareIds, List<String> raidDiskHardwareIds);
        void handleDiskInitializeProgressRequest();
        void handleDiskManagementListRequest();
        void handleEulixSystemShutdownRequest();
        void handleDiskInitializeHardwareFinishCallback();
        void handleDiskInitializeFinishCallback();
    }

    public interface DiskInitializeSinkCallback extends SinkCallback {
        void handleDisconnect();
        void handleDiskInitializeResponse(int code, String source);
        void handleDiskInitializeProgressResponse(int code, String source, DiskInitializeProgressResult result);
        void handleDiskManagementListResponse(int code, String source, DiskManageListResult result);
        void handleEulixSystemShutdownResponse(int code, String source);
    }

    private DiskInitializeBridge() {}

    public static DiskInitializeBridge getInstance() {
        return INSTANCE;
    }

    public void requestDiskInitialize(boolean isDiskEncrypt, boolean isRaid, List<String> primaryStorageHardwareIds, List<String> secondaryStorageHardwareIds, List<String> raidDiskHardwareIds) {
        if (mSourceCallback != null && mSourceCallback instanceof DiskInitializeSourceCallback) {
            ((DiskInitializeSourceCallback) mSourceCallback).handleDiskInitializeRequest(isDiskEncrypt, isRaid, primaryStorageHardwareIds, secondaryStorageHardwareIds, raidDiskHardwareIds);
        }
    }

    public void requestDiskInitializeProgress() {
        if (mSourceCallback != null && mSourceCallback instanceof DiskInitializeSourceCallback) {
            ((DiskInitializeSourceCallback) mSourceCallback).handleDiskInitializeProgressRequest();
        }
    }

    public void requestDiskManagementList() {
        if (mSourceCallback != null && mSourceCallback instanceof DiskInitializeSourceCallback) {
            ((DiskInitializeSourceCallback) mSourceCallback).handleDiskManagementListRequest();
        }
    }

    public void requestEulixSystemShutdown() {
        if (mSourceCallback != null && mSourceCallback instanceof DiskInitializeSourceCallback) {
            ((DiskInitializeSourceCallback) mSourceCallback).handleEulixSystemShutdownRequest();
        }
    }

    public void diskInitializeHardwareFinish() {
        if (mSourceCallback != null && mSourceCallback instanceof DiskInitializeSourceCallback) {
            ((DiskInitializeSourceCallback) mSourceCallback).handleDiskInitializeHardwareFinishCallback();
        }
    }

    public void diskInitializeFinish() {
        if (mSourceCallback != null && mSourceCallback instanceof DiskInitializeSourceCallback) {
            ((DiskInitializeSourceCallback) mSourceCallback).handleDiskInitializeFinishCallback();
        }
    }

    public void handleDisconnect() {
        if (mSinkCallback != null && mSinkCallback instanceof DiskInitializeSinkCallback) {
            ((DiskInitializeSinkCallback) mSinkCallback).handleDisconnect();
        }
    }

    public void responseDiskInitialize(int code, String source) {
        if (mSinkCallback != null && mSinkCallback instanceof DiskInitializeSinkCallback) {
            ((DiskInitializeSinkCallback) mSinkCallback).handleDiskInitializeResponse(code, source);
        }
    }

    public void responseDiskInitializeProgress(int code, String source, DiskInitializeProgressResult result) {
        if (mSinkCallback != null && mSinkCallback instanceof DiskInitializeSinkCallback) {
            ((DiskInitializeSinkCallback) mSinkCallback).handleDiskInitializeProgressResponse(code, source, result);
        }
    }

    public void responseDiskManagementList(int code, String source, DiskManageListResult result) {
        if (mSinkCallback != null && mSinkCallback instanceof DiskInitializeSinkCallback) {
            ((DiskInitializeSinkCallback) mSinkCallback).handleDiskManagementListResponse(code, source, result);
        }
    }

    public void responseEulixSystemShutdown(int code, String source) {
        if (mSinkCallback != null && mSinkCallback instanceof DiskInitializeSinkCallback) {
            ((DiskInitializeSinkCallback) mSinkCallback).handleEulixSystemShutdownResponse(code, source);
        }
    }
}
