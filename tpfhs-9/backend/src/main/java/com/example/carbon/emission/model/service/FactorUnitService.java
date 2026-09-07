package com.example.carbon.emission.model.service;

import com.example.carbon.emission.model.entity.FactorUnit;

import java.util.Collection;
import java.util.List;

/**
 * 碳排放因子单位 Service
 * <p>
 * 管理每行一个合法的因子单位组合（分子+分母），按排放数据小类限定。
 * 前端因子单位下拉通过小类编码批量查询可选单位。
 */
public interface FactorUnitService {

    /**
     * 查询全部启用的因子单位（按 sort_order 升序）
     */
    List<FactorUnit> findAllEnabled();

    /**
     * 按排放数据小类编码查询启用单位
     */
    List<FactorUnit> findBySubcategoryCode(String subcategoryCode);

    /**
     * 按多个小类编码批量查询启用单位（前端一次拉取多小类下拉用）
     */
    List<FactorUnit> findBySubcategoryCodes(Collection<String> subcategoryCodes);
}
