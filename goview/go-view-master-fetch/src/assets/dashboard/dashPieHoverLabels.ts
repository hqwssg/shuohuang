/** 碳排放大屏饼图 hover：扇区外侧显示「名称 + 百分比」，环心不显示大字 */

export const DASH_PIE_TOOLTIP = {
  show: false,
  trigger: 'item' as const
}

export const DASH_PIE_SERIES_HOVER = {
  label: {
    show: false,
    position: 'outside' as const
  },
  labelLine: {
    show: false
  },
  emphasis: {
    scale: true,
    scaleSize: 6,
    label: {
      show: true,
      position: 'outside' as const,
      formatter: '{b} {d}%',
      fontSize: 13,
      fontWeight: 'normal' as const,
      color: '#e6f7ff'
    },
    labelLine: {
      show: true,
      length: 10,
      length2: 8,
      lineStyle: { color: 'rgba(230, 247, 255, 0.6)' }
    }
  }
}

export function applyDashPieHoverLabels(seriesItem: Record<string, unknown>): Record<string, unknown> {
  const prevEmphasis = (seriesItem.emphasis as Record<string, unknown>) ?? {}
  const prevEmphasisLabel = (prevEmphasis.label as Record<string, unknown>) ?? {}
  const prevEmphasisLabelLine = (prevEmphasis.labelLine as Record<string, unknown>) ?? {}

  return {
    ...seriesItem,
    label: { ...(seriesItem.label as object), ...DASH_PIE_SERIES_HOVER.label },
    labelLine: { ...(seriesItem.labelLine as object), ...DASH_PIE_SERIES_HOVER.labelLine },
    emphasis: {
      ...prevEmphasis,
      ...DASH_PIE_SERIES_HOVER.emphasis,
      label: { ...prevEmphasisLabel, ...DASH_PIE_SERIES_HOVER.emphasis.label },
      labelLine: { ...prevEmphasisLabelLine, ...DASH_PIE_SERIES_HOVER.emphasis.labelLine }
    }
  }
}
