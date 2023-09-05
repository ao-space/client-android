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

import xyz.eulix.space.abs.AbsBridge;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/11/9 11:29
 */
public class DiskManagementBridge extends AbsBridge {
    private static final DiskManagementBridge INSTANCE = new DiskManagementBridge();

    public interface DiskManagementSourceCallback extends SourceCallback {
        void refreshDiskManagement();
        void sendEulixSystemShutdown();
        void diskManagementCallback();
    }

    public interface DiskManagementSinkCallback extends SinkCallback {
        void completeDiskManagement();
        void completeEulixSystemShutdown();
        void handleDisconnect();
    }

    private DiskManagementBridge() {}

    public static DiskManagementBridge getInstance() {
        return INSTANCE;
    }

    public void refreshDiskManagement() {
        if (mSourceCallback != null && mSourceCallback instanceof DiskManagementSourceCallback) {
            ((DiskManagementSourceCallback) mSourceCallback).refreshDiskManagement();
        }
    }

    public void sendEulixSystemShutdown() {
        if (mSourceCallback != null && mSourceCallback instanceof DiskManagementSourceCallback) {
            ((DiskManagementSourceCallback) mSourceCallback).sendEulixSystemShutdown();
        }
    }

    public void handleDiskManagement() {
        if (mSourceCallback != null && mSourceCallback instanceof DiskManagementSourceCallback) {
            ((DiskManagementSourceCallback) mSourceCallback).diskManagementCallback();
        }
    }

    public void completeDiskManagement() {
        if (mSinkCallback != null && mSinkCallback instanceof DiskManagementSinkCallback) {
            ((DiskManagementSinkCallback) mSinkCallback).completeDiskManagement();
        }
    }

    public void completeEulixSystemShutdown() {
        if (mSinkCallback != null && mSinkCallback instanceof DiskManagementSinkCallback) {
            ((DiskManagementSinkCallback) mSinkCallback).completeEulixSystemShutdown();
        }
    }

    public void handleDisconnect() {
        if (mSinkCallback != null && mSinkCallback instanceof DiskManagementSinkCallback) {
            ((DiskManagementSinkCallback) mSinkCallback).handleDisconnect();
        }
    }
}
