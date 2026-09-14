package com.ruoyi.carbon.report.domain;

import java.math.BigDecimal;
import com.ruoyi.common.core.web.domain.BaseEntity;

public class ReportActivityRow extends BaseEntity
{
    private Long id;
    private Long deptId;
    private Long reportId;
    private String kind;
    private String fuelType;
    private String facility;
    private String scope;
    private String site;
    private BigDecimal quantity;
    private String unit;
    private String sourceName;
    private String dataOrigin;
    private String sourceTable;
    private String sourceId;
    private BigDecimal sourceValue;
    private String sourceUnit;
    private BigDecimal normalizedValue;
    private String normalizedUnit;
    private String sourceHash;
    private String overrideReason;
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public String getKind() { return kind; }
    public void setKind(String kind) { this.kind = kind; }
    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = fuelType; }
    public String getFacility() { return facility; }
    public void setFacility(String facility) { this.facility = facility; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public String getSite() { return site; }
    public void setSite(String site) { this.site = site; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getSourceName() { return sourceName; }
    public void setSourceName(String sourceName) { this.sourceName = sourceName; }
    public String getDataOrigin() { return dataOrigin; }
    public void setDataOrigin(String dataOrigin) { this.dataOrigin = dataOrigin; }
    public String getSourceTable() { return sourceTable; }
    public void setSourceTable(String sourceTable) { this.sourceTable = sourceTable; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public BigDecimal getSourceValue() { return sourceValue; }
    public void setSourceValue(BigDecimal sourceValue) { this.sourceValue = sourceValue; }
    public String getSourceUnit() { return sourceUnit; }
    public void setSourceUnit(String sourceUnit) { this.sourceUnit = sourceUnit; }
    public BigDecimal getNormalizedValue() { return normalizedValue; }
    public void setNormalizedValue(BigDecimal normalizedValue) { this.normalizedValue = normalizedValue; }
    public String getNormalizedUnit() { return normalizedUnit; }
    public void setNormalizedUnit(String normalizedUnit) { this.normalizedUnit = normalizedUnit; }
    public String getSourceHash() { return sourceHash; }
    public void setSourceHash(String sourceHash) { this.sourceHash = sourceHash; }
    public String getOverrideReason() { return overrideReason; }
    public void setOverrideReason(String overrideReason) { this.overrideReason = overrideReason; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
