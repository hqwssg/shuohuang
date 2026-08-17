/**
 * 将碳排放全景大屏写入 GoView 项目库（新建项目）
 * Run: cd go-view-master-fetch && npx vite-node scripts/seed-carbon-dashboard-project.mjs
 *
 * Env:
 *   GOVIEW_API_BASE  默认 http://localhost:8083/api/goview
 *   GOVIEW_USERNAME  默认 admin
 *   GOVIEW_PASSWORD  默认 admin（本库 MD5 对应明文 admin，非 123456）
 */
import { buildCarbonDashboardStorage } from './generate-carbon-dashboard.mjs'

const API_BASE = process.env.GOVIEW_API_BASE ?? 'http://localhost:8083/api/goview'
const USERNAME = process.env.GOVIEW_USERNAME ?? 'admin'
const PASSWORD = process.env.GOVIEW_PASSWORD ?? 'admin'
const PROJECT_NAME = '朔黄铁路碳排放全景大屏'
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

async function createProject(authHeaders) {
  const res = await postJson(
    '/project/create',
    {
      projectName: PROJECT_NAME,
      remarks: PROJECT_REMARKS,
      indexImage: null
    },
    authHeaders
  )
  return String(res.data.id)
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

async function main() {
  console.log('API:', API_BASE)
  const { tokenValue, tokenName } = await login()
  const authHeaders = { [tokenName]: tokenValue }

  const projectId = await createProject(authHeaders)
  console.log('Created project id:', projectId)

  const storage = await buildCarbonDashboardStorage()
  storage.editCanvasConfig.projectId = projectId
  storage.editCanvasConfig.projectName = PROJECT_NAME

  await saveProjectContent(projectId, storage, authHeaders)
  await updateProjectMeta(projectId, authHeaders)

  console.log('Saved. Open: http://localhost:3000/#/project/items')
  console.log('Edit:   http://localhost:3000/#/chart/home/' + projectId)
  console.log('Preview: http://localhost:3000/#/chart/preview/' + projectId)
}

main().catch(err => {
  console.error(err)
  process.exit(1)
})
