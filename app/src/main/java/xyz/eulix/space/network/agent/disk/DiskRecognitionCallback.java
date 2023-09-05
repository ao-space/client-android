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

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/11/2 9:19
 */
public interface DiskRecognitionCallback {
    void onSuccess(int code, String source, String message, String requestId, DiskRecognitionResult result);
    void onFail(int code, String source, String message, String requestId);
    void onError(String errMsg);
}