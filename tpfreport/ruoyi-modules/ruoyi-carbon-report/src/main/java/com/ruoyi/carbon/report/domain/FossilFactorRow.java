package com.ruoyi.carbon.report.domain;

import java.math.BigDecimal;

public class FossilFactorRow
{
    private Long id;
    private String fuelType;
    private String unit;
    private BigDecimal lowerHeatingValue;
    private BigDecimal carbonContentPerUnitHeat;
    private BigDecimal fuelOxidationRate;
    private BigDecimal emissionFactor;
    private String factorUnit;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getLowerHeatingValue() { return lowerHeatingValue; }
    public void setLowerHeatingValue(BigDecimal lowerHeatingValue) { this.lowerHeatingValue = lowerHeatingValue; }
    public BigDecimal getCarbonContentPerUnitHeat() { return carbonContentPerUnitHeat; }
    public void setCarbonContentPerUnitHeat(BigDecimal carbonContentPerUnitHeat) { this.carbonContentPerUnitHeat = carbonContentPerUnitHeat; }
    public BigDecimal getFuelOxidationRate() { return fuelOxidationRate; }
    public void setFuelOxidationRate(BigDecimal fuelOxidationRate) { this.fuelOxidationRate = fuelOxidationRate; }
    public BigDecimal getEmissionFactor() { return emissionFactor; }
    public void setEmissionFactor(BigDecimal emissionFactor) { this.emissionFactor = emissionFactor; }
    public String getFactorUnit() { return factorUnit; }
    public void setFactorUnit(String factorUnit) { this.factorUnit = factorUnit; }
}
