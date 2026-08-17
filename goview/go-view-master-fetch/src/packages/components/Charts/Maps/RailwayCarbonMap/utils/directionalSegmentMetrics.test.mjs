// 与 directionalSegmentMetrics.ts 同步
import assert from 'node:assert/strict'

function buildDirectionalSegmentMetrics(segmentIndex, totalSegments, from, to, lengthKm, totalRouteKm) {
  const baseIntensity = (from.carbonIntensity + to.carbonIntensity) / 2
  const progress = totalSegments > 1 ? segmentIndex / (totalSegments - 1) : 0
  const hillFactor = Math.max(0, 1 - progress * 2.2)
  const plainFactor = Math.max(0, (progress - 0.35) * 1.8)
  const wave = 0.92 + 0.08 * Math.sin(segmentIndex * 0.85)
  const upIntensity = baseIntensity * (1.08 + 0.28 * hillFactor) * wave
  const downIntensity = baseIntensity * (0.82 + 0.32 * plainFactor) * (1.05 - wave * 0.05)
  return {
    up: { carbonIntensity: Math.round(upIntensity * 10) / 10 },
    down: { carbonIntensity: Math.round(downIntensity * 10) / 10 }
  }
}

const from = { carbonIntensity: 9, carbonEmission: 100 }
const to = { carbonIntensity: 10, carbonEmission: 120 }
const m = buildDirectionalSegmentMetrics(0, 10, from, to, 10, 100)
assert.notEqual(m.up.carbonIntensity, m.down.carbonIntensity)
const diff = Math.abs(m.up.carbonIntensity - m.down.carbonIntensity) / m.up.carbonIntensity
assert.ok(diff >= 0.15, `expected >=15% diff, got ${diff}`)

console.log('directionalSegmentMetrics.test.mjs: ok')
