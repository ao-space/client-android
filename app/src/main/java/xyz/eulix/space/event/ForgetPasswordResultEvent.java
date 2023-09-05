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
 * date: 2022/7/25 16:07
 */
public class ForgetPasswordResultEvent {
    private String requestId;
    private boolean isSuccess;

    public ForgetPasswordResultEvent(String requestId, boolean isSuccess) {
        this.requestId = requestId;
        this.isSuccess = isSuccess;
    }

    public String getRequestId() {
        return requestId;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    @Override
    public String toString() {
        return "ForgetPasswordResultEvent{" +
                "requestId='" + requestId + '\'' +
                ", isSuccess=" + isSuccess +
                '}';
    }
}
