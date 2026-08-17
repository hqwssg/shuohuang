import type { AccountingPoint } from '../types'

// 省份边界框（简化判断）
const PROVINCE_BOUNDS: Record<string, { minLng: number; maxLng: number; minLat: number; maxLat: number }> = {
  '140000': { minLng: 110.2, maxLng: 114.6, minLat: 34.5, maxLat: 40.8 }, // 山西
  '130000': { minLng: 113.4, maxLng: 119.8, minLat: 36.0, maxLat: 42.7 }  // 河北
}

/**
 * 根据站点坐标判断涉及哪些省份
 */
export function getInvolvedProvinces(points: AccountingPoint[]): string[] {
  const involved = new Set<string>()
  
  points.forEach(point => {
    Object.entries(PROVINCE_BOUNDS).forEach(([code, bounds]) => {
      if (
        point.lng >= bounds.minLng &&
        point.lng <= bounds.maxLng &&
        point.lat >= bounds.minLat &&
        point.lat <= bounds.maxLat
      ) {
        involved.add(code)
      }
    })
  })
  
  // 朔黄铁路至少包含山西和河北
  if (involved.size === 0) {
    return ['140000', '130000']
  }
  
  return Array.from(involved)
}
