package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.entity.DataSourceSystem;
import com.example.carbon.emission.model.service.DataSourceSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 数据来源系统控制器
 * 提供数据来源系统的RESTful API接口
 */
@RestController
@RequestMapping("/api/data-source-systems")
@CrossOrigin(origins = "*")
public class DataSourceSystemController {
    
    @Autowired
    private DataSourceSystemService dataSourceSystemService;
    
    /**
     * 纯字母正则表达式，用于判断是否使用拼音搜索
     */
    private static final Pattern ALPHABET_PATTERN = Pattern.compile("^[a-zA-Z]+$");
    
    /**
     * 获取所有数据来源系统
     * @return 数据来源系统列表
     */
    @GetMapping
    public ResponseEntity<List<DataSourceSystem>> getAllDataSourceSystems() {
        List<DataSourceSystem> systems = dataSourceSystemService.getAllDataSourceSystems();
        return ResponseEntity.ok(systems);
    }
    
    /**
     * 根据ID获取数据来源系统详情
     * @param id 系统ID
     * @return 系统详情或404
     */
    @GetMapping("/{id}")
    public ResponseEntity<DataSourceSystem> getDataSourceSystemById(@PathVariable Long id) {
        DataSourceSystem system = dataSourceSystemService.getDataSourceSystemById(id);
        if (system == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(system);
    }
    
    /**
     * 搜索数据来源系统
     * 根据关键词自动判断使用拼音搜索或模糊搜索
     * @param keyword 搜索关键词
     * @return 匹配的系统列表
     */
    @GetMapping("/search")
    public ResponseEntity<List<DataSourceSystem>> searchDataSourceSystems(@RequestParam(required = false) String keyword) {
        List<DataSourceSystem> systems;
        
        // 如果是纯字母，使用拼音首字母搜索；否则使用模糊搜索
        if (keyword != null && ALPHABET_PATTERN.matcher(keyword).matches()) {
            systems = dataSourceSystemService.searchByPinyinCode(keyword);
        } else {
            systems = dataSourceSystemService.searchByKeyword(keyword);
        }
        
        return ResponseEntity.ok(systems);
    }
    
    /**
     * 创建新的数据来源系统
     * @param dataSourceSystem 系统信息
     * @param userId 创建人ID
     * @return 创建结果
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createDataSourceSystem(
            @RequestBody DataSourceSystem dataSourceSystem,
            @RequestParam(required = false) Long userId) {
        try {
            DataSourceSystem created = dataSourceSystemService.createDataSourceSystem(dataSourceSystem, userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", created);
            response.put("message", "数据来源系统创建成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 更新数据来源系统
     * @param id 系统ID
     * @param dataSourceSystem 更新的系统信息
     * @param userId 更新人ID
     * @return 更新结果
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateDataSourceSystem(
            @PathVariable Long id,
            @RequestBody DataSourceSystem dataSourceSystem,
            @RequestParam(required = false) Long userId) {
        try {
            DataSourceSystem updated = dataSourceSystemService.updateDataSourceSystem(id, dataSourceSystem, userId);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", updated);
            response.put("message", "数据来源系统更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 删除数据来源系统
     * @param id 系统ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteDataSourceSystem(@PathVariable Long id) {
        try {
            dataSourceSystemService.deleteDataSourceSystem(id);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "数据来源系统删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * 检查系统名称是否已存在
     * @param systemName 系统名称
     * @return 是否存在
     */
    @GetMapping("/exists")
    public ResponseEntity<Map<String, Object>> checkSystemNameExists(@RequestParam String systemName) {
        boolean exists = dataSourceSystemService.isSystemNameExists(systemName);
        Map<String, Object> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(response);
    }
}
