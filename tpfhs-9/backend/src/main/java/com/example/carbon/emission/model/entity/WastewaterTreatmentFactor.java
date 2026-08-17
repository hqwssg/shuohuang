package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 废水处理排放因子实体类
 * <p>
 * 用于存储废水处理排放因子数据，包括处理废水种类、需氧浓度系数、甲烷产生能力、
 * 甲烷修正因子、全球变暖潜能值和碳排放因子等信息。
 * </p>
 */
@Entity
@Table(name = "emission_wastewater_treatment_factor")
@Data
public class WastewaterTreatmentFactor {

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
     * 处理废水种类，如生活污水、机车检修废水等
     */
    @Column(name = "wastewater_type", length = 200)
    private String wastewaterType;

    /**
     * 需氧浓度系数（COD或BOD，单位：mg/L）（OD）
     */
    @Column(name = "od", precision = 10, scale = 4)
    private BigDecimal od;

    /**
     * 最大甲烷产生能力（单位tCH4/t）（Bo）
     */
    @Column(name = "bo", precision = 10, scale = 4)
    private BigDecimal bo;

    /**
     * 甲烷修正因子（MCF）
     */
    @Column(name = "mcf", precision = 10, scale = 4)
    private BigDecimal mcf;

    /**
     * 甲烷的全球变暖潜能值，缺省为28（GWP）
     */
    @Column(name = "gwp", precision = 10, scale = 2)
    private BigDecimal gwp;

    /**
     * 碳排放因子（计算方式：OD * Bo * MCF * GWP / 1000000）
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
