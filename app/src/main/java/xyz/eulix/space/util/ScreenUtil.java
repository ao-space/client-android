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
import android.graphics.Bitmap;
import android.view.View;
import android.view.Window;

/**
 * @author: chenjiawei
 * date: 2021/6/1 14:52
 */
public class ScreenUtil {
    private static int screenWidth;
    private static int screenHeight;
    private static int statusBarHeight;
    private static int navigationBarHeight;
    private static float aug = 1.0f;

    private ScreenUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    public static int getScreenWidth() {
        return screenWidth;
    }

    public static void setScreenWidth(int screenWidth) {
        ScreenUtil.screenWidth = screenWidth;
    }

    public static int getScreenHeight() {
        return screenHeight;
    }

    public static void setScreenHeight(int screenHeight) {
        ScreenUtil.screenHeight = screenHeight;
    }

    public static int getStatusBarHeight() {
        return statusBarHeight;
    }

    public static void setStatusBarHeight(int statusBarHeight) {
        ScreenUtil.statusBarHeight = statusBarHeight;
    }

    public static int getNavigationBarHeight() {
        return navigationBarHeight;
    }

    public static void setNavigationBarHeight(int navigationBarHeight) {
        ScreenUtil.navigationBarHeight = navigationBarHeight;
    }

    public static float getAug() {
        return aug;
    }

    public static void setAug(float aug) {
        ScreenUtil.aug = aug;
    }

    public static Bitmap screenShot(Activity activity) {
        Bitmap bitmap = null;
        if (activity != null) {
            Window window = activity.getWindow();
            if (window != null) {
                View decorView = window.getDecorView();
                decorView.setDrawingCacheEnabled(true);
                bitmap = decorView.getDrawingCache();
            }
        }
        return bitmap;
    }

    public static void screenShotReset(Activity activity) {
        if (activity != null) {
            Window window = activity.getWindow();
            if (window != null) {
                View decorView = window.getDecorView();
                decorView.setDrawingCacheEnabled(false);
            }
        }
    }

    public static boolean checkScreenShot(String filepath) {
        boolean isScreenShot = false;
        if (filepath != null) {
            filepath = filepath.toLowerCase();
            String[] filePathSegment = filepath.split("/");
            int length = filePathSegment.length;
            for (int i = 0 ; i < Math.min(Math.max((length - 1), 1), length); i++) {
                String segment = filePathSegment[i];
                boolean isContainPrefix = false;
                boolean isContainSuffix = false;
                if (segment != null) {
                    for (String prefix : ConstantField.ScreenShotKeyword.SCREEN_SHOT_KEYWORDS_PREFIX) {
                        if (segment.contains(prefix)) {
                            isContainPrefix = true;
                            break;
                        }
                    }
                    for (String suffix : ConstantField.ScreenShotKeyword.SCREEN_SHOT_KEYWORDS_SUFFIX) {
                        if (segment.contains(suffix)) {
                            isContainSuffix = true;
                            break;
                        }
                    }
                    if (isContainPrefix && isContainSuffix) {
                        isScreenShot = true;
                        break;
                    }
                }
            }
        }
        return isScreenShot;
    }
}
