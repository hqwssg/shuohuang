 /**
 * v-hasRole 角色权限处理
 * Copyright (c) 2019 ruoyi
 */

import store from '@/store'
import { hasAnyRole } from '@/utils/permissionMatch'

export default {
  inserted(el, binding, vnode) {
    const { value } = binding
    const roles = store.getters && store.getters.roles

    if (value && value instanceof Array && value.length > 0) {
      const hasRole = hasAnyRole(roles, value)

      if (!hasRole) {
        el.parentNode && el.parentNode.removeChild(el)
      }
    } else {
      throw new Error(`请设置角色权限标签值"`)
    }
  }
}
