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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author: chenjiawei
 * date: 2021/6/17 11:06
 */
public class ThreadPool {
    // 偏向UI展示实时调用，要求及时响应
    private ThreadPoolExecutor foreThreadPoolExecutor;
    // 偏向后台运行调用，不要求及时响应
    private ThreadPoolExecutor backThreadPoolExecutor;
    private static ThreadPoolExecutor backupExecutor;
    private static ThreadPool instance;
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    // 1核：3；2核：6；4核：12；8核：24；16核：48
    private static final int BACK_CORE_POOL_SIZE = Math.max(2, CPU_COUNT * 2);
    private static final int MAXIMUM_POOL_SIZE = Math.max(BACK_CORE_POOL_SIZE, (CPU_COUNT * 3));
    // 1核：5；2核：5；4核：8；8核：16；16核：32
    private static final int FORE_POOL_SIZE = Math.max(5, CPU_COUNT * 2);
    // 1核：2；2核：3；4核：5；8核：9；16核：17
    private static final int BACKUP_POOL_SIZE = Math.max(1, (CPU_COUNT + 1));
    private static final int KEEP_ALIVE_SECONDS = 3;
    private static final int BLOCKING_KEEP_ALIVE_SECONDS = 30;
    private static final BlockingQueue<Runnable> sPoolWorkQueue =
            new LinkedBlockingQueue<Runnable>(128);
    private static final RejectedExecutionHandler foreRejectPolicy = (r, executor) -> executeBackup(r, true);

    private static final ThreadFactory sForeThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(-1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "EulixTask #" + mCount.getAndDecrement());
        }
    };

    private static final ThreadFactory sBackThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "EulixTask #" + mCount.getAndIncrement());
        }
    };

    private ThreadPool() {
        generateForeThreadPoolExecutor();
        generateBackThreadPoolExecutor();
    }

    private void generateForeThreadPoolExecutor() {
        foreThreadPoolExecutor = new ThreadPoolExecutor(FORE_POOL_SIZE, FORE_POOL_SIZE
                , KEEP_ALIVE_SECONDS, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>()
                , sForeThreadFactory, foreRejectPolicy);
        foreThreadPoolExecutor.allowCoreThreadTimeOut(true);
    }

    private void generateBackThreadPoolExecutor() {
        backThreadPoolExecutor = new ThreadPoolExecutor(BACK_CORE_POOL_SIZE, MAXIMUM_POOL_SIZE
                , BLOCKING_KEEP_ALIVE_SECONDS, TimeUnit.SECONDS, sPoolWorkQueue, sBackThreadFactory
                , new ThreadPoolExecutor.CallerRunsPolicy());
        backThreadPoolExecutor.allowCoreThreadTimeOut(true);
    }

    public synchronized static ThreadPool getInstance() {
        if (instance == null) {
            instance = new ThreadPool();
        }
        return instance;
    }

    private synchronized static boolean executeBackup(Runnable runnable, boolean isFore) {
        boolean result = false;
        if (runnable != null) {
            if (backupExecutor == null || backupExecutor.isShutdown()) {
                LinkedBlockingQueue<Runnable> sBackupExecutorQueue = new LinkedBlockingQueue<Runnable>();
                backupExecutor = new ThreadPoolExecutor(BACKUP_POOL_SIZE, BACKUP_POOL_SIZE, KEEP_ALIVE_SECONDS,
                        TimeUnit.SECONDS, sBackupExecutorQueue, (isFore ? sForeThreadFactory : sBackThreadFactory)
                        , new ThreadPoolExecutor.CallerRunsPolicy());
                backupExecutor.allowCoreThreadTimeOut(true);
            }
            result = true;
            backupExecutor.execute(runnable);
        }
        return result;
    }

    public boolean execute(Runnable runnable) throws RejectedExecutionException {
        return execute(runnable, false);
    }

    public boolean execute(Runnable runnable, boolean isFore) throws RejectedExecutionException {
        boolean result = false;
        if (runnable != null) {
            if (isFore) {
                if (foreThreadPoolExecutor != null && !foreThreadPoolExecutor.isShutdown()) {
                    result = true;
                    foreThreadPoolExecutor.execute(runnable);
                } else if (foreThreadPoolExecutor == null || foreThreadPoolExecutor.isTerminated()) {
                    generateForeThreadPoolExecutor();
                    result = true;
                    foreThreadPoolExecutor.execute(runnable);
                }
                if (!result) {
                    result = executeBackup(runnable, true);
                }
            } else {
                if (backThreadPoolExecutor != null && !backThreadPoolExecutor.isShutdown()) {
                    result = true;
                    backThreadPoolExecutor.execute(runnable);
                } else if (backThreadPoolExecutor == null || backThreadPoolExecutor.isTerminated()) {
                    generateBackThreadPoolExecutor();
                    result = true;
                    backThreadPoolExecutor.execute(runnable);
                }
                if (!result) {
                    result = executeBackup(runnable, false);
                }
            }
        }
        return result;
    }

    public ThreadPoolExecutor getForeThreadPoolExecutor() {
        return foreThreadPoolExecutor;
    }

    public ThreadPoolExecutor getBackThreadPoolExecutor() {
        return backThreadPoolExecutor;
    }
}
