import { nextTick } from 'vue'

const ENTRANCE_MS = 1000

type ChartInstance = {
  clear?: () => void
  setOption: (option: Record<string, unknown>, opts?: { notMerge?: boolean }) => void
}

function buildAnimatedSeries(series: unknown[]) {
  return series.map(s => {
    const item = { ...(s as Record<string, unknown>) }
    item.animation = true
    item.animationDuration = ENTRANCE_MS
    item.animationEasing = 'cubicOut'
    item.animationDurationUpdate = 0
    if (item.type === 'pie') {
      item.animationType = 'scale'
    }
    return item
  })
}

/** 与首次绘制一致的 ECharts 进场动画（clear + notMerge 强制重播） */
export function replayChartEntrance(
  vChartRef: { value?: unknown },
  optionValue: Record<string, unknown>
) {
  void nextTick(() => {
    const instance = vChartRef?.value as ChartInstance | null | undefined
    if (!instance?.setOption) return

    const series = Array.isArray(optionValue.series) ? optionValue.series : []
    instance.clear?.()
    instance.setOption(
      {
        ...optionValue,
        animation: true,
        animationDuration: ENTRANCE_MS,
        animationEasing: 'cubicOut',
        animationDurationUpdate: 0,
        series: buildAnimatedSeries(series)
      },
      { notMerge: true }
    )
  })
}
