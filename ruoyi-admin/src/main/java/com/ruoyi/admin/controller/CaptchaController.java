package com.ruoyi.admin.controller;

import com.ruoyi.admin.captcha.CaptchaService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import java.io.IOException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CaptchaController
{
    private final CaptchaService captchaService;

    public CaptchaController(CaptchaService captchaService)
    {
        this.captchaService = captchaService;
    }

    @GetMapping("/code")
    public AjaxResult code() throws IOException
    {
        return captchaService.createCaptcha();
    }
}
