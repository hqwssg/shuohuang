import type { AccountingPoint, LineFeature } from '../types'
import { DEFAULT_STATION_ORDER } from '../types'
import { polylineLengthKm, slicePolylineBetween } from './routeGeometry'

function findPoint(points: AccountingPoint[], name: string): AccountingPoint | undefined {
  return points.find(p => p.name === name)
}

function findFullLineParent(
  parentLines: LineFeature[],
  direction: 'up' | 'down'
): LineFeature | undefined {
  return parentLines.find(f => f.properties.direction === direction)
}

function computeSegmentEmission(
  from: AccountingPoint,
  to: AccountingPoint,
  subLengthKm: number,
  parent: LineFeature,
  parentUpIntensity: number
): number {
  const endpointAvg = (from.carbonEmission + to.carbonEmission) / 2
  const lengthRatio = subLengthKm / parent.properties.lengthKm
  const directionIntensity = parent.properties.carbonIntensity
  const intensityFactor =
    parentUpIntensity > 0 ? directionIntensity / parentUpIntensity : 1
  return endpointAvg * lengthRatio * intensityFactor
}

/**
 * 沿全线折线为相邻站点生成站间连线（每段含上行/下行）
 */
export function buildAdjacentStationLines(
  points: AccountingPoint[],
  parentLines: LineFeature[],
  stationOrder: string[] = DEFAULT_STATION_ORDER
): LineFeature[] {
  const segments: LineFeature[] = []
  const order = stationOrder.length >= 2 ? stationOrder : DEFAULT_STATION_ORDER

  const parentUp = findFullLineParent(parentLines, 'up')
  const parentDown = findFullLineParent(parentLines, 'down')
  const parentUpIntensity = parentUp?.properties.carbonIntensity ?? 1

  for (let i = 0; i < order.length - 1; i++) {
    const fromName = order[i]
    const toName = order[i + 1]
    const from = findPoint(points, fromName)
    const to = findPoint(points, toName)

    if (!from || !to) {
      console.warn(`RailwayCarbonMap: 站点未找到，跳过连线 ${fromName} → ${toName}`)
      continue
    }

    const sectionLabel = `${fromName.replace(/站$/, '')}-${toName.replace(/站$/, '')}`

    for (const direction of ['up', 'down'] as const) {
      const parent = direction === 'up' ? parentUp : parentDown
      if (!parent) continue

      const parentCoords = parent.geometry.coordinates as [number, number][]
      const coords = slicePolylineBetween(parentCoords, from, to)
      const subLengthKm = polylineLengthKm(coords)
      const carbonEmission = computeSegmentEmission(
        from,
        to,
        subLengthKm,
        parent,
        parentUpIntensity
      )

      const endpointAvgIntensity = (from.carbonIntensity + to.carbonIntensity) / 2
      const carbonIntensity =
        Math.round(
          (endpointAvgIntensity * 0.55 + parent.properties.carbonIntensity * 0.45) * 10
        ) / 10
      const energyIntensity =
        Math.round(parent.properties.energyIntensity * (0.94 + (i % 4) * 0.03) * 10) / 10

      segments.push({
        type: 'Feature',
        geometry: {
          type: 'LineString',
          coordinates: coords
        },
        properties: {
          sectionName: sectionLabel,
          direction,
          sectionId: `${fromName}-${toName}-${direction.toUpperCase()}`,
          energyIntensity,
          carbonIntensity,
          carbonEmission,
          statPeriod: parent.properties.statPeriod,
          lengthKm: Math.round(subLengthKm * 10) / 10,
          unit: from.unit
        }
      })
    }
  }

  return segments
}
