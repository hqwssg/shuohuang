import * as THREE from 'three'
import type { HeatmapPalette } from '../types'

/**
 * 从十六进制字符串创建 THREE.Color
 */
export function createColor(hex: string): THREE.Color {
  return new THREE.Color(hex)
}

/** 五段热力：锚点 t = 0, 0.25, 0.5, 0.75, 1 */
const HEAT_STOPS = [0, 0.25, 0.5, 0.75, 1] as const

/**
 * 根据归一化值(0-1)获取热力图颜色（五锚点分段线性插值）
 */
export function getHeatmapColor(value: number, palette: HeatmapPalette): THREE.Color {
  const t = Math.max(0, Math.min(1, value))
  const colors = [palette.low, palette.midLow, palette.mid, palette.midHigh, palette.high]

  if (t <= HEAT_STOPS[0]) return colors[0].clone()
  if (t >= HEAT_STOPS[4]) return colors[4].clone()

  for (let i = 0; i < HEAT_STOPS.length - 1; i++) {
    const t0 = HEAT_STOPS[i]
    const t1 = HEAT_STOPS[i + 1]
    if (t >= t0 && t <= t1) {
      const span = t1 - t0 || 1
      const seg = (t - t0) / span
      return colors[i].clone().lerp(colors[i + 1], seg)
    }
  }
  return colors[2].clone()
}

/**
 * 创建热力调色板（五色对应 0 / 0.25 / 0.5 / 0.75 / 1）
 */
export function createHeatmapPalette(
  low: string,
  midLow: string,
  mid: string,
  midHigh: string,
  high: string
): HeatmapPalette {
  return {
    low: createColor(low),
    midLow: createColor(midLow),
    mid: createColor(mid),
    midHigh: createColor(midHigh),
    high: createColor(high)
  }
}

/**
 * 计算数据矩阵最大值
 */
export function computeMatrixMax(values: number[][] | null | undefined): number {
  if (!values || !Array.isArray(values)) {
    return 1
  }
  let max = 0
  for (const row of values) {
    if (!row) continue
    for (const v of row) {
      const n = Number(v) || 0
      if (n > max) max = n
    }
  }
  return max > 0 ? max : 1
}

/**
 * 计算数据矩阵最小值
 */
export function computeMatrixMin(values: number[][] | null | undefined): number {
  if (!values || !Array.isArray(values)) {
    return 0
  }
  let min = Number.POSITIVE_INFINITY
  let any = false
  for (const row of values) {
    if (!row) continue
    for (const v of row) {
      const n = Number(v) || 0
      any = true
      if (n < min) min = n
    }
  }
  if (!any) return 0
  if (!Number.isFinite(min)) return 0
  return min
}

/**
 * 计算数据归一化值(0-1)
 */
export function normalizeValue(value: number, max: number): number {
  if (max <= 0) return 0
  return Math.min(1, Math.max(0, value / max))
}
