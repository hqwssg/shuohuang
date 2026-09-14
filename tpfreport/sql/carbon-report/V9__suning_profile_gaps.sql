-- Idempotent: 肃宁档案补概况长文、第三章说明、工作量指标名、第6章强度不适用；调试计量表补型号。
-- Does not invent meter accuracy. Does not write report_* rows.

SET NAMES utf8mb4;
USE carbon_emissions;

-- profile JSON columns
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_dept_profile' AND COLUMN_NAME = 'default_activity_prose_json');
SET @ddl := IF(@col = 0, 'ALTER TABLE report_dept_profile ADD COLUMN default_activity_prose_json json NULL COMMENT ''第三章活动说明默认'' AFTER default_equipment_json', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_dept_profile' AND COLUMN_NAME = 'default_workload_json');
SET @ddl := IF(@col = 0, 'ALTER TABLE report_dept_profile ADD COLUMN default_workload_json json NULL COMMENT ''工作量指标名默认'' AFTER default_activity_prose_json', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'report_dept_profile' AND COLUMN_NAME = 'default_chapter6_json');
SET @ddl := IF(@col = 0, 'ALTER TABLE report_dept_profile ADD COLUMN default_chapter6_json json NULL COMMENT ''第六章强度默认'' AFTER default_workload_json', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- local debug meter model columns (production tables already have meter_model)
SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emission_fossil_fuel_meter_info' AND COLUMN_NAME = 'meter_model');
SET @ddl := IF(@col = 0, 'ALTER TABLE emission_fossil_fuel_meter_info ADD COLUMN meter_model VARCHAR(30) NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emission_meter_info' AND COLUMN_NAME = 'meter_model');
SET @ddl := IF(@col = 0, 'ALTER TABLE emission_meter_info ADD COLUMN meter_model VARCHAR(30) NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @col := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'emission_purchased_heat_meter_info' AND COLUMN_NAME = 'meter_model');
SET @ddl := IF(@col = 0, 'ALTER TABLE emission_purchased_heat_meter_info ADD COLUMN meter_model VARCHAR(30) NULL', 'SELECT 1');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO report_dept_profile (dept_id, legal_name, short_name, entity_code, carbon_department, create_by, create_time)
SELECT 301, '国能朔黄铁路发展有限责任公司肃宁分公司', '肃宁分公司', 'suning', '安全环保监察部', 'admin', NOW()
WHERE NOT EXISTS (SELECT 1 FROM report_dept_profile WHERE dept_id = 301);

UPDATE report_dept_profile
SET
  business_description = '负责所辖铁路线路的日常保养、检修，以保障线路持续稳定运行。',
  company_overview = '国能朔黄铁路发展有限责任公司肃宁分公司（以下简称“肃宁分公司”）成立于2002年1月1日，隶属于朔黄铁路发展有限责任公司。主要承担朔黄铁路三汲站至黄骅港站、黄万铁路黄骅南站至神港站两个区段的行车组织和工务、电务、供电、生活等设备设施的养护维修及管理任务。管内线路总长为418公里，其中朔黄铁路自西向东途经平山、灵寿、行唐、定州、安国、博野、蠡县，肃宁、河间、沧县、沧州、黄骅等县（市），在定州西站与京广铁路交汇，在肃宁北站与京九铁路接轨，在黄骅南站与黄万铁路相连，向东至渤海湾的黄骅港；黄万铁路自南向北途经河北省黄骅市，在K69+303（大港水库与万家码头区间）处进入天津市滨海新区，到万家码头车站后经天津地铁线路至神港站，到达天津港神华天津煤炭码头。\n肃宁分公司本部设9个部门，分别是：综合管理部（党委办公室）、党建工作部、纪委办公室、生产技术部、安全环保监察部、保障服务部、人力资源部、科技部、经济核算部；设1个直属机构：生产应急指挥中心。基层设中心站8个、检修中心3个，分别是：灵寿站、定州西站、肃宁北站、沧州西站、黄骅南站、黄骅港站、北港农场站、神港站、线桥检修中心、通信检修中心、供电移动检修中心。碳排放主管部门为安全环保监察部。',
  process_description = '肃宁分公司的主要工作任务是依据各项铁路行业运行规范，负责所辖418公里线路的日常保养、检修，以保障线路的持续稳定运行。工作范围覆盖朔黄铁路三汲站至黄骅港站、黄万铁路黄骅南站至神港站两个区段的工务、电务、供电及生活等设备设施养护维修。',
  default_activity_prose_json = CAST('{
    "natural_gas": {
      "usage": "天然气用于食堂灶具",
      "metering_method": "由流量计连续计量",
      "source_documents": ["沿线各站食堂天然气统计表"],
      "unit_note": "单位为m3",
      "standard_condition_note": "由于未计量温度和压力等参数，天然气使用条件接近标况，因此按照标况计算",
      "change_note": "报告期内天然气消耗较上年基本稳定"
    },
    "liquid_fuel": {
      "usage": "公务车使用汽油，生产车辆和轨道作业车使用柴油",
      "metering_method": "由加油站或加油车的流量计每次计量",
      "source_documents": ["汽车汽油和柴油消耗统计表", "轨道车用柴油1-12月汇总"]
    },
    "electricity": {
      "non_fossil_power_note": "不存在使用非化石能源电量的情况",
      "grid_source": "从华北电网购入电力",
      "source_documents": ["电费报表-内转表"],
      "settlement_note": "由于朔黄公司及子分公司外购电量全部由肃宁分公司结算，结算发票为所有企业的合计外购电量，无法提供肃分公司单独的结算发票"
    },
    "heat": {
      "contract_note": "和当地热力公司签订协议",
      "payment_method": "按面积收费或按热量收费方式缴纳热费",
      "estimation_method": "无计量热量时按照供暖面积折算热力进行估算",
      "no_resale_note": "没有转供热力的情况",
      "source_documents": ["热力结算单"]
    }
  }' AS JSON),
  default_workload_json = CAST('[
    {"name":"线路维修里程","unit":"km"},
    {"name":"检修作业天","unit":"天"}
  ]' AS JSON),
  default_chapter6_json = CAST('{
    "emission_intensity": {
      "applicable": false,
      "not_applicable_note": "温室气体排放强度不涉及。"
    }
  }' AS JSON),
  update_by = 'admin',
  update_time = NOW()
WHERE dept_id = 301;

UPDATE emission_fossil_fuel_meter_info SET meter_model = 'LWQ-50' WHERE id = 91001 AND (meter_model IS NULL OR meter_model = '');
UPDATE emission_fossil_fuel_meter_info SET meter_model = 'LC-40' WHERE id = 91002 AND (meter_model IS NULL OR meter_model = '');
UPDATE emission_meter_info SET meter_model = 'DTZ341' WHERE id = 92001 AND (meter_model IS NULL OR meter_model = '');
UPDATE emission_purchased_heat_meter_info SET meter_model = 'RLB-80' WHERE id = 93001 AND (meter_model IS NULL OR meter_model = '');
