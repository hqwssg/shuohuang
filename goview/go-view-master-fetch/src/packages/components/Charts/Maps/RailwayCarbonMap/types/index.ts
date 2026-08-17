// 核算点类型
export type AccountingPointType = 'station' | 'marshalling' | 'maintenance' | 'other'

// 核算点数据
export interface AccountingPoint {
  lng: number
  lat: number
  name: string
  type: AccountingPointType
  latestAccountingTime: string
  carbonIntensity: number
  carbonEmission: number
  unit: string
}

// 核算点集合
export interface AccountingPointsData {
  points: AccountingPoint[]
  statPeriod: string
}

// 线路区段属性
export interface LineSectionProperties {
  sectionName: string
  direction: 'up' | 'down' | 'both'
  sectionId: string
  energyIntensity: number
  carbonIntensity: number
  carbonEmission?: number
  statPeriod: string
  lengthKm: number
  unit?: string
  chainageStartKm?: number
  chainageEndKm?: number
}

// GeoJSON LineFeature
export interface LineFeature {
  type: 'Feature'
  geometry: {
    type: 'LineString'
    coordinates: [number, number][]
  }
  properties: LineSectionProperties
}

export type LineDisplayMode = 'flow' | 'monitorGradient'

export interface MonitoringPoint {
  lng: number
  lat: number
  fromStation: string
  toStation: string
  direction: 'up' | 'down'
  chainageKm: number
  carbonIntensity: number
  energyIntensity: number
}

export interface MonitoringPointsData {
  points: MonitoringPoint[]
  statPeriod: string
}

// 完整数据集
export interface RailwayCarbonMapData {
  points: AccountingPointsData
  lines: {
    type: 'FeatureCollection'
    features: LineFeature[]
  }
  monitoringPoints?: MonitoringPointsData
  meta: {
    lineName: string
    totalLengthKm: number
    sectionCount: number
    pointCount: number
    statPeriod: string
    updateTime: string
    stationOrder?: string[]
  }
}

export const FULL_STATION_ORDER = [
  '神池南站',
  '宁武西站',
  '龙宫站',
  '北大牛站',
  '原平南站',
  '回凤站',
  '东冶站',
  '南湾站',
  '滴流蹬站',
  '猴刎站',
  '小觉站',
  '古月站',
  '温塘站',
  '西柏坡站',
  '三汲站',
  '灵寿站',
  '行唐站',
  '新曲站',
  '定州西站',
  '定州东站',
  '安国站',
  '博野站',
  '蠡县站',
  '肃宁北站',
  '太师庄站',
  '河间站',
  '行别营站',
  '黎民居站',
  '杜生站',
  '沧州西站',
  '李天木站',
  '黄骅南站',
  '段庄站',
  '黄骅港站',
  '港口站'
]

export const DEFAULT_STATION_ORDER = FULL_STATION_ORDER

export type StationLabelScope = 'all' | 'important'

/** 朔黄线重要站（固定名单，仅控制标签显示） */
export const IMPORTANT_STATION_NAMES: readonly string[] = [
  '神池南站',
  '宁武西站',
  '原平南站',
  '西柏坡站',
  '定州西站',
  '肃宁北站',
  '沧州西站',
  '黄骅港站',
  '港口站'
]

/** 非重点站相对重点站的圆点尺寸缩放（仅圆点层） */
export const MINOR_STATION_VISUAL_SCALE = 0.6

// 组件配置选项
export interface RailwayCarbonMapOption {
  dataset: RailwayCarbonMapData

  // 显示控制 - 主要组件
  showToolbar: boolean
  showLegend: boolean
  showBaseLine: boolean

  // 显示控制 - 图层开关
  showDirectionArrows: boolean
  showPointLabels: boolean
  showHaloEffect: boolean
  /** 站点标签范围：important=仅重要站标签，all=全部站标签 */
  stationLabelScope: StationLabelScope

  // 当前状态
  activeMetric: 'energy' | 'carbon'
  activeDirection: 'all' | 'up' | 'down'
  showHighOnly: boolean
  lineDisplayMode: LineDisplayMode

  // 视觉配置 - 核算点
  pointBaseSize: number
  pointSizeScale: number
  haloSize: number
  haloOpacity: number
  radiationMinPx: number
  radiationMaxPx: number
  radiationInnerAlpha: number

  // 线路热力
  showLineHeatmap: boolean
  showReferenceLines: boolean
  heatmapGridStep: number
  heatmapInfluenceRadius: number
  heatmapPointSize: number
  heatmapBlurSize: number
  heatmapOpacity: number
  heatmapAllDirectionMode: 'max' | 'avg' | 'separate'

  // 视觉配置 - 线路
  lineWidth: number
  referenceLineWidth: number
  lineFlowSpeed: number
  lineFlowTrailLength: number
  lineFlowSymbolSize: number
  lineFlowRippleScaleMin?: number
  lineFlowRippleScaleMax?: number
  lineFlowRipplePeriodMin?: number
  lineFlowRipplePeriodMax?: number
  flowDwellEnabled?: boolean
  flowDwellDurationSec?: number
  flowTourSpeedRatio?: number
  lineBloomIntensity: number
  arrowMinZoom: number

  // 视觉配置 - 标签
  labelFontSize: number
  labelColor: string
  labelYOffset: number

  // 地图配置
  mapStyle: string
  center: [number, number]
  zoom: number
  minZoom: number
  maxZoom: number

  // ECharts Geo 配置
  geoZoom?: number
  enableRipple?: boolean
  enableLineFlowRipple?: boolean
  lineCurveness?: number
  mapPaddingRatio?: number
  mapPaddingRatioX?: number
  mapLayoutSize?: string | number

  // 核算点强度视觉
  pointColorMode?: 'contrast' | 'spectrum'
  intensityGamma?: number
  showIntensityOnLabel?: boolean
  radiationMinPx?: number
  radiationMaxPx?: number
  radiationInnerAlpha?: number
}

// Tooltip数据
export interface PointTooltipData {
  name: string
  type: AccountingPointType
  carbonIntensity: number
  carbonEmission: number
  latestAccountingTime: string
  statPeriod: string
  unit: string
}

export interface LineTooltipData {
  sectionName: string
  direction: 'up' | 'down' | 'both'
  lengthKm: number
  energyIntensity: number
  carbonIntensity: number
  statPeriod: string
}
