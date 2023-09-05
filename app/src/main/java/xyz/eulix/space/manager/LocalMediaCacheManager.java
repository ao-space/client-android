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

package xyz.eulix.space.manager;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.bean.LocalMediaUpItem;
import xyz.eulix.space.bean.PhotoUpImageBucket;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.LocalMediaUpSelectHelper;
import xyz.eulix.space.util.Logger;

/**
 * Author:      Zhu Fuyu
 * Description: 用于缓存本地媒体数据文件
 * History:     2022/4/11
 */
public class LocalMediaCacheManager {

    //全部文件列表，不区分文件夹
    private static List<LocalMediaUpItem> totalImageList = new ArrayList<>();
    private static List<LocalMediaUpItem> totalVideoList = new ArrayList<>();
    private static List<PhotoUpImageBucket> imageBucketList = new ArrayList<>();
    private static List<LocalMediaUpItem> totalImageAndVideList = new ArrayList<>();
    private static List<PhotoUpImageBucket> imageAndVideoBucketList = new ArrayList<>();
    private static HashMap<String, String> currentImageAndVideMap = new HashMap<>();
    //图片、视频hashMap，用于快速查找已存在数据
    //图片分相簿，存放mediaId、bucketId用于快速定位相簿
    private static HashMap<String, String> currentImagesMap = new HashMap<>();
    //视频存放mediaId、path
    private static HashMap<String, String> currentVideosMap = new HashMap<>();

    private static String lastAddId = "";
    private static ContentResolver contentResolver;

    public static void getAllMediaListByType(int mediaType, GetAlbumListListener listener) {
        if (mediaType == ConstantField.MediaType.MEDIA_IMAGE) {
            getImageAlbumList(listener);
        } else if (mediaType == ConstantField.MediaType.MEDIA_VIDEO) {
            getAllVideoList(listener);
        } else if (mediaType == ConstantField.MediaType.MEDIA_FILE) {
            getAllFilesList(listener);
        } else if (mediaType == ConstantField.MediaType.MEDIA_IMAGE_AND_VIDEO) {
            getImageAndVideoList(listener);
        }
    }

    private static void getImageAlbumList(GetAlbumListListener listener) {
        if (totalImageList.isEmpty()) {
            imageBucketList.clear();
            currentImagesMap.clear();
            LocalMediaUpSelectHelper localMediaUpSelectHelper = LocalMediaUpSelectHelper.getHelper();
            localMediaUpSelectHelper.init(EulixSpaceApplication.getContext(), ConstantField.MediaType.MEDIA_IMAGE);
            localMediaUpSelectHelper.setCreateAll(true);
            localMediaUpSelectHelper.setGetAlbumListListener((list, totalFilesList) -> {
                if (totalFilesList != null && !totalFilesList.isEmpty()) {
                    totalImageList.addAll(totalFilesList);
                }
                for (int i = 0; i < list.size(); i++) {
                    String bucketId = list.get(i).getBucketId();
                    if (!bucketId.equals(ConstantField.ALL_IMAGES_BUCKET_ID)) {
                        for (int j = 0; j < list.get(i).getCount(); j++) {
                            currentImagesMap.put(list.get(i).getImageList().get(j).getMediaId(), bucketId);
                        }
                    }
                    imageBucketList.add(list.get(i));
                }
                if (listener != null) {
                    listener.onGetAlbumList(imageBucketList, totalImageList);
                }
            });
            localMediaUpSelectHelper.execute(false);
        } else {
            if (listener != null) {
                listener.onGetAlbumList(imageBucketList, totalImageList);
            }
        }
    }

    //获取包含图片、视频的所有列表
    private static void getImageAndVideoList(GetAlbumListListener listener) {
        if (totalImageAndVideList.isEmpty()) {
            imageAndVideoBucketList.clear();
            currentImageAndVideMap.clear();
            //获取本地相册数据
            LocalMediaUpSelectHelper localMediaUpSelectHelper = LocalMediaUpSelectHelper.getHelper();
            localMediaUpSelectHelper.init(EulixSpaceApplication.getContext().getApplicationContext(), ConstantField.MediaType.MEDIA_IMAGE_AND_VIDEO);
            localMediaUpSelectHelper.setCreateAll(true);
            localMediaUpSelectHelper.setGetCameraMediaListener((bucketList, mediaList, imageList, videoList) -> {
                if (mediaList != null && !mediaList.isEmpty()) {
                    totalImageAndVideList.addAll(mediaList);
                }
                imageAndVideoBucketList.addAll(bucketList);
                for (int i = 0; i < imageList.size(); i++) {
                    currentImageAndVideMap.put(imageList.get(i).getMediaId(), imageList.get(i).getBucketId());
                }
                for (int i = 0; i < videoList.size(); i++) {
                    currentVideosMap.put(videoList.get(i).getMediaId(), videoList.get(i).getMediaPath());
                }
                if (listener != null) {
                    listener.onGetAlbumList(imageAndVideoBucketList, totalImageAndVideList);
                }
            });
            localMediaUpSelectHelper.execute(false);
        } else {
            if (listener != null) {
                listener.onGetAlbumList(imageAndVideoBucketList, totalImageAndVideList);
            }
        }
    }

    private static void getAllVideoList(GetAlbumListListener listener) {
        if (totalVideoList.isEmpty()) {
            currentVideosMap.clear();
            LocalMediaUpSelectHelper localMediaUpSelectHelper = LocalMediaUpSelectHelper.getHelper();
            localMediaUpSelectHelper.init(EulixSpaceApplication.getContext(), ConstantField.MediaType.MEDIA_VIDEO);
            localMediaUpSelectHelper.setCreateAll(true);
            localMediaUpSelectHelper.setGetAlbumListListener((list, totalFilesList) -> {
                if (totalFilesList != null && !totalFilesList.isEmpty()) {
                    for (int i = 0; i < totalFilesList.size(); i++) {
                        LocalMediaUpItem item = totalFilesList.get(i);
                        totalVideoList.add(item);
                        currentVideosMap.put(item.getMediaId(), item.getMediaPath());
                    }
                }

                if (listener != null) {
                    listener.onGetAlbumList(null, totalVideoList);
                }
            });
            localMediaUpSelectHelper.execute(false);
        } else {
            if (listener != null) {
                listener.onGetAlbumList(null, totalVideoList);
            }
        }
    }

    private static void getAllFilesList(GetAlbumListListener listener) {
        //文件未做媒体库监听，暂不做缓存
        LocalMediaUpSelectHelper localMediaUpSelectHelper = LocalMediaUpSelectHelper.getHelper();
        localMediaUpSelectHelper.init(EulixSpaceApplication.getContext(), ConstantField.MediaType.MEDIA_FILE);
        localMediaUpSelectHelper.setCreateAll(true);
        localMediaUpSelectHelper.setGetAlbumListListener((list, totalFilesList) -> {
            if (listener != null) {
                listener.onGetAlbumList(null, totalFilesList);
            }
        });
        localMediaUpSelectHelper.execute(false);
    }

    //相册有新增
    public static void onGalleryAdd(LocalMediaUpItem addItem, Uri uri) {
        Logger.d("zfy", "onGalleryAdd");
        if (addItem.getMediaId().equals(lastAddId)) {
            Logger.d("zfy", "file repeat");
            return;
        }
        lastAddId = addItem.getMediaId();
        boolean isExit = false;
        if (addItem.getMimeType().contains("image")) {
            if (totalImageList.isEmpty()) {
                Logger.d("zfy", "has no cache data");
            } else {
                if (currentImagesMap.containsKey(addItem.getMediaId())) {
                    isExit = true;
                }
                if (!isExit) {
                    totalImageList.add(0, addItem);
                    //加入相簿
                    addImageToBucket(addItem, uri);
                }
            }
        } else {
            if (totalVideoList.isEmpty()) {
                Logger.d("zfy", "has no cache data");
            } else {
                if (currentVideosMap.containsKey(addItem.getMediaId())) {
                    isExit = true;
                }
                if (!isExit) {
                    addMediaToRightPosition(addItem, totalVideoList);
                    currentVideosMap.put(addItem.getMediaId(), addItem.getMediaPath());
                }
            }
        }

        if (totalImageAndVideList.isEmpty()) {
            Logger.d("zfy", "has no cache data");
        } else {
            boolean isExitImageAndVideo = false;
            if (currentImageAndVideMap.containsKey(addItem.getMediaId())) {
                isExitImageAndVideo = true;
            }
            if (!isExitImageAndVideo) {
                totalImageAndVideList.add(0, addItem);
                //加入相簿
                addToImageAndVideoBucket(addItem, uri);
            }
        }
    }

    private static void addImageToBucket(LocalMediaUpItem localMediaUpItem, Uri uri) {
        if (contentResolver == null) {
            contentResolver = EulixSpaceApplication.getContext().getContentResolver();
        }
        Cursor cur = contentResolver.query(uri, ConstantField.IMAGE_PROJECTION, null, null,
                MediaStore.Images.Media.DATE_MODIFIED + " desc");
        if (cur.moveToFirst()) {
            int bucketDisplayNameIndex = cur.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            int bucketIdIndex = cur.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID);
            String bucketName = cur.getString(bucketDisplayNameIndex);
            String bucketId = cur.getString(bucketIdIndex);
            boolean bucketExist = false;
            for (int i = 0; i < imageBucketList.size(); i++) {
                if (imageBucketList.get(i).getBucketId().equals(ConstantField.ALL_IMAGES_BUCKET_ID)) {
                    //添加至“所有图片”相册
                    Logger.d("zfy", "add to all image bucket:" + imageBucketList.get(i).getBucketName());
                    addMediaToRightPosition(localMediaUpItem, imageBucketList.get(i).getImageList());
                }
                if (imageBucketList.get(i).getBucketId().equals(bucketId)) {
                    Logger.d("zfy", "add to bucket:" + imageBucketList.get(i).getBucketName());
                    bucketExist = true;
                    addMediaToRightPosition(localMediaUpItem, imageBucketList.get(i).getImageList());
                    break;
                }
            }
            if (!bucketExist) {
                Logger.d("zfy", "create bucket:" + bucketName);
                PhotoUpImageBucket bucket = new PhotoUpImageBucket();
                bucket.setBucketId(bucketId);
                bucket.setBucketName(bucketName);
                bucket.setCount(1);
                List<LocalMediaUpItem> imageList = new ArrayList<>();
                imageList.add(localMediaUpItem);
                bucket.setImageList(imageList);
                imageBucketList.add(bucket);
            }

            currentImagesMap.put(localMediaUpItem.getMediaId(), bucketId);

            // 文件夹按内容数量排序
            sortImageBucketBySize(imageBucketList);
        }
    }

    private static void addToImageAndVideoBucket(LocalMediaUpItem localMediaUpItem, Uri uri) {
        if (contentResolver == null) {
            contentResolver = EulixSpaceApplication.getContext().getContentResolver();
        }
        boolean isImage = localMediaUpItem.getMimeType().contains("image");
        Cursor cur;
        if (isImage) {
            cur = contentResolver.query(uri, ConstantField.IMAGE_PROJECTION, null, null,
                    MediaStore.Images.Media.DATE_MODIFIED + " desc");
        } else {
            cur = contentResolver.query(uri, ConstantField.VIDEO_PROJECTION, null, null,
                    MediaStore.Video.Media.DATE_MODIFIED + " desc");
        }

        if (cur.moveToFirst()) {
            int bucketDisplayNameIndex = cur.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
            int bucketIdIndex = cur.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID);
            String bucketName = cur.getString(bucketDisplayNameIndex);
            String bucketId = cur.getString(bucketIdIndex);
            boolean bucketExist = false;
            for (int i = 0; i < imageAndVideoBucketList.size(); i++) {
                if (imageAndVideoBucketList.get(i).getBucketId().equals(ConstantField.ALL_IMAGES_BUCKET_ID)) {
                    //添加至“所有图片”相册
                    Logger.d("zfy", "add to all image bucket:" + imageAndVideoBucketList.get(i).getBucketName());
                    addMediaToRightPosition(localMediaUpItem, imageAndVideoBucketList.get(i).getImageList());
                }
                if (imageAndVideoBucketList.get(i).getBucketId().equals(bucketId)) {
                    Logger.d("zfy", "add to bucket:" + imageAndVideoBucketList.get(i).getBucketName());
                    bucketExist = true;
                    addMediaToRightPosition(localMediaUpItem, imageAndVideoBucketList.get(i).getImageList());
                    break;
                }
            }
            if (!bucketExist) {
                Logger.d("zfy", "create bucket:" + bucketName);
                PhotoUpImageBucket bucket = new PhotoUpImageBucket();
                bucket.setBucketId(bucketId);
                bucket.setBucketName(bucketName);
                bucket.setCount(1);
                List<LocalMediaUpItem> imageList = new ArrayList<>();
                imageList.add(localMediaUpItem);
                bucket.setImageList(imageList);
                imageAndVideoBucketList.add(bucket);
            }

            currentImageAndVideMap.put(localMediaUpItem.getMediaId(), bucketId);

            // 文件夹按内容数量排序
            sortImageBucketBySize(imageAndVideoBucketList);
        }
    }

    //添加一项到正确位置（按照修改时间倒序排放）
    private static int addMediaToRightPosition(LocalMediaUpItem addItem, List<LocalMediaUpItem> targetList) {
        int addPosition = 0;
        long addItemTime = addItem.getModifiedDate();
        for (int i = 0; i < targetList.size(); i++) {
            if (addItemTime < targetList.get(i).getModifiedDate()) {
                if (i < targetList.size() - 1) {
                    continue;
                } else {
                    //已到最后一项
                    targetList.add(addItem);
                    addPosition = targetList.size();
                }
            } else {
                targetList.add(i, addItem);
                addPosition = i;
                break;
            }
        }
        return addPosition;
    }

    private static void sortImageBucketBySize(List<PhotoUpImageBucket> imageBucketList) {
        Collections.sort(imageBucketList, (lhs, rhs) -> {
            if (lhs.getImageList() == null || rhs.getImageList() == null) {
                return 0;
            }
            int lSize = lhs.getImageList().size();
            int rSize = rhs.getImageList().size();
            return Integer.compare(rSize, lSize);
        });
    }

    public static void onGalleryDelete(boolean isImage, String mediaId) {
        onDeleteImageAndVideo(isImage, mediaId);

        if (isImage) {
            if (totalImageList.isEmpty()) {
                return;
            }
            LocalMediaUpItem deleteItem = null;
            if (!TextUtils.isEmpty(mediaId)) {
                for (int i = 0; i < totalImageList.size(); i++) {
                    if (totalImageList.get(i).getMediaId().equals(mediaId)) {
                        deleteItem = totalImageList.get(i);
                        totalImageList.remove(i);
                        break;
                    }
                }
            } else {
                //部分手机无法确定删除文件id，整体刷新
                Logger.d("zfy", "onGalleryDelete refresh totalImageList");
                for (int i = 0; i < totalImageList.size(); i++) {
                    File file = new File(totalImageList.get(i).getMediaPath());
                    if (!file.exists()) {
                        Logger.d("zfy", "remove " + totalImageList.get(i).getMediaPath());
                        deleteItem = totalImageList.get(i);
                        totalImageList.remove(i);
                        break;
                    }
                }
            }
            if (deleteItem != null) {
                String deleteItemBucketId = currentImagesMap.get(deleteItem.getMediaId());
                Logger.d("zfy", "deleteItemBucketId = " + deleteItemBucketId);
                for (int i = 0; i < imageBucketList.size(); i++) {
                    if (!imageBucketList.get(i).getBucketId().equals(ConstantField.ALL_IMAGES_BUCKET_ID) &&
                            !imageBucketList.get(i).getBucketId().equals(deleteItemBucketId)) {
                        continue;
                    }
                    boolean isDeleted = false;
                    for (int j = 0; j < imageBucketList.get(i).getImageList().size(); j++) {
                        if (imageBucketList.get(i).getImageList().get(j).getMediaId().equals(deleteItem.getMediaId())) {
                            imageBucketList.get(i).getImageList().remove(j);
                            Logger.d("zfy", "remove from bucket: " + imageBucketList.get(i).getBucketName());
                            if (!imageBucketList.get(i).getBucketId().equals(ConstantField.ALL_IMAGES_BUCKET_ID)) {
                                isDeleted = true;
                            }
                            if (imageBucketList.get(i).getImageList().isEmpty()) {
                                //图集内已没有文件，删除图集
                                imageBucketList.remove(i);
                            }
                            break;
                        }
                    }
                    if (isDeleted) {
                        // 文件夹按内容数量排序
                        sortImageBucketBySize(imageBucketList);
                        break;
                    }
                }
                currentImagesMap.remove(deleteItem.getMediaId());
            }
        } else {
            if (totalVideoList.isEmpty()) {
                return;
            }
            if (!TextUtils.isEmpty(mediaId)) {
                for (int i = 0; i < totalVideoList.size(); i++) {
                    if (totalVideoList.get(i).getMediaId().equals(mediaId)) {
                        totalVideoList.remove(i);
                        currentVideosMap.remove(mediaId);
                        break;
                    }
                }
                if (!currentVideosMap.containsKey(mediaId)) {
                    Logger.d("zfy", "mediaId not exist:" + mediaId);
                    return;
                }
            } else {
                Logger.d("zfy", "onGalleryDelete refresh totalImageList");
                //部分手机无法确定删除文件id，整体刷新
                for (int i = 0; i < totalVideoList.size(); i++) {
                    File file = new File(totalVideoList.get(i).getMediaPath());
                    if (!file.exists()) {
                        Logger.d("zfy", "remove " + totalVideoList.get(i).getMediaPath());
                        totalVideoList.remove(i);
                        currentVideosMap.remove(totalVideoList.get(i).getMediaId());
                        break;
                    }
                }
            }
        }
    }

    private static void onDeleteImageAndVideo(boolean isImage, String mediaId) {
        if (!totalImageAndVideList.isEmpty()) {
            LocalMediaUpItem deleteItem = null;
            if (!TextUtils.isEmpty(mediaId)) {
                for (int i = 0; i < totalImageAndVideList.size(); i++) {
                    if (totalImageAndVideList.get(i).getMediaId().equals(mediaId)) {
                        deleteItem = totalImageAndVideList.get(i);
                        totalImageAndVideList.remove(i);
                        break;
                    }
                }
            } else {
                //部分手机无法确定删除文件id，整体刷新
                Logger.d("zfy", "onGalleryDelete refresh totalImageAndVideList");
                for (int i = 0; i < totalImageAndVideList.size(); i++) {
                    File file = new File(totalImageAndVideList.get(i).getMediaPath());
                    if (!file.exists()) {
                        Logger.d("zfy", "remove " + totalImageAndVideList.get(i).getMediaPath());
                        deleteItem = totalImageAndVideList.get(i);
                        totalImageAndVideList.remove(i);
                        break;
                    }
                }
            }
            if (deleteItem != null) {
                String deleteItemBucketId = currentImageAndVideMap.get(deleteItem.getMediaId());
                Logger.d("zfy", "deleteItemBucketId = " + deleteItemBucketId);
                for (int i = 0; i < imageAndVideoBucketList.size(); i++) {
                    if (!imageAndVideoBucketList.get(i).getBucketId().equals(ConstantField.ALL_IMAGES_BUCKET_ID) &&
                            !imageAndVideoBucketList.get(i).getBucketId().equals(deleteItemBucketId)) {
                        continue;
                    }
                    boolean isDeleted = false;
                    for (int j = 0; j < imageAndVideoBucketList.get(i).getImageList().size(); j++) {
                        if (imageAndVideoBucketList.get(i).getImageList().get(j).getMediaId().equals(deleteItem.getMediaId())) {
                            imageAndVideoBucketList.get(i).getImageList().remove(j);
                            Logger.d("zfy", "remove from bucket: " + imageAndVideoBucketList.get(i).getBucketName());
                            if (!imageAndVideoBucketList.get(i).getBucketId().equals(ConstantField.ALL_IMAGES_BUCKET_ID)) {
                                isDeleted = true;
                            }
                            if (imageAndVideoBucketList.get(i).getImageList().isEmpty()) {
                                //图集内已没有文件，删除图集
                                imageAndVideoBucketList.remove(i);
                            }
                            break;
                        }
                    }
                    if (isDeleted) {
                        // 文件夹按内容数量排序
                        sortImageBucketBySize(imageAndVideoBucketList);
                        break;
                    }
                }
                currentImageAndVideMap.remove(deleteItem.getMediaId());
            }
        }
    }

    private boolean isItemExist(boolean isImage, LocalMediaUpItem addItem) {
        boolean isExist = false;
        if (isImage) {
            for (int i = 0; i < totalImageList.size(); i++) {
                if (totalImageList.get(i).getMediaPath().equals(addItem.getMediaPath())) {
                    isExist = true;
                    break;
                }
            }
        }
        return isExist;
    }

    public interface GetAlbumListListener {
        void onGetAlbumList(List<PhotoUpImageBucket> list, List<LocalMediaUpItem> totalFilesList);
    }
}
