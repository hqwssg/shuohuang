import assert from 'node:assert/strict'

function buildStationFittedFlowCoords(points, stationOrder) {
  const byName = new Map(points.map(p => [p.name, p]))
  const coords = []
  for (const name of stationOrder) {
    const p = byName.get(name)
    if (p) coords.push([p.lng, p.lat])
  }
  return coords.length >= 2 ? coords : []
}

const pts = [
  { name: '神池南站', lng: 1, lat: 2 },
  { name: '宁武西站', lng: 3, lat: 4 },
  { name: '龙宫站', lng: 5, lat: 6 }
]
const order = ['神池南站', '宁武西站', '龙宫站']
assert.deepEqual(buildStationFittedFlowCoords(pts, order), [[1, 2], [3, 4], [5, 6]])
assert.equal(buildStationFittedFlowCoords([pts[0]], order).length, 0)
console.log('buildStationFittedFlowPath.test.mjs: ok')
