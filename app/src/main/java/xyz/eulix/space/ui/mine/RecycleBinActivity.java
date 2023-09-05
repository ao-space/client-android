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

package xyz.eulix.space.ui.mine;

import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import xyz.eulix.space.EulixSpaceService;
import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.adapter.files.FileAdapter;
import xyz.eulix.space.bean.ArrayStack;
import xyz.eulix.space.bean.CustomizeFile;
import xyz.eulix.space.event.RestoreRecycledEvent;
import xyz.eulix.space.manager.ThumbManager;
import xyz.eulix.space.network.files.PageInfo;
import xyz.eulix.space.presenter.RecycleBinPresenter;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.OperationUtil;
import xyz.eulix.space.util.StatusBarUtil;
import xyz.eulix.space.util.ViewUtils;
import xyz.eulix.space.view.dialog.file.FileEditView;
import xyz.eulix.space.view.rv.FooterView;
import xyz.eulix.space.view.rv.HeaderFooterWrapper;

/**
 * @author: chenjiawei
 * Description:
 * date: 2022/3/10 16:00
 */
public class RecycleBinActivity extends AbsActivity<RecycleBinPresenter.IRecycleBin, RecycleBinPresenter> implements RecycleBinPresenter.IRecycleBin
        , View.OnClickListener, FileAdapter.OnItemClickListener, FileEditView.FileEditCallback, FileEditView.FileEditRecycleBinCallback {
    private RelativeLayout titleContainer;
    private ImageButton back;
    private TextView title;
    private Button functionText;
    private LinearLayout selectContainer;
    private Button cancel;
    private TextView fileSelect;
    private Button select;
    private SwipeRefreshLayout swipeRefreshContainer;
    private RelativeLayout exceptionContainer;
    private LinearLayout networkExceptionContainer;
    private Button refreshNow;
    private LinearLayout status404Container;
    private LinearLayout emptyRecycleBinContainer;
    private FrameLayout fileSubViewContainer;
    private LinearLayout fileEditContainer;
    private RecyclerView fileList;
    private RecycleBinHandler mHandler;
    private FileEditView fileEditView;
    private FileAdapter mAdapter;
    private HeaderFooterWrapper headerFooterWrapper;
    private FooterView footer;
    private List<CustomizeFile> customizeFiles;
    private List<String> selectIds;
    private int mPage = 1;
    private int mTotalPage = 1;
    private int maxChildCount = 7;
    // 上拉加载使能，本地加载时失效，网络加载到来之后生效
    private boolean isLoadingEnable = false;
    // 本地加载是否存在，不存在则展示网络错误，刷新时强制为true
    private boolean isLocalEmpty = true;

    private HeaderFooterWrapper.ILoadMore loadMore = new HeaderFooterWrapper.ILoadMore() {
        @Override
        public void loadMore() {
            if (isLoadingEnable && presenter != null && mPage < mTotalPage) {
                presenter.getRecycleBinFile((mPage + 1));
            }
        }
    };

    private static class RecycleBinHandler extends Handler {
        private WeakReference<RecycleBinActivity> recycleBinActivityWeakReference;

        public RecycleBinHandler(RecycleBinActivity activity) {
            recycleBinActivityWeakReference = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            RecycleBinActivity activity = recycleBinActivityWeakReference.get();
            if (activity == null) {
                super.handleMessage(msg);
            } else {
                switch (msg.what) {
                    default:
                        super.handleMessage(msg);
                        break;
                }
            }
        }
    }

    @Override
    public void initView() {
        setContentView(R.layout.activity_recycle_bin);
        titleContainer = findViewById(R.id.title_container);
        back = findViewById(R.id.back);
        title = findViewById(R.id.title);
        functionText = findViewById(R.id.function_text);
        selectContainer = findViewById(R.id.select_container);
        cancel = findViewById(R.id.cancel);
        fileSelect = findViewById(R.id.file_select);
        select = findViewById(R.id.select);
        swipeRefreshContainer = findViewById(R.id.swipe_refresh_container);
        exceptionContainer = findViewById(R.id.exception_container);
        networkExceptionContainer = findViewById(R.id.network_exception_container);
        refreshNow = findViewById(R.id.refresh_now);
        status404Container = findViewById(R.id.status_404_container);
        emptyRecycleBinContainer = findViewById(R.id.empty_recycle_bin_container);
        fileSubViewContainer = findViewById(R.id.file_sub_view_container);
        fileEditContainer = findViewById(R.id.file_edit_container);
        functionText.setText(R.string.clean_up);
    }

    @Override
    public void initData() {
        StatusBarUtil.setStatusBarColor(Color.WHITE, this);
        mHandler = new RecycleBinHandler(this);
        customizeFiles = new ArrayList<>();
    }

    @Override
    public void initViewData() {
        title.setText(R.string.recycle_bin);
        fileEditView = new FileEditView(this, null);
        maxChildCount = Math.max((int) (Math.ceil((ViewUtils.getScreenHeight(this) - ViewUtils.getStatusBarHeight(this))
                * 1.0 / getResources().getDimensionPixelSize(R.dimen.dp_80))), maxChildCount);
    }

    @Override
    public void initEvent() {
        fileEditView.registerCallback(this);
        fileEditView.registerRecycleBinCallback(this);
        back.setOnClickListener(this);
        functionText.setOnClickListener(this);
        cancel.setOnClickListener(this);
        select.setOnClickListener(this);
        refreshNow.setOnClickListener(this);
        if (swipeRefreshContainer != null) {
            swipeRefreshContainer.setOnRefreshListener(() -> {
                ThumbManager.getInstance().cancelCache();
                isLocalEmpty = true;
                refreshEulixSpaceRecycleBin();
            });
        }
        getLocalEulixSpaceStorage(true);
        refreshEulixSpaceRecycleBin();
    }

    @NotNull
    @Override
    public RecycleBinPresenter createPresenter() {
        return new RecycleBinPresenter();
    }

    private void addFileSubView() {
        View view = LayoutInflater.from(this).inflate(R.layout.file_sub_view, null);
        fileList = view.findViewById(R.id.file_list);
        customizeFiles = new ArrayList<>();
        mAdapter = new FileAdapter(this, customizeFiles, ConstantField.ViewType.BOX_FILE_LINEAR_VIEW, false, true);
        mAdapter.setOnItemClickListener(this);
        fileList.setLayoutManager(new LinearLayoutManager(this, RecyclerView.VERTICAL, false));
        fileList.addItemDecoration(new FileAdapter.ItemDecoration(RecyclerView.VERTICAL, Math.round(getResources().getDimension(R.dimen.dp_1))
                , getResources().getColor(R.color.white_fff7f7f9)));
        headerFooterWrapper = new HeaderFooterWrapper(mAdapter);
        fileList.setAdapter(headerFooterWrapper);
        footer = new FooterView(this);
        mAdapter.setFooterView(footer);
        if (fileSubViewContainer != null) {
            int childCount = fileSubViewContainer.getChildCount();
            if (childCount > 0) {
                View childView = fileSubViewContainer.getChildAt((childCount - 1));
                if (childView != null) {
                    childView.setVisibility(View.GONE);
                }
            }
            view.setVisibility(View.VISIBLE);
            fileSubViewContainer.addView(view, childCount);
        }
    }

    private void removeFileSubView() {
        if (fileSubViewContainer != null) {
            int childCount = fileSubViewContainer.getChildCount();
            if (childCount > 1) {
                View childView = fileSubViewContainer.getChildAt((childCount - 2));
                if (childView != null) {
                    fileList = childView.findViewById(R.id.file_list);
                    if (fileList != null) {
                        if (fileList.getAdapter() != null && fileList.getAdapter() instanceof HeaderFooterWrapper) {
                            headerFooterWrapper = (HeaderFooterWrapper) fileList.getAdapter();
                            if (headerFooterWrapper != null && headerFooterWrapper.dataAdapter != null && headerFooterWrapper.dataAdapter instanceof FileAdapter) {
                                mAdapter = (FileAdapter) headerFooterWrapper.dataAdapter;
                                customizeFiles = mAdapter.getmCustomizeFileList();
                                footer = mAdapter.getFooterView();
                            }
                        }
                    }
                    childView.setVisibility(View.VISIBLE);
                }
                fileSubViewContainer.removeViewAt((childCount - 1));
            }
        }
    }

    private void resetFileSubView() {
        if (fileSubViewContainer != null) {
            fileSubViewContainer.removeAllViews();
        }
    }

    private void setFooter(boolean isAdd, boolean isForce) {
        if (isAdd) {
            if (headerFooterWrapper != null && headerFooterWrapper.getFooterViewSize() <= 0 && fileList != null && footer != null
                    && (isForce || (fileList.canScrollVertically(-1) || fileList.canScrollVertically(1)))) {
                // 只添加一次footer
                headerFooterWrapper.addFooterView(footer, loadMore);
            }
        } else {
            if (headerFooterWrapper != null && headerFooterWrapper.getFooterViewSize() > 0) {
                headerFooterWrapper.removeAllFooters();
            }
        }
    }

    private void setFooterVisible(boolean isVisible) {
        if (footer != null) {
            ViewGroup.LayoutParams param = footer.getLayoutParams();

            if (isVisible) {
                param.width = ViewGroup.LayoutParams.MATCH_PARENT;
                param.height = getResources().getDimensionPixelSize(R.dimen.dp_33);
            } else {
                param.width = 0;
                param.height = 0;
            }
            footer.setLayoutParams(param);
        }
    }

    private void getRecycleBin() {
        if (presenter != null) {
            presenter.getRecycleBinFile(1);
        }
    }

    private void handleDataResult(int statusCode) {
        switch (statusCode) {
            case -3:
                functionText.setVisibility(View.VISIBLE);
                networkExceptionContainer.setVisibility(View.INVISIBLE);
                status404Container.setVisibility(View.INVISIBLE);
                emptyRecycleBinContainer.setVisibility(View.INVISIBLE);
                exceptionContainer.setVisibility(View.GONE);
                fileSubViewContainer.setVisibility(View.VISIBLE);
                break;
            case -2:
                functionText.setVisibility(View.GONE);
                fileSubViewContainer.setVisibility(View.INVISIBLE);
                exceptionContainer.setVisibility(View.VISIBLE);
                networkExceptionContainer.setVisibility(View.INVISIBLE);
                status404Container.setVisibility(View.INVISIBLE);
                emptyRecycleBinContainer.setVisibility(View.VISIBLE);
                break;
            case ConstantField.OBTAIN_ACCESS_TOKEN_CODE:
                obtainAccessToken();
                break;
            case ConstantField.FILE_DISCONNECT_CODE:
                if (customizeFiles == null || customizeFiles.size() <= 0) {
                    handleDataResult(-2);
                }
                showImageTextToast(R.drawable.toast_refuse, R.string.active_device_offline_hint);
                break;
            case ConstantField.SERVER_EXCEPTION_CODE:
                if (customizeFiles == null || customizeFiles.size() <= 0) {
                    handleDataResult(-2);
                }
                showServerExceptionToast();
                break;
            case ConstantField.NETWORK_ERROR_CODE:
                fileSubViewContainer.setVisibility(View.GONE);
                exceptionContainer.setVisibility(View.VISIBLE);
                networkExceptionContainer.setVisibility(View.VISIBLE);
                status404Container.setVisibility(View.INVISIBLE);
                emptyRecycleBinContainer.setVisibility(View.INVISIBLE);
                break;
            default:
                if (customizeFiles != null) {
                    customizeFiles.clear();
                }
                if (mAdapter != null && headerFooterWrapper != null) {
                    mAdapter.updateData(customizeFiles, false);
                    headerFooterWrapper.notifyDataSetChanged();
                }
                functionText.setVisibility(View.GONE);
                fileSubViewContainer.setVisibility(View.GONE);
                exceptionContainer.setVisibility(View.VISIBLE);
                networkExceptionContainer.setVisibility(View.INVISIBLE);
                status404Container.setVisibility(View.VISIBLE);
                emptyRecycleBinContainer.setVisibility(View.INVISIBLE);
                break;
        }
    }

    private void changeEditAdapterView(int showType) {
        if (mAdapter != null && fileList != null) {
            mAdapter.changeEditStatus(showType);
            int visibleCount = fileList.getChildCount();
            for (int i = 0; i < visibleCount; i++) {
                View child = fileList.getChildAt(i);
                if (child != null) {
                    switch (mAdapter.getItemViewType(0)) {
                        case ConstantField.ViewType.BOX_FILE_LINEAR_VIEW:
                            mAdapter.setMarkPattern(child, showType);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    private void openRecycleDirectory(List<CustomizeFile> customizeFileList) {
        setFooter(false, true);
        List<Integer> selectionPositionList = null;
        if (customizeFileList != null) {
            mPage = 1;
            customizeFiles = customizeFileList;
            if (mAdapter != null && headerFooterWrapper != null) {
                if (selectIds == null) {
                    mAdapter.updateData(customizeFiles, false);
                } else {
                    selectionPositionList = new ArrayList<>();
                    int size = customizeFileList.size();
                    for (int i = 0; i < size; i++) {
                        CustomizeFile customizeFile = customizeFileList.get(i);
                        if (customizeFile != null) {
                            String id = customizeFile.getId();
                            if (id != null && selectIds.contains(id)) {
                                selectionPositionList.add(i);
                            }
                        }
                    }
                    mAdapter.updateData(customizeFiles, selectionPositionList);
                }
                headerFooterWrapper.notifyDataSetChanged();
            }
            if (selectionPositionList == null) {
                handleTitleVisibility(-1, 0);
            } else {
                handleTitleVisibility(selectionPositionList.size(), customizeFileList.size());
            }
            handleDataResult(customizeFileList.size() <= 0 ? -2 : -3);
        } else {
            handleTitleVisibility(-1, 0);
            handleDataResult(-5);
        }
    }

    private void addRecycleDirectory(List<CustomizeFile> customizeFileList, PageInfo pageInfo) {
        if (pageInfo != null && pageInfo.getPage() != null) {
            List<Integer> selectionPositionList = null;
            mPage = Math.max(mPage, pageInfo.getPage());
//            List<String> currentIds = new ArrayList<>();
            if (customizeFiles != null) {
//                for (CustomizeFile customizeFile : customizeFiles) {
//                    if (customizeFile != null) {
//                        currentIds.add(customizeFile.getId());
//                    }
//                }
                if (customizeFileList != null) {
                    //                        if (customizeFile != null && !currentIds.contains(customizeFile.getId())) {
                    //                            customizeFiles.add(customizeFile);
                    //                        }
                    customizeFiles.addAll(customizeFileList);
                }
                if (mAdapter != null && headerFooterWrapper != null) {
                    if (selectIds == null) {
                        mAdapter.updateData(customizeFiles, false);
                    } else {
                        selectionPositionList = new ArrayList<>();
                        int size = customizeFiles.size();
                        for (int i = 0; i < size; i++) {
                            CustomizeFile customizeFile = customizeFiles.get(i);
                            if (customizeFile != null) {
                                String id = customizeFile.getId();
                                if (id != null && selectIds.contains(id)) {
                                    selectionPositionList.add(i);
                                }
                            }
                        }
                        mAdapter.updateData(customizeFiles, selectionPositionList);
                    }
                    headerFooterWrapper.notifyDataSetChanged();
                }
            }
            if (selectionPositionList == null) {
                if (mAdapter != null && customizeFiles != null) {
                    List<Integer> selectPosition = mAdapter.getSelectPosition();
                    handleTitleVisibility((selectPosition == null ? -1 : selectPosition.size()), customizeFiles.size());
                }
            } else {
                handleTitleVisibility(selectionPositionList.size(), customizeFiles.size());
            }
        }
    }

    private void handleSwipeEnable() {
        if (swipeRefreshContainer != null) {
            swipeRefreshContainer.setEnabled(selectIds == null);
        }
    }

    private void handleFileEditView(int selectNumber) {
        if (fileEditView != null) {
            if (selectNumber > 0) {
                fileEditView.showFileRecycleDialog();
            } else {
                fileEditView.dismissFileRecycleDialog();
            }
        }
    }

    private void handleSelectTotal() {
        if (mAdapter != null && customizeFiles != null) {
            int selectNumber = -1;
            List<Integer> selectPosition = mAdapter.getSelectPosition();
            if (selectPosition != null) {
                selectNumber = selectPosition.size();
            }
            int totalNumber = customizeFiles.size();
            if (selectNumber < totalNumber) {
                if (selectIds == null) {
                    selectIds = new ArrayList<>();
                }
                for (CustomizeFile customizeFile : customizeFiles) {
                    if (customizeFile != null) {
                        selectIds.add(customizeFile.getId());
                    }
                }
                changeEditAdapterView(ConstantField.ShowType.SELECT);
                mAdapter.changeEditStatus(ConstantField.ShowType.SELECT);
                handleTitleVisibility(totalNumber, totalNumber);
            } else {
                if (selectIds != null) {
                    selectIds.clear();
                }
                changeEditAdapterView(ConstantField.ShowType.EDIT);
                mAdapter.changeEditStatus(ConstantField.ShowType.EDIT);
                handleTitleVisibility(0);
            }
            handleSwipeEnable();
        }
    }

    private void handleTitleVisibility(int selectNumber) {
        handleTitleVisibility(selectNumber, (selectNumber + 1));
    }

    private void handleTitleVisibility(String titleText) {
        handleTitleVisibility(-1, 0, titleText);
    }

    private void handleTitleVisibility(int selectNumber, int totalNumber) {
        handleTitleVisibility(selectNumber, totalNumber, null);
    }

    private void handleTitleVisibility(int selectNumber, int totalNumber, String titleText) {
        titleContainer.setVisibility(selectNumber < 0 ? View.VISIBLE : View.GONE);
//        if (titleText != null) {
//            title.setText(titleText);
//        }
        selectContainer.setVisibility(selectNumber >= 0 ? View.VISIBLE : View.GONE);
        if (fileSelect != null) {
            String fileSelectNumber = getString(R.string.choose_file_part_1) + selectNumber +
                    getString((Math.abs(selectNumber) == 1) ? R.string.choose_file_part_2_singular : R.string.choose_file_part_2_plural);
            fileSelect.setText(fileSelectNumber);
        }
        if (select != null) {
            select.setText(selectNumber < totalNumber ? R.string.select_all : R.string.select_none);
        }
        handleFileEditView(selectNumber);
    }

    private CustomizeFile getSelectFile() {
        CustomizeFile customizeFile = null;
        List<CustomizeFile> customizeFiles = getSelectFiles();
        if (customizeFiles != null && customizeFiles.size() == 1) {
            customizeFile = customizeFiles.get(0);
        }
        return customizeFile;
    }

    private void handleRecyclePageInfo(PageInfo pageInfo, List<CustomizeFile> customizeFileList) {
        Integer pageValue = pageInfo.getPage();
        Integer totalPageValue = pageInfo.getTotal();
        if (totalPageValue != null) {
            mTotalPage = totalPageValue;
        }
        if (pageValue != null && pageValue > 1) {
            addRecycleDirectory(customizeFileList, pageInfo);
        } else {
            openRecycleDirectory(customizeFileList);
        }
        if (mTotalPage > 1) {
            Integer pageSizeValue = pageInfo.getPageSize();
            if (pageSizeValue != null) {
                int pageSize = pageSizeValue;
                if (pageSize <= 0) {
                    pageSize = customizeFileList.size();
                }
                pageSize = Math.max(pageSize, 1);
                if (presenter != null && (pageSize * mPage) <= maxChildCount && mPage < mTotalPage) {
                    presenter.getRecycleBinFile((mPage + 1));
                }
            }
        }
        if (mPage == mTotalPage) {
            if (fileList != null && (!fileList.canScrollVertically(-1) && !fileList.canScrollVertically(1))) {
                setFooterVisible(false);
            } else if (footer != null) {
                setFooterVisible(true);
                footer.showBottom(getString(R.string.home_bottom_flag));
            }
        } else {
            setFooterVisible(true);
            if (footer != null) {
                footer.showLoading();
            }
        }
    }

    private void obtainAccessToken() {
        Intent serviceIntent = new Intent(RecycleBinActivity.this, EulixSpaceService.class);
        serviceIntent.setAction(ConstantField.Action.TOKEN_ACTION);
        startService(serviceIntent);
    }

    public void getLocalEulixSpaceStorage(boolean isInit) {
        isLoadingEnable = false;
        if (isInit) {
            if (selectIds != null) {
                selectIds.clear();
                selectIds = null;
            }
            handleSwipeEnable();
            if (swipeRefreshContainer != null) {
                swipeRefreshContainer.setRefreshing(false);
            }
            addFileSubView();
        }
        if (presenter != null) {
            List<CustomizeFile> customizeFiles = presenter.getLocalEulixSpaceStorage();
            isLocalEmpty = (customizeFiles == null || customizeFiles.size() <= 0);
            openRecycleDirectory(customizeFiles);
        }
        functionText.setVisibility(isLocalEmpty ? View.GONE : View.VISIBLE);
        if (fileList != null && (!fileList.canScrollVertically(-1) && !fileList.canScrollVertically(1))
                && customizeFiles != null && customizeFiles.size() < maxChildCount) {
            setFooter(true, false);
            setFooterVisible(false);
        } else if (footer != null) {
            setFooter(true, true);
            setFooterVisible(true);
            footer.showBottom(getString(R.string.home_bottom_flag));
        }
    }

    private void refreshEulixSpaceRecycleBin() {
        getRecycleBin();
    }

    @Override
    protected void onDestroy() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        ThumbManager.getInstance().cancelCache();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (!handleRecycleBackEvent()) {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return ((keyCode == KeyEvent.KEYCODE_BACK && handleRecycleBackEvent()) || super.onKeyDown(keyCode, event));
    }

    @Override
    public void recycleBinFileResult(Integer code, List<CustomizeFile> customizeFileList, PageInfo pageInfo) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (swipeRefreshContainer != null) {
                    swipeRefreshContainer.setRefreshing(false);
                }
                isLoadingEnable = true;
                if (customizeFileList == null) {
                    if (isLocalEmpty) {
                        handleDataResult(code);
                    }
                    getLocalEulixSpaceStorage(false);
                    if (fileList != null && (!fileList.canScrollVertically(-1) && !fileList.canScrollVertically(1))) {
                        setFooterVisible(false);
                    } else if (footer != null) {
                        setFooterVisible(true);
                        footer.showBottom(getString(R.string.home_bottom_flag));
                    }
                } else {
                    if (pageInfo != null) {
                        handleRecyclePageInfo(pageInfo, customizeFileList);
                    } else {
                        mTotalPage = 1;
                        closeLoading();
                        openRecycleDirectory(customizeFileList);
                    }
                }
                setFooter(true, false);
            });
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.back:
                    if (!handleRecycleBackEvent()) {
                        finish();
                    }
                    break;
                case R.id.function_text:
                    if (fileEditView != null) {
                        fileEditView.showClearCompleteDialog();
                    }
                    break;
                case R.id.cancel:
                    handleRecycleBackEvent();
                    break;
                case R.id.select:
                    handleSelectTotal();
                    break;
                case R.id.refresh_now:
                    isLocalEmpty = true;
                    refreshEulixSpaceRecycleBin();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onItemClick(View view, int position, boolean isEnable) {
        if (mAdapter != null && customizeFiles != null) {
            List<Integer> selectPosition = mAdapter.getSelectPosition();
            int selectNumber = -1;
            if (selectPosition != null) {
                selectNumber = selectPosition.size();
            }
            handleTitleVisibility(selectNumber, customizeFiles.size());
            if (customizeFiles != null) {
                if (selectIds == null) {
                    selectIds = new ArrayList<>();
                } else {
                    selectIds.clear();
                }
                if (selectPosition != null) {
                    for (int selectP : selectPosition) {
                        if (selectP >= 0 && customizeFiles.size() > selectP) {
                            CustomizeFile customizeFile = customizeFiles.get(selectP);
                            if (customizeFile != null) {
                                selectIds.add(customizeFile.getId());
                            }
                        }
                    }
                }
            }
            handleSwipeEnable();
        }
    }

    @Override
    public void handleRefresh(boolean isSuccess, String serviceFunction) {
        if (isSuccess && mHandler != null && serviceFunction != null) {
            switch (serviceFunction) {
                case ConstantField.ServiceFunction.RESTORE_RECYCLED:
                    OperationUtil.setAllFileRefresh(true);
                    mHandler.post(this::refreshEulixSpaceRecycleBin);
                    EventBusUtil.post(new RestoreRecycledEvent());
                    break;
                case ConstantField.ServiceFunction.CLEAR_RECYCLED:
                    mHandler.post(this::refreshEulixSpaceRecycleBin);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public List<CustomizeFile> getSelectFiles() {
        List<CustomizeFile> files = new ArrayList<>();
        if (mAdapter != null && customizeFiles != null) {
            List<Integer> selectIndex = mAdapter.getSelectPosition();
            if (selectIndex != null) {
                for (int i : selectIndex) {
                    if (i >= 0 && customizeFiles.size() > i) {
                        files.add(customizeFiles.get(i));
                    }
                }
            }
        }
        return files;
    }

    @Override
    public void fileDialog(View view, boolean isShow) {
        if (fileEditContainer != null) {
            fileEditContainer.removeAllViews();
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            fileEditContainer.addView(view, layoutParams);
            fileEditContainer.setVisibility(isShow ? View.VISIBLE : View.GONE);
        }
    }

    @Override
    public ArrayStack<UUID> handleCurrentUUIDStack() {
        return null;
    }

    @Override
    public boolean handleRecycleBackEvent() {
        if (mAdapter == null) {
            return false;
        }
        if (mAdapter.getSelectPosition() != null) {
            if (mHandler != null) {
                mHandler.post(() -> {
                    changeEditAdapterView(ConstantField.ShowType.NORMAL);
                    mAdapter.changeEditStatus(ConstantField.ShowType.NORMAL);
                    handleTitleVisibility(-1);
                });
            }
            if (selectIds != null) {
                selectIds.clear();
                selectIds = null;
            }
            handleSwipeEnable();
            return true;
        }
        return false;
    }
}
