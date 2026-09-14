package com.ruoyi.carbon.report.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import com.ruoyi.carbon.report.domain.ReportTask;
import com.ruoyi.common.core.constant.HttpStatus;
import com.ruoyi.common.core.exception.ServiceException;

class ReportAccessGuardTest
{
    @Test
    void otherDeptSeesNotFound()
    {
        ReportTask task = new ReportTask();
        task.setId(1L);
        task.setDeptId(301L);
        ServiceException ex = assertThrows(ServiceException.class, () -> ReportAccessGuard.requireTask(task, 302L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getCode());
    }

    @Test
    void missingOrLogicallyDeletedTaskIsNotFound()
    {
        ServiceException ex = assertThrows(ServiceException.class, () -> ReportAccessGuard.requireTask(null, 301L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getCode());
    }

    @Test
    void forgedDeptIsNotFound()
    {
        ServiceException ex = assertThrows(ServiceException.class, () -> ReportAccessGuard.requireDept(302L, 301L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getCode());
    }

    @Test
    void versionConflict()
    {
        ServiceException ex = assertThrows(ServiceException.class, () -> ReportAccessGuard.requireVersion(2, 1));
        assertEquals(HttpStatus.CONFLICT, ex.getCode());
    }
}
