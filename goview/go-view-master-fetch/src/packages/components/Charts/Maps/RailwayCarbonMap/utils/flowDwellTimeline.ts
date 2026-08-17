import { segmentLoopDurationSec } from './lineFlowRipple'

export type MoveSegment = {
  type: 'move'
  fromPhase: number
  toPhase: number
  durationSec: number
}

export type DwellSegment = {
  type: 'dwell'
  phase: number
  stationName: string
  durationSec: number
}

export type TimelineSegment = MoveSegment | DwellSegment

export interface FlowDwellTimelineOptions {
  flowCoords: [number, number][]
  stationCoords: [number, number][]
  stationOrder: string[]
  lineFlowSpeed: number
  dwellSec: number
  speedRatio?: number
}

export interface FlowDwellTimelineResult {
  segments: TimelineSegment[]
  totalDurationSec: number
}

/** 在 flowCoords 折线上定位目标坐标对应的 phase（0–1） */
export function findPhaseForCoord(
  flowCoords: [number, number][],
  target: [number, number]
): number {
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

export function sliceCoordsByPhase(
  flowCoords: [number, number][],
  fromPhase: number,
  toPhase: number
): [number, number][] {
  if (flowCoords.length < 2) return flowCoords
  const maxIdx = flowCoords.length - 1
  const fromIdx = Math.min(maxIdx, Math.max(0, Math.round(fromPhase * maxIdx)))
  const toIdx = Math.min(maxIdx, Math.max(0, Math.round(toPhase * maxIdx)))
  if (fromIdx <= toIdx) {
    return flowCoords.slice(fromIdx, toIdx + 1)
  }
  return [...flowCoords.slice(fromIdx), ...flowCoords.slice(0, toIdx + 1)]
}

function moveDurationSec(
  flowCoords: [number, number][],
  fromPhase: number,
  toPhase: number,
  lineFlowSpeed: number,
  speedRatio: number
): number {
  const slice = sliceCoordsByPhase(flowCoords, fromPhase, toPhase)
  if (slice.length < 2) return 0
  return segmentLoopDurationSec(slice, lineFlowSpeed) / speedRatio
}

/**
 * 去程 + 回程各在每个站点停留 dwellSec；站间速度 = lineFlowSpeed * speedRatio
 */
export function buildFlowDwellTimeline(opts: FlowDwellTimelineOptions): FlowDwellTimelineResult {
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

  const segments: TimelineSegment[] = []

  const appendPass = (orderedStops: { name: string; phase: number }[]) => {
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
        durationSec: moveDurationSec(flowCoords, prev.phase, curr.phase, lineFlowSpeed, speedRatio)
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

export interface TimelineState {
  phase: number
  inDwell: boolean
  stationName?: string
  segmentIndex: number
}

/** 根据循环时间解析当前 phase 与是否处于 dwell */
export function resolveTimelineState(
  segments: TimelineSegment[],
  totalDurationSec: number,
  elapsedSec: number
): TimelineState {
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
