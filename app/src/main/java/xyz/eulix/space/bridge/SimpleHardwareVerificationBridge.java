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
 * date: 2022/7/15 11:43
 */
public class SimpleHardwareVerificationBridge extends AbsBridge {
    private static final SimpleHardwareVerificationBridge INSTANCE = new SimpleHardwareVerificationBridge();

    public interface SimpleHardwareVerificationSourceCallback extends SourceCallback {
        void startOrEndResetPassword(boolean isStart);
    }

    public interface SimpleHardwareVerificationSinkCallback extends SinkCallback {
        void handleDisconnect();
    }

    private SimpleHardwareVerificationBridge() {}

    public static SimpleHardwareVerificationBridge getInstance() {
        return INSTANCE;
    }

    public void startOrEndResetPassword(boolean isStart) {
        if (mSourceCallback != null && mSourceCallback instanceof SimpleHardwareVerificationSourceCallback) {
            ((SimpleHardwareVerificationSourceCallback) mSourceCallback).startOrEndResetPassword(isStart);
        }
    }

    public void handleDisconnect() {
        if (mSinkCallback != null && mSinkCallback instanceof SimpleHardwareVerificationSinkCallback) {
            ((SimpleHardwareVerificationSinkCallback) mSinkCallback).handleDisconnect();
        }
    }
}
