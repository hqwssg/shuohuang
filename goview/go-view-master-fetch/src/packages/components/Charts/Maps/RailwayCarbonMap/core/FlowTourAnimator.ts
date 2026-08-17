import type * as echarts from 'echarts/core'
import {
  buildFlowRippleGraphicElements,
  type LineFlowTrack
} from '../utils/lineFlowRipple'
import {
  resolveTimelineState,
  type FlowDwellTimelineResult
} from '../utils/flowDwellTimeline'

export interface PointTipTarget {
  seriesIndex: number
  nameToDataIndex: Map<string, number>
}

export interface FlowTourAnimatorOptions {
  showRippleRings?: boolean
  showFlowDot?: boolean
  lineFlowSymbolSize?: number
  onDwellEnter?: (stationName: string) => void
  onDwellLeave?: (stationName: string) => void
}

/**
 * 流动 tour：到站停留 + showTip + graphic 光点/涟漪（替代 lines effect 匀速循环）
 */
export class FlowTourAnimator {
  private rafId = 0
  private lastTs = 0
  private elapsedSec = 0
  private animTimeSec = 0
  private chart: echarts.ECharts | null = null
  private tracks: LineFlowTrack[] = []
  private timeline: FlowDwellTimelineResult | null = null
  private pointTip: PointTipTarget | null = null
  private opts: FlowTourAnimatorOptions = {}
  private activeDwellKey: string | null = null
  private activeHighlightIndex: number | null = null
  private phases = new Map<string, number>()

  start(
    chart: echarts.ECharts,
    tracks: LineFlowTrack[],
    timeline: FlowDwellTimelineResult,
    pointTip: PointTipTarget,
    opts: FlowTourAnimatorOptions = {}
  ): void {
    this.stop()
    if (tracks.length === 0 || timeline.segments.length === 0) return

    this.chart = chart
    this.tracks = tracks
    this.timeline = timeline
    this.pointTip = pointTip
    this.opts = opts
    this.lastTs = 0
    this.elapsedSec = 0
    this.animTimeSec = 0
    this.activeDwellKey = null
    this.phases.clear()
    tracks.forEach(t => this.phases.set(t.sectionId, 0))

    const loop = (ts: number) => {
      if (!this.chart || !this.timeline) return
      if (!this.lastTs) this.lastTs = ts
      const dt = Math.min((ts - this.lastTs) / 1000, 0.05)
      this.lastTs = ts
      this.elapsedSec += dt
      this.animTimeSec += dt

      const state = resolveTimelineState(
        this.timeline.segments,
        this.timeline.totalDurationSec,
        this.elapsedSec
      )

      for (const track of this.tracks) {
        this.phases.set(track.sectionId, state.phase)
      }

      this.syncDwellTooltip(state)

      const elements = buildFlowRippleGraphicElements(
        this.chart,
        this.tracks,
        this.phases,
        this.animTimeSec,
        {
          showRippleRings: this.opts.showRippleRings !== false,
          showFlowDot: this.opts.showFlowDot !== false,
          lineFlowSymbolSize: this.opts.lineFlowSymbolSize ?? 5
        }
      )

      this.chart.setOption(
        { graphic: { elements } },
        { lazyUpdate: true, silent: true, replaceMerge: ['graphic'] }
      )

      this.rafId = requestAnimationFrame(loop)
    }

    this.rafId = requestAnimationFrame(loop)
  }

  private syncDwellTooltip(state: ReturnType<typeof resolveTimelineState>): void {
    if (!this.chart || !this.pointTip) return

    const dwellKey =
      state.inDwell && state.stationName
        ? `${state.segmentIndex}:${state.stationName}`
        : null

    if (dwellKey === this.activeDwellKey) return

    if (this.activeDwellKey) {
      const colon = this.activeDwellKey.indexOf(':')
      const prevName = colon >= 0 ? this.activeDwellKey.slice(colon + 1) : ''
      if (prevName) this.opts.onDwellLeave?.(prevName)
    }

    this.clearPointHighlight()

    if (this.activeDwellKey) {
      this.chart.dispatchAction({ type: 'hideTip' })
    }

    this.activeDwellKey = dwellKey

    if (!dwellKey || !state.stationName) return

    this.opts.onDwellEnter?.(state.stationName)

    const dataIndex = this.pointTip.nameToDataIndex.get(state.stationName)
    if (dataIndex === undefined) return

    this.chart.dispatchAction({
      type: 'highlight',
      seriesIndex: this.pointTip.seriesIndex,
      dataIndex
    })
    this.activeHighlightIndex = dataIndex

    this.chart.dispatchAction({
      type: 'showTip',
      seriesIndex: this.pointTip.seriesIndex,
      dataIndex
    })
  }

  private clearPointHighlight(): void {
    if (!this.chart || !this.pointTip || this.activeHighlightIndex === null) return
    this.chart.dispatchAction({
      type: 'downplay',
      seriesIndex: this.pointTip.seriesIndex,
      dataIndex: this.activeHighlightIndex
    })
    this.activeHighlightIndex = null
  }

  stop(): void {
    if (this.rafId) {
      cancelAnimationFrame(this.rafId)
      this.rafId = 0
    }
    if (this.chart) {
      this.clearPointHighlight()
      if (this.activeDwellKey) {
        this.chart.dispatchAction({ type: 'hideTip' })
      }
      this.chart.setOption(
        { graphic: { elements: [] } },
        { silent: true, replaceMerge: ['graphic'] }
      )
    }
    this.chart = null
    this.tracks = []
    this.timeline = null
    this.pointTip = null
    this.activeDwellKey = null
    this.phases.clear()
    this.lastTs = 0
    this.elapsedSec = 0
    this.animTimeSec = 0
  }
}
