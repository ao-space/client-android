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

import java.util.List;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/7/26 18:06
 */
public class SecurityPasswordInfo implements EulixKeep {
    private int verificationCount = 3;
    private long verificationPermitTimestamp = -1L;
    private List<Long> verificationDenyTimestamp;
    private List<Long> revokeDenyTimestamp;

    public int getVerificationCount() {
        return verificationCount;
    }

    public void setVerificationCount(int verificationCount) {
        this.verificationCount = verificationCount;
    }

    public long getVerificationPermitTimestamp() {
        return verificationPermitTimestamp;
    }

    public void setVerificationPermitTimestamp(long verificationPermitTimestamp) {
        this.verificationPermitTimestamp = verificationPermitTimestamp;
    }

    public List<Long> getVerificationDenyTimestamp() {
        return verificationDenyTimestamp;
    }

    public void setVerificationDenyTimestamp(List<Long> verificationDenyTimestamp) {
        this.verificationDenyTimestamp = verificationDenyTimestamp;
    }

    public List<Long> getRevokeDenyTimestamp() {
        return revokeDenyTimestamp;
    }

    public void setRevokeDenyTimestamp(List<Long> revokeDenyTimestamp) {
        this.revokeDenyTimestamp = revokeDenyTimestamp;
    }

    @Override
    public String toString() {
        return "SecurityPasswordInfo{" +
                "verificationCount=" + verificationCount +
                ", verificationPermitTimestamp=" + verificationPermitTimestamp +
                ", verificationDenyTimestamp=" + verificationDenyTimestamp +
                ", revokeDenyTimestamp=" + revokeDenyTimestamp +
                '}';
    }
}
