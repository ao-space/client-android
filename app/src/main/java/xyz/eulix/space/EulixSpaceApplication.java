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

package xyz.eulix.space;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshHeader;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.DefaultRefreshHeaderCreator;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.bean.LocalMediaUpItem;
import xyz.eulix.space.bean.LocaleBean;
import xyz.eulix.space.database.EulixSpaceSharePreferenceHelper;
import xyz.eulix.space.manager.EulixBiometricManager;
import xyz.eulix.space.manager.EulixPushManager;
import xyz.eulix.space.manager.LocalMediaCacheManager;
import xyz.eulix.space.network.NetworkCallbackImpl;
import xyz.eulix.space.receiver.NetworkChangeReceiver;
import xyz.eulix.space.receiver.PhotoAlbumContentObserver;
import xyz.eulix.space.ui.ConfirmDialogThemeActivity;
import xyz.eulix.space.ui.EulixMainActivity;
import xyz.eulix.space.ui.ScreenShotActivity;
import xyz.eulix.space.util.BaseParamsUtil;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.DebugUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.LocalMediaUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.NetUtils;
import xyz.eulix.space.util.PreferenceUtil;
import xyz.eulix.space.util.ScreenUtil;
import xyz.eulix.space.util.SystemUtil;
import xyz.eulix.space.util.ToastManager;
import xyz.eulix.space.util.watchdog.ANRException;
import xyz.eulix.space.view.smartrefresh.CustomRefreshHeader;

/**
 * @author: chenjiawei
 * date: 2021/6/16 17:24
 */
public class EulixSpaceApplication extends Application implements NetworkCallbackImpl.ConnectivityCallback, Thread.UncaughtExceptionHandler {
    private static final String TAG = EulixSpaceApplication.class.getSimpleName();
    private static EulixSpaceApplication mApplication;
    private static Context mContext;
    private static int foregroundActivityNumber = 0;
    private String mPkgName;
    private int mainActivityCreateNumber = 0;
    private static List<Activity> activityList = new ArrayList<>();
    private Activity lastResumeActivity;
    private Activity lastLocaleActivity;
    private NetworkChangeReceiver networkReceiver = new NetworkChangeReceiver();
    private PhotoAlbumContentObserver photoAlbumContentObserver = null;
    private boolean isAppForeground = false; //app是否在前台
    private Thread.UncaughtExceptionHandler systemExceptionHandler;

    private boolean needNoticeMobileDataDialog = false;
    private boolean isMobileDataDialogShowing = false;
    private static boolean isStartApplicationLock = false;
    private static boolean isStartForegroundLockEnable = false;
    private EulixPushManager eulixPushManager;

    private String generatePkgName() {
        if (mPkgName == null) {
            mPkgName = getPackageName();
        }
        return mPkgName;
    }

    private void init() {
        //updateLocale();
        //开启debug日志
        Log.d("eulix", "buildConfig switch:" + BuildConfig.LOG_SWITCH);
        Log.d("eulix", "logSwitch:" + PreferenceUtil.getLoggerSwitch(getContext()));
        Logger.setDebuggable(BuildConfig.LOG_SWITCH || PreferenceUtil.getLoggerSwitch(getContext()));
        // 初始化客户端参数
        BaseParamsUtil.initBaseParams(this);

        EulixBiometricManager eulixBiometricManager = EulixBiometricManager.getInstance();
        if (eulixBiometricManager != null) {
            if (!isStartApplicationLock) {
                isStartApplicationLock = true;
                eulixBiometricManager.setApplicationLockEventInfo(null);
            }
        }

        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
                if (activityList != null) {
                    try {
                        activityList.add(activity);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                ComponentName componentName = activity.getComponentName();
                if (componentName != null) {
                    String activityClassName = componentName.getClassName();
                    if (activityClassName.endsWith(EulixMainActivity.TAG)) {
                        mainActivityCreateNumber += 1;
                    }
                }
            }

            @Override
            public void onActivityStarted(@NonNull Activity activity) {
                if (foregroundActivityNumber <= 0) {
                    foregroundActivityNumber = 0;
                    startMainService(false);
                }
                foregroundActivityNumber += 1;
            }

            @Override
            public void onActivityResumed(@NonNull Activity activity) {
                lastResumeActivity = activity;
                ComponentName componentName = activity.getComponentName();
                String resumeClsName = componentName.getClassName();
                if (resumeClsName.startsWith(generatePkgName())) {
                    lastLocaleActivity = activity;
                }
            }

            @Override
            public void onActivityPaused(@NonNull Activity activity) {
                // Do nothing
            }

            @Override
            public void onActivityStopped(@NonNull Activity activity) {
                foregroundActivityNumber -= 1;
                if (foregroundActivityNumber <= 0) {
                    foregroundActivityNumber = 0;
                    startMainService(true);
                }
            }

            @Override
            public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
                // Do nothing
            }

            @Override
            public void onActivityDestroyed(@NonNull Activity activity) {
                ComponentName componentName = activity.getComponentName();
                if (componentName != null) {
                    String activityClassName = componentName.getClassName();
                    if (activityClassName.endsWith(EulixMainActivity.TAG)) {
                        mainActivityCreateNumber = Math.max((mainActivityCreateNumber - 1), 0);
                    }
                }
                if (activityList != null) {
                    boolean isRemove = false;
                    try {
                        isRemove = activityList.remove(activity);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (!isRemove && activityList.contains(activity)) {
                        Iterator<Activity> activityIterator = activityList.iterator();
                        while (activityIterator.hasNext()) {
                            Activity oldActivity = activityIterator.next();
                            if (oldActivity == activity) {
                                activityIterator.remove();
                            }
                        }
                    }
                }
            }
        });

        AbsPresenter.setAbsContext(this);

        SmartRefreshLayout.setDefaultRefreshHeaderCreator(new DefaultRefreshHeaderCreator() {
            @NonNull
            @NotNull
            @Override
            public RefreshHeader createRefreshHeader(@NonNull @NotNull Context context, @NonNull @NotNull RefreshLayout layout) {
                return new CustomRefreshHeader(context);
            }
        });

        registerContentObserver();
    }

    /**
     * 启动主服务
     *
     * @param isFore 高版本前台服务（true）、后台服务（false）
     */
    private void startMainService(boolean isFore) {
        if (isFore) {
            DataUtil.setBackgroundRunningTimestamp(this, System.currentTimeMillis());
            isStartForegroundLockEnable = true;
            List<String> requestIds = DataUtil.getApplicationLockRunningRequestId();
            if (requestIds != null) {
                EulixBiometricManager eulixBiometricManager = EulixBiometricManager.getInstance();
                if (eulixBiometricManager != null && !requestIds.isEmpty()) {
                    for (String requestId : requestIds) {
                        if (requestId != null) {
                            eulixBiometricManager.cancelAuthenticate(requestId, true);
                        }
                    }
                }
            }
        } else if (isStartForegroundLockEnable) {
            long backgroundRunningTimestamp = DataUtil.getBackgroundRunningTimestamp(this);
            if (backgroundRunningTimestamp >= 0 && (backgroundRunningTimestamp + 5 * ConstantField.TimeUnit.MINUTE_UNIT) < System.currentTimeMillis()) {
                EulixBiometricManager eulixBiometricManager = EulixBiometricManager.getInstance();
                if (eulixBiometricManager != null) {
                    eulixBiometricManager.setApplicationLockEventInfo(null);
                }
            }
        }
        if (!isFore || mainActivityCreateNumber > 0) {
            Intent intent = new Intent(this, EulixSpaceService.class);
            if (isFore && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                intent.putExtra(ConstantField.EXTRA.FOREGROUND, true);
                try {
                    startForegroundService(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                intent.putExtra(ConstantField.EXTRA.FOREGROUND, false);
                intent.setAction(ConstantField.Action.TOKEN_ACTION);
                try {
                    startService(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        isAppForeground = !isFore;

        if (needNoticeMobileDataDialog && NetUtils.isMobileNetWork(mContext)) {
            needNoticeMobileDataDialog = false;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                showTransferMobileDataDialog();
            }, 500);
        }
    }

    /**
     * 更改此语言会导致默认语言失效
     */
    @Deprecated
    public static void updateLocale() {
        LocaleBean localeBean = null;
        String localeValue = DataUtil.getApplicationLocale(mContext);
        if (localeValue != null) {
            try {
                localeBean = new Gson().fromJson(localeValue, LocaleBean.class);
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
        if (localeBean != null) {
            Locale locale = localeBean.parseLocale();
            Resources resources = mContext.getResources();
            if (resources != null && locale != null) {
                Configuration configuration = resources.getConfiguration();
                if (configuration != null) {
                    configuration.setLocale(locale);
                    //createConfigurationContext(configuration);
                    resources.updateConfiguration(configuration, resources.getDisplayMetrics());
                }
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        String processName = getProcessName(this);
        if (processName != null && processName.equals(BuildConfig.APPLICATION_ID)) {
            //初始化项目默认的进程
            mApplication = this;
            mContext = this;
            generatePkgName();

            EulixSpaceSharePreferenceHelper eulixSpaceSharePreferenceHelper = EulixSpaceSharePreferenceHelper.getInstance(this);
            init();
            int environmentIndex = DebugUtil.getEnvironmentIndex();
            if (eulixSpaceSharePreferenceHelper != null && !eulixSpaceSharePreferenceHelper.containsKey(ConstantField.EulixSpaceSPKey.BOX_ENVIRONMENT)) {
                eulixSpaceSharePreferenceHelper.setInt(ConstantField.EulixSpaceSPKey.BOX_ENVIRONMENT, environmentIndex, false);
            }
            eulixPushManager = EulixPushManager.getInstance();
            eulixPushManager.prepareRefreshNotificationOffline();

            registerBroadcast();


            systemExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(this);
        }

    }

    private String getProcessName(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
        if (runningApps == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo proInfo : runningApps) {
            if (proInfo.pid == android.os.Process.myPid()) {
                if (proInfo.processName != null) {
                    return proInfo.processName;
                }
            }
        }
        return null;
    }

    private void registerBroadcast() {
        IntentFilter networkChangeFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, networkChangeFilter);
    }

    private void registerContentObserver() {
        //注册图片数据变化监听
        if (photoAlbumContentObserver == null) {
            photoAlbumContentObserver = new PhotoAlbumContentObserver(new Handler(Looper.getMainLooper()));
            ContentResolver photoAlbumContentResolver = getContentResolver();

            photoAlbumContentObserver.setOnChangeListener(uri -> {
                if (!SystemUtil.checkPermission(getContext(), ConstantField.Permission.WRITE_EXTERNAL_STORAGE)) {
                    Logger.d("zfy", "permission not allowed");
                    return;
                }
                String mediaId = uri.toString().substring(uri.toString().lastIndexOf("/") + 1);
                Logger.d("mediaId=" + mediaId);
                LocalMediaUpItem localMediaUpItem = null;
                File mediaFile = null;
                if (!uri.toString().endsWith("media")) {
                    localMediaUpItem = LocalMediaUtil.getMediaByUri(uri, photoAlbumContentResolver);
                    if (localMediaUpItem != null) {
                        mediaFile = new File(localMediaUpItem.getMediaPath());
                    }
                }
                if (mediaFile == null || !mediaFile.exists()) {
                    Logger.d("zfy", "delete a media");
                    try {
                        //检查是否为数字id
                        int mediaIdInt = Integer.parseInt(mediaId);
                        mediaId = String.valueOf(mediaIdInt);
                    } catch (Exception e) {
                        Logger.e(e.getMessage());
                        mediaId = "";
                    }
                    if (uri.toString().contains("images")) {
                        LocalMediaCacheManager.onGalleryDelete(true, mediaId);
                    } else {
                        LocalMediaCacheManager.onGalleryDelete(false, mediaId);
                    }
                    return;
                } else {
                    LocalMediaCacheManager.onGalleryAdd(localMediaUpItem, uri);
                }

                String filePath = FileUtil.getFilepath(photoAlbumContentResolver, uri);
                if (filePath != null) {
                    Logger.d(TAG, "file path on change: " + filePath);
                    String currentScreenShotPath = DataUtil.getScreenShotPath();
                    if (foregroundActivityNumber > 0 && ScreenUtil.checkScreenShot(filePath) && FileUtil.existFile(filePath)
                            && (currentScreenShotPath == null || !currentScreenShotPath.equals(filePath))
                            && (System.currentTimeMillis() - FileUtil.getFileModifiedTimestamp(filePath) < 20000)) {
                        Intent intent = new Intent(this, ScreenShotActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra(ConstantField.SCREEN_SHOT_PATH, filePath);
                        startActivity(intent);
                    }
                }
            });

            Uri photoUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            Uri videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            photoAlbumContentResolver.registerContentObserver(photoUri, true, photoAlbumContentObserver);
            photoAlbumContentResolver.registerContentObserver(videoUri, true, photoAlbumContentObserver);
        }
    }

    /**
     * 结束掉所有和参数不一样的活动
     *
     * @param currentActivity 保留的活动，如果为null则全部结束
     */
    public synchronized static void popAllOldActivity(Activity currentActivity) {
        if (activityList != null) {
            Iterator<Activity> activityIterator = activityList.iterator();
            while (activityIterator.hasNext()) {
                Activity activity = activityIterator.next();
                if (activity != null) {
                    if (currentActivity != null) {
                        if (activity != currentActivity) {
                            activityIterator.remove();
                            if (!activity.isFinishing()) {
                                activity.finish();
                                Logger.d(TAG, "activity: " + activity.getComponentName().getClassName() + " instance: " + activity.toString() + " will finish");
                            } else {
                                Logger.d(TAG, "activity: " + activity.getComponentName().getClassName() + " instance: " + activity.toString() + " has finished");
                            }
                        } else {
                            Logger.d(TAG, "activity: " + activity.getComponentName().getClassName() + " instance: " + activity.toString() + " is current activity");
                        }
                    } else {
                        activity.finish();
                    }
                }
            }
        }
    }

    /**
     * 返回可见页面数量
     *
     * @return
     */
    public static int getForegroundActivityNumber() {
        return foregroundActivityNumber;
    }

    public static Context getContext() {
        return mContext;
    }

    public static Context getResumeActivityContext() {
        Context context = mContext;
        if (mApplication != null) {
            Activity activity = mApplication.lastLocaleActivity;
            if (activity == null) {
                activity = mApplication.lastResumeActivity;
            }
            if (activity != null) {
                context = activity;
            }
        }
        return context;
    }

    @Override
    public void onConnect(boolean isConnected) {

    }

    @Override
    public void uncaughtException(@NonNull @NotNull Thread t, @NonNull @NotNull Throwable e) {
        Logger.e(TAG, "uncaught exception: " + e.getMessage());
        if (e instanceof ANRException) {
            Logger.d("zfy", "uncaughtException");
            e.printStackTrace();
        }
        //系统默认异常处理器
        if (systemExceptionHandler != null) {
            systemExceptionHandler.uncaughtException(t, e);
        }
    }

    public void showNetworkDisconnect() {
        if (!NetUtils.isNetAvailable(mContext) && isAppForeground) {
            new ToastManager(mContext).showImageTextToast(R.drawable.toast_refuse, getResumeActivityContext().getString(R.string.net_work_disconnect));
        }
    }

    //应用是否在前台
    public boolean getIsAppForeground() {
        return isAppForeground;
    }

    public static List<Activity> getActivityList() {
        return activityList;
    }

    public void confirmTransferMobileData() {
        if (isAppForeground) {
            Logger.d("mobileData", "show mobile dialog");
            showTransferMobileDataDialog();
        } else {
            needNoticeMobileDataDialog = true;
        }
    }

    private void showTransferMobileDataDialog() {
        Logger.d("mobileData", "showTransferMobileDataDialog");
        ConfirmDialogThemeActivity.start(mContext);
    }

}
