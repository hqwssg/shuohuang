package com.ruoyi.carbon.report.domain;

import com.ruoyi.common.core.web.domain.BaseEntity;

public class ReportMonitoringDevice extends BaseEntity
{
    private Long id;
    private Long deptId;
    private Long reportId;
    private String name;
    private String model;
    private String accuracy;
    private String location;
    private String calibrationFrequency;
    private String measurementRange;
    private String dataOrigin;
    private String sourceTable;
    private String sourceId;
    private Integer sortOrder;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getAccuracy() { return accuracy; }
    public void setAccuracy(String accuracy) { this.accuracy = accuracy; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getCalibrationFrequency() { return calibrationFrequency; }
    public void setCalibrationFrequency(String calibrationFrequency) { this.calibrationFrequency = calibrationFrequency; }
    public String getMeasurementRange() { return measurementRange; }
    public void setMeasurementRange(String measurementRange) { this.measurementRange = measurementRange; }
    public String getDataOrigin() { return dataOrigin; }
    public void setDataOrigin(String dataOrigin) { this.dataOrigin = dataOrigin; }
    public String getSourceTable() { return sourceTable; }
    public void setSourceTable(String sourceTable) { this.sourceTable = sourceTable; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
