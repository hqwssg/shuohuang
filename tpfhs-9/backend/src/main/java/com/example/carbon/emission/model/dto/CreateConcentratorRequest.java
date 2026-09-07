package com.example.carbon.emission.model.dto;

import lombok.Data;

/**
 * 创建集中器请求
 */
@Data
public class CreateConcentratorRequest {
    
    /**
     * 集中器名称
     */
    private String name;
    
    /**
     * 拼音首字母编码
     */
    private String pinyinCode;
    
    /**
     * 所属站点/区间ID
     */
    private Long stationIntervalId;
    
    /**
     * 变压器容量
     */
    private String transformerCapacity;
    
    /**
     * 集中器地址
     */
    private String concentratorAddress;
    
    /**
     * 描述信息
     */
    private String description;
    
    /**
     * 创建人ID
     */
    private Long createdBy;
}
