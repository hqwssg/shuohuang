package com.ruoyi.carbon.report.domain;

import java.math.BigDecimal;
import com.ruoyi.common.core.web.domain.BaseEntity;

public class ReportWorkload extends BaseEntity
{
    private Long id;
    private Long deptId;
    private Long reportId;
    private String name;
    private BigDecimal quantity;
    private String unit;
    private String dataOrigin;
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getDataOrigin() { return dataOrigin; }
    public void setDataOrigin(String dataOrigin) { this.dataOrigin = dataOrigin; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
