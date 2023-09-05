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

package xyz.eulix.space.network.net;

import xyz.eulix.space.bean.DeviceAbility;
import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.network.EulixBaseResponse;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/12/7 18:46
 */
public class DeviceAbilityResponse extends EulixBaseResponse implements EulixKeep {
    private DeviceAbility results;

    public DeviceAbility getResults() {
        return results;
    }

    public void setResults(DeviceAbility results) {
        this.results = results;
    }

    @Override
    public String toString() {
        return "DeviceAbilityResponse{" +
                "results=" + results +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}
