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

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import java.io.File;

import static android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
import static android.provider.MediaStore.MediaColumns.DATE_TAKEN;
import static android.provider.MediaStore.MediaColumns.HEIGHT;
import static android.provider.MediaStore.MediaColumns.MIME_TYPE;
import static android.provider.MediaStore.MediaColumns.ORIENTATION;
import static android.provider.MediaStore.MediaColumns.WIDTH;

/**
 * Author:      Zhu Fuyu
 * Description: 图片、视频插入系统媒体库帮助类
 * History:     2021/9/23
 */
public class AlbumNotifyHelper {

    public static final String TAG = AlbumNotifyHelper.class.getSimpleName();

    //插入到系统媒体库，可在相册中展示
    public static void insertToAlbum(Context context, File file){
        String filePath = file.getAbsolutePath();
        String mimeType = FileUtil.getMimeTypeByPath(filePath);
        if (mimeType.contains("image")) {
            insertImageToMediaStore(context, filePath, 0);
        } else if (mimeType.contains("video")) {
            insertVideoToMediaStore(context, filePath, 0);
        } else if (mimeType.contains("audio")){
            insertAudioToMediaStore(context, filePath, 0);
        }else {
            insertFileToMediaStore(context, filePath, 0);
        }
    }

    ///
    // 扫描系统相册核心方法
    ///

    /**
     * 针对系统文件夹只需要扫描,不用插入内容提供者,不然会重复
     *
     * @param context  上下文
     * @param filePath 文件路径
     */
    public static void scanFile(Context context, String filePath) {
        if (!checkFile(filePath)) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(new File(filePath)));
        context.sendBroadcast(intent);
    }


    ///
    // 非系统相册向MediaContent中插入数据，核心方法
    ///

    /**
     * 针对非系统文件夹下的文件,使用该方法
     * 插入时初始化公共字段
     *
     * @param filePath 文件
     * @param time     ms
     * @return ContentValues
     */
    private static ContentValues initCommonContentValues(String filePath, long time) {
        ContentValues values = new ContentValues();
        File saveFile = new File(filePath);
        long timeMillis = getTimeWrap(time);
        values.put(MediaStore.MediaColumns.TITLE, saveFile.getName());
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, saveFile.getName());
        values.put(MediaStore.MediaColumns.DATE_MODIFIED, timeMillis);
        values.put(MediaStore.MediaColumns.DATE_ADDED, timeMillis);
        values.put(MediaStore.MediaColumns.DATA, saveFile.getAbsolutePath());
        values.put(MediaStore.MediaColumns.SIZE, saveFile.length());
        return values;
    }

    public static void insertImageToMediaStore(Context context, String filePath, long createTime){
        BitmapFactory.Options opts = new BitmapFactory.Options();
        //只请求图片宽高，不解析图片像素(请求图片属性但不申请内存，解析bitmap对象，该对象不占内存)
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, opts);
        int imageWidth = opts.outWidth;
        int imageHeight = opts.outHeight;
        insertImageToMediaStore(context, filePath, System.currentTimeMillis(), imageWidth, imageHeight);
    }

    /**
     * 保存到照片到本地，并插入MediaStore以保证相册可以查看到,这是更优化的方法，防止读取的照片获取不到宽高
     *
     * @param context    上下文
     * @param filePath   文件路径
     * @param createTime 创建时间 <=0时为当前时间 ms
     * @param width      宽度
     * @param height     高度
     */
    public static void insertImageToMediaStore(Context context, String filePath, long createTime, int width, int height) {
        if (!checkFile(filePath))
            return;
        createTime = getTimeWrap(createTime);
        ContentValues values = initCommonContentValues(filePath, createTime);
        values.put(DATE_TAKEN, createTime);
        values.put(ORIENTATION, 0);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            if (width > 0) values.put(WIDTH, 0);
            if (height > 0) values.put(HEIGHT, 0);
        }
        values.put(MIME_TYPE, FileUtil.getMimeTypeByPath(filePath));
        context.getApplicationContext().getContentResolver().insert(EXTERNAL_CONTENT_URI, values);
    }

    public static void insertVideoToMediaStore(Context context, String filePath, long createTime){
        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()){
            retriever.setDataSource(filePath);
            int width = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
            int height = Integer.parseInt(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
            long duration = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            retriever.release();
            insertVideoToMediaStore(context, filePath, createTime, width, height, duration);
        } catch (Exception e) {
            Logger.d(e.getMessage());
        }
    }

    /**
     * 保存到视频到本地，并插入MediaStore以保证相册可以查看到,这是更优化的方法，防止读取的视频获取不到宽高
     *
     * @param context    上下文
     * @param filePath   文件路径
     * @param createTime 创建时间 <=0时为当前时间 ms
     * @param duration   视频长度 ms
     * @param width      宽度
     * @param height     高度
     */
    public static void insertVideoToMediaStore(Context context, String filePath, long createTime, int width, int height, long duration) {
        if (!checkFile(filePath))
            return;
        createTime = getTimeWrap(createTime);
        ContentValues values = initCommonContentValues(filePath, createTime);
        values.put(DATE_TAKEN, createTime);
        if (duration > 0)
            values.put(MediaStore.Video.VideoColumns.DURATION, duration);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            if (width > 0) values.put(MediaStore.Video.VideoColumns.WIDTH, width);
            if (height > 0) values.put(MediaStore.Video.VideoColumns.HEIGHT, height);
        }
        values.put(MediaStore.MediaColumns.MIME_TYPE, FileUtil.getMimeTypeByPath(filePath));
        context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
    }

    public static void insertAudioToMediaStore(Context context, String filePath, long createTime){
        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()){
            retriever.setDataSource(filePath);
            long duration = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            retriever.release();
            createTime = getTimeWrap(createTime);
            ContentValues values = initCommonContentValues(filePath, createTime);
            values.put(DATE_TAKEN, createTime);
            if (duration > 0)
                values.put(MediaStore.Audio.AudioColumns.DURATION, duration);

            values.put(MediaStore.MediaColumns.MIME_TYPE, FileUtil.getMimeTypeByPath(filePath));
            context.getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
        } catch (Exception e) {
            Logger.d(e.getMessage());
        }
    }

    public static void insertFileToMediaStore(Context context, String filePath, long createTime){
        try {
            createTime = getTimeWrap(createTime);
            ContentValues values = initCommonContentValues(filePath, createTime);

            values.put(MediaStore.MediaColumns.MIME_TYPE, FileUtil.getMimeTypeByPath(filePath));
//            context.getApplicationContext().getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            context.getApplicationContext().getContentResolver().insert(MediaStore.Files.getContentUri("external"), values);
        } catch (Exception e) {
            e.printStackTrace();
            Logger.d("zfy", "insertFileToMediaStore error");
            Logger.e(e.getMessage());
        }

    }



    /**
     * 是不是系统相册
     *
     * @param path
     * @return
     */
    private static boolean isSystemDcim(String path) {
        return path.toLowerCase().contains("dcim") || path.toLowerCase().contains("camera");
    }

    /**
     * 获取照片的mine_type
     *
     * @param path
     * @return
     */
    private static String getPhotoMimeType(String path) {
        String lowerPath = path.toLowerCase();
        if (lowerPath.endsWith("jpg") || lowerPath.endsWith("jpeg")) {
            return "image/jpeg";
        } else if (lowerPath.endsWith("png")) {
            return "image/png";
        } else if (lowerPath.endsWith("gif")) {
            return "image/gif";
        } else if (lowerPath.endsWith("heic")) {
            return "image/heic";
        } else if (lowerPath.endsWith("bmp")) {
            return "image/bmp";
        }
        return "image/jpeg";
    }

    /**
     * 获取video的mine_type
     *
     * @param path
     * @return
     */
    private static String getVideoMimeType(String path) {
        String lowerPath = path.toLowerCase();
        if (lowerPath.endsWith("mp4") || lowerPath.endsWith("mpeg4")) {
            return "video/mp4";
        } else if (lowerPath.endsWith("3gp")) {
            return "video/3gp";
        } else if (lowerPath.endsWith("mpeg") || lowerPath.endsWith("mpga")) {
            return "video/mpeg";
        } else if (lowerPath.endsWith("avi")) {
            return "video/x-msvideo";
        } else if (lowerPath.endsWith("asf")) {
            return "video/x-ms-asf";
        } else if (lowerPath.endsWith("mov")) {
            return "video/quicktime";
        } else if (lowerPath.endsWith("m4v")) {
            return "video/x-m4v";
        } else if (lowerPath.endsWith("m4u")) {
            return "video/vnd.mpegurl";
        }
        return "video/mp4";
    }

    /**
     * 获得转化后的时间
     *
     * @param time
     * @return
     */
    private static long getTimeWrap(long time) {
        if (time <= 0) {
            return System.currentTimeMillis();
        }
        return time;
    }

    /**
     * 检测文件存在
     *
     * @param filePath
     * @return
     */
    private static boolean checkFile(String filePath) {
        boolean result = false;
        File mFile = new File(filePath);
        if (mFile.exists()) {
            result = true;
        } else {
            Logger.e(TAG, "文件不存在 path = " + filePath);
        }
        return result;
    }
}
