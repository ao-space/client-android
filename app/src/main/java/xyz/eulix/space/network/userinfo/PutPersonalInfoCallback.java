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

package xyz.eulix.space.network.userinfo;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/4/7 11:50
 */
public interface PutPersonalInfoCallback {
    void onSuccess(int code, String message, String requestId, String boxUuid, String boxBind, String prefixDomain, PutPersonalInfoResult results);
    void onFailed(int code, String message, String requestId, String boxUuid, String boxBind);
    void onError(String errMsg, String boxUuid, String boxBind);
}
