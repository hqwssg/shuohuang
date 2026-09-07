-- ============================================================
-- 更新脚本 2026-09-04
-- 变更：factor_template_id（碳排放因子模版ID）从 emission_factor_template
--       表迁移至 emission_template（核算模版）表。
-- 说明：该字段表达"某核算模版使用某个碳排放因子模版"，应挂在
--       emission_template 上，外键指向 emission_factor_template.id。
-- 幂等：所有变更均先判断列/索引是否存在，可重复执行。
-- ============================================================

-- 1. 若 emission_factor_template 上误建了 idx_factor_template_id 索引，先删除
SET @exist := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE table_schema = DATABASE() AND table_name = 'emission_factor_template'
    AND index_name = 'idx_factor_template_id');
SET @sql := IF(@exist > 0,
    'ALTER TABLE emission_factor_template DROP INDEX idx_factor_template_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2. 若 emission_factor_template 上误建了 factor_template_id 列，删除
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE table_schema = DATABASE() AND table_name = 'emission_factor_template'
    AND column_name = 'factor_template_id');
SET @sql := IF(@exist > 0,
    'ALTER TABLE emission_factor_template DROP COLUMN factor_template_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3. 在 emission_template 表上增加 factor_template_id 列（若不存在）
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE table_schema = DATABASE() AND table_name = 'emission_template'
    AND column_name = 'factor_template_id');
SET @sql := IF(@exist = 0,
    'ALTER TABLE emission_template ADD COLUMN factor_template_id BIGINT COMMENT ''碳排放因子模版ID（外键，指向 emission_factor_template.id，NULL表示未关联。仅核算模版使用）'' AFTER task_config',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4. 在 emission_template 表上增加 factor_template_id 索引（若不存在）
SET @exist := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE table_schema = DATABASE() AND table_name = 'emission_template'
    AND index_name = 'idx_factor_template_id');
SET @sql := IF(@exist = 0,
    'ALTER TABLE emission_template ADD INDEX idx_factor_template_id (factor_template_id) COMMENT ''因子模版ID索引''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 第二部分：模版校验功能字段（2026-09-04）
-- 为 emission_template 增加：校验结果、校验时间、校验告警信息（富文本）
-- 幂等：所有变更均先判断列是否存在，可重复执行。
-- ============================================================

-- 5. 增加 check_result 校验结果列（若不存在）
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE table_schema = DATABASE() AND table_name = 'emission_template'
    AND column_name = 'check_result');
SET @sql := IF(@exist = 0,
    'ALTER TABLE emission_template ADD COLUMN check_result TINYINT DEFAULT 0 COMMENT ''模版校验结果：0-未检查，1-完全正确，2-正确（存在提示信息），3-存在告警，4-存在错误。模版被修改后自动重置为0'' AFTER factor_template_id',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 6. 增加 check_time 校验时间列（若不存在）
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE table_schema = DATABASE() AND table_name = 'emission_template'
    AND column_name = 'check_time');
SET @sql := IF(@exist = 0,
    'ALTER TABLE emission_template ADD COLUMN check_time DATETIME NULL COMMENT ''最近一次模版校验时间'' AFTER check_result',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 7. 增加 check_message 校验告警信息列（若不存在）
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE table_schema = DATABASE() AND table_name = 'emission_template'
    AND column_name = 'check_message');
SET @sql := IF(@exist = 0,
    'ALTER TABLE emission_template ADD COLUMN check_message TEXT COMMENT ''最近一次校验结果详情（富文本HTML：错误-红#F56C6C、告警-橙#E6A23C、提示-蓝#409EFF、通过-绿#67C23A）'' AFTER check_time',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
