import type { LineFeature, LineSectionProperties } from '../types'

export type ActiveMetric = 'carbon' | 'energy'
export type LineIntensityField = 'carbonIntensity' | 'energyIntensity'

export function getLineIntensityField(activeMetric: ActiveMetric): LineIntensityField {
  return activeMetric === 'energy' ? 'energyIntensity' : 'carbonIntensity'
}

export function getLineIntensity(
  line: LineFeature,
  field: LineIntensityField
): number {
  const value = line.properties[field]
  return typeof value === 'number' && isFinite(value) ? value : 0
}

export function getMetricLabel(activeMetric: ActiveMetric): string {
  return activeMetric === 'energy' ? '能耗强度' : '碳排放强度'
}

export function getMetricUnit(activeMetric: ActiveMetric): string {
  return activeMetric === 'energy' ? 'kWh/万吨·km' : 'tCO2e/万吨公里'
}

export function extractLineIntensities(
  lines: LineFeature[],
  field: LineIntensityField
): number[] {
  return lines
    .map(l => l.properties[field])
    .filter((v): v is number => typeof v === 'number' && isFinite(v))
}

export function getLineCarbonEmission(line: LineFeature): number {
  const value = line.properties.carbonEmission
  return typeof value === 'number' && isFinite(value) ? value : 0
}

export function extractLineEmissions(lines: LineFeature[]): number[] {
  return lines
    .map(getLineCarbonEmission)
    .filter(v => isFinite(v))
}

export function getLineEmissionRange(lines: LineFeature[]): { min: number; max: number } {
  const values = extractLineEmissions(lines)
  if (values.length === 0) {
    return { min: 0, max: 1 }
  }
  return { min: Math.min(...values), max: Math.max(...values) }
}
