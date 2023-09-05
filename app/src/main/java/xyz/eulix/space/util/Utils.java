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
import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.view.inputmethod.InputMethodManager;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.util.Locale;

import xyz.eulix.space.bean.LocaleBean;

/**
 * Author:      Zhu Fuyu
 * Description: 基础工具类
 * History:     2021/7/26
 */
public class Utils {

    public static String timeLong2Str(long time) {
        if (time <= 0) {
            return "00:00";
        }
        Long leftTime = time;

        StringBuilder timeSb = new StringBuilder();
        long hour = leftTime / (1000 * 60 * 60);
        if (hour > 0) {
            if (hour < 10) {
                timeSb.append("0");
            }
            timeSb.append(hour + ":");
            leftTime -= hour * (1000 * 60 * 60);
        }
        long minute = leftTime / (1000 * 60);
        if (minute > 9) {
            timeSb.append(minute + ":");
            leftTime -= minute * (1000 * 60);
        } else {
            timeSb.append("0" + minute + ":");
            leftTime -= minute * (1000 * 60);
        }
        long secend = leftTime / (1000);
        if (secend > 9) {
            timeSb.append(secend);
        } else {
            timeSb.append("0" + secend);
        }
        return timeSb.toString();
    }

    //获取本地磁盘大小
    public static long getLocalStorageTotalSize() {
        long size = 0;
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long blockCount = stat.getBlockCount();
        size = blockSize * blockCount;
        return size;
    }

    //获取可用磁盘大小
    public static long getLocalStorageAvailableSize() {
        long size = 0;
        //内部可用空间
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSize();
        long availableBlocks = stat.getAvailableBlocks();
        size = blockSize * availableBlocks;
        return size;
    }

    //获取已使用磁盘大小
    public static long getLocalStorageUsedSize() {
        return getLocalStorageTotalSize() - getLocalStorageAvailableSize();
    }

    /**
     * 收起软键盘
     */
    public static void forceHideSoftInput(Activity activity) {
        if (activity == null) {
            return;
        }
        try {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
        } catch (Exception e) {
            Logger.e("zfy", e.getMessage());
        }
    }

    //系统语言是否为中文
    public static boolean isChineseLanguage(Context context) {
        LocaleBean localeBean = null;
        String localeValue = DataUtil.getApplicationLocale(context);
        if (localeValue != null) {
            try {
                localeBean = new Gson().fromJson(localeValue, LocaleBean.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
        if (localeBean != null){
            return Locale.CHINESE.getLanguage().equals(localeBean.getLanguage());
        } else {
            return FormatUtil.isChinese(context, false);
        }
    }

    //获取本地视频文件时长
    public static long getLocalVideoDuration(String filePath) {
        long duration = 0;
        try (MediaMetadataRetriever retriever = new MediaMetadataRetriever()) {
            retriever.setDataSource(filePath);
            duration = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            retriever.release();

        } catch (Exception e) {
            Logger.d(e.getMessage());
        }
        return duration;
    }


    //检查当前版本是否达到目标版本
    public static boolean checkVersionAvailable(String currentVersion, String targetVersionName) {
        if (TextUtils.isEmpty(currentVersion) || TextUtils.isEmpty(targetVersionName)) {
            return false;
        }

        String[] currentArr = getVersionArray(currentVersion);
        String[] targetArr = getVersionArray(targetVersionName);

        if (currentArr == null && targetArr == null) {
            return false;
        }

        assert currentArr != null;
        assert targetArr != null;
        if (currentArr.length < targetArr.length) {
            return false;
        } else if (currentArr.length > targetArr.length) {
            return true;
        } else {
            return compareVersionIndex(currentArr, targetArr, 0);
        }
    }

    private static boolean compareVersionIndex(String[] currentArray, String[] targetArray, int index) {
        if (currentArray.length > index) {
            int currentValue = Integer.parseInt(currentArray[index]);
            int targetValue = Integer.parseInt(targetArray[index]);
            if (currentValue > targetValue) {
                return true;
            } else if (currentValue < targetValue) {
                return false;
            } else {
                if (index == currentArray.length - 1) {
                    return true;
                } else {
                    return compareVersionIndex(currentArray, targetArray, index + 1);
                }
            }
        } else {
            return false;
        }
    }

    private static String[] getVersionArray(String versionName) {
        if (TextUtils.isEmpty(versionName)) {
            return null;
        }
        if (versionName.contains("-")) {
            versionName = versionName.substring(0, versionName.indexOf("-"));
        }
        return versionName.split("\\.");
    }
}
