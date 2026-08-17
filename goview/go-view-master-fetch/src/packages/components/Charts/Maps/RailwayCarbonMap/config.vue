<template>
  <div class="railway-carbon-map-config">
    <!-- 数据设置 -->
    <setting-item-box name="数据设置">
      <setting-item name="默认指标">
        <n-select v-model:value="optionData.activeMetric" :options="metricOptions" size="small" />
      </setting-item>
      <setting-item name="默认方向">
        <n-select v-model:value="optionData.activeDirection" :options="directionOptions" size="small" />
      </setting-item>
    </setting-item-box>

    <!-- 显示控制 -->
    <setting-item-box name="显示控制">
      <setting-item name="显示工具栏">
        <n-switch v-model:value="optionData.showToolbar" />
      </setting-item>
      <setting-item name="显示图例">
        <n-switch v-model:value="optionData.showLegend" />
      </setting-item>
      <setting-item name="显示基础底线">
        <n-switch v-model:value="optionData.showBaseLine" />
      </setting-item>
      <setting-item name="显示核算点标签">
        <n-switch v-model:value="optionData.showPointLabels" />
      </setting-item>
      <setting-item name="站点标签范围">
        <n-select
          v-model:value="optionData.stationLabelScope"
          :options="stationLabelScopeOptions"
          size="small"
        />
      </setting-item>
      <setting-item name="显示强度辐射">
        <n-switch v-model:value="optionData.showHaloEffect" />
      </setting-item>
      <setting-item name="显示线路流动点（上下行）">
        <n-switch v-model:value="optionData.showDirectionArrows" />
      </setting-item>
      <setting-item name="线路表示模式">
        <n-select
          v-model:value="optionData.lineDisplayMode"
          :options="lineDisplayModeOptions"
          size="small"
        />
      </setting-item>
    </setting-item-box>

    <!-- 核算点视觉 -->
    <setting-item-box name="核算点视觉">
      <setting-item name="圆点大小（固定）">
        <n-slider v-model:value="optionData.pointBaseSize" :min="4" :max="16" :step="1" />
      </setting-item>
      <setting-item name="色阶拉伸">
        <n-slider v-model:value="optionData.intensityGamma" :min="0.4" :max="1" :step="0.05" />
      </setting-item>
      <setting-item name="标签显示强度值">
        <n-switch v-model:value="optionData.showIntensityOnLabel" />
      </setting-item>
      <setting-item name="辐射最小半径" v-if="optionData.showHaloEffect">
        <n-slider v-model:value="optionData.radiationMinPx" :min="24" :max="64" :step="1" />
      </setting-item>
      <setting-item name="辐射最大半径" v-if="optionData.showHaloEffect">
        <n-slider v-model:value="optionData.radiationMaxPx" :min="72" :max="150" :step="2" />
      </setting-item>
      <setting-item name="核算点涟漪" v-if="optionData.showHaloEffect">
        <n-switch v-model:value="optionData.enableRipple" />
      </setting-item>
      <setting-item name="辐射中心不透明度" v-if="optionData.showHaloEffect">
        <n-slider v-model:value="optionData.radiationInnerAlpha" :min="0.4" :max="1" :step="0.05" />
      </setting-item>
    </setting-item-box>

    <!-- 区域热力（方案 B，可选） -->
    <setting-item-box name="区域热力（可选）">
      <setting-item name="显示区域热力">
        <n-switch v-model:value="optionData.showLineHeatmap" />
      </setting-item>
      <setting-item name="显示参照线" v-if="optionData.showLineHeatmap">
        <n-switch v-model:value="optionData.showReferenceLines" />
      </setting-item>
      <setting-item name="栅格步长" v-if="optionData.showLineHeatmap">
        <n-slider v-model:value="optionData.heatmapGridStep" :min="0.03" :max="0.12" :step="0.01" />
      </setting-item>
      <setting-item name="走廊半径" v-if="optionData.showLineHeatmap">
        <n-slider v-model:value="optionData.heatmapInfluenceRadius" :min="0.04" :max="0.14" :step="0.01" />
      </setting-item>
      <setting-item name="热力扩散" v-if="optionData.showLineHeatmap">
        <n-slider v-model:value="optionData.heatmapPointSize" :min="6" :max="20" :step="1" />
      </setting-item>
      <setting-item name="热力模糊" v-if="optionData.showLineHeatmap">
        <n-slider v-model:value="optionData.heatmapBlurSize" :min="4" :max="16" :step="1" />
      </setting-item>
      <setting-item name="热力透明度" v-if="optionData.showLineHeatmap">
        <n-slider v-model:value="optionData.heatmapOpacity" :min="0.2" :max="0.7" :step="0.05" />
      </setting-item>
      <setting-item name="全部方向合成" v-if="optionData.showLineHeatmap">
        <n-select
          v-model:value="optionData.heatmapAllDirectionMode"
          :options="heatmapDirectionModeOptions"
          size="small"
        />
      </setting-item>
    </setting-item-box>

    <!-- 线路视觉 -->
    <setting-item-box name="线路视觉">
      <setting-item name="流动点速度" v-if="optionData.showDirectionArrows">
        <n-slider v-model:value="optionData.lineFlowSpeed" :min="15" :max="80" :step="5" />
      </setting-item>
      <setting-item name="拖尾长度（渐淡）" v-if="optionData.showDirectionArrows">
        <n-slider v-model:value="optionData.lineFlowTrailLength" :min="0.4" :max="0.9" :step="0.05" />
      </setting-item>
      <setting-item name="流动点大小" v-if="optionData.showDirectionArrows">
        <n-slider v-model:value="optionData.lineFlowSymbolSize" :min="3" :max="10" :step="1" />
      </setting-item>
      <setting-item name="到站停留(秒)" v-if="optionData.lineDisplayMode === 'flow'">
        <n-slider v-model:value="optionData.flowDwellDurationSec" :min="0" :max="5" :step="0.5" />
      </setting-item>
      <setting-item name="启用到站停留" v-if="optionData.lineDisplayMode === 'flow'">
        <n-switch v-model:value="optionData.flowDwellEnabled" />
      </setting-item>
      <setting-item name="流动点涟漪" v-if="optionData.showDirectionArrows">
        <n-switch v-model:value="optionData.enableLineFlowRipple" />
      </setting-item>
      <setting-item
        name="流动涟漪幅度"
        v-if="optionData.showDirectionArrows && optionData.enableLineFlowRipple"
      >
        <n-slider
          v-model:value="optionData.lineFlowRippleScaleMax"
          :min="3"
          :max="8"
          :step="0.2"
        />
      </setting-item>
      <setting-item name="线宽">
        <n-slider v-model:value="optionData.lineWidth" :min="1" :max="6" :step="0.5" />
      </setting-item>
      <setting-item name="参照线线宽" v-if="optionData.showLineHeatmap">
        <n-slider v-model:value="optionData.referenceLineWidth" :min="1.5" :max="4" :step="0.5" />
      </setting-item>
      <setting-item name="发光强度">
        <n-slider v-model:value="optionData.lineBloomIntensity" :min="0" :max="3" :step="0.1" />
      </setting-item>
    </setting-item-box>

    <!-- 标签视觉 -->
    <setting-item-box name="标签视觉">
      <setting-item name="字体大小">
        <n-slider v-model:value="optionData.labelFontSize" :min="12" :max="20" :step="1" />
      </setting-item>
      <setting-item name="字体颜色">
        <n-color-picker v-model:value="optionData.labelColor" />
      </setting-item>
    </setting-item-box>

    <!-- ECharts 配置 -->
    <setting-item-box name="ECharts 配置">
      <setting-item name="Geo缩放级别">
        <n-slider v-model:value="optionData.geoZoom" :min="0.5" :max="2" :step="0.1" />
      </setting-item>
      <setting-item name="线路弯曲度">
        <n-slider v-model:value="optionData.lineCurveness" :min="0" :max="0.5" :step="0.05" />
      </setting-item>
      <setting-item name="垂直留白比例">
        <n-slider v-model:value="optionData.mapPaddingRatio" :min="0.05" :max="0.3" :step="0.01" />
      </setting-item>
      <setting-item name="水平留白比例">
        <n-slider v-model:value="optionData.mapPaddingRatioX" :min="0.05" :max="0.45" :step="0.01" />
      </setting-item>
      <setting-item name="地图画布占比">
        <n-input v-model:value="optionData.mapLayoutSize" size="small" placeholder="如 88%" />
      </setting-item>
    </setting-item-box>
  </div>
</template>

<script setup lang="ts">
import { PropType, computed } from 'vue'
import { SettingItemBox, SettingItem } from '@/components/Pages/ChartItemSetting'
import type { CreateComponentType } from '@/packages/index.d'

const props = defineProps({
  optionData: {
    type: Object as PropType<CreateComponentType['option']>,
    required: true
  }
})

const metricOptions = [
  { label: '碳排放强度', value: 'carbon' },
  { label: '能耗强度', value: 'energy' }
]

const directionOptions = [
  { label: '全部', value: 'all' },
  { label: '上行', value: 'up' },
  { label: '下行', value: 'down' }
]

const lineDisplayModeOptions = [
  { label: '流动点', value: 'flow' },
  { label: '监测渐变', value: 'monitorGradient' }
]

const stationLabelScopeOptions = [
  { label: '仅重要站', value: 'important' },
  { label: '全部站点', value: 'all' }
]

const heatmapDirectionModeOptions = [
  { label: '取较大强度', value: 'max' },
  { label: '取平均强度', value: 'avg' },
  { label: '不合成（请选方向）', value: 'separate' }
]

</script>

<style scoped>
.railway-carbon-map-config {
  padding: 10px;
}
</style>
