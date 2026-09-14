package com.ruoyi.carbon.report.service;

import java.io.InputStream;
import com.ruoyi.carbon.report.domain.ReportFile;
import com.ruoyi.carbon.report.domain.ReportGenerationJob;
import com.ruoyi.carbon.report.domain.dto.ReportFactorPlan;

public interface IReportGenerationService
{
    ReportFactorPlan factorPlan(Long reportId, Long deptId);

    ReportGenerationJob create(Long reportId, Long deptId, String outputKind, boolean aiEnabled, boolean useDefaultFactors);

    ReportGenerationJob latest(Long reportId, Long deptId);

    ReportGenerationJob get(Long reportId, Long jobId, Long deptId);

    ReportFile downloadMeta(Long artifactId, Long deptId);

    InputStream downloadStream(Long artifactId, Long deptId);
}
