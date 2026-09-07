import store from '@/store'
import { hasAnyPermission, hasEveryPermission, hasAnyRole, hasEveryRole } from '@/utils/permissionMatch'

function authPermission(permission) {
  const permissions = store.getters && store.getters.permissions
  if (permission && permission.length > 0) {
    return hasAnyPermission(permissions, [permission])
  } else {
    return false
  }
}

function authRole(role) {
  const roles = store.getters && store.getters.roles
  if (role && role.length > 0) {
    return hasAnyRole(roles, [role])
  } else {
    return false
  }
}

export default {
  // 验证用户是否具备某权限
  hasPermi(permission) {
    return authPermission(permission)
  },
  // 验证用户是否含有指定权限，只需包含其中一个
  hasPermiOr(permissions) {
    return hasAnyPermission(store.getters && store.getters.permissions, permissions)
  },
  // 验证用户是否含有指定权限，必须全部拥有
  hasPermiAnd(permissions) {
    return hasEveryPermission(store.getters && store.getters.permissions, permissions)
  },
  // 验证用户是否具备某角色
  hasRole(role) {
    return authRole(role)
  },
  // 验证用户是否含有指定角色，只需包含其中一个
  hasRoleOr(roles) {
    return hasAnyRole(store.getters && store.getters.roles, roles)
  },
  // 验证用户是否含有指定角色，必须全部拥有
  hasRoleAnd(roles) {
    return hasEveryRole(store.getters && store.getters.roles, roles)
  }
}
