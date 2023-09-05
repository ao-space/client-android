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
package com.google.zxing.client.android.encode;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.util.HashMap;
import java.util.Map;

/**
 * Author:      Zhu Fuyu
 * Description:
 * History:     2023/4/4
 */
public class QrEncodeUtil {

    /**
     * 生成二维码
     */
    public static Bitmap createCode(Context context, int width, int height, String content) {
        Bitmap bitmap = null;
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            BitMatrix matrix = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            int widthMatrix = matrix.getWidth();
            int heightMatrix = matrix.getHeight();
            int[] pixels = new int[widthMatrix * heightMatrix];
            for (int y = 0; y < heightMatrix; y++) {
                for (int x = 0; x < widthMatrix; x++) {
                    if (matrix.get(x, y)) {
                        pixels[y * widthMatrix + x] = 0xff000000;
                    } else {
                        pixels[y * widthMatrix + x] = Color.WHITE;
                    }
                }
            }
            bitmap = Bitmap.createBitmap(widthMatrix, heightMatrix, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, widthMatrix, 0, 0, widthMatrix, heightMatrix);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        return bitmap;
    }
}
