<template>
  <div class="go-text-box">
    <div class="content" :class="{ 'scope-active': isOverallActive }">
      <span style="cursor: pointer; white-space: pre-wrap" v-if="link || stationScopeAction" @click="onContentClick">{{ displayText }}</span>
      <span style="white-space: pre-wrap" v-else>{{ displayText }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { PropType, toRefs, shallowReactive, watch, computed } from 'vue'
import { CreateComponentType } from '@/packages/index.d'
import { useChartDataFetch } from '@/hooks'
import { useChartEditStore } from '@/store/modules/chartEditStore/chartEditStore'
import { setPreviewStationScope } from '@/assets/dashboard/stationScopeController'
import { STAT_PERIOD_LABEL } from '@/assets/dashboard/carbon-dashboard-mock'
import { isPreview } from '@/utils'
import { option as configOption } from './config'

const props = defineProps({
  chartConfig: {
    type: Object as PropType<CreateComponentType & typeof option>,
    required: true
  }
})

const {
  linkHead,
  link,
  fontColor,
  fontSize,
  letterSpacing,
  paddingY,
  paddingX,
  textAlign,
  borderWidth,
  borderColor,
  borderRadius,
  writingMode,
  backgroundColor,
  fontWeight,
  stationScopeAction,
  highlightWhenOverall
} = toRefs(props.chartConfig.option)

const option = shallowReactive({
  dataset: configOption.dataset
})

// 手动更新
watch(
  () => props.chartConfig.option.dataset,
  (newData: any) => {
    option.dataset = newData
  },
  {
    immediate: true,
    deep: false
  }
)

// 预览更新
useChartDataFetch(props.chartConfig, useChartEditStore, (newData: string) => {
  option.dataset = newData
})

//打开链接
const click = () => {
  window.open(linkHead.value + link.value)
}

const chartEditStore = useChartEditStore()

const displayText = computed(() => {
  if (props.chartConfig.id === 'dash-period' && isPreview()) {
    return STAT_PERIOD_LABEL
  }
  return option.dataset
})

const isOverallActive = computed(() => {
  if (!highlightWhenOverall?.value) return false
  if (!isPreview()) return false
  return chartEditStore.requestGlobalConfig.requestParams.Params.dataScope !== 'station'
})

const onContentClick = () => {
  if (stationScopeAction?.value === 'overall' && isPreview()) {
    setPreviewStationScope(chartEditStore, 'overall')
    return
  }
  if (link.value) click()
}
</script>

<style lang="scss" scoped>
@include go('text-box') {
  display: flex;
  align-items: center;
  justify-content: v-bind('textAlign');
  overflow: hidden;

  .content {
    color: v-bind('fontColor');
    padding: v-bind('`${paddingY}px ${paddingX}px`');
    font-size: v-bind('fontSize + "px"');
    letter-spacing: v-bind('letterSpacing + "px"');
    writing-mode: v-bind('writingMode');
    font-weight: v-bind('fontWeight');
    border-style: solid;
    border-width: v-bind('borderWidth + "px"');
    border-radius: v-bind('borderRadius + "px"');
    border-color: v-bind('borderColor');

    background-color: v-bind('backgroundColor');
  }

  .content.scope-active {
    border-color: #2ec7ff;
    box-shadow: 0 0 8px rgba(46, 199, 255, 0.45);
  }
}
</style>
