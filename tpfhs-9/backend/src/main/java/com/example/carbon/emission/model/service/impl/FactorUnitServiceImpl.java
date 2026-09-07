package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.entity.FactorUnit;
import com.example.carbon.emission.model.repository.FactorUnitRepository;
import com.example.carbon.emission.model.service.FactorUnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Service
public class FactorUnitServiceImpl implements FactorUnitService {

    @Autowired
    private FactorUnitRepository factorUnitRepository;

    @Override
    public List<FactorUnit> findAllEnabled() {
        List<FactorUnit> list = factorUnitRepository.findAllByStatus(1);
        list.sort(Comparator.comparing(FactorUnit::getSortOrder));
        return list;
    }

    @Override
    public List<FactorUnit> findBySubcategoryCode(String subcategoryCode) {
        List<FactorUnit> list = factorUnitRepository.findBySubcategoryCodeAndStatus(subcategoryCode, 1);
        list.sort(Comparator.comparing(FactorUnit::getSortOrder));
        return list;
    }

    @Override
    public List<FactorUnit> findBySubcategoryCodes(Collection<String> subcategoryCodes) {
        if (subcategoryCodes == null || subcategoryCodes.isEmpty()) {
            return Collections.emptyList();
        }
        List<FactorUnit> list = factorUnitRepository.findBySubcategoryCodeInAndStatus(subcategoryCodes, 1);
        list.sort(Comparator.comparing(FactorUnit::getSortOrder));
        return list;
    }
}
