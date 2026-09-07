package com.example.carbon.emission.model.service;

import com.example.carbon.emission.model.entity.GhgUnit;

import java.util.List;

/**
 * 温室气体质量单位 Service
 * <p>
 * 管理碳排放因子的分子单位（kgCO2、tCO2、kgCH4 等），
 * 数据由数据库初始化固定，前端因子单位下拉从本服务拉取。
 */
public interface GhgUnitService {

    /**
     * 查询全部启用的温室气体质量单位（按 sort_order 升序）
     */
    List<GhgUnit> findAllEnabled();

    /**
     * 按 ghg_code 查询单条
     */
    GhgUnit findByCode(String ghgCode);
}
