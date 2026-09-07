package com.example.carbon.emission.model.repository;

import com.example.carbon.emission.model.entity.GhgUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 温室气体质量单位 Repository
 */
@Repository
public interface GhgUnitRepository extends JpaRepository<GhgUnit, Long> {

    /**
     * 查询全部启用/停用单位
     */
    List<GhgUnit> findAllByStatus(Integer status);

    /**
     * 按 ghg_code 查询（唯一键）
     */
    Optional<GhgUnit> findByGhgCode(String ghgCode);
}
