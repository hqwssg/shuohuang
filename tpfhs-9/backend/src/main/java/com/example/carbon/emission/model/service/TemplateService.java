package com.example.carbon.emission.model.service;

import com.example.carbon.emission.model.dto.CreateTemplateRequest;
import com.example.carbon.emission.model.dto.TemplateDTO;

import java.util.List;

public interface TemplateService {
    List<TemplateDTO> getAllTemplates();
    TemplateDTO getTemplateById(Long id);
    TemplateDTO createTemplate(CreateTemplateRequest request);
    TemplateDTO updateTemplate(Long id, String name, Long updatedBy);
    TemplateDTO updateTemplate(Long id, String description, Boolean enabled, Integer templateType, String taskConfig, Long factorTemplateId, Long updatedBy);
    TemplateDTO copyTemplate(Long sourceId, String newName, Long createdBy);
    void deleteTemplate(Long id);
}