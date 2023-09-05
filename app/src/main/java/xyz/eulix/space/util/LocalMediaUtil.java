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

package xyz.eulix.space.util;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import xyz.eulix.space.bean.LocalMediaUpItem;

/**
 * Author:      Zhu Fuyu
 * Description:
 * History:     2021/10/18
 */
public class LocalMediaUtil {

    private LocalMediaUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static LocalMediaUpItem getMediaByUri(Uri uri, ContentResolver contentResolver) {
        LocalMediaUpItem item = null;
        if (uri.toString().contains("image")) {
            item = getLocalMediaByUriAndType(uri, contentResolver, ConstantField.MediaType.MEDIA_IMAGE);
        } else if (uri.toString().contains("video")) {
            item = getLocalMediaByUriAndType(uri, contentResolver, ConstantField.MediaType.MEDIA_VIDEO);
        }

        return item;
    }

    private static LocalMediaUpItem getLocalMediaByUriAndType(Uri uri, ContentResolver contentResolver, int mediaType) {
        if (mediaType != ConstantField.MediaType.MEDIA_IMAGE && mediaType != ConstantField.MediaType.MEDIA_VIDEO) {
            return null;
        }
        Cursor cur = null;
        LocalMediaUpItem localMediaItem = null;
        try {
            int photoIDIndex;
            int photoPathIndex;
            int videoDurationIndex = 0;
            int fileSizeIndex = -1;
            int dateIndex;
            int mimeTypeIndex;
            if (mediaType == ConstantField.MediaType.MEDIA_IMAGE) {
                cur = contentResolver.query(uri, ConstantField.IMAGE_PROJECTION, null, null,
                        MediaStore.Images.Media.DATE_MODIFIED + " desc");

                photoIDIndex = cur.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                photoPathIndex = cur.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            } else {
                cur = contentResolver.query(uri, ConstantField.VIDEO_PROJECTION, null, null,
                        MediaStore.Video.Media.DATE_MODIFIED + " desc");

                photoIDIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
                photoPathIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                videoDurationIndex = cur.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION);
            }
            fileSizeIndex = cur.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);
            dateIndex = cur.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED);
            mimeTypeIndex = cur.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE);
            if (cur.moveToFirst()) {
                int typeIndex = cur.getString(photoPathIndex).lastIndexOf(".");
                String fileName = cur.getString(photoPathIndex).substring(
                        cur.getString(photoPathIndex).lastIndexOf("/") + 1,
                        typeIndex);
                String suffix = cur.getString(photoPathIndex).substring(typeIndex + 1);
                if (suffix.length() <= 0 || cur.getLong(fileSizeIndex) <= 0) {
                    //过滤没有后缀名 or 大小为0的文件
                } else {
                    String _id = cur.getString(photoIDIndex);
                    String path = cur.getString(photoPathIndex);
                    long fileSize = cur.getLong(fileSizeIndex);
                    long modifiedDate = cur.getLong(dateIndex);
                    String mimeType = cur.getString(mimeTypeIndex);

                    localMediaItem = new LocalMediaUpItem();
                    localMediaItem.setMediaId(_id);
                    localMediaItem.setDisplayName(fileName + "." + suffix);
                    localMediaItem.setMediaPath(path);
                    localMediaItem.setSize(fileSize);
                    localMediaItem.setModifiedDate(modifiedDate);
                    localMediaItem.setMimeType(mimeType);
                    if (mediaType == ConstantField.MediaType.MEDIA_VIDEO) {
                        String duration = cur.getString(videoDurationIndex);
                        localMediaItem.setDuration(duration);
                    }
                }
            }
        } catch (Exception e) {
            Logger.e("zfy", e.getMessage());
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
        return localMediaItem;
    }
}
