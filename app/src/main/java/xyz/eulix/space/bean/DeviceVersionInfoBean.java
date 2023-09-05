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

package xyz.eulix.space.bean;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.network.files.BaseResponseBody;

/**
 * Author:      Zhu Fuyu
 * Description: 设备信息详情
 * History:     2022/7/20
 */
public class DeviceVersionInfoBean extends BaseResponseBody implements Serializable {

    @SerializedName("deviceName")
    public String deviceName;
    @SerializedName("productModel")
    public String productModel;
    @SerializedName("deviceNameEn")
    public String deviceNameEn;
    @SerializedName("generationEn")
    public String generationEn;
    @SerializedName("deviceLogoUrl")
    public String deviceLogoUrl;
    @SerializedName("snNumber")
    public String snNumber;
    @SerializedName("spaceVersion")
    public String spaceVersion;
    @SerializedName("osVersion")
    public String osVersion;
    @SerializedName("serviceVersion")
    public List<ServiceVersion> serviceVersion;
    @SerializedName("serviceDetail")
    public List<ServiceDetail> serviceDetail;


    public static class ServiceVersion implements Serializable, EulixKeep {
        @SerializedName("created")
        public long created;
        @SerializedName("serviceName")
        public String serviceName;
        @SerializedName("version")
        public String version;

        @Override
        public String toString() {
            return "ServiceVersion{" +
                    "created=" + created +
                    ", serviceName='" + serviceName + '\'' +
                    ", version='" + version + '\'' +
                    '}';
        }
    }


    public static class ServiceDetail implements Serializable, EulixKeep {
        @SerializedName("Containers")
        public long Containers;
        @SerializedName("Created")
        public long Created;
        @SerializedName("Id")
        public String Id;
        @SerializedName("Labels")
        public Object Labels;
        @SerializedName("ParentId")
        public String ParentId;
        @SerializedName("RepoTag")
        public String RepoTag;
        @SerializedName("SharedSize")
        public long SharedSize;
        @SerializedName("Size")
        public long Size;
        @SerializedName("VirtualSize")
        public long VirtualSize;
        @SerializedName("RepoDigests")
        public List<String> RepoDigests;
        @SerializedName("RepoTags")
        public List<String> RepoTags;

        @Override
        public String toString() {
            return "ServiceDetail{" +
                    "Containers=" + Containers +
                    ", Created=" + Created +
                    ", Id='" + Id + '\'' +
                    ", Labels=" + Labels +
                    ", ParentId='" + ParentId + '\'' +
                    ", RepoTag='" + RepoTag + '\'' +
                    ", SharedSize=" + SharedSize +
                    ", Size=" + Size +
                    ", VirtualSize=" + VirtualSize +
                    ", RepoDigests=" + RepoDigests +
                    ", RepoTags=" + RepoTags +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "Results{" +
                "deviceName='" + deviceName + '\'' +
                ", productModel='" + productModel + '\'' +
                ", deviceLogoUrl='" + deviceLogoUrl + '\'' +
                ", snNumber='" + snNumber + '\'' +
                ", spaceVersion='" + spaceVersion + '\'' +
                ", osVersion='" + osVersion + '\'' +
                ", serviceVersion=" + serviceVersion +
                ", serviceDetail=" + serviceDetail +
                '}';
    }
}
