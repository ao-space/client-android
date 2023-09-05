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

package xyz.eulix.space.network.box;

import android.content.Context;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import java.util.concurrent.RejectedExecutionException;

import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.ThreadPool;
import xyz.eulix.space.util.Urls;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/9/18 10:11
 */
public class AccountInfoUtil {
    private static final String TAG = AccountInfoUtil.class.getSimpleName();
    private static final String API_VERSION = ConstantField.BoxVersionName.VERSION_0_2_0;

    private AccountInfoUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    public static void getDeviceStorageInfo(@NonNull Context context, String boxUuid, String clientUUID, String boxDomain, String accessToken, String secret, String transformation, String ivParams, boolean isLAN, DeviceInfoCallback callback) {
        if (!TextUtils.isEmpty(clientUUID)) {
            String finalClientUUID = clientUUID;
            String finalBoxDomain = Urls.getBaseUrl();
            try {
                ThreadPool.getInstance().execute(() -> AccountInfoManager.getDeviceStorageInfo(finalClientUUID, finalBoxDomain, accessToken, secret
                        , transformation, ivParams, API_VERSION, new IDeviceInfoCallback() {
                            @Override
                            public void onError(String msg) {
                                Logger.e(TAG, "on error: " + msg);
                                    callback.onError(msg);
                            }

                            @Override
                            public void onResult(DeviceInfoResult result) {
                                Logger.i(TAG, "on result: " + result);
                                Long totalSizeValue = null;
                                Long usedSizeValue = null;
                                if (result != null) {
                                    String spaceSizeTotal = result.getSpaceSizeTotal();
                                    String spaceSizeUsed = result.getSpaceSizeUsed();
                                    try {
                                        totalSizeValue = Long.parseLong(spaceSizeTotal);
                                    } catch (NumberFormatException e) {
                                        e.printStackTrace();
                                    }
                                    try {
                                        usedSizeValue = Long.parseLong(spaceSizeUsed);
                                    } catch (NumberFormatException e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (totalSizeValue == null || usedSizeValue == null) {
                                    if (callback != null) {
                                        callback.onFailed();
                                    }
                                } else {
                                    if (callback != null) {
                                        callback.onSuccess(boxUuid, result.getRequestId(), usedSizeValue, totalSizeValue);
                                    }
                                }
                            }
                        }));
            } catch (RejectedExecutionException e) {
                e.printStackTrace();
            }
        } else {
            if (callback != null) {
                callback.onError(null);
            }
        }
    }
}
