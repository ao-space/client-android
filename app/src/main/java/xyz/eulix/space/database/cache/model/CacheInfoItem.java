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

package xyz.eulix.space.database.cache.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Author:      Zhu Fuyu
 * Description: 缓存数据
 * History:     2022/12/28
 */
public class CacheInfoItem implements Parcelable {

    /**
     * 自增主键id
     */
    public long _id;

    /**
     * 已安装应用
     */
    public String installedApp;

    /**
     * 账号，用于多账户区分
     */
    public String account;

    /**
     * 备用字段
     */
    public String bak;

    /**
     * 额外数据1
     */
    public String ext1;

    /**
     * 额外数据2
     */
    public String ext2;

    /**
     * 额外数据3
     */
    public String ext3;

    /**
     * 额外数据4
     */
    public String ext4;


    CacheInfoItem(Parcel p) {
        _id = p.readLong();
        installedApp = p.readString();
        account = p.readString();
        bak = p.readString();
        ext1 = p.readString();
        ext2 = p.readString();
        ext3 = p.readString();
        ext4 = p.readString();
    }

    public CacheInfoItem() {

    }

    public static final Creator<CacheInfoItem> CREATOR = new Creator<CacheInfoItem>() {
        public CacheInfoItem createFromParcel(Parcel p) {
            return new CacheInfoItem(p);
        }

        public CacheInfoItem[] newArray(int size) {
            return new CacheInfoItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(_id);
        dest.writeString(installedApp);
        dest.writeString(account);
        dest.writeString(bak);
        dest.writeString(ext1);
        dest.writeString(ext2);
        dest.writeString(ext3);
        dest.writeString(ext4);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(_id + " ");
        sb.append(installedApp + "");
        sb.append(account + "");
        sb.append(bak + " ");
        sb.append(ext1 + " ");
        sb.append(ext2 + " ");
        sb.append(ext3 + " ");
        sb.append(ext4 + " ");

        return sb.toString();
    }
}
