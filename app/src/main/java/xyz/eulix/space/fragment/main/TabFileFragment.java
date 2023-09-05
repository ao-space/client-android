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

package xyz.eulix.space.fragment.main;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.google.android.material.tabs.TabLayout;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsFragment;
import xyz.eulix.space.adapter.files.FilePagerAdapter;
import xyz.eulix.space.bean.ArrayStack;
import xyz.eulix.space.bean.CustomizeFile;
import xyz.eulix.space.bean.EulixBoxBaseInfoCompatible;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.event.BoxStatusEvent;
import xyz.eulix.space.event.DeleteFileEvent;
import xyz.eulix.space.event.LanStatusEvent;
import xyz.eulix.space.event.MoveFileEvent;
import xyz.eulix.space.event.RenameFileEvent;
import xyz.eulix.space.event.SpaceOnlineCallbackEvent;
import xyz.eulix.space.event.UploadedFileEvent;
import xyz.eulix.space.fragment.files.FileAllFragment;
import xyz.eulix.space.manager.BoxNetworkCheckManager;
import xyz.eulix.space.manager.LanManager;
import xyz.eulix.space.manager.TransferTaskManager;
import xyz.eulix.space.presenter.TabFilePresenter;
import xyz.eulix.space.transfer.event.TransferringCountEvent;
import xyz.eulix.space.ui.EulixMainActivity;
import xyz.eulix.space.ui.TransferListActivity;
import xyz.eulix.space.ui.mine.RecycleBinActivity;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.NetUtils;
import xyz.eulix.space.util.ThreadPool;
import xyz.eulix.space.util.ViewUtils;
import xyz.eulix.space.view.BottomDialog;
import xyz.eulix.space.widgets.FixableViewPager;

/**
 * Author:      Zhu Fuyu
 * Description: 主页-文件
 * History:     2021/7/16
 */
public class TabFileFragment extends AbsFragment<TabFilePresenter.ITabFile, TabFilePresenter> implements TabFilePresenter.ITabFile, View.OnClickListener {
    private Context mContext;
    private FrameLayout statusBarContainer;
    private ImageButton fileSort, back;
    private ImageButton fileMore;
    private TabLayout tab;
    private TextView title, fileSelect;
    private Button cancel, select;
    private FixableViewPager pager;
    private LinearLayout fileLayoutSearch;
    private LinearLayout tabFileContainer, tabContainer, titleContainer, selectContainer, editContainer;
    private ImageButton sortExit;
    private ImageButton moreExit;
    private LinearLayout fileChooseContainer;
    private LinearLayout recycleBinContainer;
    private LinearLayout sortByNameContainer;
    private ImageView sortByNameImage;
    private TextView sortByNameText;
    private ImageView sortByNameOrder;
    private LinearLayout sortByTimeContainer;
    private ImageView sortByTimeImage;
    private TextView sortByTimeText;
    private ImageView sortByTimeOrder;
    private LinearLayout sortByTypeContainer;
    private ImageView sortByTypeImage;
    private TextView sortByTypeText;
    private ImageView sortByTypeOrder;
    private View sortDialogView;
    private Dialog sortDialog;
    private View fileMoreDialogView;
    private Dialog fileMoreDialog;
    private FileAllFragment fileAllFragment;
    private FileAllFragment fileImageFragment;
    private FileAllFragment fileVideoFragment;
    private FileAllFragment fileDocumentFragment;
    private FileAllFragment fileOtherFragment;
    private int childFragmentIndex = 0;
    private int lastChildFragmentIndex = -1;
    private String fileTarget = null;
    private EulixMainActivity parentActivity;
    private TabFileHandler mHandler;
    private ImageView imgTransferList;
    private ImageView imgIconTransferBg;
    private SmartRefreshLayout smartRefreshLayout;

    private FrameLayout layoutTransferContainer;
    private TextView tvTransferringCount;
    private boolean isCreated = false;

    private boolean isTransferAnimShowing = false;

    @Override
    public void onCreate(@androidx.annotation.Nullable @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isCreated = true;
    }

    private static class TabFileHandler extends Handler {
        private WeakReference<TabFileFragment> tabFileFragmentWeakReference;

        public TabFileHandler(TabFileFragment fragment) {
            tabFileFragmentWeakReference = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            TabFileFragment fragment = tabFileFragmentWeakReference.get();
            if (fragment == null) {
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
    public void initRootView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.tab_file_fragment_layout, container, false);
    }

    @NotNull
    @Override
    public TabFilePresenter createPresenter() {
        return new TabFilePresenter();
    }

    @Override
    public void initData() {
        mHandler = new TabFileHandler(this);
        Bundle arguments = getArguments();
        if (arguments != null && arguments.containsKey("from")) {
            fileTarget = arguments.getString("from", null);
        } else {
            fileTarget = null;
        }
        obtainParentActivity();
        fileAllFragment = new FileAllFragment();

        fileImageFragment = new FileAllFragment();
        Bundle imageData = new Bundle();
        imageData.putString(ConstantField.CATEGORY, ConstantField.Category.PICTURE);
        fileImageFragment.setArguments(imageData);

        fileVideoFragment = new FileAllFragment();
        Bundle videoData = new Bundle();
        videoData.putString(ConstantField.CATEGORY, ConstantField.Category.VIDEO);
        fileVideoFragment.setArguments(videoData);

        fileDocumentFragment = new FileAllFragment();
        Bundle documentData = new Bundle();
        documentData.putString(ConstantField.CATEGORY, ConstantField.Category.DOCUMENT);
        fileDocumentFragment.setArguments(documentData);

        fileOtherFragment = new FileAllFragment();
        Bundle otherData = new Bundle();
        otherData.putString(ConstantField.CATEGORY, ConstantField.Category.OTHER);
        fileOtherFragment.setArguments(otherData);
    }

    @Override
    public void initView(@Nullable View root) {
        if (root != null) {
            statusBarContainer = root.findViewById(R.id.status_bar_container);
            fileLayoutSearch = root.findViewById(R.id.file_layout_search);
            tabFileContainer = root.findViewById(R.id.tab_file_container);
            tab = root.findViewById(R.id.file_tab);
            tvTransferringCount = root.findViewById(R.id.file_upload_indicator);
            layoutTransferContainer = root.findViewById(R.id.file_upload_container);
            fileSort = root.findViewById(R.id.file_sort);
            fileMore = root.findViewById(R.id.file_more);
            tabContainer = root.findViewById(R.id.tab_container);
            back = root.findViewById(R.id.back);
            title = root.findViewById(R.id.title);
            titleContainer = root.findViewById(R.id.title_container);
            cancel = root.findViewById(R.id.cancel);
            fileSelect = root.findViewById(R.id.file_select);
            select = root.findViewById(R.id.select);
            selectContainer = root.findViewById(R.id.select_container);
            pager = root.findViewById(R.id.file_pager);
            editContainer = root.findViewById(R.id.file_edit_container);
            imgTransferList = root.findViewById(R.id.img_transfer_list);
            imgIconTransferBg = root.findViewById(R.id.img_transfer_bg);
            smartRefreshLayout = root.findViewById(R.id.smart_refresh_layout);
            smartRefreshLayout.setEnableOverScrollBounce(false);
            smartRefreshLayout.setOnRefreshListener(refreshLayout -> {
                if (getCurrentChildFragment() != null) {
                    getCurrentChildFragment().onSmartRefreshListener();
                }
                //刷新盒子状态
                EulixBoxBaseInfoCompatible boxInfo = EulixSpaceDBUtil.getActiveBoxBaseInfoCompatible(getContext());
                EventBusUtil.post(new SpaceOnlineCallbackEvent(boxInfo != null ? boxInfo.getBoxUuid() : "", boxInfo != null ? boxInfo.getBoxBind() : "", BoxNetworkCheckManager.getActiveDeviceOnlineStrict()));
                //检查局域网状态
                ThreadPool.getInstance().execute(() -> {
                    LanManager.getInstance().checkLanState();
                });
                TransferTaskManager.getInstance().refreshDoingCountFromDB();
            });
        }
        mContext = getContext();
        if (mContext != null) {
            sortDialogView = LayoutInflater.from(mContext).inflate(R.layout.file_sort_window, null);
            sortExit = sortDialogView.findViewById(R.id.sort_exit);
            sortDialog = new BottomDialog(mContext);
            sortDialog.setCancelable(true);
            sortDialog.setContentView(sortDialogView);

            fileMoreDialogView = LayoutInflater.from(mContext).inflate(R.layout.file_more_window, null);
            moreExit = fileMoreDialogView.findViewById(R.id.more_exit);
            fileChooseContainer = fileMoreDialogView.findViewById(R.id.file_choose_container);
            recycleBinContainer = fileMoreDialogView.findViewById(R.id.recycle_bin_container);
            sortByNameContainer = fileMoreDialogView.findViewById(R.id.sort_by_name_container);
            sortByNameImage = fileMoreDialogView.findViewById(R.id.sort_by_name_image);
            sortByNameText = fileMoreDialogView.findViewById(R.id.sort_by_name_text);
            sortByNameOrder = fileMoreDialogView.findViewById(R.id.sort_by_name_order);
            sortByTimeContainer = fileMoreDialogView.findViewById(R.id.sort_by_time_container);
            sortByTimeImage = fileMoreDialogView.findViewById(R.id.sort_by_time_image);
            sortByTimeText = fileMoreDialogView.findViewById(R.id.sort_by_time_text);
            sortByTimeOrder = fileMoreDialogView.findViewById(R.id.sort_by_time_order);
            sortByTypeContainer = fileMoreDialogView.findViewById(R.id.sort_by_type_container);
            sortByTypeImage = fileMoreDialogView.findViewById(R.id.sort_by_type_image);
            sortByTypeText = fileMoreDialogView.findViewById(R.id.sort_by_type_text);
            sortByTypeOrder = fileMoreDialogView.findViewById(R.id.sort_by_type_order);
            fileMoreDialog = new BottomDialog(mContext);
            fileMoreDialog.setCancelable(true);
            fileMoreDialog.setContentView(fileMoreDialogView);

            EventBusUtil.register(this);

            refreshTransferStyle(LanManager.getInstance().isLanEnable());

            if (NetUtils.isWifiConnected(getActivity())) {
                LanManager.getInstance().startPollCheckTask();
            }
        }
    }

    @Override
    public void initViewData() {
        statusBarContainer.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, ViewUtils.getStatusBarHeight(parentActivity)));
        setTabFileContainerBackground(getFileDepth());
        if (tab != null) {
            List<String> tabTitles = new ArrayList<>();
            tabTitles.add(getString(R.string.all));
            tabTitles.add(getString(R.string.image));
            tabTitles.add(getString(R.string.video));
            tabTitles.add(getString(R.string.document_short));
            tabTitles.add(getString(R.string.other));
            for (int i = 0; i < tabTitles.size(); i++) {
                tab.addTab(tab.newTab().setText(tabTitles.get(i)));
            }
            List<Fragment> fragments = new ArrayList<>();
            for (String title : tabTitles) {
                if (title != null) {
                    if (title.equals(getString(R.string.all))) {
                        fragments.add(fileAllFragment);
                    } else if (title.equals(getString(R.string.image))) {
                        fragments.add(fileImageFragment);
                    } else if (title.equals(getString(R.string.video))) {
                        fragments.add(fileVideoFragment);
                    } else if (title.equals(getString(R.string.document_short))) {
                        fragments.add(fileDocumentFragment);
                    } else if (title.equals(getString(R.string.other))) {
                        fragments.add(fileOtherFragment);
                    } else {
                        fragments.add(new Fragment());
                    }
                } else {
                    fragments.add(new Fragment());
                }
            }
            FilePagerAdapter adapter = new FilePagerAdapter(getChildFragmentManager(), fragments, tabTitles);
            pager.setAdapter(adapter);
            pager.setOffscreenPageLimit(4);
            pager.setSaveEnabled(false);
            tab.setupWithViewPager(pager);
        }
        selectTab();
        refreshTransferringCount(TransferTaskManager.getInstance().getTransferringCount());
    }

    @Override
    public void initEvent() {
        if (layoutTransferContainer != null) {
            layoutTransferContainer.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), TransferListActivity.class);
                startActivity(intent);
            });
        }
        if (fileLayoutSearch != null) {
            fileLayoutSearch.setOnClickListener(this);
        }
        if (fileSort != null) {
            fileSort.setOnClickListener(this);
        }
        if (fileMore != null) {
            fileMore.setOnClickListener(this);
        }
        if (back != null) {
            back.setOnClickListener(this);
        }
        if (cancel != null) {
            cancel.setOnClickListener(this);
        }
        if (select != null) {
            select.setOnClickListener(this);
        }
//        initSortClickEvent();
        initFileMoreClickEvent();
    }

    private void initSortClickEvent() {
        if (sortExit != null) {
            sortExit.setOnClickListener(v -> dismissSortDialog());
        }
        if (sortByNameContainer != null) {
            sortByNameContainer.setOnClickListener(v -> {
                String sortPattern = DataUtil.getFileSortOrder(mContext);
                if (ConstantField.Sort.NAME_ASCEND.equals(sortPattern)) {
                    sortPattern = ConstantField.Sort.NAME_DESCEND;
                } else {
                    sortPattern = ConstantField.Sort.NAME_ASCEND;
                }
                DataUtil.setFileSortOrder(mContext, sortPattern);
                setSortPattern(sortPattern);
                changeSort(sortPattern);
            });
        }
        if (sortByTimeContainer != null) {
            sortByTimeContainer.setOnClickListener(v -> {
                String sortPattern = DataUtil.getFileSortOrder(mContext);
                if (ConstantField.Sort.OPERATION_TIME_DESCEND.equals(sortPattern)) {
                    sortPattern = ConstantField.Sort.OPERATION_TIME_ASCEND;
                } else {
                    sortPattern = ConstantField.Sort.OPERATION_TIME_DESCEND;
                }
                DataUtil.setFileSortOrder(mContext, sortPattern);
                setSortPattern(sortPattern);
                changeSort(sortPattern);
            });
        }
        if (sortByTypeContainer != null) {
            sortByTypeContainer.setOnClickListener(v -> {
                String sortPattern = DataUtil.getFileSortOrder(mContext);
                if (ConstantField.Sort.MIME_ASCEND.equals(sortPattern)) {
                    sortPattern = ConstantField.Sort.MIME_DESCEND;
                } else {
                    sortPattern = ConstantField.Sort.MIME_ASCEND;
                }
                DataUtil.setFileSortOrder(mContext, sortPattern);
                setSortPattern(sortPattern);
                changeSort(sortPattern);
            });
        }
    }

    //设置下拉刷新可用性
    public void setSmartRefreshLayoutEnable(boolean isEnable) {
        if (smartRefreshLayout != null) {
            smartRefreshLayout.setEnabled(isEnable);
        }
    }

    //取消刷新
    public void cancelSmartRefreshing() {
        if (smartRefreshLayout != null) {
            smartRefreshLayout.finishRefresh();
        }
    }

    private void initFileMoreClickEvent() {
        if (moreExit != null) {
            moreExit.setOnClickListener(v -> dismissFileMoreDialog());
        }
        if (fileChooseContainer != null) {
            fileChooseContainer.setOnClickListener(v -> {
                dismissFileMoreDialog();
                switch (childFragmentIndex) {
                    case ConstantField.FragmentIndex.FILE_ALL:
                        handleEditMode(fileAllFragment);
                        break;
                    case ConstantField.FragmentIndex.FILE_IMAGE:
                        handleEditMode(fileImageFragment);
                        break;
                    case ConstantField.FragmentIndex.FILE_VIDEO:
                        handleEditMode(fileVideoFragment);
                        break;
                    case ConstantField.FragmentIndex.FILE_DOCUMENT:
                        handleEditMode(fileDocumentFragment);
                        break;
                    case ConstantField.FragmentIndex.FILE_OTHER:
                        handleEditMode(fileOtherFragment);
                        break;
                    default:
                        break;
                }
            });
        }
        if (recycleBinContainer != null){
            recycleBinContainer.setOnClickListener(v -> {
                dismissFileMoreDialog();
                Intent intent = new Intent(getActivity(), RecycleBinActivity.class);
                startActivity(intent);
            });
        }
        if (sortByNameContainer != null) {
            sortByNameContainer.setOnClickListener(v -> {
                String sortPattern = DataUtil.getFileSortOrder(mContext);
                if (ConstantField.Sort.NAME_ASCEND.equals(sortPattern)) {
                    sortPattern = ConstantField.Sort.NAME_DESCEND;
                } else {
                    sortPattern = ConstantField.Sort.NAME_ASCEND;
                }
                DataUtil.setFileSortOrder(mContext, sortPattern);
                setSortPattern(sortPattern);
                changeSort(sortPattern);
            });
        }
        if (sortByTimeContainer != null) {
            sortByTimeContainer.setOnClickListener(v -> {
                String sortPattern = DataUtil.getFileSortOrder(mContext);
                if (ConstantField.Sort.OPERATION_TIME_DESCEND.equals(sortPattern)) {
                    sortPattern = ConstantField.Sort.OPERATION_TIME_ASCEND;
                } else {
                    sortPattern = ConstantField.Sort.OPERATION_TIME_DESCEND;
                }
                DataUtil.setFileSortOrder(mContext, sortPattern);
                setSortPattern(sortPattern);
                changeSort(sortPattern);
            });
        }
        if (sortByTypeContainer != null) {
            sortByTypeContainer.setOnClickListener(v -> {
                String sortPattern = DataUtil.getFileSortOrder(mContext);
                if (ConstantField.Sort.MIME_ASCEND.equals(sortPattern)) {
                    sortPattern = ConstantField.Sort.MIME_DESCEND;
                } else {
                    sortPattern = ConstantField.Sort.MIME_ASCEND;
                }
                DataUtil.setFileSortOrder(mContext, sortPattern);
                setSortPattern(sortPattern);
                changeSort(sortPattern);
            });
        }
    }

    private void setSortPattern(String sortPattern) {
        if (sortPattern != null) {
            switch (sortPattern) {
                case ConstantField.Sort.NAME_ASCEND:
                    setSortPattern(sortByNameImage, sortByNameText, sortByNameOrder, 1, true);
                    setSortPattern(sortByTimeImage, sortByTimeText, sortByTimeOrder, 2, null);
                    setSortPattern(sortByTypeImage, sortByTypeText, sortByTypeOrder, 3, null);
                    break;
                case ConstantField.Sort.NAME_DESCEND:
                    setSortPattern(sortByNameImage, sortByNameText, sortByNameOrder, 1, false);
                    setSortPattern(sortByTimeImage, sortByTimeText, sortByTimeOrder, 2, null);
                    setSortPattern(sortByTypeImage, sortByTypeText, sortByTypeOrder, 3, null);
                    break;
                case ConstantField.Sort.OPERATION_TIME_ASCEND:
                    setSortPattern(sortByNameImage, sortByNameText, sortByNameOrder, 1, null);
                    setSortPattern(sortByTimeImage, sortByTimeText, sortByTimeOrder, 2, true);
                    setSortPattern(sortByTypeImage, sortByTypeText, sortByTypeOrder, 3, null);
                    break;
                case ConstantField.Sort.OPERATION_TIME_DESCEND:
                    setSortPattern(sortByNameImage, sortByNameText, sortByNameOrder, 1, null);
                    setSortPattern(sortByTimeImage, sortByTimeText, sortByTimeOrder, 2, false);
                    setSortPattern(sortByTypeImage, sortByTypeText, sortByTypeOrder, 3, null);
                    break;
                case ConstantField.Sort.MIME_ASCEND:
                    setSortPattern(sortByNameImage, sortByNameText, sortByNameOrder, 1, null);
                    setSortPattern(sortByTimeImage, sortByTimeText, sortByTimeOrder, 2, null);
                    setSortPattern(sortByTypeImage, sortByTypeText, sortByTypeOrder, 3, true);
                    break;
                case ConstantField.Sort.MIME_DESCEND:
                    setSortPattern(sortByNameImage, sortByNameText, sortByNameOrder, 1, null);
                    setSortPattern(sortByTimeImage, sortByTimeText, sortByTimeOrder, 2, null);
                    setSortPattern(sortByTypeImage, sortByTypeText, sortByTypeOrder, 3, false);
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * @param sortImage
     * @param sortText
     * @param sortOrder
     * @param type      1：按名称；2：按修改时间；3：按文件类型
     * @param isAscend  null：不展示
     */
    private void setSortPattern(ImageView sortImage, TextView sortText, ImageView sortOrder, int type, Boolean isAscend) {
        if (mContext != null && sortImage != null && sortText != null && sortOrder != null) {
            switch (type) {
                case 1:
                    sortImage.setImageResource((isAscend == null) ? R.drawable.ic_sort_name_off : R.drawable.ic_sort_name_on);
                    break;
                case 2:
                    sortImage.setImageResource((isAscend == null) ? R.drawable.ic_sort_time_off : R.drawable.ic_sort_time_on);
                    break;
                case 3:
                    sortImage.setImageResource((isAscend == null) ? R.drawable.ic_sort_type_off : R.drawable.ic_sort_type_on);
                    break;
                default:
                    break;
            }
            sortText.setTextColor(mContext.getResources().getColor((isAscend == null)
                    ? R.color.black_ff333333 : R.color.blue_ff337aff));
            sortOrder.setVisibility((isAscend == null) ? View.INVISIBLE : View.VISIBLE);
            if (isAscend != null) {
                sortOrder.setImageResource(isAscend ? R.drawable.ic_sort_ascend : R.drawable.ic_sort_descend);
            }
        }
    }

    private void setContainerClickable(LinearLayout container, boolean isClick) {
        if (container != null) {
            container.setClickable(isClick);
        }
    }

    private void changeSort(String sort) {
        if (fileMoreDialogView == null) {
            dismissFileMoreDialog();
        } else {
            setContainerClickable(fileChooseContainer, false);
            setContainerClickable(sortByNameContainer, false);
            setContainerClickable(sortByTimeContainer, false);
            setContainerClickable(sortByTypeContainer, false);
            fileMoreDialogView.postDelayed(this::dismissFileMoreDialog, 200);
        }
        if (fileAllFragment != null) {
            fileAllFragment.changeSort(sort);
        }
        if (fileImageFragment != null) {
            fileImageFragment.changeSort(sort);
        }
        if (fileVideoFragment != null) {
            fileVideoFragment.changeSort(sort);
        }
        if (fileDocumentFragment != null) {
            fileDocumentFragment.changeSort(sort);
        }
        if (fileOtherFragment != null) {
            fileOtherFragment.changeSort(sort);
        }
    }

    public void obtainAccessToken() {
        if (parentActivity != null) {
            parentActivity.obtainAccessToken();
        }
    }

    @Override
    public void showImageTextToast(@DrawableRes int drawableResId, @StringRes int stringResId) {
        if (parentActivity != null) {
            parentActivity.showImageTextToast(drawableResId, stringResId);
        }
    }

    private CustomizeFile getCustomFile(FileAllFragment fragment) {
        CustomizeFile customizeFile = null;
        if (fragment != null) {
            List<CustomizeFile> customizeFiles = fragment.getSelectFiles();
            if (customizeFiles != null && customizeFiles.size() == 1) {
                customizeFile = customizeFiles.get(0);
            }
        }
        return customizeFile;
    }

    private int getCustomFilesSize(FileAllFragment fragment) {
        int size = -1;
        if (fragment != null) {
            List<CustomizeFile> customizeFiles = fragment.getSelectFiles();
            if (customizeFiles != null) {
                size = customizeFiles.size();
            }
        }
        return size;
    }

    private int handleFileDepth(FileAllFragment fragment) {
        int fileDepth = 0;
        if (fragment != null) {
            fileDepth = fragment.getFileDepth();
        }
        return fileDepth;
    }

    private void handleSelectTotal(FileAllFragment fragment) {
        if (fragment != null) {
            fragment.handleSelectTotal();
        }
    }

    private void handleEditMode(FileAllFragment fragment) {
        if (fragment != null) {
            fragment.handleEditMode();
        }
    }

    /**
     * 传递是否是全选
     */
    private void handleSelect(boolean selectAll) {
        switch (childFragmentIndex) {
            case ConstantField.FragmentIndex.FILE_ALL:
                if (fileAllFragment != null) {
                    fileAllFragment.handleSelectAll(selectAll);
                }
                break;
            case ConstantField.FragmentIndex.FILE_IMAGE:
                if (fileImageFragment != null) {
                    fileImageFragment.handleSelectAll(selectAll);
                }
                break;
            case ConstantField.FragmentIndex.FILE_VIDEO:
                if (fileVideoFragment != null) {
                    fileVideoFragment.handleSelectAll(selectAll);
                }
                break;
            case ConstantField.FragmentIndex.FILE_DOCUMENT:
                if (fileDocumentFragment != null) {
                    fileDocumentFragment.handleSelectAll(selectAll);
                }
                break;
            case ConstantField.FragmentIndex.FILE_OTHER:
                if (fileOtherFragment != null) {
                    fileOtherFragment.handleSelectAll(selectAll);
                }
                break;
            default:
                break;
        }
    }

    private void obtainParentActivity() {
        if (parentActivity == null) {
            FragmentActivity activity = getActivity();
            if (activity instanceof EulixMainActivity) {
                parentActivity = (EulixMainActivity) activity;
            }
        }
    }

    private void setTabFileContainerBackground(int fileDepth) {
        if (tabFileContainer != null && parentActivity != null) {
            if (fileDepth > 0) {
                tabFileContainer.setBackgroundColor(Color.WHITE);
            } else if (mContext != null) {
                tabFileContainer.setBackgroundColor(mContext.getResources().getColor(R.color.white_fff5f6fa));
            }
        }
    }

    private int getFileDepth() {
        int fileDepth = 0;
        switch (childFragmentIndex) {
            case ConstantField.FragmentIndex.FILE_ALL:
                fileDepth = handleFileDepth(fileAllFragment);
                break;
            case ConstantField.FragmentIndex.FILE_IMAGE:
                fileDepth = handleFileDepth(fileImageFragment);
                break;
            case ConstantField.FragmentIndex.FILE_VIDEO:
                fileDepth = handleFileDepth(fileVideoFragment);
                break;
            case ConstantField.FragmentIndex.FILE_DOCUMENT:
                fileDepth = handleFileDepth(fileDocumentFragment);
                break;
            case ConstantField.FragmentIndex.FILE_OTHER:
                fileDepth = handleFileDepth(fileOtherFragment);
                break;
            default:
                break;
        }
        return fileDepth;
    }

    private boolean checkFileAllFragment(String category) {
        int nChildFragmentIndex = ConstantField.FragmentIndex.FILE_ALL;
        if (category != null) {
            switch (category) {
                case ConstantField.Category.PICTURE:
                    nChildFragmentIndex = ConstantField.FragmentIndex.FILE_IMAGE;
                    break;
                case ConstantField.Category.VIDEO:
                    nChildFragmentIndex = ConstantField.FragmentIndex.FILE_VIDEO;
                    break;
                case ConstantField.Category.DOCUMENT:
                    nChildFragmentIndex = ConstantField.FragmentIndex.FILE_DOCUMENT;
                    break;
                case ConstantField.Category.OTHER:
                    nChildFragmentIndex = ConstantField.FragmentIndex.FILE_OTHER;
                    break;
                default:
                    nChildFragmentIndex = ConstantField.FragmentIndex.TAB_FILE;
                    break;
            }
        }
        return (childFragmentIndex == nChildFragmentIndex);
    }

    public void handleTitleVisibility(int selectNumber, int totalNumber, String titleText, String category) {
        if (checkFileAllFragment(category)) {
            int fileDepth = getFileDepth();
            setTabFileContainerBackground(fileDepth);
            if (parentActivity != null && childFragmentIndex == ConstantField.FragmentIndex.FILE_ALL) {
                parentActivity.setBtnUploadVisibility(selectNumber <= 0);
            }
            if (pager != null) {
                pager.setScrollable((selectNumber < 0 && fileDepth <= 0));
            }
            if (tabContainer != null) {
                tabContainer.setVisibility(selectNumber >= 0 ? View.GONE : View.VISIBLE);
            }
            if (fileLayoutSearch != null) {
                fileLayoutSearch.setVisibility((selectNumber >= 0 || fileDepth > 0) ? View.INVISIBLE : View.VISIBLE);
            }
            if (tab != null) {
                tab.setVisibility((selectNumber >= 0 || fileDepth > 0) ? View.GONE : View.VISIBLE);
            }
            if (titleContainer != null) {
                titleContainer.setVisibility((selectNumber < 0 && fileDepth > 0) ? View.VISIBLE : View.GONE);
            }
            if (title != null && titleText != null) {
                title.setText(titleText);
            }
            if (selectContainer != null) {
                selectContainer.setVisibility(selectNumber >= 0 ? View.VISIBLE : View.GONE);
            }
            if (fileSelect != null && mContext != null) {
                String fileSelectNumber = mContext.getString(R.string.choose_file_part_1) + selectNumber +
                        mContext.getString((Math.abs(selectNumber) == 1) ? R.string.choose_file_part_2_singular : R.string.choose_file_part_2_plural);
                fileSelect.setText(fileSelectNumber);
            }
            if (select != null) {
                select.setText(selectNumber < totalNumber ? R.string.select_all : R.string.select_none);
            }
            if (parentActivity != null && parentActivity.getChildFragmentIndex() == ConstantField.FragmentIndex.TAB_FILE) {
                parentActivity.setLayoutNavigationVisibility(selectNumber < 1);
            }
            fileSelectCallback(selectNumber);
        }
    }

    private void fileSelectCallback(int selectNumber) {
        switch (childFragmentIndex) {
            case ConstantField.FragmentIndex.FILE_ALL:
                if (fileAllFragment != null) {
                    fileAllFragment.handleFileEditView(selectNumber);
                }
                break;
            case ConstantField.FragmentIndex.FILE_IMAGE:
                if (fileImageFragment != null) {
                    fileImageFragment.handleFileEditView(selectNumber);
                }
                break;
            case ConstantField.FragmentIndex.FILE_VIDEO:
                if (fileVideoFragment != null) {
                    fileVideoFragment.handleFileEditView(selectNumber);
                }
                break;
            case ConstantField.FragmentIndex.FILE_DOCUMENT:
                if (fileDocumentFragment != null) {
                    fileDocumentFragment.handleFileEditView(selectNumber);
                }
                break;
            case ConstantField.FragmentIndex.FILE_OTHER:
                if (fileOtherFragment != null) {
                    fileOtherFragment.handleFileEditView(selectNumber);
                }
                break;
            default:
                break;
        }
    }

    private void resetFileFragment(FileAllFragment fragment) {
        if (fragment != null) {
            fragment.reset();
        }
    }

    /**
     * 重置部分视图
     *
     * @param index 该视图不重置
     */
    public void resetFileFragment(int index) {
        if (index != ConstantField.FragmentIndex.FILE_ALL) {
            resetFileFragment(fileAllFragment);
        }
        if (index != ConstantField.FragmentIndex.FILE_IMAGE) {
            resetFileFragment(fileImageFragment);
        }
        if (index != ConstantField.FragmentIndex.FILE_VIDEO) {
            resetFileFragment(fileVideoFragment);
        }
        if (index != ConstantField.FragmentIndex.FILE_DOCUMENT) {
            resetFileFragment(fileDocumentFragment);
        }
        if (index != ConstantField.FragmentIndex.FILE_OTHER) {
            resetFileFragment(fileOtherFragment);
        }
    }

    public void handleTitleVisibility(int selectNumber, String category) {
        handleTitleVisibility(selectNumber, (selectNumber + 1), category);
    }

    public void handleTitleVisibility(String titleText, String category) {
        handleTitleVisibility(-1, 0, titleText, category);
    }

    public void handleTitleVisibility(int selectNumber, int totalNumber, String category) {
        handleSelect((selectNumber == totalNumber));
        handleTitleVisibility(selectNumber, totalNumber, null, category);
    }

    public int getSelectFilesSize() {
        int size = -1;
        switch (childFragmentIndex) {
            case ConstantField.FragmentIndex.FILE_ALL:
                size = getCustomFilesSize(fileAllFragment);
                break;
            case ConstantField.FragmentIndex.FILE_IMAGE:
                size = getCustomFilesSize(fileImageFragment);
                break;
            case ConstantField.FragmentIndex.FILE_VIDEO:
                size = getCustomFilesSize(fileVideoFragment);
                break;
            case ConstantField.FragmentIndex.FILE_DOCUMENT:
                size = getCustomFilesSize(fileDocumentFragment);
                break;
            case ConstantField.FragmentIndex.FILE_OTHER:
                size = getCustomFilesSize(fileOtherFragment);
                break;
            default:
                break;
        }
        return size;
    }

    public void setStatusBarColor() {
        setTabFileContainerBackground(getFileDepth());
        setChildFragmentIndex(childFragmentIndex);
    }

    public void refreshEulixSpaceStorage() {
        switch (childFragmentIndex) {
            case ConstantField.FragmentIndex.FILE_ALL:
                if (fileAllFragment != null) {
                    fileAllFragment.refreshEulixSpaceStorage(false);
                }
                break;
            case ConstantField.FragmentIndex.FILE_IMAGE:
                if (fileImageFragment != null) {
                    fileImageFragment.refreshEulixSpaceStorage(false);
                }
                break;
            case ConstantField.FragmentIndex.FILE_VIDEO:
                if (fileVideoFragment != null) {
                    fileVideoFragment.refreshEulixSpaceStorage(false);
                }
                break;
            case ConstantField.FragmentIndex.FILE_DOCUMENT:
                if (fileDocumentFragment != null) {
                    fileDocumentFragment.refreshEulixSpaceStorage(false);
                }
                break;
            case ConstantField.FragmentIndex.FILE_OTHER:
                if (fileOtherFragment != null) {
                    fileOtherFragment.refreshEulixSpaceStorage(false);
                }
                break;
            default:
                break;
        }
    }

    public boolean isShareDialogShow() {
        boolean isShow = false;
        switch (childFragmentIndex) {
            case ConstantField.FragmentIndex.FILE_ALL:
                if (fileAllFragment != null) {
                    isShow = fileAllFragment.isShareDialogShow();
                }
                break;
            case ConstantField.FragmentIndex.FILE_IMAGE:
                if (fileImageFragment != null) {
                    isShow = fileImageFragment.isShareDialogShow();
                }
                break;
            case ConstantField.FragmentIndex.FILE_VIDEO:
                if (fileVideoFragment != null) {
                    isShow = fileVideoFragment.isShareDialogShow();
                }
                break;
            case ConstantField.FragmentIndex.FILE_DOCUMENT:
                if (fileDocumentFragment != null) {
                    isShow = fileDocumentFragment.isShareDialogShow();
                }
                break;
            case ConstantField.FragmentIndex.FILE_OTHER:
                if (fileOtherFragment != null) {
                    isShow = fileOtherFragment.isShareDialogShow();
                }
                break;
            default:
                break;
        }
        return isShow;
    }

    public void dismissShareDialog() {
        switch (childFragmentIndex) {
            case ConstantField.FragmentIndex.FILE_ALL:
                if (fileAllFragment != null) {
                    fileAllFragment.dismissShareDialog(null, false);
                }
                break;
            case ConstantField.FragmentIndex.FILE_IMAGE:
                if (fileImageFragment != null) {
                    fileImageFragment.dismissShareDialog(null, false);
                }
                break;
            case ConstantField.FragmentIndex.FILE_VIDEO:
                if (fileVideoFragment != null) {
                    fileVideoFragment.dismissShareDialog(null, false);
                }
                break;
            case ConstantField.FragmentIndex.FILE_DOCUMENT:
                if (fileDocumentFragment != null) {
                    fileDocumentFragment.dismissShareDialog(null, false);
                }
                break;
            case ConstantField.FragmentIndex.FILE_OTHER:
                if (fileOtherFragment != null) {
                    fileOtherFragment.dismissShareDialog(null, false);
                }
                break;
            default:
                break;
        }
    }

    private void showSortDialog() {
        setContainerClickable(sortByNameContainer, true);
        setContainerClickable(sortByTimeContainer, true);
        setContainerClickable(sortByTypeContainer, true);
        setSortPattern(DataUtil.getFileSortOrder(mContext));
        if (sortDialog != null && !sortDialog.isShowing()) {
            sortDialog.show();
            Window window = sortDialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissSortDialog() {
        if (sortDialog != null && sortDialog.isShowing()) {
            sortDialog.dismiss();
        }
    }

    private void showFileMoreDialog() {
        setContainerClickable(fileChooseContainer, true);
        setContainerClickable(sortByNameContainer, true);
        setContainerClickable(sortByTimeContainer, true);
        setContainerClickable(sortByTypeContainer, true);
        setSortPattern(DataUtil.getFileSortOrder(mContext));
        if (fileMoreDialog != null && !fileMoreDialog.isShowing()) {
            fileMoreDialog.show();
            Window window = fileMoreDialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissFileMoreDialog() {
        if (fileMoreDialog != null && fileMoreDialog.isShowing()) {
            fileMoreDialog.dismiss();
        }
    }

    public void fileDialog(View view, boolean isShow) {
        if (editContainer != null) {
            editContainer.removeAllViews();
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            editContainer.addView(view, layoutParams);
            editContainer.setVisibility(isShow ? View.VISIBLE : View.INVISIBLE);
        }
    }

    public void selectTab() {
        if (tab != null && fileTarget != null) {
            switch (fileTarget) {
                case ConstantField.FRAG_FILE_ALL:
                    resetFileFragment(ConstantField.FragmentIndex.FILE_ALL);
                    tab.selectTab(tab.getTabAt(0));
                    break;
                case ConstantField.FRAG_FILE_IMAGE:
                    resetFileFragment(ConstantField.FragmentIndex.FILE_IMAGE);
                    tab.selectTab(tab.getTabAt(1));
                    break;
                case ConstantField.FRAG_FILE_VIDEO:
                    resetFileFragment(ConstantField.FragmentIndex.FILE_VIDEO);
                    tab.selectTab(tab.getTabAt(2));
                    break;
                case ConstantField.FRAG_FILE_DOCUMENT:
                    resetFileFragment(ConstantField.FragmentIndex.FILE_DOCUMENT);
                    tab.selectTab(tab.getTabAt(3));
                    break;
                case ConstantField.FRAG_FILE_OTHER:
                    resetFileFragment(ConstantField.FragmentIndex.FILE_OTHER);
                    tab.selectTab(tab.getTabAt(4));
                    break;
                default:
                    break;
            }
            fileTarget = null;
        }
    }

    public UUID getCurrentFolderUUID() {
        UUID uuid = null;
        if (fileAllFragment != null) {
            uuid = fileAllFragment.getCurrentFolderUUID();
        }
        return uuid;
    }

    public ArrayStack<UUID> getCurrentUUIDStack() {
        ArrayStack<UUID> uuids = null;
        if (fileAllFragment != null) {
            uuids = fileAllFragment.getCurrentUUIDStack();
        }
        return uuids;
    }

    public void handleRefreshEulixSpaceStorage(UUID parentUUID) {
        if (parentActivity != null) {
            parentActivity.refreshEulixSpaceStorage(parentUUID);
        }
    }

    public void handleDismissFolderListView(boolean isConfirm, UUID selectUUID, Boolean isCopy, ArrayStack<UUID> uuids, List<UUID> newFolderUUIDs) {
        if (parentActivity != null) {
            parentActivity.dismissFolderListView(isConfirm, selectUUID, isCopy, uuids, newFolderUUIDs);
        }
    }

    public void startFileSearch(String fileUuid) {
        if (parentActivity != null) {
            parentActivity.startFileSearch(fileUuid);
        }
    }

    public void closeLoading() {
        if (parentActivity != null) {
            parentActivity.closeLoading();
        }
    }

    public String getUuidTitle(String uuid) {
        String uuidTitle = null;
        if (parentActivity != null) {
            uuidTitle = parentActivity.getUuidTitle(uuid);
        }
        return uuidTitle;
    }

    public void setUuidTitle(String uuid, String title) {
        if (parentActivity != null) {
            parentActivity.setUuidTitle(uuid, title);
        }
    }

    public void folderChange() {
        List<UUID> newFolderList = parentActivity.getNewFolderUUIDList();
        if (newFolderList != null) {
            if (fileAllFragment != null) {
                fileAllFragment.folderChange(newFolderList);
            }
            parentActivity.resetNewFolderUUIDList();
        }
    }

    public void folderChange(UUID folderUuid, boolean isSearch) {
        if (fileAllFragment != null) {
            fileAllFragment.folderChange(folderUuid, isSearch);
        }
    }

    public void folderChange(String folderName, String folderUuid) {
        List<UUID> newFolderList = parentActivity.getNewFolderUUIDList();
        if (newFolderList != null) {
            if (fileAllFragment != null) {
                fileAllFragment.folderChange(newFolderList, folderName, folderUuid);
            }
            parentActivity.resetNewFolderUUIDList();
        }
    }

    public void folderChange(ArrayStack<UUID> uuids) {
        if (childFragmentIndex != ConstantField.FragmentIndex.FILE_ALL && pager != null && pager.isScrollable()) {
            fileTarget = ConstantField.FRAG_FILE_ALL;
            selectTab();
        }
        if (fileAllFragment != null) {
            fileAllFragment.stackChange(uuids);
        }
    }

    public boolean handleBackEvent() {
        switch (childFragmentIndex) {
            case ConstantField.FragmentIndex.FILE_ALL:
                return (fileAllFragment != null && fileAllFragment.handleBackEvent());
            case ConstantField.FragmentIndex.FILE_IMAGE:
                return (fileImageFragment != null && fileImageFragment.handleBackEvent());
            case ConstantField.FragmentIndex.FILE_VIDEO:
                return (fileVideoFragment != null && fileVideoFragment.handleBackEvent());
            case ConstantField.FragmentIndex.FILE_DOCUMENT:
                return (fileDocumentFragment != null && fileDocumentFragment.handleBackEvent());
            case ConstantField.FragmentIndex.FILE_OTHER:
                return (fileOtherFragment != null && fileOtherFragment.handleBackEvent());
            default:
                return false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        obtainParentActivity();
        if (isTransferAnimShowing) {
            if (imgIconTransferBg.getVisibility() == View.VISIBLE) {
                imgIconTransferBg.clearAnimation();
                ViewUtils.setLoadingAnim(getActivity(), imgIconTransferBg);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (isTransferAnimShowing) {
            if (imgIconTransferBg.getVisibility() == View.VISIBLE) {
                imgIconTransferBg.clearAnimation();
            }
        }
    }

    @Override
    public void onDestroy() {
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
            mHandler = null;
        }
        super.onDestroy();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBusUtil.unRegister(this);
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.file_layout_search:
                    if (parentActivity != null) {
                        parentActivity.startFileSearch(null);
                    }
                    break;
                case R.id.file_sort:
                    showSortDialog();
                    break;
                case R.id.file_more:
                    showFileMoreDialog();
                    break;
                case R.id.back:
                case R.id.cancel:
                    handleBackEvent();
                    break;
                case R.id.select:
                    switch (childFragmentIndex) {
                        case ConstantField.FragmentIndex.FILE_ALL:
                            handleSelectTotal(fileAllFragment);
                            break;
                        case ConstantField.FragmentIndex.FILE_IMAGE:
                            handleSelectTotal(fileImageFragment);
                            break;
                        case ConstantField.FragmentIndex.FILE_VIDEO:
                            handleSelectTotal(fileVideoFragment);
                            break;
                        case ConstantField.FragmentIndex.FILE_DOCUMENT:
                            handleSelectTotal(fileDocumentFragment);
                            break;
                        case ConstantField.FragmentIndex.FILE_OTHER:
                            handleSelectTotal(fileOtherFragment);
                            break;
                        default:
                            break;
                    }
                    break;
                default:
                    break;
            }
        }
    }

    //刷新传输列表角标中背景转动
    private void refreshTransferTabBg(boolean isShow) {
        if (imgIconTransferBg != null) {
            if (isShow) {
                if (imgIconTransferBg.getVisibility() != View.VISIBLE) {
                    imgIconTransferBg.setVisibility(View.VISIBLE);
                    ViewUtils.setLoadingAnim(getActivity(), imgIconTransferBg);
                }
                isTransferAnimShowing = true;
            } else {
                if (imgIconTransferBg.getVisibility() == View.VISIBLE) {
                    imgIconTransferBg.clearAnimation();
                    imgIconTransferBg.setVisibility(View.GONE);
                }
                isTransferAnimShowing = false;
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(LanStatusEvent event) {
        Logger.d("zfy", "receive LanEvent " + event.isLanEnable);
//        ConstantField.sIsLanConnect = event.isLanEnable();
        refreshTransferStyle(event.isLanEnable);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(SpaceOnlineCallbackEvent event) {
        Logger.d("zfy", "receive SpaceOnlineCallbackEvent " + event.isOnline());
        refreshTransferStyle(LanManager.getInstance().isLanEnable());
    }

    //更新传输列表角标样式
    private void refreshTransferStyle(boolean isLan) {
        if (isLan) {
            imgTransferList.setImageResource(R.drawable.icon_transfer_tab_lan);
            imgIconTransferBg.setImageResource(R.drawable.icon_transfer_bg_lan);
        } else if (!BoxNetworkCheckManager.getActiveDeviceOnlineStrict()) {
            imgTransferList.setImageResource(R.drawable.icon_transfer_tab_offline);
            imgIconTransferBg.setImageResource(R.drawable.icon_transfer_bg);
        } else {
            imgTransferList.setImageResource(R.drawable.icon_transfer_tab);
            imgIconTransferBg.setImageResource(R.drawable.icon_transfer_bg);
        }
    }

    //预览页删除文件
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(DeleteFileEvent event) {
        Logger.d("zfy", "receive DeleteEvent");
//        showImageTextToast(R.drawable.toast_right, R.string.delete_success);
        refreshEulixSpaceStorage();
    }

    //预览页删除文件
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(MoveFileEvent event) {
        Logger.d("zfy", "receive MoveEvent:" + event.getFileName() + "," + event.getUuid());
//        showImageTextToast(R.drawable.toast_right, R.string.cut_success);
        refreshEulixSpaceStorage();
    }

    //预览页修改文件
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(RenameFileEvent event) {
        Logger.d("zfy", "receive RenameEvent:" + event.getFileName() + "," + event.getUuid());
        refreshEulixSpaceStorage();
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(TransferringCountEvent event) {
        Logger.d("zfy", "receive TransferringCountEvent:" + event.currentCount);
        if (tvTransferringCount == null) {
            return;
        }
        refreshTransferringCount(event.currentCount);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(BoxStatusEvent event) {
        Logger.d("zfy", "receive BoxStatusEvent:" + event.isSelectBox);
        if (event.isSelectBox) {
            refreshTransferringCount(TransferTaskManager.getInstance().getTransferringCount());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(UploadedFileEvent event) {
        if (event != null) {
            String mimeType = FileUtil.getMimeTypeByPath(event.fileName);
            //刷新对应类型列表
            if (mimeType == null) {
                if (fileOtherFragment != null) {
                    fileOtherFragment.refreshEulixSpaceStorage(false);
                }
            } else if (mimeType.contains("image")) {
                if (fileImageFragment != null) {
                    fileImageFragment.refreshEulixSpaceStorage(false);
                }
            } else if (mimeType.contains("video")) {
                if (fileVideoFragment != null) {
                    fileVideoFragment.refreshEulixSpaceStorage(false);
                }
            }else if (FileUtil.isDocument(mimeType)){
                if (fileDocumentFragment != null){
                    fileDocumentFragment.refreshEulixSpaceStorage(false);
                }
            } else {
                if (fileOtherFragment != null) {
                    fileOtherFragment.refreshEulixSpaceStorage(false);
                }
            }
            if (fileAllFragment != null) {
                fileAllFragment.refreshEulixSpaceStorage(false);
            }
        }
    }

    public void refreshTransferringCount() {
        refreshTransferringCount(TransferTaskManager.getInstance().getTransferringCount());
    }

    private void refreshTransferringCount(int transferringCount) {
        Logger.d("zfy", "transferringCount=" + transferringCount);
        if (tvTransferringCount != null) {
            if (transferringCount > 99) {
                tvTransferringCount.setVisibility(View.VISIBLE);
                tvTransferringCount.setText("99+");
                refreshTransferTabBg(true);
            } else if (transferringCount > 0) {
                tvTransferringCount.setVisibility(View.VISIBLE);
                tvTransferringCount.setText(transferringCount + "");
                refreshTransferTabBg(true);
            } else {
                tvTransferringCount.setVisibility(View.GONE);
                tvTransferringCount.setText("");
                refreshTransferTabBg(false);
            }
        }
    }


    public int getChildFragmentIndex() {
        return childFragmentIndex;
    }

    public void setChildFragmentIndex(int childFragmentIndex) {
        this.childFragmentIndex = childFragmentIndex;
        if (parentActivity != null) {
            if (childFragmentIndex == ConstantField.FragmentIndex.FILE_ALL) {
                int selectNumber = 0;
                if (fileAllFragment != null) {
                    selectNumber = fileAllFragment.getSelectNumber();
                }
                parentActivity.setBtnUploadVisibility(selectNumber <= 0);
                folderChange();
            } else {
                parentActivity.setBtnUploadVisibility(false);
            }
            if (parentActivity.getChildFragmentIndex() == ConstantField.FragmentIndex.TAB_FILE) {
                onLogUpPage(childFragmentIndex);
            }
        }
    }

    public void setFileTarget(String fileTarget) {
        this.fileTarget = fileTarget;
    }

    public void onLogUpPage(int childFragmentIndex) {
        if (lastChildFragmentIndex != childFragmentIndex) {
            lastChildFragmentIndex = childFragmentIndex;
        }
    }

    //获取当前子页面
    private FileAllFragment getCurrentChildFragment() {
        switch (childFragmentIndex) {
            case ConstantField.FragmentIndex.FILE_ALL:
                return fileAllFragment;
            case ConstantField.FragmentIndex.FILE_IMAGE:
                return fileImageFragment;
            case ConstantField.FragmentIndex.FILE_VIDEO:
                return fileVideoFragment;
            case ConstantField.FragmentIndex.FILE_DOCUMENT:
                return fileDocumentFragment;
            case ConstantField.FragmentIndex.FILE_OTHER:
                return fileOtherFragment;
            default:
                return null;
        }
    }
}
