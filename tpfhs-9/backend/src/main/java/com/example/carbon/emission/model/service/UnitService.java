package com.example.carbon.emission.model.service;

import com.example.carbon.emission.model.entity.UnitConversion;
import com.example.carbon.emission.model.entity.UnitStandard;

import java.math.BigDecimal;
import java.util.List;

/**
 * 计量单位统一服务：管理标准单位与转换系数
 */
public interface UnitService {

    /**
     * 查询全部启用的标准单位
     */
    List<UnitStandard> findAllEnabled();

    /**
     * 按单位大类查询标准单位
     */
    List<UnitStandard> findByCategory(String unitCategory);

    /**
     * 按小类编码查询其全部转换关系
     */
    List<UnitConversion> findConversions(String subcategoryCode);

    /**
     * 按小类+源单位查询可转换的目标单位列表（用于下拉选项联动）
     */
    List<UnitConversion> findConversionsFrom(String subcategoryCode, String fromUnitCode);

    /**
     * 单位换算：返回 目标值 = 源值 × conversionFactor 的结果
     *
     * @param subcategoryCode 排放数据小类编码
     * @param fromUnitCode    源单位编码
     * @param toUnitCode      目标单位编码
     * @param value           源数值
     * @return 换算后的数值；若无匹配转换关系则返回 null
     */
    BigDecimal convert(String subcategoryCode, String fromUnitCode, String toUnitCode, BigDecimal value);

    /**
     * 校验单位编码是否存在于标准单位表中
     */
    boolean isValidUnitCode(String unitCode);

    /**
     * 查询全部单位转换系数（按小类编码、源单位编码升序）
     *
     * @return 转换系数列表；无数据返回空列表
     */
    List<UnitConversion> findAllConversions();

    /**
     * 新增单位转换系数
     * <p>
     * 唯一约束：subcategoryCode + fromUnitCode + toUnitCode。
     * 校验：三者非空、源单位与目标单位不能相同、单位编码必须为标准单位、
     * 转换系数非空。任一校验失败或唯一冲突返回 null。
     *
     * @param req 待新增实体
     * @return 新增后的实体；校验失败或唯一冲突返回 null
     */
    UnitConversion createConversion(UnitConversion req);

    /**
     * 更新单位转换系数（按 id 定位）
     * <p>
     * 可更新字段：fromUnitCode、toUnitCode、conversionFactor、remark。
     * 若更改了 (fromUnitCode, toUnitCode) 组合，需重新做唯一校验。
     *
     * @param id  主键ID
     * @param req 待更新字段
     * @return 更新后的实体；记录不存在或唯一冲突返回 null
     */
    UnitConversion updateConversion(Long id, UnitConversion req);

    /**
     * 删除单位转换系数
     *
     * @param id 主键ID
     * @return 删除成功返回 true；记录不存在返回 false
     */
    boolean deleteConversion(Long id);
}
