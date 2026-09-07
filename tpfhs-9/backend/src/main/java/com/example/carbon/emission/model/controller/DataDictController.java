package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.entity.DataDict;
import com.example.carbon.emission.model.entity.DataDictItem;
import com.example.carbon.emission.model.repository.DataDictItemRepository;
import com.example.carbon.emission.model.repository.DataDictRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 数据字典控制器
 * 提供数据字典分类和字典项的完整CRUD接口
 * 
 * 接口前缀：/api/data-dict
 * 功能包括：
 * - 字典分类管理：查询所有字典、查询单个字典、新增、更新、删除字典
 * - 字典项管理：查询字典项、新增、更新、删除字典项、批量操作
 */
@RestController
@RequestMapping("/api/data-dict")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class DataDictController {
    
    @Autowired
    private DataDictRepository dataDictRepository;
    
    @Autowired
    private DataDictItemRepository dataDictItemRepository;
    
    // ==================== 字典分类管理接口 ====================
    
    /**
     * 获取所有字典分类列表
     * 
     * @return 所有字典分类列表，按 sort_order 升序排列
     */
    @GetMapping
    public ResponseEntity<List<DataDict>> getAllDicts() {
        List<DataDict> dicts = dataDictRepository.findAllByOrderBySortOrderAsc();
        return ResponseEntity.ok(dicts);
    }
    
    /**
     * 根据ID获取单个字典分类
     * 
     * @param id 字典ID
     * @return 字典分类对象；不存在时返回 404
     */
    @GetMapping("/{id}")
    public ResponseEntity<DataDict> getDictById(@PathVariable Long id) {
        return dataDictRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 新增字典分类
     * 
     * @param dict 字典分类对象
     * @return 新增后的字典分类对象（含生成的主键ID）
     */
    @PostMapping
    public ResponseEntity<?> createDict(@RequestBody DataDict dict) {
        // 检查字典编码是否已存在
        if (dataDictRepository.existsByDictCode(dict.getDictCode())) {
            return ResponseEntity.badRequest().body(Map.of("error", "字典编码已存在: " + dict.getDictCode()));
        }
        DataDict saved = dataDictRepository.save(dict);
        return ResponseEntity.ok(saved);
    }
    
    /**
     * 更新字典分类
     * 
     * @param id   字典ID
     * @param dict 包含新字段值的字典对象
     * @return 更新后的字典对象
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDict(@PathVariable Long id, @RequestBody DataDict dict) {
        Optional<DataDict> existing = dataDictRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        DataDict toUpdate = existing.get();
        // 如果修改了字典编码，检查新编码是否已被使用
        if (!toUpdate.getDictCode().equals(dict.getDictCode())) {
            if (dataDictRepository.existsByDictCode(dict.getDictCode())) {
                return ResponseEntity.badRequest().body(Map.of("error", "字典编码已存在: " + dict.getDictCode()));
            }
            toUpdate.setDictCode(dict.getDictCode());
        }
        toUpdate.setDictName(dict.getDictName());
        toUpdate.setDescription(dict.getDescription());
        toUpdate.setSortOrder(dict.getSortOrder());
        toUpdate.setStatus(dict.getStatus());
        toUpdate.setUpdatedBy(dict.getUpdatedBy());
        
        DataDict saved = dataDictRepository.save(toUpdate);
        return ResponseEntity.ok(saved);
    }
    
    /**
     * 删除字典分类（同时删除其下所有字典项）
     * 
     * @param id 字典ID
     * @return 空响应体，HTTP 状态码 200 表示删除成功
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDict(@PathVariable Long id) {
        if (!dataDictRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        // 由于外键设置了 ON DELETE CASCADE，删除字典时会自动删除字典项
        dataDictRepository.deleteById(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 切换字典状态（启用/禁用）
     * 
     * @param id      字典ID
     * @param request 包含 status 字段的请求体
     * @return 更新后的字典对象
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<?> toggleDictStatus(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Optional<DataDict> existing = dataDictRepository.findById(id);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        DataDict dict = existing.get();
        Integer status = (Integer) request.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "缺少 status 参数"));
        }
        dict.setStatus(status);
        dict.setUpdatedBy(request.get("updatedBy") != null ? ((Number) request.get("updatedBy")).longValue() : null);
        
        DataDict saved = dataDictRepository.save(dict);
        return ResponseEntity.ok(saved);
    }
    
    // ==================== 字典项管理接口 ====================
    
    /**
     * 根据字典编码获取启用的字典项列表
     * 
     * @param dictCode 字典编码
     * @return 字典项VO列表
     */
    @GetMapping("/items/{dictCode}")
    public ResponseEntity<List<DictItemVO>> getDictItems(@PathVariable String dictCode) {
        return dataDictRepository.findByDictCode(dictCode)
            .map(dict -> {
                List<DataDictItem> items = dataDictItemRepository.findByDictIdAndStatusOrderBySortOrder(dict.getId(), 1);
                List<DictItemVO> result = items.stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());
                return ResponseEntity.ok(result);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 根据字典编码获取所有字典项（包括禁用的）
     * 
     * @param dictCode 字典编码
     * @return 字典项VO列表
     */
    @GetMapping("/items/{dictCode}/all")
    public ResponseEntity<List<DictItemVO>> getAllDictItems(@PathVariable String dictCode) {
        return dataDictRepository.findByDictCode(dictCode)
            .map(dict -> {
                List<DataDictItem> items = dataDictItemRepository.findByDictIdOrderBySortOrder(dict.getId());
                List<DictItemVO> result = items.stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());
                return ResponseEntity.ok(result);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * 根据字典ID获取所有字典项
     * 
     * @param dictId 字典ID
     * @return 字典项列表
     */
    @GetMapping("/{dictId}/items")
    public ResponseEntity<List<DictItemVO>> getItemsByDictId(@PathVariable Long dictId) {
        List<DataDictItem> items = dataDictItemRepository.findByDictIdOrderBySortOrder(dictId);
        List<DictItemVO> result = items.stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }
    
    /**
     * 新增字典项
     * 
     * @param dictId 字典ID
     * @param item   字典项对象
     * @return 新增后的字典项对象
     */
    @PostMapping("/{dictId}/items")
    public ResponseEntity<?> createItem(@PathVariable Long dictId, @RequestBody DataDictItem item) {
        // 检查字典是否存在
        if (!dataDictRepository.existsById(dictId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "字典不存在，ID: " + dictId));
        }
        // 检查字典项编码是否已存在
        if (dataDictItemRepository.existsByDictIdAndItemCode(dictId, item.getItemCode())) {
            return ResponseEntity.badRequest().body(Map.of("error", "字典项编码已存在: " + item.getItemCode()));
        }
        item.setDictId(dictId);
        DataDictItem saved = dataDictItemRepository.save(item);
        return ResponseEntity.ok(saved);
    }
    
    /**
     * 更新字典项
     * 
     * @param itemId 字典项ID
     * @param item   包含新字段值的字典项对象
     * @return 更新后的字典项对象
     */
    @PutMapping("/items/{itemId}")
    public ResponseEntity<?> updateItem(@PathVariable Long itemId, @RequestBody DataDictItem item) {
        Optional<DataDictItem> existing = dataDictItemRepository.findById(itemId);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        DataDictItem toUpdate = existing.get();
        // 如果修改了字典项编码，检查新编码是否已存在
        if (!toUpdate.getItemCode().equals(item.getItemCode())) {
            if (dataDictItemRepository.existsByDictIdAndItemCode(toUpdate.getDictId(), item.getItemCode())) {
                return ResponseEntity.badRequest().body(Map.of("error", "字典项编码已存在: " + item.getItemCode()));
            }
            toUpdate.setItemCode(item.getItemCode());
        }
        toUpdate.setItemValue(item.getItemValue());
        toUpdate.setParentCode(item.getParentCode());
        toUpdate.setSortOrder(item.getSortOrder());
        toUpdate.setStatus(item.getStatus());
        toUpdate.setUpdatedBy(item.getUpdatedBy());
        
        DataDictItem saved = dataDictItemRepository.save(toUpdate);
        return ResponseEntity.ok(saved);
    }
    
    /**
     * 删除字典项
     * 
     * @param itemId 字典项ID
     * @return 空响应体
     */
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long itemId) {
        if (!dataDictItemRepository.existsById(itemId)) {
            return ResponseEntity.notFound().build();
        }
        dataDictItemRepository.deleteById(itemId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 批量更新字典项排序
     * 
     * @param dictId  字典ID
     * @param requests 包含 id 和 sortOrder 的对象列表
     * @return 更新成功消息
     */
    @PostMapping("/{dictId}/items/batch-sort")
    public ResponseEntity<?> batchUpdateSort(@PathVariable Long dictId, @RequestBody List<Map<String, Object>> requests) {
        for (Map<String, Object> req : requests) {
            Long itemId = ((Number) req.get("id")).longValue();
            Integer sortOrder = (Integer) req.get("sortOrder");
            
            dataDictItemRepository.findById(itemId).ifPresent(item -> {
                item.setSortOrder(sortOrder);
                dataDictItemRepository.save(item);
            });
        }
        return ResponseEntity.ok(Map.of("message", "排序更新成功"));
    }
    
    /**
     * 切换字典项状态（启用/禁用）
     * 
     * @param itemId  字典项ID
     * @param request 包含 status 字段的请求体
     * @return 更新后的字典项对象
     */
    @PutMapping("/items/{itemId}/status")
    public ResponseEntity<?> toggleItemStatus(@PathVariable Long itemId, @RequestBody Map<String, Object> request) {
        Optional<DataDictItem> existing = dataDictItemRepository.findById(itemId);
        if (existing.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        DataDictItem item = existing.get();
        Integer status = (Integer) request.get("status");
        if (status == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "缺少 status 参数"));
        }
        item.setStatus(status);
        item.setUpdatedBy(request.get("updatedBy") != null ? ((Number) request.get("updatedBy")).longValue() : null);
        
        DataDictItem saved = dataDictItemRepository.save(item);
        return ResponseEntity.ok(saved);
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 将字典项实体转换为VO对象
     * 
     * @param item 字典项实体
     * @return 字典项VO
     */
    private DictItemVO convertToVO(DataDictItem item) {
        return new DictItemVO(
            item.getId(),
            item.getItemCode(),
            item.getItemValue(),
            item.getParentCode(),
            item.getSortOrder(),
            item.getStatus()
        );
    }
    
    /**
     * 字典项视图对象
     * 包含字典项的完整信息
     */
    public static class DictItemVO {
        private Long id;
        private String code;
        private String value;
        private String parentCode;
        private Integer sortOrder;
        private Integer status;
        
        public DictItemVO(Long id, String code, String value, String parentCode, Integer sortOrder, Integer status) {
            this.id = id;
            this.code = code;
            this.value = value;
            this.parentCode = parentCode;
            this.sortOrder = sortOrder;
            this.status = status;
        }
        
        // 旧版构造函数，保持兼容
        public DictItemVO(String code, String value) {
            this.code = code;
            this.value = value;
        }
        
        public Long getId() { return id; }
        public String getCode() { return code; }
        public String getValue() { return value; }
        public String getParentCode() { return parentCode; }
        public Integer getSortOrder() { return sortOrder; }
        public Integer getStatus() { return status; }
    }
}