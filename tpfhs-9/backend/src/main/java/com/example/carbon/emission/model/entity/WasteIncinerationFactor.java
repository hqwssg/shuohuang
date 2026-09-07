package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 固体废弃物焚烧排放因子实体类
 * <p>
 * 用于存储固体废弃物焚烧排放因子数据，包括固体废物种类、碳含量比例、化石碳比例、
 * 燃烧效率和碳排放因子等信息。数据来源于《省级温室气体清单编制指南（2025年版）》。
 * </p>
 */
@Entity
@Table(name = "emission_waste_incineration_factor")
@Data
public class WasteIncinerationFactor {

    /**
     * 主键ID，自增
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 碳排放因子名称
     */
    @Column(name = "emission_factor_name", length = 200)
    private String emissionFactorName;

    /**
     * 固体废物种类，如生活垃圾、危险废物等
     */
    @Column(name = "waste_type", length = 200)
    private String wasteType;

    /**
     * 废弃物中的碳含量比例（CCW）
     */
    @Column(name = "ccw", precision = 10, scale = 4)
    private BigDecimal ccw;

    /**
     * 废弃物中的化石碳在总碳中的比例（FCF）
     */
    @Column(name = "fcf", precision = 10, scale = 4)
    private BigDecimal fcf;

    /**
     * 废弃物焚烧炉的完全燃烧效率（CE）
     */
    @Column(name = "ce", precision = 10, scale = 4)
    private BigDecimal ce;

    /**
     * 碳排放因子（计算方式：CCW * FCF * CE * 44/12）
     */
    @Column(name = "emission_factor", precision = 15, scale = 6)
    private BigDecimal emissionFactor;

    /**
     * 单位
     */
    @Column(name = "unit", length = 50)
    private String unit;

    /**
     * 数据来源
     */
    @Column(name = "source", length = 200)
    private String source;

    /**
     * 碳排放因子说明
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * 创建人ID
     */
    @Column(name = "created_by")
    private Long createdBy;

    /**
     * 创建时间
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 修改人ID
     */
    @Column(name = "updated_by")
    private Long updatedBy;

    /**
     * 修改时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 实体持久化前自动设置创建时间和更新时间
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    /**
     * 实体更新前自动设置更新时间
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
