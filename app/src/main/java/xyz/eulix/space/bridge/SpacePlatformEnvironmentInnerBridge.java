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
 * date: 2022/11/28 14:54
 */
public class SpacePlatformEnvironmentInnerBridge extends AbsBridge {
    private static final SpacePlatformEnvironmentInnerBridge INSTANCE = new SpacePlatformEnvironmentInnerBridge();

    public interface SpacePlatformEnvironmentInnerSourceCallback extends SourceCallback {
        void handleRequestSwitchPlatform(String domain);
    }

    public interface SpacePlatformEnvironmentInnerSinkCallback extends SinkCallback {
        void handleSwitchPlatformResponse(int code, String source, SwitchPlatformResult result);
        void handleDisconnect();
    }

    private SpacePlatformEnvironmentInnerBridge() {}

    public static SpacePlatformEnvironmentInnerBridge getInstance() {
        return INSTANCE;
    }

    public void handleRequestSwitchPlatform(String url) {
        Logger.d("SpacePlatformEnvironmentInnerBridge", "request switch platform, url: " + url);
        if (mSourceCallback != null && mSourceCallback instanceof SpacePlatformEnvironmentInnerSourceCallback) {
            ((SpacePlatformEnvironmentInnerSourceCallback) mSourceCallback).handleRequestSwitchPlatform(url);
        }
    }

    public void handleSwitchPlatformResponse(int code, String source, SwitchPlatformResult result) {
        Logger.d("SpacePlatformEnvironmentInnerBridge", "switch platform response, code: " + code + ", source: " + source + ", result: " + result);
        if (mSinkCallback != null && mSinkCallback instanceof SpacePlatformEnvironmentInnerSinkCallback) {
            ((SpacePlatformEnvironmentInnerSinkCallback) mSinkCallback).handleSwitchPlatformResponse(code, source, result);
        }
    }

    public void handleDisconnect() {
        if (mSinkCallback != null && mSinkCallback instanceof SpacePlatformEnvironmentInnerSinkCallback) {
            ((SpacePlatformEnvironmentInnerSinkCallback) mSinkCallback).handleDisconnect();
        }
    }
}
