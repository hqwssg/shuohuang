import type { AccountingPoint, LineFeature, MonitoringPoint } from '../types'
import { DEFAULT_STATION_ORDER } from '../types'
import { polylineLengthKm, sampleAlongPolyline, slicePolylineBetween } from './routeGeometry'

const POINTS_PER_SEGMENT = 7

/** 沿真实子折线生成站间监测点；每段每方向 7 点，含起终点 */
export function buildMockMonitoringPoints(
  stations: AccountingPoint[],
  parentLines: LineFeature[],
  stationOrder: string[] = DEFAULT_STATION_ORDER
): MonitoringPoint[] {
  const result: MonitoringPoint[] = []
  const find = (name: string) => stations.find(s => s.name === name)
  const parentUp = parentLines.find(f => f.properties.direction === 'up')
  const parentDown = parentLines.find(f => f.properties.direction === 'down')

  if (!parentUp || !parentDown) return result

  const upCoords = parentUp.geometry.coordinates as [number, number][]
  const downCoords = parentDown.geometry.coordinates as [number, number][]

  for (let i = 0; i < stationOrder.length - 1; i++) {
    const fromName = stationOrder[i]
    const toName = stationOrder[i + 1]
    const from = find(fromName)
    const to = find(toName)
    if (!from || !to) continue

    const baseIntensity = (from.carbonIntensity + to.carbonIntensity) / 2
    const baseEnergy = 27 + (i % 3) * 0.8

    for (const direction of ['up', 'down'] as const) {
      const parentCoords = direction === 'up' ? upCoords : downCoords
      const segCoords = slicePolylineBetween(parentCoords, from, to)
      const segmentKm = polylineLengthKm(segCoords)
      const dirFactor = direction === 'up' ? 1 : 0.76 + (i % 3) * 0.03
      const samples = sampleAlongPolyline(segCoords, POINTS_PER_SEGMENT, true)

      samples.forEach((sample, j) => {
        const wave = 0.85 + 0.3 * Math.sin(j * 1.2 + i)
        result.push({
          lng: Math.round(sample.lng * 10000) / 10000,
          lat: Math.round(sample.lat * 10000) / 10000,
          fromStation: fromName,
          toStation: toName,
          direction,
          chainageKm: sample.chainageKm,
          carbonIntensity: Math.round(baseIntensity * dirFactor * wave * 10) / 10,
          energyIntensity: Math.round(baseEnergy * dirFactor * wave * 10) / 10
        })
      })
    }
  }
  return result
}
