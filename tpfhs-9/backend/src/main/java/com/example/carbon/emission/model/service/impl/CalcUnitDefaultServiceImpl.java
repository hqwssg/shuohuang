package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.entity.CalcUnitDefault;
import com.example.carbon.emission.model.repository.CalcUnitDefaultRepository;
import com.example.carbon.emission.model.service.CalcUnitDefaultService;
import com.example.carbon.emission.model.service.UnitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CalcUnitDefaultServiceImpl implements CalcUnitDefaultService {

    private static final Logger log = LoggerFactory.getLogger(CalcUnitDefaultServiceImpl.class);

    @Autowired
    private CalcUnitDefaultRepository calcUnitDefaultRepository;

    /**
     * UnitService 用 @Lazy 注入避免循环依赖（UnitService 被采集点保存校验也依赖到本服务上下文）
     */
    @Autowired
    @Lazy
    private UnitService unitService;

    @Override
    public List<CalcUnitDefault> findAll() {
        return calcUnitDefaultRepository.findAll(Sort.by(Sort.Direction.ASC, "subcategoryCode"));
    }

    @Override
    public CalcUnitDefault findBySubcategoryCode(String subcategoryCode) {
        Optional<CalcUnitDefault> opt = calcUnitDefaultRepository.findBySubcategoryCode(subcategoryCode);
        return opt.orElse(null);
    }

    @Override
    public CalcUnitDefault update(String subcategoryCode, String calculationUnit,
                                  String reportUnit, String remark, Long updatedBy) {
        Optional<CalcUnitDefault> opt = calcUnitDefaultRepository.findBySubcategoryCode(subcategoryCode);
        if (opt.isEmpty()) {
            log.warn("更新缺省单位失败：记录不存在，subcategoryCode={}", subcategoryCode);
            return null;
        }
        // 决策4A：严格校验，单位编码必须存在于 emission_unit_standard
        if (calculationUnit != null && !calculationUnit.isBlank()
                && !unitService.isValidUnitCode(calculationUnit.trim())) {
            log.warn("更新缺省单位失败：核算单位不是标准单位，subcategoryCode={}, calculationUnit={}",
                    subcategoryCode, calculationUnit);
            return null;
        }
        if (reportUnit != null && !reportUnit.isBlank()
                && !unitService.isValidUnitCode(reportUnit.trim())) {
            log.warn("更新缺省单位失败：报告单位不是标准单位，subcategoryCode={}, reportUnit={}",
                    subcategoryCode, reportUnit);
            return null;
        }
        CalcUnitDefault entity = opt.get();
        if (calculationUnit != null && !calculationUnit.isBlank()) {
            entity.setCalculationUnit(calculationUnit.trim());
        }
        if (reportUnit != null && !reportUnit.isBlank()) {
            entity.setReportUnit(reportUnit.trim());
        }
        if (remark != null) {
            entity.setRemark(remark);
        }
        if (updatedBy != null) {
            entity.setUpdatedBy(updatedBy);
        }
        CalcUnitDefault saved = calcUnitDefaultRepository.save(entity);
        log.info("碳排放核算缺省单位已更新：subcategoryCode={}，核算单位={}，报告单位={}",
                saved.getSubcategoryCode(), saved.getCalculationUnit(), saved.getReportUnit());
        return saved;
    }
}
