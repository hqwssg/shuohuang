# Scope 切换进场动画 — 设计说明

**日期：** 2026-05-24  
**项目：** 朔黄铁路碳排放全景大屏  
**依赖：** `2026-05-24-station-scope-linkage-design.md`（已实现 scope 联动）

## 背景

站点 scope 联动已能切换 KPI、3D 曲面、折线、饼图数据，但换数据时多数组件**不再播放**与首次加载一致的进场动画（3D 仅首屏旋转、KPI 从旧值滚到新值、ECharts 无重绘动画）。

## 已确认需求

| 决策项 | 结论 |
|--------|------|
| 动画标准 | **A**：与首次打开大屏完全一致 |
| 快速连切 | **A**：立即打断并重播，不等待上一段结束 |
| 联动组件 | KPI×4、3D、折线、饼图（排行表不变） |
| Tab 切结构 | 仅换饼图 dataset，**不**触发完整进场（仅 scope 变化时重播） |

## 各组件「首次进场」定义

| 组件 | 首次行为 | scope 切换重播 |
|------|----------|----------------|
| KPI（Number） | `from=0`，`dur=3s` 滚到目标 | 同样：`from=0` → 新目标，duration 3000ms |
| 3D（ThreeCarbonSurface） | 0.8s 水平旋转一周 | `updateData` 后 `startIntroSpin()` |
| 折线（LineCommon） | ECharts 线条绘制动画 | `setOption(..., true)` + animation ~1000ms |
| 饼图（PieCommon） | 扇区 scale 入场 | 同上，保留当前 Tab structureType |

## 架构

### scopeGeneration

`setStationScope` 在**成功写入**（非重复 set）时：

```typescript
holder.Params.scopeGeneration = (Number(holder.Params.scopeGeneration) || 0) + 1
```

初始模板 Params：`{ dataScope: 'overall', stationName: '', scopeGeneration: 0 }`

### useStationScopeSync 分流

| 触发源 | 行为 |
|--------|------|
| `dataScope` / `stationName` / `scopeGeneration` 变化 | `apply(payload)` + `replayEntrance()` |
| 仅 `structureType` 变化（饼图 Tab） | 仅 `apply(payload)`，不重播进场 |

### 打断策略

- 新 scope 写入 → 各组件 `replayEntrance` 立即执行  
- `ControlsManager.startIntroSpin()` 可重入，中途调用即重置 0.8s 计时  
- KPI 通过 `animKey++` 强制 `n-number-animation` 重挂载  
- ECharts 通过 `setOption(..., true)` 覆盖进行中断  

## 模块改动

| 文件 | 改动 |
|------|------|
| `stationScopeController.ts` | 递增 `scopeGeneration`；导出 `getScopeGeneration` |
| `useStationScopeSync.hook.ts` | 可选 `replayEntrance` 回调；分 watch |
| `replayChartEntrance.ts`（新） | 折线/饼图共用 ECharts 重播 helper |
| `Number/index.vue` | `replayEntranceFromZero` + `animKey` |
| `CarbonSurface.ts` | `replayIntroSpin()`；`updateData(data, { replayIntro })` |
| `ThreeCarbonSurface/index.vue` | scope apply 时 `replayIntro: true` |
| `LineCommon/index.vue` | scope apply 后 `replayChartEntrance` |
| `PieCommon/index.vue` | 同上；Tab 仅 apply |
| `generate-carbon-dashboard.mjs` | Params 增加 `scopeGeneration: 0` |

## 不在范围

- 排行表动画  
- 编辑态预览外的行为变更  
- 修改 KPI dur（仍 3s）或 3D 旋转时长（仍 0.8s）  
- GoView 组件外层 `:key` 整页 remount  

## 测试

- `stationScopeController.test.mjs`：`scopeGeneration` 递增；重复 set 不递增  
- 手工：Flow 连切 3 站、点总数据、Tab 切结构（仅饼图数据变、无完整重播）
