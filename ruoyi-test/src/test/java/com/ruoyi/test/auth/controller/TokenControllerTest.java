package com.ruoyi.test.auth.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ruoyi.auth.controller.TokenController;
import com.ruoyi.auth.form.LoginBody;
import com.ruoyi.auth.form.RegisterBody;
import com.ruoyi.auth.form.UnLockBody;
import com.ruoyi.auth.service.SysLoginService;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.security.service.TokenService;
import com.ruoyi.system.api.model.LoginUser;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

class TokenControllerTest
{
    private TokenService tokenService;

    private SysLoginService sysLoginService;

    private TokenController controller;

    @BeforeEach
    void setUp()
    {
        tokenService = mock(TokenService.class);
        sysLoginService = mock(SysLoginService.class);
        controller = new TokenController();
        ReflectionTestUtils.setField(controller, "tokenService", tokenService);
        ReflectionTestUtils.setField(controller, "sysLoginService", sysLoginService);
    }

    @Test
    void loginDelegatesCredentialCheckAndReturnsCreatedToken()
    {
        LoginBody form = new LoginBody();
        form.setUsername("admin");
        form.setPassword("password");
        LoginUser loginUser = new LoginUser();
        Map<String, Object> token = Map.of("access_token", "jwt-token", "expires_in", 720L);
        when(sysLoginService.login("admin", "password")).thenReturn(loginUser);
        when(tokenService.createToken(loginUser)).thenReturn(token);

        R<?> result = controller.login(form);

        assertThat(result.getCode()).isEqualTo(R.SUCCESS);
        assertThat(result.getData()).isEqualTo(token);
        verify(sysLoginService).login("admin", "password");
        verify(tokenService).createToken(loginUser);
    }

    @Test
    void logoutWithoutTokenIsAlwaysSuccessfulAndDoesNotTouchServices()
    {
        R<?> result = controller.logout(new MockHttpServletRequest());

        assertThat(result.getCode()).isEqualTo(R.SUCCESS);
        verifyNoInteractions(sysLoginService);
        verifyNoInteractions(tokenService);
    }

    @Test
    void refreshRenewsExistingLoginUser()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        LoginUser loginUser = new LoginUser();
        when(tokenService.getLoginUser(request)).thenReturn(loginUser);

        R<?> result = controller.refresh(request);

        assertThat(result.getCode()).isEqualTo(R.SUCCESS);
        verify(tokenService).refreshToken(loginUser);
    }

    @Test
    void registerDelegatesToLoginService()
    {
        RegisterBody body = new RegisterBody();
        body.setUsername("new-user");
        body.setPassword("new-password");

        R<?> result = controller.register(body);

        assertThat(result.getCode()).isEqualTo(R.SUCCESS);
        verify(sysLoginService).register("new-user", "new-password");
    }

    @Test
    void unlockScreenDelegatesPasswordValidation()
    {
        UnLockBody body = new UnLockBody();
        body.setPassword("secret");

        R<?> result = controller.unlockScreen(body);

        assertThat(result.getCode()).isEqualTo(R.SUCCESS);
        verify(sysLoginService).unlock("secret");
    }
}
