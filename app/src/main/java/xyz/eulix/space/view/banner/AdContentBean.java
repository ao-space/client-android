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

package xyz.eulix.space.view.banner;

import android.graphics.drawable.Drawable;

/**
 * Author:      Zhu Fuyu
 * Description:
 * History:     2021/11/15
 */
public class AdContentBean {
    // 标识符
    private String adId;

    //展示图片resId
    private int imgResId;
    //h5页面地址
    private String webUrl;
    //标题名称
    private String titleName;

    //封面uuid
    private String coverUuid;
    //相簿id
    private int albumId;

    private String imageAssertName;

    public String getAdId() {
        return adId;
    }

    public void setAdId(String adId) {
        this.adId = adId;
    }

    public String getTitleName() {
        return titleName;
    }

    public void setTitleName(String titleName) {
        this.titleName = titleName;
    }

    public String getCoverUuid() {
        return coverUuid;
    }

    public void setCoverUuid(String coverUuid) {
        this.coverUuid = coverUuid;
    }

    public int getAlbumId() {
        return albumId;
    }

    public void setAlbumId(int albumId) {
        this.albumId = albumId;
    }

    public int getImgResId() {
        return imgResId;
    }

    public void setImgResId(int imgResId) {
        this.imgResId = imgResId;
    }

    public String getWebUrl() {
        return webUrl;
    }

    public void setWebUrl(String webUrl) {
        this.webUrl = webUrl;
    }

    public String getImageAssertName() {
        return imageAssertName;
    }

    public void setImageAssertName(String imageAssertName) {
        this.imageAssertName = imageAssertName;
    }
}
