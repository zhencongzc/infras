package com.cmbc.infras.health.util;

import java.util.concurrent.*;

/**
 * Description:符合阿里巴巴规范的线程池
 * Author: zhencong
 * Date: 2021-12-15
 */
public class ThreadPoolUtil {

    public static ThreadPoolExecutor threadPool;

    /**
     * 无返回值直接执行
     *
     * @param runnable
     */
    public static void execute(Runnable runnable) {
        getThreadPool().execute(runnable);
    }

    /**
     * 返回值直接执行
     *
     * @param callable
     */
    public static <T> Future<T> submit(Callable<T> callable) {
        return getThreadPool().submit(callable);
    }

    /**
     * 关闭线程池
     */
    public static void shutdown() {
        getThreadPool().shutdown();
    }

    /**
     * 获取线程池
     *
     * @return 线程池对象
     */
    public static ThreadPoolExecutor getThreadPool() {
        if (threadPool != null) {
            return threadPool;
        } else {
            synchronized (ThreadPoolUtil.class) {
                if (threadPool == null) {
                    threadPool = new ThreadPoolExecutor(50, 100, 60, TimeUnit.SECONDS,
                            new LinkedBlockingQueue<>(32), new ThreadPoolExecutor.CallerRunsPolicy());
                }
                return threadPool;
            }
        }
    }
}
