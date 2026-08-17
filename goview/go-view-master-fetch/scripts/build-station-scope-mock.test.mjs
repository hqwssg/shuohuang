import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { buildDashboardScopeData } from './build-station-scope-mock.mjs'
import { kpiMock, lineTrendMock, threeSurfaceMock, structurePiesMock } from './carbon-dashboard-mock.mjs'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const mapData = JSON.parse(
  readFileSync(join(root, 'src/packages/components/Charts/Maps/RailwayCarbonMap/data.json'), 'utf8')
)

test('builds 35 station entries', () => {
  const data = buildDashboardScopeData({ mapData, kpiMock, lineTrendMock, threeSurfaceMock, structurePiesMock })
  assert.equal(Object.keys(data.stations).length, 35)
  assert.ok(data.stations['神池南站'])
})

test('station KPI scales by emission ratio', () => {
  const data = buildDashboardScopeData({ mapData, kpiMock, lineTrendMock, threeSurfaceMock, structurePiesMock })
  const total = mapData.points.points.reduce((s, p) => s + p.carbonEmission, 0)
  const ratio = mapData.points.points[0].carbonEmission / total
  const expected = Math.round(kpiMock.yearTotal.value * ratio * 10) / 10
  assert.ok(Math.abs(data.stations['神池南站'].kpi.yearTotal - expected) < 0.2)
})

test('overall block matches source mocks', () => {
  const data = buildDashboardScopeData({ mapData, kpiMock, lineTrendMock, threeSurfaceMock, structurePiesMock })
  assert.deepEqual(data.overall.kpi.yearTotal, kpiMock.yearTotal.value)
  assert.deepEqual(data.overall.line, lineTrendMock)
})
