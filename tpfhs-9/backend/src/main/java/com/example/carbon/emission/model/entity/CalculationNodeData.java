package com.example.carbon.emission.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 碳排放采集节点数据实体类
 *
 * 记录各采集节点本次核算的能耗与碳排放计算结果，包括从
 * emission_collection_record 统计得出的能耗计量值、按碳排放因子
 * 折算的碳排放量、数据状态（完整/无数据/不完整/超范围/不完整且
 * 超范围）、数据缺失说明，以及对超限/缺失情况进行插值截断修正
 * 后的能耗计量值和碳排放量。
 *
 * 字段来源说明：
 * - 当节点为采集节点（type_id=3）时，emission_subcategory、
 *   is_cumulative、is_mobile_source、measurement_unit、
 *   energy_category_l1/l2/l3、energy_use_category 等字段由
 *   对应采集点表记录提供。
 * - energy_measurement_value 由 emission_collection_record 统计得出。
 */
@Entity
@Table(name = "emission_collection_node_data")
@Data
public class CalculationNodeData {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 对应 emission_calculation_node.id（核算节点记录ID）
     */
    @Column(name = "calculation_node_id", nullable = false)
    private Long calculationNodeId;

    /**
     * 采集点类型：1-电力表，2-化石燃料，3-外购热能
     * 仅采集节点有值，复制自 emission_node_config.collection_point_type
     */
    @Column(name = "collection_point_type")
    private Integer collectionPointType;

    /**
     * 采集点ID：仅当节点为采集节点(type_id=3)时有值，
     * 指向对应采集点表（emission_meter_info / emission_fossil_fuel_meter_info /
     * emission_purchased_heat_meter_info）主键
     */
    @Column(name = "collection_point_id")
    private Long collectionPointId;

    /**
     * 能耗品种大类
     */
    @Column(name = "emission_category", length = 100)
    private String emissionCategory;

    /**
     * 能耗品种小类：采集节点时由对应采集点表记录提供
     */
    @Column(name = "emission_subcategory", length = 100)
    private String emissionSubcategory;

    /**
     * 碳排放因子
     */
    @Column(name = "carbon_emission_factor", precision = 15, scale = 6)
    private BigDecimal carbonEmissionFactor;

    /**
     * 是否累加量：1-是，0-否
     * 采集节点时由对应采集点表记录提供
     */
    @Column(name = "is_cumulative")
    @JsonProperty("isCumulative")
    private Integer isCumulative = 0;

    /**
     * 是否移动源：1-是，0-否
     * 采集节点时由对应采集点表记录提供
     */
    @Column(name = "is_mobile_source")
    @JsonProperty("isMobileSource")
    private Integer isMobileSource = 0;

    /**
     * 计量单位：采集节点时由对应采集点表记录提供
     */
    @Column(name = "measurement_unit", length = 50)
    private String measurementUnit;

    /**
     * 能耗一级分类编码：采集节点时由对应采集点表记录提供
     */
    @Column(name = "energy_category_l1", length = 50)
    private String energyCategoryL1;

    /**
     * 能耗二级分类编码：采集节点时由对应采集点表记录提供
     */
    @Column(name = "energy_category_l2", length = 50)
    private String energyCategoryL2;

    /**
     * 能耗三级分类编码：采集节点时由对应采集点表记录提供
     */
    @Column(name = "energy_category_l3", length = 50)
    private String energyCategoryL3;

    /**
     * 能耗用途分类：采集节点时由对应采集点表记录提供
     */
    @Column(name = "energy_use_category", length = 100)
    private String energyUseCategory;

    /**
     * 能耗计量值：从 emission_collection_record 统计得出的
     * 核算周期内能耗值
     */
    @Column(name = "energy_measurement_value", precision = 18, scale = 6)
    private BigDecimal energyMeasurementValue;

    /**
     * 折算的碳排放量
     */
    @Column(name = "carbon_emission", precision = 18, scale = 6)
    private BigDecimal carbonEmission;

    /**
     * 经过计算修正后的能耗计量值
     */
    @Column(name = "adjusted_energy_value", precision = 18, scale = 6)
    private BigDecimal adjustedEnergyValue;

    /**
     * 经过计算修正后的碳排放量
     */
    @Column(name = "adjusted_carbon_emission", precision = 18, scale = 6)
    private BigDecimal adjustedCarbonEmission;

    /**
     * 总排放核算方式：1-缺失情况按修正后的计量值计算，
     * 2-缺失情况按计量值计算
     */
    @Column(name = "total_emission_calc_method")
    private Integer totalEmissionCalcMethod = 1;

    /**
     * 采集记录的起始日期：所选择采集点记录中记录的采集起始日期
     */
    @Column(name = "collection_record_start_date")
    private LocalDate collectionRecordStartDate;

    /**
     * 采集记录的截止日期：所选择采集点记录中记录的采集截止日期
     */
    @Column(name = "collection_record_end_date")
    private LocalDate collectionRecordEndDate;

    /**
     * 数据状态：1-完整，2-没有数据，3-数据不完整，
     * 4-数据超范围，5-数据不完整且超范围
     */
    @Column(name = "data_status")
    private Integer dataStatus;

    /**
     * 数据缺失说明：data_status 不为1 时给出缺失/超限的具体说明
     */
    @Column(name = "data_missing_description", length = 500)
    private String dataMissingDescription;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 实体创建前自动填充默认值
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (isCumulative == null) {
            isCumulative = 0;
        }
        if (isMobileSource == null) {
            isMobileSource = 0;
        }
        if (totalEmissionCalcMethod == null) {
            totalEmissionCalcMethod = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
