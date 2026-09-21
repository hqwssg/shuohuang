package com.example.carbon.emission.model.controller;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {
    @Test
    void permissionDenialRetainsForbiddenStatus() {
        var response = new GlobalExceptionHandler().handleStatusException(
            new ResponseStatusException(HttpStatus.FORBIDDEN, "Scope denied"));
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().get("code"));
        assertEquals("Scope denied", response.getBody().get("message"));
        assertFalse(response.getBody().containsKey("stackTrace"));
    }
}
