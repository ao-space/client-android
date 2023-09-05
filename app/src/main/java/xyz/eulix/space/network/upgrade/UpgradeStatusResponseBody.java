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

package xyz.eulix.space.network.upgrade;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import xyz.eulix.space.network.files.BaseResponseBody;

/**
 * Author:      Zhu Fuyu
 * Description: 查询系统升级状态
 * History:     2021/11/8
 */
public class UpgradeStatusResponseBody extends BaseResponseBody implements Serializable {

    @SerializedName("versionId")
    public String versionId;
    //升级状态 整体流程状态："", downloading, downloaded, installing, installed, download-err，install-err
    @SerializedName("status")
    public String status;
    @SerializedName("startDownTime")
    public String startDownTime;
    @SerializedName("startInstallTime")
    public String startInstallTime;
    @SerializedName("doneDownTime")
    public String doneDownTime;
    @SerializedName("doneInstallTime")
    public String doneInstallTime;
    @SerializedName("UpdateFrom")
    public String UpdateFrom;
    @SerializedName("installStatus")
    public String installStatus;
    @SerializedName("downStatus")
    public String downStatus;

    @Override
    public String toString() {
        return "UpgradeStatusResponseBody{" +
                "versionId='" + versionId + '\'' +
                ", status='" + status + '\'' +
                ", startDownTime='" + startDownTime + '\'' +
                ", startInstallTime='" + startInstallTime + '\'' +
                ", doneDownTime='" + doneDownTime + '\'' +
                ", doneInstallTime='" + doneInstallTime + '\'' +
                ", UpdateFrom='" + UpdateFrom + '\'' +
                ", installStatus='" + installStatus + '\'' +
                ", downStatus='" + downStatus + '\'' +
                ", codeInt=" + codeInt +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}
