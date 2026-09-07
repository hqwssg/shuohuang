package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 能耗分类数据字典实体类
 * 按能耗使用场景划分的三级分类（生产用能/生产辅助用能/综合用能）
 */
@Entity
@Table(name = "emission_energy_category")
@Data
public class EnergyCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 分类编码
     */
    @Column(name = "category_code", nullable = false, unique = true, length = 50)
    private String categoryCode;

    /**
     * 分类名称
     */
    @Column(name = "category_name", nullable = false, length = 100)
    private String categoryName;

    /**
     * 父级分类ID，用于三级分类层级关系，顶级为NULL
     */
    @Column(name = "parent_id")
    private Long parentId;

    /**
     * 分类层级：1-一级，2-二级，3-三级
     */
    @Column(name = "level", nullable = false)
    private Integer level;

    /**
     * 备注说明
     */
    @Column(name = "remark", length = 500)
    private String remark;

    /**
     * 排序顺序，数值越小越靠前
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /**
     * 状态：1-启用，0-停用
     */
    @Column(name = "status")
    private Integer status = 1;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = 1;
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
