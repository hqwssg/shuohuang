import request from '@/utils/request'

export function listTasks(query) {
  return request({ url: '/carbon/report/tasks', method: 'get', params: query })
}

export function createTask(data) {
  return request({ url: '/carbon/report/tasks', method: 'post', data })
}

export function getDraft(id) {
  return request({ url: '/carbon/report/tasks/' + id + '/draft', method: 'get' })
}

export function saveSection(id, section, data) {
  return request({ url: '/carbon/report/tasks/' + id + '/sections/' + section, method: 'put', data })
}

export function listCalculations(params) {
  return request({ url: '/carbon/report/source/calculations', method: 'get', params })
}

export function prefillPreview(id, data) {
  return request({ url: '/carbon/report/tasks/' + id + '/prefill/preview', method: 'post', data })
}

export function prefillCommit(id, data) {
  return request({ url: '/carbon/report/tasks/' + id + '/prefill/commit', method: 'post', data })
}

export function validateTask(id) {
  return request({ url: '/carbon/report/tasks/' + id + '/validate', method: 'post' })
}

export function getFactorPlan(id) {
  return request({ url: '/carbon/report/tasks/' + id + '/factor-plan', method: 'get' })
}

export function createGenerationJob(id, data) {
  return request({ url: '/carbon/report/tasks/' + id + '/generation-jobs', method: 'post', data })
}

export function getLatestGenerationJob(id) {
  return request({ url: '/carbon/report/tasks/' + id + '/generation-jobs/latest', method: 'get' })
}

export function getGenerationJob(id, jobId) {
  return request({ url: '/carbon/report/tasks/' + id + '/generation-jobs/' + jobId, method: 'get' })
}

export function getDeptProfile(deptId) {
  return request({ url: '/carbon/report/dept-profile/' + deptId, method: 'get' })
}

export function saveDeptProfile(deptId, data) {
  return request({ url: '/carbon/report/dept-profile/' + deptId, method: 'put', data })
}

export function removeTask(id) {
  return request({ url: '/carbon/report/tasks/' + id, method: 'delete' })
}

export function uploadReportImage(id, role, file) {
  const data = new FormData()
  data.append('file', file)
  return request({
    url: '/carbon/report/tasks/' + id + '/images/' + role,
    method: 'post',
    data,
    headers: { repeatSubmit: false },
    timeout: 20000
  })
}

export function resetReportImage(id, role) {
  return request({ url: '/carbon/report/tasks/' + id + '/images/' + role, method: 'delete' })
}

export function fetchReportImageBlob(url) {
  return request({ url, method: 'get', responseType: 'blob', timeout: 20000 })
}

export function downloadReportArtifact(id) {
  return request({
    url: '/carbon/report/artifacts/' + id + '/download',
    method: 'get',
    responseType: 'blob',
    timeout: 60000
  })
}
