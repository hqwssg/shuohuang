package com.example.carbon.emission.model.service;

import java.math.BigDecimal;

/**
 * 单位换算 Service
 * <p>
 * 基于 emission_unit_conversion 表提供按排放数据小类的单位换算能力，
 * 并便捷支持将任意单位自动换算为该小类的缺省核算单位。
 */
public interface UnitConversionService {

    /**
     * 按排放数据小类执行单位换算
     * <p>
     * 转换公式：目标值 = value × conversionFactor
     * 若找不到对应换算关系，则记录 WARN 日志并返回原值。
     *
     * @param subcategoryCode 排放数据小类编码（限定换算范围）
     * @param fromUnit        源单位编码
     * @param toUnit          目标单位编码
     * @param value           源数值
     * @return 换算后的数值；换算关系不存在则返回原值
     */
    BigDecimal convert(String subcategoryCode, String fromUnit, String toUnit, BigDecimal value);

    /**
     * 便捷方法：将源单位数值换算为该小类的缺省核算单位
     * <p>
     * 先通过 CalcUnitDefaultRepository 查询该小类的 calculationUnit，
     * 再调用 {@link #convert(String, String, String, BigDecimal)} 执行换算。
     * 若找不到缺省核算单位，则记录 WARN 日志并返回原值。
     *
     * @param subcategoryCode 排放数据小类编码
     * @param fromUnit        源单位编码
     * @param value           源数值
     * @return 换算为缺省核算单位后的数值；若缺省单位不存在则返回原值
     */
    BigDecimal convertToCalculationUnit(String subcategoryCode, String fromUnit, BigDecimal value);
}
