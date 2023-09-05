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

import java.util.Map;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/6/29 10:04
 */
public class EulixPushReserveBean implements EulixKeep {
    // Map<1, Map<2, 3>>: 1: optType; 2: uuid; 3: EulixPushReserveRecord
    private Map<String, Map<String, String>> typeRecentRecordMap;

    public Map<String, Map<String, String>> getTypeRecentRecordMap() {
        return typeRecentRecordMap;
    }

    public void setTypeRecentRecordMap(Map<String, Map<String, String>> typeRecentRecordMap) {
        this.typeRecentRecordMap = typeRecentRecordMap;
    }
}
