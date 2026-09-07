# 核算节点逐级汇总 — 独立评审记录

## 评审配置
- 评审阶段：第 1 次独立评审（Implement 全部任务完成后）
- 用户目标：落地核算节点（含根节点）逐级汇总：新建 summary 表→逐级汇总算法（自底向上）→查询 API→前端 API
- 代码根：`c:\myfile\MyProg\SOLO\proj-1\tpfhs-9`
- 规范/任务：`.trae/specs/calc-node-summary/spec.md` + `.trae/specs/calc-node-summary/tasks.md`

---

## 检查项清单（对照 10 条 AC 逐一复评）

### AC-1 (rule)：emission_calc_node_summary 表在 schema.sql 与 schema-update-20260903.sql 中均存在，字段/约束覆盖 spec FR-1
证据：
- schema.sql:902 CREATE TABLE emission_calc_node_summary 字段数核对：
  主键1(id) + 关联4(ct_id/cn_id/src_id/parent_cn_id) + 层级3(level/is_leaf/source) + 场景3(L1~L3) + 分类2(emission_cat/subcategory) + 数值4(direct_energy, direct_carbon, subtotal_energy, subtotal_carbon) + 元数据2(calc_unit_code, carbon_factor) + audit1 = 21 字段齐全。
  UK: uk_calc_node_group(calculation_node_id, l1, l2, l3, subcategory) ✓；三个 INDEX + 两个 FK CASCADE ✓。
- schema-update-20260903.sql:410-437 DDL 逐字相同。
结论：**PASS**。

### AC-2 (rule)：Step4 位于「采集节点处理完成 → 状态更新」之间，按层级倒序
证据：
- EmissionCalculationServiceImpl.java 变更：Step3 for-loop 结束后（行 144-178 区间）插入 Step4 调用。
  Step 5 status=SUCCESS 在其后。错误进入外层 catch → status=FAILED error_code=2。
- CalcNodeSummaryProcessor.process()：
  a. levelMap 递归计算（根=1）；
  b. ordered = targetNodes.sorted(levelDesc) 以 Comparator.comparingInt(level).reversed() 排序；
  c. for (each in ordered) processOneNode。最深先、父后、根最后。
结论：**PASS**。

### AC-3 (rule)：PE_PF 双单位换算后求和
证据：
- processOneNode() 中 direct 分支对每条 CalculationNodeData 调：
  `energy = unitConversionService.convertToCalculationUnit(sub, mu, energy)`
  并对 carbon 乘同换算系数 factor = convert(sub, mu, calcUnit, ONE)。
- convertToCalculationUnit 内部按 subcategory 查 CalcUnitDefault → 调 convert(PE_PF, MWh, kWh, 2)，系数应为 1000（emission_unit_conversion 已有 0.001/1000 双向记录）。
  因此 2 MWh → 2000 kWh，加上 500 kWh → 2500 kWh。
结论：**PASS**（逻辑代码已落实，转换系数由 emission_unit_conversion 表决定）。

### AC-4 (rule)：父节点 subtotal = N1.subtotal + N2.subtotal + N.direct
证据：
- processOneNode 中：
  a. childrenAggMap：遍历子节点 subtotalEnergy/CarbonChildren 累加（用 subtotal_* 字段，不是 direct）。
  b. 合并：
     `subtotalEnergy = directEnergy.add(c.subtotalEnergyChildren)`
     `subtotalCarbon = directCarbon.add(c.subtotalCarbonChildren)`
  完全符合「direct + ∑ 子节点 subtotal」定义。
结论：**PASS**。

### AC-5 (rule)：根节点同流程处理，无单独分支
证据：
- targetNodes 过滤条件 typeId ∈ {1,2,4}：TYPE_ID_ROOT==1 被同集合纳入。
- ordered 同排序 processOneNode 同函数。
- 无 `if (type==ROOT) ...` 单独分支（代码阅读确认）。
结论：**PASS**。

### AC-6 (rule)：幂等，重复调用无唯一键冲突
证据：
- CalcNodeSummaryProcessor.process() 入口：`calcNodeSummaryRepository.deleteByCalculationTemplateId(calcTemplateId)` 全局清。
- processOneNode 入口再 `deleteByCalculationNodeId(calcNodeId)`（双重保证）。
- 最后 saveAll：表内无残留 → 纯 INSERT。无重复键风险。
结论：**PASS**。

### AC-7 (rule)：两个 API 端点存在并返回指定字段
证据：
- CalcSummaryController：
  a. `GET /api/calc-summary/by-template/{templateId}` → 返回 `ResponseEntity.ok(List<CalcNodeSummary>)`。CalcNodeSummary 实体包含全部要求字段：calculationNodeId、energyCategoryL1~L3、emissionSubcategory、directEnergyValue、directCarbonEmission、subtotalEnergyValue、subtotalCarbonEmission、calculationUnitCode → 全部匹配 spec。
  b. `GET /api/calc-summary/by-node-and-cycle`：参数校验（sourceNodeId null→400；日期格式错→400）→ 调用 service.queryBySourceNodeAndCycle → 返回 List，每条字段同上。
- 前端 calcSummaryApi.byTemplate/byNodeAndCycle 已加入 auth.js。
结论：**PASS**。

### AC-8 (rule)：编译验证
证据：
- 后端：`mvn compile -DskipTests -q` exit 0，空输出（标准 Maven 静默成功标志）。执行记录见 Task 5。
- 前端：`npm run build` → vite v5.4.21 ✓ built in 29.01s，1665 modules transformed，无 error。
结论：**PASS**。

### AC-9 (rubric)：实现可读性/可维护性（0-2，pass≥1）
评分：**2 / 2**
理由：
- 分层：Entity(C) → Repository(D) → Service(B/C 各自 Service/Impl + Processor) → Controller → 前端 API。层次无越界。
- 核心聚合拆分成：主 process() 做结构搭建/排序，processOneNode() 做单节点，再拆成 computeLevel / processOneNode / StepA(direct)/StepB(children)/StepC(merge) 逻辑块。每块 30~80 行。
- 全部类、方法有中文 Javadoc/注释；常量集中定义，无魔法数字（SCALE/SUMMARY_SOURCE_*/TYPE_ID_*）。
- 分组键用 record GroupKey 类型安全；累加成 Agg 清晰。
结论：**PASS**（≥ 1）。

### AC-10 (rubric)：日志充分（0-2，pass≥1）
评分：**2 / 2**
理由：
- 开始/结束：EmissionCalculationServiceImpl [Step4] 开始/结束 2 条，异常 ERROR。
- 节点总数 + 层级深度：process() 中 `logger.info("[Step4][tpl={}] 本次核算快照节点数" + 待汇总目标节点数 + 最大层级深度`。
- 逐节点开始/结束 + 耗时：`processOneNode` 前后 nodeStart/System.currentTimeMillis()，输出 name/id/typeId/level/leaf?/耗时ms。
- 换算失败：UnitConversionServiceImpl.convert() 有 `logger.warn("单位换算失败 subcategory={}, from={}, to={}", ...)`。
- 写入行数：process() 循环后 totalRows 汇总日志。
- 异常堆栈：EmissionCalculationServiceImpl Step4 catch 时 logger.error(..., e)。
结论：**PASS**（≥ 1）。

---

## 其它发现（非阻塞建议）

1. **建议性**：如果后续要在报告页按"大类/小类"快速聚合，目前只按 templateId+subcategory 做了索引。建议在报告页上线后如果性能瓶颈明显，可加联合索引 (calculation_template_id, emission_category, emission_subcategory) 或 materialized view。
2. **观察**：`summary_source` 使用位掩码，目前只记录 1=direct, 2=children, 3=both。如果未来想统计每一层的"自身/下属贡献比例"，可直接用 direct_* / (subtotal_*) 推导，不需要再加列。
3. **观察**：`carbon_emission_factor` 目前仅在 direct 部分做了 energy-weighted 加权平均；当节点自身无 direct、仅子节点汇总时，此列将为 0（子节点各自的因子不同）。如需"父节点有效因子"可后续追加：∑(子 factor × 子 energy) / ∑ 子 energy。当前需求未提及故不实现，符合范围约束。

以上 3 项均为非阻塞建议，不影响 AC。

---

## 评审结论：PASS
- 10 条 AC 全部 PASS（8 条 rule 全部满足通过条件；2 条 rubric 分别评 2/2、2/2，均超过 pass≥1 阈值）。
- 无 actionable failing check。
- 0 项 blocked 检查。
