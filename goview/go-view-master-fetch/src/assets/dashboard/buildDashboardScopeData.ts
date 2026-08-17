import type { RailwayCarbonMapData } from '@/packages/components/Charts/Maps/RailwayCarbonMap/types'

function scaleNum(n: number | null, ratio: number, digits = 1): number | null {
  if (n == null) return null
  const v = Number(n) * ratio
  return Math.round(v * 10 ** digits) / 10 ** digits
}

function scaleLineSource(
  source: Array<{ month: string; actual: number | null; forecast: number | null }>,
  ratio: number
) {
  return source.map(row => ({
    ...row,
    actual: row.actual == null ? null : scaleNum(row.actual, ratio, 1),
    forecast: row.forecast == null ? null : scaleNum(row.forecast, ratio, 1)
  }))
}

function scaleSurfaceValues(values: number[][], ratio: number) {
  return values.map(row => row.map(v => scaleNum(v, ratio, 1) as number))
}

function jitterPie(
  structurePiesMock: Record<string, { dimensions: string[]; source: Array<{ name: string; value: number }> }>,
  seed: number
) {
  const out: typeof structurePiesMock = {}
  for (const key of Object.keys(structurePiesMock)) {
    out[key] = {
      dimensions: [...structurePiesMock[key].dimensions],
      source: structurePiesMock[key].source.map((item, i) => ({
        ...item,
        value: Math.max(1, Math.round(item.value + ((seed + i) % 5) - 2))
      }))
    }
  }
  return out
}

export function buildDashboardScopeData(input: {
  mapData: RailwayCarbonMapData
  kpiMock: typeof import('./carbon-dashboard-mock').kpiMock
  lineTrendMock: typeof import('./carbon-dashboard-mock').lineTrendMock
  threeSurfaceMock: typeof import('./carbon-dashboard-mock').threeSurfaceMock
  structurePiesMock: typeof import('./carbon-dashboard-mock').structurePiesMock
}) {
  const { mapData, kpiMock, lineTrendMock, threeSurfaceMock, structurePiesMock } = input
  const points = mapData.points.points
  const totalEmission = points.reduce((s, p) => s + p.carbonEmission, 0)
  const stations: Record<
    string,
    {
      kpi: { yearTotal: number; monthTotal: number; yoyPercent: number; intensity: number }
      line: typeof lineTrendMock
      surface: typeof threeSurfaceMock
      pies: typeof structurePiesMock
    }
  > = {}

  points.forEach((p, idx) => {
    const ratio = p.carbonEmission / totalEmission
    stations[p.name] = {
      kpi: {
        yearTotal: scaleNum(kpiMock.yearTotal.value, ratio, 2) as number,
        monthTotal: scaleNum(kpiMock.monthTotal.value, ratio, 2) as number,
        yoyPercent: scaleNum(kpiMock.yoyPercent.value + (idx % 7) * 0.3 - 1, 1, 1) as number,
        intensity: p.carbonIntensity
      },
      line: {
        dimensions: [...lineTrendMock.dimensions],
        source: scaleLineSource(lineTrendMock.source, ratio)
      },
      surface: {
        ...threeSurfaceMock,
        values: scaleSurfaceValues(threeSurfaceMock.values, ratio)
      },
      pies: jitterPie(structurePiesMock, idx)
    }
  })

  return {
    overall: {
      kpi: {
        yearTotal: kpiMock.yearTotal.value,
        monthTotal: kpiMock.monthTotal.value,
        yoyPercent: kpiMock.yoyPercent.value,
        pointCount: kpiMock.pointCount.value
      },
      line: lineTrendMock,
      surface: threeSurfaceMock,
      pies: structurePiesMock
    },
    stations
  }
}

export function buildStationRankFromMap(mapData: RailwayCarbonMapData): string[][] {
  return mapData.points.points
    .slice()
    .sort((a, b) => b.carbonEmission - a.carbonEmission)
    .map(p => [p.name, String(Math.round(p.carbonEmission)), String(p.carbonIntensity)])
}
