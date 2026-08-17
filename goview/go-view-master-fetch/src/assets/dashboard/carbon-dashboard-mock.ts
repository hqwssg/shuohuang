/** 朔黄铁路碳排放全景大屏 — 内置演示数据 */

import mapData from '@/packages/components/Charts/Maps/RailwayCarbonMap/data.json'
import type { RailwayCarbonMapData } from '@/packages/components/Charts/Maps/RailwayCarbonMap/types'
import { buildDashboardScopeData, buildStationRankFromMap } from './buildDashboardScopeData'

/** 大屏顶部「统计周期」文案（单一数据源，预览态运行时也会读取） */
export const STAT_PERIOD_LABEL = '统计周期：2025年6月—2026年5月 · 内置演示数据'

export const kpiMock = {
  yearTotal: { value: 14250, prefix: '', suffix: '万tCO₂e', label: '本年累计排放' },
  monthTotal: { value: 1140, prefix: '', suffix: '万tCO₂e', label: '本月排放' },
  yoyPercent: { value: -3.2, prefix: '', suffix: '%', label: '同比变化' },
  pointCount: { value: 35, prefix: '', suffix: '个', label: '核算点数量' }
}

export const lineTrendMock = {
  dimensions: ['month', 'actual', 'forecast'],
  source: [
    { month: '2025-06', actual: 1075, forecast: null },
    { month: '2025-07', actual: 1050, forecast: null },
    { month: '2025-08', actual: 1060, forecast: null },
    { month: '2025-09', actual: 1105, forecast: null },
    { month: '2025-10', actual: 1155, forecast: null },
    { month: '2025-11', actual: 1210, forecast: null },
    { month: '2025-12', actual: 1140, forecast: null },
    { month: '2026-01', actual: 1120, forecast: null },
    { month: '2026-02', actual: 1095, forecast: null },
    { month: '2026-03', actual: 1080, forecast: null },
    { month: '2026-04', actual: 1135, forecast: null },
    { month: '2026-05', actual: 1140, forecast: 1140 },
    { month: '2026-06', actual: null, forecast: 1120 },
    { month: '2026-07', actual: null, forecast: 1095 },
    { month: '2026-08', actual: null, forecast: 1080 }
  ]
}

export const structurePiesMock = {
  source: {
    dimensions: ['name', 'value'],
    source: [
      { name: '外购电力', value: 38 },
      { name: '机车燃油', value: 42 },
      { name: '废弃物处理', value: 8 },
      { name: '其他', value: 12 }
    ]
  },
  type: {
    dimensions: ['name', 'value'],
    source: [
      { name: '直接排放', value: 55 },
      { name: '间接排放', value: 45 }
    ]
  },
  segment: {
    dimensions: ['name', 'value'],
    source: [
      { name: '机车运行', value: 48 },
      { name: '车站运营', value: 32 },
      { name: '维护修理', value: 20 }
    ]
  }
}

export const structureChangeMock = {
  dimensions: ['month', 'source', 'type', 'segment'],
  source: [
    { month: '7月', source: 42, type: 28, segment: 30 },
    { month: '8月', source: 40, type: 30, segment: 30 },
    { month: '9月', source: 38, type: 32, segment: 30 },
    { month: '10月', source: 36, type: 34, segment: 30 },
    { month: '11月', source: 35, type: 35, segment: 30 },
    { month: '12月', source: 34, type: 36, segment: 30 }
  ]
}

export const threeSurfaceMock = {
  timeType: 'month',
  years: [2023, 2024, 2025, 2026],
  times: ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'],
  values: [
    [1280.5, 1310.2, 1295.0, 1240.8, 1180.3, 1120.6, 1095.4, 1105.1, 1150.7, 1200.2, 1255.8, 1275.0],
    [1235.5, 1265.2, 1250.0, 1195.8, 1135.3, 1075.6, 1050.4, 1060.1, 1105.7, 1155.2, 1210.8, 1230.0],
    [1190.5, 1220.2, 1205.0, 1150.8, 1090.3, 1030.6, 1005.4, 1015.1, 1060.7, 1110.2, 1165.8, 1185.0],
    [1145.5, 1175.2, 1160.0, 1105.8, 1045.3, 985.6, 960.4, 970.1, 1015.7, 1065.2, 1120.8, 1140.0]
  ],
  unit: 'tCO2e'
}

export const tabOptionsMock = [
  { label: '排放源', value: 'source' },
  { label: '排放类型', value: 'type' },
  { label: '业务环节', value: 'segment' }
]

export const PIE_STRUCTURE_FILTER = `const type = res?.structureType || 'source'
const all = ${JSON.stringify(structurePiesMock, null, 2)}
return all[type] || all.source`

/** 饼图 scope/structure 联动改由 useStationScopeSync hook 驱动 */
export const PIE_STRUCTURE_VNODE_MOUNTED = undefined

export const dashboardScopeData = buildDashboardScopeData({
  mapData: mapData as RailwayCarbonMapData,
  kpiMock,
  lineTrendMock,
  threeSurfaceMock,
  structurePiesMock
})

export const stationRankMock: string[][] = buildStationRankFromMap(mapData as RailwayCarbonMapData)
