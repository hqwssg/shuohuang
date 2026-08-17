import assert from 'node:assert/strict'

function buildCurvedStationLines(points, stationOrder) {
  const order = stationOrder
  const stationCoords = order
    .map(name => points.find(p => p.name === name))
    .filter(Boolean)
    .map(p => [p.lng, p.lat])

  const segments = []
  for (let i = 0; i < order.length - 1; i++) {
    const from = points.find(p => p.name === order[i])
    const to = points.find(p => p.name === order[i + 1])
    if (!from || !to) continue
    segments.push({
      geometry: {
        coordinates: [
          [from.lng, from.lat],
          [from.lng + (to.lng - from.lng) * 0.5, from.lat + (to.lat - from.lat) * 0.5],
          [to.lng, to.lat]
        ]
      },
      properties: { direction: 'both', sectionId: `${from.name}-${to.name}` }
    })
  }
  return { segments, stationCount: stationCoords.length }
}

const pts = [
  { name: 'A站', lng: 1, lat: 2, carbonEmission: 10, carbonIntensity: 5, unit: 't' },
  { name: 'B站', lng: 3, lat: 4, carbonEmission: 20, carbonIntensity: 6, unit: 't' },
  { name: 'C站', lng: 5, lat: 6, carbonEmission: 30, carbonIntensity: 7, unit: 't' }
]
const { segments, stationCount } = buildCurvedStationLines(pts, ['A站', 'B站', 'C站'])
assert.equal(segments.length, 2)
assert.equal(stationCount, 3)
assert.equal(segments[0].properties.direction, 'both')
assert.ok(segments[0].geometry.coordinates.length >= 2)
console.log('buildCurvedStationLines.test.mjs: ok')
