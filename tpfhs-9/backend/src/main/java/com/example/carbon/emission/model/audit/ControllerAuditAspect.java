package com.example.carbon.emission.model.audit;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

@Aspect
@Component
public class ControllerAuditAspect {
    private final ObjectMapper objectMapper;
    private final RuoYiAuditClient auditClient;

    public ControllerAuditAspect(ObjectMapper objectMapper, RuoYiAuditClient auditClient) {
        this.objectMapper = objectMapper;
        this.auditClient = auditClient;
    }

    @Around("execution(public * com.example.carbon.emission.model.controller..*(..))")
    public Object recordMutation(ProceedingJoinPoint joinPoint) throws Throwable {
        HttpServletRequest request = currentRequest();
        if (request == null || isReadOnly(request.getMethod())) {
            return joinPoint.proceed();
        }

        long started = System.currentTimeMillis();
        Object result = null;
        Throwable failure = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            failure = ex;
            throw ex;
        } finally {
            auditClient.send(buildAudit(joinPoint, request, result, failure, System.currentTimeMillis() - started));
        }
    }

    private Map<String, Object> buildAudit(ProceedingJoinPoint joinPoint, HttpServletRequest request,
            Object result, Throwable failure, long costTime) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String controller = signature.getDeclaringType().getSimpleName().replace("Controller", "");
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("title", limit("碳核算-" + controller + "-" + signature.getName(), 50));
        audit.put("businessType", businessType(request.getMethod()));
        audit.put("method", limit(signature.toShortString(), 200));
        audit.put("requestMethod", request.getMethod());
        audit.put("operatorType", 1);
        audit.put("operName", limit(header(request, "X-User-Name", "anonymous"), 50));
        audit.put("deptName", "碳排放核算");
        audit.put("operUrl", limit(request.getRequestURI(), 255));
        audit.put("operIp", limit(clientIp(request), 128));
        audit.put("operParam", limit(toJson(requestDetails(signature, joinPoint.getArgs(), request)), 2000));
        audit.put("jsonResult", limit(toJson(result), 2000));
        audit.put("status", failure == null ? 0 : 1);
        audit.put("errorMsg", failure == null ? "" : limit(failure.getMessage(), 2000));
        audit.put("costTime", costTime);
        return audit;
    }

    private Map<String, Object> arguments(MethodSignature signature, Object[] values) {
        Map<String, Object> args = new LinkedHashMap<>();
        String[] names = signature.getParameterNames();
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (value instanceof ServletRequest || value instanceof ServletResponse || value instanceof MultipartFile) {
                continue;
            }
            args.put(names != null && i < names.length ? names[i] : "arg" + i, value);
        }
        return args;
    }

    private Map<String, Object> requestDetails(MethodSignature signature, Object[] values, HttpServletRequest request) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("path", request.getRequestURI());
        details.put("query", request.getParameterMap());
        details.put("userId", header(request, "X-User-Id", ""));
        details.put("changedFields", arguments(signature, values));
        return details;
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private String header(HttpServletRequest request, String name, String fallback) {
        String value = request.getHeader(name);
        if (value == null || value.isBlank()) return fallback;
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
    }

    private boolean isReadOnly(String method) {
        return "GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method);
    }

    private int businessType(String method) {
        if ("POST".equalsIgnoreCase(method)) return 1;
        if ("PUT".equalsIgnoreCase(method) || "PATCH".equalsIgnoreCase(method)) return 2;
        if ("DELETE".equalsIgnoreCase(method)) return 3;
        return 0;
    }

    private String toJson(Object value) {
        if (value == null) return "";
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "[unserializable]";
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }
}
