
-- ============================================================
-- 7. 新建 emission_default_factor 表（系统缺省碳排放因子设置）
--    针对能耗小类设置系统默认因子：添加小类后从对应因子库选择一条因子，
--    factor_source 标记来源因子库，factor_name/value/unit/description 为快照。
-- ============================================================
CREATE TABLE IF NOT EXISTS emission_default_factor (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    subcategory_code VARCHAR(50) NOT NULL UNIQUE COMMENT '排放数据小类编码（字典 emission_subcategory 的 item_code）',
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
    INDEX idx_subcategory_code (subcategory_code) COMMENT '小类编码索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统缺省碳排放因子设置表';

-- ============================================================
-- 8. 新建 emission_factor_template 表（碳排放因子模版）
--    管理多个碳排放因子设置库（模版），支持共享/私有、启用/停用。
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
-- 8.1 修改 emission_template 表：增加 factor_template_id 列
--     核算模版通过此列关联 emission_factor_template，表示该核算模版
--     使用指定的碳排放因子模版。仅核算模版（template_type=2）使用。
-- ============================================================
ALTER TABLE emission_template
    ADD COLUMN factor_template_id BIGINT COMMENT '碳排放因子模版ID（外键，指向 emission_factor_template.id，NULL表示未关联。仅核算模版使用）' AFTER task_config;

ALTER TABLE emission_template
    ADD INDEX idx_factor_template_id (factor_template_id) COMMENT '因子模版ID索引';

-- ============================================================
-- 9. 修改 emission_default_factor 表：增加 template_id 列
--    将系统缺省因子扩展为模版级缺省因子，template_id 关联模版ID。
--    原有 subcategory_code 唯一约束改为 (template_id, subcategory_code) 联合唯一。
-- ============================================================
ALTER TABLE emission_default_factor
    ADD COLUMN template_id BIGINT COMMENT '所属因子模版ID（NULL=系统缺省，非NULL=模版级缺省因子）' AFTER subcategory_name;

-- 删除原 subcategory_code 单列唯一索引（如存在），添加联合唯一约束
SET @exist := (SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE table_schema = DATABASE() AND table_name = 'emission_default_factor'
    AND index_name = 'subcategory_code');
SET @sql := IF(@exist > 0, 'ALTER TABLE emission_default_factor DROP INDEX subcategory_code', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

ALTER TABLE emission_default_factor
    ADD UNIQUE INDEX uk_template_subcategory (template_id, subcategory_code);
