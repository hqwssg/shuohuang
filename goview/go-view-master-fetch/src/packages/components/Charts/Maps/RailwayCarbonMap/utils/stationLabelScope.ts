import type { StationLabelScope } from '../types'
import { IMPORTANT_STATION_NAMES } from '../types'

const importantSet = new Set(IMPORTANT_STATION_NAMES)

export function shouldShowStationLabel(
  stationName: string,
  scope: StationLabelScope,
  showPointLabels: boolean
): boolean {
  if (!showPointLabels) return false
  if (scope === 'all') return true
  return importantSet.has(stationName)
}
