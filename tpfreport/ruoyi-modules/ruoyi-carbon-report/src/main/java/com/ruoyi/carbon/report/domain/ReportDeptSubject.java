package com.ruoyi.carbon.report.domain;

import com.ruoyi.common.core.web.domain.BaseEntity;

public class ReportDeptSubject extends BaseEntity
{
    private Long id;
    private Long deptId;
    private Long subjectNodeId;
    private String energyAllocation;
    private String enabled;

    public boolean isEnabled()
    {
        return "1".equals(enabled);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Long getSubjectNodeId() { return subjectNodeId; }
    public void setSubjectNodeId(Long subjectNodeId) { this.subjectNodeId = subjectNodeId; }
    public String getEnergyAllocation() { return energyAllocation; }
    public void setEnergyAllocation(String energyAllocation) { this.energyAllocation = energyAllocation; }
    public String getEnabled() { return enabled; }
    public void setEnabled(String enabled) { this.enabled = enabled; }
}
