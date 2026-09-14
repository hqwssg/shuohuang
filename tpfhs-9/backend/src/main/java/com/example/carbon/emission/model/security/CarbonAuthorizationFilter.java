package com.example.carbon.emission.model.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class CarbonAuthorizationFilter extends OncePerRequestFilter {
    private final CarbonSecurityProperties properties;
    private final RuoYiSecurityClient securityClient;

    public CarbonAuthorizationFilter(CarbonSecurityProperties properties, RuoYiSecurityClient securityClient) {
        this.properties = properties;
        this.securityClient = securityClient;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/") || "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !properties.isEnabled();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            reject(response, HttpStatus.UNAUTHORIZED, "Missing RuoYi login token");
            return;
        }

        CarbonSecurityContext context;
        try {
            context = securityClient.load(authorization);
        } catch (RestClientException | IllegalStateException ex) {
            reject(response, HttpStatus.UNAUTHORIZED, "RuoYi login session is invalid or expired");
            return;
        }

        List<String> required = CarbonPermissionResolver.requiredPermissions(request.getMethod(), request.getRequestURI());
        if (!required.isEmpty() && required.stream().noneMatch(context::hasPermission)) {
            reject(response, HttpStatus.FORBIDDEN, "No permission for this carbon operation");
            return;
        }
        request.setAttribute(CarbonSecurityContext.REQUEST_ATTRIBUTE, context);
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status.value() + ",\"message\":\"" + message + "\"}");
    }
}
