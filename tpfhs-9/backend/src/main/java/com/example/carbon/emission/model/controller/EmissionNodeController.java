package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.dto.CreateNodeRequest;
import com.example.carbon.emission.model.dto.NodeConfigDTO;
import com.example.carbon.emission.model.dto.NodeDTO;
import com.example.carbon.emission.model.dto.UpdateNodeRequest;
import com.example.carbon.emission.model.service.DataDictService;
import com.example.carbon.emission.model.service.EmissionNodeService;
import com.example.carbon.emission.model.security.CarbonDataScopeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/nodes")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class EmissionNodeController {
    
    @Autowired
    private EmissionNodeService nodeService;
    
    @Autowired
    private DataDictService dictService;

    @Autowired
    private CarbonDataScopeService dataScopeService;
    
    @GetMapping
    public ResponseEntity<NodeDTO> getAllNodes() {
        return ResponseEntity.ok(nodeService.getTree(null));
    }
    
    @GetMapping("/tree")
    public ResponseEntity<NodeDTO> getTree(@RequestParam(required = false) Long templateId) {
        return ResponseEntity.ok(dataScopeService.filterTree(nodeService.getTree(templateId)));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<NodeDTO> getNodeById(@PathVariable Long id) {
        return ResponseEntity.ok(nodeService.getNodeById(id));
    }
    
    @PostMapping
    public ResponseEntity<NodeDTO> createNode(@RequestBody CreateNodeRequest request) {
        return ResponseEntity.ok(nodeService.createNode(request));
    }

    /**
     * 挂载节点模版
     *
     * 将指定节点模版（templateType=1）所包含的全部子节点复制到当前模版树中，
     * 挂载到目标父节点（typeId=1/2 的"排放核算点"）之下。
     * 请求体需包含 templateId（来源节点模版ID）与 createdBy（创建人ID）。
     *
     * @param parentId 目标父节点ID（挂载位置）
     * @param body     请求体，包含 templateId 与 createdBy
     * @return 挂载后的目标父节点DTO
     */
    @PostMapping("/{parentId}/mount-template")
    public ResponseEntity<NodeDTO> mountNodeTemplate(@PathVariable Long parentId,
                                                     @RequestBody Map<String, Long> body) {
        Long sourceTemplateId = body.get("templateId");
        Long createdBy = body.get("createdBy");
        return ResponseEntity.ok(nodeService.mountNodeTemplate(parentId, sourceTemplateId, createdBy));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<NodeDTO> updateNode(@PathVariable Long id, @RequestBody UpdateNodeRequest request) {
        return ResponseEntity.ok(nodeService.updateNode(id, request));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNode(@PathVariable Long id) {
        nodeService.deleteNode(id);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 获取下一个可用的节点ID
     * 
     * 获取当前数据库中最大的节点ID并加1，用于在创建新节点前预测节点ID，
     * 以便生成唯一的设备编号。
     * 
     * @return 下一个可用的节点ID
     */
    @GetMapping("/next-id")
    public ResponseEntity<Long> getNextNodeId() {
        return ResponseEntity.ok(nodeService.getNextNodeId());
    }
    
    @GetMapping("/{id}/config")
    public ResponseEntity<NodeConfigDTO> getNodeConfig(@PathVariable Long id) {
        return ResponseEntity.ok(nodeService.getNodeConfig(id));
    }
    
    @PutMapping("/{id}/config")
    public ResponseEntity<NodeConfigDTO> updateNodeConfig(@PathVariable Long id, @RequestBody NodeConfigDTO config) {
        return ResponseEntity.ok(nodeService.updateNodeConfig(id, config, config.getUpdatedBy()));
    }
    
    @GetMapping("/options")
    public ResponseEntity<Map<String, Object>> getOptions() {
        return ResponseEntity.ok(dictService.getAllOptions());
    }
    
    @PostMapping("/{id}/move")
    public ResponseEntity<Void> moveNode(@PathVariable Long id, @RequestParam String direction) {
        nodeService.moveNode(id, direction);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 生成采集设备编号
     * 
     * 根据节点ID和排放类别生成设备编号，支持多种编码规则配置。
     * 编码规则由系统配置 Auto_coding_rules 控制。
     * 
     * @param id 父节点ID或节点ID
     * @param emissionCategory 排放数据大类（如"化石燃料"、"电力"等）
     * @param newNodeId 新节点ID（可选，用于新建节点场景）
     * @return 生成的设备编号
     */
    @GetMapping("/{id}/equipment-code")
    public ResponseEntity<String> generateEquipmentCode(@PathVariable Long id, @RequestParam String emissionCategory, 
                                                        @RequestParam(required = false) Long newNodeId) {
        return ResponseEntity.ok(nodeService.generateEquipmentCode(id, emissionCategory, newNodeId));
    }
}
