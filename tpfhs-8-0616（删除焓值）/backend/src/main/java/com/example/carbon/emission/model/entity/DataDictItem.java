package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "emission_data_dict_item")
@Data
public class DataDictItem {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "dict_id", nullable = false)
    private Long dictId;
    
    @Column(name = "item_code", nullable = false, length = 100)
    private String itemCode;
    
    @Column(name = "item_value", nullable = false, length = 200)
    private String itemValue;
    
    @Column(name = "parent_code", length = 100)
    private String parentCode;
    
    @Column(name = "sort_order")
    private Integer sortOrder = 0;
    
    @Column(name = "status")
    private Integer status = 1;
    
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