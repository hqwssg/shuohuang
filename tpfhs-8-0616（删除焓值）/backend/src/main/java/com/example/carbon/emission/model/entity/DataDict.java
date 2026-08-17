package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "emission_data_dict")
@Data
public class DataDict {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "dict_code", nullable = false, unique = true, length = 50)
    private String dictCode;
    
    @Column(name = "dict_name", nullable = false, length = 100)
    private String dictName;
    
    @Column(name = "description", length = 500)
    private String description;
    
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