-- Idempotent: fill 肃宁 (dept 301) organization-boundary defaults from gold sample.
-- Does not insert a second profile row.

SET NAMES utf8mb4;
USE carbon_emissions;

INSERT INTO report_dept_profile (dept_id, legal_name, short_name, entity_code, carbon_department, create_by, create_time)
SELECT 301, '国能朔黄铁路发展有限责任公司肃宁分公司', '肃宁分公司', 'suning', '安全环保监察部', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM report_dept_profile WHERE dept_id = 301);

UPDATE report_dept_profile
SET
  default_org_boundary = '根据《陆上运输企业温室气体排放核算方法与报告指南（试行）》，报告主体应以独立法人企业或视同法人的独立核算单位为企业边界，核算和报告在运营上受其控制的所有生产设施产生的温室气体排放。肃宁分公司属于铁路运输企业，核算边界包括所辖8个站及3个检测中心所产生的排放，涵盖食堂天然气、公务用车汽油柴油、轨道作业车柴油等化石燃料燃烧CO2排放，以及净购入电力和热力隐含排放。线路运输机车归属机辆分公司，不在肃宁分公司运营边界内，不计入碳排放核算边界。',
  default_accounting_method = '依据陆上交通运输企业温室气体排放核算方法与报告指南进行核算。',
  default_exclusion_note = '线路运输机车归属机辆分公司，不在肃宁分公司运营边界内，不计入碳排放核算边界。',
  update_by = 'admin',
  update_time = NOW()
WHERE dept_id = 301;
