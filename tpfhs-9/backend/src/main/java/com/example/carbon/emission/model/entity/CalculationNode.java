package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 碳排放核算节点实体类
 *
 * 快照本次核算任务涉及的节点树结构。每条记录对应一个源节点
 * （emission_node），保留节点名称、类型、父节点等树结构信息，
 * 便于事后追溯本次核算所基于的节点层级。核算节点数据表
 * （emission_calculation_node_data）通过 calculation_node_id
 * 外键关联本表 id。
 */
@Entity
@Table(name = "emission_calculation_node")
@Data
public class CalculationNode {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 对应 emission_calculation_template.id（本次核算任务记录ID）
     */
    @Column(name = "calculation_template_id", nullable = false)
    private Long calculationTemplateId;

    /**
     * 对应的节点ID，关联 emission_node.id
     */
    @Column(name = "node_id", nullable = false)
    private Long nodeId;

    /**
     * 节点名称
     */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * 节点类型ID，关联 emission_node_type 表
     */
    @Column(name = "type_id", nullable = false)
    private Integer typeId;

    /**
     * 父节点ID（对应本表中的 id，用于核算节点树层级）
     */
    @Column(name = "parent_id")
    private Long parentId;

    /**
     * 节点分类：当 type_id 对应核算子节点类型时，取值与
     * emission_node_info.node_category 一致（总公司/分公司/站点/区域）
     */
    @Column(name = "node_category", length = 50)
    private String nodeCategory;

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
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
