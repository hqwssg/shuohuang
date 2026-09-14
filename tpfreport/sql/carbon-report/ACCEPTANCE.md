# 阶段 6 灰度验收清单

1. 执行 `V1__report_tables.sql`、`V2__seed_subjects.sql`、`V3__menus.sql`。
2. 对库确认 BRANCH 节点后填写 `V4__enable_subjects.sql`，先灰度一个对照部门。
3. 一次登录总项目，进入「碳排放报告生成」。
4. 选年报期间，只看到该对照下成功核算任务。
5. 创建并带入后，活动页显示来源标签，自动字段只读。
6. 无工作量时校验标缺失，不按 0 生成。
7. 覆盖一行不改 `source_value`。
8. 生成 Word 后同快照再生成 PDF，不二次调用 LLM。
9. 伪造其他部门 reportId / artifactId 返回 404。
10. 停止 `carbon_report_system` FastAPI/Vue3。
