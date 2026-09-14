package cn.com.v2.security;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;

@Component
public class RuoYiSecurityClient
{
    private final RuoYiSecurityProperties properties;

    public RuoYiSecurityClient(RuoYiSecurityProperties properties)
    {
        this.properties = properties;
    }

    @SuppressWarnings("unchecked")
    public RuoYiSecurityContext load(String authorization)
    {
        HttpResponse response = null;
        try
        {
            response = HttpRequest.get(properties.getEndpoint())
                    .header("Authorization", authorization)
                    .timeout(properties.getTimeout())
                    .execute();
            if (!response.isOk()) throw new IllegalStateException("RuoYi rejected the current session");
            Map<String, Object> body = JSONUtil.toBean(response.body(), Map.class);
            if (!"200".equals(String.valueOf(body.get("code"))) || !(body.get("data") instanceof Map))
            {
                throw new IllegalStateException("Invalid RuoYi security response");
            }
            Map<String, Object> data = (Map<String, Object>) body.get("data");
            RuoYiSecurityContext context = new RuoYiSecurityContext();
            context.setUserId(longValue(data.get("userId")));
            context.setUserName(text(data.get("userName")));
            context.setDeptId(longValue(data.get("deptId")));
            context.setDeptName(text(data.get("deptName")));
            context.setPermissions(strings(data.get("permissions")));
            context.setAllData(Boolean.TRUE.equals(data.get("allData")));
            context.setSelfOnly(Boolean.TRUE.equals(data.get("selfOnly")));
            context.setDeptIds(longs(data.get("deptIds")));
            return context;
        }
        finally
        {
            if (response != null) response.close();
        }
    }

    private Long longValue(Object value) { return value instanceof Number ? ((Number) value).longValue() : null; }
    private String text(Object value) { return value == null ? null : String.valueOf(value); }

    private Set<String> strings(Object value)
    {
        if (!(value instanceof Iterable)) return Collections.emptySet();
        Set<String> result = new LinkedHashSet<String>();
        for (Object item : (Iterable<?>) value) result.add(String.valueOf(item));
        return result;
    }

    private List<Long> longs(Object value)
    {
        if (!(value instanceof Iterable)) return Collections.emptyList();
        List<Long> result = new ArrayList<Long>();
        for (Object item : (Iterable<?>) value)
        {
            if (item instanceof Number) result.add(((Number) item).longValue());
        }
        return result;
    }
}
