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
 * date: 2021/11/24 11:38
 */
public class EulixSpaceInfo {
    protected String boxUuid;
    protected String boxBind;

    public String getBoxUuid() {
        return boxUuid;
    }

    public void setBoxUuid(String boxUuid) {
        this.boxUuid = boxUuid;
    }

    public String getBoxBind() {
        return boxBind;
    }

    public void setBoxBind(String boxBind) {
        this.boxBind = boxBind;
    }

    @Override
    public String toString() {
        return "EulixSpaceInfo{" +
                "boxUuid='" + boxUuid + '\'' +
                ", boxBind='" + boxBind + '\'' +
                '}';
    }
}
