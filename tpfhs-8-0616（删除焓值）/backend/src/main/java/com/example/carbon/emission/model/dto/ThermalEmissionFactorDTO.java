package com.example.carbon.emission.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ThermalEmissionFactorDTO {

    private Long id;

    private String emissionFactorName;

    private BigDecimal emissionFactor;

    private String unit;

    private String source;

    private String description;

    private Long createdBy;

    private LocalDateTime createdAt;

    private Long updatedBy;

    private LocalDateTime updatedAt;
}
