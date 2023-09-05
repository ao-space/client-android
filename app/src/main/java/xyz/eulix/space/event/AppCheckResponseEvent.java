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
 * date: 2021/9/18 16:15
 */
public class AppCheckResponseEvent {
    private Long apkSize;
    private String downloadUrl;
    private Boolean isUpdate;
    private String md5;
    private String newestVersion;
    private String updateDescription;
    private boolean isRemindForce;
    private boolean isPlatformConnectFail = false;

    public AppCheckResponseEvent(Long apkSize, String downloadUrl, Boolean isUpdate, String md5, String newestVersion, String updateDescription, boolean isRemindForce, boolean isPlatformConnectFail) {
        this.apkSize = apkSize;
        this.downloadUrl = downloadUrl;
        this.isUpdate = isUpdate;
        this.md5 = md5;
        this.newestVersion = newestVersion;
        this.updateDescription = updateDescription;
        this.isRemindForce = isRemindForce;
        this.isPlatformConnectFail = isPlatformConnectFail;
    }

    public Long getApkSize() {
        return apkSize;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public Boolean getUpdate() {
        return isUpdate;
    }

    public String getMd5() {
        return md5;
    }

    public String getNewestVersion() {
        return newestVersion;
    }

    public String getUpdateDescription() {
        return updateDescription;
    }

    public boolean isRemindForce() {
        return isRemindForce;
    }

    public boolean isPlatformConnectFail() {
        return isPlatformConnectFail;
    }

    public void setPlatformConnectFail(boolean platformConnectFail) {
        isPlatformConnectFail = platformConnectFail;
    }
}
