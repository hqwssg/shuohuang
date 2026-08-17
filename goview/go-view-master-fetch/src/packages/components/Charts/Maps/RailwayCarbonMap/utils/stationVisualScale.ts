import { IMPORTANT_STATION_NAMES, MINOR_STATION_VISUAL_SCALE } from '../types'
import type { AccountingPoint } from '../types'

const importantSet = new Set(IMPORTANT_STATION_NAMES)

export function isImportantStation(name: string): boolean {
  return importantSet.has(name)
}

export function getStationVisualScale(name: string): number {
  return isImportantStation(name) ? 1 : MINOR_STATION_VISUAL_SCALE
}

export function scalePx(base: number, stationName: string): number {
  return base * getStationVisualScale(stationName)
}

export function filterImportantStations(points: AccountingPoint[]): AccountingPoint[] {
  return points.filter(p => isImportantStation(p.name))
}
