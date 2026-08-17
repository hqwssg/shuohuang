import assert from 'node:assert/strict'

function haversineKm(a, b) {
  const R = 6371
  const dLat = ((b[1] - a[1]) * Math.PI) / 180
  const dLng = ((b[0] - a[0]) * Math.PI) / 180
  const lat1 = (a[1] * Math.PI) / 180
  const lat2 = (b[1] * Math.PI) / 180
  const h =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2
  return 2 * R * Math.asin(Math.sqrt(h))
}

function polylineLengthKm(coords) {
  let total = 0
  for (let i = 1; i < coords.length; i++) total += haversineKm(coords[i - 1], coords[i])
  return total
}

function findPositionOnPolyline(coords, point) {
  let best = { index: 0, t: 0, lng: coords[0][0], lat: coords[0][1], distanceKm: 0, dist2: Infinity }
  let walked = 0
  for (let i = 1; i < coords.length; i++) {
    const a = coords[i - 1]
    const b = coords[i]
    const segLen = haversineKm(a, b)
    const dx = b[0] - a[0]
    const dy = b[1] - a[1]
    const len2 = dx * dx + dy * dy
    let t = len2 > 0 ? ((point.lng - a[0]) * dx + (point.lat - a[1]) * dy) / len2 : 0
    t = Math.max(0, Math.min(1, t))
    const lng = a[0] + dx * t
    const lat = a[1] + dy * t
    const d2 = (lng - point.lng) ** 2 + (lat - point.lat) ** 2
    if (d2 < best.dist2) {
      best = { index: i - 1, t, lng, lat, distanceKm: walked + segLen * t, dist2: d2 }
    }
    walked += segLen
  }
  const { dist2: _d, ...rest } = best
  return rest
}

function slicePolylineBetween(coords, from, to) {
  if (coords.length < 2) return [[from.lng, from.lat], [to.lng, to.lat]]
  const posA = findPositionOnPolyline(coords, from)
  const posB = findPositionOnPolyline(coords, to)
  const [start, end] = posA.distanceKm <= posB.distanceKm ? [posA, posB] : [posB, posA]
  const slice = [[from.lng, from.lat]]
  for (let i = start.index + 1; i <= end.index; i++) {
    if (i < coords.length) slice.push([coords[i][0], coords[i][1]])
  }
  const last = slice[slice.length - 1]
  if (last[0] !== to.lng || last[1] !== to.lat) slice.push([to.lng, to.lat])
  return slice
}

function sampleAlongPolyline(coords, count, includeEndpoints = true) {
  const total = polylineLengthKm(coords)
  if (count < 2 || total === 0) return []
  const samples = []
  for (let i = 0; i < count; i++) {
    const target = includeEndpoints ? (i / (count - 1)) * total : ((i + 1) / (count + 1)) * total
    let walked = 0
    for (let s = 1; s < coords.length; s++) {
      const segLen = haversineKm(coords[s - 1], coords[s])
      if (walked + segLen >= target || s === coords.length - 1) {
        const t = segLen > 0 ? (target - walked) / segLen : 0
        const c0 = coords[s - 1]
        const c1 = coords[s]
        samples.push({
          lng: c0[0] + (c1[0] - c0[0]) * Math.min(1, Math.max(0, t)),
          lat: c0[1] + (c1[1] - c0[1]) * Math.min(1, Math.max(0, t)),
          chainageKm: Math.round(target * 10) / 10
        })
        break
      }
      walked += segLen
    }
  }
  if (includeEndpoints && samples.length >= 2) {
    samples[0].lng = coords[0][0]
    samples[0].lat = coords[0][1]
    samples[0].chainageKm = 0
    const last = samples[samples.length - 1]
    last.lng = coords[coords.length - 1][0]
    last.lat = coords[coords.length - 1][1]
    last.chainageKm = Math.round(total * 10) / 10
  }
  return samples
}

const zigzag = [[0, 0], [1, 0], [1, 1], [2, 1]]
const from = { lng: 0, lat: 0, name: 'A' }
const to = { lng: 2, lat: 1, name: 'B' }

const sliced = slicePolylineBetween(zigzag, from, to)
assert.ok(sliced.length >= 3)
assert.equal(sliced[0][0], 0)
assert.equal(sliced[sliced.length - 1][0], 2)

const samples = sampleAlongPolyline(zigzag, 5, true)
assert.equal(samples.length, 5)
assert.equal(samples[0].chainageKm, 0)
assert.ok(samples[4].chainageKm > 0)

console.log('routeGeometry.test.mjs: OK')
