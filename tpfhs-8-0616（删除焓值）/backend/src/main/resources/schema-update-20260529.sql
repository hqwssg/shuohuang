/*
-- 添加新字段到 emission_node_config 表
ALTER TABLE emission_node_config
ADD COLUMN accounting_scenario VARCHAR(100) DEFAULT NULL COMMENT '核算场景',
ADD COLUMN energy_use VARCHAR(100) DEFAULT NULL COMMENT '能耗用途',
ADD COLUMN is_cumulative VARCHAR(10) DEFAULT 'true' COMMENT '是否累计量',
ADD COLUMN is_mobile_source VARCHAR(10) DEFAULT 'false' COMMENT '是否移动源',
ADD COLUMN measurement_unit VARCHAR(50) DEFAULT NULL COMMENT '计量单位';

-- 插入核算场景数据字典（如果不存在）
INSERT INTO emission_data_dict (dict_code, dict_name, description) VALUES
('accounting_scenario', '核算场景', '碳排放来源归类场景')
ON DUPLICATE KEY UPDATE dict_name = VALUES(dict_name), description = VALUES(description);

-- 获取核算场景字典ID并插入字典项
SET @dict_id = (SELECT id FROM emission_data_dict WHERE dict_code = 'accounting_scenario');

INSERT INTO emission_data_dict_item (dict_id, item_code, item_value, sort_order, status) VALUES
(@dict_id, 'TRACTION_POWER', '牵引变电所/牵引供电', 1, 1),
(@dict_id, 'TRAIN_OPERATION', '列车运行', 2, 1),
(@dict_id, 'DISPATCH_COMM', '行车调度/通信指挥', 3, 1),
(@dict_id, 'VEHICLE_REPAIR', '车辆维修/机修车间', 4, 1),
(@dict_id, 'LINE_MAINTENANCE', '线路维护保养', 5, 1),
(@dict_id, 'STATION_SYSTEM', '场站系统', 6, 1),
(@dict_id, 'GARAGE', '车库', 7, 1),
(@dict_id, 'WORKSHOP_BATHROOM', '车间浴室', 8, 1),
(@dict_id, 'OFFICE_BUILDING', '办公楼', 9, 1),
(@dict_id, 'STAFF_CANTEEN', '职工食堂', 10, 1),
(@dict_id, 'STAFF_DORMITORY', '职工宿舍', 11, 1),
(@dict_id, 'INTERNAL_VEHICLE', '内部运营车辆', 12, 1),
(@dict_id, 'INTERNAL_VEHICLE_SINGLE', '内部运营车辆（单台车）', 13, 1),
(@dict_id, 'WASTE_DISPOSAL', '废弃物处理场所', 14, 1),
(@dict_id, 'OTHER_SCENARIO', '其他场景', 15, 1)
ON DUPLICATE KEY UPDATE item_value = VALUES(item_value), sort_order = VALUES(sort_order), status = VALUES(status);

-- 插入能耗用途数据字典（如果不存在）
INSERT INTO emission_data_dict (dict_code, dict_name, description) VALUES
('energy_use', '能耗用途', '能源消耗用途分类')
ON DUPLICATE KEY UPDATE dict_name = VALUES(dict_name), description = VALUES(description);

-- 获取能耗用途字典ID并插入字典项
SET @dict_id = (SELECT id FROM emission_data_dict WHERE dict_code = 'energy_use');

INSERT INTO emission_data_dict_item (dict_id, item_code, item_value, sort_order, status) VALUES
(@dict_id, 'LIGHTING', '照明', 1, 1),
(@dict_id, 'AIR_CONDITIONING', '空调', 2, 1),
(@dict_id, 'HEATING', '取暖', 3, 1),
(@dict_id, 'HEAT_SUPPLY', '供热', 4, 1),
(@dict_id, 'ELEVATOR', '电梯', 5, 1),
(@dict_id, 'WATER_PUMP', '水泵', 6, 1),
(@dict_id, 'VEHICLE', '汽车', 7, 1),
(@dict_id, 'EQUIPMENT', '设备用能', 8, 1),
(@dict_id, 'TRAIN_OPERATION_ENERGY', '列车运行用能', 9, 1),
(@dict_id, 'MIXED_METERING', '混合用能计量', 10, 1),
(@dict_id, 'OTHER_USE', '其他', 11, 1)
ON DUPLICATE KEY UPDATE item_value = VALUES(item_value), sort_order = VALUES(sort_order), status = VALUES(status);

-- 创建数据来源系统表
CREATE TABLE IF NOT EXISTS emission_data_source_system (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    system_name VARCHAR(100) NOT NULL COMMENT '数据来源系统名称',
    description VARCHAR(500) COMMENT '说明',
    pinyin_code VARCHAR(50) COMMENT '拼音首字母编码',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_system_name (system_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据来源系统表';

-- 添加数据来源系统字段到排放节点配置表
ALTER TABLE emission_node_config
ADD COLUMN data_source_system VARCHAR(100) DEFAULT NULL COMMENT '数据来源系统';

-- 添加获取方式字段到排放节点配置表
ALTER TABLE emission_node_config
ADD COLUMN acquisition_method TEXT COMMENT '获取方式(API接口、请求参数等信息)';
*/
