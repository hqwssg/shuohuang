import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const BarGradientConfig: ConfigType = {
  key: 'BarGradient',
  chartKey: 'VBarGradient',
  conKey: 'VCBarGradient',
  title: '我的渐变柱状图',
  category: ChatCategoryEnum.BAR,
  categoryName: ChatCategoryEnumName.BAR,
  package: PackagesCategoryEnum.CHARTS,
  chartFrame: ChartFrameEnum.ECHARTS,
  // 复用现有柱状图缩略图
  image: 'bar_x.png'
}

