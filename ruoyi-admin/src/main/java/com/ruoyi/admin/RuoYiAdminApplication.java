package com.ruoyi.admin;

import com.ruoyi.common.security.annotation.EnableCustomConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Single Spring Boot entrypoint.
 */
@EnableCustomConfig
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.ruoyi.admin",
        "com.ruoyi.auth",
        "com.ruoyi.system",
        "com.ruoyi.gen",
        "com.ruoyi.job",
        "com.ruoyi.file" })
public class RuoYiAdminApplication
{
    public static void main(String[] args)
    {
        LocalPortGuard.releasePreviousInstance(args);
        SpringApplication.run(RuoYiAdminApplication.class, args);
        System.out.println("RuoYi Admin started successfully.");
    }
}
