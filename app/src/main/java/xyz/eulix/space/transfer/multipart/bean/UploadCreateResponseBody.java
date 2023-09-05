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

package xyz.eulix.space.transfer.multipart.bean;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.ArrayList;

import xyz.eulix.space.interfaces.EulixKeep;
import xyz.eulix.space.network.files.BaseResponseBody;
import xyz.eulix.space.network.files.FileListItem;

/**
 * Author:      Zhu Fuyu
 * Description: 创建分片上传任务响应数据
 * History:     2022/2/21
 */
public class UploadCreateResponseBody extends BaseResponseBody implements EulixKeep {
    @SerializedName("results")
    private Results results;

    public Results getResults() {
        return results;
    }

    public void setResults(Results results) {
        this.results = results;
    }

    public class Results implements EulixKeep, Serializable {

        //秒传返回
        @SerializedName("completeInfo")
        public FileListItem completeInfo;
        //任务已存在
        @SerializedName("conflictInfo")
        public ConflictInfo conflictInfo;
        //返回内容类型，三者取其一， 0-创建任务成功，1-秒传完成， 2-冲突，任务已存在
        @SerializedName("rspType")
        public int rspType;
        //成功刚创建任务
        @SerializedName("succInfo")
        public SuccInfo succInfo;

        public class ConflictInfo implements Serializable, EulixKeep {
            @SerializedName("betag")
            public String betag;
            @SerializedName("businessId")
            public int businessId;
            @SerializedName("createTime")
            public long createTime;
            @SerializedName("fileName")
            public String fileName;
            @SerializedName("folderId")
            public String folderId;
            @SerializedName("folderPath")
            public String folderPath;
            @SerializedName("mime")
            public String mime;
            @SerializedName("modifyTime")
            public long modifyTime;
            @SerializedName("size")
            public long size;
            @SerializedName("uploadId")
            public String uploadId;
            @SerializedName("uploadedParts")
            public ArrayList<UploadPartBean> uploadedParts;
            @SerializedName("uploadingParts")
            public ArrayList<UploadPartBean> uploadingParts;

            @Override
            public String toString() {
                return "ConflictInfo{" +
                        "betag='" + betag + '\'' +
                        ", businessId=" + businessId +
                        ", createTime=" + createTime +
                        ", fileName='" + fileName + '\'' +
                        ", folderId='" + folderId + '\'' +
                        ", folderPath='" + folderPath + '\'' +
                        ", mime='" + mime + '\'' +
                        ", modifyTime=" + modifyTime +
                        ", size=" + size +
                        ", uploadId='" + uploadId + '\'' +
                        ", uploadedParts=" + uploadedParts +
                        ", uploadingParts=" + uploadingParts +
                        '}';
            }
        }

        public class SuccInfo implements Serializable, EulixKeep {
            @SerializedName("partSize")
            public long partSize;
            @SerializedName("uploadId")
            public String uploadId;

            @Override
            public String toString() {
                return "SuccInfo{" +
                        "partSize=" + partSize +
                        ", uploadId='" + uploadId + '\'' +
                        '}';
            }
        }

    }

    @Override
    public String toString() {
        return "UploadCreateResponseBody{" +
                "results=" + results +
                ", codeInt=" + codeInt +
                ", code='" + code + '\'' +
                ", message='" + message + '\'' +
                ", requestId='" + requestId + '\'' +
                '}';
    }
}
