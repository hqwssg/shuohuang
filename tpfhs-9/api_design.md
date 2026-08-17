# 碳排放模型设计系统 - 前后端接口设计说明（版本2.0）

## 1. 接口设计概述

### 1.1 接口风格

- **协议**: HTTP/HTTPS
- **数据格式**: JSON
- **字符编码**: UTF-8
- **API路径前缀**: `/api/`

### 1.2 接口分类

| 模块   | 路径前缀            | Controller文件                | 功能描述     |
|:---- |:--------------- |:--------------------------- |:-------- |
| 用户模块 | `/api/user`     | UserController.java         | 用户登录认证   |
| 模版模块 | `/api/template` | TemplateController.java     | 模版CRUD操作 |
| 节点模块 | `/api/nodes`    | EmissionNodeController.java | 节点树状结构管理 |
| 数据字典模块 | `/api/dict`    | DataDictController.java     | 数据字典管理 |
| 数据来源系统模块 | `/api/data-source-systems` | DataSourceSystemController.java | 数据来源系统管理 |
| 排放因子库模块 | `/api/electricity-carbon-factors`, `/api/fossil-fuel-factors`, `/api/steam-enthalpy`, `/api/thermal-emission-factors`, `/api/waste-incineration-factors`, `/api/wastewater-treatment-factors` | 多个Controller | 排放因子库管理 |

### 1.3 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

| 字段      | 类型      | 说明            |
|:------- |:------- |:------------- |
| code    | Integer | 响应状态码，200表示成功 |
| message | String  | 响应消息          |
| data    | Object  | 响应数据（成功时返回）   |

---

## 2. 用户模块接口

### 2.1 用户登录

| 属性         | 值                                      |
|:---------- |:-------------------------------------- |
| **URL**    | `/api/user/login`                      |
| **HTTP方法** | POST                                   |
| **所属文件**   | `controller/UserController.java:17-20` |

#### 请求体

| 字段       | 类型     | 必填  | 说明  |
|:-------- |:------ |:--- |:--- |
| username | String | 是   | 用户名 |
| password | String | 是   | 密码  |

**请求示例：**

```json
{
  "username": "admin",
  "password": "123456"
}
```

#### 响应体

| 字段       | 类型     | 说明   |
|:-------- |:------ |:---- |
| userId   | Long   | 用户ID |
| username | String | 用户名  |
| name     | String | 用户姓名 |
| token    | String | 登录令牌 |

**成功响应示例：**

```json
{
  "userId": 1,
  "username": "admin",
  "name": "管理员",
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

---

## 3. 模版模块接口

### 3.1 获取所有模版列表

| 属性         | 值                                          |
|:---------- |:------------------------------------------ |
| **URL**    | `/api/template/list`                       |
| **HTTP方法** | GET                                        |
| **所属文件**   | `controller/TemplateController.java:23-26` |

#### 请求参数

无

#### 响应体

返回 `TemplateDTO` 数组，结构如下：

| 字段            | 类型            | 说明           |
|:------------- |:------------- |:------------ |
| id            | Long          | 模版ID         |
| name          | String        | 模版名称         |
| description   | String        | 描述信息         |
| createdBy     | Long          | 创建人ID        |
| createdByName | String        | 创建人姓名        |
| createdAt     | LocalDateTime | 创建时间         |
| updatedBy     | Long          | 更新人ID        |
| updatedByName | String        | 更新人姓名        |
| updatedAt     | LocalDateTime | 更新时间         |
| version       | Integer       | 版本号          |
| enabled       | Boolean       | 是否启用         |
| taskConfig    | String        | 计划任务配置(JSON) |

**成功响应示例：**

```json
[
  {
    "id": 1,
    "name": "运输碳排放核算模版",
    "description": "用于运输行业的碳排放核算",
    "createdBy": 1,
    "createdByName": "管理员",
    "createdAt": "2024-01-15T10:30:00",
    "updatedBy": 1,
    "updatedByName": "管理员",
    "updatedAt": "2024-01-15T10:30:00",
    "version": 1,
    "enabled": true,
    "taskConfig": "{\"cycleType\":\"DAILY\",\"startTime\":\"02:00:00\"}"
  }
]
```

---

### 3.2 获取单个模版详情

| 属性         | 值                                          |
|:---------- |:------------------------------------------ |
| **URL**    | `/api/template/{id}`                       |
| **HTTP方法** | GET                                        |
| **所属文件**   | `controller/TemplateController.java:28-31` |

#### 路径参数

| 字段  | 类型   | 必填  | 说明   |
|:--- |:---- |:--- |:---- |
| id  | Long | 是   | 模版ID |

#### 响应体

返回单个 `TemplateDTO` 对象，结构同3.1。

---

### 3.3 创建新模版

| 属性         | 值                                          |
|:---------- |:------------------------------------------ |
| **URL**    | `/api/template/create`                     |
| **HTTP方法** | POST                                       |
| **所属文件**   | `controller/TemplateController.java:33-36` |

#### 请求体

| 字段          | 类型     | 必填  | 说明    |
|:----------- |:------ |:--- |:----- |
| name        | String | 是   | 模版名称  |
| description | String | 否   | 描述信息  |
| createdBy   | Long   | 是   | 创建人ID |

**请求示例：**

```json
{
  "name": "新建模版",
  "description": "测试模版",
  "createdBy": 1
}
```

#### 响应体

返回创建后的 `TemplateDTO` 对象。

---

### 3.4 更新模版名称

| 属性         | 值                                          |
|:---------- |:------------------------------------------ |
| **URL**    | `/api/template/{id}`                       |
| **HTTP方法** | PUT                                        |
| **所属文件**   | `controller/TemplateController.java:38-43` |

#### 路径参数

| 字段  | 类型   | 必填  | 说明   |
|:--- |:---- |:--- |:---- |
| id  | Long | 是   | 模版ID |

#### 请求体

| 字段        | 类型     | 必填  | 说明     |
|:--------- |:------ |:--- |:------ |
| name      | String | 是   | 新的模版名称 |
| updatedBy | Long   | 是   | 更新人ID  |

**请求示例：**

```json
{
  "name": "更新后的名称",
  "updatedBy": 1
}
```

---

### 3.5 更新模版属性

| 属性         | 值                                          |
|:---------- |:------------------------------------------ |
| **URL**    | `/api/template/{id}/properties`            |
| **HTTP方法** | PUT                                        |
| **所属文件**   | `controller/TemplateController.java:48-69` |

#### 路径参数

| 字段  | 类型   | 必填  | 说明   |
|:--- |:---- |:--- |:---- |
| id  | Long | 是   | 模版ID |

#### 请求体

| 字段          | 类型            | 必填  | 说明     |
|:----------- |:------------- |:--- |:------ |
| description | String        | 否   | 描述信息   |
| enabled     | Boolean       | 否   | 是否启用   |
| taskConfig  | Object/String | 否   | 计划任务配置 |
| updatedBy   | Long          | 是   | 更新人ID  |

**请求示例：**

```json
{
  "description": "更新后的描述",
  "enabled": true,
  "taskConfig": {
    "cycleType": "WEEKLY",
    "startDate": "2024-01-01",
    "endDate": "",
    "startTime": "02:00:00",
    "interval": 1,
    "weekdays": [1, 3]
  },
  "updatedBy": 1
}
```

**taskConfig字段说明：**

| 字段          | 类型      | 说明                                         |
|:----------- |:------- |:------------------------------------------ |
| cycleType   | String  | 执行周期：DAILY/WEEKLY/MONTHLY/QUARTERLY/YEARLY |
| startDate   | String  | 起始日期(yyyy-MM-dd)                           |
| endDate     | String  | 截止日期(yyyy-MM-dd)，空表示无限制                    |
| startTime   | String  | 执行时间(HH:mm:ss)                             |
| interval    | Integer | 重复间隔，默认1                                   |
| weekdays    | Array   | 周执行的星期列表(1-7)                              |
| monthDays   | Array   | 月执行的日期列表(1-31)                             |
| quarterDays | Array   | 季执行的日期列表(1-31)                             |
| yearMonths  | Array   | 年执行的月份列表(1-12)                             |
| yearDays    | Array   | 年执行的日期列表(1-31)                             |

---

### 3.6 复制模版

| 属性         | 值                                          |
|:---------- |:------------------------------------------ |
| **URL**    | `/api/template/copy/{sourceId}`            |
| **HTTP方法** | POST                                       |
| **所属文件**   | `controller/TemplateController.java:71-76` |

#### 路径参数

| 字段       | 类型   | 必填  | 说明    |
|:-------- |:---- |:--- |:----- |
| sourceId | Long | 是   | 源模版ID |

#### 请求体

| 字段        | 类型     | 必填  | 说明    |
|:--------- |:------ |:--- |:----- |
| newName   | String | 是   | 新模版名称 |
| createdBy | Long   | 是   | 创建人ID |

**请求示例：**

```json
{
  "newName": "复制的模版",
  "createdBy": 1
}
```

---

### 3.7 删除模版

| 属性         | 值                                          |
|:---------- |:------------------------------------------ |
| **URL**    | `/api/template/{id}`                       |
| **HTTP方法** | DELETE                                     |
| **所属文件**   | `controller/TemplateController.java:78-82` |

#### 路径参数

| 字段  | 类型   | 必填  | 说明   |
|:--- |:---- |:--- |:---- |
| id  | Long | 是   | 模版ID |

#### 响应体

成功返回空响应，HTTP状态码200。

---

## 4. 节点模块接口

### 4.1 获取节点树

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/nodes/tree`                              |
| **HTTP方法** | GET                                            |
| **所属文件**   | `controller/EmissionNodeController.java:26-29` |

#### 请求参数

| 字段         | 类型   | 必填  | 说明           |
|:---------- |:---- |:--- |:------------ |
| templateId | Long | 否   | 模版ID，不传则返回所有 |

#### 响应体

返回 `NodeDTO` 对象，结构如下：

| 字段              | 类型            | 说明       |
|:--------------- |:------------- |:-------- |
| id              | Long          | 节点ID     |
| name            | String        | 节点名称     |
| typeName        | String        | 节点类型名称   |
| typeId          | Integer       | 节点类型ID   |
| parentId        | Long          | 父节点ID    |
| locomotiveType  | String        | 机车类型     |
| children        | List<NodeDTO> | 子节点列表    |
| canHaveChildren | Boolean       | 是否可以有子节点 |
| config          | NodeConfigDTO | 节点配置信息   |

**成功响应示例：**

```json
{
  "id": 1,
  "name": "根节点",
  "typeName": "核算子节点",
  "typeId": 1,
  "parentId": null,
  "locomotiveType": null,
  "canHaveChildren": true,
  "children": [
    {
      "id": 2,
      "name": "子节点1",
      "typeName": "排放数据采集点",
      "typeId": 3,
      "parentId": 1,
      "locomotiveType": "电力机车",
      "canHaveChildren": false,
      "children": [],
      "config": {
        "nodeId": 2,
        "statisticalCaliber": "生产排放",
        "emissionCategory": "购入的电力",
        "accountingScenario": "牵引变电所/牵引供电",
        "energyUse": "照明",
        "isCumulative": "true",
        "isMobileSource": "false",
        "measurementUnit": "MWh",
        "dataSourceSystem": "能耗监测系统",
        "acquisitionMethod": "{\"api\":\"http://api.example.com/data\",\"params\":{\"date\":\"2024-01-01\"}}"
      }
    }
  ],
  "config": null
}
```

---

### 4.2 获取单个节点

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/nodes/{id}`                              |
| **HTTP方法** | GET                                            |
| **所属文件**   | `controller/EmissionNodeController.java:31-34` |

#### 路径参数

| 字段  | 类型   | 必填  | 说明   |
|:--- |:---- |:--- |:---- |
| id  | Long | 是   | 节点ID |

#### 响应体

返回单个 `NodeDTO` 对象，结构同4.1。

---

### 4.3 创建节点

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/nodes`                                   |
| **HTTP方法** | POST                                           |
| **所属文件**   | `controller/EmissionNodeController.java:36-39` |

#### 请求体

| 字段             | 类型            | 必填  | 说明                   |
|:-------------- |:------------- |:--- |:-------------------- |
| name           | String        | 是   | 节点名称                 |
| typeId         | Integer       | 是   | 节点类型ID               |
| parentId       | Long          | 否   | 父节点ID（根节点为null）      |
| templateId     | Long          | 是   | 所属模版ID               |
| locomotiveType | String        | 否   | 机车类型                 |
| config         | NodeConfigDTO | 否   | 节点配置                 |
| nodeInfo       | NodeInfoDTO   | 否   | 核算子节点信息（typeId=2时必填） |
| createdBy      | Long          | 是   | 创建人ID                |

**请求示例：**

```json
{
  "name": "新节点",
  "typeId": 3,
  "parentId": 1,
  "templateId": 1,
  "config": {
    "statisticalCaliber": "生产排放",
    "emissionCategory": "购入的电力",
    "emissionSubcategory": "生产设施用电",
    "accountingScenario": "牵引变电所/牵引供电",
    "energyUse": "照明",
    "isCumulative": "true",
    "isMobileSource": "false",
    "measurementUnit": "MWh",
    "dataSourceSystem": "能耗监测系统",
    "acquisitionMethod": "{\"api\":\"http://api.example.com/data\"}",
    "allocationRatio": 100.00
  },
  "createdBy": 1
}
```

---

### 4.4 更新节点

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/nodes/{id}`                              |
| **HTTP方法** | PUT                                            |
| **所属文件**   | `controller/EmissionNodeController.java:41-44` |

#### 路径参数

| 字段  | 类型   | 必填  | 说明   |
|:--- |:---- |:--- |:---- |
| id  | Long | 是   | 节点ID |

#### 请求体

| 字段     | 类型            | 必填  | 说明   |
|:------ |:------------- |:--- |:---- |
| name   | String        | 否   | 节点名称 |
| config | NodeConfigDTO | 否   | 节点配置 |

**请求示例：**

```json
{
  "name": "更新后的节点名称",
  "config": {
    "statisticalCaliber": "生产排放",
    "emissionCategory": "化石燃料",
    "measurementUnit": "吨"
  }
}
```

---

### 4.5 删除节点

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/nodes/{id}`                              |
| **HTTP方法** | DELETE                                         |
| **所属文件**   | `controller/EmissionNodeController.java:46-50` |

#### 路径参数

| 字段  | 类型   | 必填  | 说明   |
|:--- |:---- |:--- |:---- |
| id  | Long | 是   | 节点ID |

#### 说明

删除节点会级联删除其所有子节点。

---

### 4.6 获取节点配置

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/nodes/{id}/config`                       |
| **HTTP方法** | GET                                            |
| **所属文件**   | `controller/EmissionNodeController.java:52-55` |

#### 路径参数

| 字段  | 类型   | 必填  | 说明   |
|:--- |:---- |:--- |:---- |
| id  | Long | 是   | 节点ID |

#### 响应体

返回 `NodeConfigDTO` 对象，结构如下：

| 字段                   | 类型         | 说明    |
|:-------------------- |:---------- |:----- |
| nodeId               | Long       | 节点ID  |
| statisticalCaliber   | String     | 统计口径  |
| emissionCategory     | String     | 排放大类  |
| emissionSubcategory  | String     | 排放小类 |
| carbonEmissionFactor | String     | 碳排放因子 |
| dataSource           | String     | 数据来源  |
| accountingScenario   | String     | 核算场景  |
| energyUse            | String     | 能耗用途  |
| isCumulative         | String     | 是否累计量 |
| isMobileSource       | String     | 是否移动源 |
| measurementUnit       | String     | 计量单位  |
| dataSourceSystem     | String     | 数据来源系统 |
| acquisitionMethod     | String     | 获取方式  |
| allocationRatio      | BigDecimal | 分配比例  |
| hasSubTable          | Boolean    | 是否有子表 |
| errorConstraint      | BigDecimal | 误差约束  |
| updateCycle          | String     | 更新周期  |
| updateTime           | String     | 更新时间  |

---

### 4.7 更新节点配置

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/nodes/{id}/config`                       |
| **HTTP方法** | PUT                                            |
| **所属文件**   | `controller/EmissionNodeController.java:57-60` |

#### 路径参数

| 字段  | 类型   | 必填  | 说明   |
|:--- |:---- |:--- |:---- |
| id  | Long | 是   | 节点ID |

#### 请求体

| 字段                   | 类型         | 必填  | 说明    |
|:-------------------- |:---------- |:--- |:----- |
| statisticalCaliber   | String     | 否   | 统计口径  |
| emissionCategory     | String     | 否   | 排放大类  |
| emissionSubcategory  | String     | 否   | 排放小类 |
| carbonEmissionFactor | String     | 否   | 碳排放因子 |
| dataSource           | String     | 否   | 数据来源  |
| accountingScenario   | String     | 否   | 核算场景  |
| energyUse            | String     | 否   | 能耗用途  |
| isCumulative         | String     | 否   | 是否累计量 |
| isMobileSource       | String     | 否   | 是否移动源 |
| measurementUnit       | String     | 否   | 计量单位  |
| dataSourceSystem     | String     | 否   | 数据来源系统 |
| acquisitionMethod     | String     | 否   | 获取方式  |
| allocationRatio      | BigDecimal | 否   | 分配比例  |
| hasSubTable          | Boolean    | 否   | 是否有子表 |
| errorConstraint      | BigDecimal | 否   | 误差约束  |
| updateCycle          | String     | 否   | 更新周期  |
| updateTime           | String     | 否   | 更新时间  |

---

### 4.8 获取下拉框选项

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/nodes/options`                           |
| **HTTP方法** | GET                                            |
| **所属文件**   | `controller/EmissionNodeController.java:62-65` |

#### 请求参数

无

#### 响应体

返回所有数据字典选项，结构如下：

| 字段                  | 类型   | 说明     |
|:------------------- |:---- |:------ |
| nodeTypes           | List | 节点类型列表 |
| statisticalCalibers | List | 统计口径列表 |
| emissionCategories  | List | 排放类别列表 |
| emissionSubcategories | Map | 排放子类别映射 |
| locomotiveTypes     | List | 机车类型列表 |
| dataSources         | List | 数据来源列表 |
| accountingScenarios  | List | 核算场景列表 |
| energyUses          | List | 能耗用途列表 |

**成功响应示例：**

```json
{
  "nodeTypes": [
    {"id": 1, "name": "根节点"},
    {"id": 2, "name": "核算子节点"},
    {"id": 3, "name": "排放数据采集点"},
    {"id": 4, "name": "运输生产碳排放核算节点"}
  ],
  "statisticalCalibers": [
    {"code": "PE", "name": "生产排放"},
    {"code": "PAOE", "name": "辅助和附属生产排放"}
  ],
  "emissionCategories": [
    {"code": "PE", "name": "购入的电力"},
    {"code": "PH", "name": "购入的热力"},
    {"code": "FF", "name": "化石燃料"},
    {"code": "EP", "name": "输出的电力"},
    {"code": "WT", "name": "废弃物处理"}
  ],
  "accountingScenarios": [
    "牵引变电所/牵引供电",
    "列车运行",
    "行车调度/通信指挥"
  ],
  "energyUses": [
    "照明",
    "空调",
    "取暖"
  ]
}
```

---

### 4.9 获取下一个可用节点ID

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/nodes/next-id`                           |
| **HTTP方法** | GET                                            |

#### 功能描述

获取当前数据库中最大的节点ID并加1，用于在创建新节点前预测节点ID，以便生成唯一的设备编号。

#### 请求参数

无

#### 响应体

| 字段 | 类型   | 说明         |
|:--- |:---- |:---------- |
| (返回值) | Long | 下一个可用的节点ID |

**成功响应示例：**

```json
307
```

---

### 4.10 生成设备编号

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/nodes/{id}/equipment-code`               |
| **HTTP方法** | GET                                            |
| **所属文件**   | `controller/EmissionNodeController.java:86-90` |

#### 功能描述

根据节点ID和排放类别生成设备编号，支持多种编码规则配置。

#### 路径参数

| 字段  | 类型   | 必填  | 说明           |
|:--- |:---- |:--- |:------------ |
| id  | Long | 是   | 父节点ID或节点ID |

#### 请求参数

| 字段               | 类型     | 必填  | 说明                     |
|:---------------- |:------ |:--- |:---------------------- |
| emissionCategory | String | 是   | 排放数据大类（如"化石燃料"、"电力"等） |
| newNodeId        | Long   | 否   | 新节点ID（用于新建节点场景）       |

#### 响应体

| 字段 | 类型     | 说明     |
|:--- |:------ |:------ |
| (返回值) | String | 生成的设备编号 |

**成功响应示例：**

```json
"y1-E1-EP-000307"
```

#### 编码规则说明

编码规则由系统配置 `Auto_coding_rules` 控制，支持以下规则：

| 规则值 | 编码格式 | 说明 |
|:----- |:------ |:--- |
| 1 | `一级编码-ID` | 仅包含一级节点编码和节点ID |
| 2 | `一级编码-二级编码-ID` | 包含一级、二级节点编码和节点ID |
| 3 | `一级编码-大类编码-ID` | 包含一级节点编码、排放大类编码和节点ID |
| 4 | `一级编码-二级编码-大类编码-ID` | 包含一级、二级节点编码、排放大类编码和节点ID |

---

### 4.11 移动节点位置

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/nodes/{id}/move`                         |
| **HTTP方法** | POST                                           |
| **所属文件**   | `controller/EmissionNodeController.java:92-96` |

#### 路径参数

| 字段  | 类型   | 必填  | 说明   |
|:--- |:---- |:--- |:---- |
| id  | Long | 是   | 节点ID |

#### 请求参数

| 字段        | 类型     | 必填  | 说明            |
|:--------- |:------ |:--- |:------------- |
| direction | String | 是   | 移动方向（up/down） |

#### 说明

- 节点只能在同级节点之间移动，不能跨级移动
- 如果节点已经是同级的第一个，向上移动无效
- 如果节点已经是同级的最后一个，向下移动无效

#### 响应体

成功返回空响应，HTTP状态码200。

---

## 5. 数据字典模块接口

### 5.1 获取所有字典

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/dict`                                    |
| **HTTP方法** | GET                                            |

#### 功能描述

获取系统中所有预定义的数据字典分类及其选项列表。

#### 请求参数

无

#### 响应体

| 字段                    | 类型   | 说明           |
|:--------------------- |:---- |:------------ |
| locomotiveTypes        | List | 机车类型列表       |
| statisticalCalibers   | List | 统计口径列表       |
| emissionCategories    | List | 排放类别列表       |
| emissionSubcategories | Map  | 排放子类别映射     |
| dataSources          | List | 数据来源列表       |
| nodeCategories        | List | 节点类型列表       |
| accountingScenarios   | List | 核算场景列表       |
| energyUses           | List | 能耗用途列表       |

---

### 5.2 获取指定字典的所有项

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/dict/{dictCode}`                          |
| **HTTP方法** | GET                                            |

#### 路径参数

| 字段       | 类型     | 必填  | 说明     |
|:-------- |:------ |:--- |:------ |
| dictCode | String | 是   | 字典编码  |

#### 响应体

返回指定字典的所有字典项列表。

---

## 6. 数据来源系统模块接口

### 6.1 获取所有数据来源系统

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/data-source-systems`                       |
| **HTTP方法** | GET                                            |

#### 功能描述

获取系统中所有已创建的数据来源系统列表。

#### 请求参数

无

#### 响应体

返回 `DataSourceSystem` 对象数组：

| 字段         | 类型           | 说明       |
|:---------- |:------------ |:-------- |
| id          | Long         | 主键ID     |
| systemName  | String       | 数据来源系统名称 |
| description | String       | 说明       |
| pinyinCode  | String       | 拼音首字母编码  |
| createdBy   | Long         | 创建人ID    |
| updatedBy   | Long         | 更新人ID    |
| createdAt   | LocalDateTime | 创建时间     |
| updatedAt   | LocalDateTime | 更新时间     |

**成功响应示例：**

```json
[
  {
    "id": 1,
    "systemName": "能耗监测系统",
    "description": "用于监测各站点能耗数据",
    "pinyinCode": "NHJCXT",
    "createdBy": 1,
    "createdAt": "2026-05-30T10:00:00",
    "updatedAt": "2026-05-30T10:00:00"
  }
]
```

---

### 6.2 获取单个数据来源系统

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/data-source-systems/{id}`                 |
| **HTTP方法** | GET                                            |

#### 路径参数

| 字段  | 类型   | 必填  | 说明     |
|:--- |:---- |:--- |:------ |
| id  | Long | 是   | 数据来源系统ID |

#### 响应体

返回单个 `DataSourceSystem` 对象，结构同6.1。

---

### 6.3 搜索数据来源系统

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/data-source-systems/search`               |
| **HTTP方法** | GET                                            |

#### 功能描述

根据关键词搜索数据来源系统。支持拼音首字母筛选和模糊查询。

#### 请求参数

| 字段     | 类型     | 必填  | 说明               |
|:------ |:------ |:--- |:---------------- |
| keyword | String | 否   | 搜索关键词           |

#### 筛选规则

- 如果关键词全部为英文字母，则按拼音首字母筛选
- 否则按系统名称进行模糊查询

---

### 6.4 创建数据来源系统

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/data-source-systems`                       |
| **HTTP方法** | POST                                           |

#### 请求体

| 字段         | 类型     | 必填  | 说明       |
|:---------- |:------ |:--- |:-------- |
| systemName  | String | 是   | 数据来源系统名称 |
| description | String | 否   | 说明       |

**请求示例：**

```json
{
  "systemName": "能耗监测系统",
  "description": "用于监测各站点能耗数据"
}
```

#### 响应体

| 字段      | 类型     | 说明     |
|:------- |:------ |:------ |
| success  | Boolean | 是否成功   |
| data     | Object  | 创建的数据  |
| message  | String  | 消息提示   |

---

### 6.5 更新数据来源系统

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/data-source-systems/{id}`                 |
| **HTTP方法** | PUT                                            |

#### 路径参数

| 字段  | 类型   | 必填  | 说明     |
|:--- |:---- |:--- |:------ |
| id  | Long | 是   | 数据来源系统ID |

#### 请求体

| 字段         | 类型     | 必填  | 说明       |
|:---------- |:------ |:--- |:-------- |
| systemName  | String | 是   | 数据来源系统名称 |
| description | String | 否   | 说明       |

---

### 6.6 删除数据来源系统

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/data-source-systems/{id}`                 |
| **HTTP方法** | DELETE                                          |

#### 路径参数

| 字段  | 类型   | 必填  | 说明     |
|:--- |:---- |:--- |:------ |
| id  | Long | 是   | 数据来源系统ID |

#### 说明

删除操作需要二次确认（前端处理）。

---

### 6.7 检查系统名称是否存在

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/data-source-systems/exists`                |
| **HTTP方法** | GET                                            |

#### 请求参数

| 字段        | 类型     | 必填  | 说明       |
|:--------- |:------ |:--- |:-------- |
| systemName | String | 是   | 数据来源系统名称 |

#### 响应体

| 字段    | 类型      | 说明       |
|:----- |:------- |:-------- |
| exists | Boolean | 是否已存在   |

---

## 7. 错误响应格式

```json
{
  "code": 400,
  "message": "参数错误：用户名不能为空",
  "data": null
}
```

| 错误码 | 说明       |
|:--- |:-------- |
| 400 | 请求参数错误   |
| 401 | 未登录或登录失效 |
| 403 | 权限不足     |
| 404 | 资源不存在    |
| 500 | 服务器内部错误  |

---

## 8. DTO类关系图

```
┌─────────────────────────────────────────────────────────────────┐
│                        请求DTO                                 │
├─────────────────────────────────────────────────────────────────┤
│  LoginRequest       CreateTemplateRequest       CreateNodeRequest│
│  ├─ username        ├─ name                     ├─ name          │
│  └─ password        ├─ description              ├─ typeId        │
│                     └─ createdBy                ├─ parentId      │
│                                                 ├─ templateId    │
│                                                 ├─ locomotiveType│
│                                                 ├─ config        │
│                                                 └─ nodeInfo      │
├─────────────────────────────────────────────────────────────────┤
│  UpdateNodeRequest                                             │
│  ├─ name                                                      │
│  ├─ config                                                     │
│  ├─ nodeInfo                                                   │
│  └─ updatedBy                                                  │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                        响应DTO                                 │
├─────────────────────────────────────────────────────────────────┤
│  LoginResponse      TemplateDTO                NodeDTO          │
│  ├─ userId          ├─ id                      ├─ id            │
│  ├─ username        ├─ name                    ├─ name          │
│  ├─ name            ├─ description             ├─ typeName      │
│  └─ token           ├─ createdBy               ├─ typeId        │
│                     ├─ createdAt               ├─ parentId      │
│                     ├─ updatedBy               ├─ locomotiveType│
│                     ├─ updatedAt               ├─ children[]    │
│                     ├─ version                 ├─ canHaveChildren│
│                     ├─ enabled                 ├─ config        │
│                     └─ taskConfig              └─ nodeInfo      │
├─────────────────────────────────────────────────────────────────┤
│  NodeConfigDTO              NodeInfoDTO                         │
│  ├─ nodeId                 ├─ nodeId                           │
│  ├─ statisticalCaliber     ├─ nodeCode                          │
│  ├─ emissionCategory       ├─ shortName                         │
│  ├─ emissionSubcategory    ├─ includeInCalculation             │
│  ├─ carbonEmissionFactor   ├─ nodeCategory                     │
│  ├─ dataSource             ├─ unitDescription                  │
│  ├─ accountingScenario     ├─ orgBoundaryDescription           │
│  ├─ energyUse              └─ operationBoundaryDescription     │
│  ├─ isCumulative                                               │
│  ├─ isMobileSource                                             │
│  ├─ measurementUnit                                             │
│  ├─ dataSourceSystem                                             │
│  ├─ acquisitionMethod                                          │
│  ├─ allocationRatio                                           │
│  ├─ hasSubTable                                               │
│  ├─ errorConstraint                                           │
│  ├─ updateCycle                                               │
│  ├─ updateTime                                                │
│  └─ taskConfig                                                │
├─────────────────────────────────────────────────────────────────┤
│  DataSourceSystem                                               │
│  ├─ id                                                         │
│  ├─ systemName                                                 │
│  ├─ description                                                │
│  ├─ pinyinCode                                                 │
│  └─ timestamps                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 9. 接口调用流程图

### 9.1 用户登录流程

```
前端                    后端                    数据库
  │                       │                        │
  │── POST /api/user/login ──▶│                        │
  │   {username, password}   │                        │
  │                       │── 查询用户 ── ▶│                        │
  │                       │◀── 返回用户 ───│                        │
  │                       │                        │
  │                       │── 验证密码 ── ▶│                        │
  │                       │                        │
  │◀── LoginResponse ─────│                        │
  │   {userId, name, token}│                        │
  │                       │                        │
```

### 9.2 获取节点树流程

```
前端                    后端                    数据库
  │                       │                        │
  │── GET /api/nodes/tree ──▶│                        │
  │   ?templateId=1        │                        │
  │                       │── 查询节点 ── ▶│                        │
  │                       │◀── 返回节点列表 ───│                        │
  │                       │                        │
  │                       │── 查询配置 ── ▶│                        │
  │                       │◀── 返回配置 ───│                        │
  │                       │                        │
  │                       │── 构建树形结构 ── ▶│                        │
  │                       │                        │
  │◀── NodeDTO(tree) ─────│                        │
  │                       │                        │
```

### 9.3 创建节点流程

```
前端                    后端                    数据库
  │                       │                        │
  │── POST /api/nodes ── ▶│                        │
  │   CreateNodeRequest   │                        │
  │                       │── 验证类型限制 ── ▶│                        │
  │                       │                        │
  │                       │── 保存节点 ── ▶│                        │
  │                       │◀── 返回节点ID ───│                        │
  │                       │                        │
  │                       │── 保存配置 ── ▶│                        │
  │                       │◀── 返回配置ID ───│                        │
  │                       │                        │
  │◀── NodeDTO ──────────│                        │
  │                       │                        │
```

### 9.4 数据来源系统选择流程

```
前端                    后端                    数据库
  │                       │                        │
  │── 点击"+"按钮 ── ▶│  打开弹窗                   │
  │                       │                        │
  │── GET /api/data-source-systems ── ▶│                        │
  │                       │── 查询列表 ── ▶│                        │
  │                       │◀── 返回列表 ───│                        │
  │                       │                        │
  │◀── 显示列表数据 ────│                        │
  │                       │                        │
  │── 输入搜索词 ── ▶│  判断是否为拼音筛选               │
  │                       │                        │
  │── GET /api/data-source-systems/search?keyword=xxx ── ▶│                        │
  │                       │── 搜索查询 ── ▶│                        │
  │                       │◀── 返回结果 ───│                        │
  │                       │                        │
  │◀── 更新列表 ──────────│                        │
  │                       │                        │
  │── 选择记录并点击"选择" ── ▶│                        │
  │                       │                        │
  │◀── 关闭弹窗，回填输入框 ──│                        │
  │                       │                        │
```

---

## 10. 前端API调用示例（Axios）

```javascript
// 用户登录
const login = async (username, password) => {
  const response = await axios.post('/api/user/login', {
    username,
    password
  });
  return response.data;
};

// 获取模版列表
const getTemplates = async () => {
  const response = await axios.get('/api/template/list');
  return response.data;
};

// 创建节点
const createNode = async (nodeData) => {
  const response = await axios.post('/api/nodes', nodeData);
  return response.data;
};

// 更新节点配置
const updateNodeConfig = async (nodeId, config) => {
  const response = await axios.put(`/api/nodes/${nodeId}/config`, config);
  return response.data;
};

// 删除节点
const deleteNode = async (nodeId) => {
  await axios.delete(`/api/nodes/${nodeId}`);
};

// 移动节点位置
const moveNode = async (nodeId, direction) => {
  await axios.post(`/api/nodes/${nodeId}/move`, null, {
    params: { direction }
  });
};

// 获取下拉框选项
const getOptions = async () => {
  const response = await axios.get('/api/nodes/options');
  return response.data;
};

// 获取所有数据来源系统
const getDataSourceSystems = async () => {
  const response = await axios.get('/api/data-source-systems');
  return response.data;
};

// 搜索数据来源系统
const searchDataSourceSystems = async (keyword) => {
  const response = await axios.get('/api/data-source-systems/search', {
    params: { keyword }
  });
  return response.data;
};

// 创建数据来源系统
const createDataSourceSystem = async (data) => {
  const response = await axios.post('/api/data-source-systems', data);
  return response.data;
};

// 更新数据来源系统
const updateDataSourceSystem = async (id, data) => {
  const response = await axios.put(`/api/data-source-systems/${id}`, data);
  return response.data;
};

// 删除数据来源系统
const deleteDataSourceSystem = async (id) => {
  await axios.delete(`/api/data-source-systems/${id}`);
};
```

---

## 7. 排放因子库模块接口

### 7.1 电力碳排放因子库

#### 7.1.1 获取所有电力碳排放因子

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/electricity-carbon-factors`               |
| **HTTP方法** | GET                                            |

#### 请求参数

无

#### 响应体

| 字段         | 类型            | 说明       |
|:---------- |:------------- |:-------- |
| id          | Long          | 主键ID     |
| factorName  | String        | 碳排放因子名称 |
| factorValue | BigDecimal    | 碳排放因子值 |
| unit        | String        | 单位       |
| description | String        | 说明       |

---

#### 7.1.2 根据ID获取电力碳排放因子

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/electricity-carbon-factors/{id}`          |
| **HTTP方法** | GET                                            |

#### 路径参数

| 字段 | 类型   | 必填  | 说明   |
|:--- |:---- |:--- |:---- |
| id  | Long | 是   | 因子ID |

---

#### 7.1.3 创建电力碳排放因子

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/electricity-carbon-factors`               |
| **HTTP方法** | POST                                           |

#### 请求体

| 字段         | 类型            | 必填  | 说明       |
|:---------- |:------------- |:--- |:-------- |
| factorName  | String        | 是   | 碳排放因子名称 |
| factorValue | BigDecimal    | 是   | 碳排放因子值 |
| unit        | String        | 是   | 单位       |
| description | String        | 否   | 说明       |

---

### 7.2 化石燃料排放因子库

#### 7.2.1 获取所有化石燃料排放因子

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/fossil-fuel-factors`                     |
| **HTTP方法** | GET                                            |

---

#### 7.2.2 根据ID获取化石燃料排放因子

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/fossil-fuel-factors/{id}`                |
| **HTTP方法** | GET                                            |

---

#### 7.2.3 创建化石燃料排放因子

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/fossil-fuel-factors`                     |
| **HTTP方法** | POST                                           |

#### 请求体

| 字段                  | 类型            | 必填  | 说明           |
|:------------------- |:------------- |:--- |:------------ |
| emissionFactorName   | String        | 是   | 碳排放因子名称   |
| fuelType            | String        | 是   | 燃料品种       |
| source              | String        | 否   | 来源           |
| unit                | String        | 否   | 计量单位       |
| lowerHeatingValue   | BigDecimal    | 否   | 低位发热量     |
| carbonContentPerUnitHeat | BigDecimal | 否   | 单位热值含碳量   |
| fuelOxidationRate   | BigDecimal    | 否   | 燃料氧化率     |
| emissionFactor      | BigDecimal    | 否   | 碳排放因子     |
| factorUnit          | String        | 否   | 单位           |
| description         | String        | 否   | 说明           |

---

### 7.3 饱和蒸汽热焓值表

#### 7.3.1 获取所有饱和蒸汽热焓值

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/saturated-steam-enthalpy`                |
| **HTTP方法** | GET                                            |

#### 响应体

| 字段         | 类型            | 说明       |
|:---------- |:------------- |:-------- |
| id          | Long          | 主键ID     |
| pressure    | BigDecimal    | 压力（Mpa） |
| temperature | BigDecimal    | 温度（°C） |
| enthalpy    | BigDecimal    | 焓（KJ/kg） |

---

### 7.4 过热蒸汽热焓值表

#### 7.4.1 获取所有过热蒸汽热焓值

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/superheated-steam-enthalpy`              |
| **HTTP方法** | GET                                            |

---

### 7.5 热力排放因子库

#### 7.5.1 获取所有热力排放因子

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/thermal-emission-factors`                |
| **HTTP方法** | GET                                            |

---

### 7.6 固体废弃物焚烧排放因子库

#### 7.6.1 获取所有固体废弃物焚烧排放因子

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/waste-incineration-factors`              |
| **HTTP方法** | GET                                            |

#### 响应体

| 字段               | 类型            | 说明           |
|:---------------- |:------------- |:------------ |
| id                | Long          | 主键ID         |
| emissionFactorName | String        | 碳排放因子名称     |
| wasteType         | String        | 固体废物种类     |
| ccw               | BigDecimal    | 废弃物中的碳含量比例 |
| fcf               | BigDecimal    | 化石碳比例       |
| ce                | BigDecimal    | 燃烧效率         |
| emissionFactor    | BigDecimal    | 碳排放因子       |
| unit              | String        | 单位           |

---

### 7.7 废水处理排放因子库

#### 7.7.1 获取所有废水处理排放因子

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/wastewater-treatment-factors`            |
| **HTTP方法** | GET                                            |

#### 响应体

| 字段               | 类型            | 说明                     |
|:---------------- |:------------- |:---------------------- |
| id                | Long          | 主键ID                   |
| emissionFactorName | String        | 碳排放因子名称             |
| wastewaterType    | String        | 处理废水种类               |
| od                | BigDecimal    | 需氧浓度系数（COD或BOD）      |
| bo                | BigDecimal    | 最大甲烷产生能力             |
| mcf               | BigDecimal    | 甲烷修正因子               |
| gwp               | BigDecimal    | 甲烷的全球变暖潜能值          |
| emissionFactor    | BigDecimal    | 碳排放因子               |
| unit              | String        | 单位                   |

---

## 8. 排放因子库前端API调用示例

```javascript
// 获取电力碳排放因子列表
const getElectricityFactors = async () => {
  const response = await axios.get('/api/electricity-carbon-factors');
  return response.data;
};

// 获取化石燃料排放因子列表
const getFossilFuelFactors = async () => {
  const response = await axios.get('/api/fossil-fuel-factors');
  return response.data;
};

// 获取饱和蒸汽热焓值列表
const getSaturatedSteamEnthalpy = async () => {
  const response = await axios.get('/api/saturated-steam-enthalpy');
  return response.data;
};

// 获取过热蒸汽热焓值列表
const getSuperheatedSteamEnthalpy = async () => {
  const response = await axios.get('/api/superheated-steam-enthalpy');
  return response.data;
};

// 获取热力排放因子列表
const getThermalFactors = async () => {
  const response = await axios.get('/api/thermal-emission-factors');
  return response.data;
};

// 获取固体废弃物焚烧排放因子列表
const getWasteIncinerationFactors = async () => {
  const response = await axios.get('/api/waste-incineration-factors');
  return response.data;
};

// 获取废水处理排放因子列表
const getWastewaterTreatmentFactors = async () => {
  const response = await axios.get('/api/wastewater-treatment-factors');
  return response.data;
};
```
