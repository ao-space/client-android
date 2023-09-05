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

package xyz.eulix.space.network.push;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/5/18 16:50
 */
public class LoginConfirmBean implements EulixKeep {
    private String aoid;
    private String uuid;
    private String terminalMode;
    private String address;
    private String terminalType;

    public String getAoid() {
        return aoid;
    }

    public void setAoid(String aoid) {
        this.aoid = aoid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getTerminalMode() {
        return terminalMode;
    }

    public void setTerminalMode(String terminalMode) {
        this.terminalMode = terminalMode;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getTerminalType() {
        return terminalType;
    }

    public void setTerminalType(String terminalType) {
        this.terminalType = terminalType;
    }

    @Override
    public String toString() {
        return "LoginConfirmBean{" +
                "aoid='" + aoid + '\'' +
                ", uuid='" + uuid + '\'' +
                ", terminalMode='" + terminalMode + '\'' +
                ", address='" + address + '\'' +
                ", terminalType='" + terminalType + '\'' +
                '}';
    }
}
