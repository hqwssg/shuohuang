package com.example.carbon.emission.model.service;

import com.example.carbon.emission.model.dto.WastewaterTreatmentFactorDTO;

import java.util.List;

public interface WastewaterTreatmentFactorService {

    List<WastewaterTreatmentFactorDTO> findAll();

    WastewaterTreatmentFactorDTO findById(Long id);

    WastewaterTreatmentFactorDTO findByEmissionFactorName(String emissionFactorName);

    WastewaterTreatmentFactorDTO create(WastewaterTreatmentFactorDTO dto);

    WastewaterTreatmentFactorDTO update(Long id, WastewaterTreatmentFactorDTO dto);

    void delete(Long id);
}
