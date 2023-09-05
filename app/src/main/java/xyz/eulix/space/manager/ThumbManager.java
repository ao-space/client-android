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

import android.text.TextUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import xyz.eulix.space.EulixSpaceApplication;
import xyz.eulix.space.bean.GatewayCommunicationBase;
import xyz.eulix.space.event.ThumbEvent;
import xyz.eulix.space.network.files.FileListManager;
import xyz.eulix.space.network.files.ThumbCacheCallback;
import xyz.eulix.space.transfer.TransferHelper;
import xyz.eulix.space.util.ConstantField;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.GatewayUtils;
import xyz.eulix.space.util.Logger;
import xyz.eulix.space.util.Urls;

/**
 * Author:      Zhu Fuyu
 * Description: 缩略图加载管理类
 * History:     2021/11/8
 */
public class ThumbManager {
    private static ThumbManager sInstance;
    private ExecutorService executor;
    private boolean isCaching = false;
    private HashMap<String, Integer> itemFailedTimes = new HashMap<>();

    //请求来源（文件/相册）
    private HashMap<String, String> fromMap = new HashMap<>();
    //缩略图本地路径
    private ConcurrentHashMap<String, String> localPathMap = new ConcurrentHashMap<>();
    //压缩图本地路径
    private ConcurrentHashMap<String, String> localCompressPathMap = new ConcurrentHashMap<>();

    //并发数量
    private static final int LIMIT_COUNT = 4;

    //失败重试次数
    private static final int RETRY_TIME = 1;

    //等待加载的队列
    private List<String> queueList = new ArrayList<>();

    private List<String> currentList = new ArrayList<>();

    //批量下载中的缩略图uuid列表
    private List<String> downloadingZipList = new ArrayList<>();

    //队列最大限制
    private static final int MAX_QUEUE_LIMIT = 80;

    public static synchronized ThumbManager getInstance() {
        if (sInstance == null) {
            sInstance = new ThumbManager();
            sInstance.initLocalPathMap();
            sInstance.initLocalCompressPathMap();
        }
        return sInstance;
    }

    //插入一项待加载对象
    public synchronized void insertItem(String uuid, String from) {
        if (currentList.contains(uuid)) {
            return;
        }
        if (downloadingZipList.contains(uuid)) {
            Logger.d("zfy", "current thumb is downloading by zip " + uuid);
            return;
        }
        if (queueList.contains(uuid)) {
            queueList.remove(uuid);
        }
        queueList.add(0, uuid);
        fromMap.put(uuid, from);
        if (queueList.size() > MAX_QUEUE_LIMIT) {
            //移除最后一项
            fromMap.remove(queueList.get(queueList.size() - 1));
            queueList.remove(queueList.size() - 1);
        }
    }

    public void start() {
        if (isCaching) {
            Logger.d("zfy", "thumbs is caching");
        } else {
            isCaching = true;
            startCore();
        }
    }

    //开始缓存
    private void startCore() {
        if (executor == null) {
            executor = Executors.newFixedThreadPool(8);
        }
        //限制数量
        if (queueList.size() > 0) {
            Logger.d("zfy", "0 thumb currentSize=" + currentList.size() + ",queueSize=" + queueList.size());
            int realLimitCount = Math.min(LIMIT_COUNT, queueList.size());
            while (currentList.size() < realLimitCount) {
                String tempUuidStr = "";
                for (int i = 0; i < queueList.size(); i++) {
                    if (!currentList.contains(queueList.get(i))) {
                        tempUuidStr = queueList.get(i);
                        currentList.add(tempUuidStr);
                        break;
                    }
                }
                if (TextUtils.isEmpty(tempUuidStr)) {
                    break;
                }
                Logger.d("zfy", "1 thumb currentSize=" + currentList.size() + ",queueSize=" + queueList.size());
                final String uuidStr = tempUuidStr;
                executor.execute(() -> {
                    if (localPathMap.contains(uuidStr)) {
                        //已缓存
                        currentList.remove(uuidStr);
                        queueList.remove(uuidStr);
                        fromMap.remove(uuidStr);
                        //发送缓存成功通知
                        EventBusUtil.post(new ThumbEvent(uuidStr, localPathMap.get(uuidStr)));
                        //继续缓存下一个
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                            Thread.currentThread().interrupt();
                        }
                        startCore();
                        return;
                    }

                    //发起缓存
                    Logger.d("zfy", "thumb cache start!current uuid:" + uuidStr);
                    GatewayCommunicationBase gatewayCommunicationBase = GatewayUtils.generateGatewayCommunication(EulixSpaceApplication.getContext());
                    if (gatewayCommunicationBase != null) {
                        String from = fromMap.get(uuidStr);
                        if (TextUtils.isEmpty(from)) {
                            from = TransferHelper.FROM_FILE;
                        }
                        FileListManager.downloadThumb(UUID.fromString(uuidStr),
                                Urls.getBaseUrl(), gatewayCommunicationBase.getAccessToken(),
                                gatewayCommunicationBase.getSecretKey(), gatewayCommunicationBase.getIvParams(),
                                ConstantField.BoxVersionName.VERSION_0_1_0, EulixSpaceApplication.getContext(), from, new ThumbCacheCallback() {
                                    @Override
                                    public void onResult(String uuid, String absolutePath) {
                                        Logger.d("zfy", "thumb cache success!uuid=" + uuid + ",path=" + absolutePath);
                                        currentList.remove(uuid);
                                        queueList.remove(uuid);
                                        fromMap.remove(uuid);
                                        //发送缓存成功通知
                                        EventBusUtil.post(new ThumbEvent(uuid, absolutePath));
                                        //继续缓存下一个
                                        try {
                                            Thread.sleep(300);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                            Thread.currentThread().interrupt();
                                        }
                                        startCore();
                                    }

                                    @Override
                                    public void onError(String msg) {
                                        Logger.d("zfy", "thumb cache failed:" + msg);
                                        //单项最多重试次数限制
                                        int thisItemFailedTime = 0;
                                        if (itemFailedTimes.containsKey(uuidStr)) {
                                            thisItemFailedTime = itemFailedTimes.get(uuidStr);
                                        }
                                        if (thisItemFailedTime < RETRY_TIME) {
                                            Logger.d("zfy", "item " + uuidStr + " failed time is:" + thisItemFailedTime + ",retry!");
                                            currentList.remove(uuidStr);
                                            thisItemFailedTime++;
                                            itemFailedTimes.put(uuidStr, thisItemFailedTime);
                                        } else {
                                            //超过重试次数
                                            Logger.d("zfy", "item failed too many time,give up " + uuidStr);
                                            if (currentList.contains(uuidStr)) {
                                                currentList.remove(uuidStr);
                                                queueList.remove(uuidStr);
                                            }
                                        }
                                        try {
                                            Thread.sleep(300);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                            Thread.currentThread().interrupt();
                                        }
                                        startCore();
                                    }
                                });

                    }
                });
            }

        } else if (isCaching) {
            isCaching = false;
            Logger.d("zfy", "thumb cache stop! currentSize=" + currentList.size() + ",queueSize=" + queueList.size());
        }
    }

    //取消缓存
    public void cancelCache() {
        Logger.d("zfy", "thumb cache cancel");
        if (executor != null) {
            executor.shutdown();
            executor = null;
            queueList.clear();
            currentList.clear();
            itemFailedTimes.clear();
            isCaching = false;
        }
    }

    //新增正在批量下载的缩略图uuid列表
    public void addDoingZipList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        downloadingZipList.addAll(list);
    }

    //移除正在批量下载的缩略图uuid列表
    public void removeDoingZipList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        downloadingZipList.removeAll(list);
    }

    //初始化本地缩略图列表
    private void initLocalPathMap() {
        String fileFolderPath = EulixSpaceApplication.getContext().getExternalCacheDir().getAbsolutePath() + ConstantField.FILE_THUMBS_CACHE_PATH;
        String albumFolderPath = EulixSpaceApplication.getContext().getExternalCacheDir().getAbsolutePath() + ConstantField.ALBUM_THUMBS_CACHE_PATH;
        genLocalPathCore(fileFolderPath, localPathMap);
        genLocalPathCore(albumFolderPath, localPathMap);
    }

    //初始化本地压缩图列表
    private void initLocalCompressPathMap() {
        String fileFolderPath = EulixSpaceApplication.getContext().getExternalCacheDir().getAbsolutePath() + ConstantField.FILE_COMPRESSED_CACHE_PATH;
        String albumFolderPath = EulixSpaceApplication.getContext().getExternalCacheDir().getAbsolutePath() + ConstantField.ALBUM_COMPRESSED_CACHE_PATH;
        genLocalPathCore(fileFolderPath, localCompressPathMap);
        genLocalPathCore(albumFolderPath, localCompressPathMap);
    }

    private void genLocalPathCore(String fileFolderPath, ConcurrentHashMap<String, String> localPathMap) {
        File fileFolder = new File(fileFolderPath);
        if (fileFolder.exists()) {
            File[] files = fileFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    String fileName;
                    if (file.getName().contains(".")) {
                        fileName = file.getName().substring(0, file.getName().lastIndexOf("."));
                    } else {
                        fileName = file.getName();
                    }
                    localPathMap.put(fileName, file.getAbsolutePath());
                }
            }
        }
    }

    //获取缩略图本地路径
    public String getLocalThumbPath(String uuid) {
        if (TextUtils.isEmpty(uuid)) {
            return null;
        }

        if (localPathMap.isEmpty()) {
            initLocalPathMap();
        }
        return localPathMap.get(uuid);
    }

    //插入本地缩略图
    public void insertLocalThumbPath(String uuid, String absolutePath) {
        localPathMap.put(uuid, absolutePath);
    }

    //重置本地缩略图
    public synchronized void resetLocalThumbPathMap() {
        localPathMap.clear();
        initLocalPathMap();
    }

    //获取压缩图本地路径
    public String getLocalCompressPath(String uuid) {
        if (TextUtils.isEmpty(uuid)) {
            return null;
        }

        if (localCompressPathMap.isEmpty()) {
            initLocalCompressPathMap();
        }
        return localCompressPathMap.get(uuid);
    }

    //插入本地压缩图
    public void insertLocalCompressPath(String uuid, String absolutePath) {
        localCompressPathMap.put(uuid, absolutePath);
    }

    //重置本地压缩图
    public synchronized void resetLocalCompressPathMap() {
        localCompressPathMap.clear();
        initLocalCompressPathMap();
    }

}
