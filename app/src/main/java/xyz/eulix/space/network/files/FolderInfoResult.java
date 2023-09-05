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

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

import xyz.eulix.space.interfaces.EulixKeep;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/1/26 15:47
 */
public class FolderInfoResult implements Serializable, EulixKeep {
    @SerializedName("name")
    private String name;
    @SerializedName("operationAt")
    private Long operationAt;
    @SerializedName("path")
    private String path;
    @SerializedName("size")
    private Long size;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getOperationAt() {
        return operationAt;
    }

    public void setOperationAt(Long operationAt) {
        this.operationAt = operationAt;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return "FolderInfoResult{" +
                "name='" + name + '\'' +
                ", operationAt=" + operationAt +
                ", path='" + path + '\'' +
                ", size=" + size +
                '}';
    }
}
