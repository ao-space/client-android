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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import xyz.eulix.space.R;
import xyz.eulix.space.bean.LocalMediaUpItem;
import xyz.eulix.space.bean.PhotoUpImageBucket;
import xyz.eulix.space.util.GlideUtil;

/**
 * Author:      Zhu Fuyu
 * Description: 相册文件夹选择适配器
 * History:     2021/9/15
 */
public class PictureAlbumDirectoryAdapter extends RecyclerView.Adapter<PictureAlbumDirectoryAdapter.ViewHolder> {
    private Context mContext;
    private List<PhotoUpImageBucket> folders = new ArrayList<>();

    public PictureAlbumDirectoryAdapter(Context mContext) {
        super();
        this.mContext = mContext;
    }

    public void bindFolderData(List<PhotoUpImageBucket> folders) {
        this.folders = folders;
        notifyDataSetChanged();
    }


    public List<PhotoUpImageBucket> getFolderData() {
        if (folders == null) {
            folders = new ArrayList<>();
        }
        return folders;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(mContext).inflate(R.layout.picture_album_folder_item, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final PhotoUpImageBucket folder = folders.get(position);
        String name = folder.getBucketName();
        int imageNum = folder.getImageList().size();
        String imagePath = folder.getImageList().get(0).getMediaPath();
        boolean isChecked = folder.isChecked();
        holder.itemView.setSelected(isChecked);
        GlideUtil.load(imagePath, holder.first_image);
        StringBuilder imageNumTextBuilder = new StringBuilder();
        imageNumTextBuilder.append(imageNum);
        if (mContext != null) {
            imageNumTextBuilder.append(mContext.getString(R.string.common_language_unit_zhang));
        }
        holder.image_num.setText(imageNumTextBuilder.toString());
        holder.tv_folder_name.setText(name);
        holder.itemView.setOnClickListener(view -> {
            if (onItemClickListener != null) {
                for (PhotoUpImageBucket mediaFolder : folders) {
                    mediaFolder.setChecked(false);
                }
                folder.setChecked(true);
                notifyDataSetChanged();
                onItemClickListener.onItemClick(folder.getBucketName(), folder.getImageList());
            }
        });
    }

    @Override
    public int getItemCount() {
        return folders.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView first_image;
        TextView tv_folder_name, image_num;

        public ViewHolder(View itemView) {
            super(itemView);
            first_image = itemView.findViewById(R.id.first_image);
            tv_folder_name = itemView.findViewById(R.id.tv_folder_name);
            image_num = itemView.findViewById(R.id.image_num);
        }
    }

    private OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onItemClick(String folderName, List<LocalMediaUpItem> images);
    }
}
