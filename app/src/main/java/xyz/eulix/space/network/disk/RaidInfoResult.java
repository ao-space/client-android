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
 * date: 2022/11/9 16:40
 */
public class RaidInfoResult implements EulixKeep {
    private Double copyPercent;
    private Integer raidType;
    private Integer copyResult;

    public Double getCopyPercent() {
        return copyPercent;
    }

    public void setCopyPercent(Double copyPercent) {
        this.copyPercent = copyPercent;
    }

    public Integer getRaidType() {
        return raidType;
    }

    public void setRaidType(Integer raidType) {
        this.raidType = raidType;
    }

    public Integer getCopyResult() {
        return copyResult;
    }

    public void setCopyResult(Integer copyResult) {
        this.copyResult = copyResult;
    }

    @Override
    public String toString() {
        return "RaidInfoResult{" +
                "copyPercent=" + copyPercent +
                ", raidType=" + raidType +
                ", copyResult=" + copyResult +
                '}';
    }
}
