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

package xyz.eulix.space.fragment;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.abs.AbsFragment;
import xyz.eulix.space.adapter.TransferredListAdapter;
import xyz.eulix.space.adapter.TransferringListAdapter;
import xyz.eulix.space.event.TransferListNetworkEvent;
import xyz.eulix.space.manager.TransferTaskManager;
import xyz.eulix.space.presenter.TransferListFragmentPresenter;
import xyz.eulix.space.transfer.TransferHelper;
import xyz.eulix.space.transfer.db.TransferDBManager;
import xyz.eulix.space.transfer.event.TransferSizeEvent;
import xyz.eulix.space.transfer.event.TransferSpeedEvent;
import xyz.eulix.space.transfer.event.TransferStateEvent;
import xyz.eulix.space.transfer.event.TransferringCountEvent;
import xyz.eulix.space.transfer.model.TransferItem;
import xyz.eulix.space.ui.FilePreviewActivity;
import xyz.eulix.space.ui.TransferListActivity;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.SystemMediaUtils;
import xyz.eulix.space.util.ThreadPool;
import xyz.eulix.space.util.ViewUtils;
import xyz.eulix.space.view.LineItemDecoration;
import xyz.eulix.space.view.TitleBarWithSelect;
import xyz.eulix.space.view.dialog.EulixDialogUtil;

/**
 * Author:      Zhu Fuyu
 * Description: 传输列表fragment
 * History:     2021/8/13
 */
public class TransferListFragment extends AbsFragment<TransferListFragmentPresenter.ITransferListFragment, TransferListFragmentPresenter> implements TransferListFragmentPresenter.ITransferListFragment, View.OnClickListener {
    private int type;
    private List<TransferItem> transferringData = new ArrayList<>();
    private List<TransferItem> transferredData = new ArrayList<>();
    private RecyclerView recyclerViewDoing;
    private RecyclerView recyclerViewDone;
    private TransferringListAdapter adapterDoing;
    private TransferredListAdapter adapterDone;
    private TextView tvDoingTitle, tvDoneTitle, tvClearDoneRecord, btnDelete;
    private LinearLayout layoutTitleDoing, layoutTitleDone, layoutDelete;
    private TextView tvDoingControlAll;
    private RelativeLayout layoutDoing;
    private RelativeLayout layoutDone;
    private boolean isSyncDetailOpen = true;
    private boolean isDoingListOpen = true;
    private boolean isDoneListOpen = true;
    private ImageView imgDoingTitleArrow, imgDoneTitleArrow;
    private RelativeLayout layoutEmpty;
    private NestedScrollView scrollviewList;
    private String deleteNoticeText;

    //正在传输整体控制(0-无任务，不显示；1-有传输中任务；2-有暂停中任务)
    private static final int DOING_CONTROL_STATE_EMPTY = 0;
    private static final int DOING_CONTROL_STATE_DOING = 1;
    private static final int DOING_CONTROL_STATE_PAUSE = 2;
    private int mCurrentDoingControlState = 0;

    private long mLastClickAllStateControl = 0L;

    public TransferListFragment(int type) {
        this.type = type;
    }

    @Override
    public void initRootView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.transfer_list_fragment_layout, container, false);
        EventBusUtil.register(this);
    }

    @NotNull
    @Override
    public TransferListFragmentPresenter createPresenter() {
        return new TransferListFragmentPresenter();
    }

    @Override
    public void initData() {
        deleteNoticeText = type == ConstantField.TransferType.TYPE_DOWNLOAD ? getResources().getString(R.string.delete_download_confirm_text)
                : getResources().getString(R.string.delete_upload_confirm_text);
    }

    @Override
    public void initView(@Nullable View root) {
        if (root == null) {
            return;
        }
        scrollviewList = root.findViewById(R.id.scrollview_list);
        layoutEmpty = root.findViewById(R.id.layout_empty);
        recyclerViewDoing = root.findViewById(R.id.recycler_view_doing);
        recyclerViewDoing.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        adapterDoing = new TransferringListAdapter(getContext(), type);
        recyclerViewDoing.setAdapter(adapterDoing);
        recyclerViewDoing.addItemDecoration(new LineItemDecoration(RecyclerView.VERTICAL, Math.round(getResources().getDimension(R.dimen.dp_1)),
                getResources().getColor(R.color.white_fff7f7f9)));

        recyclerViewDone = root.findViewById(R.id.recycler_view_done);
        recyclerViewDone.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        adapterDone = new TransferredListAdapter(getContext(), type);
        recyclerViewDone.setAdapter(adapterDone);
        recyclerViewDone.addItemDecoration(new LineItemDecoration(RecyclerView.VERTICAL, Math.round(getResources().getDimension(R.dimen.dp_1)),
                getResources().getColor(R.color.white_fff7f7f9)));

        tvDoingTitle = root.findViewById(R.id.tv_transfer_doing_title);
        tvDoneTitle = root.findViewById(R.id.tv_transfer_done_title);

        layoutDoing = root.findViewById(R.id.layout_transfer_doing);
        layoutDone = root.findViewById(R.id.layout_transfer_done);

        layoutTitleDoing = root.findViewById(R.id.layout_title_doing);
        layoutTitleDoing.setOnClickListener(this);
        layoutTitleDone = root.findViewById(R.id.layout_title_done);
        layoutTitleDone.setOnClickListener(this);

        imgDoingTitleArrow = root.findViewById(R.id.img_transfer_doing_arrow);
        imgDoneTitleArrow = root.findViewById(R.id.img_transfer_done_arrow);

        tvDoingControlAll = root.findViewById(R.id.tv_transfer_doing_control);
        tvDoingControlAll.setOnClickListener(this);

        tvClearDoneRecord = root.findViewById(R.id.tv_transfer_done_control);
        tvClearDoneRecord.setOnClickListener(this);

        layoutDelete = root.findViewById(R.id.layout_delete);
        btnDelete = root.findViewById(R.id.btn_delete);

        closeDefaultAnimator(recyclerViewDoing);
        closeDefaultAnimator(recyclerViewDone);
        btnDelete.setOnClickListener(this);
    }

    @Override
    public void initViewData() {

        new Thread(() -> {
            ArrayList<TransferItem> transferringDataTemp = TransferDBManager.getInstance(getContext()).queryUnfinishedTasks(type);
            ArrayList<TransferItem> transferredDataTemp = TransferDBManager.getInstance(getContext()).queryFinishedTasks(type);

            if (transferringDataTemp != null) {
                transferringData.clear();
                transferringData.addAll(transferringDataTemp);
            }

            if (transferredDataTemp != null) {
                transferredData.clear();
                transferredData.addAll(transferredDataTemp);
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                refreshMainViews();
            });
        }).start();

    }

    private void refreshMainViews() {
        refreshTitleCount();
        if (transferringData.size() + transferredData.size() > 0) {
            scrollviewList.setVisibility(View.VISIBLE);
            showTransferListItems();
            refreshDoingControlAll();
        } else {
            scrollviewList.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        }

    }

    //刷新传输中整体控制显示（"全部暂停/继续"）
    private void refreshDoingControlAll() {
        if (adapterDoing.dataList.isEmpty()) {
            tvDoingControlAll.setText("");
            mCurrentDoingControlState = DOING_CONTROL_STATE_EMPTY;
        } else {
            boolean hasDoingItem = false;
            int errorItemCount = 0;
            for (TransferItem doingItem : adapterDoing.dataList) {
                if (doingItem.state == TransferHelper.STATE_DOING) {
                    hasDoingItem = true;
                    break;
                } else if (doingItem.state == TransferHelper.STATE_ERROR) {
                    errorItemCount++;
                }
            }
            if (hasDoingItem) {
                //有传输中的任务
                tvDoingControlAll.setText(R.string.pause_all);
                mCurrentDoingControlState = DOING_CONTROL_STATE_DOING;
            } else {
                //判断是否全部失败
                if (errorItemCount == adapterDoing.dataList.size()) {
                    tvDoingControlAll.setText("");
                    mCurrentDoingControlState = DOING_CONTROL_STATE_EMPTY;
                } else {
                    //有传输中的任务
                    tvDoingControlAll.setText(R.string.continue_all);
                    mCurrentDoingControlState = DOING_CONTROL_STATE_PAUSE;
                }
            }
        }
    }

    private void changeAllDoingState() {
        switch (mCurrentDoingControlState) {
            case DOING_CONTROL_STATE_DOING:
                for (int i = 0; i < adapterDoing.dataList.size(); i++) {
                    if (adapterDoing.dataList.get(i).state == TransferHelper.STATE_DOING
                            || adapterDoing.dataList.get(i).state == TransferHelper.STATE_PREPARE) {
                        //暂停全部
                        adapterDoing.dataList.get(i).state = TransferHelper.STATE_PAUSE;
                        adapterDoing.notifyItemChanged(i, "refresh_state");
                    }
                }
                if (type == ConstantField.TransferType.TYPE_UPLOAD) {
                    TransferTaskManager.getInstance().stopAllUploadTasks();
                }else {
                    TransferTaskManager.getInstance().stopAllDownloadTasks();
                }
                refreshDoingControlAll();
                break;
            case DOING_CONTROL_STATE_PAUSE:
                ArrayList<TransferItem> pauseList = new ArrayList<>();
                for (int i = 0; i < adapterDoing.dataList.size(); i++) {
                    if (adapterDoing.dataList.get(i).state == TransferHelper.STATE_PAUSE) {
                        //恢复
                        pauseList.add(adapterDoing.dataList.get(i));
                        adapterDoing.dataList.get(i).state = TransferHelper.STATE_PREPARE;
                        adapterDoing.notifyItemChanged(i, "refresh_state");
                    }

                }
                if (type == ConstantField.TransferType.TYPE_UPLOAD) {
                    TransferTaskManager.getInstance().resumeUploadList(pauseList);
                }else {
                    TransferTaskManager.getInstance().resumeDownloadList(pauseList);
                }
                refreshDoingControlAll();
                break;
        }
    }

    private void showTransferListItems() {
        scrollviewList.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        adapterDoing.dataList.clear();
        adapterDone.dataList.clear();

        adapterDoing.dataList.addAll(transferringData);
        adapterDone.dataList.addAll(transferredData);
        adapterDoing.notifyDataSetChanged();
        setDoingRecyclerViewHeight();
        adapterDone.notifyDataSetChanged();
        setDoneRecyclerViewHeight();
    }

    @Override
    public void initEvent() {
        adapterDoing.setSelectListener((item, position, isSelect) -> presenter.refreshDoingSelectedData(item, isSelect));
        adapterDoing.setLongClickListener(((item, position) -> {
            //删除异常任务
            EulixDialogUtil.showBottomDeleteDialog(getActivity(), getString(R.string.delete_this_task), (dialog, which) -> {
                //删除数据库并刷新列表
                if (position >= adapterDoing.dataList.size()) {
                    return;
                }
                adapterDoing.dataList.remove(position);
                adapterDoing.notifyItemRemoved(position);
                adapterDoing.notifyItemRangeChanged(position, adapterDoing.dataList.size());
                setDoingRecyclerViewHeight();
                transferringData.remove(position);
                if (type == ConstantField.TransferType.TYPE_DOWNLOAD) {
                    TransferTaskManager.getInstance().deleteDownloadItem(item);
                } else {
                    TransferTaskManager.getInstance().deleteUploadItem(item);
                }
                refreshTitleCount();
                refreshDoingControlAll();
                TransferTaskManager.getInstance().changeTransferringCount(false);
                EventBusUtil.post(new TransferringCountEvent(TransferTaskManager.getInstance().getTransferringCount()));
            }, null);
        }));
        adapterDone.setItemClickListener((item, position, isSelectMode, isSelect) -> {
            if (isSelectMode) {
                presenter.refreshDoneSelectedData(item, isSelect, position);
            } else {
                int typeIndex = item.keyName.lastIndexOf(".");
                String suffix = item.keyName.substring(typeIndex + 1);
                String mimeType = FileUtil.getMimeType(suffix);
                //判断是否可以预览
                if (FileUtil.checkIsSupportPreview(mimeType) && !FileUtil.isOfficeFile(mimeType)) {
                    File localFile = new File(item.localPath, item.keyName);
                    if (localFile.exists()) {
                        //本地文件预览
                        FilePreviewActivity.openLocal(getActivity(), item.keyName, item.localPath, item.uuid, item.totalSize);
                    } else {
                        //在线预览
                        startPreviewFile(item);
                    }
                } else {
                    SystemMediaUtils.openMediaFile(getContext(), item.localPath + item.keyName);
                }
            }
        });
        adapterDone.setLongClickListener((item, position) -> {
            //删除数据库文件并刷新
            deleteRecordFromLongClick(item, position);
        });
    }

    private void startPreviewFile(TransferItem customizeFile) {
        Intent intent = new Intent(getActivity(), FilePreviewActivity.class);
        intent.putExtra(FilePreviewActivity.KEY_FILE_NAME, customizeFile.keyName);
        intent.putExtra(FilePreviewActivity.KEY_FILE_PATH, customizeFile.localPath);
        intent.putExtra(FilePreviewActivity.KEY_FILE_UUID, customizeFile.uuid);
        intent.putExtra(FilePreviewActivity.KEY_FILE_SIZE, customizeFile.totalSize);
        intent.putExtra(FilePreviewActivity.KEY_FILE_MD5, customizeFile.md5);
        intent.putExtra(FilePreviewActivity.KEY_FILE_TIME, customizeFile.createTime);
        intent.putExtra(FilePreviewActivity.KEY_SHOW_BOTTOM_EDIT, false);
        startActivity(intent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(TransferSizeEvent event) {
        if (type != event.transferType) {
            return;
        }
        for (int i = 0; i < adapterDoing.dataList.size(); i++) {
            TransferItem item = adapterDoing.dataList.get(i);
            if (!TextUtils.isEmpty(event.uniqueTag) && event.uniqueTag.equals(item.ext1)) {
                adapterDoing.dataList.get(i).currentSize = event.currentSize;
                adapterDoing.notifyItemChanged(i, "refresh_progress");
                break;
            }
        }
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(TransferListNetworkEvent event) {
        Logger.d("zfy","onReceive TransferListNetworkEvent");
        for (int i=0;i<adapterDoing.dataList.size();i++){
            if (adapterDoing.dataList.get(i).state == TransferHelper.STATE_DOING){
                adapterDoing.notifyItemChanged(i,"refresh_network");
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(TransferSpeedEvent.TransferSpeedMap event) {
        if (event.mMap != null) {
            adapterDoing.setSpeedMap(event.mMap);
            adapterDoing.notifyItemRangeChanged(0, adapterDoing.dataList.size(), "refresh_speed");
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(TransferSpeedEvent.TransferSpeed event) {
        for (int i = 0; i < adapterDoing.dataList.size(); i++) {
            TransferItem adapterItem = adapterDoing.dataList.get(i);
            if (!TextUtils.isEmpty(event.mKey) && event.mKey.equals(adapterItem.ext1)) {
                Logger.d("zfy","onReceive TransferSpeedEvent.TransferSpeed event "+event.mKey);
                adapterDoing.appendSpeedMap(event.mKey, event.mSpeed);
                adapterDoing.notifyItemChanged(i, "refresh_speed");
                break;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(TransferStateEvent event) {
        if (type != event.transferType) {
            return;
        }
        Logger.d("zfy", "transfer list receive TransferStateEvent:" + event.keyName + ";state=" + event.state + ";unique:" + event.uniqueTag + ";pageTransferType = " + type);
        for (int i = 0; i < adapterDoing.dataList.size(); i++) {
            TransferItem adapterItem = adapterDoing.dataList.get(i);
            Logger.d("zfy", "adapterItem uniqueTag=" + adapterItem.ext1);
            if (!TextUtils.isEmpty(event.uniqueTag) && event.uniqueTag.equals(adapterItem.ext1)) {
                Logger.d("zfy", "uniqueTag match:" + event.uniqueTag);
                if (event.state == TransferHelper.STATE_FINISH) {
                    //传输完成，删除传输中，新增传输完成
                    adapterDoing.dataList.get(i).state = TransferHelper.STATE_FINISH;
                    //删除正在传输
                    adapterDoing.dataList.remove(i);
                    adapterDoing.notifyDataSetChanged();
                    setDoingRecyclerViewHeight();
                    transferringData.remove(i);
                    //新增传输完成
                    adapterDone.dataList.add(0, adapterItem);
                    adapterDone.notifyDataSetChanged();
                    setDoneRecyclerViewHeight();
                    transferredData.add(0, adapterItem);
                    refreshTitleCount();
                } else {
                    adapterDoing.dataList.get(i).state = event.state;
                    adapterDoing.notifyItemChanged(i);
                }
                refreshDoingControlAll();
                break;
            }
        }
        new Handler(Looper.getMainLooper()).postDelayed(()->{
            //判断第一项是否已完成未刷新
            if (adapterDoing.dataList.size() > 0) {
                TransferItem firstItem = adapterDoing.dataList.get(0);
                TransferItem dbItem = TransferDBManager.getInstance(getContext()).queryByUniqueTag(firstItem.ext1, firstItem.transferType);
                if (dbItem != null && dbItem.state == TransferHelper.STATE_FINISH) {
                    //已完成进行刷新
                    firstItem.state = TransferHelper.STATE_FINISH;
                    //删除正在传输
                    adapterDoing.dataList.remove(0);
                    adapterDoing.notifyDataSetChanged();
                    setDoingRecyclerViewHeight();
                    transferringData.remove(firstItem);
                    //新增传输完成
                    adapterDone.dataList.add(0, firstItem);
                    adapterDone.notifyDataSetChanged();
                    setDoneRecyclerViewHeight();
                    transferredData.add(0, firstItem);
                    refreshTitleCount();
                }
            }
        },500);
    }

    //关闭recyclerview默认动画
    public void closeDefaultAnimator(RecyclerView recyclerView) {
        recyclerView.getItemAnimator().setAddDuration(0);
        recyclerView.getItemAnimator().setChangeDuration(0);
        recyclerView.getItemAnimator().setMoveDuration(0);
        recyclerView.getItemAnimator().setRemoveDuration(0);
        ((SimpleItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
    }

    @Override
    public void refreshSelectedCount(int selectedCount) {
        if (selectedCount > 0) {
            layoutDelete.setVisibility(View.VISIBLE);
            adapterDone.setSelectMode(true);
        } else {
            layoutDelete.setVisibility(View.GONE);
        }
        if (getActivity() instanceof TransferListActivity) {
            ((TransferListActivity) getActivity()).refreshTitleBar(selectedCount, selectedCount == transferredData.size(),
                    adapterDone.isSelectMode());
        }
    }

    //获取当前已选择情况
    public void getSelectDetail(SelectedDetailInterface listener) {
        listener.onResult(presenter.dataDoneSelected.size(),
                presenter.dataDoneSelected.size() == transferredData.size(),
                adapterDone.isSelectMode());
    }

    public interface SelectedDetailInterface {
        void onResult(int selectedCount, boolean hasSelectedAll, boolean isSelectMode);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.layout_title_doing:
                changeDoingListVisible();
                break;
            case R.id.layout_title_done:
                changeDoneListVisible();
                break;
            case R.id.btn_delete:
                deleteRecordFromSelect();
                break;
            case R.id.tv_transfer_done_control:
                deleteRecordFromTitleControl();
                break;
            case R.id.tv_transfer_doing_control:
                long clickTime = System.currentTimeMillis();
                if (clickTime - mLastClickAllStateControl < 1000){
                    return;
                }
                mLastClickAllStateControl = clickTime;
                changeAllDoingState();
                break;
            default:
                break;
        }
    }

    //删除已完成记录，选择删除
    private void deleteRecordFromSelect() {
        if (presenter.dataDoneSelected.size() == 0) {
            showPureTextToast(R.string.delete_record_empty_hint);
            return;
        }
        String noticeStr = deleteNoticeText.replace("清空", "删除");
        noticeStr = noticeStr.replace("clear", "delete");
        EulixDialogUtil.showBottomDeleteDialog(getActivity(), noticeStr, (dialog, which) -> {
            //删除数据库并刷新列表
            //排序，从高位置到低位置依次删除
            if (getActivity() instanceof AbsActivity) {
                ((AbsActivity<?, ?>) getActivity()).showLoading("");
            }
            ThreadPool.getInstance().execute(() -> {
                Collections.sort(presenter.selectedPositionList, (o1, o2) -> o2.compareTo(o1)
                );
                for (int i = 0; i < presenter.selectedPositionList.size(); i++) {
                    int removePosition = presenter.selectedPositionList.get(i);
                    TransferItem item = adapterDone.dataList.get(removePosition);
                    TransferDBManager.getInstance(getContext()).deleteByKeyName(item.keyName, item.transferType);
                    adapterDone.dataList.remove(removePosition);
                    TransferItem remove = transferredData.remove(removePosition);
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (getActivity() instanceof AbsActivity) {
                        ((AbsActivity<?, ?>) getActivity()).closeLoading();
                    }
                    adapterDone.setSelectMode(false);
                    adapterDone.notifyDataSetChanged();
                    setDoneRecyclerViewHeight();
                    presenter.dataDoneSelected.clear();
                    presenter.selectedPositionList.clear();
                    refreshSelectedCount(presenter.dataDoneSelected.size());
                    refreshTitleCount();
                });
            });
        }, null);
    }

    //点击“清空记录”删除
    private void deleteRecordFromTitleControl() {
        if (transferredData.isEmpty()) {
            showPureTextToast(R.string.clear_record_empty_hint);
            return;
        }
        EulixDialogUtil.showBottomDeleteDialog(getActivity(), deleteNoticeText, (dialog, which) -> {
            //删除数据库并刷新列表
            if (getActivity() instanceof AbsActivity) {
                ((AbsActivity<?, ?>) getActivity()).showLoading("");
            }
            ThreadPool.getInstance().execute(() -> {
                for (TransferItem item : adapterDone.dataList) {
                    TransferDBManager.getInstance(getContext()).deleteByKeyName(item.keyName, item.transferType);
                }
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (getActivity() instanceof AbsActivity) {
                        ((AbsActivity<?, ?>) getActivity()).closeLoading();
                    }
                    adapterDone.dataList.clear();
                    adapterDone.setSelectMode(false);
                    adapterDone.notifyDataSetChanged();
                    setDoneRecyclerViewHeight();
                    presenter.selectedPositionList.clear();
                    presenter.dataDoneSelected.clear();
                    refreshSelectedCount(presenter.dataDoneSelected.size());
                    transferredData.clear();
                    refreshTitleCount();
                });
            });
        }, null);
    }

    //长按删除单项
    private void deleteRecordFromLongClick(TransferItem itemSelect, int position) {
        String noticeStr = deleteNoticeText.replace("清空", "删除");
        noticeStr = noticeStr.replace("clear", "delete");
        EulixDialogUtil.showBottomDeleteDialog(getActivity(), noticeStr, (dialog, which) -> {
            //删除数据库并刷新列表
            for (int i = 0; i < adapterDone.dataList.size(); i++) {
                TransferItem item = adapterDone.dataList.get(i);
                if (item.keyName.equals(itemSelect.keyName)) {
                    adapterDone.dataList.remove(i);
                    adapterDone.notifyItemRemoved(i);
                    adapterDone.notifyItemRangeChanged(i, adapterDone.dataList.size());
                    setDoneRecyclerViewHeight();
                    transferredData.remove(i);
                    TransferDBManager.getInstance(getContext()).deleteByKeyName(item.keyName, item.transferType);
                    refreshTitleCount();
                }
            }
        }, null);
    }

    //下载中列表展开/收起
    private void changeDoingListVisible() {
        if (isDoingListOpen) {
            imgDoingTitleArrow.setImageResource(R.drawable.icon_transfer_arrow_close);
            recyclerViewDoing.setVisibility(View.GONE);
        } else {
            imgDoingTitleArrow.setImageResource(R.drawable.icon_transfer_arrow_open);
            recyclerViewDoing.setVisibility(View.VISIBLE);
        }
        setDoneLayoutMarginTop();
        isDoingListOpen = !isDoingListOpen;
    }

    private void setDoingRecyclerViewHeight() {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) recyclerViewDoing.getLayoutParams();
        if (adapterDoing.dataList.size() < 9) {
            layoutParams.height = WRAP_CONTENT;
        } else {
            int layoutRecentEmptyHeight = ViewUtils.getScreenHeight(getContext()) - ViewUtils.getStatusBarHeight(getContext()) - ViewUtils.getNavigationBarHeight(getActivity());
            layoutParams.height = layoutRecentEmptyHeight;
        }
        recyclerViewDoing.setLayoutParams(layoutParams);
    }

    private void setDoneRecyclerViewHeight() {
        RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) recyclerViewDone.getLayoutParams();
        if (adapterDone.dataList.size() < 5) {
            layoutParams.height = WRAP_CONTENT;
        } else {
            int layoutRecentEmptyHeight = ViewUtils.getScreenHeight(getContext()) - ViewUtils.getStatusBarHeight(getContext()) - ViewUtils.getNavigationBarHeight(getActivity());
            layoutParams.height = layoutRecentEmptyHeight;
        }
        recyclerViewDone.setLayoutParams(layoutParams);
    }

    //下载完成列表展开/收起
    private void changeDoneListVisible() {
        if (isDoneListOpen) {
            imgDoneTitleArrow.setImageResource(R.drawable.icon_transfer_arrow_close);
            recyclerViewDone.setVisibility(View.GONE);
        } else {
            imgDoneTitleArrow.setImageResource(R.drawable.icon_transfer_arrow_open);
            recyclerViewDone.setVisibility(View.VISIBLE);
        }
        isDoneListOpen = !isDoneListOpen;
    }


    //刷新标题显示下载数量
    private void refreshTitleCount() {
        if (type == ConstantField.TransferType.TYPE_DOWNLOAD) {
            String downloadingContent = getString(R.string.downloading) + getString(R.string.common_language_space)
                    + getString(R.string.left_bracket) + transferringData.size() + getString(R.string.right_bracket);
            String downloadedContent = getString(R.string.download_complete) + getString(R.string.common_language_space)
                    + getString(R.string.left_bracket) + transferredData.size() + getString(R.string.right_bracket);
            tvDoingTitle.setText(downloadingContent);
            tvDoneTitle.setText(downloadedContent);
        } else if (type == ConstantField.TransferType.TYPE_UPLOAD) {
            String uploadingContent = getString(R.string.uploading) + getString(R.string.common_language_space)
                    + getString(R.string.left_bracket) + transferringData.size() + getString(R.string.right_bracket);
            String uploadedContent = getString(R.string.upload_complete) + getString(R.string.common_language_space)
                    + getString(R.string.left_bracket) + transferredData.size() + getString(R.string.right_bracket);
            tvDoingTitle.setText(uploadingContent);
            tvDoneTitle.setText(uploadedContent);
        }

        if (transferredData.size() + transferringData.size() == 0) {
            scrollviewList.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
        }
        if (transferringData.size() == 0) {
            setDoneLayoutMarginTop();
        }
        if (transferredData.size() == 0) {
            tvClearDoneRecord.setVisibility(View.GONE);
        } else {
            tvClearDoneRecord.setVisibility(View.VISIBLE);
        }
    }

    //根据下载列表是否为空或是否隐藏，动态设置下载完成layout顶部margin
    private void setDoneLayoutMarginTop() {
        boolean hasMargin = recyclerViewDoing.getVisibility() != View.VISIBLE || transferringData.isEmpty();
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) layoutDone.getLayoutParams();
        if (hasMargin) {
            layoutParams.topMargin = getContext().getResources().getDimensionPixelOffset(R.dimen.dp_28);
        } else {
            layoutParams.topMargin = 0;
        }
        layoutDone.setLayoutParams(layoutParams);
    }


    public void onTitleBarStateChange(int clickEvent) {
        switch (clickEvent) {
            case TitleBarWithSelect.CLICK_EVENT_CANCEL_SELECT:
                clearSelectedItem();
                break;
            case TitleBarWithSelect.CLICK_EVENT_SELECT_ALL:
                presenter.dataDoneSelected.clear();
                presenter.dataDoneSelected.addAll(transferredData);
                presenter.selectedPositionList.clear();
                for (int i = 0; i < transferredData.size(); i++) {
                    presenter.selectedPositionList.add(i);
                }
                refreshSelectedCount(presenter.dataDoneSelected.size());
                adapterDone.setSelectAll(true);
                adapterDone.notifyDataSetChanged();
                break;
            case TitleBarWithSelect.CLICK_EVENT_SELECT_NULL:
                Logger.d("zfy", "onSelectNone");
                presenter.dataDoneSelected.clear();
                presenter.selectedPositionList.clear();
                refreshSelectedCount(presenter.dataDoneSelected.size());
                adapterDone.setSelectAll(false);
                adapterDone.notifyDataSetChanged();
                break;
            default:
                break;
        }
    }

    //取消选中状态
    public void clearSelectedItem(){
        presenter.dataDoneSelected.clear();
        adapterDone.setSelectMode(false);
        refreshSelectedCount(presenter.dataDoneSelected.size());
        adapterDone.setSelectAll(false);
        adapterDone.notifyDataSetChanged();
        layoutDelete.setVisibility(View.GONE);
    }

    @Override
    public void onDestroyView() {
        EventBusUtil.unRegister(this);
        super.onDestroyView();
    }
}
