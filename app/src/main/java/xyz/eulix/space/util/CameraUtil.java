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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import xyz.eulix.space.R;
import xyz.eulix.space.ui.mine.GalleryPictureSelectActivity;

/**
 * Author:      Zhu Fuyu
 * Description: 图片选择工具
 * History:     2021/9/14
 */
public class CameraUtil {
    private final static String PHOTO_PATH ="/pic";
    private final static String CAPTURE_IMAGE_NAME = "capture";

    public static final int REQUEST_GALLERY_CODE = 2;
    public static final int REQUEST_CAMERA_CODE = 3;
    public static final int REQUEST_CLIP_CODE = 4;

    private static Uri cameraUri;
    private static String tmpCameraPath = null;

    /**
     * 打开相机
     */
    public static void openCamera(Activity context) {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        String ss = df.format(new Date()) + "_";

        File dir = new File(context.getExternalCacheDir().getAbsolutePath(), PHOTO_PATH);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = ss + CAPTURE_IMAGE_NAME + ".jpg";
        File out = new File(dir, fileName);
        tmpCameraPath = out.getAbsolutePath();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cameraUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileProvider", out);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            cameraUri = Uri.fromFile(out);
        }

        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);

        try {
            context.startActivityForResult(intent, REQUEST_CAMERA_CODE);
        }catch (SecurityException e){
            e.printStackTrace();
            ToastUtil.showToast(context.getResources().getString(R.string.open_camera_permission_hint));
        }

    }

    /**
     * 获取拍照图片path
     */
    public static String getCameraBack() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return tmpCameraPath;
        } else {
            return cameraUri.getEncodedPath();
        }
    }

    /**
     * 打开本地相册
     */
    public static void openGallery(Activity context) {
        Intent intent = new Intent(context, GalleryPictureSelectActivity.class);
        context.startActivityForResult(intent, CameraUtil.REQUEST_GALLERY_CODE);
    }

}
