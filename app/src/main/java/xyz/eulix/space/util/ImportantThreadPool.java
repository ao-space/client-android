package xyz.eulix.space.util;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ImportantThreadPool {
    private ThreadPoolExecutor threadPoolExecutor;
    private static ImportantThreadPool instance;
    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int CORE_POOL_SIZE = Math.max(2, (CPU_COUNT + 1));
    private static final int KEEP_ALIVE_SECONDS = 3;

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "ImportantTask #" + mCount.getAndIncrement());
        }
    };

    private ImportantThreadPool() {
        generateThreadPoolExecutor();
    }

    private void generateThreadPoolExecutor() {
        threadPoolExecutor = new ThreadPoolExecutor(
                CORE_POOL_SIZE, CORE_POOL_SIZE, KEEP_ALIVE_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(), sThreadFactory, new ThreadPoolExecutor.CallerRunsPolicy());
        threadPoolExecutor.allowCoreThreadTimeOut(true);
    }

    public synchronized static ImportantThreadPool getInstance() {
        if (instance == null) {
            instance = new ImportantThreadPool();
        }
        return instance;
    }

    public boolean execute(Runnable runnable) throws RejectedExecutionException {
        boolean result = false;
        if (runnable != null) {
            if (threadPoolExecutor == null || threadPoolExecutor.isShutdown()) {
                generateThreadPoolExecutor();
            }
            result = true;
            threadPoolExecutor.execute(runnable);
        }
        return result;
    }
}
