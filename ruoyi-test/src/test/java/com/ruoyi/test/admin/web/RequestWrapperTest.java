package com.ruoyi.test.admin.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.ruoyi.admin.web.CachedBodyRequestWrapper;
import com.ruoyi.admin.web.MutableHeaderRequestWrapper;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.util.StreamUtils;

class RequestWrapperTest
{
    @Test
    void cachedBodyRequestWrapperAllowsBodyToBeReadMoreThanOnce() throws Exception
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContentType("application/json");
        request.setContent("{\"name\":\"ruoyi\"}".getBytes(StandardCharsets.UTF_8));

        CachedBodyRequestWrapper wrapper = new CachedBodyRequestWrapper(request);

        assertThat(new String(wrapper.getBody(), StandardCharsets.UTF_8)).isEqualTo("{\"name\":\"ruoyi\"}");
        assertThat(StreamUtils.copyToString(wrapper.getInputStream(), StandardCharsets.UTF_8))
                .isEqualTo("{\"name\":\"ruoyi\"}");
        assertThat(StreamUtils.copyToString(wrapper.getInputStream(), StandardCharsets.UTF_8))
                .isEqualTo("{\"name\":\"ruoyi\"}");
    }

    @Test
    void mutableHeaderRequestWrapperOverridesAndRemovesHeadersCaseInsensitively()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User-Id", "100");

        MutableHeaderRequestWrapper wrapper = new MutableHeaderRequestWrapper(request);
        wrapper.putHeader("x-user-id", "200");
        wrapper.putHeader("X-Trace-Id", "trace-001");

        assertThat(wrapper.getHeader("X-User-Id")).isEqualTo("200");
        assertThat(wrapper.getHeader("x-trace-id")).isEqualTo("trace-001");
        assertThat(Collections.list(wrapper.getHeaderNames()).stream()
                .map(name -> name.toLowerCase(Locale.ROOT)))
                .contains("x-user-id", "x-trace-id");

        wrapper.removeHeader("X-USER-ID");

        assertThat(wrapper.getHeader("x-user-id")).isNull();
        assertThat(Collections.list(wrapper.getHeaders("x-user-id"))).isEmpty();
    }
}
