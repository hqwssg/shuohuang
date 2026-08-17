import * as echarts from 'echarts/core'
import {
  MapChart,
  EffectScatterChart,
  LinesChart,
  ScatterChart,
  HeatmapChart
} from 'echarts/charts'
import { TooltipComponent, GeoComponent, VisualMapComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { use, registerMap } from 'echarts/core'
import { mergeProvinceGeoJSON, MERGED_MAP_NAME } from '../utils/mergeProvinceGeoJSON'

// 注册必要的组件
use([
  MapChart,
  EffectScatterChart,
  ScatterChart,
  HeatmapChart,
  LinesChart,
  TooltipComponent,
  GeoComponent,
  VisualMapComponent,
  CanvasRenderer
])

export class EChartsManager {
  private container: HTMLElement
  private chart: echarts.ECharts | null = null
  private registeredMaps: Set<string> = new Set()
  private lastMergedKey: string | null = null

  constructor(container: HTMLElement) {
    this.container = container
  }

  /**
   * 初始化 ECharts 实例
   */
  init(): echarts.ECharts {
    this.chart = echarts.init(this.container, undefined, {
      renderer: 'canvas'
    })
    return this.chart
  }

  /**
   * 获取 ECharts 实例
   */
  getChart(): echarts.ECharts | null {
    return this.chart
  }

  /**
   * 加载并注册省份 GeoJSON
   */
  async registerProvince(provinceCode: string): Promise<void> {
    if (this.registeredMaps.has(provinceCode)) return

    try {
      const geoJSON = await import(`../assets/provinces/${provinceCode}.json`)
      registerMap(provinceCode, geoJSON.default)
      this.registeredMaps.add(provinceCode)
    } catch (error) {
      console.error(`Failed to load province ${provinceCode}:`, error)
    }
  }

  /**
   * 注册多个省份
   */
  async registerProvinces(codes: string[]): Promise<void> {
    await Promise.all(codes.map(code => this.registerProvince(code)))
  }

  /**
   * 合并涉及省份并注册为单一地图 railwayRegion
   */
  async registerMergedRegion(codes: string[]): Promise<void> {
    const sortedKey = [...new Set(codes)].sort().join(',')
    if (this.lastMergedKey === sortedKey) return

    try {
      const mergedGeoJSON = await mergeProvinceGeoJSON(codes)
      registerMap(MERGED_MAP_NAME, mergedGeoJSON)
      this.lastMergedKey = sortedKey
      this.registeredMaps.add(MERGED_MAP_NAME)
    } catch (error) {
      console.error('Failed to register merged region map:', error)
    }
  }

  /**
   * 设置 ECharts 配置项
   */
  setOption(option: echarts.EChartsCoreOption, notMerge?: boolean): void {
    if (!this.chart) return
    this.chart.setOption(option, notMerge)
  }

  /**
   * 调整大小
   */
  resize(): void {
    this.chart?.resize()
  }

  /**
   * 销毁实例
   */
  dispose(): void {
    this.chart?.dispose()
    this.chart = null
    this.registeredMaps.clear()
    this.lastMergedKey = null
  }
}
