# 核算节点（含根节点）能耗统计与逐级汇总 — 需求规范

## 1. 问题背景

当前系统已完成 `emission_collection_node_data` 表对 **采集节点（type_id=3）** 的核算结果存储，包含每条采集点的能耗三级场景（energy_category_l1/l2/l3）和排放数据小类（emission_subcategory）字段。核算流程 EmissionCalculationServiceImpl.executeEmissionCalculation 共 5 阶段，Step4（"对核算节点和根节点逐级汇总"）目前为占位符，需要落地实现。

一个核算节点通常下属多个采集节点，同时上级核算节点可能再下属多个子核算节点。用户希望通过数据库表 **直观、可追溯** 地把每个核算节点、根节点在 **每个能耗三级场景 × 每个排放小类** 维度下的能耗总量和碳排放组织起来，并且支持自底向上（最底层核算节点→更高级→根节点）的逐级汇总。

## 2. 用户 / 目标

- **用户**：碳排放核算系统运行时的自动调度器 + 报告查看人。
- **目标**：
  1. 把采集节点核算出的明细数据，按"核算节点 × 场景三级 × 排放小类"维度 **聚合到核算节点统计行**，保留 direct_* 4 个字段用于"本节点自身直接消耗 vs 下属节点汇总"的区分。
  2. 自底向上逐级汇总：先处理所有直接挂靠采集节点的 **最底层核算节点**（type=calculation 且子节点中无 calculation/root），再逐层向上处理其父核算节点，直到根节点（type=root，作为特殊核算节点）。
  3. 统计口径与 `emission_calc_unit_default` + `emission_unit_conversion` 完全对齐，保证所有值先换算到各 emission_subcategory 的 `calculation_unit` 再求和。
  4. 结果可通过 API 供前端按节点/按场景查询。
- **非目标**：
  - 不修改现有采集节点的能耗计量值计算算法（EnergyMeasurementCalculator 不纳入本规范）。
  - 不改动 emission_factor、排放数据字典等已有配置表。
  - 不做跨核算周期的历史趋势图前端页面实现（仅提供 API 和字段）。

## 3. 功能需求

### FR-1 新建核算节点统计数据表
新建一张 `emission_calc_node_summary` 表，一行对应"一个核算任务（calculation_template_id）下的一个核算节点（calculation_node_id）× 一个能耗场景（L1+L2+L3）× 一个排放数据小类（subcategory_code）"的统计结果，字段如下：
- **主键与关联**：id、calculation_template_id、calculation_node_id（对应 emission_calculation_node 快照）、source_node_id（对应 emission_node 源节点，便于展示层按组织架构聚合查询）。
- **分类维度**：energy_category_l1 / l2 / l3（三级场景编码）、emission_category（大类，冗余便于快速筛选）、emission_subcategory（小类，主键维度之一）。
- **数值字段（均换算为 subcategory 的 calculation_unit，保证单位一致后可累加）**：
  - `direct_energy_value DECIMAL(18,6)`：本节点 **自身直接消耗** 的能耗（核算节点自己挂载的采集点贡献，单位 calculation_unit）
  - `direct_carbon_emission DECIMAL(18,6)`：本节点自身直接消耗的碳排放
  - `subtotal_energy_value DECIMAL(18,6)`：直属子核算节点（子 calculation/root）的汇总 + 本节点 direct，值 = direct_energy_value + SUM(子节点本行)
  - `subtotal_carbon_emission DECIMAL(18,6)`：直属子核算节点汇总 + 本节点 direct，值 = direct_carbon_emission + SUM(子节点本行)
- **单位信息**：calculation_unit_code（取 emission_calc_unit_default.calculation_unit，冗余记录以便核对）、carbon_emission_factor DECIMAL(15,6)（按 emission_subcategory × 核算节点配置取对应因子，可存每条行使用的加权平均因子）。
- **层级辅助**：node_level INT（本节点在本次任务树中的层级，根节点 level=1，便于逐层推进）、parent_calc_node_id BIGINT（父快照节点 id，便于向上回溯）、is_leaf_calc_node TINYINT（是否最底层核算节点：子节点全为采集节点）。
- **统计来源**：summary_source TINYINT 1=聚合采集节点, 2=汇总子节点, 3=两者都有。
- **审计**：created_at。
- **约束**：`UNIQUE(calculation_node_id, energy_category_l1, energy_category_l2, energy_category_l3, emission_subcategory)`。
- **索引**：`idx_calc_template`(calculation_template_id)、`idx_source_node`(source_node_id, calculation_template_id)、`idx_subcategory`(calculation_template_id, emission_subcategory)。

> 关于 direct_*：核算节点本身可能自己就挂了采集点（例如一个"分公司"节点同时又挂了若干采集点），这部分是"本节点直接消耗"，和"下属节点汇总"分别存放在 direct_* 与 subtotal_* 中；`subtotal = direct + Σ(子节点 subtotal)`，与 emission_calc_unit_default 约定的 calculation_unit 对齐后累加，保证任意节点 subtotal 都是该子树下全量。

### FR-2 执行顺序：自底向上推进核算节点和根节点
在 `EmissionCalculationServiceImpl.executeEmissionCalculation` 的 **Step4** 位置实现：
1. **所有采集节点（type=3）核算结果落地后**，查询本次 calculation_template_id 下所有 emission_calculation_node 的类型（type_id）与父子关系。
2. 为每个 calculation/root 型节点在内存中建立层级深度（level）。
3. 按 **层级倒序**（从最深叶子核算节点 → 父节点 → 根节点）遍历：
   - **Leaf 核算节点（is_leaf_calc_node=true）**：
     a. 统计本节点 direct 贡献：找出其直属采集节点（type=3）的 snapshot 节点，在 emission_collection_node_data 中拉取行，并做单位换算：
        - 以 emission_subcategory 为键查表 emission_calc_unit_default 得目标 calculation_unit；
        - 如果行 measurement_unit 与目标不同，在 emission_unit_conversion 中按 subcategory_code+from_unit+to_unit 取系数换算；若找不到转换系数（理论上不会发生，因为标准单位脚本已补齐），记录 WARN 日志并按系数 1 处理（避免崩溃）。
     b. 按 `(L1, L2, L3, emission_subcategory)` 分组，**SUM(换算后的 adjusted_energy_value / adjusted_carbon_emission)**，分别累加到对应组的 direct_energy_value / direct_carbon_emission。
     c. 每组 subtotal_* 先等于 direct_*（因为是叶子），summary_source=1 或 3。
     d. 批量 INSERT IGNORE（或先 DELETE + INSERT）`emission_calc_node_summary`。
   - **非 Leaf 核算节点 / 根节点（is_leaf_calc_node=false）**：
     a. direct 贡献：同上面 a.b.c 步骤（如果自身还挂了采集点）。
     b. 子节点汇总贡献：对每个子 calculation/root 节点，查询 `emission_calc_node_summary` 中对应子节点的所有 (L1,L2,L3,subcategory) 组，按同一 (L1,L2,L3,subcategory) 相加（单位已统一，可直接相加）。
     c. 合并：`subtotal_energy_value = direct_energy_value + sum(child.subtotal_energy_value)`；carbon 同理。
     d. summary_source 置对应位（有 direct 就置1 位，有子节点置2 位）。
     e. 批量写入 emission_calc_node_summary。
4. 所有节点完成后，更新 calculation_template 状态 status=2 成功；任何异常捕获后 status=3。

### FR-3 根节点处理
根节点（type_id=1）视作特殊核算节点，算法与核算节点完全一致（复用同一方法 processCalcNodeSummary），不做单独分支。根节点的 is_leaf_calc_node 几乎永远为 false。

### FR-4 提供查询 API
新增 Controller 暴露至少以下两个端点：
- `GET /api/calc-summary/by-template/{templateId}?nodeId=&subcategoryCode=&l1=&l2=&l3=`：按核算任务 + 可选节点/小类/场景查询汇总数据，返回每个 summary 行（含 direct_*、subtotal_*、calculation_unit_code、factor）。
- `GET /api/calc-summary/by-node-and-cycle?sourceNodeId=&cycleStartDate=&cycleEndDate=`：按组织架构节点源 ID + 核算周期范围查询所有任务结果。
两个端点都通过 Repository/Service 查询 emission_calc_node_summary + 关联 emission_calculation_node 名称。

### FR-5 与采集节点已有数据的兼容性
- emission_collection_node_data 存的 `adjusted_energy_value` / `adjusted_carbon_emission` 必须被 Step4 读取使用（而非原始的 energy_measurement_value），以保证缺失处理/插值后口径一致。
- emission_collection_node_data 中 energy_category_l1/l2/l3 某一级可能为 NULL 或空；聚合分组时把 NULL 视为空串（""），确保分组键稳定不丢失。
- 保存 summary 前，把 emission_category（如 PE/PH/FF/WT）通过 emission_subcategory → emission_data_dict_item parent_code 反查并冗余存储。

### FR-6 增量脚本
在 `schema-update-20260903.sql` 末尾新增：
1. emission_calc_node_summary 的 CREATE TABLE + 索引 + 唯一约束；
2. （可选）把现有所有旧 calculation_template 的 status=2 记录标记为 summary_missing 备注字段，便于后续回溯。

## 4. 非功能需求

- **性能**：单核算任务中若采集节点 < 5万、核算节点 < 1000，整段 Step4 执行时间 ≤ 30s（未引入新 IO 时）；通过一次拉取所有 collection_node_data 做内存分组，避免 N+1 查询。
- **可重入**：同一 calculation_template_id 重复执行 Step4 时，先 `DELETE FROM emission_calc_node_summary WHERE calculation_template_id=?` 再重新写入，保证结果一致。
- **单位安全**：所有累加前必须先转换到 calculation_unit，不允许不同 measurement_unit 直接数值相加；用单元测试覆盖至少 3 种 subcategory 的换算场景。
- **可观测**：关键日志点包括"开始 Step4"、"节点总数/层级深度统计"、"每个节点开始处理/结束耗时"、"换算失败的 subcategory+unit"、"写入行数"、"Step4 结束"。

## 5. 约束与依赖

1. **用户确认项**（来自之前咨询的 4 点答复，必须严格遵守）：
   - direct_* 4 字段（direct_energy_value, direct_carbon_emission 及对应 subtotal 两列）保留，不合并。
   - emission_subcategory 来源于数据字典且前端仅下拉，不需额外标准化脚本。
   - 根节点（type_id=1）与核算节点（type_id=2）同表 emission_calc_node_summary，不拆分；靠 type_id 字段识别。
   - measurement_unit 多样性已解决（emission_unit_standard + emission_unit_conversion），不再重复建设。
2. **硬约束**（来自项目内存）：
   - emission_collection_node_data 仅存采集节点（type_id=3）结果，汇总结果必须另建表，不能覆写。
   - Carbon emission 先完成所有 type=3 采集节点，再处理 type=2 和 type=1。
   - emission_calc_unit_default 的 calculation_unit 是汇总的唯一标准单位，任何输入 measurement_unit 必须转到此单位后相加。
   - schema-update 脚本（而非新表）作为增量交付；schema.sql 同步写 DDL。
   - Job 相关 Bean 使用 `@Profile("job")`，若新增线程池/异步任务，需加注解。

## 6. 假设

1. 采集节点的 adjusted_* 值是最终口径值（经 EnergyMeasurementCalculator 做数据完整性修复和插值后），Step4 不再二次修正。
2. emission_calc_unit_default 覆盖所有使用到的 emission_subcategory，查询不到时按 measurement_unit 原单位直接累加，日志 WARN。
3. emission_node_config 中碳因子按节点唯一，若同一 subcategory 在同一节点下出现多个因子，summary 中加权平均（按能耗量）存入 carbon_emission_factor 列。
4. 根节点若为空壳（既不挂采集点又无下属核算节点），summary 无行（不写空记录）。

## 7. 开放问题

无。用户已明确 4 个确认点。

## 8. 验收标准（Acceptance Criteria）

- **AC-1 (rule)**：schema.sql 和 schema-update-20260903.sql 中存在 `emission_calc_node_summary` 表，其字段、唯一约束、索引完全覆盖 FR-1 所列全部字段与约束；其中 direct_energy_value / direct_carbon_emission / subtotal_energy_value / subtotal_carbon_emission 四列同时存在且为 DECIMAL(18,6)。
- **AC-2 (rule)**：EmissionCalculationServiceImpl.executeEmissionCalculation 的 Step4 位置在"所有采集节点处理完毕 → 状态更新"之间，实现了按层级倒序处理 calculation/root 节点；处理顺序可通过日志或单元测试观察到：先叶子核算节点、再父级、最后根节点。
- **AC-3 (rule)**：当一个最底层核算节点下有 2 个采集点，分属相同 (L1=辅助生产, L2=运输组织, L3=场站系统, subcategory=PE_PF)，两个采集点的 measurement_unit 分别为 kWh 与 MWh，聚合后该组的 direct_energy_value 全部换算到 kWh（PE_PF 的 calculation_unit）并求和；MWh 的值按 emission_unit_conversion 系数 ×1000 换算。
- **AC-4 (rule)**：父级核算节点 N 有两个子核算节点 N1 和 N2，N1/N2 均已聚合完成；N 的同 (L1,L2,L3,subcategory) 组 subtotal_* 数值等于 N1 的 subtotal + N2 的 subtotal + N 自身 direct（如果有）。
- **AC-5 (rule)**：根节点在同一路径 (L1,L2,L3,subcategory) 下的 subtotal_* 值等于其所有子核算节点 subtotal_* 累加再加上根节点自身 direct；根节点与子节点走同一处理流程（同一函数/同一写入逻辑），无单独分支硬编码。
- **AC-6 (rule)**：重复调用 executeEmissionCalculation（同一 templateId）时 summary 结果稳定、不出现重复键冲突；要么幂等 DELETE+INSERT，要么 ON DUPLICATE KEY UPDATE。
- **AC-7 (rule)**：存在 `GET /api/calc-summary/by-template/{templateId}` 与 `GET /api/calc-summary/by-node-and-cycle` 两个端点；对一个已完成核算任务，返回的 JSON 中每条 summary 行包含：calculation_node_id、energy_category_l1/l2/l3、emission_subcategory、direct_energy_value、direct_carbon_emission、subtotal_energy_value、subtotal_carbon_emission、calculation_unit_code。
- **AC-8 (rule)**：后端 `mvn compile -DskipTests -q` 退出码为 0；前端若有调用新 API 的代码，`npm run build` 成功无错误。
- **AC-9 (rubric)**：实现可读性与可维护性（0-2，pass≥1）：
  - `2`：分层清晰（Service+Repository+Entity）、关键步骤有中文注释、未把数据库写入与聚合逻辑混在同一超大循环、不存在魔法数字符串；
  - `1`：分层基本合理但有轻度耦合（例如 Service 直接写大量原生 SQL 且没有抽方法）；
  - `0`：单函数 >200 行、多职责，无注释。
- **AC-10 (rubric)**：日志充分性（0-2，pass≥1）：
  - `2`：含全部关键日志点（开始/结束、节点数+层级深度、逐节点开始/结束耗时、换算失败、写入行数、异常堆栈）；
  - `1`：含开始/结束和写入行数，日志信息够用；
  - `0`：日志缺失或仅有 logger.error 无上下文。
