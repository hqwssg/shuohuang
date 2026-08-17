package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.dto.WasteIncinerationFactorDTO;
import com.example.carbon.emission.model.entity.WasteIncinerationFactor;
import com.example.carbon.emission.model.repository.WasteIncinerationFactorRepository;
import com.example.carbon.emission.model.service.WasteIncinerationFactorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WasteIncinerationFactorServiceImpl implements WasteIncinerationFactorService {

    @Autowired
    private WasteIncinerationFactorRepository repository;

    @Override
    public List<WasteIncinerationFactorDTO> findAll() {
        return repository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public WasteIncinerationFactorDTO findById(Long id) {
        WasteIncinerationFactor entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("固体废弃物焚烧排放因子不存在: " + id));
        return convertToDTO(entity);
    }

    @Override
    public WasteIncinerationFactorDTO findByEmissionFactorName(String emissionFactorName) {
        WasteIncinerationFactor entity = repository.findByEmissionFactorName(emissionFactorName)
                .orElseThrow(() -> new RuntimeException("固体废弃物焚烧排放因子不存在: " + emissionFactorName));
        return convertToDTO(entity);
    }

    @Override
    @Transactional
    public WasteIncinerationFactorDTO create(WasteIncinerationFactorDTO dto) {
        if (repository.existsByEmissionFactorName(dto.getEmissionFactorName())) {
            throw new RuntimeException("排放因子名称已存在: " + dto.getEmissionFactorName());
        }

        WasteIncinerationFactor entity = convertToEntity(dto);
        WasteIncinerationFactor savedEntity = repository.save(entity);
        return convertToDTO(savedEntity);
    }

    @Override
    @Transactional
    public WasteIncinerationFactorDTO update(Long id, WasteIncinerationFactorDTO dto) {
        WasteIncinerationFactor entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("固体废弃物焚烧排放因子不存在: " + id));

        if (repository.existsByEmissionFactorNameAndIdNot(dto.getEmissionFactorName(), id)) {
            throw new RuntimeException("排放因子名称已存在: " + dto.getEmissionFactorName());
        }

        entity.setEmissionFactorName(dto.getEmissionFactorName());
        entity.setWasteType(dto.getWasteType());
        entity.setCcw(dto.getCcw());
        entity.setFcf(dto.getFcf());
        entity.setCe(dto.getCe());
        entity.setEmissionFactor(dto.getEmissionFactor());
        entity.setUnit(dto.getUnit());
        entity.setSource(dto.getSource());
        entity.setDescription(dto.getDescription());
        entity.setUpdatedBy(dto.getUpdatedBy());

        WasteIncinerationFactor savedEntity = repository.save(entity);
        return convertToDTO(savedEntity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("固体废弃物焚烧排放因子不存在: " + id);
        }
        repository.deleteById(id);
    }

    private WasteIncinerationFactorDTO convertToDTO(WasteIncinerationFactor entity) {
        WasteIncinerationFactorDTO dto = new WasteIncinerationFactorDTO();
        dto.setId(entity.getId());
        dto.setEmissionFactorName(entity.getEmissionFactorName());
        dto.setWasteType(entity.getWasteType());
        dto.setCcw(entity.getCcw());
        dto.setFcf(entity.getFcf());
        dto.setCe(entity.getCe());
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

    private WasteIncinerationFactor convertToEntity(WasteIncinerationFactorDTO dto) {
        WasteIncinerationFactor entity = new WasteIncinerationFactor();
        entity.setEmissionFactorName(dto.getEmissionFactorName());
        entity.setWasteType(dto.getWasteType());
        entity.setCcw(dto.getCcw());
        entity.setFcf(dto.getFcf());
        entity.setCe(dto.getCe());
        entity.setEmissionFactor(dto.getEmissionFactor());
        entity.setUnit(dto.getUnit());
        entity.setSource(dto.getSource());
        entity.setDescription(dto.getDescription());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setUpdatedBy(dto.getCreatedBy());
        return entity;
    }
}
