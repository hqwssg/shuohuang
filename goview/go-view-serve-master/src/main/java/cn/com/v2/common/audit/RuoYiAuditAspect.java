package cn.com.v2.common.audit;

import java.util.ArrayList;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import cn.com.v2.model.SysUser;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONUtil;

@Aspect
@Component
public class RuoYiAuditAspect
{
    private static final int MAX_TEXT_LENGTH = 2000;

    private final RuoYiAuditClient auditClient;

    public RuoYiAuditAspect(RuoYiAuditClient auditClient)
    {
        this.auditClient = auditClient;
    }

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint joinPoint, RuoYiAudit audit) throws Throwable
    {
        long startTime = System.currentTimeMillis();
        Object result = null;
        Throwable failure = null;
        try
        {
            result = joinPoint.proceed();
            return result;
        }
        catch (Throwable throwable)
        {
            failure = throwable;
            throw throwable;
        }
        finally
        {
            HttpServletRequest request = currentRequest();
            Map<String, Object> payload = new LinkedHashMap<String, Object>();
            payload.put("title", audit.title());
            payload.put("businessType", audit.businessType());
            payload.put("method", joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName() + "()");
            payload.put("requestMethod", request == null ? "UNKNOWN" : request.getMethod());
            payload.put("operatorType", 1);
            payload.put("operName", currentUsername());
            payload.put("deptName", "GoView");
            payload.put("operUrl", request == null ? null : request.getRequestURI());
            payload.put("operIp", getClientIp(request));
            payload.put("operParam", serializeRequest(request, joinPoint.getArgs(), audit.saveRequestData()));
            payload.put("jsonResult", audit.saveResponseData() ? limit(JSONUtil.toJsonStr(result)) : null);
            payload.put("status", failure == null && isSuccessfulResult(result) ? 0 : 1);
            payload.put("errorMsg", failure == null ? resultMessage(result) : limit(failure.getMessage()));
            payload.put("costTime", System.currentTimeMillis() - startTime);
            auditClient.recordOperation(payload);
        }
    }

    private HttpServletRequest currentRequest()
    {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes)
        {
            return ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        }
        return null;
    }

    private String currentUsername()
    {
        try
        {
            if (!StpUtil.isLogin())
            {
                return "anonymous";
            }
            Object user = StpUtil.getSession().get("user");
            if (user instanceof SysUser && ((SysUser) user).getUsername() != null)
            {
                return ((SysUser) user).getUsername();
            }
            return StpUtil.getLoginIdAsString();
        }
        catch (Exception ignored)
        {
            return "anonymous";
        }
    }

    private String getClientIp(HttpServletRequest request)
    {
        if (request == null)
        {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.trim().isEmpty())
        {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return realIp == null || realIp.trim().isEmpty() ? request.getRemoteAddr() : realIp;
    }

    private String serializeArguments(Object[] arguments)
    {
        List<Object> safeArguments = new ArrayList<Object>();
        if (arguments != null)
        {
            for (Object argument : arguments)
            {
                if (argument == null || argument instanceof ServletRequest || argument instanceof ServletResponse)
                {
                    continue;
                }
                if (argument instanceof MultipartFile)
                {
                    MultipartFile file = (MultipartFile) argument;
                    Map<String, Object> fileInfo = new LinkedHashMap<String, Object>();
                    fileInfo.put("fileName", file.getOriginalFilename());
                    fileInfo.put("size", file.getSize());
                    safeArguments.add(fileInfo);
                    continue;
                }
                safeArguments.add(argument);
            }
        }
        return JSONUtil.toJsonStr(safeArguments);
    }

    private String serializeRequest(HttpServletRequest request, Object[] arguments, boolean includePayload)
    {
        Map<String, Object> details = new LinkedHashMap<String, Object>();
        if (request != null)
        {
            details.put("path", request.getRequestURI());
            details.put("query", request.getParameterMap());
        }
        String serialized = serializeArguments(arguments);
        if (includePayload)
        {
            details.put("changedFields", serialized);
        }
        else if (serialized != null && !"[]".equals(serialized))
        {
            details.put("payloadLength", serialized.length());
            details.put("payloadSha256", sha256(serialized));
            details.put("payloadPreview", serialized.substring(0, Math.min(serialized.length(), 800)));
        }
        return limit(JSONUtil.toJsonStr(details));
    }

    private String sha256(String value)
    {
        try
        {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte item : bytes)
            {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        }
        catch (Exception ignored)
        {
            return "unavailable";
        }
    }

    private boolean isSuccessfulResult(Object result)
    {
        if (!(result instanceof Map))
        {
            return true;
        }
        Object code = ((Map<?, ?>) result).get("code");
        return code == null || "200".equals(String.valueOf(code));
    }

    private String resultMessage(Object result)
    {
        if (isSuccessfulResult(result) || !(result instanceof Map))
        {
            return null;
        }
        Object message = ((Map<?, ?>) result).get("msg");
        return message == null ? "GoView operation failed" : limit(String.valueOf(message));
    }

    private String limit(String value)
    {
        return value == null || value.length() <= MAX_TEXT_LENGTH ? value : value.substring(0, MAX_TEXT_LENGTH);
    }
}
