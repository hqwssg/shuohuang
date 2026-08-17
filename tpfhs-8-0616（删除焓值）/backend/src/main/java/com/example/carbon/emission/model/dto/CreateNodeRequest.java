package com.example.carbon.emission.model.dto;

import lombok.Data;

@Data
public class CreateNodeRequest {
    
    private String name;
    
    private Integer typeId;
    
    private Long parentId;
    
    private Long templateId;
    
    private String locomotiveType;
    
    private NodeConfigDTO config;
    
    private Long createdBy;
    
    private NodeInfoDTO nodeInfo;
}