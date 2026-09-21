package com.example.carbon.emission.model.audit;

import java.util.LinkedHashMap;
import java.util.Map;

import com.example.carbon.emission.model.security.CarbonSecurityContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.http.ResponseEntity;
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
        if (request == null || isReadOnly(request.getMethod())) return joinPoint.proceed();

        CarbonSecurityContext context = securityContext(request);
        if (context != null) stampAuthenticatedActor(joinPoint.getArgs(), context.userId());

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
        CarbonSecurityContext context = securityContext(request);
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("title", limit("Carbon accounting - " + controller + " - " + signature.getName(), 50));
        audit.put("businessType", businessType(request.getMethod()));
        audit.put("method", limit(signature.toShortString(), 200));
        audit.put("requestMethod", request.getMethod());
        audit.put("operatorType", 1);
        audit.put("operName", limit(context == null ? "anonymous" : context.userName(), 50));
        audit.put("deptName", limit(context == null ? "Carbon accounting" : context.deptName(), 50));
        audit.put("deptId", context == null ? null : context.deptId());
        audit.put("operUrl", limit(request.getRequestURI(), 255));
        audit.put("operIp", limit(clientIp(request), 128));
        audit.put("operParam", limit(toJson(requestDetails(signature, joinPoint.getArgs(), request)), 2000));
        audit.put("jsonResult", limit(toJson(result), 2000));
        boolean rejected = result instanceof ResponseEntity<?> response && !response.getStatusCode().is2xxSuccessful();
        audit.put("status", failure == null && !rejected ? 0 : 1);
        audit.put("errorMsg", failure != null ? limit(failure.getMessage(), 2000)
            : rejected ? limit(toJson(((ResponseEntity<?>) result).getBody()), 2000) : "");
        audit.put("costTime", costTime);
        return audit;
    }

    private Map<String, Object> requestDetails(MethodSignature signature, Object[] values, HttpServletRequest request) {
        CarbonSecurityContext context = securityContext(request);
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("path", request.getRequestURI());
        details.put("query", request.getParameterMap());
        details.put("userId", context == null ? null : context.userId());
        details.put("deptId", context == null ? null : context.deptId());
        details.put("changedFields", arguments(signature, values));
        return details;
    }

    private Map<String, Object> arguments(MethodSignature signature, Object[] values) {
        Map<String, Object> args = new LinkedHashMap<>();
        String[] names = signature.getParameterNames();
        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (!(value instanceof ServletRequest) && !(value instanceof ServletResponse)
                    && !(value instanceof MultipartFile)) {
                args.put(names != null && i < names.length ? names[i] : "arg" + i, value);
            }
        }
        return args;
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest();
        }
        return null;
    }

    private CarbonSecurityContext securityContext(HttpServletRequest request) {
        Object value = request.getAttribute(CarbonSecurityContext.REQUEST_ATTRIBUTE);
        return value instanceof CarbonSecurityContext context ? context : null;
    }

    private void stampAuthenticatedActor(Object[] arguments, Long userId) {
        if (userId == null) return;
        for (Object argument : arguments) {
            if (argument instanceof Map<?, ?> source) {
                @SuppressWarnings("unchecked")
                Map<Object, Object> values = (Map<Object, Object>) source;
                if (values.containsKey("createdBy")) values.put("createdBy", userId);
                if (values.containsKey("updatedBy")) values.put("updatedBy", userId);
            } else {
                stampProperty(argument, "setCreatedBy", userId);
                stampProperty(argument, "setUpdatedBy", userId);
            }
        }
    }

    private void stampProperty(Object target, String methodName, Long userId) {
        if (target == null) return;
        try {
            target.getClass().getMethod(methodName, Long.class).invoke(target, userId);
        } catch (ReflectiveOperationException ignored) {
            // Not every request DTO contains audit fields.
        }
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
