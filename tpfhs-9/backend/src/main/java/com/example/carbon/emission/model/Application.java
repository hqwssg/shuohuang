package com.example.carbon.emission.model;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.AutoConfigurationExcludeFilter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 碳排放核算系统启动类
 *
 * Spring Boot 应用的入口类，负责启动整个碳排放核算系统。
 *
 * @author system
 * @version 1.0
 */
@SpringBootApplication
// 恢复 @SpringBootApplication 默认的过滤器：TypeExcludeFilter 支持
// @DataJpaTest/@WebMvcTest 等切片测试按需排除非相关 Bean（生产环境无影响），
// AutoConfigurationExcludeFilter 避免自动配置类被重复扫描
@ComponentScan(basePackages = {"com.example.carbon.emission.model"},
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),
                @ComponentScan.Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class)
        })
@EnableScheduling    // 启用定时任务支持
@EnableAsync         // 启用异步任务支持
public class Application {

    /**
     * 应用程序主入口方法
     *
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}