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

/**
 * Author:      Zhu Fuyu
 * Description: 错误码工具类
 * History:     2022/3/7
 */
public class FailCodeUtil {
    //上传本地源文件被删除
    public static final int ERROR_UPLOAD_LOCAL_SOURCE_DELETE = 10001;
    //下载-服务端文件被删除
    public static final int ERROR_DOWNLOAD_REMOTE_SOURCE_DELETE = 20001;
    //外置存储设备格式不兼容
    public static final int ERROR_EXTERNAL_STORAGE_FORMAT_WRONG = 1053;

    public static String getMessageByCode(int code) {
        String message = "";
        switch (code) {
            case ERROR_UPLOAD_LOCAL_SOURCE_DELETE:
            case ERROR_DOWNLOAD_REMOTE_SOURCE_DELETE:
                message = "原文件已丢失";
                break;
            default:
        }
        return message;
    }
}
