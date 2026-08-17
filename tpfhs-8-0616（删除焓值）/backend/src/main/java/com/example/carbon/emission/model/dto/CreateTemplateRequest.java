package com.example.carbon.emission.model.dto;

import lombok.Data;

@Data
public class CreateTemplateRequest {
    private String name;
    private String description;
    private Long createdBy;
}