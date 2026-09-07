package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 碳排放核算能耗缺省单位实体类
 * <p>
 * 按排放数据小类（emission_subcategory）为每种能耗提供两种缺省单位：
 * <ol>
 *   <li>calculationUnit（核算单位）：进行数据汇总时统一使用的计量单位（值为 emission_unit_standard.unit_code）</li>
 *   <li>reportUnit（报告单位）：碳排放报告中展示的计量单位（值为 emission_unit_standard.unit_code）</li>
 * </ol>
 * 两者之间的换算系数不在本表存储，由 emission_unit_conversion 表动态查询，
 * 避免双源数据不一致（决策2B）。
 */
@Entity
@Table(name = "emission_calc_unit_default")
@Data
public class CalcUnitDefault {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 排放数据小类编码（字典 dict_code=emission_subcategory 的 item_code）
     */
    @Column(name = "subcategory_code", nullable = false, unique = true, length = 50)
    private String subcategoryCode;

    /**
     * 排放数据小类名称（对应字典的 item_value，便于展示无需关联字典）
     */
    @Column(name = "subcategory_name", nullable = false, length = 100)
    private String subcategoryName;

    /**
     * 核算计量单位：进行数据汇总时统一的计量单位
     */
    @Column(name = "calculation_unit", nullable = false, length = 30)
    private String calculationUnit;

    /**
     * 碳排放报告计量单位：核算报告中展示的计量单位
     */
    @Column(name = "report_unit", nullable = false, length = 30)
    private String reportUnit;

    /**
     * 备注说明
     */
    @Column(name = "remark", length = 500)
    private String remark;

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
