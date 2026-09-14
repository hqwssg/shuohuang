package com.ruoyi.carbon.report.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.web.domain.BaseEntity;

public class ReportTask extends BaseEntity
{
    private Long id;
    private Long deptId;
    private String title;
    private String templateCode;
    private String periodType;
    private Integer reportYear;
    private Integer reportMonth;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date periodStart;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date periodEnd;
    private Long subjectNodeId;
    private Long emissionTemplateId;
    private Long calculationTemplateId;
    private Long factorTemplateId;
    private String sourceHash;
    private String status;
    private String currentStep;
    private Integer version;
    private String deletedFlag;
    private String metadataJson;
    private Long latestDocxFileId;
    private Long latestPdfFileId;
    private String entityProfileJson;
    private String organizationBoundaryJson;
    private String emissionBoundaryNotesJson;
    private String activityProseJson;
    private String priorYearJson;
    private String chapter6Json;
    private String lastValidationJson;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public String getPeriodType() { return periodType; }
    public void setPeriodType(String periodType) { this.periodType = periodType; }
    public Integer getReportYear() { return reportYear; }
    public void setReportYear(Integer reportYear) { this.reportYear = reportYear; }
    public Integer getReportMonth() { return reportMonth; }
    public void setReportMonth(Integer reportMonth) { this.reportMonth = reportMonth; }
    public Date getPeriodStart() { return periodStart; }
    public void setPeriodStart(Date periodStart) { this.periodStart = periodStart; }
    public Date getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(Date periodEnd) { this.periodEnd = periodEnd; }
    public Long getSubjectNodeId() { return subjectNodeId; }
    public void setSubjectNodeId(Long subjectNodeId) { this.subjectNodeId = subjectNodeId; }
    public Long getEmissionTemplateId() { return emissionTemplateId; }
    public void setEmissionTemplateId(Long emissionTemplateId) { this.emissionTemplateId = emissionTemplateId; }
    public Long getCalculationTemplateId() { return calculationTemplateId; }
    public void setCalculationTemplateId(Long calculationTemplateId) { this.calculationTemplateId = calculationTemplateId; }
    public Long getFactorTemplateId() { return factorTemplateId; }
    public void setFactorTemplateId(Long factorTemplateId) { this.factorTemplateId = factorTemplateId; }
    public String getSourceHash() { return sourceHash; }
    public void setSourceHash(String sourceHash) { this.sourceHash = sourceHash; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCurrentStep() { return currentStep; }
    public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public String getDeletedFlag() { return deletedFlag; }
    public void setDeletedFlag(String deletedFlag) { this.deletedFlag = deletedFlag; }
    public Long getLatestDocxFileId() { return latestDocxFileId; }
    public void setLatestDocxFileId(Long latestDocxFileId) { this.latestDocxFileId = latestDocxFileId; }
    public Long getLatestPdfFileId() { return latestPdfFileId; }
    public void setLatestPdfFileId(Long latestPdfFileId) { this.latestPdfFileId = latestPdfFileId; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public String getEntityProfileJson() { return entityProfileJson; }
    public void setEntityProfileJson(String entityProfileJson) { this.entityProfileJson = entityProfileJson; }
    public String getOrganizationBoundaryJson() { return organizationBoundaryJson; }
    public void setOrganizationBoundaryJson(String organizationBoundaryJson) { this.organizationBoundaryJson = organizationBoundaryJson; }
    public String getEmissionBoundaryNotesJson() { return emissionBoundaryNotesJson; }
    public void setEmissionBoundaryNotesJson(String emissionBoundaryNotesJson) { this.emissionBoundaryNotesJson = emissionBoundaryNotesJson; }
    public String getActivityProseJson() { return activityProseJson; }
    public void setActivityProseJson(String activityProseJson) { this.activityProseJson = activityProseJson; }
    public String getPriorYearJson() { return priorYearJson; }
    public void setPriorYearJson(String priorYearJson) { this.priorYearJson = priorYearJson; }
    public String getChapter6Json() { return chapter6Json; }
    public void setChapter6Json(String chapter6Json) { this.chapter6Json = chapter6Json; }
    public String getLastValidationJson() { return lastValidationJson; }
    public void setLastValidationJson(String lastValidationJson) { this.lastValidationJson = lastValidationJson; }
}
