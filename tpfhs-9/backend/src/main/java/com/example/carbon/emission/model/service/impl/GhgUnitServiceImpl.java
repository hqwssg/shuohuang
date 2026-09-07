package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.entity.GhgUnit;
import com.example.carbon.emission.model.repository.GhgUnitRepository;
import com.example.carbon.emission.model.service.GhgUnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
public class GhgUnitServiceImpl implements GhgUnitService {

    @Autowired
    private GhgUnitRepository ghgUnitRepository;

    @Override
    public List<GhgUnit> findAllEnabled() {
        List<GhgUnit> list = ghgUnitRepository.findAllByStatus(1);
        list.sort(Comparator.comparing(GhgUnit::getSortOrder));
        return list;
    }

    @Override
    public GhgUnit findByCode(String ghgCode) {
        Optional<GhgUnit> opt = ghgUnitRepository.findByGhgCode(ghgCode);
        return opt.orElse(null);
    }
}
