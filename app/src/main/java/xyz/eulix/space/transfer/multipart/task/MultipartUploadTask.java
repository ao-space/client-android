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

package xyz.eulix.space.transfer.multipart.task;

import android.content.Context;
import android.os.Looper;
import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.OkHttpClient;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.database.EulixSpaceDBUtil;
import xyz.eulix.space.interfaces.ResultCallback;
import xyz.eulix.space.interfaces.ResultCallbackObj;
import xyz.eulix.space.manager.LanManager;
import xyz.eulix.space.manager.TransferTaskManager;
import xyz.eulix.space.network.files.FileListItem;
import xyz.eulix.space.transfer.TransferProgressListener;
import xyz.eulix.space.transfer.calculator.TaskSpeed;
import xyz.eulix.space.transfer.multipart.MultipartUtil;
import xyz.eulix.space.transfer.multipart.bean.TransferItemLogBean;
import xyz.eulix.space.transfer.multipart.bean.UploadChunkBean;
import xyz.eulix.space.transfer.multipart.bean.UploadCreateResponseBody;
import xyz.eulix.space.transfer.multipart.bean.UploadPartBean;
import xyz.eulix.space.transfer.multipart.lan.LanHttpsUtil;
import xyz.eulix.space.transfer.multipart.network.MultipartNetworkManger;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.FailCodeUtil;
import xyz.eulix.space.util.GatewayUtils;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.NetUtils;
import xyz.eulix.space.util.ThreadPool;

/**
 * Author:      Zhu Fuyu
 * Description: 单文件分片上传任务类
 * History:     2022/2/21
 */
public class MultipartUploadTask {
    private Context mContext;
    private String mTargetPath;
    private String mFilePath;
    private String uniqueTag;
    private String mBetag;
    private String mUploadId;

    private GatewayCommunicationBase mGatewayCommunicationBase;

    //已完成传输的片段
    private List<UploadPartBean> mUploadedParts = new CopyOnWriteArrayList<>();

    //等待上传item列表（兼容多线程）
    private List<UploadChunkBean> mUploadPrepareList = new CopyOnWriteArrayList<>();
    //正在上传item列表
    private List<UploadChunkBean> mUploadDoingList = new CopyOnWriteArrayList<>();
    //上传失败item列表
    private List<UploadChunkBean> mUploadFailedList = new CopyOnWriteArrayList<>();

    //上传等待阻塞队列
    private BlockingDeque<UploadChunkBean> uploadWaitingQueue = new LinkedBlockingDeque<>();

    //片段上传次数
    private HashMap<String, Integer> itemFailedTimes = new HashMap<>();

    private AtomicInteger p2pChunk = new AtomicInteger(0);

    private Thread workThread;

    //任务开关
    private boolean taskSwitch = true;

    private ExecutorService uploadExecutor;
    private LanManager lanManager;
    //分片文件缓存路径
    private String mCacheDir;

    private long mFileSize = 0L;
    private long mCurrentUploadedSize = 0L;

    private int oldPercent = 0; //上次进度
    private int currentPercent;

    private boolean isPause = false;

    private CallbackListener mTaskListener;

    private boolean mHasStarted = false;

    //是否为同步
    private boolean mIsSync;

    //相簿id
    private String mAlbumId;

    private String mAoId;
    //https校验token
    private String mHttpsVerifyToken;

    private TransferItemLogBean logItem;

    public MultipartUploadTask(Context context, String targetPath, String filePath, String betag, String cacheDir, String uniqueTag, boolean isSync, String albumId) {
        this.mContext = context;
        this.mTargetPath = targetPath;
        this.mFilePath = filePath;
        this.mBetag = betag;
        this.mCacheDir = cacheDir;
        this.uniqueTag = uniqueTag;
        this.mIsSync = isSync;
        this.mAlbumId = albumId;
        lanManager = LanManager.getInstance();
        logItem = TransferTaskManager.getInstance().getLogItem(uniqueTag);
    }

    /**
     * 发起上传
     *
     * @param listener boolean结果 、 uuid（成功）/错误码
     */
    public void uploadFile(ResultCallbackObj listener) {
        mHasStarted = true;
        if (isPause) {
            return;
        }
        File file = new File(mFilePath);
        if (!file.exists()) {
            listener.onResult(false, String.valueOf(FailCodeUtil.ERROR_UPLOAD_LOCAL_SOURCE_DELETE));
            return;
        }

        mFileSize = file.length();

        mGatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(mContext);
        if (mGatewayCommunicationBase == null) {
            listener.onResult(false, "gatewayCommunicationBase is null");
            return;
        }

        mAoId = EulixSpaceDBUtil.getClientAoId(mContext);
        mHttpsVerifyToken = LanHttpsUtil.getLanHttpsHeaderToken(mAoId, mGatewayCommunicationBase.getSecretKey());

        isPause = false;
        createUpload(listener);
    }

    public boolean hasStarted() {
        return mHasStarted;
    }

    private synchronized void updateProgress(long addCount, boolean isResumeProgress) {
        if (isPause) {
            return;
        }
        if (addCount < 0) {
            return;
        }
        if (isResumeProgress) {
            mCurrentUploadedSize = addCount;
        } else {
            mCurrentUploadedSize += addCount;
        }
//        Logger.d("updateProgress", "mCurrentUploadedSize=" + mCurrentUploadedSize + ";mAppendSize=" + addCount);
        currentPercent = (int) (mCurrentUploadedSize * 100 / mFileSize);
//        Logger.d("updateProgress", "currentPercent = " + currentPercent);
        boolean isPercentChange = false;
        if (currentPercent > oldPercent) {
            isPercentChange = true;
            oldPercent = currentPercent;
        }
        //进度发生变化，回调通知
        if (mTaskListener != null) {
            mTaskListener.onProgress(mCurrentUploadedSize, mFileSize, addCount, isPercentChange, isResumeProgress);
        }
    }

    /**
     * 创建上传任务
     *
     * @param listener
     */
    private void createUpload(ResultCallbackObj listener) {
        if (isPause) {
            return;
        }
        logItem.createTaskStartTime = System.currentTimeMillis();

        MultipartNetworkManger.createUpload(mContext, mGatewayCommunicationBase, mTargetPath, mFilePath, mBetag, mIsSync, mAlbumId, new ResultCallbackObj() {
            @Override
            public void onResult(boolean result, Object extraObj) {
                Logger.d("zfy", "create upload result: " + result);
                if (result && extraObj != null) {
                    if (isPause) {
                        return;
                    }
                    logItem.createTaskEndTime = System.currentTimeMillis();
                    UploadCreateResponseBody createResponseBody = (UploadCreateResponseBody) extraObj;
                    Logger.d("zfy", "create upload success");
                    if (createResponseBody.getResults() != null) {
                        int repType = createResponseBody.getResults().rspType;
                        Logger.d("zfy", "repType = " + repType);
                        if (repType == 0) {
                            //创建任务成功。获取uploadId，进行分片、传输
                            Logger.d("zfy", "create new upload task");
                            UploadCreateResponseBody.Results.SuccInfo succInfo = createResponseBody.getResults().succInfo;
                            if (succInfo != null) {
                                mUploadId = succInfo.uploadId;
                                Logger.d("zfy", "uploadId=" + mUploadId);
                                if (mTaskListener != null) {
                                    mTaskListener.onGetUploadId(mUploadId);
                                }
                                //创建分片并开始上传
                                genChunksAndUpload(listener);
                            } else {
                                listener.onResult(false, "params error");
                            }
                        } else if (repType == 1) {
                            //秒传完成。直接返回上传成功
                            Logger.d("zfy", "秒传成功");
                            FileListItem fileItem = createResponseBody.getResults().completeInfo;
                            if (fileItem != null) {
                                listener.onResult(true, fileItem);
                            } else {
                                Logger.d("zfy", "response data error");
                                listener.onResult(false, "-1");
                            }
                        } else if (repType == 2) {
                            //冲突，任务已存在。获取已上传列表，续传剩余分片
                            Logger.d("zfy", "upload task already exist");
                            UploadCreateResponseBody.Results.ConflictInfo conflictInfo = createResponseBody.getResults().conflictInfo;
                            if (conflictInfo != null) {
                                //获取已完成传输的片段
                                mUploadId = conflictInfo.uploadId;
                                Logger.d("zfy", "uploadId=" + mUploadId);
                                if (mTaskListener != null) {
                                    mTaskListener.onGetUploadId(mUploadId);
                                }
                                ArrayList<UploadPartBean> uploadedParts = conflictInfo.uploadedParts;
                                mUploadedParts.clear();
                                if (uploadedParts != null) {
                                    mUploadedParts.addAll(uploadedParts);
                                }
                                genChunksAndUpload(listener);
                            } else {
                                listener.onResult(false, "params error");
                            }
                        } else {
                            listener.onResult(false, "params error");
                        }
                    } else {
                        Logger.d("zfy", "create upload error");
                        listener.onResult(false, "create upload error");
                    }
                }
            }

            @Override
            public void onError(String msg) {
                Logger.d("zfy", "create upload error " + msg);
                if (!isPause) {
                    listener.onResult(false, msg);
                }
            }
        });
    }


    /**
     * 构建分片并上传
     *
     * @param listener
     */
    private void genChunksAndUpload(ResultCallbackObj listener) {
        //计算当前已上传大小
        long uploadedSize = 0L;
        for (UploadPartBean uploadedPart : mUploadedParts) {
            uploadedSize += uploadedPart.end - uploadedPart.start;
        }
        if (uploadedSize > 0L) {
            updateProgress(uploadedSize, true);
        }

        mUploadPrepareList.clear();
        logItem.chunksInfoCalStartTime = System.currentTimeMillis();
        ArrayList<UploadChunkBean> chunkArray = MultipartUtil.getUnUploadedChunksInfo(mFilePath, mUploadedParts);
        logItem.chunksInfoCalEndTime = System.currentTimeMillis();
        mUploadPrepareList.addAll(chunkArray);

        logItem.chunksCount = chunkArray.size();

        if (mUploadPrepareList.isEmpty()) {
            //没有需要上传的文件，调用合并
            completeChunks(listener);
        } else {
            //执行分片上传任务

//            Boolean isComplete = P2PUpDownloadUtil.isComplete(uniqueTag);
//            if (isComplete == null || isComplete) {
//                P2PUpDownloadUtil.initFile(uniqueTag, mFilePath, true, mFileSize);
//            }
//            P2PUpDownloadUtil.startLoad(uniqueTag, System.currentTimeMillis());

            logItem.oneChunkSize = mUploadPrepareList.get(0).end - mUploadPrepareList.get(0).start;
            logItem.transferStartTime = System.currentTimeMillis();

            uploadWaitingQueue.addAll(mUploadPrepareList);
            taskSwitch = true;
            if (workThread == null) {
                workThread = new Thread(new UploadWorkManager(uploadWaitingQueue, listener), "eulix-multi-up");
                workThread.start();
            }
        }
    }


    //上传工作类
    public class UploadWorkManager implements Runnable {
        private final BlockingDeque<UploadChunkBean> waitingQueue;
        private ArrayList<UploadChunkBean> currentList = new ArrayList<>();
        private ResultCallbackObj mListener;

        public UploadWorkManager(BlockingDeque<UploadChunkBean> items, ResultCallbackObj listener) {
            this.waitingQueue = items;
            this.mListener = listener;
        }

        @Override
        public void run() {
            while (taskSwitch) {
                if (!NetUtils.isNetAvailable(mContext) || (!mIsSync && NetUtils.isMobileNetWork(mContext) && !ConstantField.sIAllowTransferWithMobileData)) {
                    Logger.d("zfy", "no network, transfer task waiting");
                    try {
                        Thread.sleep(3000);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                if ((currentList.size() - (Math.max(p2pChunk.get(), 0)) * MultipartUtil.P2P_COMPENSATION) < MultipartUtil.LIMIT_COUNT_UPLOAD) {
                    try {
                        UploadChunkBean item = waitingQueue.take();
                        mUploadPrepareList.remove(item);
                        mUploadDoingList.add(item);
                        currentList.add(item);
                        long startUpTime = System.currentTimeMillis();
                        Logger.d("zfy", "startUpTime = " + startUpTime + ", p2p chunk: " + p2pChunk.get());
                        uploadWrap(item, new ResultCallback() {
                            @Override
                            public void onResult(boolean result, String extraMsg) {
                                //完成传输，进行下一项
                                mUploadDoingList.remove(item);
                                long endUpTime = System.currentTimeMillis();

                                if (result) {
                                    Logger.d("zfy", "endUpTime = " + endUpTime);
                                    long chunkSpeed = (item.end - item.start) / (endUpTime - startUpTime) * 1000;
                                    Logger.d("zfy", "chunkSpeed=" + chunkSpeed);
                                    TaskSpeed.getInstance().start();
                                    TaskSpeed.getInstance().updateUploadSpeed(uniqueTag, (chunkSpeed * currentList.size()));
                                    if (mUploadFailedList.contains(item)) {
                                        mUploadFailedList.remove(item);
                                    }
                                    if (itemFailedTimes.containsKey(item.start + "")) {
                                        itemFailedTimes.put(item.start + "", 0);
                                    }
                                    //单片上传成功，判断是否都上传完成
                                    if (waitingQueue.isEmpty() && mUploadDoingList.isEmpty() && mUploadFailedList.isEmpty()) {
                                        Logger.d("zfy", "all chunk upload success,complete");
                                        p2pChunk.getAndSet(0);
                                        logItem.transferEndTime = System.currentTimeMillis();
                                        //全部片段上传完成，调用合并
                                        completeChunks(mListener);
                                        taskSwitch = false;
                                        if (uploadExecutor != null) {
                                            uploadExecutor.shutdownNow();
                                            uploadExecutor = null;
                                        }
                                        workThread = null;
                                    } else {
                                        currentList.remove(item);
                                        Logger.d("zfy", "upload next chunk");
                                    }

                                } else {

                                    Logger.d("zfy", "chunk " + item.start + " failed");
//                                    P2PUpDownloadUtil.addError(uniqueTag, extraMsg);

                                    if (!NetUtils.isNetAvailable(mContext)) {
                                        TaskSpeed.getInstance().start();
                                        TaskSpeed.getInstance().updateUploadSpeed(uniqueTag, 0L);
                                    }
                                    //单片上传失败
                                    if (!TextUtils.isEmpty(extraMsg) && extraMsg.equals(String.valueOf(FailCodeUtil.ERROR_UPLOAD_LOCAL_SOURCE_DELETE))) {
                                        //本地文件被删除
                                        Logger.d("zfy", "chunk " + item.start + " delete, return");
                                        mListener.onResult(false, extraMsg);
                                        return;
                                    }
                                    if (!mUploadFailedList.contains(item)) {
                                        mUploadFailedList.add(item);
                                    }


                                    if (isPause) {
                                        return;
                                    }

                                    //单片最多重试次数限制
                                    int thisItemFailedTime = 0;
                                    if (itemFailedTimes.containsKey(item.start + "")) {
                                        thisItemFailedTime = itemFailedTimes.get(item.start + "");
                                    }
                                    if (thisItemFailedTime < MultipartUtil.RETRY_TIME) {
                                        Logger.d("zfy", "chunk " + item.start + " failed time is:" + thisItemFailedTime + ",retry!");
                                        thisItemFailedTime++;
                                        try {
                                            Thread.sleep(2000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                            Thread.currentThread().interrupt();
                                        }
                                        itemFailedTimes.put(item.start + "", thisItemFailedTime);
                                        currentList.remove(item);
                                        waitingQueue.addFirst(item);
                                    } else {
                                        //超过重试次数，停止当前文件传输，返回失败
                                        if (NetUtils.isNetAvailable(mContext)) {
                                            Logger.d("zfy", "item failed too many time,give up " + item.start);
                                            taskSwitch = false;
//                                            P2PUpDownloadUtil.finishLoad(uniqueTag, System.currentTimeMillis());
                                            if (uploadExecutor != null) {
                                                uploadExecutor.shutdownNow();
                                                uploadExecutor = null;
                                            }
                                            workThread = null;

                                            mListener.onResult(false, extraMsg);
                                        } else {
                                            currentList.remove(item);
                                            waitingQueue.addFirst(item);
                                        }
                                    }
                                }
                            }
                        });
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void uploadWrap(UploadChunkBean uploadChunkItem, ResultCallback callback) {
        if (uploadExecutor == null) {
            uploadExecutor = Executors.newFixedThreadPool(8);
        }
        uploadExecutor.execute(() -> {
            if (mUploadId == null) {
                return;
            }
            if (!new File(mFilePath).exists()) {
                callback.onResult(false, FailCodeUtil.ERROR_UPLOAD_LOCAL_SOURCE_DELETE + "");
                return;
            }
            final long[] itemTransferredSize = {0L};

            mGatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(mContext);
            if (LanManager.getInstance().isHttpsAvailable()) {
                //局域网Https通道可用
                OkHttpClient okHttpClient = LanManager.getInstance().getHttpsClient();
                if (okHttpClient == null) {
                    callback.onResult(false, "-1");
                    return;
                }
                UploadChunkBean uploadChunkInfo = MultipartUtil.getPlainChunkInfo(mFilePath, uploadChunkItem.start, uploadChunkItem.end);
                if (uploadChunkInfo == null) {
                    Logger.d("zfy", "create chunk file failed");
                    callback.onResult(false, "-1");
                    return;
                }

                if (mGatewayCommunicationBase == null) {
                    if (callback != null) {
                        callback.onResult(false, "-1");
                    }
                    return;
                }

                mHttpsVerifyToken = LanHttpsUtil.getLanHttpsHeaderToken(mAoId, mGatewayCommunicationBase.getSecretKey());

                MultipartNetworkManger.uploadFromHttps(mContext, LanManager.getInstance().getHttpsClient(), LanManager.getInstance().getLanHttpsDomain(), mHttpsVerifyToken,
                        uploadChunkInfo, mUploadId, new ResultCallback() {
                            @Override
                            public void onResult(boolean result, String extraMsg) {
                                if (mUploadId == null) {
                                    return;
                                }
                                if (!result) {
                                    //恢复进度
                                    updateProgress(mCurrentUploadedSize - itemTransferredSize[0], true);
                                    itemTransferredSize[0] = 0L;
                                }
                                callback.onResult(result, extraMsg);
                            }
                        }, new TransferProgressListener() {
                            @Override
                            public void onProgress(long currentSize, long totalSize, long appendSize, boolean isPercentChange, boolean isResume) {
                                itemTransferredSize[0] += appendSize;
                                updateProgress(appendSize, false);
                            }
                        }, mIsSync);

            } else {
                //局域网Https通道不可用
                UploadChunkBean uploadChunkInfo = MultipartUtil.createEncryptChunkFile(mFilePath, mCacheDir, uploadChunkItem.start, uploadChunkItem.end,
                        mGatewayCommunicationBase.getTransformation(), mGatewayCommunicationBase.getSecretKey(), mGatewayCommunicationBase.getIvParams());
                if (uploadChunkInfo == null) {
                    Logger.d("zfy", "create chunk file failed");
                    callback.onResult(false, "-1");
                    return;
                }

                MultipartNetworkManger.uploadFile(mContext, mGatewayCommunicationBase, mUploadId, uploadChunkInfo, mIsSync, new ResultCallback() {
                    @Override
                    public void onResult(boolean result, String extraMsg) {
                        if (mUploadId == null) {
                            return;
                        }
                        if (!result) {
                            //恢复进度
                            updateProgress(mCurrentUploadedSize - itemTransferredSize[0], true);
                            itemTransferredSize[0] = 0L;
                        }
                        callback.onResult(result, extraMsg);
                    }
                }, new TransferProgressListener() {
                    @Override
                    public void onProgress(long currentSize, long totalSize, long appendSize, boolean isPercentChange, boolean isResume) {
                        itemTransferredSize[0] += appendSize;
                        updateProgress(appendSize, false);
                    }
                });
            }
        });
    }

    //合并分片
    private void completeChunks(ResultCallbackObj listener) {
        Logger.d("zfy", "ready complete chunk, is pause: " + isPause);
        if (isPause) {
            return;
        }

        logItem.completeStartTime = System.currentTimeMillis();

//        P2PUpDownloadUtil.finishLoad(uniqueTag, System.currentTimeMillis());
//        P2PUpDownloadUtil.setComplete(uniqueTag, true);

        ResultCallbackObj callbackObj = new ResultCallbackObj() {
            @Override
            public void onResult(boolean result, Object extraObj) {
                logItem.completeEndTime = System.currentTimeMillis();
                Logger.d("zfy", "complete chunk, is pause: " + isPause + ", result: " + result);
                if (isPause) {
                    return;
                }
                if (result && extraObj != null) {
                    Logger.d("zfy", "complete chunk");
                    mUploadId = null;
                    FileListItem fileListItem = (FileListItem) extraObj;
                    listener.onResult(true, fileListItem);
                } else {
                    Logger.d("zfy", "complete chunk fail");
                    if (!result && Integer.parseInt((String) extraObj) == 1032) {
                        //合并校验失败，删除已上传分片
                        Logger.d("zfy", "complete check error,delete task");
                        MultipartNetworkManger.deleteUpload(mContext, mUploadId, new ResultCallbackObj() {
                            @Override
                            public void onResult(boolean result, Object extraObj) {
                                Logger.d("zfy", "delete upload task:" + result);
                                mUploadId = null;
                            }

                            @Override
                            public void onError(String msg) {

                            }
                        });
                    }
                    listener.onResult(false, "");
                }
            }

            @Override
            public void onError(String msg) {
                Logger.d("zfy", "complete chunk error, is pause: " + isPause + ", msg: " + msg);
                if (!isPause) {
                    listener.onResult(false, msg);
                }
            }
        };
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            ThreadPool.getInstance().execute(() -> {
                MultipartNetworkManger.completeUpload(mContext, mGatewayCommunicationBase, mUploadId, callbackObj);
            });
        } else {
            MultipartNetworkManger.completeUpload(mContext, mGatewayCommunicationBase, mUploadId, callbackObj);
        }
    }


    //暂停任务
    public void pauseTask() {
//        P2PUpDownloadUtil.finishLoad(uniqueTag, System.currentTimeMillis());
        isPause = true;
        Logger.d("zfy", "pause upload task:" + mUploadId + ",path=" + mFilePath);
        if (TextUtils.isEmpty(mUploadId)) {
            return;
        }
        taskSwitch = false;
        if (uploadExecutor != null) {
            uploadExecutor.shutdownNow();
            uploadExecutor = null;
        }
        workThread = null;
    }


    public String getBetag() {
        return mBetag;
    }


    public void setCallbackListener(CallbackListener taskListener) {
        this.mTaskListener = taskListener;
    }

    public interface CallbackListener {
        //获取到updateId
        void onGetUploadId(String uploadId);

        //进度更新
        void onProgress(long currentSize, long totalSize, long appendSize, boolean isPercentChange, boolean isResume);
    }

}
