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

import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.transfer.TransferHelper;
import xyz.eulix.space.util.FileUtil;

/**
 * Author:      Zhu Fuyu
 * Description: 传输任务模型的创建工厂
 * History:     2021/8/26
 */
public class TransferItemFactory {

    private TransferItemFactory() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 根据Cursor创建DownItem
     *
     * @param cursor
     * @return
     * @throws IllegalArgumentException
     */
    public static TransferItem create(Cursor cursor)
            throws IllegalArgumentException {
        TransferItem item = new TransferItem();

        try {
            item._id = cursor.getLong(cursor
                    .getColumnIndexOrThrow(TransferHelper.KEY_ID));
            item.showName = cursor.getString(cursor
                    .getColumnIndexOrThrow(TransferHelper.KEY_SHOW_NAME));
            item.keyName = cursor.getString(cursor
                    .getColumnIndexOrThrow(TransferHelper.KEY_KEY_NAME));
            item.transferType = cursor.getInt(cursor
                    .getColumnIndexOrThrow(TransferHelper.KEY_TRANSFER_TYPE));
            item.uuid = cursor.getString(cursor
                    .getColumnIndexOrThrow(TransferHelper.KEY_UUID));
            item.localPath = cursor.getString(cursor
                    .getColumnIndexOrThrow(TransferHelper.KEY_LOCAL_PATH));
            item.remotePath = cursor.getString(cursor
                    .getColumnIndexOrThrow(TransferHelper.KEY_REMOTE_PATH));
            item.cachePath = cursor.getString(cursor
                    .getColumnIndexOrThrow(TransferHelper.KEY_CACHE_PATH));
            item.suffix = cursor.getString(cursor
                    .getColumnIndexOrThrow(TransferHelper.KEY_FILE_SUFFIX));
            item.mimeType = cursor.getString(cursor
                    .getColumnIndexOrThrow(TransferHelper.KEY_FILE_MIMETYPE));
            item.currentSize = cursor.getLong(cursor
                    .getColumnIndexOrThrow(TransferHelper.KEY_SIZE_CURRENT));
            item.totalSize = cursor.getLong(cursor
                    .getColumnIndexOrThrow(TransferHelper.KEY_SIZE_TOTAL));
            item.md5 = cursor.getString(cursor
                    .getColumnIndexOrThrow(TransferHelper.KEY_MD5));
            item.account = cursor.getString(cursor
                    .getColumnIndexOrThrow(TransferHelper.KEY_ACCOUNT));
            item.state = cursor.getInt(cursor
                    .getColumnIndexOrThrow(TransferHelper.KEY_STATE));
            item.errorCode = cursor.getInt(cursor
                    .getColumnIndexOrThrow(TransferHelper.KEY_ERROR_CODE));
            item.bak = cursor.getString(cursor
                    .getColumnIndexOrThrow(TransferHelper.KEY_BAK));
            item.ext1 = cursor.getString(cursor
                    .getColumnIndexOrThrow(TransferHelper.KEY_EXT1));
            item.ext2 = cursor.getString(cursor
                    .getColumnIndexOrThrow(TransferHelper.KEY_EXT2));
            item.ext3 = cursor.getString(cursor
                    .getColumnIndexOrThrow(TransferHelper.KEY_EXT3));
            item.ext4 = cursor.getString(cursor
                    .getColumnIndexOrThrow(TransferHelper.KEY_EXT4));
            item.priority = cursor.getInt(cursor
                    .getColumnIndexOrThrow(TransferHelper.KEY_PRIORITY));
            item.createTime = cursor.getLong(cursor
                    .getColumnIndexOrThrow(TransferHelper.KEY_CREATE_TIME));
            item.updateTime = cursor.getLong(cursor
                    .getColumnIndexOrThrow(TransferHelper.KEY_UPDATE_TIME));
        } catch (IllegalArgumentException e) {
            throw e;
        }

        return item;
    }

    /**
     * 创建传输开始任务
     */
    public static TransferItem createStartTransferItem(String filename, int transferType, String localPath, String remotePath, String cachePath, long totalSize, long modifyTime, String uuid, String md5, String uniqueTag) {
        if (TextUtils.isEmpty(filename)) {
            return null;
        }
        int typeIndex = filename.lastIndexOf(".");
        String suffix = filename.substring(typeIndex + 1);
        String mimeType = FileUtil.getMimeType(suffix);

        TransferItem item = new TransferItem();
        item.showName = filename;
        item.keyName = filename;
        item.transferType = transferType;
        item.localPath = localPath;
        item.remotePath = remotePath;
        item.cachePath = cachePath;
        item.currentSize = 0L;
        item.suffix = suffix;
        item.mimeType = mimeType;
        item.totalSize = totalSize;
        item.createTime = modifyTime;
        item.updateTime = modifyTime;
        item.state = TransferHelper.STATE_DOING;
        item.uuid = uuid;
        item.md5 = md5;
        item.account = EulixSpaceDBUtil.queryAvailableBoxUuid(EulixSpaceApplication.getContext()) + EulixSpaceDBUtil.getCurrentUserId(EulixSpaceApplication.getContext());
        item.ext1 = uniqueTag;
        return item;
    }

    /**
     * 创建上传任务item
     */
    public static TransferItem createUploadPrepareItem(Context context, String filename, String localPath, String remotePath, long totalSize, long modifyTime, String uuid, String md5, String uniqueTag, boolean isSync, String albumId) {
        if (TextUtils.isEmpty(filename)) {
            return null;
        }
        int typeIndex = filename.lastIndexOf(".");
        String suffix = filename.substring(typeIndex + 1);
        String mimeType = FileUtil.getMimeType(suffix);

        String cachePath = context.getExternalCacheDir().getAbsolutePath() + "/upload/";

        TransferItem item = new TransferItem();
        item.showName = filename;
        item.keyName = filename;
        item.transferType = isSync ? TransferHelper.TYPE_SYNC : TransferHelper.TYPE_UPLOAD;
        item.localPath = localPath;
        item.remotePath = remotePath;
        item.cachePath = cachePath;
        item.currentSize = 0L;
        item.suffix = suffix;
        item.mimeType = mimeType;
        item.totalSize = totalSize;
        item.createTime = modifyTime;
        item.updateTime = modifyTime;
        item.state = TransferHelper.STATE_PREPARE;
        item.uuid = uuid;
        item.md5 = md5;
        item.account = EulixSpaceDBUtil.queryAvailableBoxUuid(EulixSpaceApplication.getContext()) + EulixSpaceDBUtil.getCurrentUserId(EulixSpaceApplication.getContext());
        item.ext1 = uniqueTag;
        item.ext3 = albumId;
        return item;
    }

    /**
     * 创建下载开始任务
     */
    public static TransferItem createDownloadPrepareItem(String filename, String localPath, String remotePath, String cachePath, long totalSize, long modifyTime, String uuid, String md5, String uniqueTag, String from) {
        if (TextUtils.isEmpty(filename)) {
            return null;
        }
        int typeIndex = filename.lastIndexOf(".");
        String suffix = filename.substring(typeIndex + 1);
        String mimeType = FileUtil.getMimeType(suffix);

        TransferItem item = new TransferItem();
        item.showName = filename;
        item.keyName = filename;
        item.transferType = TransferHelper.TYPE_DOWNLOAD;
        item.localPath = localPath;
        item.remotePath = remotePath;
        item.cachePath = cachePath;
        item.currentSize = 0L;
        item.suffix = suffix;
        item.mimeType = mimeType;
        item.totalSize = totalSize;
        item.createTime = modifyTime;
        item.updateTime = modifyTime;
        item.state = TransferHelper.STATE_PREPARE;
        item.uuid = uuid;
        item.md5 = md5;
        item.account = EulixSpaceDBUtil.queryAvailableBoxUuid(EulixSpaceApplication.getContext()) + EulixSpaceDBUtil.getCurrentUserId(EulixSpaceApplication.getContext());
        item.ext1 = uniqueTag;
        item.ext2 = from;
        return item;
    }

    public static TransferItem createResumeItem(String keyName) {
        if (TextUtils.isEmpty(keyName)) {
            throw new IllegalArgumentException("The keyName is empty or null!");
        }

        TransferItem item = new TransferItem();

        item.keyName = keyName;
        item.state = TransferHelper.STATE_QUEUE;

        return item;
    }

    //获取唯一标识符
    public static String getUniqueTag(int transferType, String fileName, String localPath, String remotePath, String uuid) {
        return getUniqueTagWithAlbumId(transferType, fileName, localPath, remotePath, uuid, "");
    }

    //获取唯一标识符
    public static String getUniqueTagWithAlbumId(int transferType, String fileName, String localPath, String remotePath, String uuid, String albumId) {
        String uniqueTag = "";
        switch (transferType) {
            case TransferHelper.TYPE_UPLOAD:
                uniqueTag = transferType + localPath + remotePath + fileName;
                if (!TextUtils.isEmpty(albumId)) {
                    uniqueTag = uniqueTag + albumId;
                }
                break;
            case TransferHelper.TYPE_SYNC:
                uniqueTag = transferType + "/" + fileName;
                break;
            case TransferHelper.TYPE_DOWNLOAD:
            case TransferHelper.TYPE_CACHE:
            case TransferHelper.TYPE_PREVIEW:
                uniqueTag = transferType + uuid;
                break;
            default:
                break;
        }
        return uniqueTag;
    }

}
