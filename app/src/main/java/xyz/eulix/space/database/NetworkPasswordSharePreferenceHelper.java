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

package xyz.eulix.space.database;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/2/15 14:54
 */
public class NetworkPasswordSharePreferenceHelper {
    private static NetworkPasswordSharePreferenceHelper instance;
    private SharedPreferences sharedPreferences;

    private NetworkPasswordSharePreferenceHelper(Context context) {
        sharedPreferences = context.getSharedPreferences("networkpassword", Context.MODE_PRIVATE);
    }

    public synchronized static NetworkPasswordSharePreferenceHelper getInstance(Context context) {
        if (instance == null && context != null) {
            instance = new NetworkPasswordSharePreferenceHelper(context);
        }
        return instance;
    }

    public String getString(String key) {
        String value = null;
        if (sharedPreferences != null && key != null) {
            value = sharedPreferences.getString(key, "");
        }
        return value;
    }

    public boolean setString(String key, String value, boolean isImmediate) {
        boolean result = false;
        if (sharedPreferences != null && key != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (editor != null) {
                editor.putString(key, value);
                if (isImmediate) {
                    result = editor.commit();
                } else {
                    editor.apply();
                    result = true;
                }
            }
        }
        return result;
    }

    public boolean removeString(String key, boolean isImmediate) {
        boolean result = false;
        if (sharedPreferences != null && key != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (editor != null) {
                editor.remove(key);
                if (isImmediate) {
                    result = editor.commit();
                } else {
                    editor.apply();
                    result = true;
                }
            }
        }
        return result;
    }

    public boolean containsKey(String key) {
        boolean result = false;
        if (sharedPreferences != null && key != null) {
            result = sharedPreferences.contains(key);
        }
        return result;
    }

    public boolean clear(boolean isImmediate) {
        boolean result = false;
        if (sharedPreferences != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (editor != null) {
                editor.clear();
                if (isImmediate) {
                    result = editor.commit();
                } else {
                    editor.apply();
                    result = true;
                }
            }
        }
        return result;
    }
}
