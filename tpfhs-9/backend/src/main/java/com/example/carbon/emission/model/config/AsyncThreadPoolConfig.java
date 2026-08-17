package com.example.carbon.emission.model.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置类
 * 
 * 配置自定义的线程池，用于执行碳排放核算等异步任务。
 * 采用IO密集型配置策略，核心线程数为CPU核数的2倍。
 */
@Configuration
@EnableAsync
public class AsyncThreadPoolConfig {

    /**
     * 创建异步任务线程池
     * 
     * @return ThreadPoolTaskExecutor 自定义配置的线程池实例
     */
    @Bean(name = "asyncTaskExecutor")
    public Executor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 获取CPU核心数，用于计算线程池大小
        int cpuCount = Runtime.getRuntime().availableProcessors();
        
        // 核心线程数：IO密集型任务建议设置为CPU核数的2倍
        // 因为IO操作时线程会等待，多线程可以提高CPU利用率
        int corePoolSize = cpuCount * 2;
        executor.setCorePoolSize(corePoolSize);
        
        // 最大线程数：应对突发流量，设置为核心线程数的2倍
        // 当队列满时，会创建新线程直到达到最大线程数
        int maxPoolSize = corePoolSize * 2;
        executor.setMaxPoolSize(maxPoolSize);
        
        // 队列容量：设置为200
        // 任务先进入队列等待，队列满后才会创建新线程
        executor.setQueueCapacity(200);
        
        // 空闲线程存活时间：60秒
        // 超过核心线程数的空闲线程在60秒后会被回收
        executor.setKeepAliveSeconds(60);
        
        // 线程名称前缀："my-async-"
        // 便于在日志中区分不同线程池的线程
        executor.setThreadNamePrefix("my-async-");
        
        // 拒绝策略：CallerRunsPolicy
        // 当队列和线程池都满时，由调用线程执行该任务
        // 保证任务不会丢失，适用于重要任务场景
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 关闭时等待所有任务完成
        // 当应用关闭时，不会立即停止线程池，而是等待所有任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间：60秒
        // 最多等待60秒，如果还有任务未完成则强制关闭
        executor.setAwaitTerminationSeconds(60);
        
        // 初始化线程池
        executor.initialize();
        
        return executor;
    }
}