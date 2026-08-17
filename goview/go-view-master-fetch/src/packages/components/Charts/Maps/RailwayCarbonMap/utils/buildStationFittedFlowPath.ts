import type { AccountingPoint } from '../types'
import { DEFAULT_STATION_ORDER } from '../types'

export function buildStationFittedFlowCoords(
  points: AccountingPoint[],
  stationOrder: string[] = DEFAULT_STATION_ORDER
): [number, number][] {
  const byName = new Map(points.map(p => [p.name, p]))
  const coords: [number, number][] = []

  for (const name of stationOrder) {
    const p = byName.get(name)
    if (p) coords.push([p.lng, p.lat])
  }

  return coords.length >= 2 ? coords : []
}
