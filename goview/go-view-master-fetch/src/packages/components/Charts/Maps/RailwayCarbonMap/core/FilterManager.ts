import type { LineFeature, AccountingPoint, MonitoringPoint, RailwayCarbonMapOption } from '../types'
import { getLineIntensityField, extractLineIntensities } from '../utils/lineMetric'
import { resolveLinesForDirection } from '../utils/mergeDirectionalLines'

export interface FilterResult {
  lines: LineFeature[]
  points: AccountingPoint[]
  /** showHighOnly 时为 P75 阈值，否则 null（线段不删减，仅用于淡化） */
  highEmissionThreshold: number | null
  stats: {
    visibleLineCount: number
    visiblePointCount: number
    metricRange: { min: number; max: number }
    monitorMetricRange: { min: number; max: number }
  }
}

export class FilterManager {
  applyFilters(
    allLines: LineFeature[],
    allPoints: AccountingPoint[],
    options: RailwayCarbonMapOption,
    monitoringPoints: MonitoringPoint[] = []
  ): FilterResult {
    const { activeDirection, showHighOnly, activeMetric } = options
    const metricField = getLineIntensityField(activeMetric)

    let filteredLines = resolveLinesForDirection(allLines, activeDirection)
    const scopedMetricValues = extractLineIntensities(filteredLines, metricField)

    let highEmissionThreshold: number | null = null
    if (showHighOnly) {
      highEmissionThreshold = this.get75thPercentile(scopedMetricValues)
    }

    const highLineCount =
      highEmissionThreshold == null
        ? filteredLines.length
        : this.countLinesAboveThreshold(filteredLines, metricField, highEmissionThreshold)

    const visibleMetricValues = extractLineIntensities(filteredLines, metricField)
    const visibleMetricRange = this.calculateRange(visibleMetricValues)

    let scopedMonitoring = monitoringPoints
    if (activeDirection !== 'all') {
      scopedMonitoring = monitoringPoints.filter(p => p.direction === activeDirection)
    }
    const monitorValues = scopedMonitoring
      .map(p => p[metricField])
      .filter((v): v is number => typeof v === 'number' && isFinite(v))
    const monitorMetricRange = this.calculateRange(monitorValues)

    const isMonitorMode = options.lineDisplayMode === 'monitorGradient'
    const metricRange =
      isMonitorMode && monitorValues.length > 0
        ? monitorMetricRange.min === Infinity
          ? { min: 0, max: 1 }
          : monitorMetricRange
        : visibleMetricRange.min === Infinity
          ? { min: 0, max: 0 }
          : visibleMetricRange

    return {
      lines: filteredLines,
      points: allPoints,
      highEmissionThreshold,
      stats: {
        visibleLineCount: highLineCount,
        visiblePointCount: allPoints.length,
        metricRange,
        monitorMetricRange:
          monitorMetricRange.min === Infinity ? { min: 0, max: 1 } : monitorMetricRange
      }
    }
  }

  private countLinesAboveThreshold(
    lines: LineFeature[],
    metricField: 'carbonIntensity' | 'energyIntensity',
    threshold: number
  ): number {
    return lines.filter(line => {
      const value = line.properties[metricField]
      return typeof value === 'number' && value >= threshold
    }).length
  }

  private calculateRange(values: number[]): { min: number; max: number } {
    if (values.length === 0) {
      return { min: Infinity, max: -Infinity }
    }

    return {
      min: Math.min(...values),
      max: Math.max(...values)
    }
  }

  get75thPercentile(values: number[]): number {
    if (values.length === 0) return 0
    if (values.length === 1) return values[0]

    const sortedValues = [...values].sort((a, b) => a - b)
    const position = (sortedValues.length - 1) * 0.75
    const lowerIndex = Math.floor(position)
    const upperIndex = Math.ceil(position)

    if (lowerIndex === upperIndex) {
      return sortedValues[lowerIndex]
    }

    const weight = position - lowerIndex
    return sortedValues[lowerIndex] * (1 - weight) + sortedValues[upperIndex] * weight
  }
}
