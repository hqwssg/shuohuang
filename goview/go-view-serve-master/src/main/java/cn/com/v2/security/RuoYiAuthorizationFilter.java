package cn.com.v2.security;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RuoYiAuthorizationFilter extends OncePerRequestFilter
{
    private final RuoYiSecurityClient securityClient;
    private final RuoYiSecurityProperties properties;

    public RuoYiAuthorizationFilter(RuoYiSecurityClient securityClient, RuoYiSecurityProperties properties)
    {
        this.securityClient = securityClient;
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)
    {
        String path = request.getRequestURI();
        return !properties.isEnabled() || "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !(path.startsWith("/api/goview/") || path.startsWith("/api/file/")
                        || path.startsWith("/carbon/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        if (request.getRequestURI().endsWith("/sys/login"))
        {
            error(response, 410, "GoView uses the unified RuoYi login");
            return;
        }
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer "))
        {
            error(response, 401, "Missing RuoYi login token");
            return;
        }
        RuoYiSecurityContext context;
        try
        {
            context = securityClient.load(authorization);
        }
        catch (Exception ex)
        {
            error(response, 401, "RuoYi session is invalid or unavailable");
            return;
        }
        String required = isRead(request) ? "carbon:screen:view" : "carbon:screen:edit";
        if (!context.hasPermission(required))
        {
            error(response, 403, "Permission denied: " + required);
            return;
        }
        request.setAttribute(RuoYiSecurityContext.REQUEST_ATTRIBUTE, context);
        chain.doFilter(request, response);
    }

    private boolean isRead(HttpServletRequest request)
    {
        return "GET".equalsIgnoreCase(request.getMethod()) || "HEAD".equalsIgnoreCase(request.getMethod());
    }

    private void error(HttpServletResponse response, int status, String message) throws IOException
    {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"code\":" + status + ",\"msg\":\"" + message + "\"}");
    }
}
