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

package xyz.eulix.space.view.dialog.folder;

import android.app.Dialog;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Adapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import xyz.eulix.space.R;
import xyz.eulix.space.adapter.files.FileAdapter;
import xyz.eulix.space.bean.ArrayStack;
import xyz.eulix.space.bean.CustomizeFile;
import xyz.eulix.space.network.files.PageInfo;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.DataUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.ToastManager;
import xyz.eulix.space.util.ViewUtils;
import xyz.eulix.space.view.BottomDialog;
import xyz.eulix.space.view.rv.FooterView;
import xyz.eulix.space.view.rv.HeaderFooterWrapper;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/9/2 16:17
 */
public class FolderListView implements FolderListController.IFolderListCallback, FileAdapter.OnItemClickListener {
    private static final String TAG = FolderListView.class.getSimpleName();
    public static final int FOLDER_DEPTH_MAX_VALUE = 20;
    private Context mContext;
    private FolderListController mController;
    private FolderListCallback mCallback;
    private FolderNewCallback mFolderNewCallback;
    private ArrayStack<FolderListCallback> mCallbacks;
    private ArrayStack<FolderNewCallback> mFolderNewCallbacks;
    private ImageButton folderListBack, folderListExit;
    private Button folderListNewFolder, folderListConfirm, newFolderDialogCancel, newFolderDialogConfirm;
    private Button refreshNow;
    private TextView folderListTitle, folderListHint, newFolderDialogTitle;
    private EditText newFolderDialogInput;
    private ImageButton newFolderDialogInputClear;
    private FrameLayout fileSubViewContainer;
    private RecyclerView folderListFileList;
    private RelativeLayout folderListExceptionContainer;
    private LinearLayout folderListHintContainer, folderListNetworkExceptionContainer, folderListStatus404Container, folderListEmptyFolderContainer;
    private SwipeRefreshLayout swipeRefreshContainer;
    private View folderListDialogView, newFolderDialogView;
    private Dialog folderListDialog, newFolderDialog;
    private FileAdapter adapter;
    private HeaderFooterWrapper headerFooterWrapper;
    private FooterView footer;
    private List<CustomizeFile> customizeFiles;
    private List<UUID> newFolderUUIDList;
    // null: 上传; true: 复制；false：移动
    private Boolean isCopy = null;
    private boolean isSelfNewFolder;
    private int mPage = 1;
    private int mTotalPage = 1;
    private int maxChildCount = 7;
    // 上拉加载使能，本地加载时失效，网络加载到来之后生效
    private boolean isLoadingEnable = false;
    // 本地加载是否存在，不存在则展示网络错误，刷新时强制为true
    private boolean isLocalEmpty = true;
    private ToastManager toastManager;
    private FolderListHandler mHandler;

    public interface FolderListCallback {
        // 刷新Access Token
        void obtainAccessToken();
        // 复制、移动使用，操作文件数量，上传不用
        int getSelectFilesSize();
        // 当前文件夹uuid，直接调用新建文件夹而非通过dialog调用使用
        UUID getCurrentFolderUUID();
        // 当前文件夹uuid树
        ArrayStack<UUID> getCurrentUUIDStack();
        // 刷新数据接口
        void refreshEulixSpaceStorage(UUID parentUUID);
        // 主dialog消失时调用, isCopy：null：上传；true：复制；false：移动
        void dismissFolderListView(boolean isConfirm, UUID selectUUID, Boolean isCopy, ArrayStack<UUID> uuids, List<UUID> newFolderUUIDs);
    }

    public interface FolderNewCallback {
        // 单独点击新建文件夹并且确认后使用，目的是覆盖原有视图
        void dismissConfirmOuterNewFolderCallback(ArrayStack<UUID> uuids);
        // 刷新数据接口并进入当前文件夹
        void refreshEulixSpaceStorage(UUID parentUUID, String folderName, String folderUuid);
    }

    private HeaderFooterWrapper.ILoadMore loadMore = new HeaderFooterWrapper.ILoadMore() {
        @Override
        public void loadMore() {
            if (isLoadingEnable && mController != null && mPage < mTotalPage) {
                mController.getEulixSpaceStorage((mPage + 1));
            }
        }
    };

    static class FolderListHandler extends Handler {
        private WeakReference<FolderListView> folderListViewWeakReference;

        public FolderListHandler(FolderListView view) {
            folderListViewWeakReference = new WeakReference<>(view);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            FolderListView view = folderListViewWeakReference.get();
            if (view == null) {
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

    public FolderListView(@NonNull Context context) {
        mContext = context;
        mCallbacks = new ArrayStack<>();
        mFolderNewCallbacks = new ArrayStack<>();
        mController = new FolderListController(context);
        mController.registerCallback(this);
        initData();
        initView();
        initViewData();
        initEvent();
    }

    private void initData() {
        mHandler = new FolderListHandler(this);
        toastManager = new ToastManager(mContext);
    }

    private void initView() {
        folderListDialogView = LayoutInflater.from(mContext).inflate(R.layout.folder_list_dialog, null);
        folderListBack = folderListDialogView.findViewById(R.id.dialog_back);
        folderListTitle = folderListDialogView.findViewById(R.id.dialog_title);
        folderListExit = folderListDialogView.findViewById(R.id.dialog_exit);
        folderListHint = folderListDialogView.findViewById(R.id.dialog_hint);
        folderListHintContainer = folderListDialogView.findViewById(R.id.dialog_hint_container);
        fileSubViewContainer = folderListDialogView.findViewById(R.id.file_sub_view_container);

        swipeRefreshContainer = folderListDialogView.findViewById(R.id.swipe_refresh_container);
        refreshNow = folderListDialogView.findViewById(R.id.refresh_now);
        folderListExceptionContainer = folderListDialogView.findViewById(R.id.dialog_exception_container);
        folderListNetworkExceptionContainer = folderListDialogView.findViewById(R.id.network_exception_container);
        folderListStatus404Container = folderListDialogView.findViewById(R.id.status_404_container);
        folderListEmptyFolderContainer = folderListDialogView.findViewById(R.id.empty_folder_container);

        folderListNewFolder = folderListDialogView.findViewById(R.id.dialog_new_folder);
        folderListConfirm = folderListDialogView.findViewById(R.id.dialog_confirm);
        folderListDialog = new BottomDialog(mContext);
        folderListDialog.setCancelable(false);
        folderListDialog.setContentView(folderListDialogView);

        newFolderDialogView = LayoutInflater.from(mContext).inflate(R.layout.eulix_space_two_button_edit_dialog, null);
        newFolderDialogTitle = newFolderDialogView.findViewById(R.id.dialog_title);
        newFolderDialogInput = newFolderDialogView.findViewById(R.id.dialog_input);
        newFolderDialogInputClear = newFolderDialogView.findViewById(R.id.dialog_input_clear);
        newFolderDialogCancel = newFolderDialogView.findViewById(R.id.dialog_cancel);
        newFolderDialogConfirm = newFolderDialogView.findViewById(R.id.dialog_confirm);
        newFolderDialogTitle.setText(R.string.new_folder);
        newFolderDialogInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
        newFolderDialogInput.setHint(R.string.new_folder_hint);
        newFolderDialogConfirm.setText(R.string.confirm);
        newFolderDialog = new Dialog(mContext, R.style.EulixDialog);
        newFolderDialog.setCancelable(false);
        newFolderDialog.setContentView(newFolderDialogView);
    }

    private void initViewData() {
        maxChildCount = Math.max((int) (Math.ceil((ViewUtils.getScreenHeight(mContext) - ViewUtils.getStatusBarHeight(mContext))
                * 1.0 / mContext.getResources().getDimensionPixelSize(R.dimen.dp_80))), maxChildCount);
    }

    private void initEvent() {
        if (folderListBack != null) {
            folderListBack.setOnClickListener(v -> {
                if (getFolderListFileDepth() > 0 && mController != null) {
                    removeFileSubView();
                    UUID uuid = mController.handleBack();
                    if (uuid != null) {
                        if (ConstantField.UUID.FILE_ROOT_UUID.equals(uuid.toString())) {
                            getLocalEulixSpaceStorage(ConstantField.Category.FILE_ROOT, false, true);
                        } else {
                            getLocalEulixSpaceStorage(uuid.toString(), false, true);
                        }
                        if (adapter != null) {
                            mPage = adapter.getCurrentPage();
                            mTotalPage = adapter.getTotalPage();
                            isLoadingEnable = adapter.isLoadingEnable();
                        }
                        if (mPage == mTotalPage) {
                            if (folderListFileList != null && (!folderListFileList.canScrollVertically(-1) && !folderListFileList.canScrollVertically(1))) {
                                setFooterVisible(false);
                            } else if (footer != null) {
                                setFooterVisible(true);
                                footer.showBottom(mContext.getString(R.string.home_bottom_flag));
                            }
                        } else {
                            setFooterVisible(true);
                            if (footer != null) {
                                footer.showLoading();
                            }
                        }
                        setFooter(true, false);
//                        mController.getEulixSpaceStorage(uuid, 1, null, null, null);
                    }
                }
            });
        }
        if (folderListExit != null) {
            folderListExit.setOnClickListener(v -> {
                if (mCallback != null) {
                    mCallback.dismissFolderListView(false, null, null, null, newFolderUUIDList);
                }
                dismissFolderListDialog();
            });
        }
        if (folderListNewFolder != null) {
            folderListNewFolder.setOnClickListener(v -> {
                showNewFolderDialog(true);
            });
        }
        if (folderListConfirm != null) {
            folderListConfirm.setOnClickListener(v -> {
                UUID selectUUID = mController.getmUuid();
                boolean isValid = true;
                if (isCopy != null && mCallback != null) {
                    UUID originUUID = mCallback.getCurrentFolderUUID();
                    if (originUUID != null && selectUUID != null) {
                        isValid = !selectUUID.toString().equals(originUUID.toString());
                    } else if (originUUID == null && selectUUID == null) {
                        isValid = false;
                    } else {
                        if (selectUUID == null) {
                            isValid = !ConstantField.UUID.FILE_ROOT_UUID.equals(originUUID.toString());
                        } else {
                            isValid = !ConstantField.UUID.FILE_ROOT_UUID.equals(selectUUID.toString());
                        }
                    }
                }
                if (isValid) {
                    if (mCallback != null) {
                        mCallback.dismissFolderListView(true, selectUUID, isCopy, mController.getUuidStack(), newFolderUUIDList);
                    }
                    dismissFolderListDialog();
                } else if (isCopy != null) {
                    showImageTextToast(R.drawable.toast_wrong, (isCopy ? R.string.copy_fail : R.string.cut_fail));
                }
            });
        }
        if (refreshNow != null) {
            refreshNow.setOnClickListener(v -> {
                isLocalEmpty = true;
                refreshEulixSpaceStorage(false, true);
            });
        }

        if (newFolderDialogCancel != null) {
            newFolderDialogCancel.setOnClickListener(v -> dismissNewFolderDialog());
        }
        if (newFolderDialogConfirm != null) {
            newFolderDialogConfirm.setOnClickListener(v -> {
                String filename = null;
                if (newFolderDialogInput != null) {
                    filename = StringUtil.replaceBlank(newFolderDialogInput.getText().toString());
                }
                if (filename == null || filename.equalsIgnoreCase("")) {
                    showPureTextToast(R.string.new_folder_name_empty_warning);
                } else if (!StringUtil.checkContentValid(filename)) {
                    showPureTextToast(R.string.illegal_filename_hint);
                } else {
                    if (filename.length() > 10) {
                        showPureTextToast(R.string.long_filename_hint);
                    } else {
                        if (mController != null) {
                            if (isSelfNewFolder) {
                                mController.createNewFolder(filename);
                            } else {
                                UUID uuid = null;
                                if (mCallback != null) {
                                    ArrayStack<UUID> uuidStack = mCallback.getCurrentUUIDStack();
                                    if (uuidStack != null) {
                                        uuid = uuidStack.peek();
                                    }
                                    if (mFolderNewCallback != null) {
                                        mFolderNewCallback.dismissConfirmOuterNewFolderCallback(DataUtil.cloneUUIDStack(uuidStack));
                                    }
                                }
                                mController.createNewFolder(uuid, filename);
                            }
                        }
                        dismissNewFolderDialog();
                    }
                }
            });
        }
        newFolderDialogInputClear.setOnClickListener(v -> newFolderDialogInput.setText(""));
        newFolderDialogInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = 0;
                if (s != null) {
                    length = s.length();
                }
                setEditTextDialogPattern(length, newFolderDialogInputClear, newFolderDialogConfirm);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Do nothing
            }
        });
        swipeRefreshContainer.setOnRefreshListener(() -> {
            isLocalEmpty = true;
            refreshEulixSpaceStorage(false, true);
        });
    }

    private void setEditTextDialogPattern(EditText editText, ImageButton clear, Button confirm) {
        if (editText != null && clear != null && confirm != null) {
            Editable content = editText.getText();
            if (content != null) {
                setEditTextDialogPattern(content.length(), clear, confirm);
            }
        }
    }

    private void setEditTextDialogPattern(int textLength, ImageButton clear, Button confirm) {
        if (clear != null && confirm != null) {
            clear.setVisibility((textLength > 0) ? View.VISIBLE : View.GONE);
            confirm.setClickable((textLength > 0));
            confirm.setTextColor(mContext.getResources().getColor((textLength > 0) ? R.color.blue_ff337aff : R.color.gray_ffbcbfcd));
        }
    }

    private void setFooter(boolean isAdd, boolean isForce) {
        if (isAdd) {
            if (headerFooterWrapper != null && headerFooterWrapper.getFooterViewSize() <= 0 && folderListFileList != null && footer != null
                    && (isForce || (folderListFileList.canScrollVertically(-1) || folderListFileList.canScrollVertically(1)))) {
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
                param.height = mContext.getResources().getDimensionPixelSize(R.dimen.dp_33);
            } else {
                param.width = 0;
                param.height = 0;
            }
            footer.setLayoutParams(param);
        }
    }

    private void addFileSubView() {
        View view = LayoutInflater.from(mContext).inflate(R.layout.file_sub_view, null);
        folderListFileList = view.findViewById(R.id.file_list);
        customizeFiles = new ArrayList<>();
        adapter = new FileAdapter(mContext, customizeFiles, ConstantField.ViewType.BOX_FILE_LINEAR_VIEW, true, false);
        adapter.setOnItemClickListener(this);
        folderListFileList.setLayoutManager(new LinearLayoutManager(mContext, RecyclerView.VERTICAL, false));
        folderListFileList.addItemDecoration(new FileAdapter.ItemDecoration(RecyclerView.VERTICAL, Math.round(mContext.getResources().getDimension(R.dimen.dp_1))
                , mContext.getResources().getColor(R.color.white_fff7f7f9)));
        headerFooterWrapper = new HeaderFooterWrapper(adapter);
        folderListFileList.setAdapter(headerFooterWrapper);
        footer = new FooterView(mContext);
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

    private void removeFileSubView() {
        if (fileSubViewContainer != null) {
            int childCount = fileSubViewContainer.getChildCount();
            if (childCount > 1) {
                View childView = fileSubViewContainer.getChildAt((childCount - 2));
                if (childView != null) {
                    folderListFileList = childView.findViewById(R.id.file_list);
                    if (folderListFileList != null) {
                        if (folderListFileList.getAdapter() != null && folderListFileList.getAdapter() instanceof HeaderFooterWrapper) {
                            headerFooterWrapper = (HeaderFooterWrapper) folderListFileList.getAdapter();
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

    private void refreshEulixSpaceStorage(boolean isInit) {
        refreshEulixSpaceStorage(isInit, false);
    }

    private void refreshEulixSpaceStorage(boolean isInit, boolean isFore) {
        if (mController != null) {
            mController.getEulixSpaceStorage(1, isFore);
        }
//        if (NetworkUtil.isNetworkAvailable()) {
//            if (mController != null) {
//                mController.getEulixSpaceStorage(1);
//            }
//        } else if (!isInit) {
//            handleDataResult(ConstantField.NETWORK_ERROR_CODE);
//            if (swipeRefreshContainer != null) {
//                swipeRefreshContainer.setRefreshing(false);
//            }
//        }
    }

    private void setNewFolderEnable(boolean isEnable) {
        if (folderListNewFolder != null) {
            folderListNewFolder.setClickable(isEnable);
            folderListNewFolder.setBackgroundResource(isEnable ? R.drawable.background_rectangle_10_stroke_ff337aff_1
                    : R.drawable.background_ffdfe0e5_rectangle_10);
            folderListNewFolder.setTextColor(mContext.getResources().getColor(isEnable ? R.color.blue_ff337aff : R.color.gray_ffbcbfcd));
        }
    }

    private void handleDataResult(int statusCode) {
        switch (statusCode) {
            case -3:
                folderListNetworkExceptionContainer.setVisibility(View.INVISIBLE);
                folderListStatus404Container.setVisibility(View.INVISIBLE);
                folderListEmptyFolderContainer.setVisibility(View.INVISIBLE);
                folderListExceptionContainer.setVisibility(View.GONE);
                fileSubViewContainer.setVisibility(View.VISIBLE);
                break;
            case -2:
                fileSubViewContainer.setVisibility(View.INVISIBLE);
                folderListExceptionContainer.setVisibility(View.VISIBLE);
                folderListNetworkExceptionContainer.setVisibility(View.INVISIBLE);
                folderListStatus404Container.setVisibility(View.INVISIBLE);
                folderListEmptyFolderContainer.setVisibility(View.VISIBLE);
                break;
            case ConstantField.OBTAIN_ACCESS_TOKEN_CODE:
                if (mCallback != null) {
                    mCallback.obtainAccessToken();
                }
//                fileSubViewContainer.setVisibility(View.GONE);
//                folderListExceptionContainer.setVisibility(View.VISIBLE);
//                folderListNetworkExceptionContainer.setVisibility(View.INVISIBLE);
//                folderListStatus404Container.setVisibility(View.VISIBLE);
//                folderListEmptyFolderContainer.setVisibility(View.INVISIBLE);
                if (customizeFiles == null || customizeFiles.size() <= 0) {
                    handleDataResult(-2);
                }
//                showImageTextToast(R.drawable.toast_refuse, R.string.active_device_offline_hint);
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
                folderListExceptionContainer.setVisibility(View.VISIBLE);
                folderListNetworkExceptionContainer.setVisibility(View.VISIBLE);
                folderListStatus404Container.setVisibility(View.INVISIBLE);
                folderListEmptyFolderContainer.setVisibility(View.INVISIBLE);
                break;
            default:
                fileSubViewContainer.setVisibility(View.GONE);
                folderListExceptionContainer.setVisibility(View.VISIBLE);
                folderListNetworkExceptionContainer.setVisibility(View.INVISIBLE);
                folderListStatus404Container.setVisibility(View.VISIBLE);
                folderListEmptyFolderContainer.setVisibility(View.INVISIBLE);
                break;
        }
    }

    private void handleFolderListTitle(String currentDirectory) {
        boolean isRoot = getFolderListFileDepth() <= 0;
        if (folderListBack != null) {
            folderListBack.setVisibility(isRoot ? View.INVISIBLE : View.VISIBLE);
        }
        if (folderListTitle != null) {
            String title = mContext.getString(isCopy == null ? R.string.upload_to : (isCopy ? R.string.copy_to : R.string.cut_to));
            String subTitle = "";
            if (isRoot) {
                if (mContext != null) {
                    subTitle = mContext.getString(R.string.left_quotation_mark) + mContext.getString(R.string.my_space) + mContext.getString(R.string.right_quotation_mark);
                }
            } else {
                Map<String, String> uuidTitleMap = DataUtil.getUuidTitleMap();
                if (mContext != null && uuidTitleMap != null && uuidTitleMap.containsKey(currentDirectory)) {
                    subTitle = mContext.getString(R.string.left_quotation_mark) + uuidTitleMap.get(currentDirectory) + mContext.getString(R.string.right_quotation_mark);
                }
            }
            title = title + subTitle;
            folderListTitle.setText(title);
        }
    }

    public void showFolderListDialog(Boolean copy) {
        if (folderListDialog != null && !folderListDialog.isShowing()) {
            if (adapter != null && headerFooterWrapper != null) {
                adapter.updateData(new ArrayList<>(), false);
                headerFooterWrapper.notifyDataSetChanged();
            }
            isCopy = copy;
            if (mController != null) {
                ArrayStack<UUID> uuids = null;
                if (mCallback != null) {
                    uuids = mCallback.getCurrentUUIDStack();
                }
                mController.resetUUID(uuids);
                UUID uuid = mController.getmUuid();
                handleFolderListTitle(uuid == null ? "" : uuid.toString());
            }
            boolean isShow = false;
            int size = -1;
            if (mCallback != null) {
                size = mCallback.getSelectFilesSize();
            }
            if (isCopy == null || size >= 1) {
                if (folderListHint != null && folderListHintContainer != null) {
                    if (size >= 1) {
                        String folderListHintText = mContext.getString(R.string.choose_file_part_1) + size +
                                mContext.getString((Math.abs(size) == 1) ? R.string.choose_file_part_2_singular : R.string.choose_file_part_2_plural);
                        folderListHint.setText(folderListHintText);
                        folderListHintContainer.setVisibility(View.VISIBLE);
                    } else {
                        folderListHintContainer.setVisibility(View.GONE);
                    }
                }
                isShow = true;
            }
//            if (copy == null) {
//                if (folderListHintContainer != null) {
//                    folderListHintContainer.setVisibility(View.GONE);
//                }
//                isShow = true;
//            } else {
//                int size = -1;
//                if (mCallback != null) {
//                    size = mCallback.getSelectFilesSize();
//                }
//                if (size >= 1) {
//                    if (folderListHint != null && folderListHintContainer != null) {
//                        String folderListHintText = mContext.getString(R.string.choose_file_part_1) + size +
//                                mContext.getString(size > 1 ? R.string.choose_file_part_2_plural : R.string.choose_file_part_2_singular);
//                        folderListHint.setText(folderListHintText);
//                        folderListHintContainer.setVisibility(View.VISIBLE);
//                    }
//                    isShow = true;
//                }
//            }
            if (isShow) {
                folderListDialog.show();
                Window window = folderListDialog.getWindow();
                if (window != null) {
                    window.setGravity(Gravity.BOTTOM);
                    window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT
                            , Math.max((ViewUtils.getScreenHeight(mContext) - ViewUtils.getStatusBarHeight(mContext)), 0));
                }
                if (mController != null) {
                    UUID uuid = mController.getmUuid();
                    boolean isRoot = getFolderListFileDepth() <= 0;
                    getLocalEulixSpaceStorage(((isRoot || uuid == null) ? ConstantField.Category.FILE_ROOT : uuid.toString()), true, false);
                    refreshEulixSpaceStorage(true);
                }
            }
        }
    }

    public void dismissFolderListDialog() {
        if (folderListDialog != null && folderListDialog.isShowing()) {
            if (fileSubViewContainer != null) {
                fileSubViewContainer.removeAllViews();
            }
            folderListDialog.dismiss();
            if (mController != null) {
                mController.resetUUID();
            }
        }
    }

    public void showNewFolderDialog(boolean selfNewFolder) {
        if (newFolderDialog != null && !newFolderDialog.isShowing()) {
            newFolderDialogInput.setText("");
            isSelfNewFolder = selfNewFolder;
            setEditTextDialogPattern(newFolderDialogInput, newFolderDialogInputClear, newFolderDialogConfirm);
            newFolderDialog.show();
            Window window = newFolderDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                window.setLayout(mContext.getResources().getDimensionPixelSize(R.dimen.dp_259), ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissNewFolderDialog() {
        if (newFolderDialog != null && newFolderDialog.isShowing()) {
            newFolderDialog.dismiss();
        }
    }

    public void getLocalEulixSpaceStorage(String currentId, boolean isNext, boolean isBack) {
        boolean tempLoadingEnable = isLoadingEnable;
        isLoadingEnable = false;
        if (swipeRefreshContainer != null) {
            swipeRefreshContainer.setRefreshing(false);
        }
        if (mController != null) {
            List<CustomizeFile> customizeFiles = mController.getLocalEulixSpaceStorage(currentId);
            isLocalEmpty = (customizeFiles == null);
            if (ConstantField.Category.FILE_ROOT.equals(currentId) || ConstantField.Category.FILE_IMAGE.equals(currentId)
                    || ConstantField.Category.FILE_VIDEO.equals(currentId) || ConstantField.Category.FILE_DOCUMENT.equals(currentId)
                    || ConstantField.Category.FILE_OTHER.equals(currentId)) {
                currentId = ConstantField.UUID.FILE_ROOT_UUID;
            }
            if (isNext) {
                if (adapter != null) {
                    adapter.setLoadingEnable(tempLoadingEnable);
                    adapter.setCurrentPage(mPage);
                    adapter.setTotalPage(mTotalPage);
                }
                addFileSubView();
                mController.handleNext(currentId);
            }
            List<CustomizeFile> customizeFileList = null;
            if (customizeFiles != null) {
                customizeFileList = new ArrayList<>();
                for (CustomizeFile customizeFile : customizeFiles) {
                    if (customizeFile != null && ConstantField.MimeType.FOLDER.equals(customizeFile.getMime())) {
                        customizeFileList.add(customizeFile);
                    }
                }
            }
            openDirectory(currentId, customizeFileList, true, isBack);
        }
        if (folderListFileList != null && (!folderListFileList.canScrollVertically(-1) && !folderListFileList.canScrollVertically(1))
                && customizeFiles != null && customizeFiles.size() < maxChildCount) {
            setFooter(true, false);
            setFooterVisible(false);
        } else if (footer != null) {
            setFooter(true, true);
            setFooterVisible(true);
            footer.showBottom(mContext.getString(R.string.home_bottom_flag));
        }
    }

    private void handlePageInfo(PageInfo pageInfo, String currentDirectory, List<CustomizeFile> customizeFileList) {
        Integer pageValue = pageInfo.getPage();
        Integer totalPageValue = pageInfo.getTotal();
        if (totalPageValue != null) {
            mTotalPage = totalPageValue;
        }
        if (pageValue != null && pageValue > 1 && mController.isUUIDSame()) {
            addDirectory(customizeFileList, pageInfo);
        } else {
            openDirectory(currentDirectory, customizeFileList, false, false);
        }
        if (mTotalPage > 1) {
            Integer pageSizeValue = pageInfo.getPageSize();
            if (pageSizeValue != null) {
                int pageSize = pageSizeValue;
                if (pageSize <= 0) {
                    pageSize = customizeFileList.size();
                }
                pageSize = Math.max(pageSize, 1);
                if (customizeFiles != null && customizeFiles.size() <= maxChildCount && mPage < mTotalPage && mController != null) {
                    mController.getEulixSpaceStorage((mPage + 1));
                }
            }
        }
        if (mPage == mTotalPage) {
            if (folderListFileList != null && (!folderListFileList.canScrollVertically(-1) && !folderListFileList.canScrollVertically(1))) {
                setFooterVisible(false);
            } else if (footer != null) {
                setFooterVisible(true);
                footer.showBottom(mContext.getString(R.string.home_bottom_flag));
            }
        } else {
            setFooterVisible(true);
            if (footer != null) {
                footer.showLoading();
            }
        }
    }

    /**
     * 打开在线文件夹
     * @param customizeFileList
     */
    private void openDirectory(String currentDirectory, List<CustomizeFile> customizeFileList, boolean isLocal, boolean isBack) {
        if (!isBack) {
            setFooter(false, true);
        }
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
            adapter.updateData(customizeFiles, false);
            headerFooterWrapper.notifyDataSetChanged();
        }
        handleFolderListTitle(currentDirectory);
    }

    /**
     * 在线文件夹增加一页
     * @param customizeFileList
     * @param pageInfo
     */
    private void addDirectory(List<CustomizeFile> customizeFileList, PageInfo pageInfo) {
        if (pageInfo != null && pageInfo.getPage() != null) {
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
                    adapter.updateData(customizeFiles, (adapter.getSelectPosition() != null));
                    headerFooterWrapper.notifyDataSetChanged();
                }
                handleDataResult(customizeFiles.size() <= 0 ? -2 : -3);
            }
        }
    }

    private int getFolderListFileDepth() {
        return ((mController == null ? 0 : mController.getDepth()) - 1);
    }

    public void showServerExceptionToast() {
        showImageTextToast(R.drawable.toast_refuse, R.string.service_exception_hint);
    }

    public void showPureTextToast(@StringRes int resId) {
        if (toastManager != null) {
            toastManager.showPureTextToast(resId);
        }
    }

    public void showImageTextToast(@DrawableRes int drawableResId, @StringRes int stringResId) {
        if (toastManager != null) {
            toastManager.showImageTextToast(drawableResId, stringResId);
        }
    }

    public void registerCallback(FolderListCallback callback) {
        mCallback = callback;
        mCallbacks.push(callback);
    }

    public void unregisterCallback() {
        if (mCallbacks.empty()) {
            mCallback = null;
        } else {
            mCallback = mCallbacks.pop();
        }
    }

    public void registerFolderNewCallback(FolderNewCallback callback) {
        mFolderNewCallback = callback;
        mFolderNewCallbacks.push(callback);
    }

    public void unregisterFolderNewCallback() {
        if (mFolderNewCallbacks.empty()) {
            mFolderNewCallback = null;
        } else {
            mFolderNewCallback = mFolderNewCallbacks.pop();
        }
    }

    @Override
    public void getEulixSpaceFileListResult(Integer code, String currentDirectory, List<CustomizeFile> customizeFiles, PageInfo pageInfo) {
        if (mHandler != null) {
            mHandler.post(() -> {
                swipeRefreshContainer.setRefreshing(false);
                if (currentDirectory == null && customizeFiles == null) {
                    isLoadingEnable = true;
                    if (isLocalEmpty) {
                        handleDataResult(code);
                    }
                } else {
                    UUID uuid = mController.getmUuid();
                    if (uuid != null && uuid.toString().equals(currentDirectory)) {
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
    public void createEulixSpaceDirectoryResult(Integer code, boolean isOk, String currentUUID, String folderName, String folderUuid) {
        if (mHandler != null) {
            mHandler.post(() -> {
                if (isOk) {
                    toastManager.showImageTextToast(R.drawable.toast_right, R.string.create_folder_success);
                    if (isSelfNewFolder) {
                        refreshEulixSpaceStorage(false);
                        if (folderUuid != null) {
                            DataUtil.setUuidTitleMap(folderUuid, folderName);
                            UUID nFolderUuid = null;
                            try {
                                nFolderUuid = UUID.fromString(folderUuid);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (nFolderUuid != null) {
                                getLocalEulixSpaceStorage(folderUuid, true, false);
                                mController.getEulixSpaceStorage(nFolderUuid, 1, null, null, null);
                            }
                        }
                        if (newFolderUUIDList == null) {
                            newFolderUUIDList = new ArrayList<>();
                        }
                        UUID uuid = null;
                        if (currentUUID != null) {
                            try {
                                uuid = UUID.fromString(currentUUID);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        if (uuid != null) {
                            newFolderUUIDList.add(uuid);
                        }
                    } else {
                        if (mCallback != null) {
                            UUID parentUUID = null;
                            if (currentUUID != null) {
                                parentUUID = UUID.fromString(currentUUID);
                            }
                            if (parentUUID == null) {
                                parentUUID = UUID.fromString(ConstantField.UUID.FILE_ROOT_UUID);
                            }
                            mCallback.refreshEulixSpaceStorage(parentUUID);
                            if (folderUuid != null && mFolderNewCallback != null) {
                                mFolderNewCallback.refreshEulixSpaceStorage(parentUUID, folderName, folderUuid);
                            }
                        }
                    }
                } else {
                    boolean isHandle = false;
                    if (code != null) {
                        isHandle = true;
                        switch (code) {
                            case ConstantField.FILE_ALREADY_EXISTS_CODE:
                                if (toastManager != null) {
                                    toastManager.showImageTextToast(R.drawable.toast_wrong, R.string.duplicate_folder_failed);
                                }
                                break;
                            case ConstantField.SERVER_EXCEPTION_CODE:
                                showServerExceptionToast();
                                break;
                            default:
                                isHandle = false;
                                break;
                        }
                    }
                    if (!isHandle && toastManager != null) {
                        toastManager.showImageTextToast(R.drawable.toast_wrong, R.string.create_folder_failed);
                    }
                    if (code != null && code == ConstantField.OBTAIN_ACCESS_TOKEN_CODE && mCallback != null) {
                        mCallback.obtainAccessToken();
                    }
                }
            });
        }
    }

    @Override
    public void uuidStackChange(int depth) {
        if (mHandler != null) {
            try {
                mHandler.post(() -> setNewFolderEnable(depth < FOLDER_DEPTH_MAX_VALUE));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onItemClick(View view, int position, boolean isEnable) {
        if (adapter != null && position >= 0 && customizeFiles.size() > position) {
            CustomizeFile customizeFile = customizeFiles.get(position);
            if (customizeFile != null) {
                String titleContent = customizeFile.getName();
                String id = customizeFile.getId();
                if (id != null) {
                    DataUtil.setUuidTitleMap(id, titleContent);
                    UUID uuid = null;
                    try {
                        uuid = UUID.fromString(id);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (uuid != null) {
                        getLocalEulixSpaceStorage(uuid.toString(), true, false);
                        mController.getEulixSpaceStorage(uuid, 1, null, null, null);
                    }
                }
            }
        }
    }
}