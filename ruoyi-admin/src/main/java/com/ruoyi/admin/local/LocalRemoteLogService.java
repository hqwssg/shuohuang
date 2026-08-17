package com.ruoyi.admin.local;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.api.RemoteLogService;
import com.ruoyi.system.api.domain.SysLogininfor;
import com.ruoyi.system.api.domain.SysOperLog;
import com.ruoyi.system.service.ISysLogininforService;
import com.ruoyi.system.service.ISysOperLogService;
import org.springframework.stereotype.Service;

@Service
public class LocalRemoteLogService implements RemoteLogService
{
    private final ISysOperLogService operLogService;

    private final ISysLogininforService logininforService;

    public LocalRemoteLogService(ISysOperLogService operLogService, ISysLogininforService logininforService)
    {
        this.operLogService = operLogService;
        this.logininforService = logininforService;
    }

    @Override
    public R<Boolean> saveLog(SysOperLog sysOperLog, String source)
    {
        return R.ok(operLogService.insertOperlog(sysOperLog) > 0);
    }

    @Override
    public R<Boolean> saveLogininfor(SysLogininfor sysLogininfor, String source)
    {
        return R.ok(logininforService.insertLogininfor(sysLogininfor) > 0);
    }
}
