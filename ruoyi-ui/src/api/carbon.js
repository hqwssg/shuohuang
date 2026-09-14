import axios from 'axios'
import { Message } from 'element-ui'
import { getToken } from '@/utils/auth'

const carbonRequest = axios.create({
  baseURL: process.env.VUE_APP_CARBON_API || '/carbon-api',
  timeout: 15000
})

carbonRequest.interceptors.request.use(config => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

carbonRequest.interceptors.response.use(
  response => response.data,
  error => {
    const response = error.response || {}
    const data = response.data || {}
    const isProxyError = typeof data === 'string' && data.indexOf('Proxy error') !== -1
    const message = data.error || data.message || (isProxyError ? '碳排放服务未启动或代理配置异常' : error.message) || '碳排放服务连接异常'
    if (!error.config || !error.config.silent) {
      Message.error(message)
    }
    return Promise.reject(error)
  }
)

function cleanParams(params) {
  const result = {}
  Object.keys(params || {}).forEach(key => {
    const value = params[key]
    if (value !== undefined && value !== null && value !== '') {
      result[key] = value
    }
  })
  return result
}

export const templateApi = {
  list(config = {}) {
    return carbonRequest.get('/template/list', config)
  },
  detail(id) {
    return carbonRequest.get(`/template/${id}`)
  },
  create(data) {
    return carbonRequest.post('/template/create', data)
  },
  update(id, data) {
    return carbonRequest.put(`/template/${id}`, data)
  },
  updateProperties(id, data) {
    return carbonRequest.put(`/template/${id}/properties`, data)
  },
  copy(id, data) {
    return carbonRequest.post(`/template/copy/${id}`, data)
  },
  remove(id) {
    return carbonRequest.delete(`/template/${id}`)
  }
}

export const nodeApi = {
  tree(templateId, config = {}) {
    return carbonRequest.get('/nodes/tree', Object.assign({}, config, { params: cleanParams({ templateId }) }))
  },
  detail(id) {
    return carbonRequest.get(`/nodes/${id}`)
  },
  create(data) {
    return carbonRequest.post('/nodes', data)
  },
  update(id, data) {
    return carbonRequest.put(`/nodes/${id}`, data)
  },
  remove(id) {
    return carbonRequest.delete(`/nodes/${id}`)
  },
  config(id) {
    return carbonRequest.get(`/nodes/${id}/config`)
  },
  updateConfig(id, data) {
    return carbonRequest.put(`/nodes/${id}/config`, data)
  },
  options() {
    return carbonRequest.get('/nodes/options')
  }
}

export const emissionApi = {
  create(data) {
    return carbonRequest.post('/emission-data', data)
  },
  batchCreate(data) {
    return carbonRequest.post('/emission-data/batch', data)
  },
  update(id, data) {
    return carbonRequest.put(`/emission-data/${id}`, data)
  },
  remove(id) {
    return carbonRequest.delete(`/emission-data/${id}`)
  },
  detail(id) {
    return carbonRequest.get(`/emission-data/${id}`)
  },
  query(params, config = {}) {
    return carbonRequest.get('/emission-data/query', Object.assign({}, config, { params: cleanParams(params) }))
  },
  statistics(params) {
    return carbonRequest.get('/emission-data/statistics', { params: cleanParams(params) })
  },
  submit(id, data) {
    return carbonRequest.post(`/emission-data/${id}/submit`, data)
  },
  approve(id, data) {
    return carbonRequest.post(`/emission-data/${id}/approve`, data)
  },
  reject(id, data) {
    return carbonRequest.post(`/emission-data/${id}/reject`, data)
  },
  lock(id, data) {
    return carbonRequest.post(`/emission-data/${id}/lock`, data)
  },
  voidEntry(id, data) {
    return carbonRequest.post(`/emission-data/${id}/void`, data)
  },
  audits(id) {
    return carbonRequest.get(`/emission-data/${id}/audits`)
  },
  totalEmission(nodeId, params) {
    return carbonRequest.get(`/emission-data/${nodeId}/total-emission`, { params: cleanParams(params) })
  },
  averageQuality(nodeId, params) {
    return carbonRequest.get(`/emission-data/${nodeId}/average-quality`, { params: cleanParams(params) })
  },
  monthlyStatistics(nodeId) {
    return carbonRequest.get(`/emission-data/${nodeId}/monthly-statistics`)
  }
}

export function defaultCarbonOptions() {
  return {
    locomotiveTypes: ['HXN3型内燃机车', '国能八轴交流机车', '国能十二轴交流机车'],
    statisticalCalibers: ['生产排放', '生产辅助排放', '附属办公排放'],
    emissionCategories: ['购入的电力', '购入的热力', '化石燃料', '废弃物处理'],
    emissionSubcategories: {
      '购入的电力': ['生产设施用电', '辅助生产系统用电', '附属生产系统用电'],
      '购入的热力': ['热力数据', '质量单位计量的热水', '质量单位计量的蒸汽'],
      '化石燃料': ['汽油', '柴油', '原油', '燃料油', '天然气'],
      '废弃物处理': ['固体废弃物处理排放', '废水处理排放']
    },
    dataSources: ['手工录入', '数据库', 'API输入']
  }
}

export function normalizeCarbonOptions(options) {
  const defaults = defaultCarbonOptions()
  const result = Object.assign({}, defaults, options || {})
  Object.keys(result).forEach(key => {
    if (key !== 'emissionSubcategories' && !Array.isArray(result[key])) {
      result[key] = Array.from(result[key] || [])
    }
  })
  result.emissionSubcategories = result.emissionSubcategories || defaults.emissionSubcategories
  return result
}

export function flattenCarbonTree(tree) {
  const list = []
  const walk = (node, level) => {
    if (!node) return
    list.push(Object.assign({}, node, { level }))
    ;(node.children || []).forEach(child => walk(child, level + 1))
  }
  walk(tree, 0)
  return list
}
