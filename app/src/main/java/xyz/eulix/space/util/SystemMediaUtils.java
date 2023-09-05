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

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

import androidx.core.content.FileProvider;

import java.io.File;

/**
 * Author:      Zhu Fuyu
 * Description: 调用系统功能打开媒体文件
 * History:     2021/8/30
 */
public class SystemMediaUtils {

    public static void openMediaFile(Context context, String absolutePath) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        File file = new File(absolutePath);
        if (!file.exists()) {
            return;
        }
        Uri uri;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileProvider", file);
        } else {
            uri = Uri.fromFile(file);
        }
        int typeIndex = absolutePath.lastIndexOf(".");
        String suffix = absolutePath.substring(typeIndex + 1);
        String fileType = FileUtil.getMimeType(suffix);

        Logger.d("fileType " + fileType);
        Logger.d("uri: " + uri);

        intent.putExtra(Intent.EXTRA_STREAM, uri);
        // 授予目录临时共享权限
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_ACTIVITY_NEW_TASK);

        intent.setDataAndType(uri, fileType);
        context.startActivity(intent);
    }
}
