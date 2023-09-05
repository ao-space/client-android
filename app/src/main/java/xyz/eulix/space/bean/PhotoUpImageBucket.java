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
 * Author:      Zhu Fuyu
 * Description: 一个目录下的相册对象
 * History:     2021/7/22
 */
public class PhotoUpImageBucket implements Serializable {

    private int count = 0;
    private String bucketId;
    private String bucketName;
    private List<LocalMediaUpItem> imageList;
    private boolean checked = false;

    public int getCount() {
        if (imageList == null || imageList.isEmpty()){
            return 0;
        }else {
            return imageList.size();
        }
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getBucketId() {
        return bucketId;
    }

    public void setBucketId(String bucketId) {
        this.bucketId = bucketId;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public List<LocalMediaUpItem> getImageList() {
        return imageList;
    }

    public void setImageList(List<LocalMediaUpItem> imageList) {
        this.imageList = imageList;
    }

    public void setChecked(boolean checked){
        this.checked = checked;
    }

    public boolean isChecked() {
        return this.checked;
    }

}
