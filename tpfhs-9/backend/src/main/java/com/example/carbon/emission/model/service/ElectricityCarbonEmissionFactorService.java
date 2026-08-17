package com.example.carbon.emission.model.service;

import com.example.carbon.emission.model.dto.ElectricityCarbonEmissionFactorDTO;

import java.util.List;

/**
 * 电力碳排放因子服务接口
 * <p>
 * 提供电力碳排放因子的增删改查等业务操作。
 * </p>
 */
public interface ElectricityCarbonEmissionFactorService {
    
    /**
     * 获取所有电力碳排放因子列表
     *
     * @return 电力碳排放因子DTO列表
     */
    List<ElectricityCarbonEmissionFactorDTO> findAll();
    
    /**
     * 根据ID获取电力碳排放因子
     *
     * @param id 因子ID
     * @return 电力碳排放因子DTO
     */
    ElectricityCarbonEmissionFactorDTO findById(Long id);
    
    /**
     * 根据因子名称获取电力碳排放因子
     *
     * @param factorName 因子名称
     * @return 电力碳排放因子DTO
     */
    ElectricityCarbonEmissionFactorDTO findByFactorName(String factorName);
    
    /**
     * 根据因子名称搜索电力碳排放因子
     *
     * @param factorName 因子名称关键字
     * @return 匹配的电力碳排放因子DTO列表
     */
    List<ElectricityCarbonEmissionFactorDTO> searchByFactorName(String factorName);
    
    /**
     * 创建新的电力碳排放因子
     *
     * @param dto 电力碳排放因子DTO
     * @return 创建后的电力碳排放因子DTO
     */
    ElectricityCarbonEmissionFactorDTO create(ElectricityCarbonEmissionFactorDTO dto);
    
    /**
     * 更新电力碳排放因子
     *
     * @param id  因子ID
     * @param dto 更新的电力碳排放因子DTO
     * @return 更新后的电力碳排放因子DTO
     */
    ElectricityCarbonEmissionFactorDTO update(Long id, ElectricityCarbonEmissionFactorDTO dto);
    
    /**
     * 删除电力碳排放因子
     *
     * @param id 因子ID
     */
    void delete(Long id);
}