package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.dto.CreateTemplateRequest;
import com.example.carbon.emission.model.dto.TemplateDTO;
import com.example.carbon.emission.model.dto.TemplateValidationResultVO;
import com.example.carbon.emission.model.service.TemplateService;
import com.example.carbon.emission.model.service.TemplateValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/template")
@CrossOrigin(origins = "*", allowCredentials = "false")
public class TemplateController {
    
    @Autowired
    private TemplateService templateService;

    @Autowired
    private TemplateValidationService templateValidationService;

    @GetMapping("/list")
    public ResponseEntity<List<TemplateDTO>> getAllTemplates() {
        return ResponseEntity.ok(templateService.getAllTemplates());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<TemplateDTO> getTemplateById(@PathVariable Long id) {
        return ResponseEntity.ok(templateService.getTemplateById(id));
    }
    
    @PostMapping("/create")
    public ResponseEntity<TemplateDTO> createTemplate(@RequestBody CreateTemplateRequest request) {
        return ResponseEntity.ok(templateService.createTemplate(request));
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<TemplateDTO> updateTemplate(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        Long updatedBy = ((Number) request.get("updatedBy")).longValue();
        return ResponseEntity.ok(templateService.updateTemplate(id, name, updatedBy));
    }
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @PutMapping("/{id}/properties")
    public ResponseEntity<TemplateDTO> updateTemplateProperties(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        String description = (String) request.get("description");
        Boolean enabled = (Boolean) request.get("enabled");

        // 模版类型：1-节点模版；2-核算模版
        Integer templateType = null;
        Object templateTypeObj = request.get("templateType");
        if (templateTypeObj instanceof Number) {
            templateType = ((Number) templateTypeObj).intValue();
        }

        String taskConfig = null;
        Object taskConfigObj = request.get("taskConfig");
        if (taskConfigObj != null) {
            if (taskConfigObj instanceof String) {
                taskConfig = (String) taskConfigObj;
            } else {
                try {
                    taskConfig = objectMapper.writeValueAsString(taskConfigObj);
                } catch (JsonProcessingException e) {
                    taskConfig = taskConfigObj.toString();
                }
            }
        }

        Long updatedBy = ((Number) request.get("updatedBy")).longValue();

        Long factorTemplateId = null;
        Object factorTemplateIdObj = request.get("factorTemplateId");
        if (factorTemplateIdObj instanceof Number) {
            factorTemplateId = ((Number) factorTemplateIdObj).longValue();
        }

        return ResponseEntity.ok(templateService.updateTemplate(id, description, enabled, templateType, taskConfig, factorTemplateId, updatedBy));
    }
    
    @PostMapping("/copy/{sourceId}")
    public ResponseEntity<TemplateDTO> copyTemplate(@PathVariable Long sourceId, @RequestBody Map<String, Object> request) {
        String newName = (String) request.get("newName");
        Long createdBy = ((Number) request.get("createdBy")).longValue();
        return ResponseEntity.ok(templateService.copyTemplate(sourceId, newName, createdBy));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 校验核算模版（仅核算模版有效）
     * <p>
     * 执行完整性校验：因子模版设置、采集点存在且启用、能耗小类覆盖、
     * 单独因子与因子模版一致性、空核算节点。结果保存到 emission_template
     * 的 check_result / check_time / check_message 字段并返回。
     *
     * @param id 核算模版ID
     * @return 校验结果（含富文本告警信息与结构化明细）
     */
    @PostMapping("/{id}/validate")
    public ResponseEntity<TemplateValidationResultVO> validateTemplate(@PathVariable Long id) {
        return ResponseEntity.ok(templateValidationService.validateTemplate(id));
    }

    /**
     * 查看核算模版最近一次校验结果（不重新校验）
     *
     * @param id 核算模版ID
     * @return 校验结果（含富文本告警信息）；未校验过时 checkResult=0
     */
    @GetMapping("/{id}/validation-result")
    public ResponseEntity<TemplateValidationResultVO> getValidationResult(@PathVariable Long id) {
        return ResponseEntity.ok(templateValidationService.getValidationResult(id));
    }
}