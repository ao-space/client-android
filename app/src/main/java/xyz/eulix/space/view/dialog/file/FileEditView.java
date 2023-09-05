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

package xyz.eulix.space.view.dialog.file;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.EulixSpaceService;
import xyz.eulix.space.R;
import xyz.eulix.space.bean.ArrayStack;
import xyz.eulix.space.bean.CustomizeFile;
import xyz.eulix.space.event.DeleteFileEvent;
import xyz.eulix.space.event.RenameFileEvent;
import xyz.eulix.space.interfaces.ResultCallback;
import xyz.eulix.space.network.files.BaseResponseResult;
import xyz.eulix.space.network.files.FileRsp;
import xyz.eulix.space.network.files.PageInfo;
import xyz.eulix.space.network.files.RecycledListResponse;
import xyz.eulix.space.network.files.RecycledListResult;
import xyz.eulix.space.transfer.TransferHelper;
import xyz.eulix.space.ui.FileShareActivity;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.FormatUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.NetUtils;
import xyz.eulix.space.util.PermissionUtils;
import xyz.eulix.space.util.StringUtil;
import xyz.eulix.space.util.ThreadPool;
import xyz.eulix.space.util.ToastUtil;
import xyz.eulix.space.view.BottomDialog;
import xyz.eulix.space.view.dialog.EulixDialogUtil;
import xyz.eulix.space.view.dialog.EulixLoadingDialog;
import xyz.eulix.space.view.dialog.TaskProgressDialog;
import xyz.eulix.space.view.dialog.folder.FolderListView;

/**
 * @author: chenjiawei
 * Description:
 * date: 2021/9/16 13:48
 */
public class FileEditView implements FileEditController.IFileEditCallback, FolderListView.FolderListCallback, View.OnClickListener {
    private Context mContext;
    private FileEditController mController;
    private FileEditCallback mCallback;
    private FileEditPluralCallback mPluralCallback;
    private FileEditRecycleBinCallback mRecycleBinCallback;
    private ImageButton detailExit;
    private TextView detailName, detailTime, detailSize, detailPath;
    private ImageView fileImage;
    private TextView fileName;
    private ImageButton moreEditExit;
    private LinearLayout moreEditContainer;
    private TextView /*deleteDialogTitle, deleteDialogContent, */renameDialogTitle;
    private Button deleteDialogCancel, deleteDialogConfirm, renameDialogCancel, renameDialogConfirm;
    private Button restoreDialogCancel;
    private Button restoreDialogConfirm;
    private Button deleteCompleteDialogCancel;
    private Button deleteCompleteDialogConfirm;
    private Button clearCompleteDialogCancel;
    private Button clearCompleteDialogConfirm;
    private EditText renameDialogInput;
    private ImageButton renameDialogInputClear;
    private View downloadVertical, shareVertical, deleteVertical, detailVertical, moreVertical, copyVertical, cutVertical;
    private View renameVertical;
    private View restoreVertical;
    private View deleteCompleteVertical;
    private View addToVertical;
    private View renameHorizontal, copyHorizontal, cutHorizontal, detailHorizontal;
    private View fileDialogView, detailDialogView, editDialogView, deleteDialogView, renameDialogView;
    private View fileRecycleDialogView;
    private View restoreDialogView;
    private View deleteCompleteDialogView;
    private View clearCompleteDialogView;
    private Dialog detailDialog, editDialog, deleteDialog, renameDialog;
    private Dialog restoreDialog;
    private Dialog deleteCompleteDialog;
    private Dialog clearCompleteDialog;
    private EulixLoadingDialog shareLoadingDialog;
    private LinearLayout.LayoutParams splitLayoutParams, itemLayoutParams;
    private LinearLayout fileContainer1;
    private LinearLayout fileContainer2;
    private LinearLayout fileContainer3;
    private LinearLayout fileContainer4;
    private LinearLayout fileContainer5;
    private LinearLayout fileRecycleContainer1;
    private LinearLayout fileRecycleContainer2;
    private FolderListView folderListView;

    //更多视图是否包含重命名
    private boolean moreViewContainsRenameFlag = true;

    private String mFrom = TransferHelper.FROM_FILE;
    //是否展示两种删除方式
    private boolean showTwoWaysDeleteStyle = false;
    //相簿id
    private int mAlbumId;
    //是否为个人动态样式式（仅显示下载、分享、删除）
    private boolean isOperationRecordStyle = false;

    //选中的文件列表（用于执行操作，收到接口响应后使用）
    private List<CustomizeFile> mSelectedList = new ArrayList<>();

    private EulixLoadingDialog mLoadingDialog;

    private TaskProgressDialog mTaskProgressDialog;

    /**
     * 公用接口
     */
    public interface FileEditCallback {
        // 刷新当前数据，第一个参数表示结果，第二个参数见ConstantField.ServiceFunction
        void handleRefresh(boolean isSuccess, String serviceFunction);

        // 获取选择的文件列表
        List<CustomizeFile> getSelectFiles();

        // 是否展示视图
        void fileDialog(View view, boolean isShow);

        // 获取数据栈
        ArrayStack<UUID> handleCurrentUUIDStack();
    }

    /**
     * 文件首页、搜索文件类专用接口
     */
    public interface FileEditPluralCallback {
        // 删除、重命名、下载确认后调用，和调用者Activity、Fragment复用返回操作
        boolean handleBackEvent();

        // 获取文件列表回调
        void handleGetEulixSpaceFileListResult(Integer code, String currentDirectory, List<CustomizeFile> customizeFiles, PageInfo pageInfo);

        // 对指定父UUID的文件列表进行刷新，新建文件夹网络返回成功后触发
        void handleRefreshEulixSpaceStorage(UUID parentUUID);

        // 处理上传、复制、移动dialog消失后的操作
        void handleDismissFolderListView(boolean isConfirm, UUID selectUUID, Boolean isCopy, ArrayStack<UUID> uuids, List<UUID> newFolderUUIDs);

        // 处理搜索页面展示或销毁专用
        void handleShowOrDismissFileSearch(boolean isShow);

        // 调用文件夹信息后使用
        void handleFolderDetail(String folderUuid, String name, Long operationAt, String path, Long size);
    }

    /**
     * 回收站专用接口
     */
    public interface FileEditRecycleBinCallback {
        // 还原、删除确认后调用，和调用者Activity复用返回操作
        boolean handleRecycleBackEvent();
    }

    public FileEditView(Activity context, String category) {
        mContext = context;
        if (mContext == null) {
            mContext = EulixSpaceApplication.getResumeActivityContext();
        }
        mController = new FileEditController(category);
        mController.registerCallback(this);
        initView();
        initEvent();
    }

    private void initView() {
        if (mContext != null) {
            folderListView = new FolderListView(mContext);

            fileDialogView = LayoutInflater.from(mContext).inflate(R.layout.file_window, null);
            fileContainer1 = fileDialogView.findViewById(R.id.file_container_1);
            fileContainer2 = fileDialogView.findViewById(R.id.file_container_2);
            fileContainer3 = fileDialogView.findViewById(R.id.file_container_3);
            fileContainer4 = fileDialogView.findViewById(R.id.file_container_4);
            fileContainer5 = fileDialogView.findViewById(R.id.file_container_5);

            fileRecycleDialogView = LayoutInflater.from(mContext).inflate(R.layout.file_recycle_window, null);
            fileRecycleContainer1 = fileRecycleDialogView.findViewById(R.id.file_recycle_container_1);
            fileRecycleContainer2 = fileRecycleDialogView.findViewById(R.id.file_recycle_container_2);

            detailDialogView = LayoutInflater.from(mContext).inflate(R.layout.file_detail_window, null);
            detailExit = detailDialogView.findViewById(R.id.detail_exit);
            detailName = detailDialogView.findViewById(R.id.detail_name);
            detailTime = detailDialogView.findViewById(R.id.detail_time);
            detailSize = detailDialogView.findViewById(R.id.detail_size);
            detailPath = detailDialogView.findViewById(R.id.detail_path);
            detailDialog = new BottomDialog(mContext);
            detailDialog.setCancelable(true);
            detailDialog.setContentView(detailDialogView);

            editDialogView = LayoutInflater.from(mContext).inflate(R.layout.file_edit_window, null);
            fileImage = editDialogView.findViewById(R.id.file_image);
            fileName = editDialogView.findViewById(R.id.file_name);
            moreEditExit = editDialogView.findViewById(R.id.more_edit_exit);
            moreEditContainer = editDialogView.findViewById(R.id.more_edit_container);
            editDialog = new BottomDialog(mContext);
            editDialog.setCancelable(true);
            editDialog.setContentView(editDialogView);

            deleteDialogView = LayoutInflater.from(mContext).inflate(R.layout.eulix_space_bottom_dialog, null);
            TextView deleteContent = deleteDialogView.findViewById(R.id.dialog_content);
            deleteContent.setText(R.string.delete_content);
            deleteDialogCancel = deleteDialogView.findViewById(R.id.dialog_cancel);
            deleteDialogConfirm = deleteDialogView.findViewById(R.id.dialog_confirm);
            deleteDialogConfirm.setText(R.string.confirm_delete);
            deleteDialog = new BottomDialog(mContext);
            deleteDialog.setCancelable(true);
            deleteDialog.setContentView(deleteDialogView);

            renameDialogView = LayoutInflater.from(mContext).inflate(R.layout.eulix_space_two_button_edit_dialog, null);
            renameDialogTitle = renameDialogView.findViewById(R.id.dialog_title);
            renameDialogInput = renameDialogView.findViewById(R.id.dialog_input);
            renameDialogInputClear = renameDialogView.findViewById(R.id.dialog_input_clear);
            renameDialogCancel = renameDialogView.findViewById(R.id.dialog_cancel);
            renameDialogConfirm = renameDialogView.findViewById(R.id.dialog_confirm);
            renameDialogTitle.setText(R.string.rename);
            renameDialogInput.setImeOptions(EditorInfo.IME_ACTION_DONE);
            renameDialogInput.setHint(R.string.rename_hint);
            renameDialogConfirm.setText(R.string.confirm);
            renameDialog = new Dialog(mContext, R.style.EulixDialog);
            renameDialog.setCancelable(false);
            renameDialog.setContentView(renameDialogView);

            restoreDialogView = LayoutInflater.from(mContext).inflate(R.layout.eulix_space_bottom_dialog, null);
            TextView restoreContent = restoreDialogView.findViewById(R.id.dialog_content);
            restoreContent.setText(R.string.restore_content);
            restoreDialogCancel = restoreDialogView.findViewById(R.id.dialog_cancel);
            restoreDialogConfirm = restoreDialogView.findViewById(R.id.dialog_confirm);
            restoreDialogConfirm.setText(R.string.restore);
            restoreDialog = new BottomDialog(mContext);
            restoreDialog.setCancelable(true);
            restoreDialog.setContentView(restoreDialogView);

            deleteCompleteDialogView = LayoutInflater.from(mContext).inflate(R.layout.eulix_space_bottom_dialog, null);
            TextView deleteCompleteContent = deleteCompleteDialogView.findViewById(R.id.dialog_content);
            deleteCompleteContent.setText(R.string.delete_complete_content);
            deleteCompleteDialogCancel = deleteCompleteDialogView.findViewById(R.id.dialog_cancel);
            deleteCompleteDialogConfirm = deleteCompleteDialogView.findViewById(R.id.dialog_confirm);
            deleteCompleteDialogConfirm.setText(R.string.confirm_delete);
            deleteCompleteDialog = new BottomDialog(mContext);
            deleteCompleteDialog.setCancelable(true);
            deleteCompleteDialog.setContentView(deleteCompleteDialogView);

            clearCompleteDialogView = LayoutInflater.from(mContext).inflate(R.layout.eulix_space_bottom_dialog, null);
            TextView clearCompleteContent = clearCompleteDialogView.findViewById(R.id.dialog_content);
            clearCompleteContent.setText(R.string.clear_complete_content);
            clearCompleteDialogCancel = clearCompleteDialogView.findViewById(R.id.dialog_cancel);
            clearCompleteDialogConfirm = clearCompleteDialogView.findViewById(R.id.dialog_confirm);
            clearCompleteDialogConfirm.setText(R.string.clean_up);
            clearCompleteDialog = new BottomDialog(mContext);
            clearCompleteDialog.setCancelable(true);
            clearCompleteDialog.setContentView(clearCompleteDialogView);

            initExtraView();
        }
    }

    private void initExtraView() {
        generateDownloadVertical();
        generateShareVertical();
        generateDeleteVertical();
        generateDetailVertical();
        generateMoreVertical();
        generateRenameVertical();
        generateCopyVertical();
        generateCutVertical();
        generateRenameHorizontal();
        generateCopyHorizontal();
        generateCutHorizontal();
        generateRestoreVertical();
        generateDeleteCompleteVertical();
        generateAlbumAddToVertical();
        generateDetailHorizontal();
        if (mContext != null) {
            shareLoadingDialog = EulixDialogUtil.createLoadingDialog(mContext,
                    mContext.getResources().getString(R.string.waiting), true);
            itemLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT
                    , mContext.getResources().getDimensionPixelSize(R.dimen.dp_59));
            splitLayoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT
                    , mContext.getResources().getDimensionPixelSize(R.dimen.dp_1));
        }
    }

    private void generateDownloadVertical() {
        if (downloadVertical == null && mContext != null) {
            downloadVertical = LayoutInflater.from(mContext).inflate(R.layout.file_edit_vertical_item, null);
            ImageView editImage = downloadVertical.findViewById(R.id.edit_image);
            TextView editText = downloadVertical.findViewById(R.id.edit_text);
            editImage.setImageResource(R.drawable.download);
            editText.setText(R.string.save_to_local);
        }
    }

    private void generateShareVertical() {
        if (shareVertical == null && mContext != null) {
            shareVertical = LayoutInflater.from(mContext).inflate(R.layout.file_edit_vertical_item, null);
            ImageView editImage = shareVertical.findViewById(R.id.edit_image);
            TextView editText = shareVertical.findViewById(R.id.edit_text);
            editImage.setImageResource(R.drawable.share);
            editText.setText(R.string.share);
        }
    }

    private void generateDeleteVertical() {
        if (deleteVertical == null && mContext != null) {
            deleteVertical = LayoutInflater.from(mContext).inflate(R.layout.file_edit_vertical_item, null);
            ImageView editImage = deleteVertical.findViewById(R.id.edit_image);
            TextView editText = deleteVertical.findViewById(R.id.edit_text);
            editImage.setImageResource(R.drawable.delete);
            editText.setText(R.string.delete);
        }
    }

    private void generateDetailVertical() {
        if (detailVertical == null && mContext != null) {
            detailVertical = LayoutInflater.from(mContext).inflate(R.layout.file_edit_vertical_item, null);
            ImageView editImage = detailVertical.findViewById(R.id.edit_image);
            TextView editText = detailVertical.findViewById(R.id.edit_text);
            editImage.setImageResource(R.drawable.detail);
            editText.setText(R.string.detail);
        }
    }

    private void generateMoreVertical() {
        if (moreVertical == null && mContext != null) {
            moreVertical = LayoutInflater.from(mContext).inflate(R.layout.file_edit_vertical_item, null);
            ImageView editImage = moreVertical.findViewById(R.id.edit_image);
            TextView editText = moreVertical.findViewById(R.id.edit_text);
            editImage.setImageResource(R.drawable.more);
            editText.setText(R.string.more);
        }
    }

    private void generateRenameVertical() {
        if (renameVertical == null && mContext != null) {
            renameVertical = LayoutInflater.from(mContext).inflate(R.layout.file_edit_vertical_item, null);
            ImageView editImage = renameVertical.findViewById(R.id.edit_image);
            TextView editText = renameVertical.findViewById(R.id.edit_text);
            editImage.setImageResource(R.drawable.rename);
            editText.setText(R.string.rename);
        }
    }

    private void generateCopyVertical() {
        if (copyVertical == null && mContext != null) {
            copyVertical = LayoutInflater.from(mContext).inflate(R.layout.file_edit_vertical_item, null);
            ImageView editImage = copyVertical.findViewById(R.id.edit_image);
            TextView editText = copyVertical.findViewById(R.id.edit_text);
            editImage.setImageResource(R.drawable.copy);
            editText.setText(R.string.copy);
        }
    }

    private void generateCutVertical() {
        if (cutVertical == null && mContext != null) {
            cutVertical = LayoutInflater.from(mContext).inflate(R.layout.file_edit_vertical_item, null);
            ImageView editImage = cutVertical.findViewById(R.id.edit_image);
            TextView editText = cutVertical.findViewById(R.id.edit_text);
            editImage.setImageResource(R.drawable.cut);
            editText.setText(R.string.cut);
        }
    }

    private void generateRenameHorizontal() {
        if (renameHorizontal == null && mContext != null) {
            renameHorizontal = LayoutInflater.from(mContext).inflate(R.layout.file_edit_horizontal_item, null);
            ImageView editImage = renameHorizontal.findViewById(R.id.edit_image);
            TextView editText = renameHorizontal.findViewById(R.id.edit_text);
            editImage.setImageResource(R.drawable.rename);
            editText.setText(R.string.rename);
        }
    }

    private void generateCopyHorizontal() {
        if (copyHorizontal == null && mContext != null) {
            copyHorizontal = LayoutInflater.from(mContext).inflate(R.layout.file_edit_horizontal_item, null);
            ImageView editImage = copyHorizontal.findViewById(R.id.edit_image);
            TextView editText = copyHorizontal.findViewById(R.id.edit_text);
            editImage.setImageResource(R.drawable.copy);
            editText.setText(R.string.copy);
        }
    }

    private void generateCutHorizontal() {
        if (cutHorizontal == null && mContext != null) {
            cutHorizontal = LayoutInflater.from(mContext).inflate(R.layout.file_edit_horizontal_item, null);
            ImageView editImage = cutHorizontal.findViewById(R.id.edit_image);
            TextView editText = cutHorizontal.findViewById(R.id.edit_text);
            editImage.setImageResource(R.drawable.cut);
            editText.setText(R.string.cut);
        }
    }

    private void generateRestoreVertical() {
        if (restoreVertical == null && mContext != null) {
            restoreVertical = LayoutInflater.from(mContext).inflate(R.layout.file_edit_vertical_item, null);
            ImageView editImage = restoreVertical.findViewById(R.id.edit_image);
            TextView editText = restoreVertical.findViewById(R.id.edit_text);
            editImage.setImageResource(R.drawable.restore);
            editText.setText(R.string.restore);
        }
    }

    private void generateDeleteCompleteVertical() {
        if (deleteCompleteVertical == null && mContext != null) {
            deleteCompleteVertical = LayoutInflater.from(mContext).inflate(R.layout.file_edit_vertical_item, null);
            ImageView editImage = deleteCompleteVertical.findViewById(R.id.edit_image);
            TextView editText = deleteCompleteVertical.findViewById(R.id.edit_text);
            editImage.setImageResource(R.drawable.delete);
            editText.setText(R.string.delete);
        }
    }

    private void generateAlbumAddToVertical() {
        if (addToVertical == null && mContext != null) {
            addToVertical = LayoutInflater.from(mContext).inflate(R.layout.file_edit_vertical_item, null);
            ImageView editImage = addToVertical.findViewById(R.id.edit_image);
            TextView editText = addToVertical.findViewById(R.id.edit_text);
            editImage.setImageResource(R.drawable.icon_album_add);
            editText.setText(R.string.add_to);
        }
    }

    private void generateDetailHorizontal() {
        if (detailHorizontal == null && mContext != null) {
            detailHorizontal = LayoutInflater.from(mContext).inflate(R.layout.file_edit_horizontal_item, null);
            ImageView editImage = detailHorizontal.findViewById(R.id.edit_image);
            TextView editText = detailHorizontal.findViewById(R.id.edit_text);
            editImage.setImageResource(R.drawable.detail);
            editText.setText(R.string.detail);
        }
    }

    private void initEvent() {
        if (detailExit != null) {
            detailExit.setOnClickListener(this);
        }
        if (moreEditExit != null) {
            moreEditExit.setOnClickListener(this);
        }
        if (deleteDialogCancel != null) {
            deleteDialogCancel.setOnClickListener(v -> dismissDeleteDialog());
        }
        if (deleteDialogConfirm != null) {
            deleteDialogConfirm.setOnClickListener(v -> {
                List<UUID> selectUuids = getSelectUUID();
                if (selectUuids != null) {
                    deleteFile(selectUuids);
                    if (mCallback != null) {
                        mSelectedList.clear();
                        if (mCallback.getSelectFiles() != null) {
                            mSelectedList.addAll(mCallback.getSelectFiles());
                        }
                    }
                }
                if (mPluralCallback != null) {
                    mPluralCallback.handleBackEvent();
                }
                dismissDeleteDialog();
            });
        }
        if (renameDialogCancel != null) {
            renameDialogCancel.setOnClickListener(v -> dismissRenameDialog());
        }
        if (renameDialogConfirm != null) {
            renameDialogConfirm.setOnClickListener(v -> {
                String filename = null;
                if (renameDialogInput != null) {
                    filename = StringUtil.replaceBlank(renameDialogInput.getText().toString());
                }
                if (filename == null || filename.length() <= 0) {
                    if (folderListView != null) {
                        folderListView.showPureTextToast(R.string.rename_file_empty_warning);
                    }
                } else if (!StringUtil.checkContentValid(filename)) {
                    if (folderListView != null) {
                        folderListView.showPureTextToast(R.string.illegal_filename_hint);
                    }
                } else {
                    boolean isLengthPermit = true;
                    CustomizeFile customizeFile = getSelectFile();
                    if (customizeFile != null && ConstantField.MimeType.FOLDER.equals(customizeFile.getMime())) {
                        isLengthPermit = (filename.length() <= 10);
                    }
                    if (isLengthPermit) {
                        dismissEditDialog();
                        if (customizeFile != null) {
                            if (!ConstantField.MimeType.FOLDER.equals(customizeFile.getMime())) {
                                String currentName = customizeFile.getName();
                                String suffix = "";
                                if (currentName != null) {
                                    int pointIndex = currentName.lastIndexOf(".");
                                    if (pointIndex >= 0 && pointIndex < currentName.length()) {
                                        suffix = currentName.substring(pointIndex);
                                    }
                                }
                                filename = filename + suffix;
                            }
                            UUID uuid = null;
                            try {
                                uuid = UUID.fromString(customizeFile.getId());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            if (uuid != null) {
                                renameFile(uuid, filename);
                            }
                        }
                        if (mPluralCallback != null) {
                            mPluralCallback.handleBackEvent();
                        }
                        dismissRenameDialog();
                    } else if (folderListView != null) {
                        folderListView.showPureTextToast(R.string.long_filename_hint);
                    }
                }
            });
        }
        if (restoreDialogCancel != null) {
            restoreDialogCancel.setOnClickListener(v -> dismissRestoreDialog());
        }
        if (restoreDialogConfirm != null) {
            restoreDialogConfirm.setOnClickListener(v -> {
                List<UUID> selectUuids = getSelectUUID();
                if (selectUuids != null) {
                    restoreRecycledFile(selectUuids);
                }
                if (mRecycleBinCallback != null) {
                    mRecycleBinCallback.handleRecycleBackEvent();
                }
                dismissRestoreDialog();
            });
        }
        if (deleteCompleteDialogCancel != null) {
            deleteCompleteDialogCancel.setOnClickListener(v -> dismissDeleteCompleteDialog());
        }
        if (deleteCompleteDialogConfirm != null) {
            deleteCompleteDialogConfirm.setOnClickListener(v -> {
                List<UUID> selectUuids = getSelectUUID();
                if (selectUuids != null) {
                    clearRecycledFile(selectUuids);
                }
                if (mRecycleBinCallback != null) {
                    mRecycleBinCallback.handleRecycleBackEvent();
                }
                dismissDeleteCompleteDialog();
            });
        }
        if (clearCompleteDialogCancel != null) {
            clearCompleteDialogCancel.setOnClickListener(v -> dismissClearCompleteDialog());
        }
        if (clearCompleteDialogConfirm != null) {
            clearCompleteDialogConfirm.setOnClickListener(v -> {
                clearRecycledFile(null);
                if (mRecycleBinCallback != null) {
                    mRecycleBinCallback.handleRecycleBackEvent();
                }
                dismissClearCompleteDialog();
            });
        }
        if (downloadVertical != null) {
            downloadVertical.setOnClickListener(v -> {
                if (PermissionUtils.isPermissionGranted(mContext, PermissionUtils.PERMISSION_WRITE_STORAGE)) {
                    callDownloadAfterGetPermission();
                } else {
                    PermissionUtils.requestPermissionWithNotice((Activity) mContext, PermissionUtils.PERMISSION_WRITE_STORAGE, new ResultCallback() {
                        @Override
                        public void onResult(boolean result, String extraMsg) {
                            if (result) {
                                callDownloadAfterGetPermission();
                            }
                        }
                    });
                }
            });
        }
        if (shareVertical != null) {
            shareVertical.setOnClickListener(v -> {
                //分享
                CustomizeFile customizeFile = getSelectFile();
                if (customizeFile != null) {
                    FileShareActivity.startShareActivity(mContext, customizeFile, mFrom);
                }
            });
        }
        if (deleteVertical != null) {
            deleteVertical.setOnClickListener(v -> {
                mSelectedList.clear();
                if (mCallback.getSelectFiles() != null) {
                    mSelectedList.addAll(mCallback.getSelectFiles());
                }
                showDeleteDialog();
            });
        }
        if (detailVertical != null) {
            detailVertical.setOnClickListener(v -> {
                showDetailDialog();
                CustomizeFile customizeFile = getSelectFile();
                if (customizeFile != null && ConstantField.MimeType.FOLDER.equals(customizeFile.getMime())) {
                    UUID uuid = null;
                    String fileUuid = customizeFile.getId();
                    if (fileUuid != null) {
                        try {
                            uuid = UUID.fromString(fileUuid);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (uuid != null && mController != null) {
                        mController.getFolderDetail(uuid);
                    }
                }
            });
        }
        if (detailHorizontal != null) {
            detailHorizontal.setOnClickListener(v -> {
                showDetailDialog();
                CustomizeFile customizeFile = getSelectFile();
                if (customizeFile != null && ConstantField.MimeType.FOLDER.equals(customizeFile.getMime())) {
                    UUID uuid = null;
                    String fileUuid = customizeFile.getId();
                    if (fileUuid != null) {
                        try {
                            uuid = UUID.fromString(fileUuid);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (uuid != null && mController != null) {
                        mController.getFolderDetail(uuid);
                    }
                }
            });
        }
        if (moreVertical != null) {
            moreVertical.setOnClickListener(v -> showEditDialog());
        }
        if (renameVertical != null) {
            renameVertical.setOnClickListener(v -> {
                showRenameDialog();
            });
        }
        if (copyVertical != null) {
            copyVertical.setOnClickListener(v -> folderListView.showFolderListDialog(true));
        }
        if (cutVertical != null) {
            cutVertical.setOnClickListener(v -> folderListView.showFolderListDialog(false));
        }
        if (renameHorizontal != null) {
            renameHorizontal.setOnClickListener(v -> {
                dismissEditDialog();
                showRenameDialog();
            });
        }
        if (copyHorizontal != null) {
            copyHorizontal.setOnClickListener(v -> {
                dismissEditDialog();
                folderListView.showFolderListDialog(true);
            });
        }
        if (cutHorizontal != null) {
            cutHorizontal.setOnClickListener(v -> {
                dismissEditDialog();
                folderListView.showFolderListDialog(false);
            });
        }
        if (restoreVertical != null) {
            restoreVertical.setOnClickListener(v -> showRestoreDialog());
        }
        if (deleteCompleteVertical != null) {
            deleteCompleteVertical.setOnClickListener(v -> {
                showDeleteCompleteDialog();
            });
        }
        if (addToVertical != null) {
            addToVertical.setOnClickListener(v -> {
                //展示相册选择列表对话框
                List<UUID> selectUuids = getSelectUUID();
                List<String> uuids = new ArrayList<>();
                if (selectUuids != null) {
                    for (UUID item : selectUuids) {
                        uuids.add(item.toString());
                    }
                }
            });
        }
        renameDialogInputClear.setOnClickListener(v -> renameDialogInput.setText(""));
        renameDialogInput.addTextChangedListener(new TextWatcher() {
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
                setEditTextDialogPattern(length, renameDialogInputClear, renameDialogConfirm);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Do nothing
            }
        });
    }

    //获取存储权限后执行下载逻辑
    private void callDownloadAfterGetPermission() {
        if (mCallback != null) {
            List<CustomizeFile> customizeFiles = mCallback.getSelectFiles();
            if (customizeFiles != null) {
                if (NetUtils.isMobileNetWork(mContext) && !ConstantField.sIAllowTransferWithMobileData) {
                    EulixDialogUtil.showChooseAlertDialog(mContext, mContext.getResources().getString(R.string.mobile_data_download),
                            mContext.getResources().getString(R.string.mobile_data_download_desc), mContext.getResources().getString(R.string.ok),
                            (dialog, which) -> {
                                ConstantField.sIAllowTransferWithMobileData = true;
                                callDownload(customizeFiles, (result, extraMsg) -> {
                                    if (folderListView != null) {
                                        folderListView.showImageTextToast(R.drawable.toast_right, R.string.add_transfer_list);
                                    }
                                    if (mCallback != null) {
                                        mCallback.handleRefresh(true, ConstantField.ServiceFunction.DOWNLOAD_FILE);
                                    }
                                });
                            }, null);
                } else {
                    callDownload(customizeFiles, (result, extraMsg) -> {
                        if (folderListView != null) {
                            folderListView.showImageTextToast(R.drawable.toast_right, R.string.add_transfer_list);
                        }
                        if (mCallback != null) {
                            mCallback.handleRefresh(true, ConstantField.ServiceFunction.DOWNLOAD_FILE);
                        }
                    });
                }

            }

            if (mPluralCallback != null) {
                mPluralCallback.handleBackEvent();
            }
        }
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

    private void callDownload(List<CustomizeFile> customizeFiles, ResultCallback callback) {
        Logger.d("zfy", "callDownload method");
        ThreadPool.getInstance().execute(() -> {
            for (CustomizeFile customizeFile : customizeFiles) {
                if (customizeFile != null) {
                    UUID uuid = null;
                    try {
                        uuid = UUID.fromString(customizeFile.getId());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (uuid != null) {
                        Logger.d("zfy", "selected file:" + customizeFile.getName());
                        downloadFile(uuid, customizeFile.getPath(), customizeFile.getName(), customizeFile.getSize(), customizeFile.getMd5());
                    }
                }
            }
            new Handler(Looper.getMainLooper()).post(() -> {
                callback.onResult(true, "");
            });
        });

    }

    private CustomizeFile getSelectFile() {
        CustomizeFile customizeFile = null;
        if (mCallback != null) {
            List<CustomizeFile> customizeFiles = mCallback.getSelectFiles();
            if (customizeFiles != null && customizeFiles.size() == 1) {
                customizeFile = customizeFiles.get(0);
            }
        }
        return customizeFile;
    }

    private List<UUID> getSelectUUID() {
        List<UUID> uuidList = null;
        if (mCallback != null) {
            List<CustomizeFile> customizeFiles = mCallback.getSelectFiles();
            if (customizeFiles != null) {
                uuidList = new ArrayList<>();
                for (CustomizeFile customizeFile : customizeFiles) {
                    if (customizeFile != null) {
                        UUID uuid = null;
                        try {
                            uuid = UUID.fromString(customizeFile.getId());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (uuid != null) {
                            uuidList.add(uuid);
                        }
                    }
                }
            }
        }
        return uuidList;
    }

    private void showDetailDialog() {
        if (detailDialog != null && !detailDialog.isShowing()) {
            CustomizeFile customizeFile = getSelectFile();
            if (customizeFile != null) {
                if (detailName != null) {
                    detailName.setText(customizeFile.getName());
                }
                if (detailTime != null) {
                    detailTime.setText(FormatUtil.formatTime(customizeFile.getTimestamp(), ConstantField.TimeStampFormat.FILE_API_MINUTE_FORMAT));
                }
                if (detailSize != null) {
                    detailSize.setText(FormatUtil.formatSimpleSize(customizeFile.getSize(), ConstantField.SizeUnit.FORMAT_1F));
                }
                if (detailPath != null) {
                    String path = customizeFile.getPath();
                    if (path == null) {
                        path = "";
                    }
                    if (path.startsWith("/") && mContext != null) {
                        path = mContext.getString(R.string.my_space) + path;
                    }
                    detailPath.setText(path);
                }
                detailDialog.show();
                Window window = detailDialog.getWindow();
                if (window != null) {
                    window.setGravity(Gravity.BOTTOM);
                    window.setWindowAnimations(R.style.bottom_dialog_anim_style);
                    window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                }
            }
        }
    }

    private void dismissDetailDialog() {
        if (detailDialog != null && detailDialog.isShowing()) {
            detailDialog.dismiss();
        }
    }

    private void showEditDialog() {
        if (editDialog != null && !editDialog.isShowing()) {
            CustomizeFile customizeFile = getSelectFile();
            if (fileImage != null) {
                if (customizeFile == null) {
                    fileImage.setImageDrawable(null);
                } else {
                    fileImage.setImageResource(FileUtil.getMimeIcon(customizeFile.getMime()));
                }
            }
            if (fileName != null) {
                fileName.setText(customizeFile == null ? "" : customizeFile.getName());
            }
            if (moreEditContainer != null) {
                moreEditContainer.removeAllViews();
                if (renameHorizontal != null && moreViewContainsRenameFlag) {
                    if (itemLayoutParams == null) {
                        moreEditContainer.addView(renameHorizontal);
                    } else {
                        moreEditContainer.addView(renameHorizontal, itemLayoutParams);
                    }
                    if (mContext != null && splitLayoutParams != null) {
                        View view = new View(mContext);
                        moreEditContainer.addView(view, splitLayoutParams);
                    }
                }
                if (copyHorizontal != null) {
                    if (itemLayoutParams == null) {
                        moreEditContainer.addView(copyHorizontal);
                    } else {
                        moreEditContainer.addView(copyHorizontal, itemLayoutParams);
                    }
                    if (mContext != null && splitLayoutParams != null) {
                        View view = new View(mContext);
                        moreEditContainer.addView(view, splitLayoutParams);
                    }
                }
                if (cutHorizontal != null) {
                    if (itemLayoutParams == null) {
                        moreEditContainer.addView(cutHorizontal);
                    } else {
                        moreEditContainer.addView(cutHorizontal, itemLayoutParams);
                    }
                }

            }
            editDialog.show();
            Window window = editDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.BOTTOM);
                window.setWindowAnimations(R.style.bottom_dialog_anim_style);
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissEditDialog() {
        if (editDialog != null && editDialog.isShowing()) {
            editDialog.dismiss();
        }
    }

    private void showDeleteDialog() {
        if (deleteDialog != null && !deleteDialog.isShowing()) {
            deleteDialog.show();
            Window window = deleteDialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissDeleteDialog() {
        if (deleteDialog != null && deleteDialog.isShowing()) {
            deleteDialog.dismiss();
        }
    }

    private void showRenameDialog() {
        if (renameDialog != null && !renameDialog.isShowing()) {
            CustomizeFile customizeFile = getSelectFile();
            if (customizeFile != null) {
                String filename = customizeFile.getName();
                String name = filename;
                if (filename != null) {
                    int pointIndex = filename.lastIndexOf(".");
                    if (pointIndex >= 0 && pointIndex < filename.length()) {
                        name = filename.substring(0, pointIndex);
                    }
                }
                if (name == null) {
                    name = "";
                }
                renameDialogInput.setText(name);
                renameDialogInput.setSelection(name.length());
            }
            setEditTextDialogPattern(renameDialogInput, renameDialogInputClear, renameDialogConfirm);
            renameDialog.show();
            Window window = renameDialog.getWindow();
            if (window != null) {
                window.setGravity(Gravity.CENTER);
                if (mContext != null) {
                    window.setLayout(mContext.getResources().getDimensionPixelSize(R.dimen.dp_259), ViewGroup.LayoutParams.WRAP_CONTENT);
                }
            }
        }
    }

    private void dismissRenameDialog() {
        if (renameDialog != null && renameDialog.isShowing()) {
            renameDialog.dismiss();
        }
    }

    private void showRestoreDialog() {
        if (restoreDialog != null && !restoreDialog.isShowing()) {
            restoreDialog.show();
            Window window = restoreDialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissRestoreDialog() {
        if (restoreDialog != null && restoreDialog.isShowing()) {
            restoreDialog.dismiss();
        }
    }

    private void showDeleteCompleteDialog() {
        if (deleteCompleteDialog != null && !deleteCompleteDialog.isShowing()) {
            deleteCompleteDialog.show();
            Window window = deleteCompleteDialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissDeleteCompleteDialog() {
        if (deleteCompleteDialog != null && deleteCompleteDialog.isShowing()) {
            deleteCompleteDialog.dismiss();
        }
    }

    public void showClearCompleteDialog() {
        if (clearCompleteDialog != null && !clearCompleteDialog.isShowing()) {
            clearCompleteDialog.show();
            Window window = clearCompleteDialog.getWindow();
            if (window != null) {
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    private void dismissClearCompleteDialog() {
        if (clearCompleteDialog != null && clearCompleteDialog.isShowing()) {
            clearCompleteDialog.dismiss();
        }
    }

    private void showShareDialog() {
        if (shareLoadingDialog != null && !shareLoadingDialog.isShowing()) {
            shareLoadingDialog.show();
        }
    }

    public boolean isShareDialogShowing() {
        return (shareLoadingDialog != null && shareLoadingDialog.isShowing());
    }

    public void dismissShareDialog(String absolutePath, boolean isShare) {
        if (shareLoadingDialog != null && shareLoadingDialog.isShowing()) {
            shareLoadingDialog.dismiss();
            if (isShare) {
                if (!TextUtils.isEmpty(absolutePath)) {
//                    ShareUtil.shareFile(mContext, absolutePath);
                } else {
                    ToastUtil.showToast(mContext.getResources().getString(R.string.share_fail));
                }
            }
        }
    }

    public void showFileDialog(int selectNumber) {
        if (selectNumber > 0 && fileContainer1 != null && fileContainer2 != null && fileContainer3 != null
                && fileContainer4 != null && fileContainer5 != null) {
            boolean isContainsFolder = false;
            if (mCallback != null) {
                List<CustomizeFile> customizeFiles = mCallback.getSelectFiles();
                if (customizeFiles != null) {
                    for (CustomizeFile customizeFile : customizeFiles) {
                        if (customizeFile != null && ConstantField.MimeType.FOLDER.equals(customizeFile.getMime())) {
                            isContainsFolder = true;
                            break;
                        }
                    }
                }
            }
            folderListView.registerCallback(this);
            fileContainer1.removeAllViews();
            fileContainer2.removeAllViews();
            fileContainer3.removeAllViews();
            fileContainer4.removeAllViews();
            fileContainer5.removeAllViews();
            moreViewContainsRenameFlag = true;

            if (isOperationRecordStyle) {
                //个人动态样式
                if (downloadVertical != null) {
                    fileContainer1.addView(downloadVertical);
                }
                if (shareVertical != null) {
                    fileContainer2.addView(shareVertical);
                }
                if (deleteVertical != null) {
                    fileContainer3.addView(deleteVertical);
                }
                fileContainer4.setVisibility(View.GONE);
                fileContainer5.setVisibility(View.GONE);
            } else {
                if (selectNumber > 1) {
                    //多选
                    //文件样式
                    if (isContainsFolder) {
                        //包含文件夹
                        if (deleteVertical != null) {
                            fileContainer1.addView(deleteVertical);
                        }
                        if (copyVertical != null) {
                            fileContainer2.addView(copyVertical);
                        }
                        if (cutVertical != null) {
                            fileContainer3.addView(cutVertical);
                        }
                    } else {
                        //不包含文件夹
                        if (downloadVertical != null) {
                            fileContainer1.addView(downloadVertical);
                        }
                        if (deleteVertical != null) {
                            fileContainer2.addView(deleteVertical);
                        }
                        if (copyVertical != null) {
                            fileContainer3.addView(copyVertical);
                        }
                        if (cutVertical != null) {
                            fileContainer4.addView(cutVertical);
                        }
                    }
                } else {
                    //单选
                    //文件样式
                    if (isContainsFolder) {
                        moreViewContainsRenameFlag = false;
                        if (deleteVertical != null) {
                            fileContainer1.addView(deleteVertical);
                        }
                        if (detailVertical != null) {
                            fileContainer2.addView(detailVertical);
                        }
                        if (renameVertical != null) {
                            fileContainer3.addView(renameVertical);
                        }
                        if (copyVertical != null) {
                            fileContainer4.addView(copyVertical);
                        }
                        if (cutVertical != null) {
                            fileContainer5.addView(cutVertical);
                        }
                    } else {
                        if (downloadVertical != null) {
                            fileContainer1.addView(downloadVertical);
                        }
                        if (shareVertical != null) {
                            fileContainer2.addView(shareVertical);
                        }
                        if (deleteVertical != null) {
                            fileContainer3.addView(deleteVertical);
                        }
                        if (detailVertical != null) {
                            fileContainer4.addView(detailVertical);
                        }
                        if (moreVertical != null) {
                            fileContainer5.addView(moreVertical);
                        }
                    }
                }
            }
        }
        if (mCallback != null) {
            mCallback.fileDialog(fileDialogView, true);
        }
    }

    public void dismissFileDialog() {
        folderListView.unregisterCallback();
        if (mCallback != null) {
            mCallback.fileDialog(fileDialogView, false);
        }
    }

    public void showFileRecycleDialog() {
        if (fileRecycleContainer1 != null && fileRecycleContainer2 != null) {
            fileRecycleContainer1.removeAllViews();
            fileRecycleContainer2.removeAllViews();
            if (restoreVertical != null) {
                fileRecycleContainer1.addView(restoreVertical);
            }
            if (deleteCompleteVertical != null) {
                fileRecycleContainer2.addView(deleteCompleteVertical);
            }
        }
        if (mCallback != null) {
            mCallback.fileDialog(fileRecycleDialogView, true);
        }
    }

    public void dismissFileRecycleDialog() {
        if (mCallback != null) {
            mCallback.fileDialog(fileRecycleDialogView, false);
        }
    }

    public void registerCallback(FileEditCallback callback) {
        mCallback = callback;
    }

    public void registerPluralCallback(FileEditPluralCallback callback) {
        mPluralCallback = callback;
    }

    public void registerRecycleBinCallback(FileEditRecycleBinCallback callback) {
        mRecycleBinCallback = callback;
    }

    public void setFrom(String from) {
        mFrom = from;
    }

    //设置为两种方式删除（相簿内删除使用）
    public void useTwoWaysDelete(int albumId) {
        this.showTwoWaysDeleteStyle = true;
        this.mAlbumId = albumId;
    }

    public void setAlbumId(int albumId) {
        this.mAlbumId = albumId;
    }

    //设置为个人动态样式（仅显示下载、分享、删除）
    public void setOperationRecordStyle() {
        this.isOperationRecordStyle = true;
    }

    public void unregisterCallback() {
        mCallback = null;
    }

    public void unregisterPluralCallback() {
        mPluralCallback = null;
    }

    public void unregisterRecycleBinCallback() {
        mRecycleBinCallback = null;
    }

    public void getEulixSpaceStorage(Integer page) {
        mController.getEulixSpaceStorage(page);
    }

    public void getEulixSpaceStorage(Integer page, String order) {
        getEulixSpaceStorage(page, order, false);
    }

    public void getEulixSpaceStorage(Integer page, String order, boolean isFore) {
        mController.getEulixSpaceStorage(page, order, isFore);
    }

    public void getEulixSpaceStorage(UUID uuid, Integer page, Integer pageSize, String order) {
        mController.getEulixSpaceStorage(uuid, page, pageSize, order);
    }

    public void renameFile(UUID uuid, String filename) {
        mController.renameFile(uuid, filename);
    }

    public void copyFile(UUID destinationUUID, List<UUID> uuids) {
        mController.copyFile(destinationUUID, uuids);
    }

    public void cutFile(UUID destinationUUID, List<UUID> uuids) {
        mController.cutFile(destinationUUID, uuids);
    }

    public void deleteFile(List<UUID> uuids) {
        showLoading(mContext.getResources().getString(R.string.deleting));
        mController.deleteFile(uuids);
    }

    public void restoreRecycledFile(List<UUID> uuids) {
        showLoading(mContext.getResources().getString(R.string.restoring));
        mController.restoreRecycled(uuids);
    }

    public void clearRecycledFile(List<UUID> uuids) {
        showLoading("");
        mController.clearRecycled(uuids);
    }

    public void downloadFile(UUID uuid, String filepath, String filename, long fileSize, String md5) {
        mController.downloadFile(uuid, filepath, filename, fileSize, md5);
    }

    public void handleNext(String currentDirectory, boolean isSearch) {
        mController.handleNext(currentDirectory, isSearch);
    }

    public UUID handleBack() {
        return mController.handleBack();
    }

    public void reset() {
        mController.reset();
    }

    public int getDepth() {
        return mController.getDepth();
    }

    public int getSearchDepth() {
        return mController.getSearchDepth();
    }

    public boolean isUUIDSame() {
        return mController.isUUIDSame();
    }

    public UUID getUUID() {
        return mController.getmUuid();
    }

    public ArrayStack<UUID> getUUIDStack() {
        return mController.getUuidStack();
    }

    public void setStack(ArrayStack<UUID> uuids) {
        mController.setUuidStack(uuids);
    }

    @Override
    public void getEulixSpaceFileListResult(Integer code, String currentDirectory, List<CustomizeFile> customizeFiles, PageInfo pageInfo) {
        if (mPluralCallback != null) {
            mPluralCallback.handleGetEulixSpaceFileListResult(code, currentDirectory, customizeFiles, pageInfo);
        }
    }

    @Override
    public void getFolderInfoResult(Integer code, String folderUuid, String name, Long operationAt, String path, Long size) {
        if (mPluralCallback != null) {
            mPluralCallback.handleFolderDetail(folderUuid, name, operationAt, path, size);
        }
        CustomizeFile customizeFile = getSelectFile();
        if (size != null && customizeFile != null && ConstantField.MimeType.FOLDER.equals(customizeFile.getMime())
                && folderUuid != null && folderUuid.equals(customizeFile.getId()) && detailDialog != null && detailDialog.isShowing()) {
            if (detailSize != null) {
                detailSize.setText(FormatUtil.formatSimpleSize(size, ConstantField.SizeUnit.FORMAT_1F));
            }
        }
    }

    @Override
    public void modifyEulixSpaceFileResult(Integer code, boolean isOk, String parentId, Integer affectRows, String newFileName) {
        if (folderListView != null) {
            if (isOk) {
                folderListView.showImageTextToast(R.drawable.toast_right, R.string.modify_success);
            } else if (code != null && code == ConstantField.SERVER_EXCEPTION_CODE) {
                folderListView.showServerExceptionToast();
            } else {
                folderListView.showImageTextToast(R.drawable.toast_wrong, R.string.duplicate_filename_failed);
            }
        }
        if (!isOk && code != null && code == ConstantField.OBTAIN_ACCESS_TOKEN_CODE) {
            obtainAccessToken();
        }
        if (isOk) {
            CustomizeFile customizeFile = getSelectFile();
            if (customizeFile != null) {
                EventBusUtil.post(new RenameFileEvent(customizeFile.getId(), newFileName));
            }
        }
        if (mCallback != null) {
            mCallback.handleRefresh(isOk, ConstantField.ServiceFunction.MODIFY_FILE);
        }
    }

    @Override
    public void copyEulixSpaceFileResult(Integer code, boolean isOk, String sourceParentId, String destinationParentId, Integer affectRows) {
        if (folderListView != null) {
            if (isOk) {
                folderListView.showImageTextToast(R.drawable.toast_right, R.string.copy_success);
            } else if (code != null && code == ConstantField.SERVER_EXCEPTION_CODE) {
                folderListView.showServerExceptionToast();
            } else {
                folderListView.showImageTextToast(R.drawable.toast_wrong, R.string.copy_fail);
            }
        }
        if (!isOk && code != null && code == ConstantField.OBTAIN_ACCESS_TOKEN_CODE) {
            obtainAccessToken();
        }
        if (mCallback != null) {
            mCallback.handleRefresh(isOk, ConstantField.ServiceFunction.COPY_FILE);
        }
    }

    @Override
    public void cutEulixSpaceFileResult(Integer code, boolean isOk, String sourceParentId, String destinationParentId, Integer affectRows) {
        if (folderListView != null) {
            if (isOk) {
                folderListView.showImageTextToast(R.drawable.toast_right, R.string.cut_success);
            } else if (code != null && code == ConstantField.SERVER_EXCEPTION_CODE) {
                folderListView.showServerExceptionToast();
            } else {
                folderListView.showImageTextToast(R.drawable.toast_wrong, R.string.cut_fail);
            }
        }
        if (!isOk && code != null && code == ConstantField.OBTAIN_ACCESS_TOKEN_CODE) {
            obtainAccessToken();
        }
        if (mCallback != null) {
            mCallback.handleRefresh(isOk, ConstantField.ServiceFunction.MOVE_FILE);
        }
    }

    @Override
    public void deleteEulixSpaceFileResult(Integer code, boolean isOk, String parentId, Integer affectRows, FileRsp fileRsp) {
        closeLoading();
        if (code != null && ConstantField.KnownError.FileError.OPERATE_ASYNC_TASK_CODE == code && fileRsp != null && fileRsp.getResults() != null && fileRsp.getResults().taskId != null) {
            //文件操作时间长，需异步处理，展示进度
            BaseResponseResult fileRspResults = fileRsp.getResults();
            int percent = 0;
            if (fileRspResults.total > 0) {
                percent = fileRspResults.processed * 100 / fileRspResults.total;
            }
            mTaskProgressDialog = EulixDialogUtil.getTaskProgressDialog(mContext, mContext.getResources().getString(R.string.deleting));
            mTaskProgressDialog.setProgress(percent);
            mTaskProgressDialog.show();

            String taskId = fileRspResults.taskId;
            if (mController != null) {
                mController.poolCheckAsyncTaskStatus(taskId, FileEditController.OPERATE_TYPE_DELETE);
            }
        } else {
            if (folderListView != null) {
                if (isOk) {
                    folderListView.showImageTextToast(R.drawable.toast_right, R.string.delete_success);
                } else if (code != null && code == ConstantField.SERVER_EXCEPTION_CODE) {
                    folderListView.showServerExceptionToast();
                } else {
                    folderListView.showImageTextToast(R.drawable.toast_wrong, R.string.delete_fail);
                }
            }
            if (!isOk && code != null && code == ConstantField.OBTAIN_ACCESS_TOKEN_CODE) {
                obtainAccessToken();
            }
            callAfterDeleteFinish(isOk);
        }
    }

    @Override
    public void onAsyncTaskStatusRefresh(boolean isSuccess, String taskId, String taskStatus, int precessed, int total, int operateType) {
        if (isSuccess && taskStatus != null) {
            switch (taskStatus) {
                case ConstantField.FileAsyncTaskStatus.STATUS_PROCESSING:
                    if (mTaskProgressDialog != null && total > 0) {
                        mTaskProgressDialog.setProgress(precessed * 100 / total);
                    }
                    break;
                case ConstantField.FileAsyncTaskStatus.STATUS_SUCCESS:
                    if (mTaskProgressDialog != null) {
                        mTaskProgressDialog.dismiss();
                        mTaskProgressDialog = null;
                    }
                    switch (operateType) {
                        case FileEditController.OPERATE_TYPE_DELETE:
                            if (folderListView != null) {
                                folderListView.showImageTextToast(R.drawable.toast_right, R.string.delete_success);
                            }
                            callAfterDeleteFinish(true);
                            break;
                        case FileEditController.OPERATE_TYPE_RESTORE:
                            if (folderListView != null) {
                                folderListView.showImageTextToast(R.drawable.toast_right, R.string.restore_success);
                            }
                            callAfterRestoreFinish(true);
                        default:

                    }
                    break;
                case ConstantField.FileAsyncTaskStatus.STATUS_FAILED:
                    if (mTaskProgressDialog != null) {
                        mTaskProgressDialog.dismiss();
                        mTaskProgressDialog = null;
                    }
                    switch (operateType) {
                        case FileEditController.OPERATE_TYPE_DELETE:
                            if (folderListView != null) {
                                folderListView.showImageTextToast(R.drawable.toast_wrong, R.string.delete_fail);
                            }
                            callAfterDeleteFinish(false);
                            break;
                        case FileEditController.OPERATE_TYPE_RESTORE:
                            if (folderListView != null) {
                                folderListView.showImageTextToast(R.drawable.toast_wrong, R.string.restore_fail);
                            }
                            callAfterRestoreFinish(false);
                        default:
                    }
                    break;
                default:
            }
        } else {
            if (mTaskProgressDialog != null) {
                mTaskProgressDialog.dismiss();
                mTaskProgressDialog = null;
            }
            switch (operateType) {
                case FileEditController.OPERATE_TYPE_DELETE:
                    if (folderListView != null) {
                        folderListView.showImageTextToast(R.drawable.toast_wrong, R.string.delete_fail);
                    }
                    callAfterDeleteFinish(false);
                    break;
                case FileEditController.OPERATE_TYPE_RESTORE:
                    if (folderListView != null) {
                        folderListView.showImageTextToast(R.drawable.toast_wrong, R.string.restore_fail);
                    }
                    callAfterRestoreFinish(false);
                default:
            }
        }
    }

    private void callAfterDeleteFinish(boolean isSuccess) {
        if (isSuccess) {
            CustomizeFile customizeFile = getSelectFile();
            //删除本地相簿内数据
            if (!mSelectedList.isEmpty()) {
                List<String> uuidList = new ArrayList<>();
                for (CustomizeFile customizeItem : mSelectedList) {
                    if (customizeItem != null && (customizeItem.getMime().contains("image") || customizeItem.getMime().contains("video"))) {
                        UUID uuid = null;
                        try {
                            uuid = UUID.fromString(customizeItem.getId());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (uuid != null) {
                            uuidList.add(uuid.toString());
                        }
                    }
                }
                if (!uuidList.isEmpty()) {
                    EventBusUtil.post(new DeleteFileEvent(uuidList));
                }
                mSelectedList.clear();
            }
        }
        if (mCallback != null) {
            mCallback.handleRefresh(isSuccess, ConstantField.ServiceFunction.DELETE_FILE);
        }
    }

    private void callAfterRestoreFinish(boolean isSuccess) {
        if (mCallback != null) {
            mCallback.handleRefresh(isSuccess, ConstantField.ServiceFunction.RESTORE_RECYCLED);
        }
    }

    @Override
    public void downloadEulixSpaceFileResult(Integer code, boolean isOk) {
        if (!isOk && code != null && code == ConstantField.OBTAIN_ACCESS_TOKEN_CODE) {
            obtainAccessToken();
        }
        if (mCallback != null) {
            mCallback.handleRefresh(isOk, ConstantField.ServiceFunction.DOWNLOAD_FILE);
        }
    }

    @Override
    public void onCheckFileIsExist(String absolutePath) {
        dismissShareDialog(absolutePath, true);
    }

    @Override
    public void handleFileSearchShowOrDestroy(boolean isShow) {
        if (mPluralCallback != null) {
            mPluralCallback.handleShowOrDismissFileSearch(isShow);
        }
    }

    @Override
    public void restoreRecycledResult(Integer code, boolean isOk, RecycledListResponse response) {
        closeLoading();
        if (code != null && ConstantField.KnownError.FileError.OPERATE_ASYNC_TASK_CODE == code && response != null && response.getResults() != null && response.getResults().taskId != null) {
            //文件操作时间长，需异步处理，展示进度
            RecycledListResult rspResults = response.getResults();
            int percent = 0;
            if (rspResults.total > 0) {
                percent = rspResults.processed * 100 / rspResults.total;
            }
            mTaskProgressDialog = EulixDialogUtil.getTaskProgressDialog(mContext, mContext.getResources().getString(R.string.restoring));
            mTaskProgressDialog.setProgress(percent);
            mTaskProgressDialog.show();

            String taskId = rspResults.taskId;
            if (mController != null) {
                mController.poolCheckAsyncTaskStatus(taskId, FileEditController.OPERATE_TYPE_RESTORE);
            }
        } else {
            if (folderListView != null) {
                if (isOk) {
                    folderListView.showImageTextToast(R.drawable.toast_right, R.string.restore_success);
                } else if (code != null && code == ConstantField.SERVER_EXCEPTION_CODE) {
                    folderListView.showServerExceptionToast();
                } else {
                    folderListView.showImageTextToast(R.drawable.toast_wrong, R.string.restore_fail);
                }
            }
            if (!isOk && code != null) {
                switch (code) {
                    case ConstantField.OBTAIN_ACCESS_TOKEN_CODE:
                        obtainAccessToken();
                        break;
                    case ConstantField.FILE_NOT_EXIST:
                        isOk = true;
                        break;
                    default:
                        break;
                }
            }
            if (mCallback != null) {
                mCallback.handleRefresh(isOk, ConstantField.ServiceFunction.RESTORE_RECYCLED);
            }
        }

    }

    @Override
    public void clearRecycledResult(Integer code, boolean isOk) {
        closeLoading();
        if (folderListView != null) {
            if (isOk) {
                folderListView.showImageTextToast(R.drawable.toast_right, R.string.delete_success);
            } else if (code != null && code == ConstantField.SERVER_EXCEPTION_CODE) {
                folderListView.showServerExceptionToast();
            } else {
                folderListView.showImageTextToast(R.drawable.toast_wrong, R.string.delete_fail);
            }
        }
        if (!isOk && code != null) {
            switch (code) {
                case ConstantField.OBTAIN_ACCESS_TOKEN_CODE:
                    obtainAccessToken();
                    break;
                case ConstantField.FILE_NOT_EXIST:
                    isOk = true;
                    break;
                default:
                    break;
            }
        }
        if (mCallback != null) {
            mCallback.handleRefresh(isOk, ConstantField.ServiceFunction.CLEAR_RECYCLED);
        }
    }

    @Override
    public void obtainAccessToken() {
        if (mContext != null) {
            Intent serviceIntent = new Intent(mContext, EulixSpaceService.class);
            serviceIntent.setAction(ConstantField.Action.TOKEN_ACTION);
            mContext.startService(serviceIntent);
        }
    }

    @Override
    public int getSelectFilesSize() {
        int size = -1;
        if (mCallback != null) {
            List<CustomizeFile> customizeFiles = mCallback.getSelectFiles();
            if (customizeFiles != null) {
                size = customizeFiles.size();
            }
        }
        return size;
    }

    @Override
    public UUID getCurrentFolderUUID() {
        if (mController != null) {
            return mController.getmUuid();
        } else {
            return null;
        }
    }

    @Override
    public ArrayStack<UUID> getCurrentUUIDStack() {
        if (mCallback != null) {
            return mCallback.handleCurrentUUIDStack();
        } else {
            return null;
        }
    }

    @Override
    public void refreshEulixSpaceStorage(UUID parentUUID) {
        if (mPluralCallback != null) {
            mPluralCallback.handleRefreshEulixSpaceStorage(parentUUID);
        }
    }

    @Override
    public void dismissFolderListView(boolean isConfirm, UUID selectUUID, Boolean isCopy, ArrayStack<UUID> uuids, List<UUID> newFolderUUIDs) {
        if (isConfirm) {
            if (isCopy != null) {
                List<UUID> fileUuids = null;
                if (mCallback != null) {
                    List<CustomizeFile> customizeFiles = mCallback.getSelectFiles();
                    if (customizeFiles != null) {
                        fileUuids = new ArrayList<>();
                        for (CustomizeFile customizeFile : customizeFiles) {
                            if (customizeFile != null) {
                                UUID uuid = null;
                                try {
                                    uuid = UUID.fromString(customizeFile.getId());
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                if (uuid != null) {
                                    fileUuids.add(uuid);
                                }
                            }
                        }
                    }
                }
                if (fileUuids != null) {
                    if (isCopy) {
                        copyFile(selectUUID, fileUuids);
                    } else {
                        cutFile(selectUUID, fileUuids);
                    }
                }
            }
            dismissEditDialog();
        }
        if (mPluralCallback != null) {
            mPluralCallback.handleDismissFolderListView(isConfirm, selectUUID, isCopy, uuids, newFolderUUIDs);
        }
    }

    @Override
    public void onClick(View v) {
        if (v != null) {
            switch (v.getId()) {
                case R.id.detail_exit:
                    dismissDetailDialog();
                    break;
                case R.id.more_edit_exit:
                    dismissEditDialog();
                    break;
                default:
                    break;
            }
        }
    }

    private void showLoading(String text) {
        if (!(mContext instanceof Activity)) {
            return;
        }
        if (TextUtils.isEmpty(text)) {
            text = mContext.getResources().getString(R.string.waiting);
        }
        if (mLoadingDialog == null) {
            mLoadingDialog = EulixDialogUtil.createLoadingDialog(mContext, text, false);
        } else {
            mLoadingDialog.setText(text);
        }
        mLoadingDialog.show();
    }

    private void closeLoading() {
        if (mLoadingDialog != null) {
            mLoadingDialog.dismiss();
            mLoadingDialog = null;
        }
    }
}
