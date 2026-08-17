package com.example.carbon.emission.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FossilFuelEmissionFactorDTO {
    
    private Long id;
    
    private String emissionFactorName;
    
    private String fuelType;
    
    private String source;
    
    private String unit;
    
    private BigDecimal lowerHeatingValue;
    
    private BigDecimal carbonContentPerUnitHeat;
    
    private BigDecimal fuelOxidationRate;
    
    private BigDecimal emissionFactor;
    
    private String factorUnit;
    
    private String description;
    
    private Long createdBy;
    
    private LocalDateTime createdAt;
    
    private Long updatedBy;
    
    private LocalDateTime updatedAt;
}