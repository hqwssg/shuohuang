const ALL_PERMISSION = '*:*:*'
const SUPER_ADMIN = 'admin'

function escapeRegExp(value) {
  return String(value).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

export function matchPermission(userPermission, requiredPermission) {
  if (!userPermission || !requiredPermission) return false
  if (userPermission === ALL_PERMISSION || userPermission === requiredPermission) return true

  const pattern = '^' + escapeRegExp(userPermission).replace(/\\\*/g, '.*') + '$'
  return new RegExp(pattern).test(requiredPermission)
}

export function hasAnyPermission(userPermissions, requiredPermissions) {
  const permissions = Array.isArray(userPermissions) ? userPermissions : []
  const required = Array.isArray(requiredPermissions) ? requiredPermissions : [requiredPermissions]
  if (!required.length) return true

  return permissions.some(permission => {
    return required.some(item => matchPermission(permission, item))
  })
}

export function hasEveryPermission(userPermissions, requiredPermissions) {
  const permissions = Array.isArray(userPermissions) ? userPermissions : []
  const required = Array.isArray(requiredPermissions) ? requiredPermissions : [requiredPermissions]
  if (!required.length) return true

  return required.every(item => {
    return permissions.some(permission => matchPermission(permission, item))
  })
}

export function hasAnyRole(userRoles, requiredRoles) {
  const roles = Array.isArray(userRoles) ? userRoles : []
  const required = Array.isArray(requiredRoles) ? requiredRoles : [requiredRoles]
  if (!required.length) return true

  return roles.some(role => role === SUPER_ADMIN || required.includes(role))
}

export function hasEveryRole(userRoles, requiredRoles) {
  const roles = Array.isArray(userRoles) ? userRoles : []
  const required = Array.isArray(requiredRoles) ? requiredRoles : [requiredRoles]
  if (!required.length) return true
  if (roles.includes(SUPER_ADMIN)) return true

  return required.every(role => roles.includes(role))
}
