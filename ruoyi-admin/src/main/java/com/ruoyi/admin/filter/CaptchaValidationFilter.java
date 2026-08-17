package com.ruoyi.admin.filter;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.admin.captcha.CaptchaService;
import com.ruoyi.admin.web.CachedBodyRequestWrapper;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.utils.ServletUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(-200)
public class CaptchaValidationFilter extends OncePerRequestFilter
{
    private final CaptchaService captchaService;

    public CaptchaValidationFilter(CaptchaService captchaService)
    {
        this.captchaService = captchaService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException
    {
        if (!shouldValidate(request))
        {
            filterChain.doFilter(request, response);
            return;
        }

        CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);
        try
        {
            String body = new String(wrapper.getBody(), StandardCharsets.UTF_8);
            JSONObject json = JSON.parseObject(body);
            captchaService.checkCaptcha(json.getString("code"), json.getString("uuid"));
        }
        catch (Exception e)
        {
            ServletUtils.renderString(response, JSON.toJSONString(R.fail(e.getMessage())));
            return;
        }
        filterChain.doFilter(wrapper, response);
    }

    private boolean shouldValidate(HttpServletRequest request)
    {
        if (!"POST".equalsIgnoreCase(request.getMethod()))
        {
            return false;
        }
        String path = getPath(request);
        return "/auth/login".equals(path) || "/auth/register".equals(path);
    }

    private String getPath(HttpServletRequest request)
    {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath))
        {
            return uri.substring(contextPath.length());
        }
        return uri;
    }
}
