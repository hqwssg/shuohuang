# 碳排放报告子模块：详细设计与集成说明

版本日期：2026-09-11  
适用宿主：若依 RuoYi 3.6.8（Spring Boot 3 / Java 17 / Vue 2）  
本包用途：把「碳排放报告填报 + Word/PDF 生成」并入现有主系统，不替换核算、大屏、GoView。

---

## 1. 模块定位

本子模块解决的是：**选定一份已成功核算任务 → 预填报告草稿 → 本报告内可改 → 校验 → 生成温室气体排放报告（Word / PDF）**。

它**读**主系统核算数据（`emission_*`），**不回写**核算结果和因子库。报告自己的数据全部落在 `report_*` 表。

旧的独立服务 `carbon_report_system`（FastAPI + Vue3）已下线，**不要再部署**。排版引擎改为 Java 进程内用 `ProcessBuilder` 调用本机 Python CLI，不再单独起渲染容器或 8095 端口。

---

## 2. 本包目录

解压后目录与主工程对应关系如下，可按路径直接拷贝。

```
碳排放报告模块-集成包/
├── 设计与集成说明.md          ← 本文件
├── ruoyi-modules/
│   └── ruoyi-carbon-report/   → 拷到主工程 ruoyi-modules/ruoyi-carbon-report
├── ruoyi-ui/src/
│   ├── api/carbon/report.js   → 拷到 ruoyi-ui/src/api/carbon/report.js
│   └── views/carbon/report/   → 拷到 ruoyi-ui/src/views/carbon/report/
├── sql/carbon-report/         → 在主库按顺序执行（见第 7 节）
├── carbon_report_agent/       → 部署到服务器某目录，例如 /home/ruoyi/renderer/carbon_report_agent
└── docker/web-main/
    └── requirements-cli.txt   → Python 依赖清单
```

不要拷贝：`target/`、`__pycache__/`、LibreOffice 安装包、本机 `uploadPath`、已生成的 Word/PDF。

不要覆盖：上一级的 `ruoyi-ui/src/views/carbon/report.vue`（那是旧的区间统计页）。新页面在 **`views/carbon/report/` 目录**。

---

## 3. 总体架构

```
浏览器 (Vue2 ruoyi-ui)
    │  /dev-api 或 nginx /prod-api
    ▼
ruoyi-admin (Spring Boot)
    ├── ruoyi-modules-carbon-report
    │     填报 / 预填 / 校验 / 快照 / 生成任务
    │     └── ProcessBuilder
    │           python -m carbon_report_agent.cli generate|convert-pdf
    │                 ├── python-docx 生成 Word
    │                 └── LibreOffice soffice 转 PDF
    ├── MySQL  report_* （本模块表）
    └── MySQL  emission_* （只读：核算任务、活动量、因子模板）
```

关键运行时约定：

| 环境变量 | 作用 |
|---|---|
| `CARBON_REPORT_PYTHON` | Python 可执行文件，默认 `python` |
| `CARBON_REPORT_RENDERER_ROOT` | **carbon_report_agent 的上一级目录**（会作为 `PYTHONPATH`） |
| `CARBON_REPORT_SOFFICE_PATH` | LibreOffice 可执行文件。Windows 用 `soffice.com`，Linux 常用 `/usr/bin/libreoffice` |
| `CARBON_REPORT_WORK_DIR` | 渲染临时目录，默认 `./report-work` |
| `CARBON_REPORT_ARTIFACT_DIR` | Word/PDF 落盘目录，默认 `./uploadPath/reports` |
| `CARBON_REPORT_RENDER_TIMEOUT` | 子进程超时秒数，默认 300 |

可选 LLM（第 6 章 AI 撰写，默认关闭）：

- `CARBON_REPORT_LLM_ENABLED=1`
- `CARBON_REPORT_LLM_API_KEY`
- `CARBON_REPORT_LLM_BASE_URL`（默认 DeepSeek）
- `CARBON_REPORT_LLM_MODEL`

Java 调用 Python 时额外注入：`PYTHONPATH`、`PYTHONIOENCODING=utf-8`、`CARBON_REPORT_JOB_ASSETS_DIR`、`MPLCONFIGDIR`。

---

## 4. 详细设计

### 4.1 权限与部门隔离

- 接口前缀：`/carbon/report`
- 权限字：`carbon:report:list|query|add|edit|remove|import|validate|generate|download` 以及 `carbon:report:profile:query|edit`
- 所有报告行带 `dept_id`。列表用 `@DataScope`。下载、读任务、读产物都会校验当前用户部门，伪造其他部门 `reportId` / `artifactId` 应返回 404。
- **无对照不能建报告。** 对照表 `report_dept_subject`：`dept_id` + 核算 BRANCH `subject_node_id` + 能源分摊码（`suring` / `yuanping` / `jilong`）+ `enabled`。

### 4.2 数据模型（`report_*`）

| 表 | 用途 |
|---|---|
| `report_dept_subject` | 登录部门 ↔ 核算主体节点 |
| `report_dept_profile` | 企业档案：法定名称、边界默认文案、编制人等。**预填进本报告，保存本报告不回写档案** |
| `report_task` | 报告任务。状态 `DRAFT` / `GENERATED`。软删 `deleted_flag` |
| `report_activity_fuel` / `_electricity` / `_heat` | 活动数据。保留 `source_value` 以便覆盖后仍可追溯核算原值 |
| `report_workload` | 运输工作量（手填；缺失时校验提示，不按 0 生成） |
| `report_equipment` | 主要耗能设备 |
| `report_monitoring_device` | 监测设备。精度字段**不要编造** |
| `report_file` | 上传图 + 生成产物元数据，文件在磁盘不在库 |
| `report_generation_job` | 生成作业。写入不可变 `input_snapshot_json` 与 `snapshot_sha256` |
| `report_emission_source` / `report_import_batch` | 预留，当前主路径不用 |

`report_task` 上若干 JSON 列保存本报告文案：`entity_profile_json`、`organization_boundary_json`、`activity_prose_json`、`prior_year_json` 等。

任务状态：

- 创建时 `DRAFT`
- 生成成功（含命中 Word 缓存）后 `GENERATED`，`current_step=review`，**不升 version**
- 列表 / 草稿接口若发现仍是 `DRAFT` 但已有成功产物，会回写为 `GENERATED`
- 列表和填报页都会带上最新 Word、PDF 文件 ID；Word 与 PDF 可能来自不同 job，后端会分别取最新成功记录再合并

### 4.3 填报流程

前端 6 步（`edit.vue`）：基础 → 边界 → 活动数据 → 设备 → 说明 → 校验生成。

1. **新建**：选年度（及可选月份）、选一条**成功**核算任务。创建时把对照上的 `subject_node_id`、核算模板 ID 写入任务。
2. **预填**：从核算结果带入燃料 / 电 / 热活动量、边界说明、计量点基础信息。自动字段只读展示来源；用户覆盖时保留 `source_value`。
3. **档案**：打开填报时用部门档案预填空项；用户在本报告里改的内容只存 `report_task` / 行表，**不回写** `report_dept_profile`。
4. **保存分段**：`PUT /tasks/{id}/sections/{section}`，带乐观锁 `version`。
5. **校验**：`POST /tasks/{id}/validate` **只返回问题列表，不写库**（避免和保存抢 version 导致 409）。生成按钮不拦截校验结果，允许带着告警生成。
6. **上年对比**：有上年核算或上年报告快照则自动带入；可手填；不填也能生成，第 6 章同比为空。
7. **已生成再进入**：若状态已是 `GENERATED` 或草稿里已有产物，填报页直接落到「校验生成」，并请求 `GET /tasks/{id}/generation-jobs/latest` 显示下载，无需重新生成。修改填报内容后必须重新生成才会更新文件。

### 4.4 因子解析（生成用，不改核算）

Java 快照里的活动量来自报告行表，**排放量由 Python 按快照中的因子现算**，不直接采用核算表里的 `carbon_emission`。

解析顺序（`ReportFactorResolver`）：

1. 任务上的核算模板 `emission_calculation_template`
2. 其关联的 `emission_template.factor_template_id`
3. `emission_default_factor` 各槽位

槽位：天然气 `FF_NG`、汽油 `FF_G`、柴油 `FF_D`、电力 `EL`、热力 `PH_HD`（以及预留 `PE_PF`）。

化石燃料若 `factor_source=FOSSIL`：从 `emission_fossil_fuel_emission_factor` 取低位发热量、单位热值含碳量、氧化率，按  
`含碳量 = LHV × CC / 1000`，再 `× 氧化率 × 44/12`。  
电力 / 热力用模板快照 `factor_value`（电的 kgCO2/kWh 数值等于 tCO2/MWh）。

若缺项：

- `GET /tasks/{id}/factor-plan` 返回缺失槽位
- 前端确认「使用系统默认因子」后，生成请求带 `useDefaultFactors=true`
- 未确认则 400，**不会静默用引擎默认值**

系统默认仅作兜底（电 0.5366、热 0.11 等，见 `ReportFactorDefaults`）。完整因子写入快照 `factors`，Python `FactorLibrary.from_snapshot`；若快照没有 `factors`（夹具/旧数据）才回退 `default_2024`。

**精度陷阱：** 部分库的 `emission_default_factor.factor_value` 曾是 `DECIMAL(38,2)`，电因子 0.5306 会被存成 0.53。集成时建议改为 `DECIMAL(18,6)`（见 `V10` 中的 `ALTER`，可单独执行，不必整份 V10）。

### 4.5 生成与缓存

`POST /tasks/{id}/generation-jobs`，body：`outputKind=docx|pdf`，`aiEnabled`，`useDefaultFactors`。

流程：

1. 组装不可变 JSON 快照（活动量、文案、图片槽、因子）
2. 计算 `snapshot_sha256`
3. 同部门同哈希已有 Word 则复用，不再跑 Python / LLM
4. 否则 `python -m carbon_report_agent.cli generate --input ... --output ... --llm-enabled true|false`
5. 若要 PDF：`convert-pdf`，用已有 Word + LibreOffice，**不会为转 PDF 再次调用 LLM**
6. 产物写入 `CARBON_REPORT_ARTIFACT_DIR`，`report_file` + `report_generation_job` 记 ID
7. `report_task.status = GENERATED`

前端轮询 `GET /tasks/{id}/generation-jobs/{jobId}`；下载走 `GET /artifacts/{id}/download`（blob + token）。

三张静物图默认来自 `carbon_report_agent/assets/suning/`（封面 logo、组织机构图、办公地点）。用户在设备步上传后，生成时拷到 job-assets，快照 URL 改为 `/job-assets/...`。

### 4.6 Python 引擎要点

入口：`carbon_report_agent/cli.py`。

- `generate`：JSON → Word
- `convert-pdf`：Word → PDF（依赖 LibreOffice）
- 计算：`calculations.py` + `factors.py`
- 版式：`docx_pdf.py`、`publisher.py`、`style_pack.py`、样例风格包 `data/style_pack_suning_ghg_v1.json`

Python 依赖见 `docker/web-main/requirements-cli.txt`：`pydantic`、`python-docx`、`Pillow`、`matplotlib`、`jinja2`、`httpx`。

### 4.7 前端页面

| 文件 | 作用 |
|---|---|
| `views/carbon/report/index.vue` | 列表，状态「草稿 / 已生成」，可下 Word/PDF |
| `create.vue` | 新建并选择核算任务 |
| `edit.vue` + `steps/*` | 六步填报 |
| `query.vue` | 核算结果查询（只读） |
| `api/carbon/report.js` | 全部接口 |

路由（挂在碳排放 Layout 下）：

- `/carbon/report` → 列表
- `/carbon/report/create`
- `/carbon/report/edit/:id`
- `/carbon/report/query`

若依页签 `keep-alive` 会缓存填报页。已生成任务再次点「填报」时应落到校验步；部署后若仍看到旧页面，关掉「填报碳排放报告」页签或 Ctrl+F5。

---

## 5. 集成步骤（建议按此顺序）

### 5.1 拷贝源码

1. 整个 `ruoyi-modules/ruoyi-carbon-report` 放到主工程同路径（不要带 `target`）。
2. 拷贝前端 `api/carbon/report.js` 与 `views/carbon/report/`。
3. 把 `carbon_report_agent` 放到服务器固定目录，例如与 jar 同机的 `/home/ruoyi/renderer/carbon_report_agent`。  
   此时 `CARBON_REPORT_RENDERER_ROOT=/home/ruoyi/renderer`（注意是上一级）。

### 5.2 改主工程挂钩（不要整文件覆盖）

**`ruoyi-modules/pom.xml` 增加模块：**

```xml
<module>ruoyi-carbon-report</module>
```

**`ruoyi-admin/pom.xml` 增加依赖：**

```xml
<dependency>
    <groupId>com.ruoyi</groupId>
    <artifactId>ruoyi-modules-carbon-report</artifactId>
    <version>${ruoyi.version}</version>
</dependency>
```

**启动类 `RuoYiAdminApplication` 的 `@ComponentScan` 增加：**

```java
"com.ruoyi.carbon.report"
```

**`application.yml`：**

1. `mybatis.typeAliasesPackage` 追加 `com.ruoyi.carbon.report.domain`  
   （`mapperLocations: classpath*:mapper/**/*.xml` 一般已能扫到本模块 XML，不必改。）
2. 增加渲染配置：

```yaml
carbon:
  report:
    renderer:
      python-executable: ${CARBON_REPORT_PYTHON:python}
      module-root: ${CARBON_REPORT_RENDERER_ROOT:}
      work-dir: ${CARBON_REPORT_WORK_DIR:./report-work}
      artifact-dir: ${CARBON_REPORT_ARTIFACT_DIR:./uploadPath/reports}
      soffice-path: ${CARBON_REPORT_SOFFICE_PATH:}
      timeout-seconds: ${CARBON_REPORT_RENDER_TIMEOUT:300}
```

**`ruoyi-ui/src/router/index.js`：** 在碳排放子路由中，把原来的 `views/carbon/report`（单文件）换成第 4.7 节四条路由。`name` 建议用 `CarbonReport` / `CarbonReportCreate` / `CarbonReportEdit` / `CarbonReportQuery`，以便 keep-alive 区分。

**`ruoyi-ui/src/utils/carbonAccess.js`：** 报告卡片权限改为：

```javascript
permissions: ['carbon:report:list', 'carbon:report:query', 'carbon:report:add']
```

**`ruoyi-ui/package.json`：** 确认已有 `"file-saver": "2.0.5"`（若依通常已有）。没有则安装后再打包前端。

### 5.3 数据库

在**主库**执行，不要用 Flyway 强制版本号；脚本均做成幂等 `CREATE TABLE IF NOT EXISTS` / `WHERE NOT EXISTS`。

**必须执行：**

1. `sql/carbon-report/V1__report_tables.sql` — 建 `report_*` 表，不改 `emission_*` 结构（除你另行决定的 factor_value 精度）
2. `sql/carbon-report/V3__menus.sql` — 菜单与 admin 角色授权  
   - 父菜单 ID 写死为 `2000`（碳排放目录）。若主系统该目录 ID 不同，先改脚本再执行。  
   - 菜单 ID `2070–2080` 若冲突，改成空号。

**按环境填写后执行：**

3. `V2__seed_subjects.sql` — 部门对照模板。里面的 `dept_id` 301/302/303 是示例，**必须改成主系统真实部门 ID**，或删掉插入部门的语句、只插 `report_dept_subject`。
4. `V4__enable_subjects.sql` — 先查 `emission_node` 中 `node_category=BRANCH` 的真实 ID，填进 `subject_node_id` 再 `enabled='1'`。未启用的部门无法新建报告。

**不要在生产直接执行：**

| 脚本 | 原因 |
|---|---|
| `V5__local_debug_calculation_seed.sql` | 本机调试核算种子 |
| `V6`～`V9` | 肃宁档案样例文案/设备，按分公司另填 |
| `V10__suning_factor_template.sql` | 把因子模板挂到核算模板 **id=501**，只适用于当时库 |

`V10` 中下面这句建议**单独**在主库执行一次（若列已是 6 位小数可跳过）：

```sql
ALTER TABLE emission_default_factor
  MODIFY COLUMN factor_value DECIMAL(18,6) NULL COMMENT '所选因子值（快照）';
```

因子模板应在主系统「因子管理」里为各核算模板配置天然气/汽油/柴油/电力/热力；缺项时生成会弹窗询问是否用系统默认。

### 5.4 运行环境

- JDK 17、Maven、Node（前端与主工程一致）
- Python 3.10+，并安装 `requirements-cli.txt`
- LibreOffice Writer（无界面服务器需装完整 writer + 中文字体，例如 `fonts-noto-cjk`）
- 生成目录对运行用户可写：`report-work`、`uploadPath/reports`

验证 Python 能找到包（在 `CARBON_REPORT_RENDERER_ROOT` 下）：

```bash
export PYTHONPATH=/home/ruoyi/renderer
python -m carbon_report_agent.cli --help
```

Windows 开发示例：

```
CARBON_REPORT_PYTHON=python
CARBON_REPORT_RENDERER_ROOT=D:\path\containing\carbon_report_agent
CARBON_REPORT_SOFFICE_PATH=C:\Program Files\LibreOffice\program\soffice.com
```

### 5.5 编译与启动

```bash
# 先安装模块，再启动 admin（改 Java 后必须再 install，否则 ruoyi-admin 仍用旧 jar）
mvn -pl ruoyi-modules/ruoyi-carbon-report -am install -DskipTests
cd ruoyi-admin && mvn spring-boot:run
```

前端按主工程方式 `npm run dev` / 打包。菜单执行完后，用超级管理员重新登录（或清 Redis 权限缓存）才能看到新权限。

若走 Docker：把 `carbon_report_agent` 打进与 jar 同一镜像，设置第 3 节环境变量；不要再起独立 renderer 容器。参考主工程 `shuohuang-main/docker/web-main/dockerfile` 与 `copy.sh` 的 renderer 段。

---

## 6. 验收清单

1. 一次登录主系统，侧栏出现「碳排放报告」（或碳排放首页卡片能进列表）。
2. 未对照 / 未启用部门：新建应失败并提示。
3. 对照启用后：新建只能看到该主体下**成功**核算任务。
4. 预填后活动页有来源标签；覆盖一行不丢 `source_value`。
5. 保存本报告文案后，部门档案原文不变。
6. 无工作量时校验提示缺失，不按 0 出强度。
7. 不配因子模板时，生成前弹出「是否使用系统默认」；取消则不生成。
8. 生成 Word 后，同快照再点 PDF，不二次调用 LLM。
9. 生成成功后列表为「已生成」，有 Word/PDF；再点填报能直接下载，不必重生成。
10. 用其他部门 token 访问本部门 `reportId` / 文件 ID，返回 404。
11. 软删后列表消失，填报入口失效。

---

## 7. 与主系统的边界（集成时不要做的事）

- 不要给全套 `emission_*` 表加 `dept_id`，不要改核算写路径。
- 不要把报告排放结果写回核算任务。
- 不要引入 Flyway 抢主库版本。
- 不要部署已退役的 `carbon_report_system`。
- 不要把本包的肃宁种子（V5–V10）当生产数据。
- 监测设备精度、因子数值：只用库里已有数据或用户确认的默认值，不要手编。

---

## 8. 常见问题

**菜单有了但页面 404 / 仍是旧统计页**  
路由未改，或仍指向 `views/carbon/report.vue`。应指向 `views/carbon/report/index`。

**接口 401/403**  
权限缓存；`V3` 只给 `role_key='admin'` 授权，其他角色要在「角色管理」勾选。

**生成失败：No module named carbon_report_agent**  
`CARBON_REPORT_RENDERER_ROOT` 指错。必须是包含 `carbon_report_agent` **文件夹**的那一层。

**生成失败：LibreOffice / soffice**  
路径要用 `.com`（Windows）或发行版真实二进制；无头服务器还要装字体，否则 PDF 中文乱码或转译失败。

**电因子变成 0.53**  
`factor_value` 小数位不够，见 5.3 的 `ALTER`。

**改了 Java 但行为没变**  
`ruoyi-admin` 依赖的是 install 进本地仓库的模块 jar。先停 8081，再 `mvn ... install`，再启动。

**Maven install 卡住**  
通常是正在跑的 Spring Boot 锁着 jar。先停后端再 install。

---

## 9. 联系与范围

本包覆盖：报告填报、核算预填、企业档案（报告侧）、因子快照、Word/PDF 生成与下载回显。

不覆盖：核算算法、计量表、GoView 大屏、审批流、历史 UUID 旧报告迁移。
