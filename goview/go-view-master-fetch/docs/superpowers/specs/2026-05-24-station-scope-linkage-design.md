# 站点数据 scope 联动 — 设计说明

**日期：** 2026-05-24  
**项目：** 朔黄铁路碳排放全景大屏（GoView `2066429772333690882`）

## 背景

大屏当前仅展示全路汇总 mock。地图 `RailwayCarbonMap/data.json` 含 35 站排放数据，Flow tour 到站会 `showTip`，但不驱动侧栏。用户需：点击站点 / Flow 停站 / 顶部「总数据」按钮，联动左侧面板与右侧饼图切换数据；排行表始终全路对比。

## 已确认需求

| 决策项 | 结论 |
|--------|------|
| 联动组件 | **C**：4 KPI + 3D 曲面 + 折线 + 饼图；排行表不切换 |
| Flow 中手动点击 | **A**：立即切站，Flow 继续；下一 dwell 覆盖 |
| 初始状态 | **A**：全路总数据；Flow 首次停站后切站 |
| 饼图 Tab | 切换 scope 后原位换数据；结构类型仍由 Tab 当前选中项决定 |
| 总数据按钮 | 顶部新增；点击回总数据；Flow 下一 dwell 覆盖为站数据 |

## 数据 scope 优先级

| 事件 | `dataScope` | `stationName` |
|------|-------------|---------------|
| 页面加载 | `overall` | `''` |
| 点击「总数据」 | `overall` | `''` |
| Flow 进入 dwell | `station` | 当前站名 |
| 地图点击站点 | `station` | 所点站名 |
| Flow 下一 dwell | `station` | 新站名（覆盖总数据/手动选择） |

## 架构

### 全局状态

写入 `chartEditStore.requestGlobalConfig.requestParams.Params`：

```typescript
{
  dataScope: 'overall' | 'station',
  stationName: string  // scope=station 时为完整站名，如「神池南站」
}
```

预览态生效（与 Tab→饼图 一致）；编辑态可写入但不强制联动。

### 模块划分

| 模块 | 职责 |
|------|------|
| `stationScopeController.ts` | `setStationScope` / `getStationScope` / 去重 |
| `build-station-scope-mock.mjs` | 由地图点位 + 全路 mock 生成 35 站数据集 |
| `carbon-dashboard-mock.ts(.mjs)` | 导出 `dashboardScopeData` |
| `useStationScopeSync.hook.ts` | 组件 watch Params 并 apply 数据 |
| `FlowTourAnimator` | dwell 进入时回调 `onDwellEnter(stationName)` |
| `RailwayCarbonMap/index.vue` | scatter click + 注册 dwell 回调 |
| `TextCommon` | `stationScopeAction: 'overall'` 时点击设总数据 |
| `generate-carbon-dashboard.mjs` | 注入按钮、初始 Params、排行表与地图对齐 |

### 各站 mock 生成规则

- 读取 `RailwayCarbonMap/data.json` 的 `points.points[].carbonEmission`
- `ratio = stationEmission / sum(allEmissions)`
- **KPI**：全路 KPI 数值 × ratio（`pointCount` 在站 scope 下改为该站 `carbonIntensity`，suffix 仍为合理单位）
- **折线**：全路 `lineTrendMock.source` 每行 actual/forecast × ratio
- **3D**：全路 `threeSurfaceMock.values` 每格 × ratio
- **饼图**：三种 structure 的 value 不变（占比），可选 ±3 抖动；或保持占比仅 tooltip 上下文变化（实现取占比不变 + 标题感即可）
- **排行表**：始终 `stationRankMock`（数值与地图 emission 对齐，按 emission 降序）

## UI：总数据按钮

- 组件：`TextCommon`，ID `dash-btn-overall`
- 位置：KPI 行左侧，约 `x:32, y:118, w:96, h:36`
- 文案：`总数据`；`dataScope=overall` 时 cyan 边框高亮（watch Params 更新样式 via option 或 class）

## 不参与联动

- `TableScrollBoard`（`dash-table-001`）
- 地图几何与 Flow 动画本身
- `InputsTab`（仅控制 `structureType`）

## 错误处理

- 未知站名：`console.warn`，保持上一 scope
- 同站重复 set：controller 内短路，不重复 apply
- Flow tooltip 与 scope 并行，互不取消

## 测试

- `stationScopeController.test.mjs`：set/get/去重
- `build-station-scope-mock.test.mjs`：35 站、ratio、与地图 emission 一致
- `FlowTourAnimator` dwell 回调 unit test（mock chart）
- 手工：初始 overall → dwell 切站 → 总数据 → 下一 dwell 覆盖 → Flow 中点击站 → Tab 换结构后切站

## 不在范围

- 后端 per-station API
- 排行表 scope 切换
- Flow 暂停/编辑态完整联动
