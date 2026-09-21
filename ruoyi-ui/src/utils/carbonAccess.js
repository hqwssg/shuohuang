import { hasAnyPermission, hasAnyRole } from '@/utils/permissionMatch'
import { resolveServiceUrl } from '@/utils/serviceUrl'

function resolveGoviewBaseUrl() {
  const configuredUrl = process.env.VUE_APP_GOVIEW_URL
  if (configuredUrl && configuredUrl !== 'auto') {
    const configuredBase = configuredUrl.split('/index.html')[0].replace(/\/$/, '')
    if (typeof window !== 'undefined') {
      return resolveServiceUrl(configuredBase, window.location.href).replace(/\/$/, '')
    }
    return configuredBase
  }

  if (typeof window === 'undefined') {
    return 'http://localhost:3000'
  }

  const { protocol, hostname } = window.location
  const screenPort = hostname === '127.0.0.1' || hostname === 'localhost' ? '13000' : '53231'
  return `${protocol}//${hostname}:${screenPort}`
}

const goviewBaseUrl = resolveGoviewBaseUrl()
export const GOVIEW_PROJECT_ID = process.env.VUE_APP_GOVIEW_PROJECT_ID || '2066429772333690882'
export const GOVIEW_ADMIN_URL = `${goviewBaseUrl}/index.html#/project`
export const GOVIEW_URL = `${goviewBaseUrl}/index.html#/chart/preview/${GOVIEW_PROJECT_ID}`

export const carbonModules = [
  {
    key: 'model',
    title: '碳排放模型设置',
    icon: 'el-icon-s-operation',
    color: 'green',
    path: '/carbon/model',
    routeName: 'CarbonModel',
    permissions: [
      'carbon:model:view'
    ]
  },
  {
    key: 'params',
    title: '碳排放核算参数设置',
    icon: 'el-icon-set-up',
    color: 'purple',
    path: '/carbon/params',
    routeName: 'CarbonParams',
    permissions: ['carbon:params:view']
  },
  {
    key: 'screen',
    title: '碳排放大屏展示',
    icon: 'el-icon-data-analysis',
    color: 'blue',
    path: GOVIEW_URL,
    routeName: 'CarbonScreen',
    external: true,
    permissions: ['carbon:screen:view']
  },
  {
    key: 'dataEntry',
    title: '人工数据录入',
    icon: 'el-icon-edit-outline',
    color: 'orange',
    path: '/carbon/data-entry',
    routeName: 'CarbonDataEntry',
    permissions: ['carbon:data:view', 'carbon:data:edit', 'carbon:data:submit', 'carbon:data:review', 'carbon:data:lock']
  },
  {
    key: 'statistics',
    title: '统计查询',
    icon: 'el-icon-search',
    color: 'cyan',
    path: '/carbon/statistics',
    routeName: 'CarbonStatistics',
    permissions: ['carbon:statistics:view']
  },
  {
    key: 'report',
    title: '碳排放报告生成',
    icon: 'el-icon-document',
    color: 'red',
    path: '/carbon/report',
    routeName: 'CarbonReport',
    permissions: ['carbon:report:list', 'carbon:report:query', 'carbon:report:add']
  },
  {
    key: 'system',
    title: '系统管理',
    icon: 'el-icon-setting',
    color: 'gray',
    path: '/carbon/system',
    routeName: 'CarbonSystem',
    permissions: [
      'goview:system:theme',
      'goview:system:lang',
      'goview:system:setting',
      'system:user:list',
      'system:role:list',
      'system:menu:list',
      'system:dept:list',
      'system:post:list',
      'system:config:list',
      'carbon:system:view'
    ]
  },
  {
    key: 'logs',
    title: '日志审计',
    icon: 'el-icon-document-checked',
    color: 'violet',
    path: '/carbon/logs',
    routeName: 'CarbonLogs',
    permissions: [
      'carbon:logs:view',
      'system:operlog:list',
      'system:logininfor:list',
      'monitor:job:list'
    ]
  }
]

export const carbonSystemLinks = [
  { title: '采集范围分配', icon: 'el-icon-map-location', path: '/carbon/scopes', group: 'permission', description: '分配站点、区间、燃料与热能范围的公司归属及部门权限', permissions: ['carbon:scope:view'] },
  { title: '用户管理', icon: 'el-icon-user', path: '/system/user', group: 'organization', description: '维护登录账号、用户状态与所属组织', permissions: ['system:user:list'] },
  { title: '部门管理', icon: 'el-icon-office-building', path: '/system/dept', group: 'organization', description: '维护组织架构、上下级部门与负责人', permissions: ['system:dept:list'] },
  { title: '岗位管理', icon: 'el-icon-suitcase', path: '/system/post', group: 'organization', description: '维护岗位编码、岗位名称和显示顺序', permissions: ['system:post:list'] },
  { title: '角色管理', icon: 'el-icon-s-custom', path: '/system/role', group: 'permission', description: '配置角色、数据范围和授权范围', permissions: ['system:role:list'] },
  { title: '菜单管理', icon: 'el-icon-menu', path: '/system/menu', group: 'permission', description: '维护菜单、按钮权限和路由入口', permissions: ['system:menu:list'] },
  { title: '参数设置', icon: 'el-icon-setting', path: '/system/config', group: 'configuration', description: '维护系统参数、业务开关和缓存配置', permissions: ['system:config:list', 'goview:system:setting'] },
  { title: 'GoView 设置', icon: 'el-icon-monitor', path: GOVIEW_ADMIN_URL, group: 'configuration', description: '进入可视化平台配置主题、语言和系统项', external: true, permissions: ['goview:system:theme', 'goview:system:lang', 'goview:system:setting'] }
]

export const carbonSystemGroups = [
  {
    key: 'organization',
    title: '组织与人员',
    icon: 'el-icon-office-building',
    description: '归并用户、部门和岗位等组织基础资料，便于先建立人员归属关系。'
  },
  {
    key: 'permission',
    title: '权限与菜单',
    icon: 'el-icon-s-check',
    description: '归并角色授权和菜单资源配置，集中处理访问控制相关设置。'
  },
  {
    key: 'configuration',
    title: '系统配置',
    icon: 'el-icon-setting',
    description: '归并运行参数与可视化平台配置，集中维护系统级开关和外部设置。'
  }
]

export function getCarbonModulePermissions(key) {
  const module = carbonModules.find(item => item.key === key)
  return module ? module.permissions : []
}

export function canAccessByPermissions(item, permissions, roles) {
  if (hasAnyRole(roles, ['admin'])) return true
  return hasAnyPermission(permissions, item.permissions || [])
}
