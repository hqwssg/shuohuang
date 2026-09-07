package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * 电表信息实体类
 * 用于管理电表的详细配置和属性
 */
@Entity
@Table(name = "emission_meter_info")
@Data
public class MeterInfo {
    
    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 电表名称
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    /**
     * 拼音首字母编码，用于快速搜索
     */
    @Column(name = "pinyin_code", length = 50)
    private String pinyinCode;
    
    /**
     * 所属集中器ID
     */
    @Column(name = "point_id", nullable = false)
    private Long pointId;
    
    /**
     * 电表类型
     */
    @Column(name = "meter_type", length = 20)
    private String meterType;
    
    /**
     * 电表型号
     */
    @Column(name = "meter_model", length = 20)
    private String meterModel;
    
    /**
     * 电表地址
     */
    @Column(name = "meter_address", length = 20)
    private String meterAddress;
    
    /**
     * 用途描述
     */
    @Column(name = "purpose_description", length = 500)
    private String purposeDescription;
    
    /**
     * 上级电表ID，缺省为0表示无上级
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
     * 用电分类：外购电力、新能源发电（自发自用）
     */
    @Column(name = "emission_subcategory", length = 50)
    private String emissionSubcategory = "外购电力";

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
     * 肃分、原分、机辆、肃分(扣除下属)、原分(扣除下属)、机辆(扣除下属)、其他
     */
    @Column(name = "energy_allocation", length = 60)
    private String energyAllocation;
    
    /**
     * 用电分类
     * 生成用电、生成辅助用电、混合
     */
    @Column(name = "power_category", length = 20)
    private String powerCategory;
    
    /**
     * 自动抄表接口描述（JSON格式）
     * 存储调用第三方抄表接口的完整配置，包括URL、请求参数、数据映射等
     */
    @Column(name = "auto_meter_reading_config", columnDefinition = "TEXT")
    private String autoMeterReadingConfig;
    
    /**
     * 抄表方式
     * 1-自动抄表，0-人工抄表
     */
    @Column(name = "meter_reading_method")
    private Integer meterReadingMethod = 1;

    /**
     * 总表与分表关系
     * 1-总表计数等于各下属分表计数之和，2-总表计数不等于各下属分表计数之和
     */
    @Column(name = "parent_child_relationship")
    private Integer parentChildRelationship = 1;

    /**
     * 是否虚拟电表
     * 1-是虚拟电表，0-不是虚拟电表
     */
    @Column(name = "is_virtual_meter")
    @JsonProperty("isVirtualMeter")
    private Integer isVirtualMeter = 0;

    /**
     * 是否分摊子电表
     * 1-是，0-否
     */
    @Column(name = "is_allocation_child")
    @JsonProperty("isAllocationChild")
    private Integer isAllocationChild = 0;

    /**
     * 分摊比例，缺省值1.0
     */
    @Column(name = "allocation_ratio")
    private java.math.BigDecimal allocationRatio = java.math.BigDecimal.ONE;
    
    /**
     * 排序顺序，数值越小越靠前
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
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
        if (parentChildRelationship == null) {
            parentChildRelationship = 1;
        }
        if (isVirtualMeter == null) {
            isVirtualMeter = 0;
        }
        if (isAllocationChild == null) {
            isAllocationChild = 0;
        }
        if (allocationRatio == null) {
            allocationRatio = java.math.BigDecimal.ONE;
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
