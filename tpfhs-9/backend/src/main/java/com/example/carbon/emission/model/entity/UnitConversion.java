package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 单位转换系数实体类
 * <p>
 * 为每个细分种类（emission_subcategory）的计量单位提供成对的转换系数，
 * 双向显式存储，便于查找与维护。<br>
 * 转换公式：<strong>目标值 = 源值 × conversionFactor</strong>
 */
@Entity
@Table(name = "emission_unit_conversion",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_subcat_from_to",
                columnNames = {"subcategory_code", "from_unit_code", "to_unit_code"}))
@Data
public class UnitConversion {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 排放数据小类编码
     */
    @Column(name = "subcategory_code", nullable = false, length = 50)
    private String subcategoryCode;

    /**
     * 源单位编码
     */
    @Column(name = "from_unit_code", nullable = false, length = 30)
    private String fromUnitCode;

    /**
     * 目标单位编码
     */
    @Column(name = "to_unit_code", nullable = false, length = 30)
    private String toUnitCode;

    /**
     * 转换系数：目标值 = 源值 × conversionFactor
     */
    @Column(name = "conversion_factor", nullable = false, precision = 18, scale = 8)
    private BigDecimal conversionFactor;

    /**
     * 备注说明
     */
    @Column(name = "remark", length = 500)
    private String remark;

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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
