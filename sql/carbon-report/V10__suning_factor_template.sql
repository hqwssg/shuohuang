-- Idempotent: 为肃宁核算模板挂上因子模板，并从因子库写入气/油/柴/电/热缺省因子。
-- Does not invent factor values. Snapshots come from existing library rows.

SET NAMES utf8mb4;
USE carbon_emissions;

-- Live JPA created this column as DECIMAL(38,2); restore 6 decimal places so 0.5306 is not stored as 0.53.
ALTER TABLE emission_default_factor
  MODIFY COLUMN factor_value DECIMAL(18,6) NULL COMMENT '所选因子值（快照）';

INSERT INTO emission_factor_template (
  template_name, template_description, is_shared, status, created_by, created_at, updated_at
)
SELECT
  '肃宁2025报告因子模板',
  '肃宁分公司报告用因子：天然气/汽油/柴油取化石燃料库，电力取2023年全国平均，热力取全国缺省0.11。',
  1, 1, 1, NOW(6), NOW(6)
WHERE NOT EXISTS (
  SELECT 1 FROM emission_factor_template WHERE template_name = '肃宁2025报告因子模板'
);

SET @ft_id := (SELECT id FROM emission_factor_template WHERE template_name = '肃宁2025报告因子模板' LIMIT 1);

UPDATE emission_template
SET factor_template_id = @ft_id,
    updated_at = NOW()
WHERE id = 501
  AND (factor_template_id IS NULL OR factor_template_id <> @ft_id);

SET @ff_ng := (SELECT id FROM emission_fossil_fuel_emission_factor WHERE emission_factor_name = '天然气排放因子' LIMIT 1);
SET @ff_g  := (SELECT id FROM emission_fossil_fuel_emission_factor WHERE emission_factor_name = '汽油排放因子' LIMIT 1);
SET @ff_d  := (SELECT id FROM emission_fossil_fuel_emission_factor WHERE emission_factor_name = '柴油排放因子' LIMIT 1);
SET @el    := (SELECT id FROM emission_electricity_carbon_emission_factor WHERE factor_name = '2023年全国电力碳排放因子' LIMIT 1);
SET @th    := (SELECT id FROM emission_thermal_emission_factor WHERE emission_factor_name = '全国统一热力排放因子缺省值' LIMIT 1);

INSERT INTO emission_default_factor (
  template_id, subcategory_code, subcategory_name, factor_source, factor_id,
  factor_name, factor_value, factor_unit, factor_description, status, created_by, created_at, updated_at
)
SELECT @ft_id, 'FF_NG', '天然气', 'FOSSIL', @ff_ng,
       f.emission_factor_name, f.emission_factor, f.factor_unit, f.description, 1, 1, NOW(6), NOW(6)
FROM emission_fossil_fuel_emission_factor f
WHERE f.id = @ff_ng
  AND NOT EXISTS (
    SELECT 1 FROM emission_default_factor d
    WHERE d.template_id = @ft_id AND d.subcategory_code = 'FF_NG'
  );

INSERT INTO emission_default_factor (
  template_id, subcategory_code, subcategory_name, factor_source, factor_id,
  factor_name, factor_value, factor_unit, factor_description, status, created_by, created_at, updated_at
)
SELECT @ft_id, 'FF_G', '汽油', 'FOSSIL', @ff_g,
       f.emission_factor_name, f.emission_factor, f.factor_unit, f.description, 1, 1, NOW(6), NOW(6)
FROM emission_fossil_fuel_emission_factor f
WHERE f.id = @ff_g
  AND NOT EXISTS (
    SELECT 1 FROM emission_default_factor d
    WHERE d.template_id = @ft_id AND d.subcategory_code = 'FF_G'
  );

INSERT INTO emission_default_factor (
  template_id, subcategory_code, subcategory_name, factor_source, factor_id,
  factor_name, factor_value, factor_unit, factor_description, status, created_by, created_at, updated_at
)
SELECT @ft_id, 'FF_D', '柴油', 'FOSSIL', @ff_d,
       f.emission_factor_name, f.emission_factor, f.factor_unit, f.description, 1, 1, NOW(6), NOW(6)
FROM emission_fossil_fuel_emission_factor f
WHERE f.id = @ff_d
  AND NOT EXISTS (
    SELECT 1 FROM emission_default_factor d
    WHERE d.template_id = @ft_id AND d.subcategory_code = 'FF_D'
  );

INSERT INTO emission_default_factor (
  template_id, subcategory_code, subcategory_name, factor_source, factor_id,
  factor_name, factor_value, factor_unit, factor_description, status, created_by, created_at, updated_at
)
SELECT @ft_id, 'PE_PF', '生产设施用电', 'ELECTRICITY', @el,
       e.factor_name, e.factor_value, e.unit, e.description, 1, 1, NOW(6), NOW(6)
FROM emission_electricity_carbon_emission_factor e
WHERE e.id = @el
  AND NOT EXISTS (
    SELECT 1 FROM emission_default_factor d
    WHERE d.template_id = @ft_id AND d.subcategory_code = 'PE_PF'
  );

INSERT INTO emission_default_factor (
  template_id, subcategory_code, subcategory_name, factor_source, factor_id,
  factor_name, factor_value, factor_unit, factor_description, status, created_by, created_at, updated_at
)
SELECT @ft_id, 'EL', '电力', 'ELECTRICITY', @el,
       e.factor_name, e.factor_value, e.unit, e.description, 1, 1, NOW(6), NOW(6)
FROM emission_electricity_carbon_emission_factor e
WHERE e.id = @el
  AND NOT EXISTS (
    SELECT 1 FROM emission_default_factor d
    WHERE d.template_id = @ft_id AND d.subcategory_code = 'EL'
  );

INSERT INTO emission_default_factor (
  template_id, subcategory_code, subcategory_name, factor_source, factor_id,
  factor_name, factor_value, factor_unit, factor_description, status, created_by, created_at, updated_at
)
SELECT @ft_id, 'PH_HD', '热力数据', 'THERMAL', @th,
       t.emission_factor_name, t.emission_factor, TRIM(t.unit), t.description, 1, 1, NOW(6), NOW(6)
FROM emission_thermal_emission_factor t
WHERE t.id = @th
  AND NOT EXISTS (
    SELECT 1 FROM emission_default_factor d
    WHERE d.template_id = @ft_id AND d.subcategory_code = 'PH_HD'
  );
