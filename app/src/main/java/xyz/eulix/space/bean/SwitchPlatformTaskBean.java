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

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/11/29 14:12
 */
public class SwitchPlatformTaskBean implements EulixKeep {
    private String taskId;
    private long taskTimestamp;
    private boolean isPrivatePlatform;
    private String platformUrl;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public long getTaskTimestamp() {
        return taskTimestamp;
    }

    public void setTaskTimestamp(long taskTimestamp) {
        this.taskTimestamp = taskTimestamp;
    }

    public boolean isPrivatePlatform() {
        return isPrivatePlatform;
    }

    public void setPrivatePlatform(boolean privatePlatform) {
        isPrivatePlatform = privatePlatform;
    }

    public String getPlatformUrl() {
        return platformUrl;
    }

    public void setPlatformUrl(String platformUrl) {
        this.platformUrl = platformUrl;
    }

    @Override
    public String toString() {
        return "SwitchPlatformTaskBean{" +
                "taskId='" + taskId + '\'' +
                ", taskTimestamp=" + taskTimestamp +
                ", isPrivatePlatform=" + isPrivatePlatform +
                ", platformUrl='" + platformUrl + '\'' +
                '}';
    }
}