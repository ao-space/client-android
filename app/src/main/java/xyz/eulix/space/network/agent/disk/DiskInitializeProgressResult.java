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

package xyz.eulix.space.network.agent.disk;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/10/31 15:35
 */
public class DiskInitializeProgressResult implements EulixKeep {
    private Integer initialCode;
    private String initialMessage;
    private Integer initialProgress;

    public Integer getInitialCode() {
        return initialCode;
    }

    public void setInitialCode(Integer initialCode) {
        this.initialCode = initialCode;
    }

    public String getInitialMessage() {
        return initialMessage;
    }

    public void setInitialMessage(String initialMessage) {
        this.initialMessage = initialMessage;
    }

    public Integer getInitialProgress() {
        return initialProgress;
    }

    public void setInitialProgress(Integer initialProgress) {
        this.initialProgress = initialProgress;
    }

    @Override
    public String toString() {
        return "DiskInitializeProgressResult{" +
                "initialCode=" + initialCode +
                ", initialMessage='" + initialMessage + '\'' +
                ", initialProgress=" + initialProgress +
                '}';
    }
}
