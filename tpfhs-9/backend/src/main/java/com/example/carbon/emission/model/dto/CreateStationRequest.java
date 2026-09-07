package com.example.carbon.emission.model.dto;

import lombok.Data;

/**
 * 创建站点/区间请求
 */
@Data
public class CreateStationRequest {
    
    /**
     * 站点/区间名称
     */
    private String name;
    
    /**
     * 拼音首字母编码
     */
    private String pinyinCode;
    
    /**
     * 描述信息
     */
    private String description;
    
    /**
     * 创建人ID
     */
    private Long createdBy;
}
