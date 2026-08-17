package com.example.carbon.emission.model.service;

/**
 * 碳排放核算服务接口
 * 
 * 提供碳排放核算相关的计算功能。
 */
public interface EmissionCalculationService {
    
    /**
     * 执行碳排放核算
     * 
     * 根据模板ID执行碳排放核算计算，遍历模板下的所有节点，
     * 采集数据并计算碳排放量。
     * 
     * @param templateId 模板ID
     */
    void executeEmissionCalculation(Long templateId);
}