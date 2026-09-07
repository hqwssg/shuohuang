package com.example.carbon.emission.model.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置类
 *
 * 配置自定义的线程池，用于执行碳排放核算等异步任务。
 *
 * 配置策略：
 * - 首先为操作系统预留 1 核，避免 OS 中断处理、文件系统、网络栈、Tomcat 等与业务线程争抢 CPU
 * - 碳排放核算属于 IO 密集型（DB 查询/写入、数据采集等待），核心线程数设为可用核数的 2 倍
 * - 最大线程数 = 核心线程数：定时任务场景流量可预期，不需要临时线程激增带来的抖动
 * - 队列容量 100：匹配模版数量上限，避免任务堆积导致核算延迟过高
 *
 * 仅在 job Profile 下激活（--spring.profiles.active=job）：
 * asyncTaskExecutor 仅被 AsyncTaskService（job 端）使用。
 */
@Configuration
@EnableAsync
@Profile("job")
public class AsyncThreadPoolConfig {

    /** 为操作系统预留的 CPU 核数 */
    private static final int OS_RESERVED_CORES = 1;

    /** 队列容量：匹配模版数量上限 */
    private static final int QUEUE_CAPACITY = 100;

    /**
     * 创建异步任务线程池
     *
     * @return ThreadPoolTaskExecutor 自定义配置的线程池实例
     */
    @Bean(name = "asyncTaskExecutor")
    public Executor asyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        int cpuCount = Runtime.getRuntime().availableProcessors();

        // 可用核数 = 总核数 - 预留核（至少保留 1 核）
        int availableCores = Math.max(cpuCount - OS_RESERVED_CORES, 1);

        // 核心线程数：IO 密集型，可用核数 × 2（线程等待 IO 时 CPU 可被其他线程利用）
        int corePoolSize = availableCores * 2;
        executor.setCorePoolSize(corePoolSize);

        // 最大线程数 = 核心线程数：稳态场景，不创建临时线程，避免抖动
        executor.setMaxPoolSize(corePoolSize);

        // 队列容量
        executor.setQueueCapacity(QUEUE_CAPACITY);

        // 空闲线程存活时间：60 秒（实际 maxPoolSize = corePoolSize，此参数基本不生效）
        executor.setKeepAliveSeconds(60);

        // 线程名称前缀：便于在日志中区分
        executor.setThreadNamePrefix("my-async-");

        // 拒绝策略：CallerRunsPolicy —— 队列和线程池都满时由调度线程自己执行，保证任务不丢失
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        // 关闭时等待所有任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 关闭等待上限：60 秒
        executor.setAwaitTerminationSeconds(60);

        // 初始化线程池
        executor.initialize();

        return executor;
    }
}
