package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 化石燃料计量表信息实体类
 * 用于管理化石燃料相关的计量表详细配置
 */
@Entity
@Table(name = "emission_fossil_fuel_meter_info")
@Data
public class FossilFuelMeterInfo {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 计量表名称
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 拼音首字母编码，用于快速搜索
     */
    @Column(name = "pinyin_code", length = 50)
    private String pinyinCode;

    /**
     * 所属子范围ID
     */
    @Column(name = "sub_scope_id", nullable = false)
    private Long subScopeId;

    /**
     * 燃料类型
     */
    @Column(name = "emission_subcategory", length = 50)
    private String fuelType;

    /**
     * 计量表型号
     */
    @Column(name = "meter_model", length = 30)
    private String meterModel;

    /**
     * 用途描述
     */
    @Column(name = "purpose_description", length = 500)
    private String purposeDescription;

    /**
     * 上级计量表ID，缺省为0表示无上级
     */
    @Column(name = "parent_meter_id")
    private Long parentMeterId = 0L;

    /**
     * 是否累加量：1-是，0-否
     */
    @Column(name = "is_cumulative")
    @JsonProperty("isCumulative")
    private Integer isCumulative = 0;

    /**
     * 是否移动源：1-是，0-否
     */
    @Column(name = "is_mobile_source")
    @JsonProperty("isMobileSource")
    private Integer isMobileSource = 0;

    /**
     * 计量单位
     */
    @Column(name = "measurement_unit", length = 50)
    private String measurementUnit;

    /**
     * 数据来源系统
     */
    @Column(name = "data_source_system", length = 100)
    private String dataSourceSystem;

    /**
     * 非累加量计费周期单位：1-周，2-月，3-季度，4-年
     */
    @Column(name = "billing_cycle_unit")
    private Integer billingCycleUnit = 2;

    /**
     * 非累加量计费周期起始日期偏移量
     */
    @Column(name = "billing_cycle_start_date")
    private Integer billingCycleStartDate = 0;

    /**
     * 非累加量计费周期长度（按月计）
     */
    @Column(name = "billing_cycle_length")
    private Integer billingCycleLength = 1;

    /**
     * 能耗一级分类编码，关联emission_energy_category表
     */
    @Column(name = "energy_category_l1", length = 50)
    private String energyCategoryL1;

    /**
     * 能耗二级分类编码，关联emission_energy_category表
     */
    @Column(name = "energy_category_l2", length = 50)
    private String energyCategoryL2;

    /**
     * 能耗三级分类编码，关联emission_energy_category表
     */
    @Column(name = "energy_category_l3", length = 50)
    private String energyCategoryL3;

    /**
     * 能耗用途分类
     */
    @Column(name = "energy_use_category", length = 100)
    private String energyUseCategory;

    /**
     * 能耗数据划拨方式
     */
    @Column(name = "energy_allocation", length = 20)
    private String energyAllocation;

    /**
     * 抄表方式
     * 1-自动抄表，0-人工抄表
     */
    @Column(name = "meter_reading_method")
    private Integer meterReadingMethod = 1;

    /**
     * 排序顺序，数值越小越靠前
     */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /**
     * 状态
     * 1-启用，0-停用
     */
    @Column(name = "status")
    private Integer status = 1;

    /**
     * 上次采集时间：完成对该采集点数据采集后记录的最近一次采集时间
     * （对应 emission_collection_record.collection_time 字段的值）
     */
    @Column(name = "last_collection_time")
    private LocalDateTime lastCollectionTime;

    /**
     * 启用时间：当启用该节点后自动记录
     * 由 @PreUpdate 在 status 从 0→1 切换时自动写入
     */
    @Column(name = "enable_time")
    private LocalDateTime enableTime;

    /**
     * 停用时间：当停用该节点后自动记录
     * 由 @PreUpdate 在 status 从 1→0 切换时自动写入
     */
    @Column(name = "disable_time")
    private LocalDateTime disableTime;

    /**
     * 加载到持久化上下文时的原始 status 值（非持久化字段）
     * 用于 @PreUpdate 比对 status 是否变化，进而决定是否记录启停时间
     */
    @Transient
    private Integer originalStatus;

    /**
     * 能源用途
     */
    @Column(name = "energy_use", length = 20)
    private String energyUse;

    /**
     * 自动抄表接口配置（JSON格式）
     */
    @Column(name = "auto_meter_reading_config", columnDefinition = "TEXT")
    private String autoMeterReadingConfig;

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

    /**
     * 实体创建前自动填充创建时间和更新时间
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = 1;
        }
        if (meterReadingMethod == null) {
            meterReadingMethod = 1;
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
        if (parentMeterId == null) {
            parentMeterId = 0L;
        }
    }

    /**
     * 实体从数据库加载后保存原始 status 值
     * 用于 @PreUpdate 比对 status 是否变化，仅当 status 真正切换时
     * 才写入 enable_time/disable_time，避免无关更新误改时间
     */
    @PostLoad
    protected void onLoad() {
        originalStatus = status;
    }

    /**
     * 实体更新前自动更新更新时间，并按 status 变化自动记录启停时间
     * <p>
     * 仅当 originalStatus 非空（即通过 @PostLoad 进入持久化上下文的实体，
     * 适用于已有记录的更新路径）且 status 与 originalStatus 不一致时
     * 才认为发生了切换：
     * <ul>
     *   <li>0→1：记录 enable_time（启用时间）</li>
     *   <li>1→0：记录 disable_time（停用时间）</li>
     * </ul>
     * 初始创建（status 默认 1）走 @PrePersist 路径，不触发 @PreUpdate，
     * 故不会误记 enable_time
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (originalStatus != null && status != null && !status.equals(originalStatus)) {
            if (status == 1) {
                enableTime = LocalDateTime.now();
            } else if (status == 0) {
                disableTime = LocalDateTime.now();
            }
        }
        originalStatus = status;
    }
}