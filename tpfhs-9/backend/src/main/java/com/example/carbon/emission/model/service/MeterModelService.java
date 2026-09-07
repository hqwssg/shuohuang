package com.example.carbon.emission.model.service;

import com.example.carbon.emission.model.entity.MeterModel;

import java.util.List;

/**
 * 电表型号服务接口
 */
public interface MeterModelService {
    
    /**
     * 获取所有电表型号
     */
    List<MeterModel> findAll();
    
    /**
     * 根据ID获取电表型号
     */
    MeterModel findById(Long id);
    
    /**
     * 根据电表型号名称模糊查询
     */
    List<MeterModel> findByModelNameContaining(String modelName);
    
    /**
     * 保存电表型号
     */
    MeterModel save(MeterModel meterModel);
    
    /**
     * 根据ID删除电表型号
     */
    void deleteById(Long id);
    
    /**
     * 更新电表型号
     */
    MeterModel update(Long id, MeterModel meterModel, Long userId);
}
