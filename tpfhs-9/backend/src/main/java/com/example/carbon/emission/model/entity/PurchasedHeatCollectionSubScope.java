package com.example.carbon.emission.model.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 外购热能采集细分范围实体类
 * 用于管理外购热能采集的二级分类
 */
@Entity
@Table(name = "emission_purchased_heat_collection_sub_scope")
@Data
public class PurchasedHeatCollectionSubScope {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 细分范围名称
     */
    @JsonProperty("name")
    @Column(name = "sub_scope_name", nullable = false, unique = true, length = 100)
    private String subScopeName;

    /**
     * 拼音首字母编码，用于快速搜索
     */
    @Column(name = "pinyin_code", length = 30)
    private String pinyinCode;

    /**
     * 外购热能采集范围ID
     */
    @Column(name = "collection_scope_id", nullable = false)
    private Long collectionScopeId;

    /**
     * 排序顺序，数值越小越靠前
     */
    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    /**
     * 描述信息
     */
    @Column(name = "description", length = 500)
    private String description;

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
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }

    /**
     * 实体更新前自动更新更新时间
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}