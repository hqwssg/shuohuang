package com.example.carbon.emission.model.service;

import com.example.carbon.emission.model.dto.FossilFuelEmissionFactorDTO;

import java.util.List;

/**
 * 化石燃料排放因子服务接口
 * <p>
 * 提供化石燃料排放因子的增删改查等业务操作，包括烟煤、褐煤、焦炭、原油、天然气等燃料类型。
 * </p>
 */
public interface FossilFuelEmissionFactorService {
    
    /**
     * 获取所有化石燃料排放因子列表
     *
     * @return 化石燃料排放因子DTO列表
     */
    List<FossilFuelEmissionFactorDTO> findAll();
    
    /**
     * 根据ID获取化石燃料排放因子
     *
     * @param id 因子ID
     * @return 化石燃料排放因子DTO
     */
    FossilFuelEmissionFactorDTO findById(Long id);
    
    /**
     * 根据燃料类型获取化石燃料排放因子
     *
     * @param fuelType 燃料类型
     * @return 化石燃料排放因子DTO
     */
    FossilFuelEmissionFactorDTO findByFuelType(String fuelType);
    
    /**
     * 根据燃料类型获取所有匹配的化石燃料排放因子列表
     *
     * @param fuelType 燃料类型
     * @return 匹配的化石燃料排放因子DTO列表
     */
    List<FossilFuelEmissionFactorDTO> findAllByFuelType(String fuelType);
    
    /**
     * 根据燃料类型搜索化石燃料排放因子
     *
     * @param fuelType 燃料类型关键字
     * @return 匹配的化石燃料排放因子DTO列表
     */
    List<FossilFuelEmissionFactorDTO> searchByFuelType(String fuelType);
    
    /**
     * 创建新的化石燃料排放因子
     *
     * @param dto 化石燃料排放因子DTO
     * @return 创建后的化石燃料排放因子DTO
     */
    FossilFuelEmissionFactorDTO create(FossilFuelEmissionFactorDTO dto);
    
    /**
     * 更新化石燃料排放因子
     *
     * @param id  因子ID
     * @param dto 更新的化石燃料排放因子DTO
     * @return 更新后的化石燃料排放因子DTO
     */
    FossilFuelEmissionFactorDTO update(Long id, FossilFuelEmissionFactorDTO dto);
    
    /**
     * 删除化石燃料排放因子
     *
     * @param id 因子ID
     */
    void delete(Long id);
}