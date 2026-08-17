package com.ruoyi.admin.captcha;

import com.google.code.kaptcha.Producer;
import com.ruoyi.admin.config.properties.CaptchaProperties;
import com.ruoyi.common.core.constant.CacheConstants;
import com.ruoyi.common.core.constant.Constants;
import com.ruoyi.common.core.exception.CaptchaException;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.common.core.utils.sign.Base64;
import com.ruoyi.common.core.utils.uuid.IdUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.redis.service.RedisService;
import jakarta.annotation.Resource;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.util.FastByteArrayOutputStream;

@Service
public class CaptchaService
{
    @Resource(name = "captchaProducer")
    private Producer captchaProducer;

    @Resource(name = "captchaProducerMath")
    private Producer captchaProducerMath;

    private final RedisService redisService;

    private final CaptchaProperties captchaProperties;

    public CaptchaService(RedisService redisService, CaptchaProperties captchaProperties)
    {
        this.redisService = redisService;
        this.captchaProperties = captchaProperties;
    }

    public AjaxResult createCaptcha() throws IOException
    {
        AjaxResult ajax = AjaxResult.success();
        boolean captchaEnabled = Boolean.TRUE.equals(captchaProperties.getEnabled());
        ajax.put("captchaEnabled", captchaEnabled);
        if (!captchaEnabled)
        {
            return ajax;
        }

        String uuid = IdUtils.simpleUUID();
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + uuid;
        String capStr = null;
        String code = null;
        BufferedImage image = null;

        if ("math".equals(captchaProperties.getType()))
        {
            String capText = captchaProducerMath.createText();
            capStr = capText.substring(0, capText.lastIndexOf("@"));
            code = capText.substring(capText.lastIndexOf("@") + 1);
            image = captchaProducerMath.createImage(capStr);
        }
        else if ("char".equals(captchaProperties.getType()))
        {
            capStr = code = captchaProducer.createText();
            image = captchaProducer.createImage(capStr);
        }

        redisService.setCacheObject(verifyKey, code, Constants.CAPTCHA_EXPIRATION, TimeUnit.MINUTES);
        FastByteArrayOutputStream os = new FastByteArrayOutputStream();
        ImageIO.write(image, "jpg", os);
        ajax.put("uuid", uuid);
        ajax.put("img", Base64.encode(os.toByteArray()));
        return ajax;
    }

    public void checkCaptcha(String code, String uuid)
    {
        if (!Boolean.TRUE.equals(captchaProperties.getEnabled()))
        {
            return;
        }
        if (StringUtils.isEmpty(code))
        {
            throw new CaptchaException("验证码不能为空");
        }
        String verifyKey = CacheConstants.CAPTCHA_CODE_KEY + StringUtils.nvl(uuid, "");
        String captcha = redisService.getCacheObject(verifyKey);
        if (captcha == null)
        {
            throw new CaptchaException("验证码已失效");
        }
        redisService.deleteObject(verifyKey);
        if (!code.equalsIgnoreCase(captcha))
        {
            throw new CaptchaException("验证码错误");
        }
    }
}
