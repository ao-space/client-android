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
 * date: 2022/7/6 16:41
 */
public class EulixBoxBaseInfoCompatible extends EulixBoxBaseInfo {
    // 1: 在线使用中；-1：离线使用中；0：禁用
    private int spaceState;

    public int getSpaceState() {
        return spaceState;
    }

    public void setSpaceState(int spaceState) {
        this.spaceState = spaceState;
    }

    @Override
    public String toString() {
        return "EulixBoxBaseInfoCompatible{" +
                "spaceState=" + spaceState +
                ", boxUuid='" + boxUuid + '\'' +
                ", boxBind='" + boxBind + '\'' +
                ", boxDomain='" + boxDomain + '\'' +
                '}';
    }
}
