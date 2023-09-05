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
import xyz.eulix.space.network.agent.platform.SwitchPlatformResult;
import xyz.eulix.space.network.agent.platform.SwitchStatusResult;
import xyz.eulix.space.util.Logger;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/11/28 10:08
 */
public class SpacePlatformEnvironmentBridge extends AbsBridge {
    private static final SpacePlatformEnvironmentBridge INSTANCE = new SpacePlatformEnvironmentBridge();

    public interface SpacePlatformEnvironmentSourceCallback extends SourceCallback {
        void handleRequestSwitchPlatform(String transId, String domain);
        void handleRequestSwitchStatus(String transId);
    }

    public interface SpacePlatformEnvironmentSinkCallback extends SinkCallback {
        void handleSwitchPlatformResponse(int code, String source, SwitchPlatformResult result, boolean isError);
        void handleSwitchStatusResponse(int code, String source, SwitchStatusResult result);
        void handleDisconnect();
    }

    private SpacePlatformEnvironmentBridge() {}

    public static SpacePlatformEnvironmentBridge getInstance() {
        return INSTANCE;
    }

    public void handleRequestSwitchPlatform(String transId, String domain) {
        Logger.d("SpacePlatformEnvironmentBridge", "request switch platform, transId: " + transId + ", domain: " + domain);
        if (mSourceCallback != null && mSourceCallback instanceof SpacePlatformEnvironmentSourceCallback) {
            ((SpacePlatformEnvironmentSourceCallback) mSourceCallback).handleRequestSwitchPlatform(transId, domain);
        }
    }

    public void handleRequestSwitchStatus(String transId) {
        Logger.d("SpacePlatformEnvironmentBridge", "request switch status, transId: " + transId);
        if (mSourceCallback != null && mSourceCallback instanceof SpacePlatformEnvironmentSourceCallback) {
            ((SpacePlatformEnvironmentSourceCallback) mSourceCallback).handleRequestSwitchStatus(transId);
        }
    }

    public void handleSwitchPlatformResponse(int code, String source, SwitchPlatformResult result, boolean isError) {
        Logger.d("SpacePlatformEnvironmentBridge", "switch platform response, code: " + code + ", source: " + source + ", result: " + result + ", is error: " + isError);
        if (mSinkCallback != null && mSinkCallback instanceof SpacePlatformEnvironmentSinkCallback) {
            ((SpacePlatformEnvironmentSinkCallback) mSinkCallback).handleSwitchPlatformResponse(code, source, result, isError);
        }
    }

    public void handleSwitchStatusResponse(int code, String source, SwitchStatusResult result) {
        Logger.d("SpacePlatformEnvironmentBridge", "switch status response, code: " + code + ", source: " + source + ", result: " + result);
        if (mSinkCallback != null && mSinkCallback instanceof SpacePlatformEnvironmentSinkCallback) {
            ((SpacePlatformEnvironmentSinkCallback) mSinkCallback).handleSwitchStatusResponse(code, source, result);
        }
    }

    public void handleDisconnect() {
        if (mSinkCallback != null && mSinkCallback instanceof SpacePlatformEnvironmentSinkCallback) {
            ((SpacePlatformEnvironmentSinkCallback) mSinkCallback).handleDisconnect();
        }
    }
}
