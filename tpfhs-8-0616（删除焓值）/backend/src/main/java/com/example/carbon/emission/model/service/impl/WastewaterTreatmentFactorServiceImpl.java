package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.dto.WastewaterTreatmentFactorDTO;
import com.example.carbon.emission.model.entity.WastewaterTreatmentFactor;
import com.example.carbon.emission.model.repository.WastewaterTreatmentFactorRepository;
import com.example.carbon.emission.model.service.WastewaterTreatmentFactorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class WastewaterTreatmentFactorServiceImpl implements WastewaterTreatmentFactorService {

    @Autowired
    private WastewaterTreatmentFactorRepository repository;

    @Override
    public List<WastewaterTreatmentFactorDTO> findAll() {
        return repository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public WastewaterTreatmentFactorDTO findById(Long id) {
        WastewaterTreatmentFactor entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("废水处理排放因子不存在: " + id));
        return convertToDTO(entity);
    }

    @Override
    public WastewaterTreatmentFactorDTO findByEmissionFactorName(String emissionFactorName) {
        WastewaterTreatmentFactor entity = repository.findByEmissionFactorName(emissionFactorName)
                .orElseThrow(() -> new RuntimeException("废水处理排放因子不存在: " + emissionFactorName));
        return convertToDTO(entity);
    }

    @Override
    @Transactional
    public WastewaterTreatmentFactorDTO create(WastewaterTreatmentFactorDTO dto) {
        if (repository.existsByEmissionFactorName(dto.getEmissionFactorName())) {
            throw new RuntimeException("排放因子名称已存在: " + dto.getEmissionFactorName());
        }

        WastewaterTreatmentFactor entity = convertToEntity(dto);
        WastewaterTreatmentFactor savedEntity = repository.save(entity);
        return convertToDTO(savedEntity);
    }

    @Override
    @Transactional
    public WastewaterTreatmentFactorDTO update(Long id, WastewaterTreatmentFactorDTO dto) {
        WastewaterTreatmentFactor entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("废水处理排放因子不存在: " + id));

        if (repository.existsByEmissionFactorNameAndIdNot(dto.getEmissionFactorName(), id)) {
            throw new RuntimeException("排放因子名称已存在: " + dto.getEmissionFactorName());
        }

        entity.setEmissionFactorName(dto.getEmissionFactorName());
        entity.setWastewaterType(dto.getWastewaterType());
        entity.setOd(dto.getOd());
        entity.setBo(dto.getBo());
        entity.setMcf(dto.getMcf());
        entity.setGwp(dto.getGwp());
        entity.setEmissionFactor(dto.getEmissionFactor());
        entity.setUnit(dto.getUnit());
        entity.setSource(dto.getSource());
        entity.setDescription(dto.getDescription());
        entity.setUpdatedBy(dto.getUpdatedBy());

        WastewaterTreatmentFactor savedEntity = repository.save(entity);
        return convertToDTO(savedEntity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("废水处理排放因子不存在: " + id);
        }
        repository.deleteById(id);
    }

    private WastewaterTreatmentFactorDTO convertToDTO(WastewaterTreatmentFactor entity) {
        WastewaterTreatmentFactorDTO dto = new WastewaterTreatmentFactorDTO();
        dto.setId(entity.getId());
        dto.setEmissionFactorName(entity.getEmissionFactorName());
        dto.setWastewaterType(entity.getWastewaterType());
        dto.setOd(entity.getOd());
        dto.setBo(entity.getBo());
        dto.setMcf(entity.getMcf());
        dto.setGwp(entity.getGwp());
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

    private WastewaterTreatmentFactor convertToEntity(WastewaterTreatmentFactorDTO dto) {
        WastewaterTreatmentFactor entity = new WastewaterTreatmentFactor();
        entity.setEmissionFactorName(dto.getEmissionFactorName());
        entity.setWastewaterType(dto.getWastewaterType());
        entity.setOd(dto.getOd());
        entity.setBo(dto.getBo());
        entity.setMcf(dto.getMcf());
        entity.setGwp(dto.getGwp());
        entity.setEmissionFactor(dto.getEmissionFactor());
        entity.setUnit(dto.getUnit());
        entity.setSource(dto.getSource());
        entity.setDescription(dto.getDescription());
        entity.setCreatedBy(dto.getCreatedBy());
        entity.setUpdatedBy(dto.getCreatedBy());
        return entity;
    }
}
