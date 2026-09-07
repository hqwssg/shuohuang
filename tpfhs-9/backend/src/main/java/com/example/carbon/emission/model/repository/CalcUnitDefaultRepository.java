package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.CalcUnitDefault;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 碳排放核算能耗缺省单位 Repository
 */
@Repository
public interface CalcUnitDefaultRepository extends JpaRepository<CalcUnitDefault, Long> {

    /**
     * 按排放数据小类编码查询（唯一键）
     */
    Optional<CalcUnitDefault> findBySubcategoryCode(String subcategoryCode);
}
