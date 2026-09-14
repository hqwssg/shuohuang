# 阶段 0 基线确认记录

日期：2026-09-08

## 金样

- 引擎 `tests/fixtures/normalized_suning_2024.json` 已复制到
  `shuohuang-main/sql/carbon-report/fixtures/normalized_suning_2024.json`。
- 用途：Renderer 版式回归（封面、目录、页眉），允许编制日期差。

## 分公司对照（禁止按名称猜已有 dept_id / node_id）

现库 `energy_allocation` 字典码：

| 字典码 | 部门名称 | 种子 dept_id（仅当库中无同名部门时插入） | subject_node_id | enabled |
|---|---|---|---|---|
| suring | 肃宁分公司 | 301 | 待对库填写 BRANCH 节点 | 0 |
| yuanping | 原平分公司 | 302 | 待对库填写 BRANCH 节点 | 0 |
| jilong | 机辆分公司 | 303 | 待对库填写 BRANCH 节点 | 0 |

`V2__seed_subjects.sql` 用 `WHERE NOT EXISTS` 避免覆盖已有同名 `sys_dept`。
上线前执行 `V4__enable_subjects.sql` 模板：填入真实 BRANCH `emission_node.id` 后再 `enabled=1`。

## 确认项

- [x] 不把 `dept_id` 回填进 `emission_*`
- [x] 金样 JSON 入库
- [x] 对照三元组字段固定为 dept_id + BRANCH node + energy_allocation
- [ ] 生产库填写真实 `subject_node_id` 后启用（灰度阶段 6）
