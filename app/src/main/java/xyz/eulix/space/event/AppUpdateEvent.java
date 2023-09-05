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
 * date: 2021/9/22 18:19
 */
public class AppUpdateEvent {
    private Long apkSize;
    private String downloadUrl;
    private String md5;
    private String newestVersion;
    private boolean isForce;

    public AppUpdateEvent(Long apkSize, String downloadUrl, String md5, String newestVersion, boolean isForce) {
        this.apkSize = apkSize;
        this.downloadUrl = downloadUrl;
        this.md5 = md5;
        this.newestVersion = newestVersion;
        this.isForce = isForce;
    }

    public Long getApkSize() {
        return apkSize;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public String getMd5() {
        return md5;
    }

    public String getNewestVersion() {
        return newestVersion;
    }

    public boolean isForce() {
        return isForce;
    }
}
