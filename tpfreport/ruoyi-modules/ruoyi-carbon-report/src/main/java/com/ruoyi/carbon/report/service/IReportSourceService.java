package com.ruoyi.carbon.report.service;

import java.util.Date;
import java.util.List;
import com.ruoyi.carbon.report.domain.CalculationCandidate;
import com.ruoyi.carbon.report.domain.dto.PrefillResult;

public interface IReportSourceService
{
    List<CalculationCandidate> listCalculations(Long deptId, Date periodStart, Date periodEnd);

    PrefillResult preview(Long reportId, Long deptId, Long calculationTemplateId);

    PrefillResult commit(Long reportId, Long deptId, Long calculationTemplateId);
}
