import type { CarbonSurfaceData } from '../types'
import cloneDeep from 'lodash/cloneDeep'

function isValidValuesMatrix(values: unknown): values is number[][] {
  if (!Array.isArray(values) || values.length === 0) return false
  return values.every((row) => Array.isArray(row))
}

/**
 * 将接口或历史数据统一为 CarbonSurfaceData；无法识别时返回 fallback（通常为静态默认数据）
 */
export function normalizeCarbonSurfaceData(raw: unknown, fallback: CarbonSurfaceData): CarbonSurfaceData {
  const base = cloneDeep(fallback)

  if (raw == null || typeof raw !== 'object') {
    return base
  }

  const r = raw as Record<string, unknown>

  // 常见嵌套：{ data: { ... } }
  if (
    r.data != null &&
    typeof r.data === 'object' &&
    !('values' in r) &&
    !('matrix' in r)
  ) {
    return normalizeCarbonSurfaceData(r.data, base)
  }

  // 新格式
  if (isValidValuesMatrix(r.values) && Array.isArray(r.years) && Array.isArray(r.times)) {
    const years = r.years as number[]
    const times = r.times as string[]
    if (years.length === 0 || times.length === 0) {
      return base
    }
    const values = (r.values as number[][]).map((row) => (Array.isArray(row) ? [...row] : []))
    return {
      timeType: r.timeType === 'quarter' ? 'quarter' : 'month',
      years,
      times,
      values,
      unit: typeof r.unit === 'string' ? r.unit : base.unit
    }
  }

  // 旧 GoView 格式：matrix + periodLabels
  if (
    isValidValuesMatrix(r.matrix) &&
    Array.isArray(r.years) &&
    Array.isArray(r.periodLabels)
  ) {
    const years = r.years as number[]
    const times = r.periodLabels as string[]
    if (years.length === 0 || times.length === 0) {
      return base
    }
    return {
      timeType: r.granularity === 'quarter' ? 'quarter' : 'month',
      years,
      times,
      values: r.matrix as number[][],
      unit: typeof r.unit === 'string' ? r.unit : base.unit
    }
  }

  return base
}
