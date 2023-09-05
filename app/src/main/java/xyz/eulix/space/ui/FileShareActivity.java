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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.lang.ref.WeakReference;

import xyz.eulix.space.R;
import xyz.eulix.space.bean.CustomizeFile;
import xyz.eulix.space.interfaces.ResultCallback;
import xyz.eulix.space.network.files.FileListUtil;
import xyz.eulix.space.transfer.TransferHelper;
import xyz.eulix.space.transfer.db.TransferDBManager;
import xyz.eulix.space.transfer.model.TransferItem;
import xyz.eulix.space.transfer.model.TransferItemFactory;
import xyz.eulix.space.util.share.ShareUtil;
import xyz.eulix.space.view.dialog.EulixDialogUtil;
import xyz.eulix.space.view.dialog.EulixLoadingDialog;

/**
 * Author: 		Zhufy
 * Description: 单文件数据流分享
 * History:		2022/6/13
 */
public class FileShareActivity extends AppCompatActivity {
    public static final String KEY_FILE = "key_file";
    public static final String KEY_FROM = "from";
    private CustomizeFile mCustomizeFile;
    public EulixLoadingDialog mLoadingDialog;
    public boolean isFinish = false;
    private String from;

    public static void startShareActivity(Context context, CustomizeFile customizeFile, String from) {
        Intent intent = new Intent(context, FileShareActivity.class);
        intent.putExtra(KEY_FILE, customizeFile);
        intent.putExtra(KEY_FROM, from);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mCustomizeFile = (CustomizeFile) getIntent().getSerializableExtra(KEY_FILE);
        from = getIntent().getStringExtra(KEY_FROM);
        if (TextUtils.isEmpty(from)) {
            from = TransferHelper.FROM_FILE;
        }

        mLoadingDialog = EulixDialogUtil.createLoadingDialog(this, getResources().getString(R.string.waiting), true);
        mLoadingDialog.show();
        mLoadingDialog.setOnDismissListener(dialog -> {
            isFinish = true;
            finish();
        });

        if (mCustomizeFile != null) {
            checkFileExist(getApplicationContext(), mCustomizeFile.getName(), mCustomizeFile.getId(),
                    mCustomizeFile.getPath(), mCustomizeFile.getSize(), mCustomizeFile.getMd5(),
                    new HandleListener(FileShareActivity.this));
        } else {
            finish();
        }
    }

    private class HandleListener implements ResultCallback {

        private WeakReference<FileShareActivity> weakReference;

        public HandleListener(FileShareActivity activity) {
            weakReference = new WeakReference<>(activity);
        }

        @Override
        public void onResult(boolean result, String extraMsg) {
            FileShareActivity activity = weakReference.get();
            if (activity != null && !activity.isFinish) {
                if (activity.mLoadingDialog != null && activity.mLoadingDialog.isShowing()) {
                    activity.mLoadingDialog.dismiss();
                }
                if (result) {
                    String fileAbsolutePath = extraMsg;
                    ShareUtil.shareFile(activity, fileAbsolutePath);
                    activity.finish();
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
        super.onBackPressed();
    }

    public void checkFileExist(Context context, String fileName, String uuidStr, String filePath, long fileSize, String md5, ResultCallback mCallback) {
        String uniqueTag = TransferItemFactory.getUniqueTag(TransferHelper.TYPE_DOWNLOAD, null, null, null, uuidStr);
        TransferItem downloadItem = TransferDBManager.getInstance(context).queryByUniqueTag(uniqueTag, TransferHelper.TYPE_DOWNLOAD);
        //已下载
        if (downloadItem != null && downloadItem.state == TransferHelper.STATE_FINISH) {
            File fileDownload = new File(downloadItem.localPath, fileName);
            if (fileDownload.exists() && mCallback != null) {
                //文件存在
                mCallback.onResult(true, fileDownload.getAbsolutePath());
                return;
            }
        }

        //查看是否已缓存
        String cacheFilePath = FileListUtil.getCacheFilePath(context, fileName);
        if (!TextUtils.isEmpty(cacheFilePath)) {
            //文件存在
            if (mCallback != null) {
                mCallback.onResult(true, cacheFilePath);
            }
            return;
        }

        //文件不存在，重新下载
        FileListUtil.downloadFile(context.getApplicationContext(), uuidStr, filePath, fileName, fileSize, md5, true, from, new ResultCallback() {
            @Override
            public void onResult(boolean result, String extraMsg) {
                if (result) {
                    int cacheTransferType = TransferHelper.TYPE_CACHE;
                    String uniqueTag = TransferItemFactory.getUniqueTag(cacheTransferType, null, null, null, uuidStr);
                    TransferItem cacheItem = TransferDBManager.getInstance(context).queryByUniqueTag(uniqueTag, cacheTransferType);
                    File file = new File(cacheItem.localPath, cacheItem.keyName);
                    if (file.exists()) {
                        mCallback.onResult(true, file.getAbsolutePath());
                    } else {
                        mCallback.onResult(false, null);
                    }
                } else {
                    mCallback.onResult(false, null);
                }
            }
        });
    }

}