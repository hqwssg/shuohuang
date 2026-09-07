package com.example.carbon.emission.model.dto;

import lombok.Data;

/**
 * 更改电表从属数据集中器请求
 */
@Data
public class ChangeMeterPointRequest {
    
    /**
     * 新的集中器ID
     */
    private Long pointId;
    
    /**
     * 新的上级电表ID，0表示无上级电表
     */
    private Long parentMeterId;
    
    /**
     * 操作人ID
     */
    private Long userId;
}
