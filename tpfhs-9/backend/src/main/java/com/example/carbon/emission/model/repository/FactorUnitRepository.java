package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.FactorUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 碳排放因子单位 Repository
 */
@Repository
public interface FactorUnitRepository extends JpaRepository<FactorUnit, Long> {

    /**
     * 按排放数据小类编码+状态查询
     */
    List<FactorUnit> findBySubcategoryCodeAndStatus(String subcategoryCode, Integer status);

    /**
     * 按状态查询全部启用/停用单位
     */
    List<FactorUnit> findAllByStatus(Integer status);

    /**
     * 按多个小类批量查询（用于前端一次拉取多小类的可选因子单位）
     */
    List<FactorUnit> findBySubcategoryCodeInAndStatus(Collection<String> subcategoryCodes, Integer status);
}
