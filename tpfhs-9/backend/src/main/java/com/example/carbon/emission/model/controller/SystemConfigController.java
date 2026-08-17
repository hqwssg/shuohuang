package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.service.SystemConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 系统配置控制器
 */
@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class SystemConfigController {
    
    @Autowired
    private SystemConfigService configService;
    
    /**
     * 获取所有配置项
     */
    @GetMapping
    public ResponseEntity<Map<String, String>> getAllConfigs() {
        return ResponseEntity.ok(configService.getAllConfigs());
    }
    
    /**
     * 根据配置键获取配置值
     */
    @GetMapping("/{key}")
    public ResponseEntity<String> getConfig(@PathVariable String key) {
        String value = configService.getConfig(key);
        if (value == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(value);
    }
    
    /**
     * 更新配置项
     */
    @PutMapping("/{key}")
    public ResponseEntity<Void> updateConfig(@PathVariable String key, @RequestBody Map<String, Object> request) {
        String value = (String) request.get("value");
        Long userId = request.get("updatedBy") != null ? ((Number) request.get("updatedBy")).longValue() : null;
        configService.updateConfig(key, value, userId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 添加新配置项
     */
    @PostMapping
    public ResponseEntity<Void> addConfig(@RequestBody Map<String, Object> request) {
        String key = (String) request.get("configKey");
        String value = (String) request.get("configValue");
        String description = (String) request.get("description");
        String valueDescription = (String) request.get("valueDescription");
        Long userId = request.get("createdBy") != null ? ((Number) request.get("createdBy")).longValue() : null;
        
        configService.addConfig(key, value, description, valueDescription, userId);
        return ResponseEntity.ok().build();
    }
}