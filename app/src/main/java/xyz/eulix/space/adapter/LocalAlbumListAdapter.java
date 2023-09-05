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

package xyz.eulix.space.adapter;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import xyz.eulix.space.R;
import xyz.eulix.space.bean.PhotoUpImageBucket;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.GlideUtil;

/**
 * Author:      Zhu Fuyu
 * Description: 本地相簿列表适配器
 * History:     2021/7/22
 */
public class LocalAlbumListAdapter extends RecyclerView.Adapter<LocalAlbumListAdapter.ViewHolder> {
    private Context context;
    public List<PhotoUpImageBucket> dataList = new ArrayList<>();
    private int mediaType;
    private String path;
    private ContentResolver contentResolver;
    private OnItemClickListener mListener;

    public LocalAlbumListAdapter(Context context, int mediaType, String path) {
        this.context = context;
        this.mediaType = mediaType;
        this.path = path;
        this.contentResolver = context.getApplicationContext().getContentResolver();
    }

    @NonNull
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.local_album_list_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ViewHolder holder, int position) {
        PhotoUpImageBucket imageBucket = dataList.get(position);
//        GlideUtil.load("file://" + imageBucket.getImageList().get(0).getImagePath(),
//                holder.albumImage);
        if (imageBucket.getCount() == 0) {
            holder.itemView.getLayoutParams().height = 0;
            return;
        } else {
            holder.itemView.getLayoutParams().height = context.getResources().getDimensionPixelOffset(R.dimen.dp_94);
        }
        Bitmap thumbnail;
        if (mediaType == ConstantField.MediaType.MEDIA_IMAGE) {
            //加载缩略图
            thumbnail = MediaStore.Images.Thumbnails.getThumbnail(contentResolver,
                    Long.parseLong(imageBucket.getImageList().get(0).getMediaId()), MediaStore.Video.Thumbnails.MINI_KIND, null);
            holder.tvDesc.setText(imageBucket.getImageList().size() + "张");
        } else {
            String mimeType = FileUtil.getMimeTypeByPath(imageBucket.getImageList().get(0).getDisplayName());
            if (mimeType.contains("image")) {
                //加载缩略图
                thumbnail = MediaStore.Images.Thumbnails.getThumbnail(contentResolver,
                        Long.parseLong(imageBucket.getImageList().get(0).getMediaId()), MediaStore.Video.Thumbnails.MINI_KIND, null);
            } else {
                thumbnail = MediaStore.Video.Thumbnails.getThumbnail(contentResolver,
                        Long.parseLong(imageBucket.getImageList().get(0).getMediaId()), MediaStore.Video.Thumbnails.MINI_KIND, null);
            }
            holder.tvDesc.setText(imageBucket.getImageList().size() + "个");
        }
        GlideUtil.load(thumbnail, holder.albumImage);
        holder.tvTitle.setText(imageBucket.getBucketName());

        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onItemClick(imageBucket.getBucketName());
            }
        });
    }


    @Override
    public int getItemCount() {
        return dataList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView albumImage;
        public TextView tvTitle, tvDesc;

        public ViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            albumImage = itemView.findViewById(R.id.album_list_img);
            tvTitle = itemView.findViewById(R.id.album_title_tv);
            tvDesc = itemView.findViewById(R.id.alubm_desc_tv);
        }
    }

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.mListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(String bucketName);
    }
}
