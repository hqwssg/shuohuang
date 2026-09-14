package com.ruoyi.carbon.report.service;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import com.ruoyi.carbon.report.domain.ReportFile;
import com.ruoyi.carbon.report.domain.ReportTask;
import com.ruoyi.carbon.report.domain.dto.CreateReportRequest;
import com.ruoyi.carbon.report.domain.dto.ReportDraft;

public interface IReportTaskService
{
    List<ReportTask> list(ReportTask query);

    ReportTask get(Long id, Long deptId);

    ReportDraft getDraft(Long id, Long deptId);

    ReportTask create(CreateReportRequest request, Long deptId, String username);

    ReportTask saveSection(Long id, Long deptId, String section, Integer version, String payloadJson, String username);

    void remove(Long id, Long deptId, String username);

    ReportFile uploadImage(Long id, Long deptId, String role, String originalName, String contentType,
            long size, InputStream in, String username);

    void resetImage(Long id, Long deptId, String role);

    Path defaultImagePath(String role);

    void markGenerated(Long id, Long deptId);
}
