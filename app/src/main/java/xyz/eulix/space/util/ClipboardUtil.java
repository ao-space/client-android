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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import androidx.annotation.NonNull;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/10/12 9:39
 */
public class ClipboardUtil {
    public static final String TAG = ClipboardUtil.class.getSimpleName();
    public static final String LABEL = "Label";

    private ClipboardUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    public static boolean setClipData(@NonNull Context context, String plainText) {
        boolean result = false;
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager != null && plainText != null) {
            ClipData clipData = ClipData.newPlainText(LABEL, plainText);
            clipboardManager.setPrimaryClip(clipData);
            result = true;
        }
        return result;
    }

    public static ClipData getClipData(@NonNull Context context) {
        ClipData clipData = null;
        ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager != null) {
            clipData = clipboardManager.getPrimaryClip();
        }
        return clipData;
    }
}
