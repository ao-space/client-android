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
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.R;

/**
 * Author:      Zhu Fuyu
 * Description: 屏幕视图相关工具类
 * History:     2021/7/19
 */
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
        WindowManager windowManager = (WindowManager) EulixSpaceApplication.getContext().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();// 创建了一张白纸
        windowManager.getDefaultDisplay().getRealMetrics(outMetrics);// 给白纸设置宽高
        return outMetrics.widthPixels;
    }

    //获取屏幕的高度px
    public static int getScreenHeight(Context context) {
        WindowManager windowManager = (WindowManager) EulixSpaceApplication.getContext().getSystemService(Context.WINDOW_SERVICE);
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

    public static boolean hasAnim(View view) {
        boolean hasAnimation = false;
        if (view != null) {
            hasAnimation = (view.getAnimation() != null);
        }
        return hasAnimation;
    }

    //设置loading动画
    public static void setLoadingAnim(Context context, ImageView image) {
        if (context == null || image == null) {
            return;
        }
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.anim_loading_rotate);
        LinearInterpolator interPolator = new LinearInterpolator();
        animation.setInterpolator(interPolator);
        image.setAnimation(animation);
    }

    //清除动画
    public static void clearAnim(View view){
        if (view != null && view.getAnimation() != null) {
            view.clearAnimation();
        }
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
