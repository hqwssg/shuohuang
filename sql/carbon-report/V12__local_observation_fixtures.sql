-- Local observation fixtures for the carbon model and report pages.
-- Idempotent: only rows owned by this fixture are inserted or updated.
-- It does not rename or remove any existing schema tables.

SET NAMES utf8mb4;
USE carbon_emissions;

SET @fixture_user_id := COALESCE((SELECT user_id FROM sys_user WHERE user_name = 'admin' LIMIT 1), 1);
SET @fixture_dept_id := COALESCE((SELECT dept_id FROM sys_user WHERE user_name = 'admin' LIMIT 1), 3000);

-- Existing tpfhs-9 databases may have been created from an older schema where
-- the factor snapshot was DECIMAL(38,2). Keep the calculation precision at 6.
ALTER TABLE emission_default_factor
    MODIFY COLUMN factor_value DECIMAL(18,6) DEFAULT NULL;

-- 1. Factor template and its PE_PF default factor.
INSERT INTO emission_factor_template
    (template_name, template_description, is_shared, status, created_by, updated_by)
SELECT '联调示例-肃宁分公司因子模板',
       '用于观察模型设置、缺省因子回退与年度报告生成流程。',
       1, 1, @fixture_user_id, @fixture_user_id
WHERE NOT EXISTS (
    SELECT 1 FROM emission_factor_template
    WHERE template_name = '联调示例-肃宁分公司因子模板'
);

SET @fixture_factor_template_id := (
    SELECT id FROM emission_factor_template
    WHERE template_name = '联调示例-肃宁分公司因子模板'
    ORDER BY id LIMIT 1
);

INSERT INTO emission_default_factor
    (template_id, subcategory_code, subcategory_name, factor_source,
     factor_name, factor_value, factor_unit, factor_description,
     remark, status, created_by, updated_by)
VALUES
    (@fixture_factor_template_id, 'PE_PF', '外购电力', 'ELECTRICITY',
     '联调示例-华北区域电力因子', 0.536600, 'tCO2/MWh',
     '示例因子，实际项目请按核验后的因子库维护。',
     '本地观测数据', 1, @fixture_user_id, @fixture_user_id)
ON DUPLICATE KEY UPDATE
    subcategory_name = VALUES(subcategory_name),
    factor_source = VALUES(factor_source),
    factor_name = VALUES(factor_name),
    factor_value = VALUES(factor_value),
    factor_unit = VALUES(factor_unit),
    factor_description = VALUES(factor_description),
    remark = VALUES(remark),
    status = VALUES(status),
    updated_by = VALUES(updated_by);

-- 2. Use the existing tpfhs-9 calculation template and attach the fixture factor set.
UPDATE emission_template
SET template_type = 2,
    enabled = 1,
    factor_template_id = @fixture_factor_template_id,
    task_config = '{"cycleType":"DAILY","executionTime":"00:00","startDate":"2025-01-01","endDate":"2099-12-31","delayDays":0}',
    check_result = 0,
    updated_by = @fixture_user_id
WHERE id = 2;

SET @fixture_template_id := 2;
SET @fixture_root_node_id := (
    SELECT id FROM emission_node
    WHERE template_id = @fixture_template_id AND type_id = 1
    ORDER BY id LIMIT 1
);

-- 3. Model tree: root -> calculation node -> collection node.
INSERT INTO emission_node
    (name, type_id, parent_id, template_id, source_node_id, sort_order, created_by, updated_by)
SELECT '肃宁分公司-年度示例', 2, @fixture_root_node_id, @fixture_template_id, 0, 10,
       @fixture_user_id, @fixture_user_id
WHERE NOT EXISTS (
    SELECT 1 FROM emission_node
    WHERE template_id = @fixture_template_id AND name = '肃宁分公司-年度示例'
);

SET @fixture_calc_node_id := (
    SELECT id FROM emission_node
    WHERE template_id = @fixture_template_id AND name = '肃宁分公司-年度示例'
    ORDER BY id LIMIT 1
);

INSERT INTO emission_node_info
    (node_id, node_code, short_name, include_in_calculation, node_category,
     unit_description, org_boundary_description, operation_boundary_description,
     created_by, updated_by)
SELECT @fixture_calc_node_id, 'SN-DEMO', '肃宁示例', 1, 'BRANCH',
       '分公司年度碳排放核算边界', '肃宁分公司及其生产辅助部门',
       '外购电力使用环节', @fixture_user_id, @fixture_user_id
WHERE NOT EXISTS (
    SELECT 1 FROM emission_node_info WHERE node_id = @fixture_calc_node_id
);

INSERT INTO emission_station_interval
    (name, pinyin_code, description, sort_order, status, created_by, updated_by)
SELECT '肃宁示例站区', 'SNDEMO', '本地联调示例站区', 10, 1, @fixture_user_id, @fixture_user_id
WHERE NOT EXISTS (SELECT 1 FROM emission_station_interval WHERE name = '肃宁示例站区');

SET @fixture_station_id := (
    SELECT id FROM emission_station_interval WHERE name = '肃宁示例站区' LIMIT 1
);

INSERT INTO emission_concentrator
    (name, pinyin_code, station_interval_id, concentrator_address, description,
     sort_order, status, created_by, updated_by)
SELECT '肃宁示例集中器', 'SNDEMO', @fixture_station_id, 'SN-DEMO-001',
       '本地联调示例集中器', 10, 1, @fixture_user_id, @fixture_user_id
WHERE NOT EXISTS (SELECT 1 FROM emission_concentrator WHERE name = '肃宁示例集中器');

SET @fixture_concentrator_id := (
    SELECT id FROM emission_concentrator WHERE name = '肃宁示例集中器' LIMIT 1
);

INSERT INTO emission_meter_info
    (name, pinyin_code, point_id, meter_address, meter_type, meter_model,
     purpose_description, parent_meter_id, is_cumulative, is_mobile_source,
     measurement_unit, data_source_system, billing_cycle_unit,
     billing_cycle_start_date, billing_cycle_length, emission_subcategory,
     energy_category_l1, energy_category_l2, energy_category_l3,
     energy_use_category, power_category, meter_reading_method,
     parent_child_relationship, is_virtual_meter, is_allocation_child,
     allocation_ratio, sort_order, status, created_by, updated_by)
SELECT '肃宁示例外购电表', 'SNDEMO', @fixture_concentrator_id, 'SN-METER-001',
       '电力表', 'DEMO-2025', '年度外购电力示例', 0, 1, 0, 'kWh',
       '本地示例数据', 2, 0, 1, '外购电力', 'ELECTRIC', 'PURCHASED',
       'GRID', '生产用电', 'GENERATION', 0, 1, 0, 0, 1.000000,
       10, 1, @fixture_user_id, @fixture_user_id
WHERE NOT EXISTS (SELECT 1 FROM emission_meter_info WHERE name = '肃宁示例外购电表');

SET @fixture_meter_id := (
    SELECT id FROM emission_meter_info WHERE name = '肃宁示例外购电表' LIMIT 1
);

INSERT INTO emission_node
    (name, type_id, parent_id, template_id, source_node_id, sort_order, created_by, updated_by)
SELECT '外购电力-年度采集示例', 3, @fixture_calc_node_id, @fixture_template_id, 0, 10,
       @fixture_user_id, @fixture_user_id
WHERE NOT EXISTS (
    SELECT 1 FROM emission_node
    WHERE template_id = @fixture_template_id AND name = '外购电力-年度采集示例'
);

SET @fixture_collection_node_id := (
    SELECT id FROM emission_node
    WHERE template_id = @fixture_template_id AND name = '外购电力-年度采集示例'
    ORDER BY id LIMIT 1
);

INSERT INTO emission_node_config
    (node_id, emission_category, emission_subcategory, carbon_emission_factor,
     carbon_emission_factor_description, collection_description, equipment_code,
     collection_point_type, collection_point_id, collection_point_status,
     created_by, updated_by)
SELECT @fixture_collection_node_id, '购入的电力', '外购电力', NULL,
       '故意留空，观察按核算模板回退缺省因子。',
       '读取年度电表首末累计值差额。', 'SN-DEMO-METER-001', 1,
       @fixture_meter_id, 1, @fixture_user_id, @fixture_user_id
WHERE NOT EXISTS (
    SELECT 1 FROM emission_node_config WHERE node_id = @fixture_collection_node_id
);

UPDATE emission_node_config
SET emission_category = '购入的电力',
    emission_subcategory = '外购电力',
    carbon_emission_factor = NULL,
    collection_point_type = 1,
    collection_point_id = @fixture_meter_id,
    collection_point_status = 1,
    updated_by = @fixture_user_id
WHERE node_id = @fixture_collection_node_id;

-- 4. Two successful cumulative readings: 41,000 - 1,000 = 40,000 kWh.
INSERT INTO emission_collection_record
    (collection_point_type, collection_point_id, reading_value, collection_time,
     collection_status, last_fetch_time, collection_point_name,
     emission_subcategory, measurement_unit, meter_reading_method, is_cumulative,
     billing_cycle_start_date, billing_cycle_end_date, created_by, updated_by)
SELECT 1, @fixture_meter_id, 1000.000000, '2025-01-01 12:00:00', 1,
       '2025-01-01 12:01:00', '肃宁示例外购电表', '外购电力', 'kWh', 0, 1,
       NULL, NULL, @fixture_user_id, @fixture_user_id
WHERE NOT EXISTS (
    SELECT 1 FROM emission_collection_record
    WHERE collection_point_type = 1 AND collection_point_id = @fixture_meter_id
      AND collection_time = '2025-01-01 12:00:00'
);

INSERT INTO emission_collection_record
    (collection_point_type, collection_point_id, reading_value, collection_time,
     collection_status, last_fetch_time, collection_point_name,
     emission_subcategory, measurement_unit, meter_reading_method, is_cumulative,
     billing_cycle_start_date, billing_cycle_end_date, created_by, updated_by)
SELECT 1, @fixture_meter_id, 41000.000000, '2025-12-31 12:00:00', 1,
       '2025-12-31 12:01:00', '肃宁示例外购电表', '外购电力', 'kWh', 0, 1,
       NULL, NULL, @fixture_user_id, @fixture_user_id
WHERE NOT EXISTS (
    SELECT 1 FROM emission_collection_record
    WHERE collection_point_type = 1 AND collection_point_id = @fixture_meter_id
      AND collection_time = '2025-12-31 12:00:00'
);

-- 5. A visible report draft with activity, workload, equipment and monitoring data.
INSERT INTO report_task
    (dept_id, title, template_code, period_type, report_year, period_start, period_end,
     subject_node_id, emission_template_id, factor_template_id, status, current_step,
     version, deleted_flag, entity_profile_json, organization_boundary_json,
     emission_boundary_notes_json, create_by, create_time, update_by, update_time)
SELECT @fixture_dept_id, '联调示例-肃宁分公司2025年度碳排放报告', 'suning', 'YEAR', 2025,
       '2025-01-01', '2025-12-31', @fixture_calc_node_id, @fixture_template_id,
       @fixture_factor_template_id, 'DRAFT', 'activity', 1, '0',
       JSON_OBJECT('legalName', '国能朔黄铁路发展有限责任公司肃宁分公司',
                   'shortName', '肃宁分公司', 'entityCode', 'SN-DEMO-2025'),
       JSON_OBJECT('boundary', '肃宁分公司及生产辅助部门'),
       JSON_OBJECT('notes', '示例报告：外购电力年度累计值差额 40000 kWh。'),
       'admin', NOW(), 'admin', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM report_task
    WHERE dept_id = @fixture_dept_id
      AND title = '联调示例-肃宁分公司2025年度碳排放报告'
      AND deleted_flag = '0'
);

SET @fixture_report_id := (
    SELECT id FROM report_task
    WHERE dept_id = @fixture_dept_id
      AND title = '联调示例-肃宁分公司2025年度碳排放报告'
      AND deleted_flag = '0'
    ORDER BY id LIMIT 1
);

UPDATE report_task
SET subject_node_id = @fixture_calc_node_id,
    emission_template_id = @fixture_template_id,
    factor_template_id = @fixture_factor_template_id,
    period_start = '2025-01-01',
    period_end = '2025-12-31',
    current_step = 'activity',
    update_by = 'admin',
    update_time = NOW()
WHERE id = @fixture_report_id;

INSERT INTO report_activity_electricity
    (dept_id, report_id, scope, quantity_mwh, source_name, month, data_origin,
     source_table, source_id, source_value, source_unit, normalized_value,
     normalized_unit, sort_order, create_by, create_time, update_by, update_time)
SELECT @fixture_dept_id, @fixture_report_id, 'purchased-electricity', 40.000000,
       '肃宁示例外购电表', NULL, 'AUTO', 'emission_collection_record',
       CAST(@fixture_meter_id AS CHAR), 40000.000000, 'kWh', 40.000000, 'MWh',
       10, 'admin', NOW(), 'admin', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM report_activity_electricity
    WHERE report_id = @fixture_report_id AND source_id = CAST(@fixture_meter_id AS CHAR)
);

INSERT INTO report_workload
    (dept_id, report_id, metric_name, quantity, unit, data_origin, sort_order,
     create_by, create_time, update_by, update_time)
SELECT @fixture_dept_id, @fixture_report_id, '年度运输周转量', 1250000.000000, '万吨公里',
       'MANUAL', 10, 'admin', NOW(), 'admin', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM report_workload
    WHERE report_id = @fixture_report_id AND metric_name = '年度运输周转量'
);

INSERT INTO report_equipment
    (dept_id, report_id, name, category, quantity, unit, model, energy_type, remark,
     data_origin, sort_order, create_by, create_time, update_by, update_time)
SELECT @fixture_dept_id, @fixture_report_id, '肃宁示例外购电表', '电力计量设备', 1, '台',
       'DEMO-2025', '电力', '年度示例计量设备', 'MANUAL', 10,
       'admin', NOW(), 'admin', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM report_equipment
    WHERE report_id = @fixture_report_id AND name = '肃宁示例外购电表'
);

INSERT INTO report_monitoring_device
    (dept_id, report_id, name, model, accuracy, location,
     calibration_frequency, measurement_range, data_origin, source_table, source_id,
     sort_order, create_by, create_time, update_by, update_time)
SELECT @fixture_dept_id, @fixture_report_id, '肃宁示例外购电表', 'DEMO-2025', '0.5级',
       '肃宁示例站区', '12个月', '0-50000 kWh', 'MANUAL',
       'emission_meter_info', CAST(@fixture_meter_id AS CHAR), 10,
       'admin', NOW(), 'admin', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM report_monitoring_device
    WHERE report_id = @fixture_report_id AND name = '肃宁示例外购电表'
);

-- Keep the template available for the model page, but do not let the local scheduler
-- create a new calculation every day at midnight.
UPDATE emission_template
SET enabled = 1,
    task_config = '{"cycleType":"DAILY","executionTime":"00:00","startDate":"2025-01-01","endDate":"2099-12-31","delayDays":0}'
WHERE id = @fixture_template_id;

SELECT 'fixture_factor_template_id' AS item, @fixture_factor_template_id AS id
UNION ALL SELECT 'fixture_calc_node_id', @fixture_calc_node_id
UNION ALL SELECT 'fixture_collection_node_id', @fixture_collection_node_id
UNION ALL SELECT 'fixture_meter_id', @fixture_meter_id
UNION ALL SELECT 'fixture_report_id', @fixture_report_id;
