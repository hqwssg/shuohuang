/** 与 src/assets/dashboard/dashPieHoverLabels.ts 同步 */

export const DASH_PIE_TOOLTIP = {
  show: false,
  trigger: 'item'
}

export const DASH_PIE_SERIES_HOVER = {
  label: {
    show: false,
    position: 'outside'
  },
  labelLine: {
    show: false
  },
  emphasis: {
    scale: true,
    scaleSize: 6,
    label: {
      show: true,
      position: 'outside',
      formatter: '{b} {d}%',
      fontSize: 13,
      fontWeight: 'normal',
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
