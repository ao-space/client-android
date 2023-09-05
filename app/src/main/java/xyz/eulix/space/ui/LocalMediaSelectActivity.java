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

package xyz.eulix.space.ui;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import xyz.eulix.space.EulixSpaceService;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.adapter.LocalMediaSelectAdapter;
import xyz.eulix.space.bean.ArrayStack;
import xyz.eulix.space.bean.LocalMediaUpItem;
import xyz.eulix.space.bean.PhotoUpImageBucket;
import xyz.eulix.space.manager.AlreadyUploadedManager;
import xyz.eulix.space.manager.LocalMediaCacheManager;
import xyz.eulix.space.manager.TransferTaskManager;
import xyz.eulix.space.presenter.LocalMediaSelectPresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.NetUtils;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.ThreadPool;
import xyz.eulix.space.view.TitleBarWithSelect;
import xyz.eulix.space.view.dialog.EulixDialogUtil;
import xyz.eulix.space.view.dialog.folder.FolderListView;
import xyz.eulix.space.view.dragselect.DragSelectTouchListener;

/**
 * Author:      Zhu Fuyu
 * Description: 本地媒体文件选择页面
 * History:     2021/7/22
 */
public class LocalMediaSelectActivity extends AbsActivity<LocalMediaSelectPresenter.ILocalMediaSelect, LocalMediaSelectPresenter> implements LocalMediaSelectPresenter.ILocalMediaSelect, FolderListView.FolderListCallback {
    private static final String SPLIT_1 = "/";
    private static final String PATH = "path";
    private TitleBarWithSelect titleBar;
    private TextView btnUpload, tvNoDataDesc;
    private RecyclerView recyclerView;
    private LinearLayout layoutOnlyNotUpload;
    private CheckBox checkBoxShowOnlyNoUploaded;
    //所有图片数据列表
    private List<LocalMediaUpItem> dataList;
    private RelativeLayout layouNoData;
    private TextView tvUploadLocation, tvLocationTitle;
    private ImageView btnRightArrow;
    private TextView tvSizeLimit;
    private TextView tvAlbumPath;

    private PhotoUpImageBucket photoUpImageBucket;
    private LocalMediaSelectAdapter adapter;
    private int mediaType;
    private String typeSuffix;
    private int typeSuffixIndex;
    private String mPath;
    private FolderListView folderListView;
    private String bucketName;
    //页面埋点名称
    private String mLogUpPageName;

    public static final int RESULT_CODE = 11;
    //相簿id
    private int mAlbumId = -1;
    private boolean isAlbumStyle = false;

    private DragSelectTouchListener dragSelectTouchListener;

    private View.OnClickListener showFolderListListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (folderListView != null) {
                folderListView.showFolderListDialog(null);
            }
        }
    };

    @Override
    protected void resetStatusBar() {
        StatusBarUtil.setStatusBarColor(ContextCompat.getColor(this, R.color.white_ffffffff), this);
    }

    @Override
    public void initView() {
        setContentView(R.layout.activity_image_select);

        titleBar = findViewById(R.id.title_bar);
        btnUpload = findViewById(R.id.btn_upload);
        recyclerView = findViewById(R.id.rv_image_select);
        layoutOnlyNotUpload = findViewById(R.id.layout_only_not_upload);
        checkBoxShowOnlyNoUploaded = findViewById(R.id.checkbox_show_only_no_uploaded);
        layouNoData = findViewById(R.id.layout_no_data);
        tvNoDataDesc = findViewById(R.id.tv_no_data_desc);
        tvUploadLocation = findViewById(R.id.tv_upload_location);
        tvLocationTitle = findViewById(R.id.activity_select_tv_location_title);
        btnRightArrow = findViewById(R.id.btn_right_arrow);
        tvSizeLimit = findViewById(R.id.tv_size_limit);
        tvAlbumPath = findViewById(R.id.tv_album_path);

        //设置recyclerview布局
        if (mediaType == ConstantField.MediaType.MEDIA_IMAGE || mediaType == ConstantField.MediaType.MEDIA_VIDEO
                || mediaType == ConstantField.MediaType.MEDIA_IMAGE_AND_VIDEO) {
            recyclerView.setLayoutManager(new GridLayoutManager(this, 4));
        } else {
            LinearLayoutManager layoutManager = new LinearLayoutManager(this);
            layoutManager.setOrientation(RecyclerView.VERTICAL);
            recyclerView.setLayoutManager(layoutManager);
        }
        adapter = new LocalMediaSelectAdapter(this, mediaType);
        adapter.setSelectListener(new LocalMediaSelectAdapter.ImageSelectListener() {
            @Override
            public void onSelectStateChange(LocalMediaUpItem item, int position, boolean isSelect) {
                adapter.dataList.get(position).setSelected(isSelect);
                adapter.notifyItemChanged(position, "refresh_selected");
                presenter.refreshSelectedData(item, isSelect);
            }

            @Override
            public void onItemLongClick(LocalMediaUpItem item, int position) {
                if (dragSelectTouchListener != null) {
                    dragSelectTouchListener.setIsActive(true, position);
                }
            }
        });

        recyclerView.setAdapter(adapter);

        if (mediaType == ConstantField.MediaType.MEDIA_IMAGE || mediaType == ConstantField.MediaType.MEDIA_VIDEO || mediaType == ConstantField.MediaType.MEDIA_IMAGE_AND_VIDEO) {
            dragSelectTouchListener = DragSelectTouchListener.Companion.create(this, adapter, null);
            recyclerView.addOnItemTouchListener(dragSelectTouchListener);
        }

        folderListView = new FolderListView(this);
        folderListView.registerCallback(this);
    }

    @Override
    public void initData() {
        Intent intent = getIntent();
        bucketName = intent.getStringExtra("bucketName");
        mAlbumId = intent.getIntExtra("key_album_id", -1);
        if (mAlbumId > -1) {
            isAlbumStyle = true;
        }
        photoUpImageBucket = (PhotoUpImageBucket) intent.getSerializableExtra("imagelist");
        if (photoUpImageBucket != null) {
            dataList = photoUpImageBucket.getImageList();
        } else {
            dataList = new ArrayList<>();
        }
        mediaType = intent.getIntExtra("mediaType", ConstantField.MediaType.MEDIA_IMAGE);
        if (mediaType == ConstantField.MediaType.MEDIA_IMAGE) {
            typeSuffixIndex = 1;
            typeSuffix = "张图片";
        } else if (mediaType == ConstantField.MediaType.MEDIA_VIDEO) {
            typeSuffixIndex = 2;
            typeSuffix = "个视频";
        } else {
            typeSuffixIndex = 0;
            typeSuffix = "个文件";
        }
        if (intent.hasExtra(PATH)) {
            mPath = intent.getStringExtra(PATH);
        }
    }

    @Override
    public void initViewData() {
        if (!isAlbumStyle && mPath != null) {
            ArrayStack<UUID> uuidStack = null;
            try {
                uuidStack = new Gson().fromJson(mPath, new TypeToken<ArrayStack<UUID>>() {
                }.getType());
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
            if (uuidStack != null && presenter != null) {
                presenter.setUuids(uuidStack);
            }
        }

        String initTitleBarText = getString(R.string.choose_file_part_1) + "0";
        switch (typeSuffixIndex) {
            case 1:
                initTitleBarText = initTitleBarText + getString(R.string.choose_picture_part_2_plural);
                break;
            case 2:
                initTitleBarText = initTitleBarText + getString(R.string.choose_video_part_2_plural);
                break;
            default:
                initTitleBarText = initTitleBarText + getString(R.string.choose_file_part_2_plural);
                break;
        }

        if (isAlbumStyle) {
            tvAlbumPath.setText(mPath);
            tvAlbumPath.setVisibility(View.VISIBLE);
            btnRightArrow.setVisibility(View.GONE);
            tvUploadLocation.setVisibility(View.GONE);
        } else {
            tvUploadLocation.setText(generatePath(getString(R.string.my_space), SPLIT_1));
        }

        titleBar.setTitle(initTitleBarText);

        setUploadBtnStateBySelectedCount(0);

        if (mediaType == ConstantField.MediaType.MEDIA_IMAGE || mediaType == ConstantField.MediaType.MEDIA_IMAGE_AND_VIDEO) {
            showLoading(null);
            LocalMediaCacheManager.getAllMediaListByType(mediaType, new LocalMediaCacheManager.GetAlbumListListener() {
                @Override
                public void onGetAlbumList(List<PhotoUpImageBucket> list, List<LocalMediaUpItem> totalFilesList) {
                    closeLoading();
                    for (PhotoUpImageBucket bucket : list) {
                        if (bucket.getBucketName() != null && bucket.getBucketName().equals(bucketName)) {
                            dataList = bucket.getImageList();
                            break;
                        }
                    }
                    if (dataList.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        tvNoDataDesc.setText(R.string.no_data_file);
                        layouNoData.setVisibility(View.VISIBLE);
                    } else {
                        //数据不为空
                        titleBar.setDefaultShowAllSelect(true);
                        if (isAlbumStyle) {
                            layoutOnlyNotUpload.setVisibility(View.GONE);
                        }
                        setOnlyShowNotUploaded(!isAlbumStyle && checkBoxShowOnlyNoUploaded.isChecked());
                    }
                }
            });
        } else if (mediaType == ConstantField.MediaType.MEDIA_VIDEO || mediaType == ConstantField.MediaType.MEDIA_FILE) {
            showLoading(null);
            LocalMediaCacheManager.getAllMediaListByType(mediaType, new LocalMediaCacheManager.GetAlbumListListener() {
                @Override
                public void onGetAlbumList(List<PhotoUpImageBucket> list, List<LocalMediaUpItem> totalFilesList) {
                    closeLoading();
                    if (totalFilesList.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        if (mediaType == ConstantField.MediaType.MEDIA_VIDEO) {
                            tvNoDataDesc.setText(R.string.no_data_video);
                        } else if (mediaType == ConstantField.MediaType.MEDIA_FILE) {
                            tvNoDataDesc.setText(R.string.no_data_file);
                        }
                        layouNoData.setVisibility(View.VISIBLE);
                    } else {
                        //数据不为空
                        titleBar.setDefaultShowAllSelect(true);
                        dataList = totalFilesList;
                        setOnlyShowNotUploaded(checkBoxShowOnlyNoUploaded.isChecked());
                    }
                }
            });
        }
    }

    @Override
    public void initEvent() {
        titleBar.setClickListener(clickEvent -> {
            switch (clickEvent) {
                case TitleBarWithSelect.CLICK_EVENT_CANCEL_SELECT:
                    presenter.dataSelected.clear();
                    refreshShowCount(presenter.dataSelected.size());
                    for (int i = 0; i < adapter.dataList.size(); i++) {
                        adapter.dataList.get(i).setSelected(false);
//                        adapter.notifyItemChanged(i, "refresh_selected");
                    }
                    adapter.notifyItemRangeChanged(0, adapter.dataList.size(), "refresh_selected");
                    break;
                case TitleBarWithSelect.CLICK_EVENT_SELECT_ALL:
                    presenter.dataSelected.clear();
                    //超过100个，暂时只选择前100
                    if (adapter.dataList.size() <= 100) {
                        presenter.dataSelected.addAll(adapter.dataList);
                        refreshShowCount(presenter.dataSelected.size());
                        for (int i = 0; i < adapter.dataList.size(); i++) {
                            adapter.dataList.get(i).setSelected(true);
                        }
                        adapter.notifyItemRangeChanged(0, adapter.dataList.size(), "refresh_selected");
                    } else {
                        for (int i = 0; i < adapter.dataList.size(); i++) {
                            if (i < 100) {
                                presenter.dataSelected.add(adapter.dataList.get(i));
                                adapter.dataList.get(i).setSelected(true);
                            } else {
                                adapter.dataList.get(i).setSelected(false);
                            }
//                            adapter.notifyItemChanged(i, "refresh_selected");
                        }
                        adapter.notifyItemRangeChanged(0, adapter.dataList.size(), "refresh_selected");

                        refreshShowCount(presenter.dataSelected.size());
                    }
                    break;
                case TitleBarWithSelect.CLICK_EVENT_SELECT_NULL:
                    Logger.d("zfy", "onSelectNone");
                    presenter.dataSelected.clear();
                    refreshShowCount(presenter.dataSelected.size());
                    for (int i = 0; i < adapter.dataList.size(); i++) {
                        adapter.dataList.get(i).setSelected(false);
                    }
                    adapter.notifyItemRangeChanged(0, adapter.dataList.size(), "refresh_selected");
                    break;
                default:
                    break;
            }
        });
        btnUpload.setOnClickListener(v -> {
            if (NetUtils.isMobileNetWork(this) && !ConstantField.sIAllowTransferWithMobileData) {
                EulixDialogUtil.showChooseAlertDialog(this, getResources().getString(R.string.mobile_data_upload),
                        getResources().getString(R.string.mobile_data_upload_desc), getResources().getString(R.string.ok),
                        (dialog, which) -> {
                            ConstantField.sIAllowTransferWithMobileData = true;
                            callUploadManager();
                        }, null);
            } else {
                callUploadManager();
            }

        });

        checkBoxShowOnlyNoUploaded.setOnCheckedChangeListener((buttonView, isChecked) -> {
            //仅显示未上传逻辑，刷新数据
            setOnlyShowNotUploaded(isChecked);
        });
        if (!isAlbumStyle) {
            tvUploadLocation.setOnClickListener(showFolderListListener);
            tvLocationTitle.setOnClickListener(showFolderListListener);
            btnRightArrow.setOnClickListener(showFolderListListener);
        }
    }

    private void callUploadManager() {
        showLoading("");
        ThreadPool.getInstance().execute(() -> {
            ArrayList<LocalMediaUpItem> tmpList = new ArrayList<>();
            tmpList.addAll(presenter.dataSelected);
            for (LocalMediaUpItem item : tmpList) {
                Logger.d("zfy", "upload file path:" + item.getMediaPath());
                int index = item.getMediaPath().lastIndexOf("/");
                String fileName = item.getMediaPath().substring(index + 1);
                String filePath = item.getMediaPath().substring(0, index + 1);
                if (!isAlbumStyle) {
                    TransferTaskManager.getInstance().insertUploadTask(filePath, fileName, generatePath(), false, null);
                } else {
                    TransferTaskManager.getInstance().insertUploadTask(filePath, fileName, "/相册/", false, String.valueOf(mAlbumId));
                }
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                closeLoading();
                presenter.dataSelected.clear();
                showImageTextToast(R.drawable.toast_right, R.string.add_transfer_list);
                //关闭页面
                setResult(RESULT_CODE);
                finish();
            });
        });
    }

    //根据是否只显示未上传，加载数据
    private void setOnlyShowNotUploaded(boolean onlyShowNotUploaded) {
        if (onlyShowNotUploaded) {
            adapter.dataList.clear();
            for (LocalMediaUpItem item : dataList) {
                if (!AlreadyUploadedManager.getInstance().getUploadedMap().containsKey(item.getMediaPath())) {
                    adapter.dataList.add(item);
                }
            }
        } else {
            adapter.dataList.clear();
            adapter.dataList.addAll(dataList);
        }
        if (adapter.dataList.isEmpty()) {
            showEmpty(true);
        } else {
            showEmpty(false);
            adapter.notifyDataSetChanged();
        }
        //取消选择状态
        presenter.dataSelected.clear();
        for (int i = 0; i < adapter.dataList.size(); i++) {
            adapter.dataList.get(i).setSelected(false);
        }
        adapter.notifyDataSetChanged();
        refreshShowCount(presenter.dataSelected.size());
    }

    private void showEmpty(boolean isEmpty) {
        if (isEmpty) {
            recyclerView.setVisibility(View.GONE);
            if (mediaType == ConstantField.MediaType.MEDIA_VIDEO) {
                tvNoDataDesc.setText(R.string.no_data_video);
            } else if (mediaType == ConstantField.MediaType.MEDIA_FILE) {
                tvNoDataDesc.setText(R.string.no_data_file);
            }
            layouNoData.setVisibility(View.VISIBLE);
            titleBar.setDefaultShowAllSelect(false);
        } else {
            layouNoData.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
            titleBar.setDefaultShowAllSelect(true);
        }
    }

    @Override
    protected void onDestroy() {
        if (folderListView != null) {
            folderListView.unregisterCallback();
            folderListView = null;
        }
        super.onDestroy();
    }

    @NotNull
    @Override
    public LocalMediaSelectPresenter createPresenter() {
        return new LocalMediaSelectPresenter();
    }

    @Override
    public void refreshShowCount(int selectedCount) {
        setUploadBtnStateBySelectedCount(selectedCount);
        StringBuilder titleBarTextBuilder = new StringBuilder();
        titleBarTextBuilder.append(getString(R.string.choose_file_part_1));
        if (selectedCount > 999) {
            titleBarTextBuilder.append(getString(R.string.left_bracket));
            titleBarTextBuilder.append("999+");
            titleBarTextBuilder.append(getString(R.string.right_bracket));
        } else {
            titleBarTextBuilder.append(selectedCount);
        }
        switch (typeSuffixIndex) {
            case 1:
                titleBarTextBuilder.append(getString((Math.abs(selectedCount) == 1)
                        ? R.string.choose_picture_part_2_singular : R.string.choose_picture_part_2_plural));
                break;
            case 2:
                titleBarTextBuilder.append(getString((Math.abs(selectedCount) == 1)
                        ? R.string.choose_video_part_2_singular : R.string.choose_video_part_2_plural));
                break;
            default:
                titleBarTextBuilder.append(getString((Math.abs(selectedCount) == 1)
                        ? R.string.choose_file_part_2_singular : R.string.choose_file_part_2_plural));
                break;
        }
        titleBar.setTitle(titleBarTextBuilder.toString());
        titleBar.setSelectState(selectedCount > 0 ? true : false);
        if (selectedCount < adapter.dataList.size()) {
            titleBar.setHasSelectedAll(false);
        } else {
            titleBar.setHasSelectedAll(true);
        }
    }

    //设置上传按钮样式
    private void setUploadBtnStateBySelectedCount(int count) {
        if (count > 0) {
            btnUpload.setEnabled(true);
            btnUpload.setTextColor(getResources().getColor(R.color.white_ffffffff));
            String btnUploadText = getString(R.string.upload) + getString(R.string.common_language_space)
                    + getString(R.string.left_bracket) + (count > 999 ? "999+" : String.valueOf(count))
                    + getString(R.string.right_bracket);
            btnUpload.setText(btnUploadText);
        } else {
            btnUpload.setEnabled(false);
            btnUpload.setTextColor(getResources().getColor(R.color.c_ffbcbfcd));
            btnUpload.setText(R.string.upload);
        }
    }

    private String generatePath(String rootName, String split) {
        StringBuilder locationBuilder = new StringBuilder(rootName);
        Map<String, String> uuidTitleMap = DataUtil.getUuidTitleMap();
        ArrayStack<UUID> uuidStack = null;
        if (presenter != null) {
            uuidStack = presenter.getUuids();
        }
        if (uuidTitleMap != null && uuidStack != null && uuidStack.size() > 1) {
            for (UUID uuid : uuidStack) {
                if (uuid != null) {
                    if (uuidTitleMap.containsKey(uuid.toString())) {
                        locationBuilder.append(split);
                        locationBuilder.append(uuidTitleMap.get(uuid.toString()));
                    } else {
                        if (!ConstantField.UUID.FILE_ROOT_UUID.equals(uuid.toString())) {
                            locationBuilder.append(split);
                        }
                    }
                }
            }
        }
        return locationBuilder.toString();
    }

    /**
     * 上传文件所用的path
     *
     * @return
     */
    private String generatePath() {
        StringBuilder pathBuilder = new StringBuilder("/");
        Map<String, String> uuidTitleMap = DataUtil.getUuidTitleMap();
        ArrayStack<UUID> uuidStack = null;
        if (presenter != null) {
            uuidStack = presenter.getUuids();
        }
        if (uuidStack != null) {
            int size = uuidStack.size();
            if (size > 1) {
                for (int i = 1; i < size; i++) {
                    UUID uuid = uuidStack.get(i);
                    String title = "";
                    if (uuid != null && uuidTitleMap != null && uuidTitleMap.containsKey(uuid.toString())) {
                        title = uuidTitleMap.get(uuid.toString());
                    }
                    pathBuilder.append(title);
                    pathBuilder.append("/");
                }
            }
        }
        return pathBuilder.toString();
    }

    @Override
    public void obtainAccessToken() {
        Intent serviceIntent = new Intent(LocalMediaSelectActivity.this, EulixSpaceService.class);
        serviceIntent.setAction(ConstantField.Action.TOKEN_ACTION);
        startService(serviceIntent);
    }

    @Override
    public int getSelectFilesSize() {
        return presenter.dataSelected.size();
    }

    @Override
    public UUID getCurrentFolderUUID() {
        return null;
    }

    @Override
    public ArrayStack<UUID> getCurrentUUIDStack() {
        return (presenter == null ? DataUtil.getUuidStack() : presenter.getUuids());
    }

    @Override
    public void refreshEulixSpaceStorage(UUID parentUUID) {
        // Do nothing
    }

    @Override
    public void dismissFolderListView(boolean isConfirm, UUID selectUUID, Boolean isCopy, ArrayStack<UUID> uuids, List<UUID> newFolderUUIDs) {
        if (isConfirm && isCopy == null) {
            DataUtil.setUuidStack(uuids);
            if (presenter != null) {
                presenter.setUuids(uuids);
            }
            if (tvUploadLocation != null) {
                tvUploadLocation.setText(generatePath(getString(R.string.my_space), SPLIT_1));
            }
        }
    }
}