package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 碳排放因子单位实体类
 * <p>
 * 每行一个合法的因子单位组合（分子+分母），按排放数据小类限定。
 * 分子 numerator_ghg_code 关联 emission_ghg_unit.ghg_code；
 * 分母 denominator_unit_code 关联 emission_unit_standard.unit_code。
 */
@Entity
@Table(name = "emission_factor_unit")
@Data
public class FactorUnit {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 因子单位编码（ASCII，如 kgCO2_per_kWh、tCO2_per_MWh）
     */
    @Column(name = "factor_unit_code", nullable = false, unique = true, length = 50)
    private String factorUnitCode;

    /**
     * 展示名称（如 kgCO₂/kWh、tCO₂/MWh）
     */
    @Column(name = "factor_unit_name", nullable = false, length = 100)
    private String factorUnitName;

    /**
     * 分子单位编码（关联 emission_ghg_unit.ghg_code，如 kgCO2、tCO2）
     */
    @Column(name = "numerator_ghg_code", nullable = false, length = 30)
    private String numeratorGhgCode;

    /**
     * 分母单位编码（关联 emission_unit_standard.unit_code，如 kWh、MWh、t）
     */
    @Column(name = "denominator_unit_code", nullable = false, length = 30)
    private String denominatorUnitCode;

    /**
     * 适用排放数据小类编码（关联 emission_data_dict_item.item_code）；
     * 为空表示通用
     */
    @Column(name = "subcategory_code", length = 50)
    private String subcategoryCode;

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
