# 碳排放模型设计系统 - 需求说明书

## 版本信息

| 项目   | 内容         |
|:---- |:---------- |
| 文档版本 | 2.0        |
| 生成日期 | 2026-06-06 |
| 系统版本 | 2.0        |

---

## 1. 项目概述

### 1.1 项目背景

本系统是一个基于浏览器访问的碳排放模型设计系统，用于帮助用户创建和管理碳排放核算模版，实现碳排放的自动化核算。系统支持树形结构的节点管理，提供灵活的属性配置和计划任务调度功能。

### 1.2 技术架构

| 层级    | 技术栈                   | 版本    |
|:----- |:--------------------- |:----- |
| 前端框架  | Vue.js                | 3.x   |
| 前端组件库 | Element Plus          | 2.x   |
| 构建工具  | Vite                  | 6.x   |
| 后端框架  | Spring Boot           | 3.2.0 |
| 数据库   | MySQL                 | 8.0+  |
| ORM框架 | Spring Data JPA       | 3.2.x |
| 定时任务  | Spring Scheduled      | -     |
| 异步任务  | Spring Async + 自定义线程池 | -     |

### 1.3 系统架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端层 (Vue + Element Plus)               │
│  ┌─────────────┐  ┌─────────────┐  ┌───────────────────────┐    │
│  │ 登录页面    │  │ 模版列表页  │  │ 模版设计编辑器        │    │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬────────────┘    │
└─────────┼────────────────┼─────────────────────┼─────────────────┘
          │                │                     │
          ▼                ▼                     ▼
┌─────────────────────────────────────────────────────────────────┐
│                        后端层 (Spring Boot)                      │
│  ┌─────────────┐  ┌─────────────┐  ┌───────────────────────┐    │
│  │UserController││TemplateCtrl │  │EmissionNodeController│    │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬────────────┘    │
│         │                │                     │                 │
│  ┌──────▼──────┐  ┌──────▼──────┐  ┌──────────▼────────────┐    │
│  │UserService  │  │TemplateSvc  │  │EmissionNodeService   │    │
│  └──────┬──────┘  └──────┬──────┘  └──────────┬────────────┘    │
│         │                │                     │                 │
│         ▼                ▼                     ▼                 │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                    Repository Layer                        │  │
│  │  UserRepo | TemplateRepo | EmissionNodeRepo | DataDictRepo│  │
│  └───────────────────────────────────────────────────────────┘  │
│                                  │                              │
│                    ┌─────────────┴─────────────┐                │
│                    ▼                           ▼                │
│         ┌─────────────────┐       ┌─────────────────────┐       │
│         │EmissionScheduler│       │DataCollectionScheduler│      │
│         │ (核算任务调度)   │       │ (数据采集任务调度)    │       │
│         └────────┬────────┘       └──────────┬──────────┘       │
│                  │                          │                  │
│                  ▼                          ▼                  │
│         ┌─────────────────┐                                    │
│         │  AsyncTaskService│ (异步任务执行)                     │
│         │  (自定义线程池)   │                                   │
│         └─────────────────┘                                    │
└─────────────────────────────────────────────────────────────────┘
                                  │
                                  ▼
┌─────────────────────────────────────────────────────────────────┐
│                        数据库层 (MySQL)                          │
│  user | template | emission_node | emission_node_config         │
│  data_dict | data_dict_item | node_type | template             │
│  data_source_system                                            │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 功能需求

### 2.1 用户登录模块

| 需求编号    | 功能点  | 描述               | 优先级 | 实现状态 |
|:------- |:---- |:---------------- |:--- |:---- |
| REQ-001 | 用户登录 | 用户通过用户名和密码进行登录验证 | 高   | ✅    |
| REQ-002 | 会话管理 | 登录成功后创建会话，支持会话保持 | 高   | ✅    |
| REQ-003 | 用户退出 | 清除会话信息并返回登录页面    | 高   | ✅    |

### 2.2 模版管理模块

| 需求编号    | 功能点    | 描述                          | 优先级 | 实现状态 |
|:------- |:------ |:--------------------------- |:--- |:---- |
| REQ-004 | 新建模版   | 创建新的碳排放核算模版                 | 高   | ✅    |
| REQ-005 | 打开模版   | 从列表中选择并打开已有模版进行编辑           | 高   | ✅    |
| REQ-006 | 另存为    | 将当前编辑的模版另存为新的模版             | 高   | ✅    |
| REQ-007 | 删除模版   | 删除指定的模版及其所有节点数据             | 高   | ✅    |
| REQ-008 | 多版本管理  | 支持模版的多版本保存和修改               | 中   | ✅    |
| REQ-009 | 模版列表展示 | 在首页展示所有模版列表，包含名称、描述、创建时间等信息 | 高   | ✅    |
| REQ-010 | 模版属性编辑 | 支持编辑模版的描述信息、是否使能、计划任务设置     | 高   | ✅    |

### 2.3 树形结构管理模块

| 需求编号    | 功能点      | 描述                                     | 优先级 | 实现状态 |
|:------- |:-------- |:-------------------------------------- |:--- |:---- |
| REQ-011 | 节点类型     | 支持四种节点类型：根节点、核算子节点、排放数据采集点、运输生产碳排放核算节点 | 高   | ✅    |
| REQ-012 | 添加节点     | 通过点击+号添加子节点，弹出对话框选择节点类型                | 高   | ✅    |
| REQ-013 | 删除节点     | 通过点击-号删除节点及其所有子节点，需确认提示                | 高   | ✅    |
| REQ-014 | 运输节点自动创建 | 运输生产碳排放核算节点点击+号后自动创建数据采集点子节点           | 高   | ✅    |
| REQ-015 | 节点类型限制   | 运输生产碳排放核算节点只能添加排放数据采集点类型               | 高   | ✅    |
| REQ-016 | 撤销(Undo) | 支持撤销最近10级节点添加/删除操作                     | 中   | ✅    |
| REQ-017 | 重做(Redo) | 支持重做最近10级撤销操作                          | 中   | ✅    |
| REQ-018 | 节点上下移动   | 支持同级节点的上下移动，不能跨级移动                     | 中   | ✅    |

### 2.4 属性配置模块

| 需求编号    | 功能点     | 描述                             | 优先级 | 实现状态 |
|:------- |:------- |:------------------------------ |:--- |:---- |
| REQ-019 | 属性展示    | 选中节点后在右侧属性栏展示节点属性              | 高   | ✅    |
| REQ-020 | 修改状态    | 初始状态下仅"修改"按钮可用，属性不可编辑          | 高   | ✅    |
| REQ-021 | 编辑属性    | 点击"修改"后属性变为可编辑状态，"保存"和"取消"按钮启用 | 高   | ✅    |
| REQ-022 | 保存属性    | 点击"保存"按钮保存修改的属性值               | 高   | ✅    |
| REQ-023 | 取消修改    | 点击"取消"按钮放弃所有修改，恢复原值            | 高   | ✅    |
| REQ-024 | 数据字典下拉框 | 属性中的下拉框选项从数据库数据字典读取            | 高   | ✅    |
| REQ-025 | 禁用状态样式  | 禁用状态与启用状态显示一致，通过透明覆盖层阻止交互      | 中   | ✅    |

### 2.5 计划任务模块

| 需求编号    | 功能点    | 描述                    | 优先级 | 实现状态 |
|:------- |:------ |:--------------------- |:--- |:---- |
| REQ-026 | 任务使能控制 | 设置模版是否启用计划任务          | 高   | ✅    |
| REQ-027 | 执行周期设置 | 支持按日、周、月、季、年执行        | 高   | ✅    |
| REQ-028 | 执行时间设置 | 设置任务执行的具体时间点          | 高   | ✅    |
| REQ-029 | 日期范围设置 | 设置任务执行的起始日期和截止日期      | 中   | ✅    |
| REQ-030 | 重复间隔设置 | 支持设置每N周/月/季/年执行一次     | 中   | ✅    |
| REQ-031 | 周执行配置  | 支持选择每周的具体星期几执行        | 高   | ✅    |
| REQ-032 | 月执行配置  | 支持选择每月的具体日期执行         | 高   | ✅    |
| REQ-033 | 自动执行   | 系统根据设定的周期自动执行碳排放核算工作  | 高   | ✅    |
| REQ-034 | 异步执行   | 核算任务在独立线程中执行，支持并发     | 高   | ✅    |
| REQ-035 | 线程池配置  | 自定义线程池，配置核心线程数、最大线程数等 | 中   | ✅    |
| REQ-036 | 数据采集调度 | 数据采集节点的定时任务调度         | 高   | ✅    |

### 2.6 数据字典模块

| 需求编号    | 功能点    | 描述                                 | 优先级 | 实现状态 |
|:------- |:------ |:---------------------------------- |:--- |:---- |
| REQ-037 | 字典管理   | 支持数据字典的增删改查（字典编码、字典名称、描述、排序、状态）    | 中   | ✅    |
| REQ-038 | 字典项管理  | 支持字典项的增删改查（所属字典、项编码、项值、父级编码、排序、状态） | 中   | ✅    |
| REQ-039 | 下拉框数据源 | 所有下拉框选项从数据字典读取并显示名称而非ID            | 高   | ✅    |
| REQ-040 | 字典分类管理 | 支持机车类型、统计口径、排放类别、数据来源、节点类型等字典分类    | 高   | ✅    |
| REQ-041 | 树形字典结构 | 支持字典项的层级关系（通过父级编码实现）               | 中   | ✅    |
| REQ-042 | 核算场景字典 | 碳排放核算场景分类，支持拼音首字母筛选                | 高   | ✅    |
| REQ-043 | 能耗用途字典 | 能源消耗用途分类，支持拼音首字母筛选                 | 高   | ✅    |

#### 2.6.1 字典分类说明

系统预置以下数据字典分类：

| 字典编码                 | 字典名称   | 描述               | 用途                          |
|:-------------------- |:------ |:---------------- |:--------------------------- |
| locomotive_type      | 机车类型   | 运输生产碳排放核算节点的机车类型 | 运输生产碳排放核算节点的机车类型选择          |
| statistical_caliber  | 统计口径类型 | 排放数据的统计口径分类      | 排放数据采集点的统计口径选择              |
| emission_category    | 排放数据大类 | 排放数据的一级分类        | 排放数据采集点的排放类别选择              |
| emission_subcategory | 排放数据小类 | 排放数据的二级分类        | 排放数据采集点的排放子类别选择             |
| data_source          | 数据来源   | 数据采集的来源方式        | 排放数据采集点的数据来源选择              |
| node_category        | 节点类型   | 核算子节点的组织类型分类     | 核算子节点的节点类型选择（总公司、分公司、站点、区域） |
| accounting_scenario  | 核算场景   | 碳排放来源归类场景        | 排放数据采集点的核算场景选择              |
| energy_use           | 能耗用途   | 能源消耗用途分类         | 排放数据采集点的能耗用途选择              |

#### 2.6.2 字典项说明

**机车类型 (locomotive_type)**

| 项编码  | 项值        | 排序  |
|:---- |:--------- |:--- |
| HXN3 | HXN3型内燃机车 | 1   |
| BN8  | 国能八轴交流机车  | 2   |
| BN12 | 国能十二轴交流机车 | 3   |

**统计口径类型 (statistical_caliber)**

| 项编码  | 项值        | 排序  |
|:---- |:--------- |:--- |
| PE   | 生产排放      | 1   |
| PAOE | 辅助和附属生产排放 | 2   |

**排放数据大类 (emission_category)**

| 项编码 | 项值    | 排序  |
|:--- |:----- |:--- |
| PE  | 购入的电力 | 1   |
| PH  | 购入的热力 | 2   |
| FF  | 化石燃料  | 3   |
| EP  | 输出的电力 | 4   |
| WT  | 废弃物处理 | 5   |

**排放数据小类 (emission_subcategory)**

| 项编码    | 项值        | 排序  |
|:------ |:--------- |:--- |
| PE_PF  | 生产设施用电    | 1   |
| PE_AS  | 辅助生产系统用电  | 2   |
| PE_SS  | 附属生产系统用电  | 3   |
| PH_HD  | 热力数据      | 4   |
| PH_HW  | 质量单位计量的热水 | 5   |
| PH_HS  | 质量单位计量的蒸汽 | 6   |
| FF_G   | 汽油        | 7   |
| FF_D   | 柴油        | 8   |
| FF_C   | 原油        | 9   |
| FF_F   | 燃料油       | 10  |
| FF_LNG | 液化天然气     | 11  |
| FF_LPG | 液化石油气     | 12  |
| FF_NG  | 天然气       | 13  |
| FF_BFG | 高炉煤气      | 14  |
| FF_COG | 转炉煤气      | 15  |
| FF_COK | 焦炉煤气      | 16  |
| FF_B   | 烟煤        | 17  |
| FF_LB  | 褐煤        | 18  |
| FF_CK  | 焦炭        | 19  |
| FF_PC  | 石油焦       | 20  |
| WT_SW  | 固体废弃物处理排放 | 21  |
| WT_WW  | 废水处理排放    | 22  |

**数据来源 (data_source)**

| 项编码    | 项值    | 排序  |
|:------ |:----- |:--- |
| MANUAL | 手工录入  | 1   |
| DB     | 数据库   | 2   |
| API    | API输入 | 3   |

**节点类型 (node_category)**

| 项编码     | 项值  | 排序  |
|:------- |:--- |:--- |
| HQ      | 总公司 | 1   |
| BRANCH  | 分公司 | 2   |
| STATION | 站点  | 3   |
| REGION  | 区域  | 4   |

**核算场景 (accounting_scenario)**

| 项编码                     | 项值          | 排序  |
|:----------------------- |:----------- |:--- |
| TRACTION_POWER          | 牵引变电所/牵引供电  | 1   |
| TRAIN_OPERATION         | 列车运行        | 2   |
| DISPATCH_COMM           | 行车调度/通信指挥   | 3   |
| VEHICLE_REPAIR          | 车辆维修/机修车间   | 4   |
| LINE_MAINTENANCE        | 线路维护保养      | 5   |
| STATION_SYSTEM          | 场站系统        | 6   |
| GARAGE                  | 车库          | 7   |
| WORKSHOP_BATHROOM       | 车间浴室        | 8   |
| OFFICE_BUILDING         | 办公楼         | 9   |
| STAFF_CANTEEN           | 职工食堂        | 10  |
| STAFF_DORMITORY         | 职工宿舍        | 11  |
| INTERNAL_VEHICLE        | 内部运营车辆      | 12  |
| INTERNAL_VEHICLE_SINGLE | 内部运营车辆（单台车） | 13  |
| WASTE_DISPOSAL          | 废弃物处理场所     | 14  |
| OTHER_SCENARIO          | 其他场景        | 15  |

**能耗用途 (energy_use)**

| 项编码                    | 项值     | 排序  |
|:---------------------- |:------ |:--- |
| LIGHTING               | 照明     | 1   |
| AIR_CONDITIONING       | 空调     | 2   |
| HEATING                | 取暖     | 3   |
| HEAT_SUPPLY            | 供热     | 4   |
| ELEVATOR               | 电梯     | 5   |
| WATER_PUMP             | 水泵     | 6   |
| VEHICLE                | 汽车     | 7   |
| EQUIPMENT              | 设备用能   | 8   |
| TRAIN_OPERATION_ENERGY | 列车运行用能 | 9   |
| MIXED_METERING         | 混合用能计量 | 10  |
| OTHER_USE              | 其他     | 11  |

### 2.7 数据来源系统管理模块

| 需求编号    | 功能点        | 描述                   | 优先级 | 实现状态 |
|:------- |:---------- |:-------------------- |:--- |:---- |
| REQ-044 | 数据来源系统设置弹窗 | 提供数据来源系统的增删改查操作界面    | 高   | ✅    |
| REQ-045 | 新增数据来源系统   | 添加新的数据来源系统记录         | 高   | ✅    |
| REQ-046 | 修改数据来源系统   | 编辑已有数据来源系统的名称和说明     | 高   | ✅    |
| REQ-047 | 删除数据来源系统   | 删除数据来源系统（需二次确认）      | 高   | ✅    |
| REQ-048 | 查询数据来源系统   | 支持拼音首字母筛选和非英文字符的模糊查询 | 高   | ✅    |
| REQ-049 | 拼音首字母筛选    | 输入英文字母自动按拼音首字母筛选     | 高   | ✅    |
| REQ-050 | 选中并返回      | 选择数据来源系统后回填到输入框      | 高   | ✅    |

### 2.8 排放数据采集点扩展字段模块

| 需求编号    | 功能点      | 描述                        | 优先级 | 实现状态 |
|:------- |:-------- |:------------------------- |:--- |:---- |
| REQ-051 | 核算场景选择   | 下拉框选择核算场景，数据来源于数据字典       | 高   | ✅    |
| REQ-052 | 能耗用途选择   | 下拉框选择能耗用途，数据来源于数据字典       | 高   | ✅    |
| REQ-053 | 是否累计量    | 是/否选择框，默认值为"是"            | 高   | ✅    |
| REQ-054 | 是否移动源    | 是/否选择框，默认值为"否"            | 高   | ✅    |
| REQ-055 | 计量单位选择   | 根据排放大类和小类动态显示不同的计量单位选项    | 高   | ✅    |
| REQ-056 | 数据来源系统选择 | 通过弹窗选择数据来源系统，禁止手动录入       | 高   | ✅    |
| REQ-057 | 获取方式录入   | JSON格式输入框，填写API接口、请求参数等信息 | 高   | ✅    |

### 2.9 排放因子库模块

| 需求编号    | 功能点          | 描述                     | 优先级 | 实现状态 |
|:------- |:------------ |:---------------------- |:--- |:---- |
| REQ-058 | 电力碳排放因子库     | 存储电力碳排放因子数据，支持增删改查     | 高   | ✅    |
| REQ-059 | 化石燃料排放因子库    | 存储化石燃料排放因子数据，支持增删改查    | 高   | ✅    |
| REQ-060 | 饱和蒸汽热焓值表     | 存储饱和蒸汽热焓值数据，支持增删改查     | 高   | ✅    |
| REQ-061 | 过热蒸汽热焓值表     | 存储过热蒸汽热焓值数据，支持增删改查     | 高   | ✅    |
| REQ-062 | 热力排放因子库      | 存储热力排放因子数据，支持增删改查      | 高   | ✅    |
| REQ-063 | 固体废弃物焚烧排放因子库 | 存储固体废弃物焚烧排放因子数据，支持增删改查 | 高   | ✅    |
| REQ-064 | 废水处理排放因子库    | 存储废水处理排放因子数据，支持增删改查    | 高   | ✅    |

#### 2.8.1 计量单位动态规则

| 排放数据大类      | 排放数据小类                   | 计量单位选项                               | 默认值          |
|:----------- |:------------------------ |:------------------------------------ |:------------ |
| 购入的电力/输出的电力 | -                        | MWh、KWh、GWh                          | MWh          |
| 化石燃料        | 烟煤、褐煤、焦炭、石油焦             | 吨                                    | 吨            |
| 化石燃料        | 原油、燃料油、汽油、柴油、液化天然气、液化石油气 | 吨、升、千克                               | 吨            |
| 化石燃料        | 天然气、高炉煤气、转炉煤气、焦炉煤气       | 10⁴Nm³(万立方米)、10³Nm³(千立方米)、Nm³(标准立方米) | 10⁴Nm³(万立方米) |
| 购入的热力       | 热力数据                     | 吉焦(GJ)、兆焦(MJ)                        | 吉焦(GJ)       |
| 购入的热力       | 质量单位计量的蒸汽、质量单位计量的热水      | 吨                                    | 吨            |
| 废弃物处理       | 废水处理排放                   | 立方米                                  | 立方米          |
| 废弃物处理       | 固体废弃物处理排放                | 吨                                    | 吨            |

---

## 3. 非功能需求

### 3.1 性能需求

| 需求编号    | 描述                 |
|:------- |:------------------ |
| NFR-001 | 页面加载时间不超过3秒        |
| NFR-002 | 节点操作响应时间不超过500毫秒   |
| NFR-003 | 支持至少100个并发用户       |
| NFR-004 | 定时任务调度精度不超过1分钟     |
| NFR-005 | 大量任务同时执行时保证时间判断一致性 |

### 3.2 安全需求

| 需求编号    | 描述             |
|:------- |:-------------- |
| NFR-006 | 用户密码需加密存储      |
| NFR-007 | 支持会话超时自动退出     |
| NFR-008 | 所有API接口需进行身份验证 |
| NFR-009 | 防止SQL注入攻击      |
| NFR-010 | CORS跨域保护       |

### 3.3 可用性需求

| 需求编号    | 描述                             |
|:------- |:------------------------------ |
| NFR-011 | 系统可用性不低于99.5%                  |
| NFR-012 | 提供友好的错误提示信息                    |
| NFR-013 | 支持主流浏览器（Chrome、Firefox、Safari） |
| NFR-014 | 禁用状态控件与启用状态视觉一致                |

---

## 4. 数据库设计

### 4.1 数据库表结构

#### 4.1.1 用户表 (user)

| 字段名        | 类型           | 约束                          | 描述       |
|:---------- |:------------ |:--------------------------- |:-------- |
| id         | BIGINT       | PRIMARY KEY, AUTO_INCREMENT | 用户ID     |
| name       | VARCHAR(100) | NOT NULL                    | 用户姓名     |
| username   | VARCHAR(100) | NOT NULL, UNIQUE            | 用户名      |
| password   | VARCHAR(255) | NOT NULL                    | 密码（加密存储） |
| created_at | DATETIME     | -                           | 创建时间     |
| updated_at | DATETIME     | -                           | 更新时间     |

#### 4.1.2 模版表 (template)

| 字段名         | 类型           | 约束                          | 描述             |
|:----------- |:------------ |:--------------------------- |:-------------- |
| id          | BIGINT       | PRIMARY KEY, AUTO_INCREMENT | 模版ID           |
| name        | VARCHAR(200) | NOT NULL                    | 模版名称           |
| description | VARCHAR(500) | -                           | 描述信息           |
| created_by  | BIGINT       | NOT NULL, FOREIGN KEY       | 创建人ID          |
| created_at  | DATETIME     | -                           | 创建时间           |
| updated_by  | BIGINT       | FOREIGN KEY                 | 更新人ID          |
| updated_at  | DATETIME     | -                           | 更新时间           |
| version     | INT          | DEFAULT 1                   | 版本号            |
| enabled     | BOOLEAN      | DEFAULT TRUE                | 是否启用           |
| task_config | TEXT         | -                           | 计划任务配置(JSON格式) |

#### 4.1.3 排放节点表 (emission_node)

| 字段名             | 类型           | 约束                          | 描述     |
|:--------------- |:------------ |:--------------------------- |:------ |
| id              | BIGINT       | PRIMARY KEY, AUTO_INCREMENT | 节点ID   |
| name            | VARCHAR(200) | NOT NULL                    | 节点名称   |
| type_id         | BIGINT       | NOT NULL, FOREIGN KEY       | 节点类型ID |
| parent_id       | BIGINT       | FOREIGN KEY                 | 父节点ID  |
| template_id     | BIGINT       | NOT NULL, FOREIGN KEY       | 所属模版ID |
| locomotive_type | VARCHAR(100) | -                           | 机车类型   |
| sort_order      | INT          | DEFAULT 0                   | 排序顺序   |
| created_at      | DATETIME     | -                           | 创建时间   |
| updated_at      | DATETIME     | -                           | 更新时间   |

#### 4.1.4 节点配置表 (emission_node_config)

| 字段名                    | 类型            | 约束                            | 描述             |
|:---------------------- |:------------- |:----------------------------- |:-------------- |
| id                     | BIGINT        | PRIMARY KEY, AUTO_INCREMENT   | 配置ID           |
| node_id                | BIGINT        | NOT NULL, FOREIGN KEY, UNIQUE | 节点ID           |
| statistical_caliber    | VARCHAR(100)  | -                             | 统计口径           |
| emission_category      | VARCHAR(100)  | -                             | 排放类别           |
| emission_subcategory   | VARCHAR(100)  | -                             | 排放子类别          |
| carbon_emission_factor | TEXT          | -                             | 碳排放因子          |
| data_source            | VARCHAR(50)   | -                             | 数据来源           |
| accounting_scenario    | VARCHAR(100)  | -                             | 核算场景           |
| energy_use             | VARCHAR(100)  | -                             | 能耗用途           |
| is_cumulative          | VARCHAR(10)   | DEFAULT 'true'                | 是否累计量          |
| is_mobile_source       | VARCHAR(10)   | DEFAULT 'false'               | 是否移动源          |
| measurement_unit       | VARCHAR(50)   | -                             | 计量单位           |
| data_source_system     | VARCHAR(100)  | -                             | 数据来源系统         |
| acquisition_method     | TEXT          | -                             | 获取方式(API信息)    |
| allocation_ratio       | DECIMAL(10,4) | -                             | 分配比例           |
| has_sub_table          | BOOLEAN       | DEFAULT FALSE                 | 是否有子表          |
| error_constraint       | DECIMAL(10,4) | -                             | 误差约束           |
| update_cycle           | VARCHAR(50)   | -                             | 更新周期           |
| update_time            | VARCHAR(20)   | -                             | 更新时间           |
| task_config            | TEXT          | -                             | 计划任务配置(JSON格式) |
| collection_description | TEXT          | -                             | 采集说明           |
| equipment_code         | VARCHAR(100)  | -                             | 设备编号           |
| created_by             | BIGINT        | -                             | 创建人ID          |
| updated_by             | BIGINT        | -                             | 更新人ID          |
| created_at             | DATETIME      | -                             | 创建时间           |
| updated_at             | DATETIME      | -                             | 更新时间           |

#### 4.1.5 排放节点信息表 (emission_node_info)

| 字段名                            | 类型          | 约束                            | 描述                   |
|:------------------------------ |:----------- |:----------------------------- |:-------------------- |
| id                             | BIGINT      | PRIMARY KEY, AUTO_INCREMENT   | 主键ID                 |
| node_id                        | BIGINT      | NOT NULL, UNIQUE, FOREIGN KEY | 节点ID                 |
| node_code                      | VARCHAR(50) | -                             | 节点编码                 |
| short_name                     | VARCHAR(50) | -                             | 节点名称简称，用于自动生成子节点名称前缀 |
| include_in_calculation         | BOOLEAN     | DEFAULT TRUE                  | 是否纳入碳排放核算            |
| node_category                  | VARCHAR(50) | -                             | 节点类型                 |
| unit_description               | TEXT        | -                             | 单位说明                 |
| org_boundary_description       | TEXT        | -                             | 组织边界说明               |
| operation_boundary_description | TEXT        | -                             | 运营边界说明               |

#### 4.1.6 数据字典表 (data_dict)

| 字段名         | 类型           | 约束                          | 描述   |
|:----------- |:------------ |:--------------------------- |:---- |
| id          | BIGINT       | PRIMARY KEY, AUTO_INCREMENT | 字典ID |
| dict_code   | VARCHAR(50)  | NOT NULL, UNIQUE            | 字典编码 |
| dict_name   | VARCHAR(100) | NOT NULL                    | 字典名称 |
| description | VARCHAR(200) | -                           | 描述   |

#### 4.1.7 字典项表 (data_dict_item)

| 字段名        | 类型           | 约束                          | 描述     |
|:---------- |:------------ |:--------------------------- |:------ |
| id         | BIGINT       | PRIMARY KEY, AUTO_INCREMENT | 字典项ID  |
| dict_id    | BIGINT       | NOT NULL, FOREIGN KEY       | 所属字典ID |
| item_code  | VARCHAR(50)  | NOT NULL                    | 项编码    |
| item_value | VARCHAR(100) | NOT NULL                    | 项值     |
| sort_order | INT          | DEFAULT 0                   | 排序顺序   |
| status     | BOOLEAN      | DEFAULT TRUE                | 是否启用   |

#### 4.1.8 节点类型表 (node_type)

| 字段名         | 类型           | 约束                          | 描述   |
|:----------- |:------------ |:--------------------------- |:---- |
| id          | BIGINT       | PRIMARY KEY, AUTO_INCREMENT | 类型ID |
| type_name   | VARCHAR(100) | NOT NULL                    | 类型名称 |
| description | VARCHAR(200) | -                           | 描述   |

#### 4.1.9 数据来源系统表 (data_source_system)

| 字段名         | 类型           | 约束                          | 描述       |
|:----------- |:------------ |:--------------------------- |:-------- |
| id          | BIGINT       | PRIMARY KEY, AUTO_INCREMENT | 主键ID     |
| system_name | VARCHAR(100) | NOT NULL, UNIQUE            | 数据来源系统名称 |
| description | VARCHAR(500) | -                           | 说明       |
| pinyin_code | VARCHAR(50)  | -                           | 拼音首字母编码  |
| created_by  | BIGINT       | -                           | 创建人ID    |
| updated_by  | BIGINT       | -                           | 更新人ID    |
| created_at  | DATETIME     | -                           | 创建时间     |
| updated_at  | DATETIME     | -                           | 更新时间     |

#### 4.1.10 电力碳排放因子库表 (electricity_carbon_emission_factor)

| 字段名          | 类型            | 约束                                                    | 描述      |
|:------------ |:------------- |:----------------------------------------------------- |:------- |
| id           | BIGINT        | PRIMARY KEY, AUTO_INCREMENT                           | 主键ID    |
| factor_name  | VARCHAR(200)  | NOT NULL, UNIQUE                                      | 碳排放因子名称 |
| factor_value | DECIMAL(15,6) | -                                                     | 碳排放因子   |
| unit         | VARCHAR(50)   | -                                                     | 单位      |
| description  | TEXT          | -                                                     | 碳排放因子说明 |
| created_by   | BIGINT        | -                                                     | 创建人ID   |
| created_at   | TIMESTAMP     | DEFAULT CURRENT_TIMESTAMP                             | 创建时间    |
| updated_by   | BIGINT        | -                                                     | 修改人ID   |
| updated_at   | TIMESTAMP     | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 修改时间    |

#### 4.1.11 化石燃料排放因子库表 (fossil_fuel_emission_factor)

| 字段名                          | 类型            | 约束                                                    | 描述      |
|:---------------------------- |:------------- |:----------------------------------------------------- |:------- |
| id                           | BIGINT        | PRIMARY KEY, AUTO_INCREMENT                           | 主键ID    |
| emission_factor_name         | VARCHAR(200)  | NOT NULL, UNIQUE                                      | 碳排放因子名称 |
| fuel_type                    | VARCHAR(200)  | -                                                     | 燃料品种    |
| source                       | VARCHAR(200)  | -                                                     | 来源      |
| unit                         | VARCHAR(50)   | -                                                     | 计量单位    |
| lower_heating_value          | DECIMAL(15,6) | -                                                     | 低位发热量   |
| carbon_content_per_unit_heat | DECIMAL(15,6) | -                                                     | 单位热值含碳量 |
| fuel_oxidation_rate          | DECIMAL(5,4)  | -                                                     | 燃料氧化率   |
| emission_factor              | DECIMAL(15,6) | -                                                     | 碳排放因子   |
| factor_unit                  | VARCHAR(50)   | -                                                     | 单位      |
| description                  | TEXT          | -                                                     | 碳排放因子说明 |
| created_by                   | BIGINT        | -                                                     | 创建人ID   |
| created_at                   | TIMESTAMP     | DEFAULT CURRENT_TIMESTAMP                             | 创建时间    |
| updated_by                   | BIGINT        | -                                                     | 修改人ID   |
| updated_at                   | TIMESTAMP     | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 修改时间    |

#### 4.1.12 饱和蒸汽热焓值表 (saturated_steam_enthalpy)

| 字段名         | 类型            | 约束                                                    | 描述       |
|:----------- |:------------- |:----------------------------------------------------- |:-------- |
| id          | BIGINT        | PRIMARY KEY, AUTO_INCREMENT                           | 主键ID     |
| pressure    | DECIMAL(10,4) | -                                                     | 压力（Mpa）  |
| temperature | DECIMAL(10,2) | -                                                     | 温度（°C）   |
| enthalpy    | DECIMAL(10,2) | -                                                     | 焓（KJ/kg） |
| description | TEXT          | -                                                     | 描述       |
| created_by  | BIGINT        | -                                                     | 创建人ID    |
| created_at  | TIMESTAMP     | DEFAULT CURRENT_TIMESTAMP                             | 创建时间     |
| updated_by  | BIGINT        | -                                                     | 修改人ID    |
| updated_at  | TIMESTAMP     | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 修改时间     |

#### 4.1.13 过热蒸汽热焓值表 (superheated_steam_enthalpy)

| 字段名         | 类型            | 约束                                                    | 描述       |
|:----------- |:------------- |:----------------------------------------------------- |:-------- |
| id          | BIGINT        | PRIMARY KEY, AUTO_INCREMENT                           | 主键ID     |
| temperature | DECIMAL(10,2) | -                                                     | 温度（°C）   |
| pressure    | DECIMAL(10,4) | -                                                     | 压力（Mpa）  |
| enthalpy    | DECIMAL(10,2) | -                                                     | 焓（KJ/kg） |
| description | TEXT          | -                                                     | 描述       |
| created_by  | BIGINT        | -                                                     | 创建人ID    |
| created_at  | TIMESTAMP     | DEFAULT CURRENT_TIMESTAMP                             | 创建时间     |
| updated_by  | BIGINT        | -                                                     | 修改人ID    |
| updated_at  | TIMESTAMP     | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 修改时间     |

#### 4.1.14 热力排放因子库表 (thermal_emission_factor)

| 字段名                  | 类型            | 约束                                                    | 描述      |
|:-------------------- |:------------- |:----------------------------------------------------- |:------- |
| id                   | BIGINT        | PRIMARY KEY, AUTO_INCREMENT                           | 主键ID    |
| emission_factor_name | VARCHAR(200)  | NOT NULL, UNIQUE                                      | 碳排放因子名称 |
| emission_factor      | DECIMAL(15,6) | -                                                     | 碳排放因子   |
| unit                 | VARCHAR(50)   | -                                                     | 单位      |
| source               | VARCHAR(200)  | -                                                     | 来源      |
| description          | TEXT          | -                                                     | 碳排放因子说明 |
| created_by           | BIGINT        | -                                                     | 创建人ID   |
| created_at           | TIMESTAMP     | DEFAULT CURRENT_TIMESTAMP                             | 创建时间    |
| updated_by           | BIGINT        | -                                                     | 修改人ID   |
| updated_at           | TIMESTAMP     | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 修改时间    |

#### 4.1.15 固体废弃物焚烧排放因子表 (waste_incineration_factor)

| 字段名                  | 类型            | 约束                                                    | 描述              |
|:-------------------- |:------------- |:----------------------------------------------------- |:--------------- |
| id                   | BIGINT        | PRIMARY KEY, AUTO_INCREMENT                           | 主键ID            |
| emission_factor_name | VARCHAR(200)  | NOT NULL, UNIQUE                                      | 碳排放因子名称         |
| waste_type           | VARCHAR(200)  | -                                                     | 固体废物种类          |
| ccw                  | DECIMAL(10,4) | -                                                     | 废弃物中的碳含量比例      |
| fcf                  | DECIMAL(10,4) | -                                                     | 废弃物中的化石碳在总碳中的比例 |
| ce                   | DECIMAL(10,4) | -                                                     | 废弃物焚烧炉的完全燃烧效率   |
| emission_factor      | DECIMAL(15,6) | -                                                     | 碳排放因子           |
| unit                 | VARCHAR(50)   | -                                                     | 单位              |
| source               | VARCHAR(200)  | -                                                     | 来源              |
| description          | TEXT          | -                                                     | 碳排放因子说明         |
| created_by           | BIGINT        | -                                                     | 创建人ID           |
| created_at           | TIMESTAMP     | DEFAULT CURRENT_TIMESTAMP                             | 创建时间            |
| updated_by           | BIGINT        | -                                                     | 修改人ID           |
| updated_at           | TIMESTAMP     | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 修改时间            |

#### 4.1.16 废水处理排放因子表 (wastewater_treatment_factor)

| 字段名                  | 类型            | 约束                                                    | 描述                      |
|:-------------------- |:------------- |:----------------------------------------------------- |:----------------------- |
| id                   | BIGINT        | PRIMARY KEY, AUTO_INCREMENT                           | 主键ID                    |
| emission_factor_name | VARCHAR(200)  | NOT NULL, UNIQUE                                      | 碳排放因子名称                 |
| wastewater_type      | VARCHAR(200)  | -                                                     | 处理废水种类                  |
| od                   | DECIMAL(10,4) | -                                                     | 需氧浓度系数（COD或BOD，单位：mg/L） |
| bo                   | DECIMAL(10,4) | -                                                     | 最大甲烷产生能力(单位tCH4/t）      |
| mcf                  | DECIMAL(10,4) | -                                                     | 甲烷修正因子                  |
| gwp                  | DECIMAL(10,2) | -                                                     | 甲烷的全球变暖潜能值，缺省为28        |
| emission_factor      | DECIMAL(15,6) | -                                                     | 碳排放因子                   |
| unit                 | VARCHAR(50)   | -                                                     | 单位                      |
| source               | VARCHAR(200)  | -                                                     | 来源                      |
| description          | TEXT          | -                                                     | 碳排放因子说明                 |
| created_by           | BIGINT        | -                                                     | 创建人ID                   |
| created_at           | TIMESTAMP     | DEFAULT CURRENT_TIMESTAMP                             | 创建时间                    |
| updated_by           | BIGINT        | -                                                     | 修改人ID                   |
| updated_at           | TIMESTAMP     | DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP | 修改时间                    |

### 4.2 数据库关系图

```
user ───< template ───< emission_node ───< emission_node_config
                          │
                          ▼
                    node_type
                          │
                          ▼
              data_dict ───< data_dict_item
                          │
                          ▼
              data_source_system
```

---

## 5. API接口设计

### 5.1 用户接口

| API路径              | HTTP方法 | 功能描述 |
|:------------------ |:------ |:---- |
| `/api/user/login`  | POST   | 用户登录 |
| `/api/user/logout` | POST   | 用户登出 |

### 5.2 模版接口

| API路径                           | HTTP方法 | 功能描述               |
|:------------------------------- |:------ |:------------------ |
| `/api/template`                 | GET    | 获取所有模版列表           |
| `/api/template/{id}`            | GET    | 获取单个模版详情           |
| `/api/template`                 | POST   | 创建新模版              |
| `/api/template/{id}`            | PUT    | 更新模版名称             |
| `/api/template/{id}/properties` | PUT    | 更新模版属性（描述、使能、任务配置） |
| `/api/template/{id}/copy`       | POST   | 复制模版               |
| `/api/template/{id}`            | DELETE | 删除模版               |

### 5.3 节点接口

| API路径                             | HTTP方法 | 功能描述         |
|:--------------------------------- |:------ |:------------ |
| `/api/node/template/{templateId}` | GET    | 获取模版下所有节点    |
| `/api/node`                       | POST   | 创建新节点        |
| `/api/node/{id}`                  | PUT    | 更新节点信息       |
| `/api/node/{id}`                  | DELETE | 删除节点         |
| `/api/node/{id}/config`           | GET    | 获取节点配置       |
| `/api/node/{id}/config`           | PUT    | 更新节点配置       |
| `/api/node/{id}/move`             | POST   | 移动节点位置（上下移动） |
| `/api/node/{id}/equipment-code`   | GET    | 获取设备编号       |

### 5.4 数据字典接口

| API路径                  | HTTP方法 | 功能描述       |
|:---------------------- |:------ |:---------- |
| `/api/dict`            | GET    | 获取所有字典     |
| `/api/dict/{dictCode}` | GET    | 获取指定字典的所有项 |

### 5.5 数据来源系统接口

| API路径                             | HTTP方法 | 功能描述       |
|:--------------------------------- |:------ |:---------- |
| `/api/data-source-systems`        | GET    | 获取所有数据来源系统 |
| `/api/data-source-systems/{id}`   | GET    | 获取单个数据来源系统 |
| `/api/data-source-systems/search` | GET    | 搜索数据来源系统   |
| `/api/data-source-systems`        | POST   | 创建数据来源系统   |
| `/api/data-source-systems/{id}`   | PUT    | 更新数据来源系统   |
| `/api/data-source-systems/{id}`   | DELETE | 删除数据来源系统   |
| `/api/data-source-systems/exists` | GET    | 检查系统名称是否存在 |

### 5.6 排放因子库接口

#### 5.6.1 电力碳排放因子库

| API路径                                    | HTTP方法 | 功能描述          |
|:---------------------------------------- |:------ |:------------- |
| `/api/electricity-carbon-factors`        | GET    | 获取所有电力碳排放因子   |
| `/api/electricity-carbon-factors/{id}`   | GET    | 根据ID获取电力碳排放因子 |
| `/api/electricity-carbon-factors/search` | GET    | 根据名称搜索电力碳排放因子 |
| `/api/electricity-carbon-factors`        | POST   | 创建新的电力碳排放因子   |
| `/api/electricity-carbon-factors/{id}`   | PUT    | 更新电力碳排放因子     |
| `/api/electricity-carbon-factors/{id}`   | DELETE | 删除电力碳排放因子     |

#### 5.6.2 化石燃料排放因子库

| API路径                             | HTTP方法 | 功能描述           |
|:--------------------------------- |:------ |:-------------- |
| `/api/fossil-fuel-factors`        | GET    | 获取所有化石燃料排放因子   |
| `/api/fossil-fuel-factors/{id}`   | GET    | 根据ID获取化石燃料排放因子 |
| `/api/fossil-fuel-factors/search` | GET    | 根据燃料类型搜索排放因子   |
| `/api/fossil-fuel-factors`        | POST   | 创建新的化石燃料排放因子   |
| `/api/fossil-fuel-factors/{id}`   | PUT    | 更新化石燃料排放因子     |
| `/api/fossil-fuel-factors/{id}`   | DELETE | 删除化石燃料排放因子     |

#### 5.6.3 饱和蒸汽热焓值表

| API路径                                               | HTTP方法 | 功能描述          |
|:--------------------------------------------------- |:------ |:------------- |
| `/api/saturated-steam-enthalpy`                     | GET    | 获取所有饱和蒸汽热焓值   |
| `/api/saturated-steam-enthalpy/{id}`                | GET    | 根据ID获取饱和蒸汽热焓值 |
| `/api/saturated-steam-enthalpy/pressure/{pressure}` | GET    | 根据压力获取饱和蒸汽热焓值 |
| `/api/saturated-steam-enthalpy`                     | POST   | 创建新的饱和蒸汽热焓值记录 |
| `/api/saturated-steam-enthalpy/{id}`                | PUT    | 更新饱和蒸汽热焓值记录   |
| `/api/saturated-steam-enthalpy/{id}`                | DELETE | 删除饱和蒸汽热焓值记录   |

#### 5.6.4 过热蒸汽热焓值表

| API路径                                                 | HTTP方法 | 功能描述          |
|:----------------------------------------------------- |:------ |:------------- |
| `/api/superheated-steam-enthalpy`                     | GET    | 获取所有过热蒸汽热焓值   |
| `/api/superheated-steam-enthalpy/{id}`                | GET    | 根据ID获取过热蒸汽热焓值 |
| `/api/superheated-steam-enthalpy/pressure/{pressure}` | GET    | 根据压力获取过热蒸汽热焓值 |
| `/api/superheated-steam-enthalpy`                     | POST   | 创建新的过热蒸汽热焓值记录 |
| `/api/superheated-steam-enthalpy/{id}`                | PUT    | 更新过热蒸汽热焓值记录   |
| `/api/superheated-steam-enthalpy/{id}`                | DELETE | 删除过热蒸汽热焓值记录   |

#### 5.6.5 热力排放因子库

| API路径                                       | HTTP方法 | 功能描述         |
|:------------------------------------------- |:------ |:------------ |
| `/api/thermal-emission-factors`             | GET    | 获取所有热力排放因子   |
| `/api/thermal-emission-factors/{id}`        | GET    | 根据ID获取热力排放因子 |
| `/api/thermal-emission-factors/name/{name}` | GET    | 根据名称获取热力排放因子 |
| `/api/thermal-emission-factors`             | POST   | 创建新的热力排放因子   |
| `/api/thermal-emission-factors/{id}`        | PUT    | 更新热力排放因子     |
| `/api/thermal-emission-factors/{id}`        | DELETE | 删除热力排放因子     |

#### 5.6.6 固体废弃物焚烧排放因子库

| API路径                                         | HTTP方法 | 功能描述              |
|:--------------------------------------------- |:------ |:----------------- |
| `/api/waste-incineration-factors`             | GET    | 获取所有固体废弃物焚烧排放因子   |
| `/api/waste-incineration-factors/{id}`        | GET    | 根据ID获取固体废弃物焚烧排放因子 |
| `/api/waste-incineration-factors/name/{name}` | GET    | 根据名称获取固体废弃物焚烧排放因子 |
| `/api/waste-incineration-factors`             | POST   | 创建新的固体废弃物焚烧排放因子   |
| `/api/waste-incineration-factors/{id}`        | PUT    | 更新固体废弃物焚烧排放因子     |
| `/api/waste-incineration-factors/{id}`        | DELETE | 删除固体废弃物焚烧排放因子     |

#### 5.6.7 废水处理排放因子库

| API路径                                           | HTTP方法 | 功能描述           |
|:----------------------------------------------- |:------ |:-------------- |
| `/api/wastewater-treatment-factors`             | GET    | 获取所有废水处理排放因子   |
| `/api/wastewater-treatment-factors/{id}`        | GET    | 根据ID获取废水处理排放因子 |
| `/api/wastewater-treatment-factors/name/{name}` | GET    | 根据名称获取废水处理排放因子 |
| `/api/wastewater-treatment-factors`             | POST   | 创建新的废水处理排放因子   |
| `/api/wastewater-treatment-factors/{id}`        | PUT    | 更新废水处理排放因子     |
| `/api/wastewater-treatment-factors/{id}`        | DELETE | 删除废水处理排放因子     |

---

## 6. 核心组件说明

### 6.1 调度器组件

#### 6.1.1 EmissionScheduler

| 属性     | 说明                         |
|:------ |:-------------------------- |
| 类名     | `EmissionScheduler`        |
| 调度频率   | 每分钟检查一次                    |
| 功能     | 根据模版的计划任务配置，在指定时间自动执行碳排放核算 |
| 时间判断精度 | 分钟级别                       |
| 异步执行   | 是，使用自定义线程池                 |

#### 6.1.2 DataCollectionScheduler

| 属性     | 说明                        |
|:------ |:------------------------- |
| 类名     | `DataCollectionScheduler` |
| 调度频率   | 每分钟检查一次                   |
| 功能     | 根据数据采集节点的配置，在指定时间自动执行数据采集 |
| 监控节点类型 | typeId=3（数据采集点）           |

### 6.2 异步任务服务

#### AsyncTaskService

| 属性     | 说明                  |
|:------ |:------------------- |
| 类名     | `AsyncTaskService`  |
| 线程池名称  | `asyncTaskExecutor` |
| 核心线程数  | CPU核心数 * 2          |
| 最大线程数  | 核心线程数 ** 2*         |
| 队列容量   | 200                 |
| 空闲存活时间 | 60秒                 |
| 线程名称前缀 | `my-async-`         |
| 拒绝策略   | `CallerRunsPolicy`  |

### 6.3 线程池配置

| 配置项    | 值                | 说明         |
|:------ |:---------------- |:---------- |
| 核心线程数  | CPU核数 * 2        | IO密集型任务建议值 |
| 最大线程数  | 16               | 应对突发流量     |
| 队列容量   | 200              | 等待队列大小     |
| 空闲存活时间 | 60秒              | 空闲线程回收时间   |
| 线程名称前缀 | `my-async-`      | 便于日志追踪     |
| 拒绝策略   | CallerRunsPolicy | 队列满时调用线程执行 |
| 关闭等待   | 60秒              | 等待任务完成再关闭  |

---

## 7. 计划任务配置JSON结构

```json
{
  "cycleType": "DAILY",
  "startDate": "2026-01-01",
  "endDate": "",
  "executionTime": "02:00",
  "interval": 1,
  "weekDays": [],
  "monthDays": "",
  "yearMonths": "",
  "yearDays": []
}
```

| 字段            | 类型           | 说明                                         |
|:------------- |:------------ |:------------------------------------------ |
| cycleType     | String       | 执行周期：DAILY/WEEKLY/MONTHLY/QUARTERLY/YEARLY |
| startDate     | String       | 起始日期(yyyy-MM-dd)                           |
| endDate       | String       | 截止日期(yyyy-MM-dd)，空表示无限制                    |
| executionTime | String       | 执行时间(HH:mm)                                |
| interval      | Integer      | 重复间隔，默认1                                   |
| weekDays      | Array        | 周执行的星期列表(1-7)                              |
| monthDays     | Array/String | 月执行的日期列表                                   |
| yearMonths    | Array/String | 年执行的月份列表                                   |
| yearDays      | Array/String | 年执行的日期列表                                   |

---

## 8. 版本历史

| 版本  | 日期         | 变更描述                                                            |
|:--- |:---------- |:--------------------------------------------------------------- |
| 1.0 | 2024-01-01 | 初始版本，包含基本功能                                                     |
| 1.1 | 2024-01-15 | 添加undo/redo功能                                                   |
| 1.2 | 2024-01-30 | 添加计划任务自动执行功能                                                    |
| 1.3 | 2026-05-13 | 修复日期解析问题、添加异步执行支持                                               |
| 1.4 | 2026-05-20 | 添加节点上下移动、自定义线程池、禁用状态样式优化                                        |
| 1.5 | 2026-05-22 | 添加核算子节点属性(编码、核算标识、类型、边界说明)、自动添加名称前缀、弹窗遮罩层保护                     |
| 1.6 | 2026-05-28 | 分离节点编码与节点名称简称字段、左侧目录树宽度增加50%、缩小操作按钮尺寸                           |
| 1.7 | 2026-05-29 | 添加设备编号自动生成功能、支持按配置规则生成编码、新增next-id API获取下一个可用节点ID               |
| 1.8 | 2026-05-29 | 排放数据采集点新增四个字段：核算场景、能耗用途、是否累计量、是否移动源；添加对应数据字典分类                  |
| 1.9 | 2026-05-30 | 添加计量单位动态下拉框、数据来源系统表及管理功能、数据来源系统选择和获取方式录入字段                      |
| 2.0 | 2026-06-06 | 添加排放因子库模块：电力碳排放因子库、化石燃料排放因子库、蒸汽热焓值表、热力排放因子、固体废弃物焚烧排放因子、废水处理排放因子 |
