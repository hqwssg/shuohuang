package com.example.carbon.emission.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ElectricityCarbonEmissionFactorDTO {
    
    private Long id;
    
    private String factorName;
    
    private BigDecimal factorValue;
    
    private String unit;
    
    private String description;
    
    private Long createdBy;
    
    private LocalDateTime createdAt;
    
    private Long updatedBy;
    
    private LocalDateTime updatedAt;
}