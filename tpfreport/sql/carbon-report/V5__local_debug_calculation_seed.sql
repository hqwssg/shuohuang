-- Local debug seed: 2025 annual successful calculation for 肃宁 (suring).
-- Safe to re-run. Does not drop business tables.

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
USE carbon_emissions;

CREATE TABLE IF NOT EXISTS emission_calculation_template (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  template_id BIGINT NOT NULL,
  calculation_cycle_start_date DATE,
  calculation_cycle_end_date DATE,
  calculation_start_time TIMESTAMP NULL,
  calculation_end_time TIMESTAMP NULL,
  status TINYINT DEFAULT 0,
  error_code INT DEFAULT 0,
  task_initiator TINYINT DEFAULT 1,
  initiator_id BIGINT DEFAULT 0,
  initiator_name VARCHAR(100),
  created_by BIGINT,
  updated_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS emission_calculation_node (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  calculation_template_id BIGINT NOT NULL,
  node_id BIGINT NOT NULL,
  name VARCHAR(200) NOT NULL,
  type_id INT NOT NULL,
  parent_id BIGINT,
  node_category VARCHAR(50),
  created_by BIGINT,
  updated_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS emission_collection_node_data (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  calculation_node_id BIGINT NOT NULL,
  collection_point_type TINYINT,
  collection_point_id BIGINT,
  emission_category VARCHAR(100),
  emission_subcategory VARCHAR(100),
  carbon_emission_factor DECIMAL(15,6),
  energy_measurement_value DECIMAL(18,6),
  carbon_emission DECIMAL(18,6),
  adjusted_energy_value DECIMAL(18,6),
  adjusted_carbon_emission DECIMAL(18,6),
  measurement_unit VARCHAR(50),
  created_by BIGINT,
  updated_by BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS emission_fossil_fuel_meter_info (
  id BIGINT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  energy_allocation VARCHAR(64)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS emission_meter_info (
  id BIGINT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  energy_allocation VARCHAR(64)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS emission_purchased_heat_meter_info (
  id BIGINT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  energy_allocation VARCHAR(64)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Admin uses 肃宁 so listCalculations / create see enabled mapping.
UPDATE sys_user SET dept_id = 301 WHERE user_name = 'admin';

INSERT INTO emission_node (id, name, type_id, parent_id, sort_order, created_by)
SELECT 1001, '肃宁分公司', 2, 1, 1, 1
WHERE NOT EXISTS (SELECT 1 FROM emission_node WHERE id = 1001);

INSERT INTO emission_node_info (node_id, node_code, short_name, include_in_calculation, node_category)
SELECT 1001, 'SN', '肃宁', 1, 'BRANCH'
WHERE NOT EXISTS (SELECT 1 FROM emission_node_info WHERE node_id = 1001);

UPDATE report_dept_subject
SET subject_node_id = 1001,
    enabled = '1',
    energy_allocation = 'suring',
    update_by = 'admin',
    update_time = NOW()
WHERE dept_id = 301;

INSERT INTO emission_template (id, name, description, created_by, enabled)
SELECT 501, '肃宁2025年度核算模板', '本地调试用核算模板', 1, 1
WHERE NOT EXISTS (SELECT 1 FROM emission_template WHERE id = 501);

INSERT INTO emission_fossil_fuel_meter_info (id, name, energy_allocation)
VALUES (91001, '肃宁天然气计量表', 'suring'),
       (91002, '肃宁柴油计量表', 'suring')
ON DUPLICATE KEY UPDATE name = VALUES(name), energy_allocation = VALUES(energy_allocation);

INSERT INTO emission_meter_info (id, name, energy_allocation)
VALUES (92001, '肃宁外购电表', 'suring')
ON DUPLICATE KEY UPDATE name = VALUES(name), energy_allocation = VALUES(energy_allocation);

INSERT INTO emission_purchased_heat_meter_info (id, name, energy_allocation)
VALUES (93001, '肃宁外购热表', 'suring')
ON DUPLICATE KEY UPDATE name = VALUES(name), energy_allocation = VALUES(energy_allocation);

INSERT INTO emission_calculation_template (
  id, template_id, calculation_cycle_start_date, calculation_cycle_end_date,
  calculation_start_time, calculation_end_time, status, error_code,
  task_initiator, initiator_name, created_by
)
SELECT 11, 501, '2025-01-01', '2025-12-31',
       '2026-01-10 09:00:00', '2026-01-10 09:30:00', 2, 0,
       2, 'admin', 1
WHERE NOT EXISTS (SELECT 1 FROM emission_calculation_template WHERE id = 11);

INSERT INTO emission_calculation_node (
  id, calculation_template_id, node_id, name, type_id, parent_id, node_category
)
SELECT 1101, 11, 1001, '肃宁分公司', 2, NULL, 'BRANCH'
WHERE NOT EXISTS (SELECT 1 FROM emission_calculation_node WHERE id = 1101);

INSERT INTO emission_collection_node_data (
  id, calculation_node_id, collection_point_type, collection_point_id,
  emission_category, emission_subcategory, measurement_unit,
  energy_measurement_value, carbon_emission, adjusted_energy_value
)
SELECT 21001, 1101, 2, 91001, '化石燃料', 'FF_NG', '10k_m3', 2.000000, 9.000000, 2.000000
WHERE NOT EXISTS (SELECT 1 FROM emission_collection_node_data WHERE id = 21001);

INSERT INTO emission_collection_node_data (
  id, calculation_node_id, collection_point_type, collection_point_id,
  emission_category, emission_subcategory, measurement_unit,
  energy_measurement_value, carbon_emission, adjusted_energy_value
)
SELECT 21002, 1101, 2, 91002, '化石燃料', 'FF_D', 't', 10.000000, 31.500000, 10.000000
WHERE NOT EXISTS (SELECT 1 FROM emission_collection_node_data WHERE id = 21002);

INSERT INTO emission_collection_node_data (
  id, calculation_node_id, collection_point_type, collection_point_id,
  emission_category, emission_subcategory, measurement_unit,
  energy_measurement_value, carbon_emission, adjusted_energy_value
)
SELECT 21003, 1101, 1, 92001, '电力', 'EL', 'MWh', 100.000000, 53.060000, 100.000000
WHERE NOT EXISTS (SELECT 1 FROM emission_collection_node_data WHERE id = 21003);

INSERT INTO emission_collection_node_data (
  id, calculation_node_id, collection_point_type, collection_point_id,
  emission_category, emission_subcategory, measurement_unit,
  energy_measurement_value, carbon_emission, adjusted_energy_value
)
SELECT 21004, 1101, 3, 93001, '热力', 'PH_HD', 'GJ', 50.000000, 5.500000, 50.000000
WHERE NOT EXISTS (SELECT 1 FROM emission_collection_node_data WHERE id = 21004);

SET FOREIGN_KEY_CHECKS = 1;
