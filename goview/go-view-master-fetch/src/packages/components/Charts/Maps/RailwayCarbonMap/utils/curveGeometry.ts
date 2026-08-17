/** 与 ECharts lines series curveness 一致的控制点（保留供测试/兼容） */
export function getQuadraticControlPoint(
  start: [number, number],
  end: [number, number],
  curveness: number
): [number, number] {
  return [
    (start[0] + end[0]) / 2 - (end[1] - start[1]) * curveness,
    (start[1] + end[1]) / 2 + (end[0] - start[0]) * curveness
  ]
}

export function sampleQuadraticBezier(
  start: [number, number],
  control: [number, number],
  end: [number, number],
  steps: number
): [number, number][] {
  const pts: [number, number][] = []
  for (let i = 0; i <= steps; i++) {
    const t = i / steps
    const u = 1 - t
    pts.push([
      u * u * start[0] + 2 * u * t * control[0] + t * t * end[0],
      u * u * start[1] + 2 * u * t * control[1] + t * t * end[1]
    ])
  }
  return pts
}

export function sampleCurvedSegment(
  start: [number, number],
  end: [number, number],
  curveness: number,
  steps = 8
): [number, number][] {
  const cp = getQuadraticControlPoint(start, end, curveness)
  return sampleQuadraticBezier(start, cp, end, steps)
}

function clampControlPoint(
  points: [number, number][],
  index: number
): [number, number] {
  if (index < 0) return points[0]
  if (index >= points.length) return points[points.length - 1]
  return points[index]
}

/** Catmull-Rom 插值：t∈[0,1] 从 p1 到 p2 */
export function catmullRomAt(
  p0: [number, number],
  p1: [number, number],
  p2: [number, number],
  p3: [number, number],
  t: number
): [number, number] {
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

/** 全线 Catmull-Rom 样条（过所有站点，C1 连续） */
export function buildCatmullRomSplineCoords(
  stationCoords: [number, number][],
  stepsPerSegment = 8
): [number, number][] {
  if (stationCoords.length < 2) {
    return stationCoords.length === 1 ? [stationCoords[0]] : []
  }

  const path: [number, number][] = []

  for (let i = 0; i < stationCoords.length - 1; i++) {
    const seg = buildSplineSegmentCoords(stationCoords, i, stepsPerSegment)
    path.push(...(i === 0 ? seg : seg.slice(1)))
  }

  return path
}

/** 样条上相邻两站之间的子弧（用于分段着色） */
export function buildSplineSegmentCoords(
  stationCoords: [number, number][],
  segmentIndex: number,
  stepsPerSegment = 8
): [number, number][] {
  if (segmentIndex < 0 || segmentIndex >= stationCoords.length - 1) return []

  const p0 = clampControlPoint(stationCoords, segmentIndex - 1)
  const p1 = stationCoords[segmentIndex]
  const p2 = stationCoords[segmentIndex + 1]
  const p3 = clampControlPoint(stationCoords, segmentIndex + 2)

  const seg: [number, number][] = []
  for (let s = 0; s <= stepsPerSegment; s++) {
    seg.push(catmullRomAt(p0, p1, p2, p3, s / stepsPerSegment))
  }
  return seg
}

function resolveStepsPerSegment(curveness: number, base = 8): number {
  return Math.max(4, Math.round(base * (1 + Math.abs(curveness))))
}

/** 沿样条去程 + 回程，闭合至起点（供 Flow Guide polyline） */
export function buildRoundTripCurvedFlowCoords(
  stationCoords: [number, number][],
  curveness = 0.2,
  stepsPerSegment = 8
): [number, number][] {
  const steps = resolveStepsPerSegment(curveness, stepsPerSegment)
  const forward = buildCatmullRomSplineCoords(stationCoords, steps)
  if (forward.length < 2) return []

  const backward = [...forward].reverse().slice(1)
  return [...forward, ...backward]
}
