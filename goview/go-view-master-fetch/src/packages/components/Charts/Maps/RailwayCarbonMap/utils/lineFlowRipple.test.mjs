import assert from 'node:assert/strict'

function sampleFlowPosition(coords, phase) {
  if (coords.length === 0) return [0, 0]
  if (coords.length === 1) return coords[0]
  const t = ((phase % 1) + 1) % 1
  const span = coords.length - 1
  const pos = t * span
  const idx = Math.min(Math.floor(pos), span - 1)
  const frac = pos - idx
  const a = coords[idx]
  const b = coords[idx + 1]
  return [a[0] + (b[0] - a[0]) * frac, a[1] + (b[1] - a[1]) * frac]
}

assert.deepEqual(sampleFlowPosition([[0, 0], [2, 2]], 0.5), [1, 1])
console.log('lineFlowRipple.test.mjs: ok')
