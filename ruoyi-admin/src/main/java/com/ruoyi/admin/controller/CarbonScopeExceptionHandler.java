package com.ruoyi.admin.controller;

import com.ruoyi.common.core.web.domain.AjaxResult;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = CarbonScopeController.class)
public class CarbonScopeExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<AjaxResult> handle(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode()).body(
            AjaxResult.error(exception.getStatusCode().value(), exception.getReason()));
    }
}
