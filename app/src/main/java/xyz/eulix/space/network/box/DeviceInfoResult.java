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

package xyz.eulix.space.network.box;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/9/18 9:47
 */
public class DeviceInfoResult implements EulixKeep {
    private String requestId;
    private String spaceSizeTotal;
    private String spaceSizeUsed;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getSpaceSizeTotal() {
        return spaceSizeTotal;
    }

    public void setSpaceSizeTotal(String spaceSizeTotal) {
        this.spaceSizeTotal = spaceSizeTotal;
    }

    public String getSpaceSizeUsed() {
        return spaceSizeUsed;
    }

    public void setSpaceSizeUsed(String spaceSizeUsed) {
        this.spaceSizeUsed = spaceSizeUsed;
    }

    @Override
    public String toString() {
        return "DeviceInfoResult{" +
                "requestId='" + requestId + '\'' +
                ", spaceSizeTotal='" + spaceSizeTotal + '\'' +
                ", spaceSizeUsed='" + spaceSizeUsed + '\'' +
                '}';
    }
}
