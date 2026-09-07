package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.entity.MeterModel;
import com.example.carbon.emission.model.service.MeterModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 电表型号控制器
 */
@RestController
@RequestMapping("/api/meter-model")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class MeterModelController {
    
    private static final Logger logger = LoggerFactory.getLogger(MeterModelController.class);
    
    @Autowired
    private MeterModelService meterModelService;
    
    /**
     * 获取所有电表型号列表
     * 
     * @return 包含所有电表型号的响应实体；无数据时返回空列表
     */
    @GetMapping
    public ResponseEntity<List<MeterModel>> getAll() {
        return ResponseEntity.ok(meterModelService.findAll());
    }
    
    /**
     * 根据主键ID获取单个电表型号
     * 
     * @param id 电表型号主键ID
     * @return 找到时返回电表型号对象；不存在时返回 404 状态码
     */
    @GetMapping("/{id}")
    public ResponseEntity<MeterModel> getById(@PathVariable Long id) {
        MeterModel meterModel = meterModelService.findById(id);
        if (meterModel == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(meterModel);
    }
    
    /**
     * 根据型号名称关键字模糊查询电表型号
     * 
     * @param keyword 搜索关键词（支持部分匹配）
     * @return 符合条件的电表型号列表
     */
    @GetMapping("/search")
    public ResponseEntity<List<MeterModel>> search(@RequestParam String keyword) {
        return ResponseEntity.ok(meterModelService.findByModelNameContaining(keyword));
    }
    
    /**
     * 新增电表型号
     * 从请求体中解析 modelName、modelType、description、createdBy 字段并保存
     * 
     * @param request 请求体，包含 modelName（型号名称）、modelType（所属类型）、description（描述）、createdBy（创建人ID）
     * @return 保存后的电表型号对象（含生成的主键ID）
     */
    @PostMapping
    public ResponseEntity<MeterModel> create(@RequestBody Map<String, Object> request) {
        MeterModel meterModel = new MeterModel();
        meterModel.setModelName((String) request.get("modelName"));
        meterModel.setModelType((String) request.get("modelType"));
        meterModel.setDescription((String) request.get("description"));
        
        Object createdBy = request.get("createdBy");
        if (createdBy != null) {
            meterModel.setCreatedBy(((Number) createdBy).longValue());
        }
        
        MeterModel saved = meterModelService.save(meterModel);
        return ResponseEntity.ok(saved);
    }
    
    /**
     * 更新电表型号信息
     * 从请求体中解析 modelName、modelType、description、updatedBy 字段并更新指定ID的记录
     * 
     * @param id      待更新的电表型号主键ID
     * @param request 请求体，包含 modelName、modelType、description、updatedBy（修改人ID）
     * @return 更新后的电表型号对象
     */
    @PutMapping("/{id}")
    public ResponseEntity<MeterModel> update(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        MeterModel meterModel = new MeterModel();
        meterModel.setModelName((String) request.get("modelName"));
        meterModel.setModelType((String) request.get("modelType"));
        meterModel.setDescription((String) request.get("description"));
        
        Long userId = null;
        Object updatedBy = request.get("updatedBy");
        if (updatedBy != null) {
            userId = ((Number) updatedBy).longValue();
        }
        
        MeterModel updated = meterModelService.update(id, meterModel, userId);
        return ResponseEntity.ok(updated);
    }
    
    /**
     * 根据主键ID删除电表型号
     * 
     * @param id 待删除的电表型号主键ID
     * @return 空响应体，HTTP 状态码 200 表示删除成功
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        meterModelService.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
