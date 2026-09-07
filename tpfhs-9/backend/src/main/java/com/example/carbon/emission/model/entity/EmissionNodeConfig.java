package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "emission_node_config")
@Data
public class EmissionNodeConfig {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "node_id", nullable = false)
    private Long nodeId;

    @Column(name = "emission_category", length = 100)
    private String emissionCategory;

    @Column(name = "emission_subcategory", length = 100)
    private String emissionSubcategory;

    @Column(name = "carbon_emission_factor", precision = 15, scale = 6)
    private BigDecimal carbonEmissionFactor;

    @Column(name = "carbon_emission_factor_description", columnDefinition = "TEXT")
    private String carbonEmissionFactorDescription;

    @Column(name = "collection_description", columnDefinition = "TEXT")
    private String collectionDescription;
    
    @Column(name = "equipment_code", length = 100)
    private String equipmentCode;

    /**
     * 采集点类型：1-电力表，2-化石燃料，3-外购热能
     */
    @Column(name = "collection_point_type")
    private Integer collectionPointType;

    /**
     * 采集点ID，根据采集点类型指向对应采集点表的主键
     * （emission_meter_info / emission_fossil_fuel_meter_info / emission_purchased_heat_meter_info）
     */
    @Column(name = "collection_point_id")
    private Long collectionPointId;

    /**
     * 采集点状态：1-启用，2-禁用，3-被删除（找不到对应记录），4-离线
     */
    @Column(name = "collection_point_status")
    private Integer collectionPointStatus = 1;

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
