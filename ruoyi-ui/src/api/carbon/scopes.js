import request from '@/utils/request'

export const scopeOptions = () => request({ url: '/carbon/scopes/options', method: 'get' })
export const listScopes = type => request({ url: '/carbon/scopes', method: 'get', params: { type } })
export const saveScope = (type, id, data) => request({ url: `/carbon/scopes/${type}/${id}`, method: 'put', data })
export const listScopeNodes = (type, id) => request({ url: `/carbon/scopes/${type}/${id}/nodes`, method: 'get' })
export const saveNodeGrant = (type, scopeId, nodeType, nodeId, data) => request({ url: `/carbon/scopes/${type}/${scopeId}/nodes/${nodeType}/${nodeId}`, method: 'put', data })
