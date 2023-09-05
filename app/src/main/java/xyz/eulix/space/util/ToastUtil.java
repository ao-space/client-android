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
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.R;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/9/1 15:04
 */
public class ToastUtil {

    private ToastUtil() {
        throw new AssertionError("not allow to be instantiation!");
    }

    public static void showSystemToast(@NonNull Context context, @StringRes int resId, boolean isDurationLong) {
        Toast.makeText(context, resId, isDurationLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
    }

    public static void showSystemToast(@NonNull Context context, @NonNull CharSequence text, boolean isDurationLong) {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            //当前非主线程，进行切换
            new Handler(Looper.getMainLooper()).post(()->{
                Toast.makeText(context, text, isDurationLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
            });
        } else {
            Toast.makeText(context, text, isDurationLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT).show();
        }
    }

    public static void showToast(@NonNull Toast toast) {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            //当前非主线程，进行切换
            new Handler(Looper.getMainLooper()).post(()->{
                toast.show();
            });
        } else {
            toast.show();
        }
    }

    public static void showToast(String text) {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            //当前非主线程，进行切换
            new Handler(Looper.getMainLooper()).post(()->{
                makePureTextToast(text, false, Gravity.CENTER, 0, 0).show();
            });
        } else {
            makePureTextToast(text, false, Gravity.CENTER, 0, 0).show();
        }
    }

    public static Toast makeCustomToast(@NonNull Context context, @NonNull View view, boolean isDurationLong, int gravity, int xOffset, int yOffset) {
        Toast toast = new Toast(context);
        toast.setView(view);
        toast.setGravity(gravity, xOffset, yOffset);
        toast.setDuration((isDurationLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT));
        return toast;
    }

    public static void showCustomToast(@NonNull Context context, @NonNull View view, boolean isDurationLong, int gravity, int xOffset, int yOffset) {
        showToast(makeCustomToast(context, view, isDurationLong, gravity, xOffset, yOffset));
    }

    private static Toast makePureTextToast(String content, boolean isDurationLong, int gravity, int xOffset, int yOffset) {
        Context mContext = EulixSpaceApplication.getResumeActivityContext();
        View pureTextToastView = LayoutInflater.from(mContext).inflate(R.layout.custom_toast_view_pure_text_pattern, null);
        TextView text = pureTextToastView.findViewById(R.id.toast_text);
        text.setText(content);
        Toast toast = new Toast(mContext);
        toast.setView(pureTextToastView);
        toast.setGravity(gravity, xOffset, yOffset);
        toast.setDuration((isDurationLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT));
        return toast;
    }
}
