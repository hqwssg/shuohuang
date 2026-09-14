-- Carbon project cleanup, stable role numbering and permission test accounts.
-- Run after carbon_permission_model.sql in the carbon_emissions database.
-- Idempotent on MySQL 8 and MariaDB. All sample accounts use password: admin123

START TRANSACTION;
SET @previous_foreign_key_checks = @@FOREIGN_KEY_CHECKS;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TEMPORARY TABLE tmp_carbon_role_map (
  role_id BIGINT NOT NULL PRIMARY KEY,
  role_key VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL UNIQUE,
  role_name VARCHAR(30) NOT NULL,
  role_sort INT NOT NULL,
  data_scope CHAR(1) NOT NULL,
  remark VARCHAR(500) NOT NULL
) ENGINE=Memory;

INSERT INTO tmp_carbon_role_map VALUES
  (1, 'company_admin', '公司级管理员', 1, '1', '公司全域配置、授权与审计'),
  (2, 'region_admin', '地区管理员', 2, '4', '本地区及下属部门管理'),
  (3, 'department_admin', '部门管理员', 3, '4', '本部门及下属部门用户管理'),
  (4, 'carbon_accountant', '碳核算员', 4, '4', '本部门及下属部门核算与报告生成'),
  (5, 'carbon_data_entry', '数据录入员', 5, '3', '本部门排放数据录入与提交'),
  (6, 'carbon_reviewer', '数据审核员', 6, '4', '本部门及下属部门数据审核'),
  (7, 'carbon_auditor', '审计查看员', 7, '1', '公司范围日志与报表只读审计'),
  (8, 'screen_operator', '大屏运维员', 8, '1', '公司大屏与数据源维护');

-- Snapshot all carbon-role relations before replacing legacy and unordered IDs.
CREATE TEMPORARY TABLE tmp_carbon_role_menu AS
SELECT DISTINCT target.role_id, relation.menu_id
FROM tmp_carbon_role_map target
JOIN sys_role source ON source.role_key COLLATE utf8mb4_unicode_ci = target.role_key
JOIN sys_role_menu relation ON relation.role_id = source.role_id;
ALTER TABLE tmp_carbon_role_menu ADD PRIMARY KEY (role_id, menu_id);

CREATE TEMPORARY TABLE tmp_carbon_role_dept AS
SELECT DISTINCT target.role_id, relation.dept_id
FROM tmp_carbon_role_map target
JOIN sys_role source ON source.role_key COLLATE utf8mb4_unicode_ci = target.role_key
JOIN sys_role_dept relation ON relation.role_id = source.role_id
WHERE relation.dept_id >= 3000;
ALTER TABLE tmp_carbon_role_dept ADD PRIMARY KEY (role_id, dept_id);

CREATE TEMPORARY TABLE tmp_carbon_role_node AS
SELECT DISTINCT target.role_id, relation.node_id, relation.created_by,
       relation.updated_by, relation.created_at, relation.updated_at
FROM tmp_carbon_role_map target
JOIN sys_role source ON source.role_key COLLATE utf8mb4_unicode_ci = target.role_key
JOIN sys_carbon_role_node relation ON relation.role_id = source.role_id;
ALTER TABLE tmp_carbon_role_node ADD PRIMARY KEY (role_id, node_id);

CREATE TEMPORARY TABLE tmp_carbon_user_role AS
SELECT DISTINCT relation.user_id,
       COALESCE(target.role_id, 1) AS role_id
FROM sys_user_role relation
JOIN sys_role source ON source.role_id = relation.role_id
LEFT JOIN tmp_carbon_role_map target ON target.role_key = source.role_key COLLATE utf8mb4_unicode_ci
WHERE target.role_id IS NOT NULL OR source.role_key = 'admin';
ALTER TABLE tmp_carbon_user_role ADD PRIMARY KEY (user_id, role_id);

-- Remove relations for the old sample roles and every previous carbon role ID.
DELETE relation FROM sys_role_menu relation
JOIN sys_role role ON role.role_id = relation.role_id
WHERE role.role_key IN ('admin', 'common')
   OR EXISTS (SELECT 1 FROM tmp_carbon_role_map target WHERE target.role_key = role.role_key COLLATE utf8mb4_unicode_ci)
   OR role.role_id BETWEEN 1 AND 8;

DELETE relation FROM sys_role_dept relation
JOIN sys_role role ON role.role_id = relation.role_id
WHERE role.role_key IN ('admin', 'common')
   OR EXISTS (SELECT 1 FROM tmp_carbon_role_map target WHERE target.role_key = role.role_key COLLATE utf8mb4_unicode_ci)
   OR role.role_id BETWEEN 1 AND 8;

DELETE relation FROM sys_carbon_role_node relation
JOIN sys_role role ON role.role_id = relation.role_id
WHERE role.role_key IN ('admin', 'common')
   OR EXISTS (SELECT 1 FROM tmp_carbon_role_map target WHERE target.role_key = role.role_key COLLATE utf8mb4_unicode_ci)
   OR role.role_id BETWEEN 1 AND 8;

DELETE relation FROM sys_user_role relation
JOIN sys_role role ON role.role_id = relation.role_id
WHERE role.role_key IN ('admin', 'common')
   OR EXISTS (SELECT 1 FROM tmp_carbon_role_map target WHERE target.role_key = role.role_key COLLATE utf8mb4_unicode_ci)
   OR role.role_id BETWEEN 1 AND 8;

DELETE FROM sys_role
WHERE role_key IN ('admin', 'common')
   OR EXISTS (SELECT 1 FROM tmp_carbon_role_map target WHERE target.role_key = sys_role.role_key COLLATE utf8mb4_unicode_ci)
   OR role_id BETWEEN 1 AND 8;

INSERT INTO sys_role (
  role_id, role_name, role_key, role_sort, data_scope,
  menu_check_strictly, dept_check_strictly, status, del_flag,
  create_by, create_time, update_by, update_time, remark
)
SELECT role_id, role_name, role_key, role_sort, data_scope,
       1, 1, '0', '0', 'admin', NOW(), '', NULL, remark
FROM tmp_carbon_role_map;

INSERT IGNORE INTO sys_role_menu (role_id, menu_id)
SELECT role_id, menu_id FROM tmp_carbon_role_menu;

INSERT IGNORE INTO sys_role_dept (role_id, dept_id)
SELECT role_id, dept_id FROM tmp_carbon_role_dept;

INSERT IGNORE INTO sys_carbon_role_node
  (role_id, node_id, created_by, updated_by, created_at, updated_at)
SELECT role_id, node_id, created_by, updated_by, created_at, updated_at
FROM tmp_carbon_role_node;

INSERT IGNORE INTO sys_user_role (user_id, role_id)
SELECT user_id, role_id FROM tmp_carbon_user_role;

-- Remove public-facing framework samples.
DELETE FROM sys_role_menu WHERE menu_id = 4;
DELETE FROM sys_menu WHERE menu_id = 4 OR path IN ('http://ruoyi.vip', 'https://ruoyi.vip');
DELETE FROM sys_notice
WHERE notice_id IN (1, 2, 3)
   OR notice_title LIKE '%若依%'
   OR notice_content LIKE '%ruoyi.vip%';

DELETE FROM sys_user_role WHERE user_id = 2;
DELETE FROM sys_user_post WHERE user_id = 2;
DELETE FROM sys_user WHERE user_id = 2 OR user_name = 'ry';
UPDATE sys_user
SET dept_id = 3000,
    nick_name = '系统管理员',
    email = '',
    phonenumber = '',
    avatar = '',
    remark = '公司级系统管理员',
    update_by = 'admin',
    update_time = NOW()
WHERE user_id = 1;

DELETE FROM sys_dept WHERE dept_id BETWEEN 100 AND 109;

-- Replace generic sample posts with project-specific posts.
DELETE FROM sys_user_post WHERE post_id BETWEEN 1 AND 8;
DELETE FROM sys_post WHERE post_id BETWEEN 1 AND 8;
INSERT INTO sys_post
  (post_id, post_code, post_name, post_sort, status, create_by, create_time, remark)
VALUES
  (1, 'company_manager', '公司管理岗', 1, '0', 'admin', NOW(), '公司级系统管理'),
  (2, 'region_manager', '地区管理岗', 2, '0', 'admin', NOW(), '地区组织与数据管理'),
  (3, 'department_manager', '部门管理岗', 3, '0', 'admin', NOW(), '部门人员与业务管理'),
  (4, 'carbon_accounting', '碳核算岗', 4, '0', 'admin', NOW(), '核算模型与报告'),
  (5, 'data_entry', '数据录入岗', 5, '0', 'admin', NOW(), '排放数据录入'),
  (6, 'data_review', '数据审核岗', 6, '0', 'admin', NOW(), '排放数据审核'),
  (7, 'carbon_audit', '审计岗', 7, '0', 'admin', NOW(), '日志与报表审计'),
  (8, 'screen_ops', '大屏运维岗', 8, '0', 'admin', NOW(), 'GoView 大屏维护');

-- Preserve the existing administrator password hash for all permission test users.
SET @sample_password = (SELECT password FROM sys_user WHERE user_id = 1 LIMIT 1);
SET @sample_password = COALESCE(
  @sample_password,
  '$2a$10$7JB720yubVSZvUI0rEqK/.VqGOZTH.ulu33dHOiBE8ByOhJIrdAu2'
);

INSERT INTO sys_user
  (user_id, dept_id, user_name, nick_name, user_type, email, phonenumber, sex,
   avatar, password, status, del_flag, login_ip, login_date, pwd_update_date,
   create_by, create_time, update_by, update_time, remark)
VALUES
  (2, 3010, 'yuanping_admin', '原平地区管理员', '00', '', '', '0', '', @sample_password, '0', '0', '', NULL, NOW(), 'admin', NOW(), '', NULL, '测试地区及下属部门权限'),
  (3, 3020, 'suning_admin', '肃宁地区管理员', '00', '', '', '0', '', @sample_password, '0', '0', '', NULL, NOW(), 'admin', NOW(), '', NULL, '测试另一地区的数据隔离'),
  (4, 3030, 'locomotive_admin', '机辆部门管理员', '00', '', '', '0', '', @sample_password, '0', '0', '', NULL, NOW(), 'admin', NOW(), '', NULL, '测试部门管理权限'),
  (5, 3015, 'carbon_accountant', '原平碳核算员', '00', '', '', '0', '', @sample_password, '0', '0', '', NULL, NOW(), 'admin', NOW(), '', NULL, '测试模型、参数和报告权限'),
  (6, 3011, 'data_entry', '原平数据录入员', '00', '', '', '0', '', @sample_password, '0', '0', '', NULL, NOW(), 'admin', NOW(), '', NULL, '测试录入和提交权限'),
  (7, 3011, 'data_reviewer', '原平数据审核员', '00', '', '', '0', '', @sample_password, '0', '0', '', NULL, NOW(), 'admin', NOW(), '', NULL, '测试审核权限'),
  (8, 3000, 'carbon_auditor', '公司审计查看员', '00', '', '', '0', '', @sample_password, '0', '0', '', NULL, NOW(), 'admin', NOW(), '', NULL, '测试公司级只读审计权限'),
  (9, 3000, 'screen_operator', '大屏运维员', '00', '', '', '0', '', @sample_password, '0', '0', '', NULL, NOW(), 'admin', NOW(), '', NULL, '测试大屏维护权限')
ON DUPLICATE KEY UPDATE
  dept_id = VALUES(dept_id),
  user_name = VALUES(user_name),
  nick_name = VALUES(nick_name),
  password = VALUES(password),
  status = '0',
  del_flag = '0',
  remark = VALUES(remark),
  update_by = 'admin',
  update_time = NOW();

DELETE FROM sys_user_role WHERE user_id BETWEEN 1 AND 9;
INSERT INTO sys_user_role (user_id, role_id) VALUES
  (1, 1), (2, 2), (3, 2), (4, 3), (5, 4),
  (6, 5), (7, 6), (8, 7), (9, 8);

DELETE FROM sys_user_post WHERE user_id BETWEEN 1 AND 9;
INSERT INTO sys_user_post (user_id, post_id) VALUES
  (1, 1), (2, 2), (3, 2), (4, 3), (5, 4),
  (6, 5), (7, 6), (8, 7), (9, 8);

ALTER TABLE sys_role AUTO_INCREMENT = 9;
ALTER TABLE sys_user AUTO_INCREMENT = 10;
ALTER TABLE sys_post AUTO_INCREMENT = 9;

DROP TEMPORARY TABLE tmp_carbon_user_role;
DROP TEMPORARY TABLE tmp_carbon_role_node;
DROP TEMPORARY TABLE tmp_carbon_role_dept;
DROP TEMPORARY TABLE tmp_carbon_role_menu;
DROP TEMPORARY TABLE tmp_carbon_role_map;

SET FOREIGN_KEY_CHECKS = @previous_foreign_key_checks;
COMMIT;
