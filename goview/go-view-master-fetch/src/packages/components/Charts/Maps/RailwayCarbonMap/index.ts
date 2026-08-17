import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const RailwayCarbonMapConfig: ConfigType = {
  key: 'RailwayCarbonMap',
  chartKey: 'VRailwayCarbonMap',
  conKey: 'VCRailwayCarbonMap',
  title: '铁路碳排放时空分布图',
  category: ChatCategoryEnum.MAP,
  categoryName: ChatCategoryEnumName.MAP,
  package: PackagesCategoryEnum.CHARTS,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'railway_carbon_map.png'
}
