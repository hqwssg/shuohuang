import * as THREE from 'three'
import type { CarbonSurfaceData, VisualOptions, LayoutInfo, HeatmapPalette } from '../types'
import {
  computeMatrixMax,
  computeMatrixMin,
  createHeatmapPalette,
  getHeatmapColor
} from '../utils/colorUtils'
import {
  WORLD_MIN_X,
  WORLD_MAX_X,
  WORLD_MIN_Z,
  WORLD_MAX_Z,
  WORLD_WIDTH,
  WORLD_DEPTH,
  clampWorldMaxY,
  getXByTimeIndex,
  getZByYearIndex,
  getYByValue,
  heatNormalized,
  createNiceTicks
} from '../utils/spatialMapping'
import { createFloorGrid, createLeftWallGrid, createBackWallGrid } from './GridEnclosureBuilder'

export class GeometryBuilder {
  private data: CarbonSurfaceData
  private options: VisualOptions
  private layout: LayoutInfo
  private maxValue: number
  private minValue: number
  private palette: HeatmapPalette
  /** Y 轴网格分段数（与 nice ticks 数量协调） */
  private yWallSegments: number

  private surfaceMesh: THREE.Mesh | null = null
  private wireframeLines: THREE.LineSegments | null = null
  private enclosureRoot: THREE.Group | null = null

  constructor(data: CarbonSurfaceData, options: VisualOptions) {
    this.data = data
    this.options = options
    this.maxValue = computeMatrixMax(data?.values)
    this.minValue = computeMatrixMin(data?.values)
    this.palette = createHeatmapPalette(
      options.colorLow,
      options.colorMidLow,
      options.colorMid,
      options.colorMidHigh,
      options.colorHigh
    )
    this.layout = this.computeLayout()
    const ticks = createNiceTicks(this.maxValue, 6)
    this.yWallSegments = Math.max(5, Math.min(14, ticks.length + 3))
  }

  private computeLayout(): LayoutInfo {
    const yearCount = Array.isArray(this.data.years) ? this.data.years.length : 0
    const timeCount = Array.isArray(this.data.times) ? this.data.times.length : 0

    return {
      minX: WORLD_MIN_X,
      maxX: WORLD_MAX_X,
      minZ: WORLD_MIN_Z,
      maxZ: WORLD_MAX_Z,
      width: WORLD_WIDTH,
      depth: WORLD_DEPTH,
      worldMaxY: clampWorldMaxY(this.options.heightScale),
      labelOffset: 1.2,
      timeCount: Math.max(1, timeCount),
      yearCount: Math.max(1, yearCount)
    }
  }

  public getLayout(): LayoutInfo {
    return this.layout
  }

  public getMaxValue(): number {
    return this.maxValue
  }

  public getMinValue(): number {
    return this.minValue
  }

  public createSurfaceGeometry(): THREE.BufferGeometry {
    const { years, times } = this.data
    const valueMatrix = Array.isArray(this.data.values) ? this.data.values : []
    const yearCount = Array.isArray(years) ? years.length : 0
    const timeCount = Array.isArray(times) ? times.length : 0
    const { worldMaxY, width, depth } = this.layout
    const vmin = this.minValue
    const vmax = this.maxValue === vmin ? vmin + 1e-9 : this.maxValue

    const positions: number[] = []
    const colors: number[] = []
    const indices: number[] = []

    for (let yi = 0; yi < yearCount; yi++) {
      const row = valueMatrix[yi] || []
      for (let xi = 0; xi < timeCount; xi++) {
        const value = Number(row[xi]) || 0
        const x = getXByTimeIndex(xi, Math.max(1, timeCount), width)
        const z = getZByYearIndex(yi, Math.max(1, yearCount), depth)
        const y = getYByValue(value, vmin, vmax, worldMaxY)
        positions.push(x, y, z)

        const hn = heatNormalized(value, vmin, vmax)
        const color = getHeatmapColor(hn, this.palette)
        colors.push(color.r, color.g, color.b)
      }
    }

    for (let yi = 0; yi < yearCount - 1; yi++) {
      for (let xi = 0; xi < timeCount - 1; xi++) {
        const a = yi * timeCount + xi
        const b = a + 1
        const c = a + timeCount
        const d = c + 1
        indices.push(a, c, b)
        indices.push(b, c, d)
      }
    }

    const geometry = new THREE.BufferGeometry()
    geometry.setAttribute('position', new THREE.Float32BufferAttribute(positions, 3))
    geometry.setAttribute('color', new THREE.Float32BufferAttribute(colors, 3))
    geometry.setIndex(indices)
    geometry.computeVertexNormals()

    return geometry
  }

  public createSurfaceMaterial(): THREE.MeshStandardMaterial {
    return new THREE.MeshStandardMaterial({
      vertexColors: true,
      side: THREE.DoubleSide,
      wireframe: false,
      metalness: 0.05,
      roughness: 0.55,
      opacity: 0.96,
      transparent: true,
      flatShading: false
    })
  }

  private addEnclosureGrids(scene: THREE.Scene): void {
    const { minX, maxX, minZ, maxZ, worldMaxY, timeCount, yearCount } = this.layout
    const xSeg = Math.max(2, timeCount)
    const zSeg = Math.max(2, yearCount)
    const ySeg = this.yWallSegments

    this.enclosureRoot = new THREE.Group()

    const floor = createFloorGrid({
      minX,
      maxX,
      minY: 0,
      maxY: 0,
      minZ,
      maxZ,
      xSegments: xSeg,
      zSegments: zSeg,
      color: 0x9bb7c8,
      colorSecondary: 0x5ecaf0,
      opacity: 0.26,
      opacitySecondary: 0.22
    })

    const left = createLeftWallGrid({
      minX,
      maxX,
      minY: 0,
      maxY: worldMaxY,
      minZ,
      maxZ,
      zSegments: zSeg,
      ySegments: ySeg,
      color: 0xa8c7d9,
      colorSecondary: 0x6ec6ff,
      opacity: 0.22,
      opacitySecondary: 0.2
    })

    const back = createBackWallGrid({
      minX,
      maxX,
      minY: 0,
      maxY: worldMaxY,
      minZ,
      maxZ,
      xSegments: xSeg,
      ySegments: ySeg,
      color: 0xa8c7d9,
      colorSecondary: 0x6ec6ff,
      opacity: 0.2,
      opacitySecondary: 0.18
    })

    this.enclosureRoot.add(floor, left, back)
    scene.add(this.enclosureRoot)
  }

  public createWireframe(positions: Float32Array, timeCount: number, yearCount: number): THREE.LineSegments {
    const { width, depth } = this.layout
    const tc = Math.max(1, timeCount)
    const yc = Math.max(1, yearCount)

    const edgePositions: number[] = []

    for (let yi = 0; yi < yc; yi++) {
      for (let xi = 0; xi < tc - 1; xi++) {
        const idx = yi * tc + xi
        const nextIdx = idx + 1
        edgePositions.push(
          getXByTimeIndex(xi, tc, width),
          positions[idx * 3 + 1] || 0,
          getZByYearIndex(yi, yc, depth),
          getXByTimeIndex(xi + 1, tc, width),
          positions[nextIdx * 3 + 1] || 0,
          getZByYearIndex(yi, yc, depth)
        )
      }
    }

    for (let xi = 0; xi < tc; xi++) {
      for (let yi = 0; yi < yc - 1; yi++) {
        const idx = yi * tc + xi
        const nextIdx = idx + tc
        edgePositions.push(
          getXByTimeIndex(xi, tc, width),
          positions[idx * 3 + 1] || 0,
          getZByYearIndex(yi, yc, depth),
          getXByTimeIndex(xi, tc, width),
          positions[nextIdx * 3 + 1] || 0,
          getZByYearIndex(yi + 1, yc, depth)
        )
      }
    }

    const geometry = new THREE.BufferGeometry()
    geometry.setAttribute('position', new THREE.Float32BufferAttribute(edgePositions, 3))

    const material = new THREE.LineBasicMaterial({
      color: new THREE.Color(this.options.gridColor),
      transparent: true,
      opacity: this.options.wireframeOpacity
    })

    return new THREE.LineSegments(geometry, material)
  }

  public getDataIndicesFromVertexIndex(vertexIndex: number): { yearIndex: number; timeIndex: number } {
    const timeCount = Array.isArray(this.data.times) ? this.data.times.length : 1
    const safeTc = Math.max(1, timeCount)
    return {
      yearIndex: Math.floor(vertexIndex / safeTc),
      timeIndex: vertexIndex % safeTc
    }
  }

  /** 仅包围网格（先于轴与曲面） */
  public buildEnclosure(scene: THREE.Scene): void {
    if (this.enclosureRoot) {
      scene.remove(this.enclosureRoot)
      this.enclosureRoot.traverse((obj) => {
        if (obj instanceof THREE.LineSegments) {
          obj.geometry.dispose()
          ;(obj.material as THREE.Material).dispose()
        }
      })
      this.enclosureRoot.clear()
      this.enclosureRoot = null
    }
    this.addEnclosureGrids(scene)
  }

  /** 曲面 + 线框（最后加入场景） */
  public buildSurface(scene: THREE.Scene): void {
    if (this.surfaceMesh) {
      scene.remove(this.surfaceMesh)
      this.surfaceMesh.geometry.dispose()
      ;(this.surfaceMesh.material as THREE.Material).dispose()
      this.surfaceMesh = null
    }
    if (this.wireframeLines) {
      scene.remove(this.wireframeLines)
      this.wireframeLines.geometry.dispose()
      ;(this.wireframeLines.material as THREE.Material).dispose()
      this.wireframeLines = null
    }

    const geometry = this.createSurfaceGeometry()
    const material = this.createSurfaceMaterial()
    this.surfaceMesh = new THREE.Mesh(geometry, material)
    this.surfaceMesh.userData = { isSurface: true }
    scene.add(this.surfaceMesh)

    if (this.options.showWireframe) {
      const pos = geometry.attributes.position.array as Float32Array
      const ty = Array.isArray(this.data.times) ? this.data.times.length : 0
      const yr = Array.isArray(this.data.years) ? this.data.years.length : 0
      this.wireframeLines = this.createWireframe(pos, Math.max(1, ty), Math.max(1, yr))
      scene.add(this.wireframeLines)
    }
  }

  public getSurfaceMesh(): THREE.Mesh | null {
    return this.surfaceMesh
  }

  public dispose(scene: THREE.Scene): void {
    if (this.surfaceMesh) {
      scene.remove(this.surfaceMesh)
      this.surfaceMesh.geometry.dispose()
      ;(this.surfaceMesh.material as THREE.Material).dispose()
      this.surfaceMesh = null
    }

    if (this.wireframeLines) {
      scene.remove(this.wireframeLines)
      this.wireframeLines.geometry.dispose()
      ;(this.wireframeLines.material as THREE.Material).dispose()
      this.wireframeLines = null
    }

    if (this.enclosureRoot) {
      scene.remove(this.enclosureRoot)
      this.enclosureRoot.traverse((obj) => {
        if (obj instanceof THREE.LineSegments) {
          obj.geometry.dispose()
          ;(obj.material as THREE.Material).dispose()
        }
      })
      this.enclosureRoot.clear()
      this.enclosureRoot = null
    }
  }
}
