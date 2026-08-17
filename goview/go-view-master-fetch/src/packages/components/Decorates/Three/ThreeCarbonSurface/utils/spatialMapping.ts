/** 任务单：包围盒 width=12 depth=8 → x∈[-6,6] z∈[-4,4] */
export const WORLD_WIDTH = 12
export const WORLD_DEPTH = 8
export const WORLD_MIN_X = -WORLD_WIDTH / 2
export const WORLD_MAX_X = WORLD_WIDTH / 2
export const WORLD_MIN_Z = -WORLD_DEPTH / 2
export const WORLD_MAX_Z = WORLD_DEPTH / 2

/** heightScale 50–60 → 世界高度约 5–6 */
export function clampWorldMaxY(heightScale: number): number {
  const y = heightScale / 10
  return Math.min(6, Math.max(5, y))
}

export function getXByTimeIndex(timeIndex: number, timeCount: number, width: number = WORLD_WIDTH): number {
  if (timeCount <= 1) return 0
  return (timeIndex / (timeCount - 1) - 0.5) * width
}

export function getZByYearIndex(yearIndex: number, yearCount: number, depth: number = WORLD_DEPTH): number {
  if (yearCount <= 1) return 0
  return (yearIndex / (yearCount - 1) - 0.5) * depth
}

export function getYByValue(value: number, minValue: number, maxValue: number, height: number): number {
  if (maxValue === minValue) return 0
  const normalized = (value - minValue) / (maxValue - minValue)
  const t = Math.max(0, Math.min(1, normalized))
  return Math.pow(t, 0.75) * height
}

/** 热力归一化：与高度同一数值区间 */
export function heatNormalized(value: number, minValue: number, maxValue: number): number {
  if (maxValue === minValue) return 0
  return Math.max(0, Math.min(1, (value - minValue) / (maxValue - minValue)))
}

export function inverseTimeIndexFromX(x: number, timeCount: number, width: number = WORLD_WIDTH): number {
  if (timeCount <= 1) return 0
  const i = Math.round((x / width + 0.5) * (timeCount - 1))
  return Math.max(0, Math.min(timeCount - 1, i))
}

export function inverseYearIndexFromZ(z: number, yearCount: number, depth: number = WORLD_DEPTH): number {
  if (yearCount <= 1) return 0
  const j = Math.round((z / depth + 0.5) * (yearCount - 1))
  return Math.max(0, Math.min(yearCount - 1, j))
}

function niceStep(rough: number): number {
  if (!Number.isFinite(rough) || rough <= 0) return 1
  const exp = Math.floor(Math.log10(rough))
  const base = Math.pow(10, exp)
  const m = rough / base
  const f = m <= 1 ? 1 : m <= 2 ? 2 : m <= 5 ? 5 : 10
  return f * base
}

/**
 * Y 轴刻度：从 0 起，末档 ≥ maxValue，约 5–7 个易读整数
 */
export function createNiceTicks(maxValue: number, desiredCount = 6): number[] {
  if (!Number.isFinite(maxValue) || maxValue <= 0) {
    return [0]
  }
  let step = niceStep(maxValue / Math.max(1, desiredCount - 1))
  let upper = Math.ceil(maxValue / step) * step
  let ticks: number[] = []

  const build = (): void => {
    ticks = []
    for (let v = 0; v <= upper + 1e-9; v += step) {
      ticks.push(Math.round(v * 1000000) / 1000000)
    }
  }

  build()
  while (ticks.length > 8) {
    step *= 2
    upper = Math.ceil(maxValue / step) * step
    build()
  }
  if (ticks.length < 2) {
    ticks = [0, Math.max(1, Math.ceil(maxValue))]
  }
  return ticks
}
