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
 * date: 2022/7/15 11:22
 */
public class EulixAuthenticationBridge extends AbsBridge {
    private static final EulixAuthenticationBridge INSTANCE = new EulixAuthenticationBridge();

    public interface EulixAuthenticationSourceCallback extends SourceCallback {
        void followUp(boolean isFollowUp);
        void handleAuthenticationFinish();
    }

    public interface EulixAuthenticationSinkCallback extends SinkCallback {
        void handleDisconnect();
    }

    private EulixAuthenticationBridge() {}

    public static EulixAuthenticationBridge getInstance() {
        return INSTANCE;
    }

    public void followUp(boolean isFollowUp) {
        if (mSourceCallback != null && mSourceCallback instanceof EulixAuthenticationSourceCallback) {
            ((EulixAuthenticationSourceCallback) mSourceCallback).followUp(isFollowUp);
        }
    }

    public void handleAuthenticationFinish() {
        if (mSourceCallback != null && mSourceCallback instanceof EulixAuthenticationSourceCallback) {
            ((EulixAuthenticationSourceCallback) mSourceCallback).handleAuthenticationFinish();
        }
    }

    public void handleDisconnect() {
        if (mSinkCallback != null && mSinkCallback instanceof EulixAuthenticationSinkCallback) {
            ((EulixAuthenticationSinkCallback) mSinkCallback).handleDisconnect();
        }
    }
}
