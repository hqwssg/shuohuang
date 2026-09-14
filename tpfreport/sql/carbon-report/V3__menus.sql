-- Report module menus. Do not reuse goview permissions.

INSERT INTO sys_menu
SELECT 2070, '碳排放报告', 2000, 10, 'report', 'carbon/report/index', '', '', 1, 0, 'C', '0', '0',
       'carbon:report:list', 'documentation', 'admin', NOW(), '', NULL, '碳排放报告列表'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'carbon:report:list');

INSERT INTO sys_menu
SELECT 2071, '报告查询', 2070, 1, '', '', '', '', 1, 0, 'F', '0', '0',
       'carbon:report:query', '#', 'admin', NOW(), '', NULL, '查询报告'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'carbon:report:query');

INSERT INTO sys_menu
SELECT 2072, '报告新增', 2070, 2, '', '', '', '', 1, 0, 'F', '0', '0',
       'carbon:report:add', '#', 'admin', NOW(), '', NULL, '新增报告'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'carbon:report:add');

INSERT INTO sys_menu
SELECT 2073, '报告修改', 2070, 3, '', '', '', '', 1, 0, 'F', '0', '0',
       'carbon:report:edit', '#', 'admin', NOW(), '', NULL, '修改报告'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'carbon:report:edit');

INSERT INTO sys_menu
SELECT 2074, '报告删除', 2070, 4, '', '', '', '', 1, 0, 'F', '0', '0',
       'carbon:report:remove', '#', 'admin', NOW(), '', NULL, '删除报告'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'carbon:report:remove');

INSERT INTO sys_menu
SELECT 2075, '报告导入', 2070, 5, '', '', '', '', 1, 0, 'F', '0', '0',
       'carbon:report:import', '#', 'admin', NOW(), '', NULL, '导入活动数据'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'carbon:report:import');

INSERT INTO sys_menu
SELECT 2076, '报告校验', 2070, 6, '', '', '', '', 1, 0, 'F', '0', '0',
       'carbon:report:validate', '#', 'admin', NOW(), '', NULL, '校验报告'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'carbon:report:validate');

INSERT INTO sys_menu
SELECT 2077, '报告生成', 2070, 7, '', '', '', '', 1, 0, 'F', '0', '0',
       'carbon:report:generate', '#', 'admin', NOW(), '', NULL, '生成Word/PDF'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'carbon:report:generate');

INSERT INTO sys_menu
SELECT 2078, '报告下载', 2070, 8, '', '', '', '', 1, 0, 'F', '0', '0',
       'carbon:report:download', '#', 'admin', NOW(), '', NULL, '下载生成文件'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'carbon:report:download');

INSERT INTO sys_menu
SELECT 2079, '企业档案查询', 2070, 9, '', '', '', '', 1, 0, 'F', '0', '0',
       'carbon:report:profile:query', '#', 'admin', NOW(), '', NULL, '查询企业档案'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'carbon:report:profile:query');

INSERT INTO sys_menu
SELECT 2080, '企业档案编辑', 2070, 10, '', '', '', '', 1, 0, 'F', '0', '0',
       'carbon:report:profile:edit', '#', 'admin', NOW(), '', NULL, '编辑企业档案'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'carbon:report:profile:edit');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.perms IN (
  'carbon:report:list','carbon:report:query','carbon:report:add','carbon:report:edit',
  'carbon:report:remove','carbon:report:import','carbon:report:validate','carbon:report:generate',
  'carbon:report:download','carbon:report:profile:query','carbon:report:profile:edit'
)
LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id AND rm.menu_id = m.menu_id
WHERE r.role_key = 'admin' AND rm.role_id IS NULL;
