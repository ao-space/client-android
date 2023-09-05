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
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import xyz.eulix.space.R;
import xyz.eulix.space.manager.LanManager;
import xyz.eulix.space.manager.TransferTaskManager;
import xyz.eulix.space.network.files.FileListUtil;
import xyz.eulix.space.transfer.TransferHelper;
import xyz.eulix.space.transfer.calculator.TaskSpeed;
import xyz.eulix.space.transfer.db.TransferDBManager;
import xyz.eulix.space.transfer.model.TransferItem;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.FailCodeUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.GlideUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.NetUtils;
import xyz.eulix.space.util.ThreadPool;
import xyz.eulix.space.view.dialog.EulixDialogUtil;

/**
 * Author:      Zhu Fuyu
 * Description: 传输列表-传输中-适配器
 * History:     2021/8/15
 */
public class TransferringListAdapter extends RecyclerView.Adapter<TransferringListAdapter.ViewHolder> {
    private Context context;
    public List<TransferItem> dataList = new ArrayList<>();
    private int transferType;
    private ItemSelectListener listener;
    private boolean isSelectAll = false;
    private boolean isSelectMode = false;

    private Map<String, Integer> speedMap;

    private TransferredListAdapter.ItemLongClickListener longClickListener;

    private long mLastStateBtnClickTime = 0L;

    public TransferringListAdapter(Context context, int transferType) {
        this.context = context;
        this.transferType = transferType;
    }

    @NonNull
    @NotNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.transferring_item_layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ViewHolder holder, int position) {
        TransferItem item = dataList.get(position);
        if (item == null) {
            return;
        }
        int typeIndex = item.keyName.lastIndexOf(".");
        String suffix = item.keyName.substring(typeIndex + 1);
        holder.imgFileType.setImageResource(FileUtil.getMimeIcon(FileUtil.getMimeType(suffix)));
        holder.tvFileName.setText(item.keyName);
        if (isSelectMode) {
            holder.imgControlState.setVisibility(View.INVISIBLE);
            holder.imageMark.setVisibility(View.VISIBLE);
        } else {
            holder.imgControlState.setVisibility(View.VISIBLE);
            holder.imageMark.setVisibility(View.GONE);
        }
        if (isSelectAll) {
            holder.itemView.setTag(true);
            holder.imageMark.setImageResource(R.drawable.file_selected);
        } else {
            holder.itemView.setTag(false);
            holder.imageMark.setImageResource(R.drawable.background_fff5f6fa_oval_13);
        }

        String mimeType = FileUtil.getMimeType(suffix);
        if (mimeType.contains("image") || mimeType.contains("video")) {
            //图片显示内容
            String thumb = FileListUtil.getThumbPath(context, item.uuid);
            if (!TextUtils.isEmpty(thumb)) {
                GlideUtil.load(thumb, holder.imgFileType);
            } else if (transferType == TransferHelper.TYPE_UPLOAD && (FileUtil.isImageSupportView(mimeType) || FileUtil.isVideoSupportView(mimeType))) {
                GlideUtil.load(item.localPath + item.keyName, holder.imgFileType);
            } else {
                holder.imgFileType.setImageResource(FileUtil.getMimeIcon(mimeType));
            }
        } else {
            holder.imgFileType.setImageResource(FileUtil.getMimeIcon(mimeType));
        }
        int progress = (int) (item.currentSize * 100 / item.totalSize);
        if (progress < 3) {
            holder.progressBar.setProgressDrawable(context.getDrawable(R.drawable.transfer_progress_dialog_bg_less));
        } else {
            holder.progressBar.setProgressDrawable(context.getDrawable(R.drawable.transfer_progress_dialog_bg));
        }
        holder.progressBar.setProgress(progress);
        String size = FormatUtil.formatSize(item.totalSize, ConstantField.SizeUnit.FORMAT_1F);

        holder.tvFileSize.setText(size);
        holder.tvSpeed.setText("");

        setStateStyle(holder, item);

        holder.itemView.setOnClickListener(v -> {
            if (isSelectMode) {
                Logger.d("zfy", "isSelectMode");
                boolean selectedState = (boolean) holder.itemView.getTag();
                boolean isSelected = !selectedState;
                holder.itemView.setTag(isSelected);
                if (isSelected) {
                    holder.imageMark.setImageResource(R.drawable.file_selected);
                } else {
                    holder.imageMark.setImageResource(R.drawable.background_fff5f6fa_oval_13);
                }
                if (listener != null) {
                    listener.onSelectStateChange(dataList.get(position), position, isSelected);
                }
            }
        });

        holder.imgControlState.setOnClickListener(v -> {
            //阻止连续点击
            long clickTime = System.currentTimeMillis();
            if (clickTime - mLastStateBtnClickTime < 1000) {
                return;
            }
            mLastStateBtnClickTime = clickTime;

            switch (item.state) {
                case TransferHelper.STATE_ERROR:
                    if ("delete".equals((String) holder.imgControlState.getTag())) {
                        //源文件已删除
                        if (longClickListener != null) {
                            //长按删除
                            longClickListener.onItemLongClick(item, position);
                        }
                        return;
                    }
                    if (NetUtils.isMobileNetWork(context) && !ConstantField.sIAllowTransferWithMobileData) {
                        String confirmTitle = context.getResources().getString(R.string.mobile_data_upload);
                        String confirmDesc = context.getResources().getString(R.string.mobile_data_upload_desc);
                        if (item.transferType == TransferHelper.TYPE_DOWNLOAD) {
                            confirmTitle = context.getResources().getString(R.string.mobile_data_download);
                            confirmDesc = context.getResources().getString(R.string.mobile_data_download_desc);
                        }
                        EulixDialogUtil.showChooseAlertDialog(context, confirmTitle, confirmDesc,
                                context.getResources().getString(R.string.ok),
                                (dialog, which) -> {
                                    ConstantField.sIAllowTransferWithMobileData = true;
                                    callTransfer(item);
                                    //恢复状态及进度显示
                                    item.state = TransferHelper.STATE_DOING;
                                    dataList.get(position).state = TransferHelper.STATE_DOING;
                                    item.currentSize = 0L;
                                    holder.progressBar.setProgress(0);
                                    setStateStyle(holder, item);
                                }, null);
                    } else {
                        callTransfer(item);
                        //恢复状态及进度显示
                        item.state = TransferHelper.STATE_DOING;
                        dataList.get(position).state = TransferHelper.STATE_DOING;
                        item.currentSize = 0L;
                        holder.progressBar.setProgress(0);
                        setStateStyle(holder, item);
                    }

                    break;
                case TransferHelper.STATE_PREPARE:
                    //删除
                    if (longClickListener != null) {
                        //长按删除
                        longClickListener.onItemLongClick(item, position);
                    }
                    break;
                case TransferHelper.STATE_DOING:
                    if (transferType == ConstantField.TransferType.TYPE_UPLOAD) {
                        TransferTaskManager.getInstance().pauseUpload(item);
                    } else {
                        TransferTaskManager.getInstance().pauseDownload(item);
                    }
                    item.state = TransferHelper.STATE_PAUSE;
                    dataList.get(position).state = TransferHelper.STATE_PAUSE;
                    setStateStyle(holder, item);
                    break;
                case TransferHelper.STATE_PAUSE:
                    if (NetUtils.isMobileNetWork(context) && !ConstantField.sIAllowTransferWithMobileData) {
                        String confirmTitle = context.getResources().getString(R.string.mobile_data_transfer);
                        String confirmDesc = context.getResources().getString(R.string.mobile_data_transfer_desc);
                        EulixDialogUtil.showChooseAlertDialog(context, confirmTitle, confirmDesc,
                                context.getResources().getString(R.string.ok),
                                (dialog, which) -> {
                                    ConstantField.sIAllowTransferWithMobileData = true;
                                    if (transferType == ConstantField.TransferType.TYPE_UPLOAD) {
                                        TransferTaskManager.getInstance().resumeUpload(item);
                                    } else {
                                        TransferTaskManager.getInstance().resumeDownload(item);
                                    }
                                    item.state = TransferHelper.STATE_PREPARE;
                                    dataList.get(position).state = TransferHelper.STATE_PREPARE;
                                    setStateStyle(holder, item);
                                }, null);
                    } else {
                        if (transferType == ConstantField.TransferType.TYPE_UPLOAD) {
                            TransferTaskManager.getInstance().resumeUpload(item);
                        } else {
                            TransferTaskManager.getInstance().resumeDownload(item);
                        }
                        item.state = TransferHelper.STATE_PREPARE;
                        dataList.get(position).state = TransferHelper.STATE_PREPARE;
                        setStateStyle(holder, item);
                    }
                    break;
                default:
                    break;
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                //长按删除任务
                longClickListener.onItemLongClick(item, position);
            }
            return false;
        });
    }

    //修改状态样式
    private void setStateStyle(ViewHolder holder, TransferItem item) {
        switch (item.state) {
            case TransferHelper.STATE_ERROR:
                int errorCode = -1;
                String errorMessage = "";
                TransferItem currentItem = TransferDBManager.getInstance(context).queryByUniqueTag(item.ext1, item.transferType);
                if (currentItem != null) {
                    errorCode = currentItem.errorCode;
                    errorMessage = FailCodeUtil.getMessageByCode(errorCode);
                }
                if (!TextUtils.isEmpty(errorMessage)) {
                    holder.tvTransferState.setText(errorMessage);
                } else {
                    holder.tvTransferState.setText(transferType == ConstantField.TransferType.TYPE_UPLOAD
                            ? R.string.upload_fail_state : R.string.download_fail_state);
                }
                if (errorCode == FailCodeUtil.ERROR_UPLOAD_LOCAL_SOURCE_DELETE || errorCode == FailCodeUtil.ERROR_DOWNLOAD_REMOTE_SOURCE_DELETE) {
                    //源文件被删除
                    holder.imgControlState.setTag("delete");
                    holder.imgControlState.setImageResource(R.drawable.icon_transfer_state_delete);
                } else {
                    holder.imgControlState.setTag("retry");
                    holder.imgControlState.setImageResource(R.drawable.icon_transfer_state_error);
                }
                holder.tvTransferState.setTextColor(Color.parseColor("#FFF6222D"));
                holder.imgControlState.setVisibility(View.VISIBLE);
                holder.tvTransferState.setVisibility(View.VISIBLE);
                holder.tvSpeed.setVisibility(View.INVISIBLE);
                break;
            case TransferHelper.STATE_PREPARE:
                holder.imgControlState.setImageResource(R.drawable.icon_transfer_state_delete);
                holder.tvTransferState.setTextColor(context.getResources().getColor(R.color.c_ff337aff));
                holder.tvTransferState.setText(R.string.wait_for_transmit);
                holder.imgControlState.setVisibility(View.VISIBLE);
                holder.tvTransferState.setVisibility(View.VISIBLE);
                holder.tvSpeed.setVisibility(View.INVISIBLE);
                break;
            case TransferHelper.STATE_DOING:
                holder.imgControlState.setImageResource(R.drawable.icon_transfer_state_pause);
                holder.tvTransferState.setTextColor(context.getResources().getColor(R.color.c_ff337aff));
                holder.tvTransferState.setText(transferType == ConstantField.TransferType.TYPE_UPLOAD
                        ? R.string.uploading : R.string.downloading);
                holder.imgControlState.setVisibility(View.VISIBLE);
                holder.tvSpeed.setVisibility(View.VISIBLE);
                if (NetUtils.isMobileNetWork(context) && !ConstantField.sIAllowTransferWithMobileData) {
                    holder.tvTransferState.setText(context.getString(R.string.waiting_for_wifi));
                    holder.tvTransferState.setVisibility(View.VISIBLE);
                    holder.tvSpeed.setVisibility(View.INVISIBLE);
                } else {
                    holder.tvTransferState.setVisibility(View.GONE);
                    if (item.transferType == TransferHelper.TYPE_UPLOAD) {
                        int speed = TaskSpeed.getInstance().checkCurrentUploadSpeed(item.ext1);
                        if (speed > 0) {
                            String speedStr = FormatUtil.formatSize(speed, ConstantField.SizeUnit.FORMAT_1F) + "/s";
                            if (LanManager.getInstance().isLanEnable()) {
                                holder.tvSpeed.setTextColor(context.getResources().getColor(R.color.c_ff00c991));
                                String tvFastUploadContent = (context.getString(R.string.fast_upload) + speedStr);
                                holder.tvSpeed.setText(tvFastUploadContent);
                            } else {
                                holder.tvSpeed.setTextColor(context.getResources().getColor(R.color.c_ff337aff));
                                holder.tvSpeed.setText(speedStr);
                            }
                        } else {
                            holder.tvSpeed.setText("");
                        }
                    } else {
                        holder.tvSpeed.setText("");
                    }
                }
                break;
            case TransferHelper.STATE_PAUSE:
                if (transferType == ConstantField.TransferType.TYPE_UPLOAD) {
                    holder.imgControlState.setImageResource(R.drawable.icon_transfer_state_uploading);
                } else {
                    holder.imgControlState.setImageResource(R.drawable.icon_transfer_state_downloading);
                }
                holder.tvTransferState.setTextColor(context.getResources().getColor(R.color.c_ff337aff));
                holder.tvTransferState.setText(transferType == ConstantField.TransferType.TYPE_UPLOAD
                        ? R.string.upload_pause : R.string.download_pause);
                holder.imgControlState.setVisibility(View.VISIBLE);
                holder.tvTransferState.setVisibility(View.VISIBLE);
                holder.tvSpeed.setVisibility(View.INVISIBLE);
                break;
            default:
                holder.imgControlState.setImageResource(R.drawable.icon_transfer_state_delete);
                holder.tvTransferState.setTextColor(context.getResources().getColor(R.color.c_ff337aff));
                holder.tvTransferState.setText("");
                holder.imgControlState.setVisibility(View.VISIBLE);
                holder.tvTransferState.setVisibility(View.GONE);
                holder.tvSpeed.setVisibility(View.INVISIBLE);
        }
    }

    private void callTransfer(TransferItem item) {
        //重新传输
        if (item.transferType == TransferHelper.TYPE_UPLOAD) {
            //上传
            ThreadPool.getInstance().execute(() -> {
                TransferTaskManager.getInstance().insertUploadTask(item.localPath, item.keyName, item.remotePath, true, item.ext3);
            });
        } else {
            //下载
            ThreadPool.getInstance().execute(() -> {
                TransferTaskManager.getInstance().insertDownloadTask(item.uuid, item.remotePath, item.keyName, item.totalSize, item.md5, true, item.ext2);
            });
        }
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull ViewHolder holder, int position, @NonNull @NotNull List<Object> payloads) {
        if (payloads.isEmpty()) {
            onBindViewHolder(holder, position);
        } else if ("refresh_progress".equals((String) payloads.get(0))) {
            //刷新进度
            if (dataList.isEmpty() || dataList.get(position) == null) {
                return;
            }
            TransferItem item = dataList.get(position);
            int progress = (int) (item.currentSize * 100 / item.totalSize);
            int lastProgress = holder.progressBar.getProgress();
            if (progress <= lastProgress) {
                //防止进度条回退
                Logger.d("zfy", "currentProgress less than last");
                return;
            }
            Logger.d("zfy", "set progress:" + progress);
            if (progress < 3) {
                //圆角进度进度太少时会变形，需要切割
                holder.progressBar.setProgressDrawable(context.getDrawable(R.drawable.transfer_progress_dialog_bg_less));
            } else {
                holder.progressBar.setProgressDrawable(context.getDrawable(R.drawable.transfer_progress_dialog_bg));
            }
            holder.progressBar.setProgress(progress);
            holder.tvTransferState.setText("");
        } else if ("refresh_speed".equals((String) payloads.get(0))) {
            //刷新速度
            if (speedMap == null || dataList.isEmpty() || dataList.get(position) == null) {
                return;
            }
            TransferItem item = dataList.get(position);
            if (item.state != TransferHelper.STATE_DOING) {
                //未处于传输中，不刷新速度
                return;
            }
            String uniqueTag = item.ext1;
            if (speedMap.containsKey(uniqueTag)) {
                int speedPerSecond = speedMap.get(uniqueTag);
                if (speedPerSecond >= 0) {
                    String speedStr = FormatUtil.formatSize(speedPerSecond, ConstantField.SizeUnit.FORMAT_1F) + "/s";

                    if (LanManager.getInstance().isLanEnable()) {
                        holder.tvSpeed.setTextColor(context.getResources().getColor(R.color.c_ff00c991));
                        if (item.transferType == TransferHelper.TYPE_UPLOAD) {
                            String tvFastUploadContent = (context.getString(R.string.fast_upload) + speedStr);
                            holder.tvSpeed.setText(tvFastUploadContent);
                        } else {
                            String tvFastDownloadContent = (context.getString(R.string.fast_download) + speedStr);
                            holder.tvSpeed.setText(tvFastDownloadContent);
                        }
                    } else {
                        holder.tvSpeed.setTextColor(context.getResources().getColor(R.color.c_ff337aff));
                        holder.tvSpeed.setText(speedStr);
                    }
                }
            }
        } else if ("refresh_state".equals((String) payloads.get(0))) {
            //刷新状态
            if (dataList.isEmpty() || dataList.get(position) == null) {
                return;
            }

            TransferItem item = dataList.get(position);
            if (item.state == TransferHelper.STATE_ERROR) {
                TransferItem currentItem = TransferDBManager.getInstance(context).queryByUniqueTag(item.ext1, item.transferType);
                dataList.get(position).errorCode = currentItem.errorCode;
            }
            setStateStyle(holder, item);
        } else if ("refresh_network".equals((String) payloads.get(0))) {
            //刷新网络状态
            if (dataList.isEmpty() || dataList.get(position) == null) {
                return;
            }

            TransferItem item = dataList.get(position);
            if (item.state != TransferHelper.STATE_DOING) {
                return;
            }
            if (NetUtils.isMobileNetWork(context) && !ConstantField.sIAllowTransferWithMobileData) {
                holder.tvTransferState.setText(context.getString(R.string.waiting_for_wifi));
                holder.tvTransferState.setVisibility(View.VISIBLE);
                holder.tvSpeed.setVisibility(View.INVISIBLE);
            } else {
                holder.tvTransferState.setText("");
                holder.tvTransferState.setVisibility(View.GONE);
                holder.tvSpeed.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public void setSelectAll(boolean isSelectAll) {
        this.isSelectAll = isSelectAll;
    }

    //设置是否进入选择模式
    public void setSelectMode(boolean isSelectMode) {
        this.isSelectMode = isSelectMode;
    }

    public boolean isSelectMode() {
        return this.isSelectMode;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgFileType, imgControlState, imageMark;
        TextView tvFileName, tvFileSize, tvTransferState;
        TextView tvSpeed;
        ProgressBar progressBar;

        public ViewHolder(@NonNull @NotNull View itemView) {
            super(itemView);
            imgFileType = itemView.findViewById(R.id.img_sync_icon);
            imgControlState = itemView.findViewById(R.id.img_control_state);
            tvFileName = itemView.findViewById(R.id.tv_file_name);
            tvFileSize = itemView.findViewById(R.id.tv_file_size);
            progressBar = itemView.findViewById(R.id.progress_bar_sync);
            tvTransferState = itemView.findViewById(R.id.tv_transfer_state);
            imageMark = itemView.findViewById(R.id.image_mark);
            tvSpeed = itemView.findViewById(R.id.tv_speed);
        }
    }

    public void setSpeedMap(Map<String, Integer> speedMap) {
        this.speedMap = speedMap;
    }

    public void appendSpeedMap(String key, int value) {
        if (this.speedMap == null) {
            speedMap = new HashMap<>();
        }
        speedMap.put(key, value);
    }

    public void setSelectListener(ItemSelectListener listener) {
        this.listener = listener;
    }

    public void setLongClickListener(TransferredListAdapter.ItemLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public interface ItemSelectListener {
        void onSelectStateChange(TransferItem item, int position, boolean isSelect);
    }

    public interface ItemLongClickListener {
        void onItemLongClick(TransferItem item, int position);
    }
}
