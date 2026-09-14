package com.example.carbon.emission.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TemplateDTO {
    private Long id;
    private String name;
    private String description;
    private Long deptId;
    private Long createdBy;
    private String createdByName;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private String updatedByName;
    private LocalDateTime updatedAt;
    private Integer version;
    private Boolean enabled;
    /**
     * 模版类型：1-节点模版；2-核算模版
     */
    private Integer templateType;
    private String taskConfig;
    private Long factorTemplateId;

    /**
     * 模版校验结果：0-未检查，1-完全正确，2-正确（存在提示信息），
     * 3-存在告警，4-存在错误
     */
    private Integer checkResult;

    /** 最近一次模版校验时间 */
    private LocalDateTime checkTime;

    /** 最近一次校验结果详情（富文本HTML） */
    private String checkMessage;
}
