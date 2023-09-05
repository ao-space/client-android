package com.google.zxing.client.android.util;

import android.text.TextUtils;

public class FileUtil {
    public static String getMimeTypeByPath(String path) {
        String mimeType = "*/*";
        if (!TextUtils.isEmpty(path)) {
            int typeIndex = path.lastIndexOf(".");
            String suffix = path.substring(typeIndex + 1);
            mimeType = getMimeType(suffix);
        }
        return mimeType;
    }

    public static String getMimeType(String mime) {
        String mimeType = "*/*";
        if (!TextUtils.isEmpty(mime)) {
            for (String[] mimeArray : ConstantField.MimeType.MIME_MAP_TABLE) {
                if (mimeArray != null && mimeArray.length >= 2 && mimeArray[0].equalsIgnoreCase(mime)) {
                    mimeType = mimeArray[1];
                    break;
                }
            }
        }
        return mimeType;
    }
}
