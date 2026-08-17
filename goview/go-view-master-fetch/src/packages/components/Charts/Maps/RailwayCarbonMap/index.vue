<template>
  <div class="railway-carbon-map-wrap" :style="wrapStyle">
    <!-- ECharts 容器 -->
    <div ref="chartContainer" class="chart-container"></div>

    <!-- 工具栏 (条件渲染 showToolbar) -->
    <div v-if="showToolbar" class="toolbar">
      <div class="toolbar-group">
        <button
          v-for="dir in directionOptions"
          :key="dir.value"
          class="toolbar-btn"
          :class="{ active: activeDirection === dir.value }"
          @click="setDirection(dir.value)"
        >
          {{ dir.label }}
        </button>
        <button
          class="toolbar-btn"
          :class="{ active: showHighOnly }"
          @click="toggleHighOnly"
        >
          高碳排放区段
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
// Vue核心
import { computed, nextTick, onBeforeUnmount, onMounted, PropType, ref, toRefs, watch } from 'vue'
import type { CreateComponentType } from '@/packages/index.d'
import { useChartDataFetch } from '@/hooks'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import cloneDeep from 'lodash/cloneDeep'
import isEqual from 'lodash/isEqual'

// 组件配置和类型
import { option, MetricTypeEnum, DirectionEnum } from './config'
import type { RailwayCarbonMapData, RailwayCarbonMapOption } from './types'
import { DEFAULT_STATION_ORDER } from './types'

// 核心管理器
import { EChartsManager } from './core/EChartsManager'
import { SeriesManager } from './core/SeriesManager'
import { FilterManager } from './core/FilterManager'
import { LineFlowRippleAnimator } from './core/LineFlowRippleAnimator'
import { FlowTourAnimator } from './core/FlowTourAnimator'
import { buildStationFittedFlowCoords } from './utils/buildStationFittedFlowPath'
import { buildFlowDwellTimeline } from './utils/flowDwellTimeline'

// 工具函数
import { getInvolvedProvinces } from './utils/getInvolvedProvinces'
import { buildCurvedStationLines } from './utils/buildCurvedStationLines'
import { buildMonitoringGradientLines } from './utils/buildMonitoringGradientLines'
import { buildMockMonitoringPoints } from './utils/buildMockMonitoringPoints'
import { isPreview } from '@/utils'
import { setPreviewStationScope } from '@/assets/dashboard/stationScopeController'

// Props定义
const props = defineProps({
  chartConfig: {
    type: Object as PropType<CreateComponentType & typeof option>,
    required: true
  }
})

// 从props解构w, h (组件宽高)
const { w, h } = toRefs(props.chartConfig.attr)

// ref定义chartContainer (ECharts DOM容器)
const chartContainer = ref<HTMLElement>()

// 管理器实例变量
let echartsManager: EChartsManager | null = null
let seriesManager: SeriesManager | null = null
let filterManager: FilterManager | null = null
let lineFlowRippleAnimator: LineFlowRippleAnimator | null = null
let flowTourAnimator: FlowTourAnimator | null = null

// 状态变量
let isUnmounted = false
const activeDirection = ref(props.chartConfig.option.activeDirection ?? DirectionEnum.ALL)
const showHighOnly = ref(props.chartConfig.option.showHighOnly ?? false)

const directionOptions = [
  { value: DirectionEnum.ALL, label: '全部' },
  { value: DirectionEnum.UP, label: '上行' },
  { value: DirectionEnum.DOWN, label: '下行' }
]

// 计算属性
const wrapStyle = computed(() => ({
  width: `${w.value}px`,
  height: `${h.value}px`
}))

const showToolbar = computed(() => props.chartConfig.option.showToolbar !== false)

const chartEditStore = useChartEditStore()

function onMapStationClick(params: { seriesType?: string; name?: string; data?: { name?: string } }) {
  if (params?.seriesType !== 'scatter' && params?.seriesType !== 'effectScatter') return
  const name = params?.name || params?.data?.name
  if (!name) return
  setPreviewStationScope(chartEditStore, 'station', name)
}

function bindMapStationClick() {
  const chart = echartsManager?.getChart()
  if (!chart || !isPreview()) return
  chart.off('click', onMapStationClick)
  chart.on('click', onMapStationClick)
}

// 构建选项对象
function buildOptions() {
  return {
    activeMetric: props.chartConfig.option.activeMetric ?? MetricTypeEnum.CARBON,
    activeDirection: activeDirection.value,
    showHighOnly: showHighOnly.value,
    lineDisplayMode: props.chartConfig.option.lineDisplayMode ?? 'flow',
    stationLabelScope: props.chartConfig.option.stationLabelScope ?? 'important',
    showLegend: props.chartConfig.option.showLegend ?? false,
    showBaseLine: props.chartConfig.option.showBaseLine ?? true,
    showDirectionArrows: props.chartConfig.option.showDirectionArrows ?? true,
    lineFlowSpeed: props.chartConfig.option.lineFlowSpeed ?? 40,
    lineFlowTrailLength: props.chartConfig.option.lineFlowTrailLength ?? 0.88,
    lineFlowSymbolSize: props.chartConfig.option.lineFlowSymbolSize ?? 5,
    flowDwellEnabled: props.chartConfig.option.flowDwellEnabled ?? true,
    flowDwellDurationSec: props.chartConfig.option.flowDwellDurationSec ?? 2,
    flowTourSpeedRatio: props.chartConfig.option.flowTourSpeedRatio ?? 1.2,
    lineFlowRippleScaleMin: props.chartConfig.option.lineFlowRippleScaleMin ?? 2.4,
    lineFlowRippleScaleMax: props.chartConfig.option.lineFlowRippleScaleMax ?? 5.2,
    lineFlowRipplePeriodMin: props.chartConfig.option.lineFlowRipplePeriodMin ?? 3.2,
    lineFlowRipplePeriodMax: props.chartConfig.option.lineFlowRipplePeriodMax ?? 5.2,
    showPointLabels: props.chartConfig.option.showPointLabels ?? true,
    showHaloEffect: props.chartConfig.option.showHaloEffect ?? true,
    pointBaseSize: props.chartConfig.option.pointBaseSize ?? 8,
    lineWidth: props.chartConfig.option.lineWidth ?? 3,
    referenceLineWidth: props.chartConfig.option.referenceLineWidth ?? 2.5,
    radiationMinPx: props.chartConfig.option.radiationMinPx ?? 28,
    radiationMaxPx: props.chartConfig.option.radiationMaxPx ?? 82,
    radiationInnerAlpha: props.chartConfig.option.radiationInnerAlpha ?? 0.95,
    labelFontSize: props.chartConfig.option.labelFontSize ?? 15,
    labelColor: props.chartConfig.option.labelColor ?? '#ffffff',
    labelYOffset: props.chartConfig.option.labelYOffset ?? -10,
    geoZoom: props.chartConfig.option.geoZoom ?? 1.30,
    enableRipple: props.chartConfig.option.enableRipple ?? true,
    enableLineFlowRipple: props.chartConfig.option.enableLineFlowRipple ?? false,
    lineCurveness: props.chartConfig.option.lineCurveness ?? 0.2,
    mapPaddingRatio: props.chartConfig.option.mapPaddingRatio ?? 0.06,
    mapPaddingRatioX: props.chartConfig.option.mapPaddingRatioX ?? 0.18,
    mapLayoutSize: props.chartConfig.option.mapLayoutSize ?? '90%',
    pointColorMode: props.chartConfig.option.pointColorMode ?? 'contrast',
    intensityGamma: props.chartConfig.option.intensityGamma ?? 0.65,
    showIntensityOnLabel: props.chartConfig.option.showIntensityOnLabel ?? true,
    showLineHeatmap: props.chartConfig.option.showLineHeatmap ?? false,
    showReferenceLines: props.chartConfig.option.showReferenceLines ?? true,
    heatmapGridStep: props.chartConfig.option.heatmapGridStep ?? 0.05,
    heatmapInfluenceRadius: props.chartConfig.option.heatmapInfluenceRadius ?? 0.07,
    heatmapPointSize: props.chartConfig.option.heatmapPointSize ?? 12,
    heatmapBlurSize: props.chartConfig.option.heatmapBlurSize ?? 8,
    heatmapOpacity: props.chartConfig.option.heatmapOpacity ?? 0.42,
    heatmapAllDirectionMode: props.chartConfig.option.heatmapAllDirectionMode ?? 'max'
  }
}

// 获取数据
function getDataset(): RailwayCarbonMapData {
  const dataset = props.chartConfig.option.dataset
  if (!dataset || !dataset.points || !dataset.lines) {
    return cloneDeep(option.dataset) as RailwayCarbonMapData
  }
  return dataset as RailwayCarbonMapData
}

// 初始化图表
async function initChart(): Promise<void> {
  if (!chartContainer.value || isUnmounted) return

  try {
    // 1. 创建 EChartsManager
    echartsManager = new EChartsManager(chartContainer.value)
    echartsManager.init()

    // 2. 获取数据集
    const dataset = getDataset()

    // 3. 根据站点坐标判断涉及省份
    const involvedProvinces = getInvolvedProvinces(dataset.points.points)

    // 4. 注册合并省份 GeoJSON
    await echartsManager.registerMergedRegion(involvedProvinces)

    // 5. 创建 SeriesManager 和 FilterManager
    seriesManager = new SeriesManager()
    filterManager = new FilterManager()

    // 6. 更新图表
    await updateChart()

    // 7. 监听窗口大小变化
    window.addEventListener('resize', handleResize)
  } catch (error) {
    console.error('RailwayCarbonMap init error:', error)
  }
}

// 更新图表
async function updateChart(): Promise<void> {
  if (!echartsManager || !seriesManager || !filterManager) return

  lineFlowRippleAnimator?.stop()
  flowTourAnimator?.stop()

  const dataset = getDataset()
  const chartOptions = buildOptions() as RailwayCarbonMapOption
  const stationOrder = dataset.meta.stationOrder ?? DEFAULT_STATION_ORDER
  const displayPoints = dataset.points.points
  const monitoringRaw =
    dataset.monitoringPoints?.points ??
    buildMockMonitoringPoints(displayPoints, dataset.lines.features, stationOrder)
  const statPeriod =
    dataset.monitoringPoints?.statPeriod ?? dataset.points.statPeriod ?? ''

  const lineSource =
    chartOptions.lineDisplayMode === 'monitorGradient'
      ? buildMonitoringGradientLines(
          monitoringRaw,
          chartOptions.activeMetric,
          statPeriod,
          chartOptions.activeDirection
        )
      : buildCurvedStationLines(displayPoints, stationOrder, statPeriod)

  const filtered = filterManager.applyFilters(
    lineSource,
    displayPoints,
    chartOptions,
    monitoringRaw
  )

  const involvedProvinces = getInvolvedProvinces(filtered.points)

  await echartsManager.registerMergedRegion(involvedProvinces)

  seriesManager.setDataAndOptions(filtered, chartOptions)

  const chartOption = seriesManager.generateOption()
  const chart = echartsManager.getChart()
  chart?.setOption({ graphic: { elements: [] } }, { replaceMerge: ['graphic'] })
  echartsManager.setOption(chartOption, true)
  bindMapStationClick()

  if (chartOptions.lineDisplayMode === 'flow' && chart && seriesManager.shouldAnimateFlow()) {
    const tracks = seriesManager.getLineFlowTracks(filtered.lines)
    if (tracks.length > 0) {
      if (seriesManager.shouldUseFlowTour()) {
        const tourOpts = seriesManager.getFlowTourOptions()
        const stationCoords = buildStationFittedFlowCoords(displayPoints, stationOrder)
        const timeline = buildFlowDwellTimeline({
          flowCoords: tracks[0].flowCoords,
          stationCoords,
          stationOrder,
          lineFlowSpeed: tourOpts.lineFlowSpeed,
          dwellSec: tourOpts.flowDwellDurationSec,
          speedRatio: tourOpts.flowTourSpeedRatio
        })
        const pointTip = seriesManager.getCorePointTipTarget(chart)
        if (timeline.segments.length > 0 && pointTip) {
          if (!flowTourAnimator) {
            flowTourAnimator = new FlowTourAnimator()
          }
          flowTourAnimator.start(chart, tracks, timeline, pointTip, {
            showRippleRings: true,
            showFlowDot: true,
            lineFlowSymbolSize: chartOptions.lineFlowSymbolSize ?? 5,
            onDwellEnter: (stationName) => {
              if (!isPreview()) return
              setPreviewStationScope(chartEditStore, 'station', stationName)
            }
          })
        }
      } else if (seriesManager.shouldAnimateLineFlowRipple()) {
        if (!lineFlowRippleAnimator) {
          lineFlowRippleAnimator = new LineFlowRippleAnimator()
        }
        lineFlowRippleAnimator.start(chart, tracks, chartOptions.lineFlowSpeed ?? 40, {
          showRippleRings: true
        })
      }
    }
  }
}

// 窗口大小变化处理
function handleResize(): void {
  echartsManager?.resize()
}

// 设置方向
function setDirection(direction: string): void {
  if (isUnmounted) return
  activeDirection.value = direction as 'all' | 'up' | 'down'
  props.chartConfig.option.activeDirection = direction as 'all' | 'up' | 'down'
  updateChart()
}

function toggleHighOnly(): void {
  if (isUnmounted) return
  showHighOnly.value = !showHighOnly.value
  props.chartConfig.option.showHighOnly = showHighOnly.value
  updateChart()
}

// 更新数据
function updateData(data: unknown): void {
  if (isUnmounted || !echartsManager) return

  const newDataset = data as RailwayCarbonMapData
  if (!newDataset || !newDataset.points || !newDataset.lines) {
    console.warn('RailwayCarbonMap: invalid data format')
    return
  }

  // 更新图表数据
  updateChart()
}

// 监听数据变化
watch(
  () => props.chartConfig.option.dataset,
  (newData) => {
    if (isUnmounted || !newData) return
    const current = getDataset()
    if (isEqual(newData, current)) return
    updateData(newData)
  },
  { deep: false }
)

// 监听showHighOnly变化（外部改 config 时同步）
watch(
  () => props.chartConfig.option.showHighOnly,
  (val) => {
    if (isUnmounted) return
    if (showHighOnly.value !== val) {
      showHighOnly.value = val ?? false
      updateChart()
    }
  }
)

// 监听组件宽高变化，触发 ECharts resize
watch([w, h], () => {
  if (isUnmounted) return
  nextTick(() => {
    echartsManager?.resize()
  })
})

// 生命周期
onMounted(() => {
  isUnmounted = false
  void initChart()
})

onBeforeUnmount(() => {
  isUnmounted = true

  // 移除事件监听
  window.removeEventListener('resize', handleResize)

  lineFlowRippleAnimator?.stop()
  lineFlowRippleAnimator = null
  flowTourAnimator?.stop()
  flowTourAnimator = null

  const chart = echartsManager?.getChart()
  chart?.off('click', onMapStationClick)

  // 销毁管理器
  echartsManager?.dispose()
  echartsManager = null
  seriesManager = null
  filterManager = null
})

// 使用数据获取hook
useChartDataFetch(props.chartConfig, useChartEditStore, (dataset) => {
  if (!isUnmounted) {
    props.chartConfig.option.dataset = cloneDeep(dataset)
    updateChart()
  }
})
</script>

<style scoped>
/* 容器样式 */
.railway-carbon-map-wrap {
  position: relative;
  overflow: hidden;
  background: transparent;
}

.chart-container {
  width: 100%;
  height: 100%;
}

/* 工具栏样式 */
.toolbar {
  position: absolute;
  top: 12px;
  left: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  z-index: 100;
}

.toolbar-group {
  display: flex;
  gap: 6px;
  padding: 8px 10px;
  background: rgba(2, 12, 24, 0.75);
  border: 1px solid rgba(46, 199, 255, 0.35);
  border-radius: 6px;
  backdrop-filter: blur(4px);
}

.toolbar-btn {
  padding: 6px 12px;
  font-size: 12px;
  color: rgba(230, 247, 255, 0.85);
  background: rgba(10, 30, 50, 0.6);
  border: 1px solid rgba(46, 199, 255, 0.25);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.toolbar-btn:hover {
  background: rgba(46, 199, 255, 0.2);
  border-color: rgba(46, 199, 255, 0.5);
}

.toolbar-btn.active {
  color: #ffffff;
  background: rgba(46, 199, 255, 0.35);
  border-color: rgba(46, 199, 255, 0.8);
  box-shadow: 0 0 8px rgba(46, 199, 255, 0.4);
}
</style>
