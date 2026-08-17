package com.ruoyi.test.admin.captcha;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ruoyi.admin.captcha.CaptchaService;
import com.ruoyi.admin.config.properties.CaptchaProperties;
import com.ruoyi.common.core.constant.CacheConstants;
import com.ruoyi.common.core.exception.CaptchaException;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.redis.service.RedisService;
import org.junit.jupiter.api.Test;

class CaptchaServiceTest
{
    @Test
    void createCaptchaReturnsDisabledFlagWithoutTouchingRedis()
            throws Exception
    {
        RedisService redisService = mock(RedisService.class);
        CaptchaProperties properties = new CaptchaProperties();
        properties.setEnabled(false);
        CaptchaService service = new CaptchaService(redisService, properties);

        AjaxResult result = service.createCaptcha();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result).containsEntry("captchaEnabled", false)
                .doesNotContainKeys("uuid", "img");
        verifyNoInteractions(redisService);
    }

    @Test
    void checkCaptchaSkipsValidationWhenCaptchaIsDisabled()
    {
        RedisService redisService = mock(RedisService.class);
        CaptchaProperties properties = new CaptchaProperties();
        properties.setEnabled(false);
        CaptchaService service = new CaptchaService(redisService, properties);

        service.checkCaptcha("", "");

        verifyNoInteractions(redisService);
    }

    @Test
    void checkCaptchaRejectsBlankCodeBeforeReadingCache()
    {
        RedisService redisService = mock(RedisService.class);
        CaptchaService service = new CaptchaService(redisService, new CaptchaProperties());

        assertThatThrownBy(() -> service.checkCaptcha("", "uuid"))
                .isInstanceOf(CaptchaException.class);
        verifyNoInteractions(redisService);
    }

    @Test
    void checkCaptchaRejectsExpiredCode()
    {
        RedisService redisService = mock(RedisService.class);
        CaptchaService service = new CaptchaService(redisService, new CaptchaProperties());
        String key = CacheConstants.CAPTCHA_CODE_KEY + "uuid";
        when(redisService.getCacheObject(key)).thenReturn(null);

        assertThatThrownBy(() -> service.checkCaptcha("1234", "uuid"))
                .isInstanceOf(CaptchaException.class);

        verify(redisService).getCacheObject(key);
        verify(redisService, never()).deleteObject(key);
    }

    @Test
    void checkCaptchaAcceptsCodeIgnoringCaseAndClearsCache()
    {
        RedisService redisService = mock(RedisService.class);
        CaptchaService service = new CaptchaService(redisService, new CaptchaProperties());
        String key = CacheConstants.CAPTCHA_CODE_KEY + "uuid";
        when(redisService.getCacheObject(key)).thenReturn("AbC1");

        service.checkCaptcha("abc1", "uuid");

        verify(redisService).getCacheObject(key);
        verify(redisService).deleteObject(key);
    }

    @Test
    void checkCaptchaRejectsMismatchedCodeAfterCacheLookup()
    {
        RedisService redisService = mock(RedisService.class);
        CaptchaService service = new CaptchaService(redisService, new CaptchaProperties());
        String key = CacheConstants.CAPTCHA_CODE_KEY + "uuid";
        when(redisService.getCacheObject(key)).thenReturn("AbC1");

        assertThatThrownBy(() -> service.checkCaptcha("zzzz", "uuid"))
                .isInstanceOf(CaptchaException.class);

        verify(redisService).deleteObject(key);
    }
}
