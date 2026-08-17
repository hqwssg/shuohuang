import type { AccountingPoint } from '../types'

export interface DirectionalSegmentMetrics {
  carbonIntensity: number
  energyIntensity: number
  carbonEmission: number
}

export interface DirectionalSegmentPair {
  up: DirectionalSegmentMetrics
  down: DirectionalSegmentMetrics
}

function round1(n: number): number {
  return Math.round(n * 10) / 10
}

/**
 * 为站间段生成上下行差异化指标（同段差约 20%–40%）。
 */
export function buildDirectionalSegmentMetrics(
  segmentIndex: number,
  totalSegments: number,
  from: AccountingPoint,
  to: AccountingPoint,
  lengthKm: number,
  totalRouteKm: number
): DirectionalSegmentPair {
  const baseIntensity = (from.carbonIntensity + to.carbonIntensity) / 2
  const endpointAvgEmission = (from.carbonEmission + to.carbonEmission) / 2
  const lengthShare = lengthKm / totalRouteKm

  const progress = totalSegments > 1 ? segmentIndex / (totalSegments - 1) : 0
  const hillFactor = Math.max(0, 1 - progress * 2.2)
  const plainFactor = Math.max(0, (progress - 0.35) * 1.8)
  const wave = 0.92 + 0.08 * Math.sin(segmentIndex * 0.85)

  const upIntensity = baseIntensity * (1.08 + 0.28 * hillFactor) * wave
  const downIntensity = baseIntensity * (0.82 + 0.32 * plainFactor) * (1.05 - wave * 0.05)

  const upEnergy = 27.5 + baseIntensity * 0.35 + hillFactor * 4.5
  const downEnergy = 24.5 + baseIntensity * 0.28 + plainFactor * 5.2

  const upEmission = endpointAvgEmission * lengthShare * (1.06 + hillFactor * 0.18)
  const downEmission = endpointAvgEmission * lengthShare * (0.88 + plainFactor * 0.22)

  return {
    up: {
      carbonIntensity: round1(upIntensity),
      energyIntensity: round1(upEnergy),
      carbonEmission: round1(upEmission)
    },
    down: {
      carbonIntensity: round1(downIntensity),
      energyIntensity: round1(downEnergy),
      carbonEmission: round1(downEmission)
    }
  }
}
