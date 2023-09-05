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
import xyz.eulix.space.network.agent.disk.DiskRecognitionResult;
import xyz.eulix.space.network.agent.platform.SwitchPlatformResult;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/11/23 13:51
 */
public class BindResultBridge extends AbsBridge {
    private static final BindResultBridge INSTANCE = new BindResultBridge();

    public interface BindResultSourceCallback extends SourceCallback {
        void handleBindResultHardwareFinishCallback();
        void handleBindResultFinishCallback();
        void handleDiskRecognitionRequest();
        void remindDiskInitializeBridge();
        void handleForceRequestSwitchPlatform(String taskId, String platformUrl);
    }

    public interface BindResultSinkCallback extends SinkCallback {
        void handleDisconnect();
        void handleDiskRecognitionResponse(int code, String source, DiskRecognitionResult result);
        void handleSwitchPlatformResponse(int code, String source, SwitchPlatformResult result);
    }

    private BindResultBridge() {}

    public static BindResultBridge getInstance() {
        return INSTANCE;
    }

    public void bindResultHardwareFinish() {
        if (mSourceCallback != null && mSourceCallback instanceof BindResultSourceCallback) {
            ((BindResultSourceCallback) mSourceCallback).handleBindResultHardwareFinishCallback();
        }
    }

    public void bindResultFinish() {
        if (mSourceCallback != null && mSourceCallback instanceof BindResultSourceCallback) {
            ((BindResultSourceCallback) mSourceCallback).handleBindResultFinishCallback();
        }
    }

    public void requestDiskRecognition() {
        if (mSourceCallback != null && mSourceCallback instanceof BindResultSourceCallback) {
            ((BindResultSourceCallback) mSourceCallback).handleDiskRecognitionRequest();
        }
    }

    public void prepareDiskInitialize() {
        if (mSourceCallback != null && mSourceCallback instanceof BindResultSourceCallback) {
            ((BindResultSourceCallback) mSourceCallback).remindDiskInitializeBridge();
        }
    }

    public void handleRequestSwitchPlatform(String taskId, String platformHost) {
        if (mSourceCallback != null && mSourceCallback instanceof BindResultSourceCallback) {
            ((BindResultSourceCallback) mSourceCallback).handleForceRequestSwitchPlatform(taskId, platformHost);
        }
    }

    public void handleDisconnect() {
        if (mSinkCallback != null && mSinkCallback instanceof BindResultSinkCallback) {
            ((BindResultSinkCallback) mSinkCallback).handleDisconnect();
        }
    }

    public void responseDiskRecognition(int code, String source, DiskRecognitionResult result) {
        if (mSinkCallback != null && mSinkCallback instanceof BindResultSinkCallback) {
            ((BindResultSinkCallback) mSinkCallback).handleDiskRecognitionResponse(code, source, result);
        }
    }

    public void handleSwitchPlatformResponse(int code, String source, SwitchPlatformResult result) {
        if (mSinkCallback != null && mSinkCallback instanceof BindResultSinkCallback) {
            ((BindResultSinkCallback) mSinkCallback).handleSwitchPlatformResponse(code, source, result);
        }
    }
}
