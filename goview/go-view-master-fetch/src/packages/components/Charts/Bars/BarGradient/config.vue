<template>
  <!-- Echarts 全局设置 -->
  <global-setting :optionData="optionData"></global-setting>
  <CollapseItem name="渐变配置" :expanded="true">
    <SettingItemBox name="方向" :alone="true">
      <SettingItem>
        <n-select v-model:value="direction" size="small" :options="directionOptions" />
      </SettingItem>
    </SettingItemBox>
  </CollapseItem>

  <CollapseItem v-for="(item, index) in seriesList" :key="index" :name="`柱状图-${index + 1}`" :expanded="true">
    <SettingItemBox name="图形">
      <SettingItem name="宽度">
        <n-input-number v-model:value="item.barWidth" :min="1" :max="100" size="small" placeholder="自动计算" />
      </SettingItem>
      <SettingItem name="圆角">
        <n-input-number v-model:value="item.itemStyle.borderRadius" :min="0" size="small" />
      </SettingItem>
    </SettingItemBox>

    <SettingItemBox name="渐变色">
      <SettingItem name="起始色">
        <n-color-picker
          size="small"
          :modes="['hex']"
          :value="getStartColor(index)"
          @update:value="(v) => setStartColor(index, v)"
        />
      </SettingItem>
      <SettingItem name="结束色">
        <n-color-picker
          size="small"
          :modes="['hex']"
          :value="getEndColor(index)"
          @update:value="(v) => setEndColor(index, v)"
        />
      </SettingItem>
    </SettingItemBox>
  </CollapseItem>
</template>

<script setup lang="ts">
import { PropType, computed } from 'vue'
import { GlobalThemeJsonType } from '@/settings/chartThemes/index'
import { GlobalSetting, CollapseItem, SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'

type DirectionType = 'vertical' | 'horizontal'

const props = defineProps({
  optionData: {
    type: Object as PropType<GlobalThemeJsonType>,
    required: true
  }
})

const directionOptions = [
  { label: '纵向（上→下）', value: 'vertical' },
  { label: '横向（左→右）', value: 'horizontal' }
]

const seriesList = computed(() => {
  return (props.optionData as any).series || []
})

const direction = computed<DirectionType>({
  get() {
    const series = seriesList.value
    const color = series?.[0]?.itemStyle?.color
    if (color && typeof color === 'object') {
      return color.x2 === 1 ? 'horizontal' : 'vertical'
    }
    return 'vertical'
  },
  set(value) {
    seriesList.value.forEach((s: any) => {
      if (!s.itemStyle) s.itemStyle = {}
      if (!s.itemStyle.color || typeof s.itemStyle.color !== 'object') {
        s.itemStyle.color = {
          type: 'linear',
          x: 0,
          y: 0,
          x2: 0,
          y2: 1,
          colorStops: [
            { offset: 0, color: '#00E5FF' },
            { offset: 1, color: '#0050FF' }
          ]
        }
      }
      if (value === 'horizontal') {
        s.itemStyle.color.x = 0
        s.itemStyle.color.y = 0
        s.itemStyle.color.x2 = 1
        s.itemStyle.color.y2 = 0
      } else {
        s.itemStyle.color.x = 0
        s.itemStyle.color.y = 0
        s.itemStyle.color.x2 = 0
        s.itemStyle.color.y2 = 1
      }
    })
  }
})

const ensureColorStops = (s: any) => {
  if (!s.itemStyle) s.itemStyle = {}
  if (!s.itemStyle.color || typeof s.itemStyle.color !== 'object') {
    s.itemStyle.color = {
      type: 'linear',
      x: 0,
      y: 0,
      x2: 0,
      y2: 1,
      colorStops: [
        { offset: 0, color: '#00E5FF' },
        { offset: 1, color: '#0050FF' }
      ]
    }
  }
  if (!Array.isArray(s.itemStyle.color.colorStops) || s.itemStyle.color.colorStops.length < 2) {
    s.itemStyle.color.colorStops = [
      { offset: 0, color: '#00E5FF' },
      { offset: 1, color: '#0050FF' }
    ]
  }
}

const getStartColor = (index: number) => {
  const s = seriesList.value[index]
  if (!s) return '#00E5FF'
  ensureColorStops(s)
  return s.itemStyle.color.colorStops[0].color
}

const setStartColor = (index: number, value: string) => {
  const s = seriesList.value[index]
  if (!s) return
  ensureColorStops(s)
  s.itemStyle.color.colorStops[0].color = value
}

const getEndColor = (index: number) => {
  const s = seriesList.value[index]
  if (!s) return '#0050FF'
  ensureColorStops(s)
  return s.itemStyle.color.colorStops[1].color
}

const setEndColor = (index: number, value: string) => {
  const s = seriesList.value[index]
  if (!s) return
  ensureColorStops(s)
  s.itemStyle.color.colorStops[1].color = value
}
</script>

