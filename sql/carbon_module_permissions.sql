-- Carbon module entry permissions. Business data remains in the original emission_* tables.
-- System permissions are registered in the existing RuoYi sys_menu/sys_role_menu tables.

INSERT INTO sys_menu
SELECT 2060, '碳排放模型设置', 2000, 6, '', '', '', '', 1, 0, 'F', '1', '0',
       'carbon:model:view', '#', 'admin', NOW(), '', NULL, '访问碳排放模型设计模块'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'carbon:model:view');

INSERT INTO sys_menu
SELECT 2061, '碳排放核算参数设置', 2000, 7, '', '', '', '', 1, 0, 'F', '1', '0',
       'carbon:params:view', '#', 'admin', NOW(), '', NULL, '访问核算参数设置模块'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'carbon:params:view');

INSERT INTO sys_menu
SELECT 2062, '碳排放系统管理', 2000, 8, '', '', '', '', 1, 0, 'F', '1', '0',
       'carbon:system:view', '#', 'admin', NOW(), '', NULL, '访问碳排放系统管理模块'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'carbon:system:view');

INSERT INTO sys_menu
SELECT 2063, '碳排放日志审计', 2000, 9, '', '', '', '', 1, 0, 'F', '1', '0',
       'carbon:logs:view', '#', 'admin', NOW(), '', NULL, '访问统一日志审计模块'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE perms = 'carbon:logs:view');

INSERT INTO sys_role_menu (role_id, menu_id)
SELECT r.role_id, m.menu_id
FROM sys_role r
JOIN sys_menu m ON m.perms IN (
  'carbon:model:view', 'carbon:params:view', 'carbon:system:view', 'carbon:logs:view'
)
LEFT JOIN sys_role_menu rm ON rm.role_id = r.role_id AND rm.menu_id = m.menu_id
WHERE r.role_key = 'admin' AND rm.role_id IS NULL;
