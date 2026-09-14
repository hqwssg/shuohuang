package com.example.carbon.emission.model.security;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import com.example.carbon.emission.model.dto.CreateNodeRequest;

@Aspect
@Component
public class CarbonNodeScopeAspect {
    private final CarbonDataScopeService dataScope;

    public CarbonNodeScopeAspect(CarbonDataScopeService dataScope) {
        this.dataScope = dataScope;
    }

    @Before("execution(public * com.example.carbon.emission.model.controller.EmissionNodeController.*(..))")
    public void check(JoinPoint joinPoint) {
        String method = joinPoint.getSignature().getName();
        Object[] args = joinPoint.getArgs();
        boolean write = method.startsWith("create") || method.startsWith("update") || method.startsWith("delete")
                || method.startsWith("move") || method.startsWith("mount");

        if ("getAllNodes".equals(method) && !dataScope.current().allData()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN, "Select an authorized template first");
        }

        if ("getTree".equals(method)) {
            if (args.length > 0 && args[0] instanceof Long templateId) {
                dataScope.requireTemplate(templateId, false);
                return;
            }
            if (!dataScope.current().allData()) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "Select an authorized template first");
            }
        }
        if ("createNode".equals(method) && args.length > 0 && args[0] instanceof CreateNodeRequest request) {
            if (request.getParentId() != null) dataScope.requireNode(request.getParentId(), true);
            else if (request.getTemplateId() != null) dataScope.requireTemplate(request.getTemplateId(), true);
            return;
        }
        if (args.length > 0 && args[0] instanceof Long nodeId && !"getNextNodeId".equals(method)) {
            dataScope.requireNode(nodeId, write);
        }
    }
}
