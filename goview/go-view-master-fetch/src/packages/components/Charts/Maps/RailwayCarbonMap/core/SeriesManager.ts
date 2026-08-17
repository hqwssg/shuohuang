import * as echarts from 'echarts/core'
import type { RailwayCarbonMapOption, LineFeature, AccountingPoint } from '../types'
import { DEFAULT_STATION_ORDER } from '../types'
import type { FilterResult } from './FilterManager'
import { getLineColor, LINE_COLORS } from '../utils/lineColorScale'
import {
  colorWithAlpha,
  getRadiationGradient,
  getRadiationRadiusPx
} from '../utils/pointColorScale'
import { computeDataBounds } from '../utils/computeDataBounds'
import { shouldShowStationLabel } from '../utils/stationLabelScope'
import { MERGED_MAP_NAME } from '../utils/mergeProvinceGeoJSON'
import { buildLineHeatmapGrid, type HeatmapGridResult } from '../utils/buildLineHeatmapGrid'
import {
  getLineIntensityField,
  getLineIntensity,
  getMetricLabel,
  getMetricUnit
} from '../utils/lineMetric'
import { buildLineFlowTracks, buildSingleFlowGuideTrack, type LineFlowTrack } from '../utils/lineFlowRipple'
import { buildStationFittedFlowCoords } from '../utils/buildStationFittedFlowPath'
import { buildRoundTripCurvedFlowCoords } from '../utils/curveGeometry'
import { filterImportantStations, scalePx } from '../utils/stationVisualScale'

const MAP_ITEM_STYLE = {
  areaColor: {
    type: 'radial' as const,
    x: 0.5,
    y: 0.5,
    r: 0.8,
    colorStops: [
      { offset: 0, color: '#93ebf800' },
      { offset: 1, color: '#93ebf820' }
    ],
    globalCoord: false
  },
  borderColor: '#93EBF8',
  borderWidth: 1,
  shadowColor: '#80D9F842',
  shadowOffsetX: -2,
  shadowOffsetY: 2,
  shadowBlur: 10
}

/** 线段不显示颜色（透明底线，仅流动点可见） */
const INVISIBLE_LINE_COLOR = 'rgba(0, 0, 0, 0)'

const MAP_EMPHASIS = {
  itemStyle: {
    areaColor: '#389BB7',
    shadowColor: '#389BB7'
  },
  label: {
    color: '#ffffff'
  }
}

const HIGH_ONLY_DIM_OPACITY = 0.12

export class SeriesManager {
  private options: RailwayCarbonMapOption | null = null
  private filteredData: FilterResult | null = null

  setDataAndOptions(filteredData: FilterResult, options: RailwayCarbonMapOption): void {
    this.filteredData = filteredData
    this.options = options
  }

  generateOption(): echarts.EChartsCoreOption {
    if (!this.options || !this.filteredData) {
      return {}
    }

    const showLineHeatmap = this.options.showLineHeatmap === true
    const heatmapGrid = showLineHeatmap ? this.computeHeatmapGrid() : null

    return {
      backgroundColor: 'rgba(0,0,0,0)',
      tooltip: this.buildTooltip(),
      geo: this.buildGeo(),
      visualMap: this.buildVisualMap(heatmapGrid),
      series: this.buildSeries(heatmapGrid)
    }
  }

  private computeHeatmapGrid(): HeatmapGridResult | null {
    const {
      showLineHeatmap = false,
      showBaseLine = true,
      heatmapGridStep = 0.05,
      heatmapInfluenceRadius = 0.07,
      heatmapAllDirectionMode = 'max',
      activeDirection = 'all',
      mapPaddingRatio = 0.12,
      mapPaddingRatioX,
      mapLayoutSize = '88%'
    } = this.options!

    if (!showBaseLine || !showLineHeatmap) return null

    const bounds = computeDataBounds(this.filteredData!.points, this.filteredData!.lines, {
      paddingRatio: mapPaddingRatio,
      paddingRatioX: mapPaddingRatioX,
      layoutSize: mapLayoutSize
    })

    const intensityField = getLineIntensityField(this.options!.activeMetric)

    return buildLineHeatmapGrid(this.filteredData!.lines, {
      boundingCoords: bounds.boundingCoords,
      gridStep: heatmapGridStep,
      influenceRadiusDeg: heatmapInfluenceRadius,
      sampleIntervalKm: 5,
      activeDirection,
      allDirectionMode: heatmapAllDirectionMode,
      intensityField
    })
  }

  private buildGeo(): object {
    const {
      geoZoom = 1,
      mapPaddingRatio = 0.12,
      mapPaddingRatioX,
      mapLayoutSize = '88%'
    } = this.options!
    const bounds = computeDataBounds(this.filteredData!.points, this.filteredData!.lines, {
      paddingRatio: mapPaddingRatio,
      paddingRatioX: mapPaddingRatioX,
      layoutSize: mapLayoutSize
    })

    return {
      map: MERGED_MAP_NAME,
      roam: false,
      zoom: geoZoom,
      center: bounds.center,
      boundingCoords: bounds.boundingCoords,
      layoutCenter: bounds.layoutCenter,
      layoutSize: bounds.layoutSize,
      label: { show: false },
      itemStyle: MAP_ITEM_STYLE,
      emphasis: MAP_EMPHASIS
    }
  }

  private buildTooltip(): object {
    return {
      trigger: 'item',
      backgroundColor: 'rgba(10, 26, 42, 0.95)',
      borderColor: '#00ffff',
      borderWidth: 1,
      textStyle: { color: '#ffffff' },
      confine: true,
      // 自动 tour 弹框时仍保留地图上其余站点圆点与标签
      triggerOn: 'mousemove|click',
      formatter: (params: {
        seriesType?: string
        seriesName?: string
        data?: Record<string, unknown>
        name?: string
      }) => {
        if (params.seriesType === 'scatter' && params.seriesName === '核算点') {
          return this.formatPointTooltip(params.data!)
        }
        if (
          params.seriesType === 'effectScatter' &&
          params.seriesName === '核算点'
        ) {
          return this.formatPointTooltip(params.data!)
        }
        if (params.seriesType === 'lines') {
          return this.formatLineTooltip(params.data!)
        }
        if (params.seriesType === 'heatmap' && params.seriesName === '线路热力') {
          const raw = params.data as Record<string, unknown> | [number, number, number]
          const data =
            Array.isArray(raw)
              ? { value: raw, metricLabel: getMetricLabel(this.options!.activeMetric) }
              : raw
          return this.formatHeatmapTooltip(data as Record<string, unknown>)
        }
        if (params.seriesType === 'map') {
          return params.name ?? ''
        }
        return ''
      }
    }
  }

  private buildVisualMap(heatmapGrid: HeatmapGridResult | null): object | null {
    const { showLegend, showBaseLine = true, showLineHeatmap = false } = this.options!

    if (!showLegend || !showBaseLine) return null

    let min = 0
    let max = 0

    if (showLineHeatmap && heatmapGrid && heatmapGrid.data.length > 0) {
      min = heatmapGrid.min
      max = heatmapGrid.max
    } else {
      const range = this.filteredData!.stats.metricRange
      if (range.max <= range.min && range.max === 0) {
        const field = getLineIntensityField(this.options!.activeMetric)
        const values = this.filteredData!.lines
          .map(l => l.properties[field])
          .filter((v): v is number => typeof v === 'number' && isFinite(v))
        if (values.length === 0) return null
        min = Math.min(...values)
        max = Math.max(...values)
      } else {
        min = range.min
        max = range.max
      }
    }

    if (min === max) {
      max = min + 0.01
    }

    const metricLabel = getMetricLabel(this.options!.activeMetric)

    return {
      show: true,
      type: 'continuous',
      min,
      max,
      left: 'right',
      bottom: '10%',
      calculable: true,
      text: [metricLabel, ''],
      inRange: {
        color: [LINE_COLORS.low, LINE_COLORS.midLow, LINE_COLORS.midHigh, LINE_COLORS.high]
      },
      textStyle: { color: '#ffffff' }
    }
  }

  private buildSeries(heatmapGrid: HeatmapGridResult | null): echarts.SeriesOption[] {
    const series: echarts.SeriesOption[] = []
    const {
      showBaseLine = true,
      showLineHeatmap = false,
      showReferenceLines = true,
      lineWidth = 3,
      referenceLineWidth = 2.5
    } = this.options!

    const useHeatmapLayer = !!(showLineHeatmap && heatmapGrid && heatmapGrid.data.length > 0)
    const lineDisplayMode = this.options!.lineDisplayMode ?? 'flow'
    const isMonitorMode = lineDisplayMode === 'monitorGradient'
    const showFlow =
      !isMonitorMode &&
      (this.options!.showDirectionArrows !== false) &&
      !useHeatmapLayer &&
      showBaseLine
    const pointLayerZ = useHeatmapLayer
      ? { radiation: 3, ripple: 3, core: 6 }
      : showFlow
        ? { radiation: 3, ripple: 3, core: 5 }
        : { radiation: 4, ripple: 4, core: 5 }
    const { min: colorMin, max: colorMax } = this.filteredData!.stats.metricRange
    const lineColorMax = colorMax <= colorMin ? colorMin + 0.01 : colorMax

    series.push({
      name: '区域',
      type: 'map',
      geoIndex: 0,
      map: MERGED_MAP_NAME,
      roam: false,
      label: { show: false },
      itemStyle: MAP_ITEM_STYLE,
      emphasis: MAP_EMPHASIS,
      data: []
    })

    if (showBaseLine) {
      if (useHeatmapLayer) {
        series.push(this.buildHeatmapSeries(heatmapGrid!))
      }

      const shouldDrawLines =
        !useHeatmapLayer || (showReferenceLines && useHeatmapLayer)
      if (shouldDrawLines) {
        const isReference = useHeatmapLayer
        const refWidth = isReference ? referenceLineWidth : lineWidth
        const lineZUp = isReference ? 5 : 3

        if (isMonitorMode) {
          const monitorLines = this.filteredData!.lines
          if (monitorLines.length > 0) {
            series.push(
              this.buildMonitorGradientLinesSeries(
                monitorLines,
                '线路监测',
                refWidth,
                lineZUp,
                colorMin,
                lineColorMax
              )
            )
          }
        } else {
          const curvedLines = this.filteredData!.lines
          const lineZ = isReference ? 5 : 3
          const isFlowMode = lineDisplayMode === 'flow'

          if (curvedLines.length > 0) {
            series.push(
              this.buildLinesSeries(
                curvedLines,
                '朔黄线',
                refWidth,
                lineZ,
                isReference,
                { showFlow: !isFlowMode && !isReference, useCurveness: false }
              )
            )
          }

          if (isFlowMode && !isReference && !useHeatmapLayer) {
            const stationOrder =
              this.options!.dataset?.meta?.stationOrder ?? DEFAULT_STATION_ORDER
            const stationCoords = buildStationFittedFlowCoords(
              this.filteredData!.points,
              stationOrder
            )
            const { lineCurveness = 0.2 } = this.options!
            const flowCoords = buildRoundTripCurvedFlowCoords(stationCoords, lineCurveness)
            if (flowCoords.length >= 2) {
              series.push(this.buildFlowGuideSeries(flowCoords, lineZ + 1))
            }
          }
        }
      }
    }

    if (this.filteredData!.points.length > 0) {
      const allPoints = this.filteredData!.points
      const haloPoints = filterImportantStations(allPoints)
      const { showHaloEffect = true } = this.options!
      if (showHaloEffect) {
        series.push(this.buildRadiationSeries(haloPoints, pointLayerZ.radiation))
        if (this.options!.enableRipple !== false) {
          series.push(this.buildRippleSeries(haloPoints, pointLayerZ.ripple))
        }
      }
      series.push(this.buildCorePointsSeries(allPoints, pointLayerZ.core))
    }

    return series
  }

  private buildHeatmapSeries(grid: HeatmapGridResult): echarts.SeriesOption {
    const {
      heatmapPointSize = 12,
      heatmapBlurSize = 8,
      heatmapOpacity = 0.42
    } = this.options!

    return {
      name: '线路热力',
      type: 'heatmap',
      geoIndex: 0,
      zlevel: 2,
      coordinateSystem: 'geo',
      silent: false,
      pointSize: heatmapPointSize,
      blurSize: heatmapBlurSize,
      itemStyle: {
        opacity: heatmapOpacity
      },
      data: grid.data.map(([lng, lat, value]) => ({
        value: [lng, lat, value],
        metricLabel: getMetricLabel(this.options!.activeMetric),
        metricUnit: getMetricUnit(this.options!.activeMetric),
        statPeriod: this.filteredData!.lines[0]?.properties.statPeriod ?? ''
      }))
    }
  }

  getLineFlowTracks(lines: LineFeature[]): LineFlowTrack[] {
    const {
      intensityGamma = 0.65,
      lineFlowRippleScaleMin = 2.4,
      lineFlowRippleScaleMax = 5.2,
      lineFlowRipplePeriodMin = 3.2,
      lineFlowRipplePeriodMax = 5.2,
      lineDisplayMode = 'flow',
      lineCurveness = 0.2,
      lineFlowSpeed = 40
    } = this.options!

    if (lineDisplayMode !== 'flow') {
      return buildLineFlowTracks(lines, this.options!.activeMetric, {
        intensityGamma,
        lineFlowRippleScaleMin,
        lineFlowRippleScaleMax,
        lineFlowRipplePeriodMin,
        lineFlowRipplePeriodMax
      })
    }

    const stationOrder =
      this.options!.dataset?.meta?.stationOrder ?? DEFAULT_STATION_ORDER
    const stationCoords = buildStationFittedFlowCoords(this.filteredData!.points, stationOrder)
    const flowCoords = buildRoundTripCurvedFlowCoords(stationCoords, lineCurveness)
    if (flowCoords.length < 2) return []

    const field = getLineIntensityField(this.options!.activeMetric)
    const intensities = lines.map(l => getLineIntensity(l, field))
    const min = intensities.length ? Math.min(...intensities) : 0
    const max = intensities.length ? Math.max(...intensities) : 1
    const flowColor = getLineColor((min + max) / 2, min, max === min ? min + 0.01 : max)

    return [buildSingleFlowGuideTrack(flowCoords, flowColor, lineFlowSpeed)]
  }

  shouldAnimateLineFlowRipple(): boolean {
    const {
      showDirectionArrows = true,
      showBaseLine = true,
      showLineHeatmap = false,
      lineDisplayMode = 'flow',
      enableLineFlowRipple = false
    } = this.options!
    return (
      lineDisplayMode === 'flow' &&
      showDirectionArrows !== false &&
      showBaseLine &&
      showLineHeatmap !== true &&
      enableLineFlowRipple !== false &&
      !this.shouldUseFlowTour()
    )
  }

  shouldAnimateFlow(): boolean {
    const {
      showDirectionArrows = true,
      showBaseLine = true,
      showLineHeatmap = false,
      lineDisplayMode = 'flow'
    } = this.options!
    return (
      lineDisplayMode === 'flow' &&
      showDirectionArrows !== false &&
      showBaseLine &&
      showLineHeatmap !== true
    )
  }

  shouldUseFlowTour(): boolean {
    const {
      flowDwellEnabled = true,
      flowDwellDurationSec = 2,
      lineDisplayMode = 'flow'
    } = this.options!
    return (
      lineDisplayMode === 'flow' &&
      flowDwellEnabled !== false &&
      flowDwellDurationSec > 0
    )
  }

  getFlowTourOptions() {
    const {
      flowDwellDurationSec = 2,
      flowTourSpeedRatio = 1.2,
      lineFlowSpeed = 40
    } = this.options!
    return { flowDwellDurationSec, flowTourSpeedRatio, lineFlowSpeed }
  }

  getCorePointTipTarget(chart: {
    getOption: () => { series?: Array<{ name?: string; data?: Array<{ name?: string }> }> }
  }): { seriesIndex: number; nameToDataIndex: Map<string, number> } | null {
    const series = chart.getOption().series ?? []
    for (let i = 0; i < series.length; i++) {
      const item = series[i]
      if (item.name === '核算点') {
        const nameToDataIndex = new Map<string, number>()
        ;(item.data ?? []).forEach((d, idx) => {
          if (d?.name) nameToDataIndex.set(d.name, idx)
        })
        return { seriesIndex: i, nameToDataIndex }
      }
    }
    return null
  }

  private useFlowTourEffect(): boolean {
    return this.shouldUseFlowTour()
  }

  private resolveLineSegmentOpacity(intensity: number, baseOpacity: number): number {
    const threshold = this.filteredData?.highEmissionThreshold
    if (threshold == null) return baseOpacity
    return intensity >= threshold ? baseOpacity : HIGH_ONLY_DIM_OPACITY
  }

  private buildMonitorGradientLinesSeries(
    lines: LineFeature[],
    name: string,
    lineWidth: number,
    zLevel: number,
    min: number,
    max: number
  ): echarts.SeriesOption {
    const field = getLineIntensityField(this.options!.activeMetric)

    return {
      name,
      type: 'lines',
      geoIndex: 0,
      zlevel: zLevel,
      coordinateSystem: 'geo',
      animation: false,
      effect: { show: false },
      lineStyle: {
        width: lineWidth,
        opacity: 0.88,
        curveness: 0
      },
      data: lines.map(f => {
        const v = getLineIntensity(f, field)
        const color = getLineColor(v, min, max)
        const opacity = this.resolveLineSegmentOpacity(v, 0.88)
        return {
          coords: f.geometry.coordinates,
          ...f.properties,
          lineStyle: { color, width: lineWidth, opacity }
        }
      })
    }
  }

  private buildLinesSeries(
    lines: LineFeature[],
    name: string,
    lineWidth: number,
    zLevel: number,
    isReference = false,
    opts: { showFlow?: boolean; useCurveness?: boolean } = {}
  ): echarts.SeriesOption {
    const field = getLineIntensityField(this.options!.activeMetric)
    const values = lines.map(l => getLineIntensity(l, field))
    const min = Math.min(...values)
    const max = Math.max(...values)
    const {
      showDirectionArrows = true,
      showBaseLine = true,
      lineFlowSpeed = 40,
      lineFlowTrailLength = 0.88,
      lineFlowSymbolSize = 5,
      lineCurveness = 0.2
    } = this.options!

    const lineOpacity = isReference ? 0.95 : 0.88
    const showFlow =
      opts.showFlow ?? (!isReference && showDirectionArrows !== false && showBaseLine)
    const curveness = opts.useCurveness ? lineCurveness : 0
    const trailLength =
      typeof lineFlowTrailLength === 'number' && lineFlowTrailLength > 0
        ? lineFlowTrailLength
        : 0.88
    const useVisibleLineColor = !isReference

    return {
      name,
      type: 'lines',
      geoIndex: 0,
      zlevel: zLevel,
      coordinateSystem: 'geo',
      polyline: true,
      animation: false,
      effect: {
        show: showFlow,
        constantSpeed: lineFlowSpeed,
        trailLength,
        symbol: 'circle',
        symbolSize: lineFlowSymbolSize,
        loop: true
      },
      lineStyle: {
        width: isReference ? lineWidth : Math.max(lineWidth, 1),
        opacity: lineOpacity,
        curveness,
        color: useVisibleLineColor ? undefined : isReference ? undefined : INVISIBLE_LINE_COLOR,
        shadowColor: isReference ? 'rgba(0, 0, 0, 0.55)' : undefined,
        shadowBlur: isReference ? 4 : 0
      },
      data: lines.map(f => {
        const intensity = getLineIntensity(f, field)
        const flowColor = getLineColor(intensity, min, max)
        const segOpacity = this.resolveLineSegmentOpacity(intensity, lineOpacity)
        const coords = f.geometry.coordinates as [number, number][]
        const segCurveness = coords.length > 2 ? 0 : curveness
        return {
          coords,
          ...f.properties,
          lineStyle: {
            color: flowColor,
            width: Math.max(lineWidth, 1),
            opacity: segOpacity,
            curveness: segCurveness
          },
          effect: {
            show: showFlow && segOpacity >= lineOpacity,
            color: flowColor,
            constantSpeed: lineFlowSpeed,
            trailLength,
            symbolSize: lineFlowSymbolSize,
            symbol: 'circle',
            loop: true
          }
        }
      })
    }
  }

  private buildFlowGuideSeries(
    flowCoords: [number, number][],
    zLevel: number
  ): echarts.SeriesOption {
    const {
      lineFlowSpeed = 40,
      lineFlowTrailLength = 0.88,
      lineFlowSymbolSize = 5,
      activeMetric
    } = this.options!

    const useTour = this.useFlowTourEffect()

    const field = getLineIntensityField(activeMetric)
    const lines = this.filteredData!.lines
    const intensities = lines.map(l => getLineIntensity(l, field))
    const min = intensities.length ? Math.min(...intensities) : 0
    const max = intensities.length ? Math.max(...intensities) : 1
    const mid = (min + max) / 2
    const flowColor = getLineColor(mid, min, max === min ? min + 0.01 : max)

    const trailLength =
      typeof lineFlowTrailLength === 'number' && lineFlowTrailLength > 0
        ? lineFlowTrailLength
        : 0.88

    return {
      name: 'railway-flow-guide',
      type: 'lines',
      geoIndex: 0,
      zlevel: zLevel,
      coordinateSystem: 'geo',
      polyline: true,
      animation: false,
      effect: {
        show: !useTour,
        constantSpeed: lineFlowSpeed,
        trailLength,
        symbol: 'circle',
        symbolSize: lineFlowSymbolSize,
        loop: true,
        color: flowColor
      },
      lineStyle: {
        width: 0,
        opacity: 0,
        curveness: 0,
        color: INVISIBLE_LINE_COLOR
      },
      data: [
        {
          coords: flowCoords,
          effect: {
            show: !useTour,
            color: flowColor,
            constantSpeed: lineFlowSpeed,
            trailLength,
            symbolSize: lineFlowSymbolSize,
            symbol: 'circle',
            loop: true
          }
        }
      ]
    }
  }

  private getIntensityRange(points: AccountingPoint[]) {
    const intensities = points.map(p => p.carbonIntensity)
    return {
      minIntensity: Math.min(...intensities),
      maxIntensity: Math.max(...intensities)
    }
  }

  /** 底层：强度径向辐射圆盘（scatter + 径向渐变；与 effectScatter 分离以保证可见） */
  private buildRadiationSeries(
    points: AccountingPoint[],
    zlevel = 4
  ): echarts.SeriesOption {
    const {
      intensityGamma = 0.65,
      radiationMinPx = 40,
      radiationMaxPx = 115,
      radiationInnerAlpha = 0.95
    } = this.options!

    const { minIntensity, maxIntensity } = this.getIntensityRange(points)

    return {
      name: '强度辐射',
      type: 'scatter',
      geoIndex: 0,
      zlevel,
      coordinateSystem: 'geo',
      silent: true,
      symbol: 'circle',
      data: points.map(p => {
        const color = getLineColor(p.carbonIntensity, minIntensity, maxIntensity)
        const radius = getRadiationRadiusPx(
          p.carbonIntensity,
          minIntensity,
          maxIntensity,
          radiationMinPx,
          radiationMaxPx,
          intensityGamma
        )
        return {
          name: p.name,
          value: [p.lng, p.lat],
          symbolSize: radius,
          itemStyle: {
            color: getRadiationGradient(color, radiationInnerAlpha),
            borderWidth: 0
          }
        }
      })
    }
  }

  /** 波纹层：effectScatter 使用纯色描边，避免径向渐变导致涟漪不渲染 */
  private buildRippleSeries(points: AccountingPoint[], zlevel = 4): echarts.SeriesOption {
    const {
      intensityGamma = 0.65,
      radiationMinPx = 40,
      radiationMaxPx = 115,
      pointBaseSize = 11
    } = this.options!

    const { minIntensity, maxIntensity } = this.getIntensityRange(points)

    return {
      name: '强度波纹',
      type: 'effectScatter',
      geoIndex: 0,
      zlevel,
      coordinateSystem: 'geo',
      silent: true,
      symbol: 'circle',
      showEffectOn: 'render',
      rippleEffect: {
        brushType: 'stroke',
        scale: 3.5,
        period: 4,
        number: 3
      },
      data: points.map(p => {
        const color = getLineColor(p.carbonIntensity, minIntensity, maxIntensity)
        const radius = getRadiationRadiusPx(
          p.carbonIntensity,
          minIntensity,
          maxIntensity,
          radiationMinPx,
          radiationMaxPx,
          intensityGamma
        )
        const rippleAnchor = Math.max(pointBaseSize * 1.5, radius * 0.45)
        return {
          name: p.name,
          value: [p.lng, p.lat],
          symbolSize: rippleAnchor,
          itemStyle: {
            color: colorWithAlpha(color, 0.12),
            borderColor: colorWithAlpha(color, 0.75),
            borderWidth: 1.5,
            opacity: 0.15
          }
        }
      })
    }
  }

  /** 顶层：固定大小实心圆点（标签与 tooltip） */
  private buildCorePointsSeries(
    points: AccountingPoint[],
    zlevel = 5
  ): echarts.SeriesOption {
    const {
      pointBaseSize = 11,
      showPointLabels = true,
      stationLabelScope = 'important',
      intensityGamma = 0.65,
      showIntensityOnLabel = true,
      labelFontSize = 15,
      labelColor = '#ffffff'
    } = this.options!

    const { minIntensity, maxIntensity } = this.getIntensityRange(points)

    return {
      name: '核算点',
      type: 'scatter',
      geoIndex: 0,
      zlevel,
      coordinateSystem: 'geo',
      symbol: 'circle',
      emphasis: {
        focus: 'none',
        scale: 1.2,
        itemStyle: {
          borderWidth: 2,
          shadowBlur: 10,
          shadowColor: 'rgba(77, 184, 255, 0.8)'
        }
      },
      blur: {
        itemStyle: { opacity: 1 },
        label: { show: true, opacity: 1 }
      },
      label: {
        show: true,
        position: 'bottom',
        color: labelColor,
        fontSize: labelFontSize,
        formatter: (params: { data: { name: string; carbonIntensity: number } }) => {
          const { name, carbonIntensity } = params.data
          if (showIntensityOnLabel) {
            return `${name}\n${carbonIntensity}`
          }
          return name
        }
      },
      data: points.map(p => {
        const color = getLineColor(p.carbonIntensity, minIntensity, maxIntensity)
        return {
          name: p.name,
          value: [p.lng, p.lat, p.carbonIntensity],
          type: p.type,
          carbonIntensity: p.carbonIntensity,
          carbonEmission: p.carbonEmission,
          unit: p.unit,
          latestAccountingTime: p.latestAccountingTime,
          symbolSize: scalePx(pointBaseSize, p.name),
          label: {
            show: shouldShowStationLabel(p.name, stationLabelScope, showPointLabels)
          },
          itemStyle: {
            color,
            borderColor: '#ffffff',
            borderWidth: 1
          }
        }
      })
    }
  }

  private formatPointTooltip(data: Record<string, unknown>): string {
    return `
      <div style="font-weight:bold;color:#4db8ff">${data.name} (${data.type})</div>
      <div>碳排放强度: ${data.carbonIntensity}</div>
      <div>碳排放量: ${data.carbonEmission} ${data.unit}</div>
    `
  }

  private formatLineTooltip(data: Record<string, unknown>): string {
    const activeMetric = this.options!.activeMetric
    const metricLabel = getMetricLabel(activeMetric)
    const metricUnit = getMetricUnit(activeMetric)
    const field = getLineIntensityField(activeMetric)
    const intensity = data[field]
    const emissionUnit = data.unit ?? 'tCO2e'

    return `
      <div style="font-weight:bold;color:#4db8ff">${data.sectionName} (${data.direction})</div>
      <div>统计周期: ${data.statPeriod ?? ''}</div>
      ${
        typeof data.chainageStartKm === 'number' && typeof data.chainageEndKm === 'number'
          ? `<div>里程: ${data.chainageStartKm}–${data.chainageEndKm} km</div>`
          : ''
      }
      <div>长度: ${data.lengthKm} km</div>
      <div>${metricLabel}: ${intensity} ${metricUnit}</div>
      <div>碳排放量: ${typeof data.carbonEmission === 'number' ? data.carbonEmission.toFixed(1) : data.carbonEmission} ${emissionUnit}</div>
    `
  }

  private formatHeatmapTooltip(data: Record<string, unknown>): string {
    const value = Array.isArray(data.value) ? data.value[2] : data.intensity
    const metricLabel = data.metricLabel ?? getMetricLabel(this.options!.activeMetric)
    const metricUnit = data.metricUnit ?? getMetricUnit(this.options!.activeMetric)
    const coords = Array.isArray(data.value) ? data.value : []

    return `
      <div style="font-weight:bold;color:#4db8ff">线路热力</div>
      <div>统计周期: ${data.statPeriod ?? ''}</div>
      <div>${metricLabel}: ${value} ${metricUnit}</div>
      <div>位置: ${typeof coords[0] === 'number' ? coords[0].toFixed(3) : ''}, ${typeof coords[1] === 'number' ? coords[1].toFixed(3) : ''}</div>
    `
  }
}
