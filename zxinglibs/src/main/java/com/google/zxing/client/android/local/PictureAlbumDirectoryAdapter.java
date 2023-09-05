package com.google.zxing.client.android.local;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.client.android.R;
import com.google.zxing.client.android.bean.LocalMediaUpItem;
import com.google.zxing.client.android.bean.PhotoUpImageBucket;
import com.google.zxing.client.android.util.GlideUtil;

import java.util.ArrayList;
import java.util.List;

public class PictureAlbumDirectoryAdapter extends RecyclerView.Adapter<PictureAlbumDirectoryAdapter.ViewHolder> {
    private Context mContext;
    private List<PhotoUpImageBucket> folders = new ArrayList<>();
    private String imageUnit;

    public PictureAlbumDirectoryAdapter(Context mContext, String imageUnit) {
        super();
        this.mContext = mContext;
        this.imageUnit = imageUnit;
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
        if (position < 0 || folders == null || folders.size() <= position) {
            return;
        }
        final PhotoUpImageBucket folder = folders.get(position);
        if (folder == null) {
            return;
        }
        String name = folder.getBucketName();
        int imageNum = folder.getImageList().size();
        String imagePath = folder.getImageList().get(0).getMediaPath();
        boolean isChecked = folder.isChecked();
        holder.itemView.setSelected(isChecked);
        GlideUtil.load(imagePath, holder.first_image);
        StringBuilder imageNumTextBuilder = new StringBuilder();
        imageNumTextBuilder.append(imageNum);
        if (imageUnit != null) {
            imageNumTextBuilder.append(imageUnit);
        } else if (mContext != null) {
            imageNumTextBuilder.append(mContext.getString(R.string.common_language_unit_zhang));
        }
        holder.image_num.setText(imageNumTextBuilder.toString());
        holder.tv_folder_name.setText(name);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onItemClickListener != null) {
                    for (PhotoUpImageBucket mediaFolder : folders) {
                        mediaFolder.setChecked(false);
                    }
                    folder.setChecked(true);
                    notifyDataSetChanged();
                    onItemClickListener.onItemClick(folder.getBucketName(), folder.getImageList());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return (folders == null ? 0 : folders.size());
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
