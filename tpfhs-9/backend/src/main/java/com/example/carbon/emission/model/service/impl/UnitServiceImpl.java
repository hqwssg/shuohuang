package com.example.carbon.emission.model.service.impl;

import com.example.carbon.emission.model.entity.UnitConversion;
import com.example.carbon.emission.model.entity.UnitStandard;
import com.example.carbon.emission.model.repository.UnitConversionRepository;
import com.example.carbon.emission.model.repository.UnitStandardRepository;
import com.example.carbon.emission.model.service.UnitService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UnitServiceImpl implements UnitService {

    private static final Logger log = LoggerFactory.getLogger(UnitServiceImpl.class);

    @Autowired
    private UnitStandardRepository unitStandardRepository;

    @Autowired
    private UnitConversionRepository unitConversionRepository;

    @Override
    public List<UnitStandard> findAllEnabled() {
        List<UnitStandard> list = unitStandardRepository.findByStatus(1);
        list.sort(Comparator.comparing(UnitStandard::getSortOrder));
        return list;
    }

    @Override
    public List<UnitStandard> findByCategory(String unitCategory) {
        List<UnitStandard> list = unitStandardRepository.findByUnitCategory(unitCategory);
        list.sort(Comparator.comparing(UnitStandard::getSortOrder));
        return list;
    }

    @Override
    public List<UnitConversion> findConversions(String subcategoryCode) {
        return unitConversionRepository.findBySubcategoryCode(subcategoryCode);
    }

    @Override
    public List<UnitConversion> findConversionsFrom(String subcategoryCode, String fromUnitCode) {
        return unitConversionRepository.findBySubcategoryCodeAndFromUnitCode(subcategoryCode, fromUnitCode);
    }

    @Override
    public BigDecimal convert(String subcategoryCode, String fromUnitCode, String toUnitCode, BigDecimal value) {
        if (value == null) return null;
        if (fromUnitCode != null && fromUnitCode.equals(toUnitCode)) {
            return value;
        }
        Optional<UnitConversion> opt = unitConversionRepository
                .findBySubcategoryCodeAndFromUnitCodeAndToUnitCode(subcategoryCode, fromUnitCode, toUnitCode);
        if (opt.isEmpty()) {
            log.warn("单位换算失败：未找到转换关系 subcategoryCode={}, from={}, to={}",
                    subcategoryCode, fromUnitCode, toUnitCode);
            return null;
        }
        BigDecimal factor = opt.get().getConversionFactor();
        return value.multiply(factor).setScale(8, RoundingMode.HALF_UP);
    }

    @Override
    public boolean isValidUnitCode(String unitCode) {
        if (unitCode == null || unitCode.isBlank()) return false;
        return unitStandardRepository.existsByUnitCode(unitCode.trim());
    }

    /**
     * 查询全部单位转换系数（按小类编码、源单位编码升序）
     *
     * @return 转换系数列表；无数据返回空列表
     */
    @Override
    public List<UnitConversion> findAllConversions() {
        List<UnitConversion> list = unitConversionRepository.findAll();
        list.sort(Comparator
                .comparing(UnitConversion::getSubcategoryCode, Comparator.nullsLast(String::compareTo))
                .thenComparing(UnitConversion::getFromUnitCode, Comparator.nullsLast(String::compareTo)));
        return list;
    }

    /**
     * 新增单位转换系数
     * <p>
     * 校验：subcategoryCode/fromUnitCode/toUnitCode 非空且 trim、
     * 源单位与目标单位不能相同、两个单位编码必须为标准单位、
     * 转换系数非空；满足 (subcategoryCode, fromUnitCode, toUnitCode) 唯一约束。
     * 任一校验失败或唯一冲突返回 null。
     *
     * @param req 待新增实体
     * @return 新增后的实体；校验失败或唯一冲突返回 null
     */
    @Override
    @org.springframework.transaction.annotation.Transactional
    public UnitConversion createConversion(UnitConversion req) {
        String subcategory = trim(req.getSubcategoryCode());
        String from = trim(req.getFromUnitCode());
        String to = trim(req.getToUnitCode());
        if (subcategory == null || from == null || to == null) {
            log.warn("新增单位转换系数失败：小类/源单位/目标单位存在空值 subcategory={}, from={}, to={}",
                    req.getSubcategoryCode(), req.getFromUnitCode(), req.getToUnitCode());
            return null;
        }
        if (Objects.equals(from, to)) {
            log.warn("新增单位转换系数失败：源单位与目标单位相同 subcategory={}, unit={}", subcategory, from);
            return null;
        }
        if (!unitStandardRepository.existsByUnitCode(from) || !unitStandardRepository.existsByUnitCode(to)) {
            log.warn("新增单位转换系数失败：单位编码非标准单位 subcategory={}, from={}, to={}", subcategory, from, to);
            return null;
        }
        if (req.getConversionFactor() == null) {
            log.warn("新增单位转换系数失败：转换系数为空 subcategory={}", subcategory);
            return null;
        }
        if (unitConversionRepository.findBySubcategoryCodeAndFromUnitCodeAndToUnitCode(subcategory, from, to).isPresent()) {
            log.warn("新增单位转换系数失败：唯一约束冲突 subcategory={}, from={}, to={}", subcategory, from, to);
            return null;
        }
        req.setSubcategoryCode(subcategory);
        req.setFromUnitCode(from);
        req.setToUnitCode(to);
        UnitConversion saved = unitConversionRepository.save(req);
        log.info("单位转换系数已新增：id={}，subcategory={}, {}->{}，factor={}",
                saved.getId(), subcategory, from, to, saved.getConversionFactor());
        return saved;
    }

    /**
     * 更新单位转换系数（按 id 定位）
     * <p>
     * 可更新字段：fromUnitCode、toUnitCode、conversionFactor、remark。
     * 若更改了 (fromUnitCode, toUnitCode) 组合，需重新做唯一校验。
     *
     * @param id  主键ID
     * @param req 待更新字段
     * @return 更新后的实体；记录不存在或唯一冲突返回 null
     */
    @Override
    @org.springframework.transaction.annotation.Transactional
    public UnitConversion updateConversion(Long id, UnitConversion req) {
        Optional<UnitConversion> opt = unitConversionRepository.findById(id);
        if (opt.isEmpty()) {
            log.warn("更新单位转换系数失败：记录不存在 id={}", id);
            return null;
        }
        UnitConversion entity = opt.get();
        String from = trim(req.getFromUnitCode());
        String to = trim(req.getToUnitCode());
        // 若提供了 from/to，进行校验
        if (from != null || to != null) {
            String newFrom = from != null ? from : entity.getFromUnitCode();
            String newTo = to != null ? to : entity.getToUnitCode();
            if (Objects.equals(newFrom, newTo)) {
                log.warn("更新单位转换系数失败：源单位与目标单位相同 id={}", id);
                return null;
            }
            if (!unitStandardRepository.existsByUnitCode(newFrom) || !unitStandardRepository.existsByUnitCode(newTo)) {
                log.warn("更新单位转换系数失败：单位编码非标准单位 id={}, from={}, to={}", id, newFrom, newTo);
                return null;
            }
            // 唯一性校验（排除自身）
            if (!Objects.equals(newFrom, entity.getFromUnitCode()) || !Objects.equals(newTo, entity.getToUnitCode())) {
                if (unitConversionRepository
                        .findBySubcategoryCodeAndFromUnitCodeAndToUnitCode(entity.getSubcategoryCode(), newFrom, newTo)
                        .isPresent()) {
                    log.warn("更新单位转换系数失败：唯一约束冲突 id={}, {}->{}", id, newFrom, newTo);
                    return null;
                }
            }
            entity.setFromUnitCode(newFrom);
            entity.setToUnitCode(newTo);
        }
        if (req.getConversionFactor() != null) {
            entity.setConversionFactor(req.getConversionFactor());
        }
        if (req.getRemark() != null) {
            entity.setRemark(req.getRemark());
        }
        UnitConversion saved = unitConversionRepository.save(entity);
        log.info("单位转换系数已更新：id={}，subcategory={}, {}->{}，factor={}",
                saved.getId(), saved.getSubcategoryCode(), saved.getFromUnitCode(), saved.getToUnitCode(),
                saved.getConversionFactor());
        return saved;
    }

    /**
     * 删除单位转换系数
     *
     * @param id 主键ID
     * @return 删除成功返回 true；记录不存在返回 false
     */
    @Override
    @org.springframework.transaction.annotation.Transactional
    public boolean deleteConversion(Long id) {
        if (!unitConversionRepository.existsById(id)) {
            log.warn("删除单位转换系数失败：记录不存在 id={}", id);
            return false;
        }
        unitConversionRepository.deleteById(id);
        log.info("单位转换系数已删除：id={}", id);
        return true;
    }

    /**
     * trim 工具：null 返回 null，否则去首尾空白
     */
    private String trim(String s) {
        return s == null ? null : s.trim();
    }
}
