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

import android.util.Log;

import xyz.eulix.space.BuildConfig;

/**
 * Author:      Zhu Fuyu
 * Description: Log日志工具类
 * History:     2021/7/22
 */
public class Logger {
    private static final String TAG = "EulixLogger";
    private static boolean isDebug = BuildConfig.LOG_SWITCH;

    public static boolean isDebuggable() {
        return isDebug;
    }

    public static void setDebuggable(boolean isDebuggable) {
        isDebug = isDebuggable;
    }

    public static void v(String msg) {
        v(null, msg);
    }

    public static void v(String tag, String msg) {
        if (msg != null) {
            if (tag == null) {
                tag = TAG;
            }

            if (isDebug) {
                Log.v(tag, msg);
            }
        }
    }

    public static void d(String msg) {
        d(null, msg);
    }

    public static void d(String tag, String msg) {
        if (msg != null) {
            if (tag == null) {
                tag = TAG;
            }

            if (isDebug) {
                Log.d(tag, msg);
            }
        }
    }

    public static void i(String msg) {
        i(null, msg);
    }

    public static void i(String tag, String msg) {
        if (msg != null) {
            if (tag == null) {
                tag = TAG;
            }

            if (isDebug) {
                Log.i(tag, msg);
            }
        }
    }

    public static void w(String msg) {
        w(TAG, msg);
    }

    public static void w(String tag, String msg) {
        if (msg != null) {
            if (tag == null) {
                tag = TAG;
            }

            if (isDebug) {
                Log.w(tag, msg);
            }
        }
    }

    public static void e(String msg) {
        e(null, msg, null);
    }

    public static void e(String tag, String msg) {
        e(tag, msg, null);
    }

    public static void e(String tag, String msg, Throwable tr) {
        if (msg != null) {
            if (tag == null) {
                tag = TAG;
            }

            if (isDebug) {
                if (tr == null) {
                    Log.e(tag, msg);
                } else {
                    Log.e(tag, msg, tr);
                }
            }
        }
    }
}
