# 分公司对照基线（阶段 0）

现库 `energy_allocation` 字典码（不要写成 suning）：

| 字典码 | 名称 |
|---|---|
| suring | 肃宁分公司 |
| yuanping | 原平分公司 |
| jilong | 机辆分公司 |

`sys_dept` 仓库初始化脚本里没有这三家分公司。`V2__seed_subjects.sql` 会在缺少时插入占位部门（301/302/303），`subject_node_id` 先空着、`enabled=0`。

启用模板见 `V4__enable_subjects.sql`。上线前必须在真实库核对：

1. 实际 `sys_dept.dept_id`（若已存在同名部门，改对照表指向已有 ID，不要重复插入）。
2. `emission_node` + `emission_node_info` 中 `node_category='BRANCH'` 的节点 ID。
3. 把 `report_dept_subject.subject_node_id` 填上后把 `enabled` 改为 1。

金样 `ReportInput`：`fixtures/normalized_suning_2024.json`（来自引擎 fixture，用于版式回归）。
