package com.example.carbon.emission.model.service;

import com.example.carbon.emission.model.entity.CalcUnitDefault;

import java.util.List;

/**
 * 碳排放核算能耗缺省单位 Service
 * <p>
 * 行数据固定（22条排放数据小类），不允许新增/删除，仅允许修改每行的
 * calculationUnit / reportUnit / remark。换算系数不再存储，由
 * UnitService 动态查 emission_unit_conversion 表。
 */
public interface CalcUnitDefaultService {

    /**
     * 查询全部缺省单位（按 subcategoryCode 升序）
     */
    List<CalcUnitDefault> findAll();

    /**
     * 按排放数据小类编码查询
     */
    CalcUnitDefault findBySubcategoryCode(String subcategoryCode);

    /**
     * 根据 subcategoryCode 更新缺省单位设置
     *
     * @param subcategoryCode  排放数据小类编码（业务键）
     * @param calculationUnit  新的核算单位编码（emission_unit_standard.unit_code）
     * @param reportUnit       新的报告单位编码（emission_unit_standard.unit_code）
     * @param remark           新的备注说明
     * @param updatedBy        更新人ID
     * @return 更新后的实体；若记录不存在则返回 null
     */
    CalcUnitDefault update(String subcategoryCode, String calculationUnit,
                           String reportUnit, String remark, Long updatedBy);
}
