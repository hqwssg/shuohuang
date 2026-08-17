package com.example.carbon.emission.model.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class NodeConfigDTO {
    
    private Long nodeId;
    
    private Long updatedBy;
    
    private String statisticalCaliber;
    
    private String emissionCategory;
    
    private String emissionSubcategory;
    
    private BigDecimal carbonEmissionFactor;
    
    private String carbonEmissionFactorDescription;
    
    private String dataSource;
    
    private String accountingScenario;
    
    private String energyUse;
    
    private String isCumulative;
    
    private String isMobileSource;
    
    private String measurementUnit;
    
    private String dataSourceSystem;
    
    private String acquisitionMethod;
    
    private BigDecimal allocationRatio;
    
    private Boolean hasSubTable;
    
    private BigDecimal errorConstraint;
    
    private String updateCycle;
    
    private String updateTime;
    
    private Object taskConfig;
    
    private String collectionDescription;
    
    private String equipmentCode;
}
