package com.analys.utils;

import java.util.concurrent.*;

/**
 * @Author gu丶
 * @Date 2023/7/23 9:57 AM
 * @Description
 */
public class ThreadPoolUtils {


    public static Executor getThreadPool() {

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                4, 10, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(30), Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy());

        return threadPoolExecutor;
    }
}
