package com.ruoyi.admin.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "security.captcha")
public class CaptchaProperties
{
    private Boolean enabled = Boolean.TRUE;

    private String type = "math";

    public Boolean getEnabled()
    {
        return enabled;
    }

    public void setEnabled(Boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }
}
