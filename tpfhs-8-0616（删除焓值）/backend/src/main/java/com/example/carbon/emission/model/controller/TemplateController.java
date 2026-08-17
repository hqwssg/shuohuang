package com.example.carbon.emission.model.controller;

import com.example.carbon.emission.model.dto.CreateTemplateRequest;
import com.example.carbon.emission.model.dto.TemplateDTO;
import com.example.carbon.emission.model.service.TemplateService;
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
        return ResponseEntity.ok(templateService.updateTemplate(id, description, enabled, taskConfig, updatedBy));
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
}