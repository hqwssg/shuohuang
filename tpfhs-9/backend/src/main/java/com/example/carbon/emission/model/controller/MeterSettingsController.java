package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.dto.*;
import com.example.carbon.emission.model.service.MeterSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 电表设置控制器
 * 
 * 管理站点/区间-集中器-电表的树状结构
 * 提供节点的增删改查、启停、移动等RESTful API
 */
@RestController
@RequestMapping("/api/meter-settings")
@CrossOrigin(origins = "*")
public class MeterSettingsController {
    
    @Autowired
    private MeterSettingsService meterSettingsService;
    
    /**
     * 获取电表设置完整树
     * 
     * @return 树状结构，根节点为"电表设置"
     */
    @GetMapping("/tree")
    public ResponseEntity<MeterTreeNodeDTO> getTree() {
        return ResponseEntity.ok(meterSettingsService.getTree());
    }
    
    /**
     * 获取节点详情
     * 
     * @param nodeType 节点类型：station/concentrator/meter
     * @param id 节点ID
     * @return 节点详情
     */
    @GetMapping("/node/{nodeType}/{id}")
    public ResponseEntity<MeterTreeNodeDTO> getNodeDetail(
            @PathVariable String nodeType,
            @PathVariable Long id) {
        return ResponseEntity.ok(meterSettingsService.getNodeDetail(nodeType, id));
    }
    
    /**
     * 创建站点/区间
     * 
     * @param request 创建请求
     * @return 创建结果
     */
    @PostMapping("/station")
    public ResponseEntity<?> createStation(@RequestBody CreateStationRequest request) {
        try {
            MeterTreeNodeDTO result = meterSettingsService.createStation(request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 创建集中器
     * 
     * @param request 创建请求
     * @return 创建结果
     */
    @PostMapping("/concentrator")
    public ResponseEntity<?> createConcentrator(@RequestBody CreateConcentratorRequest request) {
        try {
            MeterTreeNodeDTO result = meterSettingsService.createConcentrator(request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 创建电表
     * 
     * @param request 创建请求
     * @return 创建结果
     */
    @PostMapping("/meter")
    public ResponseEntity<?> createMeter(@RequestBody CreateMeterRequest request) {
        try {
            MeterTreeNodeDTO result = meterSettingsService.createMeter(request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 更新节点
     * 
     * @param nodeType 节点类型
     * @param id 节点ID
     * @param request 更新请求（对应类型的Request对象）
     * @return 更新结果
     */
    @PutMapping("/node/{nodeType}/{id}")
    public ResponseEntity<?> updateNode(
            @PathVariable String nodeType,
            @PathVariable Long id,
            @RequestBody Object request) {
        try {
            MeterTreeNodeDTO result = meterSettingsService.updateNode(nodeType, id, request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 删除节点
     * 
     * @param nodeType 节点类型
     * @param id 节点ID
     * @param userId 操作人ID
     * @return 删除结果
     */
    @DeleteMapping("/node/{nodeType}/{id}")
    public ResponseEntity<?> deleteNode(
            @PathVariable String nodeType,
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            meterSettingsService.deleteNode(nodeType, id, userId);
            return ResponseEntity.ok(Map.of("message", "删除成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 切换节点状态（启用/停用）
     * 
     * @param nodeType 节点类型
     * @param id 节点ID
     * @param userId 操作人ID
     * @return 操作结果消息
     */
    @PostMapping("/node/{nodeType}/{id}/toggle-status")
    public ResponseEntity<?> toggleStatus(
            @PathVariable String nodeType,
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            String message = meterSettingsService.toggleStatus(nodeType, id, userId);
            Map<String, Object> result = new HashMap<>();
            result.put("message", message);
            result.put("success", message.contains("成功"));
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 上移节点
     * 
     * @param nodeType 节点类型
     * @param id 节点ID
     * @param userId 操作人ID
     * @return 操作结果
     */
    @PostMapping("/node/{nodeType}/{id}/move-up")
    public ResponseEntity<?> moveUp(
            @PathVariable String nodeType,
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            meterSettingsService.moveUp(nodeType, id, userId);
            return ResponseEntity.ok(Map.of("message", "上移成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 下移节点
     * 
     * @param nodeType 节点类型
     * @param id 节点ID
     * @param userId 操作人ID
     * @return 操作结果
     */
    @PostMapping("/node/{nodeType}/{id}/move-down")
    public ResponseEntity<?> moveDown(
            @PathVariable String nodeType,
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        try {
            meterSettingsService.moveDown(nodeType, id, userId);
            return ResponseEntity.ok(Map.of("message", "下移成功"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 更改电表级联关系
     * 
     * @param id 电表ID
     * @param request 修改请求
     * @return 修改后的电表节点DTO
     */
    @PutMapping("/meter/{id}/cascade")
    public ResponseEntity<?> changeMeterCascade(
            @PathVariable Long id,
            @RequestBody ChangeMeterCascadeRequest request) {
        try {
            MeterTreeNodeDTO result = meterSettingsService.changeMeterCascade(id, request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 更改电表从属数据集中器
     * 
     * @param id 电表ID
     * @param request 修改请求
     * @return 修改后的电表节点DTO
     */
    @PutMapping("/meter/{id}/point")
    public ResponseEntity<?> changeMeterPoint(
            @PathVariable Long id,
            @RequestBody ChangeMeterPointRequest request) {
        try {
            MeterTreeNodeDTO result = meterSettingsService.changeMeterPoint(id, request);
            return ResponseEntity.ok(result);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 获取指定集中器下的所有电表（除自身外）
     * 
     * @param pointId 集中器ID
     * @param excludeMeterId 排除的电表ID
     * @return 电表列表
     */
    @GetMapping("/meters-by-point")
    public ResponseEntity<List<MeterTreeNodeDTO>> getMetersByPoint(
            @RequestParam Long pointId,
            @RequestParam(required = false) Long excludeMeterId) {
        return ResponseEntity.ok(meterSettingsService.getMetersByPoint(pointId, excludeMeterId));
    }
    
    /**
     * 获取指定站点/区间下的所有集中器
     *
     * @param stationId 站点ID
     * @return 集中器列表
     */
    @GetMapping("/concentrators-by-station")
    public ResponseEntity<List<MeterTreeNodeDTO>> getConcentratorsByStation(
            @RequestParam Long stationId) {
        return ResponseEntity.ok(meterSettingsService.getConcentratorsByStation(stationId));
    }

    /**
     * 测试自动抄表接口（编辑模式，电表已存在）
     *
     * @param id     电表ID
     * @param body   请求体，包含 config 字段（抄表接口配置 Map）
     * @return 测试结果，包含 success 和 message
     */
    @PostMapping("/meter/{id}/test-reading")
    public ResponseEntity<?> testMeterReading(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) body.get("config");
            return ResponseEntity.ok(meterSettingsService.testMeterReading(id, config));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 测试自动抄表接口（新建模式，电表尚未保存）
     *
     * @param body 请求体，包含 config 字段（抄表接口配置 Map）
     * @return 测试结果，包含 success 和 message
     */
    @PostMapping("/meter/test-reading")
    public ResponseEntity<?> testMeterReadingWithoutId(@RequestBody Map<String, Object> body) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> config = (Map<String, Object>) body.get("config");
            return ResponseEntity.ok(meterSettingsService.testMeterReading(null, config));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
