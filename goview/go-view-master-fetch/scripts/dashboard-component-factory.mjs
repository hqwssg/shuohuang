/**
 * GoView 组件 JSON 工厂（纯 Node，不 import config.ts）
 */
import { randomUUID } from 'node:crypto'
import { DASH_PIE_TOOLTIP, DASH_PIE_SERIES_HOVER } from './dashPieHoverLabels.mjs'

const REQUEST = {
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
    Params: {}
  }
}

const EVENTS = {
  baseEvent: {
    click: undefined,
    dblclick: undefined,
    mouseenter: undefined,
    mouseleave: undefined
  },
  advancedEvents: {
    vnodeBeforeMount: undefined,
    vnodeMounted: undefined
  },
  interactEvents: []
}

const STYLES = {
  filterShow: false,
  hueRotate: 0,
  saturate: 1,
  contrast: 1,
  brightness: 1,
  opacity: 1,
  rotateZ: 0,
  rotateX: 0,
  rotateY: 0,
  skewX: 0,
  skewY: 0,
  blendMode: 'normal',
  animations: []
}

export const CHART_META = {
  RailwayCarbonMap: {
    key: 'RailwayCarbonMap',
    chartKey: 'VRailwayCarbonMap',
    conKey: 'VCRailwayCarbonMap',
    title: '铁路碳排放时空分布图',
    category: 'Maps',
    categoryName: '地图',
    package: 'Charts',
    chartFrame: 'common',
    image: 'railway_carbon_map.png'
  },
  ThreeCarbonSurface: {
    key: 'ThreeCarbonSurface',
    chartKey: 'VThreeCarbonSurface',
    conKey: 'VCThreeCarbonSurface',
    title: '3D碳排放热力曲面图',
    category: 'Three',
    categoryName: '三维',
    package: 'Decorates',
    chartFrame: 'common',
    image: 'threeCarbonSurface.png'
  },
  LineCommon: {
    key: 'LineCommon',
    chartKey: 'VLineCommon',
    conKey: 'VCLineCommon',
    title: '折线图',
    category: 'Lines',
    categoryName: '折线图',
    package: 'Charts',
    chartFrame: 'echarts',
    image: 'line.png'
  },
  InputsTab: {
    key: 'InputsTab',
    chartKey: 'VInputsTab',
    conKey: 'VCInputsTab',
    title: '标签选择器',
    category: 'Inputs',
    categoryName: '控件',
    package: 'Informations',
    chartFrame: 'static',
    image: 'inputs_tab.png'
  },
  PieCommon: {
    key: 'PieCommon',
    chartKey: 'VPieCommon',
    conKey: 'VCPieCommon',
    title: '饼图',
    category: 'Pies',
    categoryName: '饼图',
    package: 'Charts',
    chartFrame: 'echarts',
    image: 'pie.png'
  },
  TableScrollBoard: {
    key: 'TableScrollBoard',
    chartKey: 'VTableScrollBoard',
    conKey: 'VCTableScrollBoard',
    title: '轮播列表',
    category: 'Tables',
    categoryName: '表格',
    package: 'Tables',
    chartFrame: 'common',
    image: 'table_scrollboard.png'
  },
  BarLine: {
    key: 'BarLine',
    chartKey: 'VBarLine',
    conKey: 'VCBarLine',
    title: '柱状图 & 折线图',
    category: 'Bars',
    categoryName: '柱状图',
    package: 'Charts',
    chartFrame: 'echarts',
    image: 'bar_line.png'
  },
  Number: {
    key: 'Number',
    chartKey: 'VNumber',
    conKey: 'VCNumber',
    title: '数字计数',
    category: 'Mores',
    categoryName: '更多',
    package: 'Decorates',
    chartFrame: 'common',
    image: 'number.png'
  },
  TextCommon: {
    key: 'TextCommon',
    chartKey: 'VTextCommon',
    conKey: 'VCTextCommon',
    title: '文字',
    category: 'Texts',
    categoryName: '文本',
    package: 'Informations',
    chartFrame: 'common',
    image: 'text_static.png'
  },
  Border02: {
    key: 'Border02',
    chartKey: 'VBorder02',
    conKey: 'VCBorder02',
    title: '边框-02',
    category: 'Borders',
    categoryName: '边框',
    package: 'Decorates',
    chartFrame: 'static',
    image: 'border02.png'
  }
}

export function makeComponent(metaKey, { id, attr, option, extra = {} }) {
  const meta = CHART_META[metaKey]
  return {
    id: id || randomUUID(),
    isGroup: false,
    attr: {
      x: 50,
      y: 50,
      w: 500,
      h: 300,
      offsetX: 0,
      offsetY: 0,
      zIndex: -1,
      ...attr
    },
    styles: { ...STYLES, ...(extra.styles || {}) },
    preview: { overFlowHidden: false },
    status: { lock: false, hide: false },
    request: JSON.parse(JSON.stringify(extra.request || REQUEST)),
    filter: extra.filter,
    events: JSON.parse(JSON.stringify(extra.events || EVENTS)),
    key: meta.key,
    chartConfig: { ...meta },
    option,
    ...(extra.interactActions ? { interactActions: extra.interactActions } : {})
  }
}

export function makeRailwayMapOption(dataset) {
  return {
    dataset,
    showToolbar: true,
    showLegend: false,
    showBaseLine: true,
    showDirectionArrows: true,
    showPointLabels: true,
    showHaloEffect: true,
    stationLabelScope: 'important',
    pointColorMode: 'contrast',
    intensityGamma: 0.65,
    showIntensityOnLabel: true,
    activeMetric: 'carbon',
    activeDirection: 'all',
    showHighOnly: false,
    lineDisplayMode: 'flow',
    pointBaseSize: 8,
    pointSizeScale: 10,
    haloSize: 7,
    haloOpacity: 0.45,
    radiationMinPx: 28,
    radiationMaxPx: 82,
    radiationInnerAlpha: 0.95,
    showLineHeatmap: false,
    showReferenceLines: true,
    heatmapGridStep: 0.05,
    heatmapInfluenceRadius: 0.07,
    heatmapPointSize: 12,
    heatmapBlurSize: 8,
    heatmapOpacity: 0.42,
    heatmapAllDirectionMode: 'max',
    lineWidth: 3,
    referenceLineWidth: 2.5,
    lineFlowSpeed: 40,
    lineFlowTrailLength: 0.88,
    lineFlowSymbolSize: 5,
    flowDwellEnabled: true,
    flowDwellDurationSec: 2,
    flowTourSpeedRatio: 1.2,
    lineFlowRippleScaleMin: 2.4,
    lineFlowRippleScaleMax: 5.2,
    lineFlowRipplePeriodMin: 3.2,
    lineFlowRipplePeriodMax: 5.2,
    lineBloomIntensity: 1.5,
    labelFontSize: 15,
    labelColor: '#ffffff',
    labelYOffset: -10,
    geoZoom: 1.30,
    enableRipple: true,
    enableLineFlowRipple: false,
    lineCurveness: 0.2,
    mapPaddingRatio: 0.06,
    mapPaddingRatioX: 0.18,
    mapLayoutSize: '90%'
  }
}

export function makeThreeSurfaceOption(dataset) {
  return {
    dataset,
    heightScale: 56,
    showWireframe: true,
    wireframeOpacity: 0.3,
    colorLow: '#0B5CFF',
    colorMidLow: '#00D4FF',
    colorMid: '#00FFB2',
    colorMidHigh: '#FFE600',
    colorHigh: '#FF3B30',
    backgroundColor: 'transparent',
    gridColor: '#00ffff',
    gridOpacity: 0.15,
    axisColor: '#2EC7FF',
    labelColor: '#ffffff',
    enableTooltip: true,
    tooltipTrigger: 'mixed',
    showColorLegend: false,
    enableIntroSpin: true,
    granularity: 'month'
  }
}

export function makeLineOption(dataset, series) {
  return {
    tooltip: { show: true, trigger: 'axis', axisPointer: { type: 'line' } },
    xAxis: { show: true, type: 'category' },
    yAxis: { show: true, type: 'value' },
    legend: { show: true, textStyle: { color: '#ccc' } },
    dataset,
    series
  }
}

export function makePieOption(dataset) {
  return {
    isCarousel: false,
    type: 'ring',
    tooltip: DASH_PIE_TOOLTIP,
    legend: {
      show: true,
      type: 'plain',
      orient: 'vertical',
      right: 8,
      top: 'middle',
      itemWidth: 8,
      itemHeight: 8,
      itemGap: 12,
      textStyle: { color: '#B9B8CE', fontSize: 11 }
    },
    dataset,
    series: [
      {
        type: 'pie',
        radius: ['38%', '54%'],
        center: ['36%', '54%'],
        roseType: false,
        avoidLabelOverlap: false,
        itemStyle: { show: true, borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
        ...DASH_PIE_SERIES_HOVER
      }
    ]
  }
}

export function makeBarLineOption(dataset, series) {
  return {
    tooltip: { show: true, trigger: 'axis', axisPointer: { show: true, type: 'shadow' } },
    legend: { data: null },
    xAxis: { show: true, type: 'category' },
    yAxis: { show: true, type: 'value' },
    dataset,
    series
  }
}

export function makeTabOption(dataset) {
  return {
    componentInteractEventKey: 'data',
    tabLabel: '排放源',
    tabType: 'segment',
    dataset
  }
}

export function makeNumberOption(value, { prefix, suffix, precision }) {
  return {
    dataset: value,
    from: 0,
    dur: 3,
    precision,
    showSeparator: true,
    numberSize: 28,
    numberColor: '#4ddbff',
    prefixText: prefix,
    prefixColor: '#8ec8e8',
    suffixText: suffix,
    suffixColor: '#8ec8e8'
  }
}

export function makeTextOption(text, fontSize, extra = {}) {
  return {
    link: '',
    linkHead: 'http://',
    dataset: text,
    fontSize,
    fontColor: '#e6f7ff',
    paddingX: 10,
    paddingY: 10,
    textAlign: 'center',
    fontWeight: 'normal',
    borderWidth: 0,
    borderColor: '#ffffff',
    borderRadius: 5,
    letterSpacing: 5,
    writingMode: 'horizontal-tb',
    backgroundColor: '#00000000',
    ...extra
  }
}

export function makeBorderOption() {
  return {
    colors: ['#6586ec', '#2cf7fe'],
    backgroundColor: '#00000000'
  }
}

export function makeTableOption(dataset) {
  return {
    header: ['站点', '碳排放', '强度'],
    dataset,
    index: true,
    columnWidth: [50, 120, 100, 80],
    align: ['center', 'right', 'right', 'right'],
    rowNum: 8,
    waitTime: 2,
    headerHeight: 35,
    carousel: 'single',
    headerBGC: '#00BAFF',
    oddRowBGC: '#003B51',
    evenRowBGC: '#0A2732'
  }
}

export const TAB_INTERACT_ACTIONS = [
  {
    interactType: 'change',
    interactName: '选择完成',
    componentEmitEvents: {
      data: [{ value: 'data', label: '选择项' }]
    }
  }
]
