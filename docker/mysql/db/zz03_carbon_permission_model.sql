-- Carbon permission model for the management API, tpfhs and GoView.
-- Idempotent on MariaDB. Run in the carbon_emissions database.

START TRANSACTION;

-- Concrete organization used by the project.
INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time)
SELECT 3000, 0, '0', '国能朔黄铁路发展有限责任公司', 1, '公司管理员', '', '', '0', '0', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE dept_id = 3000);

INSERT INTO sys_dept (dept_id, parent_id, ancestors, dept_name, order_num, leader, phone, email, status, del_flag, create_by, create_time)
SELECT seed.dept_id, seed.parent_id, seed.ancestors, seed.dept_name, seed.order_num, '', '', '', '0', '0', 'admin', NOW()
FROM (
  SELECT 3010 dept_id, 3000 parent_id, '0,3000' ancestors, '原平分公司' dept_name, 1 order_num UNION ALL
  SELECT 3020, 3000, '0,3000', '肃宁分公司', 2 UNION ALL
  SELECT 3030, 3000, '0,3000', '机辆分公司', 3 UNION ALL
  SELECT 3011, 3010, '0,3000,3010', '原平运输生产部', 1 UNION ALL
  SELECT 3012, 3010, '0,3000,3010', '原平机务检修部', 2 UNION ALL
  SELECT 3013, 3010, '0,3000,3010', '原平供电部', 3 UNION ALL
  SELECT 3014, 3010, '0,3000,3010', '原平物资设备部', 4 UNION ALL
  SELECT 3015, 3010, '0,3000,3010', '原平安全环保部', 5 UNION ALL
  SELECT 3021, 3020, '0,3000,3020', '肃宁运输生产部', 1 UNION ALL
  SELECT 3022, 3020, '0,3000,3020', '肃宁机务检修部', 2 UNION ALL
  SELECT 3023, 3020, '0,3000,3020', '肃宁供电部', 3 UNION ALL
  SELECT 3024, 3020, '0,3000,3020', '肃宁物资设备部', 4 UNION ALL
  SELECT 3025, 3020, '0,3000,3020', '肃宁安全环保部', 5 UNION ALL
  SELECT 3031, 3030, '0,3000,3030', '机务段', 1 UNION ALL
  SELECT 3032, 3030, '0,3000,3030', '车辆段', 2 UNION ALL
  SELECT 3033, 3030, '0,3000,3030', '检修中心', 3
) seed
LEFT JOIN sys_dept existing ON existing.dept_id = seed.dept_id
WHERE existing.dept_id IS NULL;

-- Roles: 1 all, 2 custom, 3 department, 4 department and descendants, 5 self.
INSERT INTO sys_role (role_name, role_key, role_sort, data_scope, menu_check_strictly, dept_check_strictly, status, del_flag, create_by, create_time, remark)
SELECT seed.role_name, seed.role_key, seed.role_sort, seed.data_scope, 1, 1, '0', '0', 'admin', NOW(), seed.remark
FROM (
  SELECT '公司级管理员' role_name, 'company_admin' role_key, 1 role_sort, '1' data_scope, '公司全域配置与授权' remark UNION ALL
  SELECT '地区管理员', 'region_admin', 2, '4', '本地区及下属部门管理' UNION ALL
  SELECT '部门管理员', 'department_admin', 3, '4', '本部门及下属部门用户管理' UNION ALL
  SELECT '碳核算员', 'carbon_accountant', 4, '4', '本部门及下属部门核算与报告生成' UNION ALL
  SELECT '数据录入员', 'carbon_data_entry', 5, '3', '本部门数据录入与提交' UNION ALL
  SELECT '数据审核员', 'carbon_reviewer', 6, '4', '区域内审核与退回' UNION ALL
  SELECT '审计查看员', 'carbon_auditor', 7, '1', '公司范围日志与报表只读审计' UNION ALL
  SELECT '大屏运维员', 'screen_operator', 8, '1', 'GoView 大屏与数据源维护'
) seed
LEFT JOIN sys_role existing ON existing.role_key COLLATE utf8mb4_unicode_ci = seed.role_key COLLATE utf8mb4_unicode_ci
WHERE existing.role_id IS NULL;

-- Rebuild project-role grants so reruns cannot retain stale sample grants.
DELETE relation
FROM sys_role_menu relation
JOIN sys_role role ON role.role_id = relation.role_id
WHERE role.role_key IN (
  'company_admin', 'region_admin', 'department_admin', 'carbon_accountant',
  'carbon_data_entry', 'carbon_reviewer', 'carbon_auditor', 'screen_operator'
);

DELETE relation
FROM sys_role_menu relation
JOIN sys_menu menu_item ON menu_item.menu_id = relation.menu_id
WHERE menu_item.perms IN ('carbon:report:view', 'carbon:report:review', 'carbon:report:export');

DELETE FROM sys_menu
WHERE menu_type = 'F'
  AND perms IN ('carbon:report:view', 'carbon:report:review', 'carbon:report:export');

CREATE TEMPORARY TABLE sys_permission_seed (
  menu_name VARCHAR(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  perms VARCHAR(100) COLLATE utf8mb4_unicode_ci NOT NULL UNIQUE,
  order_num INT NOT NULL
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO sys_permission_seed VALUES
  ('模型查看', 'carbon:model:view', 1),
  ('模型编辑', 'carbon:model:edit', 2),
  ('模型校验', 'carbon:model:validate', 3),
  ('参数查看', 'carbon:params:view', 10),
  ('全局参数维护', 'carbon:params:edit', 11),
  ('采集点维护', 'carbon:params:collection:edit', 12),
  ('大屏查看', 'carbon:screen:view', 20),
  ('大屏维护', 'carbon:screen:edit', 21),
  ('排放数据查看', 'carbon:data:view', 30),
  ('排放数据录入', 'carbon:data:edit', 31),
  ('排放数据提交', 'carbon:data:submit', 32),
  ('排放数据审核', 'carbon:data:review', 33),
  ('排放数据锁定', 'carbon:data:lock', 34),
  ('统计查看', 'carbon:statistics:view', 40),
  ('统计导出', 'carbon:statistics:export', 41),
  ('报告列表', 'carbon:report:list', 50),
  ('报告详情', 'carbon:report:query', 51),
  ('报告新建', 'carbon:report:add', 52),
  ('报告编辑', 'carbon:report:edit', 53),
  ('报告删除', 'carbon:report:remove', 54),
  ('报告导入', 'carbon:report:import', 55),
  ('报告校验', 'carbon:report:validate', 56),
  ('报告生成', 'carbon:report:generate', 57),
  ('报告下载', 'carbon:report:download', 58),
  ('企业档案查看', 'carbon:report:profile:query', 59),
  ('企业档案编辑', 'carbon:report:profile:edit', 60),
  ('系统管理入口', 'carbon:system:view', 65),
  ('日志查看', 'carbon:logs:view', 70),
  ('日志导出', 'carbon:logs:export', 71);

INSERT INTO sys_menu (menu_name, parent_id, order_num, path, component, query, route_name,
  is_frame, is_cache, menu_type, visible, status, perms, icon, create_by, create_time, remark)
SELECT seed.menu_name, 2000, seed.order_num, '', '', '', '', 1, 0, 'F', '1', '0',
       seed.perms, '#', 'admin', NOW(), '碳排放细粒度操作权限'
FROM sys_permission_seed seed
LEFT JOIN sys_menu existing ON existing.perms COLLATE utf8mb4_unicode_ci = seed.perms
WHERE existing.menu_id IS NULL;

UPDATE sys_menu menu_item
JOIN sys_permission_seed seed
  ON menu_item.perms COLLATE utf8mb4_unicode_ci = seed.perms
SET menu_item.menu_name = seed.menu_name,
    menu_item.order_num = seed.order_num,
    menu_item.update_by = 'admin',
    menu_item.update_time = NOW();

CREATE TEMPORARY TABLE sys_role_permission_seed (
  role_key VARCHAR(100) COLLATE utf8mb4_unicode_ci,
  perms VARCHAR(100) COLLATE utf8mb4_unicode_ci
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Company administrators own every carbon permission.
INSERT INTO sys_role_permission_seed
SELECT 'company_admin', perms FROM sys_permission_seed;

INSERT INTO sys_role_permission_seed VALUES
  ('region_admin','carbon:model:view'), ('region_admin','carbon:model:edit'), ('region_admin','carbon:model:validate'),
  ('region_admin','carbon:params:view'), ('region_admin','carbon:params:collection:edit'),
  ('region_admin','carbon:screen:view'), ('region_admin','carbon:data:view'), ('region_admin','carbon:data:edit'),
  ('region_admin','carbon:data:submit'), ('region_admin','carbon:data:review'), ('region_admin','carbon:data:lock'),
  ('region_admin','carbon:statistics:view'), ('region_admin','carbon:statistics:export'),
  ('region_admin','carbon:report:list'), ('region_admin','carbon:report:query'), ('region_admin','carbon:report:add'),
  ('region_admin','carbon:report:edit'), ('region_admin','carbon:report:remove'), ('region_admin','carbon:report:import'),
  ('region_admin','carbon:report:validate'), ('region_admin','carbon:report:generate'), ('region_admin','carbon:report:download'),
  ('region_admin','carbon:report:profile:query'), ('region_admin','carbon:report:profile:edit'), ('region_admin','carbon:system:view'),
  ('region_admin','carbon:logs:view'), ('region_admin','carbon:logs:export'),

  ('department_admin','carbon:model:view'), ('department_admin','carbon:params:view'),
  ('department_admin','carbon:screen:view'), ('department_admin','carbon:data:view'),
  ('department_admin','carbon:data:edit'), ('department_admin','carbon:data:submit'),
  ('department_admin','carbon:statistics:view'), ('department_admin','carbon:report:list'),
  ('department_admin','carbon:report:query'), ('department_admin','carbon:report:add'),
  ('department_admin','carbon:report:edit'), ('department_admin','carbon:report:import'),
  ('department_admin','carbon:report:validate'), ('department_admin','carbon:report:profile:query'),
  ('department_admin','carbon:system:view'), ('department_admin','carbon:logs:view'),

  ('carbon_accountant','carbon:model:view'), ('carbon_accountant','carbon:model:edit'),
  ('carbon_accountant','carbon:model:validate'), ('carbon_accountant','carbon:params:view'),
  ('carbon_accountant','carbon:params:collection:edit'), ('carbon_accountant','carbon:screen:view'),
  ('carbon_accountant','carbon:data:view'), ('carbon_accountant','carbon:statistics:view'),
  ('carbon_accountant','carbon:statistics:export'), ('carbon_accountant','carbon:report:list'),
  ('carbon_accountant','carbon:report:query'), ('carbon_accountant','carbon:report:add'),
  ('carbon_accountant','carbon:report:edit'), ('carbon_accountant','carbon:report:import'),
  ('carbon_accountant','carbon:report:validate'), ('carbon_accountant','carbon:report:generate'),
  ('carbon_accountant','carbon:report:download'), ('carbon_accountant','carbon:report:profile:query'),
  ('carbon_accountant','carbon:logs:view'),

  ('carbon_data_entry','carbon:model:view'), ('carbon_data_entry','carbon:params:view'),
  ('carbon_data_entry','carbon:screen:view'), ('carbon_data_entry','carbon:data:view'),
  ('carbon_data_entry','carbon:data:edit'), ('carbon_data_entry','carbon:data:submit'),
  ('carbon_data_entry','carbon:statistics:view'), ('carbon_data_entry','carbon:report:list'),
  ('carbon_data_entry','carbon:report:query'),

  ('carbon_reviewer','carbon:model:view'), ('carbon_reviewer','carbon:params:view'),
  ('carbon_reviewer','carbon:screen:view'), ('carbon_reviewer','carbon:data:view'),
  ('carbon_reviewer','carbon:data:review'), ('carbon_reviewer','carbon:statistics:view'),
  ('carbon_reviewer','carbon:report:list'), ('carbon_reviewer','carbon:report:query'),
  ('carbon_reviewer','carbon:report:validate'), ('carbon_reviewer','carbon:report:profile:query'),
  ('carbon_reviewer','carbon:logs:view'),

  ('carbon_auditor','carbon:data:view'), ('carbon_auditor','carbon:statistics:view'),
  ('carbon_auditor','carbon:statistics:export'), ('carbon_auditor','carbon:report:list'),
  ('carbon_auditor','carbon:report:query'), ('carbon_auditor','carbon:report:download'),
  ('carbon_auditor','carbon:report:profile:query'), ('carbon_auditor','carbon:logs:view'),
  ('carbon_auditor','carbon:logs:export'),

  ('screen_operator','carbon:screen:view'), ('screen_operator','carbon:screen:edit'),
  ('screen_operator','carbon:params:view');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role.role_id, menu.menu_id
FROM sys_role_permission_seed seed
JOIN sys_role role ON role.role_key COLLATE utf8mb4_unicode_ci = seed.role_key
JOIN sys_menu menu ON menu.perms COLLATE utf8mb4_unicode_ci = seed.perms
LEFT JOIN sys_role_menu existing ON existing.role_id = role.role_id AND existing.menu_id = menu.menu_id
WHERE existing.role_id IS NULL;

-- Native administration permissions needed by the project roles.
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT role.role_id, menu.menu_id
FROM sys_role role
JOIN sys_menu menu ON (
  (role.role_key = 'company_admin' AND menu.perms LIKE 'system:%'
    AND menu.perms NOT IN ('system:operlog:remove','system:logininfor:remove','system:logininfor:unlock')) OR
  (role.role_key = 'region_admin' AND menu.perms IN ('system:user:list','system:user:query','system:user:add','system:user:edit','system:dept:list')) OR
  (role.role_key = 'department_admin' AND menu.perms IN ('system:user:list','system:user:query','system:user:add','system:user:edit','system:dept:list')) OR
  (role.role_key IN ('company_admin','region_admin','department_admin','carbon_accountant','carbon_reviewer','carbon_auditor')
    AND menu.perms IN ('system:operlog:list','system:logininfor:list')) OR
  (role.role_key IN ('company_admin','region_admin','carbon_auditor')
    AND menu.perms IN ('system:operlog:export','system:logininfor:export')) OR
  (role.role_key IN ('company_admin','carbon_auditor')
    AND menu.perms IN ('monitor:job:list','monitor:job:export'))
)
LEFT JOIN sys_role_menu existing ON existing.role_id = role.role_id AND existing.menu_id = menu.menu_id
WHERE existing.role_id IS NULL;

-- Add every parent menu needed to reach the assigned pages. Three passes cover the current menu depth.
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT assigned.role_id, menu.parent_id
FROM sys_role_menu assigned JOIN sys_menu menu ON menu.menu_id = assigned.menu_id
WHERE menu.parent_id > 0;
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT assigned.role_id, menu.parent_id
FROM sys_role_menu assigned JOIN sys_menu menu ON menu.menu_id = assigned.menu_id
WHERE menu.parent_id > 0;
INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT assigned.role_id, menu.parent_id
FROM sys_role_menu assigned JOIN sys_menu menu ON menu.menu_id = assigned.menu_id
WHERE menu.parent_id > 0;

CREATE TABLE IF NOT EXISTS sys_carbon_role_node (
  role_id BIGINT NOT NULL COMMENT '角色ID',
  node_id BIGINT NOT NULL COMMENT '碳核算节点ID',
  created_by BIGINT NULL COMMENT '创建人ID',
  updated_by BIGINT NULL COMMENT '更新人ID',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (role_id, node_id),
  CONSTRAINT fk_carbon_role_node_role FOREIGN KEY (role_id) REFERENCES sys_role(role_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色与碳核算节点授权关系';

SET @add_template_dept = IF(
  EXISTS(SELECT 1 FROM information_schema.columns
         WHERE table_schema = DATABASE() AND table_name = 'emission_template' AND column_name = 'dept_id'),
  'SELECT 1',
  'ALTER TABLE emission_template ADD COLUMN dept_id BIGINT NULL COMMENT ''所属部门ID'' AFTER description'
);
PREPARE permission_stmt FROM @add_template_dept;
EXECUTE permission_stmt;
DEALLOCATE PREPARE permission_stmt;

SET @add_log_dept = IF(
  EXISTS(SELECT 1 FROM information_schema.columns
         WHERE table_schema = DATABASE() AND table_name = 'sys_oper_log' AND column_name = 'dept_id'),
  'SELECT 1',
  'ALTER TABLE sys_oper_log ADD COLUMN dept_id BIGINT NULL COMMENT ''操作人部门ID'' AFTER dept_name'
);
PREPARE permission_stmt FROM @add_log_dept;
EXECUTE permission_stmt;
DEALLOCATE PREPARE permission_stmt;

DROP TEMPORARY TABLE sys_role_permission_seed;
DROP TEMPORARY TABLE sys_permission_seed;
COMMIT;
