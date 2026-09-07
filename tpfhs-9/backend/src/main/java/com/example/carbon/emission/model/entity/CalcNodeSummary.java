package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 碳排放核算节点汇总实体类
 * <p>
 * 对应表 emission_calc_node_summary，存储每次核算任务中每个节点
 * 的能耗与碳排放汇总结果，包括直接值和小计值，便于按维度快速统计
 * 与报表展示。
 */
@Entity
@Table(name = "emission_calc_node_summary")
@Data
public class CalcNodeSummary {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 核算任务记录ID（关联 emission_calculation_template.id）
     */
    @Column(name = "calculation_template_id", nullable = false)
    private Long calculationTemplateId;

    /**
     * 核算节点ID（关联 emission_calculation_node.id）
     */
    @Column(name = "calculation_node_id", nullable = false)
    private Long calculationNodeId;

    /**
     * 源节点ID（关联 emission_node.id，即实际组织/设备节点）
     */
    @Column(name = "source_node_id")
    private Long sourceNodeId;

    /**
     * 父级核算节点ID（关联 emission_calculation_node.id，根节点为空）
     */
    @Column(name = "parent_calc_node_id")
    private Long parentCalcNodeId;

    /**
     * 节点层级：1-最顶层，2-第二层，以此类推
     */
    @Column(name = "node_level", nullable = false)
    private Integer nodeLevel = 1;

    /**
     * 是否叶子核算节点：1-是，0-否
     */
    @Column(name = "is_leaf_calc_node")
    private Integer isLeafCalcNode = 0;

    /**
     * 能源大类（对应字典 energy_category_l1）
     */
    @Column(name = "energy_category_l1", length = 50)
    private String energyCategoryL1;

    /**
     * 能源中类（对应字典 energy_category_l2）
     */
    @Column(name = "energy_category_l2", length = 50)
    private String energyCategoryL2;

    /**
     * 能源小类（对应字典 energy_category_l3）
     */
    @Column(name = "energy_category_l3", length = 50)
    private String energyCategoryL3;

    /**
     * 排放大类（scope1/scope2/scope3 等，对应字典 emission_category）
     */
    @Column(name = "emission_category", length = 100)
    private String emissionCategory;

    /**
     * 排放数据小类（对应字典 emission_subcategory，非空）
     */
    @Column(name = "emission_subcategory", nullable = false, length = 100)
    private String emissionSubcategory;

    /**
     * 直接能耗值（本节点自身产生的能耗，不含子节点）
     */
    @Column(name = "direct_energy_value")
    private BigDecimal directEnergyValue = BigDecimal.ZERO;

    /**
     * 直接碳排放量（本节点自身产生的碳排放，不含子节点）
     */
    @Column(name = "direct_carbon_emission")
    private BigDecimal directCarbonEmission = BigDecimal.ZERO;

    /**
     * 能耗小计值（本节点 + 所有子节点汇总后的能耗）
     */
    @Column(name = "subtotal_energy_value")
    private BigDecimal subtotalEnergyValue = BigDecimal.ZERO;

    /**
     * 碳排放小计值（本节点 + 所有子节点汇总后的碳排放）
     */
    @Column(name = "subtotal_carbon_emission")
    private BigDecimal subtotalCarbonEmission = BigDecimal.ZERO;

    /**
     * 核算单位编码（emission_unit_standard.unit_code）
     */
    @Column(name = "calculation_unit_code", length = 30)
    private String calculationUnitCode;

    /**
     * 碳排放因子（用于本次核算的实际因子值）
     */
    @Column(name = "carbon_emission_factor")
    private BigDecimal carbonEmissionFactor;

    /**
     * 汇总数据来源：1-核算节点计算，2-公式汇总，3-手动录入
     */
    @Column(name = "summary_source")
    private Integer summarySource;

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
        if (nodeLevel == null) {
            nodeLevel = 1;
        }
        if (isLeafCalcNode == null) {
            isLeafCalcNode = 0;
        }
        if (directEnergyValue == null) {
            directEnergyValue = BigDecimal.ZERO;
        }
        if (directCarbonEmission == null) {
            directCarbonEmission = BigDecimal.ZERO;
        }
        if (subtotalEnergyValue == null) {
            subtotalEnergyValue = BigDecimal.ZERO;
        }
        if (subtotalCarbonEmission == null) {
            subtotalCarbonEmission = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
