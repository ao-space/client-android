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

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.network.files.BaseResponseBody;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/11/16 10:56
 */
public class VersionCompatibleResponseBody extends BaseResponseBody implements Serializable, EulixKeep {
    @SerializedName("results")
    public Results results;

    public static class Results implements Serializable, EulixKeep {
        @SerializedName("lastestAppPkg")
        public LatestPkg latestAppPkg;
        @SerializedName("lastestBoxPkg")
        public LatestPkg latestBoxPkg;
        @SerializedName("isBoxForceUpdate")
        public Boolean isBoxForceUpdate;
        @SerializedName("isAppForceUpdate")
        public Boolean isAppForceUpdate;

        public static class LatestPkg implements Serializable, EulixKeep {
            @SerializedName("isForceUpdate")
            public Boolean isForceUpdate;
            @SerializedName("pkgVersion")
            public String pkgVersion;   //新版本名称
            @SerializedName("md5")
            public String md5;
            @SerializedName("minIOSVersion")
            public String minIOSVersion;
            @SerializedName("downloadUrl")
            public String downloadUrl;
            @SerializedName("minAndroidVersion")
            public String minAndroidVersion;
            @SerializedName("updateDesc")   //升级说明
            public String updateDesc;
            @SerializedName("pkgSize")
            public Long pkgSize; //包体大小
            @SerializedName("pkgType")
            public String pkgType;
            @SerializedName("minBoxVersion")
            public String minBoxVersion;
            @SerializedName("pkgName")
            public String pkgName;

            @Override
            public String toString() {
                return "LatestAppPkg{" +
                        "isForceUpdate=" + isForceUpdate +
                        ", pkgVersion='" + pkgVersion + '\'' +
                        ", md5='" + md5 + '\'' +
                        ", minIOSVersion='" + minIOSVersion + '\'' +
                        ", downloadUrl='" + downloadUrl + '\'' +
                        ", minAndroidVersion='" + minAndroidVersion + '\'' +
                        ", updateDesc='" + updateDesc + '\'' +
                        ", pkgSize=" + pkgSize +
                        ", pkgType='" + pkgType + '\'' +
                        ", minBoxVersion='" + minBoxVersion + '\'' +
                        ", pkgName='" + pkgName + '\'' +
                        '}';
            }
        }

        @Override
        public String toString() {
            return "Results{" +
                    "latestAppPkg=" + latestAppPkg +
                    ", latestBoxPkg=" + latestBoxPkg +
                    ", isBoxForceUpdate=" + isBoxForceUpdate +
                    ", isAppForceUpdate=" + isAppForceUpdate +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "VersionCompatibleResponseBody{" +
                "results=" + results +
                ", codeInt=" + codeInt +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}
