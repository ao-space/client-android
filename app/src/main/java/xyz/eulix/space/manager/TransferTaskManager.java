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

package xyz.eulix.space.manager;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.bean.LocalMediaUpItem;
import xyz.eulix.space.event.UploadedFileEvent;
import xyz.eulix.space.interfaces.ResultCallback;
import xyz.eulix.space.interfaces.ResultCallbackObj;
import xyz.eulix.space.network.files.FileListItem;
import xyz.eulix.space.transfer.TransferHelper;
import xyz.eulix.space.transfer.calculator.TaskSpeed;
import xyz.eulix.space.transfer.db.TransferDBManager;
import xyz.eulix.space.transfer.event.TransferringCountEvent;
import xyz.eulix.space.transfer.model.TransferItem;
import xyz.eulix.space.transfer.model.TransferItemFactory;
import xyz.eulix.space.transfer.multipart.BetagCalculator;
import xyz.eulix.space.transfer.multipart.MultipartUtil;
import xyz.eulix.space.transfer.multipart.bean.TransferItemLogBean;
import xyz.eulix.space.transfer.multipart.network.MultipartNetworkManger;
import xyz.eulix.space.transfer.multipart.task.MultipartDownloadTask;
import xyz.eulix.space.transfer.multipart.task.MultipartUploadTask;
import xyz.eulix.space.util.AlbumNotifyHelper;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.FailCodeUtil;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.MD5Util;
import xyz.eulix.space.util.NetUtils;
import xyz.eulix.space.util.ThreadPool;

/**
 * Author:      Zhu Fuyu
 * Description: 文件传输任务管理类
 * //维护上传队列（上传中、等待中）
 * //控制同时上传文件数量
 * //任务恢复
 * History:     2021/12/6
 */
public class TransferTaskManager {

    private static TransferTaskManager sInstance;
    private ExecutorService uploadExecutor;
    private ExecutorService downExecutor;
    //文件并发数量
    private static final int LIMIT_COUNT_DOWNLOAD = 2;
    private static final int LIMIT_COUNT_UPLOAD = 2;

    private Context mContext;
    //等待上传item列表（兼容多线程）
    private List<TransferItem> mUploadPrepareList = new CopyOnWriteArrayList<>();
    //正在上传item列表
    private List<TransferItem> mUploadDoingList = new CopyOnWriteArrayList<>();
    //上传失败item列表
    private List<TransferItem> mUploadFailedList = new CopyOnWriteArrayList<>();

    //上传等待阻塞队列
    private static BlockingDeque<TransferItem> uploadWaitingQueue = new LinkedBlockingDeque<>();
    private List<TransferItem> mUploadCurrentList = new CopyOnWriteArrayList<>();

    private List<CustomUploadRunnable> customUploadRunnableList = new CopyOnWriteArrayList<>();


    //等待下载item列表
    private List<TransferItem> mDownPrepareList = new CopyOnWriteArrayList<>();
    //正在下载item列表
    private List<TransferItem> mDownDoingList = new CopyOnWriteArrayList<>();
    //下载失败item列表
    private List<TransferItem> mDownFailedList = new CopyOnWriteArrayList<>();

    //下载等待阻塞队列
    private static BlockingDeque<TransferItem> downWaitingQueue = new LinkedBlockingDeque<>();
    private List<TransferItem> mDownloadCurrentList = new CopyOnWriteArrayList<>();

    private ConcurrentHashMap<String, MultipartUploadTask> mMultiUploadTasks = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, MultipartDownloadTask> mMultiDownTasks = new ConcurrentHashMap<>();

    //任务开关
    public boolean taskSwitch = true;

    private volatile int mTransferringCount = 0;

    private UploadWorkManager mUploadWorkManager;
    private DownWorkManager mDownWorkManager;

    private String mUploadCacheDirPath;
    private String mDownloadCacheDirPath;

    //日志数据
    private List<TransferItemLogBean> logsList = new CopyOnWriteArrayList<>();

    private TransferTaskManager() {
        //数据初始化
        mContext = EulixSpaceApplication.getContext();
        String externalFileDir = mContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
        mTransferringCount = 0;
        //上传相关
        mUploadPrepareList.clear();
        mUploadDoingList.clear();
        uploadWaitingQueue.clear();
        mUploadCacheDirPath = externalFileDir + "/cache/multipart_up/";
        ArrayList<TransferItem> unFinishList = TransferDBManager.getInstance(EulixSpaceApplication.getContext()).queryUnfinishedTasks(TransferHelper.TYPE_UPLOAD);
        if (unFinishList != null) {
            for (TransferItem item : unFinishList) {
                if (item.state == TransferHelper.STATE_PREPARE) {
                    mUploadPrepareList.add(item);
                } else if (item.state == TransferHelper.STATE_DOING) {
                    mUploadDoingList.add(item);
                } else if (item.state == TransferHelper.STATE_ERROR) {
                    mUploadFailedList.add(item);
                }
                changeTransferringCount(true);
            }
            //传输中数量变化
            EventBusUtil.post(new TransferringCountEvent(getTransferringCount()));
        }
        //优先添加正在上传中的任务，恢复传输
        uploadWaitingQueue.addAll(mUploadDoingList);
        uploadWaitingQueue.addAll(mUploadPrepareList);
        mUploadWorkManager = new UploadWorkManager(uploadWaitingQueue);
        Thread workThread = new Thread(mUploadWorkManager, "eulix-up");
        workThread.start();

        //下载相关
        mDownPrepareList.clear();
        mDownDoingList.clear();
        downWaitingQueue.clear();

        mDownloadCacheDirPath = externalFileDir + "/cache/multipart_down/";
        ArrayList<TransferItem> unFinishDownList = TransferDBManager.getInstance(EulixSpaceApplication.getContext()).queryUnfinishedTasks(TransferHelper.TYPE_DOWNLOAD);
        if (unFinishDownList != null) {
            for (int i = 0; i < unFinishDownList.size(); i++) {
                TransferItem item = unFinishDownList.get(i);
                if (item.state == TransferHelper.STATE_PREPARE) {
                    mDownPrepareList.add(item);
                    Logger.d("zfy", "int mDownPrepareList size=" + mDownPrepareList.size());
                } else if (item.state == TransferHelper.STATE_DOING) {
                    mDownDoingList.add(item);
                } else if (item.state == TransferHelper.STATE_ERROR) {
                    mDownFailedList.add(item);
                }
                changeTransferringCount(true);
            }
            //传输中数量变化
            EventBusUtil.post(new TransferringCountEvent(getTransferringCount()));
        }
        //优先添加正在下载的任务，恢复传输
        downWaitingQueue.addAll(mDownDoingList);
        downWaitingQueue.addAll(mDownPrepareList);
        mDownWorkManager = new DownWorkManager(downWaitingQueue);
        Thread downWorkThread = new Thread(mDownWorkManager, "eulix-down");
        downWorkThread.start();
    }

    public static synchronized TransferTaskManager getInstance() {
        if (sInstance == null) {
            Logger.d("zfy", "create TransferTaskManager");
            sInstance = new TransferTaskManager();
        }
        return sInstance;
    }


    //插入上传任务
    public synchronized void insertUploadTask(String filepath, String filename, String remotePath, boolean insertFirst, String albumId) {
        File file = new File(filepath, filename);
        if (!file.exists()) {
            Logger.d("zfy", "file to upload not exist");
            return;
        }
        final String uniqueTag = TransferItemFactory.getUniqueTagWithAlbumId(TransferHelper.TYPE_UPLOAD, filename, filepath, remotePath, null, albumId);
        for (TransferItem item : mUploadDoingList) {
            if (uniqueTag.equals(item.ext1)) {
                Logger.d("zfy", "upload doing list exist item");
                //复查数据库
                TransferItem transferItem = TransferDBManager.getInstance(mContext).queryByUniqueTag(uniqueTag, TransferHelper.TYPE_UPLOAD);
                if (transferItem != null && transferItem.state == TransferHelper.STATE_DOING) {
                    //数据库存在
                    Logger.d("zfy", "file is uploading, exit");
                    return;
                } else {
                    //数据库不存在，删除当前正在进行列表数据
                    Logger.d("zfy", "db not exist item");
                    mDownDoingList.remove(item);
                    break;
                }
            }
        }

        TransferItem dbItem = TransferDBManager.getInstance(mContext).queryByUniqueTag(uniqueTag, TransferHelper.TYPE_UPLOAD);
        TransferItem item;
        if (dbItem != null) {
            item = dbItem;
            if (dbItem.state == TransferHelper.STATE_FINISH) {
                //重新传输
//                changeTransferringCount(false);
                item.currentSize = 0L;
                item.createTime = System.currentTimeMillis();
            } else {
                changeTransferringCount(false);
            }
            item.state = TransferHelper.STATE_PREPARE;
            item.errorCode = 0;
            item.md5 = dbItem.md5;

            TransferDBManager.getInstance(mContext).updateTransferInfo(uniqueTag, item, true);
        } else {
            Logger.d("zfy", "create upload task");
            String fileMD5 = "";

            item = TransferItemFactory.createUploadPrepareItem(mContext, filename, filepath, remotePath, file.length(), file.lastModified(), null, fileMD5, uniqueTag, false, albumId);
            mUploadPrepareList.add(0, item);
            TransferDBManager.getInstance(mContext).insert(item);
        }


        //传输中数量变化
        changeTransferringCount(true);

        if (insertFirst) {
//            changeTransferringCount(false);
            uploadWaitingQueue.addFirst(item);
        } else {
            uploadWaitingQueue.add(item);
        }

        EventBusUtil.post(new TransferringCountEvent(getTransferringCount()));
    }


    //插入上传任务
    public synchronized void insertUploadTaskList(List<LocalMediaUpItem> list, String remotePath, String albumId) {
        if (list == null || list.isEmpty()) {
            return;
        }
        ArrayList<TransferItem> transferList = new ArrayList<>();

        for (LocalMediaUpItem localItem : list) {
            int index = localItem.getMediaPath().lastIndexOf("/");
            String fileName = localItem.getMediaPath().substring(index + 1);
            String filePath = localItem.getMediaPath().substring(0, index + 1);
            File file = new File(filePath, fileName);
            if (!file.exists()) {
                Logger.d("zfy", "file to upload not exist");
                return;
            }
            final String uniqueTag = TransferItemFactory.getUniqueTagWithAlbumId(TransferHelper.TYPE_UPLOAD, fileName, filePath, remotePath, null, albumId);
            boolean isDoing = false;
            for (TransferItem item : mUploadDoingList) {
                if (uniqueTag.equals(item.ext1)) {
                    Logger.d("zfy", "file is uploading, exit");
                    isDoing = true;
                    break;
                }
            }
            if (isDoing) {
                continue;
            }

//            TransferItem dbItem = TransferDBManager.getInstance(mContext).queryByUniqueTag(uniqueTag, TransferHelper.TYPE_UPLOAD);
            TransferItem dbItem = null;
            for (TransferItem item : mUploadFailedList) {
                if (item.ext1.equals(uniqueTag)) {
                    dbItem = item;
                    break;
                }
            }
            for (TransferItem item : mUploadPrepareList) {
                if (item.ext1.equals(uniqueTag)) {
                    dbItem = item;
                    break;
                }
            }
            TransferItem item;
            if (dbItem != null) {
                if (dbItem.state != TransferHelper.STATE_FINISH) {
//                changeTransferringCount(false);
                }
                dbItem.state = TransferHelper.STATE_PREPARE;
                dbItem.errorCode = 0;
                dbItem.currentSize = 0L;
                item = dbItem;
                item.createTime = System.currentTimeMillis();
                TransferDBManager.getInstance(mContext).updateTransferInfo(uniqueTag, item, true);
            } else {
                Logger.d("zfy", "create upload task");
                String fileMD5 = "";
                item = TransferItemFactory.createUploadPrepareItem(mContext, fileName, filePath, remotePath, file.length(), System.currentTimeMillis(), null, fileMD5, uniqueTag, false, albumId);
                mUploadPrepareList.add(0, item);
                transferList.add(item);
//                TransferDBManager.getInstance(mContext).insert(item);
            }


            //传输中数量变化
            changeTransferringCount(true);


            uploadWaitingQueue.add(item);

            EventBusUtil.post(new TransferringCountEvent(getTransferringCount()));

        }

        TransferDBManager.getInstance(mContext).insertList(transferList);

    }

    //重试下载失败的任务
    public synchronized void retryDownloadFailedItems() {
        Logger.d("zfy", "retryDownloadFailedItems");
        if (mDownFailedList.size() > 0) {
            for (TransferItem item : mDownFailedList) {
                insertDownloadTask(item.uuid, item.remotePath, item.keyName, item.totalSize, item.md5, true, item.ext2);
                mDownFailedList.remove(item);
            }
        }

    }

    //重试下载失败的任务
    public synchronized void retryUploadFailedItems() {
        Logger.d("zfy", "retryUploadFailedItems");

        if (mUploadFailedList.size() > 0) {
            Logger.d("zfy", "mUploadFailedList.size = " + mUploadFailedList.size());
            for (TransferItem item : mUploadFailedList) {
                Logger.d("zfy", "insert uploadTask:" + item.keyName);
                insertUploadTask(item.localPath, item.keyName, item.remotePath, true, item.ext3);
                mUploadFailedList.remove(item);
            }
        }

    }


    //上传工作类
    public class UploadWorkManager implements Runnable {
        private final BlockingDeque<TransferItem> waitingQueue;
//        private ArrayList<TransferItem> mUploadCurrentList = new ArrayList<>();

        public UploadWorkManager(BlockingDeque<TransferItem> items) {
            waitingQueue = items;
        }

        @Override
        public void run() {
            while (taskSwitch) {
                if (mUploadCurrentList.size() < LIMIT_COUNT_UPLOAD) {
                    try {
                        TransferItem item = waitingQueue.take();
                        //获取数据库中最新数据
                        mUploadPrepareList.remove(item);
                        mUploadDoingList.add(item);
                        mUploadCurrentList.add(item);

                        TransferItemLogBean logItem = new TransferItemLogBean();
                        logItem.uuid = item.ext1;
                        logItem.transferType = TransferHelper.TYPE_UPLOAD;
                        logItem.fileName = item.keyName;
                        logItem.fileSize = item.totalSize;
                        logItem.taskStartTime = System.currentTimeMillis();
                        logsList.add(logItem);

                        uploadWrap(item, new ResultCallback() {
                            @Override
                            public void onResult(boolean result, String extraMsg) {
                                //完成传输，进行下一项
                                //优先判断是否有同beta文件在等待
                                for (Map.Entry<String, MultipartUploadTask> entry1 : mMultiUploadTasks.entrySet()) {
                                    if (!entry1.getValue().hasStarted()) {
                                        //恢复任务
                                        for (TransferItem item1 : mUploadCurrentList) {
                                            if (entry1.getKey().equals(item1.ext1)) {
                                                mMultiUploadTasks.remove(entry1.getKey());
                                                uploadWaitingQueue.addFirst(item1);
                                                mUploadDoingList.remove(item1);
                                                mUploadCurrentList.remove(item1);
                                                break;
                                            }
                                        }
                                    }
                                }

                                for (int i = 0; i < customUploadRunnableList.size(); i++) {
                                    if (customUploadRunnableList.get(i).isFinished()) {
                                        customUploadRunnableList.remove(i);
                                        i--;
                                    }
                                }

                                if (!result) {
                                    //上传失败
                                    mUploadFailedList.add(item);
                                } else {
                                    //上传成功
                                    changeTransferringCount(false);
                                }
                                EventBusUtil.post(new TransferringCountEvent(getTransferringCount()));
                                mUploadDoingList.remove(item);
                                mUploadCurrentList.remove(item);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }

        //移除正在传输项
        public void removeItem(String uniqueTag) {
            for (TransferItem item : mUploadCurrentList) {
                if (item.ext1.equals(uniqueTag)) {
                    mUploadCurrentList.remove(item);
                    mUploadDoingList.remove(item);
                    break;
                }
            }
            for (TransferItem item : mUploadDoingList) {
                if (item.ext1.equals(uniqueTag)) {
                    mUploadDoingList.remove(item);
                    break;
                }
            }
        }

        //查询是否正在传输
        public boolean isTransferring(String uniqueTag) {
            boolean result = false;
            for (TransferItem item : mUploadCurrentList) {
                if (item.ext1.equals(uniqueTag)) {
                    result = true;
                    break;
                }
            }
            return result;
        }

        //停止所有任务
        public void stopAll() {
            mUploadCurrentList.clear();
            mUploadDoingList.clear();
        }
    }

    private void uploadWrap(TransferItem transferItem, ResultCallback callback) {
        if (uploadExecutor == null || uploadExecutor.isShutdown()) {
            uploadExecutor = Executors.newFixedThreadPool(8);
        }
        CustomUploadRunnable uploadRunnable = new CustomUploadRunnable(transferItem, callback);
        customUploadRunnableList.add(uploadRunnable);
        uploadExecutor.execute(uploadRunnable);
    }


    private class CustomUploadRunnable implements Runnable {
        private TransferItem transferItem;
        private ResultCallback callback;
        private boolean isPaused;
        private boolean isFinished = false;

        private BetagCalculator betagCalculator;

        public CustomUploadRunnable(TransferItem transferItem, ResultCallback callback) {
            this.transferItem = transferItem;
            this.callback = callback;
        }

        @Override
        public void run() {
            File file = new File(transferItem.localPath, transferItem.keyName);
            if (!file.exists()) {
                Logger.d("zfy", "file to upload not exist");
                callback.onResult(false, FailCodeUtil.ERROR_UPLOAD_LOCAL_SOURCE_DELETE + "");
                return;
            }
            betagCalculator = new BetagCalculator(file.getAbsolutePath());

            transferItem = TransferDBManager.getInstance(mContext).queryByUniqueTag(transferItem.ext1, transferItem.transferType);
            if (transferItem == null) {
                callback.onResult(false, "item has deleted");
                return;
            }

            TransferItem newItem = transferItem;
            newItem.state = TransferHelper.STATE_DOING;
            newItem.errorCode = 0;
            newItem.currentSize = 0L;
            TransferDBManager.getInstance(mContext).updateTransferInfo(transferItem.ext1, newItem, true);

            getLogItem(transferItem.ext1).taskStartTime = System.currentTimeMillis();
            getLogItem(transferItem.ext1).betagStartTime = System.currentTimeMillis();

            String betag;
            String dbMd5 = transferItem.md5;
            if (TextUtils.isEmpty(dbMd5) || dbMd5.length() != 34) {
                //计算betag
//                betag = MultipartUtil.getFileBetag(file.getAbsolutePath());
                betag = betagCalculator.getFileBetag();
                if (TextUtils.isEmpty(betag)) {
                    if (isPaused) {
                        Logger.d("zfy", "customUploadRunnable is paused");
                        isFinished = true;
                    } else {
                        callback.onResult(false, "");
                    }
                    return;
                }
                TransferItem currentItem = TransferDBManager.getInstance(mContext).queryByUniqueTag(transferItem.ext1, transferItem.transferType);
                if (currentItem != null) {
                    TransferItem changeMd5Item = currentItem;
                    changeMd5Item.md5 = betag;
                    TransferDBManager.getInstance(mContext).updateTransferInfo(transferItem.ext1, changeMd5Item, false);
                } else {
                    callback.onResult(false, "item has deleted");
                    return;
                }
            } else {
                betag = dbMd5;
            }

            getLogItem(transferItem.ext1).betagEndTime = System.currentTimeMillis();

            //判断任务是否手动停止
            if (isPaused) {
                Logger.d("zfy", "customUploadRunnable is paused");
                isFinished = true;
                return;
            }

            MultipartUploadTask uploadTask = new MultipartUploadTask(mContext, transferItem.remotePath, file.getAbsolutePath(), betag, mUploadCacheDirPath, transferItem.ext1, false, transferItem.ext3);
            mMultiUploadTasks.put(transferItem.ext1, uploadTask);

            //设置任务回调
            uploadTask.setCallbackListener(new MultipartUploadTask.CallbackListener() {
                @Override
                public void onGetUploadId(String uploadId) {
                    if (!TextUtils.isEmpty(uploadId) && !uploadId.equals(transferItem.ext2)) {
                        //更新uploadId
                        TransferItem tempItem = transferItem;
                        tempItem.ext2 = uploadId;
                        TransferDBManager.getInstance(mContext).updateTransferInfo(transferItem.ext1, tempItem, false);
                    }
                }

                @Override
                public void onProgress(long currentSize, long totalSize, long appendSize, boolean isPercentChange, boolean isResume) {
                    if (isPercentChange) {
//                        Logger.d("zfy", "onProgressResult:currentSize=" + currentSize + ",totalSize=" + totalSize);
                        TransferDBManager.getInstance(mContext).updateTransferSize(transferItem.keyName, TransferHelper.TYPE_UPLOAD, currentSize, totalSize, true, transferItem.ext1);
                    }
                }
            });

            //判断是否有相同内容的文件正在上传
            boolean hasSameContentDoing = false;
            for (Map.Entry<String, MultipartUploadTask> entry1 : mMultiUploadTasks.entrySet()) {
                if (betag.equals(entry1.getValue().getBetag()) && entry1.getValue().hasStarted()) {
                    hasSameContentDoing = true;
                    break;
                }
            }
            if (hasSameContentDoing) {
                Logger.d("zfy", "has same betag task uploading");
                return;
            }

            //发起上传
            uploadTask.uploadFile(new ResultCallbackObj() {
                @Override
                public void onResult(boolean result, Object extraObj) {
                    //启动同betag文件上传
                    mMultiUploadTasks.remove(transferItem.ext1);
                    TaskSpeed.getInstance().removeTask(transferItem.ext1);

                    getLogItem(transferItem.ext1).taskEndTime = System.currentTimeMillis();

                    //更改数据库状态
                    if (result && extraObj != null) {
                        FileListItem fileListItem = (FileListItem) extraObj;
                        String fileUuid = fileListItem.getUuid();
                        Logger.d("zfy", "file upload success:" + transferItem.keyName + ";uuid=" + fileUuid);
                        if (!TextUtils.isEmpty(transferItem.ext3)) {
                            TransferDBManager.getInstance(mContext).updateTransferRemotePath(transferItem.ext1, fileListItem.getPath());
                            //相簿更新路径
                        }
                        EventBusUtil.post(new UploadedFileEvent(fileUuid, transferItem.keyName, transferItem.remotePath));
                        TransferDBManager.getInstance(mContext).updateTransferState(transferItem.keyName, TransferHelper.TYPE_UPLOAD, TransferHelper.STATE_FINISH, 0, fileUuid, true, transferItem.ext1);
                        try {
                            String md5 = MD5Util.getFileMD5String(file);
                            AlreadyUploadedManager.getInstance().insertItem(file.getAbsolutePath(), md5);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        callback.onResult(true, fileListItem.getUuid());
                    } else {
                        int code = -1;
                        try {
                            if (extraObj != null && !TextUtils.isEmpty((CharSequence) extraObj)) {
                                code = Integer.parseInt((String) extraObj);
                            }
                        } catch (Exception e) {
                            Logger.e(e.getMessage());
                        }
                        TransferDBManager.getInstance(mContext).updateTransferState(transferItem.keyName, TransferHelper.TYPE_UPLOAD, TransferHelper.STATE_ERROR, code, null, true, transferItem.ext1);
                        callback.onResult(result, code + "");
                    }
                    isFinished = true;

                }

                @Override
                public void onError(String msg) {
                    callback.onResult(false, msg);
                    isFinished = true;
                }
            });
        }

        public void setPause(boolean isPaused) {
            this.isPaused = isPaused;
            if (betagCalculator != null) {
                betagCalculator.stopCalculate();
            }
        }

        public boolean isFinished() {
            return this.isFinished;
        }
    }

    //插入下载任务
    public synchronized void insertDownloadTask(String fileUuid, String filepath, String filename, long fileSize, String md5, boolean insertFirst, String from) {

        final String uniqueTag = TransferItemFactory.getUniqueTag(TransferHelper.TYPE_DOWNLOAD, null, null, null, fileUuid);
        for (TransferItem item : mDownDoingList) {
            if (uniqueTag.equals(item.ext1)) {
                Logger.d("zfy", "download doing list exist item");
                //复查数据库
                TransferItem transferItem = TransferDBManager.getInstance(mContext).queryByUniqueTag(uniqueTag, TransferHelper.TYPE_DOWNLOAD);
                if (transferItem != null && transferItem.state == TransferHelper.STATE_DOING) {
                    //数据库存在
                    Logger.d("zfy", "file is downloading, exit");
                    return;
                } else {
                    //数据库不存在，删除当前数据
                    Logger.d("zfy", "db not exist item");
                    mDownDoingList.remove(item);
                    break;
                }
            }
        }

        TransferItem dbItem = TransferDBManager.getInstance(mContext).queryByUniqueTag(uniqueTag, TransferHelper.TYPE_DOWNLOAD);
        TransferItem item;
        if (dbItem != null) {
            item = dbItem;
            if (dbItem.state == TransferHelper.STATE_FINISH) {
                //判断文件是否本地还在
                File localFile = new File(dbItem.localPath, dbItem.keyName);
                if (localFile.exists()) {
                    Logger.d("zfy", "local file is exist");
                    long localFileSize = localFile.length();
                    if (fileSize == localFileSize) {
                        Logger.d("zfy", "local file size is match");
                        //修改时间
                        item.createTime = dbItem.createTime;
                        TransferDBManager.getInstance(mContext).updateTransferInfo(uniqueTag, dbItem, true);
                        return;
                    }
                }
                item.currentSize = 0L;
                item.createTime = System.currentTimeMillis();
            } else if (dbItem.state == TransferHelper.STATE_ERROR) {
                item.currentSize = 0L;
                changeTransferringCount(false);
            } else {
                changeTransferringCount(false);
            }
            item.state = TransferHelper.STATE_PREPARE;
            item.errorCode = 0;
            item.totalSize = fileSize;

            TransferDBManager.getInstance(mContext).updateTransferInfo(uniqueTag, item, true);
        } else {
            Logger.d("zfy", "create download task:" + filename);
            String localFilePath = getDownloadLocalFolderPath(filepath);
            //判断是否为本地文件未删除，仅下载记录删除
            item = TransferItemFactory.createDownloadPrepareItem(filename, localFilePath, filepath, null,
                    fileSize, System.currentTimeMillis(), fileUuid, md5, uniqueTag, from);
            File localFile = new File(localFilePath, filename);
            boolean hasLocalFile = false;
            if (localFile.exists()) {
                Logger.d("zfy", "local file is exist");
                long localFileSize = localFile.length();
                if (fileSize == localFileSize) {
                    Logger.d("zfy", "local file size is match");
                    hasLocalFile = true;
                }
            }
            if (hasLocalFile && item != null) {
                Logger.d("zfy", "add finish record");
                item.state = TransferHelper.STATE_FINISH;
                TransferDBManager.getInstance(mContext).insert(item);
                return;
            }
            mDownPrepareList.add(0, item);
            TransferDBManager.getInstance(mContext).insert(item);
        }

        //传输中数量变化
        changeTransferringCount(true);
        EventBusUtil.post(new TransferringCountEvent(getTransferringCount()));

        if (insertFirst) {
            downWaitingQueue.addFirst(item);
        } else {
            downWaitingQueue.add(item);
        }

    }


    //下载工作类
    public class DownWorkManager implements Runnable {
        private final BlockingDeque<TransferItem> waitingQueue;
//        private ArrayList<TransferItem> currentList = new ArrayList<>();

        public DownWorkManager(BlockingDeque<TransferItem> items) {
            Logger.d("zfy", "#create DownWorkManager");
            waitingQueue = items;
        }

        @Override
        public void run() {
            while (taskSwitch) {
                if (!NetUtils.isNetAvailable(mContext)) {
                    Logger.d("zfy", "no network, transfer task waiting");
//                    retryDownloadFailedItems();
                    try {
                        Thread.sleep(3000);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                if (mDownloadCurrentList.size() < LIMIT_COUNT_DOWNLOAD) {
                    try {
                        TransferItem item = waitingQueue.take();
                        Logger.d("zfy", "waitingQueue item:" + item.keyName + ";thread:" + Thread.currentThread());
                        mDownPrepareList.remove(item);
                        mDownDoingList.add(item);
                        mDownloadCurrentList.add(item);

                        TransferItemLogBean logItem = new TransferItemLogBean();
                        logItem.uuid = item.ext1;
                        logItem.fileName = item.keyName;
                        logItem.fileSize = item.totalSize;
                        logItem.transferType = item.transferType;
                        logItem.taskStartTime = System.currentTimeMillis();
                        logsList.add(logItem);

                        downWrap(item, new ResultCallback() {
                            @Override
                            public void onResult(boolean result, String extraMsg) {
                                //完成传输，进行下一项
                                if (!result) {
                                    //失败
                                    mDownFailedList.add(item);
                                } else {
                                    changeTransferringCount(false);
                                }
                                EventBusUtil.post(new TransferringCountEvent(getTransferringCount()));
                                mDownDoingList.remove(item);
                                mDownloadCurrentList.remove(item);
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        //移除正在传输项
        public void removeItem(String uniqueTag) {
            for (TransferItem item : mDownloadCurrentList) {
                if (item.ext1.equals(uniqueTag)) {
                    mDownloadCurrentList.remove(item);
                    mDownDoingList.remove(item);
                    break;
                }
            }
        }

        //查询是否正在传输
        public boolean isTransferring(String uniqueTag) {
            boolean result = false;
            for (TransferItem item : mDownDoingList) {
                if (item.ext1.equals(uniqueTag)) {
                    result = true;
                    break;
                }
            }
            return result;
        }

        //停止所有任务
        public void stopAll() {
            mDownloadCurrentList.clear();
            mDownDoingList.clear();
        }
    }

    private void downWrap(TransferItem transferItem, ResultCallback callback) {
        if (downExecutor == null) {
            downExecutor = Executors.newFixedThreadPool(8);
        }
        downExecutor.execute(() -> {
            //秒传判断
            if (!TextUtils.isEmpty(transferItem.md5)) {
                ArrayList<TransferItem> finishedList = TransferDBManager.getInstance(mContext).queryDownloadFinishItemsByMd5(transferItem.md5);
                if (finishedList != null && !finishedList.isEmpty()) {
                    for (TransferItem item : finishedList) {
                        File finishedFile = new File(item.localPath, item.keyName);
                        if (finishedFile.exists()) {
                            Logger.d("zfy", "has exist same md5 file,copy!");
                            //复制文件
                            try {
                                File targetFile = new File(transferItem.localPath, transferItem.keyName);
                                Files.copy(finishedFile, targetFile);
                                TransferDBManager.getInstance(mContext).updateTransferState(transferItem.keyName, TransferHelper.TYPE_DOWNLOAD, TransferHelper.STATE_FINISH, 0, transferItem.uuid, true, transferItem.ext1);
                                //加入媒体库
                                AlbumNotifyHelper.insertToAlbum(mContext, targetFile);
                                callback.onResult(true, targetFile.getAbsolutePath());
                                return;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            TransferItem newItem = transferItem;
            newItem.state = TransferHelper.STATE_DOING;
            newItem.errorCode = 0;
//            newItem.currentSize = 0L;
            TransferDBManager.getInstance(mContext).updateTransferInfo(transferItem.ext1, newItem, true);

            //判断任务是否手动停止
            if (!mDownWorkManager.isTransferring(transferItem.ext1)) {
                Logger.d("zfy", "task has removed, quite this thread");
                return;
            }

            MultipartDownloadTask downloadTask = new MultipartDownloadTask(mContext, transferItem.uuid, transferItem.localPath,
                    transferItem.keyName, transferItem.totalSize, mDownloadCacheDirPath, transferItem.ext1);
            mMultiDownTasks.put(transferItem.ext1, downloadTask);

            //设置任务回调
            downloadTask.setCallbackListener(new MultipartDownloadTask.CallbackListener() {
                @Override
                public void onProgress(long currentSize, long totalSize, long appendSize, boolean isPercentChange, boolean isResume) {
                    if (isPercentChange) {
//                        Logger.d("zfy", "onProgressResult:currentSize=" + currentSize + ",totalSize=" + totalSize);
                        TransferDBManager.getInstance(mContext).updateTransferSize(transferItem.keyName, transferItem.transferType, currentSize, totalSize, true, transferItem.ext1);
                    }
                    //统计传输速度
                    if (!isResume) {
                        TaskSpeed.getInstance().start();
                        TaskSpeed.getInstance().appendDataLength(transferItem.ext1, appendSize);
                    }
                }
            });

            //发起下载
            downloadTask.downloadFile((result, extraMsg) -> {
                mMultiDownTasks.remove(transferItem.ext1);
                TaskSpeed.getInstance().removeTask(transferItem.ext1);
                getLogItem(transferItem.ext1).taskEndTime = System.currentTimeMillis();
                //更改数据库状态
                if (result) {
                    Logger.d("zfy", "file download success:" + transferItem.keyName + ";uuid=" + transferItem.uuid);
                    TransferDBManager.getInstance(mContext).updateTransferState(transferItem.keyName, TransferHelper.TYPE_DOWNLOAD, TransferHelper.STATE_FINISH, 0, transferItem.uuid, true, transferItem.ext1);
                    //加入媒体库
                    File file = new File(transferItem.localPath, transferItem.keyName);
                    if (file.exists()) {
                        AlbumNotifyHelper.insertToAlbum(mContext, file);
                    }
                } else {
                    int code = -1;
                    try {
                        if (!TextUtils.isEmpty(extraMsg)) {
                            code = Integer.parseInt(extraMsg);
                        }
                    } catch (Exception e) {
                        Logger.e(e.getMessage());
                    }
                    TransferDBManager.getInstance(mContext).updateTransferState(transferItem.keyName, TransferHelper.TYPE_DOWNLOAD, TransferHelper.STATE_ERROR, code, transferItem.uuid, true, transferItem.ext1);
                }
                callback.onResult(result, extraMsg);
            });
        });
    }


    //获取下载文件对应本地文件夹路径
    private String getDownloadLocalFolderPath(String filepath) {
        //下载文件对应本地路径
        String nFilepath = ConstantField.SDCARD_ROOT_PATH + File.separator + ConstantField.APP_PATH;
        if (filepath != null) {
            int nLength = nFilepath.length();
            if (filepath.startsWith("/")) {
                if (nFilepath.endsWith("/")) {
                    nFilepath = nFilepath.substring(0, (nLength - 1));
                }
            } else {
                if (!nFilepath.endsWith("/")) {
                    nFilepath = nFilepath + "/";
                }
            }
            nFilepath = nFilepath + filepath;
        }
        return nFilepath;
    }

    public synchronized void changeTransferringCount(boolean isAdd) {
        if (isAdd) {
            mTransferringCount++;
        } else if (mTransferringCount >= 0) {
            mTransferringCount--;
        }
    }

    //查询数据库，更新正在进行任务数量
    public void refreshDoingCountFromDB() {
        ThreadPool.getInstance().execute(() -> {
            mTransferringCount = TransferDBManager.getInstance(mContext).queryDoingTasksCounts();
            EventBusUtil.post(new TransferringCountEvent(getTransferringCount()));
        }, true);
    }

    public synchronized int getTransferringCount() {
        return mTransferringCount;
    }

    public synchronized int getTransferringCountWithoutFailed() {
        int count = mDownloadCurrentList.size() + mUploadCurrentList.size();
        if (count > 0) {
            return count;
        } else {
            return 0;
        }
    }

    //暂停上传任务
    public synchronized void pauseUpload(TransferItem item) {
        if (item == null) {
            return;
        }
        String uniqueTag = item.ext1;
        MultipartUploadTask uploadTask = mMultiUploadTasks.get(uniqueTag);
        if (uploadTask != null) {
            uploadTask.pauseTask();
            mMultiUploadTasks.remove(uniqueTag);
        }

        if (mUploadWorkManager != null) {
            mUploadWorkManager.removeItem(item.ext1);
        }
        for (Map.Entry<String, MultipartUploadTask> entry1 : mMultiUploadTasks.entrySet()) {
            if (!entry1.getValue().hasStarted()) {
                //启动同betag等待任务
                for (TransferItem item1 : mUploadCurrentList) {
                    if (entry1.getKey().equals(item1.ext1)) {
                        mMultiUploadTasks.remove(entry1.getKey());
                        uploadWaitingQueue.addFirst(item1);
                        mUploadDoingList.remove(item1);
                        mUploadCurrentList.remove(item1);
                        break;
                    }
                }
            }
        }
        for (CustomUploadRunnable customUploadRunnable : customUploadRunnableList) {
            if (customUploadRunnable.transferItem.ext1.equals(item.ext1)) {
                customUploadRunnable.setPause(true);
                customUploadRunnableList.remove(customUploadRunnable);
                break;
            }
        }
        TaskSpeed.getInstance().removeTask(item.ext1);
        TransferDBManager.getInstance(mContext).updateTransferState(item.keyName, TransferHelper.TYPE_UPLOAD, TransferHelper.STATE_PAUSE,
                0, item.uuid, true, item.ext1);
    }

    //停止所有上传任务
    public synchronized void stopAllUploadTasks() {
        uploadWaitingQueue.clear();
        mUploadPrepareList.clear();
        customUploadRunnableList.clear();
        mDownloadCurrentList.clear();

        if (mUploadWorkManager != null) {
            mUploadWorkManager.stopAll();
        }

        for (Map.Entry<String, MultipartUploadTask> entry1 : mMultiUploadTasks.entrySet()) {
            entry1.getValue().pauseTask();
            mMultiUploadTasks.remove(entry1.getKey());
        }

        if (uploadExecutor != null) {
            uploadExecutor.shutdownNow();
            uploadExecutor = null;
        }

        for (CustomUploadRunnable customUploadRunnable : customUploadRunnableList) {
            customUploadRunnable.setPause(true);
        }

        TaskSpeed.getInstance().reset();

        TransferDBManager.getInstance(mContext).changeAllState(TransferHelper.TYPE_UPLOAD, TransferHelper.STATE_DOING, TransferHelper.STATE_PAUSE);
        TransferDBManager.getInstance(mContext).changeAllState(TransferHelper.TYPE_UPLOAD, TransferHelper.STATE_PREPARE, TransferHelper.STATE_PAUSE);
    }

    //恢复上传任务
    public synchronized void resumeUpload(TransferItem item) {
        if (item == null) {
            return;
        }
        insertUploadTask(item.localPath, item.keyName, item.remotePath, true, item.ext3);
    }

    //批量恢复上传
    public synchronized void resumeUploadList(ArrayList<TransferItem> items) {
        TransferDBManager.getInstance(mContext).changeAllState(TransferHelper.TYPE_UPLOAD, TransferHelper.STATE_PAUSE, TransferHelper.STATE_PREPARE);
        uploadWaitingQueue.addAll(items);
    }


    //暂停上传任务
    public synchronized void pauseDownload(TransferItem item) {
        if (item == null) {
            return;
        }
        String uniqueTag = item.ext1;
        MultipartDownloadTask downloadTask = mMultiDownTasks.get(uniqueTag);
        if (downloadTask != null) {
            downloadTask.pauseTask();
            mMultiDownTasks.remove(uniqueTag);
        }

        if (mDownWorkManager != null) {
            mDownWorkManager.removeItem(item.ext1);
        }

        for (TransferItem item1 : mDownDoingList) {
            if (item.ext1.equals(item1.ext1)) {
                mDownDoingList.remove(item1);
                break;
            }
        }

        for (TransferItem item1 : mDownloadCurrentList) {
            if (item.ext1.equals(item1.ext1)) {
                mDownloadCurrentList.remove(item1);
                break;
            }
        }


        TaskSpeed.getInstance().removeTask(item.ext1);
        TransferDBManager.getInstance(mContext).updateTransferState(item.keyName, item.transferType, TransferHelper.STATE_PAUSE,
                0, item.uuid, true, item.ext1);
    }

    //停止所有上传任务
    public synchronized void stopAllDownloadTasks() {
        downWaitingQueue.clear();
        mDownPrepareList.clear();
        mDownDoingList.clear();
        mDownloadCurrentList.clear();

        if (mDownWorkManager != null) {
            mDownWorkManager.stopAll();
        }

        for (Map.Entry<String, MultipartDownloadTask> entry1 : mMultiDownTasks.entrySet()) {
            entry1.getValue().pauseTask();
            mMultiDownTasks.remove(entry1.getKey());
        }

        if (downExecutor != null) {
            downExecutor.shutdownNow();
            downExecutor = null;
        }


        TaskSpeed.getInstance().reset();

        TransferDBManager.getInstance(mContext).changeAllState(TransferHelper.TYPE_DOWNLOAD, TransferHelper.STATE_DOING, TransferHelper.STATE_PAUSE);
        TransferDBManager.getInstance(mContext).changeAllState(TransferHelper.TYPE_DOWNLOAD, TransferHelper.STATE_PREPARE, TransferHelper.STATE_PAUSE);
    }

    //恢复下载任务
    public synchronized void resumeDownload(TransferItem item) {
        if (item == null) {
            return;
        }
        insertDownloadTask(item.uuid, item.remotePath, item.keyName, item.totalSize, item.md5, true, item.ext2);
    }

    //批量恢复下载
    public synchronized void resumeDownloadList(ArrayList<TransferItem> items) {
        TransferDBManager.getInstance(mContext).changeAllState(TransferHelper.TYPE_DOWNLOAD, TransferHelper.STATE_PAUSE, TransferHelper.STATE_PREPARE);
        downWaitingQueue.addAll(items);
    }

    //删除上传任务
    public synchronized void deleteUploadItem(TransferItem item) {
        if (item == null) {
            return;
        }
        //获取uploadId（因uploadId为上传过程中生成，所以需要查询数据库当前信息）
        if (item.state != TransferHelper.STATE_FINISH) {
            if (item.state == TransferHelper.STATE_DOING) {
                String uniqueTag = item.ext1;
                MultipartUploadTask uploadTask = mMultiUploadTasks.get(uniqueTag);
                if (uploadTask != null) {
                    uploadTask.pauseTask();
                    mMultiUploadTasks.remove(uniqueTag);
                }

                if (mUploadWorkManager != null) {
                    mUploadWorkManager.removeItem(item.ext1);
                }
            }

            for (TransferItem item1 : mDownPrepareList) {
                if (item1.ext1.equals(item.ext1)) {
                    mDownPrepareList.remove(item1);
                    break;
                }
            }

            for (TransferItem item1 : mUploadDoingList) {
                if (item1.ext1.equals(item.ext1)) {
                    mUploadDoingList.remove(item1);
                    break;
                }
            }

            TransferItem currentItem = TransferDBManager.getInstance(mContext).queryByUniqueTag(item.ext1, TransferHelper.TYPE_UPLOAD);
            if (currentItem != null) {
                String uploadId = currentItem.ext2;
                if (!TextUtils.isEmpty(uploadId)) {
                    ThreadPool.getInstance().execute(() -> {
                        MultipartNetworkManger.deleteUpload(mContext, uploadId, null);
                    });
                }
                TransferDBManager.getInstance(mContext).deleteByUniqueTag(item.ext1, TransferHelper.TYPE_UPLOAD);
            }
            TaskSpeed.getInstance().removeTask(item.ext1);
        }
    }

    //删除下载任务
    public synchronized void deleteDownloadItem(TransferItem item) {
        if (item == null) {
            return;
        }
        //获取uploadId（因uploadId为上传过程中生成，所以需要查询数据库当前信息）
        if (item.state != TransferHelper.STATE_FINISH) {
            if (item.state == TransferHelper.STATE_DOING) {
                String uniqueTag = item.ext1;
                MultipartDownloadTask downloadTask = mMultiDownTasks.get(uniqueTag);
                if (downloadTask != null) {
                    downloadTask.pauseTask();
                    mMultiDownTasks.remove(uniqueTag);
                }

                if (mDownWorkManager != null) {
                    mDownWorkManager.removeItem(item.ext1);
                }
            }

            for (TransferItem item1 : mDownDoingList) {
                if (item1.ext1.equals(item.ext1)) {
                    mDownDoingList.remove(item1);
                    break;
                }
            }
            for (TransferItem item1 : mDownPrepareList) {
                if (item1.ext1.equals(item.ext1)) {
                    mDownPrepareList.remove(item1);
                    break;
                }
            }
            for (TransferItem item1 : mDownloadCurrentList) {
                if (item1.ext1.equals(item.ext1)) {
                    mDownloadCurrentList.remove(item1);
                    break;
                }
            }
            TransferDBManager.getInstance(mContext).deleteByUniqueTag(item.ext1, TransferHelper.TYPE_DOWNLOAD);
            //删除已下载分片
            MultipartUtil.clearDownloadCacheChunks(mDownloadCacheDirPath, item.uuid);
            TaskSpeed.getInstance().removeTask(item.ext1);
        }
    }

    //重置管理类数据(切换盒子时调用)
    public void resetManagerData() {
        stopAllUploadTasks();
        stopAllDownloadTasks();
        String externalFileDir = mContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
        mTransferringCount = 0;
        //上传相关
        mUploadPrepareList.clear();
        mUploadDoingList.clear();
        uploadWaitingQueue.clear();
        mUploadCacheDirPath = externalFileDir + "/cache/multipart_up/";
        ArrayList<TransferItem> unFinishList = TransferDBManager.getInstance(EulixSpaceApplication.getContext()).queryUnfinishedTasks(TransferHelper.TYPE_UPLOAD);
        if (unFinishList != null) {
            for (TransferItem item : unFinishList) {
                if (item.state == TransferHelper.STATE_PREPARE) {
                    mUploadPrepareList.add(item);
                } else if (item.state == TransferHelper.STATE_DOING) {
                    mUploadDoingList.add(item);
                } else if (item.state == TransferHelper.STATE_ERROR) {
                    mUploadFailedList.add(item);
                }
                changeTransferringCount(true);
            }
            //传输中数量变化
            EventBusUtil.post(new TransferringCountEvent(getTransferringCount()));
        }
        //优先添加正在上传中的任务，恢复传输
        uploadWaitingQueue.addAll(mUploadDoingList);
        uploadWaitingQueue.addAll(mUploadPrepareList);

        //下载相关
        mDownPrepareList.clear();
        mDownDoingList.clear();
        downWaitingQueue.clear();

        mDownloadCacheDirPath = externalFileDir + "/cache/multipart_down/";
        ArrayList<TransferItem> unFinishDownList = TransferDBManager.getInstance(EulixSpaceApplication.getContext()).queryUnfinishedTasks(TransferHelper.TYPE_DOWNLOAD);
        if (unFinishDownList != null) {
            for (int i = 0; i < unFinishDownList.size(); i++) {
                TransferItem item = unFinishDownList.get(i);
                if (item.state == TransferHelper.STATE_PREPARE) {
                    mDownPrepareList.add(item);
                    Logger.d("zfy", "int mDownPrepareList size=" + mDownPrepareList.size());
                } else if (item.state == TransferHelper.STATE_DOING) {
                    mDownDoingList.add(item);
                } else if (item.state == TransferHelper.STATE_ERROR) {
                    mDownFailedList.add(item);
                }
                changeTransferringCount(true);
            }
            //传输中数量变化
            EventBusUtil.post(new TransferringCountEvent(getTransferringCount()));
        }
        //优先添加正在下载的任务，恢复传输
        downWaitingQueue.addAll(mDownDoingList);
        downWaitingQueue.addAll(mDownPrepareList);
    }

    public TransferItemLogBean getLogItem(String uniqueTag) {
        TransferItemLogBean logBean = null;

        for (int i = 0; i < logsList.size(); i++) {
            if (logsList.get(i).uuid.equals(uniqueTag)) {
                logBean = logsList.get(i);
                break;
            }
        }

        if (logBean != null) {
            return logBean;
        } else {
            TransferItemLogBean logBeanNew = new TransferItemLogBean();
            logBeanNew.uuid = uniqueTag;
            logsList.add(logBeanNew);
            return logBeanNew;
        }
    }

    //打印传输日志
    public String printAllTransferLogs() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < logsList.size(); i++) {
            sb.append(logsList.get(i).getLogStr());
            sb.append("\n\n");
        }
        return sb.toString();
    }

}
