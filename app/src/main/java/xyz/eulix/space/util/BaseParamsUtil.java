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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.net.URLEncoder;

/**
 * Author:      Zhu Fuyu
 * Description: 手机基本信息工具类
 * History:     2021/9/6
 */
public class BaseParamsUtil {
    //渠道号
    public static String sChannelCode = "00000000";
    // 手机型号
    public static String sClientUa = "";
    // 系统版本号
    public static String sApiLevel = "";
    // 当前CPU架构
    public static String sCpuAbi = "";
    // 屏幕分辨率
    public static String sScreenPx = "";
    //设备型号
    public static String sModel = "";
    //终端品牌
    public static String sBrand = "";
    // 应用包名
    public static String sAppPackageName = "";
    // 应用名称
    public static String sAppName = "";
    // apk包md5值
    public static String sApkMd5 = "";
    //AndroidId 设备首次启动后系统会随机生成一个64位的数字，用16进制字符串的形式表示
    public static String sAndroidId = "";

    private BaseParamsUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * 初始化基础数据
     *
     * @param context
     */
    public static void initBaseParams(Context context) {
        // 初始化基本参数
        try {

            sClientUa = URLEncoder.encode(Build.MODEL);
            sApiLevel = Build.VERSION.SDK_INT + "";
            sCpuAbi = Build.CPU_ABI;
            Logger.d("clientUa=" + sClientUa
                    + ",apiLevel=" + sApiLevel + ",cpuAbi=" + sCpuAbi);
            DisplayMetrics dm = new DisplayMetrics();
            WindowManager windowManager = (WindowManager) context
                    .getSystemService(Context.WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getMetrics(dm);
            sScreenPx = dm.widthPixels + "*" + dm.heightPixels;
        } catch (Exception e) {
            Logger.d("BaseParamsUtils initBaseParams 1 exception :");
            Logger.d(e.getMessage());
        }

        try {
            sModel = Build.MODEL;
            Logger.d("sModel=" + sModel);
            sBrand = Build.BRAND;
            Logger.d("sBrand=" + sBrand);

            sAndroidId = getAndroidId(context);
            Logger.d("sAndroidId=" + sAndroidId);

            PackageInfo pi = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            sAppPackageName = context.getPackageName();
            Logger.d("sAppPackageName=" + sAppPackageName);
            sAppName = pi.applicationInfo
                    .loadLabel(context.getPackageManager()).toString();
            Logger.d("sAppName=" + sAppName);
            final String apkFile = context.getApplicationInfo().publicSourceDir;
            Logger.d("apkFile=" + apkFile);

            ApplicationInfo applicationInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            if (applicationInfo.metaData != null) {
                Object channelCodeTmp = applicationInfo.metaData.get("eulix_channel_id");
                if (channelCodeTmp != null) {
                    sChannelCode = channelCodeTmp.toString();
                }
            }
            Logger.d("current channel code:" + sChannelCode);

            new Thread(() -> {
                try {
                    sApkMd5 = MD5Util.getFileMD5String(apkFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (Exception e) {
            Logger.d("BaseParamsUtils initBaseParams 2 exception :");
            Logger.d(e.getMessage());
        }

    }

    /**
     * 获取AndroidId
     *
     * @param context
     * @return
     */
    private static String getAndroidId(Context context) {
        if (context == null) {
            return "";
        }

        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        Logger.d("androidId: " + androidId);
        return (TextUtils.isEmpty(androidId) ? "" : androidId);
    }

}
