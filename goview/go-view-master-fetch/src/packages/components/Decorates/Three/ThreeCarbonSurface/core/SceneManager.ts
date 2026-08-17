import * as THREE from 'three'

/** 与 WebGL 透明清屏兼容：支持 transparent、#RRGGBBAA 末两位 00、rgba(,,,0) */
export function isCssTransparentBackground(color: string): boolean {
  const c = color.trim().toLowerCase()
  if (c === '' || c === 'transparent') return true
  if (/^#[0-9a-f]{8}$/i.test(c) && c.slice(-2) === '00') return true
  const m = c.match(
    /^rgba\(\s*[\d.]+\s*,\s*[\d.]+\s*,\s*[\d.]+\s*,\s*([\d.]+)\s*\)$/i
  )
  if (m && parseFloat(m[1]) === 0) return true
  return false
}

export class SceneManager {
  private dom: HTMLElement
  private scene: THREE.Scene
  private camera: THREE.PerspectiveCamera
  private renderer: THREE.WebGLRenderer
  private width: number
  private height: number

  constructor(dom: HTMLElement, width: number, height: number, backgroundColor: string) {
    this.dom = dom
    this.width = width
    this.height = height

    // 创建场景
    this.scene = new THREE.Scene()
    this.applySceneBackground(backgroundColor)

    // 创建相机
    this.camera = new THREE.PerspectiveCamera(52, width / height, 0.1, 5000)

    // 创建渲染器（alpha 使画布可透出大屏底色）
    this.renderer = new THREE.WebGLRenderer({
      antialias: true,
      alpha: true,
      premultipliedAlpha: false
    })
    this.renderer.setPixelRatio(window.devicePixelRatio)
    this.renderer.setSize(width, height)
    this.syncRendererClear(backgroundColor)
    this.renderer.domElement.style.background = 'transparent'
    this.dom.appendChild(this.renderer.domElement)

    // 添加光照
    this.setupLights()
  }

  private setupLights(): void {
    // 环境光
    const ambientLight = new THREE.AmbientLight(0xffffff, 0.65)
    this.scene.add(ambientLight)

    // 方向光
    const dirLight = new THREE.DirectionalLight(0xffffff, 0.75)
    dirLight.position.set(50, 120, 80)
    this.scene.add(dirLight)

    // 补光
    const fillLight = new THREE.DirectionalLight(0x4db8ff, 0.3)
    fillLight.position.set(-50, 20, -50)
    this.scene.add(fillLight)
  }

  public getScene(): THREE.Scene {
    return this.scene
  }

  public getCamera(): THREE.PerspectiveCamera {
    return this.camera
  }

  public getRenderer(): THREE.WebGLRenderer {
    return this.renderer
  }

  public getDomElement(): HTMLCanvasElement {
    return this.renderer.domElement
  }

  private applySceneBackground(color: string): void {
    if (isCssTransparentBackground(color)) {
      this.scene.background = null
    } else {
      this.scene.background = new THREE.Color(color)
    }
  }

  private syncRendererClear(color: string): void {
    if (isCssTransparentBackground(color)) {
      this.renderer.setClearColor(0x000000, 0)
    } else {
      this.renderer.setClearColor(0x000000, 1)
    }
  }

  public updateBackground(color: string): void {
    this.applySceneBackground(color)
    this.syncRendererClear(color)
  }

  public updateSize(width: number, height: number): void {
    if (width <= 0 || height <= 0) return
    
    this.width = width
    this.height = height
    
    this.camera.aspect = width / height
    this.camera.updateProjectionMatrix()
    this.renderer.setSize(width, height)
  }

  public render(): void {
    this.renderer.render(this.scene, this.camera)
  }

  public dispose(): void {
    // 释放渲染器
    this.renderer.dispose()
    
    // 移除 canvas
    const canvas = this.renderer.domElement
    if (canvas.parentNode) {
      canvas.parentNode.removeChild(canvas)
    }

    // 清理场景
    this.scene.traverse((object) => {
      if (object instanceof THREE.Mesh) {
        object.geometry?.dispose()
        if (Array.isArray(object.material)) {
          object.material.forEach(m => m.dispose())
        } else {
          object.material?.dispose()
        }
      }
    })
    this.scene.clear()
  }
}
