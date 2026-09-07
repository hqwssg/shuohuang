package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 计量标准单位实体类
 * <p>
 * 统一程序内涉及到的所有计量单位，作为单位字段下拉选项唯一数据源，
 * 禁止人工录入非标准单位。
 */
@Entity
@Table(name = "emission_unit_standard")
@Data
public class UnitStandard {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 单位编码（程序内统一编码，如 kWh/MWh/GJ/L/m3/wan_m3/kg/t/g/m2）
     */
    @Column(name = "unit_code", nullable = false, unique = true, length = 30)
    private String unitCode;

    /**
     * 单位中文名称（用于展示）
     */
    @Column(name = "unit_name", nullable = false, length = 50)
    private String unitName;

    /**
     * 单位大类：ELECTRIC/HEAT/LIQUID_FUEL/GAS_FUEL/SOLID_FUEL/AREA
     */
    @Column(name = "unit_category", nullable = false, length = 20)
    private String unitCategory;

    /**
     * 物理量：ENERGY-能量、VOLUME-体积、MASS-质量、AREA-面积
     */
    @Column(name = "physical_quantity", nullable = false, length = 20)
    private String physicalQuantity;

    /**
     * 是否本类基准单位：1-是，0-否
     */
    @Column(name = "is_base")
    @JsonProperty("isBase")
    private Integer isBase = 0;

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
        if (isBase == null) isBase = 0;
        if (sortOrder == null) sortOrder = 0;
        if (status == null) status = 1;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
