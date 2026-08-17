function scaleNum(n, ratio, digits = 1) {
  if (n == null) return null
  const v = Number(n) * ratio
  return Math.round(v * 10 ** digits) / 10 ** digits
}

function scaleLineSource(source, ratio) {
  return source.map(row => ({
    ...row,
    actual: row.actual == null ? null : scaleNum(row.actual, ratio, 1),
    forecast: row.forecast == null ? null : scaleNum(row.forecast, ratio, 1)
  }))
}

function scaleSurfaceValues(values, ratio) {
  return values.map(row => row.map(v => scaleNum(v, ratio, 1)))
}

function jitterPie(structurePiesMock, seed) {
  const out = {}
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

export function buildDashboardScopeData({
  mapData,
  kpiMock,
  lineTrendMock,
  threeSurfaceMock,
  structurePiesMock
}) {
  const points = mapData.points.points
  const totalEmission = points.reduce((s, p) => s + p.carbonEmission, 0)
  const stations = {}

  points.forEach((p, idx) => {
    const ratio = p.carbonEmission / totalEmission
    stations[p.name] = {
      kpi: {
        yearTotal: scaleNum(kpiMock.yearTotal.value, ratio, 2),
        monthTotal: scaleNum(kpiMock.monthTotal.value, ratio, 2),
        yoyPercent: scaleNum(kpiMock.yoyPercent.value + (idx % 7) * 0.3 - 1, 1, 1),
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

export function buildStationRankFromMap(mapData) {
  return mapData.points.points
    .slice()
    .sort((a, b) => b.carbonEmission - a.carbonEmission)
    .map(p => [p.name, String(Math.round(p.carbonEmission)), String(p.carbonIntensity)])
}
