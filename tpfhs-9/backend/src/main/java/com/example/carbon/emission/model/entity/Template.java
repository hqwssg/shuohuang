package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "emission_template")
@Data
public class Template {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_by")
    private Long updatedBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "version")
    private Integer version = 1;
    
    @Column(name = "enabled")
    private Boolean enabled = true;

    /**
     * 模版类型：1-节点模版（仅用于构建层级关系，作为结构被引用，不参与碳排放自动化核算）；
     *           2-核算模版（实现碳排放自动化核算功能，支持树形节点管理、属性配置及计划任务调度）
     */
    @Column(name = "template_type")
    private Integer templateType = 1;

    @Column(name = "task_config", columnDefinition = "TEXT")
    private String taskConfig;

    @Column(name = "factor_template_id")
    private Long factorTemplateId;

    /**
     * 模版校验结果：0-未检查，1-完全正确，2-正确（存在提示信息），
     * 3-存在告警，4-存在错误。模版被修改后自动重置为0。
     */
    @Column(name = "check_result")
    private Integer checkResult = 0;

    /** 最近一次模版校验时间 */
    @Column(name = "check_time")
    private LocalDateTime checkTime;

    /**
     * 最近一次校验结果详情（富文本HTML）：
     * 错误-红#F56C6C、告警-橙#E6A23C、提示-蓝#409EFF、通过-绿#67C23A
     */
    @Column(name = "check_message", columnDefinition = "TEXT")
    private String checkMessage;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        version = 1;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (version == null) {
            version = 1;
        } else {
            version++;
        }
    }
}