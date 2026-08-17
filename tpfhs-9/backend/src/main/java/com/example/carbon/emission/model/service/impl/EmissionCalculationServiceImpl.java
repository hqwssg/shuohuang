package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.service.EmissionCalculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmissionCalculationServiceImpl implements EmissionCalculationService {
    
    private static final Logger logger = LoggerFactory.getLogger(EmissionCalculationServiceImpl.class);

    @Override
    public void executeEmissionCalculation(Long templateId) {
        logger.info("***开始执行碳排放核算，模版ID: {}", templateId);
        
    }
}