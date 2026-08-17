package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.entity.SystemConfig;
import com.example.carbon.emission.model.repository.SystemConfigRepository;
import com.example.carbon.emission.model.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统配置服务实现类
 */
@Service
public class SystemConfigServiceImpl implements SystemConfigService {
    
    @Autowired
    private SystemConfigRepository configRepository;
    
    @Override
    public Map<String, String> getAllConfigs() {
        Map<String, String> configMap = new HashMap<>();
        configRepository.findAll().forEach(config -> {
            configMap.put(config.getConfigKey(), config.getConfigValue());
        });
        return configMap;
    }
    
    @Override
    public String getConfig(String key) {
        return configRepository.findByConfigKey(key)
            .map(SystemConfig::getConfigValue)
            .orElse(null);
    }
    
    @Override
    public String getConfig(String key, String defaultValue) {
        String value = getConfig(key);
        return value != null ? value : defaultValue;
    }
    
    @Override
    public Integer getConfigInt(String key) {
        String value = getConfig(key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    @Override
    public Integer getConfigInt(String key, Integer defaultValue) {
        Integer value = getConfigInt(key);
        return value != null ? value : defaultValue;
    }
    
    @Override
    @Transactional
    public void updateConfig(String key, String value, Long userId) {
        SystemConfig config = configRepository.findByConfigKey(key)
            .orElseThrow(() -> new RuntimeException("配置项不存在: " + key));
        
        config.setConfigValue(value);
        config.setUpdateBy(userId != null ? userId.toString() : null);
        configRepository.save(config);
    }
    
    @Override
    @Transactional
    public void addConfig(String key, String value, String description, String valueDescription, Long userId) {
        if (configRepository.existsByConfigKey(key)) {
            throw new RuntimeException("配置项已存在: " + key);
        }
        
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(value);
        config.setConfigName(description);
        config.setRemark(valueDescription);
        config.setCreateBy(userId != null ? userId.toString() : null);
        config.setUpdateBy(userId != null ? userId.toString() : null);
        configRepository.save(config);
    }
}