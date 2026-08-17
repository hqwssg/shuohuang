import assert from 'node:assert/strict'

function segmentLoopDurationSec(coords, constantSpeed) {
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
  let lenKm = 0
  for (let i = 1; i < coords.length; i++) {
    lenKm += haversineKm(coords[i - 1], coords[i])
  }
  const pxPerKm = 10
  const pixelLen = Math.max(lenKm * pxPerKm, 24)
  return Math.max(2.5, pixelLen / Math.max(constantSpeed, 8))
}

function findPhaseForCoord(flowCoords, target) {
  if (flowCoords.length < 2) return 0
  let bestIdx = 0
  let bestDist = Infinity
  for (let i = 0; i < flowCoords.length; i++) {
    const dx = flowCoords[i][0] - target[0]
    const dy = flowCoords[i][1] - target[1]
    const d = dx * dx + dy * dy
    if (d < bestDist) {
      bestDist = d
      bestIdx = i
    }
  }
  return bestIdx / (flowCoords.length - 1)
}

function sliceCoordsByPhase(flowCoords, fromPhase, toPhase) {
  if (flowCoords.length < 2) return flowCoords
  const maxIdx = flowCoords.length - 1
  const fromIdx = Math.min(maxIdx, Math.max(0, Math.round(fromPhase * maxIdx)))
  const toIdx = Math.min(maxIdx, Math.max(0, Math.round(toPhase * maxIdx)))
  if (fromIdx <= toIdx) return flowCoords.slice(fromIdx, toIdx + 1)
  return [...flowCoords.slice(fromIdx), ...flowCoords.slice(0, toIdx + 1)]
}

function moveDurationSec(flowCoords, fromPhase, toPhase, lineFlowSpeed, speedRatio) {
  const slice = sliceCoordsByPhase(flowCoords, fromPhase, toPhase)
  if (slice.length < 2) return 0
  return segmentLoopDurationSec(slice, lineFlowSpeed) / speedRatio
}

function buildFlowDwellTimeline(opts) {
  const {
    flowCoords,
    stationCoords,
    stationOrder,
    lineFlowSpeed,
    dwellSec,
    speedRatio = 0.2
  } = opts

  if (
    flowCoords.length < 2 ||
    stationOrder.length < 1 ||
    stationCoords.length < 1 ||
    dwellSec <= 0
  ) {
    return { segments: [], totalDurationSec: 0 }
  }

  const stops = stationOrder.map((name, i) => ({
    name,
    phase: findPhaseForCoord(flowCoords, stationCoords[i] ?? stationCoords[0])
  }))

  const segments = []

  const appendPass = orderedStops => {
    if (orderedStops.length === 0) return
    segments.push({
      type: 'dwell',
      phase: orderedStops[0].phase,
      stationName: orderedStops[0].name,
      durationSec: dwellSec
    })
    for (let i = 1; i < orderedStops.length; i++) {
      const prev = orderedStops[i - 1]
      const curr = orderedStops[i]
      segments.push({
        type: 'move',
        fromPhase: prev.phase,
        toPhase: curr.phase,
        durationSec: moveDurationSec(
          flowCoords,
          prev.phase,
          curr.phase,
          lineFlowSpeed,
          speedRatio
        )
      })
      segments.push({
        type: 'dwell',
        phase: curr.phase,
        stationName: curr.name,
        durationSec: dwellSec
      })
    }
  }

  appendPass(stops)
  appendPass([...stops].reverse())

  const totalDurationSec = segments.reduce((sum, s) => sum + s.durationSec, 0)
  return { segments, totalDurationSec }
}

function resolveTimelineState(segments, totalDurationSec, elapsedSec) {
  if (segments.length === 0 || totalDurationSec <= 0) {
    return { phase: 0, inDwell: false, segmentIndex: -1 }
  }
  const t = ((elapsedSec % totalDurationSec) + totalDurationSec) % totalDurationSec
  let acc = 0
  for (let i = 0; i < segments.length; i++) {
    const seg = segments[i]
    const end = acc + seg.durationSec
    if (t < end) {
      const local = t - acc
      if (seg.type === 'dwell') {
        return { phase: seg.phase, inDwell: true, stationName: seg.stationName, segmentIndex: i }
      }
      const ratio = seg.durationSec > 0 ? local / seg.durationSec : 1
      const phase = seg.fromPhase + (seg.toPhase - seg.fromPhase) * ratio
      return { phase, inDwell: false, segmentIndex: i }
    }
    acc = end
  }
  const last = segments[segments.length - 1]
  if (last.type === 'dwell') {
    return { phase: last.phase, inDwell: true, stationName: last.stationName, segmentIndex: segments.length - 1 }
  }
  return { phase: last.toPhase, inDwell: false, segmentIndex: segments.length - 1 }
}

// --- fixtures: 3 stations round trip ---
const stationCoords = [
  [112, 39],
  [114, 38.5],
  [116, 38]
]
const stationOrder = ['A站', 'B站', 'C站']
const forward = [
  [112, 39],
  [113, 38.8],
  [114, 38.5],
  [115, 38.2],
  [116, 38]
]
const backward = [...forward].reverse().slice(1)
const flowCoords = [...forward, ...backward]

const timeline = buildFlowDwellTimeline({
  flowCoords,
  stationCoords,
  stationOrder,
  lineFlowSpeed: 40,
  dwellSec: 2,
  speedRatio: 0.2
})

const dwellSegments = timeline.segments.filter(s => s.type === 'dwell')
assert.equal(dwellSegments.length, stationOrder.length * 2, 'forward + backward dwell count')
assert.equal(
  timeline.totalDurationSec,
  dwellSegments.reduce((s, d) => s + d.durationSec, 0) +
    timeline.segments.filter(s => s.type === 'move').reduce((s, m) => s + m.durationSec, 0)
)

const moveSegments = timeline.segments.filter(s => s.type === 'move')
assert.ok(moveSegments.length >= 2)

// speedRatio 0.2 → move duration 5× baseline
const baseline = segmentLoopDurationSec(
  sliceCoordsByPhase(flowCoords, moveSegments[0].fromPhase, moveSegments[0].toPhase),
  40
)
assert.ok(Math.abs(moveSegments[0].durationSec - baseline / 0.2) < 0.01)

// resolve dwell at start
const s0 = resolveTimelineState(timeline.segments, timeline.totalDurationSec, 0)
assert.equal(s0.inDwell, true)
assert.equal(s0.stationName, 'A站')

// forward pass move segments keep non-decreasing phase
const forwardEnd = dwellSegments.length / 2
let forwardMoveCount = 0
for (const seg of timeline.segments) {
  if (seg.type === 'move') {
    forwardMoveCount++
    if (forwardMoveCount <= stationOrder.length - 1) {
      assert.ok(seg.fromPhase <= seg.toPhase + 0.001)
    }
  }
}

console.log('flowDwellTimeline.test.mjs: ok')
