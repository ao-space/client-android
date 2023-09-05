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
 * date: 2021/6/16 15:54
 */
public class EulixSpaceSharePreferenceHelper {
    private static EulixSpaceSharePreferenceHelper instance;
    private SharedPreferences sharedPreferences;

    private EulixSpaceSharePreferenceHelper(Context context) {
        sharedPreferences = context.getSharedPreferences("eulixspace", Context.MODE_PRIVATE);
    }

    public static EulixSpaceSharePreferenceHelper getInstance() {
        return instance;
    }

    public synchronized static EulixSpaceSharePreferenceHelper getInstance(Context context) {
        if (instance == null && context != null) {
            instance = new EulixSpaceSharePreferenceHelper(context);
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

    public boolean getBoolean(String key, boolean defValue) {
        boolean value = defValue;
        if (sharedPreferences != null && key != null) {
            value = sharedPreferences.getBoolean(key, defValue);
        }
        return value;
    }

    public boolean setBoolean(String key, boolean value, boolean isImmediate) {
        boolean result = false;
        if (sharedPreferences != null && key != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (editor != null) {
                editor.putBoolean(key, value);
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

    public Integer getInt(String key) {
        Integer value = null;
        if (sharedPreferences != null && key != null) {
            value = sharedPreferences.getInt(key, -1);
        }
        return value;
    }

    public Integer getInt(String key, int defValue) {
        Integer value = null;
        if (sharedPreferences != null && key != null) {
            value = sharedPreferences.getInt(key, defValue);
        }
        return value;
    }

    public boolean setInt(String key, int value, boolean isImmediate) {
        boolean result = false;
        if (sharedPreferences != null && key != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (editor != null) {
                editor.putInt(key, value);
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

    public Long getLong(String key) {
        Long value = null;
        if (sharedPreferences != null && key != null) {
            value = sharedPreferences.getLong(key, -1L);
        }
        return value;
    }

    public boolean setLong(String key, long value, boolean isImmediate) {
        boolean result = false;
        if (sharedPreferences != null && key != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (editor != null) {
                editor.putLong(key, value);
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

    public boolean remove(String key, boolean isImmediate) {
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
