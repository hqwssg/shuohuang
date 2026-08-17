import { PublicConfigClass } from '@/packages/public'
import { CreateComponentType } from '@/packages/index.d'
import { chartInitConfig } from '@/settings/designSetting'
import { ThreeCarbonSurfaceConfig } from './index'
import dataJson from './data.json'
import cloneDeep from 'lodash/cloneDeep'

export enum CarbonSurfaceTimeTypeEnum {
  MONTH = 'month',
  QUARTER = 'quarter'
}

export const option = {
  dataset: cloneDeep(dataJson),
  heightScale: 56,
  showWireframe: true,
  wireframeOpacity: 0.3,
  colorLow: '#0B5CFF',
  colorMidLow: '#00D4FF',
  colorMid: '#00FFB2',
  colorMidHigh: '#FFE600',
  colorHigh: '#FF3B30',
  backgroundColor: 'transparent',
  gridColor: '#00ffff',
  gridOpacity: 0.15,
  axisColor: '#2EC7FF',
  labelColor: '#ffffff',
  enableTooltip: true,
  tooltipTrigger: 'mixed',
  showColorLegend: false,
  enableIntroSpin: true,
  granularity: CarbonSurfaceTimeTypeEnum.MONTH
}

export default class Config extends PublicConfigClass implements CreateComponentType {
  public key = ThreeCarbonSurfaceConfig.key
  public attr = { ...chartInitConfig, w: 800, h: 500, zIndex: -1 }
  public chartConfig = cloneDeep(ThreeCarbonSurfaceConfig)
  public option = cloneDeep(option)
}
