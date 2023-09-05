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

package xyz.eulix.space.network.disk;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/11/21 18:07
 */
public class DiskExpandProgressResult implements EulixKeep {
    public static final int CODE_EXPAND_COMPLETE = 1;
    public static final int CODE_NOT_EXPAND = 2;
    public static final int CODE_EXPANDING = 3;
    public static final int CODE_EXPAND_ERROR = 100;
    private Integer expandCode;
    private String expandMessage;
    private Integer expandProgress;

    public Integer getExpandCode() {
        return expandCode;
    }

    public void setExpandCode(Integer expandCode) {
        this.expandCode = expandCode;
    }

    public String getExpandMessage() {
        return expandMessage;
    }

    public void setExpandMessage(String expandMessage) {
        this.expandMessage = expandMessage;
    }

    public Integer getExpandProgress() {
        return expandProgress;
    }

    public void setExpandProgress(Integer expandProgress) {
        this.expandProgress = expandProgress;
    }

    @Override
    public String toString() {
        return "DiskExpandProgressResult{" +
                "expandCode=" + expandCode +
                ", expandMessage='" + expandMessage + '\'' +
                ", expandProgress=" + expandProgress +
                '}';
    }
}
