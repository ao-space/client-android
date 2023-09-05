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

import java.io.Serializable;
import java.util.List;

/**
 * @author: chenjiawei
 * date: 2021/6/1 14:49
 */
public class CustomizeFile extends Storage implements Serializable {
    private String id;
    private String mime;
    private String name;
    private long size;
    private String path;
    private String md5;
    private List<CustomizeFile> content;

    private boolean isSelected = false;
    //是否标记为喜欢（相册使用）
    private boolean isLike = false;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMime() {
        return mime;
    }

    public void setMime(String mime) {
        this.mime = mime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<CustomizeFile> getContent() {
        return content;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public void setContent(List<CustomizeFile> content) {
        this.content = content;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isLike() {
        return isLike;
    }

    public void setLike(boolean like) {
        isLike = like;
    }

    @Override
    public String toString() {
        return "CustomizeFile{" +
                "id='" + id + '\'' +
                ", mime='" + mime + '\'' +
                ", name='" + name + '\'' +
                ", size=" + size +
                ", path='" + path + '\'' +
                ", md5='" + md5 + '\'' +
                ", content=" + content +
                ", isSelected=" + isSelected +
                ", isLike=" + isLike +
                '}';
    }
}
