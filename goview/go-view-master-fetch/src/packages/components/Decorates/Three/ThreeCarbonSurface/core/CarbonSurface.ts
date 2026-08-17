import type { CarbonSurfaceParams, CarbonSurfaceData, VisualOptions } from '../types'
import { DEFAULT_VISUAL_OPTIONS } from '../types'
import { CSS2DRenderer } from 'three/examples/jsm/renderers/CSS2DRenderer.js'
import { SceneManager } from './SceneManager'
import { GeometryBuilder } from './GeometryBuilder'
import { AxisLabelManager } from './AxisLabelManager'
import { ControlsManager } from './ControlsManager'
import { RaycasterManager, type TooltipCallback, type ClickCallback } from '../interaction/RaycasterManager'

export class CarbonSurface {
  // 核心模块
  private sceneManager: SceneManager
  private geometryBuilder: GeometryBuilder
  private axisLabelManager: AxisLabelManager
  private controlsManager: ControlsManager
  private raycasterManager: RaycasterManager
  private labelRenderer: CSS2DRenderer
  private rootDom: HTMLElement

  // 状态
  private data: CarbonSurfaceData
  private options: VisualOptions
  private animationId = 0
  private isDisposed = false

  // 回调
  private onHoverCallback: TooltipCallback | null = null
  private onClickCallback: ClickCallback | null = null

  constructor(params: CarbonSurfaceParams) {
    const { dom, data, width, height, options = {} } = params
    
    // 合并选项
    this.options = { ...DEFAULT_VISUAL_OPTIONS, ...options } as VisualOptions
    this.data = data
    this.rootDom = dom

    // 1. 创建场景管理器
    this.sceneManager = new SceneManager(
      dom,
      width,
      height,
      this.options.backgroundColor
    )

    this.labelRenderer = new CSS2DRenderer()
    this.labelRenderer.setSize(width, height)
    const labelEl = this.labelRenderer.domElement
    labelEl.style.position = 'absolute'
    labelEl.style.top = '0'
    labelEl.style.left = '0'
    labelEl.style.pointerEvents = 'none'
    dom.appendChild(labelEl)

    // 2. 创建几何构建器
    this.geometryBuilder = new GeometryBuilder(data, this.options)

    // 3. 创建坐标轴标签管理器
    this.axisLabelManager = new AxisLabelManager(
      data,
      this.options,
      this.geometryBuilder.getLayout()
    )

    // 4. 创建控制器
    this.controlsManager = new ControlsManager(
      this.sceneManager.getCamera(),
      this.sceneManager.getDomElement(),
      this.geometryBuilder.getLayout(),
      this.options.enableIntroSpin !== false
    )

    // 5. 创建射线检测器
    this.raycasterManager = new RaycasterManager(
      this.sceneManager.getCamera(),
      this.sceneManager.getDomElement(),
      data,
      this.options,
      this.geometryBuilder.getMaxValue(),
      this.geometryBuilder.getMinValue(),
      this.geometryBuilder.getLayout()
    )

    // 6. 构建场景
    this.buildScene()

    // 7. 开始动画
    this.animate()
  }

  /**
   * 构建完整场景
   */
  private buildScene(): void {
    const scene = this.sceneManager.getScene()
    const maxValue = this.geometryBuilder.getMaxValue()
    const minValue = this.geometryBuilder.getMinValue()

    this.geometryBuilder.dispose(scene)

    this.geometryBuilder.buildEnclosure(scene)
    this.axisLabelManager.build(scene, minValue, maxValue)
    this.geometryBuilder.buildSurface(scene)

    // 设置射线检测目标
    this.raycasterManager.setSurfaceMesh(this.geometryBuilder.getSurfaceMesh())
    this.raycasterManager.setCallbacks(
      (data, pos) => this.onHoverCallback?.(data, pos),
      (data) => this.onClickCallback?.(data)
    )
  }

  /**
   * 动画循环
   */
  private animate = (): void => {
    if (this.isDisposed) return

    this.animationId = requestAnimationFrame(this.animate)
    // dispose 可能在本帧 rAF 已排队之后、update 之前发生，需再次检查
    if (this.isDisposed) return
    this.controlsManager.update()
    if (this.isDisposed) return
    this.sceneManager.render()
    this.labelRenderer.render(this.sceneManager.getScene(), this.sceneManager.getCamera())
  }

  /**
   * 设置事件回调
   */
  public setEventCallbacks(onHover: TooltipCallback, onClick: ClickCallback): void {
    this.onHoverCallback = onHover
    this.onClickCallback = onClick
  }

  /**
   * 更新数据
   */
  public updateData(data: CarbonSurfaceData, opts?: { replayIntro?: boolean }): void {
    if (this.isDisposed) return

    this.data = data
    const scene = this.sceneManager.getScene()

    // 先卸下旧网格，避免替换 GeometryBuilder 后旧 Mesh 残留
    this.geometryBuilder.dispose(scene)
    this.geometryBuilder = new GeometryBuilder(data, this.options)
    this.axisLabelManager.syncFrom(data, this.geometryBuilder.getLayout())

    // 更新射线检测器
    this.raycasterManager.updateData(
      data,
      this.geometryBuilder.getMaxValue(),
      this.geometryBuilder.getMinValue(),
      this.geometryBuilder.getLayout()
    )
    this.raycasterManager.setSurfaceMesh(null)

    // 重建场景（保留 OrbitControls，避免打断进场旋转）
    this.buildScene()

    if (opts?.replayIntro) {
      this.replayIntroSpin()
    }
  }

  public replayIntroSpin(): void {
    if (this.isDisposed) return
    if (this.options.enableIntroSpin === false) return
    this.controlsManager.startIntroSpin()
  }

  /**
   * 更新选项
   */
  public updateOptions(options: Partial<VisualOptions>): void {
    if (this.isDisposed) return
    
    this.options = { ...this.options, ...options }

    // 更新背景
    this.sceneManager.updateBackground(this.options.backgroundColor)

    // 重建几何（颜色/线框可能变化）
    const scene = this.sceneManager.getScene()
    this.geometryBuilder.dispose(scene)
    this.geometryBuilder = new GeometryBuilder(this.data, this.options)
    this.axisLabelManager.syncFrom(this.data, this.geometryBuilder.getLayout())
    this.buildScene()
  }

  /**
   * 更新尺寸
   */
  public updateSize(width: number, height: number): void {
    if (this.isDisposed) return
    this.sceneManager.updateSize(width, height)
    this.labelRenderer.setSize(width, height)
  }

  /**
   * 重置相机
   */
  public resetCamera(): void {
    this.controlsManager.reset()
  }

  /**
   * 获取相机位置
   */
  public getCameraPosition(): { x: number; y: number; z: number } {
    return this.controlsManager.getCameraPosition()
  }

  /**
   * 设置相机位置
   */
  public setCameraPosition(pos: { x: number; y: number; z: number }): void {
    this.controlsManager.setCameraPosition(pos)
  }

  /**
   * 获取最大排放量值
   */
  public getMaxValue(): number {
    return this.geometryBuilder.getMaxValue()
  }

  public getMinValue(): number {
    return this.geometryBuilder.getMinValue()
  }

  /**
   * 销毁
   */
  public dispose(): void {
    if (this.isDisposed) return

    this.isDisposed = true
    // 先取消下一帧，避免与 dispose 并发执行 update/render
    cancelAnimationFrame(this.animationId)
    this.animationId = 0

    // 释放射线检测器
    this.raycasterManager.dispose()

    // 释放控制器
    this.controlsManager.dispose()

    // 释放轴标签
    this.axisLabelManager.dispose(this.sceneManager.getScene())

    // 释放几何体
    this.geometryBuilder.dispose(this.sceneManager.getScene())

    // 释放场景管理器
    this.sceneManager.dispose()

    if (this.rootDom.contains(this.labelRenderer.domElement)) {
      this.rootDom.removeChild(this.labelRenderer.domElement)
    }

    // 清理回调
    this.onHoverCallback = null
    this.onClickCallback = null
  }
}
