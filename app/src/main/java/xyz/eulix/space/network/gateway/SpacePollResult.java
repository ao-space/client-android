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

package xyz.eulix.space.network.gateway;

import java.util.List;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.network.push.PushMessage;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/4/27 14:15
 */
public class SpacePollResult implements EulixKeep {
    private String status;
    private String version;
    private String message;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return "SpacePollResult{" +
                "status='" + status + '\'' +
                ", version='" + version + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
