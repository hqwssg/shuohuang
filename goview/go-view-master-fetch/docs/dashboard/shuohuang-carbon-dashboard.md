# 朔黄铁路碳排放全景大屏

GoView 1920×1080 碳排放全景视图，复用已有组件（`RailwayCarbonMap`、`ThreeCarbonSurface`、`LineCommon` 等），全部内置 mock 数据。

## 一键新建项目（推荐）

**前置：** GoView 后端已启动（默认 `http://localhost:8083`），前端 `pnpm dev`（默认 `http://localhost:3000`）。

```bash
cd go-view-master-fetch
pnpm dashboard:seed
```

或：

```bash
node scripts/seed-carbon-dashboard-project.mjs
```

成功后打开 [全部项目](http://localhost:3000/#/project/items)，应出现 **朔黄铁路碳排放全景大屏** 卡片（未发布）。

| 操作 | URL |
|------|-----|
| 项目列表 | `http://localhost:3000/#/project/items` |
| 编辑 | `http://localhost:3000/#/chart/home/{projectId}` |
| 预览 | `http://localhost:3000/#/chart/preview/{projectId}` |

### 环境变量

| 变量 | 默认值 |
|------|--------|
| `GOVIEW_API_BASE` | `http://localhost:8083/api/goview` |
| `GOVIEW_USERNAME` | `admin` |
| `GOVIEW_PASSWORD` | `admin` |

## 仅生成模板 JSON（不写入项目库）

```bash
pnpm dashboard:generate
```

产物：`public/templates/shuohuang-carbon-dashboard.json`

## 布局说明

- 底层：`RailwayCarbonMap` 全屏（zIndex -1）
- 左栏：3D 曲面、12 月趋势（含预测虚线）、结构变化柱图
- 右栏：Tab + 饼图（三结构切换）、站点排行表
- 顶部：4 个 KPI 数字 + 标题文案

## 验收清单

| # | 检查项 |
|---|--------|
| 1 | 画布 1920×1080，深色背景 `#020a14` |
| 2 | 地图全屏底层，线路 + 站点可见 |
| 3 | 4 KPI：本年累计 / 本月 / 同比 / 核算点数 |
| 4 | 3D 曲面可旋转，显示四年数据 |
| 5 | 折线：2024-01—12 实线 + 2025-01—03 预测虚线 |
| 6 | Tab 切换三种饼图（排放源 / 类型 / 环节） |
| 7 | 排行表滚动 |
| 8 | 结构变化柱图 |
| 9 | 地图无右下角 HTML 图例 |
| 10 | `pnpm dashboard:generate` 可复现 JSON |
| 11 | `pnpm dashboard:seed` 后「全部项目」出现新卡片 |

## 注意

- 每次执行 `dashboard:seed` 都会**新建**一条项目记录。
- 饼图 Tab 联动通过 `InputsTab.interactEvents` + 饼图 `vnodeMounted` 监听 `structureType` 实现（静态 mock 场景）。
