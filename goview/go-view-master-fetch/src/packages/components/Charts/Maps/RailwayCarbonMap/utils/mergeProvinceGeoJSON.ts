export const MERGED_MAP_NAME = 'railwayRegion'

interface GeoJSONFeature {
  type: 'Feature'
  properties: Record<string, unknown>
  geometry: {
    type: string
    coordinates: unknown
  }
}

interface GeoJSONFeatureCollection {
  type: 'FeatureCollection'
  features: GeoJSONFeature[]
}

const provinceLoaders: Record<string, () => Promise<{ default: GeoJSONFeatureCollection }>> = {
  '140000': () => import('../assets/provinces/140000.json'),
  '130000': () => import('../assets/provinces/130000.json')
}

/**
 * 合并多个省份 GeoJSON 为单一 FeatureCollection，供 ECharts registerMap 使用
 */
export async function mergeProvinceGeoJSON(codes: string[]): Promise<GeoJSONFeatureCollection> {
  const uniqueCodes = [...new Set(codes)]
  const features: GeoJSONFeature[] = []

  for (const code of uniqueCodes) {
    const loader = provinceLoaders[code]
    if (!loader) continue

    const module = await loader()
    const geoJSON = module.default
    if (geoJSON?.features?.length) {
      features.push(...geoJSON.features)
    }
  }

  return {
    type: 'FeatureCollection',
    features
  }
}
