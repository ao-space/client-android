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

import xyz.eulix.space.bean.developer.SpacePlatformInfo;

/**
 * @author: chenjiawei
 * Description: 增加字段需要更新EulixSpaceDBBoxManager
 * date: 2021/11/25 15:44
 */
public class EulixBoxOtherInfo {
    private Map<String, TerminalInfo> terminalInfoMap;
    private ApplicationLockInfo applicationLockInfo;
    private String eulixPushDeviceToken;
    private Map<String, String> eulixPushDeviceTokenMap;
    private boolean isDeveloperMode;
    private SpacePlatformInfo spacePlatformInfo;

    public Map<String, TerminalInfo> getTerminalInfoMap() {
        return terminalInfoMap;
    }

    public void setTerminalInfoMap(Map<String, TerminalInfo> terminalInfoMap) {
        this.terminalInfoMap = terminalInfoMap;
    }

    public ApplicationLockInfo getApplicationLockInfo() {
        return applicationLockInfo;
    }

    public void setApplicationLockInfo(ApplicationLockInfo applicationLockInfo) {
        this.applicationLockInfo = applicationLockInfo;
    }

    public String getEulixPushDeviceToken() {
        return eulixPushDeviceToken;
    }

    public void setEulixPushDeviceToken(String eulixPushDeviceToken) {
        this.eulixPushDeviceToken = eulixPushDeviceToken;
    }

    public Map<String, String> getEulixPushDeviceTokenMap() {
        return eulixPushDeviceTokenMap;
    }

    public void setEulixPushDeviceTokenMap(Map<String, String> eulixPushDeviceTokenMap) {
        this.eulixPushDeviceTokenMap = eulixPushDeviceTokenMap;
    }

    public boolean isDeveloperMode() {
        return isDeveloperMode;
    }

    public void setDeveloperMode(boolean developerMode) {
        isDeveloperMode = developerMode;
    }

    public SpacePlatformInfo getSpacePlatformInfo() {
        return spacePlatformInfo;
    }

    public void setSpacePlatformInfo(SpacePlatformInfo spacePlatformInfo) {
        this.spacePlatformInfo = spacePlatformInfo;
    }

    @Override
    public String toString() {
        return "EulixBoxOtherInfo{" +
                "terminalInfoMap=" + terminalInfoMap +
                ", applicationLockInfo=" + applicationLockInfo +
                '}';
    }
}
