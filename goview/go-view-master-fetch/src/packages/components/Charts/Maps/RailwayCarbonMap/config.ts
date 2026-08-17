import { echartOptionProfixHandle, PublicConfigClass } from '@/packages/public'
import type { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { RailwayCarbonMapConfig } from './index'
import dataJson from './data.json'
import cloneDeep from 'lodash/cloneDeep'

export enum MetricTypeEnum {
  ENERGY = 'energy',
  CARBON = 'carbon'
}

export enum DirectionEnum {
  ALL = 'all',
  UP = 'up',
  DOWN = 'down'
}

export const option = {
  dataset: cloneDeep(dataJson),

  // 主要组件显示
  showToolbar: true,
  showLegend: false,
  showBaseLine: true,

  // 图层显示控制
  showDirectionArrows: true,
  showPointLabels: true,
  showHaloEffect: true,
  stationLabelScope: 'important' as const,

  // 核算点强度视觉
  pointColorMode: 'contrast' as const,
  intensityGamma: 0.65,
  showIntensityOnLabel: true,

  // 当前状态
  activeMetric: MetricTypeEnum.CARBON,
  activeDirection: DirectionEnum.ALL,
  showHighOnly: false,
  lineDisplayMode: 'flow' as const,

  // 核算点视觉
  pointBaseSize: 8,
  pointSizeScale: 10,
  haloSize: 7,
  haloOpacity: 0.45,
  radiationMinPx: 28,
  radiationMaxPx: 82,
  radiationInnerAlpha: 0.95,

  // 区域热力（方案 B，默认关；方案 A 为线段着色）
  showLineHeatmap: false,
  showReferenceLines: true,
  heatmapGridStep: 0.05,
  heatmapInfluenceRadius: 0.07,
  heatmapPointSize: 12,
  heatmapBlurSize: 8,
  heatmapOpacity: 0.42,
  heatmapAllDirectionMode: 'max' as const,

  // 线路视觉
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

  // 标签视觉
  labelFontSize: 15,
  labelColor: '#ffffff',
  labelYOffset: -10,

  // ECharts 专用配置
  geoZoom: 1.30,
  enableRipple: true,
  enableLineFlowRipple: false,
  lineCurveness: 0.2,
  mapPaddingRatio: 0.06,
  mapPaddingRatioX: 0.18,
  mapLayoutSize: '90%'
}

export const includes = [
  'geoZoom',
  'enableRipple',
  'enableLineFlowRipple',
  'lineCurveness',
  'mapPaddingRatio',
  'mapPaddingRatioX',
  'mapLayoutSize',
  'pointColorMode',
  'intensityGamma',
  'showIntensityOnLabel',
  'haloSize',
  'haloOpacity',
  'pointBaseSize',
  'radiationMinPx',
  'radiationMaxPx',
  'radiationInnerAlpha',
  'showLineHeatmap',
  'showReferenceLines',
  'heatmapGridStep',
  'heatmapInfluenceRadius',
  'heatmapPointSize',
  'heatmapBlurSize',
  'heatmapOpacity',
  'heatmapAllDirectionMode',
  'referenceLineWidth',
  'showDirectionArrows',
  'lineDisplayMode',
  'lineFlowSpeed',
  'lineFlowTrailLength',
  'lineFlowSymbolSize',
  'lineFlowRippleScaleMin',
  'lineFlowRippleScaleMax',
  'lineFlowRipplePeriodMin',
  'lineFlowRipplePeriodMax',
  'stationLabelScope'
]

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = RailwayCarbonMapConfig.key
  public attr = { ...chartInitConfig, w: 800, h: 600, zIndex: -1 }
  public chartConfig = cloneDeep(RailwayCarbonMapConfig)
  public option = echartOptionProfixHandle(option, includes)
}
