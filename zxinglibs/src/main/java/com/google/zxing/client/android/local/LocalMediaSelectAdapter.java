package com.google.zxing.client.android.local;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.zxing.client.android.R;
import com.google.zxing.client.android.bean.LocalMediaUpItem;
import com.google.zxing.client.android.util.ConstantField;
import com.google.zxing.client.android.util.FileUtil;
import com.google.zxing.client.android.util.FormatUtil;
import com.google.zxing.client.android.util.GlideUtil;

import java.util.ArrayList;
import java.util.List;

public class LocalMediaSelectAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context context;
    public List<LocalMediaUpItem> dataList = new ArrayList<>();
    private ImageSelectListener listener;
    public static final int STATE_SELECT_NORMAL = 0;
    public static final int STATE_SELECT_ALL = 1;
    public static final int STATE_SELECT_NULL = 2;
    private int mSelectAllState = STATE_SELECT_NORMAL;
    private ContentResolver contentResolver;
    private int mediaType;
    private boolean checkStateVisible = true;
    private long mLastToastShowTime;

    public LocalMediaSelectAdapter(Context context, int mediaType) {
        this.context = context;
        this.contentResolver = context.getApplicationContext().getContentResolver();
        this.mediaType = mediaType;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ConstantField.MediaType.MEDIA_IMAGE || viewType == ConstantField.MediaType.MEDIA_VIDEO || viewType == ConstantField.MediaType.MEDIA_IMAGE_AND_VIDEO) {
            View view = LayoutInflater.from(context).inflate(R.layout.image_select_item_layout, parent, false);
            return new ImageViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.file_select_item_layout, parent, false);
            return new FileViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, final int position) {
        if (position < 0 || dataList == null || dataList.size() <= position) {
            return;
        }
        final LocalMediaUpItem item = dataList.get(position);
        if (item == null) {
            return;
        }
        if (viewHolder instanceof ImageViewHolder) {
            //加载图片、视频
            ImageViewHolder holder = (ImageViewHolder) viewHolder;
            holder.checkBox.setClickable(false);
            if (!checkStateVisible) {
                holder.checkBox.setVisibility(View.GONE);
            }
            if (item.isSelected()) {
                holder.checkBox.setChecked(true);
                holder.imgMask.setVisibility(checkStateVisible ? View.VISIBLE : View.GONE);
            } else {
                holder.checkBox.setChecked(false);
                holder.imgMask.setVisibility(View.GONE);
            }
            Bitmap thumbnail;
            String mimeType = FileUtil.getMimeTypeByPath(item.getDisplayName());
            if (mimeType.contains("image")) {
                holder.tvVideoDuration.setVisibility(View.GONE);
                //加载缩略图
                thumbnail = MediaStore.Images.Thumbnails.getThumbnail(contentResolver,
                        Long.parseLong(item.getMediaId()), MediaStore.Video.Thumbnails.MINI_KIND, null);
            } else {
                if (!TextUtils.isEmpty(item.getDuration())) {
                    holder.tvVideoDuration.setVisibility(View.VISIBLE);
                    holder.tvVideoDuration.setText(FormatUtil.timeLong2Str(Long.parseLong(item.getDuration())));
                }

                thumbnail = MediaStore.Video.Thumbnails.getThumbnail(contentResolver,
                        Long.parseLong(item.getMediaId()), MediaStore.Video.Thumbnails.MINI_KIND, null);
            }

            GlideUtil.load(thumbnail, holder.imageView);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
//                    if (!item.isSelected() && getSelectedCount() > 99) {
//                        ToastUtil.showToast(context.getResources().getString(R.string.select_count_limit));
//                        return;
//                    }

                    if (listener != null) {
                        listener.onSelectStateChange(dataList.get(position), position, !item.isSelected());
                    }
                }
            });
            holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    if (listener != null){
                        listener.onItemLongClick(dataList.get(position), position);
                    }
                    return false;
                }
            });
        } else if (viewHolder instanceof FileViewHolder) {
            //加载文件
            final FileViewHolder holder = (FileViewHolder) viewHolder;

            if (item.isSelected()) {
                holder.itemView.setTag(true);
                if (checkStateVisible) {
                    holder.imageMark.setImageResource(R.drawable.file_selected);
                }
            } else {
                holder.itemView.setTag(false);
                if (checkStateVisible) {
                    holder.imageMark.setImageResource(R.drawable.background_fff5f6fa_oval_13);
                }
            }

//            Logger.d("zfy", "mimeType=" + item.getMimeType());
            int typeIndex = item.getMediaPath().lastIndexOf(".");
            String suffix = item.getMediaPath().substring(typeIndex + 1);
//            holder.imageView.setImageResource(FileUtil.getMimeIcon(FileUtil.getMimeType(suffix)));
            int index = item.getMediaPath().lastIndexOf("/");
            String fileName = item.getMediaPath().substring(index + 1);
            holder.tvFileName.setText(fileName);

            StringBuilder sizeAndDate = new StringBuilder();
            String size = FormatUtil.formatSize(item.getSize(), ConstantField.SizeUnit.FORMAT_1F);
            if (!TextUtils.isEmpty(size)) {
                sizeAndDate.append(size);
                sizeAndDate.append(" ");
            }
            String date = FormatUtil.formatTime(item.getModifiedDate() * 1000, ConstantField.TimeStampFormat.EMAIL_FORMAT);
            if (!TextUtils.isEmpty(date)) {
                sizeAndDate.append(date);
            }
            String fileDescStr = sizeAndDate.toString();
            holder.tvFileDesc.setText(fileDescStr);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean currentSelected = !item.isSelected();
                    item.setSelected(currentSelected);
                    if (checkStateVisible) {
                        if (currentSelected) {
                            holder.imageMark.setImageResource(R.drawable.file_selected);
                        } else {
                            holder.imageMark.setImageResource(R.drawable.background_fff5f6fa_oval_13);
                        }
                    }
                    if (listener != null) {
                        listener.onSelectStateChange(dataList.get(position), position, !item.isSelected());
                    }
                }
            });
        }

    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position, @NonNull List<Object> payloads) {
        if (position < 0 || dataList == null || dataList.size() <= position) {
            return;
        }
        if (payloads.isEmpty()) {
            onBindViewHolder(viewHolder, position);
        } else if ("refresh_selected".equals((String) payloads.get(0))) {
            //刷新选中状态
            LocalMediaUpItem item = dataList.get(position);
            if (item == null) {
                return;
            }
            if (viewHolder instanceof ImageViewHolder) {
                //加载图片、视频
                ImageViewHolder holder = (ImageViewHolder) viewHolder;
                holder.checkBox.setClickable(false);
                if (!checkStateVisible) {
                    holder.checkBox.setVisibility(View.GONE);
                }
                if (item.isSelected()) {
                    holder.checkBox.setChecked(true);
                    holder.imgMask.setVisibility(checkStateVisible ? View.VISIBLE : View.GONE);
                } else {
                    holder.checkBox.setChecked(false);
                    holder.imgMask.setVisibility(View.GONE);
                }
            } else if (viewHolder instanceof FileViewHolder) {
                //加载文件
                FileViewHolder holder = (FileViewHolder) viewHolder;

                if (item.isSelected()) {
                    holder.itemView.setTag(true);
                    if (checkStateVisible) {
                        holder.imageMark.setImageResource(R.drawable.file_selected);
                    }
                } else {
                    holder.itemView.setTag(false);
                    if (checkStateVisible) {
                        holder.imageMark.setImageResource(R.drawable.background_fff5f6fa_oval_13);
                    }
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return (dataList == null ? 0 : dataList.size());
    }

    @Override
    public int getItemViewType(int position) {
        return mediaType;
    }

//    @Override
//    public void setSelected(int index, boolean selected) {
//        // do something to mark this index as selected/unselected
//        Log.d("zfy", "AlbumHomeAdapter setSelected:index=" + index + ",selected=" + selected);
//        if (dataList.isEmpty() || index >= dataList.size()) {
//            return;
//        }
//
//        LocalMediaUpItem item = dataList.get(index);
//        if (item.isSelected() != selected) {
////            if (!item.isSelected() && getSelectedCount() > 99) {
////                //控制批量选择Toast提示次数
////                if (System.currentTimeMillis()-mLastToastShowTime>3000) {
////                    ToastUtil.showToast(context.getResources().getString(R.string.select_count_limit));
////                    mLastToastShowTime = System.currentTimeMillis();
////                }
////                return;
////            }
//            if (listener != null) {
//                listener.onSelectStateChange(dataList.get(index), index, selected);
//            }
//        }
//    }
//
//    @Override
//    public boolean isIndexSelectable(int index) {
//        // if you return false, this index can't be used with setIsActive()
//        if (dataList.isEmpty() || index >= dataList.size()) {
//            return false;
//        }
//        int viewType = getItemViewType(index);
//        if (viewType == ConstantField.MediaType.MEDIA_IMAGE || viewType == ConstantField.MediaType.MEDIA_VIDEO || viewType == ConstantField.MediaType.MEDIA_IMAGE_AND_VIDEO) {
//            return true;
//        }else {
//            return false;
//        }
//    }
//
//    @Override
//    public boolean isSelected(int index) {
//        // return true if this index is currently selected
//        if (dataList.isEmpty() || index >= dataList.size()) {
//            return false;
//        }
//        LocalMediaUpItem item = dataList.get(index);
//        return item.isSelected();
//    }


    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView, imgMask;
        CheckBox checkBox;
        TextView tvVideoDuration;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_item);
            imgMask = itemView.findViewById(R.id.img_mask);
            checkBox = itemView.findViewById(R.id.checkbox_select);
            tvVideoDuration = itemView.findViewById(R.id.video_duration);
        }
    }

    static class FileViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView, imageMark;
        TextView tvFileName, tvFileDesc;

        public FileViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_item);
            imageMark = itemView.findViewById(R.id.image_mark);
            tvFileName = itemView.findViewById(R.id.tv_file_name);
            tvFileDesc = itemView.findViewById(R.id.tv_create_time);
        }
    }

    //设置选中状态显示
    public void setCheckStateVisible(boolean checkStateVisible) {
        this.checkStateVisible = checkStateVisible;
    }


    public void setSelectListener(ImageSelectListener listener) {
        this.listener = listener;
    }

    public interface ImageSelectListener {
        void onSelectStateChange(LocalMediaUpItem item, int position, boolean isSelect);
        void onItemLongClick(LocalMediaUpItem item, int position);
    }

    private int getSelectedCount() {
        int selectedCount = 0;
        for (LocalMediaUpItem item : dataList) {
            if (item.isSelected()) {
                selectedCount++;
            }
        }
        return selectedCount;
    }
}
