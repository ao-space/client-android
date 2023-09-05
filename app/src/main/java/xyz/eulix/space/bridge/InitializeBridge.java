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
import xyz.eulix.space.network.agent.disk.ReadyCheckResult;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/12/6 17:19
 */
public class InitializeBridge extends AbsBridge {
    private static final InitializeBridge INSTANCE = new InitializeBridge();

    public interface InitializeSourceCallback extends SourceCallback {
        void handleInitialize(String password);
        void handleSpaceReadyCheckRequest();
    }

    public interface InitializeSinkCallback extends SinkCallback {
        void initializeResult(int code);
        void handleDisconnect();
        void handleSpaceReadyCheckResponse(int code, String source, ReadyCheckResult result);
    }

    private InitializeBridge() {}

    public static InitializeBridge getInstance() {
        return INSTANCE;
    }

    public void requestInitialize(String password) {
        if (mSourceCallback != null && mSourceCallback instanceof InitializeSourceCallback) {
            ((InitializeSourceCallback) mSourceCallback).handleInitialize(password);
        }
    }

    public void requestSpaceReadyCheck() {
        if (mSourceCallback != null && mSourceCallback instanceof InitializeSourceCallback) {
            ((InitializeSourceCallback) mSourceCallback).handleSpaceReadyCheckRequest();
        }
    }

    public void initializeResponse(int code) {
        if (mSinkCallback != null && mSinkCallback instanceof InitializeSinkCallback) {
            ((InitializeSinkCallback) mSinkCallback).initializeResult(code);
        }
    }

    public void handleDisconnect() {
        if (mSinkCallback != null && mSinkCallback instanceof InitializeSinkCallback) {
            ((InitializeSinkCallback) mSinkCallback).handleDisconnect();
        }
    }

    public void responseSpaceReadyCheck(int code, String source, ReadyCheckResult result) {
        if (mSinkCallback != null && mSinkCallback instanceof InitializeSinkCallback) {
            ((InitializeSinkCallback) mSinkCallback).handleSpaceReadyCheckResponse(code, source, result);
        }
    }
}
