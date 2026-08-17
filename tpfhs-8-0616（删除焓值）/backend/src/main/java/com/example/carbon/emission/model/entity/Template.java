package com.example.carbon.emission.model.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "emission_template")
@Data
public class Template {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 200)
    private String name;
    
    @Column(name = "description", length = 500)
    private String description;
    
    @Column(name = "created_by", nullable = false)
    private Long createdBy;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_by")
    private Long updatedBy;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "version")
    private Integer version = 1;
    
    @Column(name = "enabled")
    private Boolean enabled = true;
    
    @Column(name = "task_config", columnDefinition = "TEXT")
    private String taskConfig;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        version = 1;
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        if (version == null) {
            version = 1;
        } else {
            version++;
        }
    }
}