import type { AccountingPoint, LineFeature } from '../types'
import { DEFAULT_STATION_ORDER } from '../types'
import { buildSplineSegmentCoords } from './curveGeometry'
import { buildStationFittedFlowCoords } from './buildStationFittedFlowPath'
import { buildDirectionalSegmentMetrics } from './directionalSegmentMetrics'

function polylineLengthKm(coords: [number, number][]): number {
  const R = 6371
  let len = 0
  for (let i = 1; i < coords.length; i++) {
    const a = coords[i - 1]
    const b = coords[i]
    const dLat = ((b[1] - a[1]) * Math.PI) / 180
    const dLng = ((b[0] - a[0]) * Math.PI) / 180
    const lat1 = (a[1] * Math.PI) / 180
    const lat2 = (b[1] * Math.PI) / 180
    const h =
      Math.sin(dLat / 2) ** 2 +
      Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) ** 2
    len += 2 * R * Math.asin(Math.sqrt(h))
  }
  return len
}

const SPLINE_STEPS_PER_SEGMENT = 8

/**
 * 相邻站间为同一条 Catmull-Rom 样条的子弧；每段生成 up/down 两套指标。
 */
export function buildCurvedStationLines(
  points: AccountingPoint[],
  stationOrder: string[] = DEFAULT_STATION_ORDER,
  statPeriod = ''
): LineFeature[] {
  const order = stationOrder.length >= 2 ? stationOrder : DEFAULT_STATION_ORDER
  const stationCoords = buildStationFittedFlowCoords(points, order)
  if (stationCoords.length < 2) return []

  const segmentMeta: {
    from: AccountingPoint
    to: AccountingPoint
    coords: [number, number][]
    lengthKm: number
    sectionLabel: string
  }[] = []

  for (let i = 0; i < order.length - 1; i++) {
    const fromName = order[i]
    const toName = order[i + 1]
    const from = points.find(p => p.name === fromName)
    const to = points.find(p => p.name === toName)
    if (!from || !to) {
      console.warn(`RailwayCarbonMap: 站点未找到，跳过连线 ${fromName} → ${toName}`)
      continue
    }

    const coords = buildSplineSegmentCoords(stationCoords, i, SPLINE_STEPS_PER_SEGMENT)
    if (coords.length < 2) continue

    segmentMeta.push({
      from,
      to,
      coords,
      lengthKm: polylineLengthKm(coords),
      sectionLabel: `${from.name.replace(/站$/, '')}-${to.name.replace(/站$/, '')}`
    })
  }

  const totalRouteKm = segmentMeta.reduce((s, m) => s + m.lengthKm, 0) || 1
  const totalSegments = segmentMeta.length
  const segments: LineFeature[] = []

  segmentMeta.forEach(({ from, to, coords, lengthKm, sectionLabel }, i) => {
    const metrics = buildDirectionalSegmentMetrics(
      i,
      totalSegments,
      from,
      to,
      lengthKm,
      totalRouteKm
    )
    const period = statPeriod || from.latestAccountingTime || ''

    for (const direction of ['up', 'down'] as const) {
      const m = metrics[direction]
      segments.push({
        type: 'Feature',
        geometry: {
          type: 'LineString',
          coordinates: coords
        },
        properties: {
          sectionName: sectionLabel,
          direction,
          sectionId: `${from.name}-${to.name}-${direction.toUpperCase()}`,
          energyIntensity: m.energyIntensity,
          carbonIntensity: m.carbonIntensity,
          carbonEmission: m.carbonEmission,
          statPeriod: period,
          lengthKm: Math.round(lengthKm * 10) / 10,
          unit: from.unit
        }
      })
    }
  })

  return segments
}
