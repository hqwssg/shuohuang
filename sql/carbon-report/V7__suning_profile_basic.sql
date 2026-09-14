-- Idempotent: fill 肃宁 (dept 301) basic-info defaults from gold sample.
-- Does not insert a second profile row. Does not set report_number.

SET NAMES utf8mb4;
USE carbon_emissions;

INSERT INTO report_dept_profile (dept_id, legal_name, short_name, entity_code, carbon_department, create_by, create_time)
SELECT 301, '国能朔黄铁路发展有限责任公司肃宁分公司', '肃宁分公司', 'suning', '安全环保监察部', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM report_dept_profile WHERE dept_id = 301);

UPDATE report_dept_profile
SET
  default_compiler = '龙源（北京）碳资产管理技术有限公司',
  default_approver = '王建军',
  default_reviewer = '李国平',
  default_checker = '陈志强',
  default_validator = '赵明华',
  default_authors = '刘洋、周敏',
  update_by = 'admin',
  update_time = NOW()
WHERE dept_id = 301;
