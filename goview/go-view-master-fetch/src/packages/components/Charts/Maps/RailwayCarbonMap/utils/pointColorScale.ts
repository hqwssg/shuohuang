export type PointColorMode = 'contrast' | 'spectrum'

const POINT_COLORS = {
  low: '#0B5CFF',
  midLow: '#00D4FF',
  mid: '#00FFB2',
  midHigh: '#FFE600',
  high: '#FF3B30'
}

const CONTRAST_COLORS = {
  low: '#1565C0',
  high: '#FF3B30'
}

function hexToRgb(hex: string): { r: number; g: number; b: number } {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex)
  if (!result) return { r: 0, g: 0, b: 0 }
  return {
    r: parseInt(result[1], 16),
    g: parseInt(result[2], 16),
    b: parseInt(result[3], 16)
  }
}

function lerp(start: number, end: number, t: number): number {
  return Math.round(start + (end - start) * t)
}

function lerpColor(color1: string, color2: string, t: number): string {
  const c1 = hexToRgb(color1)
  const c2 = hexToRgb(color2)
  const r = lerp(c1.r, c2.r, t)
  const g = lerp(c1.g, c2.g, t)
  const b = lerp(c1.b, c2.b, t)
  return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`
}

/**
 * 将强度归一化到 0–1，gamma < 1 拉开学区间两端对比
 */
export function normalizeIntensity(
  value: number,
  min: number,
  max: number,
  gamma = 0.65
): number {
  if (max === min) return 0.5
  const linear = Math.max(0, Math.min(1, (value - min) / (max - min)))
  return Math.pow(linear, gamma)
}

function getSpectrumColor(t: number): string {
  if (t <= 0.25) {
    return lerpColor(POINT_COLORS.low, POINT_COLORS.midLow, t / 0.25)
  }
  if (t <= 0.5) {
    return lerpColor(POINT_COLORS.midLow, POINT_COLORS.mid, (t - 0.25) / 0.25)
  }
  if (t <= 0.75) {
    return lerpColor(POINT_COLORS.mid, POINT_COLORS.midHigh, (t - 0.5) / 0.25)
  }
  return lerpColor(POINT_COLORS.midHigh, POINT_COLORS.high, (t - 0.75) / 0.25)
}

export function getPointColor(
  value: number,
  min: number,
  max: number,
  mode: PointColorMode = 'contrast',
  gamma = 0.65
): string {
  const t = normalizeIntensity(value, min, max, gamma)
  if (mode === 'contrast') {
    return lerpColor(CONTRAST_COLORS.low, CONTRAST_COLORS.high, t)
  }
  return getSpectrumColor(t)
}

/** 为辐射渐变附加透明度 */
export function colorWithAlpha(hex: string, alpha: number): string {
  const { r, g, b } = hexToRgb(hex)
  const a = Math.max(0, Math.min(1, alpha))
  return `rgba(${r},${g},${b},${a})`
}

/** 由内向外渐淡至透明的径向渐变（用于辐射圆盘） */
export function getRadiationGradient(centerHex: string, innerAlpha = 0.95): object {
  return {
    type: 'radial',
    x: 0.5,
    y: 0.5,
    r: 0.5,
    colorStops: [
      { offset: 0, color: colorWithAlpha(centerHex, innerAlpha) },
      { offset: 0.35, color: colorWithAlpha(centerHex, innerAlpha * 0.55) },
      { offset: 0.7, color: colorWithAlpha(centerHex, innerAlpha * 0.2) },
      { offset: 1, color: colorWithAlpha(centerHex, 0) }
    ]
  }
}

/** 按强度计算辐射圆半径（像素） */
export function getRadiationRadiusPx(
  intensity: number,
  min: number,
  max: number,
  radiationMinPx: number,
  radiationMaxPx: number,
  gamma = 0.65
): number {
  const t = normalizeIntensity(intensity, min, max, gamma)
  return radiationMinPx + t * (radiationMaxPx - radiationMinPx)
}

export { POINT_COLORS, CONTRAST_COLORS }
