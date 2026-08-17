import type { LineFeature, MonitoringPoint } from '../types'
import type { ActiveMetric } from './lineMetric'

const SUBDIVISIONS = 10

type ActiveDirection = 'all' | 'up' | 'down'

interface MergedSample {
  lng: number
  lat: number
  chainageKm: number
  carbonIntensity: number
  energyIntensity: number
  fromStation: string
  toStation: string
}

function lerp(a: number, b: number, t: number): number {
  return a + (b - a) * t
}

function chainageKey(km: number): number {
  return Math.round(km * 10) / 10
}

function mergeSegmentSamples(
  points: MonitoringPoint[],
  activeDirection: ActiveDirection
): MergedSample[] {
  if (activeDirection === 'up' || activeDirection === 'down') {
    return points
      .filter(p => p.direction === activeDirection)
      .sort((a, b) => a.chainageKm - b.chainageKm)
      .map(p => ({
        lng: p.lng,
        lat: p.lat,
        chainageKm: p.chainageKm,
        carbonIntensity: p.carbonIntensity,
        energyIntensity: p.energyIntensity,
        fromStation: p.fromStation,
        toStation: p.toStation
      }))
  }

  const buckets = new Map<number, MonitoringPoint[]>()
  for (const p of points) {
    const key = chainageKey(p.chainageKm)
    if (!buckets.has(key)) buckets.set(key, [])
    buckets.get(key)!.push(p)
  }

  return [...buckets.entries()]
    .sort((a, b) => a[0] - b[0])
    .map(([km, pts]) => ({
      lng: pts.reduce((s, p) => s + p.lng, 0) / pts.length,
      lat: pts.reduce((s, p) => s + p.lat, 0) / pts.length,
      chainageKm: km,
      carbonIntensity: pts.reduce((s, p) => s + p.carbonIntensity, 0) / pts.length,
      energyIntensity: pts.reduce((s, p) => s + p.energyIntensity, 0) / pts.length,
      fromStation: pts[0].fromStation,
      toStation: pts[0].toStation
    }))
}

function resolveLineDirection(activeDirection: ActiveDirection): 'up' | 'down' | 'both' {
  return activeDirection === 'all' ? 'both' : activeDirection
}

export function buildMonitoringGradientLines(
  monitoringPoints: MonitoringPoint[],
  activeMetric: ActiveMetric,
  statPeriod = '',
  activeDirection: ActiveDirection = 'all'
): LineFeature[] {
  const lineDirection = resolveLineDirection(activeDirection)
  const segmentGroups = new Map<string, MonitoringPoint[]>()

  for (const p of monitoringPoints) {
    const key = `${p.fromStation}|${p.toStation}`
    if (!segmentGroups.has(key)) segmentGroups.set(key, [])
    segmentGroups.get(key)!.push(p)
  }

  const features: LineFeature[] = []

  for (const [, segmentPoints] of segmentGroups) {
    const samples = mergeSegmentSamples(segmentPoints, activeDirection)
    if (samples.length < 2) continue

    const sectionLabel = `${samples[0].fromStation.replace(/站$/, '')}-${samples[0].toStation.replace(/站$/, '')}`

    for (let i = 0; i < samples.length - 1; i++) {
      const a = samples[i]
      const b = samples[i + 1]

      for (let s = 0; s < SUBDIVISIONS; s++) {
        const t0 = s / SUBDIVISIONS
        const t1 = (s + 1) / SUBDIVISIONS
        const tMid = (t0 + t1) / 2

        const carbonMid = lerp(a.carbonIntensity, b.carbonIntensity, tMid)
        const energyMid = lerp(a.energyIntensity, b.energyIntensity, tMid)
        const chainageStart = lerp(a.chainageKm, b.chainageKm, t0)
        const chainageEnd = lerp(a.chainageKm, b.chainageKm, t1)

        features.push({
          type: 'Feature',
          geometry: {
            type: 'LineString',
            coordinates: [
              [lerp(a.lng, b.lng, t0), lerp(a.lat, b.lat, t0)],
              [lerp(a.lng, b.lng, t1), lerp(a.lat, b.lat, t1)]
            ]
          },
          properties: {
            sectionName: sectionLabel,
            direction: lineDirection,
            sectionId: `${a.fromStation}-${a.toStation}-${lineDirection.toUpperCase()}-M${i}-S${s}`,
            carbonIntensity: carbonMid,
            energyIntensity: energyMid,
            statPeriod,
            lengthKm: Math.max(chainageEnd - chainageStart, 0.01),
            chainageStartKm: chainageStart,
            chainageEndKm: chainageEnd
          }
        })
      }
    }
  }

  return features
}
