import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { FilterManager } from '../src/packages/components/Charts/Maps/RailwayCarbonMap/core/FilterManager.ts'
import { buildCurvedStationLines } from '../src/packages/components/Charts/Maps/RailwayCarbonMap/utils/buildCurvedStationLines.ts'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const mapData = JSON.parse(
  readFileSync(join(root, 'src/packages/components/Charts/Maps/RailwayCarbonMap/data.json'), 'utf8')
)

function baseOptions(showHighOnly) {
  return {
    activeMetric: 'carbon',
    activeDirection: 'all',
    showHighOnly,
    lineDisplayMode: 'flow'
  }
}

test('showHighOnly keeps line count and sets threshold', () => {
  const fm = new FilterManager()
  const lines = buildCurvedStationLines(
    mapData.points.points,
    mapData.meta?.stationOrder,
    mapData.points.statPeriod
  )
  assert.ok(lines.length > 0)

  const normal = fm.applyFilters(lines, mapData.points.points, baseOptions(false))
  const highOnly = fm.applyFilters(lines, mapData.points.points, baseOptions(true))

  assert.equal(normal.highEmissionThreshold, null)
  assert.equal(highOnly.lines.length, normal.lines.length)
  assert.ok(typeof highOnly.highEmissionThreshold === 'number')
  assert.ok(highOnly.stats.visibleLineCount > 0)
  assert.ok(highOnly.stats.visibleLineCount <= highOnly.lines.length)
  assert.ok(highOnly.stats.visibleLineCount < highOnly.lines.length)
})

test('showHighOnly off restores null threshold', () => {
  const fm = new FilterManager()
  const lines = buildCurvedStationLines(mapData.points.points, mapData.meta?.stationOrder, '')
  const result = fm.applyFilters(lines, mapData.points.points, baseOptions(false))

  assert.equal(result.highEmissionThreshold, null)
  assert.equal(result.stats.visibleLineCount, result.lines.length)
})
