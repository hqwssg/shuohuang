package com.example.carbon.emission.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TemplateDTO {
    private Long id;
    private String name;
    private String description;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private String updatedByName;
    private LocalDateTime updatedAt;
    private Integer version;
    private Boolean enabled;
    private String taskConfig;
}