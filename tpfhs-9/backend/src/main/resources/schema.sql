
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

-- 能耗分类数据字典表：按能耗使用场景划分的三级分类（生产用能/生产辅助用能/综合用能）
CREATE TABLE IF NOT EXISTS emission_energy_category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    category_code VARCHAR(50) NOT NULL UNIQUE COMMENT '分类编码',
    category_name VARCHAR(100) NOT NULL COMMENT '分类名称',
    parent_id BIGINT COMMENT '父级分类ID，用于三级分类层级关系，顶级为NULL',
    level TINYINT NOT NULL COMMENT '分类层级：1-一级，2-二级，3-三级',
    remark VARCHAR(500) COMMENT '备注说明',
    sort_order INT DEFAULT 0 COMMENT '排序顺序，数值越小越靠前',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-停用',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (parent_id) REFERENCES emission_energy_category(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='能耗分类数据字典表';

-- 初始化按场景区分能耗分类数据字典（三级分类）
-- 分类原则：宜细不宜粗，条件允许时细化至三级；监测点未覆盖细分项目时按第一级统计
-- 一级分类
INSERT IGNORE INTO emission_energy_category (id, category_code, category_name, parent_id, level, sort_order) VALUES
(1, 'EC01', '生产用能', NULL, 1, 1),
(2, 'EC02', '生产辅助用能', NULL, 1, 2),
(3, 'EC03', '非生产综合用能', NULL, 1, 3),
(4, 'EC04', '混合场景用能', NULL, 1, 4);

-- 二级分类-生产用能
INSERT IGNORE INTO emission_energy_category (id, category_code, category_name, parent_id, level, remark, sort_order) VALUES
(4, 'EC0101', '牵引供电', 1, 2, '指电力机车/动车组从接触网获取的，用于列车牵引和运行的电能。该能耗数据从牵引供电变电站的计量电表获取。', 1),
(5, 'EC0102', '内燃机车牵引', 1, 2, '指内燃机车运行所消耗的柴油等燃料。适用于非电气化线路或调车作业。', 2),
(6, 'EC0103', '电力机车牵引能耗', 1, 2, '指单台电力机车的牵引耗能，通过电力机车上的车载电表获取的能耗数据。该能耗与"牵引供电"分项重合，不计入总体能耗统计，仅是为了统计和核算每台电力机车相关能耗数据而设立。', 3);

-- 二级分类-生产辅助用能
INSERT IGNORE INTO emission_energy_category (id, category_code, category_name, parent_id, level, sort_order) VALUES
(7,  'EC0201', '线路与桥隧维护（工务）', 2, 2, 1),
(8,  'EC0202', '机车运用与检修（机务）', 2, 2, 2),
(9,  'EC0203', '运输组织（车务）', 2, 2, 3),
(10, 'EC0204', '通信与信号（电务）', 2, 2, 4),
(11, 'EC0205', '车辆系统', 2, 2, 5),
(12, 'EC0206', '供电系统', 2, 2, 6);

-- 二级分类-综合用能（非生产用能）
INSERT IGNORE INTO emission_energy_category (id, category_code, category_name, parent_id, level, remark, sort_order) VALUES
(13, 'EC0301', '办公场所', 3, 2, NULL, 1),
(14, 'EC0302', '后勤及生活服务', 3, 2, '浴室、食堂、职工宿舍、运动场、洗衣房等。', 2),
(15, 'EC0303', '公共服务', 3, 2, NULL, 3),
(16, 'EC0304', '公务用车', 3, 2, '各种公务用汽车。', 4);

-- 三级分类-线路与桥隧维护（工务）
INSERT IGNORE INTO emission_energy_category (id, category_code, category_name, parent_id, level, remark, sort_order) VALUES
(17, 'EC020101', '大型养路机械', 7, 3, '包括维护用轨道车、工程车、相关设备用能。', 1),
(18, 'EC020102', '线路维护工区', 7, 3, '包括设备库/物料库、工务工区、巡道房、道口房（含电动栏杆）等场所的日常用能。', 2);

-- 三级分类-机车运用与检修（机务）
INSERT IGNORE INTO emission_energy_category (id, category_code, category_name, parent_id, level, sort_order) VALUES
(19, 'EC020201', '整备与检修库', 8, 3, 1),
(20, 'EC020202', '机车清洗/保洁库', 8, 3, 2);

-- 三级分类-运输组织（车务）
INSERT IGNORE INTO emission_energy_category (id, category_code, category_name, parent_id, level, remark, sort_order) VALUES
(21, 'EC020301', '场站系统', 9, 3, '专为重载货运服务的站场用能，包括：翻车机/装车楼、货运调度室、货场等。不包含站区内的食堂、宿舍等生活设施用能。', 1),
(22, 'EC020302', '行车指挥系统', 9, 3, '车站行车室、信号楼等场所。', 2);

-- 三级分类-通信与信号（电务）
INSERT IGNORE INTO emission_energy_category (id, category_code, category_name, parent_id, level, remark, sort_order) VALUES
(23, 'EC020401', '行车调度（信号）/通信', 10, 3, '沿线信号机、转辙机、轨道电路、闭塞设备、道口信号等。', 1),
(24, 'EC020402', '通信机房/通信基站', 10, 3, '通信基站、中继站、GSM-R核心网机房、调度通信设备的用能。', 2);

-- 三级分类-车辆系统
INSERT IGNORE INTO emission_energy_category (id, category_code, category_name, parent_id, level, remark, sort_order) VALUES
(25, 'EC020501', '列检作业场', 11, 3, '货车列检所的现场用能。包括：脱轨器、电动试风装置、微控地面试验装置检查设备、现场照明等。', 1),
(26, 'EC020502', '站修作业场', 11, 3, '站修作业场用能。', 2);

-- 三级分类-供电系统
INSERT IGNORE INTO emission_energy_category (id, category_code, category_name, parent_id, level, remark, sort_order) VALUES
(27, 'EC020601', '牵引变电所', 12, 3, '是指牵引变电所内的照明、空调、维护设备和办公设备用电，不包含牵引供电。', 1);

-- 三级分类-办公场所
INSERT IGNORE INTO emission_energy_category (id, category_code, category_name, parent_id, level, remark, sort_order) VALUES
(28, 'EC030101', '机关办公', 13, 3, '各系统（车、机、工、电、辆、供）的机关办公楼、段部办公楼、车间办公室等。包括照明、空调、办公设备、电梯等。', 1),
(29, 'EC030102', '工务', 13, 3, NULL, 2),
(30, 'EC030103', '机务', 13, 3, NULL, 3),
(31, 'EC030104', '电务（通信与信号设备）', 13, 3, NULL, 4),
(32, 'EC030105', '车辆系统', 13, 3, NULL, 5),
(33, 'EC030106', '供电系统', 13, 3, NULL, 6);

-- 三级分类-公共服务
INSERT IGNORE INTO emission_energy_category (id, category_code, category_name, parent_id, level, remark, sort_order) VALUES
(34, 'EC030301', '公共基础设施', 15, 3, '路灯、消防部门、门卫/保安室等场所。', 1),
(35, 'EC030302', '综合给排水系统', 15, 3, '独立的水塔、给水所、加压泵站（非生产用水）的用能。包含该场所内的所有用能：照明、空调、生产设备和办公设备用能。', 2),
(36, 'EC030303', '综合供暖系统', 15, 3, '集中锅炉房、换热站的用能。包含该场所内的所有用能：照明、空调、生产设备和办公设备用能。', 3),
(37, 'EC030304', '废弃物/污水处理', 15, 3, '包含污水所（站）、污水泵站、资源再生站等场所用能，包含该场所内的所有用能：照明、空调、生产设备和办公设备用能。', 4);

-- 能耗分类说明：
-- （1）宜细不宜粗，监测点未覆盖细分项目时按第一级统计，条件具备后逐步推进至二级、三级分类
-- （2）生产辅助场所若未安装独立能耗计量表，整体能耗计入"生产辅助用能"；若办公区域有独立采集点，则剥离计入"综合用能"下的"办公场所"
-- （3）场站系统中食堂、宿舍等无单独计量的生活设施用能划归"生产辅助用能"下的"场站系统"；有单独计量则计入"综合用能"下的"后勤及生活服务"
-- （4）相关场所能耗包含该场所内的所有用能：照明、空调/通风、生产设备和办公设备用能

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
    enabled BOOLEAN DEFAULT FALSE COMMENT '是否启用',
    template_type TINYINT DEFAULT 1 COMMENT '模版类型：1-节点模版（仅用于构建层级关系，作为结构被引用，不参与碳排放自动化核算）；2-核算模版（实现碳排放自动化核算功能，支持树形节点管理、属性配置及计划任务调度）',
    task_config TEXT COMMENT '模板任务配置',
    factor_template_id BIGINT COMMENT '碳排放因子模版ID（外键，指向 emission_factor_template.id，NULL表示未关联。仅核算模版使用）',
    check_result TINYINT DEFAULT 0 COMMENT '模版校验结果：0-未检查，1-完全正确，2-正确（存在提示信息），3-存在告警，4-存在错误。模版被修改后自动重置为0',
    check_time DATETIME NULL COMMENT '最近一次模版校验时间',
    check_message TEXT COMMENT '最近一次校验结果详情（富文本HTML：错误-红#F56C6C、告警-橙#E6A23C、提示-蓝#409EFF、通过-绿#67C23A）',
    INDEX idx_factor_template_id (factor_template_id) COMMENT '因子模版ID索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='模板表';


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
    source_node_id BIGINT DEFAULT 0 COMMENT '对应节点ID：节点模板挂载到树结构时复制生成的节点，记录其来源节点（节点模板中的原节点）的ID',
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
    emission_category VARCHAR(100) COMMENT '能耗品种大类',
    emission_subcategory VARCHAR(100) COMMENT '能耗品种小类',
    carbon_emission_factor DECIMAL(15,6) COMMENT '碳排放因子',
    carbon_emission_factor_description TEXT COMMENT '碳排放因子说明',
    collection_description TEXT COMMENT '采集描述说明',
    equipment_code VARCHAR(100) COMMENT '设备编码',
    collection_point_type TINYINT COMMENT '采集点类型：1-电力表，2-化石燃料，3-外购热能',
    collection_point_id BIGINT COMMENT '采集点ID，指向对应采集点表的主键（根据采集点类型指向emission_meter_info/emission_fossil_fuel_meter_info/emission_purchased_heat_meter_info）',
    collection_point_status TINYINT DEFAULT 1 COMMENT '采集点状态：1-启用，2-禁用，3-被删除（找不到对应记录），4-离线',
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

-- 复用 RuoYi sys_config；本脚本只补充核算业务配置，不创建或修改表结构。
INSERT INTO sys_config (config_key, config_value, config_name, remark)
SELECT 'Auto_coding_rules', '4', '数据采集节点编码自动生成规则配置项', '1：第一级子节点编码+采集点ID号字符串；2：第一级子节点编码+第二级子节点编码（如果有）+采集点ID号字符串；3：第一级子节点编码+排放数据大类编码+采集点ID号字符串；4：第一级子节点编码+第二级子节点编码（如果有）+排放数据大类编码+采集点自身ID号字符串'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'Auto_coding_rules');
-- 插入电表设置相关系统配置项
INSERT INTO sys_config (config_key, config_value, config_name, remark)
SELECT 'Disable_all_concentrators', '0', '在电表设置页面中，是否允许一键禁用某区域/站点下所有数据集中器', '0：不允许；1：允许。注：如果选项Disable_all_meter设置为0，则即使该选择设置为1，在操作时，也无法禁用其下属集中器'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'Disable_all_concentrators');
INSERT INTO sys_config (config_key, config_value, config_name, remark)
SELECT 'Disable_all_meter', '0', '在电表设置页面中，是否允许一键禁用数据集中器下面的所有电表', '0：不允许；1：允许。'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'Disable_all_meter');
-- 插入数据采集相关系统配置项
INSERT INTO sys_config (config_key, config_value, config_name, remark)
SELECT 'Collection_task_cycle_interval', '5', '采集任务循环执行时间间隔，单位：分', '执行完一次采集点数据遍历读取后，间隔5分钟后再进行下一次遍历读取。'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'Collection_task_cycle_interval');
INSERT INTO sys_config (config_key, config_value, config_name, remark)
SELECT 'Days_abandon_read_operation', '5', '读取失败的采集任务经过多少天以后取消对该任务的重复读取，单位：天', '读取失败的采集任务经过5天以后取消对该任务的重复读取。某些手动录入的数据可能没有及时录取，所以需要预留足够的时间反复读取。超过该时间间隔后，系统将不再读取该采集点的数据。'
WHERE NOT EXISTS (SELECT 1 FROM sys_config WHERE config_key = 'Days_abandon_read_operation');




-- 初始化数据字典
INSERT IGNORE INTO emission_data_dict (dict_code, dict_name, description, sort_order) VALUES 
('emission_category', '排放数据大类', '排放数据的一级分类', 1),
('emission_subcategory', '排放数据小类', '排放数据的二级分类', 2),
('statistical_caliber', '统计口径类型', '排放数据的统计口径分类', 3),
('data_source', '数据来源', '数据采集的来源方式', 4),
('node_category', '节点类型', '核算子节点的组织类型分类', 5),
('accounting_scenario', '核算场景', '碳排放来源归类场景', 6),
('energy_use', '能耗用途', '能源消耗用途分类', 7),
('meter_type', '电表类型', '电表类型分类', 8),
('locomotive_type', '机车类型', '运输生产碳排放核算节点的机车类型', 9),
('energy_allocation', '朔黄铁路分公司', '朔黄铁路分公司列表，用于能耗数据归属设置', 10);


-- 初始化数据字典项
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

((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'PE_PF', '外购电力', 1),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'PE_AS', '新能源发电（自发自用）', 2),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'PH_HD', '热力数据', 4),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'PH_HW', '质量单位计量的热水', 5),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'PH_HS', '质量单位计量的蒸汽', 6),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'PH_HS', '按供热面积计量的暖气', 7),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_G', '汽油', 8),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_D', '柴油', 9),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_C', '原油', 10),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_F', '燃料油', 11),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_LNG', '液化天然气', 12),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_LPG', '液化石油气', 13),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_NG', '天然气', 14),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_BFG', '高炉煤气', 15),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_COG', '转炉煤气', 16),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_COK', '焦炉煤气', 17),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_B', '烟煤', 18),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_LB', '褐煤', 19),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_CK', '焦炭', 20),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'FF_PC', '石油焦', 21),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'WT_SW', '固体废弃物处理排放', 22),
((SELECT id FROM emission_data_dict WHERE dict_code = 'emission_subcategory'), 'WT_WW', '废水处理排放', 23),

((SELECT id FROM emission_data_dict WHERE dict_code = 'data_source'), 'MANUAL', '手工录入', 1),
((SELECT id FROM emission_data_dict WHERE dict_code = 'data_source'), 'DB', '数据库', 2),
((SELECT id FROM emission_data_dict WHERE dict_code = 'data_source'), 'API', 'API输入', 3),

((SELECT id FROM emission_data_dict WHERE dict_code = 'node_category'), 'HQ', '总公司', 1),
((SELECT id FROM emission_data_dict WHERE dict_code = 'node_category'), 'BRANCH', '分公司', 2),
((SELECT id FROM emission_data_dict WHERE dict_code = 'node_category'), 'STATION', '站点', 3),
((SELECT id FROM emission_data_dict WHERE dict_code = 'node_category'), 'REGION', '区域', 4);

-- 初始化电表类型字典项
INSERT IGNORE INTO emission_data_dict_item (dict_id, item_code, item_value, sort_order) VALUES
((SELECT id FROM emission_data_dict WHERE dict_code = 'meter_type'), 'single_phase', '单相', 1),
((SELECT id FROM emission_data_dict WHERE dict_code = 'meter_type'), 'three_phase_three_wire', '三相三线', 2),
((SELECT id FROM emission_data_dict WHERE dict_code = 'meter_type'), 'three_phase_four_wire', '三相四线', 3),
((SELECT id FROM emission_data_dict WHERE dict_code = 'meter_type'), 'comprehensive_power_monitor', '综合电力监测仪表', 4);

-- 初始化能耗数据归属（朔黄铁路分公司）字典项
INSERT IGNORE INTO emission_data_dict_item (dict_id, item_code, item_value, sort_order) VALUES
((SELECT id FROM emission_data_dict WHERE dict_code = 'energy_allocation'), 'suring', '肃宁分公司', 1),
((SELECT id FROM emission_data_dict WHERE dict_code = 'energy_allocation'), 'yuanping', '原平分公司', 2),
((SELECT id FROM emission_data_dict WHERE dict_code = 'energy_allocation'), 'jilong', '机辆分公司', 3);

-- 禁用安全更新模式，允许非主键条件的 UPDATE 语句（用于初始化脚本）
SET SQL_SAFE_UPDATES = 0;

-- 设置字典项的父子关系
UPDATE emission_data_dict_item SET parent_code = 'PE' WHERE item_code IN ('PE_PF', 'PE_AS', 'PE_SS');
UPDATE emission_data_dict_item SET parent_code = 'PH' WHERE item_code IN ('PH_HD', 'PH_HW', 'PH_HS');
UPDATE emission_data_dict_item SET parent_code = 'FF' WHERE item_code IN ('FF_G', 'FF_D', 'FF_C', 'FF_F', 'FF_LNG', 'FF_LPG', 'FF_NG', 'FF_BFG', 'FF_COG', 'FF_COK', 'FF_B', 'FF_LB', 'FF_CK', 'FF_PC');
UPDATE emission_data_dict_item SET parent_code = 'WT' WHERE item_code IN ('WT_SW', 'WT_WW');
-- 恢复安全更新模式，）
SET SQL_SAFE_UPDATES = 1;

-- 获取核算场景字典ID并插入字典项
SET @dict_id = (SELECT id FROM emission_data_dict WHERE dict_code = 'accounting_scenario');

INSERT IGNORE INTO emission_data_dict_item (dict_id, item_code, item_value, sort_order, status) VALUES
(@dict_id, 'UNDIFFERENTIATED', '未区分用能场所', 1, 1),
(@dict_id, 'TRACTION_POWER', '牵引变电所/牵引供电', 2, 1),
(@dict_id, 'TRAIN_OPERATION', '列车运行', 3, 1),
(@dict_id, 'DISPATCH_COMM', '行车调度/通信指挥', 4, 1),
(@dict_id, 'VEHICLE_REPAIR', '车辆维修/机修车间', 5, 1),
(@dict_id, 'LINE_MAINTENANCE', '线路维护保养', 6, 1),
(@dict_id, 'STATION_SYSTEM', '场站系统', 7, 1),
(@dict_id, 'GARAGE', '车库', 8, 1),
(@dict_id, 'WORKSHOP_BATHROOM', '车间浴室', 9, 1),
(@dict_id, 'OFFICE_BUILDING', '办公楼', 10, 1),
(@dict_id, 'STAFF_CANTEEN', '职工食堂', 11, 1),
(@dict_id, 'STAFF_DORMITORY', '职工宿舍', 12, 1),
(@dict_id, 'INTERNAL_VEHICLE', '内部运营车辆', 13, 1),
(@dict_id, 'INTERNAL_VEHICLE_SINGLE', '内部运营车辆（单台车）', 14, 1),
(@dict_id, 'WASTE_DISPOSAL', '废弃物处理场所', 15, 1),
(@dict_id, 'OTHER_SCENARIO', '其他场景', 16, 1)
ON DUPLICATE KEY UPDATE item_value = VALUES(item_value), sort_order = VALUES(sort_order), status = VALUES(status);

-- 获取能耗用途字典ID并插入字典项
SET @dict_id = (SELECT id FROM emission_data_dict WHERE dict_code = 'energy_use');

INSERT IGNORE INTO emission_data_dict_item (dict_id, item_code, item_value, sort_order, status) VALUES
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

-- 用户信息直接复用 RuoYi sys_user，本脚本不创建或初始化用户表。

-- 初始化默认核算模板及其根节点。使用 NOT EXISTS 保留用户已经创建的模板，重复执行不会产生副本。
INSERT INTO emission_template
    (name, description, created_by, updated_by, version, enabled, template_type)
SELECT
    '朔黄铁路碳排放核算模板',
    '系统初始化的朔黄铁路碳排放核算模板，可继续维护节点、采集点、因子和调度参数。',
    1, 1, 1, FALSE, 2
WHERE NOT EXISTS (
    SELECT 1 FROM emission_template WHERE name = '朔黄铁路碳排放核算模板'
);

SET @default_template_id = (
    SELECT id FROM emission_template
    WHERE name = '朔黄铁路碳排放核算模板'
    ORDER BY id LIMIT 1
);

-- 优先把旧数据中尚未归属模板的根节点纳入默认模板。
UPDATE emission_node
SET template_id = @default_template_id,
    updated_by = COALESCE(updated_by, 1)
WHERE parent_id IS NULL
  AND template_id IS NULL
  AND name = '碳排放核算'
ORDER BY id
LIMIT 1;

INSERT INTO emission_node
    (name, type_id, parent_id, template_id, sort_order, created_by, updated_by)
SELECT
    '碳排放核算', nt.id, NULL, @default_template_id, 0, 1, 1
FROM emission_node_type nt
WHERE nt.type_name = 'root'
  AND NOT EXISTS (
      SELECT 1 FROM emission_node
      WHERE template_id = @default_template_id AND parent_id IS NULL
  )
LIMIT 1;

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
-- unit字段中气体类燃料使用 CONCAT('万Nm', UNHEX('C2B3')) 生成"万Nm³"
-- factor_unit字段根据unit取值分别为"tCO₂/t"和"tCO₂/万Nm³"
-- emission_factor字段按公式：(低位发热量×单位热值含碳量×燃料氧化率×44/12)/1000 自动计算
INSERT IGNORE INTO emission_fossil_fuel_emission_factor (emission_factor_name, fuel_type, source, unit, lower_heating_value, carbon_content_per_unit_heat, fuel_oxidation_rate, emission_factor, factor_unit, description) VALUES
('汽油排放因子','汽油', '缺省值', 't', 43.07, 18.9, 0.98, 2.925056, 'tCO₂/t', '数据取值来源为《中国能源统计年鉴2023》、《省级温室气体清单编制指南（2025）》'),
('柴油排放因子','柴油', '缺省值', 't', 42.652, 20.2, 0.98, 3.095910, 'tCO₂/t', '数据取值来源为《中国能源统计年鉴2023》、《省级温室气体清单编制指南（2025）》'),
('天然气排放因子','天然气', '缺省值', '万Nm³', 389.31, 15.3, 0.99, 21.621888, 'tCO₂/万Nm³', '数据取值来源为《中国能源统计年鉴2023》、《省级温室气体清单编制指南（2025）》'),
('烟煤排放因子','烟煤', '缺省值', 't', 19.57, 26.1, 0.93, 1.741750, 'tCO₂/t', '数据取值来源为《2005 中国温室气体清单研究》、《省级温室气体清单编制指南（2025）》'),
('褐煤排放因子','褐煤', '缺省值', 't', 11.9, 28.0, 0.96, 1.172864, 'tCO₂/t', '数据取值来源为《2006年 IPCC国家温室气体清单指南》及2019修订版、《省级温室气体清单编制指南（2025）》'),
('焦炭排放因子','焦炭', '缺省值', 't', 28.435, 29.5, 0.93, 2.860419, 'tCO₂/t', '数据取值来源为《中国能源统计年鉴2023》、《省级温室气体清单编制指南（2025）》'),
('石油焦排放因子','石油焦', '缺省值', 't', 32.5, 27.5, 0.98, 3.211542, 'tCO₂/t', '数据取值来源为《2006年 IPCC国家温室气体清单指南》及2019修订版、《省级温室气体清单编制指南（2025）》'),
('原油排放因子','原油', '缺省值', 't', 41.816, 20.1, 0.98, 3.020202, 'tCO₂/t', '数据取值来源为《中国能源统计年鉴2023》、《省级温室气体清单编制指南（2025）》'),
('燃料油排放因子','燃料油', '缺省值', 't', 41.816, 21.1, 0.98, 3.170461, 'tCO₂/t', '数据取值来源为《中国能源统计年鉴2023》、《省级温室气体清单编制指南（2025）》'),
('液化天然气排放因子','液化天然气', '缺省值', 't', 51.498, 15.3, 0.98, 2.831257, 'tCO₂/t', '数据取值来源为数据取值来源为GB/T 2589—2020、《省级温室气体清单编制指南（2025）》'),
('液化石油气排放因子','液化石油气', '缺省值', 't', 50.179, 17.2, 0.98, 3.101330, 'tCO₂/t', '数据取值来源为《中国能源统计年鉴2023》、《省级温室气体清单编制指南（2025）》'),
('高炉煤气排放因子','高炉煤气', '缺省值', '万Nm³', 33.0, 70.8, 0.99, 8.481132, 'tCO₂/万Nm³', '数据取值来源为《2005 中国温室气体清单研究》、《中国能源统计年鉴2023》、《省级温室气体清单编制指南（2025）》'),
('转炉煤气排放因子','转炉煤气', '缺省值', '万Nm³', 84.0, 49.6, 0.99, 15.124032, 'tCO₂/万Nm³', '数据取值来源为《2005 中国温室气体清单研究》、《省级温室气体清单编制指南（2025）》'),
('焦炉煤气排放因子','焦炉煤气', '缺省值', '万Nm³', 179.81, 13.58, 0.99, 8.863806, 'tCO₂/万Nm³', '数据取值来源为《中国能源统计年鉴2023》、《省级温室气体清单编制指南（2025）》');

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
('全国统一热力排放因子缺省值', 0.11, 'tCO₂/GJ', '缺省值', '全国层面未发布统一分区热力排放因子时，公开规则明确热力消费排放因子缺省推荐值为0.11 tCO₂/GJ，适用于国家温室气体排放因子数据库未发布对应数据的场景。该数值被《公共机构碳排放核算指南》、环境影响评价报告书等多个权威文件采纳，是目前全国通用的默认参考值。'),
('华北地区热力排放因子', 0.1205, 'tCO₂/GJ', '行业指南', '华北地区供热锅炉平均排放因子，考虑当地燃煤供热结构与效率综合测算。适用于华北地区（含北京、天津、河北、山西、内蒙古）热力消费碳排放核算。'),
('华东地区热力排放因子', 0.1109, 'tCO₂/GJ', '行业指南', '华东地区供热锅炉平均排放因子，考虑当地燃煤、燃气供热结构综合测算。适用于华东地区（含上海、江苏、浙江、安徽、福建、江西、山东）热力消费碳排放核算。'),
('华南地区热力排放因子', 0.1050, 'tCO₂/GJ', '行业指南', '华南地区供热锅炉平均排放因子，考虑当地燃气供热占比较高综合测算。适用于华南地区（含广东、广西、海南）热力消费碳排放核算。'),
('华中地区热力排放因子', 0.1180, 'tCO₂/GJ', '行业指南', '华中地区供热锅炉平均排放因子，考虑当地燃煤供热结构综合测算。适用于华中地区（含河南、湖北、湖南）热力消费碳排放核算。'),
('西南地区热力排放因子', 0.1165, 'tCO₂/GJ', '行业指南', '西南地区供热锅炉平均排放因子，考虑当地燃煤与水电替代综合测算。适用于西南地区（含重庆、四川、贵州、云南、西藏）热力消费碳排放核算。'),
('西北地区热力排放因子', 0.1240, 'tCO₂/GJ', '行业指南', '西北地区供热锅炉平均排放因子，考虑当地燃煤为主、冬季供暖期长综合测算。适用于西北地区（含陕西、甘肃、青海、宁夏、新疆）热力消费碳排放核算。'),
('东北地区热力排放因子', 0.1220, 'tCO₂/GJ', '行业指南', '东北地区供热锅炉平均排放因子，考虑当地燃煤为主、集中供热比重大综合测算。适用于东北地区（含辽宁、吉林、黑龙江）热力消费碳排放核算。');

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
-- emission_factor按公式：CCW * FCF * CE * 44/12 自动计算
INSERT IGNORE INTO emission_waste_incineration_factor (emission_factor_name, waste_type, ccw, fcf, ce, emission_factor, unit, source, description) VALUES
('生活垃圾焚烧', '生活垃圾', 0.4, 0.4, 0.95, 0.557333, 'tCO₂/t', '缺省值', '数据来源《省级温室气体清单编制指南（2025年版）》'),
('危险废物焚烧', '危险废物', 0.5, 0.9, 0.995, 1.641750, 'tCO₂/t', '缺省值', '数据来源《省级温室气体清单编制指南（2025年版）》');

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
-- emission_factor按公式：OD * Bo * MCF * GWP / 1000000 自动计算
INSERT IGNORE INTO emission_wastewater_treatment_factor (emission_factor_name, wastewater_type, od, bo, mcf, gwp, emission_factor, unit, source, description) VALUES
('机车检修、车辆清洗废水-厌氧处理', '机车检修、车辆清洗废水', 500, 0.25, 0.9, 28, 0.003150, 'tCO₂/m³', '缺省值', '需氧浓度系数500mg/L是GB/T 31962-2015标准中给出的污水排入城镇下水道水质控制项目限值'),
('机车检修、车辆清洗废水-好氧处理', '机车检修、车辆清洗废水', 500, 0.25, 0.1, 28, 0.000350, 'tCO₂/m³', '缺省值', '需氧浓度系数500mg/L是GB/T 31962-2015标准中给出的污水排入城镇下水道水质控制项目限值'),
('机车检修、车辆清洗废水-直接排放', '机车检修、车辆清洗废水', 500, 0.25, 0.05, 28, 0.000175, 'tCO₂/m³', '缺省值', '需氧浓度系数500mg/L是GB/T 31962-2015标准中给出的污水排入城镇下水道水质控制项目限值'),
('生活污水-化粪池', '生活污水', 200, 0.6, 0.3, 28, 0.001008, 'tCO₂/m³', '缺省值', '需氧浓度系数200mg/L是《水污染控制工程》《废水工程》等国内外经典给排水专业教材中，普通市政生活污水BOD浓度的典型推荐取值'),
('生活污水-厌氧处理系统', '生活污水', 200, 0.6, 0.9, 28, 0.003024, 'tCO₂/m³', '缺省值', '需氧浓度系数200mg/L是《水污染控制工程》《废水工程》等国内外经典给排水专业教材中，普通市政生活污水BOD浓度的典型推荐取值'),
('生活污水-好氧处理系统', '生活污水', 200, 0.6, 0.05, 28, 0.000168, 'tCO₂/m³', '缺省值', '需氧浓度系数200mg/L是《水污染控制工程》《废水工程》等国内外经典给排水专业教材中，普通市政生活污水BOD浓度的典型推荐取值');

-- 站点/区间表：存储电力监测的站点或区间信息
CREATE TABLE IF NOT EXISTS emission_station_interval (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(60) NOT NULL UNIQUE COMMENT '站点/区间名称',
    pinyin_code VARCHAR(30) COMMENT '拼音首字母编码，用于快速搜索',
    description VARCHAR(500) COMMENT '描述信息',
    sort_order INT DEFAULT 0 COMMENT '排序顺序，数值越小越靠前',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-停用',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站点/区间表';

-- 集中器表：存储集中器信息（原点位表）
CREATE TABLE IF NOT EXISTS emission_concentrator (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(60) NOT NULL UNIQUE COMMENT '集中器名称',
    pinyin_code VARCHAR(30) COMMENT '拼音首字母编码，用于快速搜索',
    station_interval_id BIGINT NOT NULL COMMENT '所属站点/区间ID，关联emission_station_interval表',
    transformer_capacity VARCHAR(30) COMMENT '变压器容量',
    concentrator_address VARCHAR(20) COMMENT '集中器地址',
    description VARCHAR(500) COMMENT '描述信息',
    sort_order INT DEFAULT 0 COMMENT '排序顺序，数值越小越靠前',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-停用',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (station_interval_id) REFERENCES emission_station_interval(id) ON DELETE CASCADE
) AUTO_INCREMENT = 9000 ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='集中器表';

-- 电表信息表：存储电表的详细配置和属性
CREATE TABLE IF NOT EXISTS emission_meter_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '电表名称',
    pinyin_code VARCHAR(50) COMMENT '拼音首字母编码，用于快速搜索',
    point_id BIGINT NOT NULL COMMENT '所属集中器ID，关联emission_concentrator表',
    meter_address VARCHAR(20) COMMENT '电表地址',
    meter_type VARCHAR(20) COMMENT '电表类型',
    meter_model VARCHAR(20) COMMENT '电表型号',
    purpose_description VARCHAR(500) COMMENT '用途描述',
    parent_meter_id BIGINT DEFAULT 0 COMMENT '上级电表ID，缺省为0表示无上级',
    is_cumulative TINYINT DEFAULT 1 COMMENT '是否累加量：1-是，0-否',
    is_mobile_source TINYINT DEFAULT 0 COMMENT '是否移动源：1-是，0-否',
    measurement_unit VARCHAR(50) COMMENT '计量单位',
    data_source_system VARCHAR(100) COMMENT '数据来源系统',
    billing_cycle_unit TINYINT DEFAULT 2 COMMENT '计量周期单位：1：周、2：月、3：季度、4：年',
    billing_cycle_start_date TINYINT DEFAULT 1 COMMENT '计量周期起始日期偏移量',
    billing_cycle_length SMALLINT DEFAULT 1 COMMENT '计量周期长度',
    emission_subcategory VARCHAR(50) DEFAULT '外购电力' COMMENT '用电分类：外购电力、新能源发电（自发自用）',
    energy_category_l1 VARCHAR(50) COMMENT '能耗一级分类编码，关联emission_energy_category表',
    energy_category_l2 VARCHAR(50) COMMENT '能耗二级分类编码，关联emission_energy_category表',
    energy_category_l3 VARCHAR(50) COMMENT '能耗三级分类编码，关联emission_energy_category表',
    energy_use_category VARCHAR(100) COMMENT '能耗用途分类',
    energy_allocation VARCHAR(60) COMMENT '能耗数据划拨',
    power_category VARCHAR(20) COMMENT '用电分类：生产用电、生产辅助用电、混合',
    auto_meter_reading_config TEXT COMMENT '自动抄表接口描述（JSON格式）',
    meter_reading_method TINYINT DEFAULT 1 COMMENT '抄表方式：1-自动抄表，0-人工抄表',
    parent_child_relationship TINYINT DEFAULT 1 COMMENT '总表与分表关系：1-总表计数等于各下属分表计数之和，2-总表计数不等于各下属分表计数之和',
    is_virtual_meter TINYINT DEFAULT 0 COMMENT '是否虚拟电表：1-是虚拟电表，0-不是虚拟电表',
    is_allocation_child TINYINT DEFAULT 0 COMMENT '是否分摊虚拟子电表：1-是，0-否',
    allocation_ratio DECIMAL(10,6) DEFAULT 1.000000 COMMENT '分摊比例，缺省值1.0',
    sort_order INT DEFAULT 0 COMMENT '排序顺序，数值越小越靠前',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-停用',
    last_collection_time TIMESTAMP NULL COMMENT '上次采集时间：完成对该采集点数据采集后记录的最近一次采集时间（对应emission_collection_record.collection_time字段的值）',
    enable_time TIMESTAMP NULL COMMENT '启用时间：当启用该节点后自动记录',
    disable_time TIMESTAMP NULL COMMENT '停用时间：当停用该节点后自动记录',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (point_id) REFERENCES emission_concentrator(id) ON DELETE CASCADE
) AUTO_INCREMENT = 90000 ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电表信息表';

-- 电表型号表：存储电表型号信息
CREATE TABLE IF NOT EXISTS emission_meter_model (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    model_name VARCHAR(30) NOT NULL COMMENT '电表型号',
    model_type VARCHAR(20) COMMENT '所属类型：单相、三相三线、三相四线',
    description VARCHAR(400) COMMENT '描述信息',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '修改人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='电表型号表';

/*
-- 初始化电表型号数据
INSERT IGNORE INTO emission_meter_model (model_name, model_type, description) VALUES
('DDZY2088', '单相', '单相电子式智能电能表，精度等级2.0级'),
('DDZY1980', '单相', '单相费控智能电能表，精度等级2.0级'),
('DTZY2088', '三相四线', '三相四线电子式智能电能表，精度等级1.0级'),
('DTZY1980', '三相四线', '三相四线费控智能电能表，精度等级1.0级'),
('DTZ2088', '三相三线', '三相三线电子式智能电能表，精度等级1.0级'),
('DTZY566', '三相四线', '三相四线智能电能表，精度等级0.5S级'),
('DDZY866', '单相', '单相智能电能表，精度等级2.0级'),
('DTZY866', '三相四线', '三相四线智能电能表，精度等级1.0级'),
('DTZY217-Z', '三相四线', '三相四线智能电能表，精度等级1.0级'),
('DDZY217-Z', '单相', '单相智能电能表，精度等级2.0级'),
('DTZ188', '三相三线', '三相三线电能表，精度等级0.5级'),
('DTZ88', '三相三线', '三相三线电能表，精度等级0.5级'),
('DT5-GX2', '三相三线', '三相三线电能表'),
('DSZY188-G', '三相四线', '三相四线智能电能表，精度等级1级'),
('DSZ71', '三相四线', '三相四线电能表，精度等级0.5s级'),
('DSZ178', '三相四线', '三相四线电能表，精度等级0.5s级'),
('DTZ341', '三相三线', '三相三线电能表，精度等级0.2s级'),
('DTZ1296', '三相三线', '三相三线电能表，精度等级0.2s级'),
('DTZ178', '三相三线', '三相三线电能表，精度等级0.5s级'),
('YD2040', '综合电力监测仪表', '三相电力监测仪表，可测量电压、电流、功率、电能等全量电参数，直连电压最高支持600V AC，适配多种接线方式，广泛用于工业电力监控场景'),
('DTZ71', '三相三线', '三相三线电能表，精度等级0.5级'),
('PD510-M14/R', '单相', '单相电能表，精度等级0.5级');
*/

-- 化石燃料采集范围定义表：存储化石燃料采集范围信息
CREATE TABLE IF NOT EXISTS emission_fossil_fuel_collection_scope (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    scope_name VARCHAR(100) NOT NULL UNIQUE COMMENT '采集范围名称',
    pinyin_code VARCHAR(30) COMMENT '拼音首字母编码，用于快速搜索',
    sort_order INT DEFAULT 0 COMMENT '排序顺序，数值越小越靠前',
    description VARCHAR(500) COMMENT '描述信息',
    status INT DEFAULT 1 COMMENT '状态：1-启用，0-停用',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='化石燃料采集范围定义表';

-- 化石燃料采集细分范围定义表：存储化石燃料采集细分范围信息
CREATE TABLE IF NOT EXISTS emission_fossil_fuel_collection_sub_scope (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    sub_scope_name VARCHAR(100) NOT NULL UNIQUE COMMENT '细分范围名称',
    pinyin_code VARCHAR(30) COMMENT '拼音首字母编码，用于快速搜索',
    collection_scope_id BIGINT NOT NULL COMMENT '化石燃料采集范围ID，关联emission_fossil_fuel_collection_scope表',
    sort_order INT DEFAULT 0 COMMENT '排序顺序，数值越小越靠前',
    description VARCHAR(500) COMMENT '描述信息',
    status INT DEFAULT 1 COMMENT '状态：1-启用，0-停用',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (collection_scope_id) REFERENCES emission_fossil_fuel_collection_scope(id) ON DELETE CASCADE
) AUTO_INCREMENT = 9000 ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='化石燃料采集细分范围定义表';

-- 化石燃料用量信息表采集点表：存储化石燃料计量表采集点信息
CREATE TABLE IF NOT EXISTS emission_fossil_fuel_meter_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '采集点名称',
    pinyin_code VARCHAR(50) COMMENT '拼音首字母编码，用于快速搜索',
    sub_scope_id BIGINT NOT NULL COMMENT '所属细分范围ID，关联emission_fossil_fuel_collection_sub_scope表',
    emission_subcategory VARCHAR(50) COMMENT '化石燃料种类（由数据字典中化石燃料大类所属的表项定义）',
    meter_model VARCHAR(30) COMMENT '计量表型号',
    purpose_description VARCHAR(500) COMMENT '用途描述',
    parent_meter_id BIGINT DEFAULT 0 COMMENT '上级计量表ID，缺省为0表示无上级',
    is_cumulative TINYINT DEFAULT 0 COMMENT '是否累加量：1-是，0-否',
    is_mobile_source TINYINT DEFAULT 0 COMMENT '是否移动源：1-是，0-否',
    measurement_unit VARCHAR(50) COMMENT '计量单位',
    data_source_system VARCHAR(100) COMMENT '数据来源系统',
    billing_cycle_unit TINYINT DEFAULT 2 COMMENT '计量周期单位：1：周、2：月、3：季度、4：年',
    billing_cycle_start_date TINYINT DEFAULT 1 COMMENT '计量周期起始日期偏移量',
    billing_cycle_length SMALLINT DEFAULT 1 COMMENT '计量周期长度',
    energy_category_l1 VARCHAR(50) COMMENT '能耗一级分类编码，关联emission_energy_category表',
    energy_category_l2 VARCHAR(50) COMMENT '能耗二级分类编码，关联emission_energy_category表',
    energy_category_l3 VARCHAR(50) COMMENT '能耗三级分类编码，关联emission_energy_category表',
    energy_use_category VARCHAR(100) COMMENT '能耗用途分类',
    energy_allocation VARCHAR(20) COMMENT '能耗数据归属',
    meter_reading_method TINYINT DEFAULT 1 COMMENT '抄表方式：1-自动抄表，0-人工录入',
    sort_order INT DEFAULT 0 COMMENT '排序顺序，数值越小越靠前',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-停用',
    last_collection_time TIMESTAMP NULL COMMENT '上次采集时间：完成对该采集点数据采集后记录的最近一次采集时间（对应emission_collection_record.collection_time字段的值）',
    enable_time TIMESTAMP NULL COMMENT '启用时间：当启用该节点后自动记录',
    disable_time TIMESTAMP NULL COMMENT '停用时间：当停用该节点后自动记录',
    energy_use VARCHAR(20) COMMENT '能耗用途：生产用能、生产辅助用能、混合',
    auto_meter_reading_config TEXT COMMENT '自动抄表接口描述（JSON格式）',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (sub_scope_id) REFERENCES emission_fossil_fuel_collection_sub_scope(id) ON DELETE CASCADE
) AUTO_INCREMENT = 90000 ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='化石燃料用量信息表采集点表';

-- 外购热能采集范围定义表：存储外购热能采集范围信息
CREATE TABLE IF NOT EXISTS emission_purchased_heat_collection_scope (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    scope_name VARCHAR(100) NOT NULL UNIQUE COMMENT '采集范围名称',
    pinyin_code VARCHAR(30) COMMENT '拼音首字母编码，用于快速搜索',
    sort_order INT DEFAULT 0 COMMENT '排序顺序，数值越小越靠前',
    description VARCHAR(500) COMMENT '描述信息',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外购热能采集范围定义表';

-- 外购热能采集细分范围定义表：存储外购热能采集细分范围信息
CREATE TABLE IF NOT EXISTS emission_purchased_heat_collection_sub_scope (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    sub_scope_name VARCHAR(100) NOT NULL UNIQUE COMMENT '细分范围名称',
    pinyin_code VARCHAR(30) COMMENT '拼音首字母编码，用于快速搜索',
    collection_scope_id BIGINT NOT NULL COMMENT '外购热能采集范围ID，关联emission_purchased_heat_collection_scope表',
    description VARCHAR(500) COMMENT '描述信息',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (collection_scope_id) REFERENCES emission_purchased_heat_collection_scope(id) ON DELETE CASCADE
) AUTO_INCREMENT = 9000 ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外购热能采集细分范围定义表';

-- 外购热能用量信息表采集点表：存储外购热能计量表采集点信息
CREATE TABLE IF NOT EXISTS emission_purchased_heat_meter_info (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '采集点名称',
    pinyin_code VARCHAR(50) COMMENT '拼音首字母编码，用于快速搜索',
    sub_scope_id BIGINT NOT NULL COMMENT '所属细分范围ID，关联emission_purchased_heat_collection_sub_scope表',
    emission_subcategory VARCHAR(50) COMMENT '热力种类（由数据字典中“购入的热力”大类所属的表项定义）',
    meter_model VARCHAR(30) COMMENT '计量表型号',
    purpose_description VARCHAR(500) COMMENT '用途描述',
    parent_meter_id BIGINT DEFAULT 0 COMMENT '上级计量表ID，缺省为0表示无上级',
    is_cumulative TINYINT DEFAULT 0 COMMENT '是否累加量：1-是，0-否',
    is_mobile_source TINYINT DEFAULT 0 COMMENT '是否移动源：1-是，0-否',
    measurement_unit VARCHAR(50) COMMENT '计量单位',
    data_source_system VARCHAR(100) COMMENT '数据来源系统',
    billing_cycle_unit TINYINT DEFAULT 2 COMMENT '计量周期单位：1：周、2：月、3：季度、4：年',
    billing_cycle_start_date TINYINT DEFAULT 0 COMMENT '计量周期起始日期偏移量，相对于计费单位首日的偏移量',
    billing_cycle_length SMALLINT DEFAULT 1 COMMENT '计量周期长度',
    energy_category_l1 VARCHAR(50) COMMENT '能耗一级分类编码，关联emission_energy_category表',
    energy_category_l2 VARCHAR(50) COMMENT '能耗二级分类编码，关联emission_energy_category表',
    energy_category_l3 VARCHAR(50) COMMENT '能耗三级分类编码，关联emission_energy_category表',
    energy_use_category VARCHAR(100) COMMENT '能耗用途分类',
    energy_allocation VARCHAR(20) COMMENT '能耗数据归属',
    meter_reading_method TINYINT DEFAULT 1 COMMENT '抄表方式：1-自动抄表，0-人工录入',
    sort_order INT DEFAULT 0 COMMENT '排序顺序，数值越小越靠前',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-停用',
    last_collection_time TIMESTAMP NULL COMMENT '上次采集时间：完成对该采集点数据采集后记录的最近一次采集时间（对应emission_collection_record.collection_time字段的值）',
    enable_time TIMESTAMP NULL COMMENT '启用时间：当启用该节点后自动记录',
    disable_time TIMESTAMP NULL COMMENT '停用时间：当停用该节点后自动记录',
    energy_use VARCHAR(20) COMMENT '能耗用途：生产用能、生产辅助用能、混合',
    auto_meter_reading_config TEXT COMMENT '自动抄表接口描述（JSON格式）',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    FOREIGN KEY (sub_scope_id) REFERENCES emission_purchased_heat_collection_sub_scope(id) ON DELETE CASCADE
) AUTO_INCREMENT = 90000 ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='外购热能用量信息表采集点表';

-- 采集记录表：针对 emission_meter_info、emission_fossil_fuel_meter_info、
-- emission_purchased_heat_meter_info 三张表记录的采集点，
-- 每次执行数据采集时生成一条记录，记录本次读取的数值、采集时间、
-- 采集状态、最后一次读取时间以及根据采集点计费周期配置计算得到的
-- 本计费周期起止日期等信息。
CREATE TABLE IF NOT EXISTS emission_collection_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    collection_point_type TINYINT NOT NULL COMMENT '采集点类型：1-电力表，2-化石燃料，3-外购热能',
    collection_point_id BIGINT NOT NULL COMMENT '采集点ID，关联emission_meter_info/emission_fossil_fuel_meter_info/emission_purchased_heat_meter_info表的主键（由collection_point_type决定指向）',
    reading_value DECIMAL(18, 6) COMMENT '本次采集读取的数值',
    collection_time TIMESTAMP NULL COMMENT '本次采集时间：指系统从计量表直接采集数据的时刻。该数据可能已由电力系统等外部系统在预设时间完成读取并存储',
    collection_status TINYINT DEFAULT 0 COMMENT '本次采集数据状态：0-还未执行读取操作，1-读取成功，2-读取失败，3-多次读取失败后取消',
    last_fetch_time TIMESTAMP NULL COMMENT '最后一次读取时间：指系统从外部系统获取其已存储数据的时刻',
    collection_point_name VARCHAR(100) COMMENT '采集点名称，对应emission_meter_info/emission_fossil_fuel_meter_info/emission_purchased_heat_meter_info表的name字段',
    emission_subcategory VARCHAR(50) COMMENT '能耗品种小类（电力表为用电分类如外购电力；化石燃料为燃料种类；外购热能为热力种类）',
    measurement_unit VARCHAR(50) COMMENT '计量单位',
    meter_reading_method TINYINT DEFAULT 1 COMMENT '抄表方式：1-自动抄表，0-人工录入',
    is_cumulative TINYINT DEFAULT 0 COMMENT '是否累加量：1-是，0-否',
    billing_cycle_start_date DATE COMMENT '本计量周期起始日期：根据采集点billing_cycle_unit、billing_cycle_start_date、billing_cycle_length三个字段计算得到',
    billing_cycle_end_date DATE COMMENT '本计量周期截止日期：根据采集点billing_cycle_unit、billing_cycle_start_date、billing_cycle_length三个字段计算得到',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '本条记录插入表的时间（系统时间戳）',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_collection_point (collection_point_type, collection_point_id) COMMENT '采集点类型+ID联合索引，用于按采集点查询历史记录',
    INDEX idx_collection_time (collection_time) COMMENT '采集时间索引，用于按时间范围查询',
    INDEX idx_collection_status (collection_status) COMMENT '采集状态索引，用于查找未读取(0)或失败(2)的数据进行重新读取',
    INDEX idx_created_at (created_at) COMMENT '插入时间索引，用于按记录生成时间排序查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采集记录表';

-- ============================================================
-- 碳排放核算相关表（核算模版/核算节点/核算节点数据）
-- 说明：核算模版表每次执行核算时生成一条记录，记录本次核算的
--       周期、执行状态、发起人等信息；核算节点表与核算节点数据表
--       分别快照本次核算涉及的节点结构和各节点能耗/碳排放计算结果。
-- ============================================================

-- 碳排放核算模版表：每次核算任务生成一条记录
CREATE TABLE IF NOT EXISTS emission_calculation_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    template_id BIGINT NOT NULL COMMENT '需要核实的碳排放模版ID，外键关联emission_template.id',
    calculation_cycle_start_date DATE COMMENT '核算周期起始日期（只保留日期部分）',
    calculation_cycle_end_date DATE COMMENT '核算周期截止日期（只保留日期部分）',
    calculation_start_time TIMESTAMP NULL COMMENT '核算开始执行时间',
    calculation_end_time TIMESTAMP NULL COMMENT '核算结束时间',
    status TINYINT DEFAULT 0 COMMENT '核算状态：0-还未开始执行，1-正在执行，2-执行成功完成，3-执行出错失败',
    error_code INT DEFAULT 0 COMMENT '执行失败错误代码：0-成功（没有出错）',
    task_initiator TINYINT DEFAULT 1 COMMENT '任务发起者：1-系统自动发起，2-人工手动发起',
    initiator_id BIGINT DEFAULT 0 COMMENT '发起人ID：task_initiator=2时填写发起人ID，否则为0',
    initiator_name VARCHAR(100) COMMENT '发起人姓名',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '该记录创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_template_id (template_id) COMMENT '模版ID索引，用于按模版查询核算历史',
    INDEX idx_status (status) COMMENT '状态索引，用于查找待执行/失败需重试的核算任务',
    INDEX idx_cycle_dates (calculation_cycle_start_date, calculation_cycle_end_date) COMMENT '核算周期索引，用于按时间范围查询',
    FOREIGN KEY (template_id) REFERENCES emission_template(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='碳排放核算模版表';

-- 碳排放核算节点表：快照本次核算涉及的节点树结构
CREATE TABLE IF NOT EXISTS emission_calculation_node (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    calculation_template_id BIGINT NOT NULL COMMENT '对应emission_calculation_template.id（本次核算任务记录ID）',
    node_id BIGINT NOT NULL COMMENT '对应的节点ID，关联emission_node.id',
    name VARCHAR(200) NOT NULL COMMENT '节点名称',
    type_id INT NOT NULL COMMENT '节点类型ID，关联emission_node_type表',
    parent_id BIGINT COMMENT '父节点ID（对应本表中的id，用于核算节点树层级）',
    node_category VARCHAR(50) COMMENT '节点分类：当type_id对应核算子节点类型时，取值与emission_node_info.node_category一致（总公司/分公司/站点/区域）',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_calculation_template_id (calculation_template_id) COMMENT '核算任务ID索引，用于按核算任务查询节点快照',
    INDEX idx_node_id (node_id) COMMENT '节点ID索引，用于按源节点查询核算历史',
    FOREIGN KEY (calculation_template_id) REFERENCES emission_calculation_template(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='碳排放核算节点表';

-- 碳排放采集节点数据表：各采集节点本次核算的能耗与碳排放计算结果
CREATE TABLE IF NOT EXISTS emission_collection_node_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    calculation_node_id BIGINT NOT NULL COMMENT '对应emission_calculation_node.id（核算节点记录ID）',
    collection_point_type TINYINT COMMENT '采集点类型：1-电力表，2-化石燃料，3-外购热能（仅采集节点有值，复制自emission_node_config.collection_point_type）',
    collection_point_id BIGINT COMMENT '采集点ID：仅当节点为采集节点(type_id=3)时有值，指向对应采集点表主键',
    emission_category VARCHAR(100) COMMENT '能耗品种大类',
    emission_subcategory VARCHAR(100) COMMENT '能耗品种小类：采集节点时由对应采集点表记录提供',
    carbon_emission_factor DECIMAL(15,6) COMMENT '碳排放因子',
    is_cumulative TINYINT DEFAULT 0 COMMENT '是否累加量：1-是，0-否。采集节点时由对应采集点表记录提供',
    is_mobile_source TINYINT DEFAULT 0 COMMENT '是否移动源：1-是，0-否。采集节点时由对应采集点表记录提供',
    measurement_unit VARCHAR(50) COMMENT '计量单位：采集节点时由对应采集点表记录提供',
    energy_category_l1 VARCHAR(50) COMMENT '能耗一级分类编码：采集节点时由对应采集点表记录提供',
    energy_category_l2 VARCHAR(50) COMMENT '能耗二级分类编码：采集节点时由对应采集点表记录提供',
    energy_category_l3 VARCHAR(50) COMMENT '能耗三级分类编码：采集节点时由对应采集点表记录提供',
    energy_use_category VARCHAR(100) COMMENT '能耗用途分类：采集节点时由对应采集点表记录提供',
    energy_measurement_value DECIMAL(18,6) COMMENT '能耗计量值：从emission_collection_record统计得出的核算周期内能耗值',
    carbon_emission DECIMAL(18,6) COMMENT '折算的碳排放量',
    adjusted_energy_value DECIMAL(18,6) COMMENT '经过计算修正后的能耗计量值',
    adjusted_carbon_emission DECIMAL(18,6) COMMENT '经过计算修正后的碳排放量',
    total_emission_calc_method TINYINT DEFAULT 1 COMMENT '总排放核算方式：1-缺失情况按修正后的计量值计算，2-缺失情况按计量值计算',
    collection_record_start_date DATE COMMENT '采集记录的起始日期：所选择采集点记录中记录的采集起始日期',
    collection_record_end_date DATE COMMENT '采集记录的截止日期：所选择采集点记录中记录的采集截止日期',
    data_status TINYINT COMMENT '数据状态：1-完整，2-没有数据，3-数据不完整，4-数据超范围，5-数据不完整且超范围',
    data_missing_description VARCHAR(500) COMMENT '数据缺失说明：data_status不为1时给出缺失/超限的具体说明',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_calculation_node_id (calculation_node_id) COMMENT '核算节点ID索引，用于按节点查询核算数据',
    INDEX idx_collection_point (collection_point_type, collection_point_id) COMMENT '采集点联合索引，用于按采集点查询核算历史',
    INDEX idx_data_status (data_status) COMMENT '数据状态索引，用于筛选数据异常的核算记录',
    FOREIGN KEY (calculation_node_id) REFERENCES emission_calculation_node(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='碳排放采集节点数据表';

-- ============================================================
-- 核算节点能耗统计汇总表 emission_calc_node_summary
-- 说明：一行对应「一次核算任务 × 一个核算/根节点 × 一个场景三级组合 × 一个排放小类」
--       的统计结果。按 emission_subcategory 的 calculation_unit 统一换算后累加。
--       direct_*：本节点自身直接挂载采集点的统计
--       subtotal_*：direct_* 加上所有直属子核算/根节点 subtotal_*（即子树下全量）
-- ============================================================
CREATE TABLE IF NOT EXISTS emission_calc_node_summary (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    calculation_template_id BIGINT NOT NULL COMMENT '核算任务ID（关联 emission_calculation_template.id）',
    calculation_node_id BIGINT NOT NULL COMMENT '本次核算节点快照ID（关联 emission_calculation_node.id）',
    source_node_id BIGINT COMMENT '源节点ID（关联 emission_node.id，便于按组织架构查询）',
    parent_calc_node_id BIGINT COMMENT '父核算快照节点ID（便于向上回溯层级）',
    node_level INT NOT NULL COMMENT '节点在本次核算树中的层级，根节点=1',
    is_leaf_calc_node TINYINT DEFAULT 0 COMMENT '是否最底层核算节点：1-是（子节点全为采集节点type=3），0-否',
    energy_category_l1 VARCHAR(50) COMMENT '能耗场景一级分类编码',
    energy_category_l2 VARCHAR(50) COMMENT '能耗场景二级分类编码',
    energy_category_l3 VARCHAR(50) COMMENT '能耗场景三级分类编码',
    emission_category VARCHAR(100) COMMENT '排放数据大类（冗余：PE/PH/FF/WT，由subcategory反查parent_code）',
    emission_subcategory VARCHAR(100) NOT NULL COMMENT '排放数据小类编码（分组主键维度）',
    direct_energy_value DECIMAL(18,6) DEFAULT 0 COMMENT '本节点直接能耗（本节点挂载采集点贡献，统一到calculation_unit）',
    direct_carbon_emission DECIMAL(18,6) DEFAULT 0 COMMENT '本节点直接碳排放',
    subtotal_energy_value DECIMAL(18,6) DEFAULT 0 COMMENT '本节点子树合计能耗（direct+子节点subtotal累加）',
    subtotal_carbon_emission DECIMAL(18,6) DEFAULT 0 COMMENT '本节点子树合计碳排放',
    calculation_unit_code VARCHAR(30) COMMENT '本小类核算单位（冗余 emission_calc_unit_default.calculation_unit）',
    carbon_emission_factor DECIMAL(15,6) COMMENT '本节点该组使用的加权平均碳因子',
    summary_source TINYINT COMMENT '数据来源：1-仅聚合自身采集点, 2-仅汇总子节点, 3=两者都有',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_calc_node_group (calculation_node_id, energy_category_l1, energy_category_l2, energy_category_l3, emission_subcategory),
    INDEX idx_calc_template (calculation_template_id) COMMENT '核算任务索引',
    INDEX idx_source_node_cycle (source_node_id, calculation_template_id) COMMENT '源节点+任务索引',
    INDEX idx_template_subcategory (calculation_template_id, emission_subcategory) COMMENT '任务+小类索引',
    FOREIGN KEY (calculation_template_id) REFERENCES emission_calculation_template(id) ON DELETE CASCADE,
    FOREIGN KEY (calculation_node_id) REFERENCES emission_calculation_node(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='核算节点能耗统计汇总表（逐级汇总中间+最终结果）';

-- ============================================================
-- 碳排放核算能耗缺省单位表
-- 说明：按 emission_subcategory（排放数据小类）为每种能耗
--       提供两种缺省单位：
--       1) calculation_unit  ：核算汇总时统一使用的计量单位
--       2) report_unit       ：碳排放报告中展示的计量单位
--       两者不同时通过 conversion_factor 进行换算，公式：
--           报告单位数值 = 核算单位数值 × conversion_factor
--       例如柴油：calculation_unit=升(L)，report_unit=吨(t)，
--       conversion_factor=0.00085（1升柴油≈0.85kg=0.00085吨）
-- ============================================================
CREATE TABLE IF NOT EXISTS emission_calc_unit_default (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    subcategory_code VARCHAR(50) NOT NULL UNIQUE COMMENT '排放数据小类编码（对应emission_data_dict_item.item_code，字典dict_code=emission_subcategory）',
    subcategory_name VARCHAR(100) NOT NULL COMMENT '排放数据小类名称（对应emission_data_dict_item.item_value，便于展示无需关联字典）',
    calculation_unit VARCHAR(30) NOT NULL COMMENT '核算计量单位：进行数据汇总时统一的计量单位（值必须为 emission_unit_standard.unit_code）',
    report_unit VARCHAR(30) NOT NULL COMMENT '碳排放报告计量单位：核算报告中展示的计量单位（值必须为 emission_unit_standard.unit_code）',
    remark VARCHAR(500) COMMENT '备注说明',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='碳排放核算能耗缺省单位表';

-- 初始化 22 条排放数据小类的缺省单位（对应 emission_data_dict_item 中 dict_code=emission_subcategory 的项）
INSERT IGNORE INTO emission_calc_unit_default
    (subcategory_code, subcategory_name, calculation_unit, report_unit, remark) VALUES
    ('PE_PF',  '外购电力',       'kWh', 'MWh', '核算单位与报告单位一致'),
    ('PE_AS',  '新能源发电（自发自用）',   'kWh', 'MWh', '核算单位与报告单位一致'),
    ('PH_HD',  '热力数据',           'GJ',  'GJ',  '能量单位计量，核算单位与报告单位一致'),
    ('PH_HW',  '质量单位计量的热水', 't',   't',   '质量单位计量，核算单位与报告单位一致'),
    ('PH_HS',  '质量单位计量的蒸汽', 't',   't',   '质量单位计量，核算单位与报告单位一致'),
    ('PH_HA',  '按供热面积计量的暖气','m2', 'm2',  '面积单位计量，核算单位与报告单位一致；与GJ的换算由 emission_unit_conversion 提供'),
    ('FF_G',   '汽油',               'L',   't',   '核算单位升(L)，报告单位吨(t)；换算系数见 emission_unit_conversion'),
    ('FF_D',   '柴油',               'L',   't',   '核算单位升(L)，报告单位吨(t)；换算系数见 emission_unit_conversion'),
    ('FF_C',   '原油',               'L',   't',   '核算单位升(L)，报告单位吨(t)；换算系数见 emission_unit_conversion'),
    ('FF_F',   '燃料油',             'L',   't',   '核算单位升(L)，报告单位吨(t)；换算系数见 emission_unit_conversion'),
    ('FF_LNG', '液化天然气',         'L',   't',   '核算单位升(L)，报告单位吨(t)；换算系数见 emission_unit_conversion'),
    ('FF_LPG', '液化石油气',         'L',   't',   '核算单位升(L)，报告单位吨(t)；换算系数见 emission_unit_conversion'),
    ('FF_NG',  '天然气',             'm3',  'wan_m3', '核算单位立方米(m³)，报告单位万立方米(万m³)；换算系数见 emission_unit_conversion'),
    ('FF_BFG', '高炉煤气',           'm3',  'wan_m3', '核算单位立方米(m³)，报告单位万立方米(万m³)；换算系数见 emission_unit_conversion'),
    ('FF_COG', '转炉煤气',           'm3',  'wan_m3', '核算单位立方米(m³)，报告单位万立方米(万m³)；换算系数见 emission_unit_conversion'),
    ('FF_COK', '焦炉煤气',           'm3',  'wan_m3', '核算单位立方米(m³)，报告单位万立方米(万m³)；换算系数见 emission_unit_conversion'),
    ('FF_B',   '烟煤',               'kg',  't',   '核算单位千克(kg)，报告单位吨(t)；换算系数见 emission_unit_conversion'),
    ('FF_LB',  '褐煤',               'kg',  't',   '核算单位千克(kg)，报告单位吨(t)；换算系数见 emission_unit_conversion'),
    ('FF_CK',  '焦炭',               'kg',  't',   '核算单位千克(kg)，报告单位吨(t)；换算系数见 emission_unit_conversion'),
    ('FF_PC',  '石油焦',             'kg',  't',   '核算单位千克(kg)，报告单位吨(t)；换算系数见 emission_unit_conversion'),
    ('WT_SW',  '固体废弃物处理排放', 't',   't',   '核算单位与报告单位一致'),
    ('WT_WW',  '废水处理排放',       't',   't',   '核算单位与报告单位一致');

-- ============================================================
-- 碳排放核算因子模版表 emission_factor_template
-- 说明：管理多个碳排放因子设置库（模版），每个模版可独立配置各能耗
--       小类的碳排放因子。模版可设为共享（所有人可用）或私有（仅创建
--       人可用），缺省共享。模版启用/停用控制其可用性。
-- ============================================================
CREATE TABLE IF NOT EXISTS emission_factor_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    template_name VARCHAR(200) NOT NULL COMMENT '模版名称',
    template_description VARCHAR(1000) COMMENT '模版说明',
    is_shared TINYINT DEFAULT 1 COMMENT '是否共享：1-共享（所有人可用），0-私有（仅创建人可用）',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-停用',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_status (status) COMMENT '状态索引',
    INDEX idx_created_by (created_by) COMMENT '创建人索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='碳排放因子模版表';

-- ============================================================
-- 系统缺省碳排放因子表 emission_default_factor
-- 说明：针对不同能耗小类（emission_subcategory）设置系统默认使用的
--       碳排放因子。用户可添加能耗小类，然后从对应因子库（电力/化石
--       燃料/热力/废弃物焚烧/废水处理）中选择一条因子作为缺省值。
--       factor_source 标记因子来源表，factor_id 为该表主键；
--       factor_name/factor_value/factor_unit/factor_description 为
--       选择时的快照，便于展示与历史稳定，因子库后续修改不影响已选快照。
--       template_id 关联 emission_factor_template.id（NULL=系统缺省）。
-- ============================================================
CREATE TABLE IF NOT EXISTS emission_default_factor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    template_id BIGINT COMMENT '所属因子模版ID（NULL=系统缺省，非NULL=模版级缺省因子）',
    subcategory_code VARCHAR(50) NOT NULL COMMENT '排放数据小类编码（字典 emission_subcategory 的 item_code）',
    subcategory_name VARCHAR(100) COMMENT '排放数据小类名称（冗余 item_value 便于展示）',
    factor_source VARCHAR(30) NOT NULL COMMENT '因子库来源：ELECTRICITY-电力因子库，FOSSIL-化石燃料因子库，THERMAL-热力因子库，WASTE_INCINERATION-固废焚烧因子库，WASTEWATER-废水处理因子库',
    factor_id BIGINT COMMENT '所选因子在对应因子库表中的主键ID',
    factor_name VARCHAR(200) COMMENT '所选因子名称（快照）',
    factor_value DECIMAL(18,6) COMMENT '所选因子值（快照；热水/蒸汽为经温度计算后的折算值）',
    factor_unit VARCHAR(50) COMMENT '所选因子单位（快照）',
    factor_description VARCHAR(1000) COMMENT '所选因子说明（快照）',
    remark VARCHAR(500) COMMENT '备注说明',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-停用',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_template_subcategory (template_id, subcategory_code) COMMENT '模版+小类联合唯一',
    INDEX idx_template_id (template_id) COMMENT '模版ID索引',
    INDEX idx_subcategory_code (subcategory_code) COMMENT '小类编码索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统缺省碳排放因子设置表';

-- ============================================================
-- 计量标准单位表
-- 统一程序内涉及到的所有计量单位，作为单位字段下拉选项唯一数据源
-- 禁止人工录入非标准单位
-- ============================================================
CREATE TABLE IF NOT EXISTS emission_unit_standard (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    unit_code VARCHAR(30) NOT NULL UNIQUE COMMENT '单位编码（程序内统一编码，如 kWh/MWh/GJ/L/m3/wan_m3/kg/t/g/m2）',
    unit_name VARCHAR(50) NOT NULL COMMENT '单位中文名称（用于展示，如 千瓦时/兆瓦时/吉焦/升/立方米/吨）',
    unit_category VARCHAR(20) NOT NULL COMMENT '单位大类：ELECTRIC-电力、HEAT-热力（能量）、LIQUID_FUEL-液体燃料、GAS_FUEL-气体燃料、SOLID_FUEL-固体燃料、AREA-面积',
    physical_quantity VARCHAR(20) NOT NULL COMMENT '物理量：ENERGY-能量、VOLUME-体积、MASS-质量、AREA-面积',
    is_base TINYINT DEFAULT 0 COMMENT '是否本类基准单位：1-是，0-否。每类一个基准，如电力 kWh、液体燃料 L',
    sort_order INT DEFAULT 0 COMMENT '排序顺序，数值越小越靠前',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-停用',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计量标准单位表';

INSERT IGNORE INTO emission_unit_standard
    (unit_code, unit_name, unit_category, physical_quantity, is_base, sort_order) VALUES
    ('kWh',    '千瓦时(kWh)',   'ELECTRIC',     'ENERGY', 1, 1),
    ('MWh',    '兆瓦时(MWh)',   'ELECTRIC',     'ENERGY', 0, 2),
    ('GJ',     '吉焦(GJ)',     'HEAT',         'ENERGY', 1, 3),
    ('MJ',     '兆焦(MJ)',     'HEAT',         'ENERGY', 0, 4),
    ('kJ',     '千焦(kJ)',     'HEAT',         'ENERGY', 0, 5),
    ('L',      '升(L)',       'LIQUID_FUEL',  'VOLUME', 1, 6),
    ('m3',     '立方米(m³)',   'GAS_FUEL',     'VOLUME', 1, 7),
    ('wan_m3', '万立方米(万m³)', 'GAS_FUEL',     'VOLUME', 0, 8),
    ('Nm3',    '标准立方米(Nm³)', 'GAS_FUEL',  'VOLUME', 0, 13),
    ('wan_Nm3','标准万立方米(万Nm³)', 'GAS_FUEL','VOLUME',0, 14),
    ('kg',     '千克(kg)',     'SOLID_FUEL',   'MASS',   1, 9),
    ('t',      '吨(t)',       'SOLID_FUEL',   'MASS',   0, 10),
    ('g',      '克(g)',       'SOLID_FUEL',   'MASS',   0, 11),
    ('m2',     '平方米(m²)',   'AREA',         'AREA',   1, 12);

-- 温室气体质量单位表：碳排放因子的分子单位
CREATE TABLE IF NOT EXISTS emission_ghg_unit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    ghg_code VARCHAR(30) NOT NULL UNIQUE COMMENT '温室气体质量单位编码（ASCII，如 kgCO2、tCO2、kgCH4、tCH4、kgN2O、tN2O）',
    ghg_name VARCHAR(100) NOT NULL COMMENT '展示名称（如 千克二氧化碳、吨二氧化碳）',
    ghg_type VARCHAR(10) NOT NULL COMMENT '温室气体种类：CO2、CH4、N2O',
    mass_unit_code VARCHAR(30) NOT NULL COMMENT '质量单位编码（关联 emission_unit_standard.unit_code，如 kg、t、g）',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-停用',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='温室气体质量单位表';

-- 初始化温室气体质量单位（6 条）
INSERT IGNORE INTO emission_ghg_unit
    (ghg_code, ghg_name, ghg_type, mass_unit_code, sort_order) VALUES
    ('kgCO2', '千克二氧化碳(kgCO₂)', 'CO2', 'kg', 1),
    ('tCO2',  '吨二氧化碳(tCO₂)',   'CO2', 't',  2),
    ('kgCH4', '千克甲烷(kgCH₄)',     'CH4', 'kg', 3),
    ('tCH4',  '吨甲烷(tCH₄)',       'CH4', 't',  4),
    ('kgN2O', '千克氧化亚氮(kgN₂O)', 'N2O', 'kg', 5),
    ('tN2O',  '吨氧化亚氮(tN₂O)',   'N2O', 't',  6);

-- 碳排放因子单位表：每行一个合法的因子单位组合（分子+分母）
-- 按排放数据小类限定可选因子单位
CREATE TABLE IF NOT EXISTS emission_factor_unit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    factor_unit_code VARCHAR(50) NOT NULL UNIQUE COMMENT '因子单位编码（ASCII，如 kgCO2_per_kWh）',
    factor_unit_name VARCHAR(100) NOT NULL COMMENT '展示名称（如 kgCO₂/kWh）',
    numerator_ghg_code VARCHAR(30) NOT NULL COMMENT '分子单位编码（关联 emission_ghg_unit.ghg_code）',
    denominator_unit_code VARCHAR(30) NOT NULL COMMENT '分母单位编码（关联 emission_unit_standard.unit_code）',
    subcategory_code VARCHAR(50) COMMENT '适用排放数据小类编码（关联 emission_data_dict_item.item_code）；为空表示通用',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态：1-启用，0-停用',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_factor_unit_subcategory (subcategory_code) COMMENT '按小类查询索引',
    INDEX idx_factor_unit_numerator (numerator_ghg_code) COMMENT '按分子查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='碳排放因子单位表';

-- 初始化因子单位（覆盖现有 8 处硬编码的全部场景，按小类限定）
-- 电力（PE_PF/PE_AS/PE_SS）：kWh、MWh
INSERT IGNORE INTO emission_factor_unit
    (factor_unit_code, factor_unit_name, numerator_ghg_code, denominator_unit_code, subcategory_code, sort_order) VALUES
    ('kgCO2_per_kWh', 'kgCO₂/kWh', 'kgCO2', 'kWh', 'PE_PF', 1),
    ('tCO2_per_MWh',  'tCO₂/MWh',  'tCO2',  'MWh', 'PE_PF', 2),
    ('kgCO2_per_kWh_AS', 'kgCO₂/kWh', 'kgCO2', 'kWh', 'PE_AS', 1),
    ('tCO2_per_MWh_AS',  'tCO₂/MWh',  'tCO2',  'MWh', 'PE_AS', 2),
    ('kgCO2_per_kWh_SS', 'kgCO₂/kWh', 'kgCO2', 'kWh', 'PE_SS', 1),
    ('tCO2_per_MWh_SS',  'tCO₂/MWh',  'tCO2',  'MWh', 'PE_SS', 2);

-- 化石燃料（液体/固体按吨）：FF_B/LB/CK/PC/CG/DF/F/LNG/LPG
INSERT IGNORE INTO emission_factor_unit
    (factor_unit_code, factor_unit_name, numerator_ghg_code, denominator_unit_code, subcategory_code, sort_order) VALUES
    ('tCO2_per_t_B',  'tCO₂/t', 'tCO2', 't', 'FF_B',  1),
    ('tCO2_per_t_LB', 'tCO₂/t', 'tCO2', 't', 'FF_LB', 1),
    ('tCO2_per_t_CK', 'tCO₂/t', 'tCO2', 't', 'FF_CK', 1),
    ('tCO2_per_t_PC', 'tCO₂/t', 'tCO2', 't', 'FF_PC', 1),
    ('tCO2_per_t_FF_G', 'tCO₂/t', 'tCO2', 't', 'FF_G',  1),
    ('tCO2_per_t_FF_D', 'tCO₂/t', 'tCO2', 't', 'FF_D',  1),
    ('tCO2_per_t_FF_C', 'tCO₂/t', 'tCO2', 't', 'FF_C',  1),
    ('tCO2_per_t_FF_F', 'tCO₂/t', 'tCO2', 't', 'FF_F',  1),
    ('tCO2_per_t_FF_LNG','tCO₂/t','tCO2', 't', 'FF_LNG',1),
    ('tCO2_per_t_FF_LPG','tCO₂/t','tCO2', 't', 'FF_LPG',1);

-- 化石燃料（气体按万Nm³）：FF_NG/BFG/COG/COK
INSERT IGNORE INTO emission_factor_unit
    (factor_unit_code, factor_unit_name, numerator_ghg_code, denominator_unit_code, subcategory_code, sort_order) VALUES
    ('tCO2_per_wanNm3_NG',  'tCO₂/万Nm³', 'tCO2', 'wan_Nm3', 'FF_NG',  1),
    ('tCO2_per_wanNm3_BFG', 'tCO₂/万Nm³', 'tCO2', 'wan_Nm3', 'FF_BFG', 1),
    ('tCO2_per_wanNm3_COG', 'tCO₂/万Nm³', 'tCO2', 'wan_Nm3', 'FF_COG', 1),
    ('tCO2_per_wanNm3_COK', 'tCO₂/万Nm³', 'tCO2', 'wan_Nm3', 'FF_COK', 1);

-- 热力（PH_HD）：GJ
INSERT IGNORE INTO emission_factor_unit
    (factor_unit_code, factor_unit_name, numerator_ghg_code, denominator_unit_code, subcategory_code, sort_order) VALUES
    ('tCO2_per_GJ_HD',  'tCO₂/GJ',  'tCO2',  'GJ', 'PH_HD', 1),
    ('kgCO2_per_GJ_HD', 'kgCO₂/GJ', 'kgCO2', 'GJ', 'PH_HD', 2);

-- 热力（质量计量的热水/蒸汽 PH_HW/PH_HS）：t
INSERT IGNORE INTO emission_factor_unit
    (factor_unit_code, factor_unit_name, numerator_ghg_code, denominator_unit_code, subcategory_code, sort_order) VALUES
    ('tCO2_per_t_HW', 'tCO₂/t', 'tCO2', 't', 'PH_HW', 1),
    ('tCO2_per_t_HS', 'tCO₂/t', 'tCO2', 't', 'PH_HS', 1);

-- 热力（按面积计量的暖气 PH_HA）：m²
INSERT IGNORE INTO emission_factor_unit
    (factor_unit_code, factor_unit_name, numerator_ghg_code, denominator_unit_code, subcategory_code, sort_order) VALUES
    ('tCO2_per_m2_HA', 'tCO₂/m²', 'tCO2', 'm2', 'PH_HA', 1);

-- 废弃物（WT_SW/WT_WW）
INSERT IGNORE INTO emission_factor_unit
    (factor_unit_code, factor_unit_name, numerator_ghg_code, denominator_unit_code, subcategory_code, sort_order) VALUES
    ('tCO2_per_m3_WW', 'tCO₂/m³', 'tCO2', 'm3', 'WT_WW', 1),
    ('kgCO2_per_m3_WW','kgCO₂/m³','kgCO2','m3', 'WT_WW', 2),
    ('tCO2_per_t_SW',  'tCO₂/t',  'tCO2', 't',  'WT_SW', 1),
    ('kgCO2_per_t_SW', 'kgCO₂/t', 'kgCO2','t',  'WT_SW', 2);

-- ============================================================
-- 单位转换系数表
-- 每对小类+单位对一条记录，双向成对存储
-- 转换公式：目标值 = 源值 × conversion_factor
-- ============================================================
CREATE TABLE IF NOT EXISTS emission_unit_conversion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    subcategory_code VARCHAR(50) NOT NULL COMMENT '排放数据小类编码（对应emission_data_dict_item.item_code，字典dict_code=emission_subcategory）',
    from_unit_code VARCHAR(30) NOT NULL COMMENT '源单位编码（关联emission_unit_standard.unit_code）',
    to_unit_code VARCHAR(30) NOT NULL COMMENT '目标单位编码（关联emission_unit_standard.unit_code）',
    conversion_factor DECIMAL(18,8) NOT NULL COMMENT '转换系数：目标值 = 源值 × conversion_factor',
    remark VARCHAR(500) COMMENT '备注说明',
    created_by BIGINT COMMENT '创建人ID',
    updated_by BIGINT COMMENT '更新人ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_subcat_from_to (subcategory_code, from_unit_code, to_unit_code),
    INDEX idx_subcategory (subcategory_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单位转换系数表';

-- 电力类（PE_PF/PE_AS/PE_SS）：kWh ↔ MWh
INSERT IGNORE INTO emission_unit_conversion
    (subcategory_code, from_unit_code, to_unit_code, conversion_factor, remark) VALUES
    ('PE_PF', 'kWh', 'MWh', 0.001,  '1 MWh = 1000 kWh'),
    ('PE_PF', 'MWh', 'kWh', 1000,    '反向'),
    ('PE_AS', 'kWh', 'MWh', 0.001,  '1 MWh = 1000 kWh'),
    ('PE_AS', 'MWh', 'kWh', 1000,    '反向');

-- 热力-能量（PH_HD）：GJ ↔ MJ ↔ kJ
INSERT IGNORE INTO emission_unit_conversion
    (subcategory_code, from_unit_code, to_unit_code, conversion_factor, remark) VALUES
    ('PH_HD', 'GJ', 'MJ', 1000,      '1 GJ = 1000 MJ'),
    ('PH_HD', 'MJ', 'GJ', 0.001,     '反向'),
    ('PH_HD', 'GJ', 'kJ', 1000000,    '1 GJ = 1,000,000 kJ'),
    ('PH_HD', 'kJ', 'GJ', 0.000001,  '反向'),
    ('PH_HD', 'MJ', 'kJ', 1000,      '1 MJ = 1000 kJ'),
    ('PH_HD', 'kJ', 'MJ', 0.001,     '反向');

-- 热力-热水（PH_HW，质量单位）：t ↔ kg
INSERT IGNORE INTO emission_unit_conversion
    (subcategory_code, from_unit_code, to_unit_code, conversion_factor, remark) VALUES
    ('PH_HW', 't',  'kg', 1000,  '1 t = 1000 kg'),
    ('PH_HW', 'kg', 't',  0.001, '反向');

-- 热力-蒸汽（PH_HS，质量单位）：t ↔ kg
INSERT IGNORE INTO emission_unit_conversion
    (subcategory_code, from_unit_code, to_unit_code, conversion_factor, remark) VALUES
    ('PH_HS', 't',  'kg', 1000,  '1 t = 1000 kg'),
    ('PH_HS', 'kg', 't',  0.001, '反向');

-- 热力-暖气面积（PH_HA）：m² ↔ GJ（系数由用户自定，先填占位 0.08 GJ/m²）
INSERT IGNORE INTO emission_unit_conversion
    (subcategory_code, from_unit_code, to_unit_code, conversion_factor, remark) VALUES
    ('PH_HA', 'm2', 'GJ', 0.08,  '占位系数：每平方米供暖季约 0.08 GJ，用户可在页面调整'),
    ('PH_HA', 'GJ', 'm2', 12.5,  '反向占位：1/0.08');

-- 液体燃料（FF_G/D/C/F/LNG/LPG）：L ↔ t
INSERT IGNORE INTO emission_unit_conversion
    (subcategory_code, from_unit_code, to_unit_code, conversion_factor, remark) VALUES
    ('FF_G',   'L', 't', 0.00075,    '汽油密度≈0.75 kg/L'),
    ('FF_G',   't', 'L', 1333.33333, '反向'),
    ('FF_D',   'L', 't', 0.00085,    '柴油密度≈0.85 kg/L'),
    ('FF_D',   't', 'L', 1176.47059, '反向'),
    ('FF_C',   'L', 't', 0.00086,    '原油密度≈0.86 kg/L'),
    ('FF_C',   't', 'L', 1162.79070, '反向'),
    ('FF_F',   'L', 't', 0.00097,    '燃料油密度≈0.97 kg/L'),
    ('FF_F',   't', 'L', 1030.92784, '反向'),
    ('FF_LNG', 'L', 't', 0.000426,   '液化天然气密度≈0.426 kg/L'),
    ('FF_LNG', 't', 'L', 2347.41784, '反向'),
    ('FF_LPG', 'L', 't', 0.00058,    '液化石油气密度≈0.58 kg/L'),
    ('FF_LPG', 't', 'L', 1724.13793, '反向');

-- 气体燃料（FF_NG/BFG/COG/COK）：m³ ↔ 万m³、Nm³ ↔ m³、Nm³ ↔ 万m³、万Nm³ ↔ m³、万Nm³ ↔ 万m³、Nm³ ↔ 万Nm³
INSERT IGNORE INTO emission_unit_conversion
    (subcategory_code, from_unit_code, to_unit_code, conversion_factor, remark) VALUES
    ('FF_NG',  'm3', 'wan_m3', 0.0001, '1 万m³ = 10000 m³'),
    ('FF_NG',  'wan_m3', 'm3', 10000,  '反向'),
    ('FF_NG',  'Nm3', 'm3', 1,           '标准状态下 1 Nm³ ≈ 1 m³'),
    ('FF_NG',  'm3', 'Nm3', 1,           '反向'),
    ('FF_NG',  'Nm3', 'wan_m3', 0.0001, '1 万m³ = 10000 Nm³'),
    ('FF_NG',  'wan_m3', 'Nm3', 10000,  '反向'),
    ('FF_NG',  'wan_Nm3', 'm3', 10000,  '1 万Nm³ = 10000 m³'),
    ('FF_NG',  'm3', 'wan_Nm3', 0.0001, '反向'),
    ('FF_NG',  'wan_Nm3', 'wan_m3', 1,  '1 万Nm³ = 1 万m³'),
    ('FF_NG',  'wan_m3', 'wan_Nm3', 1,  '反向'),
    ('FF_NG',  'Nm3', 'wan_Nm3', 0.0001,'1 万Nm³ = 10000 Nm³'),
    ('FF_NG',  'wan_Nm3', 'Nm3', 10000, '反向'),
    ('FF_BFG', 'm3', 'wan_m3', 0.0001, '1 万m³ = 10000 m³'),
    ('FF_BFG', 'wan_m3', 'm3', 10000,  '反向'),
    ('FF_BFG', 'Nm3', 'm3', 1,           '标准状态下 1 Nm³ ≈ 1 m³'),
    ('FF_BFG', 'm3', 'Nm3', 1,           '反向'),
    ('FF_BFG', 'Nm3', 'wan_m3', 0.0001, '1 万m³ = 10000 Nm³'),
    ('FF_BFG', 'wan_m3', 'Nm3', 10000,  '反向'),
    ('FF_BFG', 'wan_Nm3', 'm3', 10000,  '1 万Nm³ = 10000 m³'),
    ('FF_BFG', 'm3', 'wan_Nm3', 0.0001, '反向'),
    ('FF_BFG', 'wan_Nm3', 'wan_m3', 1,  '1 万Nm³ = 1 万m³'),
    ('FF_BFG', 'wan_m3', 'wan_Nm3', 1,  '反向'),
    ('FF_BFG', 'Nm3', 'wan_Nm3', 0.0001,'1 万Nm³ = 10000 Nm³'),
    ('FF_BFG', 'wan_Nm3', 'Nm3', 10000, '反向'),
    ('FF_COG', 'm3', 'wan_m3', 0.0001, '1 万m³ = 10000 m³'),
    ('FF_COG', 'wan_m3', 'm3', 10000,  '反向'),
    ('FF_COG', 'Nm3', 'm3', 1,           '标准状态下 1 Nm³ ≈ 1 m³'),
    ('FF_COG', 'm3', 'Nm3', 1,           '反向'),
    ('FF_COG', 'Nm3', 'wan_m3', 0.0001, '1 万m³ = 10000 Nm³'),
    ('FF_COG', 'wan_m3', 'Nm3', 10000,  '反向'),
    ('FF_COG', 'wan_Nm3', 'm3', 10000,  '1 万Nm³ = 10000 m³'),
    ('FF_COG', 'm3', 'wan_Nm3', 0.0001, '反向'),
    ('FF_COG', 'wan_Nm3', 'wan_m3', 1,  '1 万Nm³ = 1 万m³'),
    ('FF_COG', 'wan_m3', 'wan_Nm3', 1,  '反向'),
    ('FF_COG', 'Nm3', 'wan_Nm3', 0.0001,'1 万Nm³ = 10000 Nm³'),
    ('FF_COG', 'wan_Nm3', 'Nm3', 10000, '反向'),
    ('FF_COK', 'm3', 'wan_m3', 0.0001, '1 万m³ = 10000 m³'),
    ('FF_COK', 'wan_m3', 'm3', 10000,  '反向'),
    ('FF_COK', 'Nm3', 'm3', 1,           '标准状态下 1 Nm³ ≈ 1 m³'),
    ('FF_COK', 'm3', 'Nm3', 1,           '反向'),
    ('FF_COK', 'Nm3', 'wan_m3', 0.0001, '1 万m³ = 10000 Nm³'),
    ('FF_COK', 'wan_m3', 'Nm3', 10000,  '反向'),
    ('FF_COK', 'wan_Nm3', 'm3', 10000,  '1 万Nm³ = 10000 m³'),
    ('FF_COK', 'm3', 'wan_Nm3', 0.0001, '反向'),
    ('FF_COK', 'wan_Nm3', 'wan_m3', 1,  '1 万Nm³ = 1 万m³'),
    ('FF_COK', 'wan_m3', 'wan_Nm3', 1,  '反向'),
    ('FF_COK', 'Nm3', 'wan_Nm3', 0.0001,'1 万Nm³ = 10000 Nm³'),
    ('FF_COK', 'wan_Nm3', 'Nm3', 10000, '反向');

-- 固体燃料（FF_B/LB/CK/PC）：kg ↔ t ↔ g
INSERT IGNORE INTO emission_unit_conversion
    (subcategory_code, from_unit_code, to_unit_code, conversion_factor, remark) VALUES
    ('FF_B',  'kg', 't', 0.001, '1 t = 1000 kg'),
    ('FF_B',  't',  'kg', 1000, '反向'),
    ('FF_B',  'kg', 'g',  1000, '1 kg = 1000 g'),
    ('FF_B',  'g',  'kg', 0.001, '反向'),
    ('FF_LB', 'kg', 't', 0.001, '1 t = 1000 kg'),
    ('FF_LB', 't',  'kg', 1000, '反向'),
    ('FF_LB', 'kg', 'g',  1000, '1 kg = 1000 g'),
    ('FF_LB', 'g',  'kg', 0.001, '反向'),
    ('FF_CK', 'kg', 't', 0.001, '1 t = 1000 kg'),
    ('FF_CK', 't',  'kg', 1000, '反向'),
    ('FF_CK', 'kg', 'g',  1000, '1 kg = 1000 g'),
    ('FF_CK', 'g',  'kg', 0.001, '反向'),
    ('FF_PC', 'kg', 't', 0.001, '1 t = 1000 kg'),
    ('FF_PC', 't',  'kg', 1000, '反向'),
    ('FF_PC', 'kg', 'g',  1000, '1 kg = 1000 g'),
    ('FF_PC', 'g',  'kg', 0.001, '反向');

-- 废弃物（WT_SW/WT_WW）：t ↔ kg
INSERT IGNORE INTO emission_unit_conversion
    (subcategory_code, from_unit_code, to_unit_code, conversion_factor, remark) VALUES
    ('WT_SW', 't',  'kg', 1000,  '1 t = 1000 kg'),
    ('WT_SW', 'kg', 't',  0.001, '反向'),
    ('WT_WW', 't',  'kg', 1000,  '1 t = 1000 kg'),
    ('WT_WW', 'kg', 't',  0.001, '反向');
