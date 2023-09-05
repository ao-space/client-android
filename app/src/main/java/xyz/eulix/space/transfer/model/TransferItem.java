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

package xyz.eulix.space.transfer.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Author:      Zhu Fuyu
 * Description: 传输任务模型
 * History:     2021/8/26
 */
public class TransferItem implements Parcelable {

    /**
     * 自增主键id
     */
    public long _id;

    /**
     * 显示名称
     */
    public String showName;

    /**
     * 文件名,可作为主键，应保证唯一性
     */
    public String keyName;

    /**
     * 传输类型（上传 or 下载）
     */
    public int transferType;

    /**
     * 文件唯一标识符
     */
    public String uuid;

    /**
     * 本地路径（上传：对应源文件路径；下载：对应存储路径）
     */
    public String localPath;

    /**
     * 远端路径（上传：对应上传位置；下载：对应服务端存储路径）
     */
    public String remotePath;

    /**
     * 缓存路径（上传时存在，为临时加密文件存放路劲；下载不存在，直接解密数据流到本地路径）
     */
    public String cachePath;

    /**
     * 文件名后缀
     */
    public String suffix;

    /**
     * MIME TYPE
     */
    public String mimeType;

    /**
     * 已经传输大小
     */
    public long currentSize = 0;

    /**
     * 文件总大小
     */
    public long totalSize = 0;

    /**
     * MD5校验码，需要由发起下载者提供
     */
    public String md5;

    /**
     * 账号，用于多账户区分
     */
    public String account;

    /**
     * 下载状态
     */
    public int state;


    /**
     * 错误代码
     */
    public int errorCode;

    /**
     * 备用字段
     */
    public String bak;

    /**
     * 额外数据1（存放uniqueTag）
     */
    public String ext1;

    /**
     * 额外数据2（存放缓存来源）
     */
    public String ext2;

    /**
     * 额外数据3（存放albumId）
     */
    public String ext3;

    /**
     * 额外数据4
     */
    public String ext4;

    /**
     * 优先级
     */
    public int priority = 0;

    /**
     * 创建时间
     */
    public long createTime;

    /**
     * 更新时间
     */
    public long updateTime;

    //是否被选中 - 用于选中操作交互
    public boolean isSelected;


    TransferItem(Parcel p) {
        _id = p.readLong();
        showName = p.readString();
        keyName = p.readString();
        transferType = p.readInt();
        uuid = p.readString();
        localPath = p.readString();
        remotePath = p.readString();
        cachePath = p.readString();
        suffix = p.readString();
        mimeType = p.readString();
        currentSize = p.readLong();
        totalSize = p.readLong();
        md5 = p.readString();
        account = p.readString();
        state = p.readInt();
        errorCode = p.readInt();
        bak = p.readString();
        ext1 = p.readString();
        ext2 = p.readString();
        ext3 = p.readString();
        ext4 = p.readString();
        priority = p.readInt();
        createTime = p.readLong();
        updateTime = p.readLong();
        isSelected = p.readInt() == 1;
    }

    public TransferItem() {

    }

    public static final Creator<TransferItem> CREATOR = new Creator<TransferItem>() {
        public TransferItem createFromParcel(Parcel p) {
            return new TransferItem(p);
        }

        public TransferItem[] newArray(int size) {
            return new TransferItem[size];
        }
    };

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        // TODO Auto-generated method stub
        dest.writeLong(_id);
        dest.writeString(showName);
        dest.writeString(keyName);
        dest.writeInt(transferType);
        dest.writeString(uuid);
        dest.writeString(localPath);
        dest.writeString(remotePath);
        dest.writeString(cachePath);
        dest.writeString(suffix);
        dest.writeString(mimeType);
        dest.writeLong(currentSize);
        dest.writeLong(totalSize);
        dest.writeString(md5);
        dest.writeString(account);
        dest.writeInt(state);
        dest.writeInt(errorCode);
        dest.writeString(bak);
        dest.writeString(ext1);
        dest.writeString(ext2);
        dest.writeString(ext3);
        dest.writeString(ext4);
        dest.writeInt(priority);
        dest.writeLong(createTime);
        dest.writeLong(updateTime);
        dest.writeInt(isSelected ? 1 : 0);
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        StringBuilder sb = new StringBuilder();

        sb.append(_id + " ");
        sb.append(showName + " ");
        sb.append(keyName + " ");
        sb.append(uuid + " ");
        sb.append(localPath + "");
        sb.append(remotePath + "");
        sb.append(cachePath + "");
        sb.append(suffix + " ");
        sb.append(mimeType + " ");
        sb.append(currentSize + " ");
        sb.append(totalSize + " ");
        sb.append(md5 + " ");
        sb.append(account + "");
        sb.append(state + " ");
        sb.append(errorCode + " ");
        sb.append(bak + " ");
        sb.append(ext1 + " ");
        sb.append(ext2 + " ");
        sb.append(ext3 + " ");
        sb.append(ext4 + " ");
        sb.append(priority + " ");
        sb.append(createTime + " ");
        sb.append(updateTime);

        return sb.toString();
    }
}
