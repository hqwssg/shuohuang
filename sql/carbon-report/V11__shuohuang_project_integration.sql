-- Project-specific report integration for the existing Shuohuang organization.
-- Run after V1. It intentionally does not import the demo departments in V2.

SET NAMES utf8mb4;
USE carbon_emissions;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_dept_profile' AND COLUMN_NAME = 'default_equipment_json');
SET @ddl := IF(@col = 0, 'ALTER TABLE report_dept_profile ADD COLUMN default_equipment_json json NULL COMMENT ''默认用能设备'' AFTER default_authors', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_dept_profile' AND COLUMN_NAME = 'default_activity_prose_json');
SET @ddl := IF(@col = 0, 'ALTER TABLE report_dept_profile ADD COLUMN default_activity_prose_json json NULL COMMENT ''活动数据说明默认值'' AFTER default_equipment_json', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_dept_profile' AND COLUMN_NAME = 'default_workload_json');
SET @ddl := IF(@col = 0, 'ALTER TABLE report_dept_profile ADD COLUMN default_workload_json json NULL COMMENT ''工作量指标默认值'' AFTER default_activity_prose_json', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_dept_profile' AND COLUMN_NAME = 'default_chapter6_json');
SET @ddl := IF(@col = 0, 'ALTER TABLE report_dept_profile ADD COLUMN default_chapter6_json json NULL COMMENT ''第六章默认值'' AFTER default_workload_json', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- The current model has one enabled root subject. Keep each department isolated by
-- report dept_id and retain the regional allocation key for future metering data.
SET @subject_node_id := (
  SELECT id FROM emission_node
  WHERE parent_id IS NULL
  ORDER BY id
  LIMIT 1
);

INSERT INTO report_dept_subject
    (dept_id, subject_node_id, energy_allocation, enabled, create_by, create_time)
SELECT d.dept_id,
       @subject_node_id,
       CASE
         WHEN d.dept_id = 3000 THEN 'all'
         WHEN d.dept_id = 3010 OR d.ancestors LIKE '%,3010%' THEN 'yuanping'
         WHEN d.dept_id = 3020 OR d.ancestors LIKE '%,3020%' THEN 'suning'
         WHEN d.dept_id = 3030 OR d.ancestors LIKE '%,3030%' THEN 'jilong'
         ELSE CONCAT('dept-', d.dept_id)
       END,
       IF(@subject_node_id IS NULL, '0', '1'), 'admin', NOW()
FROM sys_dept d
WHERE d.status = '0' AND d.del_flag = '0'
ON DUPLICATE KEY UPDATE
    subject_node_id = VALUES(subject_node_id),
    energy_allocation = VALUES(energy_allocation),
    enabled = VALUES(enabled),
    update_by = 'admin',
    update_time = NOW();

INSERT INTO report_dept_profile
    (dept_id, legal_name, short_name, entity_code, carbon_department,
     default_accounting_method, create_by, create_time)
SELECT d.dept_id,
       CASE WHEN d.dept_id = 3000 THEN d.dept_name
            ELSE CONCAT('国能朔黄铁路发展有限责任公司', d.dept_name) END,
       d.dept_name,
       CONCAT('SH-', d.dept_id),
       CASE WHEN d.dept_id IN (3010, 3020) THEN '安全环保部' ELSE '安全环保管理部门' END,
       '依据陆上交通运输企业温室气体排放核算方法与报告指南进行核算。',
       'admin', NOW()
FROM sys_dept d
WHERE d.status = '0' AND d.del_flag = '0'
ON DUPLICATE KEY UPDATE
    legal_name = VALUES(legal_name),
    short_name = VALUES(short_name),
    entity_code = VALUES(entity_code),
    carbon_department = VALUES(carbon_department),
    update_by = 'admin',
    update_time = NOW();

-- Preserve the detailed defaults delivered with the new report module, mapped
-- to the real 肃宁分公司 department (3020) in this project.
UPDATE report_dept_profile
SET legal_name = '国能朔黄铁路发展有限责任公司肃宁分公司',
    short_name = '肃宁分公司',
    entity_code = 'suning',
    carbon_department = '安全环保监察部',
    business_description = '负责所辖铁路线路的日常保养、检修，以保障线路持续稳定运行。',
    company_overview = '国能朔黄铁路发展有限责任公司肃宁分公司隶属于朔黄铁路发展有限责任公司，承担所辖铁路区段的行车组织和工务、电务、供电、生活等设备设施的养护维修及管理任务。碳排放主管部门为安全环保监察部。',
    process_description = '主要依据铁路行业运行规范开展线路日常保养、检修以及工务、电务、供电和生活设施维护。',
    default_org_boundary = '报告边界覆盖肃宁分公司运营控制范围内生产设施产生的温室气体排放，包括食堂天然气、公务及生产车辆燃油、净购入电力和热力产生的排放。线路运输机车归属机辆分公司，不计入本分公司报告边界。',
    default_exclusion_note = '线路运输机车归属机辆分公司，不在肃宁分公司运营边界内。',
    default_equipment_json = CAST('[{"name":"线路维修车","category":"机械设备","quantity":12,"unit":"台","model":"GC-270","energy_type":"柴油","remark":"线路施工与检修作业车辆"},{"name":"办公用车","category":"车辆","quantity":8,"unit":"辆","model":"大众迈腾","energy_type":"汽油","remark":"日常行政出行用车"}]' AS JSON),
    default_workload_json = CAST('[{"name":"线路维修里程","unit":"km"},{"name":"检修作业天","unit":"天"}]' AS JSON),
    default_chapter6_json = CAST('{"emission_intensity":{"applicable":false,"not_applicable_note":"温室气体排放强度不适用。"}}' AS JSON),
    update_by = 'admin',
    update_time = NOW()
WHERE dept_id = 3020;

-- Keep factor precision compatible with values such as 0.530600.
SET @col_type := (SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emission_default_factor' AND COLUMN_NAME = 'factor_value');
SET @ddl := IF(@col_type IS NOT NULL AND @col_type <> 'decimal(18,6)', 'ALTER TABLE emission_default_factor MODIFY factor_value DECIMAL(18,6) NOT NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
