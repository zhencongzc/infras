package com.cmbc.infras.system.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

@Configuration
public class ExecutorServiceBean {

    @Bean("cachedThreadPool")
    ExecutorService genCachedThreadPool() {
//        return new ThreadPoolExecutor(10
//                , 10
//                , 30L
//                , TimeUnit.SECONDS,
//                new SynchronousQueue<Runnable>());
//        return Executors.newCachedThreadPool();
//        return Executors.newFixedThreadPool(100);
        return new ThreadPoolExecutor(20
                , 100
                , 30L
                , TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
    }


}
