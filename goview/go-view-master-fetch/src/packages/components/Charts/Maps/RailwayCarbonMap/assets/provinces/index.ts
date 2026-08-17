// 省份配置索引
export interface ProvinceConfig {
  code: string      // 行政区划代码
  name: string     // 省份名称
  file: string     // GeoJSON文件名
}

export const PROVINCE_INDEX: ProvinceConfig[] = [
  { code: '140000', name: '山西省', file: '140000.json' },
  { code: '130000', name: '河北省', file: '130000.json' }
]

// 根据省份代码获取配置
export function getProvinceConfig(code: string): ProvinceConfig | undefined {
  return PROVINCE_INDEX.find(p => p.code === code)
}

// 获取所有省份代码
export function getAllProvinceCodes(): string[] {
  return PROVINCE_INDEX.map(p => p.code)
}
