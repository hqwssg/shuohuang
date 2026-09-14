package com.example.carbon.emission.model.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.carbon.emission.model.security.CarbonSecurityContext;
import com.example.carbon.emission.model.security.CarbonDataScopeService;

@RestController
@RequestMapping("/api/security")
public class SecurityContextController {
    private final CarbonDataScopeService dataScopeService;

    public SecurityContextController(CarbonDataScopeService dataScopeService) {
        this.dataScopeService = dataScopeService;
    }

    @GetMapping("/me")
    public ResponseEntity<CarbonSecurityContext> me() {
        return ResponseEntity.ok(dataScopeService.current());
    }
}
