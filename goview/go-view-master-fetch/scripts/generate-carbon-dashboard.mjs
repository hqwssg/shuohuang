/**
 * 生成朔黄铁路碳排放全景大屏 GoView 项目 JSON
 * Run: cd go-view-master-fetch && node scripts/generate-carbon-dashboard.mjs
 */
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import {
  kpiMock,
  lineTrendMock,
  structurePiesMock,
  stationRankMock,
  threeSurfaceMock,
  tabOptionsMock,
  PIE_STRUCTURE_FILTER,
  PIE_STRUCTURE_VNODE_MOUNTED,
  STAT_PERIOD_LABEL
} from './carbon-dashboard-mock.mjs'
import {
  makeComponent,
  makeRailwayMapOption,
  makeThreeSurfaceOption,
  makeLineOption,
  makePieOption,
  makeTabOption,
  makeNumberOption,
  makeTextOption,
  makeBorderOption,
  makeTableOption,
  TAB_INTERACT_ACTIONS
} from './dashboard-component-factory.mjs'

const __dirname = dirname(fileURLToPath(import.meta.url))
const root = join(__dirname, '..')

const IDS = {
  map: 'dash-map-001',
  surface3d: 'dash-3d-001',
  lineTrend: 'dash-line-001',
  tabStructure: 'dash-tab-001',
  pieStructure: 'dash-pie-001',
  tableRank: 'dash-table-001',
  btnOverall: 'dash-btn-overall',
  kpi1: 'dash-kpi-001',
  kpi2: 'dash-kpi-002',
  kpi3: 'dash-kpi-003',
  kpi4: 'dash-kpi-004'
}

function readJson(relPath) {
  return JSON.parse(readFileSync(join(root, relPath), 'utf8'))
}

export function buildCarbonDashboardStorage() {
  const mapDataset = readJson('src/packages/components/Charts/Maps/RailwayCarbonMap/data.json')

  const map = makeComponent('RailwayCarbonMap', {
    id: IDS.map,
    attr: { x: 0, y: 0, w: 1920, h: 1080, zIndex: -1 },
    option: makeRailwayMapOption(mapDataset)
  })

  const surface = makeComponent('ThreeCarbonSurface', {
    id: IDS.surface3d,
    attr: { x: 32, y: 220, w: 388, h: 300, zIndex: 10 },
    option: makeThreeSurfaceOption(structuredClone(threeSurfaceMock))
  })

  const line = makeComponent('LineCommon', {
    id: IDS.lineTrend,
    attr: { x: 32, y: 600, w: 388, h: 240, zIndex: 10 },
    option: makeLineOption(structuredClone(lineTrendMock), [
      {
        type: 'line',
        name: '实际',
        encode: { x: 'month', y: 'actual' },
        lineStyle: { width: 3, color: '#2EC7FF' },
        symbolSize: 6,
        connectNulls: false
      },
      {
        type: 'line',
        name: '预测',
        encode: { x: 'month', y: 'forecast' },
        lineStyle: { width: 2, type: 'dashed', color: '#FFE600' },
        symbolSize: 5,
        connectNulls: false
      }
    ])
  })

  const tab = makeComponent('InputsTab', {
    id: IDS.tabStructure,
    attr: { x: 1500, y: 196, w: 388, h: 32, zIndex: 12 },
    option: makeTabOption(structuredClone(tabOptionsMock)),
    extra: {
      interactActions: TAB_INTERACT_ACTIONS,
      events: {
        baseEvent: { click: undefined, dblclick: undefined, mouseenter: undefined, mouseleave: undefined },
        advancedEvents: { vnodeBeforeMount: undefined, vnodeMounted: undefined },
        interactEvents: [
          {
            interactOn: 'change',
            interactComponentId: IDS.pieStructure,
            interactFn: { structureType: 'data' }
          }
        ]
      }
    }
  })

  const pie = makeComponent('PieCommon', {
    id: IDS.pieStructure,
    attr: { x: 1500, y: 236, w: 388, h: 300, zIndex: 10 },
    option: makePieOption(structuredClone(structurePiesMock.source)),
    extra: {
      filter: PIE_STRUCTURE_FILTER,
      request: {
        requestDataType: 0,
        requestHttpType: 'get',
        requestUrl: '',
        requestInterval: undefined,
        requestIntervalUnit: 'second',
        requestContentType: 0,
        requestParamsBodyType: 'none',
        requestSQLContent: { sql: 'select * from  where' },
        requestParams: {
          Body: { 'form-data': {}, 'x-www-form-urlencoded': {}, json: '', xml: '' },
          Header: {},
          Params: { structureType: 'source' }
        }
      },
      events: {
        baseEvent: { click: undefined, dblclick: undefined, mouseenter: undefined, mouseleave: undefined },
        advancedEvents: { vnodeBeforeMount: undefined, vnodeMounted: undefined },
        interactEvents: []
      }
    }
  })

  const table = makeComponent('TableScrollBoard', {
    id: IDS.tableRank,
    attr: { x: 1500, y: 600, w: 388, h: 440, zIndex: 10 },
    option: makeTableOption(stationRankMock)
  })

  const btnOverall = makeComponent('TextCommon', {
    id: IDS.btnOverall,
    attr: { x: 32, y: 118, w: 96, h: 36, zIndex: 16 },
    option: makeTextOption('总数据', 14, {
      fontColor: '#E6F7FF',
      borderColor: '#2EC7FF',
      borderWidth: 1,
      borderRadius: 4,
      backgroundColor: 'rgba(2,12,24,0.55)',
      letterSpacing: 0,
      paddingX: 8,
      paddingY: 6,
      stationScopeAction: 'overall',
      highlightWhenOverall: true
    })
  })

  const kpis = [
    { id: IDS.kpi1, x: 200, data: kpiMock.yearTotal, kpiField: 'yearTotal' },
    { id: IDS.kpi2, x: 432, data: kpiMock.monthTotal, kpiField: 'monthTotal' },
    { id: IDS.kpi3, x: 664, data: kpiMock.yoyPercent, kpiField: 'yoyPercent' },
    { id: IDS.kpi4, x: 896, data: kpiMock.pointCount, kpiField: 'pointCount' }
  ].map(({ id, x, data, kpiField }) =>
    makeComponent('Number', {
      id,
      attr: { x, y: 92, w: 220, h: 72, zIndex: 15 },
      option: makeNumberOption(data.value, {
        prefix: data.prefix,
        suffix: data.suffix,
        precision: data.label === '同比变化' ? 1 : 0
      }),
      extra: {
        request: {
          requestDataType: 0,
          requestHttpType: 'get',
          requestUrl: '',
          requestInterval: undefined,
          requestIntervalUnit: 'second',
          requestContentType: 0,
          requestParamsBodyType: 'none',
          requestSQLContent: { sql: 'select * from  where' },
          requestParams: {
            Body: { 'form-data': {}, 'x-www-form-urlencoded': {}, json: '', xml: '' },
            Header: {},
            Params: { kpiField }
          }
        }
      }
    })
  )

  const texts = [
    ['dash-title', 480, 16, 960, 44, '铁路运输绿色低碳节能技术数据可视化平台', 26],
    ['dash-subtitle', 520, 58, 880, 28, '朔黄铁路碳排放全景视图', 18],
    ['dash-period', 520, 86, 880, 24, STAT_PERIOD_LABEL, 14],
    ['dash-lbl-3d', 32, 192, 300, 24, '碳排放三维全景', 14],
    ['dash-lbl-line', 32, 572, 300, 24, '近12个月排放趋势', 14],
    ['dash-lbl-pie', 1500, 168, 300, 24, '排放结构分析', 14],
    ['dash-lbl-table', 1500, 572, 300, 24, '核算点排放排行', 14]
  ].map(([id, x, y, w, h, text, fontSize]) =>
    makeComponent('TextCommon', {
      id,
      attr: { x, y, w, h, zIndex: 15 },
      option: makeTextOption(text, fontSize)
    })
  )

  const borders = [
    ['dash-border-3d', 16, 180, 420, 360],
    ['dash-border-line', 16, 560, 420, 300],
    ['dash-border-pie', 1484, 180, 420, 380],
    ['dash-border-table', 1484, 560, 420, 500]
  ].map(([id, x, y, w, h]) =>
    makeComponent('Border02', {
      id,
      attr: { x, y, w, h, zIndex: 8 },
      option: makeBorderOption(),
      extra: { styles: { opacity: 0.85 } }
    })
  )

  const componentList = [map, ...borders, btnOverall, surface, line, tab, pie, table, ...kpis, ...texts]

  return {
    editCanvasConfig: {
      projectName: '朔黄铁路碳排放全景大屏',
      width: 1920,
      height: 1080,
      background: '#020a14',
      backgroundImage: null,
      selectColor: true,
      chartThemeColor: 'dark',
      chartCustomThemeColorInfo: undefined,
      chartThemeSetting: {},
      vChartThemeName: 'vScreenVolcanoBlue',
      previewScaleType: 'fit',
      projectId: null,
      remarks: '内置演示数据 · 碳排放全景视图'
    },
    requestGlobalConfig: {
      requestInterval: 0,
      requestIntervalUnit: 'second',
      requestOriginUrl: '',
      requestDataPond: [],
      requestParams: {
        Body: { 'form-data': {}, 'x-www-form-urlencoded': {}, json: '', xml: '' },
        Header: {},
        Params: { dataScope: 'overall', stationName: '', scopeGeneration: 0 }
      }
    },
    componentList
  }
}

export function writeTemplateJson(storage) {
  const outDir = join(root, 'public/templates')
  mkdirSync(outDir, { recursive: true })
  const outPath = join(outDir, 'shuohuang-carbon-dashboard.json')
  writeFileSync(outPath, JSON.stringify(storage, null, 2), 'utf8')
  console.log('Generated:', outPath, 'components:', storage.componentList.length)
  return outPath
}

function main() {
  const storage = buildCarbonDashboardStorage()
  writeTemplateJson(storage)
}

const isMain = process.argv[1] && fileURLToPath(import.meta.url) === process.argv[1]
if (isMain) {
  try {
    main()
  } catch (err) {
    console.error(err)
    process.exit(1)
  }
}
