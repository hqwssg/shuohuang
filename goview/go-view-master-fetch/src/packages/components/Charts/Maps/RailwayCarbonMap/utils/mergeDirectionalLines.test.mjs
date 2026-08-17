import assert from 'node:assert/strict'

function segmentKey(line) {
  return line.properties.sectionId.replace(/-(UP|DOWN)$/i, '')
}

function average(a, b) {
  return Math.round(((a + b) / 2) * 10) / 10
}

function mergePair(up, down) {
  return {
    properties: {
      direction: 'both',
      carbonIntensity: average(up.properties.carbonIntensity, down.properties.carbonIntensity)
    }
  }
}

function resolveLinesForDirection(allLines, activeDirection) {
  if (activeDirection === 'up') return allLines.filter(l => l.properties.direction === 'up')
  if (activeDirection === 'down') return allLines.filter(l => l.properties.direction === 'down')
  const upByKey = new Map()
  const downByKey = new Map()
  for (const line of allLines) {
    const key = segmentKey(line)
    if (line.properties.direction === 'up') upByKey.set(key, line)
    if (line.properties.direction === 'down') downByKey.set(key, line)
  }
  const merged = []
  for (const [key, upLine] of upByKey) {
    merged.push(mergePair(upLine, downByKey.get(key)))
  }
  return merged
}

function get75thPercentile(values) {
  const sorted = [...values].sort((a, b) => a - b)
  const pos = (sorted.length - 1) * 0.75
  const lo = Math.floor(pos)
  const hi = Math.ceil(pos)
  if (lo === hi) return sorted[lo]
  return sorted[lo] * (1 - (pos - lo)) + sorted[hi] * (pos - lo)
}

const lines = [
  { properties: { sectionId: 'A-B-UP', direction: 'up', carbonIntensity: 10 } },
  { properties: { sectionId: 'A-B-DOWN', direction: 'down', carbonIntensity: 6 } },
  { properties: { sectionId: 'B-C-UP', direction: 'up', carbonIntensity: 8 } },
  { properties: { sectionId: 'B-C-DOWN', direction: 'down', carbonIntensity: 12 } },
  { properties: { sectionId: 'C-D-UP', direction: 'up', carbonIntensity: 14 } },
  { properties: { sectionId: 'C-D-DOWN', direction: 'down', carbonIntensity: 4 } },
  { properties: { sectionId: 'D-E-UP', direction: 'up', carbonIntensity: 7 } },
  { properties: { sectionId: 'D-E-DOWN', direction: 'down', carbonIntensity: 9 } }
]

const upLines = resolveLinesForDirection(lines, 'up')
assert.equal(upLines.length, 4)
const upP75 = get75thPercentile(upLines.map(l => l.properties.carbonIntensity))
const upHigh = upLines.filter(l => l.properties.carbonIntensity >= upP75)
assert.ok(upHigh.length >= 1)

const merged = resolveLinesForDirection(lines, 'all')
assert.equal(merged.length, 4)
assert.equal(merged[0].properties.direction, 'both')
assert.equal(merged[0].properties.carbonIntensity, 8)

console.log('mergeDirectionalLines.test.mjs: ok')
