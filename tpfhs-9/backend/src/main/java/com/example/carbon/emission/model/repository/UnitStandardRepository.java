package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.UnitStandard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 计量标准单位 Repository
 */
@Repository
public interface UnitStandardRepository extends JpaRepository<UnitStandard, Long> {

    /**
     * 按单位编码查询（唯一键）
     */
    Optional<UnitStandard> findByUnitCode(String unitCode);

    /**
     * 按单位大类查询
     */
    List<UnitStandard> findByUnitCategory(String unitCategory);

    /**
     * 查询所有启用单位
     */
    List<UnitStandard> findByStatus(Integer status);

    /**
     * 判断单位编码是否存在
     */
    boolean existsByUnitCode(String unitCode);
}
