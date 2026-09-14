package com.ruoyi.carbon.report.service;

import com.ruoyi.carbon.report.domain.ReportTask;
import com.ruoyi.common.core.constant.HttpStatus;
import com.ruoyi.common.core.exception.ServiceException;

public final class ReportAccessGuard
{
    private ReportAccessGuard()
    {
    }

    public static ReportTask requireTask(ReportTask task, Long deptId)
    {
        if (task == null || deptId == null || !deptId.equals(task.getDeptId()))
        {
            throw new ServiceException("not found", HttpStatus.NOT_FOUND);
        }
        return task;
    }

    public static void requireDept(Long requestedDeptId, Long allowedDeptId)
    {
        if (requestedDeptId == null || allowedDeptId == null || !requestedDeptId.equals(allowedDeptId))
        {
            throw new ServiceException("not found", HttpStatus.NOT_FOUND);
        }
    }

    public static void requireVersion(Integer current, Integer incoming)
    {
        if (current == null || incoming == null || !current.equals(incoming))
        {
            throw new ServiceException("version conflict", HttpStatus.CONFLICT);
        }
    }
}
