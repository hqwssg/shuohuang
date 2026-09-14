package com.ruoyi.carbon.report.mapper;

import com.ruoyi.carbon.report.domain.ReportDeptProfile;

public interface ReportDeptProfileMapper
{
    ReportDeptProfile selectByDeptId(Long deptId);

    int insert(ReportDeptProfile profile);

    int update(ReportDeptProfile profile);
}
