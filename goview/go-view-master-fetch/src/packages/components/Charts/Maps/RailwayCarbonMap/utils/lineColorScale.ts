// 四色渐变配置：绿 → 黄 → 橙 → 红
const LINE_COLORS = {
  low: '#4CAF50',      // 绿色（低强度）
  midLow: '#FFEB3B',   // 黄色
  midHigh: '#FF9800',  // 橙色
  high: '#F44336'      // 红色（高强度）
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

export function getLineColor(value: number, min: number, max: number): string {
  if (max === min) return LINE_COLORS.midLow
  
  const normalized = Math.max(0, Math.min(1, (value - min) / (max - min)))
  
  if (normalized <= 0.33) {
    return lerpColor(LINE_COLORS.low, LINE_COLORS.midLow, normalized / 0.33)
  } else if (normalized <= 0.66) {
    return lerpColor(LINE_COLORS.midLow, LINE_COLORS.midHigh, (normalized - 0.33) / 0.33)
  } else {
    return lerpColor(LINE_COLORS.midHigh, LINE_COLORS.high, (normalized - 0.66) / 0.34)
  }
}

export { LINE_COLORS }
