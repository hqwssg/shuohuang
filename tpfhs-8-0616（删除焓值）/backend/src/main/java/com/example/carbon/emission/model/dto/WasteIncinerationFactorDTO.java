package com.example.carbon.emission.model.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WasteIncinerationFactorDTO {

    private Long id;

    private String emissionFactorName;

    private String wasteType;

    private BigDecimal ccw;

    private BigDecimal fcf;

    private BigDecimal ce;

    private BigDecimal emissionFactor;

    private String unit;

    private String source;

    private String description;

    private Long createdBy;

    private LocalDateTime createdAt;

    private Long updatedBy;

    private LocalDateTime updatedAt;
}
