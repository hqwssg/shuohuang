import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls'
import type { LayoutInfo } from '../types'
import { INTRO_SPIN_DURATION_MS } from '../constants'

export class ControlsManager {
  private controls: OrbitControls
  private camera: THREE.PerspectiveCamera
  private rendererDom: HTMLElement
  private initialTarget: THREE.Vector3
  private initialPosition: THREE.Vector3
  private introSpinning = false
  private introStartMs = 0
  private introStartAzimuth = 0
  private introRadius = 0
  private introPolar = 0
  private introCenter = new THREE.Vector3()

  constructor(
    camera: THREE.PerspectiveCamera,
    rendererDom: HTMLElement,
    _layout: LayoutInfo,
    introEnabled = true
  ) {
    this.camera = camera
    this.rendererDom = rendererDom

    this.controls = new OrbitControls(camera, rendererDom)
    this.controls.enableDamping = true
    this.controls.dampingFactor = 0.05
    this.controls.enableZoom = true
    this.controls.enablePan = false
    this.controls.enableRotate = true
    this.controls.minDistance = 8
    this.controls.maxDistance = 28
    this.controls.minPolarAngle = Math.PI / 5
    this.controls.maxPolarAngle = Math.PI / 2.25
    this.controls.autoRotate = false

    this.setInitialView()

    this.initialTarget = this.controls.target.clone()
    this.initialPosition = camera.position.clone()

    if (introEnabled) {
      this.startIntroSpin()
    }
  }

  private setInitialView(): void {
    this.camera.position.set(12, 9, 13)
    this.camera.lookAt(0, 2.5, 0)
    this.controls.target.set(0, 2.5, 0)
    this.controls.update()
  }

  /** 进场：绕目标水平快速旋转一周（固定 0.8s），结束后回到初始视角 */
  public startIntroSpin(): void {
    const offset = new THREE.Vector3().subVectors(this.camera.position, this.controls.target)
    this.introRadius = Math.max(offset.length(), 0.001)
    this.introPolar = Math.acos(THREE.MathUtils.clamp(offset.y / this.introRadius, -1, 1))
    this.introStartAzimuth = Math.atan2(offset.x, offset.z)
    this.introCenter.copy(this.controls.target)
    this.introSpinning = true
    this.introStartMs = performance.now()
    this.controls.enabled = false
  }

  private applyOrbitPosition(azimuth: number): void {
    const sinP = Math.sin(this.introPolar)
    const x = this.introRadius * sinP * Math.sin(azimuth)
    const y = this.introRadius * Math.cos(this.introPolar)
    const z = this.introRadius * sinP * Math.cos(azimuth)
    this.camera.position.set(
      this.introCenter.x + x,
      this.introCenter.y + y,
      this.introCenter.z + z
    )
    this.camera.lookAt(this.introCenter)
  }

  public update(): void {
    if (this.introSpinning) {
      const elapsed = performance.now() - this.introStartMs
      const t = Math.min(elapsed / INTRO_SPIN_DURATION_MS, 1)
      const azimuth = this.introStartAzimuth - t * Math.PI * 2
      this.applyOrbitPosition(azimuth)
      if (t >= 1) {
        this.introSpinning = false
        this.controls.enabled = true
        this.reset()
      }
      return
    }
    this.controls.update()
  }

  public reset(): void {
    this.camera.position.copy(this.initialPosition)
    this.controls.target.copy(this.initialTarget)
    this.controls.update()
  }

  public getCameraPosition(): { x: number; y: number; z: number } {
    return {
      x: this.camera.position.x,
      y: this.camera.position.y,
      z: this.camera.position.z
    }
  }

  public setCameraPosition(pos: { x: number; y: number; z: number }): void {
    this.camera.position.set(pos.x, pos.y, pos.z)
    this.controls.update()
  }

  public dispose(): void {
    this.controls.dispose()
  }
}
