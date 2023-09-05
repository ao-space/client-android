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
 * date: 2021/12/6 15:45
 */
public class SecurityPasswordBridge extends AbsBridge {
    private static final SecurityPasswordBridge INSTANCE = new SecurityPasswordBridge();

    public interface SecurityPasswordSourceCallback extends SourceCallback {
        void handleSecurityPassword(String oldPassword, String password);
        void handleResetSecurityPassword(boolean isGranter, String password, String granterSecurityToken);
        void handleNewDeviceResetPassword(String acceptSecurityToken, String emailSecurityToken, String granterClientUuid, String password);
    }

    public interface SecurityPasswordSinkCallback extends SinkCallback {
        void securityPasswordResult(int code, String source, Boolean isGranter);
        void newDeviceResetSecurityPasswordResult(String source, int code);
        void handleDisconnect();
    }

    private SecurityPasswordBridge() {}

    public static SecurityPasswordBridge getInstance() {
        return INSTANCE;
    }

    public void setPassword(String oldPassword, String password) {
        if (mSourceCallback != null && mSourceCallback instanceof SecurityPasswordSourceCallback) {
            ((SecurityPasswordSourceCallback) mSourceCallback).handleSecurityPassword(oldPassword, password);
        }
    }

    public void resetPassword(boolean isGranter, String password, String granterSecurityToken) {
        if (mSourceCallback != null && mSourceCallback instanceof SecurityPasswordSourceCallback) {
            ((SecurityPasswordSourceCallback) mSourceCallback).handleResetSecurityPassword(isGranter, password, granterSecurityToken);
        }
    }

    public void newDeviceResetPasswordRequest(String acceptSecurityToken, String emailSecurityToken, String granterClientUuid, String password) {
        if (mSourceCallback != null && mSourceCallback instanceof SecurityPasswordSourceCallback) {
            ((SecurityPasswordSourceCallback) mSourceCallback).handleNewDeviceResetPassword(acceptSecurityToken, emailSecurityToken, granterClientUuid, password);
        }
    }

    public void setPasswordResult(int code, String source, Boolean isGranter) {
        if (mSinkCallback != null && mSinkCallback instanceof SecurityPasswordSinkCallback) {
            ((SecurityPasswordSinkCallback) mSinkCallback).securityPasswordResult(code, source, isGranter);
        }
    }

    public void newDeviceResetPasswordResponse(String source, int code) {
        if (mSinkCallback != null && mSinkCallback instanceof SecurityPasswordSinkCallback) {
            ((SecurityPasswordSinkCallback) mSinkCallback).newDeviceResetSecurityPasswordResult(source, code);
        }
    }

    public void handleDisconnect() {
        if (mSinkCallback != null && mSinkCallback instanceof SecurityPasswordSinkCallback) {
            ((SecurityPasswordSinkCallback) mSinkCallback).handleDisconnect();
        }
    }
}
