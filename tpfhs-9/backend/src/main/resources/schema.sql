-- 数据字典表：存储系统中使用的数据字典分类
CREATE TABLE IF NOT EXISTS emission_data_dict (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    dict_code VARCHAR(50) NOT NULL UNIQUE COMMENT '字典编码',
    dict_name VARCHAR(100) NOT NULL COMMENT '字典名称',
    description VARCHAR(500) COMMENT '字典描述',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status INT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典表';

-- 数据字典项表：存储数据字典的具体选项
CREATE TABLE IF NOT EXISTS emission_data_dict_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    dict_id BIGINT NOT NULL COMMENT '字典ID，关联emission_data_dict表',
    item_code VARCHAR(100) NOT NULL COMMENT '字典项编码',
    item_value VARCHAR(200) NOT NULL COMMENT '字典项值',
    parent_code VARCHAR(100) COMMENT '父级编码，用于层级字典',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status INT DEFAULT 1 COMMENT '状态：1-启用，0-禁用',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (dict_id) REFERENCES emission_data_dict(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典项表';

-- 节点类型表：定义排放节点的类型
CREATE TABLE IF NOT EXISTS emission_node_type (
    id INT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    type_name VARCHAR(100) NOT NULL UNIQUE COMMENT '类型名称',
    can_have_children BOOLEAN DEFAULT TRUE COMMENT '是否可以有子节点',
    description VARCHAR(500) COMMENT '类型描述',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='节点类型表';

-- 排放节点表：碳排放核算系统的节点树结构
CREATE TABLE IF NOT EXISTS emission_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '节点名称',
    type_id INT NOT NULL COMMENT '节点类型ID，关联emission_node_type表',
    parent_id BIGINT COMMENT '父节点ID',
    locomotive_type VARCHAR(100) COMMENT '机车类型（仅运输节点使用）',
    template_id BIGINT COMMENT '模板ID，为空则为模板实例化的节点',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (type_id) REFERENCES emission_node_type(id),
    FOREIGN KEY (parent_id) REFERENCES emission_node(id) ON DELETE CASCADE,
    FOREIGN KEY (template_id) REFERENCES emission_template(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排放节点表';

-- 排放节点配置表：存储排放数据采集点和运输节点的配置信息
CREATE TABLE IF NOT EXISTS emission_node_config (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    node_id BIGINT NOT NULL COMMENT '节点ID，关联emission_node表',
    statistical_caliber VARCHAR(100) COMMENT '统计口径类型',
    emission_category VARCHAR(100) COMMENT '排放数据大类',
    emission_subcategory VARCHAR(100) COMMENT '排放数据小类',
    carbon_emission_factor DECIMAL(15,6) COMMENT '碳排放因子',
    carbon_emission_factor_description TEXT COMMENT '碳排放因子说明',
    data_source VARCHAR(50) COMMENT '数据来源',
    accounting_scenario VARCHAR(100) COMMENT '核算场景',
    energy_use VARCHAR(100) COMMENT '用途',
    is_cumulative VARCHAR(10) DEFAULT 'true' COMMENT '是否累计',
    is_mobile_source VARCHAR(10) DEFAULT 'false' COMMENT '是否移动源',
    measurement_unit VARCHAR(50) COMMENT '计量单位',
    data_source_system VARCHAR(100) COMMENT '数据来源系统',
    acquisition_method TEXT COMMENT '数据采集方式',
    allocation_ratio DECIMAL(5,2) DEFAULT 100.00 COMMENT '分摊比例',
    has_sub_table BOOLEAN DEFAULT FALSE COMMENT '是否有下级子表',
    error_constraint DECIMAL(5,2) COMMENT '核算误差约束',
    update_cycle VARCHAR(50) COMMENT '更新周期',
    update_time VARCHAR(50) COMMENT '更新时间',
    task_config TEXT COMMENT '数据采集计划任务配置（JSON格式）',
    collection_description TEXT COMMENT '采集描述说明',
    equipment_code VARCHAR(100) COMMENT '设备编码',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (node_id) REFERENCES emission_node(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排放节点配置表';

-- 排放节点信息表：专门存储核算子节点的详细信息
CREATE TABLE IF NOT EXISTS emission_node_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    node_id BIGINT NOT NULL UNIQUE COMMENT '节点ID，关联emission_node表',
    node_code VARCHAR(50) COMMENT '节点编码',
    short_name VARCHAR(50) COMMENT '节点名称简称，用于自动生成子节点名称的前缀',
    include_in_calculation BOOLEAN DEFAULT TRUE COMMENT '是否纳入碳排放核算',
    node_category VARCHAR(50) COMMENT '节点类型（总公司/分公司/站点/区域）',
    unit_description TEXT COMMENT '单位说明，用于解释单位职责、范围',
    org_boundary_description TEXT COMMENT '组织边界说明，明确哪些单位、区域纳入核算',
    operation_boundary_description TEXT COMMENT '运营边界说明，明确自有、租赁、外包、代管设施处理口径',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (node_id) REFERENCES emission_node(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='排放节点信息表';

/*
-- 系统配置表：存储系统运行所需的配置参数
-- DROP TABLE IF EXISTS sys_config;
CREATE TABLE IF NOT EXISTS sys_config (
    config_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置项标识',
    config_value VARCHAR(500) NOT NULL COMMENT '配置值',
    config_name VARCHAR(100) COMMENT '配置描述',
    remark VARCHAR(500) COMMENT '配置值描述',
    config_type CHAR(1) DEFAULT 'N' COMMENT '配置类型',
    create_by VARCHAR(64) COMMENT '创建人',
    update_by VARCHAR(64) COMMENT '更新人',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 初始化系统配置数据
INSERT IGNORE INTO sys_config (config_key, config_value, config_name, remark) VALUES 
('Auto_coding_rules', '4', '数据采集节点编码自动生成规则配置项', '1：第一级子节点编码+采集点ID号字符串；2：第一级子节点编码+第二级子节点编码（如果有）+采集点ID号字符串；3：第一级子节点编码+排放数据大类编码+采集点ID号字符串；4：第一级子节点编码+第二级子节点编码（如果有）+排放数据大类编码+采集点自身ID号字符串');
*/

/*
-- 初始化数据字典
INSERT IGNORE INTO emission_data_dict (dict_code, dict_name, description, sort_order) VALUES 
('locomotive_type', '机车类型', '运输生产碳排放核算节点的机车类型', 1),
('statistical_caliber', '统计口径类型', '排放数据的统计口径分类', 2),
('emission_category', '排放数据大类', '排放数据的一级分类', 3),
('emission_subcategory', '排放数据小类', '排放数据的二级分类', 4),
('data_source', '数据来源', '数据采集的来源方式', 5),
('node_category', '节点类型', '核算子节点的组织类型分类', 6),
('accounting_scenario', '核算场景', '碳排放来源归类场景', 7),
('energy_use', '能耗用途', '能源消耗用途分类', 8);

---- 初始化数据字典项
INSERT IGNORE INTO emission_data_dict_item (dict_id, item_code, item_value, sort_order) VALUES 
((SELECT id FROM emission_data_dict WHERE dict_code = 'locomotive_type'), 'HXN3', 'HXN3型内燃机车', 1),
((SELECT id FROM emission_data_dict WHERE dict_code = 'locomotive_type'), 'BN8', '国能八轴交流机车', 2),
((SELECT id FROM emission_data_dict WHERE dict_code = 'locomotive_type'), 'BN12', '国能十二轴交流机车', 3),

((SELECT id FROM emission_data_dict WHERE dict_code = 'statistical_caliber'), 'PE', '生产排放', 1),
((SELECT id FROM emission_data_dict WHERE dict_code = 'statistical_caliber'), 'PAOE', '辅助和附属生产排放', 2),

((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_category'), 'PE', '购入的电力', 1),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_category'), 'PH', '购入的热力', 2),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_category'), 'FF', '化石燃料', 3),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_category'), 'EP', '输出的电力', 4),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_category'), 'WT', '废弃物处理', 5),

((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'PE_PF', '生产设施用电', 1),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'PE_AS', '辅助生产系统用电', 2),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'PE_SS', '附属生产系统用电', 3),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'PH_HD', '热力数据', 4),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'PH_HW', '质量单位计量的热水', 5),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'PH_HS', '质量单位计量的蒸汽', 6),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_G', '汽油', 7),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_D', '柴油', 8),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_C', '原油', 9),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_F', '燃料油', 10),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_LNG', '液化天然气', 11),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_LPG', '液化石油气', 12),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_NG', '天然气', 13),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_BFG', '高炉煤气', 14),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_COG', '转炉煤气', 15),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_COK', '焦炉煤气', 16),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_B', '烟煤', 17),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_LB', '褐煤', 18),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_CK', '焦炭', 19),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_PC', '石油焦', 20),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'WT_SW', '固体废弃物处理排放', 21),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'WT_WW', '废水处理排放', 22),

((SELECT id FROM emission_data_dict WHERE dict_code = 'data_source'), 'MANUAL', '手工录入', 1),
((SELECT id FROM emission_data_dict WHERE dict_code = 'data_source'), 'DB', '数据库', 2),
((SELECT id FROM emission_data_dict WHERE dict_code = 'data_source'), 'API', 'API输入', 3),

((SELECT id FROM emission_data_dict WHERE dict_code = 'node_category'), 'HQ', '总公司', 1),
((SELECT id FROM emission_data_dict WHERE dict_code = 'node_category'), 'BRANCH', '分公司', 2),
((SELECT id FROM emission_data_dict WHERE dict_code = 'node_category'), 'STATION', '站点', 3),
((SELECT id FROM emission_data_dict WHERE dict_code = 'node_category'), 'REGION', '区域', 4);

-- 设置字典项的父子关系
UPDATE emission_data_dict_item SET parent_code = 'PE' WHERE item_code IN ('PE_PF', 'PE_AS', 'PE_SS');
UPDATE emission_data_dict_item SET parent_code = 'PH' WHERE item_code IN ('PH_HD', 'PH_HW', 'PH_HS');
UPDATE emission_data_dict_item SET parent_code = 'FF' WHERE item_code IN ('FF_G', 'FF_D', 'FF_C', 'FF_F', 'FF_LNG', 'FF_LPG', 'FF_NG', 'FF_BFG', 'FF_COG', 'FF_COK', 'FF_B', 'FF_LB', 'FF_CK', 'FF_PC');
UPDATE emission_data_dict_item SET parent_code = 'WT' WHERE item_code IN ('WT_SW', 'WT_WW');

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
*/

-- 初始化节点类型
INSERT IGNORE INTO emission_node_type (type_name, can_have_children, description) VALUES 
('root', TRUE, '根节点'),
('calculation', TRUE, '核算子节点'),
('data_collection', FALSE, '排放数据采集点'),
('transport', TRUE, '运输生产碳排放核算节点');

-- 数据来源系统表：存储数据来源系统信息
CREATE TABLE IF NOT EXISTS emission_data_source_system (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    system_name VARCHAR(100) NOT NULL COMMENT '系统名称',
    description VARCHAR(500) COMMENT '描述信息',
    pinyin_code VARCHAR(50) COMMENT '拼音首字母编码，用于快速搜索',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_system_name (system_name) COMMENT '系统名称唯一索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据来源系统表';

/*
-- 用户表：存储系统用户信息
CREATE TABLE IF NOT EXISTS sys_user (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    user_name VARCHAR(30) NOT NULL UNIQUE COMMENT '用户名',
    nick_name VARCHAR(30) COMMENT '用户昵称',
    dept_id BIGINT COMMENT '部门ID'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
*/
-- 模板表：存储节点树模板，用于快速创建相似结构的节点树
CREATE TABLE IF NOT EXISTS emission_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(200) NOT NULL COMMENT '模板名称',
    description VARCHAR(500) COMMENT '模板描述',
    created_by BIGINT NOT NULL COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '更新人ID',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    version INT DEFAULT 1 COMMENT '版本号',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    task_config TEXT COMMENT '模板任务配置'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板表';

-- 初始化根节点
INSERT IGNORE INTO emission_node (name, type_id, parent_id) VALUES 
('碳排放核算', 1, NULL);

-- 电力碳排放因子库表：存储电力碳排放因子信息
CREATE TABLE IF NOT EXISTS emission_electricity_carbon_emission_factor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    factor_name VARCHAR(200) NOT NULL COMMENT '碳排放因子名称',
    factor_value DECIMAL(15,6) NOT NULL COMMENT '碳排放因子',
    unit VARCHAR(50) NOT NULL COMMENT '单位',
    description TEXT COMMENT '碳排放因子说明',
    created_by BIGINT COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '修改人ID',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    UNIQUE KEY uk_factor_name (factor_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电力碳排放因子库表';

-- 初始化电力碳排放因子数据
INSERT IGNORE INTO emission_electricity_carbon_emission_factor (factor_name, factor_value, unit, description) VALUES 
('2023年全国电力碳排放因子', 0.5306, 'kgCO₂/kWh', '生态环境部和国家统计局2025年12月31日联合发布的2023年官方数据，包含所有电源类型的全国电力平均二氧化碳排放因子。'),
('2022山西省电力碳排放因子', 0.7096, 'kgCO₂/kWh', '生态环境部、国家统计局正式发布的2022年完整官方省级电力平均二氧化碳排放因子。'),
('2022河北省电力碳排放因子', 0.7252, 'kgCO₂/kWh', '生态环境部、国家统计局正式发布的2022年完整官方省级电力平均二氧化碳排放因子。'),
('2022山东省电力碳排放因子', 0.6410, 'kgCO₂/kWh', '生态环境部、国家统计局正式发布的2023年完整官方省级电力平均二氧化碳排放因子。'),
('2022天津市电力碳排放因子', 0.7041, 'kgCO₂/kWh', '生态环境部、国家统计局正式发布的2024年完整官方省级电力平均二氧化碳排放因子。');

-- 化石燃料排放因子库表：存储化石燃料排放因子信息
CREATE TABLE IF NOT EXISTS emission_fossil_fuel_emission_factor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    emission_factor_name VARCHAR(200) COMMENT '碳排放因子名称',
    fuel_type VARCHAR(200) NOT NULL COMMENT '燃料品种',
    source VARCHAR(200) COMMENT '来源',
    unit VARCHAR(50) NOT NULL COMMENT '计量单位',
    lower_heating_value DECIMAL(15,6) COMMENT '低位发热量',
    carbon_content_per_unit_heat DECIMAL(15,6) COMMENT '单位热值含碳量',
    fuel_oxidation_rate DECIMAL(5,4) COMMENT '燃料氧化率',
    emission_factor DECIMAL(15,6) COMMENT '碳排放因子',
    factor_unit VARCHAR(50) COMMENT '单位',
    description TEXT COMMENT '碳排放因子说明',
    created_by BIGINT COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '修改人ID',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    UNIQUE KEY uk_emission_factor_name (emission_factor_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='化石燃料排放因子库表';

-- 初始化化石燃料排放因子数据
INSERT IGNORE INTO emission_fossil_fuel_emission_factor (emission_factor_name, fuel_type, source, unit, lower_heating_value, carbon_content_per_unit_heat, fuel_oxidation_rate, emission_factor, factor_unit, description) VALUES 
('烟煤排放因子','烟煤', '缺省值', 't', 19.57, 26.1, 0.93, 0.0902, 'tCO2/GJ', '数据取值来源为《2005 中国温室气体清单研究》、《省级温室气体清单编制指南（2025）》'),
('褐煤排放因子','褐煤', '缺省值', 't', 11.9, 28.0, 0.96, 0.0986, 'tCO2/GJ', '数据取值来源为《2006年 IPCC国家温室气体清单指南》及2019修订版、《省级温室气体清单编制指南（2025）》'),
('焦炭排放因子','焦炭', '缺省值', 't', 28.435, 29.5, 0.93, 0.0978, 'tCO2/GJ', '数据取值来源为《中国能源统计年鉴2023》、《省级温室气体清单编制指南（2025）》'),
('石油焦排放因子','石油焦', '缺省值', 't', 32.5, 27.5, 0.98, 0.0968, 'tCO2/GJ', '数据取值来源为《2006年 IPCC国家温室气体清单指南》及2019修订版、《省级温室气体清单编制指南（2025）》'),
('原油排放因子','原油', '缺省值', 't', 41.816, 20.1, 0.98, 0.0721, 'tCO2/GJ', '数据取值来源为《中国能源统计年鉴2023》、《省级温室气体清单编制指南（2025）》'),
('燃料油排放因子','燃料油', '缺省值', 't', 41.816, 21.1, 0.98, 0.0757, 'tCO2/GJ', '数据取值来源为《中国能源统计年鉴2023》、《省级温室气体清单编制指南（2025）》'),
('汽油排放因子','汽油', '缺省值', 't', 43.07, 18.9, 0.98, 0.0679, 'tCO2/GJ', '数据取值来源为《中国能源统计年鉴2023》、《省级温室气体清单编制指南（2025）》'),  
('柴油排放因子','柴油', '缺省值', 't', 42.652, 20.2, 0.98, 0.0725, 'tCO2/GJ', '数据取值来源为《中国能源统计年鉴2023》、《省级温室气体清单编制指南（2025）》'),
('液化天然气排放因子','液化天然气', '缺省值', 't', 51.498, 15.3, 0.98, 0.0547, 'tCO2/GJ', '数据取值来源为数据取值来源为GB/T 2589—2020、《省级温室气体清单编制指南（2025）》'),
('液化石油气排放因子','液化石油气', '缺省值', 't', 50.179, 17.2, 0.98, 0.0613, 'tCO2/GJ', '数据取值来源为《中国能源统计年鉴2023》、《省级温室气体清单编制指南（2025）》'),
('天然气排放因子','天然气', '缺省值', '104 Nm3', 389.31, 15.3, 0.99, 0.0552, 'tCO2/GJ', '数据取值来源为《中国能源统计年鉴2023》、《省级温室气体清单编制指南（2025）》'),
('高炉煤气排放因子','高炉煤气', '缺省值', '104 Nm3', 33.0, 70.8, 0.99, 0.2502, 'tCO2/GJ', '数据取值来源为《2005 中国温室气体清单研究》、《中国能源统计年鉴2023》、《省级温室气体清单编制指南（2025）》'),
('转炉煤气排放因子','转炉煤气', '缺省值', '104 Nm3', 84.0, 49.6, 0.99, 0.1756, 'tCO2/GJ', '数据取值来源为《2005 中国温室气体清单研究》、《省级温室气体清单编制指南（2025）》'),
('焦炉煤气排放因子','焦炉煤气', '缺省值', '104 Nm3', 179.81, 13.58, 0.99, 0.0484, 'tCO2/GJ', '数据取值来源为《中国能源统计年鉴2023》、《省级温室气体清单编制指南（2025）》');

-- 热力排放因子库表：存储热力排放因子数据
CREATE TABLE IF NOT EXISTS emission_thermal_emission_factor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    emission_factor_name VARCHAR(200) COMMENT '碳排放因子名称',
    emission_factor DECIMAL(15, 6) COMMENT '碳排放因子',
    unit VARCHAR(50) COMMENT '单位',
    source VARCHAR(200) COMMENT '来源',
    description TEXT COMMENT '碳排放因子说明',
    created_by BIGINT COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '修改人ID',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    UNIQUE KEY uk_emission_factor_name (emission_factor_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='热力排放因子库表';

-- 初始化热力排放因子数据
INSERT IGNORE INTO emission_thermal_emission_factor (emission_factor_name, emission_factor, unit, source, description) VALUES 
('全国统一热力排放因子缺省值', 0.11, ' tCO₂/GJ', '缺省值', '全国层面未发布统一分区热力排放因子时，公开规则明确热力消费排放因子‌缺省推荐值为0.11 tCO₂/GJ‌，适用于国家温室气体排放因子数据库未发布对应数据的场景。该数值被《公共机构碳排放核算指南》、环境影响评价报告书等多个权威文件采纳，是目前全国通用的默认参考值。');

-- 固体废弃物焚烧排放因子表：存储固体废弃物焚烧排放因子数据
CREATE TABLE IF NOT EXISTS emission_waste_incineration_factor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    emission_factor_name VARCHAR(200) COMMENT '碳排放因子名称',
    waste_type VARCHAR(200) COMMENT '固体废物种类',
    ccw DECIMAL(10, 4) COMMENT '废弃物中的碳含量比例',
    fcf DECIMAL(10, 4) COMMENT '废弃物中的化石碳在总碳中的比例',
    ce DECIMAL(10, 4) COMMENT '废弃物焚烧炉的完全燃烧效率',
    emission_factor DECIMAL(15, 6) COMMENT '碳排放因子',
    unit VARCHAR(50) COMMENT '单位',
    source VARCHAR(200) COMMENT '来源',
    description TEXT COMMENT '碳排放因子说明',
    created_by BIGINT COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '修改人ID',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    UNIQUE KEY uk_emission_factor_name (emission_factor_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='固体废弃物焚烧排放因子表';

-- 初始化固体废弃物焚烧排放因子数据
INSERT IGNORE INTO emission_waste_incineration_factor (emission_factor_name, waste_type, ccw, fcf, ce, emission_factor, unit, source, description) VALUES 
('生活垃圾焚烧', '生活垃圾', 0.4, 0.4, 0.95, 0.304, 'tCO2/t固体废物', '缺省值', '数据来源《省级温室气体清单编制指南（2025年版）》'),
('危险废物焚烧', '危险废物', 0.5, 0.9, 0.995, 0.8955, 'tCO2/t固体废物', '缺省值', '数据来源《省级温室气体清单编制指南（2025年版）》');

-- 废水处理排放因子表：存储废水处理排放因子数据
CREATE TABLE IF NOT EXISTS emission_wastewater_treatment_factor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    emission_factor_name VARCHAR(200) COMMENT '碳排放因子名称',
    wastewater_type VARCHAR(200) COMMENT '处理废水种类',
    od DECIMAL(10, 4) COMMENT '需氧浓度系数（COD或BOD，单位：mg/L）',
    bo DECIMAL(10, 4) COMMENT '最大甲烷产生能力(单位tCH4/t）',
    mcf DECIMAL(10, 4) COMMENT '甲烷修正因子',
    gwp DECIMAL(10, 2) COMMENT '甲烷的全球变暖潜能值，缺省为28',
    emission_factor DECIMAL(15, 6) COMMENT '碳排放因子',
    unit VARCHAR(50) COMMENT '单位',
    source VARCHAR(200) COMMENT '来源',
    description TEXT COMMENT '碳排放因子说明',
    created_by BIGINT COMMENT '创建人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_by BIGINT COMMENT '修改人ID',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
    UNIQUE KEY uk_emission_factor_name (emission_factor_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='废水处理排放因子表';

-- 初始化废水处理排放因子数据
INSERT IGNORE INTO emission_wastewater_treatment_factor (emission_factor_name, wastewater_type, od, bo, mcf, gwp, emission_factor, unit, source, description) VALUES 
('机车检修、车辆清洗废水-厌氧处理', '机车检修、车辆清洗废水', 500, 0.25, 0.9, 28, 0.00315, 'tGH4/立方米', '缺省值', '需氧浓度系数500mg/L是GB/T 31962-2015标准中给出的污水排入城镇下水道水质控制项目限值'),
('机车检修、车辆清洗废水-好氧处理', '机车检修、车辆清洗废水', 500, 0.25, 0.1, 28, 0.00035, 'tGH4/立方米', '缺省值', '需氧浓度系数500mg/L是GB/T 31962-2015标准中给出的污水排入城镇下水道水质控制项目限值'),
('机车检修、车辆清洗废水-直接排放', '机车检修、车辆清洗废水', 500, 0.25, 0.05, 28, 0.000175, 'tGH4/立方米', '缺省值', '需氧浓度系数500mg/L是GB/T 31962-2015标准中给出的污水排入城镇下水道水质控制项目限值'),
('生活污水-化粪池', '生活污水', 200, 0.6, 0.3, 28, 0.001008, 'tGH4/立方米', '缺省值', '需氧浓度系数200mg/L是《水污染控制工程》《废水工程》等国内外经典给排水专业教材中，普通市政生活污水BOD浓度的典型推荐取值'),
('生活污水-厌氧处理系统', '生活污水', 200, 0.6, 0.9, 28, 0.003024, 'tGH4/立方米', '缺省值', '需氧浓度系数200mg/L是《水污染控制工程》《废水工程》等国内外经典给排水专业教材中，普通市政生活污水BOD浓度的典型推荐取值'),
('生活污水-好氧处理系统', '生活污水', 200, 0.6, 0.05, 28, 0.000168, 'tGH4/立方米', '缺省值', '需氧浓度系数200mg/L是《水污染控制工程》《废水工程》等国内外经典给排水专业教材中，普通市政生活污水BOD浓度的典型推荐取值');
