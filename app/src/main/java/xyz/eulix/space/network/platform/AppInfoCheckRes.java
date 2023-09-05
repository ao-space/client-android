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

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/9/15 16:52
 */
public class AppInfoCheckRes implements EulixKeep {
    private String appName;
    private Long appSize;
    private String appType;
    private String downloadUrl;
    private boolean isForceUpdate;
    private boolean isUpdate;
    private String md5;
    private String newestVersion;
    private String updateDesc;

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public Long getAppSize() {
        return appSize;
    }

    public void setAppSize(Long appSize) {
        this.appSize = appSize;
    }

    public String getAppType() {
        return appType;
    }

    public void setAppType(String appType) {
        this.appType = appType;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public boolean isForceUpdate() {
        return isForceUpdate;
    }

    public void setForceUpdate(boolean forceUpdate) {
        isForceUpdate = forceUpdate;
    }

    public boolean isUpdate() {
        return isUpdate;
    }

    public void setUpdate(boolean update) {
        isUpdate = update;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getNewestVersion() {
        return newestVersion;
    }

    public void setNewestVersion(String newestVersion) {
        this.newestVersion = newestVersion;
    }

    public String getUpdateDesc() {
        return updateDesc;
    }

    public void setUpdateDesc(String updateDesc) {
        this.updateDesc = updateDesc;
    }

    @Override
    public String toString() {
        return "AppInfoCheckRes{" +
                "appName='" + appName + '\'' +
                ", appSize=" + appSize +
                ", appType='" + appType + '\'' +
                ", downloadUrl='" + downloadUrl + '\'' +
                ", isForceUpdate=" + isForceUpdate +
                ", isUpdate=" + isUpdate +
                ", md5='" + md5 + '\'' +
                ", newestVersion='" + newestVersion + '\'' +
                ", updateDesc='" + updateDesc + '\'' +
                '}';
    }
}
