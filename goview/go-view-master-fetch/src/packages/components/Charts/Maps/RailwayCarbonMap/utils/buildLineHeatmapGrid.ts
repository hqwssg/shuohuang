import type { LineFeature } from '../types'
import type { LineIntensityField } from './lineMetric'
import { getLineIntensity } from './lineMetric'

export type HeatmapAllDirectionMode = 'max' | 'avg' | 'separate'

export interface HeatmapSample {
  lng: number
  lat: number
  value: number
  direction: 'up' | 'down'
}

export interface BuildLineHeatmapGridOptions {
  boundingCoords: [[number, number], [number, number]]
  gridStep: number
  influenceRadiusDeg: number
  sampleIntervalKm: number
  activeDirection: 'all' | 'up' | 'down'
  allDirectionMode: HeatmapAllDirectionMode
  intensityField: LineIntensityField
  idwPower?: number
}

export interface HeatmapGridResult {
  data: [number, number, number][]
  min: number
  max: number
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

function degDistanceApprox(a: [number, number], b: [number, number]): number {
  const dLng = a[0] - b[0]
  const dLat = a[1] - b[1]
  return Math.sqrt(dLng * dLng + dLat * dLat)
}

function sampleAlongLine(line: LineFeature, field: LineIntensityField, intervalKm: number): HeatmapSample[] {
  const coords = line.geometry.coordinates
  if (coords.length < 2) return []

  const intensity = getLineIntensity(line, field)
  const direction = line.properties.direction === 'down' ? 'down' : 'up'
  const samples: HeatmapSample[] = []

  let cumulative = 0
  const segmentLengths: number[] = []
  for (let i = 1; i < coords.length; i++) {
    const len = haversineKm(coords[i - 1], coords[i])
    segmentLengths.push(len)
    cumulative += len
  }

  const totalKm = cumulative || line.properties.lengthKm || 1
  const count = Math.max(2, Math.ceil(totalKm / intervalKm) + 1)

  for (let i = 0; i < count; i++) {
    const t = count === 1 ? 0 : i / (count - 1)
    const targetKm = t * totalKm
    let walked = 0
    let lng = coords[0][0]
    let lat = coords[0][1]

    for (let s = 0; s < segmentLengths.length; s++) {
      const segLen = segmentLengths[s]
      if (walked + segLen >= targetKm || s === segmentLengths.length - 1) {
        const localT = segLen > 0 ? (targetKm - walked) / segLen : 0
        const c0 = coords[s]
        const c1 = coords[s + 1]
        lng = c0[0] + (c1[0] - c0[0]) * Math.min(1, Math.max(0, localT))
        lat = c0[1] + (c1[1] - c0[1]) * Math.min(1, Math.max(0, localT))
        break
      }
      walked += segLen
    }

    samples.push({ lng, lat, value: intensity, direction })
  }

  return samples
}

function idwAtPoint(
  lng: number,
  lat: number,
  samples: HeatmapSample[],
  influenceRadiusDeg: number,
  power: number
): number | null {
  let weightSum = 0
  let valueSum = 0
  let minDist = Infinity

  for (const s of samples) {
    const dist = degDistanceApprox([lng, lat], [s.lng, s.lat])
    minDist = Math.min(minDist, dist)
    if (dist > influenceRadiusDeg) continue

    if (dist < 1e-8) {
      return s.value
    }

    const w = 1 / dist ** power
    weightSum += w
    valueSum += w * s.value
  }

  if (weightSum === 0) {
    if (minDist <= influenceRadiusDeg * 1.05) {
      let nearest = samples[0]
      let nearestDist = Infinity
      for (const s of samples) {
        const d = degDistanceApprox([lng, lat], [s.lng, s.lat])
        if (d < nearestDist) {
          nearestDist = d
          nearest = s
        }
      }
      return nearest.value
    }
    return null
  }

  return valueSum / weightSum
}

function resolveGridValue(
  lng: number,
  lat: number,
  samples: HeatmapSample[],
  options: BuildLineHeatmapGridOptions
): number | null {
  const { activeDirection, allDirectionMode, influenceRadiusDeg, idwPower = 2 } = options

  if (activeDirection === 'up' || activeDirection === 'down') {
    const filtered = samples.filter(s => s.direction === activeDirection)
    return idwAtPoint(lng, lat, filtered, influenceRadiusDeg, idwPower)
  }

  if (allDirectionMode === 'separate') {
    return null
  }

  const upSamples = samples.filter(s => s.direction === 'up')
  const downSamples = samples.filter(s => s.direction === 'down')
  const upVal = idwAtPoint(lng, lat, upSamples, influenceRadiusDeg, idwPower)
  const downVal = idwAtPoint(lng, lat, downSamples, influenceRadiusDeg, idwPower)

  if (upVal == null && downVal == null) return null
  if (upVal == null) return downVal
  if (downVal == null) return upVal

  return allDirectionMode === 'max' ? Math.max(upVal, downVal) : (upVal + downVal) / 2
}

/**
 * 由站间线段生成 Geo 热力栅格（走廊 IDW 插值）
 */
export function buildLineHeatmapGrid(
  lines: LineFeature[],
  options: BuildLineHeatmapGridOptions
): HeatmapGridResult {
  const {
    boundingCoords,
    gridStep,
    influenceRadiusDeg,
    sampleIntervalKm,
    intensityField,
    activeDirection,
    allDirectionMode
  } = options

  if (activeDirection === 'all' && allDirectionMode === 'separate') {
    return { data: [], min: 0, max: 0 }
  }

  const [[minLng, maxLat], [maxLng, minLat]] = boundingCoords
  const samples: HeatmapSample[] = []

  for (const line of lines) {
    if (activeDirection !== 'all') {
      const dir = line.properties.direction
      if (dir !== activeDirection && dir !== 'both') continue
    }
    samples.push(...sampleAlongLine(line, intensityField, sampleIntervalKm))
  }

  if (samples.length === 0) {
    return { data: [], min: 0, max: 0 }
  }

  const data: [number, number, number][] = []

  for (let lng = minLng; lng <= maxLng; lng += gridStep) {
    for (let lat = minLat; lat <= maxLat; lat += gridStep) {
      const value = resolveGridValue(lng, lat, samples, options)
      if (value != null && isFinite(value)) {
        data.push([lng, lat, Math.round(value * 100) / 100])
      }
    }
  }

  if (data.length === 0) {
    return { data: [], min: 0, max: 0 }
  }

  const values = data.map(d => d[2])
  return {
    data,
    min: Math.min(...values),
    max: Math.max(...values)
  }
}
