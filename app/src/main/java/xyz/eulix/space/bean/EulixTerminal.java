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

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/4/22 15:16
 */
public class EulixTerminal {
    private String terminalUuid;
    private String terminalRegisterKey;
    private String terminalType;
    private String terminalName;
    private long terminalTimestamp;
    private String terminalPlace;
    private boolean isMyself;
    private boolean isGranter;

    public String getTerminalUuid() {
        return terminalUuid;
    }

    public void setTerminalUuid(String terminalUuid) {
        this.terminalUuid = terminalUuid;
    }

    public String getTerminalRegisterKey() {
        return terminalRegisterKey;
    }

    public void setTerminalRegisterKey(String terminalRegisterKey) {
        this.terminalRegisterKey = terminalRegisterKey;
    }

    public String getTerminalType() {
        return terminalType;
    }

    public void setTerminalType(String terminalType) {
        this.terminalType = terminalType;
    }

    public String getTerminalName() {
        return terminalName;
    }

    public void setTerminalName(String terminalName) {
        this.terminalName = terminalName;
    }

    public long getTerminalTimestamp() {
        return terminalTimestamp;
    }

    public void setTerminalTimestamp(long terminalTimestamp) {
        this.terminalTimestamp = terminalTimestamp;
    }

    public String getTerminalPlace() {
        return terminalPlace;
    }

    public void setTerminalPlace(String terminalPlace) {
        this.terminalPlace = terminalPlace;
    }

    public boolean isMyself() {
        return isMyself;
    }

    public void setMyself(boolean myself) {
        isMyself = myself;
    }

    public boolean isGranter() {
        return isGranter;
    }

    public void setGranter(boolean granter) {
        isGranter = granter;
    }

    @Override
    public String toString() {
        return "EulixTerminal{" +
                "terminalUuid='" + terminalUuid + '\'' +
                ", terminalRegisterKey='" + terminalRegisterKey + '\'' +
                ", terminalType='" + terminalType + '\'' +
                ", terminalName='" + terminalName + '\'' +
                ", terminalTimestamp=" + terminalTimestamp +
                ", terminalPlace='" + terminalPlace + '\'' +
                ", isMyself=" + isMyself +
                ", isGranter=" + isGranter +
                '}';
    }
}
