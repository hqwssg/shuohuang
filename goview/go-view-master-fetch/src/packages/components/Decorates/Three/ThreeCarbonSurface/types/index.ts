import * as THREE from 'three'

// 数据接口
export interface CarbonSurfaceData {
  timeType: 'month' | 'quarter'
  years: number[]
  times: string[]
  values: number[][]
  unit?: string
}

// 视觉配置选项
export interface VisualOptions {
  heightScale: number
  showWireframe: boolean
  wireframeOpacity: number
  colorLow: string
  colorMidLow: string
  colorMid: string
  colorMidHigh: string
  colorHigh: string
  backgroundColor: string
  gridColor: string
  gridOpacity: number
  axisColor: string
  labelColor: string
  enableTooltip: boolean
  tooltipTrigger: 'hover' | 'click' | 'mixed'
  enableIntroSpin?: boolean
}

// 热力颜色调色板（对应 t = 0, 0.25, 0.5, 0.75, 1）
export interface HeatmapPalette {
  low: THREE.Color
  midLow: THREE.Color
  mid: THREE.Color
  midHigh: THREE.Color
  high: THREE.Color
}

// Tooltip 数据
export interface TooltipData {
  year: number
  time: string
  value: number
  unit: string
  normalizedValue: number
}

// 布局计算结果（世界包围盒 + 网格顶高）
export interface LayoutInfo {
  minX: number
  maxX: number
  minZ: number
  maxZ: number
  width: number
  depth: number
  worldMaxY: number
  labelOffset: number
  timeCount: number
  yearCount: number
}

// 组件构造参数
export interface CarbonSurfaceParams {
  dom: HTMLElement
  data: CarbonSurfaceData
  width: number
  height: number
  options?: Partial<VisualOptions>
}

// 默认主题常量
export const DEFAULT_THEME = {
  background: 'transparent',
  heatmap: {
    low: '#0B5CFF',
    midLow: '#00D4FF',
    mid: '#00FFB2',
    midHigh: '#FFE600',
    high: '#FF3B30'
  },
  grid: {
    color: '#00ffff',
    opacity: 0.15
  },
  axis: '#2EC7FF',
  label: '#ffffff'
}

// 默认视觉选项
export const DEFAULT_VISUAL_OPTIONS: VisualOptions = {
  heightScale: 56,
  showWireframe: true,
  wireframeOpacity: 0.3,
  colorLow: DEFAULT_THEME.heatmap.low,
  colorMidLow: DEFAULT_THEME.heatmap.midLow,
  colorMid: DEFAULT_THEME.heatmap.mid,
  colorMidHigh: DEFAULT_THEME.heatmap.midHigh,
  colorHigh: DEFAULT_THEME.heatmap.high,
  backgroundColor: DEFAULT_THEME.background,
  gridColor: DEFAULT_THEME.grid.color,
  gridOpacity: DEFAULT_THEME.grid.opacity,
  axisColor: DEFAULT_THEME.axis,
  labelColor: DEFAULT_THEME.label,
  enableTooltip: true,
  tooltipTrigger: 'mixed'
}
