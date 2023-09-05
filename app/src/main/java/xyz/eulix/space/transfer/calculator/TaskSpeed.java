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

package xyz.eulix.space.transfer.calculator;

import android.text.TextUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import xyz.eulix.space.transfer.event.TransferSpeedEvent;
import xyz.eulix.space.util.EventBusUtil;
import xyz.eulix.space.util.Logger;

/**
 * 传输速度监听
 */
public class TaskSpeed {

    private static TaskSpeed sInstance = null;

    private ConcurrentHashMap<String, Long> mDataLengthCache = new ConcurrentHashMap<String, Long>();
    private ConcurrentHashMap<String, Long> mLastDataLength = new ConcurrentHashMap<String, Long>();
    private HashMap<String, Integer> mLastSpeedMap = null;

    private TaskSpeedListener mListener = null;

    private boolean isRunning = false;

    private ConcurrentHashMap<String, Long> mUploadSpeedTime = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Long> mUploadCurrentSpeed = new ConcurrentHashMap<>();

    //上传速度计算窗口
    private static final long UPLOAD_INTERVAL_TIME = 60 * 1000;

    private TaskSpeed() {
    }

    public static synchronized TaskSpeed getInstance() {
        if (sInstance == null) {
            sInstance = new TaskSpeed();
        }

        return sInstance;
    }

    public synchronized boolean start() {

        return start(1); // 默认1s更新一次
    }

    public boolean start(final int intervalInSecond) {
        if (isRunning) {
            return false;
        }

        isRunning = true;
        new Thread("eulix-speed") {

            public void run() {

                mLastDataLength.putAll(mDataLengthCache);

                do {
                    try {
                        Thread.sleep(intervalInSecond * 1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    Set<String> keySets = mDataLengthCache.keySet();

//                    if (keySets.isEmpty()) {
//                        continue;
//                    }

                    HashMap<String, Integer> speedMap = new HashMap<String, Integer>();
                    if (!keySets.isEmpty()) {
                        Iterator<String> it = keySets.iterator();


                        while (it.hasNext()) {

                            String key = it.next();

                            Long current = mDataLengthCache.get(key);
                            Long last = mLastDataLength.get(key);

                            if (last == null) {
                                last = 0L;
                            }
                            if (current == null) {
                                current = 0L;
                            }

                            int speed = (int) ((current - last) / intervalInSecond);

                            Logger.d("TaskSpeed", "Name: " + key + "; Speed: "
                                    + speed / 1024 + "KB.");

                            speedMap.put(key, speed);

                            if (mListener != null) {
                                mListener.onSpeedChangeSingle(key, speed);
                            }

//                            EventBusUtil.post(new TransferSpeedEvent.TransferSpeed(key, speed));

                            mLastDataLength.put(key, current);

                        }
                    }

                    Set<String> keySetsUpload = mUploadCurrentSpeed.keySet();

//                    if (keySetsUpload.isEmpty()) {
//                        continue;
//                    }
                    if (!keySetsUpload.isEmpty()) {
                        Iterator<String> itUpload = keySetsUpload.iterator();
                        while (itUpload.hasNext()) {

                            String key = itUpload.next();

                            int speed = 0;
                            long speedPerSecond = mUploadCurrentSpeed.get(key);
                            long speedTime = mUploadSpeedTime.get(key);
                            if (speedPerSecond > 0 && System.currentTimeMillis() - speedTime < UPLOAD_INTERVAL_TIME) {
                                speed = (int) speedPerSecond;
                            } else {
                                speed = 0;
                            }

                            Logger.d("TaskSpeed Upload", "Name: " + key + "; Speed: "
                                    + speed / 1024 + "KB.");

                            speedMap.put(key, speed);

                            if (mListener != null) {
                                mListener.onSpeedChangeSingle(key, speed);
                            }

                        }
                    }

                    if (mListener != null) {
                        mListener.onSpeedChangeMap(speedMap);
                    }

                    EventBusUtil.post(new TransferSpeedEvent.TransferSpeedMap(speedMap));

                    mLastSpeedMap = speedMap;
                } while (isRunning);

                mLastDataLength.clear();
            }
        }.start();

        return true;
    }

    public void stop() {
        isRunning = false;
    }

    public int getLastSpeed(String keyName) {
        if (mLastSpeedMap == null) {
            return -1;
        }
        Integer speed = mLastSpeedMap.get(keyName);

        if (speed == null) {
            return -1;
        }

        return speed;
    }

    public void appendDataLength(String keyName, long len) {
        Long cached = mDataLengthCache.get(keyName);
        if (cached != null) {
            cached += len;
        } else {
            cached = Long.valueOf(len);
        }

        mDataLengthCache.put(keyName, cached);
    }

    public void removeTask(String keyName) {
        mDataLengthCache.remove(keyName);
        mLastDataLength.remove(keyName);

        mUploadSpeedTime.remove(keyName);
        mUploadCurrentSpeed.remove(keyName);
    }

    public void reset() {
        mDataLengthCache.clear();
        mLastDataLength.clear();

        mUploadSpeedTime.clear();
        mUploadCurrentSpeed.clear();
    }

    public void updateUploadSpeed(String keyName, long sizePerSecond) {
        boolean isFirstAdd = true;
        if (mUploadCurrentSpeed.containsKey(keyName)) {
            isFirstAdd = false;
        }
        mUploadCurrentSpeed.put(keyName, sizePerSecond);
        mUploadSpeedTime.put(keyName, System.currentTimeMillis());
        if (isFirstAdd) {
            EventBusUtil.post(new TransferSpeedEvent.TransferSpeed(keyName, (int) sizePerSecond));
        }
    }

    public int checkCurrentUploadSpeed(String keyName) {
        int speed = 0;
        if (TextUtils.isEmpty(keyName)) {
            return speed;
        }
        if (mUploadCurrentSpeed == null || !mUploadCurrentSpeed.containsKey(keyName)) {
            return speed;
        }
        try {
            speed = Integer.valueOf(String.valueOf(mUploadCurrentSpeed.get(keyName)));
        } catch (Exception e) {
            Logger.e(e.getMessage());
        }
        return speed;
    }

    public static void release() {
        sInstance = null;
    }

    public boolean registerListener(TaskSpeedListener listener, boolean replace) {
        if (mListener == null || replace) {
            mListener = listener;
            return true;
        }

        return false;
    }

    public boolean unRegisterListener() {
        if (mListener == null) {
            return false;
        }

        mListener = null;
        return true;
    }

    public interface TaskSpeedListener {

        void onSpeedChangeSingle(String keyName, int speed);

        void onSpeedChangeMap(Map<String, Integer> speedMap);
    }
}
