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
import xyz.eulix.space.network.agent.SecurityMessagePollResult;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/11/1 16:13
 */
public class BindFailBridge extends AbsBridge {
    private static final BindFailBridge INSTANCE = new BindFailBridge();

    public interface BindFailSourceCallback extends SourceCallback {
        void handleFinish();
        void handleUnbindDevice(String password);
        void handleUnbindResult(boolean isSuccess, int code, String password);
        void handleVerification();
        void handleNewDeviceApplyResetPassword(String applyId);
        void handleSecurityMessagePoll(String applyId);
    }

    public interface BindFailSinkCallback extends SinkCallback {
        void handleUnbind(int result, String boxUuid, int errorTimes, int leftTryTimes, int tryAfterSeconds);
        void handleDisconnect();
        void handleNewDeviceApplyResetPasswordResult(String source, int code);
        void handleSecurityMessagePollResult(String source, int code, String applyId, SecurityMessagePollResult result);
    }

    private BindFailBridge() {}

    public static BindFailBridge getInstance() {
        return INSTANCE;
    }

    public void prepareFinish() {
        if (mSourceCallback != null && mSourceCallback instanceof BindFailSourceCallback) {
            ((BindFailSourceCallback) mSourceCallback).handleFinish();
        }
    }

    public void unbindDevice(String password) {
        if (mSourceCallback != null && mSourceCallback instanceof BindFailSourceCallback) {
            ((BindFailSourceCallback) mSourceCallback).handleUnbindDevice(password);
        }
    }

    public void unbindResult(boolean isSuccess, int code, String password) {
        if (mSourceCallback != null && mSourceCallback instanceof BindFailSourceCallback) {
            ((BindFailSourceCallback) mSourceCallback).handleUnbindResult(isSuccess, code, password);
        }
    }

    public void handleVerification() {
        if (mSourceCallback != null && mSourceCallback instanceof BindFailSourceCallback) {
            ((BindFailSourceCallback) mSourceCallback).handleVerification();
        }
    }

    public void newDeviceApplyResetPasswordRequest(String applyId) {
        if (mSourceCallback != null && mSourceCallback instanceof BindFailSourceCallback) {
            ((BindFailSourceCallback) mSourceCallback).handleNewDeviceApplyResetPassword(applyId);
        }
    }

    public void securityMessagePollRequest(String applyId) {
        if (mSourceCallback != null && mSourceCallback instanceof BindFailSourceCallback) {
            ((BindFailSourceCallback) mSourceCallback).handleSecurityMessagePoll(applyId);
        }
    }


    public void unbindResult(int result, String boxUuid, int errorTimes, int leftTryTimes, int tryAfterSeconds) {
        if (mSinkCallback != null && mSinkCallback instanceof BindFailSinkCallback) {
            ((BindFailSinkCallback) mSinkCallback).handleUnbind(result, boxUuid, errorTimes, leftTryTimes, tryAfterSeconds);
        }
    }

    public void handleDisconnect() {
        if (mSinkCallback != null && mSinkCallback instanceof BindFailSinkCallback) {
            ((BindFailSinkCallback) mSinkCallback).handleDisconnect();
        }
    }

    public void newDeviceApplyResetPasswordResponse(String source, int code) {
        if (mSinkCallback != null && mSinkCallback instanceof BindFailSinkCallback) {
            ((BindFailSinkCallback) mSinkCallback).handleNewDeviceApplyResetPasswordResult(source, code);
        }
    }

    public void securityMessagePollResponse(String source, int code, String applyId, SecurityMessagePollResult result) {
        if (mSinkCallback != null && mSinkCallback instanceof BindFailSinkCallback) {
            ((BindFailSinkCallback) mSinkCallback).handleSecurityMessagePollResult(source, code, applyId, result);
        }
    }

}
