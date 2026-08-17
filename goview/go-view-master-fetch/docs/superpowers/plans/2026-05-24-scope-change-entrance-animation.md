# Scope 切换进场动画 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 每次 scope 换数据（Flow 停站 / 点站 / 总数据）时，KPI、3D、折线、饼图重播与首次加载一致的进场动画；快速连切立即打断并重播。

**Architecture:** `setStationScope` 成功时递增 `scopeGeneration`；`useStationScopeSync` 在 scope 变化时 `apply` 后调用各组件 `replayEntrance`；饼图 Tab 仅 structureType 变化时不重播。3D 通过 `replayIntroSpin()`，KPI 从 0 滚数，ECharts 通过 shared helper `setOption(..., true)`。

**Tech Stack:** Vue 3, Pinia, Naive UI NumberAnimation, Three.js ControlsManager, vue-echarts, Node test runner

**Spec:** `docs/superpowers/specs/2026-05-24-scope-change-entrance-animation-design.md`

---

## 文件结构

| 文件 | 职责 |
|------|------|
| `src/assets/dashboard/stationScopeController.ts` | `scopeGeneration` 递增/读取 |
| `src/assets/dashboard/stationScopeController.test.mjs` | generation 单测 |
| `src/hooks/useStationScopeSync.hook.ts` | scope vs Tab 分流 + `replayEntrance` |
| `src/assets/dashboard/replayChartEntrance.ts` | ECharts 进场重播 helper |
| `src/packages/.../Number/index.vue` | `from=0` + `animKey` |
| `src/packages/.../ThreeCarbonSurface/core/CarbonSurface.ts` | `replayIntroSpin()` |
| `src/packages/.../ThreeCarbonSurface/index.vue` | scope 更新带 `replayIntro` |
| `src/packages/.../LineCommon/index.vue` | 接 hook + chart replay |
| `src/packages/.../Pies/PieCommon/index.vue` | 接 hook + chart replay |
| `scripts/generate-carbon-dashboard.mjs` | 初始 `scopeGeneration: 0` |

---

### Task 1: scopeGeneration

**Files:**
- Modify: `src/assets/dashboard/stationScopeController.ts`
- Modify: `src/assets/dashboard/stationScopeController.test.mjs`

- [ ] **Step 1: Add failing tests**

```javascript
import {
  getStationScope,
  setStationScope,
  createScopeParamsHolder,
  getScopeGeneration
} from './stationScopeController.ts'

test('initial scopeGeneration is 0', () => {
  const holder = createScopeParamsHolder()
  assert.equal(getScopeGeneration(holder), 0)
})

test('successful set increments scopeGeneration', () => {
  const holder = createScopeParamsHolder()
  assert.equal(setStationScope(holder, 'station', '神池南站'), true)
  assert.equal(getScopeGeneration(holder), 1)
  assert.equal(setStationScope(holder, 'overall'), true)
  assert.equal(getScopeGeneration(holder), 2)
})

test('duplicate set does not increment scopeGeneration', () => {
  const holder = createScopeParamsHolder()
  setStationScope(holder, 'station', '神池南站')
  assert.equal(getScopeGeneration(holder), 1)
  assert.equal(setStationScope(holder, 'station', '神池南站'), false)
  assert.equal(getScopeGeneration(holder), 1)
})
```

- [ ] **Step 2: Run test — expect FAIL**

Run: `npx tsx src/assets/dashboard/stationScopeController.test.mjs`  
Expected: FAIL — `getScopeGeneration is not exported`

- [ ] **Step 3: Implement**

在 `createScopeParamsHolder`:

```typescript
return { Params: { dataScope: 'overall', stationName: '', scopeGeneration: 0 } }
```

在 `setStationScope` 成功写入前：

```typescript
holder.Params.scopeGeneration = (Number(holder.Params.scopeGeneration) || 0) + 1
```

新增：

```typescript
export function getScopeGeneration(holder: ScopeParamsHolder): number {
  return Number(holder.Params.scopeGeneration) || 0
}
```

- [ ] **Step 4: Run test — expect PASS**

Run: `npx tsx src/assets/dashboard/stationScopeController.test.mjs`  
Expected: 7 tests PASS（含原有 4 项）

- [ ] **Step 5: Commit**

```bash
git add src/assets/dashboard/stationScopeController.ts src/assets/dashboard/stationScopeController.test.mjs
git commit -m "feat(dashboard): increment scopeGeneration on station scope change"
```

---

### Task 2: useStationScopeSync 支持 replayEntrance

**Files:**
- Modify: `src/hooks/useStationScopeSync.hook.ts`

- [ ] **Step 1: Extend options + split watches**

```typescript
export interface UseStationScopeSyncOptions<T> {
  chartEditStore: { requestGlobalConfig: { requestParams: { Params: Record<string, unknown> } } }
  kind: ScopePanelKind
  kpiField?: KpiScopeField
  apply: (payload: T) => void
  replayEntrance?: () => void
  getStructureType?: () => string
}

export function useStationScopeSync<T>(opts: UseStationScopeSyncOptions<T>) {
  if (!isPreview()) return

  const run = (withEntrance: boolean) => {
    const scope = getPreviewStationScope(opts.chartEditStore)
    const structureType = opts.getStructureType?.() ?? 'source'
    const payload = resolvePayload(opts.kind, scope, opts.kpiField, structureType)
    if (payload == null) {
      if (scope.dataScope === 'station') {
        console.warn('[stationScope] unknown station:', scope.stationName)
      }
      return
    }
    opts.apply(payload as T)
    if (withEntrance) opts.replayEntrance?.()
  }

  watch(
    () => [
      opts.chartEditStore.requestGlobalConfig.requestParams.Params.dataScope,
      opts.chartEditStore.requestGlobalConfig.requestParams.Params.stationName,
      opts.chartEditStore.requestGlobalConfig.requestParams.Params.scopeGeneration
    ],
    () => run(true),
    { immediate: true }
  )

  if (opts.kind === 'pie' && opts.getStructureType) {
    watch(opts.getStructureType, () => run(false))
  }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/hooks/useStationScopeSync.hook.ts
git commit -m "feat(dashboard): add replayEntrance hook to useStationScopeSync"
```

---

### Task 3: ECharts 进场重播 helper

**Files:**
- Create: `src/assets/dashboard/replayChartEntrance.ts`

- [ ] **Step 1: Implement helper**

```typescript
import { setOption } from '@/packages/public/chart'

const ENTRANCE_MS = 1000

/** 与首次绘制一致的 ECharts 进场动画（notMerge 打断进行中的动画） */
export function replayChartEntrance(vChartRef: { value?: unknown }, optionValue: Record<string, unknown>) {
  if (!vChartRef?.value) return

  const series = Array.isArray(optionValue.series) ? optionValue.series : []
  setOption(
    vChartRef as never,
    {
      ...optionValue,
      animation: true,
      animationDuration: ENTRANCE_MS,
      animationEasing: 'cubicOut',
      series: series.map(s => ({
        ...(s as Record<string, unknown>),
        animation: true,
        animationDuration: ENTRANCE_MS,
        animationEasing: 'cubicOut'
      }))
    },
    true
  )
}
```

- [ ] **Step 2: Commit**

```bash
git add src/assets/dashboard/replayChartEntrance.ts
git commit -m "feat(dashboard): add shared ECharts entrance replay helper"
```

---

### Task 4: KPI 从 0 重播

**Files:**
- Modify: `src/packages/components/Decorates/Mores/Number/index.vue`

- [ ] **Step 1: Add animKey + replayEntranceFromZero**

模板 `n-number-animation` 增加 `:key="animKey"`。

```typescript
const animKey = ref(0)

const replayEntranceFromZero = (value: number) => {
  option.from = 0
  option.dataset = value
  animKey.value++
}

// useStationScopeSync 内：
useStationScopeSync<number>({
  chartEditStore,
  kind: 'kpi',
  kpiField: KPI_FIELD_BY_ID[props.chartConfig.id] ?? 'yearTotal',
  apply: v => {
    option.from = 0
    option.dataset = v
  },
  replayEntrance: () => {
    replayEntranceFromZero(option.dataset)
  }
})
```

注意：`apply` 已设值；`replayEntrance` 用当前 `option.dataset` 从 0 重播。首次 `immediate` 也会走 `replayEntrance` — 与首次加载一致。

- [ ] **Step 2: Commit**

```bash
git add src/packages/components/Decorates/Mores/Number/index.vue
git commit -m "feat(dashboard): replay KPI entrance from zero on scope change"
```

---

### Task 5: 3D 曲面 scope 切换重播旋转

**Files:**
- Modify: `src/packages/components/Decorates/Three/ThreeCarbonSurface/core/CarbonSurface.ts`
- Modify: `src/packages/components/Decorates/Three/ThreeCarbonSurface/index.vue`

- [ ] **Step 1: CarbonSurface.replayIntroSpin**

```typescript
public replayIntroSpin(): void {
  if (this.isDisposed) return
  if (this.options.enableIntroSpin === false) return
  this.controlsManager.startIntroSpin()
}

public updateData(data: CarbonSurfaceData, opts?: { replayIntro?: boolean }): void {
  // ... existing rebuild ...
  this.buildScene()
  if (opts?.replayIntro) {
    this.replayIntroSpin()
  }
}
```

- [ ] **Step 2: ThreeCarbonSurface index.vue**

```typescript
useStationScopeSync({
  chartEditStore: useChartEditStore(),
  kind: 'surface',
  apply: dataset => {
    surfaceInstance?.updateData(dataset as CarbonSurfaceData)
  },
  replayEntrance: () => {
    if (!surfaceInstance) return
    surfaceInstance.updateData(
      props.chartConfig.option.dataset as CarbonSurfaceData,
      { replayIntro: true }
    )
  }
})
```

`replayEntrance` 时数据已在 `apply` 写入 `option.dataset`；`updateData` 重建 mesh 并 `startIntroSpin()`。

非 scope 的 `updateData`（如 normalize）不传 `replayIntro`。

- [ ] **Step 3: Commit**

```bash
git add src/packages/components/Decorates/Three/ThreeCarbonSurface/core/CarbonSurface.ts src/packages/components/Decorates/Three/ThreeCarbonSurface/index.vue
git commit -m "feat(dashboard): replay 3D intro spin on scope data change"
```

---

### Task 6: 折线 / 饼图 ECharts 重播

**Files:**
- Modify: `src/packages/components/Charts/Lines/LineCommon/index.vue`
- Modify: `src/packages/components/Charts/Pies/PieCommon/index.vue`

- [ ] **Step 1: LineCommon**

```typescript
import { replayChartEntrance } from '@/assets/dashboard/replayChartEntrance'

if (isPreview()) {
  useStationScopeSync({
    chartEditStore: useChartEditStore(),
    kind: 'line',
    apply: dataset => {
      props.chartConfig.option.dataset = dataset
    },
    replayEntrance: () => {
      replayChartEntrance(vChartRef, option.value)
    }
  })
}
```

- [ ] **Step 2: PieCommon**

```typescript
import { replayChartEntrance } from '@/assets/dashboard/replayChartEntrance'

useStationScopeSync({
  chartEditStore,
  kind: 'pie',
  getStructureType: () =>
    (props.chartConfig.request?.requestParams?.Params?.structureType as string) || 'source',
  apply: dataset => {
    props.chartConfig.option.dataset = dataset
  },
  replayEntrance: () => {
    if (isPreview() && vChartRef.value) {
      replayChartEntrance(vChartRef, option.value)
    }
  }
})
```

保留现有 `watch dataset/structureType → setOption` 可删或保留作兜底；scope 变化以 `replayEntrance` 为准。

- [ ] **Step 3: Commit**

```bash
git add src/packages/components/Charts/Lines/LineCommon/index.vue src/packages/components/Charts/Pies/PieCommon/index.vue
git commit -m "feat(dashboard): replay line and pie ECharts entrance on scope change"
```

---

### Task 7: 模板 Params + upsert

**Files:**
- Modify: `scripts/generate-carbon-dashboard.mjs`

- [ ] **Step 1: 初始 Params 增加 scopeGeneration**

```javascript
Params: { dataScope: 'overall', stationName: '', scopeGeneration: 0 }
```

- [ ] **Step 2: 生成并 upsert**

```bash
cd go-view-master-fetch
node scripts/generate-carbon-dashboard.mjs
node scripts/upsert-carbon-dashboard-project.mjs
```

Expected: upsert 成功

- [ ] **Step 3: Commit**

```bash
git add scripts/generate-carbon-dashboard.mjs public/templates/shuohuang-carbon-dashboard.json
git commit -m "chore(dashboard): add scopeGeneration to template Params"
```

---

### Task 8: 手工验证

- [ ] 打开 preview，首屏 KPI/3D/折线/饼图均有进场动画  
- [ ] Flow 停站 → 四组件重播；快速连切 3 站 → 每次打断并重播  
- [ ] 点「总数据」→ 四组件重播  
- [ ] 饼图 Tab 切结构 → 仅数据切换，**不**完整重播扇区 scale  
- [ ] 排行表无变化  

---

## Spec 覆盖自检

| Spec 要求 | Task |
|-----------|------|
| scopeGeneration | Task 1, 7 |
| 立即打断重播 | Task 4–6（animKey / startIntroSpin 重入 / setOption notMerge） |
| KPI from 0, dur 3s | Task 4 |
| 3D 0.8s spin | Task 5 |
| ECharts ~1000ms | Task 3, 6 |
| Tab 不重播 | Task 2 |
| 单测 | Task 1 |

## 执行方式

Plan complete and saved to `docs/superpowers/plans/2026-05-24-scope-change-entrance-animation.md`. Two execution options:

**1. Subagent-Driven (recommended)** — 每个 Task 独立 subagent，任务间 review  

**2. Inline Execution** — 本会话按 Task 顺序直接实现  

你想用哪种方式？
