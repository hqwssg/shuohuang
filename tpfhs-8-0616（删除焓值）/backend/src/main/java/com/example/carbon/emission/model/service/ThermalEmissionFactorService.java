package com.example.carbon.emission.model.service;

import com.example.carbon.emission.model.dto.ThermalEmissionFactorDTO;

import java.util.List;

public interface ThermalEmissionFactorService {

    List<ThermalEmissionFactorDTO> findAll();

    ThermalEmissionFactorDTO findById(Long id);

    ThermalEmissionFactorDTO findByEmissionFactorName(String emissionFactorName);

    ThermalEmissionFactorDTO create(ThermalEmissionFactorDTO dto);

    ThermalEmissionFactorDTO update(Long id, ThermalEmissionFactorDTO dto);

    void delete(Long id);
}
