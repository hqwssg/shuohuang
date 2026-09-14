package com.ruoyi.carbon.report.domain.dto;

public class CreateReportRequest
{
    private String title;
    private String periodType;
    private Integer reportYear;
    private Integer reportMonth;
    private Long calculationTemplateId;
    private Long deptId;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getPeriodType() { return periodType; }
    public void setPeriodType(String periodType) { this.periodType = periodType; }
    public Integer getReportYear() { return reportYear; }
    public void setReportYear(Integer reportYear) { this.reportYear = reportYear; }
    public Integer getReportMonth() { return reportMonth; }
    public void setReportMonth(Integer reportMonth) { this.reportMonth = reportMonth; }
    public Long getCalculationTemplateId() { return calculationTemplateId; }
    public void setCalculationTemplateId(Long calculationTemplateId) { this.calculationTemplateId = calculationTemplateId; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
}
