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

package xyz.eulix.space.util.share;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.core.content.FileProvider;

import java.io.File;

import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.Logger;

/**
 * Author:      Zhu Fuyu
 * Description: 分享工具类
 * History:     2021/8/25
 */
public class ShareUtil {

    private ShareUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 分享文件
     *
     * @param context
     * @param filePath 文件绝对路径
     */
    public static void shareFile(Context context, String filePath) {
        if (context == null || TextUtils.isEmpty(filePath)) {
            Logger.d("shareFile context is null or filePath is empty.");
            return;
        }

        File file = new File(filePath);
        if (file.exists()) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_SEND);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addCategory("android.intent.category.DEFAULT");

            Uri fileUri = getFileUri(context, file);

            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            // 授予目录临时共享权限
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            int typeIndex = filePath.lastIndexOf(".");
            String suffix = filePath.substring(typeIndex + 1);
            String fileType = FileUtil.getMimeType(suffix);

            Logger.d("shareFile fileType " + fileType);
            Logger.d("shareFile uri: " + fileUri);

            intent.setDataAndType(fileUri, fileType);

            try {
                context.startActivity(Intent.createChooser(intent, file.getName()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Logger.d("file path error");
        }
    }

    //分享文本
    public static void shareStr(Context context, String text) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TEXT, text);
        context.startActivity(Intent.createChooser(intent, "链接"));
    }

    public static Uri getFileUri(Context context, File file) {
        Uri uri;
        // 低版本直接用 Uri.fromFile
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            uri = Uri.fromFile(file);
        } else {
            uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileProvider", file);

            int typeIndex = file.getAbsolutePath().lastIndexOf(".");
            String suffix = file.getAbsolutePath().substring(typeIndex + 1);
            String fileType = FileUtil.getMimeType(suffix);
            //  使用 FileProvider 会在某些 app 下不支持，对图片、视频、音频使用ConentUri
            if (uri != null && !TextUtils.isEmpty(uri.toString()) && !TextUtils.isEmpty(fileType)) {
                if (fileType.contains("video/")) {
                    uri = getVideoContentUri(context, file);
                } else if (fileType.contains("image/")) {
                    uri = getImageContentUri(context, file);
                } else if (fileType.contains("audio/")) {
                    uri = getAudioContentUri(context, file);
                }
            }
        }
        return uri;
    }

    /**
     * Gets the content:// URI from the given corresponding path to a file
     *
     * @param context
     * @param imageFile
     * @return content Uri
     */
    private static Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID}, MediaStore.Images.Media.DATA + "=? ",
                new String[]{filePath}, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }

    /**
     * Gets the content:// URI from the given corresponding path to a file
     *
     * @param context
     * @param videoFile
     * @return content Uri
     */
    private static Uri getVideoContentUri(Context context, File videoFile) {
        String filePath = videoFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Video.Media._ID}, MediaStore.Video.Media.DATA + "=? ",
                new String[]{filePath}, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/video/media");
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (videoFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Video.Media.DATA, filePath);
                return context.getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }

    /**
     * Gets the content:// URI from the given corresponding path to a file
     *
     * @param context
     * @param audioFile
     * @return content Uri
     */
    private static Uri getAudioContentUri(Context context, File audioFile) {
        String filePath = audioFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Audio.Media._ID}, MediaStore.Audio.Media.DATA + "=? ",
                new String[]{filePath}, null);
        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/audio/media");
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (audioFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Audio.Media.DATA, filePath);
                return context.getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }

}
