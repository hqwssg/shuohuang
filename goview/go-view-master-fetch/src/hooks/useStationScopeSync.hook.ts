import { watch, nextTick } from 'vue'
import { isPreview } from '@/utils'
import { getPreviewStationScope, getScopeGeneration } from '@/assets/dashboard/stationScopeController'
import { dashboardScopeData } from '@/assets/dashboard/carbon-dashboard-mock'

export type ScopePanelKind = 'kpi' | 'line' | 'surface' | 'pie'

export type KpiScopeField = 'yearTotal' | 'monthTotal' | 'yoyPercent' | 'pointCount' | 'intensity'

export interface UseStationScopeSyncOptions<T> {
  chartEditStore: {
    requestGlobalConfig: {
      requestParams: { Params: Record<string, unknown> }
    }
  }
  kind: ScopePanelKind
  kpiField?: KpiScopeField
  apply: (payload: T) => void
  replayEntrance?: () => void
  getStructureType?: () => string
}

function resolvePayload(
  kind: ScopePanelKind,
  scope: ReturnType<typeof getPreviewStationScope>,
  kpiField: KpiScopeField = 'yearTotal',
  structureType = 'source'
) {
  const bucket =
    scope.dataScope === 'station' && scope.stationName
      ? dashboardScopeData.stations[scope.stationName]
      : dashboardScopeData.overall

  if (!bucket) return null

  switch (kind) {
    case 'kpi': {
      if (scope.dataScope === 'overall' && kpiField === 'pointCount') {
        return dashboardScopeData.overall.kpi.pointCount
      }
      if (scope.dataScope === 'station' && kpiField === 'pointCount') {
        return dashboardScopeData.stations[scope.stationName]?.kpi.intensity ?? null
      }
      const key = kpiField === 'pointCount' ? 'pointCount' : kpiField
      return (bucket.kpi as Record<string, number>)[key] ?? null
    }
    case 'line':
      return bucket.line
    case 'surface':
      return bucket.surface
    case 'pie':
      return (bucket.pies as Record<string, unknown>)[structureType] ?? bucket.pies.source
    default:
      return null
  }
}

export function useStationScopeSync<T>(opts: UseStationScopeSyncOptions<T>) {
  if (!isPreview()) return

  const run = (withEntrance: boolean) => {
    const scope = getPreviewStationScope(opts.chartEditStore)
    const structureType = opts.getStructureType?.() ?? 'source'
    const payload = resolvePayload(opts.kind, scope, opts.kpiField, structureType)
    if (payload == null) {
      if (scope.dataScope === 'station') {
        console.warn('[stationScope] unknown station:', scope.stationName)
      }
      return
    }
    opts.apply(payload as T)
    if (withEntrance && getScopeGeneration(opts.chartEditStore.requestGlobalConfig.requestParams) > 0) {
      void nextTick(() => {
        opts.replayEntrance?.()
      })
    }
  }

  watch(
    () => [
      opts.chartEditStore.requestGlobalConfig.requestParams.Params.dataScope,
      opts.chartEditStore.requestGlobalConfig.requestParams.Params.stationName,
      opts.chartEditStore.requestGlobalConfig.requestParams.Params.scopeGeneration
    ],
    () => run(true),
    { immediate: true }
  )

  if (opts.kind === 'pie' && opts.getStructureType) {
    watch(opts.getStructureType, () => run(false))
  }
}
