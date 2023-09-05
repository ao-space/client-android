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

package xyz.eulix.space.event;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/9/6 17:53
 */
public class SpaceStatusTestResponseEvent {
    private int spaceStatusSource;
    private Boolean isSuccess;
    private long startTimestamp;
    private long endTimestamp;
    private long nanoTime;

    public SpaceStatusTestResponseEvent(int spaceStatusSource, Boolean isSuccess, long startTimestamp, long endTimestamp, long nanoTime) {
        this.spaceStatusSource = spaceStatusSource;
        this.isSuccess = isSuccess;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.nanoTime = nanoTime;
    }

    public int getSpaceStatusSource() {
        return spaceStatusSource;
    }

    public Boolean getSuccess() {
        return isSuccess;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public long getNanoTime() {
        return nanoTime;
    }
}
