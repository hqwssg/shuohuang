package com.ruoyi.carbon.report.service;

import com.ruoyi.carbon.report.domain.ReportDeptProfile;

public interface IReportDeptProfileService
{
    ReportDeptProfile get(Long deptId);

    ReportDeptProfile save(ReportDeptProfile profile, Long deptId, String username);
}
