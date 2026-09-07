package com.ruoyi.admin.controller.internal;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.admin.config.properties.GoviewAuditProperties;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.system.api.RemoteLogService;
import com.ruoyi.system.api.domain.SysLogininfor;
import com.ruoyi.system.api.domain.SysOperLog;
import com.ruoyi.job.domain.SysJobLog;
import com.ruoyi.job.service.ISysJobLogService;

@RestController
@RequestMapping({"/internal/goview/audit", "/internal/carbon/audit"})
public class GoviewAuditController
{
    public static final String SECRET_HEADER = "X-Goview-Audit-Secret";

    private static final Logger log = LoggerFactory.getLogger(GoviewAuditController.class);

    private final GoviewAuditProperties properties;

    private final RemoteLogService remoteLogService;

    private final ISysJobLogService jobLogService;

    public GoviewAuditController(GoviewAuditProperties properties, RemoteLogService remoteLogService,
            ISysJobLogService jobLogService)
    {
        this.properties = properties;
        this.remoteLogService = remoteLogService;
        this.jobLogService = jobLogService;
    }

    @PostMapping("/operation")
    public ResponseEntity<R<Boolean>> saveOperation(
            @RequestHeader(value = SECRET_HEADER, required = false) String suppliedSecret,
            @RequestBody SysOperLog operLog)
    {
        if (!isAuthorized(suppliedSecret))
        {
            return forbidden();
        }
        normalize(operLog);
        try
        {
            return ResponseEntity.ok(remoteLogService.saveLog(operLog, SecurityConstants.INNER));
        }
        catch (Exception e)
        {
            log.error("Failed to persist integrated operation audit log", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(R.fail("audit log persistence failed"));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<R<Boolean>> saveLogin(
            @RequestHeader(value = SECRET_HEADER, required = false) String suppliedSecret,
            @RequestBody SysLogininfor logininfor)
    {
        if (!isAuthorized(suppliedSecret))
        {
            return forbidden();
        }
        normalize(logininfor);
        try
        {
            return ResponseEntity.ok(remoteLogService.saveLogininfor(logininfor, SecurityConstants.INNER));
        }
        catch (Exception e)
        {
            log.error("Failed to persist GoView login audit log", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(R.fail("audit log persistence failed"));
        }
    }

    @PostMapping("/job")
    public ResponseEntity<R<Boolean>> saveJob(
            @RequestHeader(value = SECRET_HEADER, required = false) String suppliedSecret,
            @RequestBody SysJobLog jobLog)
    {
        if (!isAuthorized(suppliedSecret))
        {
            return forbidden();
        }
        normalize(jobLog);
        try
        {
            jobLogService.addJobLog(jobLog);
            return ResponseEntity.ok(R.ok(true));
        }
        catch (Exception e)
        {
            log.error("Failed to persist integrated job audit log", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(R.fail("job log persistence failed"));
        }
    }

    private boolean isAuthorized(String suppliedSecret)
    {
        String configuredSecret = properties.getSecret();
        if (configuredSecret == null || configuredSecret.isBlank() || suppliedSecret == null)
        {
            return false;
        }
        return MessageDigest.isEqual(configuredSecret.getBytes(StandardCharsets.UTF_8),
                suppliedSecret.getBytes(StandardCharsets.UTF_8));
    }

    private ResponseEntity<R<Boolean>> forbidden()
    {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(R.fail(HttpStatus.FORBIDDEN.value(), "forbidden"));
    }

    private void normalize(SysOperLog operLog)
    {
        operLog.setOperId(null);
        operLog.setTitle(defaultText(limit(operLog.getTitle(), 50), "GoView"));
        operLog.setBusinessType(defaultNumber(operLog.getBusinessType(), 0));
        operLog.setMethod(defaultText(limit(operLog.getMethod(), 200), "GoView"));
        operLog.setRequestMethod(defaultText(limit(operLog.getRequestMethod(), 10), "UNKNOWN"));
        operLog.setOperatorType(defaultNumber(operLog.getOperatorType(), 1));
        operLog.setOperName(defaultText(limit(operLog.getOperName(), 50), "anonymous"));
        operLog.setDeptName(defaultText(limit(operLog.getDeptName(), 50), "GoView"));
        operLog.setOperUrl(limit(operLog.getOperUrl(), 255));
        operLog.setOperIp(limit(operLog.getOperIp(), 128));
        operLog.setOperParam(limit(operLog.getOperParam(), 2000));
        operLog.setJsonResult(limit(operLog.getJsonResult(), 2000));
        operLog.setStatus(defaultNumber(operLog.getStatus(), 0));
        operLog.setErrorMsg(limit(operLog.getErrorMsg(), 2000));
        operLog.setCostTime(operLog.getCostTime() == null || operLog.getCostTime() < 0 ? 0L : operLog.getCostTime());
    }

    private void normalize(SysLogininfor logininfor)
    {
        logininfor.setInfoId(null);
        logininfor.setUserName(defaultText(limit(logininfor.getUserName(), 50), "anonymous"));
        logininfor.setIpaddr(limit(logininfor.getIpaddr(), 128));
        logininfor.setStatus("1".equals(logininfor.getStatus()) ? "1" : "0");
        logininfor.setMsg(defaultText(limit(logininfor.getMsg(), 255), "GoView login event"));
    }

    private void normalize(SysJobLog jobLog)
    {
        jobLog.setJobLogId(null);
        jobLog.setJobName(defaultText(limit(jobLog.getJobName(), 64), "集成任务"));
        jobLog.setJobGroup(defaultText(limit(jobLog.getJobGroup(), 64), "DEFAULT"));
        jobLog.setInvokeTarget(defaultText(limit(jobLog.getInvokeTarget(), 500), "integratedTask"));
        jobLog.setJobMessage(limit(jobLog.getJobMessage(), 500));
        jobLog.setStatus("1".equals(jobLog.getStatus()) ? "1" : "0");
        jobLog.setExceptionInfo(limit(jobLog.getExceptionInfo(), 2000));
        if (jobLog.getStartTime() == null)
        {
            jobLog.setStartTime(new java.util.Date());
        }
        if (jobLog.getEndTime() == null)
        {
            jobLog.setEndTime(new java.util.Date());
        }
    }

    private String limit(String value, int maxLength)
    {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String defaultText(String value, String fallback)
    {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Integer defaultNumber(Integer value, int fallback)
    {
        return value == null ? fallback : value;
    }
}
