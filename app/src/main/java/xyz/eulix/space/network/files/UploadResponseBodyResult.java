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

package xyz.eulix.space.network.files;

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * Author:      Zhu Fuyu
 * Description: 文件上传返回结果
 * History:     2021/8/23
 */
public class UploadResponseBodyResult extends BaseResponseBody implements EulixKeep {

    private Results results;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }


    public Results getResults() {
        return results;
    }

    public void setResults(Results results) {
        this.results = results;
    }

    public static class Results implements EulixKeep{
        @SerializedName("uuid")
        private String uuid;
        @SerializedName("is_dir")
        private Boolean is_dir;
        @SerializedName("isDir")
        private Boolean isDir;
        @SerializedName("name")
        private String name;
        @SerializedName("path")
        private String path;
        @SerializedName("trashed")
        private int trashed;
        @SerializedName("md5sum")
        private String md5sum;
        @SerializedName("betag")
        private String betag;
        @SerializedName("createdAt")
        private long createdAt;
        @SerializedName("updatedAt")
        private long updatedAt;
        @SerializedName("tags")
        private String tags;
        @SerializedName("size")
        private long size;
        @SerializedName("executable")
        private Boolean executable;
        @SerializedName("category")
        private String category;
        @SerializedName("mime")
        private String mime;
        @SerializedName("version")
        private Integer version;
        @SerializedName("bucketName")
        private String bucketName;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public Boolean isIsDir() {
            if (isDir){
                return true;
            }else {
                return is_dir;
            }
        }

        public void setIsDir(Boolean isDir) {
            this.isDir = isDir;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public int isTrashed() {
            return trashed;
        }

        public void setTrashed(int trashed) {
            this.trashed = trashed;
        }

        public String getMd5sum() {
            if (!TextUtils.isEmpty(betag)){
                return betag;
            }else {
                return md5sum;
            }
        }

        public void setMd5sum(String md5sum) {
            this.md5sum = md5sum;
        }

        public long getCreatedAt() {
            return createdAt;
        }

        public void setCreatedAt(long createdAt) {
            this.createdAt = createdAt;
        }

        public long getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(long updatedAt) {
            this.updatedAt = updatedAt;
        }

        public String getTags() {
            return tags;
        }

        public void setTags(String tags) {
            this.tags = tags;
        }

        public long getSize() {
            return size;
        }

        public void setSize(long size) {
            this.size = size;
        }

        public Boolean isExecutable() {
            return executable;
        }

        public void setExecutable(Boolean executable) {
            this.executable = executable;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getMime() {
            return mime;
        }

        public void setMime(String mime) {
            this.mime = mime;
        }

        public Integer getVersion() {
            return version;
        }

        public void setVersion(Integer version) {
            this.version = version;
        }

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }
    }
}
