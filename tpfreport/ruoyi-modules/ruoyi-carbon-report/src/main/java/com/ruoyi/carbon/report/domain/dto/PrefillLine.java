package com.ruoyi.carbon.report.domain.dto;

import java.math.BigDecimal;

public class PrefillLine
{
    private String dataOrigin;
    private String sourceTable;
    private String sourceId;
    private String subcategory;
    private String fuelType;
    private String facility;
    private BigDecimal sourceValue;
    private String sourceUnit;
    private BigDecimal normalizedValue;
    private String normalizedUnit;
    private BigDecimal carbonEmission;
    private String sourceHash;
    private String overrideReason;

    public PrefillLine copy()
    {
        PrefillLine line = new PrefillLine();
        line.dataOrigin = dataOrigin;
        line.sourceTable = sourceTable;
        line.sourceId = sourceId;
        line.subcategory = subcategory;
        line.fuelType = fuelType;
        line.facility = facility;
        line.sourceValue = sourceValue;
        line.sourceUnit = sourceUnit;
        line.normalizedValue = normalizedValue;
        line.normalizedUnit = normalizedUnit;
        line.carbonEmission = carbonEmission;
        line.sourceHash = sourceHash;
        line.overrideReason = overrideReason;
        return line;
    }

    public String getDataOrigin() { return dataOrigin; }
    public void setDataOrigin(String dataOrigin) { this.dataOrigin = dataOrigin; }
    public String getSourceTable() { return sourceTable; }
    public void setSourceTable(String sourceTable) { this.sourceTable = sourceTable; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getSubcategory() { return subcategory; }
    public void setSubcategory(String subcategory) { this.subcategory = subcategory; }
    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }
    public String getFacility() { return facility; }
    public void setFacility(String facility) { this.facility = facility; }
    public BigDecimal getSourceValue() { return sourceValue; }
    public void setSourceValue(BigDecimal sourceValue) { this.sourceValue = sourceValue; }
    public String getSourceUnit() { return sourceUnit; }
    public void setSourceUnit(String sourceUnit) { this.sourceUnit = sourceUnit; }
    public BigDecimal getNormalizedValue() { return normalizedValue; }
    public void setNormalizedValue(BigDecimal normalizedValue) { this.normalizedValue = normalizedValue; }
    public String getNormalizedUnit() { return normalizedUnit; }
    public void setNormalizedUnit(String normalizedUnit) { this.normalizedUnit = normalizedUnit; }
    public BigDecimal getCarbonEmission() { return carbonEmission; }
    public void setCarbonEmission(BigDecimal carbonEmission) { this.carbonEmission = carbonEmission; }
    public String getSourceHash() { return sourceHash; }
    public void setSourceHash(String sourceHash) { this.sourceHash = sourceHash; }
    public String getOverrideReason() { return overrideReason; }
    public void setOverrideReason(String overrideReason) { this.overrideReason = overrideReason; }
}
