// 与 curveGeometry.ts 同步
import assert from 'node:assert/strict'

function clampControlPoint(points, index) {
  if (index < 0) return points[0]
  if (index >= points.length) return points[points.length - 1]
  return points[index]
}

function catmullRomAt(p0, p1, p2, p3, t) {
  const t2 = t * t
  const t3 = t2 * t
  return [
    0.5 *
      (2 * p1[0] +
        (-p0[0] + p2[0]) * t +
        (2 * p0[0] - 5 * p1[0] + 4 * p2[0] - p3[0]) * t2 +
        (-p0[0] + 3 * p1[0] - 3 * p2[0] + p3[0]) * t3),
    0.5 *
      (2 * p1[1] +
        (-p0[1] + p2[1]) * t +
        (2 * p0[1] - 5 * p1[1] + 4 * p2[1] - p3[1]) * t2 +
        (-p0[1] + 3 * p1[1] - 3 * p2[1] + p3[1]) * t3)
  ]
}

function buildSplineSegmentCoords(stationCoords, segmentIndex, stepsPerSegment = 8) {
  if (segmentIndex < 0 || segmentIndex >= stationCoords.length - 1) return []
  const p0 = clampControlPoint(stationCoords, segmentIndex - 1)
  const p1 = stationCoords[segmentIndex]
  const p2 = stationCoords[segmentIndex + 1]
  const p3 = clampControlPoint(stationCoords, segmentIndex + 2)
  const seg = []
  for (let s = 0; s <= stepsPerSegment; s++) {
    seg.push(catmullRomAt(p0, p1, p2, p3, s / stepsPerSegment))
  }
  return seg
}

function buildCatmullRomSplineCoords(stationCoords, stepsPerSegment = 8) {
  if (stationCoords.length < 2) return []
  const path = []
  for (let i = 0; i < stationCoords.length - 1; i++) {
    const seg = buildSplineSegmentCoords(stationCoords, i, stepsPerSegment)
    path.push(...(i === 0 ? seg : seg.slice(1)))
  }
  return path
}

function buildRoundTripCurvedFlowCoords(stationCoords, curveness = 0.2, stepsPerSegment = 8) {
  const steps = Math.max(4, Math.round(stepsPerSegment * (1 + Math.abs(curveness))))
  const forward = buildCatmullRomSplineCoords(stationCoords, steps)
  if (forward.length < 2) return []
  return [...forward, ...[...forward].reverse().slice(1)]
}

const stations = [[0, 0], [1, 1], [2, 0], [3, 1]]
const spline = buildCatmullRomSplineCoords(stations, 6)
assert.ok(spline.length > stations.length)
assert.deepEqual(spline[0], [0, 0])
assert.deepEqual(spline[spline.length - 1], [3, 1])

const roundTrip = buildRoundTripCurvedFlowCoords(stations, 0.2, 6)
assert.deepEqual(roundTrip[0], roundTrip[roundTrip.length - 1])

const seg0 = buildSplineSegmentCoords(stations, 0, 6)
const seg1 = buildSplineSegmentCoords(stations, 1, 6)
assert.deepEqual(seg0[seg0.length - 1], seg1[0])

console.log('curveGeometry.test.mjs: ok')
