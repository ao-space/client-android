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

package xyz.eulix.space.network.agent;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/7/27 13:59
 */
public interface PairingCallback {
    void onSuccess(Integer code, String message, String deviceAddress, String boxName, String boxUuid, String boxPubKey, String authKey, String regKey, String userDomain, int paired);
    void onFailed(String message, Integer code);
    void onError(String msg);
}
