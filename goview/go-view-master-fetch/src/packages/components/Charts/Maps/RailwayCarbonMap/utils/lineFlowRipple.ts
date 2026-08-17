import type { LineFeature } from '../types'
import { getLineColor } from './lineColorScale'
import { colorWithAlpha, normalizeIntensity } from './pointColorScale'
import {
  getLineCarbonEmission,
  getLineEmissionRange,
  getLineIntensity,
  getLineIntensityField,
  type ActiveMetric
} from './lineMetric'

/** 与线路 lines effect 单圈耗时对齐（秒），用于 period 与涟漪位移 */
export function getLineFlowPeriodSec(
  coords: [number, number][],
  lineFlowSpeed: number
): number {
  return segmentLoopDurationSec(coords, lineFlowSpeed)
}

export interface LineFlowTrack {
  sectionId: string
  flowCoords: [number, number][]
  flowColor: string
  emissionNorm: number
  rippleScale: number
  ripplePeriod: number
  sectionName: string
  direction: string
  carbonEmission: number
}

function haversineKm(a: [number, number], b: [number, number]): number {
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

/** 流动方向上的坐标（下行已反转） */
export function getFlowCoords(line: LineFeature): [number, number][] {
  const raw = line.geometry.coordinates as [number, number][]
  return line.properties.direction === 'down' ? [...raw].reverse() : raw
}

export function sampleFlowPosition(coords: [number, number][], phase: number): [number, number] {
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

export function segmentLoopDurationSec(coords: [number, number][], constantSpeed: number): number {
  let lenKm = 0
  for (let i = 1; i < coords.length; i++) {
    lenKm += haversineKm(coords[i - 1], coords[i])
  }
  const pxPerKm = 10
  const pixelLen = Math.max(lenKm * pxPerKm, 24)
  return Math.max(2.5, pixelLen / Math.max(constantSpeed, 8))
}

export function buildLineFlowTracks(
  lines: LineFeature[],
  activeMetric: ActiveMetric,
  opts: {
    intensityGamma?: number
    lineFlowRippleScaleMin?: number
    lineFlowRippleScaleMax?: number
    lineFlowRipplePeriodMin?: number
    lineFlowRipplePeriodMax?: number
  }
): LineFlowTrack[] {
  const {
    intensityGamma = 0.65,
    lineFlowRippleScaleMin = 2.4,
    lineFlowRippleScaleMax = 5.2,
    lineFlowRipplePeriodMin = 3.2,
    lineFlowRipplePeriodMax = 5.2
  } = opts

  const field = getLineIntensityField(activeMetric)
  const intensities = lines.map(l => getLineIntensity(l, field))
  const minI = Math.min(...intensities)
  const maxI = Math.max(...intensities)
  const { min: minE, max: maxE } = getLineEmissionRange(lines)

  return lines.map(line => {
    const emission = getLineCarbonEmission(line)
    const emissionNorm = normalizeIntensity(emission, minE, maxE, intensityGamma)
    const intensity = getLineIntensity(line, field)
    const flowColor = getLineColor(intensity, minI, maxI)

    return {
      sectionId: line.properties.sectionId,
      flowCoords: getFlowCoords(line),
      flowColor,
      emissionNorm,
      rippleScale:
        lineFlowRippleScaleMin + emissionNorm * (lineFlowRippleScaleMax - lineFlowRippleScaleMin),
      ripplePeriod:
        lineFlowRipplePeriodMax - emissionNorm * (lineFlowRipplePeriodMax - lineFlowRipplePeriodMin),
      sectionName: line.properties.sectionName,
      direction: line.properties.direction,
      carbonEmission: emission
    }
  })
}

export function tickFlowPhases(
  phases: Map<string, number>,
  tracks: LineFlowTrack[],
  dtSec: number,
  lineFlowSpeed: number
): void {
  for (const track of tracks) {
    const oneWayDuration = segmentLoopDurationSec(track.flowCoords, lineFlowSpeed)
    const prev = phases.get(track.sectionId) ?? 0
    const nextLinear = prev + dtSec / oneWayDuration
    phases.set(track.sectionId, nextLinear)
  }
}

export function createInitialPhases(tracks: LineFlowTrack[]): Map<string, number> {
  const phases = new Map<string, number>()
  tracks.forEach(track => {
    phases.set(track.sectionId, 0)
  })
  return phases
}

/** 用 graphic 绘制流动光点 + 涟漪环；光点与拖尾由 tour 或 OSM lines effect 负责 */
export function buildFlowRippleGraphicElements(
  chart: { convertToPixel: (finder: { geoIndex: number }, value: number[]) => number[] | false },
  tracks: LineFlowTrack[],
  phases: Map<string, number>,
  animTimeSec: number,
  opts: { showRippleRings?: boolean; showFlowDot?: boolean; lineFlowSymbolSize?: number } = {}
): Record<string, unknown>[] {
  const { showRippleRings = true, showFlowDot = true, lineFlowSymbolSize = 5 } = opts
  if (!showRippleRings && !showFlowDot) return []

  const elements: Record<string, unknown>[] = []
  const rippleRingCount = 3

  for (const track of tracks) {
    const phase = ((phases.get(track.sectionId) ?? 0) % 1 + 1) % 1
    const [lng, lat] = sampleFlowPosition(track.flowCoords, phase)
    const pixel = chart.convertToPixel({ geoIndex: 0 }, [lng, lat])
    if (!pixel || pixel.length < 2 || !isFinite(pixel[0]) || !isFinite(pixel[1])) {
      continue
    }

    const stroke = colorWithAlpha(track.flowColor, 0.72)

    if (showFlowDot) {
      elements.push({
        type: 'circle',
        id: `${track.sectionId}-dot`,
        position: pixel,
        shape: { cx: 0, cy: 0, r: lineFlowSymbolSize / 2 + 1 },
        style: {
          fill: track.flowColor,
          stroke: '#ffffff',
          lineWidth: 1,
          opacity: 0.95,
          shadowBlur: 8,
          shadowColor: colorWithAlpha(track.flowColor, 0.8)
        },
        silent: true,
        z: 101
      })
    }

    if (!showRippleRings) continue

    for (let i = 0; i < rippleRingCount; i++) {
      const ringPhase = (animTimeSec / track.ripplePeriod + i / rippleRingCount) % 1
      const r = 5 + ringPhase * track.rippleScale * 5
      const opacity = (1 - ringPhase) * 0.5

      elements.push({
        type: 'circle',
        id: `${track.sectionId}-ring-${i}`,
        position: pixel,
        shape: { cx: 0, cy: 0, r },
        style: {
          stroke,
          fill: 'none',
          lineWidth: 1.5,
          opacity
        },
        silent: true,
        z: 100
      })
    }
  }

  return elements
}

export function buildSingleFlowGuideTrack(
  flowCoords: [number, number][],
  flowColor: string,
  _lineFlowSpeed: number
): LineFlowTrack {
  return {
    sectionId: 'railway-flow-guide',
    flowCoords,
    flowColor,
    emissionNorm: 0.5,
    rippleScale: 3.2,
    ripplePeriod: 4,
    sectionName: '全线流动',
    direction: 'both',
    carbonEmission: 0
  }
}
