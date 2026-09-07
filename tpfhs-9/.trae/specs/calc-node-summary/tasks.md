# 核算节点逐级汇总 — 实施任务清单

依赖基础：
- 规范：`spec.md`（本目录）
- 代码根：`c:\myfile\MyProg\SOLO\proj-1\tpfhs-9`
- 语言：后端 Java + JPA(Spring Data) / MyBatis 混用？当前以 JPA Repository 为主（CalculationTemplateRepository 等），新增 Repository 均遵循既有风格（Spring Data JPA + 可选 @Query 原生 SQL）。

---

## Task 1: emission_calc_node_summary 表 DDL + 增量脚本

**目标**：在 schema.sql 与 schema-update-20260903.sql 同步新建表，满足 AC-1。

**涉及文件**（写者独占，无并发风险）：
- `backend/src/main/resources/schema.sql`
- `backend/src/main/resources/schema-update-20260903.sql`

**工作内容**：
1. 在 schema.sql emission_collection_node_data 表定义之后（900 行附近 calc_unit_default 之前或之后，保持顺序合理），新增 emission_calc_node_summary 的 CREATE TABLE：
   - id BIGINT PK AUTO_INCREMENT
   - calculation_template_id BIGINT NOT NULL FK→emission_calculation_template(id)
   - calculation_node_id BIGINT NOT NULL FK→emission_calculation_node(id)
   - source_node_id BIGINT（冗余 emission_node 源 ID，便于按组织查询）
   - parent_calc_node_id BIGINT（父快照节点 id，便于向上遍历/回溯）
   - node_level INT NOT NULL
   - is_leaf_calc_node TINYINT DEFAULT 0
   - energy_category_l1 VARCHAR(50)
   - energy_category_l2 VARCHAR(50)
   - energy_category_l3 VARCHAR(50)
   - emission_category VARCHAR(100)（冗余大类：PE/PH/FF/EP/WT，来自 subcategory → parent_code）
   - emission_subcategory VARCHAR(100) NOT NULL
   - direct_energy_value DECIMAL(18,6) DEFAULT 0
   - direct_carbon_emission DECIMAL(18,6) DEFAULT 0
   - subtotal_energy_value DECIMAL(18,6) DEFAULT 0
   - subtotal_carbon_emission DECIMAL(18,6) DEFAULT 0
   - calculation_unit_code VARCHAR(30)（来自 emission_calc_unit_default.calculation_unit）
   - carbon_emission_factor DECIMAL(15,6)（本节点-该组使用的平均因子）
   - summary_source TINYINT
   - created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
   - UNIQUE KEY `uk_calc_node_group` (calculation_node_id, energy_category_l1, energy_category_l2, energy_category_l3, emission_subcategory)
   - INDEX `idx_calc_template` (calculation_template_id)
   - INDEX `idx_source_node_cycle` (source_node_id, calculation_template_id)
   - INDEX `idx_template_subcategory` (calculation_template_id, emission_subcategory)
   - ON DELETE CASCADE FK 指向 calculation_template
2. 在 schema-update-20260903.sql 末尾追加完全相同的 CREATE TABLE IF NOT EXISTS。
3. 脚本头部 SET SQL_SAFE_UPDATES=0 已存在，不需要重复写。
4. 为保证全新库和增量库一致，两个文件的 DDL 严格逐字相同（除顺序和 IF NOT EXISTS 外）。

**依赖**：无。

**优先级**：high。

**Test Requirements**：
- TR 1.1 (rule)：在一个空库执行 schema.sql 后，`SHOW CREATE TABLE emission_calc_node_summary` 输出能看到全部 5 个数值列（direct_energy_value/direct_carbon_emission/subtotal_energy_value/subtotal_carbon_emission/carbon_emission_factor）与 4 个索引+唯一键。
- TR 1.2 (rule)：在一个"已执行过旧版 schema.sql 的库"（假设已存在 emission_calc_unit_default 等所有旧表）执行 schema-update-20260903.sql 全部语句，不报错且不重复插入（CREATE TABLE IF NOT EXISTS + INSERT IGNORE 语义正确）。

---

## Task 2: 实体/Repository/Service + 单位换算辅助类

**目标**：落地新表的实体、Repository、查询 API 所需 Service，以及一个小工具"按 subcategory 把 measurement_unit 值换算到 calculation_unit"。

**涉及文件**：
- `backend/.../entity/CalcNodeSummary.java`（新）
- `backend/.../repository/CalcNodeSummaryRepository.java`（新）
- `backend/.../service/CalcNodeSummaryService.java`（新接口）
- `backend/.../service/impl/CalcNodeSummaryServiceImpl.java`（新实现）
- `backend/.../service/impl/UnitConversionService.java`（新，或已有类似实现若可复用则直接复用）
- `backend/.../entity/UnitConversion.java`（如已存在可直接读，否则新增）
- `backend/.../repository/UnitConversionRepository.java`（如有需要新增）
- `backend/.../repository/CalcUnitDefaultRepository.java`（复用现有表，若无则新增）

**工作内容**：
1. CalcNodeSummary 实体：所有字段用 @Column + @Table(name="emission_calc_node_summary")，遵循项目现有实体风格（@Data + jakarta.persistence.* + @PrePersist onCreate 设置 created_at 和默认值）。
2. CalcNodeSummaryRepository：Spring Data JPA。定义方法：
   - `deleteByCalculationTemplateId(Long templateId)`：幂等清理。
   - `findByCalculationTemplateId(Long templateId)`：FR-4 查询使用。
   - `findByCalculationNodeId(Long calcNodeId)`：单节点查询。
   - `findByCalculationNodeIdIn(Collection<Long> calcNodeIds)`：父节点拉取子节点汇总。
   - `findByCalculationTemplateIdAndSourceNodeId(Long templateId, Long sourceNodeId)`：按源节点查询。
   - 可选带 subcategory/场景过滤参数的方法（或在 Service 层 filter）。
3. CalcNodeSummaryService 接口 + 实现：
   - `queryByTemplate(Long templateId, CalcSummaryFilter filter)`：返回 List<CalcNodeSummaryVO>；filter 允许 nodeId、subcategory、l1/l2/l3 为 null。
   - `queryBySourceNodeAndCycle(Long sourceNodeId, LocalDate start, LocalDate end)`：先查 calculation_template，再 JOIN summary。
4. 单位换算辅助：
   - 定义 UnitConversionService.convert(...)：`BigDecimal convert(String subcategoryCode, String fromUnit, String toUnit, BigDecimal value)`。核心查 emission_unit_conversion `WHERE subcategory_code=? AND from_unit_code=? AND to_unit_code=?`。找不到 → 记录 WARN 并返回原值（系数 1）。
   - 若项目已有 emission_calc_unit_default Repository，复用 `findBySubcategoryCode(String)` 拿到 calculation_unit；否则新建。
5. 注：项目已有 emission_unit_conversion DDL，若无对应实体则需补 UnitConversion 实体与 Repository（与 FactorUnit/GhgUnit 的新建风格一致）。

**依赖**：Task 1 完成（否则编译验证无法跑）。

**优先级**：high。

**Test Requirements**：
- TR 2.1 (rule)：对 subcategory=PE_PF（calculation_unit=kWh），值 10，fromUnit=MWh → convert 后应为 10×1000=10000 kWh。
- TR 2.2 (rule)：单位换算找不到（如 subcategory=FF_X，fromUnit=xyz → toUnit=abc）时返回原值且 logger 有 WARN 级日志，不抛异常。
- TR 2.3 (rule)：deleteByCalculationTemplateId 执行后 summary 表对应 templateId 无行。

---

## Task 3: EmissionCalculationServiceImpl — 实现 Step4 逐级汇总

**目标**：在 executeEmissionCalculation 的 Step4 占位位置落地算法，覆盖 AC-2/AC-3/AC-4/AC-5/AC-6。

**涉及文件**：
- `backend/.../service/impl/EmissionCalculationServiceImpl.java`（读所有常量、修改 executeEmissionCalculation）
- （可选）若文件过大，抽一个独立 `CalcNodeSummaryProcessor.java` 组件，由 Service 注入调用（推荐）。

**工作内容**：
1. 在 executeEmissionCalculation 中，Step3 结束后（所有采集节点 data 已写入且无运行时异常分支外），新增 Step4 块：
   ```
   // Step 4：核算节点和根节点逐级汇总
   try {
       logger.info("[Step4] 开始逐级汇总核算节点，templateId={}", calcTemplate.getId());
       calcNodeSummaryProcessor.process(calcTemplate.getId());
       logger.info("[Step4] 逐级汇总完成，templateId={}", calcTemplate.getId());
   } catch (Exception e) {
       logger.error("[Step4] 失败，templateId=" + calcTemplate.getId(), e);
       // 走失败分支，calcTemplate.setStatus(3)，error_code 填具体码
       throw new RuntimeException(...);
   }
   ```
2. 新建 CalcNodeSummaryProcessor（@Service、@Profile("job")—— 因为仅在 Job 线程池内被调度调用），`process(Long calcTemplateId)` 实现：
   a. 查询本次 calculation_template_id 下所有 emission_calculation_node 快照节点（含 type_id、parent_id、node_id 源、name）。
   b. 构建节点树并计算 level：根节点 level=1；按 parent_calc_node_id 递归。
   c. 标记 is_leaf_calc_node：对每个 type=1/2 的节点，其直属子节点中**完全不含 type=1/2/4**（全部是 type=3 采集节点）→ 是 leaf。
   d. 对所有 type=1/2 的节点 **按 level 倒序**（最深先处理）。
   e. 对每个节点 `processOneNode(calcTemplateId, node)`：
      - e1. 删除旧 summary 行（deleteByCalculationNodeId）——保证可重入。
      - e2. 取本节点直属的 **采集节点（type=3）** 子节点集合 → 找出其 calc_node_id → 一次性查询其所有 emission_collection_node_data 行。
      - e3. 对每行做单位换算到 calculation_unit，得到 (L1,L2,L3,subcategory) → {energySum, carbonSum, factorWeightedSum}。
      - e4. 把结果写入 Map<Group, Accumulator>，direct_* 就是 energySum/carbonSum。
      - e5. 若非 leaf：找到直属子 calculation/root 节点的 summary 行（findByCalculationNodeIdIn），按相同 Group 累加。
      - e6. subtotal_* = direct_* + childSubtotal。
      - e7. 计算每个 Group 的 carbon_emission_factor = carbon / energy（若 energy=0，则因子=0，避免除零）。
      - e8. 反查 subcategory 对应 emission_category（查 emission_data_dict_item parent_code）并填列。
      - e9. 批量 INSERT / saveAll 到 emission_calc_node_summary。
      - e10. 关键日志：节点id+名称+level+leaf?+direct行数+子节点行数+耗时。
   f. 全部节点处理完，输出汇总日志：模板 id + 处理节点数 + summary 总行数 + 耗时。
3. 处理 transport（type=4）节点：按用户描述，根节点和核算节点算法相同；type=4 若存在且需要汇总，**同 type=2 走**，不单独分支，以确保"根节点同表"约束满足。type=4 的汇总口径沿用同一分组逻辑。
4. 保证幂等：整个 process() 入口先 `summaryRepo.deleteByCalculationTemplateId(calcTemplateId)`。
5. 常量定义（如果不存在则补）：
   - `TYPE_ID_ROOT = 1`（现有项目常量未定义，需从节点 type 表值对应：root/calculation/data_collection/transport 的 id 为 1/2/3/4）。
   - `TYPE_ID_TRANSPORT = 4`。

**依赖**：Task 1、Task 2。

**优先级**：high。

**Test Requirements**：
- TR 3.1 (rule)：模拟一个模板：根 R(1) → 子核算节点 N(2) → 两个采集点 A(3) 和 B(3)；A 与 B 同 subcategory=PE_PF 且 measurement_unit 分别为 kWh(adjusted=500) 和 MWh(adjusted=2)；执行后 N 节点对应组的 direct_energy_value = 500 + 2×1000 = 2500 kWh（因为 PE_PF calculation_unit=kWh）；R 节点 subtotal_energy_value 同值（R 无 direct 时直接 2500）。
- TR 3.2 (rule)：三级：R→P→N（均为 1/2 节点），N 下有采集点 A；N 汇总完成后再 P，最后 R；日志输出顺序 N→P→R。
- TR 3.3 (rule)：同一 templateId 调用两次 process()，summary 行数完全相同，无唯一键冲突。
- TR 3.4 (rule)：一个节点自身挂采集点（direct）且含子核算节点（subtotal 子部分），最终 subtotal = direct + sum(child subtotal)。
- TR 3.5 (rubric, 0-2, pass≥1)：日志充分。

---

## Task 4: 查询 Controller + 前端 API

**目标**：落地 FR-4 的两个端点，满足 AC-7；前端 API 封装（auth.js 内）。

**涉及文件**：
- `backend/.../controller/CalcSummaryController.java`（新）
- `frontend/src/api/auth.js`（追加 calcSummaryApi export）

**工作内容**：
1. CalcSummaryController：
   - `@RequestMapping("/api/calc-summary")`，`@CrossOrigin(origins="*")`，风格参考 FactorUnitController。
   - `GET /by-template/{templateId}`：支持可选 query `nodeId / subcategoryCode / l1 / l2 / l3`；返回 `CalcNodeSummaryVO` 列表（字段清单见 AC-7）。
   - `GET /by-node-and-cycle`：query `sourceNodeId、cycleStartDate、cycleEndDate`（ISO date）；先查 matching 的 calculation_template 列表（期间内 status=2 成功的），再反查 summary。
   - 可选新增 VO：`CalcNodeSummaryVO` 直接复制 Entity 字段或使用 Entity 自身返回（如果字段完全匹配则直接返回 Entity 即可，避免过度 DTO）。
2. 在前端 auth.js 末尾追加：
   ```javascript
   /**
    * 核算节点汇总数据 API
    */
   export const calcSummaryApi = {
     byTemplate(templateId, params = {}) {
       return axios.get(`${API_BASE_URL}/calc-summary/by-template/${templateId}`, { params });
     },
     byNodeAndCycle(params) {
       return axios.get(`${API_BASE_URL}/calc-summary/by-node-and-cycle`, { params });
     }
   };
   ```

**依赖**：Task 2（Service 定义必须存在）。

**优先级**：medium（高，因为是 API 交付，但不依赖 Task3 即可存在）。

**Test Requirements**：
- TR 4.1 (rule)：启动 Spring，调用 `/api/calc-summary/by-template/{templateId}` 返回结构中每条记录含 calculation_node_id、energy_category_l1~l3、emission_subcategory、direct_energy_value、direct_carbon_emission、subtotal_energy_value、subtotal_carbon_emission、calculation_unit_code。
- TR 4.2 (rule)：前端 auth.js 中 calcSummaryApi 通过 ESLint 语法检查，并且 import 不会破坏现有 exports（calcUnitDefaultApi/unitApi 仍可工作）。

---

## Task 5: 编译验证 + 关键 TR 手工/自动验证

**目标**：覆盖 AC-8 与所有 task 级 TR 证据。

**涉及文件**：无新文件，运行工具命令；若有编译错误则反修前序任务产出。

**工作内容**：
1. `cd backend ; mvn compile -DskipTests -q`：exit 0 记录为证据。
2. `cd frontend ; npm run build`：构建成功；chunk size warnings 非 fail。
3. 可选（若有 H2 内存测试或本地 MySQL 环境可用）运行单测验证 TR 2.1、TR 3.1。如果无法联库则至少做代码级审阅，在 Evidence 中注明原因。
4. 记录每一条 AC 的对应证据在 task completion evidence 列。

**依赖**：Task 1-4。

**优先级**：high。

**Test Requirements**：
- TR 5.1 (rule)：mvn compile 退出码为 0；无编译期错误（含实体字段缺失、类型不匹配）。
- TR 5.2 (rule)：npm run build 成功；无语法报错。
- TR 5.3 (rubric, 0-2, pass≥1)：代码可维护性满足。

---

## 任务依赖图

```
Task1 ──┐
        ├─→ Task2 ──┐
Task1 ──┘           ├─→ Task3 ──→ Task5
                    ├─→ Task4 ──┘
```

可并行：Task2 与 Task4 在 Task1 完成后可分别独立。Task3 需 Task1+2；Task5 需全完成。
