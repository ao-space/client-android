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

package xyz.eulix.space.network.register;

import android.text.TextUtils;

import xyz.eulix.space.util.Logger;

/**
 * @author: chenjiawei
 * date: 2021/6/17 9:57
 */
public class RegisterDeviceUtil {
    private static final String TAG = RegisterDeviceUtil.class.getSimpleName();

    private RegisterDeviceUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    public static void registerDevice(String clientRegKey, String clientUUID, String deviceId, String deviceToken, String platform, final RegisterDeviceCallback callback) {
        RegisterDeviceRequestBody body = new RegisterDeviceRequestBody();
        body.setClientRegKey(clientRegKey);
        body.setClientUUID(clientUUID);
        body.setDeviceId(deviceId);
        if (!TextUtils.isEmpty(deviceToken)) {
            body.setDeviceToken(deviceToken);
        }
        body.setPlatform(platform);

        RegisterDeviceManager.registerDevice(body, new IRegisterDeviceCallback() {
            @Override
            public void onResult(RegisterDeviceResponseBody result) {
                Logger.i(TAG, "on result: " + result);
                if (result == null) {
                    if (callback != null) {
                        callback.onFailed("", -1);
                    }
                } else {
                    if (callback != null) {
                        callback.onSuccess(result.getCode(), result.getData());
                    }
                }
            }

            @Override
            public void onError(String msg) {
                Logger.e(TAG, "on error: " + msg);
                if (callback != null) {
                    callback.onError(msg);
                }
            }
        });
    }
}
