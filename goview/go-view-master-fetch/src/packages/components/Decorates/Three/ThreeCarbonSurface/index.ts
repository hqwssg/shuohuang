import { ConfigType, PackagesCategoryEnum, ChartFrameEnum } from '@/packages/index.d'
import { ChatCategoryEnum, ChatCategoryEnumName } from '../../index.d'

export const ThreeCarbonSurfaceConfig: ConfigType = {
  key: 'ThreeCarbonSurface',
  chartKey: 'VThreeCarbonSurface',
  conKey: 'VCThreeCarbonSurface',
  title: '3D碳排放热力曲面图',
  category: ChatCategoryEnum.THREE,
  categoryName: ChatCategoryEnumName.THREE,
  package: PackagesCategoryEnum.DECORATES,
  chartFrame: ChartFrameEnum.COMMON,
  image: 'threeCarbonSurface.png'
}
