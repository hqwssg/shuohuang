package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.entity.CalcUnitDefault;
import com.example.carbon.emission.model.entity.UnitConversion;
import com.example.carbon.emission.model.repository.CalcUnitDefaultRepository;
import com.example.carbon.emission.model.repository.UnitConversionRepository;
import com.example.carbon.emission.model.service.UnitConversionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;
import java.util.Optional;

/**
 * 单位换算 Service 实现
 */
@Service
@Transactional(readOnly = true)
public class UnitConversionServiceImpl implements UnitConversionService {

    private static final Logger log = LoggerFactory.getLogger(UnitConversionServiceImpl.class);

    @Autowired
    private UnitConversionRepository unitConversionRepository;

    @Autowired
    private CalcUnitDefaultRepository calcUnitDefaultRepository;

    @Override
    public BigDecimal convert(String subcategoryCode, String fromUnit, String toUnit, BigDecimal value) {
        // 空值或无需换算的情况：直接返回原值
        if (value == null) {
            return null;
        }
        if (subcategoryCode == null || fromUnit == null || toUnit == null) {
            log.warn("单位换算参数存在空值，返回原值：subcategory={}, from={}, to={}",
                    subcategoryCode, fromUnit, toUnit);
            return value;
        }
        if (Objects.equals(fromUnit, toUnit)) {
            return value;
        }

        // 查询换算系数
        Optional<UnitConversion> conv = unitConversionRepository
                .findBySubcategoryCodeAndFromUnitCodeAndToUnitCode(subcategoryCode, fromUnit, toUnit);

        if (conv.isPresent()) {
            // 目标值 = 源值 × conversionFactor，精度使用 DECIMAL64
            return value.multiply(conv.get().getConversionFactor(), MathContext.DECIMAL64);
        } else {
            // 找不到换算关系：WARN 日志 + 返回原值
            log.warn("单位换算失败 subcategory={}, from={}, to={}", subcategoryCode, fromUnit, toUnit);
            return value;
        }
    }

    @Override
    public BigDecimal convertToCalculationUnit(String subcategoryCode, String fromUnit, BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (subcategoryCode == null) {
            log.warn("convertToCalculationUnit：subcategoryCode 为空，返回原值");
            return value;
        }

        // 查询该小类的缺省核算单位
        Optional<CalcUnitDefault> defOpt = calcUnitDefaultRepository.findBySubcategoryCode(subcategoryCode);
        if (defOpt.isEmpty()) {
            log.warn("convertToCalculationUnit：未找到缺省核算单位，subcategory={}，返回原值", subcategoryCode);
            return value;
        }

        String calculationUnit = defOpt.get().getCalculationUnit();
        // 调用通用换算方法执行
        return convert(subcategoryCode, fromUnit, calculationUnit, value);
    }
}
