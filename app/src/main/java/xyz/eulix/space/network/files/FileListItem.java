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

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * date: 2021/6/24 11:28
 */
public class FileListItem implements EulixKeep {
    private String uuid;
    //0.6.0替换为 isDir
    private Boolean is_dir;
    private boolean isDir = false;
    private String name;
    private String path;
    private Object trashed;
    //0.6.0调换为betag
    private String md5sum;
    private String betag;
    private long createdAt;
    private long operationAt;
    private long updatedAt;
    private long modifyAt;
    private String tags;
    private Long size;
    private Boolean executable;
    private String category;
    private String mime;
    private Integer version;
    private String bucketName;
    private Integer fileCount;
    private Object transactionId;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Boolean getIs_dir() {
        if (isDir){
            return true;
        }else {
            return is_dir;
        }
    }

    public void setIs_dir(Boolean is_dir) {
        this.is_dir = is_dir;
    }

    public boolean isDir() {
        return isDir;
    }

    public void setDir(boolean dir) {
        isDir = dir;
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

    public Object getTrashed() {
        return trashed;
    }

    public void setTrashed(Object trashed) {
        this.trashed = trashed;
    }

    public String getMd5sum() {
        String md5 = getBetag();
        if (TextUtils.isEmpty(md5)){
            md5 = md5sum;
        }
        return md5;
    }

    public void setMd5sum(String md5sum) {
        this.md5sum = md5sum;
    }

    public String getBetag() {
        return betag;
    }

    public void setBetag(String betag) {
        this.betag = betag;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getOperationAt() {
        return operationAt;
    }

    public void setOperationAt(long operationAt) {
        this.operationAt = operationAt;
    }

    public long getModifyAt() {
        return modifyAt;
    }

    public void setModifyAt(long modifyAt){
        this.modifyAt = modifyAt;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Boolean getExecutable() {
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

    public Integer getFileCount() {
        return fileCount;
    }

    public void setFileCount(Integer fileCount) {
        this.fileCount = fileCount;
    }

    public Object getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Object transactionId) {
        this.transactionId = transactionId;
    }

    @Override
    public String toString() {
        return "FileListItem{" +
                "uuid='" + uuid + '\'' +
                ", is_dir=" + is_dir +
                ", name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", trashed=" + trashed +
                ", md5sum='" + md5sum + '\'' +
                ", createdAt=" + createdAt +
                ", operationAt=" + operationAt +
                ", tags='" + tags + '\'' +
                ", size=" + size +
                ", executable=" + executable +
                ", category='" + category + '\'' +
                ", mime='" + mime + '\'' +
                ", version=" + version +
                ", bucketName='" + bucketName + '\'' +
                ", transactionId=" + transactionId +
                '}';
    }
}
