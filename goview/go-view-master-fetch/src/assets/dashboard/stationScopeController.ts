export type DataScope = 'overall' | 'station'

export interface StationScope {
  dataScope: DataScope
  stationName: string
}

export interface ScopeParamsHolder {
  Params: Record<string, unknown>
}

export function createScopeParamsHolder(): ScopeParamsHolder {
  return { Params: { dataScope: 'overall', stationName: '', scopeGeneration: 0 } }
}

export function getStationScope(holder: ScopeParamsHolder): StationScope {
  const scope = holder.Params.dataScope === 'station' ? 'station' : 'overall'
  const stationName = scope === 'station' ? String(holder.Params.stationName ?? '') : ''
  return { dataScope: scope, stationName }
}

export function getScopeGeneration(holder: ScopeParamsHolder): number {
  return Number(holder.Params.scopeGeneration) || 0
}

export function setStationScope(
  holder: ScopeParamsHolder,
  scope: DataScope,
  stationName = ''
): boolean {
  const next: StationScope =
    scope === 'station'
      ? { dataScope: 'station', stationName }
      : { dataScope: 'overall', stationName: '' }

  if (!next.stationName && next.dataScope === 'station') {
    console.warn('[stationScope] missing stationName')
    return false
  }

  const prev = getStationScope(holder)
  if (prev.dataScope === next.dataScope && prev.stationName === next.stationName) {
    return false
  }

  holder.Params.scopeGeneration = getScopeGeneration(holder) + 1
  holder.Params.dataScope = next.dataScope
  holder.Params.stationName = next.stationName
  return true
}

export function setPreviewStationScope(
  chartEditStore: { requestGlobalConfig: { requestParams: ScopeParamsHolder } },
  scope: DataScope,
  stationName = ''
): boolean {
  return setStationScope(chartEditStore.requestGlobalConfig.requestParams, scope, stationName)
}

export function getPreviewStationScope(
  chartEditStore: { requestGlobalConfig: { requestParams: ScopeParamsHolder } }
): StationScope {
  return getStationScope(chartEditStore.requestGlobalConfig.requestParams)
}
