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

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;

import xyz.eulix.space.abs.AbsPresenter;
import xyz.eulix.space.abs.IBaseView;
import xyz.eulix.space.network.files.FileListUtil;
import xyz.eulix.space.transfer.TransferHelper;
import xyz.eulix.space.transfer.db.TransferDBManager;
import xyz.eulix.space.transfer.model.TransferItem;
import xyz.eulix.space.transfer.model.TransferItemFactory;
import xyz.eulix.space.util.Logger;

/**
 * Author:      Zhu Fuyu
 * Description:
 * History:     2022/1/11
 */
public class ImgPreviewFragmentPresenter extends AbsPresenter<ImgPreviewFragmentPresenter.IImgPreviewFragment> {
    public interface IImgPreviewFragment extends IBaseView {
        //显示预览页面
        void showPreview(String absolutePath, boolean isOriginal);

        //展示缩略图
        void showThumbImage(String localPath);

        //显示缓存页面
        void showCacheView();

        //显示视频预览
        void showVideoPreview(String localPath);
    }

    //查询文件是否已下载或缓存
    public void checkFileExist(Context context, String fileName, String fileUuid, String filePath, long fileSize, String md5, String mimeType, boolean isLocal, String from) {

        //文件通过本设备传输且本地对应文件存在
        ArrayList<TransferItem> finishList = TransferDBManager.getInstance(context).queryFinishItemsByUUID(fileUuid);
        if (finishList != null && finishList.size() > 0) {
            for (TransferItem item : finishList) {
                File localFile = new File(item.localPath, item.keyName);
                if (localFile.exists()) {
                    Logger.d("zfy", "has transferred item,open local file");
                    if (mimeType.contains("video")) {
                        iView.showVideoPreview(localFile.getAbsolutePath());
                        return;
                    }
                    iView.showPreview(localFile.getAbsolutePath(), true);
                    return;
                }
            }
        }

        //查看是否已缓存
        String cacheFilePath = FileListUtil.getCacheFilePath(context, fileName);
        if (!TextUtils.isEmpty(cacheFilePath)) {
            if (mimeType.contains("video")) {
                iView.showVideoPreview(cacheFilePath);
                return;
            }
            iView.showPreview(cacheFilePath, true);
            return;
        }

//        if (mimeType.contains("video")) {
//            iView.showVideoPreview(null);
//            return;
//        }

        // 判断是否缓存中
        int cacheTransferType = TransferHelper.TYPE_CACHE;
        String uniqueTag = TransferItemFactory.getUniqueTag(cacheTransferType, null, null, null, fileUuid);
        TransferItem cacheItem = TransferDBManager.getInstance(context).queryByUniqueTag(uniqueTag, cacheTransferType);
        if (cacheItem != null && cacheItem.state == TransferHelper.STATE_DOING) {
            //正在下载
            if (mimeType.contains("image")) {
                //图片显示缩略图及进度条
                iView.showThumbImage(null);
            } else if (mimeType.contains("video")) {
                //视频获取压缩图，显示缩略图
                iView.showVideoPreview(null);
            } else {
                iView.showCacheView();
            }
            return;

        }

        if (mimeType.contains("image") || mimeType.contains("video")) {
            //图片
            //查看压缩图是否已下载
            String compressedImagePath = FileListUtil.getCompressedPath(context, fileUuid);
            if (!TextUtils.isEmpty(compressedImagePath)) {
                if (mimeType.contains("image")) {
                    iView.showPreview(compressedImagePath, false);
                } else {
                    iView.showVideoPreview(compressedImagePath);
                }
            } else {
                if (mimeType.contains("image")) {
                    iView.showThumbImage(null);
                } else {
                    iView.showVideoPreview(null);
                }
                //下载压缩图
                FileListUtil.downloadCompressedImage(context, fileUuid, fileName, from, (result, path) -> {
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (result) {
                                if (mimeType.contains("image")) {
                                    iView.showPreview(path, false);
                                }else {
                                    iView.showVideoPreview(path);
                                }
                            } else if (mimeType.contains("image")){
                                //下载压缩图失败，缓存原图
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
            FileListUtil.downloadFile(context, fileUuid, filePath, fileName, fileSize, md5, true, from, null);
        }
    }

    public void getOriginalImage(Context context, String fileName, String uuidStr, String filePath, long fileSize, String md5, String from) {
        //文件不存在，发起下载
        FileListUtil.downloadFile(context, uuidStr, filePath, fileName, fileSize, md5, true, from, null);
    }
}
