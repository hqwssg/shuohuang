/**
 * 按项目 ID 或名称覆盖更新碳排放全景大屏内容（不新建项目）
 * Run: cd go-view-master-fetch && node scripts/upsert-carbon-dashboard-project.mjs
 *
 * Env:
 *   GOVIEW_API_BASE     默认 http://localhost:8083/api/goview
 *   GOVIEW_USERNAME     默认 admin
 *   GOVIEW_PASSWORD     默认 admin
 *   GOVIEW_PROJECT_ID   默认 2066429772333690882
 *   GOVIEW_PROJECT_NAME 默认 朔黄铁路碳排放全景大屏（ID 无效时按名称查找）
 */
import { buildCarbonDashboardStorage } from './generate-carbon-dashboard.mjs'

const API_BASE = process.env.GOVIEW_API_BASE ?? 'http://localhost:8083/api/goview'
const USERNAME = process.env.GOVIEW_USERNAME ?? 'admin'
const PASSWORD = process.env.GOVIEW_PASSWORD ?? 'admin'
const PROJECT_ID = process.env.GOVIEW_PROJECT_ID ?? '2066429772333690882'
const PROJECT_NAME = process.env.GOVIEW_PROJECT_NAME ?? '朔黄铁路碳排放全景大屏'
const PROJECT_REMARKS = '内置演示数据 · 碳排放全景视图'

async function postJson(path, body, headers = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', ...headers },
    body: JSON.stringify(body)
  })
  const data = await res.json()
  if (data.code !== 200) {
    throw new Error(`${path} failed: code=${data.code} msg=${data.msg ?? data.message ?? JSON.stringify(data)}`)
  }
  return data
}

async function getJson(path, params = {}, headers = {}) {
  const qs = new URLSearchParams(params).toString()
  const url = qs ? `${API_BASE}${path}?${qs}` : `${API_BASE}${path}`
  const res = await fetch(url, { method: 'GET', headers })
  const data = await res.json()
  if (data.code !== 200) {
    throw new Error(`${path} failed: code=${data.code} msg=${data.msg ?? data.message ?? JSON.stringify(data)}`)
  }
  return data
}

async function postForm(path, form, headers = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    method: 'POST',
    headers: { ...headers },
    body: form
  })
  const data = await res.json()
  if (data.code !== 200) {
    throw new Error(`${path} failed: code=${data.code} msg=${data.msg ?? data.message ?? JSON.stringify(data)}`)
  }
  return data
}

async function login() {
  const res = await postJson('/sys/login', { username: USERNAME, password: PASSWORD })
  const { tokenValue, tokenName } = res.data.token
  return { tokenValue, tokenName }
}

async function findProjectIdByName(authHeaders) {
  const res = await getJson('/project/list', { page: 1, limit: 200 }, authHeaders)
  const list = res.data ?? []
  const hit = list.find(item => item.projectName === PROJECT_NAME)
  return hit ? String(hit.id) : null
}

async function verifyProject(projectId, authHeaders) {
  const res = await getJson('/project/getData', { projectId }, authHeaders)
  if (!res.data?.id) {
    throw new Error(`项目不存在: ${projectId}`)
  }
  return res.data
}

async function saveProjectContent(projectId, storage, authHeaders) {
  const form = new FormData()
  form.append('projectId', projectId)
  form.append('content', JSON.stringify(storage, null, 2))
  await postForm('/project/save/data', form, authHeaders)
}

async function updateProjectMeta(projectId, authHeaders) {
  await postJson(
    '/project/edit',
    {
      id: projectId,
      projectName: PROJECT_NAME,
      remarks: PROJECT_REMARKS
    },
    authHeaders
  )
}

async function resolveProjectId(authHeaders) {
  try {
    await verifyProject(PROJECT_ID, authHeaders)
    return PROJECT_ID
  } catch {
    console.warn(`项目 ID ${PROJECT_ID} 不可用，尝试按名称「${PROJECT_NAME}」查找…`)
    const found = await findProjectIdByName(authHeaders)
    if (!found) {
      throw new Error(`未找到项目：ID=${PROJECT_ID}，名称=${PROJECT_NAME}`)
    }
    return found
  }
}

async function main() {
  console.log('API:', API_BASE)
  const { tokenValue, tokenName } = await login()
  const authHeaders = { [tokenName]: tokenValue }

  const projectId = await resolveProjectId(authHeaders)
  console.log('Updating project id:', projectId)

  const storage = buildCarbonDashboardStorage()
  storage.editCanvasConfig.projectId = projectId
  storage.editCanvasConfig.projectName = PROJECT_NAME

  await saveProjectContent(projectId, storage, authHeaders)
  await updateProjectMeta(projectId, authHeaders)

  console.log('Updated. Open: http://localhost:3000/#/project/items')
  console.log('Edit:   http://localhost:3000/#/chart/home/' + projectId)
  console.log('Preview: http://localhost:3000/#/chart/preview/' + projectId)
}

main().catch(err => {
  console.error(err)
  process.exit(1)
})
