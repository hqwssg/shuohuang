package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.dto.ThermalEmissionFactorDTO;
import com.example.carbon.emission.model.entity.ThermalEmissionFactor;
import com.example.carbon.emission.model.repository.ThermalEmissionFactorRepository;
import com.example.carbon.emission.model.service.ThermalEmissionFactorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ThermalEmissionFactorServiceImpl implements ThermalEmissionFactorService {

    @Autowired
    private ThermalEmissionFactorRepository repository;

    @Override
    public List<ThermalEmissionFactorDTO> findAll() {
        return repository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public ThermalEmissionFactorDTO findById(Long id) {
        ThermalEmissionFactor entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("热力排放因子不存在: " + id));
        return convertToDTO(entity);
    }

    @Override
    public ThermalEmissionFactorDTO findByEmissionFactorName(String emissionFactorName) {
        ThermalEmissionFactor entity = repository.findByEmissionFactorName(emissionFactorName)
                .orElseThrow(() -> new RuntimeException("热力排放因子不存在: " + emissionFactorName));
        return convertToDTO(entity);
    }

    @Override
    @Transactional
    public ThermalEmissionFactorDTO create(ThermalEmissionFactorDTO dto) {
        if (repository.existsByEmissionFactorName(dto.getEmissionFactorName())) {
            throw new RuntimeException("排放因子名称已存在: " + dto.getEmissionFactorName());
        }

        ThermalEmissionFactor entity = convertToEntity(dto);
        ThermalEmissionFactor savedEntity = repository.save(entity);
        return convertToDTO(savedEntity);
    }

    @Override
    @Transactional
    public ThermalEmissionFactorDTO update(Long id, ThermalEmissionFactorDTO dto) {
        ThermalEmissionFactor entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("热力排放因子不存在: " + id));

        if (repository.existsByEmissionFactorNameAndIdNot(dto.getEmissionFactorName(), id)) {
            throw new RuntimeException("排放因子名称已存在: " + dto.getEmissionFactorName());
        }

        entity.setEmissionFactorName(dto.getEmissionFactorName());
        entity.setEmissionFactor(dto.getEmissionFactor());
        entity.setUnit(dto.getUnit());
        entity.setSource(dto.getSource());
        entity.setDescription(dto.getDescription());
        entity.setUpdatedBy(dto.getUpdatedBy());

        ThermalEmissionFactor savedEntity = repository.save(entity);
        return convertToDTO(savedEntity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("热力排放因子不存在: " + id);
        }
        repository.deleteById(id);
    }

    private ThermalEmissionFactorDTO convertToDTO(ThermalEmissionFactor entity) {
        ThermalEmissionFactorDTO dto = new ThermalEmissionFactorDTO();
        dto.setId(entity.getId());
        dto.setEmissionFactorName(entity.getEmissionFactorName());
        dto.setEmissionFactor(entity.getEmissionFactor());
        dto.setUnit(entity.getUnit());
        dto.setSource(entity.getSource());
        dto.setDescription(entity.getDescription());
        dto.setCreatedBy(entity.getCreatedBy());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedBy(entity.getUpdatedBy());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    private ThermalEmissionFactor convertToEntity(ThermalEmissionFactorDTO dto) {
        ThermalEmissionFactor entity = new ThermalEmissionFactor();
        entity.setEmissionFactorName(dto.getEmissionFactorName());
        entity.setEmissionFactor(dto.getEmissionFactor());
        entity.setUnit(dto.getUnit());
        entity.setSource(dto.getSource());
        entity.setDescription(dto.getDescription());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setUpdatedBy(dto.getCreatedBy());
        return entity;
    }
}
