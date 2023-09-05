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
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import xyz.eulix.space.EulixSpaceApplication;

/**
 * @author: chenjiawei
 * date: 2021/6/2 14:34
 */
public class SystemUtil {
    private static volatile int notificationId = ConstantField.RequestCode.EULIX_SPACE_NOTIFICATION_START_ID;

    private SystemUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    public static boolean isApplicationLaunch(String exceptTag) {
        boolean isLaunch = false;
        List<Activity> activityList = EulixSpaceApplication.getActivityList();
        if (activityList != null) {
            Iterator<Activity> activityIterator = activityList.iterator();
            while (activityIterator.hasNext()) {
                Activity activity = activityIterator.next();
                if (activity != null) {
                    ComponentName componentName = activity.getComponentName();
                    if (componentName != null) {
                        String pkgName = componentName.getPackageName();
                        String clsName = componentName.getClassName();
                        if ((ConstantField.PACKAGE_NAME.equals(pkgName) && clsName.startsWith(pkgName)
                                && (exceptTag == null || !clsName.endsWith(exceptTag)))
                                || ConstantField.OFFICE_PACKAGE_NAME.equals(pkgName)
                                || ConstantField.ZXING_PACKAGE_NAME.equals(pkgName)) {
                            isLaunch = true;
                            break;
                        }
                    }
                }
            }
        }
        return isLaunch;
    }

    public static boolean checkPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean requestPermission(Activity activity, String[] permissions, int requestCode) {
        boolean isPermit = (activity != null);
        if (isPermit && permissions != null && permissions.length > 0) {
            List<String> requestPermissions = new ArrayList<>();
            for (String permission : permissions) {
                if (permission != null && ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions.add(permission);
                    PreferenceUtil.saveBaseKeyBoolean(activity, permission, false);
                } else {
                    PreferenceUtil.saveBaseKeyBoolean(activity, permission, true);
                }
            }
            int size = requestPermissions.size();
            if (size > 0) {
                isPermit = false;
                ActivityCompat.requestPermissions(activity, requestPermissions.toArray(new String[size]), requestCode);
            }
        }
        return isPermit;
    }

    public static boolean requestNotification(Context context, boolean isExecute) {
        boolean isHandle = false;
        if (context != null) {
            NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(context);
            isHandle = notificationManagerCompat.areNotificationsEnabled();
            if (isExecute) {
                Intent intent = new Intent();
                intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("app_package", context.getPackageName());
                intent.putExtra("app_uid", context.getApplicationInfo().uid);
                intent.putExtra("android.provider.extra.APP_PACKAGE", context.getPackageName());
                if (!(context instanceof Activity)) {
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }
                context.startActivity(intent);
            }
        }
        return isHandle;
    }

    public static void goToApplicationDetailsSettings(@NonNull Activity activity) {
        Uri uri = Uri.parse("package:" + activity.getPackageName());
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri);
        activity.startActivity(intent);
    }

    public static boolean requestInstallPackages(@NonNull Activity activity, boolean isAuto) {
        boolean canRequestPackageInstalls = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PackageManager manager = activity.getPackageManager();
            if (manager == null) {
                canRequestPackageInstalls = false;
            } else {
                canRequestPackageInstalls = manager.canRequestPackageInstalls();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            canRequestPackageInstalls = (ContextCompat.checkSelfPermission(activity, ConstantField.Permission_23.REQUEST_INSTALL_PACKAGES) == PackageManager.PERMISSION_GRANTED);
        }
        if (!canRequestPackageInstalls && isAuto) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                Uri uri = Uri.parse("package:" + activity.getPackageName());
                Intent intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, uri);
                activity.startActivityForResult(intent, ConstantField.RequestCode.REQUEST_INSTALL_PACKAGES);
            }
        }
        return canRequestPackageInstalls;
    }

    public static void installPackage(Context context, String filePath) {
        if (context != null && filePath != null) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            File file = new File(filePath);
            if (file.exists()) {
                Uri apkUri;
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileProvider", file);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                } else {
                    apkUri = Uri.fromFile(file);
                }
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            }
            context.startActivity(intent);
        }
    }

    public synchronized static int getNotificationId() {
        int id = notificationId;
        notificationId = notificationId + 1;
        return id;
    }

    public static String getAppName(Context context) {
        String appName = null;
        if (context != null) {
            PackageManager manager = context.getPackageManager();
            if (manager != null) {
                PackageInfo info = null;
                try {
                    info = manager.getPackageInfo(context.getPackageName(), 0);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                if (info != null) {
                    ApplicationInfo applicationInfo = info.applicationInfo;
                    if (applicationInfo != null) {
                        try {
                            appName = context.getResources().getString(applicationInfo.labelRes);
                        } catch (Resources.NotFoundException e) {
                            e.printStackTrace();
                        } catch (Exception ee) {
                            ee.printStackTrace();
                        }
                    }
                }
            }
        }
        return appName;
    }

    public static int getVersionCode(Context context) {
        int versionCode = 0;
        if (context != null) {
            PackageManager manager = context.getPackageManager();
            if (manager != null) {
                PackageInfo info = null;
                try {
                    info = manager.getPackageInfo(context.getPackageName(), 0);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                if (info != null) {
                    versionCode = info.versionCode;
                }
            }
        }
        return versionCode;
    }

    public static String getVersionName(Context context) {
        String versionName = null;
        if (context != null) {
            PackageManager manager = context.getPackageManager();
            if (manager != null) {
                PackageInfo info = null;
                try {
                    info = manager.getPackageInfo(context.getPackageName(), 0);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                if (info != null) {
                    versionName = info.versionName;
                }
            }
        }
        return versionName;
    }

    public static int getBiometricFeatureStatus(Context context) {
        int biometricFeatureStatus = 0;
        if (context != null) {
            PackageManager manager = context.getPackageManager();
            if (manager != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    if (manager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
                        biometricFeatureStatus += 1;
                    }
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && manager.hasSystemFeature(PackageManager.FEATURE_FACE)) {
                        biometricFeatureStatus += 2;
                    }
                }
            }
            // 暂时全都归系统处理
            if (biometricFeatureStatus == 3) {
                biometricFeatureStatus = 4;
            }
        }
        return biometricFeatureStatus;
    }

    public static boolean apkUpdate(String newVersion, String currentVersion) {
        boolean isApkUpdate = false;
        int updateState = 0;
        if (newVersion != null && currentVersion != null && newVersion.contains(".") && currentVersion.contains(".")) {
            String[] newVersionArray = newVersion.split("\\.");
            String[] currentVersionArray = currentVersion.split("\\.");
            int newVersionLength = newVersionArray.length;
            int currentVersionLength = currentVersionArray.length;
            for (int i = 0; i < Math.min(newVersionLength, currentVersionLength); i++) {
                String newVersionElementValue = newVersionArray[i];
                String currentVersionElementValue = currentVersionArray[i];
                if (newVersionElementValue != null && currentVersionElementValue != null) {
                    String[] newVersionElementValueArray = newVersionElementValue.split("-");
                    String[] currentVersionElementValueArray = currentVersionElementValue.split("-");
                    int newVersionElementLength = newVersionElementValueArray.length;
                    int currentVersionElementLength = currentVersionElementValueArray.length;
                    for (int j = 0; j < Math.min(newVersionElementLength, currentVersionElementLength); j++) {
                        Integer newVersionElement = null;
                        Integer currentVersionElement = null;
                        try {
                            newVersionElement = Integer.parseInt(newVersionElementValueArray[j]);
                            currentVersionElement = Integer.parseInt(currentVersionElementValueArray[j]);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                        if (newVersionElement != null && currentVersionElement != null) {
                            updateState = newVersionElement - currentVersionElement;
                        }
                        if (updateState != 0) {
                            break;
                        }
                    }
                    if (updateState == 0) {
                        updateState = newVersionElementLength - currentVersionElementLength;
                    }
                    if (updateState != 0) {
                        break;
                    }
                }
            }
            if (updateState == 0) {
                updateState = newVersionLength - currentVersionLength;
            }
            isApkUpdate = (updateState > 0);
        }
        return isApkUpdate;
    }

    public static String getPhoneModel() {
        return (Build.BRAND + "_" + Build.MODEL);
    }
}
