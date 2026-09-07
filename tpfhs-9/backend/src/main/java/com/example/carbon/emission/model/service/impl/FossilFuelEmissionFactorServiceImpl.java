package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.dto.FossilFuelEmissionFactorDTO;
import com.example.carbon.emission.model.entity.FossilFuelEmissionFactor;
import com.example.carbon.emission.model.repository.FossilFuelEmissionFactorRepository;
import com.example.carbon.emission.model.service.FossilFuelEmissionFactorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 化石燃料排放因子服务实现类
 * <p>
 * 实现化石燃料排放因子的增删改查业务逻辑，包括数据校验、事务管理和数据转换。
 * 支持多种燃料类型：烟煤、褐煤、焦炭、石油焦、原油、燃料油、汽油、柴油、液化天然气、液化石油气、天然气、高炉煤气、转炉煤气、焦炉煤气。
 * </p>
 */
@Service
public class FossilFuelEmissionFactorServiceImpl implements FossilFuelEmissionFactorService {
    
    /**
     * 化石燃料排放因子数据访问接口
     */
    @Autowired
    private FossilFuelEmissionFactorRepository repository;
    
    /**
     * 获取所有化石燃料排放因子列表
     *
     * @return 化石燃料排放因子DTO列表
     */
    @Override
    public List<FossilFuelEmissionFactorDTO> findAll() {
        return repository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据ID获取化石燃料排放因子
     *
     * @param id 因子ID
     * @return 化石燃料排放因子DTO
     * @throws RuntimeException 当因子不存在时抛出
     */
    @Override
    public FossilFuelEmissionFactorDTO findById(Long id) {
        FossilFuelEmissionFactor entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("化石燃料排放因子不存在: " + id));
        return convertToDTO(entity);
    }
    
    /**
     * 根据燃料类型获取化石燃料排放因子
     *
     * @param fuelType 燃料类型
     * @return 化石燃料排放因子DTO
     * @throws RuntimeException 当因子不存在时抛出
     */
    @Override
    public FossilFuelEmissionFactorDTO findByFuelType(String fuelType) {
        FossilFuelEmissionFactor entity = repository.findByFuelType(fuelType)
                .orElseThrow(() -> new RuntimeException("化石燃料排放因子不存在: " + fuelType));
        return convertToDTO(entity);
    }
    
    /**
     * 根据燃料类型获取所有匹配的化石燃料排放因子列表
     *
     * @param fuelType 燃料类型
     * @return 匹配的化石燃料排放因子DTO列表
     */
    @Override
    public List<FossilFuelEmissionFactorDTO> findAllByFuelType(String fuelType) {
        return repository.findAllByFuelType(fuelType).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据燃料类型搜索化石燃料排放因子
     *
     * @param fuelType 燃料类型关键字
     * @return 匹配的化石燃料排放因子DTO列表
     */
    @Override
    public List<FossilFuelEmissionFactorDTO> searchByFuelType(String fuelType) {
        return repository.findByFuelTypeContaining(fuelType).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 创建新的化石燃料排放因子
     *
     * @param dto 化石燃料排放因子DTO
     * @return 创建后的化石燃料排放因子DTO
     * @throws RuntimeException 当燃料类型已存在时抛出
     */
    @Override
    @Transactional
    public FossilFuelEmissionFactorDTO create(FossilFuelEmissionFactorDTO dto) {
        if (repository.existsByFuelType(dto.getFuelType())) {
            throw new RuntimeException("燃料品种已存在: " + dto.getFuelType());
        }
        
        FossilFuelEmissionFactor entity = convertToEntity(dto);
        FossilFuelEmissionFactor savedEntity = repository.save(entity);
        return convertToDTO(savedEntity);
    }
    
    /**
     * 更新化石燃料排放因子
     *
     * @param id  因子ID
     * @param dto 更新的化石燃料排放因子DTO
     * @return 更新后的化石燃料排放因子DTO
     * @throws RuntimeException 当因子不存在或燃料类型已存在时抛出
     */
    @Override
    @Transactional
    public FossilFuelEmissionFactorDTO update(Long id, FossilFuelEmissionFactorDTO dto) {
        FossilFuelEmissionFactor entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("化石燃料排放因子不存在: " + id));
        
        if (repository.existsByFuelTypeAndIdNot(dto.getFuelType(), id)) {
            throw new RuntimeException("燃料品种已存在: " + dto.getFuelType());
        }
        
        entity.setEmissionFactorName(dto.getEmissionFactorName());
        entity.setFuelType(dto.getFuelType());
        entity.setSource(dto.getSource());
        entity.setUnit(dto.getUnit());
        entity.setLowerHeatingValue(dto.getLowerHeatingValue());
        entity.setCarbonContentPerUnitHeat(dto.getCarbonContentPerUnitHeat());
        entity.setFuelOxidationRate(dto.getFuelOxidationRate());
        entity.setEmissionFactor(dto.getEmissionFactor());
        entity.setFactorUnit(dto.getFactorUnit());
        entity.setDescription(dto.getDescription());
        entity.setUpdatedBy(dto.getUpdatedBy());
        
        FossilFuelEmissionFactor savedEntity = repository.save(entity);
        return convertToDTO(savedEntity);
    }
    
    /**
     * 删除化石燃料排放因子
     *
     * @param id 因子ID
     * @throws RuntimeException 当因子不存在时抛出
     */
    @Override
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("化石燃料排放因子不存在: " + id);
        }
        repository.deleteById(id);
    }
    
    /**
     * 将实体转换为DTO
     *
     * @param entity 化石燃料排放因子实体
     * @return 化石燃料排放因子DTO
     */
    private FossilFuelEmissionFactorDTO convertToDTO(FossilFuelEmissionFactor entity) {
        FossilFuelEmissionFactorDTO dto = new FossilFuelEmissionFactorDTO();
        dto.setId(entity.getId());
        dto.setEmissionFactorName(entity.getEmissionFactorName());
        dto.setFuelType(entity.getFuelType());
        dto.setSource(entity.getSource());
        dto.setUnit(entity.getUnit());
        dto.setLowerHeatingValue(entity.getLowerHeatingValue());
        dto.setCarbonContentPerUnitHeat(entity.getCarbonContentPerUnitHeat());
        dto.setFuelOxidationRate(entity.getFuelOxidationRate());
        dto.setEmissionFactor(entity.getEmissionFactor());
        dto.setFactorUnit(entity.getFactorUnit());
        dto.setDescription(entity.getDescription());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
    
    /**
     * 将DTO转换为实体
     *
     * @param dto 化石燃料排放因子DTO
     * @return 化石燃料排放因子实体
     */
    private FossilFuelEmissionFactor convertToEntity(FossilFuelEmissionFactorDTO dto) {
        FossilFuelEmissionFactor entity = new FossilFuelEmissionFactor();
        entity.setEmissionFactorName(dto.getEmissionFactorName());
        entity.setFuelType(dto.getFuelType());
        entity.setSource(dto.getSource());
        entity.setUnit(dto.getUnit());
        entity.setLowerHeatingValue(dto.getLowerHeatingValue());
        entity.setCarbonContentPerUnitHeat(dto.getCarbonContentPerUnitHeat());
        entity.setFuelOxidationRate(dto.getFuelOxidationRate());
        entity.setEmissionFactor(dto.getEmissionFactor());
        entity.setFactorUnit(dto.getFactorUnit());
        entity.setDescription(dto.getDescription());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setUpdatedBy(dto.getUpdatedBy());
        return entity;
    }
}