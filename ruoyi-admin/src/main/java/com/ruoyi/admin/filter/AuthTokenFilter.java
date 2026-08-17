package com.ruoyi.admin.filter;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.admin.config.properties.IgnoreWhiteProperties;
import com.ruoyi.admin.web.MutableHeaderRequestWrapper;
import com.ruoyi.common.core.constant.CacheConstants;
import com.ruoyi.common.core.constant.HttpStatus;
import com.ruoyi.common.core.constant.SecurityConstants;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.utils.JwtUtils;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.redis.service.RedisService;
import com.ruoyi.common.security.utils.SecurityUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(-100)
public class AuthTokenFilter extends OncePerRequestFilter
{
    private final IgnoreWhiteProperties ignoreWhite;

    private final RedisService redisService;

    public AuthTokenFilter(IgnoreWhiteProperties ignoreWhite, RedisService redisService)
    {
        this.ignoreWhite = ignoreWhite;
        this.redisService = redisService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException
    {
        String path = getPath(request);
        MutableHeaderRequestWrapper wrapper = new MutableHeaderRequestWrapper(request);
        wrapper.removeHeader(SecurityConstants.FROM_SOURCE);
        if (StringUtils.matches(path, ignoreWhite.getWhites()))
        {
            filterChain.doFilter(wrapper, response);
            return;
        }

        String token = SecurityUtils.getToken(request);
        if (StringUtils.isEmpty(token))
        {
            unauthorized(response, "令牌不能为空");
            return;
        }

        Claims claims;
        try
        {
            claims = JwtUtils.parseToken(token);
        }
        catch (Exception e)
        {
            unauthorized(response, "令牌已过期或验证不正确");
            return;
        }
        String userKey = JwtUtils.getUserKey(claims);
        if (!redisService.hasKey(CacheConstants.LOGIN_TOKEN_KEY + userKey))
        {
            unauthorized(response, "登录状态已过期");
            return;
        }
        String userId = JwtUtils.getUserId(claims);
        String username = JwtUtils.getUserName(claims);
        if (StringUtils.isEmpty(userId) || StringUtils.isEmpty(username))
        {
            unauthorized(response, "令牌验证失败");
            return;
        }

        wrapper.putHeader(SecurityConstants.USER_KEY, ServletUtils.urlEncode(userKey));
        wrapper.putHeader(SecurityConstants.DETAILS_USER_ID, ServletUtils.urlEncode(userId));
        wrapper.putHeader(SecurityConstants.DETAILS_USERNAME, ServletUtils.urlEncode(username));
        filterChain.doFilter(wrapper, response);
    }

    private void unauthorized(HttpServletResponse response, String message)
    {
        ServletUtils.renderString(response, JSON.toJSONString(R.fail(HttpStatus.UNAUTHORIZED, message)));
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
