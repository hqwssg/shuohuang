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
    
    @Column(name = "statistical_caliber", length = 100)
    private String statisticalCaliber;
    
    @Column(name = "emission_category", length = 100)
    private String emissionCategory;
    
    @Column(name = "emission_subcategory", length = 100)
    private String emissionSubcategory;
    
    @Column(name = "carbon_emission_factor", precision = 15, scale = 6)
    private BigDecimal carbonEmissionFactor;
    
    @Column(name = "carbon_emission_factor_description", columnDefinition = "TEXT")
    private String carbonEmissionFactorDescription;
    
    @Column(name = "data_source", length = 50)
    private String dataSource;
    
    @Column(name = "accounting_scenario", length = 100)
    private String accountingScenario;
    
    @Column(name = "energy_use", length = 100)
    private String energyUse;
    
    @Column(name = "is_cumulative", length = 10)
    private String isCumulative = "true";
    
    @Column(name = "is_mobile_source", length = 10)
    private String isMobileSource = "false";
    
    @Column(name = "measurement_unit", length = 50)
    private String measurementUnit;
    
    @Column(name = "data_source_system", length = 100)
    private String dataSourceSystem;
    
    @Column(name = "acquisition_method", columnDefinition = "TEXT")
    private String acquisitionMethod;
    
    @Column(name = "allocation_ratio", precision = 5, scale = 2)
    private BigDecimal allocationRatio = new BigDecimal("100.00");
    
    @Column(name = "has_sub_table")
    private Boolean hasSubTable = false;
    
    @Column(name = "error_constraint", precision = 5, scale = 2)
    private BigDecimal errorConstraint;
    
    @Column(name = "update_cycle", length = 50)
    private String updateCycle;
    
    @Column(name = "update_time", length = 50)
    private String updateTime;
    
    @Column(name = "task_config", columnDefinition = "TEXT")
    private String taskConfig;
    
    @Column(name = "collection_description", columnDefinition = "TEXT")
    private String collectionDescription;
    
    @Column(name = "equipment_code", length = 100)
    private String equipmentCode;
    
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
