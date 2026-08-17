package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.service.EmissionCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 异步任务服务类
 * 
 * 负责执行异步任务，主要用于碳排放核算的异步执行，避免长时间计算阻塞主线程。
 */
@Service
public class AsyncTaskService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncTaskService.class);

    @Autowired
    private EmissionCalculationService emissionCalculationService;

    /**
     * 异步执行碳排放核算任务
     * 
     * 使用自定义线程池执行碳排放核算，每个任务在独立线程中执行，
     * 支持多个任务并发执行，提高系统吞吐量。
     * 
     * @param templateId 模板ID
     * @param templateName 模板名称
     */
    @Async("asyncTaskExecutor")
    public void executeEmissionCalculationAsync(Long templateId, String templateName) {
        logger.info("启动异步碳排放核算任务，模板ID: {}, 名称: {}, 线程: {}", 
            templateId, templateName, Thread.currentThread().getName());
        
        try {
            // 调用碳排放核算服务执行核算
            emissionCalculationService.executeEmissionCalculation(templateId);
            logger.info("异步碳排放核算任务完成，模板ID: {}, 线程: {}", templateId, Thread.currentThread().getName());
        } catch (Exception e) {
            // 记录异常日志，但不抛出，避免影响其他任务
            logger.error("异步碳排放核算任务失败，模板ID: {}", templateId, e);
        }
    }
}