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

package xyz.eulix.space.presenter;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import xyz.eulix.space.R;
import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.interfaces.ResultCallbackObj;
import xyz.eulix.space.network.files.FileListUtil;
import xyz.eulix.space.transfer.TransferHelper;
import xyz.eulix.space.transfer.db.TransferDBManager;
import xyz.eulix.space.transfer.model.TransferItem;
import xyz.eulix.space.transfer.model.TransferItemFactory;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.FileUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.NetUtils;
import xyz.eulix.space.util.ThreadPool;
import xyz.eulix.space.view.dialog.EulixDialogUtil;

/**
 * Author:      Zhu Fuyu
 * Description: 文件预览页Presenter
 * History:     2021/9/2
 */
public class FilePreviewPresenter extends AbsPresenter<FilePreviewPresenter.IFilePreview> {
    private boolean isActivityDestroy = false;

    public interface IFilePreview extends IBaseView {
        //显示预览页面
        void showPreview(String absolutePath, boolean isOriginal);

        //下载压缩图失败
        void downloadCompressedImageFailed();

        //下载预览文件失败
        void downloadFilePreviewFailed();

        //显示缓存页面
        void showCacheView();

        //文件不存在
        void fileNotExist();

        //调用分享
        void callShareUtil(String fileAbsolutePath);

        //展示缩略图
        void showThumbImage();

        //退出预览
        void exitPreview();

        //喜欢状态变更
        void onLikeStateChange(Boolean result, boolean isLike, String uuid);
    }

    //查询文件是否已下载或缓存
    public void checkFileExist(Activity activity, String fileName, String fileUuid, String filePath, long fileSize, String md5, String mimeType, boolean isLocal, String from) {
        if (isLocal) {
            //预览文件
            File localFile = new File(filePath, fileName);
            if (localFile.exists()) {
                iView.showPreview(localFile.getAbsolutePath(), true);
            } else {
                Logger.d("local file not exist:" + localFile.getAbsolutePath());
                iView.fileNotExist();
            }
        } else {
            //文件通过本设备传输且本地对应文件存在
            ArrayList<TransferItem> finishList = TransferDBManager.getInstance(context).queryFinishItemsByUUID(fileUuid);
            if (finishList != null && finishList.size() > 0) {
                for (TransferItem item : finishList) {
                    File localFile = new File(item.localPath, item.keyName);
                    if (localFile.exists()) {
                        Logger.d("zfy", "has transferred item,open local file");
                        iView.showPreview(localFile.getAbsolutePath(), true);
                        return;
                    }
                }
            }

            //查看是否已缓存
            String cacheFilePath = FileListUtil.getCacheFilePath(context, fileName);
            if (!TextUtils.isEmpty(cacheFilePath)) {
                iView.showPreview(cacheFilePath, true);
                return;
            }

            // 判断是否缓存中
            int cacheTransferType = TransferHelper.TYPE_CACHE;
            String uniqueTag = TransferItemFactory.getUniqueTag(cacheTransferType, null, null, null, fileUuid);
            TransferItem cacheItem = TransferDBManager.getInstance(context).queryByUniqueTag(uniqueTag, cacheTransferType);
            if (cacheItem != null && cacheItem.state == TransferHelper.STATE_DOING) {
                //正在下载
                if (mimeType.contains("image")) {
                    //图片显示缩略图及进度条
                    iView.showThumbImage();
                } else {
                    iView.showCacheView();
                }
                return;

            }


            if (mimeType.contains("image")) {
                //图片
                //查看压缩图是否已下载
                String compressedImagePath = FileListUtil.getCompressedPath(context, fileUuid);
                if (!TextUtils.isEmpty(compressedImagePath)) {
                    iView.showPreview(compressedImagePath, false);
                } else {
                    iView.showThumbImage();
                    //下载压缩图
                    FileListUtil.downloadCompressedImage(context, fileUuid, fileName, from, (result, path) -> {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                if (result) {
                                    iView.showPreview(path, false);
                                } else {
//                                    下载压缩图失败，缓存原图
                                    Logger.d("zfy", "download compressed image failed,start download original image");
                                    FileListUtil.downloadFile(context, fileUuid, filePath, fileName, fileSize, md5, true, from, null);
                                }
                            }
                        });
                    });
                }
            } else {
                //文件不支持应用内预览，发起缓存
                iView.showCacheView();
                if (NetUtils.isMobileNetWork(activity) && !ConstantField.sIAllowTransferWithMobileData) {
                    EulixDialogUtil.showChooseAlertDialog(activity, context.getResources().getString(R.string.mobile_data_download),
                            context.getResources().getString(R.string.mobile_data_download_desc), context.getResources().getString(R.string.ok),
                            (dialog, which) -> {
                                ConstantField.sIAllowTransferWithMobileData = true;
                                ThreadPool.getInstance().execute(() -> {
                                    FileListUtil.downloadFile(context, fileUuid, filePath, fileName, fileSize, md5, true, from, null);
                                });
                            }, (dialog, which) -> {
                                //拒绝流量下载
                                iView.exitPreview();
                            });
                } else {
                    ThreadPool.getInstance().execute(() -> {
                        FileListUtil.downloadFile(context, fileUuid, filePath, fileName, fileSize, md5, true, from, null);
                    });
                }
            }
        }
    }


    public void getOriginalImage(Context context, String fileName, String uuidStr, String filePath, long fileSize, String md5, String from) {
        //文件不存在，发起下载
        FileListUtil.downloadFile(context, uuidStr, filePath, fileName, fileSize, md5, true, from, null);
    }

    //解析txt文件
    public void parseTxtFile(String absolutePath, ResultCallbackObj callback) {
        ThreadPool.getInstance().execute(() -> {
            StringBuilder content = new StringBuilder();
            File file = new File(absolutePath);
            try (InputStream inputStream = new FileInputStream(file);
                 InputStreamReader reader = new InputStreamReader(inputStream, FileUtil.getFileCharset(file));
                 BufferedReader bufferedReader = new BufferedReader(reader)) {

                String line;
                long currentLines = 0;
                while ((line = bufferedReader.readLine()) != null) {
                    if (isActivityDestroy) {
                        break;
                    }
                    currentLines++;
                    content.append(line).append("\n");
                    if (currentLines % 1000 == 0) {
                        //分段展示
                        String textTmp = content.toString();
                        content.delete(0, content.length() - 1);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            callback.onResult(true, textTmp);
                        });
                        //延迟加载，防止主线程阻塞
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e){
                            Thread.currentThread().interrupt();
                        }
                    }
                }
                if (!isActivityDestroy && content.length() > 0) {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        callback.onResult(true, content.toString());
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

    }

    public void setActivityDestroy() {
        this.isActivityDestroy = true;
    }
}
