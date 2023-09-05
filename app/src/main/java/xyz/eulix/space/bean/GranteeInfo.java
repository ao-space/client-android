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

package xyz.eulix.space.bean;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/4/28 15:07
 */
public class GranteeInfo implements EulixKeep {
    private String terminalType;
    private String terminalMode;
    private String clientUUID;

    public String getTerminalType() {
        return terminalType;
    }

    public void setTerminalType(String terminalType) {
        this.terminalType = terminalType;
    }

    public String getTerminalMode() {
        return terminalMode;
    }

    public void setTerminalMode(String terminalMode) {
        this.terminalMode = terminalMode;
    }

    public String getClientUUID() {
        return clientUUID;
    }

    public void setClientUUID(String clientUUID) {
        this.clientUUID = clientUUID;
    }

    @Override
    public String toString() {
        return "GranteeInfo{" +
                "terminalType='" + terminalType + '\'' +
                ", terminalMode='" + terminalMode + '\'' +
                ", clientUUID='" + clientUUID + '\'' +
                '}';
    }
}
