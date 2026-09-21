package com.ruoyi.admin.controller.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.api.domain.SysDept;
import com.ruoyi.system.api.domain.SysRole;
import com.ruoyi.system.api.domain.SysUser;
import com.ruoyi.system.api.model.LoginUser;
import com.ruoyi.system.service.ISysDeptService;
import com.ruoyi.system.service.ISysPermissionService;
import com.ruoyi.system.service.ISysUserService;

/**
 * Supplies a trusted, token-backed identity to integrated applications.
 * This endpoint is intentionally hidden by the public nginx /prod-api/internal rule.
 */
@RestController
@RequestMapping("/internal/security")
public class IntegratedSecurityContextController
{
    private final ISysUserService userService;
    private final ISysDeptService deptService;
    private final ISysPermissionService permissionService;
    private final JdbcTemplate jdbcTemplate;

    public IntegratedSecurityContextController(ISysUserService userService, ISysDeptService deptService,
            ISysPermissionService permissionService, JdbcTemplate jdbcTemplate)
    {
        this.userService = userService;
        this.deptService = deptService;
        this.permissionService = permissionService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/context")
    public R<Map<String, Object>> context()
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null || loginUser.getUserid() == null)
        {
            return R.fail(401, "Unauthenticated");
        }

        SysUser user = userService.selectUserById(loginUser.getUserid());
        if (user == null || !"0".equals(user.getStatus()) || !"0".equals(user.getDelFlag()))
        {
            return R.fail(401, "User no longer exists");
        }

        Set<String> permissions = permissionService.getMenuPermission(user);
        Set<String> roleKeys = permissionService.getRolePermission(user);
        List<SysRole> roles = user.getRoles() == null ? List.of() : user.getRoles().stream()
                .filter(role -> "0".equals(role.getStatus())).toList();
        DataScope scope = resolveDataScope(user, roles);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", user.getUserId());
        result.put("userName", user.getUserName());
        result.put("nickName", user.getNickName());
        result.put("deptId", user.getDeptId());
        result.put("deptName", user.getDept() == null ? null : user.getDept().getDeptName());
        result.put("roles", roleKeys);
        result.put("roleDetails", roles.stream().map(this::roleDetails).collect(Collectors.toList()));
        result.put("permissions", permissions);
        result.put("allData", scope.allData());
        result.put("selfOnly", scope.selfOnly());
        result.put("deptIds", scope.deptIds());
        result.put("nodeIds", resolveNodeIds(roles));
        return R.ok(result);
    }

    private Map<String, Object> roleDetails(SysRole role)
    {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("roleId", role.getRoleId());
        details.put("roleKey", role.getRoleKey());
        details.put("roleName", role.getRoleName());
        details.put("dataScope", role.getDataScope());
        return details;
    }

    private DataScope resolveDataScope(SysUser user, List<SysRole> roles)
    {
        if (user.isAdmin() || roles.stream().anyMatch(role -> "1".equals(role.getDataScope())))
        {
            return new DataScope(true, false, List.of());
        }

        Set<Long> deptIds = new LinkedHashSet<>();
        boolean hasDepartmentScope = false;
        boolean hasSelfScope = false;
        for (SysRole role : roles)
        {
            String scope = role.getDataScope();
            if ("2".equals(scope))
            {
                hasDepartmentScope = true;
                deptIds.addAll(deptService.selectDeptListByRoleId(role.getRoleId()));
            }
            else if ("3".equals(scope))
            {
                hasDepartmentScope = true;
                addIfPresent(deptIds, user.getDeptId());
            }
            else if ("4".equals(scope))
            {
                hasDepartmentScope = true;
                addDepartmentAndChildren(deptIds, user.getDeptId());
            }
            else if ("5".equals(scope))
            {
                hasSelfScope = true;
                addIfPresent(deptIds, user.getDeptId());
            }
        }
        return new DataScope(false, hasSelfScope && !hasDepartmentScope, new ArrayList<>(deptIds));
    }

    private void addDepartmentAndChildren(Set<Long> deptIds, Long parentDeptId)
    {
        if (parentDeptId == null)
        {
            return;
        }
        deptIds.add(parentDeptId);
        for (SysDept dept : deptService.selectDeptList(new SysDept()))
        {
            if (dept.getDeptId() != null && containsAncestor(dept.getAncestors(), parentDeptId))
            {
                deptIds.add(dept.getDeptId());
            }
        }
    }

    private boolean containsAncestor(String ancestors, Long deptId)
    {
        if (ancestors == null)
        {
            return false;
        }
        String target = String.valueOf(deptId);
        for (String ancestor : ancestors.split(","))
        {
            if (target.equals(ancestor.trim()))
            {
                return true;
            }
        }
        return false;
    }

    private void addIfPresent(Collection<Long> values, Long value)
    {
        if (value != null)
        {
            values.add(value);
        }
    }

    private List<Long> resolveNodeIds(List<SysRole> roles)
    {
        List<Long> roleIds = roles.stream().map(SysRole::getRoleId).filter(java.util.Objects::nonNull).toList();
        if (roleIds.isEmpty())
        {
            return List.of();
        }
        try
        {
            Integer exists = jdbcTemplate.queryForObject(
                    "select count(*) from information_schema.tables where table_schema = database() and table_name = 'sys_carbon_role_node'",
                    Integer.class);
            if (exists == null || exists == 0)
            {
                return List.of();
            }
            String placeholders = roleIds.stream().map(id -> "?").collect(Collectors.joining(","));
            return jdbcTemplate.queryForList(
                    "select distinct node_id from sys_carbon_role_node where role_id in (" + placeholders + ")",
                    Long.class, roleIds.toArray());
        }
        catch (DataAccessException ex)
        {
            return List.of();
        }
    }

    private record DataScope(boolean allData, boolean selfOnly, List<Long> deptIds)
    {
    }
}
