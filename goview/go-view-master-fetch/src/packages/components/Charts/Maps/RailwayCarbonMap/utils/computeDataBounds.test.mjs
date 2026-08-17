// 与 computeDataBounds.ts 同步
import assert from 'node:assert/strict'

function computeDataBounds(points, lines, options = {}) {
  const paddingRatio = options.paddingRatio ?? 0.12
  const padLngRatio = options.paddingRatioX ?? paddingRatio
  const padLatRatio = options.paddingRatioY ?? paddingRatio

  const coords = []
  points.forEach(p => coords.push([p.lng, p.lat]))
  lines.forEach(line => line.geometry.coordinates.forEach(c => coords.push(c)))

  let minLng = Infinity
  let maxLng = -Infinity
  let minLat = Infinity
  let maxLat = -Infinity
  coords.forEach(([lng, lat]) => {
    minLng = Math.min(minLng, lng)
    maxLng = Math.max(maxLng, lng)
    minLat = Math.min(minLat, lat)
    maxLat = Math.max(maxLat, lat)
  })

  const lngSpan = maxLng - minLng || 0.5
  const latSpan = maxLat - minLat || 0.5
  const padLng = lngSpan * padLngRatio
  const padLat = latSpan * padLatRatio

  minLng -= padLng
  maxLng += padLng
  minLat -= padLat
  maxLat += padLat

  return {
    center: [(minLng + maxLng) / 2, (minLat + maxLat) / 2],
    boundingCoords: [
      [minLng, maxLat],
      [maxLng, minLat]
    ]
  }
}

const points = [
  { lng: 112, lat: 39 },
  { lng: 117, lat: 38 }
]
const lines = [{ geometry: { coordinates: [[112.5, 38.5], [116.5, 38.2]] } }]

const symmetric = computeDataBounds(points, lines, { paddingRatio: 0.1 })
const lngSpanSym =
  symmetric.boundingCoords[1][0] - symmetric.boundingCoords[0][0]
const latSpanSym =
  symmetric.boundingCoords[0][1] - symmetric.boundingCoords[1][1]

const asymmetric = computeDataBounds(points, lines, {
  paddingRatio: 0.1,
  paddingRatioX: 0.3
})
const lngSpanAsym =
  asymmetric.boundingCoords[1][0] - asymmetric.boundingCoords[0][0]
const latSpanAsym =
  asymmetric.boundingCoords[0][1] - asymmetric.boundingCoords[1][1]

assert.ok(lngSpanAsym > lngSpanSym, 'horizontal padding should widen lng bounds')
assert.ok(Math.abs(latSpanAsym - latSpanSym) < 1e-9, 'vertical span unchanged when only paddingRatioX set')

const verticalOnly = computeDataBounds(points, lines, {
  paddingRatio: 0.1,
  paddingRatioY: 0.2
})
const latSpanY =
  verticalOnly.boundingCoords[0][1] - verticalOnly.boundingCoords[1][1]
assert.ok(latSpanY > latSpanSym, 'paddingRatioY should widen lat bounds')

console.log('computeDataBounds.test.mjs: ok')
