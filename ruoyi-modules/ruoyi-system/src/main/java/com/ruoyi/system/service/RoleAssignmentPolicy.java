package com.ruoyi.system.service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.ruoyi.common.core.exception.ServiceException;
import com.ruoyi.common.security.utils.SecurityUtils;
import com.ruoyi.system.api.domain.SysRole;
import com.ruoyi.system.api.model.LoginUser;

/** Prevents regional and department administrators from escalating privileges. */
@Service
public class RoleAssignmentPolicy {
    private static final Set<String> COMPANY_LEVEL = Set.of("company_admin");
    private static final Set<String> REGION_LEVEL = Set.of("region_admin");
    private static final Set<String> DEPARTMENT_LEVEL = Set.of("department_admin");

    private final ISysRoleService roleService;

    public RoleAssignmentPolicy(ISysRoleService roleService) {
        this.roleService = roleService;
    }

    public void checkAssignable(Long[] roleIds) {
        if (roleIds == null || roleIds.length == 0) return;
        List<SysRole> requested = Arrays.stream(roleIds).map(roleService::selectRoleById).toList();
        if (requested.stream().anyMatch(java.util.Objects::isNull)) {
            throw new ServiceException("Role does not exist");
        }
        for (SysRole role : requested) {
            if (!canAssign(role)) {
                throw new ServiceException("Cannot assign a role at or above the current administrator level: " + role.getRoleName());
            }
        }
    }

    public List<SysRole> filterAssignable(List<SysRole> roles) {
        return roles.stream().filter(this::canAssign).toList();
    }

    private boolean canAssign(SysRole target) {
        if (target == null || target.getRoleKey() == null) return false;
        if (SecurityUtils.isAdmin()) return true;
        Set<String> current = currentRoles();
        if (current.stream().anyMatch(COMPANY_LEVEL::contains)) return true;
        if (current.stream().anyMatch(REGION_LEVEL::contains)) {
            return !COMPANY_LEVEL.contains(target.getRoleKey()) && !REGION_LEVEL.contains(target.getRoleKey());
        }
        if (current.stream().anyMatch(DEPARTMENT_LEVEL::contains)) {
            return !COMPANY_LEVEL.contains(target.getRoleKey()) && !REGION_LEVEL.contains(target.getRoleKey())
                    && !DEPARTMENT_LEVEL.contains(target.getRoleKey());
        }
        return false;
    }

    private Set<String> currentRoles() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        return loginUser == null || loginUser.getRoles() == null ? Set.of() : loginUser.getRoles();
    }
}
