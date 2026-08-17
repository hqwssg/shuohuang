package com.ruoyi.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Keeps the existing module URL prefixes inside a single MVC application.
 */
@Configuration
public class ModulePathPrefixConfig implements WebMvcConfigurer
{
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer)
    {
        configurer.addPathPrefix("/auth", HandlerTypePredicate.forBasePackage("com.ruoyi.auth.controller"));
        configurer.addPathPrefix("/system", HandlerTypePredicate.forBasePackage("com.ruoyi.system.controller"));
        configurer.addPathPrefix("/code", HandlerTypePredicate.forBasePackage("com.ruoyi.gen.controller"));
        configurer.addPathPrefix("/schedule", HandlerTypePredicate.forBasePackage("com.ruoyi.job.controller"));
        configurer.addPathPrefix("/file", HandlerTypePredicate.forBasePackage("com.ruoyi.file.controller"));
    }
}
