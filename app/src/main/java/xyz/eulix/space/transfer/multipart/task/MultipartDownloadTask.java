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
import xyz.eulix.space.manager.LanManager;
import xyz.eulix.space.manager.TransferTaskManager;
import xyz.eulix.space.transfer.TransferProgressListener;
import xyz.eulix.space.transfer.multipart.MultipartUtil;
import xyz.eulix.space.transfer.multipart.bean.TransferItemLogBean;
import xyz.eulix.space.transfer.multipart.bean.UploadChunkBean;
import xyz.eulix.space.transfer.multipart.lan.LanHttpsUtil;
import xyz.eulix.space.transfer.multipart.network.MultipartNetworkManger;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.FailCodeUtil;
import xyz.eulix.space.util.GatewayUtils;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.NetUtils;

/**
 * Author:      Zhu Fuyu
 * Description: 单文件分片下载任务类
 * History:     2022/3/8
 */
public class MultipartDownloadTask {

    private Context mContext;
    private String mFileUuid;
    private String mTargetDirPath;
    private String mFileName;
    private long mFileSize;

    private String mCacheDir;

    private boolean mHasStarted = false;
    private boolean isPause = false;
    private boolean taskSwitch = true;
    private ExecutorService downExecutor;
    private LanManager lanManager;

    private Thread workThread;

    private long mCurrentDownloadedSize = 0L;

    private int oldPercent = 0; //上次进度
    private int currentPercent;

    private CallbackListener mTaskListener;

    private GatewayCommunicationBase mGatewayCommunicationBase;

    //等待下载item列表（兼容多线程）
    private List<UploadChunkBean> mDownloadPrepareList = new CopyOnWriteArrayList<>();
    //正在下载item列表
    private List<UploadChunkBean> mDownloadDoingList = new CopyOnWriteArrayList<>();
    //下载失败item列表
    private List<UploadChunkBean> mDownloadFailedList = new CopyOnWriteArrayList<>();

    //下载等待阻塞队列
    private BlockingDeque<UploadChunkBean> downloadWaitingQueue = new LinkedBlockingDeque<>();

    //片段下载次数
    private HashMap<String, Integer> itemFailedTimes = new HashMap<>();

    private String mAoId;

    //https校验token
    private String mHttpsVerifyToken;

    private String uniqueTag;

    private AtomicInteger p2pChunk = new AtomicInteger(0);

    private TransferItemLogBean logItem;

    public MultipartDownloadTask(Context context, String fileUuid, String targetDirPath, String fileName, long fileSize, String cacheDir, String uniqueTag) {
        this.mContext = context;
        this.mFileUuid = fileUuid;
        this.mTargetDirPath = targetDirPath;
        this.mFileName = fileName;
        this.mFileSize = fileSize;
        this.mCacheDir = cacheDir;
        lanManager = LanManager.getInstance();
        this.uniqueTag = uniqueTag;
        logItem = TransferTaskManager.getInstance().getLogItem(uniqueTag);
    }

    public void downloadFile(ResultCallback listener) {
        mHasStarted = true;
        if (isPause) {
            return;
        }

        mGatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(mContext);
        if (mGatewayCommunicationBase == null) {
            Logger.d("zfy", "gatewayCommunicationBase is null");
            listener.onResult(false, "gatewayCommunicationBase is null");
            return;
        }

        mAoId = EulixSpaceDBUtil.getClientAoId(mContext);
        mHttpsVerifyToken = LanHttpsUtil.getLanHttpsHeaderToken(mAoId, mGatewayCommunicationBase.getSecretKey());

        logItem.chunksInfoCalStartTime = System.currentTimeMillis();
        mDownloadPrepareList.addAll(MultipartUtil.getUnDownloadChunksInfo(mFileUuid, mFileSize, mCacheDir));
        logItem.chunksInfoCalEndTime = System.currentTimeMillis();
        logItem.chunksCount = mDownloadPrepareList.size();

        //计算未下载文件大小，恢复进度
        long unDownloadSize = 0L;
        for (UploadChunkBean chunkBean : mDownloadPrepareList) {
            unDownloadSize += chunkBean.end - chunkBean.start + 1;
        }

        updateProgress(mFileSize - unDownloadSize, true);

        if (mDownloadPrepareList.isEmpty()) {
            //合并文件
            completeChunks(listener);
        } else {

            logItem.oneChunkSize = mDownloadPrepareList.get(0).end - mDownloadPrepareList.get(0).start;
            logItem.transferStartTime = System.currentTimeMillis();

            //执行分片下载任务
            downloadWaitingQueue.addAll(mDownloadPrepareList);
            taskSwitch = true;
            if (workThread == null) {
                workThread = new Thread(new DownWorkManager(downloadWaitingQueue, listener), "eulix-multi-down");
                workThread.start();
            }
        }

    }

    //下载工作类
    public class DownWorkManager implements Runnable {
        private final BlockingDeque<UploadChunkBean> waitingQueue;
        private ArrayList<UploadChunkBean> currentList = new ArrayList<>();
        private ResultCallback mListener;

        public DownWorkManager(BlockingDeque<UploadChunkBean> items, ResultCallback listener) {
            Logger.d("zfy", "#create DownWorkManager");
            waitingQueue = items;
            this.mListener = listener;
        }

        @Override
        public void run() {
            while (taskSwitch) {
                if (!NetUtils.isNetAvailable(mContext) || (NetUtils.isMobileNetWork(mContext) && !ConstantField.sIAllowTransferWithMobileData)) {
                    Logger.d("zfy", "no network, transfer task waiting");
                    try {
                        Thread.sleep(3000);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Thread.currentThread().interrupt();
                    }
                    continue;
                }
                logItem.concurrentCount = MultipartUtil.LIMIT_COUNT_DOWNLOAD;
                if ((currentList.size() - (Math.max(p2pChunk.get(), 0)) * MultipartUtil.P2P_COMPENSATION) < MultipartUtil.LIMIT_COUNT_DOWNLOAD) {
                    try {
                        UploadChunkBean item = waitingQueue.take();
//                        Logger.d("zfy", "waitingQueue item:" + item.keyName + ";thread:" + Thread.currentThread());
                        mDownloadPrepareList.remove(item);
                        mDownloadDoingList.add(item);
                        currentList.add(item);

                        downWrap(item, new ResultCallback() {
                            @Override
                            public void onResult(boolean result, String extraMsg) {
                                //完成传输，进行下一项
                                mDownloadDoingList.remove(item);

                                if (result) {
                                    //成功
                                    if (mDownloadFailedList.contains(item)) {
                                        mDownloadFailedList.remove(item);
                                    }
                                    if (itemFailedTimes.containsKey(item.start + "")) {
                                        itemFailedTimes.put(item.start + "", 0);
                                    }

                                    //单片下载成功，判断是否都下载完成
                                    if (waitingQueue.isEmpty() && mDownloadDoingList.isEmpty() && mDownloadFailedList.isEmpty()) {
                                        Logger.d("zfy", "all chunk download success,complete");
                                        p2pChunk.getAndSet(0);
                                        logItem.transferEndTime = System.currentTimeMillis();
                                        //全部片段上传完成，调用合并
                                        completeChunks(mListener);
                                        taskSwitch = false;
                                        if (downExecutor != null) {
                                            downExecutor.shutdownNow();
                                            downExecutor = null;
                                        }
                                        workThread = null;
                                    } else {
                                        //下载下一片
                                        currentList.remove(item);
                                        Logger.d("zfy", "download next chunk");
                                    }
                                } else {
                                    if (!mDownloadFailedList.contains(item)) {
                                        mDownloadFailedList.add(item);
                                    }
                                    Logger.d("zfy", "chunk " + item.start + " failed");

//                                    P2PUpDownloadUtil.addError(mFileUuid, extraMsg);

                                    if (isPause) {
                                        return;
                                    }

                                    //判断源文件是否删除
                                    int errorCode = -1;
                                    try {
                                        errorCode = Integer.parseInt(extraMsg);
                                    } catch (Exception e) {
                                        Logger.e(e.getMessage());
                                    }
                                    if (errorCode == FailCodeUtil.ERROR_DOWNLOAD_REMOTE_SOURCE_DELETE) {
                                        taskSwitch = false;
//                                        P2PUpDownloadUtil.finishLoad(mFileUuid, System.currentTimeMillis());
                                        if (downExecutor != null) {
                                            downExecutor.shutdownNow();
                                            downExecutor = null;
                                        }
                                        workThread = null;
                                        mListener.onResult(false, errorCode + "");
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
//                                            P2PUpDownloadUtil.finishLoad(mFileUuid, System.currentTimeMillis());
                                            if (downExecutor != null) {
                                                downExecutor.shutdownNow();
                                                downExecutor = null;
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

    private void downWrap(UploadChunkBean downloadChunkItem, ResultCallback callback) {
        if (downExecutor == null) {
            downExecutor = Executors.newFixedThreadPool(8);
        }
        downExecutor.execute(() -> {
            final long[] itemTransferredSize = {0L};
            mGatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(mContext);
            if (LanManager.getInstance().isHttpsAvailable()) {
                Logger.d("zfy", "download https available");
                //局域网Https通道可用
                OkHttpClient okHttpClient = LanManager.getInstance().getHttpsClient();
                if (okHttpClient == null) {
                    callback.onResult(false, "-1");
                    return;
                }

                mHttpsVerifyToken = LanHttpsUtil.getLanHttpsHeaderToken(mAoId, mGatewayCommunicationBase.getSecretKey());
                MultipartNetworkManger.downloadFromHttps(okHttpClient, LanManager.getInstance().getLanHttpsDomain(), mHttpsVerifyToken, mFileUuid, mCacheDir,
                        downloadChunkItem.start, downloadChunkItem.end, new ResultCallback() {
                            @Override
                            public void onResult(boolean result, String extraMsg) {
                                if (!result) {
                                    //恢复进度
                                    updateProgress(mCurrentDownloadedSize - itemTransferredSize[0], true);
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
            } else {
                MultipartNetworkManger.downloadFile(mGatewayCommunicationBase, mFileUuid, mCacheDir,
                        downloadChunkItem.start, downloadChunkItem.end, new ResultCallback() {
                            @Override
                            public void onResult(boolean result, String extraMsg) {
                                if (!result) {
                                    //恢复进度
                                    updateProgress(mCurrentDownloadedSize - itemTransferredSize[0], true);
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

    public boolean hasStarted() {
        return mHasStarted;
    }

    private synchronized void updateProgress(long addCount, boolean isResumeProgress) {
        if (isPause) {
            return;
        }
        if (isResumeProgress) {
            mCurrentDownloadedSize = addCount;
        } else {
            mCurrentDownloadedSize += addCount;
        }
//        Logger.d("updateProgress", "mCurrentUploadedSize=" + mCurrentUploadedSize + ";mAppendSize=" + addCount);
        currentPercent = (int) (mCurrentDownloadedSize * 100 / mFileSize);
//        Logger.d("updateProgress", "currentPercent = " + currentPercent);
        boolean isPercentChange = false;
        if (currentPercent > oldPercent) {
            isPercentChange = true;
            oldPercent = currentPercent;
        }
        //进度发生变化，回调通知
        if (mTaskListener != null) {
            mTaskListener.onProgress(mCurrentDownloadedSize, mFileSize, addCount, isPercentChange, isResumeProgress);
        }
    }

    private void completeChunks(ResultCallback listener) {
        if (isPause) {
            return;
        }

        logItem.completeStartTime = System.currentTimeMillis();
        boolean result = MultipartUtil.mergeAllDownloadChunks(mFileUuid, mCacheDir, mTargetDirPath, mFileName, mFileSize);
        logItem.completeEndTime = System.currentTimeMillis();
        listener.onResult(result, "");


    }

    //暂停任务
    public void pauseTask() {
        isPause = true;

        taskSwitch = false;
        if (downExecutor != null) {
            downExecutor.shutdownNow();
            downExecutor = null;
        }
        workThread = null;
    }

    public void setCallbackListener(CallbackListener taskListener) {
        this.mTaskListener = taskListener;
    }

    public interface CallbackListener {
        //进度更新
        void onProgress(long currentSize, long totalSize, long appendSize, boolean isPercentChange, boolean isResume);
    }

}
