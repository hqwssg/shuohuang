import * as THREE from 'three'

/** 任务单 §7：网格平面参数 */
export type GridPlaneOptions = {
  minX: number
  maxX: number
  minY: number
  maxY: number
  minZ: number
  maxZ: number
  xSegments?: number
  ySegments?: number
  zSegments?: number
  color?: number | string
  colorSecondary?: number | string
  opacity?: number
  opacitySecondary?: number
}

function lineMaterial(color: THREE.ColorRepresentation, opacity: number): THREE.LineBasicMaterial {
  return new THREE.LineBasicMaterial({
    color: new THREE.Color(color),
    transparent: true,
    opacity,
    depthWrite: false
  })
}

function pushLine(pos: number[], x1: number, y1: number, z1: number, x2: number, y2: number, z2: number): void {
  pos.push(x1, y1, z1, x2, y2, z2)
}

/**
 * XZ 底面：沿 Z 的竖线 + 沿 X 的横线（§15 Floor）
 */
export function createFloorGrid(opts: GridPlaneOptions): THREE.Group {
  const { minX, maxX, minZ, maxZ } = opts
  const xSeg = Math.max(2, opts.xSegments ?? 12)
  const zSeg = Math.max(2, opts.zSegments ?? 8)
  const y = 0
  const c1 = opts.color ?? 0x9bb7c8
  const c2 = opts.colorSecondary ?? 0x5ecaf0
  const o1 = opts.opacity ?? 0.24
  const o2 = opts.opacitySecondary ?? 0.2

  const posZ: number[] = []
  for (let i = 0; i <= xSeg; i++) {
    const t = i / xSeg
    const x = minX + (maxX - minX) * t
    pushLine(posZ, x, y, minZ, x, y, maxZ)
  }
  const geoZ = new THREE.BufferGeometry()
  geoZ.setAttribute('position', new THREE.Float32BufferAttribute(posZ, 3))
  const linesZ = new THREE.LineSegments(geoZ, lineMaterial(c1, o1))

  const posX: number[] = []
  for (let i = 0; i <= zSeg; i++) {
    const t = i / zSeg
    const z = minZ + (maxZ - minZ) * t
    pushLine(posX, minX, y, z, maxX, y, z)
  }
  const geoX = new THREE.BufferGeometry()
  geoX.setAttribute('position', new THREE.Float32BufferAttribute(posX, 3))
  const linesX = new THREE.LineSegments(geoX, lineMaterial(c2, o2))

  const g = new THREE.Group()
  g.add(linesZ, linesX)
  return g
}

/**
 * YZ 左墙 x=minX（§15 Left Wall）
 */
export function createLeftWallGrid(opts: GridPlaneOptions): THREE.Group {
  const { minX, minY, maxY, minZ, maxZ } = opts
  const zSeg = Math.max(2, opts.zSegments ?? 8)
  const ySeg = Math.max(2, opts.ySegments ?? 6)
  const c1 = opts.color ?? 0xa8c7d9
  const c2 = opts.colorSecondary ?? 0x6ec6ff
  const o1 = opts.opacity ?? 0.2
  const o2 = opts.opacitySecondary ?? 0.18

  const vert: number[] = []
  for (let i = 0; i <= zSeg; i++) {
    const t = i / zSeg
    const z = minZ + (maxZ - minZ) * t
    pushLine(vert, minX, minY, z, minX, maxY, z)
  }
  const geoV = new THREE.BufferGeometry()
  geoV.setAttribute('position', new THREE.Float32BufferAttribute(vert, 3))
  const linesV = new THREE.LineSegments(geoV, lineMaterial(c1, o1))

  const horiz: number[] = []
  for (let i = 0; i <= ySeg; i++) {
    const t = i / ySeg
    const y = minY + (maxY - minY) * t
    pushLine(horiz, minX, y, minZ, minX, y, maxZ)
  }
  const geoH = new THREE.BufferGeometry()
  geoH.setAttribute('position', new THREE.Float32BufferAttribute(horiz, 3))
  const linesH = new THREE.LineSegments(geoH, lineMaterial(c2, o2))

  const g = new THREE.Group()
  g.add(linesV, linesH)
  return g
}

/**
 * XY 后墙 z=maxZ（§15 Back Wall）
 */
export function createBackWallGrid(opts: GridPlaneOptions): THREE.Group {
  const { minX, maxX, minY, maxY, maxZ } = opts
  const xSeg = Math.max(2, opts.xSegments ?? 12)
  const ySeg = Math.max(2, opts.ySegments ?? 6)
  const c1 = opts.color ?? 0xa8c7d9
  const c2 = opts.colorSecondary ?? 0x6ec6ff
  const o1 = opts.opacity ?? 0.18
  const o2 = opts.opacitySecondary ?? 0.16

  const vert: number[] = []
  for (let i = 0; i <= xSeg; i++) {
    const t = i / xSeg
    const x = minX + (maxX - minX) * t
    pushLine(vert, x, minY, maxZ, x, maxY, maxZ)
  }
  const geoV = new THREE.BufferGeometry()
  geoV.setAttribute('position', new THREE.Float32BufferAttribute(vert, 3))
  const linesV = new THREE.LineSegments(geoV, lineMaterial(c1, o1))

  const horiz: number[] = []
  for (let i = 0; i <= ySeg; i++) {
    const t = i / ySeg
    const y = minY + (maxY - minY) * t
    pushLine(horiz, minX, y, maxZ, maxX, y, maxZ)
  }
  const geoH = new THREE.BufferGeometry()
  geoH.setAttribute('position', new THREE.Float32BufferAttribute(horiz, 3))
  const linesH = new THREE.LineSegments(geoH, lineMaterial(c2, o2))

  const g = new THREE.Group()
  g.add(linesV, linesH)
  return g
}
