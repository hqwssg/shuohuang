package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 数据来源系统实体类
 * 用于管理数据来源系统的基本信息
 */
@Entity
@Table(name = "emission_data_source_system")
@Data
public class DataSourceSystem {
    
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 系统名称
     */
    @Column(name = "system_name", length = 100, nullable = false)
    private String systemName;
    
    /**
     * 描述信息
     */
    @Column(name = "description", length = 500)
    private String description;
    
    /**
     * 拼音首字母编码，用于快速搜索
     */
    @Column(name = "pinyin_code", length = 50)
    private String pinyinCode;
    
    /**
     * 创建人ID
     */
    @Column(name = "created_by")
    private Long createdBy;
    
    /**
     * 更新人ID
     */
    @Column(name = "updated_by")
    private Long updatedBy;
    
    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    /**
     * 实体创建前自动填充创建时间和更新时间
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    /**
     * 实体更新前自动更新更新时间
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
