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
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.R;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/9/1 15:27
 */
public class ToastManager {
    private Context mContext;
    private View pureTextToastView, imageTextToastView;

    public ToastManager(Context context) {
        mContext = context;
    }

    public Toast makePureTextToast(@StringRes int resId) {
        return makePureTextToast(resId, false, Gravity.CENTER, 0, 0);
    }

    public Toast makePureTextToast(@StringRes int resId, boolean isDurationLong) {
        Toast toast = null;
        if (pureTextToastView == null && mContext != null) {
            pureTextToastView = LayoutInflater.from(mContext).inflate(R.layout.custom_toast_view_pure_text_pattern, null);
        }
        if (pureTextToastView != null && mContext != null) {
            TextView text = pureTextToastView.findViewById(R.id.toast_text);
            if (mContext instanceof Activity) {
                text.setText(resId);
            } else {
                Context context = EulixSpaceApplication.getResumeActivityContext();
                if (context != null) {
                    text.setText(context.getString(resId));
                } else {
                    text.setText(resId);
                }
            }
            toast = new Toast(mContext);
            toast.setView(pureTextToastView);
            toast.setDuration((isDurationLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT));
        }
        return toast;
    }

    public Toast makePureTextToast(String content, boolean isDurationLong) {
        Toast toast = null;
        if (pureTextToastView == null && mContext != null) {
            pureTextToastView = LayoutInflater.from(mContext).inflate(R.layout.custom_toast_view_pure_text_pattern, null);
        }
        if (pureTextToastView != null && mContext != null) {
            TextView text = pureTextToastView.findViewById(R.id.toast_text);
            text.setText(content);
            toast = new Toast(mContext);
            toast.setView(pureTextToastView);
            toast.setDuration((isDurationLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT));
        }
        return toast;
    }

    public Toast makePureTextToast(@StringRes int resId, boolean isDurationLong, int gravity, int xOffset, int yOffset) {
        Toast toast = null;
        if (pureTextToastView == null && mContext != null) {
            pureTextToastView = LayoutInflater.from(mContext).inflate(R.layout.custom_toast_view_pure_text_pattern, null);
        }
        if (pureTextToastView != null && mContext != null) {
            TextView text = pureTextToastView.findViewById(R.id.toast_text);
            if (mContext instanceof Activity) {
                text.setText(resId);
            } else {
                Context context = EulixSpaceApplication.getResumeActivityContext();
                if (context != null) {
                    text.setText(context.getString(resId));
                } else {
                    text.setText(resId);
                }
            }
            toast = new Toast(mContext);
            toast.setView(pureTextToastView);
            toast.setGravity(gravity, xOffset, yOffset);
            toast.setDuration((isDurationLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT));
        }
        return toast;
    }

    public Toast makePureTextToast(String content, boolean isDurationLong, int gravity, int xOffset, int yOffset) {
        Toast toast = null;
        if (pureTextToastView == null && mContext != null) {
            pureTextToastView = LayoutInflater.from(mContext).inflate(R.layout.custom_toast_view_pure_text_pattern, null);
        }
        if (pureTextToastView != null && mContext != null) {
            TextView text = pureTextToastView.findViewById(R.id.toast_text);
            text.setText(content);
            toast = new Toast(mContext);
            toast.setView(pureTextToastView);
            toast.setGravity(gravity, xOffset, yOffset);
            toast.setDuration((isDurationLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT));
        }
        return toast;
    }

    public Toast makeImageTextToast(@DrawableRes int drawableResId, @StringRes int stringResId) {
        return makeImageTextToast(drawableResId, stringResId, false, Gravity.CENTER, 0, 0);
    }

    public Toast makeImageTextToast(@DrawableRes int drawableResId, @StringRes int stringResId, boolean isDurationLong, int gravity, int xOffset, int yOffset) {
        Toast toast = null;
        if (imageTextToastView == null && mContext != null) {
            imageTextToastView = LayoutInflater.from(mContext).inflate(R.layout.custom_toast_view_image_text_pattern, null);
        }
        if (imageTextToastView != null && mContext != null) {
            ImageView image = imageTextToastView.findViewById(R.id.toast_image);
            TextView text = imageTextToastView.findViewById(R.id.toast_text);
            if (mContext instanceof Activity) {
                image.setImageResource(drawableResId);
                text.setText(stringResId);
            } else {
                Context context = EulixSpaceApplication.getResumeActivityContext();
                if (context != null) {
                    image.setImageDrawable(context.getDrawable(drawableResId));
                    text.setText(context.getString(stringResId));
                } else {
                    image.setImageResource(drawableResId);
                    text.setText(stringResId);
                }
            }
            toast = new Toast(mContext);
            toast.setView(imageTextToastView);
            toast.setGravity(gravity, xOffset, yOffset);
            toast.setDuration((isDurationLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT));
        }
        return toast;
    }

    public Toast makeImageTextToast(@DrawableRes int drawableResId, String content, boolean isDurationLong, int gravity, int xOffset, int yOffset) {
        Toast toast = null;
        if (imageTextToastView == null && mContext != null) {
            imageTextToastView = LayoutInflater.from(mContext).inflate(R.layout.custom_toast_view_image_text_pattern, null);
        }
        if (imageTextToastView != null && mContext != null) {
            ImageView image = imageTextToastView.findViewById(R.id.toast_image);
            TextView text = imageTextToastView.findViewById(R.id.toast_text);
            if (mContext instanceof Activity) {
                image.setImageResource(drawableResId);
            } else {
                Context context = EulixSpaceApplication.getResumeActivityContext();
                if (context != null) {
                    image.setImageDrawable(context.getDrawable(drawableResId));
                } else {
                    image.setImageResource(drawableResId);
                }
            }
            text.setText(content);
            toast = new Toast(mContext);
            toast.setView(imageTextToastView);
            toast.setGravity(gravity, xOffset, yOffset);
            toast.setDuration((isDurationLong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT));
        }
        return toast;
    }

    public void showToast(@NonNull Toast toast) {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            //当前非主线程，进行切换
            new Handler(Looper.getMainLooper()).post(()->{
                toast.show();
            });
        } else {
            toast.show();
        }
    }

    public void showDefaultPureTextToast(@StringRes int resId) {
        showPureTextToast(resId, false);
    }

    public void showDefaultPureTextToast(String content) {
        showPureTextToast(content, false);
    }

    public void showPureTextToast(@StringRes int resId) {
        showPureTextToast(resId, false, Gravity.CENTER, 0, 0);
    }

    public void showPureTextToast(String content) {
        showPureTextToast(content, false, Gravity.CENTER, 0, 0);
    }

    public void showPureTextToast(@StringRes int resId, boolean isDurationLong) {
        Toast toast = makePureTextToast(resId, isDurationLong);
        if (toast != null) {
            showToast(toast);
        }
    }

    public void showPureTextToast(String content, boolean isDurationLong) {
        Toast toast = makePureTextToast(content, isDurationLong);
        if (toast != null) {
            showToast(toast);
        }
    }

    public void showPureTextToast(@StringRes int resId, boolean isDurationLong, int gravity, int xOffset, int yOffset) {
        Toast toast = makePureTextToast(resId, isDurationLong, gravity, xOffset, yOffset);
        if (toast != null) {
            showToast(toast);
        }
    }

    public void showPureTextToast(String content, boolean isDurationLong, int gravity, int xOffset, int yOffset) {
        Toast toast = makePureTextToast(content, isDurationLong, gravity, xOffset, yOffset);
        if (toast != null) {
            showToast(toast);
        }
    }

    public void showImageTextToast(@DrawableRes int drawableResId, @StringRes int stringResId) {
        showImageTextToast(drawableResId, stringResId, false, Gravity.CENTER, 0, 0);
    }

    public void showImageTextToast(@DrawableRes int drawableResId, String content) {
        showImageTextToast(drawableResId, content, false, Gravity.CENTER, 0, 0);
    }

    public void showImageTextToast(@DrawableRes int drawableResId, @StringRes int stringResId, boolean isDurationLong, int gravity, int xOffset, int yOffset) {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            //当前非主线程，进行切换
            new Handler(Looper.getMainLooper()).post(()->{
                Toast toast = makeImageTextToast(drawableResId, stringResId, isDurationLong, gravity, xOffset, yOffset);
                if (toast != null) {
                    showToast(toast);
                }
            });
        } else {
            Toast toast = makeImageTextToast(drawableResId, stringResId, isDurationLong, gravity, xOffset, yOffset);
            if (toast != null) {
                showToast(toast);
            }
        }
    }

    public void showImageTextToast(@DrawableRes int drawableResId, String content, boolean isDurationLong, int gravity, int xOffset, int yOffset) {
        Toast toast = makeImageTextToast(drawableResId, content, isDurationLong, gravity, xOffset, yOffset);
        if (toast != null) {
            showToast(toast);
        }
    }
}
