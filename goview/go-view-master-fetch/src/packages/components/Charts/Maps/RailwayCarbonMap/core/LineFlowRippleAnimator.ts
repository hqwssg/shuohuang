import type * as echarts from 'echarts/core'
import {
  buildFlowRippleGraphicElements,
  createInitialPhases,
  tickFlowPhases,
  type LineFlowTrack
} from '../utils/lineFlowRipple'

/**
 * 仅更新 graphic 涟漪环，不触碰 lines series，避免 OSM effect 拖尾抖动。
 */
export class LineFlowRippleAnimator {
  private rafId = 0
  private lastTs = 0
  private animTimeSec = 0
  private phases = new Map<string, number>()
  private chart: echarts.ECharts | null = null
  private tracks: LineFlowTrack[] = []
  private lineFlowSpeed = 40

  start(
    chart: echarts.ECharts,
    tracks: LineFlowTrack[],
    lineFlowSpeed: number,
    opts: { showRippleRings?: boolean } = {}
  ): void {
    this.stop()
    if (tracks.length === 0) return

    this.chart = chart
    this.tracks = tracks
    this.lineFlowSpeed = lineFlowSpeed
    this.phases = createInitialPhases(tracks)
    this.lastTs = 0
    this.animTimeSec = 0
    const graphicOpts = opts

    const loop = (ts: number) => {
      if (!this.chart) return
      if (!this.lastTs) this.lastTs = ts
      const dt = Math.min((ts - this.lastTs) / 1000, 0.05)
      this.lastTs = ts
      this.animTimeSec += dt

      tickFlowPhases(this.phases, this.tracks, dt, this.lineFlowSpeed)

      const elements = buildFlowRippleGraphicElements(
        this.chart,
        this.tracks,
        this.phases,
        this.animTimeSec,
        graphicOpts
      )

      this.chart.setOption(
        { graphic: { elements } },
        { lazyUpdate: true, silent: true, replaceMerge: ['graphic'] }
      )

      this.rafId = requestAnimationFrame(loop)
    }

    this.rafId = requestAnimationFrame(loop)
  }

  stop(): void {
    if (this.rafId) {
      cancelAnimationFrame(this.rafId)
      this.rafId = 0
    }
    if (this.chart) {
      this.chart.setOption(
        { graphic: { elements: [] } },
        { silent: true, replaceMerge: ['graphic'] }
      )
    }
    this.chart = null
    this.tracks = []
    this.phases.clear()
    this.lastTs = 0
    this.animTimeSec = 0
  }
}
