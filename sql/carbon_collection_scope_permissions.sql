-- Incremental migration; existing emission_* table names and station data are preserved.
-- Execute this file with a UTF-8 MySQL client (--default-character-set=utf8mb4).
-- In PowerShell, do not pipe Get-Content into mysql: that can convert Chinese text to '?'.
SET NAMES utf8mb4;
CREATE TABLE IF NOT EXISTS sys_carbon_company (
  dept_id BIGINT NOT NULL PRIMARY KEY,
  created_by BIGINT COMMENT '创建人ID', updated_by BIGINT COMMENT '更新人ID',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (dept_id) REFERENCES sys_dept(dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集范围所属分公司';
CREATE TABLE IF NOT EXISTS sys_carbon_scope_owner (
  scope_type VARCHAR(20) NOT NULL, scope_id BIGINT NOT NULL,
  company_dept_id BIGINT NOT NULL,
  created_by BIGINT COMMENT '创建人ID', updated_by BIGINT COMMENT '更新人ID',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY(scope_type, scope_id),
  FOREIGN KEY(company_dept_id) REFERENCES sys_carbon_company(dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站区间燃料热能范围公司归属';
CREATE TABLE IF NOT EXISTS sys_carbon_dept_scope (
  dept_id BIGINT NOT NULL, scope_type VARCHAR(20) NOT NULL, scope_id BIGINT NOT NULL,
  can_write TINYINT NOT NULL DEFAULT 0,
  created_by BIGINT COMMENT '创建人ID', updated_by BIGINT COMMENT '更新人ID',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY(dept_id, scope_type, scope_id),
  FOREIGN KEY(dept_id) REFERENCES sys_dept(dept_id),
  FOREIGN KEY(scope_type,scope_id) REFERENCES sys_carbon_scope_owner(scope_type,scope_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门工队采集范围授权';

-- Node-level grants. sys_carbon_dept_scope remains as a compatibility table;
-- this table is authoritative for read/write/delegation decisions.
CREATE TABLE IF NOT EXISTS sys_carbon_scope_grant (
  grant_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  scope_type VARCHAR(20) NOT NULL,
  scope_id BIGINT NOT NULL,
  node_type VARCHAR(30) NOT NULL COMMENT 'root/station/concentrator/sub_scope/meter',
  node_id BIGINT NOT NULL,
  node_name VARCHAR(200) NULL,
  dept_id BIGINT NOT NULL,
  can_read TINYINT NOT NULL DEFAULT 1,
  can_write TINYINT NOT NULL DEFAULT 0,
  can_delegate TINYINT NOT NULL DEFAULT 0,
  parent_grant_id BIGINT NULL,
  granted_by_dept_id BIGINT NULL,
  created_by BIGINT COMMENT '创建人ID', updated_by BIGINT COMMENT '更新人ID',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uk_carbon_scope_grant_node(dept_id,scope_type,scope_id,node_type,node_id),
  KEY idx_carbon_scope_grant_lookup(scope_type,scope_id,node_type,node_id),
  KEY idx_carbon_scope_grant_dept(dept_id,scope_type,scope_id),
  FOREIGN KEY(dept_id) REFERENCES sys_dept(dept_id),
  FOREIGN KEY(granted_by_dept_id) REFERENCES sys_dept(dept_id),
  FOREIGN KEY(parent_grant_id) REFERENCES sys_carbon_scope_grant(grant_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='碳排放节点级数据权限';

START TRANSACTION;
INSERT IGNORE INTO sys_carbon_company(dept_id,created_by,updated_by)
SELECT dept_id,1,1 FROM sys_dept WHERE dept_id IN(3010,3020,3030) AND del_flag='0';
-- Replace only the previous demonstration department labels, retaining their users and IDs.
UPDATE sys_dept SET dept_name='运输生产部',update_by='admin',update_time=NOW() WHERE dept_id=3021 AND dept_name='肃宁运输生产部';
UPDATE sys_dept SET dept_name='综合办公室',update_by='admin',update_time=NOW() WHERE dept_id=3022 AND dept_name='肃宁机务检修部';
UPDATE sys_dept SET dept_name='供电工队',parent_id=3021,ancestors='0,3000,3020,3021',update_by='admin',update_time=NOW() WHERE dept_id=3023 AND dept_name='肃宁供电部';
UPDATE sys_dept SET dept_name='保障部',update_by='admin',update_time=NOW() WHERE dept_id=3024 AND dept_name='肃宁物资设备部';
UPDATE sys_dept SET dept_name='技术部',update_by='admin',update_time=NOW() WHERE dept_id=3025 AND dept_name='肃宁安全环保部';
UPDATE sys_dept SET dept_name='电力运用管理中心（运管中心）',update_by='admin',update_time=NOW() WHERE dept_id=3031 AND dept_name='机务段';
UPDATE sys_dept SET dept_name='机车检修车间',update_by='admin',update_time=NOW() WHERE dept_id=3032 AND dept_name='车辆段';
UPDATE sys_dept SET dept_name='设备车间',update_by='admin',update_time=NOW() WHERE dept_id=3033 AND dept_name='检修中心';

-- Remove only the duplicate departments created when an older deployment
-- imported this file with a non-UTF-8 client. The real records are retained;
-- referenced departments are never removed automatically.
DELETE g FROM sys_carbon_scope_grant g JOIN sys_dept d ON d.dept_id=g.dept_id
WHERE d.dept_name REGEXP '^[?]+$' AND d.dept_id >= 3052;
DELETE g FROM sys_carbon_dept_scope g JOIN sys_dept d ON d.dept_id=g.dept_id
WHERE d.dept_name REGEXP '^[?]+$' AND d.dept_id >= 3052;
DELETE FROM sys_dept WHERE dept_name REGEXP '^[?]+$' AND dept_id >= 3052
  AND NOT EXISTS (SELECT 1 FROM sys_user u WHERE u.dept_id=sys_dept.dept_id)
  AND NOT EXISTS (SELECT 1 FROM sys_role_dept r WHERE r.dept_id=sys_dept.dept_id);

CREATE TEMPORARY TABLE sys_scope_dept_seed(parent_id BIGINT,dept_name VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,order_num INT);
INSERT INTO sys_scope_dept_seed VALUES
(3021,'车务工队',1),(3021,'工务工队',2),(3021,'电务工队',4),(3021,'综合工队',5),
(3023,'肃北供电工队',1),(3023,'灵寿供电工队',2),(3023,'定西供电工队',3),
(3023,'安国供电工队',4),(3023,'沧州西供电工队',5),(3023,'行别营供电工队',6),
(3023,'黄骅港前工队',7),(3023,'黄骅港口工队',8),(3023,'黄骅港南工队',9),
(3023,'北港供电工队',10),(3023,'神港电力作业组',11),
(3023,'定州西供电工队',12),
(3030,'公司机关',1),(3030,'列检作业单元',5);
INSERT INTO sys_dept(parent_id,ancestors,dept_name,order_num,status,del_flag,create_by,create_time)
SELECT s.parent_id,CONCAT(p.ancestors,',',p.dept_id),s.dept_name,s.order_num,'0','0','admin',NOW()
FROM sys_scope_dept_seed s JOIN sys_dept p ON p.dept_id=s.parent_id
WHERE NOT EXISTS(SELECT 1 FROM sys_dept d WHERE d.parent_id=s.parent_id AND d.dept_name COLLATE utf8mb4_unicode_ci=s.dept_name COLLATE utf8mb4_unicode_ci AND d.del_flag='0');
DROP TEMPORARY TABLE sys_scope_dept_seed;

-- A subordinate maintenance crew demonstrates department-level delegation.
INSERT INTO sys_dept(parent_id,ancestors,dept_name,order_num,status,del_flag,create_by,create_time)
SELECT 3035,'0,3000,3020,3021,3035',CONVERT(0xE7BABFE8B7AFE7BBB4E68AA4E5B7A5E9989F USING utf8mb4),20,'0','0','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_dept WHERE parent_id=3035 AND dept_name=CONVERT(0xE7BABFE8B7AFE7BBB4E68AA4E5B7A5E9989F USING utf8mb4) AND del_flag='0');

INSERT INTO emission_fossil_fuel_collection_scope(scope_name,sort_order,description,created_by,updated_by)
SELECT d.dept_name,d.order_num,'按分公司划分，部门维护范围由管理员授权',1,1
FROM sys_carbon_company c JOIN sys_dept d ON d.dept_id=c.dept_id
WHERE NOT EXISTS(SELECT 1 FROM emission_fossil_fuel_collection_scope s WHERE s.scope_name COLLATE utf8mb4_unicode_ci=d.dept_name COLLATE utf8mb4_unicode_ci);
INSERT INTO emission_purchased_heat_collection_scope(scope_name,sort_order,description,created_by,updated_by)
SELECT d.dept_name,d.order_num,'按分公司划分，部门维护范围由管理员授权',1,1
FROM sys_carbon_company c JOIN sys_dept d ON d.dept_id=c.dept_id
WHERE NOT EXISTS(SELECT 1 FROM emission_purchased_heat_collection_scope s WHERE s.scope_name COLLATE utf8mb4_unicode_ci=d.dept_name COLLATE utf8mb4_unicode_ci);
INSERT IGNORE INTO sys_carbon_scope_owner(scope_type,scope_id,company_dept_id,created_by,updated_by)
SELECT 'fossil',s.id,c.dept_id,1,1 FROM emission_fossil_fuel_collection_scope s
JOIN sys_dept d ON d.dept_name COLLATE utf8mb4_unicode_ci=s.scope_name COLLATE utf8mb4_unicode_ci JOIN sys_carbon_company c ON c.dept_id=d.dept_id;
INSERT IGNORE INTO sys_carbon_scope_owner(scope_type,scope_id,company_dept_id,created_by,updated_by)
SELECT 'heat',s.id,c.dept_id,1,1 FROM emission_purchased_heat_collection_scope s
JOIN sys_dept d ON d.dept_name COLLATE utf8mb4_unicode_ci=s.scope_name COLLATE utf8mb4_unicode_ci JOIN sys_carbon_company c ON c.dept_id=d.dept_id;
INSERT IGNORE INTO sys_carbon_dept_scope(dept_id,scope_type,scope_id,can_write,created_by,updated_by)
SELECT d.dept_id,o.scope_type,o.scope_id,1,1,1 FROM sys_carbon_scope_owner o
JOIN sys_dept d ON (o.scope_type='fossil' AND d.parent_id=3021 AND d.dept_name='工务工队')
 OR (o.scope_type='heat' AND d.dept_id=3024)
WHERE o.company_dept_id=3020;

-- Initial electric example: 定州西供电工队 can maintain 定州西站、定定区间、定州东站.
INSERT IGNORE INTO sys_carbon_scope_owner(scope_type,scope_id,company_dept_id,created_by,updated_by)
SELECT 'electricity',s.id,3020,1,1
FROM emission_station_interval s
JOIN sys_carbon_company c ON c.dept_id=3020
WHERE s.name IN ('定州西站','定定区间','定州东站');
INSERT IGNORE INTO sys_carbon_dept_scope(dept_id,scope_type,scope_id,can_write,created_by,updated_by)
SELECT d.dept_id,'electricity',o.scope_id,1,1,1
FROM sys_dept d
JOIN sys_carbon_scope_owner o ON o.scope_type='electricity'
WHERE d.dept_name='定州西供电工队' AND d.parent_id=3023 AND o.company_dept_id=3020
  AND o.scope_id IN (SELECT id FROM emission_station_interval WHERE name IN ('定州西站','定定区间','定州东站'));

-- Copy legacy flat grants to explicit root-node grants. This is idempotent and
-- keeps child-node delegations on subsequent deployments.
INSERT IGNORE INTO sys_carbon_scope_grant
  (scope_type,scope_id,node_type,node_id,node_name,dept_id,can_read,can_write,can_delegate,granted_by_dept_id,created_by,updated_by)
SELECT g.scope_type,g.scope_id,
       CASE WHEN g.scope_type='electricity' THEN 'station' ELSE 'scope' END,
       g.scope_id,
       CASE WHEN g.scope_type='electricity' THEN s.name ELSE fs.scope_name END,
       g.dept_id,1,g.can_write,1,o.company_dept_id,COALESCE(g.created_by,1),COALESCE(g.updated_by,1)
FROM sys_carbon_dept_scope g
JOIN sys_carbon_scope_owner o ON o.scope_type=g.scope_type AND o.scope_id=g.scope_id
JOIN sys_dept d ON d.dept_id=g.dept_id AND d.status='0' AND d.del_flag='0'
LEFT JOIN emission_station_interval s ON g.scope_type='electricity' AND s.id=g.scope_id
LEFT JOIN emission_fossil_fuel_collection_scope fs ON g.scope_type='fossil' AND fs.id=g.scope_id
WHERE g.scope_type IN ('electricity','fossil');
INSERT IGNORE INTO sys_carbon_scope_grant
  (scope_type,scope_id,node_type,node_id,node_name,dept_id,can_read,can_write,can_delegate,granted_by_dept_id,created_by,updated_by)
SELECT g.scope_type,g.scope_id,'scope',g.scope_id,hs.scope_name,g.dept_id,1,g.can_write,1,o.company_dept_id,COALESCE(g.created_by,1),COALESCE(g.updated_by,1)
FROM sys_carbon_dept_scope g
JOIN sys_carbon_scope_owner o ON o.scope_type=g.scope_type AND o.scope_id=g.scope_id
JOIN sys_dept d ON d.dept_id=g.dept_id AND d.status='0' AND d.del_flag='0'
JOIN emission_purchased_heat_collection_scope hs ON hs.id=g.scope_id
WHERE g.scope_type='heat';

-- Example second-level fossil categories for the Suning company.
INSERT INTO emission_fossil_fuel_collection_sub_scope(sub_scope_name,collection_scope_id,sort_order,description,created_by,updated_by)
SELECT CONVERT(0xE6B1BDE8BDA6E794A8E6B1BDE6B2B9 USING utf8mb4),s.id,1,'汽车用汽油',1,1
FROM emission_fossil_fuel_collection_scope s WHERE s.scope_name=CONVERT(0xE88283E5AE81E58886E585ACE58FB8 USING utf8mb4)
  AND NOT EXISTS(SELECT 1 FROM emission_fossil_fuel_collection_sub_scope x WHERE x.collection_scope_id=s.id AND x.sub_scope_name=CONVERT(0xE6B1BDE8BDA6E794A8E6B1BDE6B2B9 USING utf8mb4));
INSERT INTO emission_fossil_fuel_collection_sub_scope(sub_scope_name,collection_scope_id,sort_order,description,created_by,updated_by)
SELECT CONVERT(0xE6B1BDE8BDA6E794A8E69FB4E6B2B9 USING utf8mb4),s.id,2,'汽车用柴油',1,1
FROM emission_fossil_fuel_collection_scope s WHERE s.scope_name=CONVERT(0xE88283E5AE81E58886E585ACE58FB8 USING utf8mb4)
  AND NOT EXISTS(SELECT 1 FROM emission_fossil_fuel_collection_sub_scope x WHERE x.collection_scope_id=s.id AND x.sub_scope_name=CONVERT(0xE6B1BDE8BDA6E794A8E69FB4E6B2B9 USING utf8mb4));
INSERT INTO emission_fossil_fuel_collection_sub_scope(sub_scope_name,collection_scope_id,sort_order,description,created_by,updated_by)
SELECT CONVERT(0xE8BDA8E98193E8BDA6E794A8E69FB4E6B2B9 USING utf8mb4),s.id,3,'轨道车用柴油',1,1
FROM emission_fossil_fuel_collection_scope s WHERE s.scope_name=CONVERT(0xE88283E5AE81E58886E585ACE58FB8 USING utf8mb4)
  AND NOT EXISTS(SELECT 1 FROM emission_fossil_fuel_collection_sub_scope x WHERE x.collection_scope_id=s.id AND x.sub_scope_name=CONVERT(0xE8BDA8E98193E8BDA6E794A8E69FB4E6B2B9 USING utf8mb4));
INSERT IGNORE INTO sys_carbon_scope_grant
  (scope_type,scope_id,node_type,node_id,node_name,dept_id,can_read,can_write,can_delegate,granted_by_dept_id,created_by,updated_by)
SELECT 'fossil',s.id,'sub_scope',ss.id,ss.sub_scope_name,d.dept_id,1,1,0,3035,1,1
FROM emission_fossil_fuel_collection_scope s
JOIN emission_fossil_fuel_collection_sub_scope ss ON ss.collection_scope_id=s.id
JOIN sys_dept d ON d.parent_id=3035 AND d.dept_name=CONVERT(0xE7BABFE8B7AFE7BBB4E68AA4E5B7A5E9989F USING utf8mb4)
WHERE s.scope_name=CONVERT(0xE88283E5AE81E58886E585ACE58FB8 USING utf8mb4) AND ss.sub_scope_name=CONVERT(0xE8BDA8E98193E8BDA6E794A8E69FB4E6B2B9 USING utf8mb4);

INSERT INTO sys_menu(menu_name,parent_id,order_num,path,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '采集范围分配',2000,66,'','F','1','0','carbon:scope:view','#','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='carbon:scope:view');
INSERT INTO sys_menu(menu_name,parent_id,order_num,path,menu_type,visible,status,perms,icon,create_by,create_time)
SELECT '采集范围授权',2000,67,'','F','1','0','carbon:scope:edit','#','admin',NOW()
WHERE NOT EXISTS(SELECT 1 FROM sys_menu WHERE perms='carbon:scope:edit');
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m ON m.perms IN('carbon:scope:view','carbon:scope:edit')
WHERE r.role_key IN('company_admin','region_admin') AND r.status='0' AND r.del_flag='0';

-- Department administrators and operational collectors can open the same
-- page; the node-level can_delegate flag still limits what they may assign.
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m ON m.perms IN('carbon:scope:view','carbon:scope:edit')
WHERE r.role_key IN('department_admin','electricity_collector') AND r.status='0' AND r.del_flag='0';

-- Narrow functional role for staff who maintain only their authorized electric meters.
SET @electricity_collector_role_id := COALESCE(
  (SELECT role_id FROM sys_role WHERE role_key='electricity_collector' LIMIT 1),
  (SELECT COALESCE(MAX(role_id),0)+1 FROM sys_role));
INSERT INTO sys_role(role_id,role_name,role_key,role_sort,data_scope,menu_check_strictly,dept_check_strictly,status,del_flag,create_by,create_time,remark)
SELECT @electricity_collector_role_id,'供电采集员','electricity_collector',90,'4',1,1,'0','0','admin',NOW(),'按授权站点和区间维护电表，不含模型、系统和日志权限'
WHERE NOT EXISTS(SELECT 1 FROM sys_role WHERE role_key='electricity_collector');
INSERT IGNORE INTO sys_role_menu(role_id,menu_id)
SELECT r.role_id,m.menu_id FROM sys_role r JOIN sys_menu m
  ON m.perms IN('carbon:params:view','carbon:params:collection:edit')
WHERE r.role_key='electricity_collector' AND r.status='0' AND r.del_flag='0';
COMMIT;
