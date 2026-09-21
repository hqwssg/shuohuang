const assert = require('node:assert/strict')
const { execFileSync } = require('node:child_process')
const path = require('node:path')
const fs = require('node:fs')
const { chromium, request } = require('../.runtime/browser-check/node_modules/playwright')

async function run() {
  const api = await request.newContext({ baseURL: 'http://127.0.0.1:8081' })
  const browser = await chromium.launch({ channel: 'msedge', headless: true })
  const password = process.env.LOCAL_TEST_PASSWORD || 'admin123'
  async function login(username) {
    const captcha = await (await api.get('/code')).json()
    const stored = JSON.parse(execFileSync(process.env.REDIS_CLI || 'C:/PATH/redis/redis-cli.exe', ['-h', '127.0.0.1', '-p', '6379', '--raw', 'GET', `captcha_codes:${captcha.uuid}`], { encoding: 'utf8' }).trim())
    const answer = Array.isArray(stored) ? stored[0] : (typeof stored === 'object' ? stored.code : stored)
    const response = await (await api.post('/auth/login', { data: { username, password, uuid: captcha.uuid, code: answer } })).json()
    assert.equal(response.code, 200, `${username}: login failed: ${response.msg || response.message || 'unknown error'}`)
    return { Authorization: `Bearer ${response.data.access_token}` }
  }
  const admin = await login('admin')
  let stationId
  const output = path.resolve('.runtime/scope-permission-check')
  fs.mkdirSync(output, { recursive: true })
  try {
    const options = await (await api.get('/carbon/scopes/options', { headers: admin })).json()
    assert.equal(options.code, 200)
    const company = options.data.companies.find(c => c.deptName === '\u8083\u5b81\u5206\u516c\u53f8')
    const crew = options.data.departments.find(d => d.deptName === '\u8083\u5317\u4f9b\u7535\u5de5\u961f')
    assert.ok(company && crew)
    const configured = await (await api.get('/carbon/scopes?type=electricity', { headers: admin })).json()
    for (const name of ['定州西站', '定定区间', '定州东站']) {
      const row = configured.data.find(item => item.name === name)
      assert.ok(row && row.companyDeptId === 3020, `${name}: company scope missing`)
      assert.ok(row.grants.some(grant => grant.deptId === 3051 && Number(grant.canWrite) === 1), `${name}: department grant missing`)
    }
    const created = await (await api.post('http://127.0.0.1:8082/api/meter-settings/station', { headers: admin, data: { name: `permission_test_${Date.now()}`, pinyinCode: 'TEST', description: 'Temporary permission smoke test' } })).json()
    stationId = created.id
    assert.ok(stationId, JSON.stringify(created))
    const saved = await (await api.put(`/carbon/scopes/electricity/${stationId}`, { headers: admin, data: { companyDeptId: company.deptId, grants: [{ deptId: crew.deptId, canWrite: false }] } })).json()
    assert.equal(saved.code, 200, JSON.stringify(saved))
    const suning = await login('suning_admin')
    const yuanping = await login('yuanping_admin')
    const own = await api.get(`http://127.0.0.1:8082/api/meter-settings/node/station/${stationId}`, { headers: suning })
    assert.equal(own.status(), 200)
    const other = await api.get(`http://127.0.0.1:8082/api/meter-settings/node/station/${stationId}`, { headers: yuanping })
    assert.equal(other.status(), 403)
    const foreignAssignment = await api.put(`/carbon/scopes/electricity/${stationId}`, { headers: yuanping, data: { companyDeptId: company.deptId, grants: [] } })
    assert.ok(foreignAssignment.status() >= 400 || (await foreignAssignment.json()).code !== 200)
    const tree = await (await api.get('http://127.0.0.1:8082/api/meter-settings/tree', { headers: yuanping })).json()
    assert.ok(!tree.children.some(n => n.id === stationId))
    for (const viewport of [{ width: 1440, height: 1000 }, { width: 390, height: 844 }]) {
      const context = await browser.newContext({ viewport })
      await context.addCookies([{ name: 'Admin-Token', value: admin.Authorization.slice(7), url: 'http://127.0.0.1/' }])
      const page = await context.newPage()
      const errors = []
      page.on('pageerror', error => errors.push(error.message))
      await page.goto('http://127.0.0.1/carbon/scopes', { waitUntil: 'domcontentloaded', timeout: 120000 })
      await page.getByText('\u7535\u529b\u7ad9\u70b9 / \u533a\u95f4', { exact: true }).waitFor()
      await page.locator('.scope-table .el-table__body-wrapper tbody tr').first().waitFor({ state: 'visible', timeout: 30000 })
      assert.equal(await page.locator('.scope-table .el-table__body-wrapper tbody tr').count() > 0, true)
      await page.screenshot({ path: path.join(output, `scopes-${viewport.width}.png`), fullPage: true })
      assert.deepEqual(errors, [])
      if (viewport.width === 1440) {
        await page.locator('.scope-table button:visible').first().click()
        await page.locator('.el-dialog__wrapper:visible').waitFor({ state: 'visible', timeout: 30000 })
        await page.screenshot({ path: path.join(output, 'scope-assignment-dialog.png'), fullPage: true })
      }
      await context.close()
    }
    console.log('PASS ownership assignment, cross-company 403, tree filtering, desktop/mobile page, assignment dialog')
  } finally {
    try {
      if (stationId) {
        await api.post(`http://127.0.0.1:8082/api/meter-settings/node/station/${stationId}/toggle-status`, { headers: admin })
        const removed = await api.delete(`http://127.0.0.1:8082/api/meter-settings/node/station/${stationId}`, { headers: admin })
        assert.equal(removed.status(), 200)
      }
    } finally {
      await browser.close()
      await api.dispose()
    }
  }
}
run().catch(error => { console.error(error); process.exitCode = 1 })
