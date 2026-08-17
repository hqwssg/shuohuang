import router from './router'
import store from './store'
import { Message } from 'element-ui'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { isPathMatch } from '@/utils/validate'
import { isRelogin } from '@/utils/request'
import auth from '@/plugins/auth'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register']

const isWhiteList = (path) => {
  return whiteList.some(pattern => isPathMatch(pattern, path))
}

const hasRouteAccess = (route) => {
  if (auth.hasRole('admin')) return true

  const permissions = []
  const roles = []
  route.matched.forEach(record => {
    if (record.permissions) {
      permissions.push(...record.permissions)
    }
    if (record.roles) {
      roles.push(...record.roles)
    }
    if (record.meta && record.meta.permissions) {
      permissions.push(...record.meta.permissions)
    }
    if (record.meta && record.meta.roles) {
      roles.push(...record.meta.roles)
    }
  })
  if (permissions.length && !auth.hasPermiOr(permissions)) return false
  if (roles.length && !auth.hasRoleOr(roles)) return false
  return true
}

router.beforeEach((to, from, next) => {
  NProgress.start()
  if (getToken()) {
    to.meta.title && store.dispatch('settings/setTitle', to.meta.title)
    const isLock = store.getters.isLock
    /* has token*/
    if (to.path === '/login') {
      next({ path: '/' })
      NProgress.done()
    } else if (isWhiteList(to.path)) {
      next()
    } else if (isLock && to.path !== '/lock') {
      next({ path: '/lock' })
      NProgress.done()
    } else if (!isLock && to.path === '/lock') {
      next({ path: '/' })
      NProgress.done()
    } else {
      if (store.getters.roles.length === 0) {
        isRelogin.show = true
        // 判断当前用户是否已拉取完user_info信息
        store.dispatch('GetInfo').then(() => {
          isRelogin.show = false
          store.dispatch('GenerateRoutes').then(accessRoutes => {
            // 根据roles权限生成可访问的路由表
            router.addRoutes(accessRoutes) // 动态添加可访问路由表
            if (hasRouteAccess(to)) {
              next({ ...to, replace: true }) // hack方法 确保addRoutes已完成
            } else {
              next({ path: '/401' })
            }
          })
        }).catch(err => {
            store.dispatch('LogOut').then(() => {
              Message.error(err)
              next({ path: '/' })
            })
          })
      } else {
        if (hasRouteAccess(to)) {
          next()
        } else {
          next({ path: '/401' })
        }
      }
    }
  } else {
    // 没有token
    if (isWhiteList(to.path)) {
      // 在免登录白名单，直接进入
      next()
    } else {
      next(`/login?redirect=${encodeURIComponent(to.fullPath)}`) // 否则全部重定向到登录页
      NProgress.done()
    }
  }
})

router.afterEach(() => {
  NProgress.done()
})
