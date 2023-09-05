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

package xyz.eulix.space.network.gateway;

/**
 * @author: chenjiawei
 * date: 2021/7/9 14:47
 */
public interface CreateAuthTokenCallback {
    void onSuccess(String boxUUID, String boxBindValue, String accessToken, String transformation, String initializationVector, String secret, String expiresAt, Long expiresAtEpochSeconds, String refreshToken, String requestId);
    void onFailed();
    void onError(int code, String msg);
}
