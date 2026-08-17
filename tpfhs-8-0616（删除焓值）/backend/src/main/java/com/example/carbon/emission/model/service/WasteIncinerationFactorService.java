package com.example.carbon.emission.model.service;

import com.example.carbon.emission.model.dto.WasteIncinerationFactorDTO;

import java.util.List;

public interface WasteIncinerationFactorService {

    List<WasteIncinerationFactorDTO> findAll();

    WasteIncinerationFactorDTO findById(Long id);

    WasteIncinerationFactorDTO findByEmissionFactorName(String emissionFactorName);

    WasteIncinerationFactorDTO create(WasteIncinerationFactorDTO dto);

    WasteIncinerationFactorDTO update(Long id, WasteIncinerationFactorDTO dto);

    void delete(Long id);
}
