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

package xyz.eulix.space.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import java.io.File;
import java.util.List;

import xyz.eulix.space.abs.AbsActivity;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.interfaces.ResultCallback;
import xyz.eulix.space.interfaces.ResultCallbackObj;
import xyz.eulix.space.network.video.VideoNetManager;
import xyz.eulix.space.transfer.TransferHelper;
import xyz.eulix.space.transfer.db.TransferDBManager;
import xyz.eulix.space.transfer.model.TransferItem;
import xyz.eulix.space.ui.ExoPlayerActivity;
import xyz.eulix.space.ui.FilePreviewActivity;

/**
 * Author:      Zhu Fuyu
 * Description: 视频播放工具类
 * History:     2023/1/5
 */
public class VideoPlayUtil {
    public static final String M3U8_ROOT_PATH = "/mediaM3u8";

    public static void play(Activity activity, String fileUuid, String fileName, String filePath, long fileSize, long fileTime, ResultCallback callback) {
        if (!TextUtils.isEmpty(fileUuid)) {
            //根据uuid判断是否存在本地对应视频文件
            String localVideoPath = null;
            List<TransferItem> transferList = TransferDBManager.getInstance(activity).queryFinishItemsByUUID(fileUuid);
            if (transferList != null) {
                for (int i = 0; i < transferList.size(); i++) {
                    if (!TextUtils.isEmpty(transferList.get(i).localPath) && FileUtil.existFile(transferList.get(i).localPath)) {
                        localVideoPath = transferList.get(i).localPath + fileName;
                        break;
                    }
                }
            }
            if (localVideoPath != null && new File(localVideoPath).exists()){
                //播放本地视频
                ExoPlayerActivity.startLocal(activity, localVideoPath, fileName);
            } else {
                //查询是否支持在线播放
                if (activity instanceof AbsActivity) {
                    ((AbsActivity<?, ?>) activity).showLoading("");
                }
                checkOnlineSupport(activity, fileUuid, (result, extraMsg) -> {
                    if (result) {
                        //支持在线播放，下载m3u8文件包
                        downloadM3u8File(activity, fileUuid, new ResultCallbackObj() {
                            @Override
                            public void onResult(boolean result, Object extraObj) {
                                if (result && extraObj != null) {
                                    String m3u8ZipPath = (String) extraObj;
                                    //下载成功，解析文件包
                                    parseM3u8(activity, fileUuid, m3u8ZipPath, (result1, m3u8RootPath) -> {
                                        if (activity instanceof AbsActivity) {
                                            ((AbsActivity<?, ?>) activity).closeLoading();
                                        }
                                        if (result1 && !TextUtils.isEmpty(m3u8RootPath)) {
                                            //解析成功，在线播放
                                            ExoPlayerActivity.startOnline(activity, m3u8RootPath, fileName);
                                        } else {
                                            //解析失败
                                            //调用缓存后播放
                                            jumpCacheAndPlay(activity, fileUuid, fileName, filePath, fileSize, fileTime);
                                        }
                                        if (callback != null) {
                                            callback.onResult(true, null);
                                        }
                                    });
                                } else {
                                    //下载失败
                                    if (activity instanceof AbsActivity) {
                                        ((AbsActivity<?, ?>) activity).closeLoading();
                                    }
                                    //调用缓存后播放
                                    jumpCacheAndPlay(activity, fileUuid, fileName, filePath, fileSize, fileTime);
                                    if (callback != null) {
                                        callback.onResult(false, "m3u8 download fail");
                                    }
                                }
                            }

                            @Override
                            public void onError(String msg) {
                                if (activity instanceof AbsActivity) {
                                    ((AbsActivity<?, ?>) activity).closeLoading();
                                }
                                //调用缓存后播放
                                jumpCacheAndPlay(activity, fileUuid, fileName, filePath, fileSize, fileTime);
                                if (callback != null) {
                                    callback.onResult(true, null);
                                }
                            }
                        });
                    } else {
                        //不支持在线播放
                        if (activity instanceof AbsActivity) {
                            ((AbsActivity<?, ?>) activity).closeLoading();
                        }
                        //调用缓存后播放
                        jumpCacheAndPlay(activity, fileUuid, fileName, filePath, fileSize, fileTime);
                        if (callback != null) {
                            callback.onResult(true, null);
                        }
                    }
                });
            }
        } else {
            if (callback != null) {
                callback.onResult(false, "video uuid is null");
            }
        }
    }

    //检查是否支持在线播放
    private static void checkOnlineSupport(Context context, String uuid, ResultCallback callback) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase == null) {
            callback.onResult(false, "gatewayCommunicationBase is null");
            return;
        }
        ThreadPool.getInstance().execute(() -> {
            VideoNetManager.checkVideoSupport(uuid, gatewayCommunicationBase, new ResultCallbackObj() {
                @Override
                public void onResult(boolean result, Object extraObj) {
                    if (callback != null) {
                        callback.onResult(result, null);
                    }
                }

                @Override
                public void onError(String msg) {
                    if (callback != null) {
                        callback.onResult(false, msg);
                    }
                }
            });
        });
    }

    //下载m3u8文件
    private static void downloadM3u8File(Context context, String uuid, ResultCallbackObj callback) {
        GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(context);
        if (gatewayCommunicationBase == null) {
            callback.onResult(false, "gatewayCommunicationBase is null");
            return;
        }
        ThreadPool.getInstance().execute(() -> {
            VideoNetManager.downloadM3u8(context, uuid, gatewayCommunicationBase, callback);
        });
    }

    //解析m3u8
    private static void parseM3u8(Context context, String uuid, String zipSourcePath, ResultCallback callback) {
        File zipSourceFile = new File(zipSourcePath);
        if (!zipSourceFile.exists()) {
            Logger.d("zfy", "zipSourceFile not exist!" + zipSourcePath);
            if (callback != null) {
                callback.onResult(false, "zip not exist");
            }
            return;
        }

        String m3u8Path = getVideoM3u8RootPath(context, uuid);
        File m3u8RootFile = new File(m3u8Path);
        if (m3u8RootFile.exists()) {
            FileUtil.clearFolder(m3u8RootFile);
        }
        //zip解压
        boolean unZipResult = FileUtil.unZipFolder(zipSourcePath, m3u8Path);
        Logger.d("zfy", "unZipResult:" + unZipResult);
        if (callback != null) {
            callback.onResult(unZipResult, m3u8Path);
        }
    }

    private static String getVideoM3u8RootPath(Context context, String uuid) {
        return context.getExternalCacheDir().getAbsolutePath() + M3U8_ROOT_PATH + "/" + uuid;
    }

    //跳转缓存后播放
    private static void jumpCacheAndPlay(Activity activity, String fileUuid, String fileName, String filePath, long fileSize, long fileTime) {
        Intent intent = new Intent(activity, FilePreviewActivity.class);
        intent.putExtra(FilePreviewActivity.KEY_FILE_NAME, fileName);
        intent.putExtra(FilePreviewActivity.KEY_FILE_PATH, filePath);
        intent.putExtra(FilePreviewActivity.KEY_FILE_UUID, fileUuid);
        intent.putExtra(FilePreviewActivity.KEY_FILE_SIZE, fileSize);
        intent.putExtra(FilePreviewActivity.KEY_FILE_TIME, fileTime);
        intent.putExtra(FilePreviewActivity.KEY_FROM, TransferHelper.FROM_FILE);
        activity.startActivity(intent);
        activity.overridePendingTransition(0, android.R.anim.fade_out);
    }

}
