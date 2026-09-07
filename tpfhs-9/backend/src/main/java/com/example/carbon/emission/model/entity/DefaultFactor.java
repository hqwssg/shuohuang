package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 系统缺省碳排放因子设置实体类
 * <p>
 * 按排放数据小类（emission_subcategory）设置系统默认使用的碳排放因子。
 * factor_source 标记因子来源库（电力/化石燃料/热力/固废焚烧/废水处理），
 * factor_id 为所选因子在对应因子库表中的主键；
 * factor_name/factor_value/factor_unit/factor_description 为选择时的快照，
 * 因子库后续修改不影响已选快照，保证展示与历史稳定。
 */
@Entity
@Table(name = "emission_default_factor",
        uniqueConstraints = @UniqueConstraint(name = "uk_template_subcategory",
                columnNames = {"template_id", "subcategory_code"}))
@Data
public class DefaultFactor {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属因子模版ID（NULL=系统缺省，非NULL=模版级缺省因子）
     */
    @Column(name = "template_id", insertable = true, updatable = true)
    private Long templateId;

    /**
     * 排放数据小类编码（字典 dict_code=emission_subcategory 的 item_code，如 PE_PF/FF_D）
     */
    @Column(name = "subcategory_code", nullable = false, length = 50)
    private String subcategoryCode;

    /**
     * 排放数据小类名称（冗余 item_value 便于展示）
     */
    @Column(name = "subcategory_name", length = 100)
    private String subcategoryName;

    /**
     * 因子库来源：ELECTRICITY-电力、FOSSIL-化石燃料、THERMAL-热力、
     * WASTE_INCINERATION-固废焚烧、WASTEWATER-废水处理
     */
    @Column(name = "factor_source", nullable = false, length = 30)
    private String factorSource;

    /**
     * 所选因子在对应因子库表中的主键ID
     */
    @Column(name = "factor_id")
    private Long factorId;

    /**
     * 所选因子名称（快照）
     */
    @Column(name = "factor_name", length = 200)
    private String factorName;

    /**
     * 所选因子值（快照；热水/蒸汽为经温度计算后的折算值）
     */
    @Column(name = "factor_value")
    private BigDecimal factorValue;

    /**
     * 所选因子单位（快照）
     */
    @Column(name = "factor_unit", length = 50)
    private String factorUnit;

    /**
     * 所选因子说明（快照）
     */
    @Column(name = "factor_description", length = 1000)
    private String factorDescription;

    /**
     * 备注说明
     */
    @Column(name = "remark", length = 500)
    private String remark;

    /**
     * 状态：1-启用，0-停用
     */
    @Column(name = "status")
    private Integer status = 1;

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
        if (status == null) {
            status = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
