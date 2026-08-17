package com.example.carbon.emission.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class NodeDTO {
    
    private Long id;
    
    private String name;
    
    private String typeName;
    
    private Integer typeId;
    
    private Long parentId;
    
    private String locomotiveType;
    
    private List<NodeDTO> children;
    
    private Boolean canHaveChildren;
    
    private NodeConfigDTO config;
    
    private NodeInfoDTO nodeInfo;
}