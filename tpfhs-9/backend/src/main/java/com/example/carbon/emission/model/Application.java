package com.example.carbon.emission.model;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
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
@ComponentScan(basePackages = {"com.example.carbon.emission.model"})
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