<template>
  <div class="carbon-surface-config">
    <!-- 数据设置 -->
    <setting-item-box name="数据设置">
      <setting-item name="时间粒度">
        <n-select v-model:value="optionData.granularity" :options="timeTypeOptions" size="small" />
      </setting-item>
    </setting-item-box>

    <!-- 视觉设置 -->
    <setting-item-box name="视觉设置">
      <setting-item name="高度缩放">
        <n-slider v-model:value="optionData.heightScale" :min="50" :max="60" :step="1" />
      </setting-item>
      
      <setting-item name="显示线框">
        <n-switch v-model:value="optionData.showWireframe" />
      </setting-item>
      
      <setting-item v-if="optionData.showWireframe" name="线框透明度">
        <n-slider v-model:value="optionData.wireframeOpacity" :min="0" :max="1" :step="0.05" />
      </setting-item>
    </setting-item-box>

    <!-- 颜色设置 -->
    <setting-item-box name="热力颜色">
      <setting-item name="低值(蓝)">
        <n-color-picker v-model:value="optionData.colorLow" />
      </setting-item>
      <setting-item name="中低(青)">
        <n-color-picker v-model:value="optionData.colorMidLow" />
      </setting-item>
      <setting-item name="中(绿)">
        <n-color-picker v-model:value="optionData.colorMid" />
      </setting-item>
      <setting-item name="中高(黄)">
        <n-color-picker v-model:value="optionData.colorMidHigh" />
      </setting-item>
      <setting-item name="高值(红)">
        <n-color-picker v-model:value="optionData.colorHigh" />
      </setting-item>
    </setting-item-box>

    <!-- 背景设置 -->
    <setting-item-box name="背景与网格">
      <setting-item name="背景色">
        <n-color-picker v-model:value="optionData.backgroundColor" />
      </setting-item>
      <setting-item name="网格色">
        <n-color-picker v-model:value="optionData.gridColor" />
      </setting-item>
      <setting-item name="网格透明度">
        <n-slider v-model:value="optionData.gridOpacity" :min="0" :max="1" :step="0.05" />
      </setting-item>
    </setting-item-box>

    <!-- 坐标轴设置 -->
    <setting-item-box name="坐标轴">
      <setting-item name="轴线颜色">
        <n-color-picker v-model:value="optionData.axisColor" />
      </setting-item>
      <setting-item name="标签颜色">
        <n-color-picker v-model:value="optionData.labelColor" />
      </setting-item>
    </setting-item-box>

    <!-- 交互设置 -->
    <setting-item-box name="交互设置">
      <setting-item name="启用Tooltip">
        <n-switch v-model:value="optionData.enableTooltip" />
      </setting-item>
      <setting-item v-if="optionData.enableTooltip" name="触发方式">
        <n-select v-model:value="optionData.tooltipTrigger" :options="triggerOptions" size="small" />
      </setting-item>
      <setting-item name="进场旋转一周">
        <n-switch v-model:value="optionData.enableIntroSpin" />
      </setting-item>
    </setting-item-box>
  </div>
</template>

<script setup lang="ts">
import { PropType } from 'vue'
import { SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'
import { CarbonSurfaceTimeTypeEnum, option } from './config'

/** 与 ChartSetting 一致：父级传入 :optionData */
defineProps({
  optionData: {
    type: Object as PropType<typeof option>,
    required: true
  }
})

const timeTypeOptions = [
  { label: '月份', value: CarbonSurfaceTimeTypeEnum.MONTH },
  { label: '季度', value: CarbonSurfaceTimeTypeEnum.QUARTER }
]

const triggerOptions = [
  { label: '悬停', value: 'hover' },
  { label: '点击', value: 'click' },
  { label: '混合', value: 'mixed' }
]
</script>
