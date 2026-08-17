package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.dto.ElectricityCarbonEmissionFactorDTO;
import com.example.carbon.emission.model.entity.ElectricityCarbonEmissionFactor;
import com.example.carbon.emission.model.repository.ElectricityCarbonEmissionFactorRepository;
import com.example.carbon.emission.model.service.ElectricityCarbonEmissionFactorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 电力碳排放因子服务实现类
 * <p>
 * 实现电力碳排放因子的增删改查业务逻辑，包括数据校验、事务管理和数据转换。
 * </p>
 */
@Service
public class ElectricityCarbonEmissionFactorServiceImpl implements ElectricityCarbonEmissionFactorService {
    
    /**
     * 电力碳排放因子数据访问接口
     */
    @Autowired
    private ElectricityCarbonEmissionFactorRepository repository;
    
    /**
     * 获取所有电力碳排放因子列表
     * <p>
     * 从数据库查询所有记录，并转换为DTO列表返回。
     * </p>
     *
     * @return 电力碳排放因子DTO列表
     */
    @Override
    public List<ElectricityCarbonEmissionFactorDTO> findAll() {
        return repository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 根据ID获取电力碳排放因子
     * <p>
     * 根据ID查询数据库，若不存在则抛出异常。
     * </p>
     *
     * @param id 因子ID
     * @return 电力碳排放因子DTO
     * @throws RuntimeException 当因子不存在时抛出
     */
    @Override
    public ElectricityCarbonEmissionFactorDTO findById(Long id) {
        ElectricityCarbonEmissionFactor entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("电力碳排放因子不存在: " + id));
        return convertToDTO(entity);
    }
    
    /**
     * 根据因子名称获取电力碳排放因子
     * <p>
     * 根据因子名称精确匹配查询数据库，若不存在则抛出异常。
     * </p>
     *
     * @param factorName 因子名称
     * @return 电力碳排放因子DTO
     * @throws RuntimeException 当因子不存在时抛出
     */
    @Override
    public ElectricityCarbonEmissionFactorDTO findByFactorName(String factorName) {
        ElectricityCarbonEmissionFactor entity = repository.findByFactorName(factorName)
                .orElseThrow(() -> new RuntimeException("电力碳排放因子不存在: " + factorName));
        return convertToDTO(entity);
    }
    
    /**
     * 根据因子名称搜索电力碳排放因子
     * <p>
     * 根据因子名称模糊匹配查询数据库。
     * </p>
     *
     * @param factorName 因子名称关键字
     * @return 匹配的电力碳排放因子DTO列表
     */
    @Override
    public List<ElectricityCarbonEmissionFactorDTO> searchByFactorName(String factorName) {
        return repository.findByFactorNameContaining(factorName).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 创建新的电力碳排放因子
     * <p>
     * 先校验因子名称是否已存在，然后将DTO转换为实体并保存。
     * </p>
     *
     * @param dto 电力碳排放因子DTO
     * @return 创建后的电力碳排放因子DTO
     * @throws RuntimeException 当因子名称已存在时抛出
     */
    @Override
    @Transactional
    public ElectricityCarbonEmissionFactorDTO create(ElectricityCarbonEmissionFactorDTO dto) {
        if (repository.existsByFactorName(dto.getFactorName())) {
            throw new RuntimeException("碳排放因子名称已存在: " + dto.getFactorName());
        }
        
        ElectricityCarbonEmissionFactor entity = convertToEntity(dto);
        ElectricityCarbonEmissionFactor savedEntity = repository.save(entity);
        return convertToDTO(savedEntity);
    }
    
    /**
     * 更新电力碳排放因子
     * <p>
     * 先校验因子是否存在，再校验名称是否与其他记录冲突，最后更新实体。
     * </p>
     *
     * @param id  因子ID
     * @param dto 更新的电力碳排放因子DTO
     * @return 更新后的电力碳排放因子DTO
     * @throws RuntimeException 当因子不存在或名称已存在时抛出
     */
    @Override
    @Transactional
    public ElectricityCarbonEmissionFactorDTO update(Long id, ElectricityCarbonEmissionFactorDTO dto) {
        ElectricityCarbonEmissionFactor entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("电力碳排放因子不存在: " + id));
        
        if (repository.existsByFactorNameAndIdNot(dto.getFactorName(), id)) {
            throw new RuntimeException("碳排放因子名称已存在: " + dto.getFactorName());
        }
        
        entity.setFactorName(dto.getFactorName());
        entity.setFactorValue(dto.getFactorValue());
        entity.setUnit(dto.getUnit());
        entity.setDescription(dto.getDescription());
        entity.setUpdatedBy(dto.getUpdatedBy());
        
        ElectricityCarbonEmissionFactor savedEntity = repository.save(entity);
        return convertToDTO(savedEntity);
    }
    
    /**
     * 删除电力碳排放因子
     * <p>
     * 先校验因子是否存在，然后删除。
     * </p>
     *
     * @param id 因子ID
     * @throws RuntimeException 当因子不存在时抛出
     */
    @Override
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("电力碳排放因子不存在: " + id);
        }
        repository.deleteById(id);
    }
    
    /**
     * 将实体转换为DTO
     *
     * @param entity 电力碳排放因子实体
     * @return 电力碳排放因子DTO
     */
    private ElectricityCarbonEmissionFactorDTO convertToDTO(ElectricityCarbonEmissionFactor entity) {
        ElectricityCarbonEmissionFactorDTO dto = new ElectricityCarbonEmissionFactorDTO();
        dto.setId(entity.getId());
        dto.setFactorName(entity.getFactorName());
        dto.setFactorValue(entity.getFactorValue());
        dto.setUnit(entity.getUnit());
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
     * @param dto 电力碳排放因子DTO
     * @return 电力碳排放因子实体
     */
    private ElectricityCarbonEmissionFactor convertToEntity(ElectricityCarbonEmissionFactorDTO dto) {
        ElectricityCarbonEmissionFactor entity = new ElectricityCarbonEmissionFactor();
        entity.setFactorName(dto.getFactorName());
        entity.setFactorValue(dto.getFactorValue());
        entity.setUnit(dto.getUnit());
        entity.setDescription(dto.getDescription());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setUpdatedBy(dto.getCreatedBy());
        return entity;
    }
}