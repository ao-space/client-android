package com.google.zxing.client.android.util;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

public class ViewUtils {
    public static int dp2px(Context context, int dpValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }


    public static int px2dp(Context context, float pxValue) {
        float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static float getTextSize2dp(Context context, int dimension) {
        int textSizePix = context.getResources().getDimensionPixelSize(dimension);
        return (float) px2dp(context, (float) textSizePix);
    }

    //获取屏幕的宽度px
    public static int getScreenWidth(Context context) {
        WindowManager windowManager = (WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();// 创建了一张白纸
        windowManager.getDefaultDisplay().getRealMetrics(outMetrics);// 给白纸设置宽高
        return outMetrics.widthPixels;
    }

    //获取屏幕的高度px
    public static int getScreenHeight(Context context) {
        WindowManager windowManager = (WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();// 创建了一张白纸
        windowManager.getDefaultDisplay().getRealMetrics(outMetrics);// 给白纸设置宽高
        return outMetrics.heightPixels;
    }

    //获取状态栏高度
    public static int getStatusBarHeight(Context context) {
        int statusBarHeight = 0;
        try {
            int statusBarResourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
            if (statusBarResourceId > 0) {
                statusBarHeight = context.getResources().getDimensionPixelSize(statusBarResourceId);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return statusBarHeight;
    }

    //获取虚拟导航栏高度
    public static int getNavigationBarHeight(Activity activity) {
        int navigationBarHeight = 0;
        if (isNavigationBarExist(activity)) {
            try {
                int navigationBarResourceId = activity.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
                if (navigationBarResourceId > 0) {
                    navigationBarHeight = activity.getResources().getDimensionPixelSize(navigationBarResourceId);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return navigationBarHeight;
    }

    //检测底部虚拟导航栏是否存在
    public static boolean isNavigationBarExist(Activity activity) {
        ViewGroup vp = null;
        if (activity != null) {
            Window window = activity.getWindow();
            if (window != null) {
                View decorView = window.getDecorView();
                vp = (ViewGroup) decorView;
            }
        }
        if (vp != null) {
            for (int i = 0; i < vp.getChildCount(); i++) {
                vp.getChildAt(i).getContext().getPackageName();
                if (vp.getChildAt(i).getId() != View.NO_ID
                        && "navigationBarBackground".equals(activity.getResources().getResourceEntryName(vp.getChildAt(i).getId()))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 测量TextView宽高
     * @param textView
     * @return
     */
    public static int[] measureTextView(TextView textView) {
        int[] sizeArray = null;
        if (textView != null) {
            sizeArray = new int[2];
            int measureSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
            textView.measure(measureSpec, measureSpec);
            sizeArray[0] = textView.getMeasuredWidth();
            sizeArray[1] = textView.getMeasuredHeight();
        }
        return sizeArray;
    }
}
