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

package xyz.eulix.space.fragment.files;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsFragment;
import xyz.eulix.space.adapter.files.FileAdapter;
import xyz.eulix.space.bean.ArrayStack;
import xyz.eulix.space.bean.CustomizeFile;
import xyz.eulix.space.event.ThumbEvent;
import xyz.eulix.space.fragment.main.TabFileFragment;
import xyz.eulix.space.manager.ThumbManager;
import xyz.eulix.space.network.files.PageInfo;
import xyz.eulix.space.presenter.FileAllPresenter;
import xyz.eulix.space.transfer.TransferHelper;
import xyz.eulix.space.ui.FilePreviewActivity;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.OperationUtil;
import xyz.eulix.space.util.ViewUtils;
import xyz.eulix.space.view.dialog.file.FileEditView;
import xyz.eulix.space.view.rv.FooterView;
import xyz.eulix.space.view.rv.HeaderFooterWrapper;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/7/19 17:43
 */
public class FileAllFragment extends AbsFragment<FileAllPresenter.IFileAll, FileAllPresenter> implements FileAllPresenter.IFileAll, FileEditView.FileEditCallback, FileEditView.FileEditPluralCallback, View.OnClickListener, FileAdapter.OnItemClickListener {
    private static final String TAG = FileAllFragment.class.getSimpleName();
    private String mCategory;
    private FrameLayout fileSubViewContainer;
    private RecyclerView list;
    private Button refreshNow;
    private RelativeLayout exceptionContainer;
    private LinearLayout networkExceptionContainer, status404Container, emptyFileContainer;
    private FileEditView fileEditView;
    private FileAdapter adapter;
    private HeaderFooterWrapper headerFooterWrapper;
    private FooterView footer;
    private List<CustomizeFile> customizeFiles;
    private List<String> selectIds;
    private TabFileFragment parentFragment;
    private FileAllHandler handler;
    private int mPage = 1;
    private int mTotalPage = 1;
    private int maxChildCount = 7;
    // 上拉加载使能，本地加载时失效，网络加载到来之后生效
    private boolean isLoadingEnable = false;
    // 本地加载是否存在，不存在则展示网络错误，刷新时强制为true
    private boolean isLocalEmpty = true;

    private boolean isSelectAll = false;

    private Comparator<CustomizeFile> nameAscendComparator = ((o1, o2) -> {
        String name1 = null;
        String name2 = null;
        boolean isFolder1 = false;
        boolean isFolder2 = false;
        if (o1 != null) {
            name1 = o1.getName();
            isFolder1 = (ConstantField.MimeType.FOLDER.equalsIgnoreCase(o1.getMime()));
        }
        if (o2 != null) {
            name2 = o2.getName();
            isFolder2 = (ConstantField.MimeType.FOLDER.equalsIgnoreCase(o2.getMime()));
        }
        if (isFolder1 == isFolder2) {
            if (name1 == null || name2 == null) {
                if (name1 == null && name2 == null) {
                    return 0;
                } else if (name1 == null) {
                    return 1;
                } else {
                    return -1;
                }
            } else {
                return name1.compareTo(name2);
            }
        } else {
            return (isFolder1 ? -1 : 1);
        }
    });

    private Comparator<CustomizeFile> nameDescendComparator = ((o1, o2) -> {
        String name1 = null;
        String name2 = null;
        boolean isFolder1 = false;
        boolean isFolder2 = false;
        if (o1 != null) {
            name1 = o1.getName();
            isFolder1 = (ConstantField.MimeType.FOLDER.equalsIgnoreCase(o1.getMime()));
        }
        if (o2 != null) {
            name2 = o2.getName();
            isFolder2 = (ConstantField.MimeType.FOLDER.equalsIgnoreCase(o2.getMime()));
        }
        if (isFolder1 == isFolder2) {
            if (name1 == null || name2 == null) {
                if (name1 == null && name2 == null) {
                    return 0;
                } else if (name1 == null) {
                    return 1;
                } else {
                    return -1;
                }
            } else {
                return name2.compareTo(name1);
            }
        } else {
            return (isFolder1 ? -1 : 1);
        }
    });

    private Comparator<CustomizeFile> timeAscendComparator = ((o1, o2) -> {
        Long time1 = null;
        Long time2 = null;
        boolean isFolder1 = false;
        boolean isFolder2 = false;
        if (o1 != null) {
            time1 = o1.getTimestamp();
            isFolder1 = (ConstantField.MimeType.FOLDER.equalsIgnoreCase(o1.getMime()));
        }
        if (o2 != null) {
            time2 = o2.getTimestamp();
            isFolder2 = (ConstantField.MimeType.FOLDER.equalsIgnoreCase(o2.getMime()));
        }
        if (isFolder1 == isFolder2) {
            if (time1 == null || time2 == null) {
                if (time1 == null && time2 == null) {
                    return 0;
                } else if (time1 == null) {
                    return 1;
                } else {
                    return -1;
                }
            } else {
                return time1.compareTo(time2);
            }
        } else {
            return (isFolder1 ? -1 : 1);
        }
    });

    private Comparator<CustomizeFile> timeDescendComparator = ((o1, o2) -> {
        Long time1 = null;
        Long time2 = null;
        boolean isFolder1 = false;
        boolean isFolder2 = false;
        if (o1 != null) {
            time1 = o1.getTimestamp();
            isFolder1 = (ConstantField.MimeType.FOLDER.equalsIgnoreCase(o1.getMime()));
        }
        if (o2 != null) {
            time2 = o2.getTimestamp();
            isFolder2 = (ConstantField.MimeType.FOLDER.equalsIgnoreCase(o2.getMime()));
        }
        if (isFolder1 == isFolder2) {
            if (time1 == null || time2 == null) {
                if (time1 == null && time2 == null) {
                    return 0;
                } else if (time1 == null) {
                    return 1;
                } else {
                    return -1;
                }
            } else {
                return time2.compareTo(time1);
            }
        } else {
            return (isFolder1 ? -1 : 1);
        }
    });

    private Comparator<CustomizeFile> mimeAscendComparator = ((o1, o2) -> {
        String name1 = null;
        String name2 = null;
        boolean isFolder1 = false;
        boolean isFolder2 = false;
        if (o1 != null) {
            name1 = o1.getName();
            int lastIndex1 = name1.lastIndexOf(".");
            if (lastIndex1 >= 0 && (lastIndex1 + 1) < name1.length()) {
                name1 = name1.substring((lastIndex1 + 1));
            } else {
                name1 = "";
            }
            isFolder1 = (ConstantField.MimeType.FOLDER.equalsIgnoreCase(o1.getMime()));
        }
        if (o2 != null) {
            name2 = o2.getName();
            int lastIndex2 = name2.lastIndexOf(".");
            if (lastIndex2 >= 0 && (lastIndex2 + 1) < name2.length()) {
                name2 = name2.substring((lastIndex2 + 1));
            } else {
                name2 = "";
            }
            isFolder2 = (ConstantField.MimeType.FOLDER.equalsIgnoreCase(o2.getMime()));
        }
        if (isFolder1 == isFolder2) {
            if (name1 == null || name2 == null) {
                if (name1 == null && name2 == null) {
                    return 0;
                } else if (name1 == null) {
                    return 1;
                } else {
                    return -1;
                }
            } else {
                return name1.compareTo(name2);
            }
        } else {
            return (isFolder1 ? -1 : 1);
        }
    });

    private Comparator<CustomizeFile> mimeDescendComparator = ((o1, o2) -> {
        String name1 = null;
        String name2 = null;
        boolean isFolder1 = false;
        boolean isFolder2 = false;
        if (o1 != null) {
            name1 = o1.getName();
            int lastIndex1 = name1.lastIndexOf(".");
            if (lastIndex1 >= 0 && (lastIndex1 + 1) < name1.length()) {
                name1 = name1.substring((lastIndex1 + 1));
            } else {
                name1 = "";
            }
            isFolder1 = (ConstantField.MimeType.FOLDER.equalsIgnoreCase(o1.getMime()));
        }
        if (o2 != null) {
            name2 = o2.getName();
            int lastIndex2 = name2.lastIndexOf(".");
            if (lastIndex2 >= 0 && (lastIndex2 + 1) < name2.length()) {
                name2 = name2.substring((lastIndex2 + 1));
            } else {
                name2 = "";
            }
            isFolder2 = (ConstantField.MimeType.FOLDER.equalsIgnoreCase(o2.getMime()));
        }
        if (isFolder1 == isFolder2) {
            if (name1 == null || name2 == null) {
                if (name1 == null && name2 == null) {
                    return 0;
                } else if (name1 == null) {
                    return 1;
                } else {
                    return -1;
                }
            } else {
                return name2.compareTo(name1);
            }
        } else {
            return (isFolder1 ? -1 : 1);
        }
    });

    private HeaderFooterWrapper.ILoadMore loadMore = new HeaderFooterWrapper.ILoadMore() {
        @Override
        public void loadMore() {
            if (isLoadingEnable && fileEditView != null && mPage < mTotalPage) {
                fileEditView.getEulixSpaceStorage((mPage + 1), DataUtil.getFileSortOrder(getContext()));
            } else {
                footer.showBottom(getString(R.string.home_bottom_flag));
            }
        }
    };

    private static class FileAllHandler extends Handler {
        private WeakReference<FileAllFragment> fileAllFragmentWeakReference;

        public FileAllHandler(FileAllFragment fragment) {
            fileAllFragmentWeakReference = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            FileAllFragment fragment = fileAllFragmentWeakReference.get();
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
        root = inflater.inflate(R.layout.tab_file_all_fragment, container, false);
    }

    @NotNull
    @Override
    public FileAllPresenter createPresenter() {
        return new FileAllPresenter();
    }

    @Override
    public void initData() {
        handler = new FileAllHandler(this);
        Bundle arguments = getArguments();
        if (presenter != null) {
            if (arguments != null && arguments.containsKey(ConstantField.CATEGORY)) {
                mCategory = arguments.getString(ConstantField.CATEGORY, null);
            } else {
                mCategory = null;
            }
        }
        obtainParentFragment();
        int childFragmentIndex = ConstantField.FragmentIndex.FILE_ALL;
        if (mCategory != null) {
            switch (mCategory) {
                case ConstantField.Category.PICTURE:
                    childFragmentIndex = ConstantField.FragmentIndex.FILE_IMAGE;
                    break;
                case ConstantField.Category.VIDEO:
                    childFragmentIndex = ConstantField.FragmentIndex.FILE_VIDEO;
                    break;
                case ConstantField.Category.DOCUMENT:
                    childFragmentIndex = ConstantField.FragmentIndex.FILE_DOCUMENT;
                    break;
                case ConstantField.Category.OTHER:
                    childFragmentIndex = ConstantField.FragmentIndex.FILE_OTHER;
                    break;
                default:
                    break;
            }
        }
        switch (childFragmentIndex) {
            case ConstantField.FragmentIndex.FILE_ALL:
                OperationUtil.setFileAllRefresh(false);
                break;
            case ConstantField.FragmentIndex.FILE_IMAGE:
                OperationUtil.setFileImageRefresh(false);
                break;
            case ConstantField.FragmentIndex.FILE_VIDEO:
                OperationUtil.setFileVideoRefresh(false);
                break;
            case ConstantField.FragmentIndex.FILE_DOCUMENT:
                OperationUtil.setFileDocumentRefresh(false);
                break;
            case ConstantField.FragmentIndex.FILE_OTHER:
                OperationUtil.setFileOtherRefresh(false);
                break;
            default:
                break;
        }
    }

    @Override
    public void initView(@Nullable View root) {
        if (root != null) {
            fileSubViewContainer = root.findViewById(R.id.file_sub_view_container);
            refreshNow = root.findViewById(R.id.refresh_now);
            exceptionContainer = root.findViewById(R.id.exception_container);
            networkExceptionContainer = root.findViewById(R.id.network_exception_container);
            status404Container = root.findViewById(R.id.status_404_container);
            emptyFileContainer = root.findViewById(R.id.empty_file_container);
        }
        EventBusUtil.register(this);
    }

    @Override
    public void initViewData() {
        Context context = getContext();
        if (context != null) {
            maxChildCount = Math.max((int) (Math.ceil((ViewUtils.getScreenHeight(context) - ViewUtils.getStatusBarHeight(context))
                    * 1.0 / context.getResources().getDimensionPixelSize(R.dimen.dp_80))), maxChildCount);
        }
        if (parentFragment != null) {
            fileEditView = new FileEditView(parentFragment.getActivity(), mCategory);
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(ThumbEvent event) {
        Logger.d("zfy", "onReceive ThumbEvent:" + event.uuid);
        for (int i = 0; i < adapter.mCustomizeFileList.size(); i++) {
            if (adapter.mCustomizeFileList.get(i).getId().equals(event.uuid)) {
                int realPosition = headerFooterWrapper.getHeaderCount() + i;
                headerFooterWrapper.notifyItemChanged(realPosition, "refresh_thumb");
                break;
            }
        }
    }

    @Override
    public void initEvent() {
        fileEditView.registerCallback(this);
        fileEditView.registerPluralCallback(this);
        if (refreshNow != null) {
            refreshNow.setOnClickListener(this);
        }
        String currentId = generateRootCurrentId();
        if (currentId != null) {
            getLocalEulixSpaceStorage(currentId, true, false, false);
        }
        refreshEulixSpaceStorage(true);
    }

    public void onSmartRefreshListener(){
        ThumbManager.getInstance().cancelCache();
        isLocalEmpty = true;
        refreshEulixSpaceStorage(false, true);
    }

    private void addFileSubView() {
        Context context = getContext();
        if (context != null) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.file_sub_view, null);
            list = view.findViewById(R.id.file_list);
            customizeFiles = new ArrayList<>();
            adapter = new FileAdapter(context, customizeFiles, ConstantField.ViewType.BOX_FILE_LINEAR_VIEW, false, false);
            adapter.setOnItemClickListener(this);
            list.setLayoutManager(new LinearLayoutManager(context, RecyclerView.VERTICAL, false));
            list.addItemDecoration(new FileAdapter.ItemDecoration(RecyclerView.VERTICAL, Math.round(context.getResources().getDimension(R.dimen.dp_1))
                    , context.getResources().getColor(R.color.white_fff7f7f9)));
            headerFooterWrapper = new HeaderFooterWrapper(adapter);
            list.setAdapter(headerFooterWrapper);
            footer = new FooterView(getActivity());
            adapter.setFooterView(footer);
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
    }

    private void removeFileSubView() {
        if (fileSubViewContainer != null) {
            int childCount = fileSubViewContainer.getChildCount();
            if (childCount > 1) {
                View childView = fileSubViewContainer.getChildAt((childCount - 2));
                if (childView != null) {
                    list = childView.findViewById(R.id.file_list);
                    if (list != null) {
                        if (list.getAdapter() != null && list.getAdapter() instanceof HeaderFooterWrapper) {
                            headerFooterWrapper = (HeaderFooterWrapper) list.getAdapter();
                            if (headerFooterWrapper != null && headerFooterWrapper.dataAdapter != null && headerFooterWrapper.dataAdapter instanceof FileAdapter) {
                                adapter = (FileAdapter) headerFooterWrapper.dataAdapter;
                                customizeFiles = adapter.getmCustomizeFileList();
                                footer = adapter.getFooterView();
                            }
                        }
                    }
                    childView.setVisibility(View.VISIBLE);
                }
                fileSubViewContainer.removeViewAt((childCount - 1));
            }
        }
    }

    private void setFooter(boolean isAdd, boolean isForce) {
        if (isAdd) {
            if (headerFooterWrapper != null && headerFooterWrapper.getFooterViewSize() <= 0 && list != null && footer != null) {
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
        //v1.9.0保持底部统一展示
        /*if (footer != null) {
            ViewGroup.LayoutParams param = footer.getLayoutParams();

            if (isVisible) {
                param.width = ViewGroup.LayoutParams.MATCH_PARENT;
                param.height = getResources().getDimensionPixelSize(R.dimen.dp_54);
            } else {
                param.width = 0;
                param.height = 0;
            }
            footer.setLayoutParams(param);
        }*/
    }

    private void handleDataResult(int statusCode) {
        switch (statusCode) {
            case -3:
                networkExceptionContainer.setVisibility(View.INVISIBLE);
                status404Container.setVisibility(View.INVISIBLE);
                emptyFileContainer.setVisibility(View.INVISIBLE);
                exceptionContainer.setVisibility(View.GONE);
                fileSubViewContainer.setVisibility(View.VISIBLE);
                break;
            case -2:
                fileSubViewContainer.setVisibility(View.INVISIBLE);
                exceptionContainer.setVisibility(View.VISIBLE);
                networkExceptionContainer.setVisibility(View.INVISIBLE);
                status404Container.setVisibility(View.INVISIBLE);
                emptyFileContainer.setVisibility(View.VISIBLE);
                break;
            case ConstantField.OBTAIN_ACCESS_TOKEN_CODE:
                if (parentFragment != null) {
                    parentFragment.obtainAccessToken();
                }
                if (customizeFiles == null || customizeFiles.size() <= 0) {
                    handleDataResult(-2);
                }
                break;
            case ConstantField.FILE_DISCONNECT_CODE:
                if (customizeFiles == null || customizeFiles.size() <= 0) {
                    handleDataResult(-2);
                }
                if (footer != null) {
                    footer.showBottom(getString(R.string.home_bottom_flag));
                }
                showImageTextToast(R.drawable.toast_refuse, R.string.active_device_offline_hint);
                break;
            case ConstantField.SERVER_EXCEPTION_CODE:
                if (customizeFiles == null || customizeFiles.size() <= 0) {
                    handleDataResult(-2);
                }
                if (footer != null) {
                    footer.showBottom(getString(R.string.home_bottom_flag));
                }
                showServerExceptionToast();
                break;
            case ConstantField.NETWORK_ERROR_CODE:
                fileSubViewContainer.setVisibility(View.GONE);
                exceptionContainer.setVisibility(View.VISIBLE);
                networkExceptionContainer.setVisibility(View.VISIBLE);
                status404Container.setVisibility(View.INVISIBLE);
                emptyFileContainer.setVisibility(View.INVISIBLE);
                break;
            default:
                resetListData();
                fileSubViewContainer.setVisibility(View.GONE);
                exceptionContainer.setVisibility(View.VISIBLE);
                networkExceptionContainer.setVisibility(View.INVISIBLE);
                status404Container.setVisibility(View.VISIBLE);
                emptyFileContainer.setVisibility(View.INVISIBLE);
                break;
        }
    }

    private void obtainParentFragment() {
        if (parentFragment == null) {
            Fragment fragment = getParentFragment();
            if (fragment instanceof TabFileFragment) {
                parentFragment = (TabFileFragment) fragment;
            }
        }
    }

    private void changeEditAdapterView(int showType) {
        if (adapter != null && list != null) {
            adapter.changeEditStatus(showType);
            int visibleCount = list.getChildCount();
            for (int i = 0; i < visibleCount; i++) {
                View child = list.getChildAt(i);
                if (child != null) {
                    switch (adapter.getItemViewType(0)) {
                        case ConstantField.ViewType.BOX_FILE_LINEAR_VIEW:
                            adapter.setMarkPattern(child, showType);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    /**
     * 打开在线文件夹
     *
     * @param customizeFileList
     */
    private void openDirectory(String currentDirectory, List<CustomizeFile> customizeFileList, boolean isLocal, boolean isBack) {
        if (!isBack) {
            setFooter(false, true);
        }
        List<Integer> selectionPositionList = null;
        if (customizeFileList != null) {
            if (!isLocal) {
                mPage = 1;
            }
        }
        if (customizeFileList == null) {
            customizeFileList = new ArrayList<>();
        }
        handleDataResult(customizeFileList.size() <= 0 ? -2 : -3);
        customizeFiles = customizeFileList;
        if (adapter != null && headerFooterWrapper != null) {
            if (selectIds == null) {
                adapter.updateData(customizeFiles, false);
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
                adapter.updateData(customizeFiles, selectionPositionList);
            }
            headerFooterWrapper.notifyDataSetChanged();
        }
        if (parentFragment != null) {
            String title = parentFragment.getUuidTitle(currentDirectory);
            if (selectionPositionList == null) {
                parentFragment.handleTitleVisibility(title, mCategory);
            } else {
                parentFragment.handleTitleVisibility(selectionPositionList.size(), customizeFileList.size(), title == null ? "" : title, mCategory);
            }
        }
    }

    /**
     * 在线文件夹增加一页
     *
     * @param customizeFileList
     * @param pageInfo
     */
    private void addDirectory(List<CustomizeFile> customizeFileList, PageInfo pageInfo) {
        if (pageInfo != null && pageInfo.getPage() != null) {
            List<Integer> selectionPositionList = null;
            mPage = Math.max(mPage, pageInfo.getPage());
            List<String> currentIds = new ArrayList<>();
            if (customizeFiles != null) {
                for (CustomizeFile customizeFile : customizeFiles) {
                    if (customizeFile != null) {
                        currentIds.add(customizeFile.getId());
                    }
                }
                if (customizeFileList != null) {
                    for (CustomizeFile customizeFile : customizeFileList) {
                        if (customizeFile != null && !currentIds.contains(customizeFile.getId())) {
                            customizeFiles.add(customizeFile);
                        }
                    }
                }
                if (adapter != null && headerFooterWrapper != null) {
                    if (selectIds == null) {
                        adapter.updateData(customizeFiles, false);
                        headerFooterWrapper.notifyDataSetChanged();
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
                        adapter.updateData(customizeFiles, selectionPositionList);
                        headerFooterWrapper.notifyDataSetChanged();
                    }
                }
            }
            if (parentFragment != null && selectionPositionList != null) {
                parentFragment.handleTitleVisibility(selectionPositionList.size(), customizeFiles.size(), mCategory);
            }
        }
    }

    private void resetListData() {
        if (customizeFiles != null) {
            customizeFiles.clear();
        }
        if (adapter != null && headerFooterWrapper != null) {
            adapter.updateData(customizeFiles, false);
            headerFooterWrapper.notifyDataSetChanged();
        }
    }

    private void handleSwipeEnable() {
        if (parentFragment != null) {
            parentFragment.setSmartRefreshLayoutEnable(selectIds == null);
        }
    }

    public int getFileDepth() {
        if (getSearchDepth() > 0) {
            return ((fileEditView == null ? 0 : fileEditView.getDepth())) + getSearchDepth();
        } else {
            return ((fileEditView == null ? 0 : fileEditView.getDepth()) - 1);
        }
    }

    public int getSearchDepth() {
        return 0;
//        return ((fileEditView == null ? 0 : fileEditView.getSearchDepth()));
    }

    public int getSelectNumber() {
        int number = -1;
        if (adapter != null) {
            List<Integer> selectPosition = adapter.getSelectPosition();
            if (selectPosition != null) {
                number = selectPosition.size();
            }
        }
        return number;
    }

    public void handleFileEditView(int selectNumber) {
        if (fileEditView != null) {
            if (selectNumber > 0) {
                fileEditView.showFileDialog(selectNumber);
            } else {
                fileEditView.dismissFileDialog();
            }
        }
    }

    public void handleSelectAll(boolean selectAll) {
        isSelectAll = selectAll;
    }

    public void handleSelectTotal() {
        if (adapter != null && customizeFiles != null) {
            List<Integer> selectPosition = adapter.getSelectPosition();
            int selectNumber = -1;
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
                adapter.changeEditStatus(ConstantField.ShowType.SELECT);
                if (parentFragment != null) {
                    parentFragment.handleTitleVisibility(totalNumber, totalNumber, mCategory);
                }
            } else {
                if (selectIds != null) {
                    selectIds.clear();
                }
                changeEditAdapterView(ConstantField.ShowType.EDIT);
                adapter.changeEditStatus(ConstantField.ShowType.EDIT);
                if (parentFragment != null) {
                    parentFragment.handleTitleVisibility(0, mCategory);
                }
            }
            handleSwipeEnable();
        }
    }

    public void handleEditMode() {
        obtainParentFragment();
        if (adapter != null && parentFragment != null) {
            changeEditAdapterView(ConstantField.ShowType.EDIT);
            adapter.changeEditStatus(ConstantField.ShowType.EDIT);
            List<Integer> selectPosition = adapter.getSelectPosition();
            int selectNumber = -1;
            if (selectPosition != null) {
                selectNumber = selectPosition.size();
            }
            if (customizeFiles != null) {
                parentFragment.handleTitleVisibility(selectNumber, customizeFiles.size(), mCategory);
            }
            if (selectNumber >= 0) {
                if (customizeFiles != null) {
                    if (selectIds == null) {
                        selectIds = new ArrayList<>();
                    } else {
                        selectIds.clear();
                    }
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

    public void folderChange(List<UUID> newFolderList) {
        if (fileEditView != null && newFolderList != null) {
            UUID uuid = fileEditView.getUUID();
            String folderUUIDValue = ConstantField.UUID.FILE_ROOT_UUID;
            if (uuid != null) {
                folderUUIDValue = uuid.toString();
            }
            for (UUID newFolderUUID : newFolderList) {
                if (newFolderUUID != null && newFolderUUID.toString().equals(folderUUIDValue)) {
                    refreshEulixSpaceStorage(false);
                    break;
                }
            }
        }
    }

    public void folderChange(UUID folderUuid, boolean isSearch) {
        if (fileEditView != null && folderUuid != null) {
            getLocalEulixSpaceStorage(folderUuid.toString(), true, false, isSearch);
            //todo
            //fileEditView.getEulixSpaceStorage(folderUuid, 1, null, DataUtil.getFileSortOrder(getContext()));
        }
    }

    public void folderChange(List<UUID> newFolderList, String folderName, String folderUuid) {
        if (fileEditView != null && folderUuid != null && folderName != null) {
            parentFragment.setUuidTitle(folderUuid, folderName);
            UUID nFolderUuid = null;
            try {
                nFolderUuid = UUID.fromString(folderUuid);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (nFolderUuid != null) {
                getLocalEulixSpaceStorage(folderUuid, true, false, false);
                fileEditView.getEulixSpaceStorage(nFolderUuid, 1, null, DataUtil.getFileSortOrder(getContext()));
            }
        }
    }

    public void stackChange(ArrayStack<UUID> uuids) {
        if (fileEditView != null) {
            fileEditView.setStack(uuids);
        }
        if (uuids != null) {
            UUID uuid = uuids.peek();
            String currentId;
            if (uuid == null) {
                currentId = generateRootCurrentId();
            } else {
                currentId = uuid.toString();
            }
            getLocalEulixSpaceStorage(currentId, false, false, false);
        }
        refreshEulixSpaceStorage(true);
    }

    public String generateRootCurrentId() {
        String currentId = null;
        if (mCategory == null) {
            currentId = ConstantField.Category.FILE_ROOT;
        } else {
            switch (mCategory) {
                case ConstantField.Category.PICTURE:
                    currentId = ConstantField.Category.FILE_IMAGE;
                    break;
                case ConstantField.Category.VIDEO:
                    currentId = ConstantField.Category.FILE_VIDEO;
                    break;
                case ConstantField.Category.DOCUMENT:
                    currentId = ConstantField.Category.FILE_DOCUMENT;
                    break;
                case ConstantField.Category.OTHER:
                    currentId = ConstantField.Category.FILE_OTHER;
                    break;
                default:
                    break;
            }
        }
        return currentId;
    }

    public void getLocalEulixSpaceStorage(String currentId, boolean isNext, boolean isBack, boolean isSearch) {
        boolean tempLoadingEnable = isLoadingEnable;
        isLoadingEnable = false;
        if (selectIds != null) {
            selectIds.clear();
            selectIds = null;
        }
        handleSwipeEnable();
        if (parentFragment != null){
            parentFragment.cancelSmartRefreshing();
        }
        if (presenter != null) {
            String queryCurrentId = currentId;
            if (queryCurrentId != null && currentId.equals(ConstantField.UUID.FILE_ROOT_UUID)) {
                queryCurrentId = generateRootCurrentId();
            }
            List<CustomizeFile> customizeFiles = presenter.getLocalEulixSpaceStorage(queryCurrentId);
            isLocalEmpty = (customizeFiles == null);
            if (ConstantField.Category.FILE_ROOT.equals(currentId) || ConstantField.Category.FILE_IMAGE.equals(currentId)
                    || ConstantField.Category.FILE_VIDEO.equals(currentId) || ConstantField.Category.FILE_DOCUMENT.equals(currentId)
                    || ConstantField.Category.FILE_OTHER.equals(currentId)) {
                currentId = ConstantField.UUID.FILE_ROOT_UUID;
            }
            if (fileEditView != null && isNext) {
                if (adapter != null) {
                    adapter.setLoadingEnable(tempLoadingEnable);
                    adapter.setCurrentPage(mPage);
                    adapter.setTotalPage(mTotalPage);
                }
                addFileSubView();
                fileEditView.handleNext(currentId, isSearch);
            }
            openDirectory(currentId, customizeFiles, true, isBack);
        }
        if (list != null && (!list.canScrollVertically(-1) && !list.canScrollVertically(1))
                && customizeFiles != null && customizeFiles.size() < maxChildCount) {
            setFooter(true, false);
            setFooterVisible(false);
            footer.showBottom(getString(R.string.home_bottom_flag));
        } else if (footer != null) {
            setFooter(true, true);
            setFooterVisible(true);
            footer.showBottom(getString(R.string.home_bottom_flag));
        }
    }

    public void refreshEulixSpaceStorage(boolean isInit) {
        refreshEulixSpaceStorage(isInit, false);
    }

    public void refreshEulixSpaceStorage(boolean isInit, boolean isFore) {
        if (fileEditView != null) {
            fileEditView.getEulixSpaceStorage(1, DataUtil.getFileSortOrder(getContext()), isFore);
        }
    }

    public UUID getCurrentFolderUUID() {
        UUID uuid = null;
        if (fileEditView != null) {
            uuid = fileEditView.getUUID();
        }
        return uuid;
    }

    public ArrayStack<UUID> getCurrentUUIDStack() {
        ArrayStack<UUID> uuids = null;
        if (fileEditView != null) {
            uuids = fileEditView.getUUIDStack();
        }
        return uuids;
    }

    public void reset() {
        if (selectIds != null) {
            selectIds.clear();
            selectIds = null;
        }
        handleSwipeEnable();
        resetListData();
        String currentId = generateRootCurrentId();
        if (currentId != null) {
            getLocalEulixSpaceStorage(currentId, true, false, false);
        }
        if (fileEditView != null) {
            fileEditView.reset();
        }
        refreshEulixSpaceStorage(true);
    }

    private void handlePageInfo(PageInfo pageInfo, String currentDirectory, List<CustomizeFile> customizeFiles) {
        Integer pageValue = pageInfo.getPage();
        Integer totalPageValue = pageInfo.getTotal();
        if (totalPageValue != null) {
            mTotalPage = totalPageValue;
        }
        if (pageValue != null && pageValue > 1 && fileEditView.isUUIDSame()) {
            addDirectory(customizeFiles, pageInfo);
        } else {
            openDirectory(currentDirectory, customizeFiles, false, false);
        }
        if (mTotalPage > 1) {
            Integer pageSizeValue = pageInfo.getPageSize();
            if (pageSizeValue != null) {
                int pageSize = pageSizeValue;
                if (pageSize <= 0) {
                    pageSize = customizeFiles.size();
                }
                pageSize = Math.max(pageSize, 1);
                if (fileEditView != null && (pageSize * mPage) <= maxChildCount && mPage < mTotalPage) {
                    fileEditView.getEulixSpaceStorage((mPage + 1), DataUtil.getFileSortOrder(getContext()));
                }
            }
        }
        if (mPage == mTotalPage) {
            if (list != null && (!list.canScrollVertically(-1) && !list.canScrollVertically(1))) {
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

    private void offlineSort(String sort) {
        if (sort != null && customizeFiles != null) {
            switch (sort) {
                case ConstantField.Sort.NAME_ASCEND:
                    Collections.sort(customizeFiles, nameAscendComparator);
                    break;
                case ConstantField.Sort.NAME_DESCEND:
                    Collections.sort(customizeFiles, nameDescendComparator);
                    break;
                case ConstantField.Sort.OPERATION_TIME_ASCEND:
                    Collections.sort(customizeFiles, timeAscendComparator);
                    break;
                case ConstantField.Sort.OPERATION_TIME_DESCEND:
                    Collections.sort(customizeFiles, timeDescendComparator);
                    break;
                case ConstantField.Sort.MIME_ASCEND:
                    Collections.sort(customizeFiles, mimeAscendComparator);
                    break;
                case ConstantField.Sort.MIME_DESCEND:
                    Collections.sort(customizeFiles, mimeDescendComparator);
                    break;
                default:
                    break;
            }
        }
        if (fileEditView != null) {
            String currentId;
            UUID uuid = fileEditView.getUUID();
            if (uuid == null) {
                currentId = generateRootCurrentId();
            } else {
                currentId = uuid.toString();
            }
            isLoadingEnable = false;
            openDirectory(currentId, customizeFiles, true, false);
        }
    }

    private void refreshAgain() {
        ThumbManager.getInstance().cancelCache();
        isLocalEmpty = true;
        refreshEulixSpaceStorage(false);
    }

    public void changeSort(String sort) {
        if (parentFragment != null){
            parentFragment.cancelSmartRefreshing();
        }
        if (presenter != null && !presenter.isOnline()) {
            offlineSort(sort);
        }

        if (fileEditView != null) {
            fileEditView.getEulixSpaceStorage(1, sort);
        }
    }

    public boolean isShareDialogShow() {
        return (fileEditView != null && fileEditView.isShareDialogShowing());
    }

    public void dismissShareDialog(String absolutePath, boolean isShare) {
        if (fileEditView != null) {
            fileEditView.dismissShareDialog(absolutePath, isShare);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        obtainParentFragment();
        if (parentFragment != null) {
            int childFragmentIndex = ConstantField.FragmentIndex.FILE_ALL;
            if (mCategory != null) {
                switch (mCategory) {
                    case ConstantField.Category.PICTURE:
                        childFragmentIndex = ConstantField.FragmentIndex.FILE_IMAGE;
                        break;
                    case ConstantField.Category.VIDEO:
                        childFragmentIndex = ConstantField.FragmentIndex.FILE_VIDEO;
                        break;
                    case ConstantField.Category.DOCUMENT:
                        childFragmentIndex = ConstantField.FragmentIndex.FILE_DOCUMENT;
                        break;
                    case ConstantField.Category.OTHER:
                        childFragmentIndex = ConstantField.FragmentIndex.FILE_OTHER;
                        break;
                    default:
                        break;
                }
            }
            parentFragment.setChildFragmentIndex(childFragmentIndex);
            switch (childFragmentIndex) {
                case ConstantField.FragmentIndex.FILE_ALL:
                    if (OperationUtil.isFileAllRefresh()) {
                        OperationUtil.setFileAllRefresh(false);
                        refreshAgain();
                    }
                    break;
                case ConstantField.FragmentIndex.FILE_IMAGE:
                    if (OperationUtil.isFileImageRefresh()) {
                        OperationUtil.setFileImageRefresh(false);
                        refreshAgain();
                    }
                    break;
                case ConstantField.FragmentIndex.FILE_VIDEO:
                    if (OperationUtil.isFileVideoRefresh()) {
                        OperationUtil.setFileVideoRefresh(false);
                        refreshAgain();
                    }
                    break;
                case ConstantField.FragmentIndex.FILE_DOCUMENT:
                    if (OperationUtil.isFileDocumentRefresh()) {
                        OperationUtil.setFileDocumentRefresh(false);
                        refreshAgain();
                    }
                    break;
                case ConstantField.FragmentIndex.FILE_OTHER:
                    if (OperationUtil.isFileOtherRefresh()) {
                        OperationUtil.setFileOtherRefresh(false);
                        refreshAgain();
                    }
                    break;
                default:
                    break;
            }
        }
        if (mCategory == null) {
            UUID fileSearchUuid = DataUtil.getFileSearchUuid();
            if (fileSearchUuid != null) {
                folderChange(fileSearchUuid, true);
                DataUtil.setFileSearchUuid(null);
            }
        }
    }

    @Override
    public void onPause() {
        obtainParentFragment();
        if (parentFragment != null) {
            parentFragment.onLogUpPage(-1);
        }
        super.onPause();
    }

    @Override
    public void onDestroy() {
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
            handler = null;
        }
        ThumbManager.getInstance().cancelCache();
        super.onDestroy();
    }

    @Override
    public boolean handleBackEvent() {
        if (adapter == null) {
            return false;
        }
        if (adapter.getSelectPosition() != null) {
            if (handler != null) {
                handler.post(() -> {
                    changeEditAdapterView(ConstantField.ShowType.NORMAL);
                    adapter.changeEditStatus(ConstantField.ShowType.NORMAL);
                    if (parentFragment != null) {
                        parentFragment.handleTitleVisibility(-1, mCategory);
                    }
                });
            }
            if (selectIds != null) {
                selectIds.clear();
                selectIds = null;
            }
            handleSwipeEnable();
            return true;
        }
        if (getFileDepth() > 0) {
            removeFileSubView();
            UUID uuid = fileEditView.handleBack();
            if (uuid != null) {
                if (ConstantField.UUID.FILE_ROOT_UUID.equals(uuid.toString())) {
                    String currentId = generateRootCurrentId();
                    if (currentId != null) {
                        getLocalEulixSpaceStorage(currentId, false, true, (getSearchDepth() > 0));
                    }
                } else {
                    getLocalEulixSpaceStorage(uuid.toString(), false, true, (getSearchDepth() > 0));
                }
                if (adapter != null) {
                    mPage = adapter.getCurrentPage();
                    mTotalPage = adapter.getTotalPage();
                    isLoadingEnable = adapter.isLoadingEnable();
                }
                if (mPage == mTotalPage) {
                    if (list != null && (!list.canScrollVertically(-1) && !list.canScrollVertically(1))) {
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
                setFooter(true, false);
//                fileEditView.getEulixSpaceStorage(uuid, 1, null, DataUtil.getFileSortOrder(getContext()));
            }
            return true;
        }
        return false;
    }

    @Override
    public void handleRefresh(boolean isSuccess, String serviceFunction) {
        if (isSuccess && handler != null && serviceFunction != null) {
            switch (serviceFunction) {
                case ConstantField.ServiceFunction.MODIFY_FILE:
                case ConstantField.ServiceFunction.DELETE_FILE:
                case ConstantField.ServiceFunction.MOVE_FILE:
                    handler.post(() -> refreshEulixSpaceStorage(false));
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public List<CustomizeFile> getSelectFiles() {
        List<CustomizeFile> files = new ArrayList<>();
        if (adapter != null && customizeFiles != null) {
            List<Integer> selectIndex = adapter.getSelectPosition();
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
        if (parentFragment != null) {
            parentFragment.fileDialog(view, isShow);
        }
    }

    @Override
    public void handleGetEulixSpaceFileListResult(Integer code, String currentDirectory, List<CustomizeFile> customizeFiles, PageInfo pageInfo) {
        if (handler != null && presenter != null) {
            handler.post(() -> {
                if (parentFragment != null){
                    parentFragment.cancelSmartRefreshing();
                }
                if (currentDirectory == null && customizeFiles == null) {
                    isLoadingEnable = true;
                    if (isLocalEmpty) {
                        handleDataResult(code);
                    }
                } else {
                    UUID uuid = fileEditView.getUUID();
                    String mUuid;
                    if (uuid == null) {
                        mUuid = generateRootCurrentId();
                        if (mUuid != null) {
                            mUuid = ConstantField.UUID.FILE_ROOT_UUID;
                        }
                    } else {
                        mUuid = uuid.toString();
                    }
                    if (mUuid != null && mUuid.equals(currentDirectory)) {
                        isLoadingEnable = true;
                        if (customizeFiles == null) {
                            if (isLocalEmpty) {
                                handleDataResult(code);
                            }
                        } else {
                            if (pageInfo != null) {
                                handlePageInfo(pageInfo, currentDirectory, customizeFiles);
                            } else {
                                mTotalPage = 1;
                                openDirectory(currentDirectory, customizeFiles, false, false);
                            }
                        }
                    } else {
                        Logger.d(TAG, "result expired");
                    }
                }
                setFooter(true, false);
            });
        }
    }

    @Override
    public ArrayStack<UUID> handleCurrentUUIDStack() {
        return DataUtil.cloneUUIDStack(getCurrentUUIDStack());
    }

    @Override
    public void handleRefreshEulixSpaceStorage(UUID parentUUID) {
        if (parentFragment != null) {
            parentFragment.handleRefreshEulixSpaceStorage(parentUUID);
        }
    }

    @Override
    public void handleDismissFolderListView(boolean isConfirm, UUID selectUUID, Boolean isCopy, ArrayStack<UUID> uuids, List<UUID> newFolderUUIDs) {
        if (parentFragment != null) {
            parentFragment.handleDismissFolderListView(isConfirm, selectUUID, isCopy, uuids, newFolderUUIDs);
        }
    }

    @Override
    public void handleShowOrDismissFileSearch(boolean isShow) {
        if (isShow) {
//            if (parentFragment != null) {
//                parentFragment.startFileSearch(null);
//            }
        } else {
//            DataUtil.setFileSearchData(null);
        }
    }

    @Override
    public void handleFolderDetail(String folderUuid, String name, Long operationAt, String path, Long size) {
        if (handler != null) {
            handler.post(() -> {
                if (customizeFiles != null && folderUuid != null) {
                    for (CustomizeFile customizeFile : customizeFiles) {
                        if (customizeFile != null && folderUuid.equals(customizeFile.getId()) && ConstantField.MimeType.FOLDER.equals(customizeFile.getMime())) {
                            if (size != null) {
                                customizeFile.setSize(size);
                            }
                            break;
                        }
                    }
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.refresh_now:
                    isLocalEmpty = true;
                    refreshEulixSpaceStorage(false, true);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onItemClick(View view, int position, boolean isEnable) {
        obtainParentFragment();
        if (adapter != null && parentFragment != null) {
            List<Integer> selectPosition = adapter.getSelectPosition();
            int selectNumber = -1;
            if (selectPosition != null) {
                selectNumber = selectPosition.size();
            }
            if (customizeFiles != null) {
                parentFragment.handleTitleVisibility(selectNumber, customizeFiles.size(), mCategory);
            }
            if (selectNumber >= 0) {
                if (customizeFiles != null) {
                    if (selectIds == null) {
                        selectIds = new ArrayList<>();
                    } else {
                        selectIds.clear();
                    }
                    for (int selectP : selectPosition) {
                        if (selectP >= 0 && customizeFiles.size() > selectP) {
                            CustomizeFile customizeFile = customizeFiles.get(selectP);
                            if (customizeFile != null) {
                                selectIds.add(customizeFile.getId());
                            }
                        }
                    }
                }
            } else {
                if (selectIds != null) {
                    selectIds.clear();
                    selectIds = null;
                }
                if (isEnable && presenter != null && customizeFiles != null && position >= 0 && customizeFiles.size() > position) {
                    CustomizeFile customizeFile = customizeFiles.get(position);
                    if (customizeFile != null) {
                        String mime = customizeFile.getMime();
                        if (mime != null && mime.equalsIgnoreCase(ConstantField.MimeType.FOLDER)) {
                            String titleContent = customizeFile.getName();
                            String id = customizeFile.getId();
                            if (id != null) {
                                parentFragment.setUuidTitle(id, titleContent);
                                UUID uuid = null;
                                try {
                                    uuid = UUID.fromString(customizeFile.getId());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (uuid != null) {
                                    getLocalEulixSpaceStorage(uuid.toString(), true, false, (getSearchDepth() > 0));
                                    fileEditView.getEulixSpaceStorage(uuid, 1, null, DataUtil.getFileSortOrder(getContext()));
                                }
                            }
                        } else {
                            //预览文件
                            startPreviewFile(customizeFile);
                        }
                    }
                }
            }
            handleSwipeEnable();
        }
    }

    private void startPreviewFile(CustomizeFile customizeFile) {
        String mimeType = FileUtil.getMimeTypeByPath(customizeFile.getName());
        if (mimeType.contains("image") || mimeType.contains("video")) {
            List<CustomizeFile> imageList = new ArrayList<>();
            int selectPosition = 0;
            int imageIndex = 0;
            for (int i = 0; i < customizeFiles.size(); i++) {
                if (FileUtil.getMimeTypeByPath(customizeFiles.get(i).getName()).contains("image")
                        || FileUtil.getMimeTypeByPath(customizeFiles.get(i).getName()).contains("video")) {
                    imageList.add(customizeFiles.get(i));
                    if (customizeFile == customizeFiles.get(i)) {
                        selectPosition = imageIndex;
                    }
                    imageIndex++;
                }
            }
            FilePreviewActivity.openImgList(getActivity(), imageList, selectPosition, TransferHelper.FROM_FILE, -1, -1);
        } else {
            Intent intent = new Intent(getActivity(), FilePreviewActivity.class);
            intent.putExtra(FilePreviewActivity.KEY_FILE_NAME, customizeFile.getName());
            intent.putExtra(FilePreviewActivity.KEY_FILE_PATH, customizeFile.getPath());
            intent.putExtra(FilePreviewActivity.KEY_FILE_UUID, customizeFile.getId());
            intent.putExtra(FilePreviewActivity.KEY_FILE_SIZE, customizeFile.getSize());
            intent.putExtra(FilePreviewActivity.KEY_FILE_MD5, customizeFile.getMd5());
            intent.putExtra(FilePreviewActivity.KEY_FILE_TIME, customizeFile.getTimestamp());
            startActivity(intent);
            getActivity().overridePendingTransition(0, android.R.anim.fade_out);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        EventBusUtil.unRegister(this);
    }
}
