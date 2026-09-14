package com.ruoyi.carbon.report.domain;

import java.math.BigDecimal;

public class DefaultFactorRow
{
    private Long id;
    private Long templateId;
    private String subcategoryCode;
    private String subcategoryName;
    private String factorSource;
    private Long factorId;
    private String factorName;
    private BigDecimal factorValue;
    private String factorUnit;
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getSubcategoryCode() { return subcategoryCode; }
    public void setSubcategoryCode(String subcategoryCode) { this.subcategoryCode = subcategoryCode; }
    public String getSubcategoryName() { return subcategoryName; }
    public void setSubcategoryName(String subcategoryName) { this.subcategoryName = subcategoryName; }
    public String getFactorSource() { return factorSource; }
    public void setFactorSource(String factorSource) { this.factorSource = factorSource; }
    public Long getFactorId() { return factorId; }
    public void setFactorId(Long factorId) { this.factorId = factorId; }
    public String getFactorName() { return factorName; }
    public void setFactorName(String factorName) { this.factorName = factorName; }
    public BigDecimal getFactorValue() { return factorValue; }
    public void setFactorValue(BigDecimal factorValue) { this.factorValue = factorValue; }
    public String getFactorUnit() { return factorUnit; }
    public void setFactorUnit(String factorUnit) { this.factorUnit = factorUnit; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
