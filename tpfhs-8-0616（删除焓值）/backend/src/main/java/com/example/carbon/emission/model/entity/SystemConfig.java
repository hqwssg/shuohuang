package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统配置实体类
 * 
 * 用于存储系统运行所需的配置参数，支持动态配置系统行为。
 */
@Entity
@Table(name = "sys_config")
@Data
public class SystemConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "config_id")
    private Long configId;
    
    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;
    
    @Column(name = "config_value", nullable = false, length = 500)
    private String configValue;
    
    @Column(name = "config_name", length = 100)
    private String configName;
    
    @Column(name = "remark", length = 500)
    private String remark;
    
    @Column(name = "config_type", columnDefinition = "CHAR(1) DEFAULT 'N'")
    private String configType;
    
    @Column(name = "create_by", length = 64)
    private String createBy;
    
    @Column(name = "update_by", length = 64)
    private String updateBy;
    
    @Column(name = "create_time")
    private LocalDateTime createTime;
    
    @Column(name = "update_time")
    private LocalDateTime updateTime;
    
    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (configType == null) {
            configType = "N";
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }
}