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

package xyz.eulix.space.network.agent.platform;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/11/25 14:20
 */
public class SwitchStatusResult extends SwitchPlatformResult implements EulixKeep {
    public static final int STATUS_INIT = 0;
    public static final int STATUS_START = 1;
    public static final int STATUS_UPDATE_GATEWAY = 2;
    public static final int STATUS_UPDATE_BOX_INFO = 3;
    public static final int STATUS_ABORT = 99;
    public static final int STATUS_OK = 100;
    private Integer status;

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "SwitchStatusResult{" +
                "status=" + status +
                ", userDomain='" + userDomain + '\'' +
                ", transId='" + transId + '\'' +
                '}';
    }
}
