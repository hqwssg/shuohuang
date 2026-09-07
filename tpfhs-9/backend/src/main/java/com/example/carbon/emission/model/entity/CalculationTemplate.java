package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 碳排放核算模版实体类
 *
 * 每次执行碳排放核算任务时生成一条记录，记录本次核算对应的
 * 模版、核算周期起止日期、执行状态、失败错误代码、发起人
 * 等信息。核算节点表（emission_calculation_node）通过
 * calculation_template_id 外键关联本表 id。
 */
@Entity
@Table(name = "emission_calculation_template")
@Data
public class CalculationTemplate {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 需要核实的碳排放模版ID，外键关联 emission_template.id
     */
    @Column(name = "template_id", nullable = false)
    private Long templateId;

    /**
     * 核算周期起始日期（只保留日期部分）
     */
    @Column(name = "calculation_cycle_start_date")
    private LocalDate calculationCycleStartDate;

    /**
     * 核算周期截止日期（只保留日期部分）
     */
    @Column(name = "calculation_cycle_end_date")
    private LocalDate calculationCycleEndDate;

    /**
     * 核算开始执行时间
     */
    @Column(name = "calculation_start_time")
    private LocalDateTime calculationStartTime;

    /**
     * 核算结束时间
     */
    @Column(name = "calculation_end_time")
    private LocalDateTime calculationEndTime;

    /**
     * 核算状态：0-还未开始执行，1-正在执行，2-执行成功完成，3-执行出错失败
     */
    @Column(name = "status")
    private Integer status = 0;

    /**
     * 执行失败错误代码：0-成功（没有出错）
     */
    @Column(name = "error_code")
    private Integer errorCode = 0;

    /**
     * 任务发起者：1-系统自动发起，2-人工手动发起
     */
    @Column(name = "task_initiator")
    private Integer taskInitiator = 1;

    /**
     * 发起人ID：task_initiator=2 时填写发起人ID，否则为0
     */
    @Column(name = "initiator_id")
    private Long initiatorId = 0L;

    /**
     * 发起人姓名
     */
    @Column(name = "initiator_name", length = 100)
    private String initiatorName;

    /**
     * 该记录创建时间
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
     * 实体创建前自动填充创建时间和默认值
     */
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
        if (status == null) {
            status = 0;
        }
        if (errorCode == null) {
            errorCode = 0;
        }
        if (taskInitiator == null) {
            taskInitiator = 1;
        }
        if (initiatorId == null) {
            initiatorId = 0L;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
