package com.ruoyi.carbon.report.domain;

import com.ruoyi.common.core.web.domain.BaseEntity;

public class ReportGenerationJob extends BaseEntity
{
    private Long id;
    private Long deptId;
    private Long reportId;
    private String status;
    private String outputKind;
    private Integer progress;
    private String snapshotSha256;
    private String inputSnapshotJson;
    private String aiEnabled;
    private Long docxFileId;
    private Long pdfFileId;
    private String errorMessage;
    private Integer llmCallCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }
    public Long getReportId() { return reportId; }
    public void setReportId(Long reportId) { this.reportId = reportId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getOutputKind() { return outputKind; }
    public void setOutputKind(String outputKind) { this.outputKind = outputKind; }
    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }
    public String getSnapshotSha256() { return snapshotSha256; }
    public void setSnapshotSha256(String snapshotSha256) { this.snapshotSha256 = snapshotSha256; }
    public String getInputSnapshotJson() { return inputSnapshotJson; }
    public void setInputSnapshotJson(String inputSnapshotJson) { this.inputSnapshotJson = inputSnapshotJson; }
    public String getAiEnabled() { return aiEnabled; }
    public void setAiEnabled(String aiEnabled) { this.aiEnabled = aiEnabled; }
    public Long getDocxFileId() { return docxFileId; }
    public void setDocxFileId(Long docxFileId) { this.docxFileId = docxFileId; }
    public Long getPdfFileId() { return pdfFileId; }
    public void setPdfFileId(Long pdfFileId) { this.pdfFileId = pdfFileId; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public Integer getLlmCallCount() { return llmCallCount; }
    public void setLlmCallCount(Integer llmCallCount) { this.llmCallCount = llmCallCount; }
}
