package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 化石燃料排放因子实体类
 * <p>
 * 用于存储化石燃料排放因子数据，包括燃料品种、低位发热量、单位热值含碳量、
 * 燃料氧化率、碳排放因子等信息。数据来源于《中国能源统计年鉴》和《省级温室气体清单编制指南》。
 * </p>
 */
@Entity
@Table(name = "emission_fossil_fuel_emission_factor")
@Data
public class FossilFuelEmissionFactor {
    
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
     * 燃料品种，如烟煤、褐煤、焦炭等
     */
    @Column(name = "fuel_type", nullable = false, length = 200)
    private String fuelType;
    
    /**
     * 数据来源
     */
    @Column(name = "source", length = 200)
    private String source;
    
    /**
     * 计量单位
     */
    @Column(name = "unit", nullable = false, length = 50)
    private String unit;
    
    /**
     * 低位发热量（kJ/kg或kJ/Nm³）
     */
    @Column(name = "lower_heating_value", precision = 15, scale = 6)
    private BigDecimal lowerHeatingValue;
    
    /**
     * 单位热值含碳量（tC/TJ）
     */
    @Column(name = "carbon_content_per_unit_heat", precision = 15, scale = 6)
    private BigDecimal carbonContentPerUnitHeat;
    
    /**
     * 燃料氧化率（0-1之间）
     */
    @Column(name = "fuel_oxidation_rate", precision = 5, scale = 4)
    private BigDecimal fuelOxidationRate;
    
    /**
     * 碳排放因子（tCO2/GJ）
     */
    @Column(name = "emission_factor", precision = 15, scale = 6)
    private BigDecimal emissionFactor;
    
    /**
     * 排放因子单位
     */
    @Column(name = "factor_unit", length = 50)
    private String factorUnit;
    
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