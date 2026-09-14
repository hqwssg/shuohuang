export function hasPermission(user, required) {
  const permissions = Array.isArray(user?.permissions) ? user.permissions : []
  return permissions.some(granted => {
    if (granted === '*:*:*' || granted === required) return true
    const escaped = String(granted).replace(/[.+?^${}()|[\]\\]/g, '\\$&').replace(/\*/g, '.*')
    return new RegExp(`^${escaped}$`).test(required)
  })
}
