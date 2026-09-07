package cn.com.v2.common.audit;

import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONUtil;

@Service
public class RuoYiAuditClient
{
    private static final Logger log = LoggerFactory.getLogger(RuoYiAuditClient.class);

    private static final String SECRET_HEADER = "X-Goview-Audit-Secret";

    private final RuoYiAuditProperties properties;

    public RuoYiAuditClient(RuoYiAuditProperties properties)
    {
        this.properties = properties;
    }

    public void recordOperation(Map<String, Object> payload)
    {
        send("/operation", payload);
    }

    public void recordLogin(String username, boolean success, String ipAddress, String message)
    {
        Map<String, Object> payload = new LinkedHashMap<String, Object>();
        payload.put("userName", defaultText(username, "anonymous"));
        payload.put("status", success ? "0" : "1");
        payload.put("ipaddr", ipAddress);
        payload.put("msg", message);
        send("/login", payload);
    }

    private void send(String path, Map<String, Object> payload)
    {
        if (!properties.isEnabled())
        {
            return;
        }
        if (StrUtil.isBlank(properties.getBaseUrl()) || StrUtil.isBlank(properties.getSecret()))
        {
            log.error("RuoYi audit is enabled but base-url or secret is empty");
            return;
        }

        String url = StrUtil.removeSuffix(properties.getBaseUrl(), "/") + path;
        for (int attempt = 1; attempt <= 2; attempt++)
        {
            HttpResponse response = null;
            try
            {
                response = HttpRequest.post(url)
                        .header("Content-Type", "application/json;charset=UTF-8")
                        .header(SECRET_HEADER, properties.getSecret())
                        .timeout(properties.getTimeout())
                        .body(JSONUtil.toJsonStr(payload))
                        .execute();
                if (response.isOk())
                {
                    return;
                }
                log.warn("RuoYi audit request failed: attempt={}, path={}, status={}, body={}", attempt, path,
                        response.getStatus(), limit(response.body(), 500));
            }
            catch (Exception e)
            {
                log.warn("RuoYi audit request failed: attempt={}, path={}", attempt, path, e);
            }
            finally
            {
                if (response != null)
                {
                    response.close();
                }
            }
            if (attempt == 1)
            {
                try
                {
                    Thread.sleep(150L);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        log.error("RuoYi audit delivery failed after retry: path={}", path);
    }

    private String defaultText(String value, String fallback)
    {
        return StrUtil.isBlank(value) ? fallback : value;
    }

    private String limit(String value, int maxLength)
    {
        return value == null || value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
