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

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;

import androidx.annotation.DrawableRes;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/8/31 11:37
 */
public class DrawableUtil {
    private DrawableUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    public static Drawable bitmapToDrawable(Bitmap inValue, Resources resources) {
        Drawable outValue = null;
        if (inValue != null && resources != null) {
            outValue = new BitmapDrawable(resources, inValue);
        }
        return outValue;
    }

    public static Bitmap drawableToBitmap(int drawableResId, Resources resources) {
        Bitmap outValue = null;
        if (resources != null) {
            try {
                outValue = BitmapFactory.decodeResource(resources, drawableResId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return outValue;
    }

    public static byte[] bitmapToByteArray(Bitmap inValue, boolean needRecycle) {
        byte[] outValue = null;
        if (inValue != null) {
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                inValue.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                outValue = outputStream.toByteArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (needRecycle) {
                inValue.recycle();
            }
        }
        return outValue;
    }

    public static Bitmap blur(Bitmap bitmap, Context context) {
        Bitmap blurBitmap = bitmap;
        if (bitmap != null && context != null) {
            int statusBarHeight = ViewUtils.getStatusBarHeight(context);
            int screenHeight = ViewUtils.getScreenHeight(context);
            Bitmap originBitmap;
            if (bitmap.getHeight() >= screenHeight && screenHeight > statusBarHeight) {
                originBitmap = Bitmap.createBitmap(bitmap, 0, statusBarHeight, bitmap.getWidth(), (screenHeight - statusBarHeight));
            } else {
                originBitmap = Bitmap.createBitmap(bitmap);
            }
            blurBitmap = Bitmap.createBitmap(originBitmap);
            RenderScript renderScript = RenderScript.create(context);
            if (renderScript != null) {
                ScriptIntrinsicBlur scriptIntrinsicBlur = ScriptIntrinsicBlur.create(renderScript, Element.U8_4(renderScript));
                Allocation allocationIn = Allocation.createFromBitmap(renderScript, originBitmap);
                Allocation allocationOut = Allocation.createFromBitmap(renderScript, blurBitmap);
                scriptIntrinsicBlur.setRadius(24.999999f);
                scriptIntrinsicBlur.setInput(allocationIn);
                scriptIntrinsicBlur.forEach(allocationOut);
                allocationOut.copyTo(blurBitmap);
                allocationIn.destroy();
                allocationOut.destroy();
            }
        }
        return blurBitmap;
    }

}
