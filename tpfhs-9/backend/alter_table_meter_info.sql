-- 修改电表信息表，添加9个新字段
ALTER TABLE sys_emission_meter_info
    ADD COLUMN is_cumulative TINYINT DEFAULT 0 COMMENT '是否累加量：1-是，0-否' AFTER parent_meter_id,
    ADD COLUMN is_mobile_source TINYINT DEFAULT 0 COMMENT '是否移动源：1-是，0-否' AFTER is_cumulative,
    ADD COLUMN measurement_unit VARCHAR(50) COMMENT '计量单位' AFTER is_mobile_source,
    ADD COLUMN data_source_system VARCHAR(100) COMMENT '数据来源系统' AFTER measurement_unit,
    ADD COLUMN billing_cycle_unit VARCHAR(20) COMMENT '非累加量计费周期单位：周、月、季度、年' AFTER data_source_system,
    ADD COLUMN billing_cycle_start_date VARCHAR(20) COMMENT '非累加量计费周期起始日期' AFTER billing_cycle_unit,
    ADD COLUMN billing_cycle_length SMALLINT DEFAULT 1 COMMENT '非累加量计费周期长度（按月计）' AFTER billing_cycle_start_date,
    ADD COLUMN accounting_scenario VARCHAR(100) COMMENT '核算场景' AFTER billing_cycle_length,
    ADD COLUMN energy_use_category VARCHAR(100) COMMENT '能耗用途分类' AFTER accounting_scenario;

-- 修改化石燃料用量信息表采集点表，添加9个新字段
ALTER TABLE sys_emission_fossil_fuel_meter_info
    ADD COLUMN is_cumulative TINYINT DEFAULT 0 COMMENT '是否累加量：1-是，0-否' AFTER parent_meter_id,
    ADD COLUMN is_mobile_source TINYINT DEFAULT 0 COMMENT '是否移动源：1-是，0-否' AFTER is_cumulative,
    ADD COLUMN measurement_unit VARCHAR(50) COMMENT '计量单位' AFTER is_mobile_source,
    ADD COLUMN data_source_system VARCHAR(100) COMMENT '数据来源系统' AFTER measurement_unit,
    ADD COLUMN billing_cycle_unit VARCHAR(20) COMMENT '非累加量计费周期单位：周、月、季度、年' AFTER data_source_system,
    ADD COLUMN billing_cycle_start_date VARCHAR(20) COMMENT '非累加量计费周期起始日期' AFTER billing_cycle_unit,
    ADD COLUMN billing_cycle_length SMALLINT DEFAULT 1 COMMENT '非累加量计费周期长度（按月计）' AFTER billing_cycle_start_date,
    ADD COLUMN accounting_scenario VARCHAR(100) COMMENT '核算场景' AFTER billing_cycle_length,
    ADD COLUMN energy_use_category VARCHAR(100) COMMENT '能耗用途分类' AFTER accounting_scenario;

-- 修改外购热能用量信息表采集点表，添加9个新字段
ALTER TABLE sys_emission_purchased_heat_meter_info
    ADD COLUMN is_cumulative TINYINT DEFAULT 0 COMMENT '是否累加量：1-是，0-否' AFTER parent_meter_id,
    ADD COLUMN is_mobile_source TINYINT DEFAULT 0 COMMENT '是否移动源：1-是，0-否' AFTER is_cumulative,
    ADD COLUMN measurement_unit VARCHAR(50) COMMENT '计量单位' AFTER is_mobile_source,
    ADD COLUMN data_source_system VARCHAR(100) COMMENT '数据来源系统' AFTER measurement_unit,
    ADD COLUMN billing_cycle_unit VARCHAR(20) COMMENT '非累加量计费周期单位：周、月、季度、年' AFTER data_source_system,
    ADD COLUMN billing_cycle_start_date VARCHAR(20) COMMENT '非累加量计费周期起始日期' AFTER billing_cycle_unit,
    ADD COLUMN billing_cycle_length SMALLINT DEFAULT 1 COMMENT '非累加量计费周期长度（按月计）' AFTER billing_cycle_start_date,
    ADD COLUMN accounting_scenario VARCHAR(100) COMMENT '核算场景' AFTER billing_cycle_length,
    ADD COLUMN energy_use_category VARCHAR(100) COMMENT '能耗用途分类' AFTER accounting_scenario;
