package com.example.carbon.emission.model.dto;

import lombok.Data;

@Data
public class CreateTemplateRequest {
    private String name;
    private String description;
    private Long createdBy;
    /**
     * 模版类型：1-节点模版（默认）；2-核算模版
     */
    private Integer templateType;
}