import * as THREE from 'three'
import type { CarbonSurfaceData, VisualOptions, TooltipData, LayoutInfo } from '../types'
import throttle from 'lodash/throttle'
import {
  inverseTimeIndexFromX,
  inverseYearIndexFromZ,
  heatNormalized
} from '../utils/spatialMapping'

export type TooltipCallback = (data: TooltipData | null, position: { x: number; y: number }) => void
export type ClickCallback = (data: TooltipData) => void

export class RaycasterManager {
  private raycaster: THREE.Raycaster
  private mouse: THREE.Vector2
  private camera: THREE.PerspectiveCamera
  private rendererDom: HTMLElement
  private data: CarbonSurfaceData
  private options: VisualOptions
  private surfaceMesh: THREE.Mesh | null = null
  private maxValue: number
  private minValue: number
  private layout: LayoutInfo

  private onHover: TooltipCallback | null = null
  private onClick: ClickCallback | null = null

  private isHovering = false
  private lastIntersect: THREE.Intersection | null = null

  private disposed = false
  private readonly throttledMouseMove: ReturnType<typeof throttle>
  private readonly boundClick: (e: MouseEvent) => void
  private readonly boundMouseLeave: () => void

  constructor(
    camera: THREE.PerspectiveCamera,
    rendererDom: HTMLElement,
    data: CarbonSurfaceData,
    options: VisualOptions,
    maxValue: number,
    minValue: number,
    layout: LayoutInfo
  ) {
    this.raycaster = new THREE.Raycaster()
    this.mouse = new THREE.Vector2()
    this.camera = camera
    this.rendererDom = rendererDom
    this.data = data
    this.options = options
    this.maxValue = maxValue
    this.minValue = minValue
    this.layout = layout

    this.throttledMouseMove = throttle(this.handleMouseMoveInner.bind(this), 50)
    this.boundClick = (e: MouseEvent) => this.handleClick(e)
    this.boundMouseLeave = () => this.handleMouseLeave()

    this.rendererDom.addEventListener('mousemove', this.throttledMouseMove as EventListener)
    this.rendererDom.addEventListener('click', this.boundClick)
    this.rendererDom.addEventListener('mouseleave', this.boundMouseLeave)
  }

  public setSurfaceMesh(mesh: THREE.Mesh | null): void {
    this.surfaceMesh = mesh
  }

  public setCallbacks(onHover: TooltipCallback, onClick: ClickCallback): void {
    this.onHover = onHover
    this.onClick = onClick
  }

  public updateData(data: CarbonSurfaceData, maxValue: number, minValue: number, layout: LayoutInfo): void {
    this.data = data
    this.maxValue = maxValue
    this.minValue = minValue
    this.layout = layout
  }

  private handleMouseMoveInner(event: MouseEvent): void {
    if (this.disposed) return
    if (!this.surfaceMesh || !this.options.enableTooltip) return

    const rect = this.rendererDom.getBoundingClientRect()
    this.mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
    this.mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1

    this.raycaster.setFromCamera(this.mouse, this.camera)
    const intersects = this.raycaster.intersectObject(this.surfaceMesh)

    if (intersects.length > 0) {
      const intersect = intersects[0]
      this.lastIntersect = intersect
      this.isHovering = true

      if (this.options.tooltipTrigger !== 'click' && this.onHover) {
        const data = this.getTooltipData(intersect)
        this.onHover(data, { x: event.clientX - rect.left, y: event.clientY - rect.top })
      }
    } else {
      this.isHovering = false
      this.lastIntersect = null

      if (this.onHover) {
        this.onHover(null, { x: 0, y: 0 })
      }
    }
  }

  private handleClick(_event: MouseEvent): void {
    if (this.disposed) return
    if (!this.options.enableTooltip) return

    if (this.options.tooltipTrigger === 'click' || this.options.tooltipTrigger === 'mixed') {
      if (this.lastIntersect && this.onClick) {
        const data = this.getTooltipData(this.lastIntersect)
        this.onClick(data)
      }
    }
  }

  private handleMouseLeave(): void {
    if (this.disposed) return
    this.isHovering = false
    this.lastIntersect = null

    if (this.onHover) {
      this.onHover(null, { x: 0, y: 0 })
    }
  }

  private getDataIndicesFromIntersection(intersect: THREE.Intersection): {
    yearIndex: number
    timeIndex: number
  } {
    const p = intersect.point
    const timeCount = Math.max(1, Array.isArray(this.data.times) ? this.data.times.length : 1)
    const yearCount = Math.max(1, Array.isArray(this.data.years) ? this.data.years.length : 1)
    const { width, depth } = this.layout

    const timeIndex = inverseTimeIndexFromX(p.x, timeCount, width)
    const yearIndex = inverseYearIndexFromZ(p.z, yearCount, depth)

    return { yearIndex, timeIndex }
  }

  private getTooltipData(intersect: THREE.Intersection): TooltipData {
    const { yearIndex, timeIndex } = this.getDataIndicesFromIntersection(intersect)

    const years = Array.isArray(this.data.years) ? this.data.years : []
    const times = Array.isArray(this.data.times) ? this.data.times : []
    const values = Array.isArray(this.data.values) ? this.data.values : []

    const year = years[yearIndex] ?? 0
    const time = String(times[timeIndex] ?? '')
    const value = Number(values[yearIndex]?.[timeIndex]) || 0
    const vmin = this.minValue
    const vmax = this.maxValue === vmin ? vmin + 1e-9 : this.maxValue

    return {
      year,
      time,
      value,
      unit: this.data.unit || 'tCO2e',
      normalizedValue: heatNormalized(value, vmin, vmax)
    }
  }

  public dispose(): void {
    if (this.disposed) return
    this.disposed = true
    this.throttledMouseMove.cancel()
    this.rendererDom.removeEventListener('mousemove', this.throttledMouseMove as EventListener)
    this.rendererDom.removeEventListener('click', this.boundClick)
    this.rendererDom.removeEventListener('mouseleave', this.boundMouseLeave)
    this.onHover = null
    this.onClick = null
    this.surfaceMesh = null
    this.lastIntersect = null
  }
}
