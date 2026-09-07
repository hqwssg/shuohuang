package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.UnitConversion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 单位转换系数 Repository
 */
@Repository
public interface UnitConversionRepository extends JpaRepository<UnitConversion, Long> {

    /**
     * 按小类编码查询全部转换关系
     */
    List<UnitConversion> findBySubcategoryCode(String subcategoryCode);

    /**
     * 按小类+源单位+目标单位精确查询转换系数
     */
    Optional<UnitConversion> findBySubcategoryCodeAndFromUnitCodeAndToUnitCode(
            String subcategoryCode, String fromUnitCode, String toUnitCode);

    /**
     * 按小类+源单位查询所有可转换的目标单位
     */
    List<UnitConversion> findBySubcategoryCodeAndFromUnitCode(
            String subcategoryCode, String fromUnitCode);
}
