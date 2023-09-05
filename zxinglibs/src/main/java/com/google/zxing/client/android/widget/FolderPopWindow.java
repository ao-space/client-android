package com.google.zxing.client.android.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.client.android.R;
import com.google.zxing.client.android.bean.PhotoUpImageBucket;
import com.google.zxing.client.android.local.PictureAlbumDirectoryAdapter;
import com.google.zxing.client.android.util.ViewUtils;

import java.util.List;

public class FolderPopWindow extends PopupWindow implements View.OnClickListener {
    private Context context;
    private View window;
    private RecyclerView recyclerView;
    private PictureAlbumDirectoryAdapter adapter;
    private Animation animationIn;
    private Animation animationOut;
    private boolean isDismiss = false;
    private LinearLayout id_ll_root;
    private String imageUnit;

    public FolderPopWindow(Context context, String imageUnit) {
        this.context = context;
        window = LayoutInflater.from(context).inflate(R.layout.picture_window_folder, null);
        this.setContentView(window);
        this.setWidth(ViewUtils.getScreenWidth(context));
        this.setHeight(ViewUtils.getScreenHeight(context));
        this.setAnimationStyle(R.style.FolderPopWindowStyle);
        this.setFocusable(true);
        this.setOutsideTouchable(true);
        this.update();
        this.setBackgroundDrawable(new ColorDrawable(Color.argb(123, 0, 0, 0)));
        animationIn = AnimationUtils.loadAnimation(context, R.anim.photo_album_show);
        animationOut = AnimationUtils.loadAnimation(context, R.anim.photo_album_dismiss);
        this.imageUnit = imageUnit;
        initView();
    }

    public void initView() {
        id_ll_root = window.findViewById(R.id.id_ll_root);
        adapter = new PictureAlbumDirectoryAdapter(context, imageUnit);
        recyclerView = window.findViewById(R.id.folder_list);
        recyclerView.getLayoutParams().height = (int) (ViewUtils.getScreenHeight(context) * 0.7);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);
        id_ll_root.setOnClickListener(this);
    }

    public void bindFolder(List<PhotoUpImageBucket> folders) {
        adapter.bindFolderData(folders);
    }


    @Override
    public void showAsDropDown(View anchor) {
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                Rect rect = new Rect();
                anchor.getGlobalVisibleRect(rect);
                int h = anchor.getResources().getDisplayMetrics().heightPixels - rect.bottom;
                setHeight(h);
            }
            super.showAsDropDown(anchor);
            isDismiss = false;
            recyclerView.startAnimation(animationIn);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setOnItemClickListener(PictureAlbumDirectoryAdapter.OnItemClickListener onItemClickListener) {
        adapter.setOnItemClickListener(onItemClickListener);
    }

    @Override
    public void dismiss() {
        if (isDismiss) {
            return;
        }
        isDismiss = true;
        recyclerView.startAnimation(animationOut);
        dismiss();
        animationOut.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                //do nothing
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                isDismiss = false;
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
                    dismiss4Pop();
                } else {
                    FolderPopWindow.super.dismiss();
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
                //do nothing
            }
        });
    }

    /**
     * 在android4.1.1和4.1.2版本关闭PopWindow
     */
    private void dismiss4Pop() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                FolderPopWindow.super.dismiss();
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.id_ll_root) {
            dismiss();
        }
    }
}
