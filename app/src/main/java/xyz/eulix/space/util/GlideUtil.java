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
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.io.File;

import xyz.eulix.space.R;
import xyz.eulix.space.util.glide.CircularImageViewTarget;

/**
 * Author:      Zhu Fuyu
 * Description:
 * History:     2021/7/21
 */
public class GlideUtil {

    //加载图片，不适用缓存
    public static void loadNoCache(String imgUrl, ImageView img) {
        if (img == null || img.getContext() == null) {
            return;
        }
        Context context = img.getContext();
        try {
            Glide.with(context)
                    .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.shape_bg_default)
                            .skipMemoryCache(true) // 不使用内存缓存
                            .diskCacheStrategy(DiskCacheStrategy.NONE))
                    .load(imgUrl)
                    .into(img);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void load(String imgUrl, ImageView img) {
        if (img == null || img.getContext() == null) {
            return;
        }
        Context context = img.getContext();
        try {
            Glide.with(context)
                    .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.shape_bg_default))
                    .load(imgUrl)
                    .into(img);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void loadReplace(String imgUrl, ImageView img) {
        if (img == null || img.getContext() == null) {
            return;
        }
        Context context = img.getContext();
        try {
            Glide.with(context)
                    .load(imgUrl)
                    .placeholder(img.getDrawable())
                    .skipMemoryCache(false)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .dontAnimate()
                    .into(img);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void loadRes(int resId, ImageView img) {
        if (img == null || img.getContext() == null) {
            return;
        }
        Context context = img.getContext();
        try {
            Glide.with(context)
                    .load(resId)
                    .into(img);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
    public static void load(String imgUrl, ImageView img, int defaultResId) {
        if (img == null || img.getContext() == null) {
            return;
        }
        Context context = img.getContext();
        try {
            Glide.with(context)
                    .setDefaultRequestOptions(new RequestOptions().placeholder(defaultResId).error(defaultResId))
                    .load(imgUrl)
                    .into(img);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //加载网络图片
    public static void load(Bitmap bitmap, ImageView img) {
        if (img == null || img.getContext() == null) {
            return;
        }
        Context context = img.getContext();
        try {
            Glide.with(context)
                    .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.shape_bg_default))
                    .load(bitmap)
                    .into(img);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //加载圆形图片
    public static void loadCircleFromPath(String path, ImageView img) {
        if (img == null || img.getContext() == null) {
            return;
        }
        Context context = img.getContext();
        try {
            Glide.with(context)
                    .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.icon_user_header_default).skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE).dontAnimate())
                    .asBitmap().load(Uri.fromFile(new File(path))).into(new CircularImageViewTarget(img));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //加载圆形图片
    public static void loadUserCircleFromPath(String path, ImageView img) {
        if (img == null || img.getContext() == null) {
            return;
        }
        Context context = img.getContext();
        try {
            Glide.with(context)
                    .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.avatar_default).skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE).dontAnimate())
                    .asBitmap().load(Uri.fromFile(new File(path))).into(new CircularImageViewTarget(img));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //加载圆形图片
    public static void loadUserCircleFromUrl(String imageUrl, ImageView img) {
        if (img == null || img.getContext() == null) {
            return;
        }
        Context context = img.getContext();
        try {
            Glide.with(context)
                    .setDefaultRequestOptions(new RequestOptions().placeholder(R.drawable.avatar_default).skipMemoryCache(true)
                            .diskCacheStrategy(DiskCacheStrategy.NONE).dontAnimate())
                    .asBitmap().load(imageUrl).into(new CircularImageViewTarget(img));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
