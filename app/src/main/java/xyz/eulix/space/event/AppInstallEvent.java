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
 * date: 2021/10/12 15:49
 */
public class AppInstallEvent {
    private boolean isSuccess;
    private String filePath;
    private boolean isForce;

    public AppInstallEvent(boolean isSuccess, String filePath, boolean isForce) {
        this.isSuccess = isSuccess;
        this.filePath = filePath;
        this.isForce = isForce;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public String getFilePath() {
        return filePath;
    }

    public boolean isForce() {
        return isForce;
    }
}
