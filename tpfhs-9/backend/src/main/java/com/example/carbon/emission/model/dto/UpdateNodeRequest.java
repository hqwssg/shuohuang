package com.example.carbon.emission.model.dto;

import lombok.Data;

@Data
public class UpdateNodeRequest {
    
    private String name;
    
    private NodeConfigDTO config;
    
    private Long updatedBy;
    
    private NodeInfoDTO nodeInfo;
}