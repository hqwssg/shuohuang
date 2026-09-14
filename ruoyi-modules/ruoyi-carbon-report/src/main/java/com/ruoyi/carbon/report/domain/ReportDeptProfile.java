package com.ruoyi.carbon.report.domain;

import com.ruoyi.common.core.web.domain.BaseEntity;

public class ReportDeptProfile extends BaseEntity
{
    private Long id;
    private Long deptId;
    private String legalName;
    private String shortName;
    private String entityCode;
    private String defaultCompiler;
    private String carbonDepartment;
    private String businessDescription;
    private String companyOverview;
    private String processDescription;
    private String defaultOrgBoundary;
    private String defaultAccountingMethod;
    private String defaultExclusionNote;
    private String defaultApprover;
    private String defaultValidator;
    private String defaultReviewer;
    private String defaultChecker;
    private String defaultAuthors;
    private String defaultEquipmentJson;
    private String defaultActivityProseJson;
    private String defaultWorkloadJson;
    private String defaultChapter6Json;
    private String headerTemplate;
    private Integer version;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public String getLegalName() { return legalName; }
    public void setLegalName(String legalName) { this.legalName = legalName; }
    public String getShortName() { return shortName; }
    public void setShortName(String shortName) { this.shortName = shortName; }
    public String getEntityCode() { return entityCode; }
    public void setEntityCode(String entityCode) { this.entityCode = entityCode; }
    public String getDefaultCompiler() { return defaultCompiler; }
    public void setDefaultCompiler(String defaultCompiler) { this.defaultCompiler = defaultCompiler; }
    public String getCarbonDepartment() { return carbonDepartment; }
    public void setCarbonDepartment(String carbonDepartment) { this.carbonDepartment = carbonDepartment; }
    public String getBusinessDescription() { return businessDescription; }
    public void setBusinessDescription(String businessDescription) { this.businessDescription = businessDescription; }
    public String getCompanyOverview() { return companyOverview; }
    public void setCompanyOverview(String companyOverview) { this.companyOverview = companyOverview; }
    public String getProcessDescription() { return processDescription; }
    public void setProcessDescription(String processDescription) { this.processDescription = processDescription; }
    public String getDefaultOrgBoundary() { return defaultOrgBoundary; }
    public void setDefaultOrgBoundary(String defaultOrgBoundary) { this.defaultOrgBoundary = defaultOrgBoundary; }
    public String getDefaultAccountingMethod() { return defaultAccountingMethod; }
    public void setDefaultAccountingMethod(String defaultAccountingMethod) { this.defaultAccountingMethod = defaultAccountingMethod; }
    public String getDefaultExclusionNote() { return defaultExclusionNote; }
    public void setDefaultExclusionNote(String defaultExclusionNote) { this.defaultExclusionNote = defaultExclusionNote; }
    public String getDefaultApprover() { return defaultApprover; }
    public void setDefaultApprover(String defaultApprover) { this.defaultApprover = defaultApprover; }
    public String getDefaultValidator() { return defaultValidator; }
    public void setDefaultValidator(String defaultValidator) { this.defaultValidator = defaultValidator; }
    public String getDefaultReviewer() { return defaultReviewer; }
    public void setDefaultReviewer(String defaultReviewer) { this.defaultReviewer = defaultReviewer; }
    public String getDefaultChecker() { return defaultChecker; }
    public void setDefaultChecker(String defaultChecker) { this.defaultChecker = defaultChecker; }
    public String getDefaultAuthors() { return defaultAuthors; }
    public void setDefaultAuthors(String defaultAuthors) { this.defaultAuthors = defaultAuthors; }
    public String getDefaultEquipmentJson() { return defaultEquipmentJson; }
    public void setDefaultEquipmentJson(String defaultEquipmentJson) { this.defaultEquipmentJson = defaultEquipmentJson; }
    public String getDefaultActivityProseJson() { return defaultActivityProseJson; }
    public void setDefaultActivityProseJson(String defaultActivityProseJson) { this.defaultActivityProseJson = defaultActivityProseJson; }
    public String getDefaultWorkloadJson() { return defaultWorkloadJson; }
    public void setDefaultWorkloadJson(String defaultWorkloadJson) { this.defaultWorkloadJson = defaultWorkloadJson; }
    public String getDefaultChapter6Json() { return defaultChapter6Json; }
    public void setDefaultChapter6Json(String defaultChapter6Json) { this.defaultChapter6Json = defaultChapter6Json; }
    public String getHeaderTemplate() { return headerTemplate; }
    public void setHeaderTemplate(String headerTemplate) { this.headerTemplate = headerTemplate; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
