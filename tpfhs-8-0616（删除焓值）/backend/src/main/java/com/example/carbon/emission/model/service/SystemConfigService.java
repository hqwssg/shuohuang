package com.example.carbon.emission.model.service;

import java.util.Map;

/**
 * 系统配置服务接口
 */
public interface SystemConfigService {
    
    /**
     * 获取所有配置项
     * 
     * @return 配置项Map，key为配置键，value为配置值
     */
    Map<String, String> getAllConfigs();
    
    /**
     * 根据配置键获取配置值
     * 
     * @param key 配置键
     * @return 配置值，不存在返回null
     */
    String getConfig(String key);
    
    /**
     * 根据配置键获取配置值，带默认值
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值，不存在返回默认值
     */
    String getConfig(String key, String defaultValue);
    
    /**
     * 获取配置整数值
     * 
     * @param key 配置键
     * @return 配置整数值，不存在或转换失败返回null
     */
    Integer getConfigInt(String key);
    
    /**
     * 获取配置整数值，带默认值
     * 
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置整数值，不存在或转换失败返回默认值
     */
    Integer getConfigInt(String key, Integer defaultValue);
    
    /**
     * 更新配置项
     * 
     * @param key 配置键
     * @param value 配置值
     * @param userId 更新人ID
     */
    void updateConfig(String key, String value, Long userId);
    
    /**
     * 添加新配置项
     * 
     * @param key 配置键
     * @param value 配置值
     * @param description 配置描述
     * @param valueDescription 配置值描述
     * @param userId 创建人ID
     */
    void addConfig(String key, String value, String description, String valueDescription, Long userId);
}