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

package xyz.eulix.space.network.platform;

import java.io.File;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/9/15 17:54
 */
public class AppInfoUtil {
    private static final String TAG = AppInfoUtil.class.getSimpleName();

    private AppInfoUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    public static void downloadApk(String appName, String downloadUrl, String savePath, AppDownloadCallback callback) {
        AppInfoManager.downloadApk(appName, downloadUrl, savePath, new IAppDownloadCallback() {
            @Override
            public void onError(String msg) {
                if (callback != null) {
                    callback.onError(msg);
                }
            }

            @Override
            public void onSuccess(String filePath) {
                File file = new File(filePath);
                if (file.exists()) {
                    if (callback != null) {
                        callback.onSuccess(filePath);
                    }
                } else {
                    if (callback != null) {
                        callback.onFailed();
                    }
                }
            }
        });
    }

}
