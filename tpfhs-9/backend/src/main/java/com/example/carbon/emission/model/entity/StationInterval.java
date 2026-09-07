package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 站点/区间实体类
 * 用于管理电力监测的站点或区间信息
 */
@Entity
@Table(name = "emission_station_interval")
@Data
public class StationInterval {
    
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 站点/区间名称
     */
    @Column(name = "name", nullable = false, unique = true, length = 60)
    private String name;
    
    /**
     * 拼音首字母编码，用于快速搜索
     */
    @Column(name = "pinyin_code", length = 30)
    private String pinyinCode;
    
    /**
     * 描述信息
     */
    @Column(name = "description", length = 500)
    private String description;
    
    /**
     * 排序顺序，数值越小越靠前
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
    /**
     * 状态
     * 1-启用，0-停用
     */
    @Column(name = "status")
    private Integer status = 1;
    
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
        if (status == null) {
            status = 1;
        }
    }
    
    /**
     * 实体更新前自动更新更新时间
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
