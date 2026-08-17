package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 排放节点信息实体类
 * 
 * 用于存储核算子节点(typeId=2)的详细信息，包括节点编码、简称、核算标识、类型及边界说明等。
 */
@Entity
@Table(name = "emission_node_info")
@Data
public class EmissionNodeInfo {
    
    /** 主键ID */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /** 节点ID，关联emission_node表 */
    @Column(name = "node_id", nullable = false, unique = true)
    private Long nodeId;
    
    /** 节点编码 */
    @Column(name = "node_code", length = 50)
    private String nodeCode;
    
    /** 
     * 节点名称简称
     * 用于自动生成子节点名称的前缀，如：HQ-BRANCH01-子节点名称
     */
    @Column(name = "short_name", length = 50)
    private String shortName;
    
    /** 是否纳入碳排放核算 */
    @Column(name = "include_in_calculation")
    private Boolean includeInCalculation = true;
    
    @Column(name = "node_category", length = 50)
    private String nodeCategory;
    
    @Column(name = "unit_description", columnDefinition = "TEXT")
    private String unitDescription;
    
    @Column(name = "org_boundary_description", columnDefinition = "TEXT")
    private String orgBoundaryDescription;
    
    @Column(name = "operation_boundary_description", columnDefinition = "TEXT")
    private String operationBoundaryDescription;
    
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
        if (includeInCalculation == null) {
            includeInCalculation = true;
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}