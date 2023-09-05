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
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import xyz.eulix.space.R;
import xyz.eulix.space.network.files.FileListUtil;
import xyz.eulix.space.transfer.model.TransferItem;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.GlideUtil;

/**
 * Author:      Zhu Fuyu
 * Description: 传输列表-完成-适配器
 * History:     2021/8/15
 */
public class TransferredListAdapter extends RecyclerView.Adapter<TransferredListAdapter.ViewHolder> {
    private Context context;
    public List<TransferItem> dataList = new ArrayList<>();
    private int transferType;
    private ItemClickListener listener;
    private boolean isSelectAll = false;
    private boolean isSelectMode = false;
    private ItemLongClickListener longClickListener;

    public TransferredListAdapter(Context context, int transferType) {
        this.context = context;
        this.transferType = transferType;
    }

    @NonNull
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.transferred_item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ViewHolder holder, int position) {
        TransferItem item = dataList.get(position);
        int typeIndex = item.keyName.lastIndexOf(".");
        String suffix = item.keyName.substring(typeIndex + 1);
        holder.tvFileName.setText(item.keyName);
        holder.imageMark.setImageResource(item.isSelected ? R.drawable.file_selected : R.drawable.background_fff5f6fa_oval_13);
        String mimeType = FileUtil.getMimeType(suffix);
        if (mimeType.contains("image") || mimeType.contains("video")) {
            //图片显示内容
            String thumb = FileListUtil.getThumbPath(context, item.uuid);
            if (!TextUtils.isEmpty(thumb)) {
                GlideUtil.load(thumb, holder.imgFileType);
            } else if (new File(item.localPath, item.keyName).exists()) {
                GlideUtil.load(item.localPath + item.keyName, holder.imgFileType);
            } else {
                holder.imgFileType.setImageResource(FileUtil.getMimeIcon(mimeType));
            }
        } else {
            holder.imgFileType.setImageResource(FileUtil.getMimeIcon(mimeType));
        }

        holder.tvFileName.setText(item.keyName);

        StringBuilder sizeAndDate = new StringBuilder();
        String size = FormatUtil.formatSize(item.totalSize, ConstantField.SizeUnit.FORMAT_1F);
        if (!TextUtils.isEmpty(size)) {
            sizeAndDate.append(size);
            sizeAndDate.append(" ");
        }
        String date = FormatUtil.formatTime(item.updateTime, ConstantField.TimeStampFormat.EMAIL_FORMAT);
        if (!TextUtils.isEmpty(date)) {
            sizeAndDate.append(date);
        }
        if (transferType == ConstantField.TransferType.TYPE_UPLOAD) {
            sizeAndDate.append("\n");
            if (context != null) {
                sizeAndDate.append(context.getString(R.string.upload_to_path_prefix));
            }
            sizeAndDate.append(item.remotePath);
        }
        String fileDescStr = sizeAndDate.toString();
        holder.tvFileDesc.setText(fileDescStr);

        holder.imageMark.setOnClickListener(v -> {
            boolean isSelected = !item.isSelected;
            item.isSelected = isSelected;
            if (isSelected) {
                holder.imageMark.setImageResource(R.drawable.file_selected);
            } else {
                holder.imageMark.setImageResource(R.drawable.background_fff5f6fa_oval_13);
            }
            if (listener != null) {
                listener.onSelectStateChange(dataList.get(position), position, true, isSelected);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (isSelectMode) {
                boolean isSelected = !item.isSelected;
                item.isSelected = isSelected;
                if (isSelected) {
                    holder.imageMark.setImageResource(R.drawable.file_selected);
                } else {
                    holder.imageMark.setImageResource(R.drawable.background_fff5f6fa_oval_13);
                }
                if (listener != null) {
                    listener.onSelectStateChange(dataList.get(position), position, true, isSelected);
                }
            } else {
                if (listener != null) {
                    listener.onSelectStateChange(dataList.get(position), position, false, false);
                }
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (!isSelectMode && longClickListener != null) {
                //非选择状态长按可删除单条数据
                longClickListener.onItemLongClick(dataList.get(position), position);
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public void setSelectAll(boolean isSelectAll) {
        this.isSelectAll = isSelectAll;
        for (int i = 0; i < dataList.size(); i++) {
            dataList.get(i).isSelected = isSelectAll;
        }
    }

    //设置是否进入选择模式
    public void setSelectMode(boolean isSelectMode) {
        this.isSelectMode = isSelectMode;
    }

    public boolean isSelectMode() {
        return this.isSelectMode;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFileType, imageMark;
        TextView tvFileName, tvFileDesc;

        public ViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            imgFileType = itemView.findViewById(R.id.img_sync_icon);
            tvFileName = itemView.findViewById(R.id.tv_file_name);
            tvFileDesc = itemView.findViewById(R.id.tv_create_time);
            imageMark = itemView.findViewById(R.id.image_mark);
        }
    }

    public void setItemClickListener(ItemClickListener listener) {
        this.listener = listener;
    }

    public void setLongClickListener(ItemLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public interface ItemClickListener {
        void onSelectStateChange(TransferItem item, int position, boolean isSelectMode, boolean isSelect);
    }

    public interface ItemLongClickListener {
        void onItemLongClick(TransferItem item, int position);
    }
}
