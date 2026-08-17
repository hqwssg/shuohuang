import { echartOptionProfixHandle, PublicConfigClass } from '@/packages/public'
import { BarGradientConfig } from './index'
import { CreateComponentType } from '@/packages/index.d'
import cloneDeep from 'lodash/cloneDeep'
import dataJson from './data.json'

export const includes = ['legend', 'xAxis', 'yAxis', 'grid']

export type GradientDirectionType = 'vertical' | 'horizontal'

export const seriesItem = {
  type: 'bar',
  barWidth: 15,
  label: {
    show: true,
    position: 'top',
    color: '#fff',
    fontSize: 12
  },
  itemStyle: {
    borderRadius: 2,
    // ECharts 支持对象式渐变，配置面板会直接修改此对象
    color: {
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
}

export const option = {
  tooltip: {
    show: true,
    trigger: 'axis',
    axisPointer: {
      show: true,
      type: 'shadow'
    }
  },
  xAxis: {
    show: true,
    type: 'category'
  },
  yAxis: {
    show: true,
    type: 'value'
  },
  dataset: { ...dataJson },
  series: [cloneDeep(seriesItem), cloneDeep(seriesItem)]
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = BarGradientConfig.key
  public chartConfig = cloneDeep(BarGradientConfig)
  public option = echartOptionProfixHandle(option, includes)
}

