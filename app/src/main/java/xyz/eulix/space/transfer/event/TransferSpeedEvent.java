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

package xyz.eulix.space.transfer.event;

import java.util.Map;

/**
 * Author:      Zhu Fuyu
 * Description: 传输速度事件
 * History:     2021/12/9
 */
public class TransferSpeedEvent {

    public static class TransferSpeed {

        public String mKey;

        public int mSpeed;

        public TransferSpeed(String key, int speed) {
            mKey = key;
            mSpeed = speed;
        }
    }

    public static class TransferSpeedMap {

        public Map<String, Integer> mMap;

        public TransferSpeedMap(Map<String, Integer> map) {
            mMap = map;
        }
    }
}
