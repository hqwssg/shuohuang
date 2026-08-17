import type { AccountingPoint, LineFeature } from '../types'

export interface DataBoundsOptions {
  /** 垂直方向留白；未指定 paddingRatioY 时使用 */
  paddingRatio?: number
  /** 水平方向留白；未指定时回退 paddingRatio */
  paddingRatioX?: number
  /** 垂直方向留白；未指定时回退 paddingRatio */
  paddingRatioY?: number
  layoutSize?: string | number
  layoutCenter?: [string, string]
}

export interface DataBoundsResult {
  center: [number, number]
  boundingCoords: [[number, number], [number, number]]
  layoutCenter: [string, string]
  layoutSize: string | number
}

const DEFAULT_PADDING_RATIO = 0.12
const DEFAULT_LAYOUT_SIZE = '88%'
const DEFAULT_LAYOUT_CENTER: [string, string] = ['50%', '50%']

function collectCoordinates(
  points: AccountingPoint[],
  lines: LineFeature[]
): [number, number][] {
  const coords: [number, number][] = []

  points.forEach(p => coords.push([p.lng, p.lat]))
  lines.forEach(line => {
    line.geometry.coordinates.forEach(c => coords.push(c))
  })

  return coords
}

/**
 * 根据站点与线路坐标计算 Geo viewport，含四周留白
 */
export function computeDataBounds(
  points: AccountingPoint[],
  lines: LineFeature[],
  options: DataBoundsOptions = {}
): DataBoundsResult {
  const paddingRatio = options.paddingRatio ?? DEFAULT_PADDING_RATIO
  const padLngRatio = options.paddingRatioX ?? paddingRatio
  const padLatRatio = options.paddingRatioY ?? paddingRatio
  const layoutSize = options.layoutSize ?? DEFAULT_LAYOUT_SIZE
  const layoutCenter = options.layoutCenter ?? DEFAULT_LAYOUT_CENTER

  const coords = collectCoordinates(points, lines)

  if (coords.length === 0) {
    return {
      center: [114.2, 38.45],
      boundingCoords: [
        [110.5, 40.5],
        [117.5, 37.5]
      ],
      layoutCenter,
      layoutSize
    }
  }

  let minLng = Infinity
  let maxLng = -Infinity
  let minLat = Infinity
  let maxLat = -Infinity

  coords.forEach(([lng, lat]) => {
    minLng = Math.min(minLng, lng)
    maxLng = Math.max(maxLng, lng)
    minLat = Math.min(minLat, lat)
    maxLat = Math.max(maxLat, lat)
  })

  const lngSpan = maxLng - minLng || 0.5
  const latSpan = maxLat - minLat || 0.5
  const padLng = lngSpan * padLngRatio
  const padLat = latSpan * padLatRatio

  minLng -= padLng
  maxLng += padLng
  minLat -= padLat
  maxLat += padLat

  return {
    center: [(minLng + maxLng) / 2, (minLat + maxLat) / 2],
    boundingCoords: [
      [minLng, maxLat],
      [maxLng, minLat]
    ],
    layoutCenter,
    layoutSize
  }
}
