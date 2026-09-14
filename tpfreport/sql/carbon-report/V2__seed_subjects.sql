-- Placeholder departments and subject mapping. Do not guess live node IDs.
-- Fill subject_node_id from emission_node (BRANCH) before enabling.

INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, status, del_flag, create_by, create_time)
SELECT 301, 100, '0,100', '肃宁分公司', 1, '0', '0', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE dept_id = 301 OR dept_name = '肃宁分公司');

INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, status, del_flag, create_by, create_time)
SELECT 302, 100, '0,100', '原平分公司', 2, '0', '0', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE dept_id = 302 OR dept_name = '原平分公司');

INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, status, del_flag, create_by, create_time)
SELECT 303, 100, '0,100', '机辆分公司', 3, '0', '0', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE dept_id = 303 OR dept_name = '机辆分公司');

INSERT INTO report_dept_subject (dept_id, subject_node_id, energy_allocation, enabled, create_by, create_time)
SELECT d.dept_id, NULL, 'suring', '0', 'admin', NOW()
FROM sys_dept d
WHERE d.dept_name = '肃宁分公司'
  AND NOT EXISTS (SELECT 1 FROM report_dept_subject s WHERE s.dept_id = d.dept_id);

INSERT INTO report_dept_subject (dept_id, subject_node_id, energy_allocation, enabled, create_by, create_time)
SELECT d.dept_id, NULL, 'yuanping', '0', 'admin', NOW()
FROM sys_dept d
WHERE d.dept_name = '原平分公司'
  AND NOT EXISTS (SELECT 1 FROM report_dept_subject s WHERE s.dept_id = d.dept_id);

INSERT INTO report_dept_subject (dept_id, subject_node_id, energy_allocation, enabled, create_by, create_time)
SELECT d.dept_id, NULL, 'jilong', '0', 'admin', NOW()
FROM sys_dept d
WHERE d.dept_name = '机辆分公司'
  AND NOT EXISTS (SELECT 1 FROM report_dept_subject s WHERE s.dept_id = d.dept_id);

INSERT INTO report_dept_profile (dept_id, legal_name, short_name, entity_code, carbon_department, create_by, create_time)
SELECT d.dept_id, d.dept_name, d.dept_name, 'suning', '安全环保监察部', 'admin', NOW()
FROM sys_dept d
WHERE d.dept_name = '肃宁分公司'
  AND NOT EXISTS (SELECT 1 FROM report_dept_profile p WHERE p.dept_id = d.dept_id);
