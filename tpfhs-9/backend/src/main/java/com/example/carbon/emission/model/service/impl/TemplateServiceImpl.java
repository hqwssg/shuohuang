package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.dto.CreateTemplateRequest;
import com.example.carbon.emission.model.dto.TemplateDTO;
import com.example.carbon.emission.model.entity.EmissionNode;
import com.example.carbon.emission.model.entity.EmissionNodeConfig;
import com.example.carbon.emission.model.entity.Template;
import com.example.carbon.emission.model.entity.User;
import com.example.carbon.emission.model.repository.EmissionNodeConfigRepository;
import com.example.carbon.emission.model.repository.EmissionNodeRepository;
import com.example.carbon.emission.model.repository.TemplateRepository;
import com.example.carbon.emission.model.repository.UserRepository;
import com.example.carbon.emission.model.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TemplateServiceImpl implements TemplateService {
    
    @Autowired
    private TemplateRepository templateRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private EmissionNodeRepository nodeRepository;
    
    @Autowired
    private EmissionNodeConfigRepository configRepository;
    
    @Override
    public List<TemplateDTO> getAllTemplates() {
        return templateRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }
    
    @Override
    public TemplateDTO getTemplateById(Long id) {
        Template template = templateRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("模版不存在"));
        return convertToDTO(template);
    }
    
    @Override
    @Transactional
    public TemplateDTO createTemplate(CreateTemplateRequest request) {
        Template template = new Template();
        template.setName(request.getName());
        template.setDescription(request.getDescription());
        template.setCreatedBy(request.getCreatedBy());
        template.setUpdatedBy(request.getCreatedBy());
        // 模版类型：未指定时默认为1（节点模版）
        template.setTemplateType(request.getTemplateType() != null ? request.getTemplateType() : 1);

        Template saved = templateRepository.save(template);
        return convertToDTO(saved);
    }
    
    @Override
    @Transactional
    public TemplateDTO updateTemplate(Long id, String name, Long updatedBy) {
        Template template = templateRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("模版不存在"));

        if (name != null && !name.isEmpty()) {
            template.setName(name);
        }
        template.setUpdatedBy(updatedBy);
        // 模版被修改，校验结果自动重置为未检查
        resetCheckStatus(template);

        Template saved = templateRepository.save(template);
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public TemplateDTO updateTemplate(Long id, String description, Boolean enabled, Integer templateType, String taskConfig, Long factorTemplateId, Long updatedBy) {
        Template template = templateRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("模版不存在"));

        if (description != null) {
            template.setDescription(description);
        }
        if (enabled != null) {
            template.setEnabled(enabled);
        }
        // 模版类型：1-节点模版；2-核算模版
        if (templateType != null) {
            template.setTemplateType(templateType);
        }
        if (taskConfig != null) {
            template.setTaskConfig(taskConfig);
        }
        // factorTemplateId 允许设为 null（清除关联）
        template.setFactorTemplateId(factorTemplateId);
        template.setUpdatedBy(updatedBy);
        // 模版被修改，校验结果自动重置为未检查
        resetCheckStatus(template);

        Template saved = templateRepository.save(template);
        return convertToDTO(saved);
    }

    /**
     * 模版被修改后重置校验状态为未检查（check_result=0，清空校验时间与告警信息）
     *
     * @param template 待重置的模版实体
     */
    private void resetCheckStatus(Template template) {
        template.setCheckResult(0);
        template.setCheckTime(null);
        template.setCheckMessage(null);
    }
    
    @Override
    @Transactional
    public TemplateDTO copyTemplate(Long sourceId, String newName, Long createdBy) {
        Template source = templateRepository.findById(sourceId)
            .orElseThrow(() -> new RuntimeException("源模版不存在"));
        
        Template copy = new Template();
        copy.setName(newName);
        copy.setDescription(source.getDescription());
        copy.setCreatedBy(createdBy);
        copy.setUpdatedBy(createdBy);
        copy.setVersion(1);
        copy.setEnabled(source.getEnabled());
        copy.setTemplateType(source.getTemplateType() != null ? source.getTemplateType() : 1);
        copy.setTaskConfig(source.getTaskConfig());
        
        Template saved = templateRepository.save(copy);
        
        List<EmissionNode> sourceNodes = nodeRepository.findByTemplateId(sourceId);
        
        Map<Long, Long> nodeIdMap = new HashMap<>();
        
        for (EmissionNode sourceNode : sourceNodes) {
            EmissionNode newNode = new EmissionNode();
            newNode.setName(sourceNode.getName());
            newNode.setTypeId(sourceNode.getTypeId());
            newNode.setParentId(nodeIdMap.getOrDefault(sourceNode.getParentId(), null));
            newNode.setTemplateId(saved.getId());
            newNode.setLocomotiveType(sourceNode.getLocomotiveType());
            newNode.setSortOrder(sourceNode.getSortOrder());
            newNode.setCreatedBy(createdBy);
            newNode.setUpdatedBy(createdBy);
            
            EmissionNode savedNode = nodeRepository.save(newNode);
            nodeIdMap.put(sourceNode.getId(), savedNode.getId());
        }
        
        for (EmissionNode sourceNode : sourceNodes) {
            configRepository.findByNodeId(sourceNode.getId()).ifPresent(sourceConfig -> {
                EmissionNodeConfig newConfig = new EmissionNodeConfig();
                newConfig.setNodeId(nodeIdMap.get(sourceNode.getId()));
                newConfig.setEmissionCategory(sourceConfig.getEmissionCategory());
                newConfig.setEmissionSubcategory(sourceConfig.getEmissionSubcategory());
                newConfig.setCarbonEmissionFactor(sourceConfig.getCarbonEmissionFactor());
                newConfig.setCarbonEmissionFactorDescription(sourceConfig.getCarbonEmissionFactorDescription());
                newConfig.setCollectionPointType(sourceConfig.getCollectionPointType());
                newConfig.setCollectionPointId(sourceConfig.getCollectionPointId());
                newConfig.setCreatedBy(createdBy);
                newConfig.setUpdatedBy(createdBy);
                
                configRepository.save(newConfig);
            });
        }
        
        return convertToDTO(saved);
    }
    
    @Override
    @Transactional
    public void deleteTemplate(Long id) {
        if (!templateRepository.existsById(id)) {
            throw new RuntimeException("模版不存在");
        }
        templateRepository.deleteById(id);
    }
    
    private TemplateDTO convertToDTO(Template template) {
        TemplateDTO dto = new TemplateDTO();
        dto.setId(template.getId());
        dto.setName(template.getName());
        dto.setDescription(template.getDescription());
        dto.setCreatedBy(template.getCreatedBy());
        dto.setCreatedAt(template.getCreatedAt());
        dto.setUpdatedBy(template.getUpdatedBy());
        dto.setUpdatedAt(template.getUpdatedAt());
        dto.setVersion(template.getVersion());
        dto.setEnabled(template.getEnabled());
        dto.setTemplateType(template.getTemplateType() != null ? template.getTemplateType() : 1);
        dto.setTaskConfig(template.getTaskConfig());
        dto.setFactorTemplateId(template.getFactorTemplateId());
        dto.setCheckResult(template.getCheckResult() != null ? template.getCheckResult() : 0);
        dto.setCheckTime(template.getCheckTime());
        dto.setCheckMessage(template.getCheckMessage());
        
        if (template.getCreatedBy() != null) {
            userRepository.findById(template.getCreatedBy())
                .ifPresent(user -> dto.setCreatedByName(user.getNickName()));
        }
        if (template.getUpdatedBy() != null) {
            userRepository.findById(template.getUpdatedBy())
                .ifPresent(user -> dto.setUpdatedByName(user.getNickName()));
        }
        
        return dto;
    }
}