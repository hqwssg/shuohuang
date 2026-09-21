package com.ruoyi.admin.local;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.system.api.RemoteUserService;
import com.ruoyi.system.api.domain.SysUser;
import com.ruoyi.system.api.model.LoginUser;
import com.ruoyi.system.api.domain.SysDept;
import com.ruoyi.system.service.ISysDeptService;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.system.service.ISysPermissionService;
import com.ruoyi.system.service.ISysUserService;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class LocalRemoteUserService implements RemoteUserService
{
    private final ISysUserService userService;

    private final ISysPermissionService permissionService;

    private final ISysConfigService configService;

    private final ISysDeptService deptService;

    public LocalRemoteUserService(ISysUserService userService, ISysPermissionService permissionService,
            ISysConfigService configService, ISysDeptService deptService)
    {
        this.userService = userService;
        this.permissionService = permissionService;
        this.configService = configService;
        this.deptService = deptService;
    }

    @Override
    public R<LoginUser> getUserInfo(String username, String source)
    {
        SysUser sysUser = userService.selectUserByUserName(username);
        if (StringUtils.isNull(sysUser))
        {
            return R.fail("用户名或密码错误");
        }
        Set<String> roles = permissionService.getRolePermission(sysUser);
        Set<String> permissions = permissionService.getMenuPermission(sysUser);
        LoginUser loginUser = new LoginUser();
        loginUser.setSysUser(sysUser);
        loginUser.setRoles(roles);
        loginUser.setPermissions(permissions);
        return R.ok(loginUser);
    }

    @Override
    public R<Boolean> registerUserInfo(SysUser sysUser, String source)
    {
        String username = sysUser.getUserName();
        if (!"true".equals(configService.selectConfigByKey("sys.account.registerUser")))
        {
            return R.fail("当前系统没有开启注册功能");
        }
        if (!userService.checkUserNameUnique(sysUser))
        {
            return R.fail("保存用户'" + username + "'失败，注册账号已存在");
        }
        if (sysUser.getDeptId() != null && sysUser.getDeptId() != 0)
        {
            SysDept dept = deptService.selectDeptById(sysUser.getDeptId());
            if (dept == null || !"0".equals(dept.getStatus()) || !"0".equals(dept.getDelFlag()))
            {
                return R.fail("所选公司、部门或工队不存在或已停用");
            }
        }
        return R.ok(userService.registerUser(sysUser));
    }

    @Override
    public R<Boolean> recordUserLogin(SysUser sysUser, String source)
    {
        return R.ok(userService.updateLoginInfo(sysUser));
    }
}
