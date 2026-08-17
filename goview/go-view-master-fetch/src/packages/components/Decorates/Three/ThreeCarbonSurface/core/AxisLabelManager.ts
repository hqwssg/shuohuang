import * as THREE from 'three'
import { CSS2DObject } from 'three/examples/jsm/renderers/CSS2DRenderer.js'
import type { CarbonSurfaceData, VisualOptions, LayoutInfo } from '../types'
import {
  getXByTimeIndex,
  getZByYearIndex,
  getYByValue,
  createNiceTicks
} from '../utils/spatialMapping'

export class AxisLabelManager {
  private data: CarbonSurfaceData
  private options: VisualOptions
  private layout: LayoutInfo
  private axisGroup: THREE.Group
  private labelsGroup: THREE.Group

  constructor(data: CarbonSurfaceData, options: VisualOptions, layout: LayoutInfo) {
    this.data = data
    this.options = options
    this.layout = layout
    this.axisGroup = new THREE.Group()
    this.labelsGroup = new THREE.Group()
  }

  private axisColor(): THREE.Color {
    return new THREE.Color(this.options.axisColor || '#2EC7FF')
  }

  private createAxisLine(start: THREE.Vector3, end: THREE.Vector3): THREE.Line {
    const geometry = new THREE.BufferGeometry().setFromPoints([start, end])
    const material = new THREE.LineBasicMaterial({
      color: this.axisColor(),
      transparent: true,
      opacity: 0.92
    })
    return new THREE.Line(geometry, material)
  }

  private makeTickLabel(text: string, position: THREE.Vector3): CSS2DObject {
    const div = document.createElement('div')
    div.className = 'carbon-axis-label'
    div.textContent = text
    div.style.pointerEvents = 'none'
    const obj = new CSS2DObject(div)
    obj.position.copy(position)
    return obj
  }

  private makeTitleLabel(text: string, position: THREE.Vector3): CSS2DObject {
    const div = document.createElement('div')
    div.className = 'carbon-axis-title'
    div.textContent = text
    div.style.pointerEvents = 'none'
    const obj = new CSS2DObject(div)
    obj.position.copy(position)
    return obj
  }

  private buildAxes(): void {
    const { minX, maxX, minZ, maxZ, worldMaxY } = this.layout

    const xStart = new THREE.Vector3(minX, 0, minZ)
    const xEnd = new THREE.Vector3(maxX, 0, minZ)
    this.axisGroup.add(this.createAxisLine(xStart, xEnd))

    const zStart = new THREE.Vector3(maxX, 0, minZ)
    const zEnd = new THREE.Vector3(maxX, 0, maxZ)
    this.axisGroup.add(this.createAxisLine(zStart, zEnd))

    const yStart = new THREE.Vector3(minX, 0, minZ)
    const yEnd = new THREE.Vector3(minX, worldMaxY, minZ)
    this.axisGroup.add(this.createAxisLine(yStart, yEnd))
  }

  private buildXAxisLabels(): void {
    const { minZ, width, labelOffset, timeCount } = this.layout
    const { times } = this.data
    const tc = Math.max(1, timeCount)

    for (let i = 0; i < tc; i++) {
      const x = getXByTimeIndex(i, tc, width)
      const text = times[i] != null ? String(times[i]) : String(i + 1)
      const pos = new THREE.Vector3(x, -labelOffset, minZ - labelOffset)
      this.labelsGroup.add(this.makeTickLabel(text, pos))
    }

    const titlePos = new THREE.Vector3(0, -labelOffset * 2, minZ - labelOffset * 2)
    this.labelsGroup.add(this.makeTitleLabel('月份（1-12）', titlePos))
  }

  private buildZAxisLabels(): void {
    const { maxX, minZ, depth, labelOffset, yearCount } = this.layout
    const { years } = this.data
    const yc = Math.max(1, yearCount)

    for (let j = 0; j < yc; j++) {
      const z = getZByYearIndex(j, yc, depth)
      const year = years[j]
      const text = year != null ? String(year) : String(j)
      const pos = new THREE.Vector3(maxX + labelOffset, -labelOffset, z)
      this.labelsGroup.add(this.makeTickLabel(text, pos))
    }

    const titlePos = new THREE.Vector3(maxX + labelOffset * 2, -labelOffset * 2, 0)
    this.labelsGroup.add(this.makeTitleLabel('年份', titlePos))
  }

  private buildYAxisLabels(minValue: number, maxValue: number): void {
    const { minX, minZ, worldMaxY, labelOffset } = this.layout
    const vmax = maxValue === minValue ? minValue + 1e-9 : maxValue
    const ticks = createNiceTicks(maxValue, 6)

    for (const tick of ticks) {
      const y = getYByValue(tick, minValue, vmax, worldMaxY)
      const pos = new THREE.Vector3(minX - labelOffset, y, minZ)
      const text =
        tick >= 1000 ? `${(tick / 1000).toFixed(1)}k` : String(Math.round(tick))
      this.labelsGroup.add(this.makeTickLabel(text, pos))
    }

    const unit = (this.data.unit || '吨').replace(/CO2/gi, 'CO₂')
    const titlePos = new THREE.Vector3(minX - labelOffset * 2, worldMaxY + labelOffset, minZ)
    this.labelsGroup.add(this.makeTitleLabel(`碳排放量（${unit}）`, titlePos))
  }

  public syncFrom(data: CarbonSurfaceData, layout: LayoutInfo): void {
    this.data = data
    this.layout = layout
  }

  public build(scene: THREE.Scene, minValue: number, maxValue: number): void {
    this.dispose(scene)

    this.axisGroup = new THREE.Group()
    this.labelsGroup = new THREE.Group()

    this.buildAxes()
    this.buildXAxisLabels()
    this.buildZAxisLabels()
    this.buildYAxisLabels(minValue, maxValue)

    scene.add(this.axisGroup)
    scene.add(this.labelsGroup)
  }

  public dispose(scene: THREE.Scene): void {
    if (this.axisGroup.parent === scene) {
      scene.remove(this.axisGroup)
    }
    this.axisGroup.traverse((obj) => {
      if (obj instanceof THREE.Line) {
        obj.geometry.dispose()
        ;(obj.material as THREE.Material).dispose()
      }
    })
    this.axisGroup.clear()

    if (this.labelsGroup.parent === scene) {
      scene.remove(this.labelsGroup)
    }
    this.labelsGroup.traverse((obj) => {
      if (obj instanceof CSS2DObject && obj.element) {
        obj.element.remove()
      }
    })
    this.labelsGroup.clear()
  }
}
