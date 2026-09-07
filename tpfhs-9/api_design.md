# 碳排放模型设计系统 - 前后端接口设计说明（版本6.1）

## 1. 接口设计概述

### 1.1 接口风格

- **协议**: HTTP/HTTPS
- **数据格式**: JSON
- **字符编码**: UTF-8
- **API路径前缀**: `/api/`

### 1.2 接口分类

| 模块       | 路径前缀                                                                                                                                                                                          | Controller文件                    | 功能描述     |
|:-------- |:--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |:------------------------------- |:-------- |
| 用户模块     | `/api/user`                                                                                                                                                                                   | UserController.java             | 用户登录认证   |
| 模版模块     | `/api/template`                                                                                                                                                                               | TemplateController.java         | 模版CRUD操作 |
| 节点模块     | `/api/nodes`                                                                                                                                                                                  | EmissionNodeController.java     | 节点树状结构管理 |
| 数据字典模块   | `/api/dict`, `/api/data-dict`                                                                                                                                                                 | DataDictController.java         | 数据字典管理   |
| 数据来源系统模块 | `/api/data-source-systems`                                                                                                                                                                    | DataSourceSystemController.java | 数据来源系统管理 |
| 排放因子库模块  | `/api/electricity-carbon-factors`, `/api/fossil-fuel-factors`, `/api/steam-enthalpy`, `/api/thermal-emission-factors`, `/api/waste-incineration-factors`, `/api/wastewater-treatment-factors` | 多个Controller                    | 排放因子库管理  |
| 电表设置模块   | `/api/meter-settings`                                                                                                                                                                         | MeterSettingsController.java    | 站点/区间-集中器-电表树状结构管理 |
| 化石燃料采集设置模块 | `/api/fossil-fuel-collection`                                                                                                                                                                  | FossilFuelCollectionController.java | 采集范围-细分范围-采集点树状结构管理 |
| 电表型号模块   | `/api/meter-model`                                                                                                                                                                            | MeterModelController.java       | 电表型号CRUD操作 |
| 能耗分类数据字典模块 | `/api/energy-categories`                                                                                                                                                                      | EnergyCategoryController.java   | 三级能耗分类字典查询（供表单级联下拉框使用） |
| 因子模版管理模块 | `/api/factor-templates`                                                                                                                                                                        | EmissionFactorTemplateController.java | 碳排放因子模版CRUD、拷贝、引用计数 |
| 缺省因子管理模块 | `/api/default-factors`                                                                                                                                                                          | DefaultFactorController.java    | 模版级缺省碳排放因子CRUD |

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
| userName | String | 用户名  |
| nickName | String | 用户昵称 |
| deptId   | Long   | 部门ID |

**成功响应示例：**

```json
{
  "userId": 1,
  "userName": "admin",
  "nickName": "管理员",
  "deptId": 100
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
| templateType  | Integer       | 模版类型：1-节点模版，2-核算模版 |

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
    "taskConfig": "{\"cycleType\":\"DAILY\",\"startTime\":\"02:00:00\"}",
    "templateType": 2
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
| templateType | Integer | 否   | 模版类型：1-节点模版（默认），2-核算模版 |

**请求示例：**

```json
{
  "name": "新建模版",
  "description": "测试模版",
  "createdBy": 1,
  "templateType": 2
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
| templateType | Integer       | 否   | 模版类型：1-节点模版，2-核算模版 |
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

### 3.8 校验核算模版

| 属性         | 值                                              |
|:---------- |:----------------------------------------------- |
| **URL**    | `/api/template/{id}/validate`                   |
| **HTTP方法** | POST                                            |
| **所属文件**   | `controller/TemplateController.java:113-116`    |
| **前端调用**   | `components/TemplateList.vue`（"校验模版"按钮）         |

#### 功能描述

对核算模版（templateType=2）执行完整性校验，并**重新计算并保存**校验结果到
`sys_emission_template` 表的 `check_result` / `check_time` / `check_message` 字段。仅核算模版可调用，节点模版（templateType=1）返回错误。

校验项包括：

1. 模版是否设置了碳排放因子模版（未设置-错误；因子模版不存在-错误；因子模版中无缺省因子-告警）
2. 各采集节点引用的采集点是否存在且启用（电力表/化石燃料/外购热能；不存在或停用-错误）
3. 能耗小类覆盖检查（核算节点子树未覆盖因子模版中的小类-告警；采集节点小类不在因子模版中-提示）
4. 单独设置因子与因子模版一致性检查（不一致-告警）
5. 空核算节点检查（无任何采集节点的核算节点-告警）

#### 路径参数

| 字段  | 类型   | 必填  | 说明    |
|:--- |:---- |:--- |:----- |
| id  | Long | 是   | 核算模版ID |

#### 响应体

| 字段           | 类型      | 说明                                       |
|:----------- |:------- |:----------------------------------------- |
| checkResult | Integer | 校验结果：0-未检查，1-完全正确，2-正确(存在提示)，3-存在告警，4-存在错误 |
| checkTime   | String  | 校验时间(yyyy-MM-dd HH:mm:ss)                |
| checkMessage| String  | 校验结果详情（富文本HTML，错误红/告警橙/提示蓝/通过绿）           |
| items       | Array   | 结构化明细项列表                                  |

**items 明细项结构：**

| 字段      | 类型     | 说明                              |
|:------- |:------ |:-------------------------------- |
| level   | String | 级别：ERROR-错误，WARNING-告警，INFO-提示   |
| message | String | 明细内容                             |

**响应示例：**

```json
{
  "checkResult": 3,
  "checkTime": "2026-09-04 15:30:00",
  "checkMessage": "<div>...富文本HTML...</div>",
  "items": [
    { "level": "WARNING", "message": "碳排放因子模版「通用因子」中未设置任何缺省因子" },
    { "level": "ERROR", "message": "采集节点「电表A」引用的电力表「1#变压器」处于停用状态" }
  ]
}
```

#### 错误响应

| 场景         | HTTP状态码 | 响应体                          |
|:---------- |:------- |:----------------------------- |
| 模版不存在      | 500     | `{"error": "模版不存在: {id}", ...}` |
| 非核算模版      | 500     | `{"error": "仅核算模版支持校验", ...}`   |

---

### 3.9 查看校验结果

| 属性         | 值                                              |
|:---------- |:----------------------------------------------- |
| **URL**    | `/api/template/{id}/validation-result`          |
| **HTTP方法** | GET                                             |
| **所属文件**   | `controller/TemplateController.java:124-127`    |
| **前端调用**   | `components/TemplateList.vue`（"校验结果查看"按钮）       |

#### 功能描述

查询模版最近一次保存的校验结果（读取 `sys_emission_template` 表字段），**不重新执行校验**。
模版从未校验过时 checkResult=0、checkTime 与 checkMessage 为空。
注意：模版（属性/节点/节点配置）发生变更后，check_result 会自动重置为 0（未检查）。

#### 路径参数

| 字段  | 类型   | 必填  | 说明    |
|:--- |:---- |:--- |:----- |
| id  | Long | 是   | 核算模版ID |

#### 响应体

| 字段           | 类型      | 说明                                       |
|:----------- |:------- |:----------------------------------------- |
| checkResult | Integer | 校验结果：0-未检查，1-完全正确，2-正确(存在提示)，3-存在告警，4-存在错误 |
| checkTime   | String  | 最近一次校验时间，未校验过为空                          |
| checkMessage| String  | 富文本详情，未校验过为空                             |
| items       | Array   | 恒为空列表（明细仅校验接口实时返回）                        |

**响应示例：**

```json
{
  "checkResult": 4,
  "checkTime": "2026-09-04 15:30:00",
  "checkMessage": "<div>...富文本HTML...</div>",
  "items": []
}
```

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
        "emissionCategory": "购入的电力",
        "emissionSubcategory": "生产设施用电",
        "carbonEmissionFactor": 0.5306,
        "carbonEmissionFactorDescription": "2023年全国电力碳排放因子",
        "collectionDescription": "生产设施用电采集",
        "equipmentCode": "SB-0002",
        "collectionPointType": 1,
        "collectionPointId": 5
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
    "emissionCategory": "购入的电力",
    "emissionSubcategory": "生产设施用电",
    "carbonEmissionFactor": 0.5306,
    "carbonEmissionFactorDescription": "2023年全国电力碳排放因子",
    "collectionDescription": "生产设施用电采集",
    "equipmentCode": "SB-0002",
    "collectionPointType": 1,
    "collectionPointId": 5
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
    "emissionCategory": "化石燃料",
    "carbonEmissionFactor": 2.66,
    "carbonEmissionFactorDescription": "烟煤碳排放因子（用户手动修改）"
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

| 字段                                | 类型         | 说明                          |
|:--------------------------------- |:---------- |:--------------------------- |
| nodeId                            | Long       | 节点ID                        |
| emissionCategory                  | String     | 能耗品种大类                      |
| emissionSubcategory               | String     | 能耗品种小类                      |
| carbonEmissionFactor              | BigDecimal | 碳排放因子                       |
| carbonEmissionFactorDescription   | String     | 碳排放因子说明                     |
| collectionDescription             | String     | 采集描述说明                      |
| equipmentCode                     | String     | 采集设备编号                      |
| collectionPointType               | Integer    | 采集点类型：1-电力表，2-化石燃料，3-外购热能   |
| collectionPointId                 | Long       | 采集点ID，指向对应采集点表的主键           |
| updatedBy                         | Long       | 更新人ID（更新接口使用）               |

> **变更说明（5.1）**：sys_emission_node_config 表已删除统计口径（statisticalCaliber）、数据来源（dataSource）、核算场景（accountingScenario）、能耗用途（energyUse）、是否累计量（isCumulative）、是否移动源（isMobileSource）、计量单位（measurementUnit）、数据来源系统（dataSourceSystem）、获取方式（acquisitionMethod）、分摊比例（allocationRatio）、是否有子表（hasSubTable）、误差约束（errorConstraint）、更新周期（updateCycle）、更新时间（updateTime）、任务配置（taskConfig）字段，本接口不再返回/接收这些字段；新增采集点关联字段 collectionPointType/collectionPointId。

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

| 字段                                | 类型         | 必填  | 说明                        |
|:--------------------------------- |:---------- |:--- |:------------------------ |
| emissionCategory                  | String     | 否   | 能耗品种大类                    |
| emissionSubcategory               | String     | 否   | 能耗品种小类                    |
| carbonEmissionFactor              | BigDecimal | 否   | 碳排放因子                     |
| carbonEmissionFactorDescription   | String     | 否   | 碳排放因子说明                   |
| collectionDescription             | String     | 否   | 采集描述说明                    |
| equipmentCode                     | String     | 否   | 采集设备编号                    |
| collectionPointType               | Integer    | 否   | 采集点类型：1-电力表，2-化石燃料，3-外购热能 |
| collectionPointId                 | Long       | 否   | 采集点ID，指向对应采集点表的主键         |
| updatedBy                         | Long       | 否   | 更新人ID                     |

> **变更说明（5.1）**：原 statisticalCaliber、dataSource、accountingScenario、energyUse、isCumulative、isMobileSource、measurementUnit、dataSourceSystem、acquisitionMethod、allocationRatio、hasSubTable、errorConstraint、updateCycle、updateTime、taskConfig 等字段已随 sys_emission_node_config 表字段清理从请求体中移除。

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

| 字段                    | 类型   | 说明      |
|:--------------------- |:---- |:------- |
| nodeTypes             | List | 节点类型列表  |
| statisticalCalibers   | List | 统计口径列表  |
| emissionCategories    | List | 排放类别列表  |
| emissionSubcategories | Map  | 排放子类别映射 |
| locomotiveTypes       | List | 机车类型列表  |
| dataSources           | List | 数据来源列表  |
| nodeCategories        | Map  | 节点类别映射（总公司/分公司/站点/区域） |
| accountingScenarios   | List | 核算场景列表  |
| energyUses            | List | 能耗用途列表  |

> **说明（5.1）**：statisticalCalibers/dataSources/accountingScenarios/energyUses 等字典选项仍由本接口返回（数据字典模块保留维护），但节点配置（sys_emission_node_config）已不再存储这些属性。

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

| 属性         | 值                    |
|:---------- |:-------------------- |
| **URL**    | `/api/nodes/next-id` |
| **HTTP方法** | GET                  |

#### 功能描述

获取当前数据库中最大的节点ID并加1，用于在创建新节点前预测节点ID，以便生成唯一的设备编号。

#### 请求参数

无

#### 响应体

| 字段    | 类型   | 说明         |
|:----- |:---- |:---------- |
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

| 字段  | 类型   | 必填  | 说明         |
|:--- |:---- |:--- |:---------- |
| id  | Long | 是   | 父节点ID或节点ID |

#### 请求参数

| 字段               | 类型     | 必填  | 说明                    |
|:---------------- |:------ |:--- |:--------------------- |
| emissionCategory | String | 是   | 排放数据大类（如"化石燃料"、"电力"等） |
| newNodeId        | Long   | 否   | 新节点ID（用于新建节点场景）       |

#### 响应体

| 字段    | 类型     | 说明      |
|:----- |:------ |:------- |
| (返回值) | String | 生成的设备编号 |

**成功响应示例：**

```json
"y1-E1-EP-000307"
```

#### 编码规则说明

编码规则由系统配置 `Auto_coding_rules` 控制，支持以下规则：

| 规则值 | 编码格式                | 说明                      |
|:--- |:------------------- |:----------------------- |
| 1   | `一级编码-ID`           | 仅包含一级节点编码和节点ID          |
| 2   | `一级编码-二级编码-ID`      | 包含一级、二级节点编码和节点ID        |
| 3   | `一级编码-大类编码-ID`      | 包含一级节点编码、排放大类编码和节点ID    |
| 4   | `一级编码-二级编码-大类编码-ID` | 包含一级、二级节点编码、排放大类编码和节点ID |

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

### 4.12 挂载节点模版

| 属性         | 值                                              |
|:---------- |:---------------------------------------------- |
| **URL**    | `/api/nodes/{parentId}/mount-template`         |
| **HTTP方法** | POST                                           |
| **所属文件**   | `controller/EmissionNodeController.java:57-63` |

#### 功能描述

将指定节点模版（templateType=1）所包含的全部子节点复制到当前模版树中，挂载到目标父节点（typeId=1/2 的"排放核算点"）之下。每个被复制的节点通过 `source_node_id` 字段记录其来源节点ID，并同步复制对应的 `sys_emission_node_config` 与 `sys_emission_node_info`。顶层挂载节点 sort_order 从目标父节点下现有同级节点数量开始递增，子树内部保持来源 sort_order。

#### 路径参数

| 字段       | 类型   | 必填  | 说明                                |
|:-------- |:---- |:--- |:--------------------------------- |
| parentId | Long | 是   | 目标父节点ID（需为 typeId=1 或 2 的"排放核算点"） |

#### 请求体

| 字段         | 类型   | 必填  | 说明               |
|:---------- |:---- |:--- |:---------------- |
| templateId | Long | 是   | 来源节点模版ID（templateType=1） |
| createdBy  | Long | 是   | 创建人ID            |

**请求示例：**

```json
{
  "templateId": 5,
  "createdBy": 1
}
```

#### 响应体

返回挂载后的目标父节点 `NodeDTO` 对象（结构同 4.2）。

#### 说明

- 目标父节点必须存在且 typeId 为 1（根节点）或 2（核算子节点），否则返回错误
- 来源模版必须存在且 templateType=1（节点模版），否则返回错误
- 仅复制来源模版根节点的全部子节点（不含根节点本身），递归复制整个子树
- 复制产生的新节点 template_id 重置为目标父节点所属模版，parent_id 顶层映射到目标父节点、子层按映射重建
- 每个新节点 source_node_id 记录来源节点ID，便于追溯节点模版的挂载来源

---

## 5. 数据字典模块接口

### 5.1 获取所有字典

| 属性         | 值           |
|:---------- |:----------- |
| **URL**    | `/api/dict` |
| **HTTP方法** | GET         |

#### 功能描述

获取系统中所有预定义的数据字典分类及其选项列表。

#### 请求参数

无

#### 响应体

| 字段                    | 类型   | 说明      |
|:--------------------- |:---- |:------- |
| locomotiveTypes       | List | 机车类型列表  |
| statisticalCalibers   | List | 统计口径列表  |
| emissionCategories    | List | 排放类别列表  |
| emissionSubcategories | Map  | 排放子类别映射 |
| dataSources           | List | 数据来源列表  |
| nodeCategories        | List | 节点类型列表  |
| accountingScenarios   | List | 核算场景列表  |
| energyUses            | List | 能耗用途列表  |

---

### 5.2 获取指定字典的所有项

| 属性         | 值                      |
|:---------- |:---------------------- |
| **URL**    | `/api/dict/{dictCode}` |
| **HTTP方法** | GET                    |

#### 路径参数

| 字段       | 类型     | 必填  | 说明   |
|:-------- |:------ |:--- |:---- |
| dictCode | String | 是   | 字典编码 |

#### 响应体

返回指定字典的所有字典项列表。

---

### 5.3 根据字典编码获取启用的字典项

| 属性         | 值                                |
|:---------- |:-------------------------------- |
| **URL**    | `/api/data-dict/items/{dictCode}` |
| **HTTP方法** | GET                              |
| **所属文件**   | `controller/DataDictController.java` |

#### 功能描述

根据字典编码获取该字典下所有启用状态（status=1）的字典项，按 sort_order 升序排列。常用于下拉框数据源加载（如电表类型 meter_type）。

#### 路径参数

| 字段       | 类型     | 必填  | 说明                          |
|:-------- |:------ |:--- |:--------------------------- |
| dictCode | String | 是   | 字典编码（如 meter_type、locomotive_type） |

#### 响应体

返回 `DictItemVO` 对象数组：

| 字段    | 类型     | 说明    |
|:----- |:------ |:----- |
| code  | String | 字典项编码 |
| value | String | 字典项值  |

**成功响应示例：**

```json
[
  { "code": "single_phase", "value": "单相" },
  { "code": "three_phase_three_wire", "value": "三相三线" },
  { "code": "three_phase_four_wire", "value": "三相四线" },
  { "code": "comprehensive_power_monitor", "value": "综合电力监测仪表" }
]
```

---

### 5.4 根据字典编码获取所有字典项（含禁用）

| 属性         | 值                                     |
|:---------- |:------------------------------------- |
| **URL**    | `/api/data-dict/items/{dictCode}/all` |
| **HTTP方法** | GET                                   |

#### 功能描述

根据字典编码获取该字典下所有字典项（包括禁用状态），按 sort_order 升序排列。

#### 路径参数

| 字段       | 类型     | 必填  | 说明   |
|:-------- |:------ |:--- |:---- |
| dictCode | String | 是   | 字典编码 |

#### 响应体

返回 `DictItemVO` 对象数组，结构同 5.3。

---

## 6. 数据来源系统模块接口

### 6.1 获取所有数据来源系统

| 属性         | 值                          |
|:---------- |:-------------------------- |
| **URL**    | `/api/data-source-systems` |
| **HTTP方法** | GET                        |

#### 功能描述

获取系统中所有已创建的数据来源系统列表。

#### 请求参数

无

#### 响应体

返回 `DataSourceSystem` 对象数组：

| 字段          | 类型            | 说明       |
|:----------- |:------------- |:-------- |
| id          | Long          | 主键ID     |
| systemName  | String        | 数据来源系统名称 |
| description | String        | 说明       |
| pinyinCode  | String        | 拼音首字母编码  |
| createdBy   | Long          | 创建人ID    |
| updatedBy   | Long          | 更新人ID    |
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

| 属性         | 值                               |
|:---------- |:------------------------------- |
| **URL**    | `/api/data-source-systems/{id}` |
| **HTTP方法** | GET                             |

#### 路径参数

| 字段  | 类型   | 必填  | 说明       |
|:--- |:---- |:--- |:-------- |
| id  | Long | 是   | 数据来源系统ID |

#### 响应体

返回单个 `DataSourceSystem` 对象，结构同6.1。

---

### 6.3 搜索数据来源系统

| 属性         | 值                                 |
|:---------- |:--------------------------------- |
| **URL**    | `/api/data-source-systems/search` |
| **HTTP方法** | GET                               |

#### 功能描述

根据关键词搜索数据来源系统。支持拼音首字母筛选和模糊查询。

#### 请求参数

| 字段      | 类型     | 必填  | 说明    |
|:------- |:------ |:--- |:----- |
| keyword | String | 否   | 搜索关键词 |

#### 筛选规则

- 如果关键词全部为英文字母，则按拼音首字母筛选
- 否则按系统名称进行模糊查询

---

### 6.4 创建数据来源系统

| 属性         | 值                          |
|:---------- |:-------------------------- |
| **URL**    | `/api/data-source-systems` |
| **HTTP方法** | POST                       |

#### 请求体

| 字段          | 类型     | 必填  | 说明       |
|:----------- |:------ |:--- |:-------- |
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

| 字段      | 类型      | 说明    |
|:------- |:------- |:----- |
| success | Boolean | 是否成功  |
| data    | Object  | 创建的数据 |
| message | String  | 消息提示  |

---

### 6.5 更新数据来源系统

| 属性         | 值                               |
|:---------- |:------------------------------- |
| **URL**    | `/api/data-source-systems/{id}` |
| **HTTP方法** | PUT                             |

#### 路径参数

| 字段  | 类型   | 必填  | 说明       |
|:--- |:---- |:--- |:-------- |
| id  | Long | 是   | 数据来源系统ID |

#### 请求体

| 字段          | 类型     | 必填  | 说明       |
|:----------- |:------ |:--- |:-------- |
| systemName  | String | 是   | 数据来源系统名称 |
| description | String | 否   | 说明       |

---

### 6.6 删除数据来源系统

| 属性         | 值                               |
|:---------- |:------------------------------- |
| **URL**    | `/api/data-source-systems/{id}` |
| **HTTP方法** | DELETE                          |

#### 路径参数

| 字段  | 类型   | 必填  | 说明       |
|:--- |:---- |:--- |:-------- |
| id  | Long | 是   | 数据来源系统ID |

#### 说明

删除操作需要二次确认（前端处理）。

---

### 6.7 检查系统名称是否存在

| 属性         | 值                                 |
|:---------- |:--------------------------------- |
| **URL**    | `/api/data-source-systems/exists` |
| **HTTP方法** | GET                               |

#### 请求参数

| 字段         | 类型     | 必填  | 说明       |
|:---------- |:------ |:--- |:-------- |
| systemName | String | 是   | 数据来源系统名称 |

#### 响应体

| 字段     | 类型      | 说明    |
|:------ |:------- |:----- |
| exists | Boolean | 是否已存在 |

---

## 7. 电表设置模块接口

电表设置模块管理站点/区间-集中器-电表的树状结构，支持节点的增删改查、启停、移动、电表级联关系与从属集中器的修改。所有接口前缀为 `/api/meter-settings`，Controller 文件为 `MeterSettingsController.java`。

### 7.1 获取电表设置完整树

| 属性         | 值                                      |
|:---------- |:-------------------------------------- |
| **URL**    | `/api/meter-settings/tree`             |
| **HTTP方法** | GET                                    |

#### 功能描述

获取完整的电表设置树状结构，根节点为"电表设置"，下挂站点/区间→集中器→电表（含多级子表）。

#### 响应体

返回 `MeterTreeNodeDTO` 对象，结构见 7.13 节。

### 7.2 获取节点详情

| 属性         | 值                                                  |
|:---------- |:-------------------------------------------------- |
| **URL**    | `/api/meter-settings/node/{nodeType}/{id}`         |
| **HTTP方法** | GET                                                |

#### 路径参数

| 字段       | 类型     | 必填  | 说明                                |
|:-------- |:------ |:--- |:--------------------------------- |
| nodeType | String | 是   | 节点类型：station/concentrator/meter |
| id       | Long   | 是   | 节点ID                             |

### 7.3 创建站点/区间

| 属性         | 值                                      |
|:---------- |:-------------------------------------- |
| **URL**    | `/api/meter-settings/station`          |
| **HTTP方法** | POST                                   |

#### 请求体（CreateStationRequest）

| 字段          | 类型     | 必填  | 说明       |
|:----------- |:------ |:--- |:-------- |
| name        | String | 是   | 站点/区间名称  |
| pinyinCode  | String | 否   | 拼音编码（自动生成） |
| description | String | 否   | 描述信息     |
| createdBy   | Long   | 是   | 创建人ID    |

### 7.4 创建集中器

| 属性         | 值                                      |
|:---------- |:-------------------------------------- |
| **URL**    | `/api/meter-settings/concentrator`     |
| **HTTP方法** | POST                                   |

#### 请求体（CreateConcentratorRequest）

| 字段                  | 类型     | 必填  | 说明                |
|:------------------- |:------ |:--- |:----------------- |
| name                | String | 是   | 集中器名称             |
| pinyinCode          | String | 否   | 拼音编码（自动生成）        |
| stationIntervalId   | Long   | 是   | 所属站点/区间ID         |
| transformerCapacity | String | 否   | 变压器容量             |
| concentratorAddress | String | 否   | 集中器地址（仅含数字0-9）    |
| description         | String | 否   | 描述信息              |
| createdBy           | Long   | 是   | 创建人ID             |

### 7.5 创建电表

| 属性         | 值                                      |
|:---------- |:-------------------------------------- |
| **URL**    | `/api/meter-settings/meter`            |
| **HTTP方法** | POST                                   |

#### 请求体（CreateMeterRequest）

| 字段                    | 类型        | 必填  | 说明                          |
|:--------------------- |:--------- |:--- |:--------------------------- |
| name                  | String    | 是   | 电表名称                        |
| pinyinCode            | String    | 否   | 拼音编码（自动生成）                  |
| pointId               | Long      | 是   | 所属集中器ID                     |
| parentMeterId         | Long      | 否   | 上级电表ID，0表示无上级               |
| meterType             | String    | 否   | 电表类型（取自字典 meter_type）       |
| meterModel            | String    | 否   | 电表型号                        |
| meterAddress          | String    | 否   | 电表地址（仅含数字0-9）               |
| purposeDescription    | String    | 否   | 用途描述                        |
| energyAllocation      | String    | 否   | 能耗数据划拨                      |
| powerCategory         | String    | 否   | 用电分类                        |
| meterReadingMethod    | Integer   | 否   | 抄表方式：1-自动抄表，0-人工抄表          |
| autoMeterReadingConfig | String   | 否   | 自动抄表接口配置（JSON）              |
| energyCategoryL1      | String    | 否   | 能耗一级分类编码（关联 sys_emission_energy_category，如 EC03） |
| energyCategoryL2      | String    | 否   | 能耗二级分类编码（如 EC0303）          |
| energyCategoryL3      | String    | 否   | 能耗三级分类编码（如 EC030303）        |
| energyUseCategory     | String    | 否   | 能耗用途分类（替代原 accountingScenario） |
| parentChildRelationship | Integer | 否   | 总表与分表关系：1-总表计数等于分表之和（默认），2-不等于 |
| isVirtualMeter        | Integer   | 否   | 是否虚拟电表：1-是虚拟电表，0-不是（默认0，由系统维护） |
| isAllocationChild     | Integer   | 否   | 是否分摊虚拟子电表：1-是，0-否（默认0）；创建分摊子电表时置1 |
| allocationRatio       | BigDecimal | 否   | 分摊比例，默认 1.0；分摊子电表场景下需指定    |
| createdBy             | Long      | 是   | 创建人ID                       |

#### 说明

- 在电表节点下创建子表时，`parentMeterId` 指向当前电表，`pointId` 自动从父电表继承（子表与父表同属一个集中器）。
- 上级电表必须与当前电表同属一个集中器。
- `isVirtualMeter` 由系统在创建时维护：用户通过"添加下级虚拟电表"入口创建虚拟电表时置 1；创建分摊子电表时继承其父节点的 `isVirtualMeter` 值，不能强制修改为 1。
- `energyCategoryL1/L2/L3` 为三级级联编码，修改一级时需重置二级/三级，修改二级时需重置三级。
- 更新电表接口（7.6）不支持修改 `isVirtualMeter`（由系统维护）；可修改 `parentChildRelationship`、`isAllocationChild`、`allocationRatio`。

### 7.6 更新节点

| 属性         | 值                                                  |
|:---------- |:-------------------------------------------------- |
| **URL**    | `/api/meter-settings/node/{nodeType}/{id}`         |
| **HTTP方法** | PUT                                                |

#### 路径参数

| 字段       | 类型     | 必填  | 说明                                |
|:-------- |:------ |:--- |:--------------------------------- |
| nodeType | String | 是   | 节点类型：station/concentrator/meter |
| id       | Long   | 是   | 节点ID                             |

#### 请求体

根据 nodeType 对应 CreateStationRequest / CreateConcentratorRequest / CreateMeterRequest。

### 7.7 删除节点

| 属性         | 值                                                  |
|:---------- |:-------------------------------------------------- |
| **URL**    | `/api/meter-settings/node/{nodeType}/{id}`         |
| **HTTP方法** | DELETE                                             |

#### 请求头

| 字段         | 类型   | 必填  | 说明    |
|:---------- |:---- |:--- |:----- |
| X-User-Id | Long | 否   | 操作人ID |

#### 说明

- 仅停用状态（status=0）的节点可删除。
- 删除电表时，会先将其子电表的 parent_meter_id 置为0（解除父子关系）。

### 7.8 切换节点状态（启用/停用）

| 属性         | 值                                                              |
|:---------- |:-------------------------------------------------------------- |
| **URL**    | `/api/meter-settings/node/{nodeType}/{id}/toggle-status`      |
| **HTTP方法** | POST                                                           |

#### 请求头

| 字段         | 类型   | 必填  | 说明    |
|:---------- |:---- |:--- |:----- |
| X-User-Id | Long | 否   | 操作人ID |

#### 响应体

| 字段      | 类型      | 说明                          |
|:------- |:------- |:--------------------------- |
| message | String  | 操作结果消息（含失败原因，如级联约束）         |
| success | Boolean | 是否成功                        |

#### 说明

- 站点停用受 `Disable_all_concentrators` 与 `Disable_all_meter` 配置控制。
- 集中器/电表停用受 `Disable_all_meter` 配置控制。
- 当配置允许时，停用上级节点可一键级联停用所有下级节点。

### 7.9 上移/下移节点

| 属性         | 值                                                              |
|:---------- |:-------------------------------------------------------------- |
| **URL**    | `/api/meter-settings/node/{nodeType}/{id}/move-up`            |
| **HTTP方法** | POST                                                           |

| 属性         | 值                                                              |
|:---------- |:-------------------------------------------------------------- |
| **URL**    | `/api/meter-settings/node/{nodeType}/{id}/move-down`          |
| **HTTP方法** | POST                                                           |

#### 请求头

| 字段         | 类型   | 必填  | 说明    |
|:---------- |:---- |:--- |:----- |
| X-User-Id | Long | 否   | 操作人ID |

#### 说明

- 节点只能在同级（同父节点下）移动，不能跨级移动。
- 已是第一个/最后一个节点时移动无效。

### 7.10 更改电表级联关系

| 属性         | 值                                      |
|:---------- |:-------------------------------------- |
| **URL**    | `/api/meter-settings/meter/{id}/cascade` |
| **HTTP方法** | PUT                                    |

#### 路径参数

| 字段  | 类型   | 必填  | 说明   |
|:--- |:---- |:--- |:---- |
| id  | Long | 是   | 电表ID |

#### 请求体（ChangeMeterCascadeRequest）

| 字段            | 类型   | 必填  | 说明                  |
|:------------- |:---- |:--- |:------------------- |
| parentMeterId | Long | 是   | 新的上级电表ID，0表示无上级电表   |
| userId        | Long | 否   | 操作人ID              |

#### 校验规则

- 上级电表必须在同一集中器下。
- 不能将自身设置为上级电表。
- 不能将子表设置为上级电表（防止循环引用）。

#### 响应体

返回更新后的 `MeterTreeNodeDTO` 对象。

### 7.11 更改电表从属数据集中器

| 属性         | 值                                      |
|:---------- |:-------------------------------------- |
| **URL**    | `/api/meter-settings/meter/{id}/point` |
| **HTTP方法** | PUT                                    |

#### 路径参数

| 字段  | 类型   | 必填  | 说明   |
|:--- |:---- |:--- |:---- |
| id  | Long | 是   | 电表ID |

#### 请求体（ChangeMeterPointRequest）

| 字段            | 类型   | 必填  | 说明                  |
|:------------- |:---- |:--- |:------------------- |
| pointId       | Long | 是   | 新的集中器ID             |
| parentMeterId | Long | 是   | 新的上级电表ID，0表示无上级电表   |
| userId        | Long | 否   | 操作人ID              |

#### 校验规则

- 新集中器必须存在。
- 若指定上级电表，则上级电表必须在新集中器下。
- 不能将自身或子表设置为上级电表（防止循环引用）。

#### 响应体

返回更新后的 `MeterTreeNodeDTO` 对象。

### 7.12 查询辅助接口

#### 7.12.1 获取指定集中器下的所有电表

| 属性         | 值                                                |
|:---------- |:------------------------------------------------ |
| **URL**    | `/api/meter-settings/meters-by-point`            |
| **HTTP方法** | GET                                              |

#### 请求参数

| 字段             | 类型   | 必填  | 说明          |
|:-------------- |:---- |:--- |:----------- |
| pointId        | Long | 是   | 集中器ID       |
| excludeMeterId | Long | 否   | 排除的电表ID（自身） |

#### 响应体

返回 `MeterTreeNodeDTO` 数组（不含子节点列表）。

#### 7.12.2 获取指定站点/区间下的所有集中器

| 属性         | 值                                                    |
|:---------- |:---------------------------------------------------- |
| **URL**    | `/api/meter-settings/concentrators-by-station`       |
| **HTTP方法** | GET                                                  |

#### 请求参数

| 字段        | 类型   | 必填  | 说明     |
|:--------- |:---- |:--- |:------ |
| stationId | Long | 是   | 站点/区间ID |

#### 响应体

返回 `MeterTreeNodeDTO` 数组（不含子节点列表）。

### 7.13 MeterTreeNodeDTO 结构

| 字段                    | 类型                    | 说明                                            |
|:--------------------- |:--------------------- |:--------------------------------------------- |
| id                    | Long                  | 节点ID                                         |
| name                  | String                | 节点名称                                         |
| nodeType              | String                | 节点类型：root/station/concentrator/meter        |
| status                | Integer               | 状态：1-启用，0-停用                                |
| statusText            | String                | 状态文本：启用/停用                                   |
| pinyinCode            | String                | 拼音首字母编码                                      |
| description           | String                | 描述信息                                         |
| parentId              | Long                  | 所属父节点ID                                      |
| pointId               | Long                  | 所属集中器ID（电表节点专用）                              |
| transformerCapacity   | String                | 变压器容量（集中器节点）                                 |
| concentratorAddress   | String                | 集中器地址（集中器节点）                                 |
| meterType             | String                | 电表类型（电表节点）                                   |
| meterModel            | String                | 电表型号（电表节点）                                   |
| meterAddress          | String                | 电表地址（电表节点）                                   |
| powerCategory         | String                | 用电分类（电表节点）                                   |
| energyAllocation      | String                | 能耗数据划拨（电表节点）                                 |
| meterReadingMethod    | Integer               | 抄表方式：1-自动抄表，0-人工抄表（电表节点）                    |
| autoMeterReadingConfig | String               | 自动抄表接口配置（电表节点）                               |
| energyCategoryL1      | String                | 能耗一级分类编码（电表节点，关联 sys_emission_energy_category）  |
| energyCategoryL2      | String                | 能耗二级分类编码（电表节点）                              |
| energyCategoryL3      | String                | 能耗三级分类编码（电表节点）                              |
| energyUseCategory     | String                | 能耗用途分类（电表节点）                                |
| parentChildRelationship | Integer             | 总表与分表关系：1-等于分表之和，2-不等于（电表节点）                |
| isVirtualMeter        | Integer               | 是否虚拟电表：1-是虚拟电表，0-不是（电表节点）                  |
| isAllocationChild     | Integer               | 是否分摊虚拟子电表：1-是，0-否（电表节点）                    |
| allocationRatio       | BigDecimal            | 分摊比例（电表节点，分摊子电表场景下使用）                       |
| children              | List<MeterTreeNodeDTO> | 子节点列表                                        |
| hasChildren           | Boolean               | 是否有子节点                                       |
| createdAt             | String                | 创建时间                                         |
| updatedAt             | String                | 更新时间                                         |

---

## 8. 电表型号模块接口

电表型号模块提供电表型号的增删改查接口。所有接口前缀为 `/api/meter-model`，Controller 文件为 `MeterModelController.java`。

### 8.1 获取所有电表型号

| 属性         | 值                          |
|:---------- |:-------------------------- |
| **URL**    | `/api/meter-model`         |
| **HTTP方法** | GET                        |

#### 响应体

返回 `MeterModel` 对象数组：

| 字段          | 类型      | 说明       |
|:----------- |:------- |:-------- |
| id          | Long    | 主键ID     |
| modelName   | String  | 电表型号     |
| modelType   | String  | 所属类型     |
| description | String  | 描述信息     |
| createdBy   | Long    | 创建人ID    |
| updatedBy   | Long    | 修改人ID    |
| createdAt   | String  | 创建时间     |
| updatedAt   | String  | 修改时间     |

### 8.2 根据ID获取电表型号

| 属性         | 值                          |
|:---------- |:-------------------------- |
| **URL**    | `/api/meter-model/{id}`    |
| **HTTP方法** | GET                        |

#### 路径参数

| 字段  | 类型   | 必填  | 说明     |
|:--- |:---- |:--- |:------ |
| id  | Long | 是   | 电表型号ID |

### 8.3 根据型号名称模糊查询

| 属性         | 值                              |
|:---------- |:------------------------------ |
| **URL**    | `/api/meter-model/search`      |
| **HTTP方法** | GET                            |

#### 请求参数

| 字段      | 类型     | 必填  | 说明   |
|:------- |:------ |:--- |:---- |
| keyword | String | 是   | 搜索关键词 |

### 8.4 新增电表型号

| 属性         | 值                          |
|:---------- |:-------------------------- |
| **URL**    | `/api/meter-model`         |
| **HTTP方法** | POST                       |

#### 请求体

| 字段          | 类型     | 必填  | 说明                  |
|:----------- |:------ |:--- |:------------------- |
| modelName   | String | 是   | 电表型号                |
| modelType   | String | 否   | 所属类型：单相、三相三线、三相四线   |
| description | String | 否   | 描述信息                |
| createdBy   | Long   | 否   | 创建人ID               |

### 8.5 更新电表型号

| 属性         | 值                          |
|:---------- |:-------------------------- |
| **URL**    | `/api/meter-model/{id}`    |
| **HTTP方法** | PUT                        |

#### 路径参数

| 字段  | 类型   | 必填  | 说明     |
|:--- |:---- |:--- |:------ |
| id  | Long | 是   | 电表型号ID |

#### 请求体

| 字段          | 类型     | 必填  | 说明            |
|:----------- |:------ |:--- |:------------- |
| modelName   | String | 是   | 电表型号          |
| modelType   | String | 否   | 所属类型          |
| description | String | 否   | 描述信息          |
| updatedBy   | Long   | 否   | 修改人ID         |

### 8.6 删除电表型号

| 属性         | 值                          |
|:---------- |:-------------------------- |
| **URL**    | `/api/meter-model/{id}`    |
| **HTTP方法** | DELETE                     |

#### 路径参数

| 字段  | 类型   | 必填  | 说明     |
|:--- |:---- |:--- |:------ |
| id  | Long | 是   | 电表型号ID |

---

## 9. 错误响应格式

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

## 10. DTO类关系图

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
│  ├─ emissionCategory       ├─ nodeCode                          │
│  ├─ emissionSubcategory    ├─ shortName                         │
│  ├─ carbonEmissionFactor   ├─ includeInCalculation             │
│  ├─ carbonEmissionFactorDescription ├─ nodeCategory             │
│  ├─ collectionDescription  ├─ unitDescription                  │
│  ├─ equipmentCode          ├─ orgBoundaryDescription           │
│  ├─ collectionPointType    └─ operationBoundaryDescription     │
│  ├─ collectionPointId                                           │
│  └─ updatedBy                                                   │
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

## 11. 接口调用流程图

### 11.1 用户登录流程

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
  │   {userId, userName, nickName, deptId}│         │
  │                       │                        │
```

### 11.1.1 登录后首页导航流程

```
前端                    后端                    数据库
  │                       │                        │
  │── 登录成功后 ──▶│  跳转到功能首页(HomePage)        │
  │                       │                        │
  │                       │  显示用户信息(nickName)    │
  │                       │                        │
  │── 点击"碳排放模型管理" ──▶│  跳转到模版列表页        │
  │   传递用户信息到后续页面  │                        │
  │                       │                        │
  │── 点击"碳排放核算参数设置" ──▶│  (预留入口，待实现)   │
  │                       │                        │
```

### 11.2 获取节点树流程

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

### 11.3 创建节点流程

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

### 11.4 数据来源系统选择流程

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

## 12. 前端API调用示例（Axios）

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

## 13. 排放因子库模块接口

### 13.1 电力碳排放因子库

#### 13.1.1 获取所有电力碳排放因子

| 属性         | 值                                 |
|:---------- |:--------------------------------- |
| **URL**    | `/api/electricity-carbon-factors` |
| **HTTP方法** | GET                               |

#### 请求参数

无

#### 响应体

| 字段          | 类型         | 说明      |
|:----------- |:---------- |:------- |
| id          | Long       | 主键ID    |
| factorName  | String     | 碳排放因子名称 |
| factorValue | BigDecimal | 碳排放因子值  |
| unit        | String     | 单位      |
| description | String     | 说明      |

---

#### 13.1.2 根据ID获取电力碳排放因子

| 属性         | 值                                      |
|:---------- |:-------------------------------------- |
| **URL**    | `/api/electricity-carbon-factors/{id}` |
| **HTTP方法** | GET                                    |

#### 路径参数

| 字段  | 类型   | 必填  | 说明   |
|:--- |:---- |:--- |:---- |
| id  | Long | 是   | 因子ID |

---

#### 13.1.3 创建电力碳排放因子

| 属性         | 值                                 |
|:---------- |:--------------------------------- |
| **URL**    | `/api/electricity-carbon-factors` |
| **HTTP方法** | POST                              |

#### 请求体

| 字段          | 类型         | 必填  | 说明      |
|:----------- |:---------- |:--- |:------- |
| factorName  | String     | 是   | 碳排放因子名称 |
| factorValue | BigDecimal | 是   | 碳排放因子值  |
| unit        | String     | 是   | 单位      |
| description | String     | 否   | 说明      |

---

### 13.2 化石燃料排放因子库

#### 13.2.1 获取所有化石燃料排放因子

| 属性         | 值                          |
|:---------- |:-------------------------- |
| **URL**    | `/api/fossil-fuel-factors` |
| **HTTP方法** | GET                        |

---

#### 13.2.2 根据ID获取化石燃料排放因子

| 属性         | 值                               |
|:---------- |:------------------------------- |
| **URL**    | `/api/fossil-fuel-factors/{id}` |
| **HTTP方法** | GET                             |

---

#### 13.2.3 创建化石燃料排放因子

| 属性         | 值                          |
|:---------- |:-------------------------- |
| **URL**    | `/api/fossil-fuel-factors` |
| **HTTP方法** | POST                       |

#### 请求体

| 字段                       | 类型         | 必填  | 说明      |
|:------------------------ |:---------- |:--- |:------- |
| emissionFactorName       | String     | 是   | 碳排放因子名称 |
| fuelType                 | String     | 是   | 燃料品种    |
| source                   | String     | 否   | 来源      |
| unit                     | String     | 否   | 计量单位    |
| lowerHeatingValue        | BigDecimal | 否   | 低位发热量   |
| carbonContentPerUnitHeat | BigDecimal | 否   | 单位热值含碳量 |
| fuelOxidationRate        | BigDecimal | 否   | 燃料氧化率   |
| emissionFactor           | BigDecimal | 否   | 碳排放因子   |
| factorUnit               | String     | 否   | 单位      |
| description              | String     | 否   | 说明      |

---

### 13.3 饱和蒸汽热焓值表

#### 13.3.1 获取所有饱和蒸汽热焓值

| 属性         | 值                               |
|:---------- |:------------------------------- |
| **URL**    | `/api/saturated-steam-enthalpy` |
| **HTTP方法** | GET                             |

#### 响应体

| 字段          | 类型         | 说明       |
|:----------- |:---------- |:-------- |
| id          | Long       | 主键ID     |
| pressure    | BigDecimal | 压力（Mpa）  |
| temperature | BigDecimal | 温度（°C）   |
| enthalpy    | BigDecimal | 焓（KJ/kg） |

---

### 13.4 过热蒸汽热焓值表

#### 7.4.1 获取所有过热蒸汽热焓值

| 属性         | 值                                 |
|:---------- |:--------------------------------- |
| **URL**    | `/api/superheated-steam-enthalpy` |
| **HTTP方法** | GET                               |

---

### 13.5 热力排放因子库

#### 13.5.1 获取所有热力排放因子

| 属性         | 值                               |
|:---------- |:------------------------------- |
| **URL**    | `/api/thermal-emission-factors` |
| **HTTP方法** | GET                             |

---

### 13.6 固体废弃物焚烧排放因子库

#### 13.6.1 获取所有固体废弃物焚烧排放因子

| 属性         | 值                                 |
|:---------- |:--------------------------------- |
| **URL**    | `/api/waste-incineration-factors` |
| **HTTP方法** | GET                               |

#### 响应体

| 字段                 | 类型         | 说明         |
|:------------------ |:---------- |:---------- |
| id                 | Long       | 主键ID       |
| emissionFactorName | String     | 碳排放因子名称    |
| wasteType          | String     | 固体废物种类     |
| ccw                | BigDecimal | 废弃物中的碳含量比例 |
| fcf                | BigDecimal | 化石碳比例      |
| ce                 | BigDecimal | 燃烧效率       |
| emissionFactor     | BigDecimal | 碳排放因子      |
| unit               | String     | 单位         |

---

### 13.7 废水处理排放因子库

#### 13.7.1 获取所有废水处理排放因子

| 属性         | 值                                   |
|:---------- |:----------------------------------- |
| **URL**    | `/api/wastewater-treatment-factors` |
| **HTTP方法** | GET                                 |

#### 响应体

| 字段                 | 类型         | 说明              |
|:------------------ |:---------- |:--------------- |
| id                 | Long       | 主键ID            |
| emissionFactorName | String     | 碳排放因子名称         |
| wastewaterType     | String     | 处理废水种类          |
| od                 | BigDecimal | 需氧浓度系数（COD或BOD） |
| bo                 | BigDecimal | 最大甲烷产生能力        |
| mcf                | BigDecimal | 甲烷修正因子          |
| gwp                | BigDecimal | 甲烷的全球变暖潜能值      |
| emissionFactor     | BigDecimal | 碳排放因子           |
| unit               | String     | 单位              |

---

## 14. 排放因子库前端API调用示例

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

## 15. 化石燃料采集设置模块接口

本模块管理化石燃料能耗数据采集设备的"采集范围 → 细分范围 → 采集点"三级树状结构，采集点支持多级子表挂接。所有接口前缀为 `/api/fossil-fuel-collection`，对应 `FossilFuelCollectionController.java`。

### 15.1 获取采集树

| 项目   | 说明                                  |
|:---- |:------------------------------------ |
| 路径   | `GET /api/fossil-fuel-collection/tree` |
| 功能   | 构建完整的化石燃料采集树形结构，包含采集范围、细分范围、采集点三级及以上的层级关系 |

**响应示例**：

```json
{
  "id": 1,
  "name": "化石燃料采集设置",
  "nodeType": "root",
  "children": [
    {
      "id": 10,
      "name": "原平分公司",
      "nodeType": "scope",
      "pinyinCode": "YPFGS",
      "sortOrder": 0,
      "status": 1,
      "children": [
        {
          "id": 100,
          "name": "炼焦车间",
          "nodeType": "sub_scope",
          "subScopeName": "炼焦车间",
          "collectionScopeId": 10,
          "sortOrder": 0,
          "status": 1,
          "children": [
            {
              "id": 1000,
              "name": "1号焦炉计量表",
              "nodeType": "meter",
              "fuelType": "FF_COK",
              "meterModel": "MODEL-A",
              "parentMeterId": 0,
              "parentMeterName": "",
              "subScopeId": 100,
              "subScopeName": "炼焦车间",
              "collectionScopeId": 10,
              "meterReadingMethod": 1,
              "energyUse": "生产",
              "energyAllocation": "原分",
              "status": 1,
              "children": []
            }
          ]
        }
      ]
    }
  ]
}
```

### 15.2 采集范围（Scope）接口

| 方法     | 路径                                          | 功能                      |
|:------ |:-------------------------------------------- |:------------------------ |
| GET    | `/api/fossil-fuel-collection/scopes`        | 获取所有采集范围列表               |
| GET    | `/api/fossil-fuel-collection/scopes/{id}`   | 获取指定ID的采集范围              |
| POST   | `/api/fossil-fuel-collection/scopes`        | 新增采集范围（sortOrder 强制取最大值+1） |
| PUT    | `/api/fossil-fuel-collection/scopes/{id}`   | 更新采集范围信息                 |
| DELETE | `/api/fossil-fuel-collection/scopes/{id}`   | 删除采集范围（级联删除下属细分范围与采集点）  |

### 15.3 细分范围（SubScope）接口

| 方法     | 路径                                                              | 功能                              |
|:------ |:---------------------------------------------------------------- |:-------------------------------- |
| GET    | `/api/fossil-fuel-collection/sub-scopes`                         | 获取所有细分范围列表（含所属采集范围名称）           |
| GET    | `/api/fossil-fuel-collection/sub-scopes/{id}`                    | 获取指定ID的细分范围                     |
| GET    | `/api/fossil-fuel-collection/scopes/{scopeId}/sub-scopes`        | 获取指定采集范围下的所有细分范围                |
| POST   | `/api/fossil-fuel-collection/sub-scopes`                         | 新增细分范围（sortOrder 强制取最大值+1）       |
| PUT    | `/api/fossil-fuel-collection/sub-scopes/{id}`                    | 更新细分范围信息                         |
| DELETE | `/api/fossil-fuel-collection/sub-scopes/{id}`                    | 删除细分范围（级联删除下属采集点）              |

### 15.4 采集点（Meter）接口

| 方法     | 路径                                                               | 功能                       |
|:------ |:----------------------------------------------------------------- |:------------------------- |
| GET    | `/api/fossil-fuel-collection/meters`                              | 获取所有采集点列表                 |
| GET    | `/api/fossil-fuel-collection/meters/{id}`                         | 获取指定ID的采集点               |
| GET    | `/api/fossil-fuel-collection/sub-scopes/{subScopeId}/meters`      | 获取指定细分范围下的采集点列表          |
| POST   | `/api/fossil-fuel-collection/meters`                              | 新增采集点（sortOrder 强制取最大值+1） |
| PUT    | `/api/fossil-fuel-collection/meters/{id}`                         | 更新采集点信息                  |
| DELETE | `/api/fossil-fuel-collection/meters/{id}`                         | 删除采集点（子采集点 parentMeterId 置0） |

### 15.5 状态与排序接口

| 方法   | 路径                                                       | 功能                                  |
|:---- |:--------------------------------------------------------- |:------------------------------------ |
| PUT  | `/api/fossil-fuel-collection/{id}/status`                 | 更新节点状态（支持 scope/sub_scope/meter 三种类型） |
| PUT  | `/api/fossil-fuel-collection/scopes/{id}/sort`            | 更新采集范围排序值                            |
| PUT  | `/api/fossil-fuel-collection/sub-scopes/{id}/sort`        | 更新细分范围排序值                            |
| PUT  | `/api/fossil-fuel-collection/meters/{id}/sort`            | 更新采集点排序值                             |

**状态更新请求体**：

```json
{
  "nodeType": "meter",
  "status": 0,
  "userId": 1
}
```

### 15.6 采集点级联关系与所属细分范围变更

#### 15.6.1 获取细分范围下的采集点列表

| 项目   | 说明                                          |
|:---- |:-------------------------------------------- |
| 路径   | `GET /api/fossil-fuel-collection/meters-by-sub-scope` |
| 功能   | 获取指定细分范围下的采集点列表，可排除指定采集点（用于级联关系/细分范围变更弹窗） |

**请求参数**：

| 参数名            | 类型     | 必填 | 说明                |
|:-------------- |:------ |:--- |:------------------ |
| subScopeId     | Long   | 是   | 细分范围ID            |
| excludeMeterId | Long   | 否   | 需排除的采集点ID（通常为当前节点） |

**响应示例**：

```json
[
  { "id": 1001, "name": "2号焦炉计量表", "parentMeterId": 0, "sortOrder": 1 },
  { "id": 1002, "name": "3号焦炉计量表", "parentMeterId": 1001, "sortOrder": 2 }
]
```

#### 15.6.2 更改采集点级联关系

| 项目   | 说明                                                      |
|:---- |:-------------------------------------------------------- |
| 路径   | `PUT /api/fossil-fuel-collection/meters/{id}/cascade`    |
| 功能   | 更改指定采集点的上级采集点（parentMeterId），不改变所属细分范围     |

**请求体**：

```json
{
  "parentMeterId": 1001,
  "userId": 1
}
```

| 字段名           | 类型   | 说明                          |
|:------------- |:----- |:--------------------------- |
| parentMeterId | Long  | 新的上级采集点ID，0表示无上级采集点         |
| userId        | Long  | 操作人ID                       |

#### 15.6.3 更改采集点所属细分范围

| 项目   | 说明                                                       |
|:---- |:--------------------------------------------------------- |
| 路径   | `PUT /api/fossil-fuel-collection/meters/{id}/sub-scope`   |
| 功能   | 更改指定采集点的所属细分范围（subScopeId）和上级采集点（parentMeterId），并**递归更新所有子孙采集点**的 subScopeId |

**请求体**：

```json
{
  "subScopeId": 200,
  "parentMeterId": 0,
  "userId": 1
}
```

| 字段名           | 类型   | 说明                                  |
|:------------- |:----- |:----------------------------------- |
| subScopeId    | Long  | 新的细分范围ID                            |
| parentMeterId | Long  | 新的上级采集点ID，0表示无上级采集点                 |
| userId        | Long  | 操作人ID                               |

**递归更新逻辑**：调用此接口后，后端会递归遍历当前采集点下的所有子孙采集点，将它们的 `sub_scope_id` 同步更新为新的细分范围ID，保证整棵子树的从属关系一致。

### 15.7 节点上下移动

| 方法   | 路径                                                              | 功能                            |
|:---- |:---------------------------------------------------------------- |:------------------------------ |
| POST | `/api/fossil-fuel-collection/{nodeType}/{id}/move-up`            | 将指定节点在同级中上移一位（交换 sortOrder）    |
| POST | `/api/fossil-fuel-collection/{nodeType}/{id}/move-down`          | 将指定节点在同级中下移一位（交换 sortOrder）    |

**路径参数**：

| 参数名      | 类型     | 说明                                  |
|:-------- |:------ |:------------------------------------ |
| nodeType | String | 节点类型：`scope`、`sub_scope`、`meter`     |
| id       | Long   | 节点ID                                |

### 15.8 前端 API 调用示例（Axios）

```javascript
const API_BASE = '/api/fossil-fuel-collection';

// 获取采集树
const loadTree = async () => {
  const response = await axios.get(`${API_BASE}/tree`);
  return response.data;
};

// 新增采集点（所属细分范围从父节点自动继承）
const createMeter = async (meterData) => {
  const response = await axios.post(`${API_BASE}/meters`, meterData);
  return response.data;
};

// 更改采集点级联关系
const updateMeterCascade = async (meterId, parentMeterId) => {
  const response = await axios.put(`${API_BASE}/meters/${meterId}/cascade`, {
    parentMeterId: parentMeterId,
    userId: 1
  });
  return response.data;
};

// 更改采集点所属细分范围（递归更新子节点）
const updateMeterSubScope = async (meterId, subScopeId, parentMeterId) => {
  const response = await axios.put(`${API_BASE}/meters/${meterId}/sub-scope`, {
    subScopeId: subScopeId,
    parentMeterId: parentMeterId,
    userId: 1
  });
  return response.data;
};

// 获取细分范围下的采集点列表（排除自身）
const getMetersBySubScope = async (subScopeId, excludeMeterId) => {
  const response = await axios.get(`${API_BASE}/meters-by-sub-scope`, {
    params: { subScopeId, excludeMeterId }
  });
  return response.data;
};
```

---

## 16. 能耗分类数据字典模块接口

本模块提供三级能耗分类字典的查询能力，供电力表设置、化石燃料采集设置、外购热力采集设置等模块表单的"能耗场景大类 → 能耗二级分类 → 能耗三级分类"级联下拉框使用。所有接口前缀为 `/api/energy-categories`，Controller 文件为 `EnergyCategoryController.java`。

### 16.1 获取所有能耗分类

| 属性         | 值                                                |
|:---------- |:------------------------------------------------- |
| **URL**    | `/api/energy-categories`                          |
| **HTTP方法** | GET                                               |
| **所属文件**   | `controller/EnergyCategoryController.java:31-48` |

#### 功能描述

返回所有能耗分类的扁平列表，按 `level` 升序、`sortOrder` 升序排列。前端可依据返回数据中的 `parentId` 与 `level` 字段自行构建三级级联关系。组件挂载时调用一次即可完成本地缓存。

#### 响应体

返回 `List<Map<String, Object>>`，每项结构如下：

| 字段           | 类型      | 说明                            |
|:------------ |:------- |:---------------------------- |
| id           | Long    | 主键ID                          |
| categoryCode | String  | 分类编码（如 EC01、EC0101、EC010101） |
| categoryName | String  | 分类名称（如"生产用能"）                |
| parentId     | Long    | 父级分类ID，顶级为 null              |
| level        | Integer | 分类层级：1-一级，2-二级，3-三级         |
| remark       | String  | 备注说明                          |
| sortOrder    | Integer | 排序顺序                          |
| status       | Integer | 状态：1-启用，0-停用                 |

**响应示例：**

```json
[
  { "id": 1, "categoryCode": "EC01", "categoryName": "生产用能", "parentId": null, "level": 1, "remark": null, "sortOrder": 1, "status": 1 },
  { "id": 2, "categoryCode": "EC02", "categoryName": "生产辅助用能", "parentId": null, "level": 1, "remark": null, "sortOrder": 2, "status": 1 },
  { "id": 4, "categoryCode": "EC0101", "categoryName": "牵引供电", "parentId": 1, "level": 2, "remark": "指电力机车/动车组从接触网获取的电能", "sortOrder": 1, "status": 1 }
]
```

### 16.2 获取指定层级的能耗分类

| 属性         | 值                                                |
|:---------- |:------------------------------------------------- |
| **URL**    | `/api/energy-categories/level/{level}`            |
| **HTTP方法** | GET                                               |
| **所属文件**   | `controller/EnergyCategoryController.java:56-73` |

#### 路径参数

| 字段     | 类型     | 必填  | 说明                    |
|:------ |:------ |:--- |:--------------------- |
| level  | Integer | 是   | 分类层级：1-一级，2-二级，3-三级 |

#### 响应体

返回 `List<Map<String, Object>>`，结构与 16.1 相同，仅包含指定层级、按 `sortOrder` 升序排列的分类。

### 16.3 获取指定父级下的子分类

| 属性         | 值                                                  |
|:---------- |:--------------------------------------------------- |
| **URL**    | `/api/energy-categories/children/{parentId}`       |
| **HTTP方法** | GET                                                 |
| **所属文件**   | `controller/EnergyCategoryController.java:81-98` |

#### 路径参数

| 字段       | 类型   | 必填  | 说明        |
|:-------- |:---- |:--- |:-------- |
| parentId | Long | 是   | 父级分类ID |

#### 响应体

返回 `List<Map<String, Object>>`，结构与 16.1 相同，仅包含指定父级下的子分类、按 `sortOrder` 升序排列。

### 16.4 前端 API 调用示例（Axios）

```javascript
const API_BASE = '/api/energy-categories';

// 组件挂载时一次性加载全部分类并缓存到本地
const loadEnergyCategories = async () => {
  const response = await axios.get(API_BASE);
  return response.data; // 数组，按 level 与 sortOrder 排序
};

// 计算一级分类下拉选项（前端 computed 过滤 level === 1）
const energyCategoryL1Options = computed(() =>
  energyCategories.value.filter(c => c.level === 1)
);

// 选中一级后，计算其下属二级分类下拉选项
const energyCategoryL2Options = computed(() => {
  if (!form.energyCategoryL1) return [];
  const parent = energyCategories.value.find(c => c.categoryCode === form.energyCategoryL1);
  return parent
    ? energyCategories.value.filter(c => c.parentId === parent.id)
    : [];
});
```

**前端使用约定：**

- 推荐在组件挂载时调用一次 16.1 接口将全部分类缓存到本地（如 `energyCategories.value`），后续二级/三级下拉选项通过前端 `computed` 按 `parentId` 过滤即可，避免重复请求。
- 三级级联选择：能耗场景大类（L1）选中后，二级分类下拉框自动加载该大类 `children`；二级选中后，三级分类下拉框自动加载其 `children`。
- 级联重置：修改 L1 时清空已选 L2/L3，修改 L2 时清空已选 L3。

---

## 17. 因子模版管理模块

因子模版管理模块提供碳排放因子模版的 CRUD、拷贝、引用计数等功能。Controller 文件为 `EmissionFactorTemplateController.java`，前缀 `/api/factor-templates`。

### 17.1 获取全部因子模版

| 属性         | 值                                      |
|:---------- |:-------------------------------------- |
| **URL**    | `/api/factor-templates`                |
| **HTTP方法** | GET                                    |

**响应体**：因子模版对象数组（含 id、templateName、templateDescription、isShared、status、createdBy、createdAt 等）

### 17.2 获取单个因子模版

| 属性         | 值                                      |
|:---------- |:-------------------------------------- |
| **URL**    | `/api/factor-templates/{id}`           |
| **HTTP方法** | GET                                    |

### 17.3 新建因子模版

| 属性         | 值                                      |
|:---------- |:-------------------------------------- |
| **URL**    | `/api/factor-templates`                |
| **HTTP方法** | POST                                   |

**请求体**：

| 字段                  | 类型      | 必填 | 说明                         |
|:------------------- |:------ |:--- |:-------------------------- |
| templateName         | String | 是   | 模版名称（唯一）                   |
| templateDescription | String | 否   | 模版说明                       |
| isShared             | Integer| 否   | 是否共享：1-共享（默认），0-私有         |
| createdBy            | Long   | 是   | 创建人ID                      |

### 17.4 更新因子模版

| 属性         | 值                                      |
|:---------- |:-------------------------------------- |
| **URL**    | `/api/factor-templates/{id}`           |
| **HTTP方法** | PUT                                    |

**请求体**：templateName、templateDescription、isShared、status、updatedBy

### 17.5 删除因子模版

| 属性         | 值                                      |
|:---------- |:-------------------------------------- |
| **URL**    | `/api/factor-templates/{id}`           |
| **HTTP方法** | DELETE                                 |

删除模版时级联清理该模版下全部缺省因子（sys_emission_default_factor）。

### 17.6 查询因子模版引用计数

| 属性         | 值                                      |
|:---------- |:-------------------------------------- |
| **URL**    | `/api/factor-templates/{id}/reference-count` |
| **HTTP方法** | GET                                    |

**查询参数**：

| 参数     | 类型   | 必填 | 说明                                        |
|:------ |:----- |:--- |:----------------------------------------- |
| userId | Long  | 否   | 传入时仅统计他人核算模版引用数（用于编辑锁定判断）；不传则统计全部引用数 |

**响应**：`{ "success": true, "count": <数量> }`

### 17.7 拷贝因子模版

| 属性         | 值                                      |
|:---------- |:-------------------------------------- |
| **URL**    | `/api/factor-templates/copy`           |
| **HTTP方法** | POST                                   |

**请求体**：

| 字段        | 类型     | 必填 | 说明                |
|:--------- |:------ |:--- |:----------------- |
| sourceId  | Long   | 是   | 源因子模版ID          |
| newName   | String | 是   | 新模版名称             |
| createdBy | Long   | 是   | 创建人ID             |

拷贝操作在事务中完成：复制模版记录 + 批量复制该模版下全部缺省因子。

---

## 18. 缺省因子管理模块

缺省因子管理模块提供模版级缺省碳排放因子的 CRUD。Controller 文件为 `DefaultFactorController.java`，前缀 `/api/default-factors`。

### 18.1 获取全部缺省因子（系统级）

| 属性         | 值                                      |
|:---------- |:-------------------------------------- |
| **URL**    | `/api/default-factors`                 |
| **HTTP方法** | GET                                    |

返回 `template_id IS NULL` 的系统缺省因子列表。

### 18.2 按模版ID获取缺省因子

| 属性         | 值                                      |
|:---------- |:-------------------------------------- |
| **URL**    | `/api/default-factors/by-template/{templateId}` |
| **HTTP方法** | GET                                    |

返回指定因子模版下的全部缺省因子。

### 18.3 新建缺省因子

| 属性         | 值                                      |
|:---------- |:-------------------------------------- |
| **URL**    | `/api/default-factors`                 |
| **HTTP方法** | POST                                   |

**请求体**：

| 字段               | 类型        | 必填 | 说明                                                     |
|:---------------- |:--------- |:--- |:------------------------------------------------------ |
| templateId       | Long      | 否   | 所属因子模版ID（NULL=系统缺省）                                  |
| subcategoryCode  | String    | 是   | 排放数据小类编码                                               |
| subcategoryName  | String    | 否   | 排放数据小类名称                                               |
| factorSource     | String    | 是   | 因子来源：ELECTRICITY/FOSSIL/THERMAL/WASTE_INCINERATION/WASTEWATER |
| factorId         | Long      | 否   | 所选因子ID                                                 |
| factorName       | String    | 否   | 因子名称（快照）                                               |
| factorValue      | BigDecimal| 否   | 因子值（快照）                                                |
| factorUnit       | String    | 否   | 因子单位（快照）                                               |
| factorDescription| String    | 否   | 因子说明（快照）                                               |
| createdBy        | Long      | 是   | 创建人ID                                                  |

校验：subcategoryCode + factorSource 非空且为合法值；(templateId, subcategoryCode) 联合唯一，重复返回 400。

### 18.4 更新缺省因子

| 属性         | 值                                      |
|:---------- |:-------------------------------------- |
| **URL**    | `/api/default-factors/{id}`            |
| **HTTP方法** | PUT                                    |

未传 factorSource 时保留原值。

### 18.5 删除缺省因子

| 属性         | 值                                      |
|:---------- |:-------------------------------------- |
| **URL**    | `/api/default-factors/{id}`            |
| **HTTP方法** | DELETE                                 |

### 18.6 按模版ID删除全部缺省因子

| 属性         | 值                                      |
|:---------- |:-------------------------------------- |
| **URL**    | `/api/default-factors/by-template/{templateId}` |
| **HTTP方法** | DELETE                                 |

删除指定因子模版下的全部缺省因子（删模版时级联调用）。

---

## 19. 核算模版属性更新（补充：factor_template_id）

### 19.1 更新模版属性

| 属性         | 值                                      |
|:---------- |:-------------------------------------- |
| **URL**    | `/api/template/{id}/properties`        |
| **HTTP方法** | PUT                                    |

**请求体**（在原有 description/enabled/templateType/taskConfig 基础上新增）：

| 字段               | 类型   | 必填 | 说明                                                    |
|:---------------- |:----- |:--- |:------------------------------------------------------ |
| factorTemplateId | Long  | 否   | 碳排放因子模版ID（NULL=清除关联，指向 sys_emission_factor_template.id） |

**说明**：仅核算模版（templateType=2）使用此字段。前端在编辑核算模版弹窗中通过"选择碳排放因子模版"按钮设置该值。
