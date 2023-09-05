package com.google.zxing.client.android.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.google.zxing.client.android.R;

import java.io.File;

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
}
