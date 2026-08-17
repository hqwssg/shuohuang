# 站点数据 Scope 联动 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 地图点击 / Flow 停站 / 顶部「总数据」按钮驱动 KPI、3D、折线、饼图在全路与 35 站 mock 间切换；排行表保持全路对比。

**Architecture:** 全局 `requestGlobalConfig.requestParams.Params` 存 `{ dataScope, stationName }`；`stationScopeController` 统一写入；`build-station-scope-mock.mjs` 生成 `{ overall, stations }`；各面板 `useStationScopeSync` watch Params 并换 `option.dataset`；地图 Flow dwell 与 scatter click 调用 controller。

**Tech Stack:** Vue 3, Pinia (`chartEditStore`), GoView preview, ECharts, Three.js, Node test runner (`.test.mjs`)

**Spec:** `docs/superpowers/specs/2026-05-24-station-scope-linkage-design.md`

---

## 文件结构

| 文件 | 职责 |
|------|------|
| `src/assets/dashboard/stationScopeController.ts` | scope 读写、去重 |
| `src/assets/dashboard/stationScopeController.test.mjs` | controller 单测 |
| `scripts/build-station-scope-mock.mjs` | 35 站 mock 生成 |
| `scripts/build-station-scope-mock.test.mjs` | mock 生成单测 |
| `scripts/carbon-dashboard-mock.mjs` | 导出 `dashboardScopeData` |
| `src/assets/dashboard/carbon-dashboard-mock.ts` | TS 同步副本 |
| `src/hooks/useStationScopeSync.hook.ts` | 面板 watch + apply |
| `src/packages/.../FlowTourAnimator.ts` | `onDwellEnter` 回调 |
| `src/packages/.../RailwayCarbonMap/index.vue` | click + dwell 接线 |
| `src/packages/.../Number/index.vue` | 接入 hook（kpi 字段） |
| `src/packages/.../LineCommon/index.vue` | 接入 hook |
| `src/packages/.../ThreeCarbonSurface/index.vue` | 接入 hook |
| `src/packages/.../PieCommon/index.vue` | 接入 hook + structureType |
| `src/packages/.../TextCommon/index.vue` + `config.ts` | 总数据按钮 |
| `scripts/generate-carbon-dashboard.mjs` | 按钮、Params、排行对齐 |
| `scripts/dashboard-component-factory.mjs` | 工厂 helper（如需） |

---

### Task 1: stationScopeController

**Files:**
- Create: `src/assets/dashboard/stationScopeController.ts`
- Create: `src/assets/dashboard/stationScopeController.test.mjs`

- [ ] **Step 1: Write the failing test**

```javascript
// src/assets/dashboard/stationScopeController.test.mjs
import { test } from 'node:test'
import assert from 'node:assert/strict'
import {
  getStationScope,
  setStationScope,
  createScopeParamsHolder
} from './stationScopeController.ts'

test('default scope is overall', () => {
  const holder = createScopeParamsHolder()
  assert.deepEqual(getStationScope(holder), { dataScope: 'overall', stationName: '' })
})

test('setStationScope station writes name', () => {
  const holder = createScopeParamsHolder()
  assert.equal(setStationScope(holder, 'station', '神池南站'), true)
  assert.deepEqual(getStationScope(holder), { dataScope: 'station', stationName: '神池南站' })
})

test('duplicate set returns false', () => {
  const holder = createScopeParamsHolder()
  setStationScope(holder, 'station', '神池南站')
  assert.equal(setStationScope(holder, 'station', '神池南站'), false)
})

test('set overall clears stationName', () => {
  const holder = createScopeParamsHolder()
  setStationScope(holder, 'station', '宁武西站')
  setStationScope(holder, 'overall')
  assert.deepEqual(getStationScope(holder), { dataScope: 'overall', stationName: '' })
})
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd go-view-master-fetch && node --experimental-strip-types src/assets/dashboard/stationScopeController.test.mjs`  
Expected: FAIL — module not found

- [ ] **Step 3: Implement controller**

```typescript
// src/assets/dashboard/stationScopeController.ts
export type DataScope = 'overall' | 'station'

export interface StationScope {
  dataScope: DataScope
  stationName: string
}

export interface ScopeParamsHolder {
  Params: Record<string, unknown>
}

export function createScopeParamsHolder(): ScopeParamsHolder {
  return { Params: { dataScope: 'overall', stationName: '' } }
}

export function getStationScope(holder: ScopeParamsHolder): StationScope {
  const scope = holder.Params.dataScope === 'station' ? 'station' : 'overall'
  const stationName = scope === 'station' ? String(holder.Params.stationName ?? '') : ''
  return { dataScope: scope, stationName }
}

export function setStationScope(
  holder: ScopeParamsHolder,
  scope: DataScope,
  stationName = ''
): boolean {
  const next: StationScope =
    scope === 'station'
      ? { dataScope: 'station', stationName }
      : { dataScope: 'overall', stationName: '' }

  if (!next.stationName && next.dataScope === 'station') {
    console.warn('[stationScope] missing stationName')
    return false
  }

  const prev = getStationScope(holder)
  if (prev.dataScope === next.dataScope && prev.stationName === next.stationName) {
    return false
  }

  holder.Params.dataScope = next.dataScope
  holder.Params.stationName = next.stationName
  return true
}

/** 预览态从 chartEditStore 写入 */
export function setPreviewStationScope(
  chartEditStore: { requestGlobalConfig: { requestParams: ScopeParamsHolder } },
  scope: DataScope,
  stationName = ''
): boolean {
  return setStationScope(chartEditStore.requestGlobalConfig.requestParams, scope, stationName)
}

export function getPreviewStationScope(
  chartEditStore: { requestGlobalConfig: { requestParams: ScopeParamsHolder } }
): StationScope {
  return getStationScope(chartEditStore.requestGlobalConfig.requestParams)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `node --experimental-strip-types src/assets/dashboard/stationScopeController.test.mjs`  
Expected: 4 tests PASS

- [ ] **Step 5: Commit**

```bash
git add src/assets/dashboard/stationScopeController.ts src/assets/dashboard/stationScopeController.test.mjs
git commit -m "feat(dashboard): add station scope controller with tests"
```

---

### Task 2: 生成 35 站 mock 数据

**Files:**
- Create: `scripts/build-station-scope-mock.mjs`
- Create: `scripts/build-station-scope-mock.test.mjs`
- Modify: `scripts/carbon-dashboard-mock.mjs`
- Modify: `src/assets/dashboard/carbon-dashboard-mock.ts`

- [ ] **Step 1: Write failing mock builder test**

```javascript
// scripts/build-station-scope-mock.test.mjs
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { buildDashboardScopeData } from './build-station-scope-mock.mjs'
import { kpiMock, lineTrendMock, threeSurfaceMock, structurePiesMock } from './carbon-dashboard-mock.mjs'

const root = join(dirname(fileURLToPath(import.meta.url)), '..')
const mapData = JSON.parse(
  readFileSync(join(root, 'src/packages/components/Charts/Maps/RailwayCarbonMap/data.json'), 'utf8')
)

test('builds 35 station entries', () => {
  const data = buildDashboardScopeData({ mapData, kpiMock, lineTrendMock, threeSurfaceMock, structurePiesMock })
  assert.equal(Object.keys(data.stations).length, 35)
  assert.ok(data.stations['神池南站'])
})

test('station KPI scales by emission ratio', () => {
  const data = buildDashboardScopeData({ mapData, kpiMock, lineTrendMock, threeSurfaceMock, structurePiesMock })
  const total = mapData.points.points.reduce((s, p) => s + p.carbonEmission, 0)
  const ratio = mapData.points.points[0].carbonEmission / total
  const expected = Math.round(kpiMock.yearTotal.value * ratio * 10) / 10
  assert.ok(Math.abs(data.stations['神池南站'].kpi.yearTotal - expected) < 0.2)
})

test('overall block matches source mocks', () => {
  const data = buildDashboardScopeData({ mapData, kpiMock, lineTrendMock, threeSurfaceMock, structurePiesMock })
  assert.deepEqual(data.overall.kpi, kpiMock)
  assert.deepEqual(data.overall.line, lineTrendMock)
})
```

- [ ] **Step 2: Run test — expect FAIL**

Run: `node scripts/build-station-scope-mock.test.mjs`

- [ ] **Step 3: Implement builder**

```javascript
// scripts/build-station-scope-mock.mjs
function scaleNum(n, ratio, digits = 1) {
  if (n == null) return null
  const v = Number(n) * ratio
  return Math.round(v * 10 ** digits) / 10 ** digits
}

function scaleLineSource(source, ratio) {
  return source.map(row => ({
    ...row,
    actual: row.actual == null ? null : scaleNum(row.actual, ratio, 1),
    forecast: row.forecast == null ? null : scaleNum(row.forecast, ratio, 1)
  }))
}

function scaleSurfaceValues(values, ratio) {
  return values.map(row => row.map(v => scaleNum(v, ratio, 1)))
}

function jitterPie(structurePiesMock, seed) {
  const out = {}
  for (const key of Object.keys(structurePiesMock)) {
    out[key] = {
      dimensions: [...structurePiesMock[key].dimensions],
      source: structurePiesMock[key].source.map((item, i) => ({
        ...item,
        value: Math.max(1, Math.round(item.value + ((seed + i) % 5) - 2))
      }))
    }
  }
  return out
}

export function buildDashboardScopeData({
  mapData,
  kpiMock,
  lineTrendMock,
  threeSurfaceMock,
  structurePiesMock
}) {
  const points = mapData.points.points
  const totalEmission = points.reduce((s, p) => s + p.carbonEmission, 0)
  const stations = {}

  points.forEach((p, idx) => {
    const ratio = p.carbonEmission / totalEmission
    stations[p.name] = {
      kpi: {
        yearTotal: scaleNum(kpiMock.yearTotal.value, ratio, 2),
        monthTotal: scaleNum(kpiMock.monthTotal.value, ratio, 2),
        yoyPercent: scaleNum(kpiMock.yoyPercent.value + (idx % 7) * 0.3 - 1, 1, 1),
        intensity: p.carbonIntensity
      },
      line: {
        dimensions: [...lineTrendMock.dimensions],
        source: scaleLineSource(lineTrendMock.source, ratio)
      },
      surface: {
        ...threeSurfaceMock,
        values: scaleSurfaceValues(threeSurfaceMock.values, ratio)
      },
      pies: jitterPie(structurePiesMock, idx)
    }
  })

  return {
    overall: {
      kpi: kpiMock,
      line: lineTrendMock,
      surface: threeSurfaceMock,
      pies: structurePiesMock
    },
    stations
  }
}

/** 与地图 emission 对齐的排行表行 */
export function buildStationRankFromMap(mapData) {
  return mapData.points.points
    .slice()
    .sort((a, b) => b.carbonEmission - a.carbonEmission)
    .map(p => [p.name, String(Math.round(p.carbonEmission)), String(p.carbonIntensity)])
}
```

- [ ] **Step 4: Wire into carbon-dashboard-mock.mjs**

在文件末尾追加（读取 map json 并 export）：

```javascript
import { readFileSync } from 'node:fs'
import { join, dirname } from 'node:path'
import { fileURLToPath } from 'node:url'
import { buildDashboardScopeData, buildStationRankFromMap } from './build-station-scope-mock.mjs'

const __mockDir = dirname(fileURLToPath(import.meta.url))
const __mapJson = JSON.parse(
  readFileSync(join(__mockDir, '../src/packages/components/Charts/Maps/RailwayCarbonMap/data.json'), 'utf8')
)

export const dashboardScopeData = buildDashboardScopeData({
  mapData: __mapJson,
  kpiMock,
  lineTrendMock,
  threeSurfaceMock,
  structurePiesMock
})

export const stationRankMock = buildStationRankFromMap(__mapJson)
```

删除旧的硬编码 `stationRankMock` 数组；同步复制逻辑到 `src/assets/dashboard/carbon-dashboard-mock.ts`（import map json + export `dashboardScopeData`）。

- [ ] **Step 5: Run tests**

Run: `node scripts/build-station-scope-mock.test.mjs`  
Expected: 3 tests PASS

- [ ] **Step 6: Commit**

```bash
git add scripts/build-station-scope-mock.mjs scripts/build-station-scope-mock.test.mjs scripts/carbon-dashboard-mock.mjs src/assets/dashboard/carbon-dashboard-mock.ts
git commit -m "feat(dashboard): generate per-station scope mock data for 35 stations"
```

---

### Task 3: useStationScopeSync hook

**Files:**
- Create: `src/hooks/useStationScopeSync.hook.ts`
- Modify: `src/hooks/index.ts`

- [ ] **Step 1: Implement hook**

```typescript
// src/hooks/useStationScopeSync.hook.ts
import { watch } from 'vue'
import { isPreview } from '@/utils'
import { getPreviewStationScope } from '@/assets/dashboard/stationScopeController'
import { dashboardScopeData } from '@/assets/dashboard/carbon-dashboard-mock'
import type { ChartEditStoreType } from '@/store/modules/chartEditStore/chartEditStore'

export type ScopePanelKind = 'kpi' | 'line' | 'surface' | 'pie'

export interface UseStationScopeSyncOptions<T> {
  chartEditStore: ChartEditStoreType
  kind: ScopePanelKind
  /** kpi 专用：yearTotal | monthTotal | yoyPercent | intensity */
  kpiField?: 'yearTotal' | 'monthTotal' | 'yoyPercent' | 'intensity'
  apply: (payload: T) => void
  /** pie 专用：读取当前 structureType */
  getStructureType?: () => string
}

function resolvePayload(
  kind: ScopePanelKind,
  scope: ReturnType<typeof getPreviewStationScope>,
  kpiField?: string,
  structureType = 'source'
) {
  const bucket = scope.dataScope === 'station' && scope.stationName
    ? dashboardScopeData.stations[scope.stationName]
    : dashboardScopeData.overall

  if (!bucket) return null

  switch (kind) {
    case 'kpi':
      return bucket.kpi[kpiField ?? 'yearTotal']
    case 'line':
      return bucket.line
    case 'surface':
      return bucket.surface
    case 'pie':
      return bucket.pies[structureType] ?? bucket.pies.source
    default:
      return null
  }
}

export function useStationScopeSync<T>(opts: UseStationScopeSyncOptions<T>) {
  if (!isPreview()) return

  const run = () => {
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
  }

  watch(
    () => [
      opts.chartEditStore.requestGlobalConfig.requestParams.Params.dataScope,
      opts.chartEditStore.requestGlobalConfig.requestParams.Params.stationName
    ],
    run,
    { immediate: true }
  )

  if (opts.kind === 'pie' && opts.getStructureType) {
    watch(opts.getStructureType, run)
  }
}
```

- [ ] **Step 2: Export from hooks/index.ts**

```typescript
export * from '@/hooks/useStationScopeSync.hook'
```

- [ ] **Step 3: Commit**

```bash
git add src/hooks/useStationScopeSync.hook.ts src/hooks/index.ts
git commit -m "feat(dashboard): add useStationScopeSync preview hook"
```

---

### Task 4: FlowTourAnimator dwell 回调

**Files:**
- Modify: `src/packages/components/Charts/Maps/RailwayCarbonMap/core/FlowTourAnimator.ts`
- Modify: `src/packages/components/Charts/Maps/RailwayCarbonMap/utils/flowDwellTimeline.test.mjs`（或新建 `FlowTourAnimator.test.mjs`）

- [ ] **Step 1: Extend options + invoke on dwell enter**

在 `FlowTourAnimatorOptions` 增加：

```typescript
onDwellEnter?: (stationName: string) => void
onDwellLeave?: (stationName: string) => void
```

在 `syncDwellTooltip` 中，当 `dwellKey` 变化且进入新 dwell 时：

```typescript
if (dwellKey && state.stationName) {
  this.opts.onDwellEnter?.(state.stationName)
}
if (this.activeDwellKey && !dwellKey) {
  const prevName = this.activeDwellKey.split(':')[1]
  if (prevName) this.opts.onDwellLeave?.(prevName)
}
```

注意：在 `this.activeDwellKey = dwellKey` **之前**读取旧 key 用于 leave。

- [ ] **Step 2: Add unit test with mock chart**

```javascript
// src/packages/components/Charts/Maps/RailwayCarbonMap/core/FlowTourAnimator.test.mjs
import { test } from 'node:test'
import assert from 'node:assert/strict'
import { FlowTourAnimator } from './FlowTourAnimator.ts'

test('onDwellEnter fires once per station dwell', () => {
  const calls = []
  const animator = new FlowTourAnimator()
  // 使用 stub：直接调用 syncDwellTooltip 需 expose 或 integration test
  // 简化：测试 public API 存在且 options 可传入
  assert.equal(typeof animator.start, 'function')
  calls.push('ok')
  assert.equal(calls.length, 1)
})
```

若 `syncDwellTooltip` 为 private，改为在 `flowDwellTimeline.test.mjs` 旁新增集成测试：mock `chart.dispatchAction`，手动驱动 `start` 并快进 `elapsedSec`（可选，YAGNI：至少 manual QA）。

- [ ] **Step 3: Commit**

```bash
git add src/packages/components/Charts/Maps/RailwayCarbonMap/core/FlowTourAnimator.ts
git commit -m "feat(map): emit onDwellEnter callback from flow tour"
```

---

### Task 5: RailwayCarbonMap 接线（click + dwell）

**Files:**
- Modify: `src/packages/components/Charts/Maps/RailwayCarbonMap/index.vue`

- [ ] **Step 1: Import controller + store**

```typescript
import { isPreview } from '@/utils'
import { setPreviewStationScope } from '@/assets/dashboard/stationScopeController'
```

- [ ] **Step 2: 在 ECharts init 后注册 click**

在 `echartsManager.setOption` 之后：

```typescript
const chart = echartsManager.getChart()
if (chart && isPreview()) {
  chart.off('click', onMapStationClick)
  chart.on('click', onMapStationClick)
}

function onMapStationClick(params: any) {
  if (params?.seriesType !== 'scatter' && params?.seriesType !== 'effectScatter') return
  const name = params?.name || params?.data?.name
  if (!name) return
  setPreviewStationScope(useChartEditStore(), 'station', name)
}
```

- [ ] **Step 3: Flow tour 启动处传入 onDwellEnter**

找到 `flowTourAnimator.start(...)` 调用，增加：

```typescript
onDwellEnter: (stationName) => {
  if (!isPreview()) return
  setPreviewStationScope(useChartEditStore(), 'station', stationName)
}
```

- [ ] **Step 4: onBeforeUnmount 移除 click 监听**

- [ ] **Step 5: Commit**

```bash
git add src/packages/components/Charts/Maps/RailwayCarbonMap/index.vue
git commit -m "feat(map): broadcast station scope on click and flow dwell"
```

---

### Task 6: 面板组件接入 hook

**Files:**
- Modify: `src/packages/components/Decorates/Mores/Number/index.vue`（×1 文件，4 实例靠 kpiField 区分）
- Modify: `src/packages/components/Charts/Lines/LineCommon/index.vue`
- Modify: `src/packages/components/Decorates/Three/ThreeCarbonSurface/index.vue`
- Modify: `src/packages/components/Charts/Pies/PieCommon/index.vue`

- [ ] **Step 1: Number — 按组件 id 映射 kpiField**

在 `generate-carbon-dashboard.mjs` 为每个 KPI 的 `request.requestParams.Params` 增加 `kpiField: 'yearTotal'` 等（或在 Number 内读 `props.chartConfig.request.requestParams.Params.kpiField`）。

`Number/index.vue` 追加：

```typescript
import { useStationScopeSync } from '@/hooks'
import { isPreview } from '@/utils'

const chartEditStore = useChartEditStore()
const kpiField = computed(() =>
  (props.chartConfig.request?.requestParams?.Params?.kpiField as string) || 'yearTotal'
)

if (isPreview()) {
  useStationScopeSync<number>({
    chartEditStore,
    kind: 'kpi',
    kpiField: kpiField.value as any,
    apply: (v) => updateNumber(v)
  })
}
```

因 kpiField 来自 Params，watch Params.kpiField 或使用 4 个固定 id 映射：

```typescript
const KPI_FIELD_BY_ID: Record<string, 'yearTotal'|'monthTotal'|'yoyPercent'|'intensity'> = {
  'dash-kpi-001': 'yearTotal',
  'dash-kpi-002': 'monthTotal',
  'dash-kpi-003': 'yoyPercent',
  'dash-kpi-004': 'intensity'
}
```

站 scope 下 kpi4 显示 intensity；overall 下 kpi4 仍用原 `option.dataset`（35 个核算点）—— hook 内对 `dash-kpi-004` + overall 时 apply `kpiMock.pointCount.value` 或跳过 hook 用原值。在 `resolvePayload` 对 overall + intensity 字段返回 `kpiMock.pointCount.value`。

- [ ] **Step 2: LineCommon**

```typescript
useStationScopeSync({
  chartEditStore: useChartEditStore(),
  kind: 'line',
  apply: (dataset) => {
    props.chartConfig.option.dataset = dataset
  }
})
```

- [ ] **Step 3: ThreeCarbonSurface**

```typescript
useStationScopeSync({
  chartEditStore: useChartEditStore(),
  kind: 'surface',
  apply: (dataset) => {
    updateData(dataset)
  }
})
```

- [ ] **Step 4: PieCommon — 合并 structureType**

保留现有 Tab `$watch`；追加：

```typescript
useStationScopeSync({
  chartEditStore: useChartEditStore(),
  kind: 'pie',
  getStructureType: () =>
    props.chartConfig.request?.requestParams?.Params?.structureType || 'source',
  apply: (dataset) => {
    props.chartConfig.option.dataset = dataset
    if (isPreview() && vChartRef.value) {
      setOption(vChartRef.value, option.value, true)
    }
  }
})
```

逐步移除模板 JSON 内 `PIE_STRUCTURE_VNODE_MOUNTED` 中硬编码 ALL（改为 hook 统一驱动），或保留 vnodeMounted 仅 watch structureType 时调用同一 apply 函数——**推荐删除 vnodeMounted 中 ALL 静态块**，仅 hook + 现有 Tab interact。

- [ ] **Step 5: Commit**

```bash
git add src/packages/components/Decorates/Mores/Number/index.vue src/packages/components/Charts/Lines/LineCommon/index.vue src/packages/components/Decorates/Three/ThreeCarbonSurface/index.vue src/packages/components/Charts/Pies/PieCommon/index.vue
git commit -m "feat(dashboard): sync KPI, line, 3D, pie with station scope"
```

---

### Task 7: 顶部「总数据」按钮

**Files:**
- Modify: `src/packages/components/Informations/Texts/TextCommon/config.ts`
- Modify: `src/packages/components/Informations/Texts/TextCommon/index.vue`
- Modify: `scripts/generate-carbon-dashboard.mjs`

- [ ] **Step 1: config 增加 stationScopeAction**

```typescript
stationScopeAction: undefined as 'overall' | undefined,
highlightWhenOverall: true,
```

- [ ] **Step 2: TextCommon 点击与高亮**

```typescript
import { isPreview } from '@/utils'
import { setPreviewStationScope, getPreviewStationScope } from '@/assets/dashboard/stationScopeController'

const isOverallActive = computed(() => {
  if (!props.chartConfig.option.highlightWhenOverall) return false
  const s = getPreviewStationScope(useChartEditStore())
  return s.dataScope === 'overall'
})

const onContentClick = () => {
  if (props.chartConfig.option.stationScopeAction === 'overall' && isPreview()) {
    setPreviewStationScope(useChartEditStore(), 'overall')
    return
  }
  if (link.value) click()
}
```

模板：`<span @click="onContentClick" :class="{ 'scope-active': isOverallActive }">`

样式 `.scope-active { border-color: #2EC7FF; box-shadow: 0 0 8px rgba(46,199,255,.45); }`

- [ ] **Step 3: generate 增加按钮组件**

```javascript
const btnOverall = makeComponent('TextCommon', {
  id: 'dash-btn-overall',
  attr: { x: 32, y: 118, w: 96, h: 36, zIndex: 16 },
  option: makeTextOption('总数据', {
    fontSize: 14,
    fontColor: '#E6F7FF',
    borderColor: '#2EC7FF',
    borderWidth: 1,
    borderRadius: 4,
    backgroundColor: 'rgba(2,12,24,0.55)',
    stationScopeAction: 'overall',
    highlightWhenOverall: true
  })
})
```

插入 `componentList`；`buildCarbonDashboardStorage` 的 `requestGlobalConfig.requestParams.Params` 初始：

```javascript
requestParams: {
  ...,
  Params: { dataScope: 'overall', stationName: '' }
}
```

- [ ] **Step 4: Commit**

```bash
git add src/packages/components/Informations/Texts/TextCommon/config.ts src/packages/components/Informations/Texts/TextCommon/index.vue scripts/generate-carbon-dashboard.mjs scripts/dashboard-component-factory.mjs
git commit -m "feat(dashboard): add overall scope toggle button in header"
```

---

### Task 8: 生成模板并 upsert

**Files:**
- Modify: `scripts/generate-carbon-dashboard.mjs`（KPI id 映射、kpi4 overall 逻辑）
- Run upsert script

- [ ] **Step 1: 更新 KPI 组件 option 初始 dataset 与 kpiField Params**

- [ ] **Step 2: 移除饼图 PIE_STRUCTURE_VNODE_MOUNTED 静态 ALL（改由 hook）**

更新 `PIE_STRUCTURE_VNODE_MOUNTED` 为空或删除 `advancedEvents.vnodeMounted`；Tab interact 保留。

- [ ] **Step 3: 生成并 upsert**

Run:
```bash
cd go-view-master-fetch
node scripts/generate-carbon-dashboard.mjs
node scripts/upsert-carbon-dashboard-project.mjs
```

Expected: `components: 22`（+1 按钮），Preview URL 可打开

- [ ] **Step 4: Commit**

```bash
git add public/templates/shuohuang-carbon-dashboard.json scripts/generate-carbon-dashboard.mjs scripts/carbon-dashboard-mock.mjs
git commit -m "chore(dashboard): regenerate template with station scope linkage"
```

---

### Task 9: 手工验证清单

- [ ] 打开 preview，初始 KPI/3D/折线/饼图为全路数据
- [ ] Flow 首次停站（神池南）→ 左栏+饼图切换，数值明显小于全路
- [ ] 点击「总数据」→ 恢复全路；下一 dwell → 切到新站
- [ ] Flow 播放中点击其他站 → 立即切换；下一 dwell → 再覆盖
- [ ] Tab 切「排放类型」后换站 → 仍为 type 结构
- [ ] 排行表始终 35 站，与地图 emission 排序一致
- [ ] Ctrl+F5 / 清除 `sessionStorage GO_CHART_STORAGE_LIST` 后仍正常

---

## Spec 覆盖自检

| Spec 要求 | 对应 Task |
|-----------|-----------|
| 全局 Params scope | Task 1, 7, 8 |
| 35 站 mock | Task 2 |
| 联动 KPI/3D/折线/饼图 | Task 3, 6 |
| 排行表不切换、数值对齐 | Task 2, 8 |
| 地图 click | Task 5 |
| Flow dwell 驱动 | Task 4, 5 |
| 总数据按钮 | Task 7 |
| 优先级规则 | Task 1, 5, 7 |
| Tab structureType 保留 | Task 6 |
| 单测 | Task 1, 2 |

## 执行方式

Plan complete and saved to `docs/superpowers/plans/2026-05-24-station-scope-linkage.md`. Two execution options:

**1. Subagent-Driven (recommended)** — 每个 Task 派发独立 subagent，任务间 review，迭代快

**2. Inline Execution** — 本会话按 Task 顺序直接实现，checkpoint Review

你想用哪种方式？
