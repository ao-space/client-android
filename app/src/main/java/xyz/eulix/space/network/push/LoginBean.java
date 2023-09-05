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
 * date: 2022/5/7 13:58
 */
public class LoginBean implements EulixKeep {
    private int id;
    private String aoid;
    private String uuid;
    private String terminalMode;
    private String clientRegKey;
    private String address;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

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

    public String getClientRegKey() {
        return clientRegKey;
    }

    public void setClientRegKey(String clientRegKey) {
        this.clientRegKey = clientRegKey;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "LoginBean{" +
                "id=" + id +
                ", aoid='" + aoid + '\'' +
                ", uuid='" + uuid + '\'' +
                ", terminalMode='" + terminalMode + '\'' +
                ", clientRegKey='" + clientRegKey + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}
