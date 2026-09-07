package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 温室气体质量单位实体类
 * <p>
 * 碳排放因子的分子单位（kgCO2、tCO2、kgCH4、tCH4、kgN2O、tN2O 等）。
 * mass_unit_code 关联 emission_unit_standard.unit_code，确保质量单位合法。
 */
@Entity
@Table(name = "emission_ghg_unit")
@Data
public class GhgUnit {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 温室气体质量单位编码（ASCII，如 kgCO2、tCO2、kgCH4、tCH4、kgN2O、tN2O）
     */
    @Column(name = "ghg_code", nullable = false, unique = true, length = 30)
    private String ghgCode;

    /**
     * 展示名称（如 千克二氧化碳(kgCO₂)）
     */
    @Column(name = "ghg_name", nullable = false, length = 100)
    private String ghgName;

    /**
     * 温室气体种类：CO2、CH4、N2O
     */
    @Column(name = "ghg_type", nullable = false, length = 10)
    private String ghgType;

    /**
     * 质量单位编码（关联 emission_unit_standard.unit_code，如 kg、t、g）
     */
    @Column(name = "mass_unit_code", nullable = false, length = 30)
    private String massUnitCode;

    /**
     * 排序顺序
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /**
     * 状态：1-启用，0-停用
     */
    @Column(name = "status")
    private Integer status = 1;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (sortOrder == null) sortOrder = 0;
        if (status == null) status = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
