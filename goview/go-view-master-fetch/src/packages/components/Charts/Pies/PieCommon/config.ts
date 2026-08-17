import { echartOptionProfixHandle, PublicConfigClass } from '@/packages/public'
import { PieCommonConfig } from './index'
import { CreateComponentType } from '@/packages/index.d'
import cloneDeep from 'lodash/cloneDeep'
import dataJson from './data.json'
import { DASH_PIE_TOOLTIP, DASH_PIE_SERIES_HOVER } from '@/assets/dashboard/dashPieHoverLabels'

// 不 merge 全局 legend，避免大屏饼图图例被主题 top/center 覆盖导致与圆环重叠
export const includes: string[] = []

export enum PieTypeEnum {
  NORMAL = '常规图',
  RING = '环形图',
  ROSE = '玫瑰图'
}

export const PieTypeObject = {
  [PieTypeEnum.NORMAL]: 'nomal',
  [PieTypeEnum.RING]: 'ring',
  [PieTypeEnum.ROSE]: 'rose'
}

// 其它配置
const otherConfig = {
  // 轮播动画
  isCarousel: false,
}

const option = {
  ...otherConfig,
  type: 'ring',
  tooltip: DASH_PIE_TOOLTIP,
  legend: {
    show: true
  },
  dataset: { ...dataJson },
  series: [
    {
      type: 'pie',
      radius: ['40%', '65%'],
      center: ['50%', '60%'],
      roseType: false,
      avoidLabelOverlap: false,
      itemStyle: {
        show: true,
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      ...DASH_PIE_SERIES_HOVER
    }
  ]
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key: string = PieCommonConfig.key

  public chartConfig = cloneDeep(PieCommonConfig)

  // 图表配置项
  public option = echartOptionProfixHandle(option, includes)
}
