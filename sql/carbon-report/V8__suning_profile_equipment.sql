-- Idempotent: add default equipment JSON on the dept profile and seed 肃宁 from gold sample.
-- Does not insert a second profile row. Does not write report_equipment.

SET NAMES utf8mb4;
USE carbon_emissions;

SET @col_exists := (
  SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'report_dept_profile'
    AND COLUMN_NAME = 'default_equipment_json'
);
SET @ddl := IF(@col_exists = 0,
  'ALTER TABLE report_dept_profile ADD COLUMN default_equipment_json json NULL COMMENT ''分公司默认用能设备'' AFTER default_authors',
  'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

INSERT INTO report_dept_profile (dept_id, legal_name, short_name, entity_code, carbon_department, create_by, create_time)
SELECT 301, '国能朔黄铁路发展有限责任公司肃宁分公司', '肃宁分公司', 'suning', '安全环保监察部', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM report_dept_profile WHERE dept_id = 301);

UPDATE report_dept_profile
SET
  default_equipment_json = CAST('[
    {"name":"线路维修车","category":"机械设备","quantity":12,"unit":"台","model":"GC-270","energy_type":"柴油","remark":"线路施工与检修作业车辆"},
    {"name":"办公用车","category":"车辆","quantity":8,"unit":"辆","model":"大众迈腾","energy_type":"汽油","remark":"日常行政出行用车"}
  ]' AS JSON),
  update_by = 'admin',
  update_time = NOW()
WHERE dept_id = 301;
