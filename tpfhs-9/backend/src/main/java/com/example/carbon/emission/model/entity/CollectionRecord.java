package com.example.carbon.emission.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采集记录实体类
 *
 * 针对 emission_meter_info（电力表）、emission_fossil_fuel_meter_info（化石燃料）、
 * emission_purchased_heat_meter_info（外购热能）三张表记录的采集点，
 * 每次执行数据采集时生成一条记录，记录本次读取的数值、采集时间、
 * 采集状态、最后一次读取时间，以及根据采集点计费周期配置
 * （billing_cycle_unit、billing_cycle_start_date、billing_cycle_length）
 * 计算得到的本计费周期起止日期等信息。
 */
@Entity
@Table(name = "emission_collection_record")
@Data
public class CollectionRecord {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 采集点类型：1-电力表，2-化石燃料，3-外购热能
     * 决定 collectionPointId 指向哪张采集点表
     */
    @Column(name = "collection_point_type", nullable = false)
    private Integer collectionPointType;

    /**
     * 采集点ID，关联 emission_meter_info /
     * emission_fossil_fuel_meter_info /
     * emission_purchased_heat_meter_info 表的主键
     * （由 collectionPointType 决定指向）
     */
    @Column(name = "collection_point_id", nullable = false)
    private Long collectionPointId;

    /**
     * 本次采集读取的数值
     */
    @Column(name = "reading_value", precision = 18, scale = 6)
    private BigDecimal readingValue;

    /**
     * 本次采集时间：指系统从计量表直接采集数据的时刻。
     * 该数据可能已由电力系统等外部系统在预设时间完成读取并存储。
     */
    @Column(name = "collection_time")
    private LocalDateTime collectionTime;

    /**
     * 本次采集数据状态：0-还未执行读取操作，1-读取成功，2-读取失败，3-多次读取失败后取消
     */
    @Column(name = "collection_status")
    private Integer collectionStatus = 0;

    /**
     * 最后一次读取时间：指系统从外部系统获取其已存储数据的时刻
     */
    @Column(name = "last_fetch_time")
    private LocalDateTime lastFetchTime;

    /**
     * 采集点名称，对应采集点表（emission_meter_info 等）的 name 字段
     */
    @Column(name = "collection_point_name", length = 100)
    private String collectionPointName;

    /**
     * 能耗品种小类
     * 电力表为用电分类（如外购电力）；化石燃料为燃料种类；外购热能为热力种类
     */
    @Column(name = "emission_subcategory", length = 50)
    private String emissionSubcategory;

    /**
     * 计量单位
     */
    @Column(name = "measurement_unit", length = 50)
    private String measurementUnit;

    /**
     * 抄表方式：1-自动抄表，0-人工录入
     */
    @Column(name = "meter_reading_method")
    private Integer meterReadingMethod = 1;

    /**
     * 是否累加量：1-是，0-否
     */
    @Column(name = "is_cumulative")
    @JsonProperty("isCumulative")
    private Integer isCumulative = 0;

    /**
     * 本计费周期起始日期
     * 根据采集点 billing_cycle_unit、billing_cycle_start_date、
     * billing_cycle_length 三个字段计算得到，仅保留日期信息
     */
    @Column(name = "billing_cycle_start_date")
    private LocalDate billingCycleStartDate;

    /**
     * 本计费周期截止日期
     * 根据采集点 billing_cycle_unit、billing_cycle_start_date、
     * billing_cycle_length 三个字段计算得到，仅保留日期信息
     */
    @Column(name = "billing_cycle_end_date")
    private LocalDate billingCycleEndDate;

    /**
     * 本条记录插入表的时间（系统时间戳）
     */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    /**
     * 实体创建前自动填充创建时间
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (collectionStatus == null) {
            collectionStatus = 0;
        }
        if (meterReadingMethod == null) {
            meterReadingMethod = 1;
        }
        if (isCumulative == null) {
            isCumulative = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
