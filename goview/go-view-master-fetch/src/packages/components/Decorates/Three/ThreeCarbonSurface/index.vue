<template>
  <div class="carbon-surface-wrap" :style="{ width: `${w}px`, height: `${h}px` }">
    <div ref="chartRef" class="carbon-surface-canvas"></div>
    
    <!-- Tooltip：v-show 避免 v-if 频繁挂载/卸载，降低与编辑器壳层 patch 冲突概率 -->
    <div 
      v-show="tooltipVisible && tooltipData != null" 
      class="carbon-tooltip"
      :style="tooltipStyle"
    >
      <div class="tooltip-header">{{ tooltipData?.year }}年 {{ tooltipData?.time }}</div>
      <div class="tooltip-value">{{ formattedTooltipValue }}</div>
      <div v-if="tooltipMode === 'detail'" class="tooltip-detail">
        <div>占当年: {{ yearPercentage }}%</div>
        <div>占峰值: {{ globalPercentage }}%</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, PropType, ref, toRefs, watch } from 'vue'
import type { CreateComponentType } from '@/packages/index.d'
import { useChartDataFetch, useStationScopeSync } from '@/hooks'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { option } from './config'
import { CarbonSurface } from './core/CarbonSurface'
import type { CarbonSurfaceData, TooltipData } from './types'
import { normalizeCarbonSurfaceData } from './utils/normalizeCarbonData'
import throttle from 'lodash/throttle'
import cloneDeep from 'lodash/cloneDeep'
import isEqual from 'lodash/isEqual'
import { isPreview } from '@/utils'

const props = defineProps({
  chartConfig: {
    type: Object as PropType<CreateComponentType & typeof option>,
    required: true
  }
})

const chartRef = ref<HTMLElement>()
const { w, h } = toRefs(props.chartConfig.attr)
let surfaceInstance: CarbonSurface | null = null
/** 卸载后为 true，阻止 rAF/节流/射线回调再碰 Vue 状态 */
let isUnmounted = false

// Tooltip 状态
const tooltipVisible = ref(false)
const tooltipData = ref<TooltipData | null>(null)
const tooltipPosition = ref({ x: 0, y: 0 })
const tooltipMode = ref<'hover' | 'detail'>('hover')
const fallbackDataset = (): CarbonSurfaceData => cloneDeep(option.dataset) as CarbonSurfaceData

const formattedTooltipValue = computed(() => {
  if (!tooltipData.value) return ''
  const { value, unit } = tooltipData.value
  return `${value.toFixed(1)} ${unit}`
})

const yearPercentage = computed(() => {
  if (!tooltipData.value) return '0'
  const dataset = props.chartConfig.option.dataset as CarbonSurfaceData
  if (!dataset?.values) return '0'
  
  const yearIndex = dataset.years.indexOf(tooltipData.value.year)
  const yearValues = dataset.values[yearIndex] || []
  const yearTotal = yearValues.reduce((a, b) => a + b, 0)
  
  if (yearTotal <= 0) return '0'
  return ((tooltipData.value.value / yearTotal) * 100).toFixed(1)
})

const globalPercentage = computed(() => {
  if (!tooltipData.value) return '0'
  const max = surfaceInstance?.getMaxValue() || 1
  return ((tooltipData.value.value / max) * 100).toFixed(1)
})

const tooltipStyle = computed(() => ({
  left: `${tooltipPosition.value.x + 15}px`,
  top: `${tooltipPosition.value.y + 15}px`
}))

// 构建视觉选项
const buildVisualOptions = () => ({
  heightScale: props.chartConfig.option.heightScale ?? 56,
  showWireframe: props.chartConfig.option.showWireframe !== false,
  wireframeOpacity: props.chartConfig.option.wireframeOpacity ?? 0.3,
  colorLow: props.chartConfig.option.colorLow || '#0B5CFF',
  colorMidLow: props.chartConfig.option.colorMidLow || '#00D4FF',
  colorMid: props.chartConfig.option.colorMid || '#00FFB2',
  colorMidHigh: props.chartConfig.option.colorMidHigh || '#FFE600',
  colorHigh: props.chartConfig.option.colorHigh || '#FF3B30',
  backgroundColor: props.chartConfig.option.backgroundColor ?? 'transparent',
  gridColor: props.chartConfig.option.gridColor || '#00ffff',
  gridOpacity: props.chartConfig.option.gridOpacity ?? 0.15,
  axisColor: props.chartConfig.option.axisColor || '#2EC7FF',
  labelColor: props.chartConfig.option.labelColor || '#ffffff',
  enableTooltip: props.chartConfig.option.enableTooltip !== false,
  tooltipTrigger: props.chartConfig.option.tooltipTrigger || 'mixed',
  enableIntroSpin: props.chartConfig.option.enableIntroSpin !== false
})

// 初始化
const init = () => {
  const dom = chartRef.value
  if (!dom) return
  
  try {
    const ua = navigator.userAgent
    if (ua.indexOf('Chrome') < 0 && ua.indexOf('Edg') < 0) {
      window['$message']?.error('三维曲面组件建议在【Chrome / Edge】浏览器中展示')
    }
    
    const dataset = normalizeCarbonSurfaceData(props.chartConfig.option.dataset, fallbackDataset())
    if (!isEqual(dataset, props.chartConfig.option.dataset)) {
      props.chartConfig.option.dataset = dataset
    }

    surfaceInstance = new CarbonSurface({
      dom,
      data: dataset,
      width: w.value,
      height: h.value,
      options: buildVisualOptions()
    })
    
    // 设置回调
    surfaceInstance.setEventCallbacks(
      (data, position) => {
        if (isUnmounted) return
        void nextTick(() => {
          if (isUnmounted) return
          if (data) {
            tooltipData.value = data
            tooltipPosition.value = position
            tooltipVisible.value = true
            tooltipMode.value = 'hover'
          } else {
            tooltipVisible.value = false
          }
        })
      },
      (data) => {
        if (isUnmounted) return
        void nextTick(() => {
          if (isUnmounted) return
          tooltipData.value = data
          tooltipMode.value = 'detail'
          tooltipVisible.value = true
        })
      }
    )
  } catch (error) {
    console.error('CarbonSurface init error:', error)
  }
}

// 更新数据（避免反复 normalize 赋新引用 → watch(dataset) → updateData 递归）
const updateData = (data: unknown) => {
  if (isUnmounted || !surfaceInstance) return
  const normalized = normalizeCarbonSurfaceData(data, fallbackDataset())
  const current = props.chartConfig.option.dataset as CarbonSurfaceData
  if (isEqual(normalized, current)) {
    return
  }
  try {
    props.chartConfig.option.dataset = normalized
    surfaceInstance.updateData(normalized)
  } catch (error) {
    console.error('CarbonSurface updateData error:', error)
  }
}

// 监听粒度变化
watch(
  () => props.chartConfig.option.granularity,
  (granularity) => {
    if (isUnmounted) return
    const params = props.chartConfig.request.requestParams.Params || {}
    params.granularity = granularity
    props.chartConfig.request.requestParams.Params = params
  },
  { immediate: true }
)

// 监听视觉选项变化
watch(
  () => [
    props.chartConfig.option.heightScale,
    props.chartConfig.option.showWireframe,
    props.chartConfig.option.wireframeOpacity,
    props.chartConfig.option.colorLow,
    props.chartConfig.option.colorMidLow,
    props.chartConfig.option.colorMid,
    props.chartConfig.option.colorMidHigh,
    props.chartConfig.option.colorHigh,
    props.chartConfig.option.backgroundColor,
    props.chartConfig.option.gridColor,
    props.chartConfig.option.gridOpacity,
    props.chartConfig.option.axisColor,
    props.chartConfig.option.labelColor
  ],
  () => {
    if (isUnmounted) return
    surfaceInstance?.updateOptions(buildVisualOptions())
  }
)

const throttledResize = throttle((newWidth: number, newHeight: number) => {
  if (isUnmounted) return
  surfaceInstance?.updateSize(newWidth, newHeight)
}, 100)

// 监听尺寸变化
watch(
  () => [w.value, h.value],
  ([newWidth, newHeight]) => {
    throttledResize(newWidth, newHeight)
  }
)

// 监听数据变化
watch(
  () => props.chartConfig.option.dataset,
  (newData) => {
    if (isUnmounted || !newData) return
    updateData(newData)
  },
  { deep: false }
)

onMounted(() => {
  isUnmounted = false
  init()
})

onBeforeUnmount(() => {
  isUnmounted = true
  throttledResize.cancel()
  tooltipVisible.value = false
  tooltipData.value = null
  surfaceInstance?.dispose()
  surfaceInstance = null
})

useChartDataFetch(props.chartConfig, useChartEditStore, updateData)

if (isPreview()) {
  useStationScopeSync({
    chartEditStore: useChartEditStore(),
    kind: 'surface',
    apply: dataset => {
      updateData(dataset)
    },
    replayEntrance: () => {
      if (!surfaceInstance) return
      surfaceInstance.updateData(
        props.chartConfig.option.dataset as CarbonSurfaceData,
        { replayIntro: true }
      )
    }
  })
}
</script>

<style scoped>
.carbon-surface-wrap {
  position: relative;
  overflow: hidden;
  background: transparent;
}

.carbon-surface-canvas {
  position: relative;
  width: 100%;
  height: 100%;
}

.carbon-tooltip {
  position: absolute;
  background: rgba(10, 26, 42, 0.95);
  border: 1px solid #00ffff;
  border-radius: 4px;
  padding: 10px 14px;
  color: #ffffff;
  font-size: 12px;
  pointer-events: none;
  z-index: 1000;
  box-shadow: 0 4px 12px rgba(0, 255, 255, 0.2);
  min-width: 140px;
}

.tooltip-header {
  font-weight: bold;
  color: #4db8ff;
  margin-bottom: 6px;
  font-size: 13px;
}

.tooltip-value {
  font-size: 16px;
  color: #00ffff;
  margin-bottom: 4px;
}

.tooltip-detail {
  margin-top: 8px;
  padding-top: 6px;
  border-top: 1px solid rgba(0, 255, 255, 0.3);
  font-size: 11px;
  color: #aaaaaa;
}

.tooltip-detail > div {
  margin: 2px 0;
}

</style>

<style>
/* CSS2DObject 挂载在运行时 DOM，需全局类名 */
.carbon-axis-label {
  color: rgba(230, 247, 255, 0.9);
  font-size: 12px;
  line-height: 1;
  white-space: nowrap;
  text-shadow: 0 0 6px rgba(46, 199, 255, 0.75);
  pointer-events: none;
}

.carbon-axis-title {
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  text-shadow: 0 0 8px rgba(46, 199, 255, 0.95);
  pointer-events: none;
}
</style>
