-- Repair only values and metadata that were stored as literal ASCII '?' by an
-- older non-UTF-8 import. No rows are deleted and all unrelated JSON fields
-- and business columns are preserved.
SET NAMES utf8mb4;
START TRANSACTION;

-- Restore table and column comments. These are metadata only; table contents
-- remain untouched.
SET @comment := CONVERT(0xE68AA5E5918AE4BC81E4B89AE6A1A3E6A188 USING utf8mb4);
SET @ddl := CONCAT('ALTER TABLE report_dept_profile COMMENT = ', QUOTE(@comment));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @comment := CONVERT(0xE68AA5E5918AE983A8E997A8E4B88EE6A0B8E7AE97E4B8BBE4BD93E5AFB9E785A7 USING utf8mb4);
SET @ddl := CONCAT('ALTER TABLE report_dept_subject COMMENT = ', QUOTE(@comment));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @comment := CONVERT(0xE7A2B3E68E92E694BEE68AA5E5918AE4BBBBE58AA1 USING utf8mb4);
SET @ddl := CONCAT('ALTER TABLE report_task COMMENT = ', QUOTE(@comment));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @comment := CONVERT(0xE7A2B3E68E92E694BEE88A82E782B9E7BAA7E695B0E68DAEE69D83E99990 USING utf8mb4);
SET @ddl := CONCAT('ALTER TABLE sys_carbon_scope_grant COMMENT = ', QUOTE(@comment));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @comment := CONVERT(0xE9BB98E8AEA4E794A8E883BDE8AEBEE5A487 USING utf8mb4);
SET @ddl := CONCAT('ALTER TABLE report_dept_profile MODIFY COLUMN default_equipment_json JSON DEFAULT NULL COMMENT ', QUOTE(@comment));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @comment := CONVERT(0xE6B4BBE58AA8E695B0E68DAEE8AFB4E6988EE9BB98E8AEA4E580BC USING utf8mb4);
SET @ddl := CONCAT('ALTER TABLE report_dept_profile MODIFY COLUMN default_activity_prose_json JSON DEFAULT NULL COMMENT ', QUOTE(@comment));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @comment := CONVERT(0xE5B7A5E4BD9CE9878FE68C87E6A087E9BB98E8AEA4E580BC USING utf8mb4);
SET @ddl := CONCAT('ALTER TABLE report_dept_profile MODIFY COLUMN default_workload_json JSON DEFAULT NULL COMMENT ', QUOTE(@comment));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @comment := CONVERT(0xE7ACACE585ADE7ABA0E9BB98E8AEA4E580BC USING utf8mb4);
SET @ddl := CONCAT('ALTER TABLE report_dept_profile MODIFY COLUMN default_chapter6_json JSON DEFAULT NULL COMMENT ', QUOTE(@comment));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @comment := CONVERT(0xE5889BE5BBBAE4BABA4944 USING utf8mb4);
SET @ddl := CONCAT('ALTER TABLE sys_carbon_scope_grant MODIFY COLUMN created_by BIGINT DEFAULT NULL COMMENT ', QUOTE(@comment));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @ddl := CONCAT('ALTER TABLE sys_carbon_scope_grant MODIFY COLUMN updated_by BIGINT DEFAULT NULL COMMENT ', QUOTE(@comment));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @comment := CONVERT(0xE5889BE5BBBAE697B6E997B4 USING utf8mb4);
SET @ddl := CONCAT('ALTER TABLE sys_carbon_scope_grant MODIFY COLUMN created_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP COMMENT ', QUOTE(@comment));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;
SET @comment := CONVERT(0xE69BB4E696B0E697B6E997B4 USING utf8mb4);
SET @ddl := CONCAT('ALTER TABLE sys_carbon_scope_grant MODIFY COLUMN updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ', QUOTE(@comment));
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Restore business values without changing row identities or relationships.
UPDATE emission_fossil_fuel_collection_sub_scope
SET description = CASE id
  WHEN 9000 THEN CONVERT(0xE6B1BDE8BDA6E794A8E6B1BDE6B2B9 USING utf8mb4)
  WHEN 9001 THEN CONVERT(0xE6B1BDE8BDA6E794A8E69FB4E6B2B9 USING utf8mb4)
  WHEN 9002 THEN CONVERT(0xE8BDA8E98193E8BDA6E794A8E69FB4E6B2B9 USING utf8mb4)
END
WHERE id IN (9000, 9001, 9002) AND description REGEXP '^[?]+$';

SET @method := CONVERT(0xE4BE9DE68DAEE99986E4B88AE4BAA4E9809AE8BF90E8BE93E4BC81E4B89AE6B8A9E5AEA4E6B094E4BD93E68E92E694BEE6A0B8E7AE97E696B9E6B395E4B88EE68AA5E5918AE68C87E58D97E8BF9BE8A18CE6A0B8E7AE97E38082 USING utf8mb4);
UPDATE report_dept_profile
SET default_accounting_method = @method
WHERE default_accounting_method REGEXP '^[?]+$';

-- Use the canonical company name from the department profile for corrupted
-- JSON snapshots, retaining every other snapshot property.
UPDATE report_task t
JOIN report_dept_profile p ON p.dept_id = t.dept_id
SET t.metadata_json = JSON_SET(t.metadata_json, '$.organizationName', p.legal_name)
WHERE JSON_UNQUOTE(JSON_EXTRACT(t.metadata_json, '$.organizationName')) REGEXP '^[?]+$';

UPDATE report_generation_job
SET input_snapshot_json = JSON_SET(input_snapshot_json, '$.organization_boundary.accounting_method', @method)
WHERE JSON_UNQUOTE(JSON_EXTRACT(input_snapshot_json, '$.organization_boundary.accounting_method')) REGEXP '^[?]+$';

SET @permission_remark := CONVERT(0xE7A2B3E68E92E694BEE7BB86E7B292E5BAA6E6938DE4BD9CE69D83E99990 USING utf8mb4);
UPDATE sys_menu
SET remark = @permission_remark
WHERE perms IN ('carbon:report:add','carbon:report:download','carbon:report:edit',
                'carbon:report:import','carbon:report:list','carbon:report:profile:edit',
                'carbon:report:profile:query','carbon:report:query',
                'carbon:report:remove','carbon:report:validate')
  AND remark REGEXP '^[?]+$';

SET @company_name := CONVERT(0xE59BBDE883BDE69C94E9BB84E99381E8B7AFE58F91E5B195E69C89E99990E8B4A3E4BBBBE585ACE58FB8 USING utf8mb4);
UPDATE sys_oper_log
SET json_result = REPLACE(json_result, '????', @company_name),
    oper_param = REPLACE(oper_param, '????', @company_name)
WHERE json_result LIKE '%????%' OR oper_param LIKE '%????%';

COMMIT;
